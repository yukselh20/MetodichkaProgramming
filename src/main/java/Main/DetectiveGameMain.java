package Main;

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
    private static final String CASES_DIR_ENV = "CASES_DIR"; // Environment variable for custom cases directory
    private static final String DEFAULT_CASES_DIR = "cases"; // Default directory for cases

    /**
     * Main entry point of the game.
     */
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


                GameContext context = new GameContext(null, null, null, null, null, null);

                CaseFile selectedCase = selectCase(scanner, cases, casesDir, context);

                // Check if the user wants to exit the application
                if (selectedCase == null) {
                    exitApplication = true; // Exit the application if 'quit' was called
                    continue;
                }

                exitApplication = startGame(scanner, selectedCase, casesDir);
            }
        }
    }

    /**
     * Displays the case selection menu with available cases.
     */
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

    /**
     * Handles case selection logic, including adding new cases or quitting the game.
     */
    private static CaseFile selectCase(Scanner scanner, List<CaseFile> cases, String casesDir, GameContext context) {
        while (true) {
            context.setInCaseSelectionMenu(true); // Indicate that we are in the case selection menu
            System.out.print("Enter case number (0 to add case, 'quit' to exit game): ");
            String input = scanner.nextLine().trim();

            try {
                if (input.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting the game. Goodbye!");
                    return null; // Signal to exit the main loop
                }

                if (input.equalsIgnoreCase("add case") || input.startsWith("add case ")) {
                    // Handle adding a new case
                    handleAddCase(scanner, input, casesDir, context);
                    cases = CaseLoader.loadCases(casesDir); // Reload cases after adding a new one
                    displayCaseMenu(cases); // Redisplay the updated case menu
                    continue; // Stay in the loop to allow selecting a case
                }

                int choice = Integer.parseInt(input);
                if (choice == 0) {
                    // Handle adding a new case when '0' is entered
                    handleAddCase(scanner, "add case", casesDir, context);
                    cases = CaseLoader.loadCases(casesDir); // Reload cases after adding a new one
                    displayCaseMenu(cases); // Redisplay the updated case menu
                    continue; // Stay in the loop to allow selecting a case
                } else if (choice > 0 && choice <= cases.size()) {
                    context.setInCaseSelectionMenu(false); // Reset the flag before returning
                    return cases.get(choice - 1); // Return the selected case
                } else {
                    System.out.println("Invalid choice. Please select a valid case number.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number, 'add case', or 'quit'.");
            }
        }
    }

    /**
     * Handles the addition of a new case file.
     */
    private static void handleAddCase(Scanner scanner, String input, String casesDir, GameContext context) {
        Command addCaseCommand = CommandFactory.getCommand("add");
        if (addCaseCommand == null) {
            System.out.println("Error: Add case command not found.");
            return;
        }

        if (input.equalsIgnoreCase("add case")) {
            System.out.print("Enter the file path: ");
            String filePath = scanner.nextLine().trim();
            addCaseCommand.execute(new String[]{"add", "case", filePath}, context); // Pass the context
        } else if (input.startsWith("add case ")) {
            // Validate input length before extracting the file path
            if (input.length() <= "add case ".length()) {
                System.out.println("Error: No file path provided. Please specify a file path.");
                return;
            }
            String filePath = input.substring("add case ".length()).trim();
            addCaseCommand.execute(new String[]{"add", "case", filePath}, context); // Pass the context
        } else {
            System.out.println("Invalid input. Please type 'add case' or 'add case [file_path]'.");
        }
    }

    /**
     * Starts the game for the selected case.
     */
    private static boolean startGame(Scanner scanner, CaseFile caseFile, String casesDir) {
        // Load the building structure for the case
        Building building = BuildingExtractor.loadBuilding(caseFile);
        if (building == null) {
            return false;
        }

        try {
            SuspectExtractor.loadSuspects(caseFile, building);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return false;
        }
        GameObjectExtractor.loadObjects(caseFile, building); // Load game objects into the building

        // Initialize game components
        TaskList taskList = new TaskList(caseFile.getTasks());
        Detective detective = new Detective("Sherlock Holmes");
        DoctorWatson watson = new DoctorWatson(caseFile.getWatsonHints());
        GameContext context = new GameContext(building, detective, watson, new Journal(), taskList, caseFile);

        // Set Watson's starting room and register him with the building
        watson.setCurrentRoom(building.getCurrentRoom());
        building.setWatson(watson);

        // Display the case invitation letter
        Letter letter = new Letter();
        LetterExtractor.loadLetter(caseFile, letter);
        letter.displayInvitation();
        System.out.println("\nNow type 'start case' to begin the investigation.");


        while (!context.isExitCurrentGame()) {
            System.out.print("<CaseFile>");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String commandName = CommandParser.parseCommand(input);

            Command command = CommandFactory.getCommand(commandName);
            if (command != null) {

                command.execute(input.split(" "), context);
            } else {
                System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        }
        return false;
    }
}