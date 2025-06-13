package server.SQLClasses.auth;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for hashing and verifying passwords using SHA-512. This keeps all
 * security-related hashing logic in one isolated place.
 */
public class PasswordHasher {

  private static final Logger logger = LoggerFactory.getLogger(PasswordHasher.class);

  /** Private constructor to prevent instantiation of this utility class. */
  private PasswordHasher() {}

  /**
   * Hashes a plain-text password using the SHA-512 algorithm.
   *
   * @param password The plain-text password to hash.
   * @return A hexadecimal string representation of the SHA-512 hash.
   */
  public static String hash(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Password cannot be null or empty.");
    }
    try {
      // Get an instance of the SHA-512 message digest algorithm.
      MessageDigest md = MessageDigest.getInstance("SHA-512");

      // Calculate the message digest of the input password string.
      byte[] messageDigest = md.digest(password.getBytes(StandardCharsets.UTF_8));

      // Convert the byte array into a signum representation.
      BigInteger no = new BigInteger(1, messageDigest);

      // Convert the message digest into a hex value.
      String hashtext = no.toString(16);

      // Add preceding 0s to make it 128 characters long.
      while (hashtext.length() < 128) {
        hashtext = "0" + hashtext;
      }

      return hashtext;
    } catch (NoSuchAlgorithmException e) {
      // This exception should theoretically never happen if SHA-512 is a
      // standard algorithm provided by the Java security architecture.
      logger.error("FATAL: SHA-512 algorithm not found in the Java security architecture.", e);
      throw new RuntimeException("SHA-512 algorithm not found", e);
    }
  }

  /**
   * Verifies a plain-text password against a stored hash.
   *
   * @param plainPassword The password entered by the user.
   * @param hashedPassword The hash stored in the database.
   * @return true if the passwords match, false otherwise.
   */
  public static boolean verify(String plainPassword, String hashedPassword) {
    if (plainPassword == null || hashedPassword == null) {
      return false;
    }
    // Hash the plain-text password and compare it to the stored hash.
    return hash(plainPassword).equals(hashedPassword);
  }
}
