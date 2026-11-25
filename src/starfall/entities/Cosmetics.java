package starfall.entities;

public final class Cosmetics {

    private Cosmetics() {}

    public enum Hat {
        NONE("No Hat"),
        CROWN("Royal Crown"),
        ASTRONAUT("Space Helmet"),
        COWBOY("Cowboy Hat"),
        VIKING("Viking Horns"),
        BEANIE("Red Beanie"),
        LEAF("Sprout Leaf");

        private final String displayName;

        Hat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        public Hat next() {
            Hat[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum Skin {
        DEFAULT("Standard Suit"),
        LAB_COAT("MedBay Lab Coat"),
        CAPTAIN("Captain Uniform"),
        MECHANIC("Engineer Overalls"),
        SECURITY("Security Vest");

        private final String displayName;

        Skin(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        public Skin next() {
            Skin[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum Pet {
        NONE("No Pet"),
        MINI_CREW("Mini Crewmate"),
        UFO_DRONE("UFO Drone"),
        SPACE_DOG("Astro Pup");

        private final String displayName;

        Pet(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        public Pet next() {
            Pet[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}