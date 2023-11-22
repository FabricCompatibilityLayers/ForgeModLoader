package net.minecraft.src;

import java.util.HashMap;
import java.util.Map;

public class EntityList {
    private static final Map<String, Class<? extends Entity>> stringToClassMapping = new HashMap<>();
    private static final Map<Class<? extends Entity>, String> classToStringMapping = new HashMap<>();
    private static final Map<Integer, Class<? extends Entity>> IDtoClassMapping = new HashMap<>();
    private static final Map<Class<? extends Entity>, Integer> classToIDMapping = new HashMap<>();
    private static final Map<String, Integer> stringToIDMapping = new HashMap<>();
    public static HashMap<Integer, EntityEggInfo> entityEggs = new HashMap<>();

    private static void addMapping(Class<? extends Entity> clazz, String string, int id) {
        stringToClassMapping.put(string, clazz);
        classToStringMapping.put(clazz, string);
        IDtoClassMapping.put(id, clazz);
        classToIDMapping.put(clazz, id);
        stringToIDMapping.put(string, id);
    }

    private static void addMapping(Class<? extends Entity> class_, String string, int i, int j, int par4) {
        addMapping(class_, string, i);
        entityEggs.put(i, new EntityEggInfo(i, j, par4));
    }

    public static Entity createEntityByName(String string, World par1World) {
        Entity var2 = null;

        try {
            Class<? extends Entity> var3 = stringToClassMapping.get(string);
            if (var3 != null) {
                var2 = var3.getConstructor(World.class).newInstance(par1World);
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return var2;
    }

    public static Entity createEntityFromNBT(NBTTagCompound nBTTagCompound, World par1World) {
        Entity var2 = null;

        try {
            Class<? extends Entity> var3 = stringToClassMapping.get(nBTTagCompound.getString("id"));
            if (var3 != null) {
                var2 = var3.getConstructor(World.class).newInstance(par1World);
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        if (var2 != null) {
            var2.readFromNBT(nBTTagCompound);
        } else {
            System.out.println("Skipping Entity with id " + nBTTagCompound.getString("id"));
        }

        return var2;
    }

    public static Entity createEntityByID(int i, World par1World) {
        Entity var2 = null;

        try {
            Class<? extends Entity> var3 = IDtoClassMapping.get(i);
            if (var3 != null) {
                var2 = var3.getConstructor(World.class).newInstance(par1World);
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        if (var2 == null) {
            System.out.println("Skipping Entity with id " + i);
        }

        return var2;
    }

    public static int getEntityID(Entity par0Entity) {
        return classToIDMapping.get(par0Entity.getClass());
    }

    public static String getEntityString(Entity par0Entity) {
        return classToStringMapping.get(par0Entity.getClass());
    }

    public static String getStringFromID(int par0) {
        Class<? extends Entity> var1 = IDtoClassMapping.get(par0);
        return var1 != null ? classToStringMapping.get(var1) : null;
    }

    static {
        addMapping(EntityItem.class, "Item", 1);
        addMapping(EntityXPOrb.class, "XPOrb", 2);
        addMapping(EntityPainting.class, "Painting", 9);
        addMapping(EntityArrow.class, "Arrow", 10);
        addMapping(EntitySnowball.class, "Snowball", 11);
        addMapping(EntityFireball.class, "Fireball", 12);
        addMapping(EntitySmallFireball.class, "SmallFireball", 13);
        addMapping(EntityEnderPearl.class, "ThrownEnderpearl", 14);
        addMapping(EntityEnderEye.class, "EyeOfEnderSignal", 15);
        addMapping(EntityPotion.class, "ThrownPotion", 16);
        addMapping(EntityExpBottle.class, "ThrownExpBottle", 17);
        addMapping(EntityTNTPrimed.class, "PrimedTnt", 20);
        addMapping(EntityFallingSand.class, "FallingSand", 21);
        addMapping(EntityMinecart.class, "Minecart", 40);
        addMapping(EntityBoat.class, "Boat", 41);
        addMapping(EntityLiving.class, "Mob", 48);
        addMapping(EntityMob.class, "Monster", 49);
        addMapping(EntityCreeper.class, "Creeper", 50, 894731, 0);
        addMapping(EntitySkeleton.class, "Skeleton", 51, 12698049, 4802889);
        addMapping(EntitySpider.class, "Spider", 52, 3419431, 11013646);
        addMapping(EntityGiantZombie.class, "Giant", 53);
        addMapping(EntityZombie.class, "Zombie", 54, 44975, 7969893);
        addMapping(EntitySlime.class, "Slime", 55, 5349438, 8306542);
        addMapping(EntityGhast.class, "Ghast", 56, 16382457, 12369084);
        addMapping(EntityPigZombie.class, "PigZombie", 57, 15373203, 5009705);
        addMapping(EntityEnderman.class, "Enderman", 58, 1447446, 0);
        addMapping(EntityCaveSpider.class, "CaveSpider", 59, 803406, 11013646);
        addMapping(EntitySilverfish.class, "Silverfish", 60, 7237230, 3158064);
        addMapping(EntityBlaze.class, "Blaze", 61, 16167425, 16775294);
        addMapping(EntityMagmaCube.class, "LavaSlime", 62, 3407872, 16579584);
        addMapping(EntityDragon.class, "EnderDragon", 63);
        addMapping(EntityPig.class, "Pig", 90, 15771042, 14377823);
        addMapping(EntitySheep.class, "Sheep", 91, 15198183, 16758197);
        addMapping(EntityCow.class, "Cow", 92, 4470310, 10592673);
        addMapping(EntityChicken.class, "Chicken", 93, 10592673, 16711680);
        addMapping(EntitySquid.class, "Squid", 94, 2243405, 7375001);
        addMapping(EntityWolf.class, "Wolf", 95, 14144467, 13545366);
        addMapping(EntityMooshroom.class, "MushroomCow", 96, 10489616, 12040119);
        addMapping(EntitySnowman.class, "SnowMan", 97);
        addMapping(EntityOcelot.class, "Ozelot", 98, 15720061, 5653556);
        addMapping(EntityIronGolem.class, "VillagerGolem", 99);
        addMapping(EntityVillager.class, "Villager", 120, 5651507, 12422002);
        addMapping(EntityEnderCrystal.class, "EnderCrystal", 200);
    }

    public static void addNewEntityListMapping(Class<? extends Entity> entityClass, String entityName, int id) {
        addMapping(entityClass, entityName, id);
    }

    public static void addNewEntityListMapping(Class<? extends Entity> entityClass, String entityName, int id, int backgroundEggColour, int foregroundEggColour) {
        addMapping(entityClass, entityName, id, backgroundEggColour, foregroundEggColour);
    }

    public static Map<String, Class<? extends Entity>> getEntityToClassMapping() {
        return stringToClassMapping;
    }
}
