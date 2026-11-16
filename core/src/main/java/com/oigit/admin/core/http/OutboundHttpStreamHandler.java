package com.oigit.admin.core.http;

import java.io.IOException;

@FunctionalInterface
public interface OutboundHttpStreamHandler<T> {

    T handle(OutboundHttpStreamResponse response) throws IOException;
}
