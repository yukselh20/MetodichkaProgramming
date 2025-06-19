package server.SQLClasses.auth;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This utility class for hashing and verifying passwords using SHA-512.
// It keeps all my security-related hashing logic in one isolated place.
public class PasswordHasher {

  private static final Logger logger = LoggerFactory.getLogger(PasswordHasher.class);

  // Private constructor to prevent instantiation.
  private PasswordHasher() {}

  // This hashes a plain-text password using the SHA-512 algorithm.
  public static String hash(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Password cannot be null or empty.");
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte[] messageDigest = md.digest(password.getBytes(StandardCharsets.UTF_8));
      BigInteger no = new BigInteger(1, messageDigest);
      String hashtext = no.toString(16);

      while (hashtext.length() < 128) {
        hashtext = "0" + hashtext;
      }

      return hashtext;
    } catch (NoSuchAlgorithmException e) {
      // This exception should theoretically never happen if SHA-512 is a standard algorithm.
      logger.error("FATAL: SHA-512 algorithm not found in the Java security architecture.", e);
      throw new RuntimeException("SHA-512 algorithm not found", e);
    }
  }

  // This verifies a plain-text password against a stored hash.
  public static boolean verify(String plainPassword, String hashedPassword) {
    if (plainPassword == null || hashedPassword == null) {
      return false;
    }
    // Hash the plain-text password and compare it to the stored hash.
    return hash(plainPassword).equals(hashedPassword);
  }
}
