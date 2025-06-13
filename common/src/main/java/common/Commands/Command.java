package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

/**
 * Defines the contract for all executable commands in the game. Being `Serializable` is crucial as
 * it allows command objects to be sent over the network from the client to the server.
 */
public interface Command extends Serializable {

  /**
   * Executes the command's primary logic using the provided context. The context acts as a bridge,
   * giving the command access to the game state and actions without coupling it to specific server
   * classes.
   */
  void execute(String[] args, ICommandContext context);

  /**
   * Returns a user-friendly description of what the command does, primarily used for the 'help'
   * command.
   */
  String getDescription();

  /**
   * Sets the ID of the player who issued the command. This is typically called by the server upon
   * receiving the command object.
   */
  void setPlayerId(String playerId);

  /** Gets the ID of the player who issued the command. */
  String getPlayerId();
}
