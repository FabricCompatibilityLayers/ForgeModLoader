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
package cpw.mods.fml.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FMLModContainer implements ModContainer {
    private Mod modDescriptor;
    private Object modInstance;
    private File source;
    private ModMetadata modMetadata;

    public FMLModContainer(String dummy) {
        this(new File(dummy));
    }

    public FMLModContainer(File source) {
        this.source = source;
    }

    public FMLModContainer(Class<?> clazz) {
        if (clazz == null) return;

        modDescriptor = clazz.getAnnotation(Mod.class);

        try {
            modInstance = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ModContainer buildFor(Class<?> clazz) {
        return new FMLModContainer(clazz);
    }

    @Override
    public boolean wantsPreInit() {
        return modDescriptor.wantsPreInit();
    }

    @Override
    public boolean wantsPostInit() {
        return modDescriptor.wantsPostInit();
    }

    @Override
    public void preInit() {
    }

    @Override
    public void init() {
    }

    @Override
    public void postInit() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ModState getModState() {
        return null;
    }

    @Override
    public void nextState() {
    }

    @Override
    public String getSortingRules() {
        return null;
    }

    @Override
    public void tickStart(TickType type, Object... data) {
    }

    @Override
    public void tickEnd(TickType type, Object... data) {
    }

    @Override
    public boolean matches(Object mod) {
        return false;
    }

    @Override
    public File getSource() {
        return source;
    }

    @Override
    public Object getMod() {
        return null;
    }

    @Override
    public boolean generatesWorld() {
        return false;
    }

    @Override
    public IWorldGenerator getWorldGenerator() {
        return null;
    }

    @Override
    public int lookupFuelValue(int itemId, int itemDamage) {
        return 0;
    }

    @Override
    public boolean wantsPickupNotification() {
        return false;
    }

    @Override
    public IPickupNotifier getPickupNotifier() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#wantsToDispense()
     */
    @Override
    public boolean wantsToDispense() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getDispenseHandler()
     */
    @Override
    public IDispenseHandler getDispenseHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#wantsCraftingNotification()
     */
    @Override
    public boolean wantsCraftingNotification() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getCraftingHandler()
     */
    @Override
    public ICraftingHandler getCraftingHandler() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getDependencies()
     */
    @Override
    public List<String> getDependencies() {
        return new ArrayList<>(0);
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getPreDepends()
     */
    @Override
    public List<String> getPreDepends() {
        return new ArrayList<>(0);
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getPostDepends()
     */
    @Override
    public List<String> getPostDepends() {
        return new ArrayList<>(0);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSource().getName();
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#wantsNetworkPackets()
     */
    @Override
    public boolean wantsNetworkPackets() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getNetworkHandler()
     */
    @Override
    public INetworkHandler getNetworkHandler() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#ownsNetworkChannel(java.lang.String)
     */
    @Override
    public boolean ownsNetworkChannel(String channel) {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#wantsConsoleCommands()
     */
    @Override
    public boolean wantsConsoleCommands() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getConsoleHandler()
     */
    @Override
    public IConsoleHandler getConsoleHandler() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#wantsPlayerTracking()
     */
    @Override
    public boolean wantsPlayerTracking() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getPlayerTracker()
     */
    @Override
    public IPlayerTracker getPlayerTracker() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getKeys()
     */
    @Override
    public List<IKeyHandler> getKeys() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getSourceType()
     */
    @Override
    public SourceType getSourceType() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#setSourceType(cpw.mods.fml.common.ModContainer.SourceType)
     */
    @Override
    public void setSourceType(SourceType type) {
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getMetadata()
     */
    @Override
    public ModMetadata getMetadata() {
        return modMetadata;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#setMetadata(cpw.mods.fml.common.ModMetadata)
     */
    @Override
    public void setMetadata(ModMetadata meta) {
        this.modMetadata = meta;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#gatherRenderers(java.util.Map)
     */
    @Override
    public void gatherRenderers(Map<?, ?> renderers) {
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#requestAnimations()
     */
    @Override
    public void requestAnimations() {
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.ModContainer#getVersion()
     */
    @Override
    public String getVersion() {
        return null;
    }
}
