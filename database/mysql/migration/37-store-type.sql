-- 医院列表新增"医院类型"字段（admin-web 新增/编辑医院弹窗，位于"医院名称"上方，必选，默认社区卫生服务中心）：
-- 1=社区卫生服务中心 2=卫生院 3=妇幼保健院 4=医院 5=其它
-- NOT NULL DEFAULT 1 会自动为存量行回填
ALTER TABLE store_info
    ADD COLUMN store_type TINYINT NOT NULL DEFAULT 1 COMMENT '医院类型 1=社区卫生服务中心 2=卫生院 3=妇幼保健院 4=医院 5=其它' AFTER store_name;
