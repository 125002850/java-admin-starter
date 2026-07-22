package com.oigit.admin.core.export.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.oigit.admin.core.export.model.ExportTaskResult;

public interface ExportTaskSubmitter {

    ExportTaskResult submit(String sceneCode, JsonNode query);
}
