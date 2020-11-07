package com.marcusyates.sse.stats;


public interface StatsEvent {
    class Data implements StatsEvent {
        public final VmstatEvent vmstatEvent;

        public Data(VmstatEvent vmstatEvent) {
            this.vmstatEvent = vmstatEvent;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "vmstatEvent=" + vmstatEvent +
                    '}';
        }
    }

    class Subscribe implements StatsEvent {
        public final SubscriberId subscriberId;
        public final String lastEventId;

        public Subscribe(SubscriberId subscriberId, String lastEventId) {
            this.subscriberId = subscriberId;
            this.lastEventId = lastEventId;
        }

        @Override
        public String toString() {
            return "Subscribe{" +
                    "subscriberId=" + subscriberId +
                    ", lastEventId='" + lastEventId + '\'' +
                    '}';
        }

    }

    class Unsubscribe implements StatsEvent {
        public final SubscriberId subscriberId;

        public Unsubscribe(SubscriberId subscriberId) {
            this.subscriberId = subscriberId;
        }

        @Override
        public String toString() {
            return "Unsubscribe{" +
                    "subscriberId=" + subscriberId +
                    '}';
        }
    }
}
