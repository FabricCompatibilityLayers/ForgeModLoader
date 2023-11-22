package net.minecraft.src;

import java.util.Arrays;
import java.util.List;

public class WorldType {
    public static final WorldType[] worldTypes = new WorldType[16];
    public static final WorldType DEFAULT = new WorldType(0, "default", 1) {
        @Override
        public boolean func_48453_c() {
            return true;
        }

        @Override
        public WorldChunkManager getChunkManager(World var1) {
            return new WorldChunkManagerHell(BiomeGenBase.plains, 0.5F, 0.5F);
        }

        @Override
        public IChunkProvider getChunkGenerator(World var1) {
            return new ChunkProviderFlat(var1, var1.getSeed(), var1.getWorldInfo().isMapFeaturesEnabled());
        }

        @Override
        public int getSeaLevel(World world) {
            return 4;
        }

        @Override
        public boolean hasVoidParticles(boolean var1) {
            return false;
        }

        @Override
        public double voidFadeMagnitude() {
            return 1.0D;
        }
    };
    public static final WorldType FLAT = new WorldType(1, "flat");
    public static final WorldType DEFAULT_1_1 = new WorldType(8, "default_1_1", 0).setCanBeCreated(false);
    private final String worldType;
    private final int generatorVersion;
    private boolean canBeCreated;
    private boolean field_48460_h;

    private BiomeGenBase[] biomesForWorldType;

    private WorldType(int i, String string) {
        this(i, string, 0);
    }

    private WorldType(int i, String string, int j) {
        this.worldType = string;
        this.generatorVersion = j;
        this.canBeCreated = true;
        worldTypes[i] = this;

        if (i == 8) biomesForWorldType = new BiomeGenBase[] {
            BiomeGenBase.desert, BiomeGenBase.forest, BiomeGenBase.extremeHills,
            BiomeGenBase.swampland, BiomeGenBase.plains, BiomeGenBase.taiga
        };
        else biomesForWorldType = new BiomeGenBase[] {
            BiomeGenBase.desert, BiomeGenBase.forest, BiomeGenBase.extremeHills,
            BiomeGenBase.swampland, BiomeGenBase.plains, BiomeGenBase.taiga, BiomeGenBase.jungle
        };
    }

    public static WorldType parseWorldType(String par0Str) {
        for (WorldType type : worldTypes) {
            if (type != null && type.worldType.equalsIgnoreCase(par0Str)) {
                return type;
            }
        }

        return null;
    }

    public String func_48449_a() {
        return this.worldType;
    }

    public int getGeneratorVersion() {
        return this.generatorVersion;
    }

    public WorldType func_48451_a(int i) {
        return this == DEFAULT && i == 0 ? DEFAULT_1_1 : this;
    }

    private WorldType setCanBeCreated(boolean par1) {
        this.canBeCreated = par1;
        return this;
    }

    private WorldType func_48448_d() {
        this.field_48460_h = true;
        return this;
    }

    public boolean func_48453_c() {
        return this.field_48460_h;
    }

    public WorldChunkManager getChunkManager(World var1) {
        return new WorldChunkManager(var1);
    }

    public IChunkProvider getChunkGenerator(World var1) {
        return new ChunkProviderGenerate(var1, var1.getSeed(), var1.getWorldInfo().isMapFeaturesEnabled());
    }

    public int getSeaLevel(World var1) {
        return 64;
    }

    public boolean hasVoidParticles(boolean var1) {
        return !var1;
    }

    public double voidFadeMagnitude() {
        return 0.03125D;
    }

    public BiomeGenBase[] getBiomesForWorldType() {
        return biomesForWorldType;
    }

    public void addNewBiome(BiomeGenBase biome) {
        List<BiomeGenBase> biomes = Arrays.asList(biomesForWorldType);
        biomes.add(biome);
        biomesForWorldType = biomes.toArray(new BiomeGenBase[0]);
    }

    public void removeBiome(BiomeGenBase biome) {
        List<BiomeGenBase> biomes = Arrays.asList(biomesForWorldType);
        biomes.remove(biome);
        biomesForWorldType = biomes.toArray(new BiomeGenBase[0]);
    }
}
