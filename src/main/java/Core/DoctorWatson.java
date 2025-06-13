package Core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the NPC Dr. Watson, a specialized MovableCharacter who can provide players with hints
 * from a predefined list.
 */
public class DoctorWatson extends MovableCharacter {

  private final List<String> allHints;
  private List<String> remainingHintsInCycle;

  public DoctorWatson(List<String> hints) {
    Objects.requireNonNull(hints, "Hints list cannot be null for Dr. Watson.");
    // Defensive copies are created to prevent external modification of the hint lists.
    this.allHints = new ArrayList<>(hints);
    this.remainingHintsInCycle = new ArrayList<>(hints);
  }

  /**
   * Provides a hint to the player from the list of available hints. When all hints have been given,
   * the list is reset for the next cycle.
   */
  public String provideHint() {
    if (allHints.isEmpty()) {
      return "I have no specific insights at this moment.";
    }
    // If the current cycle of hints is exhausted, it's refilled from the master list.
    if (remainingHintsInCycle.isEmpty()) {
      remainingHintsInCycle.addAll(allHints);
    }
    // This second check handles the edge case where the master list was empty to begin with.
    if (remainingHintsInCycle.isEmpty()) {
      return "I seem to be out of thoughts for now.";
    }
    // A random hint is selected and removed from the current cycle.
    return remainingHintsInCycle.remove(random.nextInt(remainingHintsInCycle.size()));
  }
}
