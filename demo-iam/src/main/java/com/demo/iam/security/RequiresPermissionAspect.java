package com.demo.iam.security;

import com.demo.iam.annotation.RequiresPermission;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequiresPermissionAspect {

    @Before("@annotation(com.demo.iam.annotation.RequiresPermission) || @within(com.demo.iam.annotation.RequiresPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        RequiresPermission annotation = resolveAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }
        IamPrincipal principal = CurrentIam.principal()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("not authenticated"));
        if (!principal.hasPermission(annotation.value())) {
            throw new AccessDeniedException("permission denied: " + annotation.value());
        }
    }

    private RequiresPermission resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresPermission annotation = AnnotatedElementUtils.findMergedAnnotation(
                signature.getMethod(),
                RequiresPermission.class
        );
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(
                signature.getDeclaringType(),
                RequiresPermission.class
        );
    }
}
