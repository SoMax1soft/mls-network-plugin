// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import java.util.List;

public final class TableColumns {
    public static final List<String> TICK = List.of("row", "segment_id", "server_tick", "elapsed_ns", "dt_ms",
            "tick_ends", "in_combat", "ticks_since_attack", "ms_since_attack", "rotation_present",
            "position_present", "on_ground", "cancelled", "echo", "no_packet", "yaw", "pitch", "rot_valid",
            "delta_yaw", "delta_pitch", "accel_yaw", "accel_pitch", "jerk_yaw", "jerk_pitch", "mode_yaw",
            "mode_pitch", "mode_yaw_valid", "mode_pitch_valid", "dots_yaw", "dots_pitch", "gcd_error_yaw",
            "gcd_error_pitch", "move_valid", "move_x", "move_y", "move_z", "attacks", "player_attacks", "swings",
            "air_swings", "slot_changes", "rows_since_swing", "self_valid", "attack_cooldown", "fall_distance",
            "sprinting", "sneaking", "weapon", "held_item", "attack_speed", "game_mode", "reach", "target_valid",
            "target_source", "target_changed", "target_held_ms", "targets_in_range", "comp_mode", "ping_ms",
            "comp_outstanding", "ray_hit", "ray_hit_strict", "rays_hit", "primary_hit", "hit_rel_x", "hit_rel_y",
            "hit_rel_z", "miss_distance", "aim_error", "aim_error_yaw", "aim_error_pitch", "inflation",
            "ray_occluded", "center_occluded", "exposure", "target_distance", "angle_to_target", "aim_lead",
            "aim_lead_pitch", "target_rel_x", "target_rel_y", "target_rel_z", "on_target_ticks", "on_target_ms");
    public static final List<String> CLICK = List.of("row", "segment_id", "server_tick", "elapsed_ns",
            "resolve_ms", "rotation_source", "entity_id", "cancelled", "target_is_player", "target_is_view_target",
            "target_valid", "comp_mode", "ping_ms", "comp_outstanding", "ray_hit", "ray_hit_strict", "rays_hit",
            "primary_hit", "hit_rel_x", "hit_rel_y", "hit_rel_z", "miss_distance", "aim_error", "aim_error_yaw",
            "aim_error_pitch", "inflation", "ray_occluded", "center_occluded", "exposure", "wall_margin",
            "target_distance", "angle_to_target", "aim_lead", "aim_lead_pitch", "on_target_ticks", "on_target_ms",
            "ms_since_last_attack", "rows_since_last_attack", "swings_since_last_attack",
            "air_swings_since_last_attack", "slot_changes_since_last_attack", "rows_since_swing", "ms_since_swing",
            "weapon", "held_item", "attack_speed", "game_mode", "reach", "reach_distance", "attack_cooldown",
            "fall_distance", "on_ground", "sprinting", "sneaking", "climbing", "in_water", "blind", "riding",
            "is_critical", "yaw", "pitch", "delta_yaw", "delta_pitch");

    public static final String TICK_HEADER = "is_cheating," + String.join(",", TICK);
    public static final String CLICK_HEADER = "is_cheating," + String.join(",", CLICK);

    private TableColumns() {
    }
}
