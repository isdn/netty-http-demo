package dev.isdn.demo.netty.server.http.router;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.CharsetUtil;
import io.netty.buffer.Unpooled;
import java.util.Hashtable;

public class Router {

    private Hashtable<HttpMethod, Hashtable<String, RouteHandler>> routes;
    public final static HttpMethod[] allowedMethods = { HttpMethod.GET, HttpMethod.POST, HttpMethod.HEAD };

    public Router() {

        this.routes = new Hashtable<>();
        this.routes.put(HttpMethod.GET, new Hashtable<>());
        this.routes.put(HttpMethod.POST, new Hashtable<>());
    }

    public Router GET(String path, RouteHandler handler) {

        this.routes.get(HttpMethod.GET).put(path, handler);
        return this;
    }

    public Router POST(String path, RouteHandler handler) {

        this.routes.get(HttpMethod.POST).put(path, handler);
        return this;
    }

    public FullHttpResponse process(FullHttpRequest request) {

        String path = request.uri().split("\\?")[0];

        if (HttpMethod.GET.equals(request.method()) || HttpMethod.HEAD.equals(request.method())) {
            if (this.routes.get(HttpMethod.GET).containsKey(path)) {
                return this.routes.get(HttpMethod.GET).get(path).getResponse(request);
            }
        }
        if (HttpMethod.POST.equals(request.method())) {
            if (this.routes.get(HttpMethod.POST).containsKey(path)) {
                return this.routes.get(HttpMethod.POST).get(path).getResponse(request);
            }
        }
        return getNotFound();
    }

    private static FullHttpResponse getNotFound() {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        StringBuilder resp = new StringBuilder()
                .append("\r\nNot found\r\n");
        final ByteBuf buffer = Unpooled.copiedBuffer(resp, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        return response;
    }
}
