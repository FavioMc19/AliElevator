package net.nexarys.alielevator.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.objects.Config;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ElevatorManager {
    private final Config config;
    public static String elevatorBlock = "AliElevatorBlock";
    private final Map<String, ElevatorWorld> elevators = new HashMap<>();

    public ElevatorManager() {
        this.config = AliElevator.getInstance().getConfigService().getConfig("elevators.ali");
        loadElevators();
    }

    public void loadElevators() {
        for (String worldName : config.getKeys("worlds")) {
            String jsonData = config.getString("worlds." + worldName, "{}");
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            ElevatorWorld elevatorWorld = ElevatorWorld.fromJson(jsonObject);
            elevators.put(worldName, elevatorWorld);
        }
    }

    public ElevatorWorld get(String worldName) {
        return elevators.computeIfAbsent(worldName, key -> new ElevatorWorld(worldName));
    }
}
