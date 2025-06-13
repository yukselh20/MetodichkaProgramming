package client;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Handles all raw user input by running in a dedicated thread. Its sole responsibility is to read
 * lines from the console and delegate the processing to the GameClient mediator.
 */
public class ClientInputHandler implements Runnable {

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
    try {
      // The loop continues as long as the application is running and there is
      // input to be read, preventing the thread from terminating prematurely.
      while (stateManager.isRunning() && scanner.hasNextLine()) {
        String input = scanner.nextLine().trim();
        gameClient.processUserInput(input);
      }
      // This catch block handles the expected exception when the input stream
      // is closed, which is a normal part of the application shutdown process.
    } catch (NoSuchElementException | IllegalStateException e) {
      // This can happen if System.in is closed, which is a clean exit condition.
    } finally {
      // In case of any exit from the loop (error or normal), this signals
      // all other threads that the client is shutting down.
      stateManager.setRunning(false);
    }
  }
}
