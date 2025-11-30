package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

// This defines the contract for all executable commands in the game.
public interface Command extends Serializable {

  // Executes the command's primary logic. The context acts as a bridge, giving me access
  // to the game state and actions without coupling to specific server classes.
  void execute(String[] args, ICommandContext context);

  // Returns a user-friendly description of what the command does.
  String getDescription();

  void setPlayerId(String playerId);

  String getPlayerId();
}
