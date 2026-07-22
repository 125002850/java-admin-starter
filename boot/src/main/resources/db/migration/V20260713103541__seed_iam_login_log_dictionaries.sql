insert ignore into sys_dict_type_global
  (dict_type_code, dict_type_name, remark, status, create_by, update_by, deleted)
values
  ('IAM_LOGIN_EVENT_TYPE', '登录日志事件', 'IAM 登录日志 event_type 字段', 'enable', 0, 0, 0),
  ('IAM_LOGIN_RESULT', '登录日志结果', 'IAM 登录日志 result 字段', 'enable', 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', '登录日志失败原因', 'IAM 登录日志 failure_reason 字段', 'enable', 0, 0, 0);

insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('IAM_LOGIN_EVENT_TYPE', 'LOGIN', '登录', 'enable', 1, 0, 0, 0),
  ('IAM_LOGIN_EVENT_TYPE', 'REFRESH', '刷新令牌', 'enable', 2, 0, 0, 0),
  ('IAM_LOGIN_EVENT_TYPE', 'LOGOUT', '退出登录', 'enable', 3, 0, 0, 0),
  ('IAM_LOGIN_RESULT', 'SUCCESS', '成功', 'enable', 1, 0, 0, 0),
  ('IAM_LOGIN_RESULT', 'FAIL', '失败', 'enable', 2, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'BAD_CREDENTIALS', '用户名或密码错误', 'enable', 1, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'STAFF_DISABLED', '员工已禁用', 'enable', 2, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'REFRESH_TOKEN_INVALID', '刷新令牌无效', 'enable', 3, 0, 0, 0),
  ('IAM_LOGIN_FAILURE_REASON', 'REFRESH_TOKEN_EXPIRED', '刷新令牌已过期', 'enable', 4, 0, 0, 0);

update sys_login_log
set failure_reason = 'REFRESH_TOKEN_INVALID'
where failure_reason in ('refresh token 无效', 'refresh token invalid', '员工不存在');

update sys_login_log
set failure_reason = 'REFRESH_TOKEN_EXPIRED'
where failure_reason in ('refresh token 已过期', 'refresh token expired');
