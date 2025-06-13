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

/**
 * Manages all Non-Player Characters (NPCs) within a single game context. This class is responsible
 * for initializing NPCs from a case file and handling their autonomous movement logic.
 */
public class NpcManager {

  private final GameContextServer context;
  private List<Suspect> suspects = new ArrayList<>();
  private DoctorWatson watson;

  public NpcManager(GameContextServer context) {
    this.context = Objects.requireNonNull(context);
  }

  /** Clears any existing NPCs and initializes new ones based on the provided CaseFile. */
  public void initializeNpcs(CaseFile caseFile, Map<String, Room> allRooms) {
    this.suspects.clear();
    this.watson = null;

    if (!server.extractors.SuspectExtractorServer.loadSuspects(caseFile, context)) {
      System.err.println("Failed to load suspects for case: " + caseFile.getTitle());
    }

    if (caseFile.getWatsonHints() != null && !caseFile.getWatsonHints().isEmpty()) {
      this.watson = new DoctorWatson(caseFile.getWatsonHints());
      Room watsonStart = context.getRoomByName(caseFile.getStartingRoom());
      if (watsonStart == null && !allRooms.isEmpty()) {
        watsonStart = allRooms.values().stream().findFirst().orElse(null);
      }
      if (watsonStart != null) {
        this.watson.setCurrentRoom(watsonStart);
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

  /**
   * This logic is triggered when a player leaves a room. It checks if any NPCs in that room should
   * now consider moving into an adjacent, unoccupied room.
   */
  public void triggerNpcMovementChecks(Room roomLeft, Map<String, Detective> players) {
    // NPC movement is only considered if the room they are in becomes empty of all players.
    boolean isRoomEmptyOfPlayers =
        players.values().stream().noneMatch(p -> roomLeft.equals(p.getCurrentRoom()));

    if (isRoomEmptyOfPlayers) {
      System.out.println("Room '" + roomLeft.getName() + "' is now empty. Checking for NPC moves.");

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

  // Moves an NPC to a random, valid, and player-unoccupied adjacent room.
  private void moveNpcToValidRoom(MovableCharacter npc, Map<String, Detective> players) {
    if (npc.getCurrentRoom() == null) return;

    Room oldNpcRoom = npc.getCurrentRoom();
    String npcName = (npc instanceof Suspect) ? ((Suspect) npc).getName() : "Dr. Watson";

    // First, determine all rooms currently occupied by players.
    Set<Room> playerOccupiedRooms =
        players.values().stream()
            .map(Detective::getCurrentRoom)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // Then, find all neighboring rooms that are not occupied by any player.
    List<Room> validDestinations =
        oldNpcRoom.getNeighbors().values().stream()
            .filter(Objects::nonNull)
            .filter(neighbor -> !playerOccupiedRooms.contains(neighbor))
            .collect(Collectors.toList());

    if (!validDestinations.isEmpty()) {
      Collections.shuffle(validDestinations);
      Room newNpcRoom = validDestinations.get(0);
      npc.setCurrentRoom(newNpcRoom);

      System.out.println(
          "NPC "
              + npcName
              + " moved from '"
              + oldNpcRoom.getName()
              + "' to '"
              + newNpcRoom.getName()
              + "'.");

      // Notify any players in the destination room that the NPC has arrived.
      players.forEach(
          (pid, detective) -> {
            if (newNpcRoom.equals(detective.getCurrentRoom())) {
              context.sendResponseToPlayer(
                  pid, new TextMessage(npcName + " just entered the room."));
            }
          });
    } else {
      System.out.println(
          "NPC "
              + npcName
              + " in '"
              + oldNpcRoom.getName()
              + "' has no valid, unoccupied rooms to move to.");
    }
  }
}
