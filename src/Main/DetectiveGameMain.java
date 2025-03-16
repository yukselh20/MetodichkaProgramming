package Main;

import Commands.Command;
import Commands.CommandFactory;
import Core.*;
import Extractors.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//for New Branch

public class DetectiveGameMain {
    public static void main(String[] args) {
        List<CaseFile> cases = loadCases();
        displayCaseMenu(cases);
        CaseFile selectedCase = selectCase(cases);
        startGame(selectedCase);
    }

    private static List<CaseFile> loadCases() {
        List<CaseFile> cases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        File folder = new File("cases");

        if (folder.exists()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        CaseFile caseFile = mapper.readValue(file, CaseFile.class);
                        cases.add(caseFile);
                    } catch (Exception e) {
                        System.out.println("Error loading case: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        return cases;
    }

    private static void displayCaseMenu(List<CaseFile> cases) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                     SELECT A CASE TO INVESTIGATE                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════════════════════╣");

        if (cases.isEmpty()) {
            System.out.println("║ No cases available. Please add cases to the 'cases' folder.                                    ║");
        } else {
            for (int i = 0; i < cases.size(); i++) {
                System.out.printf("║ %d. %-85s ║%n", i + 1, cases.get(i).getTitle());
            }
        }

        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    private static CaseFile selectCase(List<CaseFile> cases) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter the number of the case you wish to investigate: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                if (choice > 0 && choice <= cases.size()) {
                    return cases.get(choice - 1);
                }
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid choice. Please select a valid case number.");
        }
    }

    private static void startGame(CaseFile caseFile) {
        // Initialize game components
        Building building = BuildingExtractor.loadBuilding(caseFile);
        SuspectExtractor.loadSuspects(caseFile, building);
        GameObjectExtractor.loadObjects(caseFile, building);

        // Initialize task list
        TaskList taskList = new TaskList(caseFile.getTasks());

        // Initialize characters
        Detective detective = new Detective("Sherlock Holmes");
        List<String> watsonHints = caseFile.getWatsonHints(); // Ensure CaseFile has this getter
        DoctorWatson watson = new DoctorWatson(watsonHints);

        // Initialize game context
        GameContext context = new GameContext(building, detective, watson, new Journal(), taskList, caseFile); // Pass the selected case

        // Ensure Watson's starting room matches the player's starting room
        watson.setCurrentRoom(building.getCurrentRoom());
        building.setWatson(watson);

        // Display case invitation immediately after selecting a case
        Letter letter = new Letter();
        LetterExtractor.loadLetter(caseFile, letter);
        letter.displayInvitation();
        System.out.println("\nNow type 'start case' to begin the investigation.");

        // Command loop
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("C:\\DetectiveGame> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] tokens = input.split(" ");
            String commandName = tokens[0].toLowerCase();

            // Handle multi-word commands explicitly
            if (input.toLowerCase().startsWith("final exam")) commandName = "final exam";
            if (input.toLowerCase().startsWith("journal add")) commandName = "journal add";
            if (input.toLowerCase().startsWith("ask watson")) commandName = "ask watson";
            if (input.toLowerCase().startsWith("start case")) commandName = "start case";

            Command command = CommandFactory.getCommand(commandName);
            if (command != null) {
                command.execute(tokens, context);

                // Update suspect/Watson positions after movement commands
                if (commandName.equals("move") || commandName.equals("enter")) {
                    building.updateMovements(watson);
                }
            } else {
                System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        }
    }
}