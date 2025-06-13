package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;

/**
 * A command for a player to make a logical deduction about an object. The core game logic for this
 * action is complex, so this command simply delegates the work to the execution context.
 */
public class DeduceCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 102L;
  private final String targetObjectName;

  public DeduceCommand(String targetObjectName) {
    // This command is only valid after the investigation has started.
    super(true);
    this.targetObjectName =
        Objects.requireNonNull(targetObjectName, "Target object name cannot be null").toLowerCase();
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String playerId = getPlayerId();
    // By delegating to the context, this command remains simple and unaware
    // of the complex state changes involved in making a deduction.
    context.handleDeduceCommand(playerId, targetObjectName);
  }

  @Override
  public String getDescription() {
    return "Use Sherlock Holmes skills to make a deduction: deduce [object_name]. Overuse decreases rank.";
  }
}
