// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.compensation.LensMode;
import italy.mlsac.net.pipeline.compensation.LensReading;
import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.HitMeasurement;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.rotation.RotationStep;
import italy.mlsac.net.pipeline.rows.AttackHistory;
import italy.mlsac.net.pipeline.rows.AttackerState;
import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.ClickTarget;
import italy.mlsac.net.pipeline.rows.ClickTiming;
import italy.mlsac.net.pipeline.rows.InputCounts;
import italy.mlsac.net.pipeline.rows.MoveDelta;
import italy.mlsac.net.pipeline.rows.OnTarget;
import italy.mlsac.net.pipeline.rows.PacketTraits;
import italy.mlsac.net.pipeline.rows.RowTiming;
import italy.mlsac.net.pipeline.rows.SelfState;
import italy.mlsac.net.pipeline.rows.TargetView;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.HashMap;
import java.util.Map;

final class StoredRows {
    private static final Box ANY_BOX = Box.at(Vec3.ZERO);

    private final Map<String, Integer> columns = new HashMap<>();
    private String[] fields;

    StoredRows(String header) {
        String[] names = header.split(",", -1);
        for (int index = 0; index < names.length; index++) {
            columns.put(names[index], index);
        }
    }

    StoredRows line(String line) {
        fields = line.split(",", -1);
        return this;
    }

    private String text(String column) {
        return fields[columns.get(column)];
    }

    private double real(String column) {
        return Double.parseDouble(text(column));
    }

    private float single(String column) {
        return Float.parseFloat(text(column));
    }

    private long whole(String column) {
        return Long.parseLong(text(column));
    }

    private int integer(String column) {
        return Integer.parseInt(text(column));
    }

    private boolean flag(String column) {
        return "1".equals(text(column));
    }

    private TargetMeasurement measurement() {
        boolean valid = flag("target_valid");
        return new TargetMeasurement(
                new LensReading(LensMode.values()[modeIndex(integer("comp_mode"))], real("ping_ms"),
                        integer("comp_outstanding"), valid ? ANY_BOX : null, valid ? ANY_BOX : null),
                new HitMeasurement(valid, flag("ray_hit"), flag("ray_hit_strict"), integer("rays_hit"),
                        flag("primary_hit"), new Vec3(real("hit_rel_x"), real("hit_rel_y"), real("hit_rel_z")),
                        real("miss_distance"), real("aim_error"), real("aim_error_yaw"), real("aim_error_pitch"),
                        real("inflation"), real("target_distance")));
    }

    private static int modeIndex(int code) {
        for (LensMode mode : LensMode.values()) {
            if (mode.code() == code) {
                return mode.ordinal();
            }
        }
        throw new IllegalArgumentException("comp_mode " + code);
    }

    TickRow tick() {
        return new TickRow(
                new RowTiming(whole("row"), whole("segment_id"), whole("server_tick"), whole("elapsed_ns"),
                        real("dt_ms"), integer("tick_ends"), flag("in_combat"), integer("ticks_since_attack"),
                        real("ms_since_attack")),
                new PacketTraits(flag("rotation_present"), flag("position_present"), flag("on_ground"),
                        flag("cancelled"), flag("echo"), flag("no_packet"), single("yaw"), single("pitch")),
                new RotationStep(flag("rotation_present"), flag("rot_valid"), real("delta_yaw"), real("delta_pitch"),
                        real("accel_yaw"), real("accel_pitch"), real("jerk_yaw"), real("jerk_pitch"),
                        real("mode_yaw"), real("mode_pitch"), flag("mode_yaw_valid"), flag("mode_pitch_valid"),
                        real("dots_yaw"), real("dots_pitch"), real("gcd_error_yaw"), real("gcd_error_pitch")),
                new MoveDelta(flag("move_valid"), real("move_x"), real("move_y"), real("move_z")),
                new InputCounts(integer("attacks"), integer("player_attacks"), integer("swings"),
                        integer("air_swings"), integer("slot_changes"), integer("rows_since_swing")),
                new SelfState(flag("self_valid"), real("attack_cooldown"), real("fall_distance"), flag("sprinting"),
                        flag("sneaking"), integer("weapon"), text("held_item"), real("attack_speed"),
                        integer("game_mode"), real("reach")),
                new TargetView(integer("target_source"), flag("target_changed"), real("target_held_ms"),
                        integer("targets_in_range")),
                measurement(),
                OcclusionCell.of(integer("ray_occluded"), integer("center_occluded"), real("exposure"), -1.0),
                new AimRelation(real("angle_to_target"), real("aim_lead"), real("aim_lead_pitch"),
                        new Vec3(real("target_rel_x"), real("target_rel_y"), real("target_rel_z"))),
                new OnTarget(integer("on_target_ticks"), real("on_target_ms")));
    }

    ClickRow click() {
        return new ClickRow(
                new ClickTiming(whole("row"), whole("segment_id"), whole("server_tick"), whole("elapsed_ns"),
                        real("resolve_ms"), integer("rotation_source")),
                new ClickTarget(integer("entity_id"), flag("cancelled"), flag("target_is_player"),
                        flag("target_is_view_target")),
                measurement(),
                OcclusionCell.of(integer("ray_occluded"), integer("center_occluded"), real("exposure"),
                        real("wall_margin")),
                new AimRelation(real("angle_to_target"), real("aim_lead"), real("aim_lead_pitch"), Vec3.ZERO),
                new OnTarget(integer("on_target_ticks"), real("on_target_ms")),
                new AttackHistory(real("ms_since_last_attack"), integer("rows_since_last_attack"),
                        integer("swings_since_last_attack"), integer("air_swings_since_last_attack"),
                        integer("slot_changes_since_last_attack"), integer("rows_since_swing"),
                        real("ms_since_swing")),
                new AttackerState(integer("weapon"), text("held_item"), real("attack_speed"), integer("game_mode"),
                        real("reach"), real("attack_cooldown"), real("fall_distance"), flag("on_ground"),
                        new PlayerPosture(flag("sprinting"), flag("sneaking"), flag("climbing"), flag("in_water"),
                                flag("blind"), flag("riding")),
                        flag("is_critical")),
                real("reach_distance"), single("yaw"), single("pitch"), real("delta_yaw"), real("delta_pitch"));
    }
}
