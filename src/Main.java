import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.event.WebWalkEvent;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.Condition;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;
import java.util.concurrent.TimeUnit;


@ScriptManifest(author = "David", info = "Monk Robe collector", name = "Monk", version = 1, logo = "")
public class Main extends Script {

    //areas
    Area monkArea = new Area(3056, 3489, 3059, 3487).setPlane(1);
    Area boneArea = new Area(new int[][]{{ 3234, 3609 }, { 3234, 3605 }, { 3237, 3603 }, { 3240, 3603 }, { 3239, 3608 }});
    Area bankArea = new Area(3093, 3497, 3098, 3488);

    //constants for paint
    private final Font font = new Font("Arial", Font.BOLD, 11);
    long timeRan;
    long timeBegan;
    long robesOnStart;
    long robesCollected;
    String status;


    public enum State {
        WALKMONK, WALKBONES, PICKUPBONES, PICKUPMONK, BURY, WALKBANK, BANK
    }

    private State getState() {

        if (getSkills().getDynamic(Skill.PRAYER) < 31 && !boneArea.contains(myPosition())) {
            status = "Walk to bones..";
            return State.WALKBONES;
        }

        if (getSkills().getDynamic(Skill.PRAYER) >= 31 && !monkArea.contains(myPosition()) && !getInventory().isFull()) {
            status = "Walk to monks..";
            return State.WALKMONK;
        }

        if (getInventory().isFull() && (getInventory().contains("Monk's robe")) && !bankArea.contains(myPosition())) {
            status = "Walking to bank..";
            return State.WALKBANK;
        }

        if (bankArea.contains(myPosition()) && getInventory().contains("Monk's robe")){
            status = "Opening bank..";
            return State.BANK;
        }

        if (boneArea.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            status = "Picking up bones..";
            return State.PICKUPBONES;
        }

        if (monkArea.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            status = "Picking up monk..";
            return State.PICKUPMONK;
        }

        if (getInventory().isFull() && getInventory().contains("Bones")) {
            status = "Burying Bones..";
            return State.BURY;
        }

        return null;
    }

    @Override
    public void onStart() {
        timeBegan = System.currentTimeMillis();
        getExperienceTracker().start(Skill.PRAYER);
        robesOnStart = getInventory().getAmount("Monk's robe", "Monk's robe top");
    }

    @Override
    public int onLoop() throws InterruptedException {
        try {
            switch (getState()) {
                case WALKMONK:
                    walkToArea(monkArea);
                    break;
                case WALKBONES:
                    walkToArea(boneArea);
                    break;
                case WALKBANK:
                    walkToArea(bankArea);
                    break;
                case BANK:
                    bank();
                    break;
                case PICKUPBONES:
                    pickup(boneArea, "Bone");
                    break;
                case PICKUPMONK:
                    pickup(monkArea, "Monk");
                    break;
                case BURY:
                    bury();
                    break;
            }
        } catch (Exception e){
            //log(e.toString() + "State");
        }
        return random(100, 150);
    }

    @Override
    public void onPaint(Graphics2D g) {
        //setfont
        g.setColor(Color.WHITE);
        g.setFont(font);

        timeRan = System.currentTimeMillis() - this.timeBegan;
        //status
        g.drawString(status, 400, 325);
        //info
        //g.drawString("Coal miner ~ David", 15, 240);
        //g.drawString("Ore mined: " + coalCollected, 15, 265);
        g.drawString("Time ran: " + ft(timeRan), 15, 280);

        if(getState() == State.PICKUPBONES || getState() == State.BURY) {
            paintXp(g);
        } else if (getState() == State.PICKUPMONK || getState() == State.BANK ||  getState() == State.WALKMONK||  getState() == State.WALKBANK){
            update();
            paintMonk(g);
        }

    }

    @Override
    public void onExit() {
        log("byebye");
    }

    // Walk to Area
    public void walkToArea(Area a) {

        //check for wilderness border crossing
        if (widgets.get(475, 11) != null) {
            widgets.get(475, 11).interact();
            log("Crossing Wilderness Ditch");
            //sleep(500);
            walking.webWalk(a);
        }
        //check for monk talking to him
        if (dialogues.isPendingOption()) {
            dialogues.selectOption(1);
        } else if (dialogues.isPendingContinuation()) {
            dialogues.clickContinue();
        } else {
            walking.webWalk(a);
        }

        //wait for one of the events to return true
        WebWalkEvent evt = new WebWalkEvent(a);
        evt.setBreakCondition(new Condition() {
            @Override
            public boolean evaluate() {
                return widgets.get(475, 11) != null || dialogues.isPendingOption() || dialogues.isPendingContinuation();
            }
        });
        execute(evt);

    }

    //pickup Bones || robes
    public void pickup(Area area, String item) {
        GroundItem Items = getGroundItems().closest(obj -> obj.getName().startsWith(item) && area.contains(obj));
            if(Items != null) {
                log(item + " in Area");
                long count = inventory.getAmount(item);
                try {
                        if (Items.interact("Take")) {
                            log(item + " Found " + Items.getX() + Items.getY());
                            if (new ConditionalSleep(1000,400) {
                                @Override
                                public boolean condition() {
                                    log(item + " Walking to");
                                    return inventory.getAmount(item) > count;
                                }
                            }.sleep()) ;
                        }
                } catch (Exception e) {
                }
            } else {
                try {
                    sleep(300);{
                    }
                } catch (Exception e) {}
                worlds.hopToF2PWorld();
            }
    }

    //Burying bones
    public void bury() {
        inventory.dropAllExcept("Bones");
        while (inventory.contains("Bones")) {
            if(inventory.interact("Bury", "Bones")) {
                long count = inventory.getAmount("Bones");
                if (new ConditionalSleep(600) {
                    @Override
                    public boolean condition() {
                        return inventory.getAmount("Bones") > count;
                    }
                }.sleep()) ;
            }
        }
    }

    public void bank() {
        try {
            if (!getBank().isOpen()) {
                try {
                    getBank().open();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    log(e.toString());
                    log("Cant bank");
                }
                new ConditionalSleep(2500, 3000) {
                    @Override
                    public boolean condition() {
                        return getBank().isOpen();
                    }
                }.sleep();
            } else {
                getBank().depositAll();
                getBank().close();
            }
        } catch (Exception e){
            log(e.toString() + "Banking");
        }
    }

    public void update(){
        long amount = getInventory().getAmount("Monk's robe", "Monk's robe top");
        if(robesOnStart != amount) {
            if (amount == 0)
            {
                robesOnStart = amount;
            } else {
                robesCollected += amount - robesOnStart;
                robesOnStart = amount;
            }
        }
    }

    public void paintXp (Graphics g){
        g.drawString("Prayer xp | p/h: " + getExperienceTracker().getGainedXP(Skill.PRAYER) + " | "
                + getExperienceTracker().getGainedXPPerHour(Skill.PRAYER), 15, 295);
        g.drawString("TTL: " + ft(getExperienceTracker().getTimeToLevel(Skill.PRAYER)), 15, 310);
        g.drawString("Levels gained | Current lvl: " + getExperienceTracker().getGainedLevels(Skill.PRAYER) + " | " + getSkills().getDynamic(Skill.PRAYER), 15, 325);
    }

    public void paintMonk(Graphics g){
        g.drawString("Robes collected: " + robesCollected, 15, 295);
    }

    //Displays time from milliseconds to hour:minute:seconds
    private String ft(long duration) {
        String res = "";
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        if (days == 0) {
            res = (hours + ":" + minutes + ":" + seconds);
        } else {
            res = (days + ":" + hours + ":" + minutes + ":" + seconds);
        }
        return res;
    }
}