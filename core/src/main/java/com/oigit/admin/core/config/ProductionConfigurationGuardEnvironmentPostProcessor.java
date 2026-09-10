package com.oigit.admin.core.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在生产配置创建数据源或渠道客户端之前，拒绝继续使用镜像内的开发默认值。
 */
public final class ProductionConfigurationGuardEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    private final Log logger;

    public ProductionConfigurationGuardEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        ProductionConfigurationGuard.VerificationResult result =
                ProductionConfigurationGuard.verify(environment);
        if (result.required()) {
            logger.info("Production configuration source verification passed; protectedPropertyCount="
                    + result.protectedPropertyCount() + "; raw values are omitted");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
