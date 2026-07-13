package com.example.admin.core.export.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.admin.core.export.model.ExportTaskResult;

public interface ExportTaskSubmitter {

    ExportTaskResult submit(String sceneCode, JsonNode query);
}
