package Commands;

import Core.GameContext;

public interface Command {
    void execute(String[] args, GameContext context);
}

