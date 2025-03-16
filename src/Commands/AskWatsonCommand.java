package Commands;

import Core.GameContext;

public class AskWatsonCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        if (context.getWatson().getCurrentRoom().getName().equals(
                context.getBuilding().getCurrentRoom().getName())) {
            context.getWatson().provideHint();
        } else {
            System.out.println("Dr. Watson is not in this room.");
        }
    }
}