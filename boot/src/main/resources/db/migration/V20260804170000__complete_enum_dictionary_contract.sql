-- Every @DictionaryEnum code must exist in the matching global dictionary.
-- Existing migrations are immutable, so the newly introduced DELETED status
-- is added in a forward-only migration.
insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('EXPORT_RECORD_STATUS', '5', '已删除', 'enable', 5, 0, 0, 0);

-- The local-IAM main branch exposes the following enums through the same
-- dictionary contract used by generated frontend clients.
insert ignore into sys_dict_type_global
  (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted)
values
  ('IAM_STATUS', 'IAM状态', 'IAM 通用启停状态', 'enable', 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'IAM数据范围', 'IAM 角色数据权限范围', 'enable', 0, 0, 0),
  ('IAM_MENU_TYPE', 'IAM菜单类型', 'IAM 菜单节点类型', 'enable', 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM操作日志模块', 'IAM 操作日志 module 字段', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('IAM_STATUS', 'ENABLED', '启用', 'enable', 1, 0, 0, 0),
  ('IAM_STATUS', 'DISABLED', '禁用', 'enable', 2, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'ALL', '全部数据', 'enable', 1, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'DEPT_AND_CHILD', '本部门及子部门', 'enable', 2, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'DEPT_ONLY', '本部门', 'enable', 3, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'SELF', '仅本人', 'enable', 4, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'CUSTOM_DEPT', '自定义部门', 'enable', 5, 0, 0, 0),
  ('IAM_DATA_SCOPE_TYPE', 'MIXED', '混合范围', 'enable', 6, 0, 0, 0),
  ('IAM_MENU_TYPE', 'DIR', '目录', 'enable', 1, 0, 0, 0),
  ('IAM_MENU_TYPE', 'MENU', '菜单', 'enable', 2, 0, 0, 0),
  ('IAM_MENU_TYPE', 'BUTTON', '按钮', 'enable', 3, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_AUTH', 'IAM认证', 'enable', 1, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_STAFF', '员工管理', 'enable', 2, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_DEPT', '部门管理', 'enable', 3, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_ROLE', '角色管理', 'enable', 4, 0, 0, 0),
  ('IAM_OPERATION_LOG_MODULE', 'IAM_MENU', '菜单管理', 'enable', 5, 0, 0, 0);
