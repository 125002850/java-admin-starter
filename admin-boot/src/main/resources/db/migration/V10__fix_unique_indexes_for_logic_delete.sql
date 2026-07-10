-- Fix: Logic delete timestamp can cause unique key conflicts
-- The unique indexes on relationship tables include the "deleted" column,
-- but production uses now() (second-granularity in MySQL < 5.6.4) as the
-- logic-delete value. If two records with the same business key are
-- soft-deleted within the same second, the unique constraint is violated.
-- Fix: drop indexes that include "deleted" and recreate them on the
-- business key columns only. Application code already ensures no duplicate
-- active (deleted=0) records exist.

-- sys_staff_role: (staff_id, role_id, deleted) -> (staff_id, role_id)
alter table sys_staff_role drop index uk_sys_staff_role_deleted;
create unique index uk_sys_staff_role on sys_staff_role (staff_id, role_id);

-- sys_role_menu: (role_id, menu_id, deleted) -> (role_id, menu_id)
alter table sys_role_menu drop index uk_sys_role_menu_deleted;
create unique index uk_sys_role_menu on sys_role_menu (role_id, menu_id);

-- sys_role_data_scope_dept: (role_id, dept_id, deleted) -> (role_id, dept_id)
alter table sys_role_data_scope_dept drop index uk_sys_role_scope_dept_deleted;
create unique index uk_sys_role_scope_dept on sys_role_data_scope_dept (role_id, dept_id);

-- sys_dept: unique keys include "deleted" — same issue
alter table sys_dept drop index uk_sys_dept_parent_code_deleted;
create unique index uk_sys_dept_parent_code on sys_dept (parent_id, dept_code);

alter table sys_dept drop index uk_sys_dept_parent_name_deleted;
create unique index uk_sys_dept_parent_name on sys_dept (parent_id, dept_name);

-- sys_staff: unique keys include "deleted"
alter table sys_staff drop index uk_sys_staff_username_deleted;
create unique index uk_sys_staff_username on sys_staff (username);

alter table sys_staff drop index uk_sys_staff_code_deleted;
create unique index uk_sys_staff_code on sys_staff (staff_code);

-- sys_role: unique keys include "deleted"
alter table sys_role drop index uk_sys_role_code_deleted;
create unique index uk_sys_role_code on sys_role (role_code);

alter table sys_role drop index uk_sys_role_name_deleted;
create unique index uk_sys_role_name on sys_role (role_name);

-- sys_menu: unique keys include "deleted"
alter table sys_menu drop index uk_sys_menu_code_deleted;
create unique index uk_sys_menu_code on sys_menu (menu_code);

alter table sys_menu drop index uk_sys_menu_permission_deleted;
-- permission_code can be NULL, and MySQL allows multiple NULLs in a unique index
create unique index uk_sys_menu_permission on sys_menu (permission_code);