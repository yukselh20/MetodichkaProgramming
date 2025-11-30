package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// I send this DTO from the server to clients to communicate the final exam results.
public class ExamResultDTO implements Serializable {
  private static final long serialVersionUID = 19L;

  private final int score;
  private final int totalQuestions;
  private final String rankAchieved;
  private final String feedbackMessage;
  private final List<String> incorrectAnswerDetails;

  public ExamResultDTO(
      int score,
      int totalQuestions,
      String rankAchieved,
      String feedbackMessage,
      List<String> incorrectAnswerDetails) {
    if (score < 0 || (totalQuestions > 0 && score > totalQuestions)) {
      throw new IllegalArgumentException("Score out of bounds.");
    }
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.rankAchieved = Objects.requireNonNull(rankAchieved, "Rank achieved cannot be null");
    this.feedbackMessage =
        Objects.requireNonNull(feedbackMessage, "Feedback message cannot be null");
    this.incorrectAnswerDetails =
        (incorrectAnswerDetails != null)
            ? Collections.unmodifiableList(new ArrayList<>(incorrectAnswerDetails))
            : Collections.emptyList();
  }

  public int getScore() {
    return score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public String getRankAchieved() {
    return rankAchieved;
  }

  public String getFeedbackMessage() {
    return feedbackMessage;
  }

  public List<String> getIncorrectAnswerDetails() {
    return incorrectAnswerDetails;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n--- FINAL EXAM RESULT ---\n");
    sb.append(feedbackMessage).append("\n");
    sb.append("Score: ").append(score).append("/").append(totalQuestions).append("\n");
    sb.append("Final Rank: ").append(rankAchieved).append("\n");

    if (!incorrectAnswerDetails.isEmpty()) {
      sb.append("\n--- Review of Incorrect Questions ---\n");
      for (String detail : incorrectAnswerDetails) {
        sb.append(detail).append("\n");
      }
    }
    sb.append("\n--- Final Exam Concluded ---");
    return sb.toString();
  }
}
