package Commands;

import Core.CaseFile;
import Core.GameContext;
import Core.Letter;
import Core.Room;
import Extractors.LetterExtractor;

public class StartCaseCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        CaseFile caseFile = context.getSelectedCase(); // Retrieve the selected case
        if (caseFile == null) {
            System.out.println("No case selected. Please choose a case first.");
            return;
        }

        // Display case description
        Letter letter = new Letter();
        LetterExtractor.loadLetter(caseFile, letter);
        System.out.println("\n--- Case Description ---");
        letter.displayCaseDescription();

        // Display tasks
        System.out.println("\n--- Case Tasks ---");
        context.getTaskList().printTasks();

        // Display starting room details
        Room currentRoom = context.getBuilding().getCurrentRoom();
        System.out.println("\nYou are now at the starting location: " + currentRoom.getName());
        System.out.println(currentRoom.getDescription());

        System.out.println(context.getBuilding().getOccupantsDescription());

        displayExits(currentRoom);

        System.out.println("\nType 'help' to see commands.");
    }

    private void displayExits(Room room) {
        var neighbors = room.getNeighbors();
        if (!neighbors.isEmpty()) {
            System.out.print("Exits: ");
            for (var entry : neighbors.entrySet()) {
                System.out.print(entry.getKey() + " (" + entry.getValue().getName() + ") ");
            }
            System.out.println();
        } else {
            System.out.println("Exits: None");
        }
    }
}