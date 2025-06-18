package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;

// This command lets a player make a logical deduction about an object. The core game logic
// is complex, so I simply delegate the work to the execution context.
public class DeduceCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 102L;
  private final String targetObjectName;

  public DeduceCommand(String targetObjectName) {
    super(true);
    this.targetObjectName =
        Objects.requireNonNull(targetObjectName, "Target object name cannot be null").toLowerCase();
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String playerId = getPlayerId();
    context.handleDeduceCommand(playerId, targetObjectName);
  }

  @Override
  public String getDescription() {
    return "Use Sherlock Holmes skills to make a deduction: deduce [object_name]. Overuse decreases rank.";
  }
}
