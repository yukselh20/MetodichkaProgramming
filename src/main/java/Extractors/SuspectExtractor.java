package Extractors;

import Core.Building;
import Core.Room;
import Core.Suspect;
import JsonDTO.CaseFile;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SuspectExtractor {
    public static void loadSuspects(CaseFile caseFile, Building building) {
        boolean hasErrors = false; // Flag to track if there are any errors

        // Use a set to track suspect names and detect duplicates
        Set<String> suspectNames = new HashSet<>();

        for (CaseFile.SuspectData suspectData : caseFile.getSuspects()) {
            String suspectName = suspectData.getName();

            // Validate for duplicate suspect names
            if (!suspectNames.add(suspectName)) { // `add()` returns false if the name already exists
                System.out.println(
                        "Duplicate suspect name '" + suspectName + "' found. Skipping this suspect.");
                hasErrors = true; // Mark that there was an error
                continue; // Skip this suspect
            }

            Suspect suspect =
                    new Suspect(suspectData.getName(), suspectData.getStatement(), suspectData.getClue());

            // Assign starting room
            try {
                Room startingRoom = assignStartingRoom(suspect, building);
                suspect.setCurrentRoom(startingRoom);
                building.addSuspect(suspect);
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
                hasErrors = true; // Mark that there was an error
            }
        }

        // If there were errors, throw an exception to indicate failure
        if (hasErrors) {
            throw new IllegalStateException(
                    "Errors encountered while loading suspects. Exiting to case selection menu.");
        }
    }

    private static Room assignStartingRoom(Suspect suspect, Building building) {
        return building.getRooms().values().stream()
                .skip(new Random().nextInt(building.getRooms().size()))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No valid rooms for suspect: " + suspect.getName()));
    }
}
