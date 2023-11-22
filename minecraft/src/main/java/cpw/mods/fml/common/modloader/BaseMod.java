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

import java.util.Map;

import cpw.mods.fml.common.IConsoleHandler;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.IDispenseHandler;
import cpw.mods.fml.common.INetworkHandler;
import cpw.mods.fml.common.IPickupNotifier;
import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.ModContainer.TickType;

/**
 * Marker interface for BaseMod
 *
 * @author cpw
 */
public interface BaseMod extends IWorldGenerator, IPickupNotifier, IDispenseHandler, ICraftingHandler, INetworkHandler, IConsoleHandler, IPlayerTracker {
    void modsLoaded();

    void load();

    boolean doTickInGame(TickType tick, boolean b, Object minecraftInstance, Object... data);

    String getName();

    String getPriorities();

    int addFuel(int itemId, int itemDamage);

    void onRenderHarvest(Map<?, ?> renderers);

    void onRegisterAnimations();

    String getVersion();
}
