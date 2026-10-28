package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * 万金油传送带：左右两侧交替优先分流，两侧都收不了才继续向前。
 * 不需要任何配置，摆放朝向与普通传送带一致。
 *
 * @author Jiane
 */
public class UtilityConveyor extends Conveyor{

    public UtilityConveyor(String name){
        super(name);
        //atlas entries are prefixed with the content type, i.e. "block-armored-conveyor-full"
        fullOverride = "block-armored-conveyor-full";
    }

    @Override
    public void load(){
        super.load();

        //复用装甲传送带贴图，不额外增加美术资源
        for(int i = 0; i < regions.length; i++){
            for(int j = 0; j < regions[i].length; j++){
                //the atlas only ships 5 blend groups; fall back to the first one for the rest
                regions[i][j] = Core.atlas.find("armored-conveyor-" + i + "-" + j, Core.atlas.find("armored-conveyor-0-" + j));
            }
        }

        fullIcon = Core.atlas.find("block-armored-conveyor-full");
        uiIcon = Core.atlas.find("block-armored-conveyor-ui", fullIcon);
        region = Core.atlas.find("armored-conveyor-0-0");
    }

    @Override
    public void postInit(){
        super.postInit();
        //Block.postInit infers shownPlanets from requirements, and that runs before modded planets exist.
        //clearing it makes the block available on every planet, including mod ones.
        shownPlanets.clear();
    }

    public class UtilityConveyorBuild extends ConveyorBuild{
        /** 下一次优先尝试的侧向，保证左右交替。 */
        private boolean leftFirst;
        /** 最近一次来料方块，避免把物品原路送回。 */
        private @Nullable Building lastSource;

        @Override
        public void handleItem(Building source, Item item){
            lastSource = source;
            super.handleItem(source, item);
        }

        @Override
        public boolean pass(Item item){
            if(item == null) return false;

            int leftDir = Mathf.mod(rotation + 1, 4), rightDir = Mathf.mod(rotation + 3, 4);
            boolean sent = leftFirst ? trySide(leftDir, item) || trySide(rightDir, item)
                                     : trySide(rightDir, item) || trySide(leftDir, item);

            if(sent){
                leftFirst = !leftFirst;
                return true;
            }

            return super.pass(item);
        }

        /** 只向朝外的支线分流；同向平行、反向、正对我们的旋转方块一律跳过，避免串货和回灌。 */
        private boolean trySide(int dir, Item item){
            Building to = nearby(dir);
            if(to == null || to == next || to == lastSource || to.team != team) return false;
            if(to.block.rotate && to.rotation != dir) return false;

            if(to.acceptItem(this, item)){
                to.handleItem(this, item);
                return true;
            }
            return false;
        }
    }
}
