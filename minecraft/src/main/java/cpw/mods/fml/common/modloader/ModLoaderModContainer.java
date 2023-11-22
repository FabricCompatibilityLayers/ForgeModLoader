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
package cpw.mods.fml.common.modloader;

import cpw.mods.fml.common.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ModLoaderModContainer implements ModContainer {
    private Class<? extends BaseMod> modClazz;
    private BaseMod mod;
    private EnumSet<TickType> ticks;
    private File modSource;
    private ArrayList<String> dependencies;
    private ArrayList<String> preDependencies;
    private ArrayList<String> postDependencies;
    private ArrayList<IKeyHandler> keyHandlers;
    private ModState state;
    private SourceType sourceType;
    private ModMetadata metadata;

    public ModLoaderModContainer(Class<? extends BaseMod> modClazz, File modSource) {
        this.modClazz = modClazz;
        this.modSource = modSource;
        this.ticks = EnumSet.noneOf(TickType.class);
        // We are unloaded
        nextState();
    }

    /**
     * We only instantiate this for "not mod mods"
     */
    ModLoaderModContainer(BaseMod instance) {
        FMLCommonHandler.instance().addAuxilliaryModContainer(this);
        this.mod = instance;
        this.ticks = EnumSet.noneOf(TickType.class);
    }

    public static ModContainer findContainerFor(BaseMod mod) {
        for (ModContainer mc : Loader.getModList()) {
            if (mc.matches(mod)) {
                return mc;
            }
        }

        return null;
    }

    /**
     * Find all the BaseMods in the system
     */
    @SuppressWarnings("unchecked")
    public static <A extends BaseMod> List<A> findAll(Class<A> clazz) {
        ArrayList<A> modList = new ArrayList<>();

        for (ModContainer mc : Loader.getModList()) {
            if (mc instanceof ModLoaderModContainer && mc.getMod() != null) {
                modList.add((A) ((ModLoaderModContainer) mc).mod);
            }
        }

        return modList;
    }

    @Override
    public boolean wantsPreInit() {
        return true;
    }

    @Override
    public boolean wantsPostInit() {
        return true;
    }

    @Override
    public void preInit() {
        try {
            configureMod();
            mod = modClazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new LoaderException(e);
        }
    }

    @Override
    public ModState getModState() {
        return state;
    }

    @Override
    public void nextState() {
        if (state == null) {
            state = ModState.UNLOADED;
            return;
        }
        if (state.ordinal() + 1 < ModState.values().length) {
            state = ModState.values()[state.ordinal() + 1];
        }
    }

    /**
     *
     */
    private void configureMod() {
        IFMLSidedHandler sideHandler = FMLCommonHandler.instance().getSidedDelegate();
        File configDir = Loader.instance().getConfigDir();
        String modConfigName = modClazz.getSimpleName();
        File modConfig = new File(configDir, String.format("%s.cfg", modConfigName));
        Properties props = new Properties();

        if (modConfig.exists()) {
            try {
                Loader.log.fine(String.format("Reading existing configuration file for %s : %s", modConfigName, modConfig.getName()));
                FileReader configReader = new FileReader(modConfig);
                props.load(configReader);
                configReader.close();
            } catch (Exception e) {
                Loader.log.severe(String.format("Error occured reading mod configuration file %s", modConfig.getName()));
                Loader.log.throwing("ModLoaderModContainer", "configureMod", e);
                throw new LoaderException(e);
            }
        }

        StringBuilder comments = new StringBuilder();
        comments.append("MLProperties: name (type:default) min:max -- information\n");

        try {
            for (Field f : modClazz.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    continue;
                }

                ModProperty property = sideHandler.getModLoaderPropertyFor(f);
                if (property == null) {
                    continue;
                }
                String propertyName = property.name().length() > 0 ? property.name() : f.getName();
                String propertyValue = null;
                Object defaultValue = null;

                try {
                    defaultValue = f.get(null);
                    propertyValue = props.getProperty(propertyName, extractValue(defaultValue));
                    Object currentValue = parseValue(propertyValue, property, f.getType(), propertyName, modConfigName);
                    Loader.log.finest(String.format("Configuration for %s.%s found values default: %s, configured: %s, interpreted: %s", modConfigName, propertyName, defaultValue, propertyValue, currentValue));

                    if (currentValue != null && !currentValue.equals(defaultValue)) {
                        Loader.log.finest(String.format("Configuration for %s.%s value set to: %s", modConfigName, propertyName, currentValue));
                        f.set(null, currentValue);
                    }
                } catch (Exception e) {
                    Loader.log.severe(String.format("Invalid configuration found for %s in %s", propertyName, modConfig.getName()));
                    Loader.log.throwing("ModLoaderModContainer", "configureMod", e);
                    throw new LoaderException(e);
                } finally {
                    comments.append(String.format("MLProp : %s (%s:%s", propertyName, f.getType().getName(), defaultValue));

                    if (property.min() != Double.MIN_VALUE) {
                        comments.append(",>=").append(String.format("%.1f", property.min()));
                    }

                    if (property.max() != Double.MAX_VALUE) {
                        comments.append(",<=").append(String.format("%.1f", property.max()));
                    }

                    comments.append(")");

                    if (property.info().length() > 0) {
                        comments.append(" -- ").append(property.info());
                    }

                    if (propertyValue != null) {
                        props.setProperty(propertyName, extractValue(propertyValue));
                    }
                    comments.append("\n");
                }
            }
        } finally {
            try {
                FileWriter configWriter = new FileWriter(modConfig);
                props.store(configWriter, comments.toString());
                configWriter.close();
                Loader.log.fine(String.format("Configuration for %s written to %s", modConfigName, modConfig.getName()));
            } catch (IOException e) {
                Loader.log.warning(String.format("Error trying to write the config file %s", modConfig.getName()));
                Loader.log.throwing("ModLoaderModContainer", "configureMod", e);
                FMLCommonHandler.instance().raiseException(new LoaderException(e), "", false);
            }
        }
    }

    private Object parseValue(String val, ModProperty property, Class<?> type, String propertyName, String modConfigName) {
        if (type.isAssignableFrom(String.class)) {
            return val;
        } else if (type.isAssignableFrom(Boolean.TYPE) || type.isAssignableFrom(Boolean.class)) {
            return Boolean.parseBoolean(val);
        } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            Number n;

            if (type.isAssignableFrom(Double.TYPE) || Double.class.isAssignableFrom(type)) {
                n = Double.parseDouble(val);
            } else if (type.isAssignableFrom(Float.TYPE) || Float.class.isAssignableFrom(type)) {
                n = Float.parseFloat(val);
            } else if (type.isAssignableFrom(Long.TYPE) || Long.class.isAssignableFrom(type)) {
                n = Long.parseLong(val);
            } else if (type.isAssignableFrom(Integer.TYPE) || Integer.class.isAssignableFrom(type)) {
                n = Integer.parseInt(val);
            } else if (type.isAssignableFrom(Short.TYPE) || Short.class.isAssignableFrom(type)) {
                n = Short.parseShort(val);
            } else if (type.isAssignableFrom(Byte.TYPE) || Byte.class.isAssignableFrom(type)) {
                n = Byte.parseByte(val);
            } else {
                throw new IllegalArgumentException(String.format("MLProp declared on %s of type %s, an unsupported type", propertyName, type.getName()));
            }

            if (n.doubleValue() < property.min() || n.doubleValue() > property.max()) {
                Loader.log.warning(String.format("Configuration for %s.%s found value %s outside acceptable range %s,%s", modConfigName, propertyName, n, property.min(), property.max()));
                return null;
            } else {
                return n;
            }
        }

        throw new IllegalArgumentException(String.format("MLProp declared on %s of type %s, an unsupported type", propertyName, type.getName()));
    }

    private String extractValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        } else {
            throw new IllegalArgumentException("MLProp declared on non-standard type");
        }
    }

    @Override
    public void init() {
        mod.load();
    }

    @Override
    public void postInit() {
        mod.modsLoaded();
    }

    @Override
    public void tickStart(TickType tick, Object... data) {
        if (ticks.contains(tick)) {
            boolean keepTicking = mod.doTickInGame(tick, false, FMLCommonHandler.instance().getMinecraftInstance(), data);
            if (!keepTicking) {
                ticks.remove(tick);
                ticks.removeAll(tick.partnerTicks());
            }
        }
    }

    @Override
    public void tickEnd(TickType tick, Object... data) {
        if (ticks.contains(tick)) {
            boolean keepTicking = mod.doTickInGame(tick, true, FMLCommonHandler.instance().getMinecraftInstance(), data);
            if (!keepTicking) {
                ticks.remove(tick);
                ticks.removeAll(tick.partnerTicks());
            }
        }
    }

    @Override
    public String getName() {
        return mod != null ? mod.getName() : modClazz.getSimpleName();
    }

    @Override
    public String getSortingRules() {
        if (mod != null) {
            return mod.getPriorities();
        } else {
            return "";
        }
    }

    @Override
    public boolean matches(Object mod) {
        return modClazz.isInstance(mod);
    }

    public void setTickType(EnumSet<TickType> type) {
        this.ticks = EnumSet.copyOf(type);
    }

    @Override
    public File getSource() {
        return modSource;
    }

    @Override
    public Object getMod() {
        return mod;
    }

    @Override
    public boolean generatesWorld() {
        return true;
    }

    @Override
    public IWorldGenerator getWorldGenerator() {
        return mod;
    }

    @Override
    public int lookupFuelValue(int itemId, int itemDamage) {
        return mod.addFuel(itemId, itemDamage);
    }

    @Override
    public boolean wantsPickupNotification() {
        return true;
    }

    @Override
    public IPickupNotifier getPickupNotifier() {
        return mod;
    }

    @Override
    public boolean wantsToDispense() {
        return true;
    }

    @Override
    public IDispenseHandler getDispenseHandler() {
        return mod;
    }

    @Override
    public boolean wantsCraftingNotification() {
        return true;
    }

    @Override
    public ICraftingHandler getCraftingHandler() {
        return mod;
    }

    private void computeDependencies() {
        dependencies = new ArrayList<>();
        preDependencies = new ArrayList<>();
        postDependencies = new ArrayList<>();

        if (mod.getPriorities() == null || mod.getPriorities().length() == 0) {
            return;
        }

        boolean parseFailure = false;
        StringTokenizer st = new StringTokenizer(mod.getPriorities(), ";");

        while (st.hasMoreTokens()) {
            String dep = st.nextToken();
            String[] depparts = dep.split(":");

            if (depparts.length < 2) {
                parseFailure = true;
            } else if ("required-before".equals(depparts[0]) || "required-after".equals(depparts[0])) {
                if (!depparts[1].trim().equals("*")) {
                    dependencies.add(depparts[1]);
                } else {
                    parseFailure = true;
                }
            } else if ("before".equals(depparts[0])) {
                postDependencies.add(depparts[1]);
            } else if ("after".equals(depparts[0])) {
                preDependencies.add(depparts[1]);
            } else {
                parseFailure = true;
            }
        }

        if (parseFailure) {
            FMLCommonHandler.instance().getFMLLogger().warning(String.format("The mod %s has an incorrect dependency string {%s}", mod.getName(), mod.getPriorities()));
        }
    }

    @Override
    public List<String> getDependencies() {
        if (dependencies == null) {
            computeDependencies();
        }

        return dependencies;
    }

    @Override
    public List<String> getPostDepends() {
        if (dependencies == null) {
            computeDependencies();
        }

        return postDependencies;
    }

    @Override
    public List<String> getPreDepends() {
        if (dependencies == null) {
            computeDependencies();
        }
        return preDependencies;
    }


    public String toString() {
        return modClazz.getSimpleName();
    }

    @Override
    public boolean wantsNetworkPackets() {
        return true;
    }

    @Override
    public INetworkHandler getNetworkHandler() {
        return mod;
    }

    @Override
    public boolean ownsNetworkChannel(String channel) {
        return FMLCommonHandler.instance().getChannelListFor(this).contains(channel);
    }

    @Override
    public boolean wantsConsoleCommands() {
        return true;
    }

    @Override
    public IConsoleHandler getConsoleHandler() {
        return mod;
    }

    @Override
    public boolean wantsPlayerTracking() {
        return true;
    }

    @Override
    public IPlayerTracker getPlayerTracker() {
        return mod;
    }

    public EnumSet<TickType> getTickTypes() {
        return ticks;
    }

    public void addKeyHandler(IKeyHandler handler) {
        if (keyHandlers == null) {
            keyHandlers = new ArrayList<>();
        }
        keyHandlers.add(handler);
    }

    @Override
    public List<IKeyHandler> getKeys() {
        if (keyHandlers == null) {
            return Collections.emptyList();
        }
        return keyHandlers;
    }

    @Override
    public SourceType getSourceType() {
        return sourceType;
    }

    @Override
    public void setSourceType(SourceType type) {
        this.sourceType = type;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getMetadata()
     */
    @Override
    public ModMetadata getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#setMetadata(cpw.mods.fml.common.ModMetadata)
     */
    @Override
    public void setMetadata(ModMetadata meta) {
        this.metadata = meta;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#gatherRenderers(java.util.Map)
     */
    @Override
    public void gatherRenderers(Map<?, ?> renderers) {
        mod.onRenderHarvest(renderers);
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#requestAnimations()
     */
    @Override
    public void requestAnimations() {
        mod.onRegisterAnimations();
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getVersion()
     */
    @Override
    public String getVersion() {
        return mod != null ? mod.getVersion() : "Not available";
    }
}