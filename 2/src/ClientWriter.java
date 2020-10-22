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
                String send = "hello im an overload bot from " + s.getRemoteAddress() + ". ID: " + id++;
                buf = StandardCharsets.UTF_8.encode(send);
                buf.flip();
                s.write(buf);
                buf.clear();
                Thread.sleep(ms);
            }
        }
        catch(Exception e) {e.toString();}
    }
}
