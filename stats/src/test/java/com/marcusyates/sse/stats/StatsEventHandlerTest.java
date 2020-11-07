package com.marcusyates.sse.stats;

import com.marcusyates.sse.stats.StatsEvent;
import com.marcusyates.sse.stats.StatsEventHandler;
import com.marcusyates.sse.stats.SubscriberId;
import com.marcusyates.sse.stats.VmstatEvent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StatsEventHandlerTest {
    private final List<String> messages = new ArrayList<>();
    private final BiConsumer<SubscriberId, String> notifier = (channelId, data) -> messages.add(channelId + " <- " + data);

    @Test
    public void shouldNotPublishWhenNoSubscribers() {
        final StatsEventHandler handler = new StatsEventHandler(notifier, 10);

        handler.handle(new StatsEvent.Data(new VmstatEvent(0L, 0L, 0L)));
        handler.handle(new StatsEvent.Data(new VmstatEvent(1L, 0L, 0L)));

        assertThat(messages.size(), is(0));
    }

    @Test
    public void shouldPublishWhenOneSubscriber() {
        final StatsEventHandler handler = new StatsEventHandler(notifier, 10);

        handler.handle(new StatsEvent.Subscribe(SubscriberId.of(42L), null));
        handler.handle(new StatsEvent.Data(new VmstatEvent(0L, 0L, 0L)));
        handler.handle(new StatsEvent.Data(new VmstatEvent(1L, 0L, 0L)));

        assertThat(messages.size(), is(2));
    }

    @Test
    public void shouldReplayOnSubscribeUptoCachedAmount() {
        final StatsEventHandler handler = new StatsEventHandler(notifier, 2);

        handler.handle(new StatsEvent.Data(new VmstatEvent(0L, 0L, 0L)));
        handler.handle(new StatsEvent.Data(new VmstatEvent(1L, 0L, 0L)));
        handler.handle(new StatsEvent.Data(new VmstatEvent(3L, 0L, 0L)));
        handler.handle(new StatsEvent.Subscribe(SubscriberId.of(42), null));

        assertThat(messages.size(), is(2));
    }

    @Test
    public void shouldStopNotifyingWhenUnsubscribed() {
        final StatsEventHandler handler = new StatsEventHandler(notifier, 10);
        SubscriberId id = SubscriberId.of(42);

        handler.handle(new StatsEvent.Subscribe(id, null));
        handler.handle(new StatsEvent.Data(new VmstatEvent(0L, 0L, 0L)));
        handler.handle(new StatsEvent.Data(new VmstatEvent(1L, 0L, 0L)));
        handler.handle(new StatsEvent.Unsubscribe(id));
        handler.handle(new StatsEvent.Data(new VmstatEvent(2L, 0L, 0L)));
        handler.handle(new StatsEvent.Data(new VmstatEvent(3L, 0L, 0L)));

        assertThat(messages.size(), is(2));
    }
}