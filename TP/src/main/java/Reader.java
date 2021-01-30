import lib.Message;
import lib.Tools;
import spullara.nio.channels.FutureSocketChannel;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class Reader {
    private FutureSocketChannel socket;
    private Map<Integer, Message> incoming;
    private ReentrantLock lock;
    private Map<Integer, CompletableFuture<Message>> current_handlers;
    private int lastRead;

    public Reader(FutureSocketChannel s){
        socket = s;
        incoming = new HashMap<>();
        lock = new ReentrantLock();
        current_handlers = new HashMap<>();
        lastRead = 0;
        read();
    }

    public void tryAccept(){
        if (current_handlers.isEmpty()) return;
        for (Map.Entry<Integer, Message> e: incoming.entrySet()){
            if (current_handlers.containsKey(e.getKey())){
                lastRead++;
                incoming.remove(e.getKey());
                current_handlers.get(e.getKey()).complete(e.getValue());
                current_handlers.remove(e.getKey());
                return;
            }
        }
    }

    public CompletableFuture<Message> receive(int expected){
        lock.lock();
        CompletableFuture<Message> cf = new CompletableFuture<>();
        current_handlers.put(expected, cf);
        tryAccept();
        lock.unlock();
        return cf;
    }

    public CompletableFuture<Message> receive(){
        lock.lock();
        CompletableFuture<Message> cf = new CompletableFuture<>();
        current_handlers.put(lastRead + 1, cf);
        tryAccept();
        lock.unlock();
        return cf;
    }

    public void read(){
        ByteBuffer b = ByteBuffer.allocate(4);
        socket.read(b).thenAccept(rd -> {
            b.flip();
            int tam = b.getInt();
            ByteBuffer buf = ByteBuffer.allocate(tam);
            socket.read(buf).thenAccept(rd_ -> {
                buf.flip();
                try {
                    Message m = (Message) Tools.fromByteArray(buf.array());
                    lock.lock();
                    incoming.put(m.getSeq(), m);
                    tryAccept();
                    lock.unlock();
                } catch (Exception e) {e.printStackTrace();}
                read();
            });
        });
    }


    /* Buffer reusage (doesn't work) */


    private int readMessageInto(ByteBuffer buf, ByteArrayOutputStream bytes, int bytesLeft){
        if (bytesLeft == -1){
            bytesLeft = buf.getInt();
        }
        if (bytesLeft <= buf.remaining()){
            try {
                byte[] arr = new byte[bytesLeft];
                buf.get(arr, 0, bytesLeft);
                bytes.write(arr);
                bytesLeft = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                int read_bytes = buf.remaining();
                byte[] arr = new byte[read_bytes];
                buf.get(arr);
                bytes.write(arr);
                bytesLeft -= read_bytes;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bytesLeft;
    }



    private void readRec(FutureSocketChannel s,
                         ByteBuffer buf,
                         ByteArrayOutputStream bytes,
                         int bytesLeft) {
        s.read(buf).thenAccept(rd -> {
            if (rd == -1) {
                System.out.println("Connection closed");
                return;
            }
            buf.flip();
            int bleft = 0;
            while (buf.remaining() > 4) {
                bleft = readMessageInto(buf, bytes, bytesLeft);
                try {
                    if (bleft == 0) {
                        Message m = (Message) Tools.fromByteArray(bytes.toByteArray());
                        this.incoming.put(m.getSeq(),m);
                        bytes.reset();
                        bleft = -1;
                    } else {
                        int read_bytes = buf.remaining();
                        byte[] arr = new byte[read_bytes];
                        buf.get(arr);
                        bytes.write(arr);
                        bleft -= read_bytes;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("BUF => pos: " + buf.position() + " limit: " + buf.limit());
            buf.compact();
            System.out.println("BUF => pos: " + buf.position() + " limit: " + buf.limit());
            System.out.println("bytesleft: " + bleft);
            readRec(s, buf, bytes, bleft);
        });
    }
}