package lib;

import java.io.*;
import java.util.Collection;
import java.util.Map;

public class Message implements Serializable,Comparable<Message> {
    private String type;
    private int seq;
    private byte[] content;

    public Message(String t, int s, byte[] c){
        type = t;
        seq = s;
        content = c;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public int compareTo(Message message) {
        return Integer.compare(this.seq,message.getSeq());
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Tipo da mensagem: ").append(type).append(" Nº: ").append(seq).append("\n");
        Object p = null;
        if (content.length == 0) return sb.toString();
        try {
            p = Tools.fromByteArray(content);
        } catch (Exception e){e.printStackTrace();}
        if (p instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Long, byte[]> pp = (Map<Long,byte[]>) p;
            pp.forEach((k, v) -> {
                try {
                    sb.append("key => ").append(k).append("; value => ").append((String) Tools.fromByteArray(v)).append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        else {
            @SuppressWarnings("unchecked")
            Collection<Long> pp = (Collection<Long>) p;
            pp.forEach(k -> sb.append("key ").append(k).append("\n"));
        }
        return sb.toString();
    }
}
