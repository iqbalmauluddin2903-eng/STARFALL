package starfall.tasks;

import starfall.map.RoomId;

import java.util.ArrayList;
import java.util.List;

public final class TaskRegistry {

    private TaskRegistry() {}

    public static List<Task> createAllShipTasks() {
        List<Task> tasks = new ArrayList<>();

        tasks.add(new WiringTask("t_wiring", "task_wiring", RoomId.ELECTRICAL));
        tasks.add(new CardTask("t_card", "task_swipe_card", RoomId.ADMIN));
        tasks.add(new DownloadTask("t_download", "task_comms_dl", "Download Data", RoomId.COMMUNICATIONS));
        tasks.add(new DownloadTask("t_upload", "task_upload_admin", "Upload Data", RoomId.ADMIN));
        tasks.add(new AsteroidsTask("t_asteroids", "task_asteroids", RoomId.WEAPONS));
        tasks.add(new ReactorTask("t_reactor", "task_reactor", RoomId.REACTOR));

        tasks.add(new CalibrationTask("t_trash", "task_trash_cafe", "Empty Garbage", RoomId.CAFETERIA,
                "Activate all 4 airlock chute levers to eject waste."));
        tasks.add(new CalibrationTask("t_o2", "task_o2_filter", "Clean O2 Filter", RoomId.O2,
                "Purge clogged intake vents across all 4 O2 scrubbers."));
        tasks.add(new CalibrationTask("t_nav", "task_nav_steer", "Stabilize Steering", RoomId.NAVIGATION,
                "Lock orbital trajectory vectors for Navigation thrusters."));
        tasks.add(new CalibrationTask("t_shields", "task_shields", "Prime Shields", RoomId.SHIELDS,
                "Energize all 4 deflector shield hexagonal nodes."));
        tasks.add(new CalibrationTask("t_fuel", "task_fuel", "Fuel Engines", RoomId.STORAGE,
                "Pressurize fuel cells for Upper and Lower Engines."));
        tasks.add(new CalibrationTask("t_align", "task_align_lower", "Align Engine Output", RoomId.LOWER_ENGINE,
                "Synchronize thrust output harmonics on Lower Engine."));
        tasks.add(new CalibrationTask("t_manifolds", "task_manifolds", "Unlock Manifolds", RoomId.UPPER_ENGINE,
                "Release pressure locks on Upper Engine manifolds."));
        tasks.add(new CalibrationTask("t_scan", "task_med_scan", "Submit Scan", RoomId.MEDBAY,
                "Complete full biometric crew health diagnostics."));

        return tasks;
    }
}