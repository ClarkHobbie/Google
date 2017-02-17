import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;


public class Google {
    public static class LocalChannelHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] buffer = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(0, buffer);
            String s = new String(buffer);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            System.exit(1);
        }
    }

    public static class LocalChannelInitializer extends ChannelInitializer<SocketChannel> {
        private SslContext sslContext;

        public LocalChannelInitializer(SslContext sslContext) {
            this.sslContext = sslContext;
        }

        public void initChannel(SocketChannel ch) throws Exception {
            SslHandler sslHandler = sslContext.newHandler(ch.alloc());
            ch.pipeline().addLast(sslHandler);

            LocalChannelHandler localChannelHandler = new LocalChannelHandler();
            ch.pipeline().addLast(localChannelHandler);
            ch.pipeline().addLast(new HttpRequestEncoder());
        }
    }

    public static class LocalChannelListener implements ChannelFutureListener {
        private Google google;
        private SslContext sslContext;

        public LocalChannelListener(Google google, SslContext sslContext) {
            this.google = google;
            this.sslContext = sslContext;
        }

        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (channelFuture.isSuccess()) {
                google.setChannel(channelFuture.channel());
                System.out.println("Got connection");
            }
        }
    }

    private Channel channel;
    private String host;
    private int port;
    private SslContext sslContext;

    public SslContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Google() {
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }


    public static Bootstrap createClientBootstrap(ChannelHandler channelHandler) {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(channelHandler);

        return bootstrap;
    }

    public void connect() {
        try {
            System.out.println("Connecting to " + getHost() + ":" + getPort());

            LocalChannelListener localChannelListener = new LocalChannelListener(this, sslContext);

            LocalChannelInitializer localChannelInitializer = new LocalChannelInitializer(sslContext);
            Bootstrap bootstrap = createClientBootstrap(localChannelInitializer);
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            channelFuture.addListener(localChannelListener);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void send(String path) {
        try {
            FullHttpRequest fullHttpRequestRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

            getChannel().writeAndFlush(fullHttpRequestRequest).sync();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static SslContext createSimpleClientContext() {
        SslContext sslContext = null;

        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return sslContext;
    }

    public static void main(String[] argv) {
        Google google = new Google();
        google.setHost("www.google.com");
        google.setPort(443);
        google.setSslContext(createSimpleClientContext());
        google.connect();
        google.send("/");

        sleep(500);

        System.exit(0);
    }
}
