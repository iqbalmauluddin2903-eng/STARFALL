package starfall.gameplay;

import java.awt.*;
import java.util.*;
import java.util.List;
import starfall.core.InputManager;
import starfall.entities.Crewmate;
import starfall.entities.Player;
import starfall.map.GameMap;
import starfall.map.InteractiveObject;
import starfall.tasks.Task;
import starfall.tasks.TaskRegistry;
import starfall.utils.Logger;

public class TaskManager {

    private final List<Task> assignedTasks = new ArrayList<>();
    private final Map<String, Task> taskByInteractable = new HashMap<>();
    private Task activeTask = null;

    private int totalBotTasks = 0;
    private int completedBotTasks = 0;
    private Float networkTaskProgressOverride = null;
    private String lastCompletedTaskId = null;

    public void assignTasksForMatch(GameMap map, int count, int aiCrewmateCount) {
        assignedTasks.clear();
        taskByInteractable.clear();
        activeTask = null;
        totalBotTasks = Math.max(0, aiCrewmateCount * 4);
        completedBotTasks = 0;
        networkTaskProgressOverride = null;
        lastCompletedTaskId = null;

        List<Task> pool = TaskRegistry.createAllShipTasks();
        Collections.shuffle(pool);

        int limit = Math.min(count, pool.size());
        for (int i = 0; i < limit; i++) {
            Task t = pool.get(i);
            assignedTasks.add(t);
            taskByInteractable.put(t.getInteractableId(), t);
        }

        for (InteractiveObject obj : map.getAllInteractables()) {
            if (obj.getType() == InteractiveObject.InteractionType.TASK_TERMINAL) {
                obj.setCompleted(!taskByInteractable.containsKey(obj.getId()));
            }
        }
    }

    public boolean tryOpenTaskForInteractable(String interactableId) {
        Task task = taskByInteractable.get(interactableId);
        if (task != null && !task.isCompleted()) {
            activeTask = task;
            task.open();
            Logger.info("Opened task mini-game: " + task.getName());
            return true;
        }
        return false;
    }

    public void recordExternalTaskCompletion(Player botPlayer, Task task) {
        if (task == null || task.isCompleted()) return;
        forceCompleteTask(task);
        completedBotTasks = Math.min(totalBotTasks, completedBotTasks + 1);
        if (botPlayer.getCharacter() instanceof Crewmate crew) {
            crew.incrementCompletedTasks();
        }
        Logger.info("AI Crewmate " + botPlayer.getCharacter().getName() + " completed task: " + task.getName());
    }

    private void forceCompleteTask(Task task) {
        task.close();
        try {
            java.lang.reflect.Method m = Task.class.getDeclaredMethod("markCompleted");
            m.setAccessible(true);
            m.invoke(task);
        } catch (Exception e) {
            Logger.error("Failed to mark bot task complete", e);
        }
    }

    public void update(double dt, InputManager input, int screenW, int screenH, Player localPlayer, GameMap map) {
        if (activeTask == null) return;

        activeTask.update(dt, input, screenW, screenH);
        if (activeTask.isCompleted()) {
            Logger.info("Completed task: " + activeTask.getName());
            lastCompletedTaskId = activeTask.getTaskId();
            if (localPlayer.getCharacter() instanceof Crewmate crewmate) {
                crewmate.incrementCompletedTasks();
            }
            for (InteractiveObject obj : map.getAllInteractables()) {
                if (obj.getId().equals(activeTask.getInteractableId())) {
                    obj.setCompleted(true);
                    break;
                }
            }
            activeTask = null;
        } else if (!activeTask.isActive()) {
            activeTask = null;
        }
    }

    public String consumeLastCompletedTaskId() {
        String id = lastCompletedTaskId;
        lastCompletedTaskId = null;
        return id;
    }

    public void setNetworkTaskProgress(Float progress) {
        this.networkTaskProgressOverride = progress;
    }

    public void closeActiveTask() {
        if (activeTask != null) {
            activeTask.close();
            activeTask = null;
        }
    }

    public void renderActiveTask(Graphics2D g2, int screenW, int screenH) {
        if (activeTask != null && activeTask.isActive()) {
            activeTask.render(g2, screenW, screenH);
        }
    }

    public float getGlobalTaskProgress() {
        if (networkTaskProgressOverride != null) {
            return networkTaskProgressOverride;
        }

        int localTotal = assignedTasks.size();
        int localDone = 0;
        for (Task t : assignedTasks) {
            if (t.isCompleted()) localDone++;
        }

        int grandTotal = localTotal + totalBotTasks;
        if (grandTotal <= 0) return 1.0f;
        return (localDone + completedBotTasks) / (float) grandTotal;
    }

    public boolean isAllTasksCompleted() {
        if (networkTaskProgressOverride != null) {
            return networkTaskProgressOverride >= 0.999f;
        }
        for (Task t : assignedTasks) {
            if (!t.isCompleted()) return false;
        }
        return completedBotTasks >= totalBotTasks && (!assignedTasks.isEmpty() || totalBotTasks > 0);
    }

    public boolean hasActiveTaskOpen() { return activeTask != null && activeTask.isActive(); }
    public List<Task> getAssignedTasks() { return Collections.unmodifiableList(assignedTasks); }
}