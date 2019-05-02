package dev.isdn.demo.netty.server.http.router;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface RouteHandler {

    FullHttpResponse getResponse(FullHttpRequest request);
}
