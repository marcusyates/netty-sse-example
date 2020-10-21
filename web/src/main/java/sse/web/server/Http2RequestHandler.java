package sse.web.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sse.web.routes.Router;

@Sharable
class Http2RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(Http2RequestHandler.class);
    private final Router router;

    public Http2RequestHandler(Router router) {
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        logger.info("{} requested {}: {}", ctx.name(), request.method(), request.uri());
        router.route(request.method(), request.uri()).handle(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("exception caught", cause);
        ctx.close();
    }
}
