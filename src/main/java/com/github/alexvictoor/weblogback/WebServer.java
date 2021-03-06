package com.github.alexvictoor.weblogback;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer {

    public static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    private final String host;
    private final int port;
    private final int replayBufferSize;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private ChannelGroup allChannels;

    public WebServer(String host, int port, int replayBufferSize) {
        this.host = host;
        this.port = port;
        this.replayBufferSize = replayBufferSize;
    }

    public ChannelOutputStream start() {
        logger.info("Starting server, listening on port {}", port);
        allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        ChannelOutputStream channelOutputStream = new ChannelOutputStream(allChannels, replayBufferSize);

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer(channelOutputStream, host, port));

            serverChannel = b.bind(port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return channelOutputStream;
    }

    public void stop() {
        if (serverChannel == null) {
            return;
        }
        try {
            serverChannel.close().sync();
            allChannels.close().sync();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
