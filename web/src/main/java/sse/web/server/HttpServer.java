package sse.web.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import sse.web.routes.Router;

import java.net.InetSocketAddress;

public final class HttpServer {
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;

    private final EventLoopGroup group;
    private final InetSocketAddress socketAddress;
    private final Router router;

    public HttpServer(EventLoopGroup eventLoopGroup, InetSocketAddress socketAddress, Router router) {
        this.group = eventLoopGroup;
        this.socketAddress = socketAddress;
        this.router = router;
    }

    public ChannelFuture start() throws Exception {
        final ServerBootstrap b = new ServerBootstrap();

        // requested maximum length of the queue of incoming connections
        b.option(ChannelOption.SO_BACKLOG, 1024);

        // event group for the parent (acceptor)
        b.group(group);

        // create channels with NIO based selector
        b.channel(NioServerSocketChannel.class);

        // log all events
        b.handler(new LoggingHandler(HttpServer.class, LogLevel.DEBUG));

        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(
                        new HttpRequestDecoder(),
                        new HttpResponseEncoder(),
                        new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                        new HttpRequestHandler(router));
            }
        });

        final Channel channel = b
                .bind(socketAddress.getAddress(), socketAddress.getPort())
                .sync()
                .channel();

        return channel.closeFuture();
    }
}
