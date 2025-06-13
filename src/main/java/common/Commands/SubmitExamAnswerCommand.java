package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;

/**
 * A command representing a player's answer submission for a final exam question. This object
 * encapsulates the answer data to be sent to the server.
 */
public class SubmitExamAnswerCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 118L;

  private final int questionNumber;
  private final String answerText;

  public SubmitExamAnswerCommand(int questionNumber, String answerText) {
    // Submitting an answer requires the case to be started. The context will
    // perform a more specific check to ensure the exam is actually active.
    super(true);
    if (questionNumber <= 0)
      throw new IllegalArgumentException("Question number must be positive.");
    this.questionNumber = questionNumber;
    this.answerText = Objects.requireNonNull(answerText, "Answer text cannot be null");
  }

  public int getQuestionNumber() {
    return questionNumber;
  }

  public String getAnswerText() {
    return answerText;
  }

  @Override
  protected void executeCommand(String[] args, ICommandContext context) {
    String playerId = getPlayerId();
    // The command delegates the complex logic of answer validation, scoring,
    // and advancing the exam state to the execution context.
    context.processExamAnswer(playerId, questionNumber, answerText.trim());
  }

  @Override
  public String getDescription() {
    // This command is not intended to be typed directly by the user; it's
    // constructed by the client in response to an exam question prompt.
    return "Submits an answer to an exam question.";
  }
}
