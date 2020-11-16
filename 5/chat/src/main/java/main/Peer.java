package main;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

class Peer<T> {

    private final int clientID;
    private final Address[] allClients;
    private final int[] lastFrom;

    private final Serializer serializer;
    private final ManagedMessagingService messagingService;
    private final ExecutorService executorService;

    private final Map<String, Consumer<T>> handlers;

    private final List<Message<T>> buffer;

    public Peer(final int clientID, final Address[] allClients) {
        this.clientID = clientID;
        this.allClients = allClients;

        this.lastFrom = new int[allClients.length];
        for (int i = 0; i < this.lastFrom.length; i++) {
            lastFrom[i] = 0;
        }

        this.serializer = new SerializerBuilder()
                .addType(Message.class)
                .build();

        this.messagingService = NettyMessagingService.builder()
                .withAddress(Address.from("localhost:" + (12345 + clientID)))
                .build();
        this.messagingService.start();

        this.executorService = Executors.newSingleThreadExecutor();

        this.handlers = new HashMap<>();
        this.buffer = new ArrayList<>();
    }

    void registerHandler(final String group, final Consumer<T> consumer) {
        this.handlers.put(group, consumer);

        this.messagingService.registerHandler(group, (o, m) -> { // when message of type "chat" arrives:
            Message<T> message = serializer.decode(m);
            System.out.println(message);
            if (message.maybeAccept(this.lastFrom)) {
                accept(message, group);
            } else {
                buffer.add(message);
            }

        }, this.executorService);
    }

    void broadcast(final String group, final T message) {
        this.lastFrom[clientID]++;
        for (int i = 0; i < this.allClients.length; i++) {
            if (i != clientID) {
                messagingService.sendAsync(this.allClients[i], group, // sendAsync sends to array of addresses messages of type "chat"
                        serializer.encode(new Message<>(this.clientID, this.lastFrom, message)));
            }
        }
    }

    private void accept(Message<T> message, String group) {
        this.handlers.get(group).accept(message.getMessage());
        int[] r = message.getVectorClock();
        for (int i = 0; i < this.lastFrom.length; i++) {
            this.lastFrom[i] = Math.max(this.lastFrom[i], r[i]); // update message IDs
        }
        this.buffer.forEach(m -> {
            if (m.maybeAccept(this.lastFrom)) {
                accept(m, group);
            }
        });
    }
}
