package common.Commands;

import common.ICommandContext;
import common.dto.JournalEntryDTO;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

// This is my command for viewing journal entries. It supports both listing all entries
// and searching for entries containing a specific keyword.
public class JournalCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 107L;
  // A null keyword here signifies that the user wants to list all of the journal.
  private final String searchKeyword;

  public JournalCommand(String searchKeyword) {
    super(true);
    this.searchKeyword = searchKeyword;
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String playerId = getPlayerId();
    List<JournalEntryDTO> allEntries = context.getJournalEntries();
    List<JournalEntryDTO> entriesToSend;

    // If a search keyword is provided, it filters the list. Otherwise, use the full list.
    if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
      String lowerKeyword = searchKeyword.toLowerCase();
      entriesToSend =
          allEntries.stream()
              .filter(entry -> entry.getFormattedEntry().toLowerCase().contains(lowerKeyword))
              .collect(Collectors.toList());
    } else {
      entriesToSend = allEntries;
    }

    StringBuilder responseText = new StringBuilder();
    if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
      responseText
          .append("--- Journal Search Results for '")
          .append(searchKeyword)
          .append("' ---\n");
    } else {
      responseText.append("--- Journal Contents ---\n");
    }

    if (entriesToSend.isEmpty()) {
      if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
        responseText.append("No entries found matching your keyword.\n");
      } else {
        responseText.append("Journal is currently empty.\n");
      }
    } else {
      entriesToSend.forEach(entry -> responseText.append(entry.getFormattedEntry()).append("\n"));
    }
    responseText.append("--- End of Journal ---");

    context.sendResponseToPlayer(playerId, new TextMessage(responseText.toString()));
  }

  @Override
  public String getDescription() {
    return "Review journal entries. Usage: journal [optional_keyword]";
  }
}
