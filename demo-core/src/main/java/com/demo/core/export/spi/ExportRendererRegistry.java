package com.demo.core.export.spi;

public interface ExportRendererRegistry {

    ExportRenderer getRequired(String fileType);
}
