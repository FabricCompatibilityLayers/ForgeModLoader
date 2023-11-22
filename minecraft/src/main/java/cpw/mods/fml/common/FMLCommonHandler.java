/*
 * The FML Forge Mod Loader suite.
 * Copyright (C) 2012 cpw
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
package cpw.mods.fml.common;

import cpw.mods.fml.common.ModContainer.SourceType;
import cpw.mods.fml.common.ModContainer.TickType;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <p>The main class for non-obfuscated hook handling code</p>
 *
 * <p>Anything that doesn't require obfuscated or client/server specific code should
 * go in this handler</p>
 *
 * <p>It also contains a reference to the sided handler instance that is valid
 * allowing for common code to access specific properties from the obfuscated world
 * without a direct dependency</p>
 *
 * @author cpw
 */
public class FMLCommonHandler {
    /**
     * The singleton
     */
    private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();
    private static final Pattern metadataFile = Pattern.compile("/modinfo.json$");
    /**
     * A map of mods to their network channels
     */
    private final Map<ModContainer, Set<String>> channelList = new HashMap<>();
    /**
     * A map of channels to mods
     */
    private final Map<String, ModContainer> modChannels = new HashMap<>();
    /**
     * A map of active channels per player
     */
    private final Map<Object, Set<String>> activeChannels = new HashMap<>();
    private final List<ModContainer> auxilliaryContainers = new ArrayList<>();
    private final Map<String, Properties> modLanguageData = new HashMap<>();
    /**
     * The delegate for side specific data and functions
     */
    private IFMLSidedHandler sidedDelegate;
    private int uniqueEntityListId = 220;

    /**
     * @return the instance
     */
    public static FMLCommonHandler instance() {
        return INSTANCE;
    }

    /**
     * We register our delegate here
     */
    public void registerSidedDelegate(IFMLSidedHandler handler) {
        sidedDelegate = handler;
    }

    /**
     * Pre-tick the mods
     */
    public void worldTickStart() {
        tickStart(ModContainer.TickType.WORLD, 0.0f);
    }

    /**
     * Post-tick the mods
     */
    public void worldTickEnd() {
        tickEnd(ModContainer.TickType.WORLD, 0.0f);
    }

    public void tickStart(TickType type, Object... data) {
        sidedDelegate.profileStart("modTickStart");
        sidedDelegate.profileStart(type.name());
        for (ModContainer mod : Loader.getModList()) {
            sidedDelegate.profileStart(mod.getName());
            mod.tickStart(type, data);
            sidedDelegate.profileEnd();
        }
        for (ModContainer mod : auxilliaryContainers) {
            sidedDelegate.profileStart(mod.getMod().getClass().getSimpleName());
            mod.tickStart(type, data);
            sidedDelegate.profileEnd();
        }
        sidedDelegate.profileEnd();
        sidedDelegate.profileEnd();
    }

    public void tickEnd(TickType type, Object... data) {
        sidedDelegate.profileStart("modTickEnd");
        sidedDelegate.profileStart(type.name());
        for (ModContainer mod : Loader.getModList()) {
            sidedDelegate.profileStart(mod.getName());
            mod.tickEnd(type, data);
            sidedDelegate.profileEnd();
        }
        for (ModContainer mod : auxilliaryContainers) {
            sidedDelegate.profileStart(mod.getMod().getClass().getSimpleName());
            mod.tickEnd(type, data);
            sidedDelegate.profileEnd();
        }
        sidedDelegate.profileEnd();
        sidedDelegate.profileEnd();
    }

    public List<IKeyHandler> gatherKeyBindings() {
        List<IKeyHandler> allKeys = new ArrayList<>();
        for (ModContainer mod : Loader.getModList()) {
            allKeys.addAll(mod.getKeys());
        }
        for (ModContainer mod : auxilliaryContainers) {
            allKeys.addAll(mod.getKeys());
        }
        return allKeys;
    }

    /**
     * Lookup the mod for a channel
     */
    public ModContainer getModForChannel(String channel) {
        return modChannels.get(channel);
    }

    /**
     * Get the channel list for a mod
     */
    public Set<String> getChannelListFor(ModContainer container) {
        return channelList.get(container);
    }

    /**
     * register a channel to a mod
     */
    public void registerChannel(ModContainer container, String channelName) {
        if (modChannels.containsKey(channelName)) {
            return;
            // NOOP
        }

        Set<String> list = channelList.computeIfAbsent(container, k -> new HashSet<>());

        list.add(channelName);
        modChannels.put(channelName, container);
    }

    /**
     * Activate the channel for the player
     */
    public void activateChannel(Object player, String channel) {
        Set<String> active = activeChannels.computeIfAbsent(player, k -> new HashSet<>());

        active.add(channel);
    }

    /**
     * Deactivate the channel for the player
     */
    public void deactivateChannel(Object player, String channel) {
        Set<String> active = activeChannels.computeIfAbsent(player, k -> new HashSet<>());

        active.remove(channel);
    }

    /**
     * Get the packet 250 channel registration string
     */
    public byte[] getPacketRegistry() {
        StringBuilder sb = new StringBuilder();

        for (String chan : modChannels.keySet()) {
            sb.append(chan).append("\0");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Is the specified channel active for the player?
     */
    public boolean isChannelActive(String channel, Object player) {
        return activeChannels.get(player).contains(channel);
    }

    /**
     * Get the forge mod loader logging instance (goes to the forgemodloader log file)
     */
    public Logger getFMLLogger() {
        return Loader.log;
    }

    /**
     * Get the minecraft logger (goes to the server log file)
     */
    public Logger getMinecraftLogger() {
        return sidedDelegate.getMinecraftLogger();
    }

    /**
     * Is this a modloader mod?
     */
    public boolean isModLoaderMod(Class<?> clazz) {
        return sidedDelegate.isModLoaderMod(clazz);
    }

    /**
     * Load the modloader mod
     */
    public ModContainer loadBaseModMod(Class<?> clazz, File canonicalFile) {
        return sidedDelegate.loadBaseModMod(clazz, canonicalFile);
    }

    public File getMinecraftRootDirectory() {
        return sidedDelegate.getMinecraftRootDirectory();
    }

    public Object getMinecraftInstance() {
        return sidedDelegate.getMinecraftInstance();
    }

    public int nextUniqueEntityListId() {
        return uniqueEntityListId++;
    }

    public void addStringLocalization(String key, String lang, String value) {
        Properties langPack = modLanguageData.get(lang);
        if (langPack == null) {
            langPack = new Properties();
            modLanguageData.put(lang, langPack);
        }
        langPack.put(key, value);

        handleLanguageLoad(sidedDelegate.getCurrentLanguageTable(), lang);
    }

    public void handleLanguageLoad(Properties languagePack, String lang) {
        Properties usPack = modLanguageData.get("en_US");
        if (usPack != null) {
            languagePack.putAll(usPack);
        }
        Properties langPack = modLanguageData.get(lang);
        if (langPack == null) {
            return;
        }
        languagePack.putAll(langPack);
    }

    public boolean isServer() {
        return sidedDelegate.isServer();
    }

    public boolean isClient() {
        return sidedDelegate.isClient();
    }

    public void addAuxilliaryModContainer(ModContainer ticker) {
        auxilliaryContainers.add(ticker);
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

    public void addNameForObject(Object minecraftObject, String lang, String name) {
        String label = sidedDelegate.getObjectName(minecraftObject);
        addStringLocalization(label, lang, name);
    }


    /**
     * Raise an exception
     */
    public void raiseException(Throwable exception, String message, boolean stopGame) {
        FMLCommonHandler.instance().getFMLLogger().throwing("FMLHandler", "raiseException", exception);
        throw new RuntimeException(exception);
    }

    public String[] getBrandingStrings(String mcVersion) {
        ArrayList<String> brandings = new ArrayList<>();
        brandings.add(mcVersion);
        brandings.add(Loader.instance().getFMLVersionString());
        try {
            brandings.add((String) Class.forName("forge.MinecraftForge").getMethod("getVersionString").invoke(null));
        } catch (Exception ex) {
            try {
                brandings.add((String) Class.forName("net.minecraft.src.forge.MinecraftForge").getMethod("getVersionString").invoke(null));
            } catch (Exception ex2) {
                // Ignore- forge isn't loaded
            }
        }
        try {
            Properties props = new Properties();
            props.load(FMLCommonHandler.class.getClassLoader().getResourceAsStream("fmlbranding.properties"));
            brandings.add(props.getProperty("fmlbranding"));
        } catch (Exception ex) {
            // Ignore - no branding file found
        }
        brandings.add(String.format("%d mod%s loaded", Loader.getModList().size(), Loader.getModList().size() != 1 ? "s" : ""));
        Collections.reverse(brandings);
        return brandings.toArray(new String[0]);
    }

    public void loadMetadataFor(ModContainer mod) {
        if (mod.getSourceType() == SourceType.JAR) {
            try (ZipFile jar = new ZipFile(mod.getSource())) {
                ZipEntry infoFile = jar.getEntry("mcmod.info");
                if (infoFile != null) {
                    InputStream input = jar.getInputStream(infoFile);
                    ModMetadata data = sidedDelegate.readMetadataFrom(input, mod);
                    mod.setMetadata(data);
                } else {
                    getFMLLogger().fine(String.format("Failed to find mcmod.info file in %s for %s", mod.getSource().getName(), mod.getName()));
                }
            } catch (Exception e) {
                // Something wrong but we don't care
                getFMLLogger().fine(String.format("Failed to find mcmod.info file in %s for %s", mod.getSource().getName(), mod.getName()));
                getFMLLogger().throwing("FMLCommonHandler", "loadMetadataFor", e);
            }
        } else {
            try {
                InputStream input = Loader.instance().getModClassLoader().getResourceAsStream(mod.getName() + ".info");
                if (input == null) {
                    input = Loader.instance().getModClassLoader().getResourceAsStream("net/minecraft/src/" + mod.getName() + ".info");
                }
                if (input != null) {
                    ModMetadata data = sidedDelegate.readMetadataFrom(input, mod);
                    mod.setMetadata(data);
                }
            } catch (Exception e) {
                // Something wrong but we don't care
                getFMLLogger().fine(String.format("Failed to find %s.info file in %s for %s", mod.getName(), mod.getSource().getName(), mod.getName()));
                getFMLLogger().throwing("FMLCommonHandler", "loadMetadataFor", e);
            }
        }
    }

    public IFMLSidedHandler getSidedDelegate() {
        return sidedDelegate;
    }
}
