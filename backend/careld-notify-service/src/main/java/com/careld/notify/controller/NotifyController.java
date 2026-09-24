package com.careld.notify.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.DataScopeHelper;
import com.careld.notify.dto.NotifyConfigRequest;
import com.careld.notify.dto.NotifyConfigVO;
import com.careld.notify.dto.NotifyQuery;
import com.careld.notify.dto.NotifyRecordPageVO;
import com.careld.notify.entity.StoreNotifyAccount;
import com.careld.notify.entity.StoreNotifyConfig;
import com.careld.notify.service.NotifyAccountService;
import com.careld.notify.service.NotifyConfigService;
import com.careld.notify.service.NotifyRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 医院端「系统设置 → 通知服务」：通道开关 / 通知类型 / 通知记录 / 费用账户
 */
@RestController
@RequestMapping("/api/v1/notify")
@RequiredArgsConstructor
@Tag(name = "通知服务", description = "医院端通知服务配置、通知记录与短信计费")
public class NotifyController {

    private final NotifyConfigService configService;
    private final NotifyRecordService recordService;
    private final NotifyAccountService accountService;

    @GetMapping("/config")
    @Operation(summary = "查询通知服务配置")
    public Result<NotifyConfigVO> config(@RequestParam(required = false) Long storeId) {
        return Result.success(toVO(configService.getOrCreate(resolveStoreId(storeId))));
    }

    @PutMapping("/config")
    @Operation(summary = "保存通知服务配置")
    public Result<NotifyConfigVO> saveConfig(@RequestParam(required = false) Long storeId,
                                            @RequestBody NotifyConfigRequest request) {
        return Result.success(toVO(configService.save(resolveStoreId(storeId), request)));
    }

    @GetMapping("/records")
    @Operation(summary = "分页查询通知记录（含费用总额与可用余额）")
    public Result<NotifyRecordPageVO> records(NotifyQuery query) {
        query.setStoreId(resolveStoreId(query.getStoreId()));
        String keyword = StringUtils.hasText(query.getChildNameLike()) ? query.getChildNameLike().trim() : null;
        if (StringUtils.hasText(keyword)) {
            List<Long> childIds = recordService.resolveChildIds(query.getStoreId(), keyword);
            // 未命中任何儿童时不再拼 IN 条件，交由脱敏姓名快照模糊匹配兜底
            query.setChildIds(childIds.isEmpty() ? null : childIds);
            query.setChildNameLike(keyword);
        }
        return Result.success(recordService.page(query));
    }

    @GetMapping("/account")
    @Operation(summary = "查询短信费用账户")
    public Result<StoreNotifyAccount> account(@RequestParam(required = false) Long storeId) {
        return Result.success(accountService.getOrCreate(resolveStoreId(storeId)));
    }

    private NotifyConfigVO toVO(StoreNotifyConfig config) {
        NotifyConfigVO vo = new NotifyConfigVO();
        vo.setStoreId(config.getStoreId());
        vo.setSmsEnabled(config.getSmsEnabled());
        vo.setWechatEnabled(config.getWechatEnabled());
        vo.setEnabledTypes(NotifyConfigService.parseTypes(config.getEnabledTypes()));
        return vo;
    }

    /** 门店维护类用户强制取本人医院，总部/超管需显式指定医院 */
    private Long resolveStoreId(Long paramStoreId) {
        Long storeId = DataScopeHelper.resolveStoreId(paramStoreId);
        if (storeId == null) {
            throw new BusinessException(400, "未指定医院");
        }
        return storeId;
    }
}
