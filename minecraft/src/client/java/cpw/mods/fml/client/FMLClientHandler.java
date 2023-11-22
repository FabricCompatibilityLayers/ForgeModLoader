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

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.ModContainer.TickType;
import cpw.mods.fml.common.modloader.ModLoaderHelper;
import cpw.mods.fml.common.modloader.ModLoaderModContainer;
import cpw.mods.fml.common.modloader.ModProperty;
import cpw.mods.fml.common.registry.FMLRegistry;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;

/**
 * <p>Handles primary communication from hooked code into the system</p>
 *
 * <p>The FML entry point is {@link #onPreLoad(Minecraft)} called from {@link Minecraft}</p>
 *
 * <p>Obfuscated code should focus on this class and other members of the "server" (or "client") code</p>
 *
 * <p>The actual mod loading is handled at arms length by {@link Loader}</p>
 *
 * <p>It is expected that a similar class will exist for each target environment: Bukkit and Client side.</p>
 *
 * <p>It should not be directly modified.</p>
 *
 * @author cpw
 */
@SuppressWarnings("unchecked")
public class FMLClientHandler implements IFMLSidedHandler {
    /**
     * The singleton
     */
    private static final FMLClientHandler INSTANCE = new FMLClientHandler();
    // Cached lookups
    private final HashMap<String, ArrayList<OverrideInfo>> overrideInfo = new HashMap<>();
    private final HashMap<Integer, BlockRenderInfo> blockModelIds = new HashMap<>();
    @Getter
    private final HashMap<KeyBinding, ModContainer> keyBindings = new HashMap<>();
    private final HashSet<OverrideInfo> animationSet = new HashSet<>();
    private final List<TextureFX> addedTextureFX = new ArrayList<>();
    private final HashMap<Integer, Dimension> textureDims = new HashMap<>();
    private final IdentityHashMap<TextureFX, Integer> effectTextures = new IdentityHashMap<>();
    /**
     * A reference to the server itself
     */
    private Minecraft client;
    /**
     * A handy list of the default overworld biomes
     */
    private BiomeGenBase[] defaultOverworldBiomes;
    private int nextRenderId = 30;
    private TexturePackBase fallbackTexturePack;
    private NetClientHandler networkClient;
    private ModContainer animationCallbackMod;
    private boolean firstTick;

    /**
     * @return the instance
     */
    public static FMLClientHandler instance() {
        return INSTANCE;
    }

    /**
     * Called to start the whole game off from
     * {@link Minecraft#startGame()}
     */
    public void onPreLoad(Minecraft minecraft) {
        client = minecraft;
        ReflectionHelper.detectObfuscation(World.class);
        FMLCommonHandler.instance().registerSidedDelegate(this);
        FMLRegistry.registerRegistry(new ClientRegistry());
        Loader.instance().loadMods();
    }

    /**
     * Called a bit later on during initialization to finish loading mods.
     * Also initializes key bindings
     */
    public void onLoadComplete() {
        client.renderEngine.refreshTextures();
        Loader.instance().initializeMods();
        for (ModContainer mod : Loader.getModList()) {
            mod.gatherRenderers(RenderManager.instance.getRendererList());
            for (Render r : RenderManager.instance.getRendererList().values()) {
                r.setRenderManager(RenderManager.instance);
            }
        }
        // Load the key bindings into the settings table

        GameSettings gs = client.gameSettings;
        KeyBinding[] modKeyBindings = harvestKeyBindings();
        KeyBinding[] allKeys = new KeyBinding[gs.keyBindings.length + modKeyBindings.length];
        System.arraycopy(gs.keyBindings, 0, allKeys, 0, gs.keyBindings.length);
        System.arraycopy(modKeyBindings, 0, allKeys, gs.keyBindings.length, modKeyBindings.length);
        gs.keyBindings = allKeys;
        gs.loadOptions();

        // Mark this as a "first tick"

        firstTick = true;
    }

    public KeyBinding[] harvestKeyBindings() {
        List<IKeyHandler> allKeys = FMLCommonHandler.instance().gatherKeyBindings();
        KeyBinding[] keys = new KeyBinding[allKeys.size()];
        int i = 0;
        for (IKeyHandler key : allKeys) {
            keys[i++] = (KeyBinding) key.getKeyBinding();
            keyBindings.put((KeyBinding) key.getKeyBinding(), key.getOwningContainer());
        }
        return keys;
    }

    /**
     * Every tick just before world and other ticks occur
     */
    public void onPreWorldTick() {
        if (client.theWorld != null) {
            FMLCommonHandler.instance().worldTickStart();
            FMLCommonHandler.instance().tickStart(TickType.WORLDGUI, 0.0f, client.currentScreen);
        }
    }

    /**
     * Every tick just after world and other ticks occur
     */
    public void onPostWorldTick() {
        if (client.theWorld != null) {
            FMLCommonHandler.instance().worldTickEnd();
            FMLCommonHandler.instance().tickEnd(TickType.WORLDGUI, 0.0f, client.currentScreen);
        }
    }

    public void onWorldLoadTick() {
        if (client.theWorld != null) {
            if (firstTick) {
                loadTextures(fallbackTexturePack);
                firstTick = false;
            }
            FMLCommonHandler.instance().tickStart(TickType.WORLDLOADTICK);
            FMLCommonHandler.instance().tickStart(TickType.GUILOADTICK);
        }
    }

    public void onRenderTickStart(float partialTickTime) {
        if (client.theWorld != null) {
            FMLCommonHandler.instance().tickStart(TickType.RENDER, partialTickTime);
            FMLCommonHandler.instance().tickStart(TickType.GUI, partialTickTime, client.currentScreen);
        }
    }

    public void onRenderTickEnd(float partialTickTime) {
        if (client.theWorld != null) {
            FMLCommonHandler.instance().tickEnd(TickType.RENDER, partialTickTime);
            FMLCommonHandler.instance().tickEnd(TickType.GUI, partialTickTime, client.currentScreen);
        }
    }

    /**
     * Get the server instance
     */
    public Minecraft getClient() {
        return client;
    }

    /**
     * Get a handle to the client's logger instance
     * The client actually doesn't have one, so we return {@literal null}
     */
    @Override
    public Logger getMinecraftLogger() {
        return null;
    }

    /**
     * Called from ChunkProvider when a chunk needs to be populated
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
     * Is the offered class and instance of BaseMod and therefore a ModLoader
     * mod?
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
        @SuppressWarnings("unchecked") Class<? extends BaseMod> bmClazz = (Class<? extends BaseMod>) clazz;
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
    public boolean handleChatPacket(Packet3Chat chat) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsNetworkPackets() && mod.getNetworkHandler().onChat(chat)) {
                return true;
            }
        }

        return false;
    }

    public void handleServerLogin(Packet1Login loginPacket, NetClientHandler handler, NetworkManager networkManager) {
        this.networkClient = handler;
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = "REGISTER";
        packet.data = FMLCommonHandler.instance().getPacketRegistry();
        packet.length = packet.data.length;
        if (packet.length > 0) {
            networkManager.addToSendQueue(packet);
        }
        for (ModContainer mod : Loader.getModList()) {
            mod.getNetworkHandler().onServerLogin(handler);
        }
    }

    /**
     * Called when a packet 250 packet is received from the player
     */
    public void handlePacket250(Packet250CustomPayload packet) {
        if ("REGISTER".equals(packet.channel) || "UNREGISTER".equals(packet.channel)) {
            handleServerRegistration(packet);
            return;
        }

        ModContainer mod = FMLCommonHandler.instance().getModForChannel(packet.channel);

        if (mod != null) {
            mod.getNetworkHandler().onPacket250Packet(packet);
        }
    }

    /**
     * Handle register requests for packet 250 channels
     */
    private void handleServerRegistration(Packet250CustomPayload packet) {
        if (packet.data == null) {
            return;
        }
        for (String channel : new String(packet.data, StandardCharsets.UTF_8).split("\0")) {
            // Skip it if we don't know it
            if (FMLCommonHandler.instance().getModForChannel(channel) == null) {
                continue;
            }

            if ("REGISTER".equals(packet.channel)) {
                FMLCommonHandler.instance().activateChannel(client.thePlayer, channel);
            } else {
                FMLCommonHandler.instance().deactivateChannel(client.thePlayer, channel);
            }
        }
    }

    /**
     * Are we a server?
     */
    @Override
    public boolean isServer() {
        return false;
    }

    /**
     * Are we a client?
     */
    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public File getMinecraftRootDirectory() {
        return client.mcDataDir;
    }

    public void announceLogout(EntityPlayer player) {
        for (ModContainer mod : Loader.getModList()) {
            if (mod.wantsPlayerTracking()) {
                mod.getPlayerTracker().onPlayerLogout(player);
            }
        }
    }

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

    /**
     * Return the minecraft instance
     */
    @Override
    public Object getMinecraftInstance() {
        return client;
    }

    /**
     * {@inheritDoc}
     *
     * @see cpw.mods.fml.common.IFMLSidedHandler#getCurrentLanguage()
     */
    @Override
    public String getCurrentLanguage() {
        return StringTranslate.getInstance().getCurrentLanguage();
    }

    @Override
    public Properties getCurrentLanguageTable() {
        return StringTranslate.getInstance().getTranslationTable();
    }

    public int addNewArmourRendererPrefix(String armor) {
        return RenderPlayer.addNewArmourPrefix(armor);
    }

    public void addNewTextureOverride(String textureToOverride, String overridingTexturePath, int location) {
        if (!overrideInfo.containsKey(textureToOverride)) {
            overrideInfo.put(textureToOverride, new ArrayList<>());
        }
        ArrayList<OverrideInfo> list = overrideInfo.get(textureToOverride);
        OverrideInfo info = new OverrideInfo();
        info.index = location;
        info.override = overridingTexturePath;
        info.texture = textureToOverride;
        list.add(info);
        FMLCommonHandler.instance().getFMLLogger().log(Level.FINE, String.format("Overriding %s @ %d with %s. %d slots remaining", textureToOverride, location, overridingTexturePath, SpriteHelper.freeSlotCount(textureToOverride)));
    }

    public int obtainBlockModelIdFor(BaseMod mod, boolean inventoryRenderer) {
        ModLoaderModContainer mlmc = ModLoaderHelper.registerRenderHelper(mod);
        int renderId = nextRenderId++;
        BlockRenderInfo bri = new BlockRenderInfo(renderId, inventoryRenderer, mlmc);
        blockModelIds.put(renderId, bri);
        return renderId;
    }

    public BufferedImage loadImageFromTexturePack(RenderEngine renderEngine, String path) throws IOException {
        InputStream image = client.texturePackList.selectedTexturePack.getResourceAsStream(path);
        if (image == null) {
            throw new RuntimeException(String.format("The requested image path %s is not found", path));
        }
        BufferedImage result = ImageIO.read(image);
        if (result == null) {
            throw new RuntimeException(String.format("The requested image path %s appears to be corrupted", path));
        }
        return result;
    }

    public void displayGuiScreen(EntityPlayer player, GuiScreen gui) {
        if (client.renderViewEntity == player && gui != null) {
            client.displayGuiScreen(gui);
        }
    }

    public void registerKeyHandler(BaseMod mod, KeyBinding keyHandler, boolean allowRepeat) {
        ModLoaderModContainer mlmc = ModLoaderHelper.registerKeyHelper(mod);
        mlmc.addKeyHandler(new KeyBindingHandler(keyHandler, allowRepeat, mlmc));
    }

    public boolean renderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelId) {
        if (!blockModelIds.containsKey(modelId)) {
            return false;
        }
        BlockRenderInfo bri = blockModelIds.get(modelId);
        return bri.renderWorldBlock(world, x, y, z, block, modelId, renderer);
    }

    public void renderInventoryBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {
        if (!blockModelIds.containsKey(modelID)) {
            return;
        }
        BlockRenderInfo bri = blockModelIds.get(modelID);
        bri.renderInventoryBlock(block, metadata, modelID, renderer);
    }

    public boolean renderItemAsFull3DBlock(int modelId) {
        BlockRenderInfo bri = blockModelIds.get(modelId);
        if (bri != null) {
            return bri.shouldRender3DInInventory();
        }
        return false;
    }

    public void registerTextureOverrides(RenderEngine renderer) {
        for (ModContainer mod : Loader.getModList()) {
            registerAnimatedTexturesFor(mod);
        }

        for (OverrideInfo animationOverride : animationSet) {
            renderer.registerTextureFX(animationOverride.textureFX);
            addedTextureFX.add(animationOverride.textureFX);
            FMLCommonHandler.instance().getFMLLogger().finer(String.format("Registered texture override %d (%d) on %s (%d)", animationOverride.index, animationOverride.textureFX.iconIndex, animationOverride.textureFX.getClass().getSimpleName(), animationOverride.textureFX.tileImage));
        }

        for (String fileToOverride : overrideInfo.keySet()) {
            for (OverrideInfo override : overrideInfo.get(fileToOverride)) {
                try {
                    BufferedImage image = loadImageFromTexturePack(renderer, override.override);
                    ModTextureStatic mts = new ModTextureStatic(override.index, 1, override.texture, image);
                    renderer.registerTextureFX(mts);
                    addedTextureFX.add(mts);
                    FMLCommonHandler.instance().getFMLLogger().finer(String.format("Registered texture override %d (%d) on %s (%d)", override.index, mts.iconIndex, override.texture, mts.tileImage));
                } catch (IOException e) {
                    FMLCommonHandler.instance().getFMLLogger().throwing("FMLClientHandler", "registerTextureOverrides", e);
                }
            }
        }
    }

    private void registerAnimatedTexturesFor(ModContainer mod) {
        this.animationCallbackMod = mod;
        mod.requestAnimations();
        this.animationCallbackMod = null;
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
        JsonNode root = new JdomParser().parse(new InputStreamReader(input));
        List<JsonNode> lst = root.getArrayNode();
        JsonNode modinfo = null;
        for (JsonNode tmodinfo : lst) {
            if (mod.getName().equals(tmodinfo.getStringValue("modid"))) {
                modinfo = tmodinfo;
                break;
            }
        }
        if (modinfo == null) {
            FMLCommonHandler.instance().getFMLLogger().fine(String.format("Unable to process JSON modinfo file for %s", mod.getName()));
            return null;
        }
        ModMetadata meta = new ModMetadata(mod);
        try {
            meta.name = modinfo.getStringValue("name");
            meta.description = modinfo.getStringValue("description");
            meta.version = modinfo.getStringValue("version");
            meta.credits = modinfo.getStringValue("credits");
            List<?> authors = modinfo.getArrayNode("authors");
            StringBuilder sb = new StringBuilder();
            for (Object author : authors) {
                meta.authorList.add(((JsonNode) author).getText());
            }
            meta.logoFile = modinfo.getStringValue("logoFile");
            meta.url = modinfo.getStringValue("url");
            meta.updateUrl = modinfo.getStringValue("updateUrl");
            meta.parent = modinfo.getStringValue("parent");
            List<?> screenshots = modinfo.getArrayNode("screenshots");
            meta.screenshots = new String[screenshots.size()];
            for (int i = 0; i < screenshots.size(); i++) {
                meta.screenshots[i] = ((JsonNode) screenshots.get(i)).getText();
            }
        } catch (Exception e) {
            FMLCommonHandler.instance().getFMLLogger().log(Level.FINE, String.format("An error occurred reading the info file for %s", mod.getName()), e);
        }
        return meta;
    }

    public void pruneOldTextureFX(TexturePackBase var1, List<TextureFX> effects) {
        ListIterator<TextureFX> li = addedTextureFX.listIterator();
        while (li.hasNext()) {
            TextureFX tex = li.next();
            if (tex instanceof FMLTextureFX) {
                if (((FMLTextureFX) tex).unregister(client.renderEngine, effects)) {
                    li.remove();
                }
            } else {
                effects.remove(tex);
                li.remove();
            }
        }
    }

    /**
     *
     */
    public void loadTextures(TexturePackBase texturePack) {
        registerTextureOverrides(client.renderEngine);
    }

    public void onEarlyTexturePackLoad(TexturePackBase fallback) {
        if (client == null) {
            // We're far too early, let's wait
            this.fallbackTexturePack = fallback;
        } else {
            loadTextures(fallback);
        }
    }

    public void sendPacket(Packet packet) {
        if (this.networkClient != null) {
            this.networkClient.addToSendQueue(packet);
        }
    }

    public void addAnimation(TextureFX anim) {
        if (animationCallbackMod == null) {
            return;
        }
        OverrideInfo info = new OverrideInfo();
        info.index = anim.iconIndex;
        info.imageIndex = anim.tileImage;
        info.textureFX = anim;
        animationSet.remove(info);
        animationSet.add(info);
    }

    @Override
    public void profileStart(String profileLabel) {
        Profiler.startSection(profileLabel);
    }

    @Override
    public void profileEnd() {
        Profiler.endSection();
    }

    public void preGameLoad(String user, String sessionToken) {
        // Currently this does nothing, but it's possible I could relaunch Minecraft in a new classloader if I wished
        Minecraft.fmlReentry(user, sessionToken);
    }

    public void onTexturePackChange(RenderEngine engine, TexturePackBase texturepack, List<TextureFX> effects) {
        FMLClientHandler.instance().pruneOldTextureFX(texturepack, effects);

        for (TextureFX tex : effects) {
            if (tex instanceof ITextureFX) {
                ((ITextureFX) tex).onTexturePackChanged(engine, texturepack, getTextureDimensions(tex));
            }
        }

        FMLClientHandler.instance().loadTextures(texturepack);
    }

    public void setTextureDimensions(int id, int width, int height, List<TextureFX> effects) {
        Dimension dim = new Dimension(width, height);
        textureDims.put(id, dim);

        for (TextureFX tex : effects) {
            if (getEffectTexture(tex) == id && tex instanceof ITextureFX) {
                ((ITextureFX) tex).onTextureDimensionsUpdate(width, height);
            }
        }
    }

    public Dimension getTextureDimensions(TextureFX effect) {
        return getTextureDimensions(getEffectTexture(effect));
    }

    public Dimension getTextureDimensions(int id) {
        return textureDims.get(id);
    }

    public int getEffectTexture(TextureFX effect) {
        Integer id = effectTextures.get(effect);
        if (id != null) {
            return id;
        }

        int old = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);

        effect.bindImage(client.renderEngine);

        id = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);

        GL11.glBindTexture(GL_TEXTURE_2D, old);

        effectTextures.put(effect, id);

        return id;
    }

    public boolean onUpdateTextureEffect(TextureFX effect) {
        Logger log = FMLCommonHandler.instance().getFMLLogger();
        ITextureFX ifx = (effect instanceof ITextureFX ? ((ITextureFX) effect) : null);

        if (ifx != null && ifx.getErrored()) {
            return false;
        }

        String name = effect.getClass().getSimpleName();
        Profiler.startSection(name);

        try {
            effect.onTick();
        } catch (Exception e) {
            log.warning(String.format("Texture FX %s has failed to animate. Likely caused by a texture pack change that they did not respond correctly to", name));
            if (ifx != null) {
                ifx.setErrored(true);
            }
            Profiler.endSection();
            return false;
        }
        Profiler.endSection();

        Dimension dim = getTextureDimensions(effect);
        int target = ((dim.width >> 4) * (dim.height >> 4)) << 2;
        if (effect.imageData.length != target) {
            log.warning(String.format("Detected a texture FX sizing discrepancy in %s (%d, %d)", name, effect.imageData.length, target));
            if (ifx != null) {
                ifx.setErrored(true);
            }
            return false;
        }
        return true;
    }

    public void onPreRegisterEffect(TextureFX effect) {
        Dimension dim = getTextureDimensions(effect);
        if (effect instanceof ITextureFX) {
            ((ITextureFX) effect).onTextureDimensionsUpdate(dim.width, dim.height);
        }
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
