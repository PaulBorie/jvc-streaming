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
import java.util.regex.Pattern;
import java.util.Arrays;

import org.apache.spark.api.java.Optional;
import scala.Tuple2;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;


public class Main4 {

    private static final Pattern SPACE = Pattern.compile(" ");

    
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
        
        JavaDStream<String> messagesWords = posts.flatMap(x -> Arrays.asList(SPACE.split(x.getMessage())).iterator());
        JavaDStream<String> longMessagesWords = messagesWords.filter(w -> w.length() >= 5);
        JavaPairDStream<String, Integer> tuples = longMessagesWords.mapToPair(s -> new Tuple2<>(s, 1));
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

        orderedSortedCounts.print(15);
        
        ssc.start();
        ssc.awaitTermination();
      }
    
}
