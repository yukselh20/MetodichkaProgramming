package Core;

import java.util.List;

public class TaskList {
    private List<String> tasks;

    public TaskList(List<String> tasks) {
        this.tasks = tasks;
    }

    public void printTasks() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks available for this case.");
        } else {
            System.out.println("Case Tasks:");
            for (int i = 0; i < tasks.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, tasks.get(i));
            }
        }
    }
}