-- Every @DictionaryEnum code must exist in the matching global dictionary.
-- Existing migrations are immutable, so the newly introduced DELETED status
-- is added in a forward-only migration.
insert ignore into sys_dict_item_global
  (dict_type_code, dict_item_code, dict_item_name, status, sort_order, create_by, update_by, deleted)
values
  ('EXPORT_RECORD_STATUS', '5', '已删除', 'enable', 5, 0, 0, 0);
