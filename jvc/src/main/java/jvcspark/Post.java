package jvcspark;

import java.io.Serializable;
import java.util.List;


public class Post implements Serializable {

    private long id; 
    private String hour;
    private String topic;
    private String auteur;
    private String message;
    private List<String> stickers;

    public Post(long id, String hour, String topic, String auteur, String message, List<String> stickers){
        this.id = id;
        this.hour = hour;
        this.topic = topic;
        this.auteur = auteur;
        this.message = message;
        this.stickers= stickers;
    }

    public long getId(){
        return this.id;
    }

    public String getTopic(){
        return this.topic;
    }

    public String getAuteur(){
        return this.auteur;
    }

    public String getMessage(){
        return this.message;
    }
    public List<String> getStickers(){
        return this.stickers;
    }

    public String getStickersAsString(){
        String res = "";
        for(String s : this.stickers){
            res += s + " ";
        }
        return res;
    }


    public String toString() { 
        String res =  this.hour + "\n" + this.topic + "\n" + this.auteur + "\n" + this.message + "\n";
        for(String s : this.stickers){
            res += s + "\n";
        }
        res = res.substring(0, res.length()-1);  
        return res;
    } 



    
}
