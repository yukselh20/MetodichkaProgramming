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

    public void printEntries() {
        System.out.println("Journal Contents:");
        for (String entry : entries) {
            System.out.println(" - " + entry);
        }
    }
}