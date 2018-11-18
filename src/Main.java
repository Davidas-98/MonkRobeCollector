import org.osbot.rs07.api.GroundItems;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;
import java.util.concurrent.TimeUnit;


@ScriptManifest(author = "David", info = "Monk Robe collector", name = "Monk", version = 1, logo = "")
public class Main extends Script {

    //areas
    Area Monk = new Area(3056, 3489, 3059, 3482).setPlane(1);
    Area Bones = new Area(3225, 3301, 3236, 3287);

    //constants for paint
    private final Font font = new Font("Arial", Font.BOLD, 11);
    long timeRan;
    long timeBegan;
    String status;

    public enum State {
        WALKMONK, WALKBONES, PICKUPBONES, PICKUPMONK, BURY, BANK
    }

    private State getState() {

        if (getSkills().getDynamic(Skill.PRAYER) <= 31 && !Bones.contains(myPosition())) {
            log("Walk to bones");
            status = "Walk to bones..";
            return State.WALKBONES;
        }

        if (getSkills().getDynamic(Skill.PRAYER) >= 32 && !Monk.contains(myPosition())) {
            log("Walk to monks");
            status = "Walk to monks..";
            return State.WALKMONK;
        }

        if (Bones.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            log("Pickup Bones");
            status = "Picking up bones..";
            return State.PICKUPBONES;
        }

        if (Monk.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            log("Pickup Monk");
            status = "Picking up monk..";
            return State.PICKUPMONK;
        }

        if (getInventory().getAmount("Bones") == 28 || getInventory().isFull()) {
            log("Bury");
            status = "Burying Bones..";
            return State.BURY;
        }

        if (getInventory().getAmount("Monk Robe") == 28) {
            status = "Banking";
            return State.BANK;
        }

        return null;
    }

    @Override
    public void onStart() {
        timeBegan = System.currentTimeMillis();
        getExperienceTracker().start(Skill.PRAYER);
    }

    @Override
    public int onLoop() throws InterruptedException {
        try {
            switch (getState()) {
                case WALKMONK:
                    walkToArea(Monk);
                    break;
                case WALKBONES:
                    walkToArea(Bones);
                    break;
                case PICKUPBONES:
                    pickup(Bones);
                    break;
                case PICKUPMONK:
                    pickup(Monk);
                    break;
                case BURY:
                    bury();
                    break;
                case BANK:
                    bank();
                    break;
            }
        } catch (Exception e){

        }
        return random(500, 500);
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
        switch (getState()) {
            case PICKUPBONES:
                paintXp(g);
                break;
            case BURY:
                paintXp(g);
                break;
        }

    }

    @Override
    public void onExit() {
        log("byebye");
    }

    // Walk to Area
    public void walkToArea(Area a) {
        getWalking().webWalk(a);
    }

    //pickup Bones || robes
    public void pickup(Area a) {
        GroundItem Bones = getGroundItems().closest(obj -> obj.getName().equals("Bones") && a.contains(obj));
        try {
            if (Bones != null) {
                if (Bones.interact("Take")) {
                    long boneCount = inventory.getAmount("Bones");
                    if (new ConditionalSleep(2000, 1000) {
                        @Override
                        public boolean condition() {
                            log("Taken");
                            return inventory.getAmount("Bones") > boneCount;
                        }
                    }.sleep()) ;
                }
            }
        } catch (Exception e) {

        }
    }

    //Burying bones
    public void bury() {
        inventory.dropAllExcept("Bones");
        while (inventory.contains("Bones")) {
                inventory.interact("Bury", "Bones");
            try {
                sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void bank() {

    }

    public void paintXp (Graphics g){
        g.drawString("Prayer xp | p/h: " + getExperienceTracker().getGainedXP(Skill.PRAYER) + " | "
                + getExperienceTracker().getGainedXPPerHour(Skill.PRAYER), 15, 295);
        g.drawString("TTL: " + ft(getExperienceTracker().getTimeToLevel(Skill.PRAYER)), 15, 310);
        g.drawString("Levels gained | Current lvl: " + getExperienceTracker().getGainedLevels(Skill.PRAYER) + " | " + getSkills().getDynamic(Skill.PRAYER), 15, 325);
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