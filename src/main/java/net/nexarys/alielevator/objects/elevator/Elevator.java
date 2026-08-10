package net.nexarys.alielevator.objects.elevator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.enums.ElevatorIcon;
import net.nexarys.alielevator.managers.ElevatorManager;
import net.nexarys.alielevator.utils.Utils;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class Elevator {
    private final AliElevator plugin = AliElevator.getInstance();
    private final UUID owner;
    private List<String> allowedPlayers = new ArrayList<>();
    private int y;
    private boolean isPublic = true;
    private ElevatorIcon icon = ElevatorIcon.CLOUD;
    private String color = "#ffffff";
    private String block = ElevatorManager.elevatorBlock;

    public Elevator(UUID owner, int y) {
        this.owner = owner;
        this.y = y;
    }

    public boolean canUse(PlayerRef playerRef) {
        return isPublic || allowedPlayers.contains(playerRef.getUuid().toString());
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        json.addProperty("owner", owner.toString());
        json.addProperty("y", y);
        json.addProperty("public", isPublic);
        json.addProperty("icon", icon.name());
        json.addProperty("color", color);
        json.addProperty("block", block);

        JsonArray allowed = new JsonArray();
        for (String playerName : allowedPlayers) {
            allowed.add(playerName);
        }
        json.add("allowedPlayers", allowed);

        return json;
    }

    public static Elevator fromJson(JsonObject json) {
        Elevator elevator = new Elevator(UUID.fromString(json.get("owner").getAsString()), json.get("y").getAsInt());

        elevator.setY(json.get("y").getAsInt());
        elevator.setPublic(json.get("public").getAsBoolean());
        elevator.setIcon(ElevatorIcon.fromName(json.get("icon").getAsString()));
        elevator.setColor(json.get("color").getAsString());

        JsonArray allowed = json.getAsJsonArray("allowedPlayers");
        for (int i = 0; i < allowed.size(); i++) {
            elevator.getAllowedPlayers().add(allowed.get(i).getAsString());
        }

        if (json.has("block")) {
            elevator.setBlock(json.get("block").getAsString());
        }

        return elevator;
    }

    public void teleport(PlayerRef player, Elevator from) {
        UUID worldUUID = player.getWorldUuid();
        if (worldUUID == null) return;

        World world = Universe.get().getWorld(worldUUID);
        if (world == null) return;

        Vector3d position = player.getTransform().getPosition();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> entityRef = player.getReference();
            if (entityRef == null || !entityRef.isValid()) return;

            EffectControllerComponent effectControllerComponent = store.getComponent(entityRef, EffectControllerComponent.getComponentType());
            if (effectControllerComponent != null) {
                EntityEffect entityEffect = EntityEffect.getAssetMap().getAsset("AliElevatorEffect");
                assert entityEffect != null;
                effectControllerComponent.addEffect(entityRef, entityEffect, 1, OverlapBehavior.OVERWRITE, store);
            }

            int sound = SoundEvent.getAssetMap().getIndex("SFX_Portal_Neutral_Teleport_Local");
            SoundUtil.playSoundEvent2d(sound, SoundCategory.UI, 0.2f, 1, store);

            plugin.getTeleportManager().requestTeleport(player, y + 1, 10, () -> {
                Utils.playSoundEvent3d(sound, SoundCategory.SFX, position.x, from.y + 1, position.z, 0.2f, 1, (entityStoreRef) -> !entityRef.equals(entityStoreRef), store, 10);
                Utils.playSoundEvent3d(sound, SoundCategory.SFX, position.x, y + 1, position.z, 0.2f, 1, (entityStoreRef) -> !entityRef.equals(entityStoreRef), store, 10);
            });
        });
    }

    public boolean hasPermission(PlayerRef playerRef) {
        return playerRef.getUuid().equals(owner) || playerRef.hasPermission("alielevator.admin");
    }

    public String getBlockItemId() {
        return block;
    }
}
