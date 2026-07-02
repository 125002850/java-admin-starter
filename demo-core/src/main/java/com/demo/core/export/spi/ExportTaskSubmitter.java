package com.demo.core.export.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.demo.core.export.model.ExportTaskResult;

public interface ExportTaskSubmitter {

    ExportTaskResult submit(String sceneCode, JsonNode query);
}
