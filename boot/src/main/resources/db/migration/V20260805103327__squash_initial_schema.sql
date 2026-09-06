-- ============================================================
-- Squashed initial schema (replaces original V1..V15 migration chain)
--
-- 仅适用于全新数据库初始化：空库执行本文件即得到原 V1..V15
-- 依次执行后的最终 schema 与种子数据。
-- 已执行过旧迁移的库会与 flyway_schema_history 冲突，需 flyway clean 重建。
--
-- 与原迁移链的等价性说明（本文件已省略的中间状态）：
--   - sys_tenant_global / sys_user / mdm_dict_type / mdm_dict_item
--     在原 V6 中被 drop，故不建表；
--   - sys_user_cache 在原 V14 中被 drop，故不建表；
--   - V10 移除含 deleted 的唯一索引、V11 又恢复，最终回到 V9 建表形态，
--     故按 V9 建表保留含 deleted 的唯一索引；
--   - V4 补充的 create_time/update_time 默认值直接写入建表语句。
-- ============================================================

-- ------------------------------------------------------------
-- 全局字典类型（原 V2 建表 + V4 默认时间戳 + V8 扩展字段）
-- ------------------------------------------------------------
create table sys_dict_type_global (
  id bigint primary key auto_increment,
  dict_type_code varchar(64) not null,
  dict_type_name varchar(128) not null,
  remark varchar(512) null comment '备注',
  status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用',
  version int not null default 0 comment '乐观锁版本号',
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  constraint uk_sys_dict_type_global_code unique (dict_type_code)
);

-- ------------------------------------------------------------
-- 全局字典项（原 V2 建表 + V4 默认时间戳 + V8 扩展字段/索引）
-- ------------------------------------------------------------
create table sys_dict_item_global (
  id bigint primary key auto_increment,
  dict_type_code varchar(64) not null,
  dict_item_code varchar(64) not null,
  dict_item_name varchar(128) not null,
  remark varchar(512) null comment '备注',
  status varchar(32) not null default 'enable' comment '状态：enable启用，disable禁用',
  sort_order int not null default 0 comment '排序号',
  version int not null default 0 comment '乐观锁版本号',
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  constraint uk_sys_dict_item_global_type_code unique (dict_type_code, dict_item_code),
  key idx_sys_dict_item_global_type_sort (dict_type_code, sort_order)
);

-- ------------------------------------------------------------
-- 全局导出记录（原 V7 建表 + V8 乐观锁版本号字段）
-- ------------------------------------------------------------
create table sys_export_record_global (
  id bigint primary key auto_increment,
  export_biz_code varchar(64) not null,
  export_biz_name varchar(128) not null,
  file_name varchar(256) not null,
  file_type varchar(32) not null,
  content_type varchar(128) null,
  file_size bigint null,
  object_key varchar(256) null,
  storage_type varchar(32) null,
  status tinyint not null,
  finished_time datetime null,
  expire_time datetime not null,
  deleted_time datetime null,
  delete_reason tinyint null,
  fail_code varchar(64) null,
  fail_message varchar(255) null,
  query_snapshot_json longtext not null,
  query_snapshot_summary varchar(512) not null,
  download_count int not null default 0,
  last_download_time datetime null,
  last_download_by bigint null,
  expire_seconds int not null,
  version int not null default 0 comment '乐观锁版本号',
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  key idx_sys_export_record_global_creator_status_time (create_by, status, create_time),
  key idx_sys_export_record_global_status_expire_time (status, expire_time),
  key idx_sys_export_record_global_biz_code_time (export_biz_code, create_time)
);

-- ------------------------------------------------------------
-- IAM 本地表（原 V9 建表，含 deleted 的唯一索引为最终形态）
-- ------------------------------------------------------------
create table sys_dept (
  id bigint not null auto_increment,
  parent_id bigint null,
  dept_code varchar(64) not null,
  dept_name varchar(128) not null,
  full_path varchar(512) null,
  sort_order int not null default 0,
  status varchar(32) not null default 'ENABLED',
  remark varchar(512) null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_dept_parent_code_deleted (parent_id, dept_code, deleted),
  unique key uk_sys_dept_parent_name_deleted (parent_id, dept_name, deleted),
  key idx_sys_dept_parent_status (parent_id, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM部门';

create table sys_staff (
  id bigint not null auto_increment,
  username varchar(64) not null,
  password_hash varchar(255) not null,
  staff_code varchar(64) not null,
  staff_name varchar(128) not null,
  dept_id bigint not null,
  phone varchar(32) null,
  email varchar(128) null,
  avatar varchar(512) null,
  status varchar(32) not null default 'ENABLED',
  must_change_password tinyint(1) not null default 1,
  password_updated_time datetime not null default current_timestamp,
  remark varchar(512) null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_staff_username_deleted (username, deleted),
  unique key uk_sys_staff_code_deleted (staff_code, deleted),
  key idx_sys_staff_dept_status (dept_id, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM员工';

create table sys_role (
  id bigint not null auto_increment,
  role_code varchar(64) not null,
  role_name varchar(128) not null,
  sort_order int not null default 0,
  status varchar(32) not null default 'ENABLED',
  data_scope_type varchar(32) not null default 'SELF',
  system_builtin tinyint(1) not null default 0,
  remark varchar(512) null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_role_code_deleted (role_code, deleted),
  unique key uk_sys_role_name_deleted (role_name, deleted),
  key idx_sys_role_status_sort (status, sort_order)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM角色';

create table sys_menu (
  id bigint not null auto_increment,
  parent_id bigint null,
  menu_code varchar(128) not null,
  menu_name varchar(128) not null,
  menu_type varchar(32) not null,
  route_path varchar(256) null,
  component_path varchar(256) null,
  icon varchar(128) null,
  sort_order int not null default 0,
  hidden tinyint(1) not null default 0,
  cached tinyint(1) not null default 0,
  status varchar(32) not null default 'ENABLED',
  permission_code varchar(128) null,
  remark varchar(512) null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_menu_code_deleted (menu_code, deleted),
  unique key uk_sys_menu_permission_deleted (permission_code, deleted),
  key idx_sys_menu_parent_status_sort (parent_id, status, sort_order)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM菜单与权限';

create table sys_staff_role (
  id bigint not null auto_increment,
  staff_id bigint not null,
  role_id bigint not null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_staff_role_deleted (staff_id, role_id, deleted),
  key idx_sys_staff_role_role (role_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM员工角色关系';

create table sys_role_menu (
  id bigint not null auto_increment,
  role_id bigint not null,
  menu_id bigint not null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_role_menu_deleted (role_id, menu_id, deleted),
  key idx_sys_role_menu_menu (menu_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM角色菜单关系';

create table sys_role_data_scope_dept (
  id bigint not null auto_increment,
  role_id bigint not null,
  dept_id bigint not null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_role_scope_dept_deleted (role_id, dept_id, deleted),
  key idx_sys_role_scope_dept_dept (dept_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM角色自定义数据权限部门';

create table sys_refresh_token (
  id bigint not null auto_increment,
  staff_id bigint not null,
  token_hash varchar(128) not null,
  session_id varchar(64) not null,
  device_id varchar(64) not null,
  ip varchar(64) null,
  user_agent varchar(512) null,
  issued_time datetime not null,
  expire_time datetime not null,
  last_used_time datetime null,
  revoked_time datetime null,
  revoke_reason varchar(64) null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  unique key uk_sys_refresh_token_hash (token_hash),
  key idx_sys_refresh_token_staff_valid (staff_id, revoked_time, expire_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM refresh token';

create table sys_login_log (
  id bigint not null auto_increment,
  staff_id bigint null,
  username varchar(64) null,
  event_type varchar(32) not null,
  result varchar(32) not null,
  failure_reason varchar(255) null,
  ip varchar(64) null,
  user_agent varchar(512) null,
  token_id varchar(128) null,
  operation_time datetime not null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  key idx_sys_login_log_username_time (username, operation_time),
  key idx_sys_login_log_staff_time (staff_id, operation_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM登录日志';

create table sys_operation_log (
  id bigint not null auto_increment,
  operator_id bigint null,
  operator_username varchar(64) null,
  operator_staff_name varchar(128) null,
  module varchar(64) not null,
  action varchar(64) not null,
  request_path varchar(256) null,
  http_method varchar(16) null,
  request_summary varchar(2048) null,
  response_summary varchar(2048) null,
  success tinyint(1) not null,
  error_message varchar(1024) null,
  ip varchar(64) null,
  user_agent varchar(512) null,
  cost_millis bigint not null default 0,
  operation_time datetime not null,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  key idx_sys_operation_log_operator_time (operator_id, operation_time),
  key idx_sys_operation_log_module_action_time (module, action, operation_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment='IAM操作日志';

-- ------------------------------------------------------------
-- 通用字典种子（原 V8）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- IAM 初始数据（原 V9）
-- ------------------------------------------------------------
insert into sys_dept
  (id, parent_id, dept_code, dept_name, full_path, sort_order, status, remark, create_by, update_by, deleted)
values
  (1, null, 'HQ', '总部', '总部', 1, 'ENABLED', '系统内置根部门', 0, 0, 0);

insert into sys_staff
  (id, username, password_hash, staff_code, staff_name, dept_id, status, must_change_password, password_updated_time, remark, create_by, update_by, deleted)
values
  (1, 'admin', '$2a$10$WbuU43YMwePH06bezaQJBO3NG8OvpJBpN/kq13BLpS.25GE6gfwf2', 'ADMIN', '超级管理员', 1, 'ENABLED', 1, current_timestamp, '系统初始管理员', 0, 0, 0);

insert into sys_role
  (id, role_code, role_name, sort_order, status, data_scope_type, system_builtin, remark, create_by, update_by, deleted)
values
  (1, 'SUPER_ADMIN', '超级管理员', 1, 'ENABLED', 'ALL', 1, '系统内置超级管理员角色', 0, 0, 0);

insert into sys_staff_role
  (id, staff_id, role_id, create_by, update_by, deleted)
values
  (1, 1, 1, 0, 0, 0);

insert into sys_menu
  (id, parent_id, menu_code, menu_name, menu_type, route_path, component_path, icon, sort_order, hidden, cached, status, permission_code, remark, create_by, update_by, deleted)
values
  (1000, null, 'system', '系统管理', 'DIR', '/system', null, 'Settings', 10, 0, 0, 'ENABLED', null, '系统管理目录', 0, 0, 0),
  (1100, 1000, 'iam_staff', '员工管理', 'MENU', '/dashboard/iam/staff', '/dashboard/iam/staff', 'Users', 10, 0, 0, 'ENABLED', 'iam:staff:query', null, 0, 0, 0),
  (1101, 1100, 'iam_staff_create', '新增员工', 'BUTTON', null, null, null, 10, 1, 0, 'ENABLED', 'iam:staff:create', null, 0, 0, 0),
  (1102, 1100, 'iam_staff_update', '编辑员工', 'BUTTON', null, null, null, 20, 1, 0, 'ENABLED', 'iam:staff:update', null, 0, 0, 0),
  (1103, 1100, 'iam_staff_delete', '删除员工', 'BUTTON', null, null, null, 30, 1, 0, 'ENABLED', 'iam:staff:delete', null, 0, 0, 0),
  (1104, 1100, 'iam_staff_password_reset', '重置密码', 'BUTTON', null, null, null, 40, 1, 0, 'ENABLED', 'iam:staff:password:reset', null, 0, 0, 0),
  (1200, 1000, 'iam_dept', '部门管理', 'MENU', '/dashboard/iam/dept', '/dashboard/iam/dept', 'Network', 20, 0, 0, 'ENABLED', 'iam:dept:manage', null, 0, 0, 0),
  (1300, 1000, 'iam_role', '角色管理', 'MENU', '/dashboard/iam/role', '/dashboard/iam/role', 'Shield', 30, 0, 0, 'ENABLED', 'iam:role:manage', null, 0, 0, 0),
  (1400, 1000, 'iam_menu', '菜单管理', 'MENU', '/dashboard/iam/menu', '/dashboard/iam/menu', 'Menu', 40, 0, 0, 'ENABLED', 'iam:menu:manage', null, 0, 0, 0),
  (1500, 1000, 'iam_login_log', '登录日志', 'MENU', '/dashboard/iam/log/login', '/dashboard/iam/log/login', 'FileClock', 50, 0, 0, 'ENABLED', 'iam:log:login:query', null, 0, 0, 0),
  (1600, 1000, 'iam_operation_log', '操作日志', 'MENU', '/dashboard/iam/log/operation', '/dashboard/iam/log/operation', 'ScrollText', 60, 0, 0, 'ENABLED', 'iam:log:operation:query', null, 0, 0, 0),
  (2000, null, 'mdm', '主数据', 'DIR', '/mdm', null, 'Database', 20, 0, 0, 'ENABLED', null, '主数据目录', 0, 0, 0),
  (2100, 2000, 'mdm_dict', '字典管理', 'MENU', '/mdm/dict', '/mdm/dict/index', 'BookOpen', 10, 0, 0, 'ENABLED', 'mdm:dict:manage', null, 0, 0, 0),
  (3000, null, 'system_integration', '系统集成', 'DIR', '/integration', null, 'Boxes', 30, 0, 0, 'ENABLED', null, '系统集成目录', 0, 0, 0),
  (3100, 3000, 'file_storage', '文件管理', 'MENU', '/integration/file', '/integration/file/index', 'FolderUp', 10, 0, 0, 'ENABLED', 'integration:file:manage', null, 0, 0, 0),
  (4000, null, 'export_center', '导出中心', 'MENU', '/export', '/export/index', 'Download', 40, 0, 0, 'ENABLED', 'integration:export:manage', null, 0, 0, 0);

insert into sys_role_menu
  (role_id, menu_id, create_by, update_by, deleted)
select 1, id, 0, 0, 0
from sys_menu
where deleted = 0;

-- ------------------------------------------------------------
-- 操作日志动作字典种子（原 V20260713095526）
-- ------------------------------------------------------------
insert ignore into sys_dict_type_global
  (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted)
values
  ('IAM_OPERATION_LOG_ACTION', '操作日志动作', 'IAM 操作日志 action 字段', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('IAM_OPERATION_LOG_ACTION', 'CREATE', '新增', 'enable', 1, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'UPDATE', '编辑', 'enable', 2, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'DELETE', '删除', 'enable', 3, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'STATUS_UPDATE', '状态变更', 'enable', 4, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'ASSIGN', '分配', 'enable', 5, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'RESET_PASSWORD', '重置密码', 'enable', 6, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'CHANGE_PASSWORD', '修改密码', 'enable', 7, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'LOGIN', '登录', 'enable', 8, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'LOGOUT', '退出', 'enable', 9, 0, 0, 0);

-- ------------------------------------------------------------
-- 登录日志字典种子（原 V20260713103541）
-- ------------------------------------------------------------
insert ignore into sys_dict_type_global
  (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted)
values
  ('IAM_LOGIN_EVENT_TYPE', '登录日志事件', 'IAM 登录日志 event_type 字段', 'enable', 0, 0, 0),
  ('IAM_LOGIN_RESULT', '登录日志结果', 'IAM 登录日志 result 字段', 'enable', 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', '登录日志失败原因', 'IAM 登录日志 failure_reason 字段', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('IAM_LOGIN_EVENT_TYPE', 'LOGIN', '登录', 'enable', 1, 0, 0, 0),
  ('IAM_LOGIN_EVENT_TYPE', 'REFRESH', '刷新令牌', 'enable', 2, 0, 0, 0),
  ('IAM_LOGIN_EVENT_TYPE', 'LOGOUT', '退出登录', 'enable', 3, 0, 0, 0),
  ('IAM_LOGIN_RESULT', 'SUCCESS', '成功', 'enable', 1, 0, 0, 0),
  ('IAM_LOGIN_RESULT', 'FAIL', '失败', 'enable', 2, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'BAD_CREDENTIALS', '用户名或密码错误', 'enable', 1, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'STAFF_DISABLED', '员工已禁用', 'enable', 2, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'REFRESH_TOKEN_INVALID', '刷新令牌无效', 'enable', 3, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'REFRESH_TOKEN_EXPIRED', '刷新令牌已过期', 'enable', 4, 0, 0, 0);

-- ------------------------------------------------------------
-- 枚举字典契约种子（原 V20260804170000，已并入基线）
-- ------------------------------------------------------------
insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('EXPORT_RECORD_STATUS', '5', '已删除', 'enable', 5, 0, 0, 0);

insert ignore into sys_dict_type_global
  (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted)
values
  ('IAM_STATUS', 'IAM状态', 'IAM 通用启停状态', 'enable', 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'IAM数据范围', 'IAM 角色数据权限范围', 'enable', 0, 0, 0),
  ('IAM_MENU_TYPE', 'IAM菜单类型', 'IAM 菜单节点类型', 'enable', 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM操作日志模块', 'IAM 操作日志 module 字段', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('IAM_STATUS', 'ENABLED', '启用', 'enable', 1, 0, 0, 0),
  ('IAM_STATUS', 'DISABLED', '禁用', 'enable', 2, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'ALL', '全部数据', 'enable', 1, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'DEPT_AND_CHILD', '本部门及子部门', 'enable', 2, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'DEPT_ONLY', '本部门', 'enable', 3, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'SELF', '仅本人', 'enable', 4, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'CUSTOM_DEPT', '自定义部门', 'enable', 5, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'MIXED', '混合范围', 'enable', 6, 0, 0, 0),
  ('IAM_MENU_TYPE', 'DIR', '目录', 'enable', 1, 0, 0, 0),
  ('IAM_MENU_TYPE', 'MENU', '菜单', 'enable', 2, 0, 0, 0),
  ('IAM_MENU_TYPE', 'BUTTON', '按钮', 'enable', 3, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_AUTH', 'IAM认证', 'enable', 1, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_STAFF', '员工管理', 'enable', 2, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_DEPT', '部门管理', 'enable', 3, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_ROLE', '角色管理', 'enable', 4, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_MENU', '菜单管理', 'enable', 5, 0, 0, 0);

-- 历史数据兼容：把旧文案失败原因收敛为枚举（原 V20260713103541）
update sys_login_log
set failure_reason = 'REFRESH_TOKEN_INVALID'
where failure_reason in ('refresh token 无效', 'refresh token invalid', '员工不存在');

update sys_login_log
set failure_reason = 'REFRESH_TOKEN_EXPIRED'
where failure_reason in ('refresh token 已过期', 'refresh token expired');

-- ------------------------------------------------------------
-- 菜单结构调整（原 V20260715143000）
-- ------------------------------------------------------------
update sys_menu
set menu_code = 'basic_settings',
    menu_name = '基础设置',
    route_path = '/dashboard/basic-settings',
    icon = 'SlidersHorizontal',
    sort_order = 10,
    remark = '基础设置目录',
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1000
  and deleted = 0;

update sys_menu
set menu_code = 'system_management',
    menu_name = '系统管理',
    route_path = '/dashboard/system-management',
    icon = 'Settings',
    sort_order = 20,
    remark = '系统管理目录',
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 2000
  and deleted = 0;

update sys_menu
set menu_code = 'log_management',
    menu_name = '日志管理',
    route_path = '/dashboard/log-management',
    icon = 'Logs',
    sort_order = 30,
    remark = '日志管理目录',
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 3000
  and deleted = 0;

update sys_menu
set parent_id = 1000,
    route_path = '/dashboard/basic-settings/staff',
    component_path = '/dashboard/basic-settings/staff',
    sort_order = 10,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1100
  and deleted = 0;

update sys_menu
set parent_id = 1000,
    route_path = '/dashboard/basic-settings/dept',
    component_path = '/dashboard/basic-settings/dept',
    sort_order = 20,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1200
  and deleted = 0;

update sys_menu
set parent_id = 1000,
    route_path = '/dashboard/basic-settings/role',
    component_path = '/dashboard/basic-settings/role',
    sort_order = 30,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1300
  and deleted = 0;

update sys_menu
set parent_id = 1000,
    route_path = '/dashboard/basic-settings/menu',
    component_path = '/dashboard/basic-settings/menu',
    sort_order = 40,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1400
  and deleted = 0;

update sys_menu
set parent_id = 2000,
    route_path = '/dashboard/system-management/dictionaries',
    component_path = '/dashboard/system-management/dictionaries',
    sort_order = 10,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 2100
  and deleted = 0;

update sys_menu
set parent_id = 2000,
    route_path = '/dashboard/system-management/export-center',
    component_path = '/dashboard/system-management/export-center',
    sort_order = 20,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 4000
  and deleted = 0;

update sys_menu
set parent_id = 3000,
    route_path = '/dashboard/log-management/login',
    component_path = '/dashboard/log-management/login',
    sort_order = 10,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1500
  and deleted = 0;

update sys_menu
set parent_id = 3000,
    route_path = '/dashboard/log-management/operation',
    component_path = '/dashboard/log-management/operation',
    sort_order = 20,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 1600
  and deleted = 0;

update sys_role_menu
set deleted = id,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where menu_id = 3100
  and deleted = 0;

update sys_menu
set deleted = id,
    update_time = current_timestamp,
    update_by = 0,
    version = version + 1
where id = 3100
  and deleted = 0;
