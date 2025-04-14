package Commands;

import Core.GameContext;
import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AddCaseCommand implements Command {
    private static final String DEFAULT_CASES_DIR = "cases";

    @Override
    public void execute(String[] args, GameContext context) {

        // Check if the program is in the case selection menu
        if (context == null || !context.isInCaseSelectionMenu()) {
            System.out.println("The 'add case' command can only be used in the case selection menu.");
            return;
        }

        // Validate input arguments
        if (args.length < 3 || !args[1].equalsIgnoreCase("case")) {
            System.out.println("Usage: add case [file_path]");
            return;
        }

        String filePath = args[2];

        // Validate and process the case file
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Error: The specified file does not exist or is invalid.");
                return;
            }

            // Parse the JSON file into a CaseFile object
            ObjectMapper mapper = new ObjectMapper();
            CaseFile newCase = mapper.readValue(file, CaseFile.class);

            // Check for duplicate cases
            List<CaseFile> existingCases = loadExistingCases(DEFAULT_CASES_DIR);
            for (CaseFile caseFile : existingCases) {
                if (caseFile.getTitle().equalsIgnoreCase(newCase.getTitle())) {
                    System.out.println("Error: Case with this title already exists.");
                    return;
                }
            }

            // Copy the file to the "cases" folder
            File casesFolder = new File(DEFAULT_CASES_DIR);
            if (!casesFolder.exists()) {
                casesFolder.mkdir();
            }
            File destination = new File(casesFolder, file.getName());
            if (destination.exists()) {
                System.out.println("Error: Case file already exists in the 'cases' folder.");
                return;
            }
            Files.copy(file.toPath(), destination.toPath());

            System.out.println("Case '" + newCase.getTitle() + "' added successfully!");
        } catch (Exception e) {
            System.out.println("Error adding case: " + e.getMessage());
        }
    }

    /**
     * Loads existing cases from the "cases" directory.
     *
     * @param casesDir The directory containing case files.
     * @return A list of existing CaseFile objects.
     */
    private List<CaseFile> loadExistingCases(String casesDir) {
        ObjectMapper mapper = new ObjectMapper();
        File folder = new File(casesDir);
        List<CaseFile> cases = new ArrayList<>();

        if (folder.exists()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        CaseFile caseFile = mapper.readValue(file, CaseFile.class);
                        cases.add(caseFile);
                    } catch (Exception e) {
                        System.out.println("Error loading case: " + file.getName());
                        e.printStackTrace(); // Show detailed error
                    }
                }
            }
        }
        return cases;
    }

    @Override
    public String getDescription() {
        return "Add a new mystery case to the game.";
    }
}