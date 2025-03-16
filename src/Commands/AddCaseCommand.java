package Commands;

import Core.CaseFile;
import Core.GameContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

public class AddCaseCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        if (args.length < 2) {
            System.out.println("Usage: add case [file_path]");
            return;
        }

        String filePath = args[1];
        ObjectMapper mapper = new ObjectMapper();

        try {
            CaseFile caseFile = mapper.readValue(new File(filePath), CaseFile.class);
            context.getBuilding().addCase(caseFile);
            System.out.println("Case '" + caseFile.getTitle() + "' added successfully!");
        } catch (Exception e) {
            System.out.println("Error loading case: " + e.getMessage());
        }
    }
}