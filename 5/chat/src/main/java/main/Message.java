package main;

import java.util.Arrays;

public class Message<T> {

    private int clientID;
    private int[] vectorClock;
    private T message;

    public Message(int clientID, int[] vectorClock, T message) {
        this.clientID = clientID;
        this.vectorClock = vectorClock;
        this.message = message;
    }

    public int[] getVectorClock() {
        return vectorClock;
    }

    public T getMessage() {
        return message;
    }

    // Accept only if the message is not repeated/useless
    public boolean maybeAccept (int[] lastFrom){
        if (lastFrom[this.clientID] + 1 != this.vectorClock[this.clientID]) {
            return false;
        }
        for (int j = 0; j < lastFrom.length; j++) {
            if (j != this.clientID && this.vectorClock[j] > lastFrom[j]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "Message{" +
                "clientID=" + clientID +
                ", vectorClock=" + Arrays.toString(vectorClock) +
                ", message=" + message +
                '}';
    }
}
