package com.oigit.admin.core.export.support;

import com.oigit.admin.core.export.spi.ExportHandler;
import com.oigit.admin.core.export.spi.ExportSceneRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
public class SpringExportSceneRegistry implements ExportSceneRegistry {

    private final Map<String, ExportHandler<?>> handlerMap;

    public SpringExportSceneRegistry(List<ExportHandler<?>> handlers) {
        this.handlerMap = new HashMap<>();
        for (ExportHandler<?> handler : handlers) {
            String sceneCode = handler.sceneCode();
            if (!StringUtils.hasText(sceneCode)) {
                throw new IllegalStateException("Export sceneCode must not be blank");
            }
            if (handlerMap.putIfAbsent(sceneCode, handler) != null) {
                throw new IllegalStateException("Duplicate export sceneCode: " + sceneCode);
            }
        }
    }

    @Override
    public ExportHandler<?> getRequired(String sceneCode) {
        ExportHandler<?> handler = handlerMap.get(sceneCode);
        if (handler == null) {
            throw new NoSuchElementException("Export scene not found: " + sceneCode);
        }
        return handler;
    }
}
