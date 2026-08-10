package net.nexarys.alielevator.objects.elevator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.objects.Config;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.Map;

public class ElevatorWorld {
    private final AliElevator plugin = AliElevator.getInstance();
    private final String worldName;
    private final Map<Vector2i, ElevatorColumn> columns = new HashMap<>();

    public ElevatorWorld(String worldName) {
        this.worldName = worldName;
    }

    public void save() {
        Config config = plugin.getElevatorManager().getConfig();
        config.set("worlds." + worldName, toJson().toString());
        config.save();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        json.addProperty("worldName", worldName);

        JsonArray columnsArray = new JsonArray();

        for (ElevatorColumn column : columns.values()) {
            if (column.isEmpty()) continue;
            columnsArray.add(column.toJson());
        }

        json.add("columns", columnsArray);

        return json;
    }

    public static ElevatorWorld fromJson(JsonObject json) {
        ElevatorWorld world = new ElevatorWorld(json.get("worldName").getAsString());

        JsonArray columns = json.getAsJsonArray("columns");

        for (int i = 0; i < columns.size(); i++) {
            ElevatorColumn column = ElevatorColumn.fromJson(columns.get(i).getAsJsonObject());
            world.columns.put(column.position, column);
        }

        return world;
    }

    public ElevatorColumn getOrCreate(Vector2i columnPosition) {
        return columns.computeIfAbsent(columnPosition, key -> new ElevatorColumn(columnPosition));
    }

    public ElevatorColumn getColumn(Vector2i columnPosition) {
        return columns.get(columnPosition);
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }
}
