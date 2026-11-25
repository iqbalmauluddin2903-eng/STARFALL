package starfall.map;

import java.awt.*;

public class Wall {

    public enum PropStyle {
        CAFETERIA_TABLE,
        MEDBAY_SCANNER,
        REACTOR_CORE,
        ENGINE_TURBINE,
        WEAPONS_CHAIR,
        NAVIGATION_CONSOLE,
        O2_FILTER,
        SHIELDS_ARRAY,
        COMMS_TOWER,
        STORAGE_CRATE,
        ADMIN_TABLE,
        ELECTRICAL_RACK,
        SECURITY_DESK
    }

    private final Rectangle bounds;
    private final PropStyle style;
    private final boolean solid;
    private final Color primaryColor;

    public Wall(Rectangle bounds, PropStyle style, boolean solid, Color primaryColor) {
        this.bounds = bounds;
        this.style = style;
        this.solid = solid;
        this.primaryColor = primaryColor;
    }

    public Rectangle getBounds() { return bounds; }
    public PropStyle getStyle() { return style; }
    public boolean isSolid() { return solid; }
    public Color getPrimaryColor() { return primaryColor; }
}