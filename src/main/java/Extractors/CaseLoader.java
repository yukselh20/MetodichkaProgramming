package Extractors;

import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CaseLoader {
    public static List<CaseFile> loadCases(String directory) {
        List<CaseFile> cases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        File folder = new File(directory);

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
}
