package com.careld.notify.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.UserContext;
import com.careld.notify.dto.WechatBindQrVO;
import com.careld.notify.dto.WechatStatusVO;
import com.careld.notify.mapper.NotifySourceMapper;
import com.careld.notify.service.NotifyChannelRuntimeService;
import com.careld.notify.service.WechatBindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 家长端「我的 → 微信公众号」绑定
 *
 * <p>公众号凭据配置齐全时走真实链路：展示带参二维码 → 扫码关注 → 微信回调自动写入 openid
 * （见 {@link WechatBindService}）；未配置凭据时保留模拟绑定，便于功能演示。</p>
 */
@RestController
@RequestMapping("/api/v1/notify/wechat")
@RequiredArgsConstructor
@Tag(name = "微信公众号绑定", description = "家长端微信公众号关注与绑定")
public class NotifyWechatController {

    private static final String SIMULATED_PREFIX = "wxopenid_";

    private final NotifySourceMapper sourceMapper;
    private final NotifyChannelRuntimeService runtimeService;
    private final WechatBindService bindService;

    @GetMapping("/status")
    @Operation(summary = "查询本人微信公众号绑定状态")
    public Result<WechatStatusVO> status() {
        return Result.success(statusOf(currentParentId()));
    }

    @GetMapping("/bind-qr")
    @Operation(summary = "取扫码绑定二维码（带参二维码，扫码关注后自动绑定）")
    public Result<WechatBindQrVO> bindQr() {
        return Result.success(bindService.createBindQr(currentParentId()));
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定微信公众号（未配置凭据时写入模拟 openid）")
    public Result<WechatStatusVO> bind(@RequestBody(required = false) Map<String, String> body) {
        Long userId = currentParentId();
        String openid = body == null ? null : body.get("openid");
        if (!StringUtils.hasText(openid)) {
            if (realChannelReady()) {
                throw new BusinessException(1005, "请用微信扫码关注公众号，关注后会自动完成绑定");
            }
            openid = SIMULATED_PREFIX + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        sourceMapper.bindWechat(userId, openid);
        return Result.success(statusOf(userId));
    }

    @PostMapping("/unbind")
    @Operation(summary = "解除微信公众号绑定")
    public Result<WechatStatusVO> unbind() {
        Long userId = currentParentId();
        sourceMapper.unbindWechat(userId);
        return Result.success(statusOf(userId));
    }

    private WechatStatusVO statusOf(Long userId) {
        Map<String, Object> user = sourceMapper.selectUserById(userId);
        WechatStatusVO vo = new WechatStatusVO();
        vo.setOfficialAccountName(runtimeService.officialAccountName());
        String openid = user == null || user.get("wechatOpenid") == null ? null : String.valueOf(user.get("wechatOpenid"));
        if (StringUtils.hasText(openid)) {
            vo.setBound(true);
            vo.setOpenidTail(openid.length() <= 6 ? openid : openid.substring(openid.length() - 6));
            Object boundAt = user.get("wechatBoundAt");
            if (boundAt instanceof LocalDateTime time) {
                vo.setBoundAt(time);
            } else if (boundAt != null) {
                vo.setBoundAt(LocalDateTime.parse(String.valueOf(boundAt).replace(' ', 'T')));
            }
        }
        return vo;
    }

    /** 公众号凭据是否已配置（配置后绑定只能扫码，避免写入模拟 openid 导致真实发送失败） */
    private boolean realChannelReady() {
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        return StringUtils.hasText(runtime.appId()) && StringUtils.hasText(runtime.appSecret());
    }

    private Long currentParentId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException(1006, "仅家长账号可绑定微信公众号");
        }
        return userId;
    }
}
