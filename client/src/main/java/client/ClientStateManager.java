package client;

import common.dto.CaseInfoDTO;
import common.dto.PublicGameInfoDTO;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A thread-safe, centralized store for the client's state. It manages the
// current UI state (ClientState) and holds cached data from the server.
public class ClientStateManager {

  private static final Logger logger = LoggerFactory.getLogger(ClientStateManager.class);

  private final AtomicReference<ClientState> currentState =
      new AtomicReference<>(ClientState.INITIAL_MENU);
  private final AtomicBoolean running = new AtomicBoolean(true);

  private volatile List<CaseInfoDTO> availableCasesCache = Collections.emptyList();
  private volatile List<PublicGameInfoDTO> publicGamesCache = Collections.emptyList();
  private volatile String selectedCaseTitleCache = null;
  private volatile String selectedCaseDescriptionCache = null;
  private volatile String gameSessionIdCache = null;
  private volatile String myPlayerIdCache = null;
  private volatile int currentExamQuestionNumberBeingAnswered = 0;
  private volatile boolean isHost = false;

  public ClientState getCurrentState() {
    return currentState.get();
  }

  public ClientState getAndSetState(ClientState newState) {
    ClientState oldState = currentState.getAndSet(newState);
    logger.debug("Client state changed from {} to {}", oldState, newState);
    return oldState;
  }

  public boolean isHost() {
    return isHost;
  }

  public void setIsHost(boolean isHost) {
    logger.debug("Setting client as host: {}", isHost);
    this.isHost = isHost;
  }

  public boolean isRunning() {
    return running.get();
  }

  public void setRunning(boolean isRunning) {
    logger.debug("Setting running state to: {}", isRunning);
    this.running.set(isRunning);
  }

  public List<CaseInfoDTO> getAvailableCasesCache() {
    return availableCasesCache;
  }

  public void setAvailableCasesCache(List<CaseInfoDTO> availableCasesCache) {
    this.availableCasesCache = Collections.unmodifiableList(availableCasesCache);
    logger.debug("Set available cases cache with {} items.", this.availableCasesCache.size());
  }

  public List<PublicGameInfoDTO> getPublicGamesCache() {
    return publicGamesCache;
  }

  public void setPublicGamesCache(List<PublicGameInfoDTO> publicGamesCache) {
    this.publicGamesCache = Collections.unmodifiableList(publicGamesCache);
    logger.debug("Set public games cache with {} items.", this.publicGamesCache.size());
  }

  public String getSelectedCaseTitleCache() {
    return selectedCaseTitleCache;
  }

  public void setSelectedCaseTitleCache(String selectedCaseTitleCache) {
    logger.debug("Setting selected case title cache to: '{}'", selectedCaseTitleCache);
    this.selectedCaseTitleCache = selectedCaseTitleCache;
  }

  public String getSelectedCaseDescriptionCache() {
    return selectedCaseDescriptionCache;
  }

  public void setSelectedCaseDescriptionCache(String desc) {
    this.selectedCaseDescriptionCache = desc;
  }

  public String getMyPlayerIdCache() {
    return myPlayerIdCache;
  }

  public void setMyPlayerIdCache(String myPlayerIdCache) {
    logger.debug("Setting player ID cache to: '{}'", myPlayerIdCache);
    this.myPlayerIdCache = myPlayerIdCache;
  }

  public String getGameSessionIdCache() {
    return gameSessionIdCache;
  }

  public void setGameSessionIdCache(String gameSessionIdCache) {
    logger.debug("Setting game session ID cache to: '{}'", gameSessionIdCache);
    this.gameSessionIdCache = gameSessionIdCache;
  }

  public int getCurrentExamQuestionNumberBeingAnswered() {
    return currentExamQuestionNumberBeingAnswered;
  }

  public void setCurrentExamQuestionNumberBeingAnswered(int number) {
    logger.debug("Setting current exam question number being answered to: {}", number);
    this.currentExamQuestionNumberBeingAnswered = number;
  }

  // `clear` methods to reset the client's state when a
  // player navigates back in the UI, ensuring no stale data is used.
  public void clearCaseSelectionCache() {
    logger.debug("Clearing case selection cache.");
    this.selectedCaseTitleCache = null;
    this.selectedCaseDescriptionCache = null;
  }

  public void clearPublicGamesCache() {
    logger.debug("Clearing public games cache.");
    this.publicGamesCache = Collections.emptyList();
  }

  public void clearSessionData() {
    logger.debug("Clearing all session-specific cache data.");
    this.selectedCaseTitleCache = null;
    this.selectedCaseDescriptionCache = null;
    this.publicGamesCache = Collections.emptyList();
    this.gameSessionIdCache = null;
    this.myPlayerIdCache = null;
    this.isHost = false; // Reset the host status
  }
}
