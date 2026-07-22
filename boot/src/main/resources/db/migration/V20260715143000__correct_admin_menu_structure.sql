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
