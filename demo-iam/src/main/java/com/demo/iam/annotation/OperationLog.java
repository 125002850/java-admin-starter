package com.demo.iam.annotation;

import com.demo.iam.enums.OperationLogAction;
import com.demo.iam.enums.OperationLogModule;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    OperationLogModule module();

    OperationLogAction action();
}
