package Core;

public class Detective {
    private String name;
    private String rank;
    private int deduceCount = 0; // Tracks how many times the player has used deduction
    private int finalExamScore = 0; // Tracks the score from the final exam

    public Detective(String name) {
        this.name = name;
        this.rank = "Junior Investigator";
    }

    public void incrementDeduceCount() {
        deduceCount++;
    }

    public int getDeduceCount() {
        return deduceCount;
    }

    public String getName() {
        return name;
    }

    public String getRank() {
        return rank;
    }

    public void setFinalExamScore(int score) {
        this.finalExamScore = score;
    }

    public int getFinalExamScore() {
        return finalExamScore;
    }

    /** Evaluates the player's rank based on deduceCount and finalExamScore. */
    public void evaluateRank() {
        // Define thresholds for rank evaluation
        if (finalExamScore >= 3 && deduceCount <= 5) {
            // High final exam score and minimal deductions indicate a skilled detective
            rank = "Senior Investigator";
        } else if (finalExamScore >= 2 && deduceCount <= 10) {
            // Moderate final exam score and reasonable deductions indicate competence
            rank = "Intermediate Investigator";
        } else {
            // Low final exam score or excessive deductions demote the player
            rank = "Junior Investigator";
        }
    }
}
