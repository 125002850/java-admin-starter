package com.oigit.admin.mdm.export.service;

import com.oigit.admin.core.operator.OperatorContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExportTaskDispatcher {

    private final TaskExecutor taskExecutor;

    public ExportTaskDispatcher(@Qualifier("exportTaskExecutor") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void dispatch(Runnable task) {
        OperatorSnapshot operatorSnapshot = OperatorSnapshot.capture();
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        taskExecutor.execute(() -> runWithContext(task, operatorSnapshot, callerMdc));
    }

    private void runWithContext(
            Runnable task,
            OperatorSnapshot operatorSnapshot,
            Map<String, String> callerMdc
    ) {
        OperatorSnapshot previousOperator = OperatorSnapshot.capture();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        try {
            operatorSnapshot.restore();
            restoreMdc(callerMdc);
            task.run();
        } finally {
            previousOperator.restore();
            restoreMdc(previousMdc);
        }
    }

    private void restoreMdc(Map<String, String> context) {
        MDC.clear();
        if (context != null) {
            MDC.setContextMap(context);
        }
    }

    private record OperatorSnapshot(
            Long operatorId,
            String operatorName,
            String operatorPhone,
            String operatorRealName
    ) {

        private static OperatorSnapshot capture() {
            return new OperatorSnapshot(
                    OperatorContext.getOperatorId(),
                    OperatorContext.getOperatorName(),
                    OperatorContext.getOperatorPhone(),
                    OperatorContext.getOperatorRealName()
            );
        }

        private void restore() {
            OperatorContext.clear();
            OperatorContext.set(operatorId, operatorName, operatorPhone, operatorRealName);
        }
    }
}
