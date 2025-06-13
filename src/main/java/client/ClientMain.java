package client;

import common.NetworkConstants;
import java.util.Scanner;

/**
 * The main entry point for the client application. Its purpose is to initialize and start the
 * GameClient.
 */
public class ClientMain {
  public static void main(String[] args) {
    String host = NetworkConstants.DEFAULT_HOST;
    int port = NetworkConstants.DEFAULT_PORT;

    // Command-line arguments provide a simple way to override the
    // default host and port for connecting to different servers.
    if (args.length > 0) {
      host = args[0];
      if (args.length > 1) {
        try {
          port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
          System.err.println("Invalid port number provided. Using default port " + port);
        }
      }
    }

    System.out.println("Attempting to connect to server at " + host + ":" + port);

    GameClient client = new GameClient(host, port);
    // The try-with-resources statement ensures that the Scanner is always
    // closed properly, preventing resource leaks.
    try (Scanner scanner = new Scanner(System.in)) {
      client.start(scanner);
    } catch (Exception e) {
      System.err.println("\nCLIENT MAIN ERROR: An unexpected error occurred: " + e.getMessage());
      e.printStackTrace();
    } finally {
      System.out.println("\nClient application is terminating...");
    }
  }
}
