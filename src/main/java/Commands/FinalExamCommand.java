package Commands;

import Core.GameContext;
import JsonDTO.CaseFile;
import java.util.List;
import java.util.Scanner;

public class FinalExamCommand extends BaseCommand {
  public FinalExamCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {
    // Retrieve the selected case and its final exam questions
    CaseFile caseFile = context.getSelectedCase();
    List<CaseFile.ExamQuestion> examQuestions = caseFile.getFinalExam();

    // Initialize variables
    Scanner scanner = new Scanner(System.in);
    int score = 0;

    System.out.println("Final Exam:");
    for (CaseFile.ExamQuestion question : examQuestions) {
      boolean answered = false;

      while (!answered) {
        // Display the question
        System.out.print(question.getQuestion() + " ");
        String answer = scanner.nextLine().trim();

        // Validate input
        if (answer.isEmpty()) {
          System.out.println("Please provide an answer.");
          continue;
        }

        // Check the answer
        if (answer.equalsIgnoreCase(question.getAnswer())) {
          System.out.println("Correct!");
          score++;
        } else {
          System.out.println("Incorrect");
        }

        // Mark as answered
        answered = true;
      }
    }

    // Evaluate the detective's rank based on the score
    context.getDetective().setFinalExamScore(score);
    context.getDetective().evaluateRank();

    // Provide feedback based on the score
    if (score == examQuestions.size()) {
      System.out.println("Congratulations! You solved the case perfectly.");
    } else if (score > 0) {
      System.out.println("You made some progress, but there are still clues to uncover.");
    } else {
      System.out.println(
          "Unfortunately, you failed to solve the case. Review your clues and try again.");
    }

    // Display the detective's rank
    System.out.println("Your current rank: " + context.getDetective().getRank());
  }

  @Override
  public String getDescription() {
    return "Answer key questions to solve the case.";
  }
}
