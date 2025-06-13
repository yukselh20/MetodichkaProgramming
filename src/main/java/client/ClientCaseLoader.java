package client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.CaseInfoDTO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A utility to load minimal case information from the client's local files. It only reads the title
 * and description, which is all the server needs to present a list of available cases to the player
 * for hosting or joining a game.
 */
public class ClientCaseLoader {

  private static final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // A private constructor prevents instantiation of this utility class.
  private ClientCaseLoader() {}

  // This lightweight inner class is an optimization. It allows us to deserialize
  // only the essential fields (title, description) from a potentially large and
  // complex case JSON file, improving performance and reducing memory usage.
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class PartialCaseFileForInfo {
    private String title;
    private String description;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  public static List<CaseInfoDTO> loadLocalCaseInfo(String directoryPath) {
    Objects.requireNonNull(directoryPath, "Directory path for local cases cannot be null.");
    Path dir = Paths.get(directoryPath);

    if (!Files.exists(dir)) {
      System.out.println(
          "Client Info: Local case directory '"
              + dir.toAbsolutePath()
              + "' does not exist. No local cases will be reported.");
      return Collections.emptyList();
    }
    if (!Files.isDirectory(dir)) {
      System.err.println(
          "Client Error: Local case path '" + dir.toAbsolutePath() + "' is not a directory.");
      return Collections.emptyList();
    }

    List<CaseInfoDTO> caseInfos = new ArrayList<>();

    try (Stream<Path> stream = Files.list(dir)) {
      stream
          .filter(path -> !Files.isDirectory(path))
          .filter(path -> path.toString().toLowerCase().endsWith(".json"))
          .forEach(
              filePath -> {
                File file = filePath.toFile();
                try {
                  // We deserialize into the lightweight `PartialCaseFileForInfo` class
                  // instead of the full `CaseFile` DTO for efficiency.
                  PartialCaseFileForInfo partialCase =
                      mapper.readValue(file, PartialCaseFileForInfo.class);

                  String title = partialCase.getTitle();
                  String description = partialCase.getDescription();

                  if (title != null && !title.isBlank()) {
                    caseInfos.add(new CaseInfoDTO(title, description));
                  } else {
                    System.err.println(
                        "Client Warning: Skipping local file "
                            + file.getName()
                            + " (missing title in JSON).");
                  }
                } catch (IOException e) {
                  System.err.println(
                      "Client Error: Could not read/parse local case file "
                          + file.getName()
                          + " for info: "
                          + e.getMessage());
                }
              });
    } catch (IOException e) {
      System.err.println(
          "Client Error: Could not list files in local case directory "
              + dir.toAbsolutePath()
              + ": "
              + e.getMessage());
      // Returning an empty list is safer than null to prevent NullPointerExceptions.
      return Collections.emptyList();
    }

    return caseInfos;
  }
}
