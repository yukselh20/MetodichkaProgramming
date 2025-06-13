package common;

import Core.*;
import common.dto.JournalEntryDTO;
import common.dto.RoomDescriptionDTO;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for a command's execution context, acting as a crucial abstraction layer.
 * This interface allows commands in the `common` package to interact with the server's game state
 * and trigger actions without depending on concrete server classes, which is a key application of
 * the Dependency Inversion Principle.
 */
public interface ICommandContext {

  // --- Methods for Reading Game State ---
  boolean isCaseStarted(String playerId);

  Detective getPlayerDetective(String playerId);

  DoctorWatson getWatson();

  TaskList getTaskList();

  Optional<Suspect> findSuspect(String suspectName);

  List<JournalEntryDTO> getJournalEntries();

  // --- Methods for Performing Actions & Sending Responses ---

  /** Sends a serializable message back to a specific player. */
  void sendResponseToPlayer(String playerId, Serializable message);

  /** Adds an entry to the game's shared journal and broadcasts the update. */
  void addEntryToJournal(String entry, String playerId);

  /** Generates a detailed description DTO for a given room. */
  RoomDescriptionDTO generateRoomDescriptionDTO(Room room, String requestingPlayerId);

  /** Handles the complex logic for a 'deduce' action. */
  void handleDeduceCommand(String playerId, String targetObjectName);

  /** Notifies the context of player movement to trigger other game logic. */
  void handlePlayerMovement(String playerId, Room oldRoom, Room newRoom);

  /** Processes a player's attempt to start the case. */
  void processStartCaseAttempt(String initiatingPlayerId);

  /** Processes a player's attempt to initiate the final exam. */
  void processFinalExamAttempt(String initiatingPlayerId);

  /** Processes a submitted answer for the final exam. */
  void processExamAnswer(String playerId, int questionNumber, String answerText);
}
