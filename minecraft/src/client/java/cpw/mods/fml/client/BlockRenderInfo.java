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

package cpw.mods.fml.client;

import cpw.mods.fml.common.ModContainer;
import lombok.Getter;
import net.minecraft.src.BaseMod;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;

/**
 * @author cpw
 */
public class BlockRenderInfo {
    @Getter
    private final int renderId;
    private final boolean render3DInInventory;
    private final ModContainer modContainer;

    public BlockRenderInfo(int renderId, boolean render3DInInventory, ModContainer modContainer) {
        this.renderId = renderId;
        this.render3DInInventory = render3DInInventory;
        this.modContainer = modContainer;
    }

    public boolean shouldRender3DInInventory() {
        return render3DInInventory;
    }

    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        return ((BaseMod) modContainer.getMod()).renderWorldBlock(renderer, world, x, y, z, block, modelId);
    }

    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        ((BaseMod) modContainer.getMod()).renderInvBlock(renderer, block, metadata, modelID);
    }

}
