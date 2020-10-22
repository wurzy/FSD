import java.net.*;

public class Client {

    public static void main(String... args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 12345);
        ClientReader cr = new ClientReader(socket,5);
        ClientWriter cw = new ClientWriter(socket, 1);

        Thread t1 = new Thread(cr);
        Thread t2 = new Thread(cw);
        t1.start();
        t2.start();
    }

}
