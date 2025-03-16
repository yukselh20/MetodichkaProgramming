package Core;

import java.util.Map;
import java.util.HashMap;

public abstract class Room {
    public String name;
    protected String description;
    protected Map<String, Room> neighbors = new HashMap<>();
    protected Map<String, GameObject> objects = new HashMap<>();

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Room> getNeighbors() {
        return neighbors;
    }

    public void setNeighbor(String direction, Room neighbor) {
        neighbors.put(direction.toLowerCase(), neighbor);
    }

    public Room getNeighbor(String direction) {
        return neighbors.get(direction.toLowerCase());
    }

    public GameObject getObject(String name) {
        return objects.get(name.toLowerCase());
    }

    public void addObject(String name, GameObject object) {
        objects.put(name.toLowerCase(), object);
    }

    public Map<String, GameObject> getObjects() {
        return objects;
    }

    public String getExitsDescription() {
        if (neighbors.isEmpty()) {
            return "Exits: None";
        }
        StringBuilder sb = new StringBuilder("Exits: ");
        for (Map.Entry<String, Room> entry : neighbors.entrySet()) {
            sb.append(entry.getKey()).append(" (").append(entry.getValue().getName()).append(") ");
        }
        return sb.toString();
    }

    public String getObjectsDescription() {
        if (objects.isEmpty()) {
            return "No objects of interest here.";
        }
        StringBuilder sb = new StringBuilder("Objects present: ");
        for (String objectName : objects.keySet()) {
            sb.append(objectName).append(", ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }


    public String examine(String objectName) {
        GameObject obj = getObject(objectName);
        if (obj != null) {
            String examineText = obj.getExamine(); // Try to get the examine text
            if (examineText != null && !examineText.isEmpty()) {
                return "You examine the " + objectName + ": " + examineText;
            }
            // Fall back to the description if no examine text is available
            return "You examine the " + objectName + ": " + obj.getDescription();
        }
        return "There is no such object here.";
    }
}