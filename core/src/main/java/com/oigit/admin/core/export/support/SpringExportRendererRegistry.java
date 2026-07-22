package com.oigit.admin.core.export.support;

import com.oigit.admin.core.export.spi.ExportRenderer;
import com.oigit.admin.core.export.spi.ExportRendererRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
public class SpringExportRendererRegistry implements ExportRendererRegistry {

    private final Map<String, ExportRenderer> rendererMap;

    public SpringExportRendererRegistry(List<ExportRenderer> renderers) {
        this.rendererMap = new HashMap<>();
        for (ExportRenderer renderer : renderers) {
            String key = normalize(renderer.fileType());
            if (rendererMap.putIfAbsent(key, renderer) != null) {
                throw new IllegalStateException("Duplicate export renderer for fileType: " + key);
            }
        }
    }

    @Override
    public ExportRenderer getRequired(String fileType) {
        ExportRenderer renderer = rendererMap.get(normalize(fileType));
        if (renderer == null) {
            throw new NoSuchElementException("Export renderer not found for fileType: " + fileType);
        }
        return renderer;
    }

    private String normalize(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            throw new IllegalArgumentException("Export renderer fileType must not be blank");
        }
        return fileType.trim().toLowerCase(Locale.ROOT);
    }
}
