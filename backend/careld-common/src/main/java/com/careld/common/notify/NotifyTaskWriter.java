package com.careld.common.notify;

import com.alibaba.fastjson2.JSON;
import com.careld.common.entity.NotifyTask;
import com.careld.common.mapper.NotifyTaskWriteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 业务通知任务写入器
 *
 * <p>业务事件（建档成功 / 预约成功 / 取消 / 调整 / 养护完成）在各自事务内调用，
 * 通知失败或表结构缺失一律降级为 warn，绝不影响主业务。</p>
 */
@Slf4j
@Component
public class NotifyTaskWriter {

    private final NotifyTaskWriteMapper mapper;

    public NotifyTaskWriter(ObjectProvider<NotifyTaskWriteMapper> mapperProvider) {
        this.mapper = mapperProvider.getIfAvailable();
        if (this.mapper == null) {
            log.warn("未找到 NotifyTaskWriteMapper，业务通知任务将不会落库");
        }
    }

    /**
     * 写入一条待发送通知任务
     *
     * @param storeId   医院ID
     * @param childId   儿童ID
     * @param eventType {@link NotifyEventTypes} 中的编码
     * @param refId     关联业务ID（如预约ID），可为空
     * @param payload   业务快照（日期、时段、取消原因、视力值等），可为空
     */
    public void push(Long storeId, Long childId, String eventType, Long refId, Map<String, Object> payload) {
        if (mapper == null || storeId == null || childId == null || eventType == null) {
            return;
        }
        try {
            NotifyTask task = new NotifyTask();
            task.setStoreId(storeId);
            task.setChildId(childId);
            task.setEventType(eventType);
            task.setRefId(refId);
            task.setPayload(payload == null || payload.isEmpty() ? null : JSON.toJSONString(payload));
            task.setStatus(0);
            task.setRetryCount(0);
            mapper.insert(task);
        } catch (Exception e) {
            log.warn("通知任务落库失败 storeId={} childId={} event={}: {}", storeId, childId, eventType, e.getMessage());
        }
    }
}
