package com.aquarium.service.tcp;

import com.aquarium.service.tcp.handler.TcpMessageDecoder;
import com.aquarium.service.tcp.handler.TcpMessageEncoder;
import com.aquarium.service.tcp.handler.SensorDataHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpGatewayServer {

    @Value("${tcp.gateway.port:9527}")
    private int port;

    @Value("${tcp.gateway.boss-threads:1}")
    private int bossThreads;

    @Value("${tcp.gateway.worker-threads:4}")
    private int workerThreads;

    @Value("${tcp.gateway.heartbeat-interval:60}")
    private int heartbeatInterval;

    private final SensorDataHandler sensorDataHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(heartbeatInterval * 2, 0, 0))
                                .addLast(new TcpMessageDecoder())
                                .addLast(new TcpMessageEncoder())
                                .addLast(sensorDataHandler);
                    }
                });

        channelFuture = bootstrap.bind(port).sync();
        log.info("TCP Gateway started on port {}", port);
    }

    @PreDestroy
    public void shutdown() {
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("TCP Gateway shutdown complete");
    }
}
