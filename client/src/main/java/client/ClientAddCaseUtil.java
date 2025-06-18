package client;

import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this utility with static methods for the client to add a case file
// to its local storage. This is done entirely on the client side without server interaction.
public class ClientAddCaseUtil {

  private static final Logger logger = LoggerFactory.getLogger(ClientAddCaseUtil.class);

  private static final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final String LOCAL_CASES_DIR = "cases";

  private ClientAddCaseUtil() {}

  public static boolean addCaseFromFile(String sourceFilePath) {
    if (sourceFilePath == null || sourceFilePath.isBlank()) {
      logger.error("Source file path cannot be empty.");
      return false;
    }

    File sourceFile = new File(sourceFilePath);
    if (!sourceFile.exists() || !sourceFile.isFile()) {
      logger.error("Source file does not exist or is not a file: {}", sourceFilePath);
      return false;
    }
    if (!sourceFile.getName().toLowerCase().endsWith(".json")) {
      logger.error("Source file must be a .json file: {}", sourceFilePath);
      return false;
    }

    try {
      // I read the case file here first to extract its title. I need this
      // to check for duplicates against existing local cases.
      CaseFile newCaseData = mapper.readValue(sourceFile, CaseFile.class);
      if (newCaseData.getTitle() == null || newCaseData.getTitle().isBlank()) {
        logger.error("The provided JSON file is missing a 'title'. Cannot add.");
        return false;
      }

      File localCasesDirFile = new File(LOCAL_CASES_DIR);
      if (!localCasesDirFile.exists()) {
        if (!localCasesDirFile.mkdirs()) {
          logger.error("Could not create local 'cases' directory.");
          return false;
        }
        logger.info("Created local 'cases' directory.");
      }

      List<CaseFile> existingLocalCases = loadExistingLocalCasesInfo(LOCAL_CASES_DIR);
      for (CaseFile existingCase : existingLocalCases) {
        if (existingCase.getTitle() != null
            && existingCase.getTitle().equalsIgnoreCase(newCaseData.getTitle())) {
          logger.warn("A case with the title '{}' already exists locally.", newCaseData.getTitle());
          return false;
        }
      }

      String originalFileName = sourceFile.getName();
      String fileNameWithoutExtension =
          originalFileName.substring(0, originalFileName.lastIndexOf('.'));
      String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
      File destinationFile = new File(localCasesDirFile, originalFileName);

      // If a file with the same name already exists, I'll append a number to create a
      // unique filename. This prevents from accidentally overwriting other case files.
      int counter = 1;
      while (destinationFile.exists()) {
        String newFileName = fileNameWithoutExtension + "_" + counter + extension;
        destinationFile = new File(localCasesDirFile, newFileName);
        counter++;
      }

      Files.copy(
          sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      logger.info(
          "Case '{}' added locally as: {}", newCaseData.getTitle(), destinationFile.getName());
      return true;

    } catch (IOException e) {
      logger.error("Error adding case file '{}'", sourceFilePath, e);
      return false;
    } catch (Exception e) {
      logger.error("Unexpected error processing case file '{}'", sourceFilePath, e);
      return false;
    }
  }

  private static List<CaseFile> loadExistingLocalCasesInfo(String directoryPath) {
    Path dir = Paths.get(directoryPath);
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      return Collections.emptyList();
    }
    List<CaseFile> cases = new ArrayList<>();
    try (Stream<Path> stream = Files.list(dir)) {
      stream
          .filter(
              path -> !Files.isDirectory(path) && path.toString().toLowerCase().endsWith(".json"))
          .forEach(
              filePath -> {
                try {
                  CaseFile caseFile = mapper.readValue(filePath.toFile(), CaseFile.class);
                  if (caseFile.getTitle() != null) {
                    cases.add(caseFile);
                  }
                  // A malformed JSON file shouldn't crash the entire process.
                  // I just log the error and continue.
                } catch (IOException e) {
                  // I'll intentionally ignore files that cannot be parsed.
                  logger.warn("Could not parse file '{}', skipping.", filePath.getFileName(), e);
                }
              });
    } catch (IOException e) {
      logger.error("Could not list files in directory '{}'", directoryPath, e);
    }
    return cases;
  }
}
