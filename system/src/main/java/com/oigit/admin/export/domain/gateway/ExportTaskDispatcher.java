package com.oigit.admin.export.domain.gateway;

/** 将导出任务提交到应用配置的异步执行器。 */
public interface ExportTaskDispatcher {

    void dispatch(Runnable task);
}
