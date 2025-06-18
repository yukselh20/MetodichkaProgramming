package server.SQLClasses.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.GameStateData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the Data Access Object (DAO) for the 'game_sessions' table. It encapsulates
// all database operations for saving, loading, and deleting game state.
public class GameSessionDAO {

  private static final Logger logger = LoggerFactory.getLogger(GameSessionDAO.class);

  // I use ObjectMapper to convert the GameStateData DTO to and from a JSON string.
  private final ObjectMapper objectMapper = new ObjectMapper();

  // This saves or updates a game state for a specific user and case. If a save already exists,
  // it will be overwritten. Otherwise, a new record is created.
  public void saveOrUpdateGameState(int ownerId, String caseTitle, GameStateData data) {
    // This SQL statement uses a PostgreSQL-specific "ON CONFLICT" clause. It attempts an
    // INSERT, and if a row with the same unique key exists, it performs an UPDATE instead.
    // This is perfect for my automatic, overwriting save logic.
    String sql =
        "INSERT INTO game_sessions (owner_id, case_title, game_state_data, last_saved_at) "
            + "VALUES (?, ?, ?::jsonb, NOW()) "
            + "ON CONFLICT (owner_id, case_title) "
            + "DO UPDATE SET game_state_data = EXCLUDED.game_state_data, last_saved_at = NOW()";

    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      // I'll serialize the GameStateData object into a JSON string for storage.
      String gameStateJson = objectMapper.writeValueAsString(data);

      pstmt.setInt(1, ownerId);
      pstmt.setString(2, caseTitle);
      pstmt.setString(3, gameStateJson);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      logger.error(
          "Database error during saveOrUpdateGameState for user {} and case '{}'",
          ownerId,
          caseTitle,
          e);
    } catch (JsonProcessingException e) {
      logger.error(
          "Error serializing game state to JSON for user {} and case '{}'", ownerId, caseTitle, e);
    }
  }

  // Note: I will implement the `load` and `delete` methods later.
  // For now, saveOrUpdateGameState is the priority for automatic saving.
}
