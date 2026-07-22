create table mdm_dict_type (
  id bigint primary key,
  tenant_id bigint not null,
  dict_type_code varchar(64) not null,
  dict_type_name varchar(128) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  constraint uk_mdm_dict_type_tenant_code unique (tenant_id, dict_type_code)
);

create table mdm_dict_item (
  id bigint primary key,
  tenant_id bigint not null,
  dict_type_code varchar(64) not null,
  dict_item_code varchar(64) not null,
  dict_item_name varchar(128) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除',
  constraint uk_mdm_dict_item_tenant_type_code unique (tenant_id, dict_type_code, dict_item_code)
);
