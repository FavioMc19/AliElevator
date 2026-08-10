package net.nexarys.alielevator.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.hud.AliElevatorHud;
import net.nexarys.alielevator.objects.elevator.Elevator;
import net.nexarys.alielevator.objects.elevator.ElevatorColumn;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;
import net.nexarys.alielevator.pages.AliElevatorPage;
import net.nexarys.alielevator.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class HudManager {
    private final AliElevator plugin = AliElevator.getInstance();
    private final Map<PlayerRef, AliElevatorHud> views  = new HashMap<>();

    public void tick(PlayerRef playerRef, Player player) {
        boolean create = !views.containsKey(playerRef);
        AliElevatorHud hud = views.computeIfAbsent(playerRef, AliElevatorHud::new);

        if (create) {
            player.getHudManager().addCustomHud(playerRef, hud);
        }

        hud.tick();

        Transform transform = playerRef.getTransform();
        Vector3d position = transform.getPosition();

        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y - 1);
        int z = (int) Math.floor(position.z);

        World world = Utils.getWorld(playerRef);
        if (world == null) return;

        ElevatorWorld elevatorWorld = plugin.getElevatorManager().get(world.getName());
        if (elevatorWorld.isEmpty()) return;

        Vector2i lookupKey = hud.getLookupKey().set(x, z);
        ElevatorColumn column = elevatorWorld.getColumn(lookupKey);

        if (column == null || !column.hasElevator(y)) {
            hud.hideIfVisible();
            return;
        }

        Elevator elevator = column.getElevator(y);

        if (elevator == null) {
            hud.hideIfVisible();
            return;
        }

        if (plugin.getTeleportManager().isWaitingTeleport(playerRef))
            return;

        hud.updateHud(column, elevator);
    }

    public void openPage(ElevatorWorld elevatorWorld, Elevator elevator, PlayerRef playerRef, BlockPosition blockPosition) {
        Ref<EntityStore> ref = playerRef.getReference();

        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new AliElevatorPage(playerRef, elevatorWorld, elevator, blockPosition));
        });
    }
}
