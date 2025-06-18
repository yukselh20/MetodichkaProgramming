package Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// This represents the in-game journal for a single session. It collects entries
// from player actions and custom notes.
public class Journal {
  private final List<String> entries;

  public Journal() {
    this.entries = new ArrayList<>();
  }

  public boolean addEntry(String entry) {
    Objects.requireNonNull(entry, "Journal entry cannot be null.");
    if (!entry.isBlank() && !entries.contains(entry)) {
      return entries.add(entry);
    }
    return false;
  }

  public List<String> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  public void clear() {
    entries.clear();
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }
}
