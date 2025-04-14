package Extractors;

import Core.Building;
import Core.GameObject;
import Core.Room;
import JsonDTO.CaseFile;

public class GameObjectExtractor {
    public static void loadObjects(CaseFile caseFile, Building building) {
        for (CaseFile.RoomData roomData : caseFile.getRooms()) {
            Room room = building.getRoomByName(roomData.getName());
            if (room != null && roomData.getObjects() != null) {
                for (CaseFile.GameObjectData objData : roomData.getObjects()) {
                    // Create GameObject with all fields
                    GameObject obj =
                            new GameObject(
                                    objData.getName(),
                                    objData.getDescription(),
                                    objData.getExamine(),
                                    objData.getDeduce());
                    room.addObject(obj.getName(), obj);
                }
            }
        }
    }
}
