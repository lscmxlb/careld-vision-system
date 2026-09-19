package com.careld.common.log;

import com.careld.common.entity.SysOperationLog;
import com.careld.common.mapper.OperationLogWriteMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 操作日志异步写入器
 *
 * <p>日志落库不得影响业务：入队与写库异常一律降级为 warn，不向外抛出；
 * 队列满时丢弃最旧任务，避免阻塞请求线程。Mapper 缺失时静默降级（仅在启动日志告警）。</p>
 */
@Slf4j
@Component
public class OperationLogWriter {

    private static final int QUEUE_CAPACITY = 5000;

    private final OperationLogWriteMapper mapper;
    private final ThreadPoolExecutor executor;

    public OperationLogWriter(ObjectProvider<OperationLogWriteMapper> mapperProvider) {
        this.mapper = mapperProvider.getIfAvailable();
        if (this.mapper == null) {
            log.warn("未找到 OperationLogWriteMapper，操作日志将不会落库");
        }
        this.executor = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "careld-operation-log");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * 异步写入一条操作日志
     */
    public void write(SysOperationLog entity) {
        if (mapper == null || entity == null) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    mapper.insert(entity);
                } catch (Exception e) {
                    log.warn("操作日志落库失败: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("操作日志入队失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
