package net.minecraft.src;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RenderPlayer extends RenderLiving {
    private final ModelBiped modelBipedMain = (ModelBiped)this.mainModel;
    private final ModelBiped modelArmorChestplate = new ModelBiped(1.0F);
    private final ModelBiped modelArmor = new ModelBiped(0.5F);
    private static String[] armorFilenamePrefix = new String[]{"cloth", "chain", "iron", "diamond", "gold"};

    public RenderPlayer() {
        super(new ModelBiped(0.0F), 0.5F);
    }

    protected int setArmorModel(EntityPlayer entityPlayer, int i, float par3) {
        ItemStack var4 = entityPlayer.inventory.armorItemInSlot(3 - i);
        if (var4 != null) {
            Item var5 = var4.getItem();
            if (var5 instanceof ItemArmor) {
                ItemArmor var6 = (ItemArmor)var5;
                this.loadTexture("/armor/" + armorFilenamePrefix[var6.renderIndex] + "_" + (i == 2 ? 2 : 1) + ".png");
                ModelBiped var7 = i == 2 ? this.modelArmor : this.modelArmorChestplate;
                var7.bipedHead.showModel = i == 0;
                var7.bipedHeadwear.showModel = i == 0;
                var7.bipedBody.showModel = i == 1 || i == 2;
                var7.bipedRightArm.showModel = i == 1;
                var7.bipedLeftArm.showModel = i == 1;
                var7.bipedRightLeg.showModel = i == 2 || i == 3;
                var7.bipedLeftLeg.showModel = i == 2 || i == 3;
                this.setRenderPassModel(var7);
                if (var4.isItemEnchanted()) {
                    return 15;
                }

                return 1;
            }
        }

        return -1;
    }

    public void renderPlayer(EntityPlayer entityPlayer, double d, double e, double f, float g, float par9) {
        ItemStack var10 = entityPlayer.inventory.getCurrentItem();
        this.modelArmorChestplate.heldItemRight = this.modelArmor.heldItemRight = this.modelBipedMain.heldItemRight = var10 != null ? 1 : 0;
        if (var10 != null && entityPlayer.getItemInUseCount() > 0) {
            EnumAction var11 = var10.getItemUseAction();
            if (var11 == EnumAction.block) {
                this.modelArmorChestplate.heldItemRight = this.modelArmor.heldItemRight = this.modelBipedMain.heldItemRight = 3;
            } else if (var11 == EnumAction.bow) {
                this.modelArmorChestplate.aimedBow = this.modelArmor.aimedBow = this.modelBipedMain.aimedBow = true;
            }
        }

        this.modelArmorChestplate.isSneak = this.modelArmor.isSneak = this.modelBipedMain.isSneak = entityPlayer.isSneaking();
        double var13 = e - (double)entityPlayer.yOffset;
        if (entityPlayer.isSneaking() && !(entityPlayer instanceof EntityPlayerSP)) {
            var13 -= 0.125;
        }

        super.doRenderLiving(entityPlayer, d, var13, f, g, par9);
        this.modelArmorChestplate.aimedBow = this.modelArmor.aimedBow = this.modelBipedMain.aimedBow = false;
        this.modelArmorChestplate.isSneak = this.modelArmor.isSneak = this.modelBipedMain.isSneak = false;
        this.modelArmorChestplate.heldItemRight = this.modelArmor.heldItemRight = this.modelBipedMain.heldItemRight = 0;
    }

    protected void renderName(EntityPlayer entityPlayer, double d, double e, double par6) {
        if (Minecraft.isGuiEnabled() && entityPlayer != this.renderManager.livingPlayer) {
            float var8 = 1.6F;
            float var9 = 0.016666668F * var8;
            float var10 = entityPlayer.getDistanceToEntity(this.renderManager.livingPlayer);
            float var11 = entityPlayer.isSneaking() ? 32.0F : 64.0F;
            if (var10 < var11) {
                String var12 = entityPlayer.username;
                if (!entityPlayer.isSneaking()) {
                    if (entityPlayer.isPlayerSleeping()) {
                        this.renderLivingLabel(entityPlayer, var12, d, e - 1.5, par6, 64);
                    } else {
                        this.renderLivingLabel(entityPlayer, var12, d, e, par6, 64);
                    }
                } else {
                    FontRenderer var13 = this.getFontRendererFromRenderManager();
                    GL11.glPushMatrix();
                    GL11.glTranslatef((float)d + 0.0F, (float)e + 2.3F, (float)par6);
                    GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
                    GL11.glScalef(-var9, -var9, var9);
                    GL11.glDisable(2896);
                    GL11.glTranslatef(0.0F, 0.25F / var9, 0.0F);
                    GL11.glDepthMask(false);
                    GL11.glEnable(3042);
                    GL11.glBlendFunc(770, 771);
                    Tessellator var14 = Tessellator.instance;
                    GL11.glDisable(3553);
                    var14.startDrawingQuads();
                    int var15 = var13.getStringWidth(var12) / 2;
                    var14.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
                    var14.addVertex(-var15 - 1, -1.0, 0.0);
                    var14.addVertex(-var15 - 1, 8.0, 0.0);
                    var14.addVertex(var15 + 1, 8.0, 0.0);
                    var14.addVertex(var15 + 1, -1.0, 0.0);
                    var14.draw();
                    GL11.glEnable(3553);
                    GL11.glDepthMask(true);
                    var13.drawString(var12, -var13.getStringWidth(var12) / 2, 0, 553648127);
                    GL11.glEnable(2896);
                    GL11.glDisable(3042);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glPopMatrix();
                }
            }
        }
    }

    protected void renderSpecials(EntityPlayer entityPlayer, float par2) {
        super.renderEquippedItems(entityPlayer, par2);
        ItemStack var3 = entityPlayer.inventory.armorItemInSlot(3);
        if (var3 != null && var3.getItem().shiftedIndex < 256) {
            GL11.glPushMatrix();
            this.modelBipedMain.bipedHead.postRender(0.0625F);
            if (RenderBlocks.renderItemIn3d(Block.blocksList[var3.itemID].getRenderType())) {
                float var4 = 0.625F;
                GL11.glTranslatef(0.0F, -0.25F, 0.0F);
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(var4, -var4, var4);
            }

            this.renderManager.itemRenderer.renderItem(entityPlayer, var3, 0);
            GL11.glPopMatrix();
        }

        if (entityPlayer.username.equals("deadmau5") && this.loadDownloadableImageTexture(entityPlayer.skinUrl, null)) {
            for(int var19 = 0; var19 < 2; ++var19) {
                float var5 = entityPlayer.prevRotationYaw
                    + (entityPlayer.rotationYaw - entityPlayer.prevRotationYaw) * par2
                    - (entityPlayer.prevRenderYawOffset + (entityPlayer.renderYawOffset - entityPlayer.prevRenderYawOffset) * par2);
                float var6 = entityPlayer.prevRotationPitch + (entityPlayer.rotationPitch - entityPlayer.prevRotationPitch) * par2;
                GL11.glPushMatrix();
                GL11.glRotatef(var5, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(var6, 1.0F, 0.0F, 0.0F);
                GL11.glTranslatef(0.375F * (float)(var19 * 2 - 1), 0.0F, 0.0F);
                GL11.glTranslatef(0.0F, -0.375F, 0.0F);
                GL11.glRotatef(-var6, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-var5, 0.0F, 1.0F, 0.0F);
                float var7 = 1.3333334F;
                GL11.glScalef(var7, var7, var7);
                this.modelBipedMain.renderEars(0.0625F);
                GL11.glPopMatrix();
            }
        }

        if (this.loadDownloadableImageTexture(entityPlayer.playerCloakUrl, null)) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 0.125F);
            double var20 = entityPlayer.field_20066_r
                + (entityPlayer.field_20063_u - entityPlayer.field_20066_r) * (double)par2
                - (entityPlayer.prevPosX + (entityPlayer.posX - entityPlayer.prevPosX) * (double)par2);
            double var23 = entityPlayer.field_20065_s
                + (entityPlayer.field_20062_v - entityPlayer.field_20065_s) * (double)par2
                - (entityPlayer.prevPosY + (entityPlayer.posY - entityPlayer.prevPosY) * (double)par2);
            double var8 = entityPlayer.field_20064_t
                + (entityPlayer.field_20061_w - entityPlayer.field_20064_t) * (double)par2
                - (entityPlayer.prevPosZ + (entityPlayer.posZ - entityPlayer.prevPosZ) * (double)par2);
            float var10 = entityPlayer.prevRenderYawOffset + (entityPlayer.renderYawOffset - entityPlayer.prevRenderYawOffset) * par2;
            double var11 = MathHelper.sin(var10 * (float) Math.PI / 180.0F);
            double var13 = -MathHelper.cos(var10 * (float) Math.PI / 180.0F);
            float var15 = (float)var23 * 10.0F;
            if (var15 < -6.0F) {
                var15 = -6.0F;
            }

            if (var15 > 32.0F) {
                var15 = 32.0F;
            }

            float var16 = (float)(var20 * var11 + var8 * var13) * 100.0F;
            float var17 = (float)(var20 * var13 - var8 * var11) * 100.0F;
            if (var16 < 0.0F) {
                var16 = 0.0F;
            }

            float var18 = entityPlayer.prevCameraYaw + (entityPlayer.cameraYaw - entityPlayer.prevCameraYaw) * par2;
            var15 += MathHelper.sin(
                (entityPlayer.prevDistanceWalkedModified + (entityPlayer.distanceWalkedModified - entityPlayer.prevDistanceWalkedModified) * par2) * 6.0F
            )
                * 32.0F
                * var18;
            if (entityPlayer.isSneaking()) {
                var15 += 25.0F;
            }

            GL11.glRotatef(6.0F + var16 / 2.0F + var15, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(var17 / 2.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-var17 / 2.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            this.modelBipedMain.renderCloak(0.0625F);
            GL11.glPopMatrix();
        }

        ItemStack var21 = entityPlayer.inventory.getCurrentItem();
        if (var21 != null) {
            GL11.glPushMatrix();
            this.modelBipedMain.bipedRightArm.postRender(0.0625F);
            GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
            if (entityPlayer.fishEntity != null) {
                var21 = new ItemStack(Item.stick);
            }

            EnumAction var22 = null;
            if (entityPlayer.getItemInUseCount() > 0) {
                var22 = var21.getItemUseAction();
            }

            if (var21.itemID < 256 && RenderBlocks.renderItemIn3d(Block.blocksList[var21.itemID].getRenderType())) {
                float var27 = 0.5F;
                GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
                var27 *= 0.75F;
                GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(var27, -var27, var27);
            } else if (var21.itemID == Item.bow.shiftedIndex) {
                float var24 = 0.625F;
                GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
                GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(var24, -var24, var24);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            } else if (Item.itemsList[var21.itemID].isFull3D()) {
                float var25 = 0.625F;
                if (Item.itemsList[var21.itemID].shouldRotateAroundWhenRendering()) {
                    GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glTranslatef(0.0F, -0.125F, 0.0F);
                }

                if (entityPlayer.getItemInUseCount() > 0 && var22 == EnumAction.block) {
                    GL11.glTranslatef(0.05F, 0.0F, -0.1F);
                    GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                }

                GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
                GL11.glScalef(var25, -var25, var25);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            } else {
                float var26 = 0.375F;
                GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
                GL11.glScalef(var26, var26, var26);
                GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
            }

            if (var21.getItem().func_46058_c()) {
                for(int var29 = 0; var29 <= 1; ++var29) {
                    int var30 = var21.getItem().getColorFromDamage(var21.getItemDamage(), var29);
                    float var31 = (float)(var30 >> 16 & 0xFF) / 255.0F;
                    float var9 = (float)(var30 >> 8 & 0xFF) / 255.0F;
                    float var32 = (float)(var30 & 0xFF) / 255.0F;
                    GL11.glColor4f(var31, var9, var32, 1.0F);
                    this.renderManager.itemRenderer.renderItem(entityPlayer, var21, var29);
                }
            } else {
                this.renderManager.itemRenderer.renderItem(entityPlayer, var21, 0);
            }

            GL11.glPopMatrix();
        }
    }

    protected void renderPlayerScale(EntityPlayer entityPlayer, float par2) {
        float var3 = 0.9375F;
        GL11.glScalef(var3, var3, var3);
    }

    public void drawFirstPersonHand() {
        this.modelBipedMain.onGround = 0.0F;
        this.modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
        this.modelBipedMain.bipedRightArm.render(0.0625F);
    }

    protected void renderPlayerSleep(EntityPlayer entityPlayer, double d, double e, double par6) {
        if (entityPlayer.isEntityAlive() && entityPlayer.isPlayerSleeping()) {
            super.renderLivingAt(
                entityPlayer, d + (double)entityPlayer.field_22063_x, e + (double)entityPlayer.field_22062_y, par6 + (double)entityPlayer.field_22061_z
            );
        } else {
            super.renderLivingAt(entityPlayer, d, e, par6);
        }
    }

    protected void rotatePlayer(EntityPlayer entityPlayer, float f, float g, float par4) {
        if (entityPlayer.isEntityAlive() && entityPlayer.isPlayerSleeping()) {
            GL11.glRotatef(entityPlayer.getBedOrientationInDegrees(), 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(this.getDeathMaxRotation(entityPlayer), 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
        } else {
            super.rotateCorpse(entityPlayer, f, g, par4);
        }
    }

    public static int addNewArmourPrefix(String prefix) {
        List<String> armours = new ArrayList<>(Arrays.asList(armorFilenamePrefix));
        armours.add(prefix);
        armorFilenamePrefix = armours.toArray(new String[0]);
        return armours.indexOf(prefix);
    }
}
