import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {

    public static void main(String... args) throws Exception{

        ServerSocketChannel sSocket = ServerSocketChannel.open();
        sSocket.bind(new InetSocketAddress(12345));
        List<SocketChannel> clients = new ArrayList<>();

        while(true){
            SocketChannel clSock = sSocket.accept();
            clients.add(clSock);

            System.out.println("CONNECTED: " + clSock.getRemoteAddress());
            Worker w = new Worker(clSock,clients);

            Thread t = new Thread(w);
            t.start();
        }
    }
}
