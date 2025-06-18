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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this utility to load minimal case info from local files. It only reads the title
// and description
public class ClientCaseLoader {

  private static final Logger logger = LoggerFactory.getLogger(ClientCaseLoader.class);

  private static final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private ClientCaseLoader() {}

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
      logger.info(
          "Local case directory '{}' does not exist. No local cases will be reported.",
          dir.toAbsolutePath());
      return Collections.emptyList();
    }
    if (!Files.isDirectory(dir)) {
      logger.error("Local case path '{}' is not a directory.", dir.toAbsolutePath());
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
                  // Deserialize into the lightweight inner class for efficiency.
                  PartialCaseFileForInfo partialCase =
                      mapper.readValue(file, PartialCaseFileForInfo.class);

                  String title = partialCase.getTitle();
                  String description = partialCase.getDescription();

                  if (title != null && !title.isBlank()) {
                    caseInfos.add(new CaseInfoDTO(title, description));
                  } else {
                    logger.warn("Skipping local file {} (missing title in JSON).", file.getName());
                  }
                } catch (IOException e) {
                  logger.error(
                      "Could not read/parse local case file {} for info.", file.getName(), e);
                }
              });
    } catch (IOException e) {
      logger.error("Could not list files in local case directory '{}'", dir.toAbsolutePath(), e);
      return Collections.emptyList();
    }

    logger.info("Loaded {} local case(s) from '{}'.", caseInfos.size(), directoryPath);
    return caseInfos;
  }
}
