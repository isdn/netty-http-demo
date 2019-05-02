package dev.isdn.demo.netty.server.http;

import dev.isdn.demo.netty.server.http.router.RouteHandler;
import dev.isdn.demo.netty.server.http.router.Router;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

class ServerHandlerTest {

    private final static String TEST_RESPONSE = "%testing%";
    private final static String TEST_URI = "/test";
    private Router router;
    private EmbeddedChannel channel;


    @BeforeAll
    static void prepare() {

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    @BeforeEach
    void setUp() {

        assertEquals(ResourceLeakDetector.Level.PARANOID, ResourceLeakDetector.getLevel());

        router = new Router();
        router.GET(TEST_URI, new TestHandler());

        channel = new EmbeddedChannel();
        channel.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(65536))
                .addLast(new HttpContentCompressor())
                .addLast(new ServerHandler(router));
    }

    @AfterEach
    void tearDown() {

        router = null;
        channel = null;
    }

    @Test
    void testGetRouterHandler200() {

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, TEST_URI);
        httpRequest.headers().add("Connection", "close");
        // writeInbound returns true only if we hit the end of the pipeline queue
        // in this case we can readInbound
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains(TEST_RESPONSE));
        response.release();
    }

    @Test
    void testGetRouterHandler200WithParams() {

        final String uri = TEST_URI.concat("?foo=bar#123");
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains(TEST_RESPONSE));
        response.release();
    }

    @Test
    void testGetRouterHandler404() {

        final String uri = "/testing404";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("404 Not Found"));
        response.release();
    }

    @Test
    void testGetRouterHandler404Root() {

        final String uri = "/";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("404 Not Found"));
        response.release();
    }

    @Test
    void testGetRouterHandler403NoUri() {

        final String uri = "";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("403 Forbidden"));
        response.release();
    }

    @Test
    void testGetRouterHandler403UriWithoutRoot() {

        final String uri = TEST_URI.substring(1);
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("403 Forbidden"));
        response.release();
    }

    @Test
    void testGetRouterHandlerMultipleRequests() {

        final String uri1 = TEST_URI.concat("?foo=bar#123"); // 200
        final String uri2 = "/testing"; // 404
        final String uri3 = ""; // 403
        ByteBuf response;

        FullHttpRequest httpRequest1 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, TEST_URI);
        FullHttpRequest httpRequest2 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri1);
        FullHttpRequest httpRequest3 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri2);
        FullHttpRequest httpRequest4 = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri3);
        httpRequest4.headers().add("Connection", "close");

        channel.writeInbound(httpRequest1);
        channel.writeInbound(httpRequest2);
        channel.writeInbound(httpRequest3);
        channel.writeInbound(httpRequest4);

        assertEquals(4, channel.outboundMessages().size());

        response = channel.readOutbound();
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains(TEST_RESPONSE));
        response.release();

        assertEquals(3, channel.outboundMessages().size());

        response = channel.readOutbound();
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains(TEST_RESPONSE));
        response.release();

        assertEquals(2, channel.outboundMessages().size());

        response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("404 Not Found"));
        response.release();

        assertEquals(1, channel.outboundMessages().size());

        response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("403 Forbidden"));
        response.release();

        assertEquals(0, channel.outboundMessages().size());
    }

    @Test
    void testRouterHandlerUnsupportedMethod() {

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, TEST_URI);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("405 Method Not Allowed"));
        response.release();
    }

    @Test
    void testHeadRouterHandler200() {

        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, TEST_URI);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("200 OK"));
        // no response body
        assertFalse(response.toString(CharsetUtil.UTF_8).contains(TEST_RESPONSE));
        assertTrue(response.toString(CharsetUtil.UTF_8).endsWith("\r\n\r\n"));
        response.release();
    }

    @Test
    void testHeadRouterHandler200WithParams() {

        final String uri = TEST_URI.concat("?foo=bar#123");
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("200 OK"));
        assertFalse(response.toString(CharsetUtil.UTF_8).contains(TEST_RESPONSE));
        response.release();
    }

    @Test
    void testHeadRouterHandler404() {

        final String uri = "/testing404";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, uri);
        httpRequest.headers().add("Connection", "close");
        channel.writeInbound(httpRequest);
        final ByteBuf response = channel.readOutbound();
        assertEquals(-1, response.toString(CharsetUtil.UTF_8).indexOf("200 OK"));
        assertTrue(response.toString(CharsetUtil.UTF_8).contains("404 Not Found"));
        response.release();
    }

    private class TestHandler implements RouteHandler {

        @Override
        public FullHttpResponse getResponse(FullHttpRequest request) {

            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            StringBuilder resp = new StringBuilder()
                    .append(TEST_RESPONSE);
            final ByteBuf buffer = Unpooled.copiedBuffer(resp, CharsetUtil.UTF_8);
            response.content().writeBytes(buffer);
            buffer.release();
            return response;
        }
    }
}