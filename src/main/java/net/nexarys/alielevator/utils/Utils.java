package net.nexarys.alielevator.utils;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.world.PlaySoundEvent3D;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static void teleportPlayerY(PlayerRef playerRef, double newY) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) return;

        Vector3d currentPosition = transformComponent.getPosition();

        teleportPlayer(playerRef, currentPosition.x(), newY, currentPosition.z());
    }

    public static void teleportPlayer(PlayerRef playerRef, double x, double y, double z) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());

        if (transformComponent == null) return;

        Vector3d targetPosition = new Vector3d(x, y, z);

        Teleport teleport = Teleport.createForPlayer(transformComponent.getTransform());
        teleport.setPosition(targetPosition);
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }

    public static World getWorld(PlayerRef playerRef) {
        UUID worldUUID = playerRef.getWorldUuid();
        if (worldUUID == null) return null;

        return Universe.get().getWorld(worldUUID);
    }

    static Pattern hex_separator_pattern = Pattern.compile("(&#|#)[A-Fa-f0-9]{6}|&[lL]");

    public static Message getTooltip(String... text) {
        List<Message> messages = new ArrayList<>();
        for (String line : text) {
            messages.add(getText(line));
        }

        return Message.join(messages.toArray(new Message[0]));
    }

    public static Message getText(String text) {
        if (text == null) return null;

        Message base = Message.raw("");
        text = applyColorsToText(text);

        String color = null;
        boolean bold = false;
        for (String part : hexSeparator(text)) {
            if (isHex(part)) {
                color = part;
                bold = false;
                continue;
            }

            if (part.equalsIgnoreCase("&l")) {
                bold = true;
                continue;
            }

            Message component = Message.raw(part);

            if (color != null) {
                component.color(color.startsWith("&") ? color.substring(1) : color);
            }

            component.bold(bold);

            base.insert(component);

        }

        return base;
    }

    public static String applyColorsToText(String text) {
        return text
                .replaceAll("&0", "&#000000")
                .replaceAll("&1", "&#0000aa")
                .replaceAll("&2", "&#00aa00")
                .replaceAll("&3", "&#00aaaa")
                .replaceAll("&4", "&#aa0000")
                .replaceAll("&5", "&#aa00aa")
                .replaceAll("&6", "&#ffaa00")
                .replaceAll("&7", "&#aaaaaa")
                .replaceAll("&8", "&#555555")
                .replaceAll("&9", "&#5555ff")
                .replaceAll("&a", "&#55ff55")
                .replaceAll("&b", "&#55ffff")
                .replaceAll("&c", "&#ff5555")
                .replaceAll("&d", "&#ff55ff")
                .replaceAll("&e", "&#ffff55")
                .replaceAll("&f", "&#ffffff");
    }

    public static boolean isHex(String text) {
        if (text == null)
            return false;

        if (text.startsWith("&"))
            text = text.substring(1);

        Matcher matcher = hex_separator_pattern.matcher(text);

        return matcher.matches();
    }

    public static List<String> hexSeparator(String text) {
        List<String> texts = new ArrayList<>();

        Matcher matcher = hex_separator_pattern.matcher(text);

        int index = 0;

        while (matcher.find()) {
            texts.add(text.substring(index, matcher.start()));
            texts.add(matcher.group().startsWith("&") ? matcher.group() : "&" + matcher.group());
            index = matcher.end();
        }

        texts.add(text.substring(index));
        return texts;
    }

    public static void debug(String message) {
        Universe.get().sendMessage(getText("&7[&cDEBUG&7] &f" + message));
    }

    public static void setBlock(World world, Vector3d position, String id) {
        setBlock(world, (int) position.x, (int) position.y, (int) position.z, id);
    }

    public static void setBlock(World world, Vector3i pos, String id) {
        setBlock(world, pos.x, pos.y, pos.z, id);
    }

    public static void setBlock(World world, int x, int y, int z, String id) {
        if (world == null) return;

        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk != null) {
            BlockType blockType = BlockType.fromString(id);
            assert blockType != null;
            chunk.setBlock(x, y, z, blockType);
        }
    }

    public static void dropItem(World world, Store<EntityStore> store, String itemId, int quantity, Vector3d position) {
        List<ItemStack> itemStacks = List.of(new ItemStack(itemId, quantity));
        Holder<EntityStore>[] itemEntityHolders = ItemComponent.generateItemDrops(store, itemStacks, position, Rotation3f.IDENTITY);
        world.execute(() -> store.addEntities(itemEntityHolders, AddReason.SPAWN));
    }

    public static void playSoundEvent3d(int soundEventIndex, @Nonnull SoundCategory soundCategory, double x, double y, double z, float volumeModifier, float pitchModifier, @Nonnull Predicate<Ref<EntityStore>> shouldHear, @Nonnull ComponentAccessor<EntityStore> componentAccessor, double distance) {
        if (soundEventIndex != 0) {
            SoundEvent soundEvent = SoundEvent.getAssetMap().getAsset(soundEventIndex);
            if (soundEvent != null) {
                PlaySoundEvent3D packet = new PlaySoundEvent3D(soundEventIndex, soundCategory, new Position(x, y, z), volumeModifier, pitchModifier);
                SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = componentAccessor.getResource(EntityModule.get().getPlayerSpatialResourceType());
                List<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
                playerSpatialResource.getSpatialStructure().collect(new Vector3d(x, y, z), distance, results);

                for(Ref<EntityStore> playerRef : results) {
                    if (playerRef != null && playerRef.isValid() && shouldHear.test(playerRef)) {
                        PlayerRef playerRefComponent = componentAccessor.getComponent(playerRef, PlayerRef.getComponentType());

                        assert playerRefComponent != null;

                        playerRefComponent.getPacketHandler().write(packet);
                    }
                }
            }
        }
    }
}
