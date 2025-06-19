package common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This utility class to centralize all static network-related constants.
// This makes them easy to find and modify.
public final class NetworkConstants {

  private static final Logger logger = LoggerFactory.getLogger(NetworkConstants.class);

  public static final String DEFAULT_HOST = "localhost";
  public static final String PORT_FROM_ENV = System.getenv("SERVER_PORT");
  //to fall back to
  public static final int DEFAULT_PORT = 8080;

  public static final int MAX_PLAYERS_PER_GAME = 2;
  public static final int DEFAULT_BUFFER_SIZE = 8192; // 8 KB
  // This defines the size of the integer prefix for message framing, which tells
  // the receiver how long the incoming message is.
  public static final int MESSAGE_LENGTH_HEADER_SIZE = 4;

  // Private constructor prevents this utility class from being instantiated.
  private NetworkConstants() {}
}