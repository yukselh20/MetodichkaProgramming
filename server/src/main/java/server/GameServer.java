package server;

import common.*;
import common.dto.TextMessage;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main server class that handles all low-level network I/O and client management. It uses a
 * single thread with a Selector (Java NIO) to manage many client connections efficiently without
 * needing a thread per client.
 */
public class GameServer {
  private final int port;
  private final Selector selector;
  private final ServerSocketChannel serverSocketChannel;
  private volatile boolean running = true;

  // A thread-safe map to track all connected clients by their network channel.
  private final Map<SocketChannel, ClientSession> clients = new ConcurrentHashMap<>();
  private final GameSessionManager gameSessionManager;

  private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

  public GameServer(int port, String casesDir) throws IOException {
    this.port = port;
    this.selector = Selector.open();
    this.serverSocketChannel = ServerSocketChannel.open();
    this.gameSessionManager = new GameSessionManager(this, casesDir);

    serverSocketChannel.bind(new InetSocketAddress(port));
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    // The server registers interest in OP_ACCEPT to be notified of new client connections.
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    logger.info("GameServer started on port {}", port);
  }

  /**
   * The main server event loop. It waits for network events (like new connections or incoming data)
   * and dispatches them to handler methods.
   */
  public void start() {
    logger.info("Server event loop started.");
    try {
      while (running) {
        // `selector.select()` blocks until at least one registered event occurs.
        if (selector.select() == 0) {
          if (!running) break;
          continue;
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        while (keyIterator.hasNext()) {
          SelectionKey key = keyIterator.next();
          keyIterator.remove();

          try {
            if (!key.isValid()) {
              disconnectClient(key);
              continue;
            }
            if (key.isAcceptable()) {
              handleAccept(key);
            } else if (key.isReadable()) {
              handleRead(key);
            } else if (key.isWritable()) {
              handleWrite(key);
            }
          } catch (CancelledKeyException | IOException e) {
            disconnectClient(key);
          } catch (Exception e) {
            logger.error("Error processing key for {}: {}", getClientInfo(key), e.getMessage(), e);
            disconnectClient(key);
          }
        }
      }
    } catch (ClosedSelectorException e) {
      logger.warn("Selector closed, server loop stopping.");
    } catch (IOException e) {
      logger.error("IOException in server loop.", e);
    } finally {
      logger.info("Server loop finished.");
      if (running) shutdown();
    }
  }

  // Accepts a new client connection and sets up a ClientSession for it.
  private void handleAccept(SelectionKey key) throws IOException {
    logger.debug("==> handleAccept() method has been entered.");
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel clientChannel = serverChannel.accept();
    if (clientChannel == null) {
      logger.debug("clientChannel was null, exiting handleAccept.");
      return;
    }

    logger.debug("Connection accepted from: {}", clientChannel.getRemoteAddress());

    clientChannel.configureBlocking(false);
    clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

    String playerId = "Player_" + UUID.randomUUID().toString().substring(0, 6);
    ClientSession session = new ClientSession(clientChannel, playerId);
    clients.put(clientChannel, session);

    try {
      // The new client channel is registered with the selector, with interest in read operations.
      clientChannel.register(selector, SelectionKey.OP_READ);
      logger.info("Client connected: {}, ID: {}", getClientInfo(clientChannel), playerId);
      gameSessionManager.handleNewClientConnection(session);
    } catch (ClosedChannelException e) {
      logger.error("Channel closed for new client {} during registration.", playerId, e);
      clients.remove(clientChannel);
    }
  }

  // Handles reading incoming data from a client channel.
  private void handleRead(SelectionKey key) throws IOException {
    SocketChannel clientChannel = (SocketChannel) key.channel();
    ClientSession session = clients.get(clientChannel);
    if (session == null) {
      key.channel().close();
      key.cancel();
      return;
    }

    ByteBuffer readBuffer = session.getReadBuffer();
    int bytesRead;
    try {
      bytesRead = clientChannel.read(readBuffer);
    } catch (IOException e) {
      throw new IOException(
          "Client " + session.getPlayerId() + " read error: " + e.getMessage(), e);
    }

    if (bytesRead == -1)
      throw new IOException("Client " + session.getPlayerId() + " closed connection.");

    // This block processes the received bytes, deframing messages based on their length prefix.
    if (bytesRead > 0) {
      readBuffer.flip();
      while (readBuffer.remaining() >= NetworkConstants.MESSAGE_LENGTH_HEADER_SIZE) {
        readBuffer.mark();
        int messageLength = readBuffer.getInt();

        if (messageLength <= 0 || messageLength > readBuffer.capacity() * 2) {
          throw new IOException(
              "Invalid message length " + messageLength + " from " + session.getPlayerId());
        }
        if (readBuffer.remaining() >= messageLength) {
          byte[] messageBytes = new byte[messageLength];
          readBuffer.get(messageBytes);
          try {
            Object message = SerializationUtils.deserialize(messageBytes);
            gameSessionManager.routeMessage(session, message);
          } catch (Exception e) {
            logger.error(
                "Deserialization/Routing error from {}: {}",
                session.getPlayerId(),
                e.getMessage(),
                e);
            queueWrite(session.getChannel(), new TextMessage("Error: Bad message format."));
          }
        } else {
          readBuffer.reset();
          break;
        }
      }
      readBuffer.compact();
    }
  }

  // Handles writing data from a client's outgoing queue to their channel.
  private void handleWrite(SelectionKey key) throws IOException {
    SocketChannel clientChannel = (SocketChannel) key.channel();
    ClientSession session = clients.get(clientChannel);
    if (session == null) {
      key.cancel();
      if (clientChannel.isOpen()) clientChannel.close();
      return;
    }

    Queue<ByteBuffer> writeQueue = session.getWriteQueue();
    while (!writeQueue.isEmpty()) {
      ByteBuffer buffer = writeQueue.peek();
      if (buffer == null) {
        writeQueue.poll();
        continue;
      }
      if (clientChannel.write(buffer) == 0) return; // Socket buffer is full, must wait.
      if (!buffer.hasRemaining()) writeQueue.poll();
      else return; // Message was only partially written.
    }
    // Once the queue is empty, we are no longer interested in write events for this key.
    if (writeQueue.isEmpty() && key.isValid()) key.interestOps(SelectionKey.OP_READ);
  }

  /**
   * Queues a message to be sent to a specific client. This method is thread-safe and can be called
   * from any part of the server logic.
   */
  public void queueWrite(SocketChannel channel, Serializable message) {
    ClientSession session = clients.get(channel);
    if (session == null || !channel.isOpen()) return;
    try {
      ByteBuffer buffer = SerializationUtils.serializeWithFraming(message);
      session.getWriteQueue().offer(buffer);
      SelectionKey key = channel.keyFor(selector);
      // If we aren't already registered for write events, we add OP_WRITE interest
      // and wake up the selector to process it immediately.
      if (key != null && key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
      }
    } catch (IOException | CancelledKeyException e) {
      disconnectClient(channel.keyFor(selector));
    }
  }

  // A robust method for closing a client's connection and cleaning up resources.
  private void disconnectClient(SelectionKey key) {
    if (key == null) return;
    Channel channel = key.channel();
    if (!(channel instanceof SocketChannel)) {
      try {
        key.cancel();
        if (channel != null && channel.isOpen()) channel.close();
      } catch (IOException e) {
        // Ignored, as we are already handling a disconnect.
      }
      return;
    }
    SocketChannel clientChannel = (SocketChannel) channel;
    ClientSession session = clients.remove(clientChannel);

    if (session != null) {
      logger.info(
          "Client disconnected: {} (ID: {})", getClientInfo(clientChannel), session.getPlayerId());
      gameSessionManager.handleClientDisconnect(session);
    }
    try {
      key.cancel();
      if (clientChannel.isOpen()) clientChannel.close();
    } catch (IOException e) {
      // Ignored.
    }
  }

  // A graceful shutdown sequence for the entire server.
  public void shutdown() {
    if (!running) return;
    logger.info("Server shutdown initiated...");
    running = false;
    if (selector.isOpen()) selector.wakeup();
    try {
      if (selector.isOpen()) {
        for (SelectionKey key : selector.keys()) {
          try {
            if (key.channel() != null) key.channel().close();
          } catch (IOException e) {
            // Ignored, we are shutting down anyway.
          }
        }
        selector.close();
      }
      clients.clear();
      logger.info("Server shutdown complete.");
    } catch (IOException e) {
      logger.error("Error during server shutdown.", e);
    }
  }

  // Helper methods for logging client connection info.
  private String getClientInfo(SelectionKey key) {
    if (key == null) return "[NullKey]";
    return getClientInfo(key.channel());
  }

  private String getClientInfo(Channel channel) {
    if (channel == null) return "[NullChannel]";

    if (channel instanceof SocketChannel) {
      SocketChannel sc = (SocketChannel) channel;
      try {
        return sc.getRemoteAddress() != null
            ? sc.getRemoteAddress().toString()
            : "[SC-NoRemoteAddr]";
      } catch (IOException | NullPointerException e) {
        return "[SC-AddrErr]";
      }
    }
    if (channel instanceof ServerSocketChannel) {
      ServerSocketChannel ssc = (ServerSocketChannel) channel;
      try {
        SocketAddress localAddress = ssc.getLocalAddress();
        if (localAddress instanceof InetSocketAddress) {
          return "ServerSocket@" + ((InetSocketAddress) localAddress).getPort();
        } else {
          return "ServerSocket@"
              + (localAddress != null ? localAddress.toString() : "[NoLocalAddr]");
        }
      } catch (IOException e) {
        return "[SrvSock-AddrErr]";
      }
    }
    return "[UnknownChannelType:" + channel.getClass().getSimpleName() + "]";
  }

  public GameSessionManager getGameSessionManager() {
    return this.gameSessionManager;
  }

  public Map<SocketChannel, ClientSession> getClients() {
    return this.clients;
  }
}
