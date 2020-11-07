package com.marcusyates.sse.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;

@SuppressWarnings("SpellCheckingInspection")
public final class Vmstat {
    private static final Logger logger = LoggerFactory.getLogger(Vmstat.class);
    private static final ProcessBuilder VMSTAT = new ProcessBuilder("vmstat", "--stats", "--unit=k");

    public VmstatEvent getVmstat() {
        final long currentTimeMillis = System.currentTimeMillis();

        try {
            final Process process = VMSTAT.start();

            try (final BufferedInputStream is = new BufferedInputStream(process.getInputStream())) {
                final byte[] bytes = is.readAllBytes();
                return VmstatEvent.parse(currentTimeMillis, bytes);
            } finally {
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed while fetching vmstat", e);
            throw new RuntimeException(e);
        }
    }
}