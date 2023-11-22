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

/**
 * @author cpw
 */
public class ModProperty {
    private final String info;
    private final double min;
    private final double max;
    private final String name;

    public ModProperty(String info, double min, double max, String name) {
        this.info = info;
        this.min = min;
        this.max = max;
        this.name = name;
    }

    public String name() {
        return name;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public String info() {
        return info;
    }
}
