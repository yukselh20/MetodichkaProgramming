package Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// This is a simple class to hold the list of objectives for the current case.
public class TaskList {
  private final List<String> tasks;

  public TaskList(List<String> initialTasks) {
    Objects.requireNonNull(initialTasks, "Initial tasks list cannot be null.");
    // I make a defensive copy of the task list to ensure the internal
    // state of this object is not affected by external changes.
    this.tasks = new ArrayList<>(initialTasks);
  }

  public List<String> getTasks() {
    return Collections.unmodifiableList(tasks);
  }

  public boolean isEmpty() {
    return tasks.isEmpty();
  }
}
