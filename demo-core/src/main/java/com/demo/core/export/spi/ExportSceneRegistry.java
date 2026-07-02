package com.demo.core.export.spi;

public interface ExportSceneRegistry {

    ExportHandler<?> getRequired(String sceneCode);
}
