CREATE TABLE sys_schedule_job_request_snapshot (
    job_id BIGINT NOT NULL COMMENT '任务ID；每任务只保留最近执行快照',
    execution_id BIGINT NOT NULL COMMENT '执行记录ID',
    start_time DATETIME NOT NULL COMMENT '执行开始时间',
    snapshot_json LONGTEXT NOT NULL COMMENT '有界脱敏请求快照',
    CONSTRAINT pk_sys_schedule_job_request_snapshot PRIMARY KEY (job_id)
) COMMENT='调度出站请求诊断快照；覆盖保存的派生数据';
