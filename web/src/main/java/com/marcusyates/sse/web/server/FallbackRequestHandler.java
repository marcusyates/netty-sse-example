package com.marcusyates.sse.web.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2CodecUtil;

import static io.netty.buffer.Unpooled.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * Handles the exceptional case where HTTP 1.x was negotiated under TLS.
 */
final class FallbackRequestHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final ByteBuf response = unreleasableBuffer(copiedBuffer("<!DOCTYPE html>"
            + "<html><body><h2>To view the example you need a browser that supports HTTP/2 ("
            + Http2CodecUtil.TLS_UPGRADE_PROTOCOL_NAME
            + ")</h2></body></html>", UTF_8)).asReadOnly();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, EMPTY_BUFFER));
        }

        final ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(response.duplicate());

        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
