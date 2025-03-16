package Commands;

import Core.GameContext;

import java.util.Scanner;

public class FinalExamCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Final Exam:");

        // Question 1
        System.out.print("1. How did the killer leave the locked room? ");
        String answer1 = scanner.nextLine().trim();
        boolean correct1 = answer1.equalsIgnoreCase("hidden passage");

        // Question 2
        System.out.print("2. Who gets the money if Sir Reginald dies? ");
        String answer2 = scanner.nextLine().trim();
        boolean correct2 = answer2.equalsIgnoreCase("Lady Eleanor");

        // Question 3
        System.out.print("3. Where was the murder weapon hidden? ");
        String answer3 = scanner.nextLine().trim();
        boolean correct3 = answer3.equalsIgnoreCase("hidden compartment");

        // Evaluate results of the first three questions
        int score = 0;
        if (correct1) score++;
        if (correct2) score++;
        if (correct3) score++;

        if (score == 3) {
            System.out.println("Correct! You have solved the case.");
            context.getDetective().promote(); // Promote to Senior Investigator

            // Extra question for promotion (only if the first three are correct)
            System.out.print("Extra Question: Who helped Lady Eleanor commit the crime? ");
            String answer4 = scanner.nextLine().trim();
            boolean correct4 = answer4.equalsIgnoreCase("Dr.JonathanGraves");

            if (correct4) {
                System.out.println("Excellent! You uncovered the accomplice.");
                context.getDetective().promote(); // Further promotion for solving the extra question
            } else {
                System.out.println("Incorrect. The accomplice remains at large.");
            }
        } else {
            System.out.println("Incorrect. Please review your clues and try again.");
            context.getDetective().demote(); // Demote for incorrect answers
        }

        // Display final rank
        System.out.println("Your current rank is: " + context.getDetective().getRank());
    }
}