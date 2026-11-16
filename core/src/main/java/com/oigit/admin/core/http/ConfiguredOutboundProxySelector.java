package com.oigit.admin.core.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConfiguredOutboundProxySelector extends ProxySelector {

    private final ProxySelector delegate;
    private final Proxy httpProxy;
    private final Proxy httpsProxy;
    private final List<String> noProxyPatterns;

    ConfiguredOutboundProxySelector(OutboundHttpProxyProperties properties, ProxySelector delegate) {
        this.delegate = delegate;
        this.httpProxy = parseProxy(properties.getHttpUrl());
        this.httpsProxy = parseProxy(properties.getHttpsUrl());
        this.noProxyPatterns = parseNoProxy(properties.getNoProxy());
    }

    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        String host = uri.getHost();
        if (host == null || shouldBypass(host)) {
            return List.of(Proxy.NO_PROXY);
        }
        Proxy configured = "https".equalsIgnoreCase(uri.getScheme()) ? httpsProxy : httpProxy;
        if (configured != null) {
            return List.of(configured);
        }
        return delegate == null ? List.of(Proxy.NO_PROXY) : delegate.select(uri);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException exception) {
        if (delegate != null) {
            delegate.connectFailed(uri, socketAddress, exception);
        }
    }

    private boolean shouldBypass(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalized) || "::1".equals(normalized) || normalized.startsWith("127.")) {
            return true;
        }
        for (String pattern : noProxyPatterns) {
            if ("*".equals(pattern)) {
                return true;
            }
            String suffix = pattern.startsWith("*.") ? pattern.substring(2)
                    : pattern.startsWith(".") ? pattern.substring(1) : pattern;
            if (normalized.equals(suffix) || normalized.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }

    private static Proxy parseProxy(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        final URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("outbound HTTP proxy URL is invalid");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalStateException("outbound HTTP proxy URL must contain a host and no inline credentials");
        }
        Proxy.Type type = switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
            case "http", "https" -> Proxy.Type.HTTP;
            case "socks", "socks5" -> Proxy.Type.SOCKS;
            default -> throw new IllegalStateException("outbound HTTP proxy scheme is unsupported");
        };
        int port = uri.getPort() > 0 ? uri.getPort() : type == Proxy.Type.SOCKS ? 1080 : 80;
        return new Proxy(type, InetSocketAddress.createUnresolved(uri.getHost(), port));
    }

    private static List<String> parseNoProxy(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> patterns = new ArrayList<>();
        for (String item : value.split(",")) {
            String normalized = item.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                patterns.add(stripPort(normalized));
            }
        }
        return List.copyOf(patterns);
    }

    private static String stripPort(String value) {
        int separator = value.lastIndexOf(':');
        if (separator > 0 && value.indexOf(':') == separator) {
            return value.substring(0, separator);
        }
        return value;
    }
}
