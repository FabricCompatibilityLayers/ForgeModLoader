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
package cpw.mods.fml.server;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.modloader.ModLoaderModContainer;
import cpw.mods.fml.common.modloader.ModProperty;
import cpw.mods.fml.common.registry.FMLRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Handles primary communication from hooked code into the system
 * <p>
 * The FML entry point is {@link #onPreLoad(MinecraftServer)} called from
 * {@link MinecraftServer}
 * <p>
 * Obfuscated code should focus on this class and other members of the "server"
 * (or "client") code
 * <p>
 * The actual mod loading is handled at arms length by {@link Loader}
 * <p>
 * It is expected that a similar class will exist for each target environment:
 * Bukkit and Client side.
 * <p>
 * It should not be directly modified.
 *
 * @author cpw
 */
public class FMLServerHandler implements IFMLSidedHandler {
    /**
     * The singleton
     */
    private static final FMLServerHandler INSTANCE = new FMLServerHandler();

    /**
     * A reference to the server itself
     */
    private MinecraftServer server;

    /**
     * A handy list of the default overworld biomes
     */
    private BiomeGenBase[] defaultOverworldBiomes;

    /**
     * @return the instance
     */
    public static FMLServerHandler instance() {
        return INSTANCE;
    }

    /**
     * Called to start the whole game off from
     *
     * @see MinecraftServer#startServer
     */
    public void onPreLoad(MinecraftServer minecraftServer) {
        try {
            Class.forName("BaseModMp", false, getClass().getClassLoader());
            MinecraftServer.logger.severe(new StringBuilder()
                .append("Forge Mod Loader has detected that this server has an ModLoaderMP installed alongside Forge Mod Loader.\n")
                .append("This will cause a serious problem with compatibility. To protect your worlds, this Minecraft server will now shutdown.\n")
                .append("You should follow the installation instructions of either Minecraft Forge of Forge Mod Loader and NOT install ModLoaderMP\n")
                .append("into the minecraft_server.jar file before this server will be allowed to start up.\n\n")
                .append("Failure to do so will simply result in more startup failures.\n\n")
                .append("The authors of Minecraft Forge and Forge Mod Loader strongly suggest you talk to your mod's authors and get them to\n")
                .append("update their requirements. ModLoaderMP is not compatible with Minecraft Forge on the server and they will need to update their mod\n")
                .append("for Minecraft Forge and other server compatibility, unless they are Minecraft Forge mods, in which case they already\n")
                .append("don't need ModLoaderMP and the mod author simply has failed to update his requirements and should be informed appropriately.\n\n")
                .append("The authors of Forge Mod Loader would like to be compatible with ModLoaderMP but it is closed source and owned by SDK.\n")
                .append("SDK, the author of ModLoaderMP, has a standing invitation to submit compatibility patches\n")
                .append("to the open source community project that is Forge Mod Loader so that this incompatibility doesn't last.\n")
                .append("Users who wish to enjoy mods of both types are encouraged to request of SDK that he submit a\ncompatibility patch to the Forge Mod Loader project at\n")
                .append("http://github.com/cpw/FML.\nPosting on the Minecraft forums at\n")
                .append("http://www.minecraftforum.net/topic/86765- (the MLMP thread)\n")
                .append("may encourage him in this effort. However, I ask that your requests be polite.\n")
                .append("Now, the server has to shutdown so you can reinstall your minecraft_server.jar\nproperly, until such time as we can work together.")
                .toString()
            );
            throw new RuntimeException("This FML based server has detected an installation of ModLoaderMP alongside. " +
                "This will cause serious compatibility issues, so the server will now shut down.");
        } catch (ClassNotFoundException ignored) {
            // We're safe. continue
        }
        server = minecraftServer;
        ReflectionHelper.detectObfuscation(World.class);
        FMLCommonHandler.instance().registerSidedDelegate(this);
        FMLRegistry.registerRegistry(new ServerRegistry());
        Loader.instance().loadMods();
    }

    /**
     * Called a bit later on during server initialization to finish loading mods
     */
    public void onLoadComplete() {
        Loader.instance().initializeMods();
    }

    /**
     * Every tick just before world and other ticks occur
     */
    public void onPreTick() {
        FMLCommonHandler.instance().worldTickStart();
    }

    /**
     * Every tick just after world and other ticks occur
     */
    public void onPostTick() {
        FMLCommonHandler.instance().worldTickEnd();
    }

    /**
     * Get the server instance
     */
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Get a handle to the server's logger instance
     */
    @Override
    public Logger getMinecraftLogger() {
        return MinecraftServer.logger;
    }

    /**
     * Called from ChunkProviderServer when a chunk needs to be populated
     * <p>
     * To avoid polluting the worldgen seed, we generate a new random from the
     * world seed and generate a seed from that
     */
    public void onChunkPopulate(IChunkProvider chunkProvider, int chunkX, int chunkZ, World world, IChunkProvider generator) {
        Random fmlRandom = new Random(world.getSeed());
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        fmlRandom.setSeed((xSeed * chunkX + zSeed * chunkZ) ^ world.getSeed());

        for (ModContainer mod : Loader.getModList()) {
            if (mod.generatesWorld()) {
                mod.getWorldGenerator().generate(fmlRandom, chunkX, chunkZ, world, generator, chunkProvider);
            }
        }
    }

    /**
     * Called from the furnace to lookup fuel values
     */
    public int fuelLookup(int itemId, int itemDamage) {
        int fv = 0;

        for (ModContainer mod : Loader.getModList()) {
            fv = Math.max(fv, mod.lookupFuelValue(itemId, itemDamage));
        }

        return fv;
    }

    /**
     * Is the offered class and instance of BaseMod and therefore a ModLoader mod?
     */
    @Override
    public boolean isModLoaderMod(Class<?> clazz) {
        return BaseMod.class.isAssignableFrom(clazz);
    }

    /**
     * Load the supplied mod class into a mod container
     */
    @Override
    public ModContainer loadBaseModMod(Class<?> clazz, File canonicalFile) {
        @SuppressWarnings("unchecked")
        Class<? extends BaseMod> bmClazz = (Class<? extends BaseMod>) clazz;
        return new ModLoaderModContainer(bmClazz, canonicalFile);
    }

    /**
     * Called to notify that an item was picked up from the world
     */
    public void notifyItemPickup(EntityItem entityItem, EntityPlayer entityPlayer) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsPickupNotification()) {
                mod.getPickupNotifier().notifyPickup(entityItem, entityPlayer);
            }
        }
    }

    /**
     * Raise an exception
     */
    public void raiseException(Throwable exception, String message, boolean stopGame) {
        FMLCommonHandler.instance().getFMLLogger().throwing("FMLHandler", "raiseException", exception);
        throw new RuntimeException(exception);
    }

    /**
     * Attempt to dispense the item as an entity other than just as the item itself
     */
    public boolean tryDispensingEntity(World world, double x, double y, double z, byte xVelocity, byte zVelocity, ItemStack item) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsToDispense() && mod.getDispenseHandler().dispense(x, y, z, xVelocity, zVelocity, world, item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Build a list of default overworld biomes
     */
    public BiomeGenBase[] getDefaultOverworldBiomes() {
        if (defaultOverworldBiomes == null) {
            ArrayList<BiomeGenBase> biomes = new ArrayList<>(20);

            for (int i = 0; i < 23; i++) {
                if ("Sky".equals(BiomeGenBase.biomeList[i].biomeName) || "Hell".equals(BiomeGenBase.biomeList[i].biomeName)) {
                    continue;
                }

                biomes.add(BiomeGenBase.biomeList[i]);
            }

            defaultOverworldBiomes = new BiomeGenBase[biomes.size()];
            biomes.toArray(defaultOverworldBiomes);
        }

        return defaultOverworldBiomes;
    }

    /**
     * Called when an item is crafted
     */
    public void onItemCrafted(EntityPlayer player, ItemStack craftedItem, IInventory craftingGrid) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsCraftingNotification()) {
                mod.getCraftingHandler().onCrafting(player, craftedItem, craftingGrid);
            }
        }
    }

    /**
     * Called when an item is smelted
     */
    public void onItemSmelted(EntityPlayer player, ItemStack smeltedItem) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsCraftingNotification()) {
                mod.getCraftingHandler().onSmelting(player, smeltedItem);
            }
        }
    }

    /**
     * Called when a chat packet is received
     *
     * @return true if you want the packet to stop processing and not echo to
     * the rest of the world
     */
    public boolean handleChatPacket(Packet3Chat chat, EntityPlayer player) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsNetworkPackets() && mod.getNetworkHandler().onChat(chat, player)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Called when a packet 250 packet is received from the player
     */
    public void handlePacket250(Packet250CustomPayload packet, EntityPlayer player) {
        if ("REGISTER".equals(packet.channel) || "UNREGISTER".equals(packet.channel)) {
            handleClientRegistration(packet, player);
            return;
        }

        ModContainer mod = FMLCommonHandler.instance().getModForChannel(packet.channel);

        if (mod != null) {
            mod.getNetworkHandler().onPacket250Packet(packet, player);
        }
    }

    /**
     * Handle register requests for packet 250 channels
     */
    private void handleClientRegistration(Packet250CustomPayload packet, EntityPlayer player) {
        if (packet.data == null) {
            return;
        }
        for (String channel : new String(packet.data, StandardCharsets.UTF_8).split("\0")) {
            // Skip it if we don't know it
            if (FMLCommonHandler.instance().getModForChannel(channel) == null) {
                continue;
            }

            if ("REGISTER".equals(packet.channel)) {
                FMLCommonHandler.instance().activateChannel(player, channel);
            } else {
                FMLCommonHandler.instance().deactivateChannel(player, channel);
            }
        }
    }

    /**
     * Handle a login
     */
    public void handleLogin(Packet1Login loginPacket, NetworkManager networkManager) {
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = "REGISTER";
        packet.data = FMLCommonHandler.instance().getPacketRegistry();
        packet.length = packet.data.length;
        if (packet.length > 0) {
            networkManager.addToSendQueue(packet);
        }
    }

    public void announceLogin(EntityPlayer player) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsPlayerTracking()) {
                mod.getPlayerTracker().onPlayerLogin(player);
            }
        }
    }

    /**
     * Are we a server?
     */
    @Override
    public boolean isServer() {
        return true;
    }

    /**
     * Are we a client?
     */
    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public File getMinecraftRootDirectory() {
        try {
            return server.getFile(".").getCanonicalFile();
        } catch (IOException ioe) {
            return new File(".");
        }
    }

    /**
     *
     */
    public boolean handleServerCommand(String command, String player, ICommandListener listener) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsConsoleCommands() && mod.getConsoleHandler().handleCommand(command, player, listener)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     */
    public void announceLogout(EntityPlayer player) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsPlayerTracking()) {
                mod.getPlayerTracker().onPlayerLogout(player);
            }
        }
    }

    /**
     *
     */
    public void announceDimensionChange(EntityPlayer player) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsPlayerTracking()) {
                mod.getPlayerTracker().onPlayerChangedDimension(player);
            }
        }
    }

    public void addBiomeToDefaultWorldGenerator(BiomeGenBase biome) {
        WorldType.DEFAULT.addNewBiome(biome);
    }

    @Override
    public Object getMinecraftInstance() {
        return server;
    }

    @Override
    public String getCurrentLanguage() {
        return StringTranslate.getInstance().getCurrentLanguage();
    }

    @Override
    public Properties getCurrentLanguageTable() {
        return StringTranslate.getInstance().getCurrentLanguageTable();
    }

    @Override
    public String getObjectName(Object instance) {
        String objectName;
        if (instance instanceof Item) {
            objectName = ((Item) instance).getItemName();
        } else if (instance instanceof Block) {
            objectName = ((Block) instance).getBlockName();
        } else if (instance instanceof ItemStack) {
            objectName = Item.itemsList[((ItemStack) instance).itemID].getItemNameIS((ItemStack) instance);
        } else {
            throw new IllegalArgumentException(String.format("Illegal object for naming %s", instance));
        }
        objectName += ".name";
        return objectName;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.IFMLSidedHandler#readMetadataFrom(java.io.InputStream, cpw.mods.fml.common.ModContainer)
     */
    @Override
    public ModMetadata readMetadataFrom(InputStream input, ModContainer mod) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.IFMLSidedHandler#profileStart(java.lang.String)
     */
    @Override
    public void profileStart(String profileLabel) {
        Profiler.startSection(profileLabel);
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.IFMLSidedHandler#profileEnd()
     */
    @Override
    public void profileEnd() {
        Profiler.endSection();
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.IFMLSidedHandler#getModLoaderPropertyFor(java.lang.reflect.Field)
     */
    @Override
    public ModProperty getModLoaderPropertyFor(Field f) {
        if (f.isAnnotationPresent(MLProp.class)) {
            MLProp prop = f.getAnnotation(MLProp.class);
            return new ModProperty(prop.info(), prop.min(), prop.max(), prop.name());
        }
        return null;
    }
}
