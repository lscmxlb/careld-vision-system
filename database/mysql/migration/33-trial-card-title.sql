-- 项目体验卡：新增「发行标题」列（每批发行卡的关键信息，随批次一并落库并在列表/导出展示）
ALTER TABLE trial_card
    ADD COLUMN title VARCHAR(100) NULL COMMENT '发行标题（本批体验卡的关键信息）' AFTER verify_code;
