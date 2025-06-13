package server.extractors;

import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * A server-side utility class responsible for discovering and loading all valid case files from a
 * specified directory on disk.
 */
public class CaseLoader {

  private static final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private CaseLoader() {}

  public static List<CaseFile> loadCases(String directoryPath) {
    Path dir = Paths.get(directoryPath);
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      System.err.println("CaseLoader: Directory not found or invalid: " + directoryPath);
      return Collections.emptyList();
    }

    List<CaseFile> cases = new ArrayList<>();

    // Using a try-with-resources block ensures the file stream is properly closed.
    try (Stream<Path> stream = Files.list(dir)) {
      stream
          .filter(path -> !Files.isDirectory(path))
          .filter(path -> path.toString().toLowerCase().endsWith(".json"))
          .forEach(
              filePath -> {
                File file = filePath.toFile();
                try {
                  CaseFile caseFile = mapper.readValue(file, CaseFile.class);
                  // A case file is only considered valid if it has both a title and at least one
                  // room.
                  if (caseFile.getTitle() != null
                      && !caseFile.getTitle().isBlank()
                      && caseFile.getRooms() != null
                      && !caseFile.getRooms().isEmpty()) {
                    cases.add(caseFile);
                  } else {
                    System.err.println(
                        "CaseLoader: Invalid case structure (missing title/rooms) in "
                            + file.getName()
                            + ". Skipping.");
                  }
                } catch (IOException e) {
                  System.err.println(
                      "CaseLoader: Error reading/parsing "
                          + file.getName()
                          + ": "
                          + e.getMessage());
                } catch (Exception e) {
                  System.err.println(
                      "CaseLoader: Unexpected error loading "
                          + file.getName()
                          + ": "
                          + e.getMessage());
                  e.printStackTrace();
                }
              });
    } catch (IOException e) {
      System.err.println(
          "CaseLoader: Error listing files in " + directoryPath + ": " + e.getMessage());
      return Collections.emptyList();
    }
    return cases;
  }
}
