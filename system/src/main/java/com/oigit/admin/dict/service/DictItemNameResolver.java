package com.oigit.admin.dict.service;

import java.util.Collection;
import java.util.Map;

public interface DictItemNameResolver {

    Map<String, Map<String, String>> resolveItemNames(Collection<String> dictTypeCodes);
}
