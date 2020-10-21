package sse.web.server;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sse.web.routes.Router;

import javax.net.ssl.SSLHandshakeException;

import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;

class Http2OrHttp1NegotiationHandler extends ApplicationProtocolNegotiationHandler {
    private static final Logger logger = LoggerFactory.getLogger(Http2OrHttp1NegotiationHandler.class);

    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final Router router;

    protected Http2OrHttp1NegotiationHandler(Router router) {
        super(HTTP_1_1); // fallback to http1.1
        this.router = router;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
        if (HTTP_2.equals(protocol)) {
            configureHttp2(ctx, router);
            return;
        }

        if (HTTP_1_1.equals(protocol)) {
            configureHttp1(ctx);
            return;
        }

        throw new IllegalStateException("unknown protocol: " + protocol);
    }

    private static void configureHttp1(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(
                new HttpServerCodec(),
                new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                new FallbackRequestHandler());
    }

    private static void configureHttp2(ChannelHandlerContext ctx, Router router) {
        final ChannelPipeline p = ctx.pipeline();

        p.addLast(Http2FrameCodecBuilder.forServer().build());

        p.addLast(new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                // each stream is allocated a pipeline to handle http2 requests
                ch.pipeline().addLast(
                        new Http2StreamFrameToHttpObjectCodec(true, false),
                        new Http2RequestHandler(router));
            }
        }));

        p.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                logger.debug("received exception", cause);
            }
        });
    }

    protected void handshakeFailure(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SSLHandshakeException) {
            logger.trace("{} TLS handshake failed:", ctx.channel(), cause);
        } else {
            logger.warn("{} TLS handshake failed:", ctx.channel(), cause);
        }
        ctx.close();
    }
}
