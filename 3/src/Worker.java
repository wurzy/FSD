import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.*;

public class Worker implements Runnable {
    ByteBuffer buf;
    private SocketChannel s;
    private List<SocketChannel> clients;

    public Worker(SocketChannel s, List<SocketChannel> clients){
        this.s = s;
        buf = ByteBuffer.allocate(1024);
        this.clients = clients;
    }

    public void run(){
        try{
            while(s.read(buf)!=-1){
                buf.flip();
                String in = StandardCharsets.UTF_8.decode(buf.duplicate()).toString();
                System.out.println(s.getRemoteAddress() +" >> " + in);
                replyAll();
                buf.clear();
            }
            System.out.println(s.getRemoteAddress() + " disconnected.");
            s.shutdownInput();
            s.shutdownOutput();
            s.close();
        }
        catch(Exception e){System.out.println(e.toString());}
    }

    private void replyAll(){
        try{
            for(SocketChannel s: clients) {
                s.write(buf.duplicate());
                s.write(StandardCharsets.UTF_8.encode("\n"));
            }
        }
        catch(Exception e){System.out.println(e.toString());}
    }
}
