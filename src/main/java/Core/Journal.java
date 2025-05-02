package Core;

import java.util.ArrayList;
import java.util.List;

public class Journal {
  private List<String> entries;

  public Journal() {
    entries = new ArrayList<>();
  }

  // Add an entry only if it doesn't already exist
  public void addEntry(String entry) {
    if (!entries.contains(entry)) {
      entries.add(entry);
    }
  }

  // Provide data access instead of IO
  public List<String> getEntries() {
    return entries;
  }

  // Method to print journal entries
  public void printEntries() {
    System.out.println("Journal Contents:");
    for (String entry : entries) {
      System.out.println(" - " + entry);
    }
  }
}
