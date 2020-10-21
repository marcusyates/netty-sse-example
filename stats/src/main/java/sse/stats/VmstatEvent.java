package sse.stats;

import java.util.Arrays;
import java.util.Map;

import static java.lang.Long.parseLong;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

public class VmstatEvent {
    public final long timestampMillis;
    public final long totalMemoryKb;
    public final long idleCpuTicks;

    public VmstatEvent(long timestampMillis, long totalMemoryKb, long idleCpuTicks) {
        this.timestampMillis = timestampMillis;
        this.totalMemoryKb = totalMemoryKb;
        this.idleCpuTicks = idleCpuTicks;
    }

    public static VmstatEvent parse(long timestampMillis, byte[] bytes) {
        final String[] lines = new String(bytes, UTF_8).split("\n");
        final String firstLine = lines[0];
        final int idx = firstLine.indexOf("k total memory") - 1;

        final Map<String, Long> attributes = Arrays
                .stream(lines)
                .collect(toMap(
                        line -> line.substring(idx).trim(),
                        line -> parseLong(line.substring(0, idx).trim())));

        return new VmstatEvent(
                timestampMillis,
                attributes.get("k total memory"),
                attributes.get("idle cpu ticks"));
    }

    @Override
    public String toString() {
        return "VmstatEvent{" +
                "timestampMillis=" + timestampMillis +
                ", totalMemoryKb=" + totalMemoryKb +
                ", idleCpuTicks=" + idleCpuTicks +
                '}';
    }
}
