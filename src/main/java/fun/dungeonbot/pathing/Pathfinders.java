package fun.dungeonbot.pathing;

public final class Pathfinders {
    private static Pathfinder instance;

    private Pathfinders() {
    }

    public static Pathfinder get() {
        if (instance != null) return instance;
        if (!isBaritonePresent()) return null;

        instance = new BaritonePathfinder();
        return instance;
    }

    public static boolean isBaritonePresent() {
        try {
            Class.forName("baritone.api.BaritoneAPI", false, Pathfinders.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
