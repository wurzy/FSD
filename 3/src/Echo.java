import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.defaultThreadFactory;

public class Echo {

    private static class Context {

        public Context(ByteBuffer buf, AsynchronousSocketChannel sc) {
            this.buf = buf;
            this.sc = sc;
        }

        ByteBuffer buf;
        AsynchronousSocketChannel sc;
        List<AsynchronousServerSocketChannel> clients;
    }

    private static final CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> ach = new CompletionHandler<>() {
        @Override
        public void completed(AsynchronousSocketChannel sc, AsynchronousServerSocketChannel o) {
            System.out.println("Accepted!");

            ByteBuffer buf = ByteBuffer.allocate(1000);
            Context c = new Context(buf, sc);

            sc.read(buf, c, new CompletionHandler<Integer, Context>() {
                @Override
                public void completed(Integer integer, Context c) {
                    if(integer<=0) {
                        try {
                            c.sc.close();
                            //return; // ou estoura no failure
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    c.buf.flip();

                    c.sc.write(c.buf, c, new CompletionHandler<Integer, Context>() {
                        @Override
                        public void completed(Integer integer, Context context) {
                            System.out.println("Done!");
                        }

                        @Override
                        public void failed(Throwable throwable, Context context) {
                            System.out.println("Poof");
                        }
                    });
                    c.sc.read(buf.clear(),c,this);
                }

                @Override
                public void failed(Throwable throwable, Context c) {
                }
            });

            acceptRec(o);
        }

        @Override
        public void failed(Throwable throwable, AsynchronousServerSocketChannel o) {

        }
    };

    public static void acceptRec(AsynchronousServerSocketChannel ssc) {
        ssc.accept(ssc, ach);
    }

    // usar um booleano para os sockets quando tao a ser usados e meter a mensagem na fila, ContextoSocket(bool, socket, queue).
    public static void main(String[] args) throws Exception {
        List<AsynchronousSocketChannel> clients = new ArrayList<>();
        AsynchronousChannelGroup g =
                AsynchronousChannelGroup.withFixedThreadPool(1, defaultThreadFactory());

        AsynchronousServerSocketChannel ssc =
                AsynchronousServerSocketChannel.open(g);
        ssc.bind(new InetSocketAddress(12345));

        acceptRec(ssc);

        g.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        System.out.println("Terminei!");
    }
}
