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
        String input;
        int n;
        while(true) {
            try {
                if( (n = s.read(buf)) == -1 ) {
                    s.close();
                    break;
                }
                System.out.println(n);
                buf.flip();
                input = StandardCharsets.UTF_8.decode(buf).toString();
                System.out.println(s.getRemoteAddress() +" >> " + input);
                replyAll(input);
                buf.clear();
            } catch (IOException e) {
                e.toString();
            }
        }
    }

    void replyAll(String msg){
        try{
            for(SocketChannel s: clients) {
                s.write(buf.duplicate());
            }
        }
        catch(Exception e){e.toString();}
    }
}
