package com.marcusyates.sse.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class StatsEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatsEventHandler.class);

    private final ArrayDeque<String> vmstatEvents = new ArrayDeque<>(10);
    private final Set<SubscriberId> subscribers = new HashSet<>();
    private final BiConsumer<SubscriberId, String> notifier;
    private final int eventsCapacity;
    private long cnt = 0;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatsEventHandler(BiConsumer<SubscriberId, String> notifier, int eventsCapacity) {
        this.notifier = notifier;
        this.eventsCapacity = eventsCapacity;
    }

    public void handle(final StatsEvent event) {
        if (event instanceof StatsEvent.Data) {
            onData((StatsEvent.Data) event);
        } else if (event instanceof StatsEvent.Subscribe) {
            onSubscription((StatsEvent.Subscribe) event);
        } else if (event instanceof StatsEvent.Unsubscribe) {
            onUnsubscription((StatsEvent.Unsubscribe) event);
        }
    }

    private void onData(StatsEvent.Data data) {
        final VmstatEvent vmstatEvent = data.vmstatEvent;
        final String message = MessageFormat.format(
                "event: message\n" +
                        "id: {0}\n" +
                        "data: {1}\n\n",
                cnt++, toJson(vmstatEvent));

        vmstatEvents.addLast(message);
        if (vmstatEvents.size() > eventsCapacity) {
            vmstatEvents.removeFirst();
        }
        subscribers.forEach(subscriberId -> notifier.accept(subscriberId, message));
    }

    private void onSubscription(StatsEvent.Subscribe subscribe) {
        subscribers.add(subscribe.subscriberId);
        vmstatEvents.forEach(message -> notifier.accept(subscribe.subscriberId, message));
    }

    private void onUnsubscription(StatsEvent.Unsubscribe unsubscribe) {
        subscribers.remove(unsubscribe.subscriberId);
    }

    private String toJson(VmstatEvent vmstatEvent) {
        try {
            return objectMapper.writeValueAsString(vmstatEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
