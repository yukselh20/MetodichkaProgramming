package server.extractors;

import Core.Room;
import Core.Suspect;
import JsonDTO.CaseFile;
import java.util.*;
import server.GameContextServer;
import server.GameSession;

// This utility class is used to parse suspect data from a CaseFile, create Suspect objects, and
// place them into the game world.
public class SuspectExtractor {

  private SuspectExtractor() {}

  public static class NoValidRoomsException extends RuntimeException {
    public NoValidRoomsException(String message) {
      super(message);
    }
  }

  public static boolean loadSuspects(CaseFile caseFile, GameContextServer context) {
    Objects.requireNonNull(caseFile, "CaseFile cannot be null.");
    Objects.requireNonNull(context, "GameContextServer cannot be null.");

    GameSession parent = context.getParentSession();
    String sessionId = (parent != null) ? parent.getSessionId() : "[NoSession]";
    boolean hasErrors = false;

    if (context.getRooms().isEmpty()) {
      throw new NoValidRoomsException(
          "Cannot place suspects: No rooms loaded for session " + sessionId);
    }
    if (caseFile.getSuspects() == null || caseFile.getSuspects().isEmpty()) {
      System.out.println("Session [" + sessionId + "]: No suspects in case file.");
      return true;
    }

    Set<String> suspectNamesLower = new HashSet<>();

    for (CaseFile.SuspectData suspectData : caseFile.getSuspects()) {
      if (suspectData.getName() == null || suspectData.getName().isBlank()) {
        System.err.println("Session [" + sessionId + "]: Suspect with blank name. Skipping.");
        hasErrors = true;
        continue;
      }
      String suspectName = suspectData.getName();
      String suspectNameLower = suspectName.toLowerCase();

      // I use a set to ensure that every suspect has a unique name.
      if (!suspectNamesLower.add(suspectNameLower)) {
        System.err.println(
            "Session [" + sessionId + "]: Duplicate suspect '" + suspectName + "'. Skipping.");
        hasErrors = true;
        continue;
      }
      Suspect suspect =
          new Suspect(
              suspectName,
              suspectData.getStatement() != null ? suspectData.getStatement() : "No comment.",
              suspectData.getClue());
      try {
        // I place each suspect in a random starting room.
        Room startingRoom = assignRandomStartingRoom(suspect, context, sessionId);
        suspect.setCurrentRoom(startingRoom);
        context.addSuspect(suspect);
      } catch (NoValidRoomsException e) {
        System.err.println(
            "Session ["
                + sessionId
                + "]: Error assigning room for '"
                + suspect.getName()
                + "': "
                + e.getMessage());
        // If a suspect can't be placed, it's a fatal error for the case loading process.
        return false;
      }
    }
    return !hasErrors;
  }

  private static Room assignRandomStartingRoom(
      Suspect suspect, GameContextServer context, String sessionId) throws NoValidRoomsException {
    Collection<Room> availableRooms = context.getRooms().values();
    if (availableRooms.isEmpty()) {
      throw new NoValidRoomsException(
          "No rooms in session " + sessionId + " for suspect " + suspect.getName());
    }
    List<Room> roomList = new ArrayList<>(availableRooms);
    return roomList.get(new Random().nextInt(roomList.size()));
  }
}
