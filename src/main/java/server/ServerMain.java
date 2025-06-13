package server;

import common.NetworkConstants;
import java.io.IOException;

/**
 * The main entry point for the server application. Its sole responsibility is to parse
 * configuration, initialize the GameServer, and start it.
 */
public class ServerMain {

  public static void main(String[] args) {
    int port = NetworkConstants.DEFAULT_PORT;
    // The cases directory can be configured via an environment variable or a command-line argument.
    String casesDir = System.getenv().getOrDefault("CASES_DIR", "cases");

    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Invalid port: " + args[0] + ". Default: " + port);
      }
    }
    if (args.length > 1) {
      casesDir = args[1];
    }

    GameServer server = null;
    try {
      System.out.println("Server init: Port=" + port + ", CasesDir='" + casesDir + "'");
      server = new GameServer(port, casesDir);

      // A shutdown hook is registered with the JVM to ensure that the server's
      // shutdown logic (like saving games) is called even if the server is
      // terminated externally (e.g., with Ctrl+C).
      final GameServer finalServer = server;
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    System.out.println("Shutdown hook initiated...");
                    if (finalServer != null) {
                      finalServer.shutdown();
                    }
                  },
                  "ServerShutdownHook"));

      System.out.println("Starting server...");
      server.start();

    } catch (IOException e) {
      System.err.println("FATAL: Server failed to initialize/start: " + e.getMessage());
      e.printStackTrace();
      if (server != null) server.shutdown();
      System.exit(1);
    } catch (Exception e) {
      System.err.println("FATAL: Unexpected server error: " + e.getMessage());
      e.printStackTrace();
      if (server != null) server.shutdown();
      System.exit(1);
    }
    System.out.println("Server process has ended.");
  }
}
