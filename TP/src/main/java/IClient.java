import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IClient {
    CompletableFuture<Void> connect(String[] ports);
    CompletableFuture<Void> put(Map<Long, byte[]> keys);
    CompletableFuture<Map<Long, byte[]>> get(Collection<Long> keys);
}
