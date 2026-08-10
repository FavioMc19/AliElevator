package net.nexarys.alielevator.managers;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.nexarys.alielevator.objects.TeleportRequest;
import net.nexarys.alielevator.utils.Utils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    private final Map<UUID, TeleportRequest> pending = new ConcurrentHashMap<>();

    public void requestTeleport(PlayerRef player, double targetY, int delayTicks, Runnable runnable) {
        pending.put(player.getUuid(), new TeleportRequest(player, targetY, delayTicks, runnable));
    }

    public void tick(PlayerRef player) {
        UUID uuid = player.getUuid();
        TeleportRequest request = pending.get(uuid);
        if (request == null) return;

        if (!request.progress()) {
            return;
        }

        pending.remove(uuid);
        assert player.getWorldUuid() != null;
        Objects.requireNonNull(Universe.get().getWorld(player.getWorldUuid())).execute(() -> {
            Utils.teleportPlayerY(request.getPlayer(), request.getTargetY());
            request.getRunnable().run();
        });
    }

    public boolean isWaitingTeleport(PlayerRef playerRef) {
        TeleportRequest request = pending.get(playerRef.getUuid());
        if (request == null) return false;
        return request.getTicksRemaining() > 0;
    }
}