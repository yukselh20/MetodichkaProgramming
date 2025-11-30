package common;

import java.io.*;
import java.nio.ByteBuffer;

// This utility class with static methods for Java object serialization and
// deserialization to abstract the low-level I/O operations for network communication.
public final class SerializationUtils {

  public static byte[] serialize(Serializable object) throws IOException {
    if (object == null) {
      throw new IllegalArgumentException("Cannot serialize null object.");
    }
    // A try-with-resources block ensures the streams are always closed correctly.
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      return baos.toByteArray();
    }
  }

  public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Cannot deserialize from null or empty byte array.");
    }
    try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      return ois.readObject();
    }
  }

  // This method serializes an object and prepends its length as a 4-byte integer. This "message
  // framing"
  // is essential for the receiver to know how many bytes to read from the network for one complete
  // object.
  public static ByteBuffer serializeWithFraming(Serializable object) throws IOException {
    byte[] objectBytes = serialize(object);
    int length = objectBytes.length;

    ByteBuffer buffer = ByteBuffer.allocate(NetworkConstants.MESSAGE_LENGTH_HEADER_SIZE + length);
    buffer.putInt(length);
    buffer.put(objectBytes);
    buffer.flip();
    return buffer;
  }

  // Private constructor prevents instantiation of this utility class.
  private SerializationUtils() {}
}
