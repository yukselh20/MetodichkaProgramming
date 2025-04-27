package Commands;

import Core.GameContext;
import Core.Letter;
import Core.Room;
import Extractors.LetterExtractor;
import JsonDTO.CaseFile;
import java.util.List;

public class StartCaseCommand extends BaseCommand {
  public StartCaseCommand() {
    super(false); // Does NOT require the case to be started
  }

  @Override
  protected void executeCommand(String[] args, GameContext context) {
    // Retrieve the selected case
    CaseFile caseFile = context.getSelectedCase();
    if (caseFile == null) {
      System.out.println("No case selected. Please choose a case first.");
      return;
    }

    // Display case description
    Letter letter = new Letter();
    LetterExtractor.loadLetter(caseFile, letter);
    System.out.println("\n--- Case Description ---");
    letter.displayCaseDescription();

    // Display tasks using TaskList data instead of direct I/O
    System.out.println("\n--- Case Tasks ---");
    List<String> tasks = context.getTaskList().getTasks(); // Use TaskList from GameContext
    if (tasks.isEmpty()) {
      System.out.println("No tasks available for this case.");
    } else {
      for (int i = 0; i < tasks.size(); i++) {
        System.out.printf("%d. %s%n", i + 1, tasks.get(i));
      }
    }

    // Display starting room details
    Room currentRoom =
        context.getDetective().getCurrentRoom(); // Directly use getCurrentRoom from GameContext
    System.out.println("\nYou are now at the starting location: " + currentRoom.getName());
    System.out.println(currentRoom.getDescription());
    displayExits(currentRoom);

    // Display occupants in the current room
    System.out.println(
        context.getOccupantsDescription()); // Use getOccupantsDescription from GameContext

    System.out.println("\nType 'help' to see commands.");

    // Set the flag to indicate the case has started
    context.setCaseStarted(true);
  }

  private void displayExits(Room room) {
    System.out.println(room.getExitsDescription());
  }

  @Override
  public String getDescription() {
    return "Show the case description.";
  }
}
