package com.careld.notify.service;

import com.careld.common.result.PageResult;
import com.careld.notify.dto.NotifyQuery;
import com.careld.notify.dto.NotifyRecordPageVO;
import com.careld.notify.dto.NotifyRecordVO;
import com.careld.notify.entity.NotifyRecord;
import com.careld.notify.entity.StoreNotifyAccount;
import com.careld.notify.mapper.NotifyRecordMapper;
import com.careld.notify.mapper.NotifySourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通知记录查询与落库
 */
@Service
@RequiredArgsConstructor
public class NotifyRecordService {

    private final NotifyRecordMapper recordMapper;
    private final NotifySourceMapper sourceMapper;
    private final NotifyAccountService accountService;
    private final NotifyCipherHelper cipherHelper;

    /** 分页查询：列表 + 当前查询条件下的费用总额 + 账户余额 */
    public NotifyRecordPageVO page(NotifyQuery query) {
        long total = recordMapper.countByQuery(query);
        List<NotifyRecord> rows = total == 0 ? List.of() : recordMapper.selectPageByQuery(query);
        List<NotifyRecordVO> list = rows.stream().map(NotifyRecordVO::of).toList();

        NotifyRecordPageVO vo = new NotifyRecordPageVO();
        vo.setList(list);
        vo.setPagination(new PageResult.Pagination(query.getOffset() / query.getSafeSize() + 1, query.getSafeSize(), total));
        vo.setFilterFee(total == 0 ? BigDecimal.ZERO : recordMapper.sumFeeByQuery(query));

        StoreNotifyAccount account = accountService.getOrCreate(query.getStoreId());
        vo.setBalance(account.getBalance());
        vo.setTotalFee(account.getTotalFee());
        vo.setTotalRecharge(account.getTotalRecharge());
        vo.setSmsUnitPrice(NotifyDispatchService.SMS_UNIT_PRICE);
        return vo;
    }

    /** 按姓名关键字解析命中的儿童ID：脱敏值片段或解密后全名命中 */
    public List<Long> resolveChildIds(Long storeId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String trimmed = keyword.trim();
        return sourceMapper.selectChildNameCandidates(storeId).stream()
                .filter(candidate -> cipherHelper.matchesName(candidate.getNameMask(), candidate.getNameEncrypted(), trimmed))
                .map(NotifySourceMapper.ChildNameCandidate::getId)
                .toList();
    }

    public void save(NotifyRecord record) {
        recordMapper.insert(record);
    }

    public long countByTask(Long taskId) {
        return recordMapper.countByTask(taskId);
    }
}
