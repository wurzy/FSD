import java.net.*;
import java.util.*;

public class Server {

    public static void main(String... args) throws Exception{

        ServerSocket sSocket = new ServerSocket(12345);
        List<Socket> clients = new ArrayList<>();

        while(true){
            Socket clSock = sSocket.accept();
            clients.add(clSock);

            System.out.println("CONNECTED: " + clSock.getRemoteSocketAddress());
            Worker w = new Worker(clSock,clients);

            Thread t = new Thread(w);
            t.start();
        }
    }
}
