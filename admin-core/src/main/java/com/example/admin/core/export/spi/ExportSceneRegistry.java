package com.example.admin.core.export.spi;

public interface ExportSceneRegistry {

    ExportHandler<?> getRequired(String sceneCode);
}
