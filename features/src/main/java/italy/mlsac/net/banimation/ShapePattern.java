// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;

public enum ShapePattern {
    POINT {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            return List.of(new Emission(center, count));
        }
    },
    RISING_SPIRAL {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            double angle = Math.toRadians((tick * 20) % 360);
            return List.of(new Emission(ring(center, angle, radius), count));
        }
    },
    SPHERE {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            int points = Math.max(1, count);
            List<Emission> emissions = new ArrayList<>(points);
            for (int i = 0; i < points; i++) {
                double phi = Math.acos(1 - 2.0 * i / points);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + Math.toRadians(4 * tick);
                emissions.add(new Emission(center.plus(radius * Math.sin(phi) * Math.cos(theta),
                        radius * Math.cos(phi), radius * Math.sin(phi) * Math.sin(theta)), 1));
            }
            return emissions;
        }
    },
    EXPLOSION {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            List<Emission> emissions = new ArrayList<>(Math.max(0, count));
            for (int i = 0; i < count; i++) {
                Point direction = new Point(signed(random), signed(random), signed(random));
                double length = direction.length();
                Point unit = length == 0 ? direction : direction.times(1.0 / length);
                emissions.add(new Emission(center.plus(unit.times(radius)), count));
            }
            return emissions;
        }
    },
    HELIX {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            double angle = Math.toRadians((tick * 15) % 360);
            return List.of(new Emission(ring(center, angle, radius), count),
                    new Emission(ring(center, angle + Math.PI, radius), count));
        }
    },
    CIRCLE {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            int points = Math.max(8, count);
            List<Emission> emissions = new ArrayList<>(points);
            for (int i = 0; i < points; i++) {
                emissions.add(new Emission(ring(center, 2 * Math.PI * i / points + Math.toRadians(2 * tick), radius), 1));
            }
            return emissions;
        }
    },
    NONE {
        @Override
        public List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random) {
            return List.of();
        }
    };

    public static ShapePattern named(String raw) {
        String name = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (ShapePattern shape : values()) {
            if (shape != NONE && shape.name().equals(name)) {
                return shape;
            }
        }
        return NONE;
    }

    public abstract List<Emission> emit(Point center, double radius, int count, long tick, DoubleSupplier random);

    private static Point ring(Point center, double angle, double radius) {
        return center.plus(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static double signed(DoubleSupplier random) {
        return random.getAsDouble() * 2 - 1;
    }
}
