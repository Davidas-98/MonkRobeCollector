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
    Area monkArea = new Area(3056, 3489, 3059, 3487).setPlane(1);
    Area boneArea = new Area(3225, 3301, 3236, 3287);
    Area bankArea = new Area(3093, 3497, 3094, 3490);

    //constants for paint
    private final Font font = new Font("Arial", Font.BOLD, 11);
    long timeRan;
    long timeBegan;
    String status;

    //constant items
    String[] items = new String[] {"Bones", "Monk's robe", "Monk's robe top"};
    private FrostHopper hopper;

    public enum State {
        WALKMONK, WALKBONES, PICKUPBONES, PICKUPMONK, BURY, WALKBANK, BANK
    }

    private State getState() {

        if (getSkills().getDynamic(Skill.PRAYER) < 31 && !boneArea.contains(myPosition())) {
            log("Walk to bones");
            status = "Walk to bones..";
            return State.WALKBONES;
        }

        if (getSkills().getDynamic(Skill.PRAYER) >= 31 && !monkArea.contains(myPosition()) && !getInventory().isFull()) {
            log("Walk to monks");
            status = "Walk to monks..";
            return State.WALKMONK;
        }

        if (getInventory().isFull() && (getInventory().contains("Monk's Robe") || getInventory().contains("Monk's Robe Top")) && !bankArea.contains(myPosition())) {
            status = "Walking to bank";
            return State.WALKBANK;
        }

        if (boneArea.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            log("Pickup Bones");
            status = "Picking up bones..";
            return State.PICKUPBONES;
        }

        if (monkArea.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            log("Pickup Monk");
            status = "Picking up monk..";
            return State.PICKUPMONK;
        }

        if (getInventory().getAmount("Bones") == 28) {
            log("Bury");
            status = "Burying Bones..";
            return State.BURY;
        }

        if (bankArea.contains(myPosition()) && getInventory().getEmptySlotCount() == 0){
            log("Banking");
            status = "Opening bank..";
            return State.BANK;
        }

        return null;
    }

    @Override
    public void onStart() {
        hopper = new FrostHopper(this);
        timeBegan = System.currentTimeMillis();
        getExperienceTracker().start(Skill.PRAYER);
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
                case PICKUPBONES:
                    pickup(boneArea);
                    break;
                case PICKUPMONK:
                    pickup(monkArea);
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
        return random(3000, 5000);
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
        try {
            switch (getState()) {
                case PICKUPBONES:
                    paintXp(g);
                    break;
                case BURY:
                    paintXp(g);
                    break;
            }
        } catch (Exception e){

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
    public void pickup(Area area) {
        for (String i : items) {
            GroundItem Items = getGroundItems().closest(obj -> obj.getName().equals(i) && area.contains(obj));
            try {
                if (Items != null && Items.isVisible()) {
                    if (Items.interact("Take")) {
                        long count = inventory.getAmount(i);
                        if (new ConditionalSleep(2000, 1000) {
                            @Override
                            public boolean condition() {
                                status = ("Taken");
                                return inventory.getAmount(i) > count;
                            }
                        }.sleep()) ;
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    //Burying bones
    public void bury() {
        inventory.dropAllExcept("Bones");
        while (inventory.contains("Bones")) {
            if(inventory.interact("Bury", "Bones")) {
                long count = inventory.getAmount("Bones");
                if (new ConditionalSleep(500, 100) {
                    @Override
                    public boolean condition() {
                        log("Buried");
                        return inventory.getAmount("Bones") > count;
                    }
                }.sleep()) ;
            }
        }
    }

    public void bank() {
        if (!getBank().isOpen()) {
            try {
                getBank().open();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log(e.toString());
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