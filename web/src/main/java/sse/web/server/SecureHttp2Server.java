package sse.web.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sse.web.routes.Router;

import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

import static io.netty.handler.codec.http2.Http2SecurityUtil.CIPHERS;

public class SecureHttp2Server {
    private static final Logger logger = LoggerFactory.getLogger(SecureHttp2Server.class);

    private final EventLoopGroup group;
    private final InetSocketAddress socketAddress;
    private final Router router;
    private InetSocketAddress localAddress;

    public SecureHttp2Server(EventLoopGroup eventLoopGroup, InetSocketAddress socketAddress, Router router) {
        this.group = eventLoopGroup;
        this.socketAddress = socketAddress;
        this.router = router;
    }

    private static SslContext configureTLS() throws SSLException {
        final ApplicationProtocolConfig apn = new ApplicationProtocolConfig(
                Protocol.ALPN,
                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                SelectorFailureBehavior.NO_ADVERTISE,
                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_2,
                ApplicationProtocolNames.HTTP_1_1);

        return SslContextBuilder
                .forServer(
                        getResourceAsStream("tls/server-cert.pem"),
                        getResourceAsStream("tls/server-key.pem"))
                .ciphers(CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(apn)
                .build();
    }

    private static InputStream getResourceAsStream(String fileName) {
        return SecureHttp2Server.class.getClassLoader().getResourceAsStream(fileName);
    }

    @SuppressWarnings("UnusedReturnValue")
    public ChannelFuture start() throws Exception {
        final SslContext sslCtx = configureTLS();

        final ServerBootstrap b = new ServerBootstrap();

        b.option(ChannelOption.SO_BACKLOG, 1024);

        b.group(group);

        b.channel(NioServerSocketChannel.class);

        b.handler(new LoggingHandler(SecureHttp2Server.class, LogLevel.DEBUG));

        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(
                        sslCtx.newHandler(ch.alloc()),
                        new CloseOnExceptionHandler(),
                        new Http2OrHttp1NegotiationHandler(router));
            }
        });

        final Channel ch = b.bind(socketAddress.getAddress(), socketAddress.getPort()).sync().channel();

        localAddress = (InetSocketAddress) ch.localAddress();
        logger.info("open your web browser and navigate to https://{}:{}", localAddress.getHostName(), localAddress.getPort());

        return ch.closeFuture();
    }

    public Optional<InetSocketAddress> getLocalAddress() {
        return Optional.ofNullable(localAddress);
    }

    private static class CloseOnExceptionHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }
}
