import java.io.*;
import java.net.*;

public class ClientReader implements Runnable{
    private Socket s;
    private int seconds;

    public ClientReader(Socket s, int seconds) {
        this.s = s;
        this.seconds = seconds;
    }

    public void run(){
        String str;
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            while((str = in.readLine())!=null){
                System.out.println(str);
                Thread.sleep(seconds * 1000);
            }
        }
        catch(Exception e) {e.toString();}
    }
}
