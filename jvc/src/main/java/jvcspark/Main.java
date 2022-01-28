package jvcspark;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.api.java.function.*;
import org.apache.spark.api.java.JavaPairRDD;

import java.util.List;

import org.apache.spark.api.java.Optional;
import scala.Tuple2;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;


public class Main {
    

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

        //On créer un stream de Post grâce à notre custom receiver
        JavaReceiverInputDStream<Post> posts = ssc.receiverStream(new JvcCustomReceiver(scraperHost, Integer.parseInt("6667")));

        //on prend que les post qui comportent des stickers
        JavaDStream<Post> withSticker = posts.filter(x -> !x.getStickers().isEmpty());
        JavaDStream<String> stickers = withSticker.flatMap(x -> x.getStickers().iterator());

        //On prend que les stickers Noelshack
        JavaDStream<String> noelShackStickers = stickers.filter(x -> x.startsWith("https://image.noelshack.com"));

        //On créer les tuples 
        JavaPairDStream<String, Integer> tuples = noelShackStickers.mapToPair(s -> new Tuple2<>(s, 1));
        //on réduit par clé
        JavaPairDStream<String, Integer> reduced = tuples.reduceByKey((x,y) -> x + y);


        Function2<List<Integer>, Optional<Integer>, Optional<Integer>> updateFunction =
                new Function2<List<Integer>, Optional<Integer>, Optional<Integer>>() {
                    public Optional<Integer> call(List<Integer> values, Optional<Integer> state) {
                        Integer newSum = state.or(0);
                        for(int i : values)
                        {
                            newSum += i;
                        }
                        return Optional.of(newSum);
                    }
                };

        JavaPairDStream<String, Integer> runningCounts = reduced.updateStateByKey(updateFunction);
        JavaPairDStream<Integer,String> swappedPair = runningCounts.mapToPair(x -> x.swap());
        JavaPairDStream<Integer,String> sortedCounts = swappedPair.transformToPair(new Function<JavaPairRDD<Integer,String>, JavaPairRDD<Integer,String>>() {
          @Override
          public JavaPairRDD<Integer,String> call(JavaPairRDD<Integer,String> jPairRDD) throws Exception {
                     return jPairRDD.sortByKey(false);
                   }
               });
        JavaPairDStream<String,Integer> orderedSortedCounts = sortedCounts.mapToPair(x -> x.swap());
        
        //on print les 15 stickers les plus utilisés
        orderedSortedCounts.print(15);
        
        ssc.start();
        ssc.awaitTermination();
      }
    
}
