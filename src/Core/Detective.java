package Core;

public class Detective {
    private String name;
    private String rank;
    private int deduceCount = 0;// "Junior Investigator" or "Senior Investigator"

    public Detective(String name) {
        this.name = name;
        this.rank = "Junior Investigator";
    }

    public void incrementDeduceCount() {
        deduceCount++;
    }

    public int getDeduceCount() {
        return deduceCount;
    }

    public String getName() {
        return name;
    }

    public String getRank() {
        return rank;
    }

    public void promote() {
        rank = "Senior Investigator";
    }

    public void demote() {
        rank = "Junior Investigator";
    }
}