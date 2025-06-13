package server;

import Core.Detective;
import JsonDTO.CaseFile;
import common.dto.ExamQuestionInfoDTO;
import common.dto.ExamResultDTO;
import common.dto.TextMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the state and logic for the final exam portion of a game session. This class is
 * responsible for asking questions, tracking answers, scoring, and determining the outcome of the
 * exam.
 */
public class FinalExamManager {

  private final GameContextServer context;
  private final GameSession parentSession;

  private int currentQuestionIndex = 0;
  private Map<String, Integer> playerExamScores = new HashMap<>();
  private Map<Integer, String> playerSubmittedAnswers = new HashMap<>();

  public FinalExamManager(GameContextServer context, GameSession parentSession) {
    this.context = Objects.requireNonNull(context);
    this.parentSession = Objects.requireNonNull(parentSession);
  }

  public boolean isExamActive() {
    return parentSession.getCurrentState() == GameSession.SessionState.FINAL_EXAM;
  }

  /**
   * Processes a player's attempt to initiate the final exam, checking permissions and game state
   * before starting the process.
   */
  public void processFinalExamAttempt(String initiatingPlayerId) {
    if (!context.isCaseStarted(initiatingPlayerId)) {
      context.sendResponseToPlayer(
          initiatingPlayerId, new TextMessage("The case has not started yet."));
      return;
    }
    if (isExamActive()) {
      context.sendResponseToPlayer(
          initiatingPlayerId, new TextMessage("The final exam is already in progress."));
      return;
    }
    // The exam can only be started from an active game, not from a lobby or ended state.
    if (parentSession.getCurrentState() != GameSession.SessionState.ACTIVE) {
      context.sendResponseToPlayer(
          initiatingPlayerId,
          new TextMessage("Cannot start final exam. Game not in active play state."));
      return;
    }

    // Only the host can start the exam directly. A guest's attempt is
    // forwarded as a request to the host.
    if (parentSession.isHost(initiatingPlayerId)) {
      System.out.println("HOST " + initiatingPlayerId + " is initiating Final Exam.");
      startExamProcess(initiatingPlayerId);
    } else {
      String hostId = parentSession.getHostPlayerId();
      if (hostId != null) {
        context.sendResponseToPlayer(
            hostId,
            new TextMessage(
                initiatingPlayerId + " requests you to start the final exam. Type 'final exam'."));
        context.sendResponseToPlayer(
            initiatingPlayerId,
            new TextMessage("Request sent to host (" + hostId + ") to initiate final exam."));
      } else {
        context.sendResponseToPlayer(
            initiatingPlayerId,
            new TextMessage("Error: Could not send exam start request (host not found)."));
      }
    }
  }

  // Resets all exam state and begins the questioning process.
  private void startExamProcess(String hostPlayerId) {
    CaseFile selectedCase = context.getSelectedCase();
    if (selectedCase == null
        || selectedCase.getFinalExam() == null
        || selectedCase.getFinalExam().isEmpty()) {
      context.sendResponseToPlayer(
          hostPlayerId, new TextMessage("No final exam questions available for this case."));
      return;
    }

    parentSession.setSessionState(GameSession.SessionState.FINAL_EXAM);
    currentQuestionIndex = 0;
    playerExamScores.clear();
    playerSubmittedAnswers.clear();

    parentSession.broadcastToSession(
        new TextMessage("--- Final Exam Initiated by " + hostPlayerId + " ---"), null);
    sendNextExamQuestionToSession(hostPlayerId);
  }

  // Sends the next question to all players or ends the exam if all questions have been asked.
  private void sendNextExamQuestionToSession(String hostPlayerId) {
    CaseFile selectedCase = context.getSelectedCase();
    if (selectedCase == null || selectedCase.getFinalExam() == null) return;
    List<CaseFile.ExamQuestion> examQuestions = selectedCase.getFinalExam();

    if (currentQuestionIndex < examQuestions.size()) {
      CaseFile.ExamQuestion currentQ = examQuestions.get(currentQuestionIndex);
      ExamQuestionInfoDTO qInfo =
          new ExamQuestionInfoDTO(
              currentQ.getQuestion(), currentQuestionIndex + 1, examQuestions.size());
      parentSession.broadcastToSession(qInfo, null);

      // Specific prompts are sent to the host (to answer) and guests (to wait).
      parentSession.sendMessageToPlayer(
          hostPlayerId,
          new TextMessage("Host, please submit your answer for Q" + (currentQuestionIndex + 1)));
      for (String playerId : parentSession.getPlayerIds()) {
        if (!playerId.equals(hostPlayerId)) {
          parentSession.sendMessageToPlayer(
              playerId,
              new TextMessage(
                  hostPlayerId
                      + " is answering question "
                      + (currentQuestionIndex + 1)
                      + "/"
                      + examQuestions.size()
                      + "."));
        }
      }
    } else {
      evaluateAndBroadcastExamResults(hostPlayerId);
    }
  }

  // Validates and records an answer submitted by the host.
  public void processExamAnswer(String hostPlayerId, int questionNumber, String answerText) {
    if (!isExamActive()) {
      context.sendResponseToPlayer(
          hostPlayerId, new TextMessage("The final exam is not currently active."));
      return;
    }
    if (!parentSession.isHost(hostPlayerId)) {
      context.sendResponseToPlayer(
          hostPlayerId, new TextMessage("Only the host can submit exam answers."));
      return;
    }
    // Ensures answers are submitted in the correct order.
    if (questionNumber != (currentQuestionIndex + 1)) {
      context.sendResponseToPlayer(
          hostPlayerId,
          new TextMessage("Please answer the current question: Q" + (currentQuestionIndex + 1)));
      return;
    }

    CaseFile.ExamQuestion currentQ =
        context.getSelectedCase().getFinalExam().get(currentQuestionIndex);
    boolean isCorrect = currentQ.getAnswer().equalsIgnoreCase(answerText.trim());

    playerSubmittedAnswers.put(questionNumber, answerText.trim());
    playerExamScores.putIfAbsent(hostPlayerId, 0);
    if (isCorrect) {
      playerExamScores.compute(hostPlayerId, (k, v) -> v + 1);
    }

    context.sendResponseToPlayer(
        hostPlayerId, new TextMessage("Answer for Q" + questionNumber + " has been recorded."));
    currentQuestionIndex++;
    sendNextExamQuestionToSession(hostPlayerId);
  }

  // Calculates the final score, determines the outcome, and broadcasts the results.
  private void evaluateAndBroadcastExamResults(String hostPlayerId) {
    int score = playerExamScores.getOrDefault(hostPlayerId, 0);
    int totalQuestions = context.getSelectedCase().getFinalExam().size();
    boolean isVictory = (totalQuestions > 0 && score == totalQuestions);

    Detective hostDetective = context.getPlayerDetective(hostPlayerId);
    if (hostDetective != null) {
      hostDetective.setCaseFinalExamScore(score);
    }
    String rank = (hostDetective != null) ? hostDetective.getRank() : "Junior Investigator";

    List<String> incorrectDetails = new ArrayList<>();
    String feedbackMessage;
    if (isVictory) {
      feedbackMessage =
          "Flawless victory! You have pieced together the entire puzzle and solved the case.";
    } else {
      feedbackMessage = "The mystery remains largely unsolved. Further investigation was needed.";
      // If the player failed, this loop gathers details on the incorrect answers for their review.
      for (int i = 0; i < totalQuestions; i++) {
        CaseFile.ExamQuestion question = context.getSelectedCase().getFinalExam().get(i);
        String submittedAnswer = playerSubmittedAnswers.getOrDefault(i + 1, "No answer given");
        if (!question.getAnswer().equalsIgnoreCase(submittedAnswer)) {
          incorrectDetails.add(
              String.format(
                  "Q%d: %s\n  Your Answer: '%s'",
                  (i + 1), question.getQuestion(), submittedAnswer));
        }
      }
    }

    ExamResultDTO resultDTO =
        new ExamResultDTO(score, totalQuestions, rank, feedbackMessage, incorrectDetails);
    parentSession.broadcastToSession(resultDTO, null);

    // On victory, the game is marked as completed. On failure, the exam state
    // is reset, and the game returns to the active investigation phase.
    if (isVictory) {
      parentSession.setGameCompleted(true);
    } else {
      currentQuestionIndex = 0;
      playerExamScores.clear();
      playerSubmittedAnswers.clear();
      parentSession.setSessionState(GameSession.SessionState.ACTIVE);
    }
  }
}
