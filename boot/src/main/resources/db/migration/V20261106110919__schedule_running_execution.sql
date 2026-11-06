ALTER TABLE sys_schedule_job_execution_log
    MODIFY COLUMN status varchar(32) NOT NULL COMMENT '执行状态：running执行中，success成功，failed失败';

CREATE INDEX idx_sche_job_exec_log_latest
    ON sys_schedule_job_execution_log (job_id, start_time DESC, id DESC);
