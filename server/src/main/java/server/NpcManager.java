package server;

import Core.Detective;
import Core.DoctorWatson;
import Core.MovableCharacter;
import Core.Room;
import Core.Suspect;
import JsonDTO.CaseFile;
import common.dto.TextMessage;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.extractors.SuspectExtractor;

// This to manage all Non-Player Characters (NPCs) within a single game context.
// It's responsible for initializing them from a case file and handling their movement logic.
public class NpcManager {

  private static final Logger logger = LoggerFactory.getLogger(NpcManager.class);
  private final GameContextServer context;
  private List<Suspect> suspects = new ArrayList<>();
  private DoctorWatson watson;

  public NpcManager(GameContextServer context) {
    this.context = Objects.requireNonNull(context);
  }

  // This clears any existing NPCs and initializes new ones from the provided CaseFile.
  public void initializeNpcs(CaseFile caseFile, Map<String, Room> allRooms) {
    this.suspects.clear();
    this.watson = null;

    if (!SuspectExtractor.loadSuspects(caseFile, context)) {
      logger.error("Failed to load suspects for case: {}", caseFile.getTitle());
    }

    if (caseFile.getWatsonHints() != null && !caseFile.getWatsonHints().isEmpty()) {
      this.watson = new DoctorWatson(caseFile.getWatsonHints());
      Room watsonStart = context.getRoomByName(caseFile.getStartingRoom());
      if (watsonStart == null && !allRooms.isEmpty()) {
        watsonStart = allRooms.values().stream().findFirst().orElse(null);
      }
      if (watsonStart != null) {
        this.watson.setCurrentRoom(watsonStart);
        logger.debug("Dr. Watson initialized and placed in room '{}'.", watsonStart.getName());
      } else {
        logger.warn("Dr. Watson was created but could not be placed in a starting room.");
      }
    }
  }

  public void addSuspect(Suspect suspect) {
    this.suspects.add(suspect);
  }

  public Optional<Suspect> findSuspect(String name) {
    return suspects.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
  }

  public DoctorWatson getWatson() {
    return this.watson;
  }

  public List<Suspect> getSuspects() {
    return Collections.unmodifiableList(suspects);
  }

  // I trigger this logic when a player leaves a room. It checks if any NPCs in that
  // room should now consider moving into an adjacent, unoccupied room.
  public void triggerNpcMovementChecks(Room roomLeft, Map<String, Detective> players) {
    // I only consider NPC movement if the room they are in becomes empty of all players.
    boolean isRoomEmptyOfPlayers =
        players.values().stream().noneMatch(p -> roomLeft.equals(p.getCurrentRoom()));

    if (isRoomEmptyOfPlayers) {
      logger.info("Room '{}' is now empty. Checking for NPC moves.", roomLeft.getName());

      List<MovableCharacter> npcsToMove = new ArrayList<>();
      suspects.stream().filter(s -> roomLeft.equals(s.getCurrentRoom())).forEach(npcsToMove::add);
      if (watson != null && roomLeft.equals(watson.getCurrentRoom())) {
        npcsToMove.add(watson);
      }

      if (!npcsToMove.isEmpty()) {
        npcsToMove.forEach(npc -> moveNpcToValidRoom(npc, players));
      }
    }
  }

  // This moves an NPC to a random, valid, and player-unoccupied adjacent room.
  private void moveNpcToValidRoom(MovableCharacter npc, Map<String, Detective> players) {
    if (npc.getCurrentRoom() == null) return;

    Room oldNpcRoom = npc.getCurrentRoom();
    String npcName = (npc instanceof Suspect) ? ((Suspect) npc).getName() : "Dr. Watson";

    // First, I determine all rooms currently occupied by players.
    Set<Room> playerOccupiedRooms =
        players.values().stream()
            .map(Detective::getCurrentRoom)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // Then, I find all neighboring rooms that are not occupied by any player.
    List<Room> validDestinations =
        oldNpcRoom.getNeighbors().values().stream()
            .filter(Objects::nonNull)
            .filter(neighbor -> !playerOccupiedRooms.contains(neighbor))
            .collect(Collectors.toList());

    if (!validDestinations.isEmpty()) {
      Collections.shuffle(validDestinations);
      Room newNpcRoom = validDestinations.get(0);
      npc.setCurrentRoom(newNpcRoom);

      logger.info(
          "NPC {} moved from '{}' to '{}'.", npcName, oldNpcRoom.getName(), newNpcRoom.getName());

      // I'll notify any players in the destination room that the NPC has arrived.
      players.forEach(
          (pid, detective) -> {
            if (newNpcRoom.equals(detective.getCurrentRoom())) {
              context.sendResponseToPlayer(
                  pid, new TextMessage(npcName + " just entered the room."));
            }
          });
    } else {
      logger.debug(
          "NPC {} in '{}' has no valid, unoccupied rooms to move to.",
          npcName,
          oldNpcRoom.getName());
    }
  }
}
