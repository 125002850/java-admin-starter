create table sys_tenant_global (
  id bigint primary key,
  tenant_name varchar(128) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除'
);

create table sys_user (
  id bigint primary key,
  tenant_id bigint not null,
  username varchar(64) not null,
  password varchar(255) not null,
  create_time datetime not null,
  update_time datetime not null,
  create_by bigint null,
  update_by bigint null,
  deleted bigint not null default 0 comment '逻辑删除：0未删除，1已删除'
);
