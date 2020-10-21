package sse.app;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sse.app.handler.IndexRequestHandler;
import sse.app.handler.VmstatSseRequestHandler;
import sse.stats.StatsEvent;
import sse.stats.StatsEventHandler;
import sse.stats.Vmstat;
import sse.stats.VmstatEvent;
import sse.web.routes.Router;
import sse.web.server.HttpServer;
import sse.web.server.SecureHttp2Server;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

public class VmstatSseApplication {
    private static final Logger logger = LoggerFactory.getLogger(VmstatSseApplication.class);

    private static final int HTTP_PORT = 8080;
    private static final int HTTPS_PORT = 8443;

    public static void main(String[] args) {
        final EventLoopGroup group = new NioEventLoopGroup();
        final DefaultChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        final ExecutorService statsEventListenerExecutorService = Executors.newSingleThreadExecutor();
        final ScheduledExecutorService vmstatsExecutor = Executors.newSingleThreadScheduledExecutor();

        final Vmstat vmstatRunner = new Vmstat();
        final BlockingQueue<StatsEvent> eventsPublisher = new MpscBlockingConsumerArrayQueue<>(4);
        final StatsEventHandler statsEventHandler = new StatsEventHandler(new VmstatSseRequestHandler.MessagePublisher(channels), 20);
        final VmstatSseRequestHandler.ChannelSubscriber channelSubscriber = new VmstatSseRequestHandler.ChannelSubscriber(channels, eventsPublisher);

        final Router routes = Router
                .builder()
                .get("/", new IndexRequestHandler())
                .get("/vmstat", new VmstatSseRequestHandler(channelSubscriber))
                .build();

        try {
            final HttpServer http1 = new HttpServer(group, new InetSocketAddress("localhost", HTTP_PORT), routes);
            final SecureHttp2Server http2 = new SecureHttp2Server(group, new InetSocketAddress("localhost", HTTPS_PORT), routes);

            statsEventListenerExecutorService.execute(() -> {
                try {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        statsEventHandler.handle(eventsPublisher.take());
                    }
                } catch (Exception ex) {
                    logger.error("Something bad happened while handling events", ex);
                    group.shutdownGracefully();
                }
            });

            vmstatsExecutor.scheduleAtFixedRate(
                    () -> {
                        try {
                            final VmstatEvent vmstatEvent = vmstatRunner.getVmstat();
                            final StatsEvent.Data dataEvent = new StatsEvent.Data(vmstatEvent);
                            eventsPublisher.offer(dataEvent);
                        } catch (Exception ex) {
                            logger.error("Something bad happened while trying to push a vmstat event", ex);
                            group.shutdownGracefully();
                        }
                    }, 0, 2, SECONDS);

            http2.start();
            http1.start().sync();
        } catch (Exception e) {
            logger.error("exception occurred", e);
        } finally {
            vmstatsExecutor.shutdown();
            statsEventListenerExecutorService.shutdown();
            group.shutdownGracefully();
        }
    }

}
