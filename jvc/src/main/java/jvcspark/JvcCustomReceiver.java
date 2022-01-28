package jvcspark;

import com.google.common.io.Closeables;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;


/**
 * Custom Receiver that receives data over a socket. Received bytes is interpreted as
 * text and \n delimited lines are considered as records. They are then counted and printed.
 *
 *
 */

public class JvcCustomReceiver extends Receiver<Post> {


  // ============= Receiver code that receives data over a socket ==============

  String host = null;
  int port = -1;

  public JvcCustomReceiver(String host_ , int port_) {
    super(StorageLevel.MEMORY_AND_DISK_2());
    host = host_;
    port = port_;
  }

  @Override
  public void onStart() {
    // Start the thread that receives data over a connection
    new Thread(this::receive).start();
  }

  @Override
  public void onStop() {
    // There is nothing much to do as the thread calling receive()
    // is designed to stop by itself isStopped() returns false
  }

  /** Create a socket connection and receive data until receiver is stopped */
  private void receive() {
    try {
      Socket socket = null;
      BufferedReader reader = null;
      try {
        // connect to the server
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        // Until stopped or connection broken continue reading
        String userInput;
        while (!isStopped() && (userInput = reader.readLine()) != null) {
          //System.out.println(userInput);

          Gson gson = new Gson();
          Post post = gson.fromJson(userInput, Post.class);
          store(post);
          //System.out.println(post);
          //System.out.println();
        }

      } finally {
        Closeables.close(reader, /* swallowIOException = */ true);
        Closeables.close(socket,  /* swallowIOException = */ true);
      }
      // Restart in an attempt to connect again when server is active again
      restart("Trying to connect again");
    } catch(ConnectException ce) {
      // restart if could not connect to server
      restart("Could not connect", ce);
    } catch(Throwable t) {
      restart("Error receiving data", t);
    }
  }
}
