import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.Executors.defaultThreadFactory;

public class EchoFut {

    static void read(CompletableFuture<FutureSocketChannel> sc, ByteBuffer buf){
        sc.thenAccept(s->{ // s é um FutureSocketChannel

            CompletableFuture<Integer> read = s.read(buf);

            read.thenAccept(i->{
                buf.flip();
                System.out.print("Entrei: ");
                System.out.println(StandardCharsets.UTF_8.decode(buf).toString());
                if(i<=0) return;
                else{
                    buf.clear();
                    read(sc,buf);
                }
            });
        });
    }

    public static void main(String[] args) throws Exception {
        FutureServerSocketChannel ssc = new FutureServerSocketChannel();
        ssc.bind(new InetSocketAddress(12345));
        ByteBuffer buf = ByteBuffer.allocate(1000);

        while(true){
            CompletableFuture<FutureSocketChannel> sc = ssc.accept();
            read(sc,buf);
            System.out.println("done");
        }
    }
}
