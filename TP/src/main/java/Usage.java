import lib.Tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Usage {

    public static void oneHundredGets (String[] args) throws InterruptedException {
        Client c = new Client();
        c.connect(args).thenRunAsync(() -> {
            Map<Long, byte[]> map = new HashMap<>();
            Collection<Long> list = new ArrayList<>();
            for(int i=0;i<10;i++){
                map.put((long)i, Tools.toByteArray("value"+i));
            }
            for(int i=0;i<5;i++){
                list.add((long)i);
            }

            CompletableFuture<Void> p = c.put(map);
            for (int i=0;i<100;i++){
                CompletableFuture<Map<Long, byte[]>> g = c.get(list);
            }
        });
        TimeUnit.DAYS.sleep(1);
    }

    public static void example(String[] args) throws InterruptedException {
        Client c = new Client();
        c.connect(args).thenRunAsync(() -> {
            Map<Long, byte[]> map = new HashMap<>();
            Collection<Long> list = new ArrayList<>();
            for(int i=0;i<10;i++){
                map.put((long)i, Tools.toByteArray("value"+i));
            }
            for(int i=0;i<5;i++){
                list.add((long)i);
            }

            CompletableFuture<Void> p = c.put(map);
            CompletableFuture<Map<Long, byte[]>> g = c.get(list);
        });
        TimeUnit.DAYS.sleep(1);
    }

    public static void main(String[] args) throws Exception {
        oneHundredGets(args);
    }
}
