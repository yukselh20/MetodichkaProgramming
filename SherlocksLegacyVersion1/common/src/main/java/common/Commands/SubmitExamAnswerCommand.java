package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;

// This command represents a player's answer submission for a final exam question.
// used to encapsulate the answer data to be sent to the server.
public class SubmitExamAnswerCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 118L;

  private final int questionNumber;
  private final String answerText;

  public SubmitExamAnswerCommand(int questionNumber, String answerText) {
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
    context.processExamAnswer(playerId, questionNumber, answerText.trim());
  }

  @Override
  public String getDescription() {
    // I don't intend for this command to be typed by the user;
    return "Submits an answer to an exam question.";
  }
}
