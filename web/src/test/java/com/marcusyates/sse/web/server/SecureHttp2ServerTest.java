package com.marcusyates.sse.web.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.marcusyates.sse.web.routes.RequestHandler;
import com.marcusyates.sse.web.routes.Router;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http2.HttpConversionUtil.ExtensionHeaderNames.STREAM_ID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class SecureHttp2ServerTest {
    private NioEventLoopGroup executors;

    @Before
    public void setUp() {
        executors = new NioEventLoopGroup();
    }

    @After
    public void tearDown() throws Exception {
        executors.shutdownGracefully().await(5, TimeUnit.SECONDS);
    }

    @Test
    public void shouldGetIndex() throws Exception {
        final Router router = Router.builder()
                .get("/", new IndexHandler())
                .build();

        final SecureHttp2Server server = new SecureHttp2Server(executors, new InetSocketAddress("localhost", 0), router);
        server.start();
        final InetSocketAddress serverAddress = server.getLocalAddress().orElseThrow();

        final Response response =
                createTestHttpClient()
                        .newCall(
                                new Request.Builder()
                                        .url(new URL(
                                                "https",
                                                serverAddress.getHostName(),
                                                serverAddress.getPort(),
                                                ""))
                                        .build())
                        .execute();

        final int code = response.code();
        final String body = Objects.requireNonNull(response.body()).string();

        assertThat(code, equalTo(200));
        assertThat(body, containsString("hello"));
    }

    private static class IndexHandler implements RequestHandler {
        @Override
        public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
            final ByteBuf content = ByteBufAllocator.DEFAULT.buffer();
            content.writeCharSequence("<!DOCTYPE html><html><head></head><body>hello</body></html>", UTF_8);

            final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, content);

            // http2
            final String streamId = request.headers().get(STREAM_ID.text());
            if (null != streamId) {
                response.headers().set(STREAM_ID.text(), streamId);
            }

            response.headers().set("Content-Type", "text/html; charset=UTF-8");
            ctx.writeAndFlush(response);
        }
    }

    private static OkHttpClient createTestHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}