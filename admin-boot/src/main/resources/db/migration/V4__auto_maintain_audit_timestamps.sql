alter table sys_tenant_global
  modify create_time datetime not null default current_timestamp;
alter table sys_tenant_global
  modify update_time datetime not null default current_timestamp;

alter table sys_user
  modify create_time datetime not null default current_timestamp;
alter table sys_user
  modify update_time datetime not null default current_timestamp;

alter table sys_dict_type_global
  modify create_time datetime not null default current_timestamp;
alter table sys_dict_type_global
  modify update_time datetime not null default current_timestamp;

alter table sys_dict_item_global
  modify create_time datetime not null default current_timestamp;
alter table sys_dict_item_global
  modify update_time datetime not null default current_timestamp;

alter table mdm_dict_type
  modify create_time datetime not null default current_timestamp;
alter table mdm_dict_type
  modify update_time datetime not null default current_timestamp;

alter table mdm_dict_item
  modify create_time datetime not null default current_timestamp;
alter table mdm_dict_item
  modify update_time datetime not null default current_timestamp;
