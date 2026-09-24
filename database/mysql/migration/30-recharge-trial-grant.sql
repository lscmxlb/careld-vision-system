-- 短信试用赠送：医院列表「短信试用」入口给医院短信账户赠送额度
--
-- 赠送不经过微信支付，落库即生成一条「已支付」充值记录，备注标注来源（试用赠送）；
-- 微信扫码充值订单该列为空，管理后台充值记录「备注」列据此区分。
ALTER TABLE store_recharge_order
    ADD COLUMN remark VARCHAR(100) NULL COMMENT '备注（如：试用赠送）' AFTER mock_flag;
