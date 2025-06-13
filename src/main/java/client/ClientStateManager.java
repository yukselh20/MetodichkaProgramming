package client;

import common.dto.CaseInfoDTO;
import common.dto.PublicGameInfoDTO;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Acts as the thread-safe, centralized store for the client's state. It manages the current UI
 * state (`ClientState`) and holds cached data received from the server, serving as the single
 * source of truth for the client.
 */
public class ClientStateManager {

  // Atomic types are used for the core state variables to ensure safe
  // access and modification from multiple threads (e.g., network and input).
  private final AtomicReference<ClientState> currentState =
      new AtomicReference<>(ClientState.INITIAL_MENU);
  private final AtomicBoolean running = new AtomicBoolean(true);

  // Volatile fields ensure that changes made by the network thread are
  // immediately visible to the UI and input threads.
  private volatile List<CaseInfoDTO> availableCasesCache = Collections.emptyList();
  private volatile List<PublicGameInfoDTO> publicGamesCache = Collections.emptyList();
  private volatile String selectedCaseTitleCache = null;
  private volatile String selectedCaseDescriptionCache = null;
  private volatile String gameSessionIdCache = null;
  private volatile String myPlayerIdCache = null;
  private volatile int currentExamQuestionNumberBeingAnswered = 0;

  public ClientState getCurrentState() {
    return currentState.get();
  }

  public ClientState getAndSetState(ClientState newState) {
    return currentState.getAndSet(newState);
  }

  public boolean isRunning() {
    return running.get();
  }

  public void setRunning(boolean isRunning) {
    this.running.set(isRunning);
  }

  public List<CaseInfoDTO> getAvailableCasesCache() {
    return availableCasesCache;
  }

  // Caches are wrapped in unmodifiable lists to prevent accidental
  // modification from outside this class, enforcing its role as the sole manager.
  public void setAvailableCasesCache(List<CaseInfoDTO> availableCasesCache) {
    this.availableCasesCache = Collections.unmodifiableList(availableCasesCache);
  }

  public List<PublicGameInfoDTO> getPublicGamesCache() {
    return publicGamesCache;
  }

  public void setPublicGamesCache(List<PublicGameInfoDTO> publicGamesCache) {
    this.publicGamesCache = Collections.unmodifiableList(publicGamesCache);
  }

  public String getSelectedCaseTitleCache() {
    return selectedCaseTitleCache;
  }

  public void setSelectedCaseTitleCache(String selectedCaseTitleCache) {
    this.selectedCaseTitleCache = selectedCaseTitleCache;
  }

  public void setSelectedCaseDescriptionCache(String desc) {
    this.selectedCaseDescriptionCache = desc;
  }

  public String getSelectedCaseDescriptionCache() {
    return selectedCaseDescriptionCache;
  }

  public void setMyPlayerIdCache(String myPlayerIdCache) {
    this.myPlayerIdCache = myPlayerIdCache;
  }

  public String getMyPlayerIdCache() {
    return myPlayerIdCache;
  }

  public void setGameSessionIdCache(String gameSessionIdCache) {
    this.gameSessionIdCache = gameSessionIdCache;
  }

  public int getCurrentExamQuestionNumberBeingAnswered() {
    return currentExamQuestionNumberBeingAnswered;
  }

  public void setCurrentExamQuestionNumberBeingAnswered(int number) {
    this.currentExamQuestionNumberBeingAnswered = number;
  }

  // These `clear` methods are important for resetting the client's state
  // when a player navigates back in the UI, ensuring no stale data is used.
  public void clearCaseSelectionCache() {
    this.selectedCaseTitleCache = null;
    this.selectedCaseDescriptionCache = null;
  }

  public void clearPublicGamesCache() {
    this.publicGamesCache = Collections.emptyList();
  }
}
