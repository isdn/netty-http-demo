package dev.isdn.demo.netty.server.http;

import dev.isdn.demo.netty.server.http.router.Router;
import dev.isdn.demo.netty.server.http.router.StatusHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {

        Router router = new Router();
        router.GET("/status", new StatusHandler());

        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(65536))
                .addLast(new HttpContentCompressor())
                // A ChannelHandler that adds support for writing a large data stream asynchronously neither spending a lot of memory
                // nor getting OutOfMemoryError. Large data streaming such as file transfer requires complicated state management
                // in a ChannelHandler implementation. ChunkedWriteHandler manages such complicated states so that you can send a
                // large data stream without difficulties.
                // .addLast(new ChunkedWriteHandler());
                .addLast(new ServerHandler(router));
    }
}
