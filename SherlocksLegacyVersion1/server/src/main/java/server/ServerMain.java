package server;

import common.NetworkConstants;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the main entry point for my server application. Its sole responsibility is
// to parse configuration, initialize the GameServer, and start it.
public class ServerMain {

  private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

  public static void main(String[] args) {
    int port = NetworkConstants.DEFAULT_PORT;
    String casesDir = System.getenv().getOrDefault("CASES_DIR", "cases");


    String portEnv = System.getenv("SERVER_PORT");
    if (portEnv != null) {
      try {
        int parsedPort = Integer.parseInt(portEnv);

        if (parsedPort >= 1 && parsedPort <= 65535) {
          port = parsedPort;
          logger.info("Using port from SERVER_PORT environment variable: {}", port);
        } else {
          // It's a number, but out of range. Log an error and use the default.
          logger.error(
                  "Port {} from SERVER_PORT is out of valid range (1-65535). Using default port {}.",
                  parsedPort,
                  NetworkConstants.DEFAULT_PORT);
        }
      } catch (NumberFormatException e) {
        // It's not a number. Log an error and use the default.
        logger.error(
                "Invalid number format for SERVER_PORT: '{}'. Using default port {}.",
                portEnv,
                NetworkConstants.DEFAULT_PORT);
      }
    }



    if (args.length > 1) {
      casesDir = args[1];
    }

    GameServer server = null;
    try {
      logger.info("Server init: Port={}, CasesDir='{}'", port, casesDir);
      server = new GameServer(port, casesDir);

      // I register a shutdown hook with the JVM to ensure my server's shutdown logic
      // is called, even if the server is terminated externally (e.g., with Ctrl+C).
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
