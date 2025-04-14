package Core;

import java.util.ArrayList;
import java.util.List;

public class Journal {
    private List<String> entries;

    public Journal() {
        entries = new ArrayList<>();
    }

    public void addEntry(String entry) {
        entries.add(entry);
    }

    // Provide data access instead of IO
    public List<String> getEntries() {
        return entries;
    }
}
