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
