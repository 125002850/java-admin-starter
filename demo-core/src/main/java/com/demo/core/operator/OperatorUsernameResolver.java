package com.demo.core.operator;

import java.util.Collection;
import java.util.Map;

/**
 * 批量解析操作者 ID 对应的用户名。
 */
public interface OperatorUsernameResolver {

    Map<Long, String> resolveUsernames(Collection<Long> operatorIds);
}
