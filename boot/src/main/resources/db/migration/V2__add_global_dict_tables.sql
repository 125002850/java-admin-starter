create table sys_dict_type_global (
  id bigint primary key,
  dict_type_code varchar(64) not null,
  dict_type_name varchar(128) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  constraint uk_sys_dict_type_global_code unique (dict_type_code)
);

create table sys_dict_item_global (
  id bigint primary key,
  dict_type_code varchar(64) not null,
  dict_item_code varchar(64) not null,
  dict_item_name varchar(128) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  constraint uk_sys_dict_item_global_type_code unique (dict_type_code, dict_item_code)
);
