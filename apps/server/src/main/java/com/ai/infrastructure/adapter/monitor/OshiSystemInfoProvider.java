package com.ai.infrastructure.adapter.monitor;

import com.ai.infrastructure.config.MonitorProperties;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * oshi-based implementation of SystemInfoProvider.
 * Collects OS-level metrics using the oshi library.
 */
@Component
public class OshiSystemInfoProvider implements SystemInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(OshiSystemInfoProvider.class);

    private final oshi.SystemInfo systemInfo;
    private final MonitorProperties monitorProperties;

    public OshiSystemInfoProvider(MonitorProperties monitorProperties) {
        this.monitorProperties = monitorProperties;
        this.systemInfo = new oshi.SystemInfo();
    }

    @Override
    public CpuInfo cpuUsagePercent() {
        try {
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            CentralProcessor cpu = hal.getProcessor();

            // Get CPU ticks before
            long[][] ticksBefore = cpu.getProcessorCpuLoadTicks();
            // Wait for sample interval
            try {
                Thread.sleep(monitorProperties.getCpuSampleSeconds() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Get CPU ticks after
            long[][] ticksAfter = cpu.getProcessorCpuLoadTicks();

            // Calculate system-wide CPU usage from tick deltas
            long user = 0, system = 0, idle = 0;
            for (int i = 0; i < ticksBefore.length; i++) {
                user += ticksAfter[i][CentralProcessor.TickType.USER.getIndex()] - ticksBefore[i][CentralProcessor.TickType.USER.getIndex()];
                system += ticksAfter[i][CentralProcessor.TickType.SYSTEM.getIndex()] - ticksBefore[i][CentralProcessor.TickType.SYSTEM.getIndex()];
                idle += ticksAfter[i][CentralProcessor.TickType.IDLE.getIndex()] - ticksBefore[i][CentralProcessor.TickType.IDLE.getIndex()];
            }
            long total = user + system + idle;
            double userPercent = total > 0 ? (double) user / total * 100 : 0;
            double systemPercent = total > 0 ? (double) system / total * 100 : 0;
            double idlePct = total > 0 ? (double) idle / total * 100 : 0;

            // Per-core load - compute from tick arrays directly
            double[] perCorePercents = new double[ticksBefore.length];
            for (int i = 0; i < ticksBefore.length; i++) {
                long u = ticksAfter[i][CentralProcessor.TickType.USER.getIndex()] - ticksBefore[i][CentralProcessor.TickType.USER.getIndex()];
                long s = ticksAfter[i][CentralProcessor.TickType.SYSTEM.getIndex()] - ticksBefore[i][CentralProcessor.TickType.SYSTEM.getIndex()];
                long id = ticksAfter[i][CentralProcessor.TickType.IDLE.getIndex()] - ticksBefore[i][CentralProcessor.TickType.IDLE.getIndex()];
                long t = u + s + id;
                perCorePercents[i] = t > 0 ? ((double) (u + s) / t * 100) : 0;
            }

            return new CpuInfo(
                cpu.getProcessorIdentifier().getName(),
                cpu.getPhysicalProcessorCount(),
                cpu.getLogicalProcessorCount(),
                systemPercent + userPercent,
                userPercent,
                idlePct,
                perCorePercents
            );
        } catch (Exception e) {
            log.warn("Failed to get CPU info: {}", e.getMessage());
            return new CpuInfo("Unknown", 0, 0, 0, 0, 100, new double[0]);
        }
    }

    @Override
    public MemoryInfo memory() {
        try {
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            GlobalMemory mem = hal.getMemory();

            long total = mem.getTotal();
            long available = mem.getAvailable();
            long used = total - available;
            long swapTotal = mem.getVirtualMemory().getSwapTotal();
            long swapUsed = mem.getVirtualMemory().getSwapUsed();
            double usedPercent = total > 0 ? (double) used / total * 100 : 0;

            // JVM memory
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            if (heapMax < 0) heapMax = heapUsed; // -1 if undefined
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            double heapPercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

            JvmMemoryInfo jvmInfo = new JvmMemoryInfo(heapUsed, heapMax, heapPercent, nonHeapUsed);

            return new MemoryInfo(
                total, used, available,
                swapTotal, swapUsed, swapTotal - swapUsed,
                usedPercent,
                jvmInfo
            );
        } catch (Exception e) {
            log.warn("Failed to get memory info: {}", e.getMessage());
            return new MemoryInfo(0, 0, 0, 0, 0, 0, 0, new JvmMemoryInfo(0, 0, 0, 0));
        }
    }

    @Override
    public List<DiskInfo> disks() {
        List<DiskInfo> result = new ArrayList<>();
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            FileSystem fs = os.getFileSystem();

            for (OSFileStore store : fs.getFileStores()) {
                long total = store.getTotalSpace();
                long free = store.getFreeSpace();
                long used = total - free;
                double usedPercent = total > 0 ? (double) used / total * 100 : 0;

                result.add(new DiskInfo(
                    store.getMount(),
                    store.getName(),
                    store.getType(),
                    total, used, free, usedPercent,
                    store.getTotalInodes(),
                    store.getTotalInodes() - store.getFreeInodes()
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to get disk info: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public LoadInfo loadAverage() {
        try {
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            CentralProcessor cpu = hal.getProcessor();
            double[] loads = cpu.getSystemLoadAverage(3);

            return new LoadInfo(
                loads.length > 0 && loads[0] >= 0 ? loads[0] : 0,
                loads.length > 1 && loads[1] >= 0 ? loads[1] : 0,
                loads.length > 2 && loads[2] >= 0 ? loads[2] : 0,
                Runtime.getRuntime().availableProcessors()
            );
        } catch (Exception e) {
            log.warn("Failed to get load average: {}", e.getMessage());
            return new LoadInfo(0, 0, 0, Runtime.getRuntime().availableProcessors());
        }
    }

    @Override
    public JvmInfo jvm() {
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            if (heapMax < 0) heapMax = heapUsed;
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            double heapPercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            int threads = threadMXBean.getThreadCount();
            int daemonThreads = threadMXBean.getDaemonThreadCount();

            long classesLoaded = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();

            long uptimeSeconds = runtimeMXBean.getUptime() / 1000;

            List<GarbageCollectorInfo> gcInfos = new ArrayList<>();
            for (GarbageCollectorMXBean gc : gcBeans) {
                gcInfos.add(new GarbageCollectorInfo(
                    gc.getName(),
                    gc.getCollectionCount(),
                    gc.getCollectionTime()
                ));
            }

            return new JvmInfo(
                heapUsed, heapMax, heapPercent, nonHeapUsed,
                threads, daemonThreads,
                classesLoaded, uptimeSeconds,
                gcInfos,
                runtimeMXBean.getInputArguments().toArray(new String[0])
            );
        } catch (Exception e) {
            log.warn("Failed to get JVM info: {}", e.getMessage());
            return new JvmInfo(0, 0, 0, 0, 0, 0, 0, 0, List.of(), new String[0]);
        }
    }
}
