package dev.isdn.demo.netty.server.http.router;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.util.CharsetUtil;


public class StatusHandler implements RouteHandler {

    @Override
    public FullHttpResponse getResponse(FullHttpRequest request) {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        StringBuilder resp = new StringBuilder()
                .append("OK\r\n");
        final ByteBuf buffer = Unpooled.copiedBuffer(resp, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        return response;
    }

}
