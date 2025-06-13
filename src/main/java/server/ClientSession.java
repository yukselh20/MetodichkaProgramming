package server;

import common.NetworkConstants;
import common.dto.CaseInfoDTO;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import server.SQLClasses.model.User;

/**
 * Represents a single connected client on the server. This class holds the client's network
 * channel, unique ID, and session-specific state, acting as a stateful wrapper around the low-level
 * network connection.
 */
public class ClientSession {
  private final SocketChannel channel;
  private final String playerId;
  private String gameSessionId;
  private List<CaseInfoDTO> localCases = Collections.emptyList();
  private User authenticatedUser = null;

  // Each client has their own queue for outgoing messages to ensure
  // thread-safe writes from multiple server threads.
  private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
  // A dedicated buffer for reading data from this specific client's channel.
  private final ByteBuffer readBuffer = ByteBuffer.allocate(NetworkConstants.DEFAULT_BUFFER_SIZE);

  public ClientSession(SocketChannel channel, String playerId) {
    this.channel = channel;
    this.playerId = playerId;
    this.gameSessionId = null;
  }

  public SocketChannel getChannel() {
    return channel;
  }

  public String getPlayerId() {
    return playerId;
  }

  public String getGameSessionId() {
    return gameSessionId;
  }

  public Queue<ByteBuffer> getWriteQueue() {
    return writeQueue;
  }

  public ByteBuffer getReadBuffer() {
    return readBuffer;
  }

  public List<CaseInfoDTO> getLocalCases() {
    return localCases;
  }

  public void setGameSessionId(String gameSessionId) {
    this.gameSessionId = gameSessionId;
  }

  public void setLocalCases(List<CaseInfoDTO> localCases) {
    this.localCases =
        (localCases != null) ? Collections.unmodifiableList(localCases) : Collections.emptyList();
  }

  public User getAuthenticatedUser() {
    return authenticatedUser;
  }

  public void setAuthenticatedUser(User authenticatedUser) {
    this.authenticatedUser = authenticatedUser;
  }

  @Override
  public String toString() {
    return "ClientSession{playerId='" + playerId + "', gameSessionId='" + gameSessionId + "'}";
  }
}
