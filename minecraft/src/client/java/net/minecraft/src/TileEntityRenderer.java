package net.minecraft.src;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class TileEntityRenderer {
    private final Map<Class<? extends TileEntity>, TileEntitySpecialRenderer> specialRendererMap = new HashMap<>();
    public static TileEntityRenderer instance = new TileEntityRenderer();
    private FontRenderer fontRenderer;
    public static double staticPlayerX;
    public static double staticPlayerY;
    public static double staticPlayerZ;
    public RenderEngine renderEngine;
    public World worldObj;
    public EntityLiving entityLivingPlayer;
    public float playerYaw;
    public float playerPitch;
    public double playerX;
    public double playerY;
    public double playerZ;

    private TileEntityRenderer() {
        this.specialRendererMap.put(TileEntitySign.class, new TileEntitySignRenderer());
        this.specialRendererMap.put(TileEntityMobSpawner.class, new TileEntityMobSpawnerRenderer());
        this.specialRendererMap.put(TileEntityPiston.class, new TileEntityRendererPiston());
        this.specialRendererMap.put(TileEntityChest.class, new TileEntityChestRenderer());
        this.specialRendererMap.put(TileEntityEnchantmentTable.class, new RenderEnchantmentTable());
        this.specialRendererMap.put(TileEntityEndPortal.class, new RenderEndPortal());

        for(TileEntitySpecialRenderer var2 : this.specialRendererMap.values()) {
            var2.setTileEntityRenderer(this);
        }
    }

    public static void setTileEntityRenderer(Class<? extends TileEntity> tileEntityClass,
                                             TileEntitySpecialRenderer specialRenderer) {
        instance.specialRendererMap.put(tileEntityClass, specialRenderer);
    }

    public TileEntitySpecialRenderer getSpecialRendererForClass(Class<? extends TileEntity> par1Class) {
        TileEntitySpecialRenderer var2 = this.specialRendererMap.get(par1Class);
        if (var2 == null && par1Class != TileEntity.class) {
            var2 = this.getSpecialRendererForClass((Class<? extends TileEntity>) par1Class.getSuperclass());
            this.specialRendererMap.put(par1Class, var2);
        }

        return var2;
    }

    public boolean hasSpecialRenderer(TileEntity par1TileEntity) {
        return this.getSpecialRendererForEntity(par1TileEntity) != null;
    }

    public TileEntitySpecialRenderer getSpecialRendererForEntity(TileEntity par1TileEntity) {
        return par1TileEntity == null ? null : this.getSpecialRendererForClass(par1TileEntity.getClass());
    }

    public void cacheActiveRenderInfo(World world, RenderEngine renderEngine, FontRenderer fontRenderer, EntityLiving entityLiving, float par5) {
        if (this.worldObj != world) {
            this.cacheSpecialRenderInfo(world);
        }

        this.renderEngine = renderEngine;
        this.entityLivingPlayer = entityLiving;
        this.fontRenderer = fontRenderer;
        this.playerYaw = entityLiving.prevRotationYaw + (entityLiving.rotationYaw - entityLiving.prevRotationYaw) * par5;
        this.playerPitch = entityLiving.prevRotationPitch + (entityLiving.rotationPitch - entityLiving.prevRotationPitch) * par5;
        this.playerX = entityLiving.lastTickPosX + (entityLiving.posX - entityLiving.lastTickPosX) * (double)par5;
        this.playerY = entityLiving.lastTickPosY + (entityLiving.posY - entityLiving.lastTickPosY) * (double)par5;
        this.playerZ = entityLiving.lastTickPosZ + (entityLiving.posZ - entityLiving.lastTickPosZ) * (double)par5;
    }

    public void func_40742_a() {
    }

    public void renderTileEntity(TileEntity tileEntity, float par2) {
        if (tileEntity.getDistanceFrom(this.playerX, this.playerY, this.playerZ) < 4096.0) {
            int var3 = this.worldObj.getLightBrightnessForSkyBlocks(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 0);
            int var4 = var3 % 65536;
            int var5 = var3 / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)var4 / 1.0F, (float)var5 / 1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.renderTileEntityAt(
                tileEntity, (double)tileEntity.xCoord - staticPlayerX, (double)tileEntity.yCoord - staticPlayerY, (double)tileEntity.zCoord - staticPlayerZ, par2
            );
        }
    }

    public void renderTileEntityAt(TileEntity tileEntity, double d, double e, double f, float par8) {
        TileEntitySpecialRenderer var9 = this.getSpecialRendererForEntity(tileEntity);
        if (var9 != null) {
            var9.renderTileEntityAt(tileEntity, d, e, f, par8);
        }
    }

    public void cacheSpecialRenderInfo(World par1World) {
        this.worldObj = par1World;

        for(TileEntitySpecialRenderer var3 : this.specialRendererMap.values()) {
            if (var3 != null) {
                var3.cacheSpecialRenderInfo(par1World);
            }
        }
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }
}
