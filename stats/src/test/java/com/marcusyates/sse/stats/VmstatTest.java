package com.marcusyates.sse.stats;

import com.marcusyates.sse.stats.Vmstat;
import com.marcusyates.sse.stats.VmstatEvent;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class VmstatTest {
    @Test
    public void shouldGetVmstat() {
        final VmstatEvent vmstatEvent = new Vmstat().getVmstat();
        assertThat(vmstatEvent, notNullValue());
    }
}