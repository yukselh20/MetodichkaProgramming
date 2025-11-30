package client;

import java.util.NoSuchElementException;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is run in a dedicated thread to handle all raw user input. Its sole responsibility
// is to read lines from the console and delegate the processing to the GameClient mediator.
public class ClientInputHandler implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ClientInputHandler.class);

  private final Scanner scanner;
  private final ClientStateManager stateManager;
  private final GameClient gameClient;

  public ClientInputHandler(
      Scanner scanner, GameClient gameClient, ClientStateManager stateManager) {
    this.scanner = scanner;
    this.gameClient = gameClient;
    this.stateManager = stateManager;
  }

  @Override
  public void run() {
    logger.info("Client input handler thread started.");
    try {
      while (stateManager.isRunning() && scanner.hasNextLine()) {
        String input = scanner.nextLine().trim();
        logger.debug("Read user input: '{}'", input);
        gameClient.processUserInput(input);
      }
      // This catch block handles the expected exception when the input stream
      // is closed, which is a normal part of the shutdown process.
    } catch (NoSuchElementException | IllegalStateException e) {
      // This can happen if System.in gets closed, which I treat as a clean exit.
      logger.info("Input stream closed, shutting down input handler thread.");
    } finally {
      // If I exit the loop for any reason, I must signal other threads to shut down.
      logger.info("ClientInputHandler is signaling application to shut down.");
      stateManager.setRunning(false);
    }
  }
}
