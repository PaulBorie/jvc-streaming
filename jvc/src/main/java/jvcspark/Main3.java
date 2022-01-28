package jvcspark;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;


public class Main3 {

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        String scraperHost = System.getenv("SCRAPER_HOST");
        if (scraperHost == null){
            scraperHost = "localhost";
        }

        // Create the context with a 3 second batch size
        SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("JavaCustomReceiver");
        JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(3000));
        ssc.checkpoint(".");
        JavaReceiverInputDStream<Post> posts = ssc.receiverStream(new JvcCustomReceiver(scraperHost, Integer.parseInt("6667")));
        //On affiche  tous les post du pseudo "surf_colos2"
        JavaDStream<Post> colos = posts.filter(x -> x.getAuteur().equals("surf_colos2"));
        colos.print();
        
        ssc.start();
        ssc.awaitTermination();
      }
    
}
