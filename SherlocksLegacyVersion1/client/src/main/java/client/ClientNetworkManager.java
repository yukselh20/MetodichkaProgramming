package client;

import common.NetworkConstants;
import common.SerializationUtils;
import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// all low-level network I/O for the client is managed using Java NIO. I run this in a separate
// thread for non-blocking communication and use callbacks to pass events back to the GameClient,
// decoupling the network logic from the game logic.
public class ClientNetworkManager implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ClientNetworkManager.class);

  private final String host;
  private final int port;
  private final ClientStateManager stateManager;
  private final Consumer<Object> onMessageReceived;
  private final Consumer<String> onDisconnect;

  private volatile SocketChannel socketChannel;
  private volatile Selector selector;

  private final BlockingQueue<Serializable> outgoingMessages = new LinkedBlockingQueue<>();
  private final ByteBuffer readBuffer = ByteBuffer.allocate(NetworkConstants.DEFAULT_BUFFER_SIZE);

  public ClientNetworkManager(
      String host,
      int port,
      ClientStateManager stateManager,
      Consumer<Object> onMessageReceived,
      Consumer<String> onDisconnect) {
    this.host = host;
    this.port = port;
    this.stateManager = stateManager;
    this.onMessageReceived = onMessageReceived;
    this.onDisconnect = onDisconnect;
  }

  public void sendMessage(Serializable message) {
    outgoingMessages.add(message);
    logger.debug(
        "Message {} added to outgoing queue (current size: {}).",
        message.getClass().getSimpleName(),
        outgoingMessages.size());
    if (selector != null && selector.isOpen()) {
      selector.wakeup();
    }
  }

  @Override
  public void run() {
    while (stateManager.isRunning()) {
      try {
        if (socketChannel == null
            || !socketChannel.isOpen()
            || selector == null
            || !selector.isOpen()) {
          Thread.sleep(200);
          continue;
        }

        if (!outgoingMessages.isEmpty()) {
          SelectionKey key = socketChannel.keyFor(selector);
          if (key != null && key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
          }
        }
        if (selector.select(1000) == 0) {
          continue;
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        while (keyIterator.hasNext()) {
          SelectionKey key = keyIterator.next();
          keyIterator.remove();

          if (!key.isValid()) continue;

          if (key.isReadable()) {
            handleRead();
          }
          if (key.isWritable()) {
            handleWrite(key);
          }
        }
      } catch (IOException | CancelledKeyException e) {
        logger.warn("Connection lost in main loop: {}", e.getMessage());
        handleDisconnect("Connection lost: " + e.getMessage());
      } catch (InterruptedException e) {
        logger.info("Network thread interrupted, shutting down.");
        Thread.currentThread().interrupt();
        stateManager.setRunning(false);
      }
    }
    close();
  }

  public boolean connect() {
    int maxRetries = 5;
    long delayMillis = 1000;
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      logger.info("Connection attempt #{}...", attempt);
      if (!stateManager.isRunning()) return false;

      try {
        // create a new Selector and SocketChannel for each connection
        // attempt to ensure a clean state after a failure.
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.connect(new InetSocketAddress(host, port));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        long connectTimeout = 5000;
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < connectTimeout) {
          if (selector.select(1000) > 0) {
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
              SelectionKey key = keys.next();
              keys.remove();
              if (key.isConnectable()) {
                logger.debug("Channel is connectable.");
                if (socketChannel.finishConnect()) {
                  logger.info("==> Connection FINISHED successfully!");
                  // Once connected, I'm only interested in reading data
                  // until there is something to send.
                  key.interestOps(SelectionKey.OP_READ);
                  return true;
                }
              }
            }
          }
          if (!socketChannel.isConnectionPending()) break;
        }
        close();

      } catch (ConnectException e) {
        logger.warn("Connection attempt #{} failed: {}", attempt, e.getMessage());
        close();
      } catch (IOException e) {
        logger.error("Error during connection attempt:", e);
        close();
        onDisconnect.accept("Error connecting to server: " + e.getMessage());
        return false;
      }

      if (attempt < maxRetries) {
        try {
          Thread.sleep(delayMillis * attempt);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    }
    logger.error("Failed to connect to the server after {} attempts.", maxRetries);
    onDisconnect.accept("Failed to connect to the server after " + maxRetries + " attempts.");
    return false;
  }

  private void handleRead() throws IOException {
    int bytesRead = socketChannel.read(readBuffer);
    if (bytesRead == -1) {
      throw new IOException("Server closed the connection gracefully.");
    }

    if (bytesRead > 0) {
      logger.debug("Read {} bytes from the network.", bytesRead);
      readBuffer.flip();
      // This loop processes all complete messages within the buffer. It's
      // important because a single read might receive multiple messages
      // or an incomplete one from the TCP stream.
      while (readBuffer.remaining() >= NetworkConstants.MESSAGE_LENGTH_HEADER_SIZE) {
        readBuffer.mark();
        int messageLength = readBuffer.getInt();

        if (messageLength <= 0 || messageLength > readBuffer.capacity() * 2) {
          throw new IOException("Invalid message length from server: " + messageLength);
        }

        if (readBuffer.remaining() >= messageLength) {
          byte[] messageBytes = new byte[messageLength];
          readBuffer.get(messageBytes);
          try {
            Object received = SerializationUtils.deserialize(messageBytes);
            logger.debug("Deserialized message of type: {}", received.getClass().getSimpleName());
            onMessageReceived.accept(received);
          } catch (Exception e) {
            logger.error("Error deserializing server message", e);
          }
        } else {
          // If the buffer doesn't contain the full message yet, I reset its
          // position to the mark and wait for more data to arrive.
          logger.trace(
              "Incomplete message received. Waiting for more data. Needed: {}, Have: {}",
              messageLength,
              readBuffer.remaining());
          readBuffer.reset();
          break;
        }
      }
      // `compact()` moves any leftover, partial message data to the beginning
      // of the buffer, preparing it for the next read.
      readBuffer.compact();
    }
  }

  private void handleWrite(SelectionKey key) throws IOException {
    Serializable messageToSend;
    while ((messageToSend = outgoingMessages.peek()) != null) {
      ByteBuffer buffer = SerializationUtils.serializeWithFraming(messageToSend);
      logger.debug(
          "Attempting to write message of type {} ({} bytes) to network.",
          messageToSend.getClass().getSimpleName(),
          buffer.limit());

      while (buffer.hasRemaining()) {
        // If write() returns 0, the socket's send buffer is full.
        // I must stop writing and wait for the selector to notify me again.
        if (socketChannel.write(buffer) == 0) {
          logger.warn("Socket buffer is full. Pausing write.");
          return;
        }
      }
      // I use `poll()` to remove the message from the queue only after it's
      // been completely written to the socket.
      outgoingMessages.poll();
    }

    // After the send queue is empty, I remove the OP_WRITE interest to
    // stop receiving unnecessary notifications from the selector.
    if (key.isValid()) {
      logger.trace("Write queue empty. Unregistering OP_WRITE interest.");
      key.interestOps(SelectionKey.OP_READ);
    }
  }

  private void handleDisconnect(String reason) {
    close();
    onDisconnect.accept(reason);
  }

  public void close() {
    logger.debug("Closing network resources (selector and socket channel)...");
    outgoingMessages.clear();
    try {
      if (selector != null && selector.isOpen()) {
        selector.close();
      }
    } catch (IOException e) {
      // Ignored, I'm already shutting down.
      logger.warn("Exception while closing selector, ignoring as we are shutting down.", e);
    }
    try {
      if (socketChannel != null && socketChannel.isOpen()) {
        socketChannel.close();
      }
    } catch (IOException e) {
      // Ignored.
      logger.warn("Exception while closing socket channel, ignoring as we are shutting down.", e);
    }
  }
}
