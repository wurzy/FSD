package main;

import io.atomix.utils.net.Address;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {
    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        int nClients = 3;

        Address[] peers = new Address[nClients];
        for (int i = 0; i < peers.length; i++) {
            peers[i] = Address.from("localhost:" + (12345 + i));
        }

        Peer<String> cd = new Peer<>(id, peers);
        cd.registerHandler("chat", System.out::println);

        BufferedReader in_terminal = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            while ((line = in_terminal.readLine()) != null) {
                cd.broadcast("chat", line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
