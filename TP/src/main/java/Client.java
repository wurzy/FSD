import lib.Tools;
import org.apache.commons.math3.util.Pair;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class Client implements IClient{
    private List<FutureSocketChannel> p2p;
    private Map<FutureSocketChannel, Communications> comms;
    private FutureSocketChannel coordinator;

    public Client() {
        p2p = new ArrayList<>();
        comms = new HashMap<>();
    }

    public CompletableFuture<Void> connect (String [] args){
        List<CompletableFuture<Void>> cfs = new ArrayList<>();
        Arrays.sort(args,1,args.length);
        try {
            CompletableFuture<Void> ccf = new CompletableFuture<>();
            cfs.add(ccf);
            int coordinator_port = Integer.parseInt(args[0]);
            FutureSocketChannel cs = new FutureSocketChannel();
            cs.connect(new InetSocketAddress(coordinator_port)).thenAccept(v -> {
                coordinator = cs;
                comms.put(cs, new Communications(cs));
                ccf.complete(null);
            });

            for (int i = 1;i<args.length;i++) {
                CompletableFuture<Void> cf = new CompletableFuture<>();
                cfs.add(cf);
                int port = Integer.parseInt(args[i]);
                FutureSocketChannel s = new FutureSocketChannel();
                p2p.add(s);
                s.connect(new InetSocketAddress(port)).thenAccept(v -> {
                    comms.put(s, new Communications(s));
                    cf.complete(null);
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
    }

    private CompletableFuture<Void> putAux(FutureSocketChannel s, Map<Long, byte[]> keys){
        CompletableFuture<Void> ret = new CompletableFuture<>();
        Pair<Integer, CompletableFuture<Void>> sent = comms.get(s).sendMessage("put", Tools.toByteArray(keys));
        int seq = sent.getKey();
        sent.getValue().thenRunAsync(() -> {
            comms.get(s).receiveMessage(seq).thenAcceptAsync(msg -> {
                if (msg == null){
                    System.out.println("Connection down");
                    return;
                }
                try {
                    System.out.println(msg.toString());
                    ret.complete(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        return ret;
    }

    public CompletableFuture<Void> put(Map<Long, byte[]> keys){
        Map<FutureSocketChannel, Map<Long, byte[]>> atrib_put = new HashMap<>();
        List<CompletableFuture<Void>> cfs = new ArrayList<>();
        CompletableFuture<Void> ret = new CompletableFuture<>();
        keys.forEach((k,v) -> {
            int p = (int) (k % p2p.size());
            FutureSocketChannel s = p2p.get(p);
            if (atrib_put.containsKey(s)){
                atrib_put.get(s).put(k,v);
            }
            else{
                Map<Long, byte[]> x = new HashMap<>();
                x.put(k,v);
                atrib_put.put(s, x);
            }
        });

        lockOperation("putLock").thenRunAsync(() -> {
            System.out.println("Acquired lock");
            atrib_put.forEach((k, v) -> cfs.add(putAux(k, v)));
            CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]))
                    .thenRunAsync(() -> ret.complete(null));

            lockOperation("putUnlock").thenRunAsync(() ->{
                System.out.println("Released lock");
            });
        });

        return ret;
    }


    private CompletableFuture<Map<Long, byte[]>> getAux(FutureSocketChannel s, Collection<Long> keys){
        CompletableFuture<Map<Long, byte[]>> receiver = new CompletableFuture<>();

        Pair<Integer, CompletableFuture<Void>> sent = comms.get(s).sendMessage("get", Tools.toByteArray(keys));
        int seq = sent.getKey();

        sent.getValue().thenRunAsync(() -> {
            comms.get(s).receiveMessage(seq).thenAcceptAsync(msg -> {
                if (msg == null){
                    System.out.println("Connection down");
                    return;
                }
                try {
                    System.out.println(msg.toString());
                    receiver.complete((Map<Long, byte[]>) Tools.fromByteArray(msg.getContent()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        return receiver;
    }

    public CompletableFuture<Map<Long, byte[]>> get(Collection<Long> keys){
        Map<FutureSocketChannel, Collection<Long>> atrib_get = new HashMap<>();
        List<CompletableFuture<Map<Long, byte[]>>> cfs = new ArrayList<>();
        CompletableFuture<Map<Long, byte[]>> ret = new CompletableFuture<>();
        keys.forEach(k -> {
            int p = (int) (k % p2p.size());
            FutureSocketChannel s = p2p.get(p);
            if (atrib_get.containsKey(s)){
                atrib_get.get(s).add(k);
            }
            else{
                Collection<Long> x = new ArrayList<>();
                x.add(k);
                atrib_get.put(s, x);
            }
        });

        lockOperation("getLock").thenRunAsync(() -> {
            System.out.println("Acquired lock");
            atrib_get.forEach((k,v) -> cfs.add(getAux(k,v)));

            CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]))
                    .thenAccept(v -> {
                        Map<Long, byte[]> r = new HashMap<>();
                        cfs.forEach(cf -> cf.thenAcceptAsync(r::putAll));
                        ret.complete(r);
                    });

            lockOperation("getUnlock").thenRunAsync(() ->{
                System.out.println("Released lock");
            });
        });
        return ret;
    }

    private CompletableFuture<Void> lockOperation(String type){
        CompletableFuture<Void> confirmation = new CompletableFuture<>();

        Pair<Integer, CompletableFuture<Void>> sent = comms.get(coordinator).sendMessage(type, new byte[0]);
        int seq = sent.getKey();

        sent.getValue().thenRunAsync(() -> {
            comms.get(coordinator).receiveMessage(seq).thenAcceptAsync(msg -> {
                if (msg == null){
                    System.out.println("Connection down");
                    return;
                }
                try {
                    System.out.println(msg.toString());
                    confirmation.complete(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        return confirmation;
    }
}