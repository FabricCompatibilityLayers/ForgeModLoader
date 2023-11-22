package net.minecraft.src;

import java.util.HashMap;
import java.util.Map;

public class TileEntity {
    private static final Map<String, Class<? extends TileEntity>> nameToClassMap = new HashMap<>();
    private static final Map<Class<? extends TileEntity>, String> classToNameMap = new HashMap<>();
    public World worldObj;
    public int xCoord;
    public int yCoord;
    public int zCoord;
    protected boolean tileEntityInvalid;
    public int blockMetadata = -1;
    public Block blockType;

    private static void addMapping(Class<? extends TileEntity> class_, String par1Str) {
        if (nameToClassMap.containsKey(par1Str)) {
            throw new IllegalArgumentException("Duplicate id: " + par1Str);
        } else {
            nameToClassMap.put(par1Str, class_);
            classToNameMap.put(class_, par1Str);
        }
    }

    public static void addNewTileEntityMapping(Class<? extends TileEntity> tileEntityClass, String id) {
        addMapping(tileEntityClass, id);
    }

    public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
        this.xCoord = par1NBTTagCompound.getInteger("x");
        this.yCoord = par1NBTTagCompound.getInteger("y");
        this.zCoord = par1NBTTagCompound.getInteger("z");
    }

    public void writeToNBT(NBTTagCompound par1NBTTagCompound) {
        String var2 = classToNameMap.get(this.getClass());
        if (var2 == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        } else {
            par1NBTTagCompound.setString("id", var2);
            par1NBTTagCompound.setInteger("x", this.xCoord);
            par1NBTTagCompound.setInteger("y", this.yCoord);
            par1NBTTagCompound.setInteger("z", this.zCoord);
        }
    }

    public void updateEntity() {
    }

    public static TileEntity createAndLoadEntity(NBTTagCompound par0NBTTagCompound) {
        TileEntity var1 = null;

        try {
            Class<? extends TileEntity> var2 = nameToClassMap.get(par0NBTTagCompound.getString("id"));
            if (var2 != null) {
                var1 = var2.newInstance();
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        if (var1 != null) {
            var1.readFromNBT(par0NBTTagCompound);
        } else {
            System.out.println("Skipping TileEntity with id " + par0NBTTagCompound.getString("id"));
        }

        return var1;
    }

    public int getBlockMetadata() {
        if (this.blockMetadata == -1) {
            this.blockMetadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        }

        return this.blockMetadata;
    }

    public void onInventoryChanged() {
        if (this.worldObj != null) {
            this.blockMetadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
            this.worldObj.updateTileEntityChunkAndDoNothing(this.xCoord, this.yCoord, this.zCoord, this);
        }
    }

    public double getDistanceFrom(double d, double e, double par5) {
        double var7 = (double)this.xCoord + 0.5 - d;
        double var9 = (double)this.yCoord + 0.5 - e;
        double var11 = (double)this.zCoord + 0.5 - par5;
        return var7 * var7 + var9 * var9 + var11 * var11;
    }

    public Block getBlockType() {
        if (this.blockType == null) {
            this.blockType = Block.blocksList[this.worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord)];
        }

        return this.blockType;
    }

    public boolean isInvalid() {
        return this.tileEntityInvalid;
    }

    public void invalidate() {
        this.tileEntityInvalid = true;
    }

    public void validate() {
        this.tileEntityInvalid = false;
    }

    public void onTileEntityPowered(int i, int par2) {
    }

    public void updateContainingBlockInfo() {
        this.blockType = null;
        this.blockMetadata = -1;
    }

    static {
        addMapping(TileEntityFurnace.class, "Furnace");
        addMapping(TileEntityChest.class, "Chest");
        addMapping(TileEntityRecordPlayer.class, "RecordPlayer");
        addMapping(TileEntityDispenser.class, "Trap");
        addMapping(TileEntitySign.class, "Sign");
        addMapping(TileEntityMobSpawner.class, "MobSpawner");
        addMapping(TileEntityNote.class, "Music");
        addMapping(TileEntityPiston.class, "Piston");
        addMapping(TileEntityBrewingStand.class, "Cauldron");
        addMapping(TileEntityEnchantmentTable.class, "EnchantTable");
        addMapping(TileEntityEndPortal.class, "Airportal");
    }
}
