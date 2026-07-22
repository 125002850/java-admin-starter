package com.oigit.admin.core.export.spi;

import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportMeta;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;

import java.util.List;

public interface ExportHandler<Q> {

    String sceneCode();

    String sceneName();

    Class<Q> queryType();

    void validate(Q query);

    ExportMeta buildMeta(Q query);

    default QueryAst summaryQueryAst(Q query) {
        return null;
    }

    default SceneQueryDefinition<?> summarySceneQueryDefinition() {
        return null;
    }

    List<ExportColumn> columns(Q query);

    List<?> queryRows(Q query);
}
