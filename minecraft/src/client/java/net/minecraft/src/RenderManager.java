package net.minecraft.src;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class RenderManager {
    private Map<Class<? extends Entity>, Render> entityRenderMap = new HashMap<>();
    public static RenderManager instance = new RenderManager();
    private FontRenderer fontRenderer;
    public static double renderPosX;
    public static double renderPosY;
    public static double renderPosZ;
    public RenderEngine renderEngine;
    public ItemRenderer itemRenderer;
    public World worldObj;
    public EntityLiving livingPlayer;
    public float playerViewY;
    public float playerViewX;
    public GameSettings options;
    public double field_1222_l;
    public double field_1221_m;
    public double field_1220_n;

    private RenderManager() {
        this.entityRenderMap.put(EntitySpider.class, new RenderSpider());
        this.entityRenderMap.put(EntityCaveSpider.class, new RenderSpider());
        this.entityRenderMap.put(EntityPig.class, new RenderPig(new ModelPig(), new ModelPig(0.5F), 0.7F));
        this.entityRenderMap.put(EntitySheep.class, new RenderSheep(new ModelSheep2(), new ModelSheep1(), 0.7F));
        this.entityRenderMap.put(EntityCow.class, new RenderCow(new ModelCow(), 0.7F));
        this.entityRenderMap.put(EntityMooshroom.class, new RenderMooshroom(new ModelCow(), 0.7F));
        this.entityRenderMap.put(EntityWolf.class, new RenderWolf(new ModelWolf(), 0.5F));
        this.entityRenderMap.put(EntityChicken.class, new RenderChicken(new ModelChicken(), 0.3F));
        this.entityRenderMap.put(EntityOcelot.class, new RenderOcelot(new ModelOcelot(), 0.4F));
        this.entityRenderMap.put(EntitySilverfish.class, new RenderSilverfish());
        this.entityRenderMap.put(EntityCreeper.class, new RenderCreeper());
        this.entityRenderMap.put(EntityEnderman.class, new RenderEnderman());
        this.entityRenderMap.put(EntitySnowman.class, new RenderSnowMan());
        this.entityRenderMap.put(EntitySkeleton.class, new RenderBiped(new ModelSkeleton(), 0.5F));
        this.entityRenderMap.put(EntityBlaze.class, new RenderBlaze());
        this.entityRenderMap.put(EntityZombie.class, new RenderBiped(new ModelZombie(), 0.5F));
        this.entityRenderMap.put(EntitySlime.class, new RenderSlime(new ModelSlime(16), new ModelSlime(0), 0.25F));
        this.entityRenderMap.put(EntityMagmaCube.class, new RenderMagmaCube());
        this.entityRenderMap.put(EntityPlayer.class, new RenderPlayer());
        this.entityRenderMap.put(EntityGiantZombie.class, new RenderGiantZombie(new ModelZombie(), 0.5F, 6.0F));
        this.entityRenderMap.put(EntityGhast.class, new RenderGhast());
        this.entityRenderMap.put(EntitySquid.class, new RenderSquid(new ModelSquid(), 0.7F));
        this.entityRenderMap.put(EntityVillager.class, new RenderVillager());
        this.entityRenderMap.put(EntityIronGolem.class, new RenderIronGolem());
        this.entityRenderMap.put(EntityLiving.class, new RenderLiving(new ModelBiped(), 0.5F));
        this.entityRenderMap.put(EntityDragon.class, new RenderDragon());
        this.entityRenderMap.put(EntityEnderCrystal.class, new RenderEnderCrystal());
        this.entityRenderMap.put(Entity.class, new RenderEntity());
        this.entityRenderMap.put(EntityPainting.class, new RenderPainting());
        this.entityRenderMap.put(EntityArrow.class, new RenderArrow());
        this.entityRenderMap.put(EntitySnowball.class, new RenderSnowball(Item.snowball.getIconFromDamage(0)));
        this.entityRenderMap.put(EntityEnderPearl.class, new RenderSnowball(Item.enderPearl.getIconFromDamage(0)));
        this.entityRenderMap.put(EntityEnderEye.class, new RenderSnowball(Item.eyeOfEnder.getIconFromDamage(0)));
        this.entityRenderMap.put(EntityEgg.class, new RenderSnowball(Item.egg.getIconFromDamage(0)));
        this.entityRenderMap.put(EntityPotion.class, new RenderSnowball(154));
        this.entityRenderMap.put(EntityExpBottle.class, new RenderSnowball(Item.expBottle.getIconFromDamage(0)));
        this.entityRenderMap.put(EntityFireball.class, new RenderFireball(2.0F));
        this.entityRenderMap.put(EntitySmallFireball.class, new RenderFireball(0.5F));
        this.entityRenderMap.put(EntityItem.class, new RenderItem());
        this.entityRenderMap.put(EntityXPOrb.class, new RenderXPOrb());
        this.entityRenderMap.put(EntityTNTPrimed.class, new RenderTNTPrimed());
        this.entityRenderMap.put(EntityFallingSand.class, new RenderFallingSand());
        this.entityRenderMap.put(EntityMinecart.class, new RenderMinecart());
        this.entityRenderMap.put(EntityBoat.class, new RenderBoat());
        this.entityRenderMap.put(EntityFishHook.class, new RenderFish());
        this.entityRenderMap.put(EntityLightningBolt.class, new RenderLightningBolt());

        for(Render var2 : this.entityRenderMap.values()) {
            var2.setRenderManager(this);
        }
    }

    public Render getEntityClassRenderObject(Class par1Class) {
        Render var2 = (Render)this.entityRenderMap.get(par1Class);
        if (var2 == null && par1Class != Entity.class) {
            var2 = this.getEntityClassRenderObject(par1Class.getSuperclass());
            this.entityRenderMap.put(par1Class, var2);
        }

        return var2;
    }

    public Render getEntityRenderObject(Entity par1Entity) {
        return this.getEntityClassRenderObject(par1Entity.getClass());
    }

    public void cacheActiveRenderInfo(
        World world, RenderEngine renderEngine, FontRenderer fontRenderer, EntityLiving entityLiving, GameSettings gameSettings, float par6
    ) {
        this.worldObj = world;
        this.renderEngine = renderEngine;
        this.options = gameSettings;
        this.livingPlayer = entityLiving;
        this.fontRenderer = fontRenderer;
        if (entityLiving.isPlayerSleeping()) {
            int var7 = world.getBlockId(
                MathHelper.floor_double(entityLiving.posX), MathHelper.floor_double(entityLiving.posY), MathHelper.floor_double(entityLiving.posZ)
            );
            if (var7 == Block.bed.blockID) {
                int var8 = world.getBlockMetadata(
                    MathHelper.floor_double(entityLiving.posX), MathHelper.floor_double(entityLiving.posY), MathHelper.floor_double(entityLiving.posZ)
                );
                int var9 = var8 & 3;
                this.playerViewY = (float)(var9 * 90 + 180);
                this.playerViewX = 0.0F;
            }
        } else {
            this.playerViewY = entityLiving.prevRotationYaw + (entityLiving.rotationYaw - entityLiving.prevRotationYaw) * par6;
            this.playerViewX = entityLiving.prevRotationPitch + (entityLiving.rotationPitch - entityLiving.prevRotationPitch) * par6;
        }

        if (gameSettings.thirdPersonView == 2) {
            this.playerViewY += 180.0F;
        }

        this.field_1222_l = entityLiving.lastTickPosX + (entityLiving.posX - entityLiving.lastTickPosX) * (double)par6;
        this.field_1221_m = entityLiving.lastTickPosY + (entityLiving.posY - entityLiving.lastTickPosY) * (double)par6;
        this.field_1220_n = entityLiving.lastTickPosZ + (entityLiving.posZ - entityLiving.lastTickPosZ) * (double)par6;
    }

    public void renderEntity(Entity entity, float par2) {
        double var3 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)par2;
        double var5 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)par2;
        double var7 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)par2;
        float var9 = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * par2;
        int var10 = entity.getBrightnessForRender(par2);
        if (entity.isBurning()) {
            var10 = 15728880;
        }

        int var11 = var10 % 65536;
        int var12 = var10 / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)var11 / 1.0F, (float)var12 / 1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderEntityWithPosYaw(entity, var3 - renderPosX, var5 - renderPosY, var7 - renderPosZ, var9, par2);
    }

    public void renderEntityWithPosYaw(Entity entity, double d, double e, double f, float g, float par9) {
        Render var10 = this.getEntityRenderObject(entity);
        if (var10 != null) {
            var10.doRender(entity, d, e, f, g, par9);
            var10.doRenderShadowAndFire(entity, d, e, f, g, par9);
        }
    }

    public void set(World par1World) {
        this.worldObj = par1World;
    }

    public double getDistanceToCamera(double d, double e, double par5) {
        double var7 = d - this.field_1222_l;
        double var9 = e - this.field_1221_m;
        double var11 = par5 - this.field_1220_n;
        return var7 * var7 + var9 * var9 + var11 * var11;
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }

    public Map<Class<? extends Entity>, Render> getRendererList() {
        return entityRenderMap;
    }
}
