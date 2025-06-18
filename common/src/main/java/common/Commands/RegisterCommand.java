package common.Commands;

import common.ICommandContext;
import java.util.Objects;

// I send this command from the client to the server to request a new user registration.
public class RegisterCommand extends BaseCommand {
  private final String username;
  private final String password;

  public RegisterCommand(String username, String password) {
    super(false);
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
  protected void executeCommand(String[] args, ICommandContext context) {}

  @Override
  public String getDescription() {
    return "Registers a new user: register [username] [password]";
  }
}
