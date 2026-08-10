package net.nexarys.alielevator.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.managers.ElevatorManager;
import net.nexarys.alielevator.objects.elevator.ElevatorColumn;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;
import net.nexarys.alielevator.utils.Utils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class BreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private final AliElevator plugin = AliElevator.getInstance();

    public BreakBlockSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, BreakBlockEvent event) {
        World world = commandBuffer.getExternalData().getWorld();
        Vector3i position = event.getTargetBlock();
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());

        String worldName = world.getName();
        ElevatorWorld elevatorWorld = plugin.getElevatorManager().get(worldName);

        if (elevatorWorld.isEmpty() || playerRef == null) return;
        Ref<EntityStore> ref = playerRef.getReference();

        if (event.isCancelled() || ref == null || !ref.isValid()) return;

        Vector2i columnPosition = new Vector2i(position.x, position.z);
        int yPosition = position.y;

        ElevatorColumn elevatorColumn = elevatorWorld.getColumn(columnPosition);
        if (elevatorColumn == null || elevatorColumn.isEmpty()) return;

        if (!elevatorColumn.hasElevator(yPosition)) return;

        event.setCancelled(true);

        Utils.setBlock(world, event.getTargetBlock(), "Empty");
        Utils.dropItem(world, store, ElevatorManager.elevatorBlock, 1, new Vector3d(position.x, position.y, position.z));

        elevatorColumn.removeElevator(yPosition);
        elevatorWorld.save();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}