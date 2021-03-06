package com.whammich.sstow.tile;

import com.google.common.base.Strings;
import com.whammich.repack.tehnut.lib.annot.Handler;
import com.whammich.sstow.ConfigHandler;
import com.whammich.sstow.api.ShardHelper;
import com.whammich.sstow.api.SoulShardsAPI;
import com.whammich.sstow.block.BlockCage;
import com.whammich.sstow.item.ItemSoulShard;
import com.whammich.sstow.registry.ModItems;
import com.whammich.sstow.util.EntityMapper;
import com.whammich.sstow.util.TierHandler;
import com.whammich.sstow.util.Utils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ITickable;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Getter
@Setter
@Handler
public class TileEntityCage extends TileInventory implements ITickable {

    public static final String TIER = "tier";
    public static final String ACTIVE_TIME = "activeTime";
    public static final String ENT_NAME = "entName";
    public static final String OWNER = "owner";
    public static final String SSTOW = "SSTOW";

    private int activeTime;
    private int tier;
    private String entName = "";
    private String owner = "";

    public TileEntityCage() {
        super(1, "cage");
    }

    @Override
    public void update() {
        if (getWorld().isRemote)
            return;

        if (tier == 0 || Strings.isNullOrEmpty(entName)) {
            setActiveState(false);
            return;
        }

        if (TierHandler.getTier(tier).equals(TierHandler.BLANK_TIER)) {
            setActiveState(false);
            return;
        }

        if (ConfigHandler.requireOwnerOnline && !Utils.isOwnerOnline(owner)) {
            setActiveState(false);
            return;
        }

        if (!ConfigHandler.entityList.contains(entName) && !ConfigHandler.enableBlacklistedSpawning) {
            setActiveState(false);
            return;
        }

        if (TierHandler.checksRedstone(tier) && isRedstoned()) {
            setActiveState(false);
            return;
        }

        if (TierHandler.checksPlayer(tier) && !isPlayerClose()) {
            setActiveState(false);
            return;
        }

        if (SoulShardsAPI.isEntityBlacklisted(EntityMapper.getNewEntityInstance(getWorld(), entName, getPos()))) {
            setActiveState(false);
            return;
        }

        activeTime++;
        setActiveState(true);

        if (activeTime % (TierHandler.getCooldown(tier) * (ConfigHandler.cooldownUsesSeconds ? 20 : 1)) == 0) {
            EntityLiving[] toSpawn = new EntityLiving[TierHandler.getSpawnAmount(tier)];

            for (int i = 0; i < toSpawn.length; i++) {
                toSpawn[i] = EntityMapper.getNewEntityInstance(getWorld(), entName, getPos());

                toSpawn[i].getEntityData().setBoolean(SSTOW, true);
                toSpawn[i].forceSpawn = true;
                toSpawn[i].enablePersistence();
            }

            spawnEntities(toSpawn);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);

        this.tier = tagCompound.getInteger(TIER);
        this.entName = tagCompound.getString(ENT_NAME);
        this.activeTime = tagCompound.getInteger(ACTIVE_TIME);
        this.owner = tagCompound.getString(OWNER);
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);

        tagCompound.setInteger(TIER, tier);
        tagCompound.setString(ENT_NAME, entName);
        tagCompound.setInteger(ACTIVE_TIME, activeTime);
        tagCompound.setString(OWNER, owner);
    }

    public boolean getActiveState() {
        return getWorld().getBlockState(getPos()).getValue(BlockCage.ACTIVE);
    }

    public void setActiveState(boolean activeState) {
        getWorld().setBlockState(getPos(), getWorld().getBlockState(getPos()).withProperty(BlockCage.ACTIVE, activeState));
    }

    private boolean canSpawnAtCoords(EntityLiving ent) {
        return worldObj.getCollidingBoundingBoxes(ent, ent.getEntityBoundingBox()).isEmpty();
    }

    private void spawnEntities(EntityLiving[] ents) {
        for (EntityLiving ent : ents) {
            int counter = 0;

            if (TierHandler.checksLight(tier) && !canSpawnInLight(ent))
                break;

            do {
                counter++;
                if (counter >= 5) {
                    ent.setDead();
                    break;
                }

                double x = getPos().getX() + (getWorld().rand.nextDouble() - getWorld().rand.nextDouble()) * 4.0D;
                double y = getPos().getY() + getWorld().rand.nextInt(3) - 1;
                double z = getPos().getZ() + (getWorld().rand.nextDouble() - getWorld().rand.nextDouble()) * 4.0D;
                ent.setLocationAndAngles(x, y, z, getWorld().rand.nextFloat() * 360.0F, 0.0F);
            } while (!canSpawnAtCoords(ent) || counter >= 5);

            if (!ent.isDead && !hasReachedSpawnCap(ent))
                getWorld().spawnEntityInWorld(ent);
        }
    }

    private boolean isRedstoned() {
        return getWorld().isBlockPowered(getPos());
    }

    private boolean canSpawnInLight(EntityLiving entityLiving) {
        return !(entityLiving instanceof EntityMob || entityLiving instanceof IMob) || Utils.getBlockLightLevel(getWorld(), getPos(), getWorld().isDaytime()) <= 8;
    }

    private boolean isPlayerClose() {
        return getWorld().getClosestPlayer(getPos().getX(), getPos().getY(), getPos().getZ(), 16D) != null;
    }

    private boolean hasReachedSpawnCap(EntityLiving living) {
        AxisAlignedBB box = AxisAlignedBB.fromBounds(getPos().getX() - 16, getPos().getY() - 16, getPos().getZ() - 16, getPos().getX() + 16, getPos().getY() + 16, getPos().getZ() + 16);

        int mobCount = 0;

        for (EntityLiving entity : getWorld().getEntitiesWithinAABB(living.getClass(), box))
            if (Utils.isCageBorn(entity))
                mobCount++;

        return mobCount > ConfigHandler.spawnCap;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return stack.getItem() == ModItems.getItem(ItemSoulShard.class) && ShardHelper.getTierFromShard(stack) > 0 && ShardHelper.isBound(stack);
    }

    @SubscribeEvent
    public void onDeath(LivingExperienceDropEvent event) {
        if (!ConfigHandler.enableExperienceDrop && Utils.isCageBorn(event.entityLiving))
            event.setDroppedExperience(0);
    }
}
