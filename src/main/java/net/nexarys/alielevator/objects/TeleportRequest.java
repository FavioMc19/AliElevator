package net.nexarys.alielevator.objects;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TeleportRequest {
    private final PlayerRef player;
    private final double targetY;
    private int ticksRemaining;
    private final Runnable runnable;

    public TeleportRequest(PlayerRef player, double targetY, int ticksRemaining, Runnable runnable) {
        this.player = player;
        this.targetY = targetY;
        this.ticksRemaining = ticksRemaining;
        this.runnable = runnable;
    }

    public boolean progress() {
        ticksRemaining--;
        return ticksRemaining <= 0;
    }
}