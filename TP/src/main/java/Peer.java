import lib.Message;
import lib.Tools;
import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Peer {

    private static FutureServerSocketChannel ssc;
    private static Map<Long, byte[]> pairs = new HashMap<>();
    private static Map<FutureSocketChannel,Communications> comms;


    private static void getRequest(FutureSocketChannel s, Message m){
        System.out.println(m.toString());
        try {
            @SuppressWarnings("unchecked")
            Collection<Long> p = (Collection<Long>) Tools.fromByteArray(m.getContent());
            Map<Long, byte[]> ret = p.stream()
                    .filter(k -> pairs.containsKey(k))
                    .map(k -> new AbstractMap.SimpleEntry<>(k, pairs.get(k)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            comms.get(s).sendMessage("responseget", m.getSeq(), Tools.toByteArray(ret));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void putRequest(FutureSocketChannel s, Message m) {
        System.out.println(m.toString());
        try {
            @SuppressWarnings("unchecked")
            Map<Long, byte[]> p = (Map<Long, byte[]>) Tools.fromByteArray(m.getContent());
            pairs.putAll(p);

            comms.get(s).sendMessage("putConfirmation", m.getSeq(), new byte[0]);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void handleRequest(FutureSocketChannel s, Message m) {
        String type = m.getType();
        switch (type){
            case "get":
                getRequest(s, m);
                break;
            case "put":
                putRequest(s, m);
                break;
            default:
                System.out.println("Not a valid message type");
        }
    }


    static void read(FutureSocketChannel s){
        comms.get(s).receiveMessage().thenAcceptAsync(m -> {
            if (m == null){
                System.out.println("Connection lost");
                return;
            }
            //System.out.println(m.toString());
            handleRequest(s, m);
            read(s);
        });
    }

    static void acceptConnection(){
        CompletableFuture<FutureSocketChannel> sc = ssc.accept();

        sc.thenAccept(s -> {
            comms.put(s, new Communications(s));
            read(s);
            acceptConnection();
        });
    }

    public static void main(String[] args) throws Exception{
        int myPort = Integer.parseInt(args[0]);
        comms = new HashMap<>();

        ssc = new FutureServerSocketChannel();
        ssc.bind(new InetSocketAddress(myPort));

        acceptConnection();

        TimeUnit.DAYS.sleep(10);
    }
}