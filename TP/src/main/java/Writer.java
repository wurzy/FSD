import lib.AsyncMap;
import lib.Message;
import lib.Tools;
import org.apache.commons.math3.util.Pair;
import spullara.nio.channels.FutureSocketChannel;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class Writer {
    private FutureSocketChannel socket;
    private ReentrantLock lock;
    private AsyncMap outgoing;
    private Map<Integer, CompletableFuture<Void>> send_hanlders;
    private int lastSent;
    private int nextToSend;

    public Writer(FutureSocketChannel s){
        socket = s;
        lock = new ReentrantLock();
        outgoing = new AsyncMap();
        send_hanlders = new HashMap<>();
        lastSent = 0;
        nextToSend = 1;
        sender();
    }

    // Put message on ongoing messages with message seq being the current send number
    public Pair<Integer,CompletableFuture<Void>> sendMessage(String type, byte[] content){
        lock.lock();
        Message m = new Message(type, nextToSend, content);

        CompletableFuture<Void> ret = new CompletableFuture<>();
        int r = nextToSend;
        outgoing.put(nextToSend, m);
        send_hanlders.put(nextToSend, ret);
        nextToSend++;
        lock.unlock();
        return new Pair<>(r,ret);
    }

    // Put message on ongoing messages with a custom seq
    public CompletableFuture<Void> sendMessage(String type, int seq, byte[] content){
        lock.lock();
        Message m = new Message(type, seq, content);

        CompletableFuture<Void> ret = new CompletableFuture<>();
        outgoing.put(nextToSend, m);
        send_hanlders.put(seq, ret);
        nextToSend++;
        lock.unlock();
        return ret;
    }


    public void sender(){
        lock.lock();
        CompletableFuture<Message> cf = outgoing.get(lastSent + 1);
        lock.unlock();
        cf.thenAcceptAsync(m -> {
            try {
                CompletableFuture<Void> sent = new CompletableFuture<>();
                byte[] message = Tools.toByteArray(m);
                byte[] tam = ByteBuffer.allocate(4).putInt(message.length).array();
                byte[] concat = new byte[message.length + tam.length];
                System.arraycopy(tam, 0, concat, 0, tam.length);
                System.arraycopy(message, 0, concat, tam.length, message.length);

                ByteBuffer buffer = ByteBuffer.wrap(concat);
                send(buffer, sent);
                sent.thenRunAsync(() -> {
                    if (send_hanlders.containsKey(m.getSeq())){
                        send_hanlders.get(m.getSeq()).complete(null);
                        send_hanlders.remove(m.getSeq());
                    }
                    lastSent++;
                    sender();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void send(ByteBuffer buffer, CompletableFuture<Void> acceptor){
        socket.write(buffer).thenAccept(x -> {
            if(buffer.hasRemaining()){
                send(buffer,acceptor);
            }
            else {
                acceptor.complete(null);
            }
        });
    }
}
