package common;

/**
 * A utility class that centralizes all static network-related constants. Using a single class for
 * these values makes them easy to find and modify.
 */
public final class NetworkConstants {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8080;
  public static final int MAX_PLAYERS_PER_GAME = 2;
  public static final int DEFAULT_BUFFER_SIZE = 8192; // 8 KB

  // Defines the size of the integer prefix used for message framing,
  // which tells the receiver how long the incoming message is.
  public static final int MESSAGE_LENGTH_HEADER_SIZE = 4;

  // A private constructor prevents this utility class from being instantiated.
  private NetworkConstants() {}
}
