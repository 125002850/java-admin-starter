package com.oigit.admin.core.query.scene;

import com.oigit.admin.core.query.ast.QueryAst;

public interface SceneQueryMapper<ReqDTO> {

    QueryAst toQueryAst(ReqDTO reqDTO);
}
