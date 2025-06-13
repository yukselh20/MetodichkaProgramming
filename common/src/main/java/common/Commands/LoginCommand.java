package common.Commands;

import common.ICommandContext;
import java.util.Objects;

/** A command sent from the client to the server to log in an existing user. */
public class LoginCommand extends BaseCommand {
  private final String username;
  private final String password;

  public LoginCommand(String username, String password) {
    super(false); // Login doesn't require a case to be started.
    this.username = Objects.requireNonNull(username);
    this.password = Objects.requireNonNull(password);
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  protected void executeCommand(String[] args, ICommandContext context) {
    // This command is handled by the GameSessionManager, not the GameContext.
    // This method should not be called.
  }

  @Override
  public String getDescription() {
    return "Logs in a user: login [username] [password]";
  }
}
