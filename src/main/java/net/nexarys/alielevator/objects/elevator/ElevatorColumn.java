package net.nexarys.alielevator.objects.elevator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.managers.ConfigManager;
import org.joml.Vector2i;

import java.util.Map;
import java.util.TreeMap;

@Getter
public class ElevatorColumn {
    public final Vector2i position;
    private final TreeMap<Integer, Elevator> elevators = new TreeMap<>();

    public ElevatorColumn(Vector2i position) {
        this.position = position;
    }

    public Elevator getElevator(int y) {
        return elevators.get(y);
    }

    public boolean hasElevator(int y) {
        return elevators.containsKey(y);
    }

    public boolean canPlace() {
        ConfigManager configManager = AliElevator.getInstance().getConfigManager();
        return !configManager.isLimitElevatorColumnSize() || elevators.size() < configManager.getMaxElevatorsPerColumn();
    }

    public void addElevator(Elevator elevator) {
        elevators.put(elevator.getY(), elevator);
    }

    public void removeElevator(int y) {
        elevators.remove(y);
    }

    public Elevator getAbove(int y) {
        Map.Entry<Integer, Elevator> entry = elevators.higherEntry(y);
        return entry != null ? entry.getValue() : null;
    }

    public Elevator getBelow(int y) {
        Map.Entry<Integer, Elevator> entry = elevators.lowerEntry(y);
        return entry != null ? entry.getValue() : null;
    }

    public int size() {
        return elevators.size();
    }

    public boolean isEmpty() {
        return elevators.isEmpty();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        json.addProperty("x", position.x);
        json.addProperty("z", position.y);

        JsonArray elevatorsArray = new JsonArray();

        for (Elevator elevator : elevators.values()) {
            elevatorsArray.add(elevator.toJson());
        }

        json.add("elevators", elevatorsArray);

        return json;
    }

    public static ElevatorColumn fromJson(JsonObject json) {
        Vector2i position = new Vector2i(json.get("x").getAsInt(), json.get("z").getAsInt());

        ElevatorColumn column = new ElevatorColumn(position);

        JsonArray elevators = json.getAsJsonArray("elevators");

        for (int i = 0; i < elevators.size(); i++) {
            Elevator elevator = Elevator.fromJson(elevators.get(i).getAsJsonObject());
            column.addElevator(elevator);
        }

        return column;
    }
}