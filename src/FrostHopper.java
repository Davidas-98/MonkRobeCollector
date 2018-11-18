import org.osbot.rs07.api.filter.Filter;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.input.mouse.RectangleDestination;
import org.osbot.rs07.input.mouse.WidgetDestination;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.Condition;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.osbot.rs07.script.MethodProvider.random;
import static org.osbot.rs07.script.MethodProvider.sleep;

/**
 * Tool for instant world hopping
 *
 * @author FrostBug
 */
public class FrostHopper {

    private final Script parent;

    public static enum HopMode {

        ANY(-1),
        F2P(1130);

        private final int spriteID;

        private HopMode(int spriteID) {
            this.spriteID = spriteID;
        }

        public int getSpriteID() {
            return this.spriteID;
        }
    }


    private final static int WIDGET_INDEX_ROOT = 15;
    private final static int WIDGET_CONTENT_ROOT = 8;
    private final static int WIDGET_ROOT = 69;
    private final static int WIDGET_SCROLL = 16;
    private final static int WIDGET_SCROLL_UP = 4;
    private final static int WIDGET_SCROLL_DOWN = 5;
    private final static int WIDGET_WORLD = 0;
    private final static int WIDGET_WORLDTYPE = 1;
    private final static int WIDGET_WORLDNUMBER = 2;
    private final static int WIDGET_PLAYERCOUNT = 4;
    private final static int WIDGET_WORLDACTIVITY = 5;
    private final static int WIDGET_OFFSET = 6;

    private final static int MIN_FRAME_Y = 230;
    private final static int MAX_FRAME_Y = 416;

    private final List<Integer> disabled;
    private HopMode hopMode;
    private int sourceWorld;
    private RS2Widget destinationWorld;
    private int destW;
    private int attempts;

    public FrostHopper(Script parent) {
        this.parent = parent;
        this.hopMode = HopMode.F2P;
        this.sourceWorld = -1;
        this.attempts = 0;
        this.disabled = Arrays.asList(25, 29, 37, 65, 21, 49);
    }

    public void hop(HopMode hopMode) {
        this.hopMode = hopMode;
        this.sourceWorld = parent.getWorlds().getCurrentWorld();
        this.destinationWorld = null;
        this.destW = -1;
        this.parent.log("[FrostHopper] Starting quickhop");
        this.attempts = 0;
    }

    private void finishHop() {
        parent.log("[FrostHopper] Hopped successfully");
        this.hopMode = HopMode.F2P;
        this.sourceWorld = -1;
        this.destinationWorld = null;
        this.destW = -1;
        this.attempts = 0;
    }

    public boolean isHopping() {
        return this.hopMode == HopMode.F2P;
    }

    public int execute() throws InterruptedException {
        int delay = random(30, 60);

        if (getCurrentWorld() != getSourceWorld()) {
            if (destW != getCurrentWorld()) {
                parent.log("[FrostHopper] ERROR: Incorrect destination world. Hopping again");
                this.destinationWorld = null;
                this.destW = -1;
                this.sourceWorld = parent.getWorlds().getCurrentWorld();
                this.attempts = 0;
            } else {
                finishHop();
                return random(4000, 5000);
            }
        }

        if (parent.getDialogues().isPendingOption()
                && (!parent.getWidgets().containingText("Switch to World " + destW).isEmpty()
                || !parent.getWidgets().containingText("World " + destW + " is a High Risk world!").isEmpty())){
            if (parent.getDialogues().selectOption("Yes.", "Switch to the High Risk world.")) {
                delay = random(3000, 4000);
            }
        } else if (this.parent.getTabs().getOpen() == Tab.LOGOUT) {
            if (isQuickhopOpen()) {
                if (!parent.getWidgets().containingText("Loading...").isEmpty()) {
                    parent.log("Loading worlds..");
                    return random(1000, 1500);
                }
                if (this.destinationWorld == null || attempts >= 3) {
                    attempts = 0;
                    RS2Widget dest = pickWorld();
                    if (dest != null) {
                        parent.log("[FrostHopper] Hopping to world: " + getWorldNumber(dest));
                        this.destinationWorld = dest;
                        this.destW = getWorldNumber(dest);
                    } else {
                        parent.log("[FrostHopper] Failed to select world");
                    }
                } else {
                    if (isSelectable(this.destinationWorld)) {
                        attempts = 0;
                        parent.log("Interacting with world widget..");
                        Rectangle rect = this.destinationWorld.getBounds();
                        rect.translate(0, 2);
                        rect.setSize((int) rect.getWidth(), (int) rect.getHeight() - 4);
                        RectangleDestination dest = new RectangleDestination(parent.getBot(), rect);
                        if (parent.getMouse().move(dest)) {
                            sleep(random(100, 150));
                            parent.getMouse().click(false);
                            delay = random(800, 1100);
                        }
                    } else {
                        attempts++;
                        scrollToWidget(this.destinationWorld);
                        delay = random(200, 300);
                    }
                }
            } else if (openQuickhop()) {
                delay = random(800, 1200);
            }
        } else if (this.parent.getTabs().open(Tab.LOGOUT)) {
            delay = random(200, 300);
        }
        return delay;
    }

    private int getWorldWidgetIndex(RS2Widget widget) {
        int index = 0;
        for (int i = 300; i < 401; i++) {
            RS2Widget w = parent.getWidgets().get(WIDGET_ROOT, WIDGET_INDEX_ROOT, i);
            if (w != null && !w.getHiddenUntilMouseOver()) {
                if (i == widget.getThirdLevelId()) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    private boolean isQuickhopOpen() {
        return parent.getTabs().getOpen() == Tab.LOGOUT
                && parent.getWidgets().containingText("World Switcher").isEmpty();
    }

    private boolean openQuickhop() {
        List<RS2Widget> list = parent.getWidgets().containingText("World Switcher");
        if (!list.isEmpty()) {
            return list.get(0).interact();
        }
        return false;
    }


    private RS2Widget pickWorld() {
        List<RS2Widget> list = (List<RS2Widget>) parent.getWidgets().filter(WIDGET_ROOT, (Filter<RS2Widget>) (RS2Widget w) -> {
            if (!w.isThirdLevel()
                    || w.getSecondLevelId() != WIDGET_INDEX_ROOT
                    || w.getThirdLevelId() <= 300
                    || w.getThirdLevelId() > 400
                    || w.getHiddenUntilMouseOver()) {
                return false;
            }
            int worldNumber = trimWorldNumber(w.getThirdLevelId());
            int index = getWorldWidgetIndex(w);
            RS2Widget world = parent.getWidgets().get(WIDGET_ROOT, WIDGET_CONTENT_ROOT, WIDGET_WORLD + (index * WIDGET_OFFSET));
            RS2Widget worldType = parent.getWidgets().get(WIDGET_ROOT, WIDGET_CONTENT_ROOT, WIDGET_WORLDTYPE + (index * WIDGET_OFFSET));
            RS2Widget worldActivity = parent.getWidgets().get(WIDGET_ROOT, WIDGET_CONTENT_ROOT, WIDGET_WORLDACTIVITY + (index * WIDGET_OFFSET));
            RS2Widget worldCount = parent.getWidgets().get(WIDGET_ROOT, WIDGET_CONTENT_ROOT, WIDGET_PLAYERCOUNT + (index * WIDGET_OFFSET));
            String activity = worldActivity.getMessage() == null ? "" : worldActivity.getMessage().toLowerCase();

            boolean accessible = world.getTextColor() != 0;
            boolean notOwn = (worldNumber != getCurrentWorld());
            boolean correctType = this.hopMode == HopMode.ANY
                    || (worldType.getSpriteIndex1() == this.hopMode.getSpriteID());
            boolean nonPvP = (!activity.contains("pvp"));
            boolean restricted = disabled.contains(worldNumber);
            boolean deadman = (activity.contains("deadman"));
            boolean skill = activity.contains("king of");
            boolean full = Integer.parseInt(worldCount.getMessage()) >= 1980;

            return accessible && notOwn && correctType && nonPvP && !deadman && !restricted && !full && !skill;
        });
        return list.isEmpty() ? null : list.get(random(list.size()));
    }

    private int trimWorldNumber(int number) {
        return (number > 300) ? number - 300 : number;
    }

    private int getSourceWorld() {
        return trimWorldNumber(this.sourceWorld);
    }

    private int getCurrentWorld() {
        return trimWorldNumber(parent.getWorlds().getCurrentWorld());
    }

    private int getWorldNumber(RS2Widget widget) {
        return trimWorldNumber(widget.getThirdLevelId());
    }

    private void scrollToWidget(RS2Widget widget) {
        RS2Widget scroll;
        if (widget.getPosition().getY() < MIN_FRAME_Y) {
            scroll = parent.getWidgets().get(WIDGET_ROOT, WIDGET_SCROLL, WIDGET_SCROLL_UP);
        } else if (widget.getPosition().getY() > MAX_FRAME_Y) {
            scroll = parent.getWidgets().get(WIDGET_ROOT, WIDGET_SCROLL, WIDGET_SCROLL_DOWN);
        } else {
            parent.log("[FrostHopper] Error determining scroll requirement");
            return;
        }

        if (scroll != null) {
            final long startTime = System.currentTimeMillis();
            WidgetDestination wDest = new WidgetDestination(parent.getBot(), scroll);
            parent.getMouse().continualClick(wDest, new Condition() {
                @Override
                public boolean evaluate() {
                    return isSelectable(widget) || (System.currentTimeMillis() - startTime) >= 6000;
                }
            });
        } else {
            parent.log("[FrostHopper] Cannot find scroll button");
        }
    }

    private boolean isSelectable(RS2Widget widget) {
        return widget.getPosition().getY() >= MIN_FRAME_Y
                && widget.getPosition().getY() <= MAX_FRAME_Y;
    }
}