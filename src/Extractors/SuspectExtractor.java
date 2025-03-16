package Extractors;

import Core.Building;
import Core.Room;
import Core.Suspect;
import Core.CaseFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SuspectExtractor {
    public static void loadSuspects(CaseFile caseFile, Building building) {
        for (CaseFile.SuspectData suspectData : caseFile.getSuspects()) {
            Suspect suspect = new Suspect(
                    suspectData.getName(),
                    suspectData.getStatement(),
                    suspectData.getClue()
            );
            // Assign starting room
            suspect.setCurrentRoom(assignStartingRoom(suspect, building));
            building.addSuspect(suspect);
        }
    }

    private static Room assignStartingRoom(Suspect suspect, Building building) {
        List<Room> allRooms = new ArrayList<>(building.getRooms().values()); // Include ALL rooms
        if (allRooms.isEmpty()) {
            throw new IllegalStateException("No valid rooms for suspect: " + suspect.getName());
        }
        return allRooms.get(new Random().nextInt(allRooms.size()));
    }
}