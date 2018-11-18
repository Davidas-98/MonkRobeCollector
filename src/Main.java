import org.osbot.rs07.api.GroundItems;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;


@ScriptManifest(author = "David", info = "Monk Robe collector", name = "Monk", version = 1, logo = "")
public class Main extends Script {

    Area Monk = new Area(3056, 3489, 3059, 3482).setPlane(1);
    Area Bones = new Area(3225, 3301, 3236, 3287);


    public enum State {
        WALKMONK, WALKBONES, PICKUPBONES, PICKUPMONK, BURY, BANK
    }

    private State getState() {

        if (getSkills().getDynamic(Skill.PRAYER) <= 31 && !Bones.contains(myPosition())) {
            log("Walk to bones");
            return State.WALKBONES;
        }

        if (getSkills().getDynamic(Skill.PRAYER) > 31 && !Monk.contains(myPosition())) {
            log("Walk to monks");
            return State.WALKMONK;
        }

        if (Bones.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            log("Pickup Bones");
            return State.PICKUPBONES;
        }

        if (Monk.contains(myPosition()) && getInventory().getEmptySlotCount() != 0) {
            log("Pickup Monk");
            return State.PICKUPMONK;
        }

        if (getInventory().getAmount("Bones") == 28 || getInventory().isFull()) {
            log("Bury");
            return State.BURY;
        }

        if (getInventory().getAmount("Monk Robe") == 28) {
            return State.BANK;
        }

        return null;
    }

    @Override
    public void onStart() {
        int count;
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

    }

    @Override
    public void onExit() {
        log("byebye");
    }

    public void walkToArea(Area a) {
        getWalking().webWalk(a);
    }

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

    public void bury() {
        inventory.dropAllExcept("Bones");
        while (inventory.contains("Bones")) {
                inventory.interact("Bury", "Bones");
            }
    }

    public void bank() {

    }

}