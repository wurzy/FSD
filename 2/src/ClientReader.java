import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ClientReader implements Runnable{
    private SocketChannel s;
    private long ms;
    private ByteBuffer buf;

    public ClientReader(SocketChannel s, long ms) {
        this.s = s;
        this.ms = ms;
        this.buf = ByteBuffer.allocate(1024);
    }

    public void run(){
        String str;
        try{
            int n;
            buf.flip();
            while((n= s.read(buf))!=-1){
                str = StandardCharsets.UTF_8.decode(buf).toString();
                System.out.println(str);
                buf.clear();
                Thread.sleep(ms);
            }
        }
        catch(Exception e) {e.toString();}
    }
}
