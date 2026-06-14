package com.ai.infrastructure.adapter.monitor;

import java.util.List;

/**
 * Provider interface for system information.
 * Abstracts OS-level metrics retrieval for use by monitor tools.
 */
public interface SystemInfoProvider {

    /**
     * Returns CPU usage statistics.
     *
     * @return CpuInfo with usage percentage and per-core load
     */
    CpuInfo cpuUsagePercent();

    /**
     * Returns memory and swap statistics.
     *
     * @return MemoryInfo with total, used, free memory and swap
     */
    MemoryInfo memory();

    /**
     * Returns disk/inode statistics for all mounted filesystems.
     *
     * @return list of DiskInfo for each mounted filesystem
     */
    List<DiskInfo> disks();

    /**
     * Returns system load averages.
     *
     * @return LoadInfo with 1/5/15-minute load averages
     */
    LoadInfo loadAverage();

    /**
     * Returns JVM runtime statistics.
     *
     * @return JvmInfo with heap, non-heap, threads, classes, uptime
     */
    JvmInfo jvm();

    /**
     * CPU information record.
     */
    record CpuInfo(
        String modelName,
        int physicalCores,
        int logicalCores,
        double systemLoadPercent,
        double userLoadPercent,
        double idlePercent,
        double[] perCoreLoadPercent
    ) {}

    /**
     * Memory information record.
     */
    record MemoryInfo(
        long totalBytes,
        long usedBytes,
        long freeBytes,
        long swapTotalBytes,
        long swapUsedBytes,
        long swapFreeBytes,
        double usedPercent,
        JvmMemoryInfo jvm
    ) {}

    /**
     * JVM memory information record.
     */
    record JvmMemoryInfo(
        long heapUsedBytes,
        long heapMaxBytes,
        double heapUsedPercent,
        long nonHeapUsedBytes
    ) {}

    /**
     * Disk/filesystem information record.
     */
    record DiskInfo(
        String mountPoint,
        String name,
        String type,
        long totalBytes,
        long usedBytes,
        long freeBytes,
        double usedPercent,
        long totalInodes,
        long usedInodes
    ) {}

    /**
     * Load average information record.
     */
    record LoadInfo(
        double load1Min,
        double load5Min,
        double load15Min,
        int availableProcessors
    ) {}

    /**
     * JVM runtime information record.
     */
    record JvmInfo(
        long heapUsedBytes,
        long heapMaxBytes,
        double heapUsedPercent,
        long nonHeapUsedBytes,
        int threads,
        int daemonThreads,
        long classesLoaded,
        long uptimeSeconds,
        List<GarbageCollectorInfo> gcCollectors,
        String[] jvmArgs
    ) {}

    /**
     * Garbage collector information record.
     */
    record GarbageCollectorInfo(
        String name,
        long collectionCount,
        long collectionTimeMs
    ) {}
}
