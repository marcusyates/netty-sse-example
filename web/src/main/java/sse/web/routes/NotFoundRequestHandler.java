package sse.web.routes;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http2.HttpConversionUtil.ExtensionHeaderNames.STREAM_ID;

public class NotFoundRequestHandler implements RequestHandler {
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND);

        // move into common area
        final String streamId = request.headers().get(STREAM_ID.text());
        if (streamId != null) {
            response.headers().set(STREAM_ID.text(), streamId);
        }

        ctx.writeAndFlush(response);
    }
}
