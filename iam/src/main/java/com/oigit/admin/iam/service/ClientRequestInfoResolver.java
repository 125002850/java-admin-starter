package com.oigit.admin.iam.service;

import com.oigit.admin.iam.config.ClientIpProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class ClientRequestInfoResolver {

    private static final int MAX_IP_SOURCE_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final List<IpAddressMatcher> trustedProxies;

    public ClientRequestInfoResolver(ClientIpProperties properties) {
        trustedProxies = properties.getTrustedProxyCidrs().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(IpAddressMatcher::new)
                .toList();
    }

    public ClientRequestInfo current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new ClientRequestInfo(null, null);
        }
        return resolve(attributes.getRequest());
    }

    public ClientRequestInfo resolve(HttpServletRequest request) {
        String remoteAddress = normalizeIp(request.getRemoteAddr());
        String ip = remoteAddress;
        if (isTrusted(remoteAddress)) {
            List<String> forwardedAddresses = forwardedAddresses(request);
            if (!forwardedAddresses.isEmpty()) {
                ip = resolveForwardedChain(remoteAddress, forwardedAddresses);
            } else {
                String realIp = normalizeIp(request.getHeader("X-Real-IP"));
                if (realIp != null) {
                    ip = realIp;
                }
            }
        }
        return new ClientRequestInfo(ip, truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
    }

    private List<String> forwardedAddresses(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return Arrays.asList(forwardedFor.split(",", -1));
        }
        String forwarded = request.getHeader("Forwarded");
        if (!StringUtils.hasText(forwarded)) {
            return Collections.emptyList();
        }
        return Arrays.stream(forwarded.split(",", -1))
                .map(this::extractForwardedFor)
                .toList();
    }

    private String extractForwardedFor(String element) {
        for (String parameter : element.split(";", -1)) {
            int separator = parameter.indexOf('=');
            if (separator < 0 || !parameter.substring(0, separator).trim().equalsIgnoreCase("for")) {
                continue;
            }
            String value = parameter.substring(separator + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return stripForwardedPort(value);
        }
        return "";
    }

    private String stripForwardedPort(String value) {
        if (value.startsWith("[")) {
            int bracket = value.indexOf(']');
            if (bracket > 0 && (bracket == value.length() - 1 || value.charAt(bracket + 1) == ':')) {
                return value.substring(1, bracket);
            }
        }
        int colon = value.lastIndexOf(':');
        if (colon > 0 && value.indexOf(':') == colon && value.substring(0, colon).contains(".")
                && value.substring(colon + 1).chars().allMatch(Character::isDigit)) {
            return value.substring(0, colon);
        }
        return value;
    }

    private String resolveForwardedChain(String remoteAddress, List<String> forwardedAddresses) {
        String current = remoteAddress;
        for (int i = forwardedAddresses.size() - 1; i >= 0 && isTrusted(current); i--) {
            String candidate = normalizeIp(forwardedAddresses.get(i));
            if (candidate == null) {
                break;
            }
            current = candidate;
        }
        return current;
    }

    private boolean isTrusted(String address) {
        return StringUtils.hasText(address) && trustedProxies.stream().anyMatch(matcher -> matcher.matches(address));
    }

    private String normalizeIp(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String value = source.trim();
        if (value.length() > MAX_IP_SOURCE_LENGTH) {
            return null;
        }
        if (value.indexOf(':') >= 0) {
            return normalizeIpv6(value);
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        StringJoiner normalized = new StringJoiner(".");
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return null;
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) {
                return null;
            }
            normalized.add(Integer.toString(octet));
        }
        return normalized.toString();
    }

    private String normalizeIpv6(String value) {
        if (!value.matches("[0-9A-Fa-f:.]+")) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(value);
            return address.getHostAddress();
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private String truncate(String value, int maxCodePoints) {
        if (value == null) {
            return null;
        }
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
}
