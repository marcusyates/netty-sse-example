package com.marcusyates.sse.stats;

import com.marcusyates.sse.stats.VmstatEvent;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class VmstatEventTest {
    private final String vmstatOutput = "" +
            "     67357312 k total memory\n" +
            "      9749639 k used memory\n" +
            "     19906524 k active memory\n" +
            "     10644873 k inactive memory\n" +
            "     31449124 k free memory\n" +
            "      3296645 k buffer memory\n" +
            "     22861906 k swap cache\n" +
            "     33806088 k total swap\n" +
            "        75497 k used swap\n" +
            "     33730588 k free swap\n" +
            "     34341027 non-nice user cpu ticks\n" +
            "      2957667 nice user cpu ticks\n" +
            "     12083698 system cpu ticks\n" +
            "   1318870785 idle cpu ticks\n" +
            "       488646 IO-wait cpu ticks\n" +
            "      1324042 IRQ cpu ticks\n" +
            "      1691568 softirq cpu ticks\n" +
            "            0 stolen cpu ticks\n" +
            "     46294910 pages paged in\n" +
            "    213765591 pages paged out\n" +
            "         1132 pages swapped in\n" +
            "        36447 pages swapped out\n" +
            "   4152561782 interrupts\n" +
            "   2916503941 CPU context switches\n" +
            "   1600187667 boot time\n" +
            "      3024306 forks\n";

    @Test
    public void shouldParseVmstatTotalMemoryKb() {

        final VmstatEvent data = VmstatEvent.parse(System.currentTimeMillis(), vmstatOutput.getBytes(UTF_8));
        assertThat(data.idleCpuTicks, is(1318870785L));
    }

    @Test
    public void shouldParseVmstatIdleCpuTicks() {

        final VmstatEvent data = VmstatEvent.parse(System.currentTimeMillis(), vmstatOutput.getBytes(UTF_8));
        assertThat(data.idleCpuTicks, is(1318870785L));
    }
}