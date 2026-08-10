package net.nexarys.alielevator;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import net.nexarys.alielevator.listeners.PlayerPacketListener;
import net.nexarys.alielevator.managers.ConfigManager;
import net.nexarys.alielevator.managers.ElevatorManager;
import net.nexarys.alielevator.managers.HudManager;
import net.nexarys.alielevator.managers.TeleportManager;
import net.nexarys.alielevator.objects.ConfigService;
import net.nexarys.alielevator.systems.BreakBlockSystem;
import net.nexarys.alielevator.systems.CrouchAndJumpSystem;
import net.nexarys.alielevator.systems.PlaceBlockSystem;
import net.nexarys.alielevator.systems.PlayerTickSystem;

import javax.annotation.Nonnull;

@Getter
public class AliElevator extends JavaPlugin {
    @Getter
    private static AliElevator instance;

    private ConfigService configService;

    private ConfigManager configManager;
    private ElevatorManager elevatorManager;
    private TeleportManager teleportManager;
    private HudManager hudManager;

    public AliElevator(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        initListeners();
        initClass();
        initSystems();
    }

    @Override
    protected void start() {
    }

    private void initListeners() {
        PacketAdapters.registerInbound(new PlayerPacketListener());
    }

    private void initClass() {
        configService = new ConfigService(this);
        configManager = new ConfigManager(configService);
        elevatorManager = new ElevatorManager();
        teleportManager = new TeleportManager();
        hudManager = new HudManager();
    }

    private void initSystems() {
        ComponentRegistryProxy<EntityStore> registry = getEntityStoreRegistry();
        registry.registerSystem(new BreakBlockSystem());
        registry.registerSystem(new PlaceBlockSystem());
        registry.registerSystem(new CrouchAndJumpSystem());
        registry.registerSystem(new PlayerTickSystem());
    }
}