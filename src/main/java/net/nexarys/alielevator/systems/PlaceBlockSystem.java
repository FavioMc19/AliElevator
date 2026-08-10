package net.nexarys.alielevator.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.enums.ElevatorIcon;
import net.nexarys.alielevator.managers.ElevatorManager;
import net.nexarys.alielevator.objects.elevator.Elevator;
import net.nexarys.alielevator.objects.elevator.ElevatorColumn;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.Random;
import java.util.UUID;

public class PlaceBlockSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final AliElevator plugin = AliElevator.getInstance();

    public PlaceBlockSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, PlaceBlockEvent event) {
        World world = commandBuffer.getExternalData().getWorld();
        Vector3i position = event.getTargetBlock();
        ItemStack item = event.getItemInHand();
        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null || item == null) return;

        if (!ElevatorManager.elevatorBlock.equals(item.getBlockKey())) return;

        UUID entityUuid = uuidComponent.getUuid();

        Vector2i columnPosition = new Vector2i(position.x, position.z);
        int yPosition = position.y;

        String worldName = world.getName();
        ElevatorWorld elevatorWorld = plugin.getElevatorManager().get(worldName);
        ElevatorColumn elevatorColumn = elevatorWorld.getOrCreate(columnPosition);

        if (event.isCancelled()) return;

        if (!elevatorColumn.canPlace()) {
            Universe.get().sendMessage(Message.raw("Superaste el limite de elevadores en esta columna"));
            event.setCancelled(true);
            return;
        }

        Random random = new Random();

        Elevator elevator = new Elevator(entityUuid, yPosition);
        elevator.setIcon(ElevatorIcon.values()[random.nextInt(ElevatorIcon.values().length)]);
        elevator.setColor("#%06X".formatted(random.nextInt(0x1000000)));

        elevatorColumn.addElevator(elevator);
        elevatorWorld.save();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}