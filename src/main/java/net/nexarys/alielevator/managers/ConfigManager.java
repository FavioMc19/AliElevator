package net.nexarys.alielevator.managers;

import lombok.Getter;
import net.nexarys.alielevator.objects.Config;
import net.nexarys.alielevator.objects.ConfigService;

@Getter
public class ConfigManager {
    private final Config config;

    private boolean limitElevatorColumnSize;
    private int maxElevatorsPerColumn;

    public ConfigManager(ConfigService configService) {
        this.config = configService.getConfig("config.yml");
        loadConfig();
    }

    public void loadConfig() {
        limitElevatorColumnSize = config.getBoolean("limit-elevator-column-size", true);
        maxElevatorsPerColumn = config.getInt("max-elevators-per-column",10);
    }
}