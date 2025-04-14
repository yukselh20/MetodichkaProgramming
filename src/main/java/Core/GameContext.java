package Core;

import JsonDTO.CaseFile;

public class GameContext {
    private Building building; // Dynamic game world
    private Detective detective;
    private DoctorWatson watson;
    private Journal journal;
    private TaskList taskList;
    private CaseFile selectedCase; // Centralized case reference
    private boolean exitCurrentGame = false;
    private boolean CaseStarted = false;
    private boolean InCaseSelectionMenu;

    public GameContext(
            Building building,
            Detective detective,
            DoctorWatson watson,
            Journal journal,
            TaskList taskList,
            CaseFile selectedCase) {
        this.building = building;
        this.detective = detective;
        this.watson = watson;
        this.journal = journal;
        this.taskList = taskList;
        this.selectedCase = selectedCase;
    }


    //Setters
    public void setInCaseSelectionMenu(boolean isInCaseSelectionMenu) { this.InCaseSelectionMenu = isInCaseSelectionMenu; }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public void setTaskList(TaskList taskList) {
        this.taskList = taskList;
    }

    public void setCaseStarted(boolean isCaseStarted) { this.CaseStarted = isCaseStarted; }

    // Getters
    public boolean isInCaseSelectionMenu() { return InCaseSelectionMenu; }

    public CaseFile getSelectedCase() {
        return selectedCase;
    }

    public Building getBuilding() {
        return building;
    }

    public Detective getDetective() {
        return detective;
    }

    public DoctorWatson getWatson() {
        return watson;
    }

    public Journal getJournal() {
        return journal;
    }

    public TaskList getTaskList() {
        return taskList;
    }

    public boolean isExitCurrentGame() {
        return exitCurrentGame;
    }

    public void setExitCurrentGame(boolean exit) {
        this.exitCurrentGame = exit;
    }

    public boolean isCaseStarted() { return CaseStarted; }
}
