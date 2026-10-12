create table sys_user_query_scheme (
  id bigint not null auto_increment,
  user_id bigint not null,
  scene_code varchar(128) not null,
  scheme_name varchar(64) not null,
  content_json longtext not null,
  schema_version int not null default 1,
  is_default tinyint not null default 0,
  create_time datetime not null default current_timestamp,
  update_time datetime not null default current_timestamp,
  create_by bigint not null default 0,
  update_by bigint not null default 0,
  deleted bigint not null default 0,
  version int not null default 0,
  primary key (id),
  constraint uk_sys_user_query_scheme_name unique (user_id, scene_code, scheme_name, deleted)
);

create index idx_sys_user_query_scheme_scene
  on sys_user_query_scheme (user_id, scene_code, is_default, update_time);

-- 每个用户/场景一行，串行化方案数量及默认方案变更。
create table sys_user_query_scheme_lock (
  user_id bigint not null,
  scene_code varchar(128) not null,
  primary key (user_id, scene_code)
);
