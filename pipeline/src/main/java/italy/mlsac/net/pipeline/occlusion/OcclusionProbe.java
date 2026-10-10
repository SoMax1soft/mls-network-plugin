// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OcclusionProbe {
    private static final double SHORTEST_SEGMENT = 0.001;
    private static final double[] MARGIN_SHIFTS = {0.0, 0.03, 0.1, 0.3};
    private static final double EDGE_PULLBACK = 0.05;
    private static final double EXPOSURE_POINTS = 9.0;
    private static final double FULLY_EXPOSED = 1.0;
    private static final int EXPOSURE_TARGETS = 9;
    private static final int CORNERS = 8;

    private final WorldSightLines rays;

    public OcclusionProbe(WorldSightLines rays) {
        this.rays = rays;
    }

    public int crosshairVerdict(WallCheckRequest job) {
        boolean cut = false;
        boolean unknown = false;
        for (int index = 0; index < job.eyes().size(); index++) {
            Vec3 end = job.crosshairEnds().get(index);
            if (end == null) {
                continue;
            }
            RayVerdict verdict = trace(job.world(), job.eyes().get(index), end);
            if (verdict == RayVerdict.CLEAR) {
                return 0;
            }
            if (verdict == RayVerdict.UNKNOWN) {
                unknown = true;
            } else {
                cut = true;
            }
        }
        return cut && !unknown ? 1 : -1;
    }

    public int centreVerdict(WallCheckRequest job) {
        boolean cut = false;
        boolean unknown = false;
        Vec3 centre = job.tight().centre();
        for (Vec3 eye : job.eyes()) {
            RayVerdict verdict = trace(job.world(), eye, centre);
            if (verdict == RayVerdict.CLEAR) {
                return 0;
            }
            if (verdict == RayVerdict.UNKNOWN) {
                unknown = true;
            } else {
                cut = true;
            }
        }
        return cut && !unknown ? 1 : -1;
    }

    public double pickSlack(WallCheckRequest job) {
        boolean aimed = false;
        boolean unknown = false;
        for (double shift : MARGIN_SHIFTS) {
            for (int index = 0; index < job.eyes().size(); index++) {
                Vec3 end = job.crosshairEnds().get(index);
                if (end == null) {
                    continue;
                }
                aimed = true;
                for (Vec3 probe : probes(end, shift, job.surface())) {
                    RayVerdict verdict = trace(job.world(), job.eyes().get(index), probe);
                    if (verdict == RayVerdict.UNKNOWN) {
                        unknown = true;
                    } else if (verdict == RayVerdict.CLEAR) {
                        return shift;
                    }
                }
            }
        }
        return aimed && !unknown ? 1.0 : -1.0;
    }

    public double exposure(WallCheckRequest job) {
        Box tight = job.tight();
        Vec3 centre = tight.centre();
        Box targetSpan = targetSpan(tight, centre);
        Vec3[] targets = null;
        double best = -1.0;
        for (Vec3 eye : job.eyes()) {
            double share;
            if (rays.provenClear(job.world(), eye, targetSpan)) {
                share = FULLY_EXPOSED;
            } else {
                if (targets == null) {
                    targets = exposureTargets(tight, centre);
                }
                share = eyeExposure(job.world(), eye, targets);
            }
            best = Math.max(best, share);
            if (best >= 1.0) {
                break;
            }
        }
        return best;
    }

    private double eyeExposure(UUID world, Vec3 eye, Vec3[] targets) {
        int visible = 0;
        for (Vec3 point : targets) {
            RayVerdict verdict = trace(world, eye, point);
            if (verdict == RayVerdict.UNKNOWN) {
                return -1.0;
            }
            if (verdict == RayVerdict.CLEAR) {
                visible++;
            }
        }
        return visible / EXPOSURE_POINTS;
    }

    private static Vec3[] exposureTargets(Box tight, Vec3 centre) {
        Vec3[] points = new Vec3[EXPOSURE_TARGETS];
        points[0] = centre;
        for (int corner = 0; corner < CORNERS; corner++) {
            double x = (corner & 1) == 0 ? tight.low().x() : tight.high().x();
            double y = (corner & 2) == 0 ? tight.low().y() : tight.high().y();
            double z = (corner & 4) == 0 ? tight.low().z() : tight.high().z();
            points[corner + 1] = new Vec3(inset(x, centre.x()), inset(y, centre.y()), inset(z, centre.z()));
        }
        return points;
    }

    private static Box targetSpan(Box tight, Vec3 centre) {
        double lowX = inset(tight.low().x(), centre.x());
        double lowY = inset(tight.low().y(), centre.y());
        double lowZ = inset(tight.low().z(), centre.z());
        double highX = inset(tight.high().x(), centre.x());
        double highY = inset(tight.high().y(), centre.y());
        double highZ = inset(tight.high().z(), centre.z());
        return new Box(lowest(lowX, highX, centre.x()), lowest(lowY, highY, centre.y()),
                lowest(lowZ, highZ, centre.z()), highest(lowX, highX, centre.x()), highest(lowY, highY, centre.y()),
                highest(lowZ, highZ, centre.z()));
    }

    private static double lowest(double first, double second, double third) {
        return Math.min(Math.min(first, second), third);
    }

    private static double highest(double first, double second, double third) {
        return Math.max(Math.max(first, second), third);
    }

    private static double inset(double edge, double centre) {
        return edge + Math.signum(centre - edge) * EDGE_PULLBACK;
    }

    private static List<Vec3> probes(Vec3 end, double shift, Box surface) {
        List<Vec3> points = new ArrayList<>(14);
        if (shift == 0.0) {
            points.add(end);
            return points;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int weight = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (weight == 1 || weight == 3) {
                        points.add(surface.clamp(new Vec3(end.x() + dx * shift, end.y() + dy * shift,
                                end.z() + dz * shift)));
                    }
                }
            }
        }
        return points;
    }

    private RayVerdict trace(UUID world, Vec3 from, Vec3 to) {
        if (Math.sqrt(from.squaredDistanceTo(to)) < SHORTEST_SEGMENT) {
            return RayVerdict.CLEAR;
        }
        return rays.trace(world, from, to);
    }
}
