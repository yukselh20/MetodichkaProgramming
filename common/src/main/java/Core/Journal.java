package Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the in-game journal for a single session. It collects entries from player actions
 * (like examining or questioning) and custom notes.
 */
public class Journal {
  private final List<String> entries;

  public Journal() {
    this.entries = new ArrayList<>();
  }

  /**
   * Adds a new entry to the journal, but only if it's not blank or a duplicate. This prevents the
   * journal from getting cluttered with empty or repeated information.
   */
  public boolean addEntry(String entry) {
    Objects.requireNonNull(entry, "Journal entry cannot be null.");
    if (!entry.isBlank() && !entries.contains(entry)) {
      return entries.add(entry);
    }
    return false;
  }

  // The returned list is unmodifiable to prevent direct manipulation of the
  // journal's internal state from outside this class.
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
