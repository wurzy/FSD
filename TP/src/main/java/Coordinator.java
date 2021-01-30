import lib.Message;
import org.apache.commons.math3.util.Pair;
import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Coordinator {
    private static FutureServerSocketChannel ssc;
    private static Map<FutureSocketChannel,Communications> comms;
    private static Queue<Pair<String, Pair<Integer, FutureSocketChannel>>> waitingForLock;
    private static ReentrantLock lock;
    private static int readers;
    private static int writers;

    private static void putLockRequest(FutureSocketChannel s, Message m){
        lock.lock();
        System.out.println(m.toString());
        if (readers > 0 || writers > 0 || !waitingForLock.isEmpty()) {
            System.out.println("PUT EM ESPERA");
            waitingForLock.add(new Pair<>("put", new Pair<>(m.getSeq(), s)));
            lock.unlock();
            return;
        }
        System.out.println("PUT ENTROU SEM ESPERAR");
        writers++;
        lock.unlock();
        comms.get(s).sendMessage("OK", m.getSeq(), new byte[0]);
    }

    private static void getLockRequest(FutureSocketChannel s, Message m){
        lock.lock();
        System.out.println(m.toString());
        if (writers > 0 || !waitingForLock.isEmpty()) {
            System.out.println("GET EM ESPERA");
            waitingForLock.add(new Pair<>("get", new Pair<>(m.getSeq(), s)));
            lock.unlock();
            return;
        }
        System.out.println("GET ENTROU SEM ESPERAR");
        readers++;
        lock.unlock();
        comms.get(s).sendMessage("OK", m.getSeq(), new byte[0]);
    }

    public static void getUnlockRequest(FutureSocketChannel s, Message m){
        lock.lock();
        System.out.println(m.toString());
        readers--;
        comms.get(s).sendMessage("OK", m.getSeq(), new byte[0]);
        if (readers == 0 && !waitingForLock.isEmpty()){
            System.out.println("LIBERTEI UM PUT");
            Pair<Integer,FutureSocketChannel> r = waitingForLock.remove().getValue();
            comms.get(r.getValue()).sendMessage("OK", r.getKey(), new byte[0]);
            writers++;
        }
        lock.unlock();
    }

    public static void putUnlockRequest(FutureSocketChannel s, Message m){
        lock.lock();
        System.out.println(m.toString());
        writers--;
        comms.get(s).sendMessage("OK", m.getSeq(), new byte[0]);
        if (writers == 0 && !waitingForLock.isEmpty()){
            int i=0;
            do{
                i++;
                Pair<String, Pair<Integer, FutureSocketChannel>> p = waitingForLock.remove();
                comms.get(p.getValue().getValue()).sendMessage("OK", p.getValue().getKey(), new byte[0]);
                if (p.getKey().equals("put")){
                    writers++;
                    break;
                }
                readers++;
            } while(!waitingForLock.isEmpty() && waitingForLock.peek().getKey().equals("get"));
            System.out.println("LIBERTEI " + i + " PEDIDOS");
        }
        lock.unlock();
    }


    private static void handleRequest(FutureSocketChannel s, Message m) {
        String type = m.getType();
        switch (type){
            case "putLock":
                putLockRequest(s, m);
                break;
            case "getLock":
                getLockRequest(s, m);
                break;
            case "putUnlock":
                putUnlockRequest(s, m);
                break;
            case "getUnlock":
                getUnlockRequest(s, m);
                break;
            default:
                System.out.println("Not a valid message type");
        }
    }

    private static void read(FutureSocketChannel s){
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

    private static void acceptConnection(){
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
        waitingForLock = new LinkedList<>();
        lock = new ReentrantLock();
        readers = 0;
        writers = 0;

        ssc = new FutureServerSocketChannel();
        ssc.bind(new InetSocketAddress(myPort));

        acceptConnection();

        TimeUnit.DAYS.sleep(10);
    }
}
