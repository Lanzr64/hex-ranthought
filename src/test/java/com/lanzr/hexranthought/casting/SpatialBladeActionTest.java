package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpatialBladeActionTest {
    private static final AABB BOX = new AABB(0, 0, 0, 1, 1, 1);

    @Test void registeredPatternMatchesProvidedNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("start_dir", (byte) 0);
        tag.putByteArray("angles", new byte[] {0, 1, 5, 5, 5, 5, 5, 4, 0, 0, 0});
        assertTrue(HexPattern.fromNBT(tag).sigsEqual(
                HexPattern.fromAngles("weqqqqqawww", HexDir.NORTH_EAST)));
    }

    @Test void costUsesSecondVectorLength() {
        assertEquals(150_000L, SpatialBladeAction.costFor(5.0));
        assertEquals(42_427L, SpatialBladeAction.costFor(Math.sqrt(2.0)));
    }

    @Test void completePassCountsOnlyTheDistanceInsideTheBox() {
        Vec3 start = new Vec3(-1, 0.5, 0.5);
        Vec3 end = new Vec3(2, 0.5, 0.5);
        assertEquals(1.0, SpatialBladeAction.penetrationLength(start, end, 3.0, BOX), 1.0e-9);
        assertEquals(1.0, SpatialBladeAction.penetrationLength(end, start, 3.0, BOX), 1.0e-9);
    }

    @Test void damageUsesLongestBoundingBoxDimension() {
        AABB wideBox = new AABB(0, 0, 0, 2, 1, 1);
        assertEquals(25.0, SpatialBladeAction.damageForBox(
                new Vec3(-1, 0.5, 0.5), new Vec3(3, 0.5, 0.5), 4.0, wideBox), 1.0e-9);
        assertEquals(12.5, SpatialBladeAction.damageForBox(
                new Vec3(1, -1, 0.5), new Vec3(1, 2, 0.5), 3.0, wideBox), 1.0e-9);
    }

    @Test void partialPassAndTangentDoNotPierce() {
        assertEquals(0, SpatialBladeAction.penetrationLength(
                new Vec3(0.5, 0.5, 0.5), new Vec3(2, 0.5, 0.5), 1.5, BOX));
        assertEquals(0, SpatialBladeAction.penetrationLength(
                new Vec3(-1, 0.5, 0.5), new Vec3(0.5, 0.5, 0.5), 1.5, BOX));
        assertEquals(0, SpatialBladeAction.penetrationLength(
                new Vec3(-1, 1, 0.5), new Vec3(2, 1, 0.5), 3, BOX));
    }
}
