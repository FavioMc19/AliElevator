package net.nexarys.alielevator.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.objects.elevator.Elevator;
import net.nexarys.alielevator.objects.elevator.ElevatorColumn;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector2i;
import org.joml.Vector3d;

import java.util.function.BiFunction;

public class CrouchAndJumpSystem extends EntityTickingSystem<EntityStore> {
    private final AliElevator plugin = AliElevator.getInstance();

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        World world = commandBuffer.getExternalData().getWorld();
        MovementStatesComponent movementStatesComponent = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        MovementStates current = movementStatesComponent.getMovementStates();
        MovementStates previous = movementStatesComponent.getSentMovementStates();

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (!ref.isValid()) return;

        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) return;

        if (current.crouching && !previous.crouching) {
            world.execute(() -> onCrouch(player, world.getName()));
        }

        if (current.jumping && !previous.jumping) {
            world.execute(() -> onJump(player, world.getName()));
        }
    }

    private void onJump(PlayerRef player, String worldName) {
        moveElevator(player, worldName, ElevatorColumn::getAbove, true);
    }

    private void onCrouch(PlayerRef player, String worldName) {
        moveElevator(player, worldName, ElevatorColumn::getBelow, false);
    }

    private void moveElevator(PlayerRef player, String worldName, BiFunction<ElevatorColumn, Integer, Elevator> selector, boolean up) {
        Vector3d position = player.getTransform().getPosition();
        ElevatorWorld elevatorWorld = plugin.getElevatorManager().get(worldName);

        if (elevatorWorld.isEmpty()) return;

        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);

        Vector2i columnPosition = new Vector2i(x, z);
        int yPosition = (int) Math.floor(position.y) - 1;

        ElevatorColumn elevatorColumn = elevatorWorld.getColumn(columnPosition);
        if (elevatorColumn == null || elevatorColumn.isEmpty()) return;

        Elevator target = selector.apply(elevatorColumn, yPosition);
        if (target == null) return;

        if (target.canUse(player)) {
            target.teleport(player, elevatorColumn.getElevator(yPosition));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType());
    }
}