package Core;

import java.util.*;

public class DoctorWatson extends MovableCharacter {
    private List<String> hints;
    private List<String> remainingHints;

    public DoctorWatson(List<String> hints) {
        this.hints = new ArrayList<>(hints);
        this.remainingHints = new ArrayList<>(hints);
    }

    public void provideHint() {
        if (remainingHints.isEmpty()) {
            // Reset the remaining hints if all have been used
            remainingHints.addAll(hints);
        }

        // Randomly select a hint from the remaining list
        int index = random.nextInt(remainingHints.size());
        String hint = remainingHints.remove(index); // Remove the hint after providing it

        System.out.println("Watson: " + hint);
    }
}
