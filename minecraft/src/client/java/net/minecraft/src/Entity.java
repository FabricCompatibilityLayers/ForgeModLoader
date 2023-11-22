package net.minecraft.src;

import java.util.List;
import java.util.Random;

public abstract class Entity {
    private static int nextEntityID = 0;
    public int entityId = nextEntityID++;
    public double renderDistanceWeight = 1.0;
    public boolean preventEntitySpawning = false;
    public Entity riddenByEntity;
    public Entity ridingEntity;
    public World worldObj;
    public double prevPosX;
    public double prevPosY;
    public double prevPosZ;
    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    public float rotationYaw;
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;
    public final AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    public boolean onGround = false;
    public boolean isCollidedHorizontally;
    public boolean isCollidedVertically;
    public boolean isCollided = false;
    public boolean velocityChanged = false;
    protected boolean isInWeb;
    public boolean field_9293_aM = true;
    public boolean isDead = false;
    public float yOffset = 0.0F;
    public float width = 0.6F;
    public float height = 1.8F;
    public float prevDistanceWalkedModified = 0.0F;
    public float distanceWalkedModified = 0.0F;
    public float fallDistance = 0.0F;
    private int nextStepDistance = 1;
    public double lastTickPosX;
    public double lastTickPosY;
    public double lastTickPosZ;
    public float ySize = 0.0F;
    public float stepHeight = 0.0F;
    public boolean noClip = false;
    public float entityCollisionReduction = 0.0F;
    protected Random rand = new Random();
    public int ticksExisted = 0;
    public int fireResistance = 1;
    private int fire = 0;
    protected boolean inWater = false;
    public int heartsLife = 0;
    private boolean firstUpdate = true;
    public String skinUrl;
    public String cloakUrl;
    protected boolean isImmuneToFire = false;
    protected DataWatcher dataWatcher = new DataWatcher();
    private double entityRiderPitchDelta;
    private double entityRiderYawDelta;
    public boolean addedToChunk = false;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;
    public int serverPosX;
    public int serverPosY;
    public int serverPosZ;
    public boolean ignoreFrustumCheck;
    public boolean isAirBorne;

    public Entity(World world) {
        this.worldObj = world;
        this.setPosition(0.0, 0.0, 0.0);
        this.dataWatcher.addObject(0, (byte)0);
        this.dataWatcher.addObject(1, (short)300);
        this.entityInit();
    }

    protected abstract void entityInit();

    public DataWatcher getDataWatcher() {
        return this.dataWatcher;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Entity) {
            return ((Entity)object).entityId == this.entityId;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.entityId;
    }

    protected void preparePlayerToSpawn() {
        if (this.worldObj != null) {
            while(this.posY > 0.0) {
                this.setPosition(this.posX, this.posY, this.posZ);
                if (this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).size() == 0) {
                    break;
                }

                ++this.posY;
            }

            this.motionX = this.motionY = this.motionZ = 0.0;
            this.rotationPitch = 0.0F;
        }
    }

    public void setDead() {
        this.isDead = true;
    }

    protected void setSize(float f, float par2) {
        this.width = f;
        this.height = par2;
    }

    protected void setRotation(float f, float par2) {
        this.rotationYaw = f % 360.0F;
        this.rotationPitch = par2 % 360.0F;
    }

    public void setPosition(double d, double e, double par5) {
        this.posX = d;
        this.posY = e;
        this.posZ = par5;
        float var7 = this.width / 2.0F;
        float var8 = this.height;
        this.boundingBox
            .setBounds(
                d - (double)var7,
                e - (double)this.yOffset + (double)this.ySize,
                par5 - (double)var7,
                d + (double)var7,
                e - (double)this.yOffset + (double)this.ySize + (double)var8,
                par5 + (double)var7
            );
    }

    public void setAngles(float f, float par2) {
        float var3 = this.rotationPitch;
        float var4 = this.rotationYaw;
        this.rotationYaw = (float)((double)this.rotationYaw + (double)f * 0.15);
        this.rotationPitch = (float)((double)this.rotationPitch - (double)par2 * 0.15);
        if (this.rotationPitch < -90.0F) {
            this.rotationPitch = -90.0F;
        }

        if (this.rotationPitch > 90.0F) {
            this.rotationPitch = 90.0F;
        }

        this.prevRotationPitch += this.rotationPitch - var3;
        this.prevRotationYaw += this.rotationYaw - var4;
    }

    public void onUpdate() {
        this.onEntityUpdate();
    }

    public void onEntityUpdate() {
        Profiler.startSection("entityBaseTick");
        if (this.ridingEntity != null && this.ridingEntity.isDead) {
            this.ridingEntity = null;
        }

        ++this.ticksExisted;
        this.prevDistanceWalkedModified = this.distanceWalkedModified;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        if (this.isSprinting() && !this.isInWater()) {
            int var1 = MathHelper.floor_double(this.posX);
            int var2 = MathHelper.floor_double(this.posY - 0.2F - (double)this.yOffset);
            int var3 = MathHelper.floor_double(this.posZ);
            int var4 = this.worldObj.getBlockId(var1, var2, var3);
            if (var4 > 0) {
                this.worldObj
                    .spawnParticle(
                        "tilecrack_" + var4,
                        this.posX + ((double)this.rand.nextFloat() - 0.5) * (double)this.width,
                        this.boundingBox.minY + 0.1,
                        this.posZ + ((double)this.rand.nextFloat() - 0.5) * (double)this.width,
                        -this.motionX * 4.0,
                        1.5,
                        -this.motionZ * 4.0
                    );
            }
        }

        if (this.handleWaterMovement()) {
            if (!this.inWater && !this.firstUpdate) {
                float var6 = MathHelper.sqrt_double(this.motionX * this.motionX * 0.2F + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.2F) * 0.2F;
                if (var6 > 1.0F) {
                    var6 = 1.0F;
                }

                this.worldObj.playSoundAtEntity(this, "random.splash", var6, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                float var7 = (float)MathHelper.floor_double(this.boundingBox.minY);

                for(int var8 = 0; (float)var8 < 1.0F + this.width * 20.0F; ++var8) {
                    float var10 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    float var5 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    this.worldObj
                        .spawnParticle(
                            "bubble",
                            this.posX + (double)var10,
                            var7 + 1.0F,
                            this.posZ + (double)var5,
                            this.motionX,
                            this.motionY - (double)(this.rand.nextFloat() * 0.2F),
                            this.motionZ
                        );
                }

                for(int var9 = 0; (float)var9 < 1.0F + this.width * 20.0F; ++var9) {
                    float var11 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    float var12 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                    this.worldObj
                        .spawnParticle(
                            "splash", this.posX + (double)var11, var7 + 1.0F, this.posZ + (double)var12, this.motionX, this.motionY, this.motionZ
                        );
                }
            }

            this.fallDistance = 0.0F;
            this.inWater = true;
            this.fire = 0;
        } else {
            this.inWater = false;
        }

        if (this.worldObj.isRemote) {
            this.fire = 0;
        } else if (this.fire > 0) {
            if (this.isImmuneToFire) {
                this.fire -= 4;
                if (this.fire < 0) {
                    this.fire = 0;
                }
            } else {
                if (this.fire % 20 == 0) {
                    this.attackEntityFrom(DamageSource.onFire, 1);
                }

                --this.fire;
            }
        }

        if (this.handleLavaMovement()) {
            this.setOnFireFromLava();
            this.fallDistance *= 0.5F;
        }

        if (this.posY < -64.0) {
            this.kill();
        }

        if (!this.worldObj.isRemote) {
            this.setFlag(0, this.fire > 0);
            this.setFlag(2, this.ridingEntity != null);
        }

        this.firstUpdate = false;
        Profiler.endSection();
    }

    protected void setOnFireFromLava() {
        if (!this.isImmuneToFire) {
            this.attackEntityFrom(DamageSource.lava, 4);
            this.setFire(15);
        }
    }

    public void setFire(int par1) {
        int var2 = par1 * 20;
        if (this.fire < var2) {
            this.fire = var2;
        }
    }

    public void extinguish() {
        this.fire = 0;
    }

    protected void kill() {
        this.setDead();
    }

    public boolean isOffsetPositionInLiquid(double d, double e, double par5) {
        AxisAlignedBB var7 = this.boundingBox.getOffsetBoundingBox(d, e, par5);
        List<?> var8 = this.worldObj.getCollidingBoundingBoxes(this, var7);
        if (var8.size() > 0) {
            return false;
        } else {
            return !this.worldObj.isAnyLiquid(var7);
        }
    }

    public void moveEntity(double d, double e, double par5) {
        if (this.noClip) {
            this.boundingBox.offset(d, e, par5);
            this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0;
            this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
            this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0;
        } else {
            Profiler.startSection("move");
            this.ySize *= 0.4F;
            double var7 = this.posX;
            double var9 = this.posZ;
            if (this.isInWeb) {
                this.isInWeb = false;
                d *= 0.25;
                e *= 0.05F;
                par5 *= 0.25;
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
            }

            double var11 = d;
            double var13 = e;
            double var15 = par5;
            AxisAlignedBB var17 = this.boundingBox.copy();
            boolean var18 = this.onGround && this.isSneaking() && this instanceof EntityPlayer;
            if (var18) {
                double var19;
                for(var19 = 0.05;
                    d != 0.0 && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.getOffsetBoundingBox(d, -1.0, 0.0)).size() == 0;
                    var11 = d
                ) {
                    if (d < var19 && d >= -var19) {
                        d = 0.0;
                    } else if (d > 0.0) {
                        d -= var19;
                    } else {
                        d += var19;
                    }
                }

                for(;
                    par5 != 0.0 && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.getOffsetBoundingBox(0.0, -1.0, par5)).size() == 0;
                    var15 = par5
                ) {
                    if (par5 < var19 && par5 >= -var19) {
                        par5 = 0.0;
                    } else if (par5 > 0.0) {
                        par5 -= var19;
                    } else {
                        par5 += var19;
                    }
                }

                while(d != 0.0 && par5 != 0.0 && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.getOffsetBoundingBox(d, -1.0, par5)).size() == 0) {
                    if (d < var19 && d >= -var19) {
                        d = 0.0;
                    } else if (d > 0.0) {
                        d -= var19;
                    } else {
                        d += var19;
                    }

                    if (par5 < var19 && par5 >= -var19) {
                        par5 = 0.0;
                    } else if (par5 > 0.0) {
                        par5 -= var19;
                    } else {
                        par5 += var19;
                    }

                    var11 = d;
                    var15 = par5;
                }
            }

            List<?> var36 = this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.addCoord(d, e, par5));

            for (Object o3 : var36) {
                e = ((AxisAlignedBB) o3).calculateYOffset(this.boundingBox, e);
            }

            this.boundingBox.offset(0.0, e, 0.0);
            if (!this.field_9293_aM && var13 != e) {
                par5 = 0.0;
                e = 0.0;
                d = 0.0;
            }

            boolean var38 = this.onGround || var13 != e && var13 < 0.0;

            for (Object o2 : var36) {
                d = ((AxisAlignedBB) o2).calculateXOffset(this.boundingBox, d);
            }

            this.boundingBox.offset(d, 0.0, 0.0);
            if (!this.field_9293_aM && var11 != d) {
                par5 = 0.0;
                e = 0.0;
                d = 0.0;
            }

            for (Object o1 : var36) {
                par5 = ((AxisAlignedBB) o1).calculateZOffset(this.boundingBox, par5);
            }

            this.boundingBox.offset(0.0, 0.0, par5);
            if (!this.field_9293_aM && var15 != par5) {
                par5 = 0.0;
                e = 0.0;
                d = 0.0;
            }

            if (this.stepHeight > 0.0F && var38 && (var18 || this.ySize < 0.05F) && (var11 != d || var15 != par5)) {
                double var40 = d;
                double var23 = e;
                double var25 = par5;
                d = var11;
                e = this.stepHeight;
                par5 = var15;
                AxisAlignedBB var27 = this.boundingBox.copy();
                this.boundingBox.setBB(var17);
                var36 = this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.addCoord(var11, e, var15));

                for (Object element : var36) {
                    e = ((AxisAlignedBB) element).calculateYOffset(this.boundingBox, e);
                }

                this.boundingBox.offset(0.0, e, 0.0);
                if (!this.field_9293_aM && var13 != e) {
                    par5 = 0.0;
                    e = 0.0;
                    d = 0.0;
                }

                for (Object item : var36) {
                    d = ((AxisAlignedBB) item).calculateXOffset(this.boundingBox, d);
                }

                this.boundingBox.offset(d, 0.0, 0.0);
                if (!this.field_9293_aM && var11 != d) {
                    par5 = 0.0;
                    e = 0.0;
                    d = 0.0;
                }

                for (Object value : var36) {
                    par5 = ((AxisAlignedBB) value).calculateZOffset(this.boundingBox, par5);
                }

                this.boundingBox.offset(0.0, 0.0, par5);
                if (!this.field_9293_aM && var15 != par5) {
                    par5 = 0.0;
                    e = 0.0;
                    d = 0.0;
                }

                if (!this.field_9293_aM && var13 != e) {
                    par5 = 0.0;
                    e = 0.0;
                    d = 0.0;
                } else {
                    e = -this.stepHeight;

                    for (Object o : var36) {
                        e = ((AxisAlignedBB) o).calculateYOffset(this.boundingBox, e);
                    }

                    this.boundingBox.offset(0.0, e, 0.0);
                }

                if (var40 * var40 + var25 * var25 >= d * d + par5 * par5) {
                    d = var40;
                    e = var23;
                    par5 = var25;
                    this.boundingBox.setBB(var27);
                } else {
                    double var51 = this.boundingBox.minY - (double)((int)this.boundingBox.minY);
                    if (var51 > 0.0) {
                        this.ySize = (float)((double)this.ySize + var51 + 0.01);
                    }
                }
            }

            Profiler.endSection();
            Profiler.startSection("rest");
            this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0;
            this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
            this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0;
            this.isCollidedHorizontally = var11 != d || var15 != par5;
            this.isCollidedVertically = var13 != e;
            this.onGround = var13 != e && var13 < 0.0;
            this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
            this.updateFallState(e, this.onGround);
            if (var11 != d) {
                this.motionX = 0.0;
            }

            if (var13 != e) {
                this.motionY = 0.0;
            }

            if (var15 != par5) {
                this.motionZ = 0.0;
            }

            double var41 = this.posX - var7;
            double var42 = this.posZ - var9;
            if (this.canTriggerWalking() && !var18 && this.ridingEntity == null) {
                this.distanceWalkedModified = (float)((double)this.distanceWalkedModified + (double)MathHelper.sqrt_double(var41 * var41 + var42 * var42) * 0.6);
                int var43 = MathHelper.floor_double(this.posX);
                int var26 = MathHelper.floor_double(this.posY - 0.2F - (double)this.yOffset);
                int var46 = MathHelper.floor_double(this.posZ);
                int var52 = this.worldObj.getBlockId(var43, var26, var46);
                if (var52 == 0 && this.worldObj.getBlockId(var43, var26 - 1, var46) == Block.fence.blockID) {
                    var52 = this.worldObj.getBlockId(var43, var26 - 1, var46);
                }

                if (this.distanceWalkedModified > (float)this.nextStepDistance && var52 > 0) {
                    this.nextStepDistance = (int)this.distanceWalkedModified + 1;
                    this.playStepSound(var43, var26, var46, var52);
                    Block.blocksList[var52].onEntityWalking(this.worldObj, var43, var26, var46, this);
                }
            }

            int var44 = MathHelper.floor_double(this.boundingBox.minX + 0.001);
            int var45 = MathHelper.floor_double(this.boundingBox.minY + 0.001);
            int var47 = MathHelper.floor_double(this.boundingBox.minZ + 0.001);
            int var53 = MathHelper.floor_double(this.boundingBox.maxX - 0.001);
            int var29 = MathHelper.floor_double(this.boundingBox.maxY - 0.001);
            int var30 = MathHelper.floor_double(this.boundingBox.maxZ - 0.001);
            if (this.worldObj.checkChunksExist(var44, var45, var47, var53, var29, var30)) {
                for(int var31 = var44; var31 <= var53; ++var31) {
                    for(int var32 = var45; var32 <= var29; ++var32) {
                        for(int var33 = var47; var33 <= var30; ++var33) {
                            int var34 = this.worldObj.getBlockId(var31, var32, var33);
                            if (var34 > 0) {
                                Block.blocksList[var34].onEntityCollidedWithBlock(this.worldObj, var31, var32, var33, this);
                            }
                        }
                    }
                }
            }

            boolean var54 = this.isWet();
            if (this.worldObj.isBoundingBoxBurning(this.boundingBox.contract(0.001, 0.001, 0.001))) {
                this.dealFireDamage(1);
                if (!var54) {
                    ++this.fire;
                    if (this.fire == 0) {
                        this.setFire(8);
                    }
                }
            } else if (this.fire <= 0) {
                this.fire = -this.fireResistance;
            }

            if (var54 && this.fire > 0) {
                this.worldObj.playSoundAtEntity(this, "random.fizz", 0.7F, 1.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                this.fire = -this.fireResistance;
            }

            Profiler.endSection();
        }
    }

    protected void playStepSound(int i, int j, int k, int par4) {
        StepSound var5 = Block.blocksList[par4].stepSound;
        if (this.worldObj.getBlockId(i, j + 1, k) == Block.snow.blockID) {
            var5 = Block.snow.stepSound;
            this.worldObj.playSoundAtEntity(this, var5.getStepSound(), var5.getVolume() * 0.15F, var5.getPitch());
        } else if (!Block.blocksList[par4].blockMaterial.isLiquid()) {
            this.worldObj.playSoundAtEntity(this, var5.getStepSound(), var5.getVolume() * 0.15F, var5.getPitch());
        }
    }

    protected boolean canTriggerWalking() {
        return true;
    }

    protected void updateFallState(double d, boolean par3) {
        if (par3) {
            if (this.fallDistance > 0.0F) {
                if (this instanceof EntityLiving) {
                    int var4 = MathHelper.floor_double(this.posX);
                    int var5 = MathHelper.floor_double(this.posY - 0.2F - (double)this.yOffset);
                    int var6 = MathHelper.floor_double(this.posZ);
                    int var7 = this.worldObj.getBlockId(var4, var5, var6);
                    if (var7 == 0 && this.worldObj.getBlockId(var4, var5 - 1, var6) == Block.fence.blockID) {
                        var7 = this.worldObj.getBlockId(var4, var5 - 1, var6);
                    }

                    if (var7 > 0) {
                        Block.blocksList[var7].onFallenUpon(this.worldObj, var4, var5, var6, this, this.fallDistance);
                    }
                }

                this.fall(this.fallDistance);
                this.fallDistance = 0.0F;
            }
        } else if (d < 0.0) {
            this.fallDistance = (float)((double)this.fallDistance - d);
        }
    }

    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    protected void dealFireDamage(int par1) {
        if (!this.isImmuneToFire) {
            this.attackEntityFrom(DamageSource.inFire, par1);
        }
    }

    public final boolean isImmuneToFire() {
        return this.isImmuneToFire;
    }

    protected void fall(float par1) {
        if (this.riddenByEntity != null) {
            this.riddenByEntity.fall(par1);
        }
    }

    public boolean isWet() {
        return this.inWater
            || this.worldObj.canLightningStrikeAt(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ));
    }

    public boolean isInWater() {
        return this.inWater;
    }

    public boolean handleWaterMovement() {
        return this.worldObj.handleMaterialAcceleration(this.boundingBox.expand(0.0, -0.4F, 0.0).contract(0.001, 0.001, 0.001), Material.water, this);
    }

    public boolean isInsideOfMaterial(Material par1Material) {
        double var2 = this.posY + (double)this.getEyeHeight();
        int var4 = MathHelper.floor_double(this.posX);
        int var5 = MathHelper.floor_float((float)MathHelper.floor_double(var2));
        int var6 = MathHelper.floor_double(this.posZ);
        int var7 = this.worldObj.getBlockId(var4, var5, var6);
        if (var7 != 0 && Block.blocksList[var7].blockMaterial == par1Material) {
            float var8 = BlockFluid.getFluidHeightPercent(this.worldObj.getBlockMetadata(var4, var5, var6)) - 0.11111111F;
            float var9 = (float)(var5 + 1) - var8;
            return var2 < (double)var9;
        } else {
            return false;
        }
    }

    public float getEyeHeight() {
        return 0.0F;
    }

    public boolean handleLavaMovement() {
        return this.worldObj.isMaterialInBB(this.boundingBox.expand(-0.1F, -0.4F, -0.1F), Material.lava);
    }

    public void moveFlying(float f, float g, float par3) {
        float var4 = MathHelper.sqrt_float(f * f + g * g);
        if (!(var4 < 0.01F)) {
            if (var4 < 1.0F) {
                var4 = 1.0F;
            }

            var4 = par3 / var4;
            f *= var4;
            g *= var4;
            float var5 = MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F);
            float var6 = MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F);
            this.motionX += f * var6 - g * var5;
            this.motionZ += g * var6 + f * var5;
        }
    }

    public int getBrightnessForRender(float par1) {
        int var2 = MathHelper.floor_double(this.posX);
        int var3 = MathHelper.floor_double(this.posZ);
        if (this.worldObj.blockExists(var2, 0, var3)) {
            double var4 = (this.boundingBox.maxY - this.boundingBox.minY) * 0.66;
            int var6 = MathHelper.floor_double(this.posY - (double)this.yOffset + var4);
            return this.worldObj.getLightBrightnessForSkyBlocks(var2, var6, var3, 0);
        } else {
            return 0;
        }
    }

    public float getBrightness(float par1) {
        int var2 = MathHelper.floor_double(this.posX);
        int var3 = MathHelper.floor_double(this.posZ);
        if (this.worldObj.blockExists(var2, 0, var3)) {
            double var4 = (this.boundingBox.maxY - this.boundingBox.minY) * 0.66;
            int var6 = MathHelper.floor_double(this.posY - (double)this.yOffset + var4);
            return this.worldObj.getLightBrightness(var2, var6, var3);
        } else {
            return 0.0F;
        }
    }

    public void setWorld(World par1World) {
        this.worldObj = par1World;
    }

    public void setPositionAndRotation(double d, double e, double f, float g, float par8) {
        this.prevPosX = this.posX = d;
        this.prevPosY = this.posY = e;
        this.prevPosZ = this.posZ = f;
        this.prevRotationYaw = this.rotationYaw = g;
        this.prevRotationPitch = this.rotationPitch = par8;
        this.ySize = 0.0F;
        double var9 = this.prevRotationYaw - g;
        if (var9 < -180.0) {
            this.prevRotationYaw += 360.0F;
        }

        if (var9 >= 180.0) {
            this.prevRotationYaw -= 360.0F;
        }

        this.setPosition(this.posX, this.posY, this.posZ);
        this.setRotation(g, par8);
    }

    public void setLocationAndAngles(double d, double e, double f, float g, float par8) {
        this.lastTickPosX = this.prevPosX = this.posX = d;
        this.lastTickPosY = this.prevPosY = this.posY = e + (double)this.yOffset;
        this.lastTickPosZ = this.prevPosZ = this.posZ = f;
        this.rotationYaw = g;
        this.rotationPitch = par8;
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    public float getDistanceToEntity(Entity par1Entity) {
        float var2 = (float)(this.posX - par1Entity.posX);
        float var3 = (float)(this.posY - par1Entity.posY);
        float var4 = (float)(this.posZ - par1Entity.posZ);
        return MathHelper.sqrt_float(var2 * var2 + var3 * var3 + var4 * var4);
    }

    public double getDistanceSq(double d, double e, double par5) {
        double var7 = this.posX - d;
        double var9 = this.posY - e;
        double var11 = this.posZ - par5;
        return var7 * var7 + var9 * var9 + var11 * var11;
    }

    public double getDistance(double d, double e, double par5) {
        double var7 = this.posX - d;
        double var9 = this.posY - e;
        double var11 = this.posZ - par5;
        return MathHelper.sqrt_double(var7 * var7 + var9 * var9 + var11 * var11);
    }

    public double getDistanceSqToEntity(Entity par1Entity) {
        double var2 = this.posX - par1Entity.posX;
        double var4 = this.posY - par1Entity.posY;
        double var6 = this.posZ - par1Entity.posZ;
        return var2 * var2 + var4 * var4 + var6 * var6;
    }

    public void onCollideWithPlayer(EntityPlayer par1EntityPlayer) {
    }

    public void applyEntityCollision(Entity par1Entity) {
        if (par1Entity.riddenByEntity != this && par1Entity.ridingEntity != this) {
            double var2 = par1Entity.posX - this.posX;
            double var4 = par1Entity.posZ - this.posZ;
            double var6 = MathHelper.abs_max(var2, var4);
            if (var6 >= 0.01F) {
                var6 = MathHelper.sqrt_double(var6);
                var2 /= var6;
                var4 /= var6;
                double var8 = 1.0 / var6;
                if (var8 > 1.0) {
                    var8 = 1.0;
                }

                var2 *= var8;
                var4 *= var8;
                var2 *= 0.05F;
                var4 *= 0.05F;
                var2 *= 1.0F - this.entityCollisionReduction;
                var4 *= 1.0F - this.entityCollisionReduction;
                this.addVelocity(-var2, 0.0, -var4);
                par1Entity.addVelocity(var2, 0.0, var4);
            }
        }
    }

    public void addVelocity(double d, double e, double par5) {
        this.motionX += d;
        this.motionY += e;
        this.motionZ += par5;
        this.isAirBorne = true;
    }

    protected void setBeenAttacked() {
        this.velocityChanged = true;
    }

    public boolean attackEntityFrom(DamageSource damageSource, int par2) {
        this.setBeenAttacked();
        return false;
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public boolean canBePushed() {
        return false;
    }

    public void addToPlayerScore(Entity entity, int par2) {
    }

    public boolean isInRangeToRenderVec3D(Vec3D par1Vec3D) {
        double var2 = this.posX - par1Vec3D.xCoord;
        double var4 = this.posY - par1Vec3D.yCoord;
        double var6 = this.posZ - par1Vec3D.zCoord;
        double var8 = var2 * var2 + var4 * var4 + var6 * var6;
        return this.isInRangeToRenderDist(var8);
    }

    public boolean isInRangeToRenderDist(double par1) {
        double var3 = this.boundingBox.getAverageEdgeLength();
        var3 *= 64.0 * this.renderDistanceWeight;
        return par1 < var3 * var3;
    }

    public String getTexture() {
        return null;
    }

    public boolean addEntityID(NBTTagCompound par1NBTTagCompound) {
        String var2 = this.getEntityString();
        if (!this.isDead && var2 != null) {
            par1NBTTagCompound.setString("id", var2);
            this.writeToNBT(par1NBTTagCompound);
            return true;
        } else {
            return false;
        }
    }

    public void writeToNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setTag("Pos", this.newDoubleNBTList(this.posX, this.posY + (double)this.ySize, this.posZ));
        par1NBTTagCompound.setTag("Motion", this.newDoubleNBTList(this.motionX, this.motionY, this.motionZ));
        par1NBTTagCompound.setTag("Rotation", this.newFloatNBTList(this.rotationYaw, this.rotationPitch));
        par1NBTTagCompound.setFloat("FallDistance", this.fallDistance);
        par1NBTTagCompound.setShort("Fire", (short)this.fire);
        par1NBTTagCompound.setShort("Air", (short)this.getAir());
        par1NBTTagCompound.setBoolean("OnGround", this.onGround);
        this.writeEntityToNBT(par1NBTTagCompound);
    }

    public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
        NBTTagList var2 = par1NBTTagCompound.getTagList("Pos");
        NBTTagList var3 = par1NBTTagCompound.getTagList("Motion");
        NBTTagList var4 = par1NBTTagCompound.getTagList("Rotation");
        this.motionX = ((NBTTagDouble)var3.tagAt(0)).data;
        this.motionY = ((NBTTagDouble)var3.tagAt(1)).data;
        this.motionZ = ((NBTTagDouble)var3.tagAt(2)).data;
        if (Math.abs(this.motionX) > 10.0) {
            this.motionX = 0.0;
        }

        if (Math.abs(this.motionY) > 10.0) {
            this.motionY = 0.0;
        }

        if (Math.abs(this.motionZ) > 10.0) {
            this.motionZ = 0.0;
        }

        this.prevPosX = this.lastTickPosX = this.posX = ((NBTTagDouble)var2.tagAt(0)).data;
        this.prevPosY = this.lastTickPosY = this.posY = ((NBTTagDouble)var2.tagAt(1)).data;
        this.prevPosZ = this.lastTickPosZ = this.posZ = ((NBTTagDouble)var2.tagAt(2)).data;
        this.prevRotationYaw = this.rotationYaw = ((NBTTagFloat)var4.tagAt(0)).data;
        this.prevRotationPitch = this.rotationPitch = ((NBTTagFloat)var4.tagAt(1)).data;
        this.fallDistance = par1NBTTagCompound.getFloat("FallDistance");
        this.fire = par1NBTTagCompound.getShort("Fire");
        this.setAir(par1NBTTagCompound.getShort("Air"));
        this.onGround = par1NBTTagCompound.getBoolean("OnGround");
        this.setPosition(this.posX, this.posY, this.posZ);
        this.setRotation(this.rotationYaw, this.rotationPitch);
        this.readEntityFromNBT(par1NBTTagCompound);
    }

    protected final String getEntityString() {
        return EntityList.getEntityString(this);
    }

    protected abstract void readEntityFromNBT(NBTTagCompound par1NBTTagCompound);

    protected abstract void writeEntityToNBT(NBTTagCompound par1NBTTagCompound);

    protected NBTTagList newDoubleNBTList(double... par1ArrayOfDouble) {
        NBTTagList var2 = new NBTTagList();

        for(double var6 : par1ArrayOfDouble) {
            var2.appendTag(new NBTTagDouble(null, var6));
        }

        return var2;
    }

    protected NBTTagList newFloatNBTList(float... par1ArrayOfFloat) {
        NBTTagList var2 = new NBTTagList();

        for(float var6 : par1ArrayOfFloat) {
            var2.appendTag(new NBTTagFloat(null, var6));
        }

        return var2;
    }

    public float getShadowSize() {
        return this.height / 2.0F;
    }

    public EntityItem dropItem(int i, int par2) {
        return this.dropItemWithOffset(i, par2, 0.0F);
    }

    public EntityItem dropItemWithOffset(int i, int j, float par3) {
        return this.entityDropItem(new ItemStack(i, j, 0), par3);
    }

    public EntityItem entityDropItem(ItemStack itemStack, float par2) {
        EntityItem var3 = new EntityItem(this.worldObj, this.posX, this.posY + (double)par2, this.posZ, itemStack);
        var3.delayBeforeCanPickup = 10;
        this.worldObj.spawnEntityInWorld(var3);
        return var3;
    }

    public boolean isEntityAlive() {
        return !this.isDead;
    }

    public boolean isEntityInsideOpaqueBlock() {
        for(int var1 = 0; var1 < 8; ++var1) {
            float var2 = ((float)((var1 >> 0) % 2) - 0.5F) * this.width * 0.8F;
            float var3 = ((float)((var1 >> 1) % 2) - 0.5F) * 0.1F;
            float var4 = ((float)((var1 >> 2) % 2) - 0.5F) * this.width * 0.8F;
            int var5 = MathHelper.floor_double(this.posX + (double)var2);
            int var6 = MathHelper.floor_double(this.posY + (double)this.getEyeHeight() + (double)var3);
            int var7 = MathHelper.floor_double(this.posZ + (double)var4);
            if (this.worldObj.isBlockNormalCube(var5, var6, var7)) {
                return true;
            }
        }

        return false;
    }

    public boolean interact(EntityPlayer par1EntityPlayer) {
        return false;
    }

    public AxisAlignedBB getCollisionBox(Entity par1Entity) {
        return null;
    }

    public void updateRidden() {
        if (this.ridingEntity.isDead) {
            this.ridingEntity = null;
        } else {
            this.motionX = 0.0;
            this.motionY = 0.0;
            this.motionZ = 0.0;
            this.onUpdate();
            if (this.ridingEntity != null) {
                this.ridingEntity.updateRiderPosition();
                this.entityRiderYawDelta += this.ridingEntity.rotationYaw - this.ridingEntity.prevRotationYaw;
                this.entityRiderPitchDelta += this.ridingEntity.rotationPitch - this.ridingEntity.prevRotationPitch;

                while(this.entityRiderYawDelta >= 180.0) {
                    this.entityRiderYawDelta -= 360.0;
                }

                while(this.entityRiderYawDelta < -180.0) {
                    this.entityRiderYawDelta += 360.0;
                }

                while(this.entityRiderPitchDelta >= 180.0) {
                    this.entityRiderPitchDelta -= 360.0;
                }

                while(this.entityRiderPitchDelta < -180.0) {
                    this.entityRiderPitchDelta += 360.0;
                }

                double var1 = this.entityRiderYawDelta * 0.5;
                double var3 = this.entityRiderPitchDelta * 0.5;
                float var5 = 10.0F;
                if (var1 > (double)var5) {
                    var1 = var5;
                }

                if (var1 < (double)(-var5)) {
                    var1 = -var5;
                }

                if (var3 > (double)var5) {
                    var3 = var5;
                }

                if (var3 < (double)(-var5)) {
                    var3 = -var5;
                }

                this.entityRiderYawDelta -= var1;
                this.entityRiderPitchDelta -= var3;
                this.rotationYaw = (float)((double)this.rotationYaw + var1);
                this.rotationPitch = (float)((double)this.rotationPitch + var3);
            }
        }
    }

    public void updateRiderPosition() {
        this.riddenByEntity.setPosition(this.posX, this.posY + this.getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ);
    }

    public double getYOffset() {
        return this.yOffset;
    }

    public double getMountedYOffset() {
        return (double)this.height * 0.75;
    }

    public void mountEntity(Entity par1Entity) {
        this.entityRiderPitchDelta = 0.0;
        this.entityRiderYawDelta = 0.0;
        if (par1Entity == null) {
            if (this.ridingEntity != null) {
                this.setLocationAndAngles(
                    this.ridingEntity.posX,
                    this.ridingEntity.boundingBox.minY + (double)this.ridingEntity.height,
                    this.ridingEntity.posZ,
                    this.rotationYaw,
                    this.rotationPitch
                );
                this.ridingEntity.riddenByEntity = null;
            }

            this.ridingEntity = null;
        } else if (this.ridingEntity == par1Entity) {
            this.ridingEntity.riddenByEntity = null;
            this.ridingEntity = null;
            this.setLocationAndAngles(
                par1Entity.posX, par1Entity.boundingBox.minY + (double)par1Entity.height, par1Entity.posZ, this.rotationYaw, this.rotationPitch
            );
        } else {
            if (this.ridingEntity != null) {
                this.ridingEntity.riddenByEntity = null;
            }

            if (par1Entity.riddenByEntity != null) {
                par1Entity.riddenByEntity.ridingEntity = null;
            }

            this.ridingEntity = par1Entity;
            par1Entity.riddenByEntity = this;
        }
    }

    public void setPositionAndRotation2(double d, double e, double f, float g, float h, int par9) {
        this.setPosition(d, e, f);
        this.setRotation(g, h);
        List<?> var10 = this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox.contract(0.03125, 0.0, 0.03125));
        if (var10.size() > 0) {
            double var11 = 0.0;

            for (Object o : var10) {
                AxisAlignedBB var14 = (AxisAlignedBB) o;
                if (var14.maxY > var11) {
                    var11 = var14.maxY;
                }
            }

            e += var11 - this.boundingBox.minY;
            this.setPosition(d, e, f);
        }
    }

    public float getCollisionBorderSize() {
        return 0.1F;
    }

    public Vec3D getLookVec() {
        return null;
    }

    public void setInPortal() {
    }

    public void setVelocity(double d, double e, double par5) {
        this.motionX = d;
        this.motionY = e;
        this.motionZ = par5;
    }

    public void handleHealthUpdate(byte par1) {
    }

    public void performHurtAnimation() {
    }

    public void updateCloak() {
    }

    public void outfitWithItem(int i, int j, int par3) {
    }

    public boolean isBurning() {
        return this.fire > 0 || this.getFlag(0);
    }

    public boolean isRiding() {
        return this.ridingEntity != null || this.getFlag(2);
    }

    public boolean isSneaking() {
        return this.getFlag(1);
    }

    public void setSneaking(boolean par1) {
        this.setFlag(1, par1);
    }

    public boolean isSprinting() {
        return this.getFlag(3);
    }

    public void setSprinting(boolean par1) {
        this.setFlag(3, par1);
    }

    public boolean isEating() {
        return this.getFlag(4);
    }

    public void setEating(boolean par1) {
        this.setFlag(4, par1);
    }

    protected boolean getFlag(int par1) {
        return (this.dataWatcher.getWatchableObjectByte(0) & 1 << par1) != 0;
    }

    protected void setFlag(int i, boolean par2) {
        byte var3 = this.dataWatcher.getWatchableObjectByte(0);
        if (par2) {
            this.dataWatcher.updateObject(0, (byte)(var3 | 1 << i));
        } else {
            this.dataWatcher.updateObject(0, (byte)(var3 & ~(1 << i)));
        }
    }

    public int getAir() {
        return this.dataWatcher.getWatchableObjectShort(1);
    }

    public void setAir(int par1) {
        this.dataWatcher.updateObject(1, (short)par1);
    }

    public void onStruckByLightning(EntityLightningBolt par1EntityLightningBolt) {
        this.dealFireDamage(5);
        ++this.fire;
        if (this.fire == 0) {
            this.setFire(8);
        }
    }

    public void onKillEntity(EntityLiving par1EntityLiving) {
    }

    protected boolean pushOutOfBlocks(double d, double e, double par5) {
        int var7 = MathHelper.floor_double(d);
        int var8 = MathHelper.floor_double(e);
        int var9 = MathHelper.floor_double(par5);
        double var10 = d - (double)var7;
        double var12 = e - (double)var8;
        double var14 = par5 - (double)var9;
        if (this.worldObj.isBlockNormalCube(var7, var8, var9)) {
            boolean var16 = !this.worldObj.isBlockNormalCube(var7 - 1, var8, var9);
            boolean var17 = !this.worldObj.isBlockNormalCube(var7 + 1, var8, var9);
            boolean var18 = !this.worldObj.isBlockNormalCube(var7, var8 - 1, var9);
            boolean var19 = !this.worldObj.isBlockNormalCube(var7, var8 + 1, var9);
            boolean var20 = !this.worldObj.isBlockNormalCube(var7, var8, var9 - 1);
            boolean var21 = !this.worldObj.isBlockNormalCube(var7, var8, var9 + 1);
            byte var22 = -1;
            double var23 = 9999.0;
            if (var16 && var10 < var23) {
                var23 = var10;
                var22 = 0;
            }

            if (var17 && 1.0 - var10 < var23) {
                var23 = 1.0 - var10;
                var22 = 1;
            }

            if (var18 && var12 < var23) {
                var23 = var12;
                var22 = 2;
            }

            if (var19 && 1.0 - var12 < var23) {
                var23 = 1.0 - var12;
                var22 = 3;
            }

            if (var20 && var14 < var23) {
                var23 = var14;
                var22 = 4;
            }

            if (var21 && 1.0 - var14 < var23) {
                var23 = 1.0 - var14;
                var22 = 5;
            }

            float var25 = this.rand.nextFloat() * 0.2F + 0.1F;
            if (var22 == 0) {
                this.motionX = -var25;
            }

            if (var22 == 1) {
                this.motionX = var25;
            }

            if (var22 == 2) {
                this.motionY = -var25;
            }

            if (var22 == 3) {
                this.motionY = var25;
            }

            if (var22 == 4) {
                this.motionZ = -var25;
            }

            if (var22 == 5) {
                this.motionZ = var25;
            }

            return true;
        } else {
            return false;
        }
    }

    public void setInWeb() {
        this.isInWeb = true;
        this.fallDistance = 0.0F;
    }

    public Entity[] getParts() {
        return null;
    }

    public boolean isEntityEqual(Entity par1Entity) {
        return this == par1Entity;
    }

    public void func_48079_f(float f) {
    }

    public boolean canAttackWithItem() {
        return true;
    }

    public static int getNextId() {
        return nextEntityID++;
    }
}
