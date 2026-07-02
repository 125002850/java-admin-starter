package com.demo.core.query.scene;

import com.demo.core.query.ast.QueryAst;

public interface SceneQueryMapper<ReqDTO> {

    QueryAst toQueryAst(ReqDTO reqDTO);
}
