package lib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncMap {
    private Map<Integer, Message> map;
    private Map<Integer, CompletableFuture<Message>> cfs;

    public AsyncMap() {
        map = new HashMap<>();
        cfs = new HashMap<>();
    }

    public CompletableFuture<Message> get(int key){
        CompletableFuture<Message> cfm = new CompletableFuture<>();
        if (map.containsKey(key)){
            cfm.complete(map.get(key));
            map.remove(key);
        }
        else{
            cfs.put(key, cfm);
        }
        return cfm;
    }

    public void put(int key, Message value){
        if (cfs.containsKey(key)){
            cfs.get(key).complete(value);
            cfs.remove(key);
        }
        else{
            map.put(key, value);
        }
    }
}
