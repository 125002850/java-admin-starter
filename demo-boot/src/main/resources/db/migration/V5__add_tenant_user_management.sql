-- 加唯一约束前先跑存量查重；查出数据先清理再继续
-- select tenant_name, count(*) as cnt
-- from sys_tenant_global
-- where deleted = 0
-- group by tenant_name
-- having cnt > 1;
--
-- select tenant_id, username, count(*) as cnt
-- from sys_user
-- where deleted = 0
-- group by tenant_id, username
-- having cnt > 1;

alter table sys_tenant_global
    add constraint uk_sys_tenant_global_name unique (tenant_name);

alter table sys_user
    add column status tinyint not null default 1;

alter table sys_user
    add column display_name varchar(64) null;

alter table sys_user
    add column mobile varchar(32) null;

alter table sys_user
    add column email varchar(128) null;

alter table sys_user
    add constraint uk_sys_user_tenant_username unique (tenant_id, username);
