package dev.isdn.demo.netty.server.http;

import dev.isdn.demo.netty.server.http.router.Router;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URLDecoder;
import java.util.Arrays;

public class ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private Router router;

    ServerHandler(Router router) {
        this.router = router;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) { ctx.flush(); }

    // Please keep in mind that #channelRead0(ChannelHandlerContext, I)
    // will be renamed to messageReceived(ChannelHandlerContext, I) in 5.0.
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {

        if (!validateRequest(ctx, request)) {
            return;
        }

        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        final String uri = getSanitizedUri(request.uri());

        if (uri == null) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }
        request.setUri(uri);

        FullHttpResponse response = router.process(request);

        HttpUtil.setContentLength(response, response.content().readableBytes());
        if (HttpMethod.HEAD.equals(request.method())) {
            response.content().clear();
        }

        sendResponse(ctx, response, keepAlive);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static void send302Redirect(ChannelHandlerContext ctx, String newUri, boolean keepAlive) {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        sendResponse(ctx, response, keepAlive);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        sendResponse(ctx, response, false);
    }

    private static void sendResponse(ChannelHandlerContext ctx, FullHttpResponse response, boolean keepAlive) {

        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static boolean validateRequest(ChannelHandlerContext ctx, FullHttpRequest request) {

        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return false;
        }
        if (Arrays.stream(Router.allowedMethods).noneMatch(m -> m.equals(request.method()))) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return false;
        }
        return true;
    }

    private static String getSanitizedUri(String uri) {

        String result;
        try {
            result = URLDecoder.decode(uri, CharsetUtil.UTF_8);
        } catch (NullPointerException | IllegalArgumentException e) {
            return null;
        }
        if (result.isEmpty() || result.charAt(0) != '/') {
            return null;
        }
        return result;
    }
}
