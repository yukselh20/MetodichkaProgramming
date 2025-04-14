package Commands;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandFactory {
    private static final Map<String, Command> commandMap = new LinkedHashMap<>();

    static {
        // Initialize all commands in the desired order
        commandMap.put("start case", new StartCaseCommand());
        commandMap.put("look", new LookCommand());
        commandMap.put("move", new MoveCommand());
        commandMap.put("examine", new ExamineCommand());
        commandMap.put("question", new QuestionCommand());
        commandMap.put("journal", new JournalCommand());
        commandMap.put("journal add", new JournalAddCommand());
        commandMap.put("deduce", new DeduceCommand());
        commandMap.put("final exam", new FinalExamCommand());
        commandMap.put("ask watson", new AskWatsonCommand());
        commandMap.put("help", new HelpCommand());
        commandMap.put("tasks", new TaskCommand());
        commandMap.put("exit", new ExitCommand());
    }

    public static Command getCommand(String commandName) {
        return commandMap.get(commandName.toLowerCase());
    }

    public static Map<String, Command> getCommands() {
        return commandMap; // Return the ordered map
    }
}