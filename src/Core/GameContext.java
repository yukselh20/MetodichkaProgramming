package Core;


public class GameContext {
    private Building building; // Replaces 'Mansion'
    private Detective detective;
    private DoctorWatson watson;
    private Journal journal;
    private TaskList taskList;
    private CaseFile selectedCase;

    public GameContext(Building building, Detective detective, DoctorWatson watson, Journal journal, TaskList taskList, CaseFile selectedCase) {
        this.building = building;
        this.detective = detective;
        this.watson = watson;
        this.journal = journal;
        this.taskList = taskList;
        this.selectedCase = selectedCase;

    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public void setTaskList(TaskList taskList) {
        this.taskList = taskList;
    }

    // Getters
    public CaseFile getSelectedCase() { return selectedCase; }
    public Building getBuilding() { return building; }
    public Detective getDetective() { return detective; }
    public DoctorWatson getWatson() { return watson; }
    public Journal getJournal() { return journal; }
    public TaskList getTaskList() { return taskList;}
}