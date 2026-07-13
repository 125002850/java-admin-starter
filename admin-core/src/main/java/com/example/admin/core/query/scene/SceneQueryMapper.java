package com.example.admin.core.query.scene;

import com.example.admin.core.query.ast.QueryAst;

public interface SceneQueryMapper<ReqDTO> {

    QueryAst toQueryAst(ReqDTO reqDTO);
}
