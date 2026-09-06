package com.oigit.admin.core.operator;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析端口；实现必须依据直连对端和可信代理配置决定是否接受转发头。
 */
@FunctionalInterface
public interface ClientIpResolver {

    String resolveClientIp(HttpServletRequest request);
}
