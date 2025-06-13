package server;

import common.NetworkConstants;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for the server application. Its sole responsibility is to parse
 * configuration, initialize the GameServer, and start it.
 */
public class ServerMain {

  // Create a static logger instance for this class
  private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

  public static void main(String[] args) {
    int port = NetworkConstants.DEFAULT_PORT;
    // The cases directory can be configured via an environment variable or a command-line argument.
    String casesDir = System.getenv().getOrDefault("CASES_DIR", "cases");

    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        logger.error("Invalid port: {}. Using default: {}", args[0], port);
      }
    }
    if (args.length > 1) {
      casesDir = args[1];
    }

    GameServer server = null;
    try {
      logger.info("Server init: Port={}, CasesDir='{}'", port, casesDir);
      server = new GameServer(port, casesDir);

      // A shutdown hook is registered with the JVM to ensure that the server's
      // shutdown logic (like saving games) is called even if the server is
      // terminated externally (e.g., with Ctrl+C).
      final GameServer finalServer = server;
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    logger.info("Shutdown hook initiated...");
                    finalServer.shutdown();
                  },
                  "ServerShutdownHook"));

      logger.info("Starting server...");
      server.start();

    } catch (IOException e) {
      logger.error("FATAL: Server failed to initialize/start.", e);
      System.exit(1);
    } catch (Exception e) {
      logger.error("FATAL: Unexpected server error.", e);
      if (server != null) server.shutdown();
      System.exit(1);
    }
    logger.info("Server process has ended.");
  }
}
