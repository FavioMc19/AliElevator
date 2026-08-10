package net.nexarys.alielevator.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.Getter;
import lombok.Setter;
import net.nexarys.alielevator.AliElevator;
import net.nexarys.alielevator.objects.elevator.Elevator;
import net.nexarys.alielevator.objects.elevator.ElevatorColumn;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class AliElevatorHud extends CustomUIHud {
    private final AliElevator plugin = AliElevator.getInstance();
    private final PlayerRef playerRef;

    private boolean visible = false;
    private final Vector2i lookupKey = new Vector2i();

    private int hideTicks = -1;

    private ElevatorColumn lastColumn;
    private int lastElevatorY = Integer.MIN_VALUE;

    public AliElevatorHud(@NonNullDecl PlayerRef playerRef) {
        super(playerRef, "AliElevatorHud");
        this.playerRef = playerRef;
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder ui) {
        ui.append("Hud/ElevatorView.ui");
    }

    public void updateHud(ElevatorColumn column, Elevator current) {
        boolean sameState = visible && column == lastColumn && current.getY() == lastElevatorY;

        visible = true;

        if (sameState) return;

        lastColumn = column;
        lastElevatorY = current.getY();

        UICommandBuilder ui = new UICommandBuilder();
        ui.set("#main.Visible", true);

        List<Elevator> elevators = new ArrayList<>(column.getElevators().values()).reversed();

        int currentIndex = 0;
        for (int i = 0; i < elevators.size(); i++) {
            if (elevators.get(i).getY() == current.getY()) {
                currentIndex = i;
                break;
            }
        }

        int start = Math.max(0, currentIndex - 2);
        int end = Math.min(elevators.size() - 1, start + 4);

        if (end - start < 4) {
            start = Math.max(0, end - 4);
        }

        ui.clear("#entries");
        for (int i = start; i <= end; i++) {
            Elevator elevator = elevators.get(i);

            boolean isCurrent = elevator.getY() == current.getY();

            ui.append("#entries", "Hud/ElevatorViewEntry.ui");

            String sel = "#entries[" + (i - start) + "]";

            if (isCurrent) {
                ui.set(sel + " #selector.Background", "Hud/Textures/frame-selector.png");
            }

            ui.set(sel + " #color.Background", elevator.getColor() + (isCurrent ? "E6" : "99"));
            ui.set(sel + " #icon.Background", "Hud/Textures/icons/%s%s.png".formatted(elevator.getIcon().getFileName(), isCurrent ? "Selected" : ""));
        }

        update(false, ui);
    }

    public void hideIfVisible() {
        if (!visible) return;
        this.visible = false;
        hideTicks = 15;
    }

    public void hide() {
        UICommandBuilder ui = new UICommandBuilder();
        ui.set("#main.Visible", false);
        update(false, ui);

        lastColumn = null;
        lastElevatorY = Integer.MIN_VALUE;
    }

    public void tick() {
        if (hideTicks > 0) {
            hideTicks--;
        }

        if (hideTicks == 0 && !visible) {
            hide();
            hideTicks = -1;
        }
    }
}