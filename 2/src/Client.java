import java.net.*;
import java.nio.channels.SocketChannel;

public class Client {

    public static void main(String... args) throws Exception {
        SocketChannel socket = SocketChannel.open(new InetSocketAddress("localhost",12345));
        ClientReader cr = new ClientReader(socket, 1000);
        ClientWriter cw = new ClientWriter(socket, 1000);

        Thread t1 = new Thread(cr);
        Thread t2 = new Thread(cw);
        t1.start();
        t2.start();
    }

}
