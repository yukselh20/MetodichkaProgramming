package Commands;

import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AddCaseCommand {
  /**
   * Adds a new case from the specified file path.
   *
   * @param filePath The path to the JSON file containing the case data.
   */
  public static void addCaseFromFile(String filePath) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      File file = new File(filePath);
      if (!file.exists()) {
        System.out.println("Error: File does not exist.");
        return;
      }
      CaseFile newCase = mapper.readValue(file, CaseFile.class);

      // Check for duplicates based on case title
      List<CaseFile> existingCases = loadExistingCases();
      for (CaseFile caseFile : existingCases) {
        if (caseFile.getTitle().equalsIgnoreCase(newCase.getTitle())) {
          System.out.println("Error: Case with this title already exists.");
          return;
        }
      }

      // Copy the file to the "cases" folder with a unique name
      File casesFolder = new File("cases");
      if (!casesFolder.exists()) {
        casesFolder.mkdir();
      }

      // Generate a unique file name
      String originalFileName = file.getName();
      String fileNameWithoutExtension =
          originalFileName.substring(0, originalFileName.lastIndexOf('.'));
      String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
      File destination = new File(casesFolder, originalFileName);

      int counter = 1;
      while (destination.exists()) {
        String newFileName = fileNameWithoutExtension + "_" + counter + extension;
        destination = new File(casesFolder, newFileName);
        counter++;
      }

      // Copy the file to the destination
      Files.copy(file.toPath(), destination.toPath());

      System.out.println("Case '" + newCase.getTitle() + "' added successfully!");
      System.out.println("File saved as: " + destination.getName());
    } catch (Exception e) {
      System.out.println("Error adding case: " + e.getMessage());
    }
  }

  /**
   * Loads existing cases from the "cases" directory.
   *
   * @return A list of existing CaseFile objects.
   */
  private static List<CaseFile> loadExistingCases() {
    ObjectMapper mapper = new ObjectMapper();
    File folder = new File("cases");
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
}
