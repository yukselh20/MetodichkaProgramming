package server.SQLClasses.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.GameStateData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object (DAO) for the 'game_sessions' table. This class encapsulates all database
 * operations for saving, loading, and deleting game state progress.
 */
public class GameSessionDAO {

  private static final Logger logger = LoggerFactory.getLogger(GameSessionDAO.class);

  // ObjectMapper is used to convert the GameStateData DTO to and from a JSON string.
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Saves or updates a game state for a specific user and case. If a save already exists for this
   * user and case, it will be overwritten. Otherwise, a new save record will be created.
   *
   * @param ownerId The ID of the user who owns this save.
   * @param caseTitle The title of the case being saved.
   * @param data The GameStateData object to be saved.
   */
  public void saveOrUpdateGameState(int ownerId, String caseTitle, GameStateData data) {
    // This SQL statement uses a PostgreSQL-specific "ON CONFLICT" clause.
    // It attempts to INSERT a new row. If a row with the same (owner_id, case_title)
    // already exists, it will UPDATE that existing row instead. This is perfect
    // for our automatic, overwriting save logic.
    String sql =
        "INSERT INTO game_sessions (owner_id, case_title, game_state_data, last_saved_at) "
            + "VALUES (?, ?, ?::jsonb, NOW()) "
            + "ON CONFLICT (owner_id, case_title) "
            + "DO UPDATE SET game_state_data = EXCLUDED.game_state_data, last_saved_at = NOW()";

    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      // Serialize the GameStateData object into a JSON string for storage.
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

  // Note: will implement the `load` and `delete` methods later.
  // For now, saveOrUpdateGameState is the priority for automatic saving.
}
