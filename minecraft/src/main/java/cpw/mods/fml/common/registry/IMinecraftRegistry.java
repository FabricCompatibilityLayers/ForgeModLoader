package cpw.mods.fml.common.registry;

import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;

public interface IMinecraftRegistry {
    void removeSpawn(String entityName, EnumCreatureType spawnList, BiomeGenBase... biomes);

    void removeSpawn(Class<? extends EntityLiving> entityClass, EnumCreatureType typeOfCreature, BiomeGenBase... biomes);

    void removeBiome(BiomeGenBase biome);

    void addSpawn(String entityName, int weightedProb, int min, int max, EnumCreatureType spawnList, BiomeGenBase... biomes);

    void addSpawn(Class<? extends EntityLiving> entityClass, int weightedProb, int min, int max, EnumCreatureType typeOfCreature, BiomeGenBase... biomes);

    void addBiome(BiomeGenBase biome);

    void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id);

    void registerEntityID(Class<? extends Entity> entityClass, String entityName, int id, int backgroundEggColour, int foregroundEggColour);

    void registerEntityID(Class<? extends Entity> entityClass, String entityName, int id);

    void registerBlock(Block block, Class<? extends ItemBlock> itemclass);

    void registerBlock(Block block);

    void addSmelting(int input, ItemStack output);

    void addShapelessRecipe(ItemStack output, Object... params);

    void addRecipe(ItemStack output, Object... params);
}
