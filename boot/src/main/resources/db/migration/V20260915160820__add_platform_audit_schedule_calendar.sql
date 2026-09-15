create table sys_operation_audit_log (
  id bigint not null auto_increment comment '操作日志ID',
  module_code varchar(100) not null comment '审计模块稳定编码',
  module_name varchar(128) not null comment '操作发生时的模块名称快照',
  action_code varchar(32) not null comment '操作动作编码',
  operation_description varchar(255) not null comment '操作描述快照',
  operator_id bigint null comment '操作人ID快照',
  operator_name varchar(128) null comment '操作人展示名称快照',
  operator_username varchar(128) null comment '操作人用户名快照',
  operator_real_name varchar(128) null comment '操作人真实姓名快照',
  request_method varchar(16) not null comment 'HTTP请求方法',
  request_path varchar(512) not null comment 'HTTP请求路径',
  client_ip varchar(64) null comment '客户端IP地址',
  trace_id varchar(128) null comment '链路追踪ID，仅用于筛选且不建索引',
  request_params text null comment '脱敏且限制为8KB的请求参数',
  result_status varchar(32) not null comment '操作结果状态编码',
  http_status int not null comment 'HTTP响应状态码',
  result_code int null comment '统一响应业务状态码',
  error_message varchar(1000) null comment '用户可见失败信息',
  duration_ms bigint not null comment '请求耗时，单位毫秒',
  operation_time datetime not null comment '操作开始时间',
  create_time datetime not null default current_timestamp comment '创建时间',
  update_time datetime not null default current_timestamp comment '更新时间',
  create_by bigint not null default 0 comment '创建人ID',
  update_by bigint not null default 0 comment '更新人ID',
  deleted bigint not null default 0 comment '逻辑删除标识，操作日志不提供删除入口',
  version int not null default 0 comment '乐观锁版本号',
  primary key (id)
) comment = '全局API操作审计日志表';

create index IDX_SYS_OPERATION_AUDIT_TIME
  on sys_operation_audit_log (operation_time, id);
create index IDX_SYS_OPERATION_AUDIT_MODULE
  on sys_operation_audit_log (module_code, operation_time, id);
create index IDX_SYS_OPERATION_AUDIT_ACTION
  on sys_operation_audit_log (action_code, operation_time, id);
create index IDX_SYS_OPERATION_AUDIT_OPERATOR
  on sys_operation_audit_log (operator_id, operation_time, id);
create index IDX_SYS_OPERATION_AUDIT_STATUS
  on sys_operation_audit_log (result_status, operation_time, id);


CREATE TABLE `sys_schedule_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '定时任务主键ID',
  `job_name` varchar(128) NOT NULL COMMENT '任务名称',
  `job_code` varchar(64) NOT NULL COMMENT '任务编码',
  `default_cron` varchar(64) DEFAULT NULL COMMENT '默认cron表达式',
  `cron_expression` varchar(64) DEFAULT NULL COMMENT 'cron表达式（当前生效）',
  `invoke_route` varchar(256) DEFAULT NULL COMMENT '调用路由（接口URL或接口路径-反射调用）',
  `status` varchar(32) NOT NULL DEFAULT 'enable' COMMENT '启用状态：enable启用，disable禁用',
  `lock_lease_seconds` int NOT NULL DEFAULT '60' COMMENT '锁租约(秒)',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `group_name` varchar(64) DEFAULT NULL COMMENT '任务分组',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，非0为删除时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_schedule_job_code` (`job_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_schedule_job_execution_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '定时任务执行日志主键ID',
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `job_code` varchar(64) NOT NULL COMMENT '任务编码',
  `job_name` varchar(128) NOT NULL COMMENT '任务名称',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` varchar(32) NOT NULL COMMENT '执行状态：success成功，failed失败',
  `error_message` varchar(2048) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint NOT NULL DEFAULT 0,
  `update_by` bigint NOT NULL DEFAULT 0,
  `deleted` bigint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sche_job_exec_log_job_id` (`job_id`),
  KEY `idx_sche_job_exec_log_job_code` (`job_code`),
  KEY `idx_sche_job_exec_log_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_schedule_job_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '定时任务操作日志主键ID',
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `job_code` varchar(64) NOT NULL COMMENT '任务编码',
  `job_name` varchar(128) NOT NULL COMMENT '任务名称',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：CREATE-新增，UPDATE-修改，DELETE-删除，ENABLE-启用，DISABLE-禁用',
  `operation_content` text COMMENT '操作内容（JSON格式）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint NOT NULL DEFAULT 0,
  `update_by` bigint NOT NULL DEFAULT 0,
  `deleted` bigint NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sche_job_op_log_job_id` (`job_id`),
  KEY `idx_sche_job_op_log_job_code` (`job_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_work_calendar_date_override` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作日历特殊日期主键ID',
  `calendar_version_id` bigint NOT NULL COMMENT '工作日历版本ID',
  `calendar_date` date NOT NULL COMMENT '特殊日期',
  `override_type` varchar(32) NOT NULL COMMENT '日期覆盖类型',
  `date_name` varchar(128) DEFAULT NULL COMMENT '日期名称',
  `custom_periods_json` longtext COMMENT '自定义工作时段（JSON数组）',
  `source_note` varchar(256) DEFAULT NULL COMMENT '日期来源说明',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，非0为删除时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_work_calendar_date_deleted` (`calendar_version_id`,`calendar_date`,`deleted`),
  KEY `idx_sys_work_calendar_date_type` (`calendar_version_id`,`override_type`,`calendar_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作日历特殊日期覆盖';

CREATE TABLE `sys_work_calendar_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作日历版本主键ID',
  `calendar_year_id` bigint NOT NULL COMMENT '工作日历年度记录ID',
  `calendar_year` smallint NOT NULL COMMENT '日历年份',
  `version_no` int NOT NULL COMMENT '版本号',
  `status` varchar(32) NOT NULL COMMENT '工作日历版本状态',
  `standard_periods_json` longtext NOT NULL COMMENT '标准工作时段（JSON数组）',
  `content_hash` char(64) NOT NULL COMMENT '内容快照哈希值',
  `published_by` bigint DEFAULT NULL COMMENT '发布人ID',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，非0为删除时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_work_calendar_version_no_deleted` (`calendar_year_id`,`version_no`,`deleted`),
  KEY `idx_sys_work_calendar_version_year_status_no` (`calendar_year_id`,`status`,`version_no`),
  KEY `idx_sys_work_calendar_version_published_at` (`status`,`published_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作日历年度版本';

CREATE TABLE `sys_work_calendar_year` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作日历年度主键ID',
  `calendar_year` smallint NOT NULL COMMENT '日历年份',
  `active_version_id` bigint DEFAULT NULL COMMENT '当前生效的日历版本ID',
  `draft_version_id` bigint DEFAULT NULL COMMENT '当前草稿版本ID',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，非0为删除时间戳',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_work_calendar_year_deleted` (`calendar_year`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作日历年度版本入口';

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_YEAR_VIEW', 'WORK_CALENDAR_YEAR_VIEW', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_YEAR_VIEW','draft','草稿',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_YEAR_VIEW','active','当前生效版本',2,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_DATE_OVERRIDE_TYPE', 'WORK_CALENDAR_DATE_OVERRIDE_TYPE', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DATE_OVERRIDE_TYPE','adjusted_workday','调休工作日',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DATE_OVERRIDE_TYPE','public_holiday','法定节假日',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DATE_OVERRIDE_TYPE','other_non_working_day','其他非工作日',3,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_EFFECTIVE_DAY_KIND', 'WORK_CALENDAR_EFFECTIVE_DAY_KIND', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_EFFECTIVE_DAY_KIND','regular_workday','普通工作日',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_EFFECTIVE_DAY_KIND','adjusted_workday','调休工作日',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_EFFECTIVE_DAY_KIND','weekend','周末',3,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_EFFECTIVE_DAY_KIND','public_holiday','法定节假日',4,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_EFFECTIVE_DAY_KIND','other_non_working_day','其他非工作日',5,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_CLASSIFICATION_BASIS', 'WORK_CALENDAR_CLASSIFICATION_BASIS', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_CLASSIFICATION_BASIS','published_snapshot','已发布年度快照',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_CLASSIFICATION_BASIS','weekly_fallback','每周规则临时推导',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_CLASSIFICATION_BASIS','saved_draft','已保存草稿',3,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_VERSION_STATUS', 'WORK_CALENDAR_VERSION_STATUS', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_VERSION_STATUS','draft','草稿',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_VERSION_STATUS','published','已发布',2,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_DAY_TRAIT', 'WORK_CALENDAR_DAY_TRAIT', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DAY_TRAIT','weekend','周末',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DAY_TRAIT','public_holiday','法定节假日',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DAY_TRAIT','adjusted_workday','调休工作日',3,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_DAY_TRAIT','other_non_working_day','其他非工作日',4,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_HOLIDAY_STATUS', 'WORK_CALENDAR_HOLIDAY_STATUS', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_HOLIDAY_STATUS','confirmed','已确认',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_HOLIDAY_STATUS','candidate','草稿候选',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_HOLIDAY_STATUS','unknown','未知',3,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_TIME_CLASSIFICATION', 'WORK_CALENDAR_TIME_CLASSIFICATION', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_TIME_CLASSIFICATION','working_time','工作时间',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_TIME_CLASSIFICATION','workday_break','工作日休息时段',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_TIME_CLASSIFICATION','weekend','周末',3,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_TIME_CLASSIFICATION','public_holiday','法定节假日',4,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_TIME_CLASSIFICATION','other_non_working_day','其他非工作日',5,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('WORK_CALENDAR_PUBLICATION_CHANGE_TYPE', 'WORK_CALENDAR_PUBLICATION_CHANGE_TYPE', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_PUBLICATION_CHANGE_TYPE','added','新增',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_PUBLICATION_CHANGE_TYPE','removed','删除',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('WORK_CALENDAR_PUBLICATION_CHANGE_TYPE','changed','修改',3,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('OPERATION_AUDIT_ACTION', 'OPERATION_AUDIT_ACTION', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','create','新增',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','update','修改',2,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','delete','删除',3,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','status_change','状态变更',4,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','confirm','确认',5,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','publish','发布',6,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','archive','归档',7,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','sync','同步',8,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','import','导入',9,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','export','导出',10,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','upload','上传',11,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','download','下载',12,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_ACTION','execute','执行',13,'enable');

insert into sys_dict_type_global (dict_type_code, dict_type_name, status) values ('OPERATION_AUDIT_STATUS', 'OPERATION_AUDIT_STATUS', 'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_STATUS','success','成功',1,'enable');
insert into sys_dict_item_global (dict_type_code, dict_item_code, dict_item_name, sort_order, status) values ('OPERATION_AUDIT_STATUS','failed','失败',2,'enable');
