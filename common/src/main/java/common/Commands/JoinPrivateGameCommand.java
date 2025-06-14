package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command representing a client's request to join a private game session using a specific code.
 */
public class JoinPrivateGameCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(JoinPrivateGameCommand.class);
  private static final long serialVersionUID = 117L;
  private final String privateCode;

  public JoinPrivateGameCommand(String privateCode) {
    // This command is used in the lobby, so it does not require a case to be started.
    super(false);
    this.privateCode = Objects.requireNonNull(privateCode, "Private code cannot be null");
    if (privateCode.isBlank()) throw new IllegalArgumentException("Private code cannot be blank.");
  }

  public String getPrivateCode() {
    return privateCode;
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    // This command should be intercepted by the GameSessionManager. If it reaches
    // a GameContext, it means the player is already in a session, which is an
    // invalid state for this action. This block handles that error case.
    logger.warn(
        "JoinPrivateGameCommand reached GameContextServer for player {}. This indicates a message routing error and should be handled by GameSessionManager.",
        getPlayerId());
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Cannot join a game while already in a session."));
  }

  @Override
  public String getDescription() {
    return "Attempts to join a private game lobby using a 4-digit code.";
  }
}
