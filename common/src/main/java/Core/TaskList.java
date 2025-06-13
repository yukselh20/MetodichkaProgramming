package Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A simple class to hold the list of objectives for the current case. */
public class TaskList {
  private final List<String> tasks;

  public TaskList(List<String> initialTasks) {
    Objects.requireNonNull(initialTasks, "Initial tasks list cannot be null.");
    // A defensive copy of the task list is made to ensure the internal
    // state of this object is not affected by external changes.
    this.tasks = new ArrayList<>(initialTasks);
  }

  public List<String> getTasks() {
    // The returned list is made unmodifiable to prevent direct changes from
    // outside this class, enforcing controlled state management.
    return Collections.unmodifiableList(tasks);
  }

  public boolean isEmpty() {
    return tasks.isEmpty();
  }
}
