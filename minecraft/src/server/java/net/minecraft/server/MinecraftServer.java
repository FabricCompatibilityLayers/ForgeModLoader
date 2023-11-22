//
// Source code recreated from a .class file by Quiltflower
//

package net.minecraft.server;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.src.AnvilSaveConverter;
import net.minecraft.src.AnvilSaveHandler;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.ConsoleCommandHandler;
import net.minecraft.src.ConsoleLogManager;
import net.minecraft.src.ConvertProgressUpdater;
import net.minecraft.src.EntityTracker;
import net.minecraft.src.ICommandListener;
import net.minecraft.src.ISaveFormat;
import net.minecraft.src.IServer;
import net.minecraft.src.IUpdatePlayerListBox;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NetworkListenThread;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet4UpdateTime;
import net.minecraft.src.PropertyManager;
import net.minecraft.src.RConConsoleSource;
import net.minecraft.src.RConThreadMain;
import net.minecraft.src.RConThreadQuery;
import net.minecraft.src.ServerCommand;
import net.minecraft.src.ServerConfigurationManager;
import net.minecraft.src.ServerGUI;
import net.minecraft.src.StatList;
import net.minecraft.src.ThreadCommandReader;
import net.minecraft.src.ThreadServerApplication;
import net.minecraft.src.ThreadServerSleep;
import net.minecraft.src.Vec3D;
import net.minecraft.src.WorldManager;
import net.minecraft.src.WorldServer;
import net.minecraft.src.WorldServerMulti;
import net.minecraft.src.WorldSettings;
import net.minecraft.src.WorldType;

public class MinecraftServer implements Runnable, ICommandListener, IServer {
    public static Logger logger = Logger.getLogger("Minecraft");
    public static HashMap field_6037_b = new HashMap<>();
    private String hostname;
    private int serverPort;
    public NetworkListenThread networkServer;
    public PropertyManager propertyManagerObj;
    public WorldServer[] worldMngr;
    public long[] field_40027_f = new long[100];
    public long[][] field_40028_g;
    public ServerConfigurationManager configManager;
    private ConsoleCommandHandler commandHandler;
    private boolean serverRunning = true;
    public boolean serverStopped = false;
    int deathTime = 0;
    public String currentTask;
    public int percentDone;
    private List<IUpdatePlayerListBox> playersOnline = new ArrayList<>();
    private List commands = Collections.synchronizedList(new ArrayList<>());
    public EntityTracker[] entityTracker = new EntityTracker[3];
    public boolean onlineMode;
    public boolean spawnPeacefulMobs;
    public boolean field_44002_p;
    public boolean pvpOn;
    public boolean allowFlight;
    public String motd;
    public int buildLimit;
    private long field_48074_E;
    private long field_48075_F;
    private long field_48076_G;
    private long field_48077_H;
    public long[] field_48080_u = new long[100];
    public long[] field_48079_v = new long[100];
    public long[] field_48078_w = new long[100];
    public long[] field_48082_x = new long[100];
    private RConThreadQuery rconQueryThread;
    private RConThreadMain rconMainThread;

    public MinecraftServer() {
        new ThreadServerSleep(this);
    }

    private boolean startServer() throws UnknownHostException {
        this.commandHandler = new ConsoleCommandHandler(this);
        ThreadCommandReader var1 = new ThreadCommandReader(this);
        var1.setDaemon(true);
        var1.start();
        ConsoleLogManager.init();
        logger.info("Starting minecraft server version 1.2.5");
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            logger.warning("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        logger.info("Loading properties");
        this.propertyManagerObj = new PropertyManager(new File("server.properties"));
        this.hostname = this.propertyManagerObj.getStringProperty("server-ip", "");
        this.onlineMode = this.propertyManagerObj.getBooleanProperty("online-mode", true);
        this.spawnPeacefulMobs = this.propertyManagerObj.getBooleanProperty("spawn-animals", true);
        this.field_44002_p = this.propertyManagerObj.getBooleanProperty("spawn-npcs", true);
        this.pvpOn = this.propertyManagerObj.getBooleanProperty("pvp", true);
        this.allowFlight = this.propertyManagerObj.getBooleanProperty("allow-flight", false);
        this.motd = this.propertyManagerObj.getStringProperty("motd", "A Minecraft Server");
        this.motd.replace('§', '$');
        InetAddress var2 = null;
        if (this.hostname.length() > 0) {
            var2 = InetAddress.getByName(this.hostname);
        }

        this.serverPort = this.propertyManagerObj.getIntProperty("server-port", 25565);
        logger.info("Starting Minecraft server on " + (this.hostname.length() == 0 ? "*" : this.hostname) + ":" + this.serverPort);

        this.networkServer = new NetworkListenThread(this, var2, this.serverPort);

        if (!this.onlineMode) {
            logger.warning("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            logger.warning("The server will make no attempt to authenticate usernames. Beware.");
            logger.warning("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            logger.warning("To change this, set \"online-mode\" to \"true\" in the server.settings file.");
        }

        this.configManager = new ServerConfigurationManager(this);
        this.entityTracker[0] = new EntityTracker(this, 0);
        this.entityTracker[1] = new EntityTracker(this, -1);
        this.entityTracker[2] = new EntityTracker(this, 1);
        long var3 = System.nanoTime();
        String var5 = this.propertyManagerObj.getStringProperty("level-name", "world");
        String var6 = this.propertyManagerObj.getStringProperty("level-seed", "");
        String var7 = this.propertyManagerObj.getStringProperty("level-type", "DEFAULT");
        long var8 = new Random().nextLong();
        if (var6.length() > 0) {
            try {
                long var10 = Long.parseLong(var6);
                if (var10 != 0L) {
                    var8 = var10;
                }
            } catch (NumberFormatException var14) {
                var8 = (long)var6.hashCode();
            }
        }

        WorldType var16 = WorldType.parseWorldType(var7);
        if (var16 == null) {
            var16 = WorldType.DEFAULT;
        }

        this.buildLimit = this.propertyManagerObj.getIntProperty("max-build-height", 256);
        this.buildLimit = (this.buildLimit + 8) / 16 * 16;
        this.buildLimit = MathHelper.clamp_int(this.buildLimit, 64, 256);
        this.propertyManagerObj.setProperty("max-build-height", this.buildLimit);
        logger.info("Preparing level \"" + var5 + "\"");
        this.initWorld(new AnvilSaveConverter(new File(".")), var5, var8, var16);
        long var11 = System.nanoTime() - var3;
        String var13 = String.format("%.3fs", (double)var11 / 1.0E9);
        logger.info("Done (" + var13 + ")! For help, type \"help\" or \"?\"");
        if (this.propertyManagerObj.getBooleanProperty("enable-query", false)) {
            logger.info("Starting GS4 status listener");
            this.rconQueryThread = new RConThreadQuery(this);
            this.rconQueryThread.startThread();
        }

        if (this.propertyManagerObj.getBooleanProperty("enable-rcon", false)) {
            logger.info("Starting remote control listener");
            this.rconMainThread = new RConThreadMain(this);
            this.rconMainThread.startThread();
        }

        return true;
    }

    private void initWorld(ISaveFormat iSaveFormat, String string, long l, WorldType par5WorldType) {
        if (iSaveFormat.isOldMapFormat(string)) {
            logger.info("Converting map!");
            iSaveFormat.convertMapFormat(string, new ConvertProgressUpdater(this));
        }

        this.worldMngr = new WorldServer[3];
        this.field_40028_g = new long[this.worldMngr.length][100];
        int var6 = this.propertyManagerObj.getIntProperty("gamemode", 0);
        var6 = WorldSettings.validGameType(var6);
        logger.info("Default game type: " + var6);
        boolean var7 = this.propertyManagerObj.getBooleanProperty("generate-structures", true);
        WorldSettings var8 = new WorldSettings(l, var6, var7, false, par5WorldType);
        AnvilSaveHandler var9 = new AnvilSaveHandler(new File("."), string, true);

        for(int var10 = 0; var10 < this.worldMngr.length; ++var10) {
            byte var11 = 0;
            if (var10 == 1) {
                var11 = -1;
            }

            if (var10 == 2) {
                var11 = 1;
            }

            if (var10 == 0) {
                this.worldMngr[var10] = new WorldServer(this, var9, string, var11, var8);
            } else {
                this.worldMngr[var10] = new WorldServerMulti(this, var9, string, var11, var8, this.worldMngr[0]);
            }

            this.worldMngr[var10].addWorldAccess(new WorldManager(this, this.worldMngr[var10]));
            this.worldMngr[var10].difficultySetting = this.propertyManagerObj.getIntProperty("difficulty", 1);
            this.worldMngr[var10].setAllowedSpawnTypes(this.propertyManagerObj.getBooleanProperty("spawn-monsters", true), this.spawnPeacefulMobs);
            this.worldMngr[var10].getWorldInfo().setGameType(var6);
            this.configManager.setPlayerManager(this.worldMngr);
        }

        short var23 = 196;
        long var24 = System.currentTimeMillis();

        for(int var13 = 0; var13 < 1; ++var13) {
            logger.info("Preparing start region for level " + var13);
            WorldServer var14 = this.worldMngr[var13];
            ChunkCoordinates var15 = var14.getSpawnPoint();

            for(int var16 = -var23; var16 <= var23 && this.serverRunning; var16 += 16) {
                for(int var17 = -var23; var17 <= var23 && this.serverRunning; var17 += 16) {
                    long var18 = System.currentTimeMillis();
                    if (var18 < var24) {
                        var24 = var18;
                    }

                    if (var18 > var24 + 1000L) {
                        int var20 = (var23 * 2 + 1) * (var23 * 2 + 1);
                        int var21 = (var16 + var23) * (var23 * 2 + 1) + var17 + 1;
                        this.outputPercentRemaining("Preparing spawn area", var21 * 100 / var20);
                        var24 = var18;
                    }

                    var14.chunkProviderServer.loadChunk(var15.posX + var16 >> 4, var15.posZ + var17 >> 4);

                    while(var14.updatingLighting() && this.serverRunning) {
                    }
                }
            }
        }

        this.clearCurrentTask();
    }

    private void outputPercentRemaining(String string, int par2) {
        this.currentTask = string;
        this.percentDone = par2;
        logger.info(string + ": " + par2 + "%");
    }

    private void clearCurrentTask() {
        this.currentTask = null;
        this.percentDone = 0;
    }

    private void saveServerWorld() {
        logger.info("Saving chunks");

        for(int var1 = 0; var1 < this.worldMngr.length; ++var1) {
            WorldServer var2 = this.worldMngr[var1];
            var2.saveWorld(true, null);
            var2.func_30006_w();
        }
    }

    private void stopServer() {
        logger.info("Stopping server");
        if (this.configManager != null) {
            this.configManager.savePlayerStates();
        }

        for(int var1 = 0; var1 < this.worldMngr.length; ++var1) {
            WorldServer var2 = this.worldMngr[var1];
            if (var2 != null) {
                this.saveServerWorld();
            }
        }
    }

    public void initiateShutdown() {
        this.serverRunning = false;
    }

    // $QF: renamed from: run () void
    @Override
    public void run() {
        try {
            if (this.startServer()) {
                long var1 = System.currentTimeMillis();

                for(long var3 = 0L; this.serverRunning; Thread.sleep(1L)) {
                    long var5 = System.currentTimeMillis();
                    long var7 = var5 - var1;
                    if (var7 > 2000L) {
                        logger.warning("Can't keep up! Did the system time change, or is the server overloaded?");
                        var7 = 2000L;
                    }

                    if (var7 < 0L) {
                        logger.warning("Time ran backwards! Did the system time change?");
                        var7 = 0L;
                    }

                    var3 += var7;
                    var1 = var5;
                    if (this.worldMngr[0].isAllPlayersFullyAsleep()) {
                        this.doTick();
                        var3 = 0L;
                    } else {
                        while(var3 > 50L) {
                            var3 -= 50L;
                            this.doTick();
                        }
                    }
                }
            } else {
                while(this.serverRunning) {
                    this.commandLineParser();

                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException var57) {
                        var57.printStackTrace();
                    }
                }
            }
        } catch (Throwable var58) {
            var58.printStackTrace();
            logger.log(Level.SEVERE, "Unexpected exception", var58);

            while(this.serverRunning) {
                this.commandLineParser();

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException var56) {
                    var56.printStackTrace();
                }
            }
        } finally {
            try {
                this.stopServer();
                this.serverStopped = true;
            } catch (Throwable var54) {
                var54.printStackTrace();
            } finally {
                System.exit(0);
            }
        }
    }

    private void doTick() {
        long var1 = System.nanoTime();
        ArrayList var3 = new ArrayList();

        for(Object var5 : field_6037_b.keySet()) {
            int var6 = (int) field_6037_b.get(var5);
            if (var6 > 0) {
                field_6037_b.put(var5, var6 - 1);
            } else {
                var3.add(var5);
            }
        }

        for(int var9 = 0; var9 < var3.size(); ++var9) {
            field_6037_b.remove(var3.get(var9));
        }

        AxisAlignedBB.clearBoundingBoxPool();
        Vec3D.initialize();
        ++this.deathTime;

        for(int var10 = 0; var10 < this.worldMngr.length; ++var10) {
            long var13 = System.nanoTime();
            if (var10 == 0 || this.propertyManagerObj.getBooleanProperty("allow-nether", true)) {
                WorldServer var7 = this.worldMngr[var10];
                if (this.deathTime % 20 == 0) {
                    this.configManager.sendPacketToAllPlayersInDimension(new Packet4UpdateTime(var7.getWorldTime()), var7.worldProvider.worldType);
                }

                var7.tick();

                while(var7.updatingLighting()) {
                }

                var7.updateEntities();
            }

            this.field_40028_g[var10][this.deathTime % 100] = System.nanoTime() - var13;
        }

        this.networkServer.handleNetworkListenThread();
        this.configManager.onTick();

        for(int var11 = 0; var11 < this.entityTracker.length; ++var11) {
            this.entityTracker[var11].updateTrackedEntities();
        }

        for(int var12 = 0; var12 < this.playersOnline.size(); ++var12) {
            ((IUpdatePlayerListBox)this.playersOnline.get(var12)).update();
        }

        try {
            this.commandLineParser();
        } catch (Exception var8) {
            logger.log(Level.WARNING, "Unexpected exception while parsing console command", (Throwable)var8);
        }

        this.field_40027_f[this.deathTime % 100] = System.nanoTime() - var1;
        this.field_48080_u[this.deathTime % 100] = Packet.field_48099_n - this.field_48074_E;
        this.field_48074_E = Packet.field_48099_n;
        this.field_48079_v[this.deathTime % 100] = Packet.field_48100_o - this.field_48075_F;
        this.field_48075_F = Packet.field_48100_o;
        this.field_48078_w[this.deathTime % 100] = Packet.field_48101_l - this.field_48076_G;
        this.field_48076_G = Packet.field_48101_l;
        this.field_48082_x[this.deathTime % 100] = Packet.field_48102_m - this.field_48077_H;
        this.field_48077_H = Packet.field_48102_m;
    }

    public void addCommand(String string, ICommandListener par2ICommandListener) {
        this.commands.add(new ServerCommand(string, par2ICommandListener));
    }

    public void commandLineParser() {
        while(this.commands.size() > 0) {
            ServerCommand var1 = (ServerCommand)this.commands.remove(0);
            this.commandHandler.handleCommand(var1);
        }
    }

    public void addToOnlinePlayerList(IUpdatePlayerListBox par1IUpdatePlayerListBox) {
        this.playersOnline.add(par1IUpdatePlayerListBox);
    }

    public static void main(String[] strings) {
        StatList.func_27092_a();

        try {
            MinecraftServer var1 = new MinecraftServer();
            if (!GraphicsEnvironment.isHeadless() && (strings.length <= 0 || !strings[0].equals("nogui"))) {
                ServerGUI.initGui(var1);
            }

            new ThreadServerApplication("Server thread", var1).start();
        } catch (Exception var2) {
            logger.log(Level.SEVERE, "Failed to start the minecraft server", (Throwable)var2);
        }
    }

    public File getFile(String par1Str) {
        return new File(par1Str);
    }

    // $QF: renamed from: log (java.lang.String) void
    public void method_0(String par1Str) {
        logger.info(par1Str);
    }

    @Override
    public void logWarning(String par1Str) {
        logger.warning(par1Str);
    }

    @Override
    public String getUsername() {
        return "CONSOLE";
    }

    public WorldServer getWorldManager(int par1) {
        if (par1 == -1) {
            return this.worldMngr[1];
        } else {
            return par1 == 1 ? this.worldMngr[2] : this.worldMngr[0];
        }
    }

    public EntityTracker getEntityTracker(int par1) {
        if (par1 == -1) {
            return this.entityTracker[1];
        } else {
            return par1 == 1 ? this.entityTracker[2] : this.entityTracker[0];
        }
    }

    @Override
    public int getIntProperty(String string, int par2) {
        return this.propertyManagerObj.getIntProperty(string, par2);
    }

    @Override
    public String getStringProperty(String string, String par2Str) {
        return this.propertyManagerObj.getStringProperty(string, par2Str);
    }

    @Override
    public void setProperty(String string, Object par2Obj) {
        this.propertyManagerObj.setProperty(string, par2Obj);
    }

    @Override
    public void saveProperties() {
        this.propertyManagerObj.saveProperties();
    }

    @Override
    public String getSettingsFilename() {
        File var1 = this.propertyManagerObj.getPropertiesFile();
        return var1 != null ? var1.getAbsolutePath() : "No settings file";
    }

    @Override
    public String getHostname() {
        return this.hostname;
    }

    @Override
    public int getPort() {
        return this.serverPort;
    }

    @Override
    public String getMotd() {
        return this.motd;
    }

    @Override
    public String getVersionString() {
        return "1.2.5";
    }

    @Override
    public int playersOnline() {
        return this.configManager.playersOnline();
    }

    @Override
    public int getMaxPlayers() {
        return this.configManager.getMaxPlayers();
    }

    @Override
    public String[] getPlayerNamesAsList() {
        return this.configManager.getPlayerNamesAsList();
    }

    @Override
    public String getWorldName() {
        return this.propertyManagerObj.getStringProperty("level-name", "world");
    }

    @Override
    public String getPlugin() {
        return "";
    }

    @Override
    public void func_40010_o() {
    }

    @Override
    public String handleRConCommand(String par1Str) {
        RConConsoleSource.instance.resetLog();
        this.commandHandler.handleCommand(new ServerCommand(par1Str, RConConsoleSource.instance));
        return RConConsoleSource.instance.getLogContents();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public void logSevere(String par1Str) {
        logger.log(Level.SEVERE, par1Str);
    }

    @Override
    public void logIn(String par1Str) {
        if (this.isDebuggingEnabled()) {
            logger.log(Level.INFO, par1Str);
        }
    }

    @Override
    public void log(String message) {
        logIn(message);
    }

    public String[] getBannedIPsList() {
        return (String[]) this.configManager.getBannedIPsList().toArray(new String[0]);
    }

    public String[] getBannedPlayersList() {
        return (String[]) this.configManager.getBannedPlayersList().toArray(new String[0]);
    }

    public String func_52003_getServerModName() {
        return "vanilla";
    }
}
