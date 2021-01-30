import lib.Message;
import org.apache.commons.math3.util.Pair;
import spullara.nio.channels.FutureSocketChannel;

import java.util.concurrent.CompletableFuture;

public class Communications {
    private Writer writer;
    private Reader reader;

    public Communications(FutureSocketChannel s){
        writer = new Writer(s);
        reader = new Reader(s);
    }

    public Pair<Integer,CompletableFuture<Void>> sendMessage(String type, byte[] content){
        return writer.sendMessage(type, content);
    }

    public CompletableFuture<Void> sendMessage(String type, int seq, byte[] content){
        return writer.sendMessage(type, seq, content);
    }

    public CompletableFuture<Message> receiveMessage(int expected){
        return reader.receive(expected);
    }

    public CompletableFuture<Message> receiveMessage(){
        return reader.receive();
    }
}
