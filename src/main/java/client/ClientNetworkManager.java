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

/**
 * Manages all low-level network I/O for the client using Java NIO. It runs in a separate thread to
 * handle non-blocking communication, and uses callbacks to pass events (like received messages or
 * disconnects) back to the GameClient, decoupling network logic from game logic.
 */
public class ClientNetworkManager implements Runnable {

  private final String host;
  private final int port;
  private final ClientStateManager stateManager;
  // Callbacks are used to invert control, allowing this class to notify
  // the GameClient of network events without needing a direct reference to it.
  private final Consumer<Object> onMessageReceived;
  private final Consumer<String> onDisconnect;

  private volatile SocketChannel socketChannel;
  private volatile Selector selector;

  // A thread-safe queue is used to pass messages from the main input thread
  // to this dedicated network thread for sending.
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
    // Waking up the selector is crucial. If the selector is blocked in a call
    // to select(), this ensures it wakes up to register the new write interest.
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
          // A short sleep prevents this loop from consuming 100% CPU
          // while waiting for a connection to be established.
          Thread.sleep(200);
          continue;
        }

        // If the outgoing queue has messages, we register OP_WRITE interest.
        // This tells the selector to notify us when the socket is ready for writing.
        if (!outgoingMessages.isEmpty()) {
          SelectionKey key = socketChannel.keyFor(selector);
          if (key != null && key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
          }
        }

        // The timeout on select() ensures the loop doesn't block forever,
        // allowing it to periodically check the stateManager.isRunning() flag.
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
        handleDisconnect("Connection lost: " + e.getMessage());
      } catch (InterruptedException e) {
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
      // --- NEW DEBUG LINE ---
      System.out.println("[CLIENT-DEBUG] Connection attempt #" + attempt);
      if (!stateManager.isRunning()) return false;

      try {
        // A new Selector and SocketChannel are created for each connection
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
                // --- NEW DEBUG LINE ---
                System.out.println("[CLIENT-DEBUG] Channel is connectable.");
                if (socketChannel.finishConnect()) {
                  // --- NEW DEBUG LINE ---
                  System.out.println("[CLIENT-DEBUG] ==> Connection FINISHED successfully!");
                  // Once connected, we are only interested in reading data
                  // until we have something to send.
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
        close();
      } catch (IOException e) {
        close();
        onDisconnect.accept("Error connecting to server: " + e.getMessage());
        return false;
      }

      // Exponential backoff increases the delay between retries, giving the
      // server time to recover without overwhelming it with connection requests.
      if (attempt < maxRetries) {
        try {
          Thread.sleep(delayMillis * attempt);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    }
    onDisconnect.accept("Failed to connect to the server after " + maxRetries + " attempts.");
    return false;
  }

  private void handleRead() throws IOException {
    int bytesRead = socketChannel.read(readBuffer);
    if (bytesRead == -1) {
      throw new IOException("Server closed the connection gracefully.");
    }

    if (bytesRead > 0) {
      readBuffer.flip();
      // This loop processes all complete messages within the buffer. This is
      // important because a single read operation might receive multiple
      // messages or an incomplete one from the TCP stream.
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
            onMessageReceived.accept(received);
          } catch (Exception e) {
            System.err.println("\nError deserializing server message: " + e.getMessage());
          }
        } else {
          // If the buffer doesn't contain the full message yet, we reset its
          // position to the mark and wait for more data to arrive.
          readBuffer.reset();
          break;
        }
      }
      // `compact()` moves any leftover, partial message data to the beginning
      // of the buffer, preparing it for the next read operation.
      readBuffer.compact();
    }
  }

  private void handleWrite(SelectionKey key) throws IOException {
    Serializable messageToSend;
    while ((messageToSend = outgoingMessages.peek()) != null) {
      ByteBuffer buffer = SerializationUtils.serializeWithFraming(messageToSend);

      while (buffer.hasRemaining()) {
        // If write() returns 0, it means the socket's send buffer is full.
        // We must stop writing and wait for the selector to notify us again.
        if (socketChannel.write(buffer) == 0) {
          return;
        }
      }
      // `poll()` removes the message from the queue only after it has been
      // completely written to the socket.
      outgoingMessages.poll();
    }

    // After the send queue is empty, we remove the OP_WRITE interest to
    // stop receiving unnecessary notifications from the selector.
    if (key.isValid()) {
      key.interestOps(SelectionKey.OP_READ);
    }
  }

  private void handleDisconnect(String reason) {
    close();
    onDisconnect.accept(reason);
  }

  // Making the close method idempotent means it can be called multiple times
  // without causing errors, which is useful for cleanup in complex error scenarios.
  public void close() {
    outgoingMessages.clear();
    try {
      if (selector != null && selector.isOpen()) {
        selector.close();
      }
    } catch (IOException e) {
      // Ignored, as we are already shutting down.
    }
    try {
      if (socketChannel != null && socketChannel.isOpen()) {
        socketChannel.close();
      }
    } catch (IOException e) {
      // Ignored.
    }
  }
}
