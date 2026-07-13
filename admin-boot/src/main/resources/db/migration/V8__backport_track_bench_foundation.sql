-- ============================================================
-- V8: backport track-bench foundation schema
-- Only platform foundation tables are adjusted here. Postloan
-- business tables and data are intentionally excluded.
-- ============================================================

alter table sys_dict_type_global
  modify column id bigint not null auto_increment;

alter table sys_dict_type_global
  add column remark varchar(512) null comment '备注' after dict_type_name;

alter table sys_dict_type_global
  add column status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用' after remark;

alter table sys_dict_type_global
  add column version int not null default 0 comment '乐观锁版本号' after status;

alter table sys_dict_item_global
  modify column id bigint not null auto_increment;

alter table sys_dict_item_global
  add column remark varchar(512) null comment '备注' after dict_item_name;

alter table sys_dict_item_global
  add column status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用' after remark;

alter table sys_dict_item_global
  add column sort_order int not null default 0 comment '排序号' after status;

alter table sys_dict_item_global
  add column version int not null default 0 comment '乐观锁版本号' after sort_order;

create index idx_sys_dict_item_global_type_sort
  on sys_dict_item_global (dict_type_code, sort_order);

alter table sys_export_record_global
  modify column id bigint not null auto_increment;

alter table sys_export_record_global
  add column version int not null default 0 comment '乐观锁版本号' after expire_seconds;

alter table sys_export_record_global
  modify column download_count int not null default 0 comment '下载链接获取次数';

alter table sys_export_record_global
  modify column last_download_time datetime null comment '最近获取下载链接时间';

alter table sys_export_record_global
  modify column last_download_by bigint null comment '最近获取下载链接人ID';

create table sys_user_cache (
  user_id bigint not null comment '用户ID',
  user_name varchar(128) default null comment '用户名称',
  user_phone varchar(32) default null comment '用户手机号',
  real_name varchar(128) default null comment '用户真实姓名',
  user_code varchar(64) default null comment '用户编码',
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp on update current_timestamp,
  primary key (user_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='SSO用户缓存表';

insert ignore into sys_dict_type_global (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted) values
('ENABLE_STATUS', '启用状态', '通用启用/禁用状态', 'enable', 0, 0, 0),
('YES_NO', '是否', '通用是否判断', 'enable', 0, 0, 0),
('EXPORT_RECORD_STATUS', '导出记录状态', '导出中心导出记录状态', 'enable', 0, 0, 0),
('EXPORT_DELETE_REASON', '导出删除原因', '导出记录删除原因', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
('ENABLE_STATUS', 'enable', '启用', 'enable', 1, 0, 0, 0),
('ENABLE_STATUS', 'disable', '禁用', 'enable', 2, 0, 0, 0),
('YES_NO', '1', '是', 'enable', 1, 0, 0, 0),
('YES_NO', '0', '否', 'enable', 2, 0, 0, 0),
('EXPORT_RECORD_STATUS', '1', '处理中', 'enable', 1, 0, 0, 0),
('EXPORT_RECORD_STATUS', '2', '成功', 'enable', 2, 0, 0, 0),
('EXPORT_RECORD_STATUS', '3', '失败', 'enable', 3, 0, 0, 0),
('EXPORT_RECORD_STATUS', '4', '已过期', 'enable', 4, 0, 0, 0),
('EXPORT_DELETE_REASON', '1', '手动删除', 'enable', 1, 0, 0, 0),
('EXPORT_DELETE_REASON', '2', '过期清理', 'enable', 2, 0, 0, 0);
