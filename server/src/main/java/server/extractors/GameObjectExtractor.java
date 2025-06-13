package server.extractors;

import Core.GameObject;
import Core.Room;
import JsonDTO.CaseFile;
import java.util.Objects;
import server.GameContextServer;
import server.GameSession;

/**
 * A utility class for parsing game object data from a CaseFile and populating the corresponding
 * Room objects within a GameContext.
 */
public class GameObjectExtractor {

  private GameObjectExtractor() {}

  public static void loadObjects(CaseFile caseFile, GameContextServer context) {
    Objects.requireNonNull(caseFile, "CaseFile cannot be null.");
    Objects.requireNonNull(context, "GameContextServer cannot be null.");

    GameSession parent = context.getParentSession();
    String sessionId = (parent != null) ? parent.getSessionId() : "[NoSession]";

    if (caseFile.getRooms() == null) return;

    // This process iterates through each room defined in the case file, finds
    // the already-created Room object, and adds its game objects to it.
    for (CaseFile.RoomData roomData : caseFile.getRooms()) {
      if (roomData.getName() == null) continue;
      Room room = context.getRoomByName(roomData.getName());

      if (room != null && roomData.getObjects() != null) {
        for (CaseFile.GameObjectData objData : roomData.getObjects()) {
          if (objData.getName() == null || objData.getName().isBlank()) {
            System.err.println(
                "Session ["
                    + sessionId
                    + "]: Object with blank name in room '"
                    + room.getName()
                    + "'. Skipping.");
            continue;
          }
          GameObject obj =
              new GameObject(
                  objData.getName(),
                  objData.getDescription() != null
                      ? objData.getDescription()
                      : "A " + objData.getName() + ".",
                  objData.getExamine() != null ? objData.getExamine() : objData.getDescription(),
                  objData.getDeduce() != null ? objData.getDeduce() : "Nothing further to deduce.");
          room.addObject(obj.getName(), obj);
        }
      } else if (room == null) {
        System.err.println(
            "Session ["
                + sessionId
                + "]: Room '"
                + roomData.getName()
                + "' not found in context while loading objects.");
      }
    }
  }
}
