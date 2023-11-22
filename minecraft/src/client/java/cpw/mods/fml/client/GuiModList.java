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

package cpw.mods.fml.client;

import cpw.mods.fml.common.FMLModLoaderContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

/**
 * @author cpw
 */
public class GuiModList extends GuiScreen {
    private final GuiScreen mainMenu;
    private final ArrayList<ModContainer> mods;
    private GuiSlotModList modList;
    private int selected = -1;
    private ModContainer selectedMod;
    private int listWidth;

    public GuiModList(GuiScreen mainMenu) {
        this.mainMenu = mainMenu;
        this.mods = new ArrayList<>();
        mods.add(new FMLModLoaderContainer());
        for (ModContainer mod : Loader.getModList()) {
            if (mod.getMetadata() != null && mod.getMetadata().parentMod != null) {
                continue;
            }
            mods.add(mod);
        }
    }

    @Override
    public void initGui() {
        for (ModContainer mod : mods) {
            listWidth = Math.max(listWidth, getFontRenderer().getStringWidth(mod.getName()) + 10);
            listWidth = Math.max(listWidth, getFontRenderer().getStringWidth(mod.getVersion()) + 10);
        }
        listWidth = Math.min(listWidth, 150);
        StringTranslate translations = StringTranslate.getInstance();
        this.controlList.add(new GuiSmallButton(6, this.width / 2 - 75, this.height - 38, translations.translateKey("gui.done")));
        this.modList = new GuiSlotModList(this, mods, listWidth);
        this.modList.registerScrollButtons(this.controlList, 7, 8);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.id == 6) {
            this.mc.displayGuiScreen(this.mainMenu);
            return;
        }
        super.actionPerformed(button);
    }

    public int drawLine(String line, int offset, int shiftY) {
        this.fontRenderer.drawString(line, offset, shiftY, 0xd7edea);
        return shiftY + 10;
    }

    @Override
    public void drawScreen(int p_571_1_, int p_571_2_, float p_571_3_) {
        this.modList.drawScreen(p_571_1_, p_571_2_, p_571_3_);
        this.drawCenteredString(this.fontRenderer, "Mod List", this.width / 2, 16, 0xFFFFFF);
        int offset = this.listWidth + 20;
        if (selectedMod != null) {
            if (selectedMod.getMetadata() != null) {
                int shifty = 35;
                if (!selectedMod.getMetadata().logoFile.isEmpty()) {
                    int texture = this.mc.renderEngine.getTexture(selectedMod.getMetadata().logoFile);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    this.mc.renderEngine.bindTexture(texture);
                    this.drawTexturedModalRect(offset, 32, 0, 0, 256, 58);

                    shifty += 65;
                }
                this.fontRenderer.drawStringWithShadow(selectedMod.getMetadata().name, offset, shifty, 0xFFFFFF);
                shifty += 12;

                shifty = drawLine(String.format("Version: %s (%s)", selectedMod.getMetadata().version, selectedMod.getVersion()), offset, shifty);
                if (!selectedMod.getMetadata().credits.isEmpty()) {
                    shifty = drawLine(String.format("Credits: %s", selectedMod.getMetadata().credits), offset, shifty);
                }
                shifty = drawLine(String.format("Authors: %s", selectedMod.getMetadata().getAuthorList()), offset, shifty);
                shifty = drawLine(String.format("URL: %s", selectedMod.getMetadata().url), offset, shifty);
                shifty = drawLine(selectedMod.getMetadata().childMods.isEmpty() ? "No child mods for this mod" : String.format("Child mods: %s", selectedMod.getMetadata().getChildModList()), offset, shifty);
                this.getFontRenderer().drawSplitString(selectedMod.getMetadata().description, offset, shifty + 10, this.width - this.listWidth - 30, 0xDDDDDD);
            } else {
                offset = (this.listWidth + this.width) / 2;
                this.drawCenteredString(this.fontRenderer, selectedMod.getName(), offset, 35, 0xFFFFFF);
                this.drawCenteredString(this.fontRenderer, String.format("Version: %s", selectedMod.getVersion()), offset, 45, 0xFFFFFF);
                this.drawCenteredString(this.fontRenderer, "No mod information found", offset, 55, 0xDDDDDD);
                this.drawCenteredString(this.fontRenderer, "Ask your mod author to provide a mod .info file", offset, 65, 0xDDDDDD);
            }
        }
        super.drawScreen(p_571_1_, p_571_2_, p_571_3_);
    }

    Minecraft getMinecraftInstance() {
        return mc;
    }

    FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    public void selectModIndex(int var1) {
        this.selected = var1;
        if (var1 >= 0 && var1 <= mods.size()) {
            this.selectedMod = mods.get(selected);
        } else {
            this.selectedMod = null;
        }
    }

    public boolean modIndexSelected(int var1) {
        return var1 == selected;
    }
}
