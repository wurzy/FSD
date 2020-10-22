import java.net.*;
import java.io.*;

public class ClientWriter implements Runnable{
    private Socket s;
    private int seconds;

    public ClientWriter(Socket s, int seconds){
        this.s = s;
        this.seconds = seconds;
    }
    public void run(){
        try{
            PrintWriter out = new PrintWriter(s.getOutputStream());
            while(true){
                out.println("hello im an overload bot from " + s.getInetAddress() + ". Another message in " + seconds + " seconds.");
                out.flush();
                Thread.sleep(seconds * 1000);
            }
        }
        catch(Exception e) {e.toString();}
    }
}
