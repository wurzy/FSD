import java.io.*;
import java.util.*;
import java.net.*;

public class Worker implements Runnable {
    private BufferedReader in;
    private Socket s;
    private List<Socket> clients;

    public Worker(Socket s, List<Socket> clients) throws Exception{
        this.s = s;
        this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        this.clients = clients;
    }

    public void run(){
        String input;
        while(true) {
            try {
                input=in.readLine();
                if(input == null) {
                    s.shutdownInput();
                    s.shutdownOutput();
                    s.close();
                    break;
                }
                System.out.println(s.getRemoteSocketAddress() +" >> " + input);
                replyAll(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void replyAll(String msg){
        PrintWriter cl;
        try{
            for(Socket s: clients) {
                cl = new PrintWriter(s.getOutputStream());
                cl.println(msg);
                cl.flush();
            }
        }
        catch(Exception e){e.printStackTrace();}
    }
}
