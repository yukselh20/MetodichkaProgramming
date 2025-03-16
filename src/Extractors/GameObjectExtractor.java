package Extractors;

import Core.CaseFile;
import Core.Building;
import Core.GameObject;
import Core.Room;

public class GameObjectExtractor {
    public static void loadObjects(CaseFile caseFile, Building building) {
        for (CaseFile.RoomData roomData : caseFile.getRooms()) {
            Room room = building.getRoomByName(roomData.getName());
            if (room != null && roomData.getObjects() != null) {
                for (CaseFile.GameObjectData objData : roomData.getObjects()) {
                    GameObject obj = new GameObject(objData.getName(), objData.getDescription(), objData.getExamine()) {
                        @Override
                        public String deduce() {
                            return objData.getDeduce();
                        }
                    };
                    room.addObject(obj.getName(), obj); // Now works
                }
            }
        }
    }
}