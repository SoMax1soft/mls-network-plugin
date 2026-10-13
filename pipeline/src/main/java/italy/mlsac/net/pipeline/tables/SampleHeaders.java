// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

public final class SampleHeaders {
    public static final String SAMPLES = "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,angle_to_target,"
            + "aim_alignment,target_angular_velocity,aim_lead,ticks_since_attack,speed_xz,fall_distance,dt_ms,"
            + "jerk_yaw,jerk_pitch,gcd_error_yaw,gcd_error_pitch,abs_delta_yaw,abs_delta_pitch,smooth_yaw,"
            + "smooth_pitch,speed_yaw,speed_pitch,total_speed,std_yaw,std_pitch,dir_changes_yaw,dir_changes_pitch,"
            + "snap_ratio_yaw,snap_ratio_pitch,constancy_yaw,constancy_pitch,avg_aim_angle,pitch_abs,"
            + "aim_lead_pitch,ray_hit,hitbox_margin_degrees,target_time_since_acquired_ms,target_switch_count,"
            + "overshoot_count,lock_fraction,target_speed_xz,target_yaw_rate,target_pitch_rate,time_in_aim_ms,"
            + "attack_cooldown,tick_index,segment_id,server_tick,elapsed_ns,target_valid,target_distance,"
            + "target_id,target_uuid,target_source,target_changed,rotation_valid,angular_velocity_valid,"
            + "context_valid,self_snapshot_age_ms,target_snapshot_age_ms,yaw,pitch,position_x,position_y,"
            + "position_z,target_x,target_y,target_z,target_height,target_width,rotation_changed,"
            + "position_changed,packet_cancelled,attack_count,ms_since_attack";
    public static final String ATTACKS = "is_cheating,elapsed_ns,server_tick,segment_id,attacked_entity_id,"
            + "packet_cancelled,attack_interval_ms,rotation_age_ms,yaw,pitch,target_valid,target_uuid,"
            + "angle_to_target,aim_alignment,aim_lead,aim_lead_pitch,target_distance,target_snapshot_age_ms,"
            + "ray_hit,hitbox_margin_degrees,time_in_aim_ms,attack_cooldown";

    private SampleHeaders() {
    }
}
