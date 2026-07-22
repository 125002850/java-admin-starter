package com.oigit.admin.core.export.spi;

public interface ExportRendererRegistry {

    ExportRenderer getRequired(String fileType);
}
