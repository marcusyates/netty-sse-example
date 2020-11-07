package com.marcusyates.sse.stats;

import java.util.Objects;

public class SubscriberId {
    private final Object inner;

    private SubscriberId(Object inner) {
        this.inner = inner;
    }

    public static SubscriberId of(Object inner) {
        return new SubscriberId(inner);
    }

    public Object getInner() {
        return inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriberId that = (SubscriberId) o;
        return Objects.equals(inner, that.inner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
