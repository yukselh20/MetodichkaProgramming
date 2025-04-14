package Commands;

import Core.GameContext;

public abstract class BaseCommand implements Command {
    private boolean requiresCaseStarted;

    public BaseCommand(boolean requiresCaseStarted) {
        this.requiresCaseStarted = requiresCaseStarted;
    }

    @Override
    public final void execute(String[] args, GameContext context) {
        if (requiresCaseStarted && !context.isCaseStarted()) {
            System.out.println("The case has not started yet. Use 'start case' to begin.");
            return;
        }
        if (!requiresCaseStarted && context.isCaseStarted()) {
            System.out.println("The case has already started. Exit the case first to use this command.");
            return;
        }
        executeCommand(args, context);
    }

    protected abstract void executeCommand(String[] args, GameContext context);
}