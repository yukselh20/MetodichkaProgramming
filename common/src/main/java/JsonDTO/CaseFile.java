package JsonDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

// This is my Plain Old Java Object (POJO) that directly maps to the structure of a case JSON file.
// I use it with the Jackson library to deserialize the entire case data into a single object.
// The nested static classes provide structure for the complex data within the file.
@JsonIgnoreProperties(ignoreUnknown = true)
// This annotation is crucial for forward compatibility. It tells Jackson to ignore any
// properties in the JSON file that don't have a corresponding field in this class,
// which prevents parsing errors if I evolve the JSON schema.
public class CaseFile {
  private String title;
  private String invitation;
  private String description;
  private String startingRoom;
  private List<SuspectData> suspects;
  private List<RoomData> rooms;
  private List<ExamQuestion> finalExam;
  private List<String> tasks;
  private List<String> watsonHints;

  public String getTitle() {
    return title;
  }

  public String getInvitation() {
    return invitation;
  }

  public String getDescription() {
    return description;
  }

  public String getStartingRoom() {
    return startingRoom;
  }

  public List<SuspectData> getSuspects() {
    return suspects;
  }

  public List<RoomData> getRooms() {
    return rooms;
  }

  public List<ExamQuestion> getFinalExam() {
    return finalExam;
  }

  public List<String> getTasks() {
    return tasks;
  }

  public List<String> getWatsonHints() {
    return watsonHints;
  }

  // I need these public setters for the Jackson library to populate the fields
  // during deserialization from the JSON file.
  public void setTitle(String title) {
    this.title = title;
  }

  public void setInvitation(String invitation) {
    this.invitation = invitation;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setStartingRoom(String startingRoom) {
    this.startingRoom = startingRoom;
  }

  public void setSuspects(List<SuspectData> suspects) {
    this.suspects = suspects;
  }

  public void setRooms(List<RoomData> rooms) {
    this.rooms = rooms;
  }

  public void setFinalExam(List<ExamQuestion> finalExam) {
    this.finalExam = finalExam;
  }

  public void setTasks(List<String> tasks) {
    this.tasks = tasks;
  }

  public void setWatsonHints(List<String> watsonHints) {
    this.watsonHints = watsonHints;
  }

  // Nested static class for suspect data in the JSON file.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SuspectData {
    private String name;
    private String statement;
    private String clue;
    private List<String> allowedRooms;

    public String getName() {
      return name;
    }

    public String getStatement() {
      return statement;
    }

    public String getClue() {
      return clue;
    }

    public List<String> getAllowedRooms() {
      return allowedRooms;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setStatement(String statement) {
      this.statement = statement;
    }

    public void setClue(String clue) {
      this.clue = clue;
    }

    public void setAllowedRooms(List<String> allowedRooms) {
      this.allowedRooms = allowedRooms;
    }
  }

  // Nested static class for room data in the JSON file.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RoomData {
    private String name;
    private String description;
    private Map<String, String> neighbors;
    private List<GameObjectData> objects;

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public Map<String, String> getNeighbors() {
      return neighbors;
    }

    public List<GameObjectData> getObjects() {
      return objects;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void setNeighbors(Map<String, String> neighbors) {
      this.neighbors = neighbors;
    }

    public void setObjects(List<GameObjectData> objects) {
      this.objects = objects;
    }
  }

  // Nested static class for game object data in the JSON file.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class GameObjectData {
    private String name;
    private String description;
    private String deduce;
    private String examine;

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getDeduce() {
      return deduce;
    }

    public String getExamine() {
      return examine;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void setDeduce(String deduce) {
      this.deduce = deduce;
    }

    public void setExamine(String examine) {
      this.examine = examine;
    }
  }

  // Nested static class for a single question-answer pair for the final exam.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ExamQuestion {
    private String question;
    private String answer;

    public String getQuestion() {
      return question;
    }

    public String getAnswer() {
      return answer;
    }

    public void setQuestion(String question) {
      this.question = question;
    }

    public void setAnswer(String answer) {
      this.answer = answer;
    }
  }
}
