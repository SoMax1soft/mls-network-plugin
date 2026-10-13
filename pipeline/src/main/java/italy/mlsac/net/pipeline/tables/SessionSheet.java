// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import italy.mlsac.net.pipeline.json.PrettyJsonDocument;
import java.util.ArrayList;
import java.util.List;

public final class SessionSheet {
    public static final List<String> OWN_WORDING_KEYS = List.of("sampling", "clock", "normalization",
            "target_angular_velocity_unit", "aim_lead_definition", "extra_feature_definitions", "target_point",
            "v2_sampling", "v2_attack_rotation", "v2_no_packet", "v2_target", "v2_rotation", "v2_weapon", "v2_reach",
            "v2_walls", "v2_units");

    private static final List<String> FEATURE_COLUMNS = List.of("delta_yaw", "delta_pitch", "accel_yaw",
            "accel_pitch", "angle_to_target", "aim_alignment", "target_angular_velocity", "aim_lead",
            "ticks_since_attack", "speed_xz", "fall_distance", "dt_ms");
    private static final List<String> EXTRA_FEATURE_COLUMNS = List.of("jerk_yaw", "jerk_pitch", "gcd_error_yaw",
            "gcd_error_pitch", "abs_delta_yaw", "abs_delta_pitch", "smooth_yaw", "smooth_pitch", "speed_yaw",
            "speed_pitch", "total_speed", "std_yaw", "std_pitch", "dir_changes_yaw", "dir_changes_pitch",
            "snap_ratio_yaw", "snap_ratio_pitch", "constancy_yaw", "constancy_pitch", "avg_aim_angle", "pitch_abs",
            "aim_lead_pitch", "ray_hit", "hitbox_margin_degrees", "target_time_since_acquired_ms",
            "target_switch_count", "overshoot_count", "lock_fraction", "target_speed_xz", "target_yaw_rate",
            "target_pitch_rate", "time_in_aim_ms", "attack_cooldown");

    private SessionSheet() {
    }

    public static String render(SessionFacts facts) {
        List<String> allFeatures = new ArrayList<>(FEATURE_COLUMNS);
        allFeatures.addAll(EXTRA_FEATURE_COLUMNS);
        PrettyJsonDocument document = new PrettyJsonDocument()
                .text("schema_version", "devultra-v2")
                .text("v2_schema", "ultra-v2")
                .text("client_version", facts.clientLabel())
                .text("session_id", facts.sessionKey().toString())
                .text("player_uuid", facts.player().toString())
                .text("player_name", facts.playerNick())
                .text("label", facts.label())
                .text("comment", facts.comment());
        for (SessionNote note : facts.notes()) {
            document.text(note.key(), note.value());
        }
        return document
                .text("started_at_utc", facts.beganAt().toString())
                .text("saved_at_utc", facts.savedAt().toString())
                .textList("feature_cols", FEATURE_COLUMNS)
                .textList("extra_feature_cols", EXTRA_FEATURE_COLUMNS)
                .textList("all_feature_cols", allFeatures)
                .text("sampling", "one sample per rotation packet, taken only inside an attack window; a packet "
                        + "that leaves both yaw and pitch unchanged adds nothing")
                .bool("idle_samples_saved", false)
                .integer("combat_timeout_ticks", 80)
                .text("clock", "elapsed_ns = nanoseconds on the monotonic JVM clock from the start of this recording "
                        + "to the moment the server took the packet in")
                .text("normalization", "none: every value is stored unscaled, the training side does its own log "
                        + "transform and scaling")
                .text("speed_xz_unit", "blocks_per_second")
                .text("target_angular_velocity_unit", "degrees per second: how fast the signed angle error changes")
                .text("aim_lead_definition", "yaw of the packet minus the yaw that would point at the target, folded "
                        + "into a single turn, in degrees")
                .text("extra_feature_definitions", "computed causally over the last 20 rotation packets: jerk, abs, speed, "
                        + "std and constancy; snap_ratio = share of those packets turning faster than 180 degrees "
                        + "per second; ray_hit = server-side box ray check; lock_fraction counts packets within 3 "
                        + "degrees of the target; target motion comes from two successive server snapshots")
                .text("target_point", "middle of the target's body at the server's own position, not rewound to "
                        + "what the client was shown")
                .integer("target_max_distance_blocks", 8)
                .integer("candidate_fov_degrees", 90)
                .integer("target_memory_ms", 4000)
                .integer("snapshot_max_age_ms", 250)
                .integer("segment_gap_ms", 1000)
                .integer("samples", facts.samples())
                .integer("attacks", facts.attacks())
                .textList("v2_tick_cols", TableColumns.TICK)
                .textList("v2_hit_cols", TableColumns.CLICK)
                .integer("v2_ticks", facts.ticks())
                .integer("v2_hits", facts.hits())
                .text("v2_sampling", "a row for each movement packet as long as the latest hit on a player happened "
                        + "no more than 80 server ticks ago, led by the 40 packets that came right before the first "
                        + "such hit (those rows have in_combat=0); hitting a mob or an unidentified entity only raises "
                        + "the attacks counter and never opens a fight, player_attacks counts the hits on players")
                .text("v2_attack_rotation", "an attack is measured with the rotation and position of the next "
                        + "movement packet, with entities placed as the client saw them on that tick; "
                        + "rotation_source=2: the tick ended with no movement packet, so the view had not moved; "
                        + "0: cut off by a break, last known rotation")
                .text("v2_no_packet", "no_packet=1 rows are ticks a 1.21.2+ client ended without a movement "
                        + "packet: nothing moved. Older clients send nothing on such ticks and get no row, and "
                        + "their tick_ends is -1: tick ends synthesised by protocol translation layers are ignored")
                .text("v2_target", "comp_mode 1: boxes rebuilt from entity packets the client acknowledged "
                        + "(transaction pings); 2: server history rewound by ping; 0: none")
                .text("v2_rotation", "mode = estimated mouse step (sensitivity), skipping turns finer than float "
                        + "resolution and the first implausible divisor; dots = |delta| divided by mode; mode, dots "
                        + "and gcd_error stay 0 while the matching *_valid flag is 0")
                .text("v2_weapon", "weapon codes = unknown 0, bare hand 1, sword 2, axe 3, mace 4, trident 5, spear "
                        + "6, pickaxe 7, shovel 8, hoe 9, bow or crossbow 10, shield 11, anything else 12; held_item "
                        + "names the material; attack_speed = how many attacks per second the server allows (20 "
                        + "divided by the cooldown in ticks), or -1 if that is not known")
                .text("v2_reach", "game_mode codes: 0 survival, 1 creative, 2 adventure, 3 spectator. reach = attack "
                        + "range granted by the server (entity interaction range attribute; vanilla gives 3 in "
                        + "survival and 5 in creative). reach_distance (hits) = smallest eye-to-box distance over "
                        + "both tick ends and the three eye heights, measured to the possible box; a legit hit "
                        + "stays within reach")
                .text("v2_walls", "server blocks, outline shapes as the client's crosshair pick uses, fluids "
                        + "ignored, checked one tick after the aim from both ends of the tick (this packet's "
                        + "position and the previous one); the possible box is taken standing-tall (a crouch at "
                        + "most). ray_occluded (ticks): the crosshair ray to the possible box is cut from both (-1 "
                        + "when the crosshair was not on the target). ray_occluded (hits): 1 when no eye sees the "
                        + "target within 0.1 blocks of the crosshair's point on it; wall_margin (hits): the first "
                        + "of [0.0, 0.03, 0.1, 0.3] blocks from that point at which the target shows, 1.0 when it "
                        + "does not, -1 unknown. center_occluded: eye to target centre cut from both; exposure: "
                        + "share of 9 target points (centre and 8 corners) visible from the better eye; -1 when not "
                        + "checked")
                .text("v2_units", "units: angles in degrees, distances in blocks, *_ms in milliseconds; aim_error* "
                        + "is scaled by the target's angular half-size; hit_rel_* is measured from the centre of the "
                        + "tight box")
                .render();
    }
}
