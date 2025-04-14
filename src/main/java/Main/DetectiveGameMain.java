package Main;

import Commands.AddCaseCommand;
import Commands.Command;
import Commands.CommandFactory;
import Commands.CommandParser;
import Core.*;
import Extractors.*;
import JsonDTO.CaseFile;
import java.util.List;
import java.util.Scanner;

public class DetectiveGameMain {

  // Constants for cases directory configuration
  private static final String CASES_DIR_ENV =
      "CASES_DIR"; // Environment variable for custom cases directory
  private static final String DEFAULT_CASES_DIR = "cases"; // Default directory for cases

  /** Main entry point of the game. */
  public static void main(String[] args) {
    String casesDir = System.getenv().getOrDefault(CASES_DIR_ENV, DEFAULT_CASES_DIR);
    if (args.length > 0) {
      casesDir = args[0]; // Allow overriding cases directory via command-line arguments
    }

    try (Scanner scanner = new Scanner(System.in)) {
      boolean exitApplication = false; // Controls the outer loop for quitting the application

      while (!exitApplication) {
        // Load available cases and display the case selection menu
        List<CaseFile> cases = CaseLoader.loadCases(casesDir);
        displayCaseMenu(cases);

        CaseFile selectedCase = selectCase(scanner, cases, casesDir);

        // Check if the user wants to exit the application
        if (selectedCase == null) {
          exitApplication = true; // Exit the application if 'quit' was called
          continue;
        }

        exitApplication = startGame(scanner, selectedCase, casesDir);
      }
    }
  }

  /** Displays the case selection menu with available cases. */
  private static void displayCaseMenu(List<CaseFile> cases) {
    // Print the case selection menu border
    System.out.println(
        "╔══════════════════════════════════════════════════════════════════════════════════════════════╗");
    System.out.println(
        "║                                     SELECT A CASE TO INVESTIGATE                             ║");
    System.out.println(
        "╠══════════════════════════════════════════════════════════════════════════════════════════════╣");

    if (cases.isEmpty()) {
      // Display message if no cases are available
      System.out.println(
          "║ No cases available. Please add cases to the cases folder.                                     ║");
    } else {
      // List all available cases
      for (int i = 0; i < cases.size(); i++) {
        System.out.printf("║ %d. %-89s ║%n", i + 1, cases.get(i).getTitle());
      }
    }

    // Print the bottom border of the menu
    System.out.println(
        "╚══════════════════════════════════════════════════════════════════════════════════════════════╝");
  }

  /** Handles case selection logic, including adding new cases or quitting the game. */
  private static CaseFile selectCase(Scanner scanner, List<CaseFile> cases, String casesDir) {
    while (true) {
      System.out.print("Enter case number (0 to add case, 'quit' to exit game): ");
      String input = scanner.nextLine().trim();

      try {
        if (input.equalsIgnoreCase("quit")) {
          System.out.println("Exiting the game. Goodbye!");
          return null; // Signal to exit the main loop
        }

        if (input.equalsIgnoreCase("add case") || input.startsWith("add case ")) {
          // Handle adding a new case
          handleAddCase(scanner, input);
          cases = CaseLoader.loadCases(casesDir); // Reload cases after adding a new one
          displayCaseMenu(cases); // Redisplay the updated case menu
          continue; // Stay in the loop to allow selecting a case
        }

        int choice = Integer.parseInt(input);
        if (choice == 0) {
          // Handle adding a new case when '0' is entered
          handleAddCase(scanner, "add case");
          cases = CaseLoader.loadCases(casesDir); // Reload cases after adding a new one
          displayCaseMenu(cases); // Redisplay the updated case menu
          continue; // Stay in the loop to allow selecting a case
        } else if (choice > 0 && choice <= cases.size()) {
          return cases.get(choice - 1); // Return the selected case
        } else {
          System.out.println("Invalid choice. Please select a valid case number.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number, 'add case', or 'quit'.");
      }
    }
  }

  /** Handles the addition of a new case file using the AddCaseCommand utility. */
  private static void handleAddCase(Scanner scanner, String input) {
    if (input.equalsIgnoreCase("add case")) {
      System.out.print("Enter the file path: ");
      String filePath = scanner.nextLine().trim();
      AddCaseCommand.addCaseFromFile(filePath); // Call with one argument
    } else if (input.startsWith("add case ")) {
      // Validate input length before extracting the file path
      if (input.length() <= "add case ".length()) {
        System.out.println("Error: No file path provided. Please specify a file path.");
        return;
      }
      String filePath = input.substring("add case ".length()).trim();
      AddCaseCommand.addCaseFromFile(filePath); // Call with one argument
    } else {
      System.out.println("Invalid input. Please type 'add case' or 'add case [file_path]'.");
    }
  }

  /** Starts the game for the selected case. */
  private static boolean startGame(Scanner scanner, CaseFile caseFile, String casesDir) {
    // Initialize GameContext for the case
    GameContext context =
        new GameContext(
            new Detective("Sherlock Holmes"),
            new DoctorWatson(caseFile.getWatsonHints()),
            new Journal(),
            new TaskList(caseFile.getTasks()),
            caseFile);

    // Load the building structure (rooms and neighbors) into the GameContext
    BuildingExtractor.loadBuilding(caseFile, context);

    try {
      // Load suspects into the GameContext
      SuspectExtractor.loadSuspects(caseFile, context);
    } catch (IllegalStateException e) {
      System.out.println(e.getMessage());
      return false;
    }

    // Load game objects into the GameContext
    GameObjectExtractor.loadObjects(caseFile, context);

    // Set Watson's starting room and register him with the GameContext
    DoctorWatson watson = context.getWatson();
    watson.setCurrentRoom(context.getCurrentRoom());
    context.setWatson(watson);

    // Display the case invitation letter
    Letter letter = new Letter();
    LetterExtractor.loadLetter(caseFile, letter);
    letter.displayInvitation();
    System.out.println("\nNow type 'start case' to begin the investigation.");

    // Main command loop
    while (!context.isExitCurrentGame()) {
      System.out.print("<CaseFile>");
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) continue;

      Command command = CommandFactory.getCommand(CommandParser.parseCommand(input));
      if (command != null) {
        command.execute(input.split(" "), context);
      } else {
        System.out.println("Unknown command. Type 'help' for a list of commands.");
      }
    }
    return false;
  }
}
