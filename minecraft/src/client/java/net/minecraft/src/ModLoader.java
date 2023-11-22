/*
 * The FML Forge Mod Loader suite. Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.minecraft.src;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.SpriteHelper;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ReflectionHelper;
import cpw.mods.fml.common.modloader.ModLoaderHelper;
import cpw.mods.fml.common.modloader.ModLoaderModContainer;
import cpw.mods.fml.common.registry.FMLRegistry;
import net.minecraft.client.Minecraft;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ModLoader {
    // TODO dirty workaround for Millénaire
    @Deprecated
    public static final Map<String, Map<String, String>> localizedStrings = Collections.emptyMap();

    /**
     * Not used on the server.
     */
    public static void addAchievementDesc(Achievement achievement, String name, String description) {
        String achName = achievement.getName();
        addLocalization(achName, name);
        addLocalization(achName + ".desc", description);
    }

    /**
     * This method is a call in the hook from modified external code. Implemented elsewhere.
     *
     * @see FMLCommonHandler#fuelLookup(int, int)
     */
    @Deprecated
    public static int addAllFuel(int id, int metadata) {
        return 0;
    }

    @Deprecated
    public static void addAllRenderers(Map<Class<? extends Entity>, Render> renderers) {
    }

    public static void addAnimation(TextureFX anim) {
        FMLClientHandler.instance().addAnimation(anim);
    }

    /**
     * This method is unimplemented in server versions to date.
     */
    public static int addArmor(String armor) {
        return FMLClientHandler.instance().addNewArmourRendererPrefix(armor);
    }

    /**
     * This method adds the supplied biome to the set of candidate biomes for the default world generator type.
     */
    public static void addBiome(BiomeGenBase biome) {
        FMLClientHandler.instance().addBiomeToDefaultWorldGenerator(biome);
    }

    /**
     * Add localization for the specified string
     */
    public static void addLocalization(String key, String value) {
        addLocalization(key, "en_US", value);
    }

    /**
     * Add localization for the specified string
     */
    public static void addLocalization(String key, String lang, String value) {
        FMLCommonHandler.instance().addStringLocalization(key, lang, value);
    }

    /**
     * Name the specified Minecraft object with the supplied name
     */
    public static void addName(Object instance, String name) {
        addName(instance, "en_US", name);
    }

    /**
     * Unimplemented on the server as it does not generate names
     */
    public static void addName(Object instance, String lang, String name) {
        FMLCommonHandler.instance().addNameForObject(instance, lang, name);
    }

    /**
     * Unimplemented on the server as it does not render textures
     */
    public static int addOverride(String fileToOverride, String fileToAdd) {
        int idx = SpriteHelper.getUniqueSpriteIndex(fileToOverride);
        addOverride(fileToOverride, fileToAdd, idx);
        return idx;
    }

    /**
     * Unimplemented on the server as it does not render textures
     */
    public static void addOverride(String path, String overlayPath, int index) {
        FMLClientHandler.instance().addNewTextureOverride(path, overlayPath, index);
    }

    /**
     * Add a Shaped Recipe
     */
    public static void addRecipe(ItemStack output, Object... params) {
        FMLRegistry.addRecipe(output, params);
    }

    /**
     * Add a shapeless recipe
     */
    public static void addShapelessRecipe(ItemStack output, Object... params) {
        FMLRegistry.addShapelessRecipe(output, params);
    }

    /**
     * Add a new product to be smelted
     */
    public static void addSmelting(int input, ItemStack output) {
        FMLRegistry.addSmelting(input, output);
    }

    /**
     * Add a mob to the spawn list
     */
    public static void addSpawn(Class<? extends EntityLiving> entityClass, int weightedProb, int min, int max, EnumCreatureType spawnList) {
        FMLRegistry.addSpawn(entityClass, weightedProb, min, max, spawnList, FMLClientHandler.instance().getDefaultOverworldBiomes());
    }

    /**
     * Add a mob to the spawn list
     */
    public static void addSpawn(Class<? extends EntityLiving> entityClass, int weightedProb, int min, int max, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        FMLRegistry.addSpawn(entityClass, weightedProb, min, max, spawnList, biomes);
    }

    /**
     * Add a mob to the spawn list
     */
    public static void addSpawn(String entityName, int weightedProb, int min, int max, EnumCreatureType spawnList) {
        FMLRegistry.addSpawn(entityName, weightedProb, min, max, spawnList, FMLClientHandler.instance().getDefaultOverworldBiomes());
    }

    /**
     * Add a mob to the spawn list
     */
    public static void addSpawn(String entityName, int weightedProb, int min, int max, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        FMLRegistry.addSpawn(entityName, weightedProb, min, max, spawnList, biomes);
    }

    /**
     * This method is a call in the hook from modified external code. Implemented elsewhere.
     *
     * @see FMLClientHandler#tryDispensingEntity(World, double, double, double, byte, byte, ItemStack)
     */
    @Deprecated
    public static boolean dispenseEntity(World world, double x, double y, double z, int xVel, int zVel, ItemStack item) {
        return false;
    }

    /**
     * Remove a container and drop all the items in it on the ground around
     */
    public static void genericContainerRemoval(World world, int x, int y, int z) {
        TileEntity te = world.getBlockTileEntity(x, y, z);

        if (!(te instanceof IInventory)) {
            return;
        }

        IInventory inv = (IInventory) te;

        for (int l = 0; l < inv.getSizeInventory(); l++) {
            ItemStack itemstack = inv.getStackInSlot(l);

            if (itemstack == null) {
                continue;
            }

            float f = world.rand.nextFloat() * 0.8F + 0.1F;
            float f1 = world.rand.nextFloat() * 0.8F + 0.1F;
            float f2 = world.rand.nextFloat() * 0.8F + 0.1F;

            while (itemstack.stackSize > 0) {
                int i1 = world.rand.nextInt(21) + 10;

                if (i1 > itemstack.stackSize) {
                    i1 = itemstack.stackSize;
                }

                itemstack.stackSize -= i1;
                EntityItem entityitem = new EntityItem(world, (float) te.xCoord + f, (float) te.yCoord + f1, (float) te.zCoord + f2, new ItemStack(itemstack.itemID, i1, itemstack.getItemDamage()));
                float f3 = 0.05F;
                entityitem.motionX = (float) world.rand.nextGaussian() * f3;
                entityitem.motionY = (float) world.rand.nextGaussian() * f3 + 0.2F;
                entityitem.motionZ = (float) world.rand.nextGaussian() * f3;

                if (itemstack.hasTagCompound()) {
                    entityitem.item.setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());
                }

                world.spawnEntityInWorld(entityitem);
            }
        }
    }

    /**
     * Get a list of all BaseMod loaded into the system
     *
     * @see ModLoaderModContainer#findAll
     */
    public static List<BaseMod> getLoadedMods() {
        return ModLoaderModContainer.findAll(BaseMod.class);
    }

    /**
     * Get a logger instance from {@link FMLCommonHandler#getFMLLogger()}
     */
    public static Logger getLogger() {
        return FMLCommonHandler.instance().getFMLLogger();
    }

    public static Minecraft getMinecraftInstance() {
        return FMLClientHandler.instance().getClient();
    }

    /**
     * This is not the server
     *
     * @see FMLClientHandler#getClient()
     */
    public static Object getMinecraftServerInstance() {
        return getMinecraftInstance();
    }

    /**
     * Get a value from a field using reflection
     *
     * @see ReflectionHelper#getPrivateValue(Class, Object, int)
     */
    public static <T, E> T getPrivateValue(Class<? super E> instanceclass, E instance, int fieldIndex) {
        return ReflectionHelper.getPrivateValue(instanceclass, instance, fieldIndex);
    }

    /**
     * Get a value from a field using reflection
     *
     * @see ReflectionHelper#getPrivateValue(Class, Object, String)
     */
    public static <T, E> T getPrivateValue(Class<? super E> instanceclass, E instance, String field) {
        return ReflectionHelper.getPrivateValue(instanceclass, instance, field);
    }

    /**
     * Stubbed method on the server to return a unique model id
     */
    public static int getUniqueBlockModelID(BaseMod mod, boolean inventoryRenderer) {
        return FMLClientHandler.instance().obtainBlockModelIdFor(mod, inventoryRenderer);
    }

    /**
     * Get a new unique entity id
     *
     * @see Entity#getNextId()
     */
    public static int getUniqueEntityId() {
        return FMLCommonHandler.instance().nextUniqueEntityListId();
    }

    public static int getUniqueSpriteIndex(String path) {
        return SpriteHelper.getUniqueSpriteIndex(path);
    }

    /**
     * To properly implement packet 250 protocol you should always check your
     * channel is active prior to sending the packet
     */
    public static boolean isChannelActive(EntityPlayer player, String channel) {
        return FMLCommonHandler.instance().isChannelActive(channel, player);
    }

    public static boolean isGUIOpen(Class<? extends GuiScreen> gui) {
        return FMLClientHandler.instance().getClient().currentScreen != null && FMLClientHandler.instance().getClient().currentScreen.getClass().equals(gui);
    }

    /**
     * Is the named mod loaded?
     *
     * @see Loader#isModLoaded(String)
     */
    public static boolean isModLoaded(String modname) {
        return Loader.isModLoaded(modname);
    }

    /**
     * Implemented elsewhere
     */
    @Deprecated
    public static void loadConfig() {
    }

    public static BufferedImage loadImage(RenderEngine renderEngine, String path) throws Exception {
        return FMLClientHandler.instance().loadImageFromTexturePack(renderEngine, path);
    }

    /**
     * Call in from elsewhere. Unimplemented here.
     */
    @Deprecated
    public static void onItemPickup(EntityPlayer player, ItemStack item) {
    }

    /**
     * Call in from elsewhere. Unimplemented here.
     */
    @Deprecated
    public static void onTick(float tick, Minecraft game) {
    }

    public static void openGUI(EntityPlayer player, GuiScreen gui) {
        FMLClientHandler.instance().displayGuiScreen(player, gui);
    }

    @Deprecated
    public static void populateChunk(IChunkProvider generator, int chunkX, int chunkZ, World world) {
    }

    /**
     * This method is a call in the hook from modified external code. Implemented elsewhere.
     *
     * @see FMLClientHandler#handlePacket250(Packet250CustomPayload)
     */
    @Deprecated
    public static void receivePacket(Packet250CustomPayload packet) {
    }

    @Deprecated
    public static KeyBinding[] registerAllKeys(KeyBinding[] keys) {
        return keys;
    }

    @Deprecated
    public static void registerAllTextureOverrides(RenderEngine cache) {
    }

    /**
     * Register a new block
     */
    public static void registerBlock(Block block) {
        FMLRegistry.registerBlock(block);
    }

    /**
     * Register a new block
     */
    public static void registerBlock(Block block, Class<? extends ItemBlock> itemclass) {
        FMLRegistry.registerBlock(block, itemclass);
    }

    /**
     * Register a new entity ID
     */
    public static void registerEntityID(Class<? extends Entity> entityClass, String entityName, int id) {
        FMLRegistry.registerEntityID(entityClass, entityName, id);
    }

    /**
     * Register a new entity ID
     */
    public static void registerEntityID(Class<? extends Entity> entityClass, String entityName, int id, int background, int foreground) {
        FMLRegistry.registerEntityID(entityClass, entityName, id, background, foreground);
    }

    public static void registerKey(BaseMod mod, KeyBinding keyHandler, boolean allowRepeat) {
        FMLClientHandler.instance().registerKeyHandler(mod, keyHandler, allowRepeat);
    }

    /**
     * Register the mod for packets on this channel. This only registers the
     * channel with Forge Mod Loader, not with clients connecting, use
     * BaseMod.onClientLogin to tell them about your custom channel
     *
     * @see FMLCommonHandler#registerChannel(cpw.mods.fml.common.ModContainer, String)
     */
    public static void registerPacketChannel(BaseMod mod, String channel) {
        FMLCommonHandler.instance().registerChannel(ModLoaderModContainer.findContainerFor(mod), channel);
    }

    /**
     * Register a new tile entity class
     */
    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id) {
        FMLRegistry.registerTileEntity(tileEntityClass, id);
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id, TileEntitySpecialRenderer renderer) {
        ClientRegistry.instance().registerTileEntity(tileEntityClass, id, renderer);
    }

    /**
     * Remove a biome from the list of generated biomes
     */
    public static void removeBiome(BiomeGenBase biome) {
        FMLRegistry.removeBiome(biome);
    }

    /**
     * Remove a spawn
     */
    public static void removeSpawn(Class<? extends EntityLiving> entityClass, EnumCreatureType spawnList) {
        FMLRegistry.removeSpawn(entityClass, spawnList, FMLClientHandler.instance().getDefaultOverworldBiomes());
    }

    /**
     * Remove a spawn
     */
    public static void removeSpawn(Class<? extends EntityLiving> entityClass, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        FMLRegistry.removeSpawn(entityClass, spawnList, biomes);
    }

    /**
     * Remove a spawn
     */
    public static void removeSpawn(String entityName, EnumCreatureType spawnList) {
        FMLRegistry.removeSpawn(entityName, spawnList, FMLClientHandler.instance().getDefaultOverworldBiomes());
    }

    /**
     * Remove a spawn
     */
    public static void removeSpawn(String entityName, EnumCreatureType spawnList, BiomeGenBase... biomes) {
        FMLRegistry.removeSpawn(entityName, spawnList, biomes);
    }

    @Deprecated
    public static boolean renderBlockIsItemFull3D(int modelID) {
        return FMLClientHandler.instance().renderItemAsFull3DBlock(modelID);
    }

    @Deprecated
    public static void renderInvBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {
        FMLClientHandler.instance().renderInventoryBlock(renderer, block, metadata, modelID);
    }

    @Deprecated
    public static boolean renderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelID) {
        return FMLClientHandler.instance().renderWorldBlock(renderer, world, x, y, z, block, modelID);
    }

    /**
     * Configuration is handled elsewhere
     * @see ModLoaderModContainer
     */
    @Deprecated
    public static void saveConfig() {
    }

    public static void sendPacket(Packet packet) {
        FMLClientHandler.instance().sendPacket(packet);
    }

    /**
     * Send a chat message to the server
     *
     * @see FMLClientHandler#handleChatPacket(Packet3Chat)
     */
    @Deprecated
    public static void serverChat(String text) {
        //TODO
    }

    @Deprecated
    public static void serverLogin(NetClientHandler handler, Packet1Login loginPacket) {
        //TODO
    }

    /**
     * Indicate that you want to receive ticks
     *
     * @param mod      receiving the events
     * @param enable   indicates whether you want to receive them or not
     * @param useClock don't receive render sub-ticks, just world ticks
     */
    public static void setInGameHook(BaseMod mod, boolean enable, boolean useClock) {
        ModLoaderHelper.updateStandardTicks(mod, enable, useClock);
    }


    public static void setInGUIHook(BaseMod mod, boolean enable, boolean useClock) {
        ModLoaderHelper.updateGUITicks(mod, enable, useClock);
    }

    /**
     * Set a private field to a value using reflection
     * @see ReflectionHelper#setPrivateValue(Class, Object, int, Object)
     */
    public static <T, E> void setPrivateValue(Class<? super T> instanceclass, T instance, int fieldIndex, E value) {
        ReflectionHelper.setPrivateValue(instanceclass, instance, fieldIndex, value);
    }

    /**
     * Set a private field to a value using reflection
     * @see ReflectionHelper#setPrivateValue(Class, Object, String, Object)
     */
    public static <T, E> void setPrivateValue(Class<? super T> instanceclass, T instance, String field, E value) {
        ReflectionHelper.setPrivateValue(instanceclass, instance, field, value);
    }

    /**
     * This method is a call in the hook from modified external code. Implemented elsewhere.
     * @see FMLClientHandler#onItemCrafted(EntityPlayer, ItemStack, IInventory)
     */
    @Deprecated
    public static void takenFromCrafting(EntityPlayer player, ItemStack item, IInventory matrix) {
    }

    /**
     * This method is a call in the hook from modified external code. Implemented elsewhere.
     *
     * @see FMLClientHandler#onItemSmelted(EntityPlayer, ItemStack)
     */
    @Deprecated
    public static void takenFromFurnace(EntityPlayer player, ItemStack item) {
    }

    /**
     * Throw the offered exception. Likely will stop the game.
     *
     * @see FMLCommonHandler#raiseException(Throwable, String, boolean)
     */
    public static void throwException(String message, Throwable e) {
        FMLCommonHandler.instance().raiseException(e, message, true);
    }

    public static void throwException(Throwable e) {
        throwException("Exception in ModLoader", e);
    }
}
