package net.minecraft.client;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.src.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public abstract class Minecraft implements Runnable {
    public static byte[] field_28006_b = new byte[10485760];
    private static Minecraft theMinecraft;
    public PlayerController playerController;
    private boolean fullscreen;
    private boolean hasCrashed = false;
    public int displayWidth;
    public int displayHeight;
    private final Timer timer = new Timer(20.0F);
    public World theWorld;
    public RenderGlobal renderGlobal;
    public EntityPlayerSP thePlayer;
    public EntityLiving renderViewEntity;
    public EffectRenderer effectRenderer;
    public Session session = null;
    public String minecraftUri;
    public Canvas mcCanvas;
    public boolean hideQuitButton = false;
    public volatile boolean isGamePaused = false;
    public RenderEngine renderEngine;
    public FontRenderer fontRenderer;
    public FontRenderer standardGalacticFontRenderer;
    public GuiScreen currentScreen = null;
    public LoadingScreenRenderer loadingScreen;
    public EntityRenderer entityRenderer;
    private ThreadDownloadResources downloadResourcesThread;
    private int ticksRan = 0;
    private int leftClickCounter = 0;
    private int tempDisplayWidth;
    private final int tempDisplayHeight;
    public GuiAchievement guiAchievement = new GuiAchievement(this);
    public GuiIngame ingameGUI;
    public boolean skipRenderWorld = false;
    public ModelBiped playerModelBiped = new ModelBiped(0.0F);
    public MovingObjectPosition objectMouseOver = null;
    public GameSettings gameSettings;
    protected MinecraftApplet mcApplet;
    public SoundManager sndManager = new SoundManager();
    public MouseHelper mouseHelper;
    public TexturePackList texturePackList;
    public File mcDataDir;
    private ISaveFormat saveLoader;
    public static long[] frameTimes = new long[512];
    public static long[] tickTimes = new long[512];
    public static int numRecordedFrameTimes = 0;
    public static long hasPaidCheckTime = 0L;
    private int rightClickDelayTimer = 0;
    public StatFileWriter statFileWriter;
    private String serverName;
    private int serverPort;
    private final TextureWaterFX textureWaterFX = new TextureWaterFX();
    private final TextureLavaFX textureLavaFX = new TextureLavaFX();
    private static File minecraftDir = null;
    public volatile boolean running = true;
    public String debug = "";
    long debugUpdateTime = System.currentTimeMillis();
    int fpsCounter = 0;
    boolean isTakingScreenshot = false;
    long prevFrameTime = -1L;
    private String debugProfilerName = "root";
    public boolean inGameHasFocus = false;
    public boolean isRaining = false;
    long systemTime = System.currentTimeMillis();
    private int joinPlayerCounter = 0;

    public Minecraft(Component component, Canvas canvas, MinecraftApplet minecraftApplet, int i, int j, boolean bl) {
        StatList.func_27360_a();
        this.tempDisplayHeight = j;
        this.fullscreen = bl;
        this.mcApplet = minecraftApplet;
        Packet3Chat.field_52010_b = 32767;
        new ThreadClientSleep(this, "Timer hack thread");
        this.mcCanvas = canvas;
        this.displayWidth = i;
        this.displayHeight = j;
        this.fullscreen = bl;
        if (minecraftApplet == null || "true".equals(minecraftApplet.getParameter("stand-alone"))) {
            this.hideQuitButton = false;
        }

        theMinecraft = this;
    }

    public void onMinecraftCrash(UnexpectedThrowable par1UnexpectedThrowable) {
        this.hasCrashed = true;
        this.displayUnexpectedThrowable(par1UnexpectedThrowable);
    }

    public abstract void displayUnexpectedThrowable(UnexpectedThrowable par1UnexpectedThrowable);

    public void setServer(String string, int par2) {
        this.serverName = string;
        this.serverPort = par2;
    }

    public void startGame() throws LWJGLException {
        if (this.mcCanvas != null) {
            Graphics var1 = this.mcCanvas.getGraphics();
            if (var1 != null) {
                var1.setColor(Color.BLACK);
                var1.fillRect(0, 0, this.displayWidth, this.displayHeight);
                var1.dispose();
            }

            Display.setParent(this.mcCanvas);
        } else if (this.fullscreen) {
            Display.setFullscreen(true);
            this.displayWidth = Display.getDisplayMode().getWidth();
            this.displayHeight = Display.getDisplayMode().getHeight();
            if (this.displayWidth <= 0) {
                this.displayWidth = 1;
            }

            if (this.displayHeight <= 0) {
                this.displayHeight = 1;
            }
        } else {
            Display.setDisplayMode(new DisplayMode(this.displayWidth, this.displayHeight));
        }

        Display.setTitle("Minecraft Minecraft 1.2.5");
        System.out.println("LWJGL Version: " + Sys.getVersion());

        try {
            PixelFormat var7 = new PixelFormat();
            var7 = var7.withDepthBits(24);
            Display.create(var7);
        } catch (LWJGLException var6) {
            var6.printStackTrace();

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }

            Display.create();
        }

        OpenGlHelper.initializeTextures();
        this.mcDataDir = getMinecraftDir();
        this.saveLoader = new AnvilSaveConverter(new File(this.mcDataDir, "saves"));
        this.gameSettings = new GameSettings(this, this.mcDataDir);
        this.texturePackList = new TexturePackList(this, this.mcDataDir);
        this.renderEngine = new RenderEngine(this.texturePackList, this.gameSettings);
        this.loadScreen();
        this.fontRenderer = new FontRenderer(this.gameSettings, "/font/default.png", this.renderEngine, false);
        this.standardGalacticFontRenderer = new FontRenderer(this.gameSettings, "/font/alternate.png", this.renderEngine, false);
        if (this.gameSettings.language != null) {
            StringTranslate.getInstance().setLanguage(this.gameSettings.language);
            this.fontRenderer.setUnicodeFlag(StringTranslate.getInstance().isUnicode());
            this.fontRenderer.setBidiFlag(StringTranslate.isBidrectional(this.gameSettings.language));
        }

        ColorizerWater.setWaterBiomeColorizer(this.renderEngine.getTextureContents("/misc/watercolor.png"));
        ColorizerGrass.setGrassBiomeColorizer(this.renderEngine.getTextureContents("/misc/grasscolor.png"));
        ColorizerFoliage.getFoilageBiomeColorizer(this.renderEngine.getTextureContents("/misc/foliagecolor.png"));
        this.entityRenderer = new EntityRenderer(this);
        RenderManager.instance.itemRenderer = new ItemRenderer(this);
        this.statFileWriter = new StatFileWriter(this.session, this.mcDataDir);
        AchievementList.openInventory.setStatStringFormatter(new StatStringFormatKeyInv(this));
        this.loadScreen();
        Mouse.create();
        this.mouseHelper = new MouseHelper(this.mcCanvas);

        try {
            Controllers.create();
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        func_52004_D();
        this.checkGLError("Pre startup");
        GL11.glEnable(3553);
        GL11.glShadeModel(7425);
        GL11.glClearDepth(1.0);
        GL11.glEnable(2929);
        GL11.glDepthFunc(515);
        GL11.glEnable(3008);
        GL11.glAlphaFunc(516, 0.1F);
        GL11.glCullFace(1029);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        FMLClientHandler.instance().onLoadComplete();
        this.textureLavaFX.setup();
        this.textureWaterFX.setup();
        this.checkGLError("Startup");
        this.sndManager.loadSoundSettings(this.gameSettings);
        this.renderEngine.registerTextureFX(this.textureLavaFX);
        this.renderEngine.registerTextureFX(this.textureWaterFX);
        this.renderEngine.registerTextureFX(new TexturePortalFX());
        this.renderEngine.registerTextureFX(new TextureCompassFX(this));
        this.renderEngine.registerTextureFX(new TextureWatchFX(this));
        this.renderEngine.registerTextureFX(new TextureWaterFlowFX());
        this.renderEngine.registerTextureFX(new TextureLavaFlowFX());
        this.renderEngine.registerTextureFX(new TextureFlamesFX(0));
        this.renderEngine.registerTextureFX(new TextureFlamesFX(1));
        this.renderGlobal = new RenderGlobal(this, this.renderEngine);
        GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
        this.effectRenderer = new EffectRenderer(this.theWorld, this.renderEngine);

        try {
            this.downloadResourcesThread = new ThreadDownloadResources(this.mcDataDir, this);
            this.downloadResourcesThread.start();
        } catch (Exception ignored) {
        }

        this.checkGLError("Post startup");
        this.ingameGUI = new GuiIngame(this);
        if (this.serverName != null) {
            this.displayGuiScreen(new GuiConnecting(this, this.serverName, this.serverPort));
        } else {
            this.displayGuiScreen(new GuiMainMenu());
        }

        this.loadingScreen = new LoadingScreenRenderer(this);
    }

    private void loadScreen() throws LWJGLException {
        ScaledResolution var1 = new ScaledResolution(this.gameSettings, this.displayWidth, this.displayHeight);
        GL11.glClear(16640);
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, var1.scaledWidthD, var1.scaledHeightD, 0.0, 1000.0, 3000.0);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Tessellator var2 = Tessellator.instance;
        GL11.glDisable(2896);
        GL11.glEnable(3553);
        GL11.glDisable(2912);
        GL11.glBindTexture(3553, this.renderEngine.getTexture("/title/mojang.png"));
        var2.startDrawingQuads();
        var2.setColorOpaque_I(16777215);
        var2.addVertexWithUV(0.0, this.displayHeight, 0.0, 0.0, 0.0);
        var2.addVertexWithUV(this.displayWidth, this.displayHeight, 0.0, 0.0, 0.0);
        var2.addVertexWithUV(this.displayWidth, 0.0, 0.0, 0.0, 0.0);
        var2.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
        var2.draw();
        short var3 = 256;
        short var4 = 256;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        var2.setColorOpaque_I(16777215);
        this.scaledTessellator((var1.getScaledWidth() - var3) / 2, (var1.getScaledHeight() - var4) / 2, 0, 0, var3, var4);
        GL11.glDisable(2896);
        GL11.glDisable(2912);
        GL11.glEnable(3008);
        GL11.glAlphaFunc(516, 0.1F);
        Display.swapBuffers();
    }

    public void scaledTessellator(int i, int j, int k, int l, int m, int par6) {
        float var7 = 0.00390625F;
        float var8 = 0.00390625F;
        Tessellator var9 = Tessellator.instance;
        var9.startDrawingQuads();
        var9.addVertexWithUV(i + 0, j + par6, 0.0, (float)(k + 0) * var7, (float)(l + par6) * var8);
        var9.addVertexWithUV(i + m, j + par6, 0.0, (float)(k + m) * var7, (float)(l + par6) * var8);
        var9.addVertexWithUV(i + m, j + 0, 0.0, (float)(k + m) * var7, (float)(l + 0) * var8);
        var9.addVertexWithUV(i + 0, j + 0, 0.0, (float)(k + 0) * var7, (float)(l + 0) * var8);
        var9.draw();
    }

    public static File getMinecraftDir() {
        if (minecraftDir == null) {
            minecraftDir = getAppDir("minecraft");
        }

        return minecraftDir;
    }

    // $QF: Unable to simplify switch on enum
    // Please report this to the Quiltflower issue tracker, at https://github.com/QuiltMC/quiltflower/issues with a copy of the class file (if you have the rights to distribute it!)
    public static File getAppDir(String par0Str) {
        String var1 = System.getProperty("user.home", ".");
        File var2;
        switch (getOs().ordinal() + 1) {
            case 1:
            case 2:
                var2 = new File(var1, '.' + par0Str + '/');
                break;
            case 3:
                String var3 = System.getenv("APPDATA");
                if (var3 != null) {
                    var2 = new File(var3, "." + par0Str + '/');
                } else {
                    var2 = new File(var1, '.' + par0Str + '/');
                }
                break;
            case 4:
                var2 = new File(var1, "Library/Application Support/" + par0Str);
                break;
            default:
                var2 = new File(var1, par0Str + '/');
                break;
        }

        if (!var2.exists() && !var2.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + var2);
        } else {
            return var2;
        }
    }

    private static EnumOS2 getOs() {
        String var0 = System.getProperty("os.name").toLowerCase();
        if (var0.contains("win")) {
            return EnumOS2.windows;
        } else if (var0.contains("mac")) {
            return EnumOS2.macos;
        } else if (var0.contains("solaris")) {
            return EnumOS2.solaris;
        } else if (var0.contains("sunos")) {
            return EnumOS2.solaris;
        } else if (var0.contains("linux")) {
            return EnumOS2.linux;
        } else {
            return var0.contains("unix") ? EnumOS2.linux : EnumOS2.unknown;
        }
    }

    public ISaveFormat getSaveLoader() {
        return this.saveLoader;
    }

    public void displayGuiScreen(GuiScreen par1GuiScreen) {
        if (!(this.currentScreen instanceof GuiErrorScreen)) {
            if (this.currentScreen != null) {
                this.currentScreen.onGuiClosed();
            }

            if (par1GuiScreen instanceof GuiMainMenu) {
                this.statFileWriter.func_27175_b();
            }

            this.statFileWriter.syncStats();
            if (par1GuiScreen == null && this.theWorld == null) {
                par1GuiScreen = new GuiMainMenu();
            } else if (par1GuiScreen == null && this.thePlayer.getHealth() <= 0) {
                par1GuiScreen = new GuiGameOver();
            }

            if (par1GuiScreen instanceof GuiMainMenu) {
                this.gameSettings.showDebugInfo = false;
                this.ingameGUI.clearChatMessages();
            }

            this.currentScreen = par1GuiScreen;
            if (par1GuiScreen != null) {
                this.setIngameNotInFocus();
                ScaledResolution var2 = new ScaledResolution(this.gameSettings, this.displayWidth, this.displayHeight);
                int var3 = var2.getScaledWidth();
                int var4 = var2.getScaledHeight();
                par1GuiScreen.setWorldAndResolution(this, var3, var4);
                this.skipRenderWorld = false;
            } else {
                this.setIngameFocus();
            }
        }
    }

    private void checkGLError(String par1Str) {
        int var2 = GL11.glGetError();
        if (var2 != 0) {
            String var3 = GLU.gluErrorString(var2);
            System.out.println("########## GL ERROR ##########");
            System.out.println("@ " + par1Str);
            System.out.println(var2 + ": " + var3);
        }
    }

    public void shutdownMinecraftApplet() {
        try {
            this.statFileWriter.func_27175_b();
            this.statFileWriter.syncStats();
            if (this.mcApplet != null) {
                this.mcApplet.clearApplet();
            }

            try {
                if (this.downloadResourcesThread != null) {
                    this.downloadResourcesThread.closeMinecraft();
                }
            } catch (Exception ignored) {
            }

            System.out.println("Stopping!");

            try {
                this.changeWorld1(null);
            } catch (Throwable ignored) {
            }

            try {
                GLAllocation.deleteTexturesAndDisplayLists();
            } catch (Throwable ignored) {
            }

            this.sndManager.closeMinecraft();
            Mouse.destroy();
            Keyboard.destroy();
        } finally {
            Display.destroy();
            if (!this.hasCrashed) {
                System.exit(0);
            }
        }

        System.gc();
    }

    @Override
    public void run() {
        this.running = true;

        try {
            this.startGame();
        } catch (Exception var11) {
            var11.printStackTrace();
            this.onMinecraftCrash(new UnexpectedThrowable("Failed to start game", var11));
            return;
        }

        try {
            while(this.running) {
                try {
                    this.runGameLoop();
                } catch (MinecraftException var9) {
                    this.theWorld = null;
                    this.changeWorld1(null);
                    this.displayGuiScreen(new GuiConflictWarning());
                } catch (OutOfMemoryError var10) {
                    this.freeMemory();
                    this.displayGuiScreen(new GuiMemoryErrorScreen());
                    System.gc();
                }
            }
        } catch (MinecraftError ignored) {
        } catch (Throwable var13) {
            this.freeMemory();
            var13.printStackTrace();
            this.onMinecraftCrash(new UnexpectedThrowable("Unexpected error", var13));
        } finally {
            this.shutdownMinecraftApplet();
        }
    }

    private void runGameLoop() {
        if (this.mcApplet != null && !this.mcApplet.isActive()) {
            this.running = false;
        } else {
            AxisAlignedBB.clearBoundingBoxPool();
            Vec3D.initialize();
            Profiler.startSection("root");
            if (this.mcCanvas == null && Display.isCloseRequested()) {
                this.shutdown();
            }

            if (this.isGamePaused && this.theWorld != null) {
                float var1 = this.timer.renderPartialTicks;
                this.timer.updateTimer();
                this.timer.renderPartialTicks = var1;
            } else {
                this.timer.updateTimer();
            }

            long var6 = System.nanoTime();
            Profiler.startSection("tick");

            for(int var3 = 0; var3 < this.timer.elapsedTicks; ++var3) {
                ++this.ticksRan;

                try {
                    this.runTick();
                } catch (MinecraftException var5) {
                    this.theWorld = null;
                    this.changeWorld1(null);
                    this.displayGuiScreen(new GuiConflictWarning());
                }
            }

            Profiler.endSection();
            long var7 = System.nanoTime() - var6;
            this.checkGLError("Pre render");
            RenderBlocks.fancyGrass = this.gameSettings.fancyGraphics;
            Profiler.startSection("sound");
            this.sndManager.setListener(this.thePlayer, this.timer.renderPartialTicks);
            Profiler.endStartSection("updatelights");
            if (this.theWorld != null) {
                this.theWorld.updatingLighting();
            }

            Profiler.endSection();
            Profiler.startSection("render");
            Profiler.startSection("display");
            GL11.glEnable(3553);
            if (!Keyboard.isKeyDown(65)) {
                Display.update();
            }

            if (this.thePlayer != null && this.thePlayer.isEntityInsideOpaqueBlock()) {
                this.gameSettings.thirdPersonView = 0;
            }

            Profiler.endSection();
            if (!this.skipRenderWorld) {
                Profiler.startSection("gameMode");
                if (this.playerController != null) {
                    this.playerController.setPartialTime(this.timer.renderPartialTicks);
                }

                FMLClientHandler.instance().onRenderTickStart(this.timer.renderPartialTicks);
                Profiler.endStartSection("gameRenderer");
                this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
                Profiler.endSection();
                FMLClientHandler.instance().onRenderTickEnd(this.timer.renderPartialTicks);
            }

            GL11.glFlush();
            Profiler.endSection();
            if (!Display.isActive() && this.fullscreen) {
                this.toggleFullscreen();
            }

            Profiler.endSection();
            if (this.gameSettings.showDebugInfo && this.gameSettings.field_50119_G) {
                if (!Profiler.profilingEnabled) {
                    Profiler.clearProfiling();
                }

                Profiler.profilingEnabled = true;
                this.displayDebugInfo(var7);
            } else {
                Profiler.profilingEnabled = false;
                this.prevFrameTime = System.nanoTime();
            }

            this.guiAchievement.updateAchievementWindow();
            Profiler.startSection("root");
            Thread.yield();
            if (Keyboard.isKeyDown(65)) {
                Display.update();
            }

            this.screenshotListener();
            if (this.mcCanvas != null && !this.fullscreen && (this.mcCanvas.getWidth() != this.displayWidth || this.mcCanvas.getHeight() != this.displayHeight)) {
                this.displayWidth = this.mcCanvas.getWidth();
                this.displayHeight = this.mcCanvas.getHeight();
                if (this.displayWidth <= 0) {
                    this.displayWidth = 1;
                }

                if (this.displayHeight <= 0) {
                    this.displayHeight = 1;
                }

                this.resize(this.displayWidth, this.displayHeight);
            }

            this.checkGLError("Post render");
            ++this.fpsCounter;

            for(this.isGamePaused = !this.isMultiplayerWorld() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame();
                System.currentTimeMillis() >= this.debugUpdateTime + 1000L;
                this.fpsCounter = 0
            ) {
                this.debug = this.fpsCounter + " fps, " + WorldRenderer.chunksUpdated + " chunk updates";
                WorldRenderer.chunksUpdated = 0;
                this.debugUpdateTime += 1000L;
            }

            Profiler.endSection();
        }
    }

    public void freeMemory() {
        try {
            field_28006_b = new byte[0];
            this.renderGlobal.func_28137_f();
        } catch (Throwable ignored) {
        }

        try {
            System.gc();
            AxisAlignedBB.clearBoundingBoxes();
            Vec3D.clearVectorList();
        } catch (Throwable ignored) {
        }

        try {
            System.gc();
            this.changeWorld1(null);
        } catch (Throwable ignored) {
        }

        System.gc();
    }

    private void screenshotListener() {
        if (Keyboard.isKeyDown(60)) {
            if (!this.isTakingScreenshot) {
                this.isTakingScreenshot = true;
                this.ingameGUI.addChatMessage(ScreenShotHelper.saveScreenshot(minecraftDir, this.displayWidth, this.displayHeight));
            }
        } else {
            this.isTakingScreenshot = false;
        }
    }

    private void updateDebugProfilerName(int par1) {
        List<?> var2 = Profiler.getProfilingData(this.debugProfilerName);
        if (var2 != null && var2.size() != 0) {
            ProfilerResult var3 = (ProfilerResult)var2.remove(0);
            if (par1 == 0) {
                if (var3.name.length() > 0) {
                    int var4 = this.debugProfilerName.lastIndexOf(".");
                    if (var4 >= 0) {
                        this.debugProfilerName = this.debugProfilerName.substring(0, var4);
                    }
                }
            } else {
                --par1;
                if (par1 < var2.size() && !((ProfilerResult)var2.get(par1)).name.equals("unspecified")) {
                    if (this.debugProfilerName.length() > 0) {
                        this.debugProfilerName = this.debugProfilerName + ".";
                    }

                    this.debugProfilerName = this.debugProfilerName + ((ProfilerResult)var2.get(par1)).name;
                }
            }
        }
    }

    private void displayDebugInfo(long par1) {
        List<?> var3 = Profiler.getProfilingData(this.debugProfilerName);
        ProfilerResult var4 = (ProfilerResult)var3.remove(0);
        long var5 = 16666666L;
        if (this.prevFrameTime == -1L) {
            this.prevFrameTime = System.nanoTime();
        }

        long var7 = System.nanoTime();
        tickTimes[numRecordedFrameTimes & frameTimes.length - 1] = par1;
        frameTimes[numRecordedFrameTimes++ & frameTimes.length - 1] = var7 - this.prevFrameTime;
        this.prevFrameTime = var7;
        GL11.glClear(256);
        GL11.glMatrixMode(5889);
        GL11.glEnable(2903);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, this.displayWidth, this.displayHeight, 0.0, 1000.0, 3000.0);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        GL11.glLineWidth(1.0F);
        GL11.glDisable(3553);
        Tessellator var9 = Tessellator.instance;
        var9.startDrawing(7);
        int var10 = (int)(var5 / 200000L);
        var9.setColorOpaque_I(536870912);
        var9.addVertex(0.0, this.displayHeight - var10, 0.0);
        var9.addVertex(0.0, this.displayHeight, 0.0);
        var9.addVertex(frameTimes.length, this.displayHeight, 0.0);
        var9.addVertex(frameTimes.length, this.displayHeight - var10, 0.0);
        var9.setColorOpaque_I(538968064);
        var9.addVertex(0.0, this.displayHeight - var10 * 2, 0.0);
        var9.addVertex(0.0, this.displayHeight - var10, 0.0);
        var9.addVertex(frameTimes.length, this.displayHeight - var10, 0.0);
        var9.addVertex(frameTimes.length, this.displayHeight - var10 * 2, 0.0);
        var9.draw();
        long var11 = 0L;

        for (long frameTime : frameTimes) {
            var11 += frameTime;
        }

        int var26 = (int)(var11 / 200000L / (long)frameTimes.length);
        var9.startDrawing(7);
        var9.setColorOpaque_I(541065216);
        var9.addVertex(0.0, this.displayHeight - var26, 0.0);
        var9.addVertex(0.0, this.displayHeight, 0.0);
        var9.addVertex(frameTimes.length, this.displayHeight, 0.0);
        var9.addVertex(frameTimes.length, this.displayHeight - var26, 0.0);
        var9.draw();
        var9.startDrawing(1);

        for(int var14 = 0; var14 < frameTimes.length; ++var14) {
            int var15 = (var14 - numRecordedFrameTimes & frameTimes.length - 1) * 255 / frameTimes.length;
            int var16 = var15 * var15 / 255;
            var16 = var16 * var16 / 255;
            int var17 = var16 * var16 / 255;
            var17 = var17 * var17 / 255;
            if (frameTimes[var14] > var5) {
                var9.setColorOpaque_I(-16777216 + var16 * 65536);
            } else {
                var9.setColorOpaque_I(-16777216 + var16 * 256);
            }

            long var18 = frameTimes[var14] / 200000L;
            long var20 = tickTimes[var14] / 200000L;
            var9.addVertex((float)var14 + 0.5F, (float)((long)this.displayHeight - var18) + 0.5F, 0.0);
            var9.addVertex((float)var14 + 0.5F, (float)this.displayHeight + 0.5F, 0.0);
            var9.setColorOpaque_I(-16777216 + var16 * 65536 + var16 * 256 + var16 * 1);
            var9.addVertex((float)var14 + 0.5F, (float)((long)this.displayHeight - var18) + 0.5F, 0.0);
            var9.addVertex((float)var14 + 0.5F, (float)((long)this.displayHeight - (var18 - var20)) + 0.5F, 0.0);
        }

        var9.draw();
        short var27 = 160;
        int var28 = this.displayWidth - var27 - 10;
        int var30 = this.displayHeight - var27 * 2;
        GL11.glEnable(3042);
        var9.startDrawingQuads();
        var9.setColorRGBA_I(0, 200);
        var9.addVertex((float)var28 - (float)var27 * 1.1F, (float)var30 - (float)var27 * 0.6F - 16.0F, 0.0);
        var9.addVertex((float)var28 - (float)var27 * 1.1F, var30 + var27 * 2, 0.0);
        var9.addVertex((float)var28 + (float)var27 * 1.1F, var30 + var27 * 2, 0.0);
        var9.addVertex((float)var28 + (float)var27 * 1.1F, (float)var30 - (float)var27 * 0.6F - 16.0F, 0.0);
        var9.draw();
        GL11.glDisable(3042);
        double var32 = 0.0;

        for (Object o : var3) {
            ProfilerResult var34 = (ProfilerResult) o;
            int var21 = MathHelper.floor_double(var34.sectionPercentage / 4.0) + 1;
            var9.startDrawing(6);
            var9.setColorOpaque_I(var34.getDisplayColor());
            var9.addVertex(var28, var30, 0.0);

            for (int var22 = var21; var22 >= 0; --var22) {
                float var23 = (float) ((var32 + var34.sectionPercentage * (double) var22 / (double) var21) * (float) Math.PI * 2.0 / 100.0);
                float var24 = MathHelper.sin(var23) * (float) var27;
                float var25 = MathHelper.cos(var23) * (float) var27 * 0.5F;
                var9.addVertex((float) var28 + var24, (float) var30 - var25, 0.0);
            }

            var9.draw();
            var9.startDrawing(5);
            var9.setColorOpaque_I((var34.getDisplayColor() & 16711422) >> 1);

            for (int var41 = var21; var41 >= 0; --var41) {
                float var47 = (float) ((var32 + var34.sectionPercentage * (double) var41 / (double) var21) * (float) Math.PI * 2.0 / 100.0);
                float var48 = MathHelper.sin(var47) * (float) var27;
                float var49 = MathHelper.cos(var47) * (float) var27 * 0.5F;
                var9.addVertex((float) var28 + var48, (float) var30 - var49, 0.0);
                var9.addVertex((float) var28 + var48, (float) var30 - var49 + 10.0F, 0.0);
            }

            var9.draw();
            var32 += var34.sectionPercentage;
        }

        DecimalFormat var33 = new DecimalFormat("##0.00");
        GL11.glEnable(3553);
        String var35 = "";
        if (!var4.name.equals("unspecified")) {
            var35 = var35 + "[0] ";
        }

        if (var4.name.length() == 0) {
            var35 = var35 + "ROOT ";
        } else {
            var35 = var35 + var4.name + " ";
        }

        int var39 = 16777215;
        this.fontRenderer.drawStringWithShadow(var35, var28 - var27, var30 - var27 / 2 - 16, var39);
        this.fontRenderer
            .drawStringWithShadow(
                var35 = var33.format(var4.globalPercentage) + "%", var28 + var27 - this.fontRenderer.getStringWidth(var35), var30 - var27 / 2 - 16, var39
            );

        for(int var38 = 0; var38 < var3.size(); ++var38) {
            ProfilerResult var40 = (ProfilerResult)var3.get(var38);
            String var42 = "";
            if (!var40.name.equals("unspecified")) {
                var42 = var42 + "[" + (var38 + 1) + "] ";
            } else {
                var42 = var42 + "[?] ";
            }

            var42 = var42 + var40.name;
            this.fontRenderer.drawStringWithShadow(var42, var28 - var27, var30 + var27 / 2 + var38 * 8 + 20, var40.getDisplayColor());
            this.fontRenderer
                .drawStringWithShadow(
                    var42 = var33.format(var40.sectionPercentage) + "%",
                    var28 + var27 - 50 - this.fontRenderer.getStringWidth(var42),
                    var30 + var27 / 2 + var38 * 8 + 20,
                    var40.getDisplayColor()
                );
            this.fontRenderer
                .drawStringWithShadow(
                    var42 = var33.format(var40.globalPercentage) + "%",
                    var28 + var27 - this.fontRenderer.getStringWidth(var42),
                    var30 + var27 / 2 + var38 * 8 + 20,
                    var40.getDisplayColor()
                );
        }
    }

    public void shutdown() {
        this.running = false;
    }

    public void setIngameFocus() {
        if (Display.isActive()) {
            if (!this.inGameHasFocus) {
                this.inGameHasFocus = true;
                this.mouseHelper.grabMouseCursor();
                this.displayGuiScreen(null);
                this.leftClickCounter = 10000;
            }
        }
    }

    public void setIngameNotInFocus() {
        if (this.inGameHasFocus) {
            KeyBinding.unPressAllKeys();
            this.inGameHasFocus = false;
            this.mouseHelper.ungrabMouseCursor();
        }
    }

    public void displayInGameMenu() {
        if (this.currentScreen == null) {
            this.displayGuiScreen(new GuiIngameMenu());
        }
    }

    private void sendClickBlockToController(int i, boolean par2) {
        if (!par2) {
            this.leftClickCounter = 0;
        }

        if (i != 0 || this.leftClickCounter <= 0) {
            if (par2 && this.objectMouseOver != null && this.objectMouseOver.typeOfHit == EnumMovingObjectType.TILE && i == 0) {
                int var3 = this.objectMouseOver.blockX;
                int var4 = this.objectMouseOver.blockY;
                int var5 = this.objectMouseOver.blockZ;
                this.playerController.onPlayerDamageBlock(var3, var4, var5, this.objectMouseOver.sideHit);
                if (this.thePlayer.canPlayerEdit(var3, var4, var5)) {
                    this.effectRenderer.addBlockHitEffects(var3, var4, var5, this.objectMouseOver.sideHit);
                    this.thePlayer.swingItem();
                }
            } else {
                this.playerController.resetBlockRemoving();
            }
        }
    }

    private void clickMouse(int par1) {
        if (par1 != 0 || this.leftClickCounter <= 0) {
            if (par1 == 0) {
                this.thePlayer.swingItem();
            }

            if (par1 == 1) {
                this.rightClickDelayTimer = 4;
            }

            boolean var2 = true;
            ItemStack var3 = this.thePlayer.inventory.getCurrentItem();
            if (this.objectMouseOver == null) {
                if (par1 == 0 && this.playerController.isNotCreative()) {
                    this.leftClickCounter = 10;
                }
            } else if (this.objectMouseOver.typeOfHit == EnumMovingObjectType.ENTITY) {
                if (par1 == 0) {
                    this.playerController.attackEntity(this.thePlayer, this.objectMouseOver.entityHit);
                }

                if (par1 == 1) {
                    this.playerController.interactWithEntity(this.thePlayer, this.objectMouseOver.entityHit);
                }
            } else if (this.objectMouseOver.typeOfHit == EnumMovingObjectType.TILE) {
                int var4 = this.objectMouseOver.blockX;
                int var5 = this.objectMouseOver.blockY;
                int var6 = this.objectMouseOver.blockZ;
                int var7 = this.objectMouseOver.sideHit;
                if (par1 == 0) {
                    this.playerController.clickBlock(var4, var5, var6, this.objectMouseOver.sideHit);
                } else {
                    int var9 = var3 != null ? var3.stackSize : 0;
                    if (this.playerController.onPlayerRightClick(this.thePlayer, this.theWorld, var3, var4, var5, var6, var7)) {
                        var2 = false;
                        this.thePlayer.swingItem();
                    }

                    if (var3 == null) {
                        return;
                    }

                    if (var3.stackSize == 0) {
                        this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
                    } else if (var3.stackSize != var9 || this.playerController.isInCreativeMode()) {
                        this.entityRenderer.itemRenderer.func_9449_b();
                    }
                }
            }

            if (var2 && par1 == 1) {
                ItemStack var10 = this.thePlayer.inventory.getCurrentItem();
                if (var10 != null && this.playerController.sendUseItem(this.thePlayer, this.theWorld, var10)) {
                    this.entityRenderer.itemRenderer.func_9450_c();
                }
            }
        }
    }

    public void toggleFullscreen() {
        try {
            this.fullscreen = !this.fullscreen;
            if (this.fullscreen) {
                Display.setDisplayMode(Display.getDesktopDisplayMode());
                this.displayWidth = Display.getDisplayMode().getWidth();
                this.displayHeight = Display.getDisplayMode().getHeight();
                if (this.displayWidth <= 0) {
                    this.displayWidth = 1;
                }

                if (this.displayHeight <= 0) {
                    this.displayHeight = 1;
                }
            } else {
                if (this.mcCanvas != null) {
                    this.displayWidth = this.mcCanvas.getWidth();
                    this.displayHeight = this.mcCanvas.getHeight();
                } else {
                    this.displayWidth = this.tempDisplayWidth;
                    this.displayHeight = this.tempDisplayHeight;
                }

                if (this.displayWidth <= 0) {
                    this.displayWidth = 1;
                }

                if (this.displayHeight <= 0) {
                    this.displayHeight = 1;
                }
            }

            if (this.currentScreen != null) {
                this.resize(this.displayWidth, this.displayHeight);
            }

            Display.setFullscreen(this.fullscreen);
            Display.update();
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    private void resize(int i, int par2) {
        if (i <= 0) {
            i = 1;
        }

        if (par2 <= 0) {
            par2 = 1;
        }

        this.displayWidth = i;
        this.displayHeight = par2;
        if (this.currentScreen != null) {
            ScaledResolution var3 = new ScaledResolution(this.gameSettings, i, par2);
            int var4 = var3.getScaledWidth();
            int var5 = var3.getScaledHeight();
            this.currentScreen.setWorldAndResolution(this, var4, var5);
        }
    }

    private void startThreadCheckHasPaid() {
        new ThreadCheckHasPaid(this).start();
    }

    public void runTick() {
        if (this.rightClickDelayTimer > 0) {
            --this.rightClickDelayTimer;
        }

        if (this.ticksRan == 6000) {
            this.startThreadCheckHasPaid();
        }

        FMLClientHandler.instance().onPreWorldTick();
        Profiler.startSection("stats");
        this.statFileWriter.func_27178_d();
        Profiler.endStartSection("gui");
        if (!this.isGamePaused) {
            this.ingameGUI.updateTick();
        }

        Profiler.endStartSection("pick");
        this.entityRenderer.getMouseOver(1.0F);
        Profiler.endStartSection("centerChunkSource");
        if (this.thePlayer != null) {
            IChunkProvider var1 = this.theWorld.getChunkProvider();
            if (var1 instanceof ChunkProviderLoadOrGenerate) {
                ChunkProviderLoadOrGenerate var2 = (ChunkProviderLoadOrGenerate)var1;
                int var3 = MathHelper.floor_float((float)((int)this.thePlayer.posX)) >> 4;
                int var4 = MathHelper.floor_float((float)((int)this.thePlayer.posZ)) >> 4;
                var2.setCurrentChunkOver(var3, var4);
            }
        }

        Profiler.endStartSection("gameMode");
        if (!this.isGamePaused && this.theWorld != null) {
            this.playerController.updateController();
        }

        GL11.glBindTexture(3553, this.renderEngine.getTexture("/terrain.png"));
        Profiler.endStartSection("textures");
        if (!this.isGamePaused) {
            this.renderEngine.updateDynamicTextures();
        }

        if (this.currentScreen == null && this.thePlayer != null) {
            if (this.thePlayer.getHealth() <= 0) {
                this.displayGuiScreen(null);
            } else if (this.thePlayer.isPlayerSleeping() && this.theWorld != null && this.theWorld.isRemote) {
                this.displayGuiScreen(new GuiSleepMP());
            }
        } else if (this.currentScreen != null && this.currentScreen instanceof GuiSleepMP && !this.thePlayer.isPlayerSleeping()) {
            this.displayGuiScreen(null);
        }

        if (this.currentScreen != null) {
            this.leftClickCounter = 10000;
        }

        if (this.currentScreen != null) {
            this.currentScreen.handleInput();
            if (this.currentScreen != null) {
                this.currentScreen.guiParticles.update();
                this.currentScreen.updateScreen();
            }
        }

        if (this.currentScreen == null || this.currentScreen.allowUserInput) {
            Profiler.endStartSection("mouse");

            while(Mouse.next()) {
                KeyBinding.setKeyBindState(Mouse.getEventButton() - 100, Mouse.getEventButtonState());
                if (Mouse.getEventButtonState()) {
                    KeyBinding.onTick(Mouse.getEventButton() - 100);
                }

                long var5 = System.currentTimeMillis() - this.systemTime;
                if (var5 <= 200L) {
                    int var9 = Mouse.getEventDWheel();
                    if (var9 != 0) {
                        this.thePlayer.inventory.changeCurrentItem(var9);
                        if (this.gameSettings.noclip) {
                            if (var9 > 0) {
                                var9 = 1;
                            }

                            if (var9 < 0) {
                                var9 = -1;
                            }

                            this.gameSettings.noclipRate += (float)var9 * 0.25F;
                        }
                    }

                    if (this.currentScreen == null) {
                        if (!this.inGameHasFocus && Mouse.getEventButtonState()) {
                            this.setIngameFocus();
                        }
                    } else if (this.currentScreen != null) {
                        this.currentScreen.handleMouseInput();
                    }
                }
            }

            if (this.leftClickCounter > 0) {
                --this.leftClickCounter;
            }

            Profiler.endStartSection("keyboard");

            while(Keyboard.next()) {
                KeyBinding.setKeyBindState(Keyboard.getEventKey(), Keyboard.getEventKeyState());
                if (Keyboard.getEventKeyState()) {
                    KeyBinding.onTick(Keyboard.getEventKey());
                }

                if (Keyboard.getEventKeyState()) {
                    if (Keyboard.getEventKey() == 87) {
                        this.toggleFullscreen();
                    } else {
                        if (this.currentScreen != null) {
                            this.currentScreen.handleKeyboardInput();
                        } else {
                            if (Keyboard.getEventKey() == 1) {
                                this.displayInGameMenu();
                            }

                            if (Keyboard.getEventKey() == 31 && Keyboard.isKeyDown(61)) {
                                this.forceReload();
                            }

                            if (Keyboard.getEventKey() == 20 && Keyboard.isKeyDown(61)) {
                                this.renderEngine.refreshTextures();
                            }

                            if (Keyboard.getEventKey() == 33 && Keyboard.isKeyDown(61)) {
                                boolean var6 = Keyboard.isKeyDown(42) | Keyboard.isKeyDown(54);
                                this.gameSettings.setOptionValue(EnumOptions.RENDER_DISTANCE, var6 ? -1 : 1);
                            }

                            if (Keyboard.getEventKey() == 30 && Keyboard.isKeyDown(61)) {
                                this.renderGlobal.loadRenderers();
                            }

                            if (Keyboard.getEventKey() == 59) {
                                this.gameSettings.hideGUI = !this.gameSettings.hideGUI;
                            }

                            if (Keyboard.getEventKey() == 61) {
                                this.gameSettings.showDebugInfo = !this.gameSettings.showDebugInfo;
                                this.gameSettings.field_50119_G = !GuiScreen.func_50049_m();
                            }

                            if (Keyboard.getEventKey() == 63) {
                                ++this.gameSettings.thirdPersonView;
                                if (this.gameSettings.thirdPersonView > 2) {
                                    this.gameSettings.thirdPersonView = 0;
                                }
                            }

                            if (Keyboard.getEventKey() == 66) {
                                this.gameSettings.smoothCamera = !this.gameSettings.smoothCamera;
                            }
                        }

                        for(int var7 = 0; var7 < 9; ++var7) {
                            if (Keyboard.getEventKey() == 2 + var7) {
                                this.thePlayer.inventory.currentItem = var7;
                            }
                        }

                        if (this.gameSettings.showDebugInfo && this.gameSettings.field_50119_G) {
                            if (Keyboard.getEventKey() == 11) {
                                this.updateDebugProfilerName(0);
                            }

                            for(int var8 = 0; var8 < 9; ++var8) {
                                if (Keyboard.getEventKey() == 2 + var8) {
                                    this.updateDebugProfilerName(var8 + 1);
                                }
                            }
                        }
                    }
                }
            }

            while(this.gameSettings.keyBindInventory.isPressed()) {
                this.displayGuiScreen(new GuiInventory(this.thePlayer));
            }

            while(this.gameSettings.keyBindDrop.isPressed()) {
                this.thePlayer.dropOneItem();
            }

            while(this.isMultiplayerWorld() && this.gameSettings.keyBindChat.isPressed()) {
                this.displayGuiScreen(new GuiChat());
            }

            if (this.isMultiplayerWorld() && this.currentScreen == null && (Keyboard.isKeyDown(53) || Keyboard.isKeyDown(181))) {
                this.displayGuiScreen(new GuiChat("/"));
            }

            if (this.thePlayer.isUsingItem()) {
                if (!this.gameSettings.keyBindUseItem.pressed) {
                    this.playerController.onStoppedUsingItem(this.thePlayer);
                }

                while (this.gameSettings.keyBindAttack.isPressed());
                while (this.gameSettings.keyBindUseItem.isPressed());
                while (this.gameSettings.keyBindPickBlock.isPressed());
            } else {
                while(this.gameSettings.keyBindAttack.isPressed()) {
                    this.clickMouse(0);
                }

                while(this.gameSettings.keyBindUseItem.isPressed()) {
                    this.clickMouse(1);
                }

                while(this.gameSettings.keyBindPickBlock.isPressed()) {
                    this.clickMiddleMouseButton();
                }
            }

            if (this.gameSettings.keyBindUseItem.pressed && this.rightClickDelayTimer == 0 && !this.thePlayer.isUsingItem()) {
                this.clickMouse(1);
            }

            this.sendClickBlockToController(0, this.currentScreen == null && this.gameSettings.keyBindAttack.pressed && this.inGameHasFocus);
        }

        if (this.theWorld != null) {
            if (this.thePlayer != null) {
                ++this.joinPlayerCounter;
                if (this.joinPlayerCounter == 30) {
                    this.joinPlayerCounter = 0;
                    this.theWorld.joinEntityInSurroundings(this.thePlayer);
                }
            }

            if (this.theWorld.getWorldInfo().isHardcoreModeEnabled()) {
                this.theWorld.difficultySetting = 3;
            } else {
                this.theWorld.difficultySetting = this.gameSettings.difficulty;
            }

            if (this.theWorld.isRemote) {
                this.theWorld.difficultySetting = 1;
            }

            Profiler.endStartSection("gameRenderer");
            if (!this.isGamePaused) {
                this.entityRenderer.updateRenderer();
            }

            Profiler.endStartSection("levelRenderer");
            if (!this.isGamePaused) {
                this.renderGlobal.updateClouds();
            }

            Profiler.endStartSection("level");
            if (!this.isGamePaused) {
                if (this.theWorld.lightningFlash > 0) {
                    --this.theWorld.lightningFlash;
                }

                this.theWorld.updateEntities();
            }

            if (!this.isGamePaused || this.isMultiplayerWorld()) {
                this.theWorld.setAllowedSpawnTypes(this.theWorld.difficultySetting > 0, true);
                this.theWorld.tick();
            }

            Profiler.endStartSection("animateTick");
            if (!this.isGamePaused && this.theWorld != null) {
                this.theWorld
                    .randomDisplayUpdates(
                        MathHelper.floor_double(this.thePlayer.posX), MathHelper.floor_double(this.thePlayer.posY), MathHelper.floor_double(this.thePlayer.posZ)
                    );
            }

            Profiler.endStartSection("particles");
            if (!this.isGamePaused) {
                this.effectRenderer.updateEffects();
            }
        }

        Profiler.endSection();
        this.systemTime = System.currentTimeMillis();
    }

    private void forceReload() {
        System.out.println("FORCING RELOAD!");
        this.sndManager = new SoundManager();
        this.sndManager.loadSoundSettings(this.gameSettings);
        this.downloadResourcesThread.reloadResources();
    }

    public boolean isMultiplayerWorld() {
        return this.theWorld != null && this.theWorld.isRemote;
    }

    public void startWorld(String string, String string2, WorldSettings par3WorldSettings) {
        this.changeWorld1(null);
        System.gc();
        if (this.saveLoader.isOldMapFormat(string)) {
            this.convertMapFormat(string, string2);
        } else {
            if (this.loadingScreen != null) {
                this.loadingScreen.printText(StatCollector.translateToLocal("menu.switchingLevel"));
                this.loadingScreen.displayLoadingString("");
            }

            ISaveHandler var4 = this.saveLoader.getSaveLoader(string, false);
            Object var5 = null;
            World var6 = new World(var4, string2, par3WorldSettings);
            if (var6.isNewWorld) {
                this.statFileWriter.readStat(StatList.createWorldStat, 1);
                this.statFileWriter.readStat(StatList.startGameStat, 1);
                this.changeWorld2(var6, StatCollector.translateToLocal("menu.generatingLevel"));
            } else {
                this.statFileWriter.readStat(StatList.loadWorldStat, 1);
                this.statFileWriter.readStat(StatList.startGameStat, 1);
                this.changeWorld2(var6, StatCollector.translateToLocal("menu.loadingLevel"));
            }
        }
    }

    public void usePortal(int par1) {
        int var2 = this.thePlayer.dimension;
        this.thePlayer.dimension = par1;
        this.theWorld.setEntityDead(this.thePlayer);
        this.thePlayer.isDead = false;
        double var3 = this.thePlayer.posX;
        double var5 = this.thePlayer.posZ;
        double var7 = 1.0;
        if (var2 > -1 && this.thePlayer.dimension == -1) {
            var7 = 0.125;
        } else if (var2 == -1 && this.thePlayer.dimension > -1) {
            var7 = 8.0;
        }

        var3 *= var7;
        var5 *= var7;
        if (this.thePlayer.dimension == -1) {
            this.thePlayer.setLocationAndAngles(var3, this.thePlayer.posY, var5, this.thePlayer.rotationYaw, this.thePlayer.rotationPitch);
            if (this.thePlayer.isEntityAlive()) {
                this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
            }

            Object var9 = null;
            World var13 = new World(this.theWorld, WorldProvider.getProviderForDimension(this.thePlayer.dimension));
            this.changeWorld(var13, "Entering the Nether", this.thePlayer);
        } else if (this.thePlayer.dimension == 0) {
            if (this.thePlayer.isEntityAlive()) {
                this.thePlayer.setLocationAndAngles(var3, this.thePlayer.posY, var5, this.thePlayer.rotationYaw, this.thePlayer.rotationPitch);
                this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
            }

            Object var14 = null;
            World var15 = new World(this.theWorld, WorldProvider.getProviderForDimension(this.thePlayer.dimension));
            if (var2 == -1) {
                this.changeWorld(var15, "Leaving the Nether", this.thePlayer);
            } else {
                this.changeWorld(var15, "Leaving the End", this.thePlayer);
            }
        } else {
            World var16 = null;
            var16 = new World(this.theWorld, WorldProvider.getProviderForDimension(this.thePlayer.dimension));
            ChunkCoordinates var10 = var16.getEntrancePortalLocation();
            var3 = var10.posX;
            this.thePlayer.posY = var10.posY;
            var5 = var10.posZ;
            this.thePlayer.setLocationAndAngles(var3, this.thePlayer.posY, var5, 90.0F, 0.0F);
            if (this.thePlayer.isEntityAlive()) {
                var16.updateEntityWithOptionalForce(this.thePlayer, false);
            }

            this.changeWorld(var16, "Entering the End", this.thePlayer);
        }

        this.thePlayer.worldObj = this.theWorld;
        System.out.println("Teleported to " + this.theWorld.worldProvider.worldType);
        if (this.thePlayer.isEntityAlive() && var2 < 1) {
            this.thePlayer.setLocationAndAngles(var3, this.thePlayer.posY, var5, this.thePlayer.rotationYaw, this.thePlayer.rotationPitch);
            this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
            new Teleporter().placeInPortal(this.theWorld, this.thePlayer);
        }
    }

    public void exitToMainMenu(String par1Str) {
        this.theWorld = null;
        this.changeWorld2(null, par1Str);
    }

    public void changeWorld1(World par1World) {
        this.changeWorld2(par1World, "");
    }

    public void changeWorld2(World world, String par2Str) {
        this.changeWorld(world, par2Str, null);
    }

    public void changeWorld(World world, String string, EntityPlayer par3EntityPlayer) {
        this.statFileWriter.func_27175_b();
        this.statFileWriter.syncStats();
        this.renderViewEntity = null;
        if (this.loadingScreen != null) {
            this.loadingScreen.printText(string);
            this.loadingScreen.displayLoadingString("");
        }

        this.sndManager.playStreaming(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        if (this.theWorld != null) {
            this.theWorld.saveWorldIndirectly(this.loadingScreen);
        }

        this.theWorld = world;
        if (world != null) {
            if (this.playerController != null) {
                this.playerController.onWorldChange(world);
            }

            if (!this.isMultiplayerWorld()) {
                if (par3EntityPlayer == null) {
                    this.thePlayer = (EntityPlayerSP)world.func_4085_a(EntityPlayerSP.class);
                }
            } else if (this.thePlayer != null) {
                this.thePlayer.preparePlayerToSpawn();
                if (world != null) {
                    world.spawnEntityInWorld(this.thePlayer);
                }
            }

            if (!world.isRemote) {
                this.preloadWorld(string);
            }

            if (this.thePlayer == null) {
                this.thePlayer = (EntityPlayerSP)this.playerController.createPlayer(world);
                this.thePlayer.preparePlayerToSpawn();
                this.playerController.flipPlayer(this.thePlayer);
            }

            this.thePlayer.movementInput = new MovementInputFromOptions(this.gameSettings);
            if (this.renderGlobal != null) {
                this.renderGlobal.changeWorld(world);
            }

            if (this.effectRenderer != null) {
                this.effectRenderer.clearEffects(world);
            }

            if (par3EntityPlayer != null) {
                world.func_6464_c();
            }

            IChunkProvider var4 = world.getChunkProvider();
            if (var4 instanceof ChunkProviderLoadOrGenerate) {
                ChunkProviderLoadOrGenerate var5 = (ChunkProviderLoadOrGenerate)var4;
                int var6 = MathHelper.floor_float((float)((int)this.thePlayer.posX)) >> 4;
                int var7 = MathHelper.floor_float((float)((int)this.thePlayer.posZ)) >> 4;
                var5.setCurrentChunkOver(var6, var7);
            }

            world.spawnPlayerWithLoadedChunks(this.thePlayer);
            this.playerController.func_6473_b(this.thePlayer);
            if (world.isNewWorld) {
                world.saveWorldIndirectly(this.loadingScreen);
            }

            this.renderViewEntity = this.thePlayer;
        } else {
            this.saveLoader.flushCache();
            this.thePlayer = null;
        }

        System.gc();
        this.systemTime = 0L;
        FMLClientHandler.instance().onWorldLoadTick();
    }

    private void convertMapFormat(String string, String par2Str) {
        this.loadingScreen.printText("Converting World to " + this.saveLoader.getFormatName());
        this.loadingScreen.displayLoadingString("This may take a while :)");
        this.saveLoader.convertMapFormat(string, this.loadingScreen);
        this.startWorld(string, par2Str, new WorldSettings(0L, 0, true, false, WorldType.DEFAULT));
    }

    private void preloadWorld(String par1Str) {
        if (this.loadingScreen != null) {
            this.loadingScreen.printText(par1Str);
            this.loadingScreen.displayLoadingString(StatCollector.translateToLocal("menu.generatingTerrain"));
        }

        short var2 = 128;
        if (this.playerController.func_35643_e()) {
            var2 = 64;
        }

        int var3 = 0;
        int var4 = var2 * 2 / 16 + 1;
        var4 *= var4;
        IChunkProvider var5 = this.theWorld.getChunkProvider();
        ChunkCoordinates var6 = this.theWorld.getSpawnPoint();
        if (this.thePlayer != null) {
            var6.posX = (int)this.thePlayer.posX;
            var6.posZ = (int)this.thePlayer.posZ;
        }

        if (var5 instanceof ChunkProviderLoadOrGenerate) {
            ChunkProviderLoadOrGenerate var7 = (ChunkProviderLoadOrGenerate)var5;
            var7.setCurrentChunkOver(var6.posX >> 4, var6.posZ >> 4);
        }

        for(int var11 = -var2; var11 <= var2; var11 += 16) {
            for(int var8 = -var2; var8 <= var2; var8 += 16) {
                if (this.loadingScreen != null) {
                    this.loadingScreen.setLoadingProgress(var3++ * 100 / var4);
                }

                this.theWorld.getBlockId(var6.posX + var11, 64, var6.posZ + var8);
                if (!this.playerController.func_35643_e()) {
                    while(this.theWorld.updatingLighting()) {
                    }
                }
            }
        }

        if (!this.playerController.func_35643_e()) {
            if (this.loadingScreen != null) {
                this.loadingScreen.displayLoadingString(StatCollector.translateToLocal("menu.simulating"));
            }

            var4 = 2000;
            this.theWorld.dropOldChunks();
        }
    }

    public void installResource(String string, File par2File) {
        int var3 = string.indexOf("/");
        String var4 = string.substring(0, var3);
        string = string.substring(var3 + 1);
        if (var4.equalsIgnoreCase("sound")) {
            this.sndManager.addSound(string, par2File);
        } else if (var4.equalsIgnoreCase("newsound")) {
            this.sndManager.addSound(string, par2File);
        } else if (var4.equalsIgnoreCase("streaming")) {
            this.sndManager.addStreaming(string, par2File);
        } else if (var4.equalsIgnoreCase("music")) {
            this.sndManager.addMusic(string, par2File);
        } else if (var4.equalsIgnoreCase("newmusic")) {
            this.sndManager.addMusic(string, par2File);
        }
    }

    public String debugInfoRenders() {
        return this.renderGlobal.getDebugInfoRenders();
    }

    public String getEntityDebug() {
        return this.renderGlobal.getDebugInfoEntities();
    }

    public String getWorldProviderName() {
        return this.theWorld.getProviderName();
    }

    public String debugInfoEntities() {
        return "P: " + this.effectRenderer.getStatistics() + ". T: " + this.theWorld.getDebugLoadedEntities();
    }

    public void respawn(boolean bl, int i, boolean par3) {
        if (!this.theWorld.isRemote && !this.theWorld.worldProvider.canRespawnHere()) {
            this.usePortal(0);
        }

        ChunkCoordinates var4 = null;
        ChunkCoordinates var5 = null;
        boolean var6 = true;
        if (this.thePlayer != null && !bl) {
            var4 = this.thePlayer.getSpawnChunk();
            if (var4 != null) {
                var5 = EntityPlayer.verifyRespawnCoordinates(this.theWorld, var4);
                if (var5 == null) {
                    this.thePlayer.addChatMessage("tile.bed.notValid");
                }
            }
        }

        if (var5 == null) {
            var5 = this.theWorld.getSpawnPoint();
            var6 = false;
        }

        IChunkProvider var7 = this.theWorld.getChunkProvider();
        if (var7 instanceof ChunkProviderLoadOrGenerate) {
            ChunkProviderLoadOrGenerate var8 = (ChunkProviderLoadOrGenerate)var7;
            var8.setCurrentChunkOver(var5.posX >> 4, var5.posZ >> 4);
        }

        this.theWorld.setSpawnLocation();
        this.theWorld.updateEntityList();
        int var10 = 0;
        if (this.thePlayer != null) {
            var10 = this.thePlayer.entityId;
            this.theWorld.setEntityDead(this.thePlayer);
        }

        EntityPlayerSP var9 = this.thePlayer;
        this.renderViewEntity = null;
        this.thePlayer = (EntityPlayerSP)this.playerController.createPlayer(this.theWorld);
        if (par3) {
            this.thePlayer.copyPlayer(var9);
        }

        this.thePlayer.dimension = i;
        this.renderViewEntity = this.thePlayer;
        this.thePlayer.preparePlayerToSpawn();
        if (var6) {
            this.thePlayer.setSpawnChunk(var4);
            this.thePlayer
                .setLocationAndAngles((float)var5.posX + 0.5F, (float)var5.posY + 0.1F, (float)var5.posZ + 0.5F, 0.0F, 0.0F);
        }

        this.playerController.flipPlayer(this.thePlayer);
        this.theWorld.spawnPlayerWithLoadedChunks(this.thePlayer);
        this.thePlayer.movementInput = new MovementInputFromOptions(this.gameSettings);
        this.thePlayer.entityId = var10;
        this.thePlayer.func_6420_o();
        this.playerController.func_6473_b(this.thePlayer);
        this.preloadWorld(StatCollector.translateToLocal("menu.respawning"));
        if (this.currentScreen instanceof GuiGameOver) {
            this.displayGuiScreen(null);
        }
    }

    public static void startMainThread1(String string, String par1Str) {
        startMainThread(string, par1Str, null);
    }

    public static void startMainThread(String string, String string2, String par2Str) {
        boolean var3 = false;
        Frame var5 = new Frame("Minecraft");
        Canvas var6 = new Canvas();
        var5.setLayout(new BorderLayout());
        var5.add(var6, "Center");
        var6.setPreferredSize(new Dimension(854, 480));
        var5.pack();
        var5.setLocationRelativeTo(null);
        MinecraftImpl var7 = new MinecraftImpl(var5, var6, null, 854, 480, var3, var5);
        Thread var8 = new Thread(var7, "Minecraft main thread");
        var8.setPriority(10);
        var7.minecraftUri = "www.minecraft.net";
        if (string != null && string2 != null) {
            var7.session = new Session(string, string2);
        } else {
            var7.session = new Session("Player" + System.currentTimeMillis() % 1000L, "");
        }

        if (par2Str != null) {
            String[] var9 = par2Str.split(":");
            var7.setServer(var9[0], Integer.parseInt(var9[1]));
        }

        var5.setVisible(true);
        var5.addWindowListener(new GameWindowListener(var7, var8));
        var8.start();
    }

    public NetClientHandler getSendQueue() {
        return this.thePlayer instanceof EntityClientPlayerMP ? ((EntityClientPlayerMP)this.thePlayer).sendQueue : null;
    }

    public static void main(String[] strings) {
        String username = "Player" + System.currentTimeMillis() % 1000L;
        if (strings.length > 0) username = strings[0];

        String sessionToken = "-";
        if (strings.length > 1) sessionToken = strings[1];

        FMLClientHandler.instance().preGameLoad(username, sessionToken);
    }

    public static void fmlReentry(String username, String sessionToken) {
        startMainThread1(username, sessionToken);
    }

    public static boolean isGuiEnabled() {
        return theMinecraft == null || !theMinecraft.gameSettings.hideGUI;
    }

    public static boolean isFancyGraphicsEnabled() {
        return theMinecraft != null && theMinecraft.gameSettings.fancyGraphics;
    }

    public static boolean isAmbientOcclusionEnabled() {
        return theMinecraft != null && theMinecraft.gameSettings.ambientOcclusion;
    }

    public static boolean isDebugInfoEnabled() {
        return theMinecraft != null && theMinecraft.gameSettings.showDebugInfo;
    }

    public boolean lineIsCommand(String par1Str) {
        return par1Str.startsWith("/");
    }

    private void clickMiddleMouseButton() {
        if (this.objectMouseOver != null) {
            boolean var1 = this.thePlayer.capabilities.isCreativeMode;
            int var2 = this.theWorld.getBlockId(this.objectMouseOver.blockX, this.objectMouseOver.blockY, this.objectMouseOver.blockZ);
            if (!var1) {
                if (var2 == Block.grass.blockID) {
                    var2 = Block.dirt.blockID;
                }

                if (var2 == Block.stairDouble.blockID) {
                    var2 = Block.stairSingle.blockID;
                }

                if (var2 == Block.bedrock.blockID) {
                    var2 = Block.stone.blockID;
                }
            }

            int var3 = 0;
            boolean var4 = false;
            if (Item.itemsList[var2] != null && Item.itemsList[var2].getHasSubtypes()) {
                var3 = this.theWorld.getBlockMetadata(this.objectMouseOver.blockX, this.objectMouseOver.blockY, this.objectMouseOver.blockZ);
                var4 = true;
            }

            if (Item.itemsList[var2] != null && Item.itemsList[var2] instanceof ItemBlock) {
                Block var5 = Block.blocksList[var2];
                int var6 = var5.idDropped(var3, this.thePlayer.worldObj.rand, 0);
                if (var6 > 0) {
                    var2 = var6;
                }
            }

            this.thePlayer.inventory.setCurrentItem(var2, var3, var4, var1);
            if (var1) {
                int var7 = this.thePlayer.inventorySlots.inventorySlots.size() - 9 + this.thePlayer.inventory.currentItem;
                this.playerController.sendSlotPacket(this.thePlayer.inventory.getStackInSlot(this.thePlayer.inventory.currentItem), var7);
            }
        }
    }

    public static String func_52003_C() {
        return "1.2.5";
    }

    public static void func_52004_D() {
        PlayerUsageSnooper var0 = new PlayerUsageSnooper("client");
        var0.func_52022_a("version", func_52003_C());
        var0.func_52022_a("os_name", System.getProperty("os.name"));
        var0.func_52022_a("os_version", System.getProperty("os.version"));
        var0.func_52022_a("os_architecture", System.getProperty("os.arch"));
        var0.func_52022_a("memory_total", Runtime.getRuntime().totalMemory());
        var0.func_52022_a("memory_max", Runtime.getRuntime().maxMemory());
        var0.func_52022_a("java_version", System.getProperty("java.version"));
        var0.func_52022_a("opengl_version", GL11.glGetString(7938));
        var0.func_52022_a("opengl_vendor", GL11.glGetString(7936));
        var0.func_52021_a();
    }
}
