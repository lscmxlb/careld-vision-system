-- 项目体验卡（后台「医院管理 → 项目体验卡」发行，家长端「儿童档案 → 兑换次数」核销）
-- 1) trial_card  体验卡（编号 + 验证码 + 适用范围 + 使用信息）
-- 2) sys_menu    医院管理（parent_id=3）下新增「项目体验卡」菜单
-- 3) sys_role_menu  总部管理员（role_id=7）授权

CREATE TABLE IF NOT EXISTS trial_card (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    card_no            VARCHAR(16)  NOT NULL COMMENT '体验卡编号（11位：2位年份+4位区号+5位顺序号）',
    verify_code        VARCHAR(16)  NOT NULL COMMENT '体验卡验证码（8位数字，末2位为校验位）',
    center_id          BIGINT       NOT NULL COMMENT '适用运营中心ID（必填）',
    agent_id           BIGINT       NULL COMMENT '适用代理商ID（空=该运营中心下全部代理商）',
    store_id           BIGINT       NULL COMMENT '适用医院ID（空=该运营中心/代理商下全部医院）',
    area_code          VARCHAR(8)   NOT NULL COMMENT '城市电话区号（4位，3位区号前补0）',
    seq_no             INT          NOT NULL COMMENT '同前缀（年份+区号）内的5位顺序编号',
    status             TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1已使用',
    used_at            DATETIME     NULL COMMENT '使用时间',
    used_store_id      BIGINT       NULL COMMENT '使用的医院ID（儿童档案所属医院）',
    used_child_id      BIGINT       NULL COMMENT '使用的儿童档案ID',
    used_parent_user_id BIGINT      NULL COMMENT '使用的家长账号ID',
    remark             VARCHAR(255) NULL COMMENT '备注',
    created_by         BIGINT       NULL COMMENT '创建人',
    updated_by         BIGINT       NULL COMMENT '更新人',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at         DATETIME     NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trial_card_no (card_no),
    KEY idx_trial_card_status (status, id),
    KEY idx_trial_card_scope (center_id, agent_id, store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目体验卡';

-- 医院管理 → 项目体验卡
INSERT INTO sys_menu (parent_id, menu_name, menu_type, menu_path, menu_icon, permission_key, sort_order, visible, status)
SELECT 3, '项目体验卡', 2, '/store/trial-card', 'Ticket', 'trialcard:view', 4, 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/store/trial-card');

INSERT INTO sys_role_menu (role_id, menu_id, actions)
SELECT 7, m.id, '["view","create","export"]'
FROM sys_menu m
WHERE m.menu_path = '/store/trial-card'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 7 AND rm.menu_id = m.id);
