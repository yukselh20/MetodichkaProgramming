package common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO that carries the information for a single final exam question, sent from the server to the
 * client to be displayed to the players.
 */
public class ExamQuestionInfoDTO implements Serializable {
  private static final long serialVersionUID = 18L;

  private final String questionText;
  private final int questionNumber;
  private final int totalQuestions;

  public ExamQuestionInfoDTO(String questionText, int questionNumber, int totalQuestions) {
    this.questionText = Objects.requireNonNull(questionText, "Question text cannot be null");
    // Constructor validation ensures that the DTO cannot be created with
    // invalid data, such as a negative question number.
    if (questionNumber <= 0)
      throw new IllegalArgumentException("Question number must be positive.");
    if (totalQuestions <= 0)
      throw new IllegalArgumentException("Total questions must be positive.");
    if (questionNumber > totalQuestions)
      throw new IllegalArgumentException("Question number cannot exceed total questions.");

    this.questionNumber = questionNumber;
    this.totalQuestions = totalQuestions;
  }

  public String getQuestionText() {
    return questionText;
  }

  public int getQuestionNumber() {
    return questionNumber;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  @Override
  public String toString() {
    return String.format("Q%d/%d: %s", questionNumber, totalQuestions, questionText);
  }
}
