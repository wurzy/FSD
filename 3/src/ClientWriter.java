import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ClientWriter implements Runnable{
    private SocketChannel s;
    private long ms;
    private ByteBuffer buf;

    public ClientWriter(SocketChannel s, long ms){
        this.s = s;
        this.ms = ms;
    }

    public void run(){
        try{
            int id = 0;
            while(true){
                String send = "hello im an overload bot from " + s.getLocalAddress() + ". ID: " + id++;
                buf = StandardCharsets.UTF_8.encode(send);
                System.out.println("WRITER: " + StandardCharsets.UTF_8.decode(buf.duplicate()).toString()); // duplicate para nao estourar os pointers
                //buf.flip();  Não colocar isto com o encode porque ele já faz o flip internamente.
                s.write(buf);
                buf.clear();
                Thread.sleep(ms);
            }
        }
        catch(Exception e) {e.toString();}
    }
}
