package client;

import common.NetworkConstants;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the main entry point for the client application. Its purpose is to initialize
// and start the GameClient.
public class ClientMain {
  private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

  public static void main(String[] args) {
    String host = NetworkConstants.DEFAULT_HOST;
    int port = NetworkConstants.DEFAULT_PORT;

    // allows the command-line arguments to override the default host and port.
    if (args.length > 0) {
      host = args[0];
      if (args.length > 1) {
        try {
          port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
          logger.error("Invalid port number provided: '{}'. Using default port {}.", args[1], port);
        }
      }
    }

    logger.info("Attempting to connect to server at {}:{}", host, port);

    GameClient client = new GameClient(host, port);
    // try-with-resources statement is used here to ensure the Scanner is always closed properly.
    try (Scanner scanner = new Scanner(System.in)) {
      client.start(scanner);
    } catch (Exception e) {
      logger.error("CLIENT MAIN ERROR: An unexpected error occurred.", e);
    } finally {
      logger.info("Client application is terminating...");
    }
  }
}
