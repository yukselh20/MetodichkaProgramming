package Commands;

import Core.GameContext;
import Core.TaskList;

public class TaskCommand extends BaseCommand {
    public TaskCommand() {
        super(true); // Requires the case to be started
    }
    @Override
    public void executeCommand(String[] args, GameContext context) {

        TaskList taskList = context.getTaskList();

        if (taskList.isEmpty()) {
            System.out.println("No tasks available for this case.");
        } else {
            System.out.println("Case Tasks:");
            int index = 1;
            for (String task : taskList.getTasks()) {
                System.out.printf("%d. %s%n", index++, task);
            }
        }
    }

    @Override
    public String getDescription() {
        return "View your investigation guide.";
    }
}
