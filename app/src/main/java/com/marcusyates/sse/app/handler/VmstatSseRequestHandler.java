package com.marcusyates.sse.app.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marcusyates.sse.stats.StatsEvent;
import com.marcusyates.sse.stats.SubscriberId;
import com.marcusyates.sse.web.routes.RequestHandler;

import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http2.HttpConversionUtil.ExtensionHeaderNames.STREAM_ID;
import static java.nio.charset.StandardCharsets.UTF_8;

public class VmstatSseRequestHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(VmstatSseRequestHandler.class);
    private final ChannelSubscriber channelSubscriber;

    public VmstatSseRequestHandler(final ChannelSubscriber channelSubscriber) {
        this.channelSubscriber = channelSubscriber;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        final DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        final HttpHeaders headers = response.headers();

        final String streamId = request.headers().get(STREAM_ID.text());
        if (null != streamId) {
            headers.set(STREAM_ID.text(), streamId);
        }

        headers.set("Content-Type", "text/event-stream; charset=UTF-8");
        headers.set("Connection", "keep-alive");
        headers.set("Cache-Control", "no-cache");
        headers.set("Access-Control-Allow-Origin", "*");

        ctx.writeAndFlush(response);

        // client may choose to resume from an event, with SSE's last-event-id
        final String lastEventId = request.headers().get("last-event-id");

        ctx.channel().closeFuture().addListener(f -> channelSubscriber.unsubscribe(ctx.channel()));
        channelSubscriber.subscribe(ctx.channel(), lastEventId);
    }

    public static class ChannelSubscriber {
        private final DefaultChannelGroup channels;
        private final BlockingQueue<StatsEvent> queue;

        public ChannelSubscriber(DefaultChannelGroup channels, BlockingQueue<StatsEvent> queue) {
            this.channels = channels;
            this.queue = queue;
        }

        public void subscribe(Channel channel, String lastEventId) {
            channels.add(channel);
            queue.offer(new StatsEvent.Subscribe(SubscriberId.of(channel.id()), lastEventId));
        }

        public void unsubscribe(Channel channel) {
            channels.remove(channel);
            queue.offer(new StatsEvent.Unsubscribe(SubscriberId.of(channel.id())));
        }
    }

    public static class MessagePublisher implements BiConsumer<SubscriberId, String> {
        private final DefaultChannelGroup channels;

        public MessagePublisher(DefaultChannelGroup channels) {
            this.channels = channels;
        }

        @Override
        public void accept(final SubscriberId subscriberId, final String data) {
            if (subscriberId.getInner() instanceof ChannelId) {
                try {
                    channels.close(cm -> !cm.isWritable()).sync();
                    if (channels.isEmpty()) {
                        return;
                    }

                    final Channel channel = channels.find((ChannelId) subscriberId.getInner());
                    if (channel == null) {
                        return;
                    }

                    final ByteBuf content = ByteBufAllocator.DEFAULT.buffer();
                    content.writeBytes(data.getBytes(UTF_8));
                    channel.writeAndFlush(new DefaultHttpContent(content));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
