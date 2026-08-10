package net.nexarys.alielevator.pages;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockFace;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RequiredBlockFaceSupport;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.nexarys.alielevator.enums.ElevatorIcon;
import net.nexarys.alielevator.managers.ElevatorManager;
import net.nexarys.alielevator.objects.elevator.Elevator;
import net.nexarys.alielevator.objects.elevator.ElevatorWorld;
import net.nexarys.alielevator.utils.Utils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;

import java.util.*;

public class AliElevatorPage extends InteractiveCustomUIPage<AliElevatorPage.Data> {

    private final ElevatorWorld elevatorWorld;
    private final Elevator elevator;
    private boolean accessPublic;
    private String selectedPlayer = "";
    private final List<String> allowedPlayers;
    private String block;
    private ElevatorIcon icon;
    private String color;
    private final BlockPosition blockPosition;

    public AliElevatorPage(@NonNullDecl PlayerRef playerRef, ElevatorWorld elevatorWorld, Elevator elevator, BlockPosition blockPosition) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.elevatorWorld = elevatorWorld;
        this.elevator = elevator;
        this.blockPosition = blockPosition;
        this.accessPublic = elevator.isPublic();
        this.allowedPlayers = new ArrayList<>(elevator.getAllowedPlayers());
        this.block = elevator.getBlockItemId();
        this.icon = elevator.getIcon();
        this.color = elevator.getColor();
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder event, @NonNullDecl Store<EntityStore> store) {
        ui.append("Pages/AliElevatorPage.ui");

        event.addEventBinding(CustomUIEventBindingType.Activating, "#saveButton", EventData.of("Action", "save"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#closeButton", EventData.of("Action", "close"), false);

        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#colorPicker", EventData.of("Action", "colorPicker").append("@PickerColor", "#colorPicker.Value"), false);
        ui.set("#colorPicker.Value", color + "ff");

        buildBlockList(ui, event, ref, store);

        List<DropdownEntryInfo> onlinePlayers = new ArrayList<>();
        for (PlayerRef other : Universe.get().getPlayers()) {
            if (other.getUsername().equals(playerRef.getUsername()) || elevator.getAllowedPlayers().contains(other.getUsername())) continue;
            onlinePlayers.add(new DropdownEntryInfo(LocalizableString.fromString(other.getUsername()), other.getUsername()));
        }

        event.addEventBinding(CustomUIEventBindingType.Activating, "#addPlayerButton", EventData.of("Action", "addPlayer"), false);

        ui.set("#onlinePlayersDropdown.Entries", onlinePlayers);
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#onlinePlayersDropdown", EventData.of("Action", "playerList").append("@DropDown", "#onlinePlayersDropdown.Value"), false);

        List<DropdownEntryInfo> accessMode = List.of(new DropdownEntryInfo(LocalizableString.fromString("Public"), "public"),
                new DropdownEntryInfo(LocalizableString.fromString("Private"), "private"));

        ui.set("#accessMode.Entries", accessMode);
        ui.set("#accessMode.Value", elevator.isPublic() ? "public" : "private");
        event.addEventBinding(CustomUIEventBindingType.ValueChanged, "#accessMode", EventData.of("Action", "accessMode").append("@DropDown", "#accessMode.Value"), false);

        buildIconList(ui, event);

        buildPlayerList(ui, event);
    }

    public void buildPlayerList(UICommandBuilder ui, UIEventBuilder event) {
        ui.clear("#playersList");
        for (int i = 0; i < allowedPlayers.size(); i++) {
            ui.append("#playersList", "Pages/PlayerAccessEntry.ui");
            ui.set("#playersList[" + i + "] #playerName.Text", allowedPlayers.get(i));
            event.addEventBinding(CustomUIEventBindingType.Activating, "#playersList[" + i + "] #removeButton", EventData.of("Action", "removePlayer").append("Line", allowedPlayers.get(i)), false);
        }
    }

    public void buildIconList(UICommandBuilder ui, UIEventBuilder event) {
        ElevatorIcon[] icons = ElevatorIcon.values();
        ui.clear("#iconList");
        for (int i = 0; i < icons.length; i++) {
            ElevatorIcon icon = icons[i];
            boolean isCurrent = icon == this.icon;

            ui.append("#iconList", "Pages/IconOption.ui");
            String sel = "#iconList[" + i + "]";
            ui.set(sel + " #icon.Background", "Hud/Textures/icons/%s%s.png".formatted(icon.getFileName(), isCurrent ? "Selected" : ""));

            if (isCurrent) {
                ui.set(sel + " #selector.Background", "Hud/Textures/frame-selector.png");
            }

            event.addEventBinding(CustomUIEventBindingType.Activating, sel + " #icon", EventData.of("Action", "icon").append("Line", icon.getFileName()), false);
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl Data data) {
        if (data.action == null) return;

        switch (data.action) {
            case "accessMode" -> accessPublic = data.dropDown.equals("public");
            case "playerList" -> selectedPlayer = data.dropDown;
            case "colorPicker" -> color = data.pickerColor.length() == 9 ? data.pickerColor.substring(0, 7) : data.pickerColor;

            case "icon" -> {
                icon = ElevatorIcon.fromName(data.line);
                UICommandBuilder ui = new UICommandBuilder();
                UIEventBuilder event = new UIEventBuilder();
                buildIconList(ui, event);
                sendUpdate(ui, event, false);
            }
            case "block" -> {
                block = data.line;
                UICommandBuilder ui = new UICommandBuilder();
                UIEventBuilder event = new UIEventBuilder();

                buildBlockList(ui, event, ref, store);
                sendUpdate(ui, event, false);
            }

            case "addPlayer" -> {
                if (selectedPlayer != null) {
                    if (!allowedPlayers.contains(selectedPlayer)) {
                        allowedPlayers.add(selectedPlayer);
                        UICommandBuilder ui = new UICommandBuilder();
                        UIEventBuilder event = new UIEventBuilder();
                        buildPlayerList(ui, event);
                        sendUpdate(ui, event, false);
                    }
                }
            }

            case "removePlayer" -> {
                allowedPlayers.remove(data.line);
                UICommandBuilder ui = new UICommandBuilder();
                UIEventBuilder event = new UIEventBuilder();
                buildPlayerList(ui, event);
                sendUpdate(ui, event, false);
            }

            case "save" -> {
                if (!elevator.getBlock().equals(block)) {
                    World world = store.getExternalData().getWorld();
                    Utils.setBlock(world, new Vector3d(blockPosition.x, blockPosition.y, blockPosition.z), block);
                }

                elevator.setIcon(icon);
                elevator.setBlock(block);
                elevator.setPublic(accessPublic);
                elevator.setAllowedPlayers(allowedPlayers);
                elevator.setColor(color);
                elevatorWorld.save();
                close();
            }
            case "cancel" -> close();
        }
    }

    private void buildBlockList(UICommandBuilder ui, UIEventBuilder event, Ref<EntityStore> ref, Store<EntityStore> store) {
        LinkedHashSet<String> blockIds = new LinkedHashSet<>();

        InventoryComponent.Hotbar hotbar = (InventoryComponent.Hotbar) store.getComponent(ref, Objects.requireNonNull(InventoryComponent.getComponentTypeById(InventoryComponent.HOTBAR_SECTION_ID)));
        InventoryComponent inventory = store.getComponent(ref, Objects.requireNonNull(InventoryComponent.getComponentTypeById(InventoryComponent.STORAGE_SECTION_ID)));

        if (hotbar != null) collectBlockIds(hotbar.getInventory(), blockIds);
        if (inventory != null) collectBlockIds(inventory.getInventory(), blockIds);

        blockIds.add(ElevatorManager.elevatorBlock);

        ui.clear("#blockList");

        int i = 0;
        for (String itemId : blockIds) {
            boolean isCurrent = itemId.equals(block);

            ui.append("#blockList", "Pages/ElevatorBlockEntry.ui");
            String sel = "#blockList[" + i + "]";

            ui.set(sel + " #icon.ItemId", itemId);
            ui.set(sel + " #icon.ShowQuantity", false);
            ui.set(sel + " #selectedOverlay.Visible", isCurrent);

            event.addEventBinding(CustomUIEventBindingType.Activating, sel, EventData.of("Action", "block").append("Line", itemId), false);
            i++;
        }
    }

    private void collectBlockIds(ItemContainer container, Set<String> out) {
        for (short slot = 0; slot < container.getCapacity(); slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || !stack.isValid()) continue;

            Item item = stack.getItem();
            if (!item.hasBlockType()) continue;

            BlockType blockType = BlockType.fromString(item.getBlockId());
            if (blockType == null || !blockType.isFullySupportive()) continue;
            Map<BlockFace, RequiredBlockFaceSupport[]> support = blockType.getSupport(0);

            if (support != null && !support.isEmpty()) {
                continue;
            }

            out.add(item.getId());
        }
    }

    public static final class Data {
        public String action;
        public String dropDown;
        public String line;
        public String pickerColor;

        public static final BuilderCodec<Data> CODEC =
                BuilderCodec.builder(Data.class, Data::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                        .append(new KeyedCodec<>("Line", Codec.STRING), (d, v) -> d.line = v, d -> d.line).add()
                        .append(new KeyedCodec<>("@DropDown", Codec.STRING), (d, v) -> d.dropDown = v, d -> d.dropDown).add()
                        .append(new KeyedCodec<>("@PickerColor", Codec.STRING), (d, v) -> d.pickerColor = v, d -> d.pickerColor).add()
                        .build();
    }
}