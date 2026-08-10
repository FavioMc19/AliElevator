package net.nexarys.alielevator.listeners;

import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.objects.elevator.Elevator;
import net.nexarys.alielevator.objects.elevator.ElevatorColumn;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;
import org.joml.Vector2i;

import java.util.UUID;

public class PlayerPacketListener implements PlayerPacketFilter {
    private final AliElevator plugin = AliElevator.getInstance();
    private final Vector2i checker =  new Vector2i();

    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        if (packet instanceof SyncInteractionChains interactionChains) {
            for (SyncInteractionChain update : interactionChains.updates) {
                if (update.interactionType == InteractionType.Use && update.data != null) {
                    UUID worldUuid = playerRef.getWorldUuid();
                    if (worldUuid != null) {
                        World world = Universe.get().getWorld(worldUuid);
                        if (world != null) {

                            BlockPosition blockPosition = update.data.blockPosition;
                            if (blockPosition != null) {

                                int x =  blockPosition.x;
                                int y = blockPosition.y;
                                int z = blockPosition.z;

                                checker.set(x, z);
                                ElevatorWorld elevatorWorld = plugin.getElevatorManager().get(world.getName());

                                ElevatorColumn elevatorColumn = elevatorWorld.getColumn(checker);
                                if (elevatorColumn != null) {

                                    Elevator elevator = elevatorColumn.getElevator(y);
                                    if (elevator != null && elevator.hasPermission(playerRef)) {
                                        plugin.getHudManager().openPage(elevatorWorld, elevator, playerRef, blockPosition);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
