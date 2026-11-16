package com.oigit.admin.core.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@FunctionalInterface
public interface OutboundHostResolver {

    List<InetAddress> lookup(String hostname) throws UnknownHostException;
}
