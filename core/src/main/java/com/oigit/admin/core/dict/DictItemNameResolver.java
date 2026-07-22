package com.oigit.admin.core.dict;

import java.util.Collection;
import java.util.Map;

public interface DictItemNameResolver {

    Map<String, Map<String, String>> resolveItemNames(Collection<String> dictTypeCodes);
}
