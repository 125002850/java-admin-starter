insert ignore into sys_dict_type_global
  (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted)
values
  ('IAM_OPERATION_LOG_ACTION', '操作日志动作', 'IAM 操作日志 action 字段', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('IAM_OPERATION_LOG_ACTION', 'CREATE', '新增', 'enable', 1, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'UPDATE', '编辑', 'enable', 2, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'DELETE', '删除', 'enable', 3, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'STATUS_UPDATE', '状态变更', 'enable', 4, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'ASSIGN', '分配', 'enable', 5, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'RESET_PASSWORD', '重置密码', 'enable', 6, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'CHANGE_PASSWORD', '修改密码', 'enable', 7, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'LOGIN', '登录', 'enable', 8, 0, 0, 0),
  ('IAM_OPERATION_LOG_ACTION', 'LOGOUT', '退出', 'enable', 9, 0, 0, 0);
