-- V10 removed `deleted` from unique indexes, which prevents a logically
-- deleted record from being recreated. Restore the active-record uniqueness
-- contract for IAM master and relationship tables.

alter table sys_staff_role drop index uk_sys_staff_role;
create unique index uk_sys_staff_role_deleted on sys_staff_role (staff_id, role_id, deleted);

alter table sys_role_menu drop index uk_sys_role_menu;
create unique index uk_sys_role_menu_deleted on sys_role_menu (role_id, menu_id, deleted);

alter table sys_role_data_scope_dept drop index uk_sys_role_scope_dept;
create unique index uk_sys_role_scope_dept_deleted on sys_role_data_scope_dept (role_id, dept_id, deleted);

alter table sys_dept drop index uk_sys_dept_parent_code;
create unique index uk_sys_dept_parent_code_deleted on sys_dept (parent_id, dept_code, deleted);

alter table sys_dept drop index uk_sys_dept_parent_name;
create unique index uk_sys_dept_parent_name_deleted on sys_dept (parent_id, dept_name, deleted);

alter table sys_staff drop index uk_sys_staff_username;
create unique index uk_sys_staff_username_deleted on sys_staff (username, deleted);

alter table sys_staff drop index uk_sys_staff_code;
create unique index uk_sys_staff_code_deleted on sys_staff (staff_code, deleted);

alter table sys_role drop index uk_sys_role_code;
create unique index uk_sys_role_code_deleted on sys_role (role_code, deleted);

alter table sys_role drop index uk_sys_role_name;
create unique index uk_sys_role_name_deleted on sys_role (role_name, deleted);

alter table sys_menu drop index uk_sys_menu_code;
create unique index uk_sys_menu_code_deleted on sys_menu (menu_code, deleted);

alter table sys_menu drop index uk_sys_menu_permission;
create unique index uk_sys_menu_permission_deleted on sys_menu (permission_code, deleted);
