package com.careld.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.notify.dto.RechargeOrderQuery;
import com.careld.notify.entity.StoreRechargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StoreRechargeOrderMapper extends BaseMapper<StoreRechargeOrder> {

    String FILTERS =
            "<if test='storeId != null'>AND o.store_id = #{storeId} </if>" +
            "<if test='status != null'>AND o.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (o.order_no LIKE CONCAT('%', #{keyword}, '%') OR o.transaction_id LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<if test='startTime != null'>AND o.created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND o.created_at &lt;= #{endTime} </if>";

    @Select("SELECT * FROM store_recharge_order WHERE order_no = #{orderNo} AND deleted_at IS NULL LIMIT 1")
    StoreRechargeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 置为已支付：仅 status=0 时可更新，重复回调/查单并发行只会成功一次，保证入账幂等
     */
    @Update("UPDATE store_recharge_order SET status = 1, transaction_id = #{transactionId}, trade_state = #{tradeState}, "
            + "payer_openid = #{payerOpenid}, paid_at = #{paidAt}, mock_flag = #{mockFlag}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = 0 AND deleted_at IS NULL")
    int markPaid(@Param("id") Long id, @Param("transactionId") String transactionId,
                 @Param("tradeState") String tradeState, @Param("payerOpenid") String payerOpenid,
                 @Param("paidAt") LocalDateTime paidAt, @Param("mockFlag") Integer mockFlag);

    /** 关闭订单（微信侧已关单/支付失败/订单过期） */
    @Update("UPDATE store_recharge_order SET status = 2, trade_state = #{tradeState}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = 0 AND deleted_at IS NULL")
    int markClosed(@Param("id") Long id, @Param("tradeState") String tradeState);

    /** 充值记录分页：total 为条数，paidAmount 为范围内已支付金额合计 */
    @Select("<script>SELECT COUNT(*) AS total, IFNULL(SUM(CASE WHEN o.status = 1 THEN o.amount ELSE 0 END), 0) AS paidAmount "
            + "FROM store_recharge_order o WHERE o.deleted_at IS NULL " + FILTERS + "</script>")
    Map<String, Object> summaryByQuery(RechargeOrderQuery query);

    @Select("<script>SELECT o.*, s.store_name AS storeName FROM store_recharge_order o "
            + "LEFT JOIN store_info s ON o.store_id = s.id "
            + "WHERE o.deleted_at IS NULL " + FILTERS
            + " ORDER BY o.id DESC LIMIT #{offset}, #{safeSize}</script>")
    List<StoreRechargeOrder> selectPageByQuery(RechargeOrderQuery query);
}
