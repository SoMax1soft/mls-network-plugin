// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.environment.SightRays;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class BukkitSightLines implements WorldSightLines {

    private final ServerRoster roster;
    private final ThreadOwnership ownership;
    private final AirReading air;
    private final PluginLog log;
    private final ThreadLocal<SightCaster> casters = ThreadLocal.withInitial(SightCaster::new);
    private final BooleanSupplier shared;

    public BukkitSightLines(ServerRoster roster, ThreadOwnership ownership, AirReading air, PluginLog log) {
        this(roster, ownership, air, log, false);
    }

    public BukkitSightLines(ServerRoster roster, ThreadOwnership ownership, AirReading air, PluginLog log,
                            boolean shared) {
        this(roster, ownership, air, log, shared ? () -> true : () -> false);
    }

    public BukkitSightLines(ServerRoster roster, ThreadOwnership ownership, AirReading air, PluginLog log,
                            BooleanSupplier shared) {
        this.roster = roster;
        this.ownership = ownership;
        this.air = air;
        this.log = log;
        this.shared = shared;
    }

    @Override
    public RayVerdict trace(UUID world, Vec3 from, Vec3 to) {
        if (!SightSegment.finite(from, to)) {
            return RayVerdict.UNKNOWN;
        }
        try {
            World found = roster.worldOrNull(world);
            if (found == null || !readable(found, from) || !readable(found, to)) {
                return RayVerdict.UNKNOWN;
            }
            SightSegment segment = SightSegment.between(from, to);
            if (segment.tooShort()) {
                return RayVerdict.CLEAR;
            }
            return verdictOf(casters.get().cast(found, segment), segment);
        } catch (RuntimeException failure) {
            log.warn(LogTopic.DATA, "A sight-line check failed: " + failure.getClass().getSimpleName(), failure);
            return RayVerdict.UNKNOWN;
        }
    }

    @Override
    public boolean provenClear(UUID world, Vec3 from, Box ends) {
        if (!AirVolume.spannable(from, ends)) {
            return false;
        }
        AirVolume volume = AirVolume.spanning(from, ends);
        if (!volume.small()) {
            return false;
        }
        try {
            World found = roster.worldOrNull(world);
            return found != null && volume.readable(found, ownership) && volume.onlyAir(found, air);
        } catch (RuntimeException failure) {
            log.debug(LogTopic.DATA, () -> "Open-air proof unavailable, asking each sight line: " + failure);
            return false;
        }
    }

    @Override
    public SightRays rays(UUID world, Vec3 eye, Box ends) {
        if (shared.getAsBoolean() && AirVolume.spannable(eye, ends)) {
            AirVolume volume = AirVolume.spanning(eye, ends);
            if (SightBundle.blocks(volume) <= SightBundle.MOST_SHARED_BLOCKS) {
                try {
                    World found = roster.worldOrNull(world);
                    if (found != null && volume.readable(found, ownership)) {
                        return new SightBundle(this, world, found, volume, air, log);
                    }
                } catch (RuntimeException failure) {
                    log.debug(LogTopic.DATA, () -> "Shared sight lines unavailable: " + failure);
                }
            }
        }
        return WorldSightLines.super.rays(world, eye, ends);
    }

    private boolean readable(World world, Vec3 point) {
        int chunkX = SightSegment.chunkIndex(point.x());
        int chunkZ = SightSegment.chunkIndex(point.z());
        return ownership.ownsChunk(world, chunkX, chunkZ) && world.isChunkLoaded(chunkX, chunkZ);
    }

    static RayVerdict verdictOf(RayTraceResult hit, SightSegment segment) {
        if (hit == null) {
            return RayVerdict.CLEAR;
        }
        Vector position = hit.getHitPosition();
        return segment.cutAt(position.getX(), position.getY(), position.getZ())
                ? RayVerdict.BLOCKED : RayVerdict.CLEAR;
    }
}
