package com.marcusyates.sse.app.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import com.marcusyates.sse.web.routes.RequestHandler;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http2.HttpConversionUtil.ExtensionHeaderNames.STREAM_ID;
import static java.nio.charset.StandardCharsets.UTF_8;

public class IndexRequestHandler implements RequestHandler {

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        final ByteBuf content = ByteBufAllocator.DEFAULT.buffer();
        content.writeCharSequence("<!DOCTYPE html><html><head></head><body>hello</body></html>", UTF_8);

        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
        final String streamId = request.headers().get(STREAM_ID.text());
        if (null != streamId) {
            response.headers().set(STREAM_ID.text(), streamId);
        }
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        ctx.writeAndFlush(response);
    }
}
