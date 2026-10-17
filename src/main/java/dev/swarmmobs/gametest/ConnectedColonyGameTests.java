package dev.swarmmobs.gametest;

import dev.swarmmobs.SwarmMobs;
import dev.swarmmobs.colony.ColonyPhysicalGrowth;
import dev.swarmmobs.colony.ColonyPopulationRegistry;
import net.minecraft.world.entity.EntityType;
import dev.swarmmobs.colony.SwarmNestBlockEntity;
import dev.swarmmobs.registry.SwarmNestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-world integration tests independent of both experimental auto-growth flags. */
public final class ConnectedColonyGameTests {
    private static final String TEMPLATE = "empty5x4x5";
    private ConnectedColonyGameTests() {}

    @PrefixGameTestTemplate(false)
    @GameTest(batch = "swarm_colony_blocks", templateNamespace = SwarmMobs.MOD_ID,
            template = TEMPLATE, timeoutTicks = 40)
    public static void monsterExtensionIsPassiveAndOwned(GameTestHelper helper) {
        var parent = new BlockPos(2, 1, 2);
        var target = new BlockPos(3, 1, 2);
        helper.setBlock(parent, SwarmNestBlocks.NEST_CORE.get());
        helper.setBlock(target, Blocks.AIR);
        var level = helper.getLevel();
        if (!ColonyPhysicalGrowth.placeOne(level, helper.absolutePos(parent),
                helper.absolutePos(target), ColonyPhysicalGrowth.Kind.MONSTER)) {
            helper.fail("Monster core failed face-adjacent placement");
            return;
        }
        if (!(level.getBlockEntity(helper.absolutePos(target)) instanceof SwarmNestBlockEntity extension)
                || !extension.isConnectedExtension()
                || !helper.absolutePos(parent).equals(extension.connectedRoot())) {
            helper.fail("Placed core is not a passive extension of original root");
            return;
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(batch = "swarm_colony_blocks", templateNamespace = SwarmMobs.MOD_ID,
            template = TEMPLATE, timeoutTicks = 40)
    public static void independentRootsMustNotMerge(GameTestHelper helper) {
        var first = new BlockPos(1, 1, 2);
        var second = new BlockPos(3, 1, 2);
        var between = new BlockPos(2, 1, 2);
        helper.setBlock(first, SwarmNestBlocks.NEST_CORE.get());
        helper.setBlock(second, SwarmNestBlocks.NEST_CORE.get());
        helper.setBlock(between, Blocks.AIR);
        boolean merged = ColonyPhysicalGrowth.placeOne(helper.getLevel(),
                helper.absolutePos(first), helper.absolutePos(between),
                ColonyPhysicalGrowth.Kind.MONSTER);
        if (merged || !helper.getLevel().getBlockState(
                helper.absolutePos(between)).isAir()) {
            helper.fail("Growth incorrectly merged two independent monster roots");
            return;
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(batch = "swarm_colony_blocks", templateNamespace = SwarmMobs.MOD_ID,
            template = TEMPLATE, timeoutTicks = 40)
    public static void vanillaHiveExpansionCreatesEmptyVanillaEntity(GameTestHelper helper) {
        var parent = new BlockPos(2, 1, 2);
        var target = new BlockPos(3, 1, 2);
        helper.setBlock(parent, Blocks.BEEHIVE);
        helper.setBlock(target, Blocks.AIR);
        var level = helper.getLevel();
        if (!ColonyPhysicalGrowth.placeOne(level, helper.absolutePos(parent),
                helper.absolutePos(target), ColonyPhysicalGrowth.Kind.BEEHIVE)
                || !level.getBlockState(helper.absolutePos(target)).is(Blocks.BEEHIVE)
                || !(level.getBlockEntity(helper.absolutePos(target)) instanceof BeehiveBlockEntity hive)
                || hive.getOccupantCount() != 0) {
            helper.fail("Vanilla hive extension was not created as an empty vanilla hive");
            return;
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(batch = "swarm_colony_census", templateNamespace = SwarmMobs.MOD_ID,
            template = TEMPLATE, timeoutTicks = 50)
    public static void distributedPopulationCountsUniqueOwnerTaggedResidents(GameTestHelper helper) {
        var home = helper.absolutePos(new BlockPos(2, 1, 2));
        var unrelated = helper.absolutePos(new BlockPos(4, 1, 4));
        var level = helper.getLevel();
        var worker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        var scout = helper.spawn(EntityType.SPIDER, new BlockPos(3, 1, 3));
        var outsider = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 1));
        for (var member : java.util.List.of(worker, scout)) {
            member.getPersistentData().putLong("SwarmColonyNest", home.asLong());
            member.getPersistentData().putString("SwarmColonyDimension",
                    level.dimension().location().toString());
            ColonyPopulationRegistry.observe(level, member, level.getGameTime());
        }
        outsider.getPersistentData().putLong("SwarmColonyNest", unrelated.asLong());
        outsider.getPersistentData().putString("SwarmColonyDimension",
                level.dimension().location().toString());
        ColonyPopulationRegistry.observe(level, outsider, level.getGameTime());
        // Multiple tick reports from the same mob must not inflate capacity.
        ColonyPopulationRegistry.observe(level, worker, level.getGameTime());
        if (ColonyPopulationRegistry.count(level, home, level.getGameTime()) != 2
                || ColonyPopulationRegistry.count(level, unrelated, level.getGameTime()) != 1) {
            helper.fail("Census counted duplicate or foreign colony members");
            return;
        }
        helper.succeed();
    }
}
