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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpw.mods.fml.common.toposort.ModSorter;
import cpw.mods.fml.common.toposort.TopologicalSort;

/**
 * The loader class performs the actual loading of the mod code from disk.
 * 
 * <p>There are several {@link State}s to mod loading, triggered in two different stages from the FML handler code's hooks into the
 * minecraft code.</p>
 * 
 * <ol>
 * <li>LOADING. Scanning the filesystem for mod containers to load (zips, jars, directories), adding them to the {@link #modClassLoader}
 * Scanning, the loaded containers for mod classes to load and registering them appropriately.</li>
 * <li>PREINIT. The mod classes are configured, they are sorted into a load order, and instances of the mods are constructed.</li>
 * <li>INIT. The mod instances are initialized. For BaseMod mods, this involves calling the load method.</li>
 * <li>POSTINIT. The mod instances are post initialized. For BaseMod mods this involves calling the modsLoaded method.</li>
 * <li>UP. The Loader is complete</li>
 * <li>ERRORED. The loader encountered an error during the LOADING phase and dropped to this state instead. It will not complete
 * loading from this state, but it attempts to continue loading before abandoning and giving a fatal error.</li>
 * </ol>
 * 
 * Phase 1 code triggers the LOADING and PREINIT states. Phase 2 code triggers the INIT and POSTINIT states.
 * 
 * @author cpw
 * 
 */
public class Loader
{
    private static Pattern zipJar = Pattern.compile("([^\\s]+).(zip|jar)$");
    private static Pattern modClass = Pattern.compile("(.*/?)(mod\\_[^\\s]+).class$");

    /**
     * The state enum used to help track state progression for the loader
     * @author cpw
     *
     */
    private enum State
    {
        NOINIT, LOADING, PREINIT, INIT, POSTINIT, UP, ERRORED
    };

    /**
     * The singleton instance
     */
    private static Loader instance;
    /**
     * Our special logger for logging issues to. We copy various assets from the Minecraft logger to acheive a similar appearance.
     */
    public static Logger log = Logger.getLogger("ForgeModLoader");


    /**
     * Build information for tracking purposes.
     */
    private static String major = "@MAJOR@";
    private static String minor = "@MINOR@";
    private static String rev = "@REV@";
    private static String build = "@BUILD@";
    private static String mcversion = "@MCVERSION@";

    /**
     * The {@link State} of the loader
     */
    private State state;
    /**
     * The class loader we load the mods into.
     */
    private ModClassLoader modClassLoader;
    /**
     * The sorted list of mods.
     */
    private List<ModContainer> mods;
    /**
     * A named list of mods
     */
    private Map<String, ModContainer> namedMods;
    /**
     * The canonical configuration directory
     */
    private File canonicalConfigDir;
    /**
     * The canonical minecraft directory
     */
    private File canonicalMinecraftDir;

    
    public static Loader instance()
    {
        if (instance == null)
        {
            instance = new Loader();
        }

        return instance;
    }

    private Loader()
    {
        Loader.log.setParent(FMLCommonHandler.instance().getMinecraftLogger());
        Loader.log.setLevel(Level.ALL);
        FileHandler fileHandler;

        try
        {
            fileHandler = new FileHandler("ForgeModLoader-%g.log", 0, 3);
            // We're stealing minecraft's log formatter
            fileHandler.setFormatter(FMLCommonHandler.instance().getMinecraftLogger().getHandlers()[0].getFormatter());
            fileHandler.setLevel(Level.ALL);
            Loader.log.addHandler(fileHandler);
        }
        catch (Exception e)
        {
            // Whatever - give up
        }

        log.info(String.format("Forge Mod Loader version %s.%s.%s.%s for Minecraft %s loading", major, minor, rev, build, mcversion));
    }

    /**
     * Sort the mods into a sorted list, using dependency information from the containers. The sorting is performed
     * using a {@link TopologicalSort} based on the pre- and post- dependency information provided by the mods.
     */
    private void sortModList()
    {
        log.fine("Verifying mod dependencies are satisfied");

        for (ModContainer mod : mods)
        {
            if (!namedMods.keySet().containsAll(mod.getDependencies()))
            {
                log.severe(String.format("The mod %s requires mods %s to be available, one or more are not", mod.getName(), mod.getDependencies()));
                LoaderException le = new LoaderException();
                log.throwing("Loader", "sortModList", le);
                throw new LoaderException();
            }
        }

        log.fine("All dependencies are satisfied");
        ModSorter sorter = new ModSorter(mods, namedMods);

        try
        {
            log.fine("Sorting mods into an ordered list");
            mods = sorter.sort();
            log.fine(String.format("Sorted mod list %s", mods));
        }
        catch (IllegalArgumentException iae)
        {
            log.severe("A dependency cycle was detected in the input mod set so they cannot be loaded in order");
            log.throwing("Loader", "sortModList", iae);
            throw new LoaderException(iae);
        }
    }

    /**
     * The first mod initialization stage, performed immediately after the jar files and mod classes are loaded,
     * {@link State#PREINIT}. The mods are configured from their configuration data and instantiated (for BaseMod mods).
     */
    private void preModInit()
    {
        state = State.PREINIT;
        log.fine("Beginning mod pre-initialization");

        for (ModContainer mod : mods)
        {
            if (mod.wantsPreInit())
            {
                log.finer(String.format("Pre-initializing %s", mod.getSource()));
                mod.preInit();
                namedMods.put(mod.getName(), mod);
            }
        }

        log.fine("Mod pre-initialization complete");
    }

    /**
     * The main mod initialization stage, performed on the sorted mod list.
     */
    private void modInit()
    {
        state = State.INIT;
        log.fine("Beginning mod initialization");

        for (ModContainer mod : mods)
        {
            log.finer(String.format("Initializing %s", mod.getName()));
            mod.init();
        }

        log.fine("Mod initialization complete");
    }

    private void postModInit()
    {
        state = State.POSTINIT;
        log.fine("Beginning mod post-initialization");

        for (ModContainer mod : mods)
        {
            if (mod.wantsPostInit())
            {
                log.finer(String.format("Post-initializing %s", mod.getName()));
                mod.postInit();
            }
        }

        log.fine("Mod post-initialization complete");
    }

    /**
     * The primary loading code
     * 
     * This is visited during first initialization by Minecraft to scan and load the mods 
     * from all sources
     * 1. The minecraft jar itself (for loading of in jar mods- I would like to remove this if possible but forge depends on it at present)
     * 2. The mods directory with expanded subdirs, searching for mods named mod_*.class
     * 3. The mods directory for zip and jar files, searching for mod classes named mod_*.class again
     * 
     * The found resources are first loaded into the {@link #modClassLoader} (always) then scanned for class resources matching the specification above.
     * 
     * If they provide the {@link Mod} annotation, they will be loaded as "FML mods", which currently is effectively a NO-OP.
     * If they are determined to be {@link net.minecraft.src.BaseMod} subclasses they are loaded as such.
     * 
     * Finally, if they are successfully loaded as classes, they are then added to the available mod list.
     */
    private void load()
    {
        File minecraftDir = FMLCommonHandler.instance().getMinecraftRootDirectory();
        File modsDir = new File(minecraftDir, "mods");
        File configDir = new File(minecraftDir, "config");
        String canonicalModsPath;
        String canonicalConfigPath;

        try
        {
            canonicalMinecraftDir = minecraftDir.getCanonicalFile();
            canonicalModsPath = modsDir.getCanonicalPath();
            canonicalConfigPath = configDir.getCanonicalPath();
            canonicalConfigDir = configDir.getCanonicalFile();
        }
        catch (IOException ioe)
        {
            log.severe(String.format("Failed to resolve mods directory mods %s", modsDir.getAbsolutePath()));
            log.throwing("fml.server.Loader", "initialize", ioe);
            throw new LoaderException(ioe);
        }

        if (!modsDir.exists())
        {
            log.fine(String.format("No mod directory found, creating one: %s", canonicalModsPath));

            try
            {
                modsDir.mkdir();
            }
            catch (Exception e)
            {
                log.throwing("fml.server.Loader", "initialize", e);
                throw new LoaderException(e);
            }
        }

        if (!configDir.exists())
        {
            log.fine(String.format("No config directory found, creating one: %s", canonicalConfigPath));

            try
            {
                configDir.mkdir();
            }
            catch (Exception e)
            {
                log.throwing("fml.server.Loader", "initialize", e);
                throw new LoaderException(e);
            }
        }

        if (!modsDir.isDirectory())
        {
            log.severe(String.format("Attempting to load mods from %s, which is not a directory", canonicalModsPath));
            LoaderException loaderException = new LoaderException();
            log.throwing("fml.server.Loader", "initialize", loaderException);
            throw loaderException;
        }

        if (!configDir.isDirectory())
        {
            log.severe(String.format("Attempting to load configuration from %s, which is not a directory", canonicalConfigPath));
            LoaderException loaderException = new LoaderException();
            log.throwing("fml.server.Loader", "initialize", loaderException);
            throw loaderException;
        }

        state = State.LOADING;
        modClassLoader = new ModClassLoader();
        log.fine("Attempting to load mods contained in the minecraft jar file");
        File minecraftSource=modClassLoader.getParentSource();
        if (minecraftSource.isFile()) {
            log.fine(String.format("Minecraft is a file at %s, loading",minecraftSource.getName()));
            attemptFileLoad(minecraftSource);
        } else if (minecraftSource.isDirectory()) {
            log.fine(String.format("Minecraft is a directory at %s, loading",minecraftSource.getName()));
            attemptDirLoad(minecraftSource);
        } else {
            log.severe(String.format("Unable to locate minecraft data at %s\n",minecraftSource.getName()));
            throw new LoaderException();
        }
        log.fine("Minecraft jar mods loaded successfully");
        
        log.info(String.format("Loading mods from %s", canonicalModsPath));
        File[] modList = modsDir.listFiles();
        // Sort the files into alphabetical order first
        Arrays.sort(modList);

        for (File modFile : modList)
        {
            if (modFile.isDirectory())
            {
                log.fine(String.format("Found a directory %s, attempting to load it", modFile.getName()));
                boolean modFound = attemptDirLoad(modFile);

                if (modFound)
                {
                    log.fine(String.format("Directory %s loaded successfully", modFile.getName()));
                }
                else
                {
                    log.info(String.format("Directory %s contained no mods", modFile.getName()));
                }
            }
            else
            {
                Matcher matcher = zipJar.matcher(modFile.getName());

                if (matcher.matches())
                {
                    log.fine(String.format("Found a zip or jar file %s, attempting to load it", matcher.group(0)));
                    boolean modFound = attemptFileLoad(modFile);

                    if (modFound)
                    {
                        log.fine(String.format("File %s loaded successfully", matcher.group(0)));
                    }
                    else
                    {
                        log.info(String.format("File %s contained no mods", matcher.group(0)));
                    }
                }
            }
        }

        if (state == State.ERRORED)
        {
            log.severe("A problem has occured during mod loading, giving up now");
            throw new RuntimeException("Giving up please");
        }

        log.info(String.format("Forge Mod Loader has loaded %d mods", mods.size()));
    }

    private boolean attemptDirLoad(File modDir)
    {
        extendClassLoader(modDir);
        boolean foundAModClass = false;
        File[] content = modDir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return modClass.matcher(name).find();
            }
        });

        for (File modClassFile : content)
        {
            String clazzName = modClass.matcher(modClassFile.getName()).group(2);
            log.fine(String.format("Found a mod class %s in directory %s, attempting to load it", clazzName, modDir.getName()));
            loadModClass(modDir, modClassFile.getName(), clazzName);
            log.fine(String.format("Successfully loaded mod class %s", modClassFile.getName()));
            foundAModClass = true;
        }

        return foundAModClass;
    }

    private void loadModClass(File classSource, String classFileName, String clazzName)
    {
        try
        {
            Class<?> clazz = Class.forName(clazzName, false, modClassLoader);

            if (clazz.isAnnotationPresent(Mod.class))
            {
                // an FML mod
                mods.add(FMLModContainer.buildFor(clazz));
            }
            else if (FMLCommonHandler.instance().isModLoaderMod(clazz))
            {
                log.fine(String.format("ModLoader BaseMod class %s found, loading", clazzName));
                ModContainer mc = FMLCommonHandler.instance().loadBaseModMod(clazz, classSource.getCanonicalPath());
                mods.add(mc);
                log.fine(String.format("ModLoader BaseMod class %s loaded", clazzName));
            }
            else
            {
                // Unrecognized
            }
        }
        catch (Exception e)
        {
            log.warning(String.format("Failed to load mod class %s in %s", classFileName, classSource.getAbsoluteFile()));
            log.throwing("fml.server.Loader", "attemptLoad", e);
            state = State.ERRORED;
        }
    }

    private void extendClassLoader(File file)
    {
        try
        {
            modClassLoader.addFile(file);
        }
        catch (MalformedURLException e)
        {
            throw new LoaderException(e);
        }
    }

    private boolean attemptFileLoad(File modFile)
    {
        extendClassLoader(modFile);
        boolean foundAModClass = false;

        try
        {
            ZipFile jar = new ZipFile(modFile);

            for (ZipEntry ze : Collections.list(jar.entries()))
            {
                Matcher match = modClass.matcher(ze.getName());

                if (match.matches())
                {
                    String pkg = match.group(1).replace('/', '.');
                    String clazzName = pkg + match.group(2);
                    log.fine(String.format("Found a mod class %s in file %s, attempting to load it", clazzName, modFile.getName()));
                    loadModClass(modFile, ze.getName(), clazzName);
                    log.fine(String.format("Mod class %s loaded successfully", clazzName, modFile.getName()));
                    foundAModClass = true;
                }
            }
        }
        catch (Exception e)
        {
            log.warning(String.format("Zip file %s failed to read properly", modFile.getName()));
            log.throwing("fml.server.Loader", "attemptFileLoad", e);
            state = State.ERRORED;
        }

        return foundAModClass;
    }

    public static List<ModContainer> getModList()
    {
        return instance().mods;
    }

    /**
     * Called from the hook to start mod loading. We trigger the {@link #load()} and {@link #preModInit()} phases here.
     * Finally, the mod list is frozen completely and is consider immutable from then on.
     */
    public void loadMods()
    {
        state = State.NOINIT;
        mods = new ArrayList<ModContainer>();
        namedMods = new HashMap<String, ModContainer>();
        load();
        preModInit();
        sortModList();
        // Make mod list immutable
        mods = Collections.unmodifiableList(mods);
    }

    /**
     * Complete the initialization of the mods {@link #initializeMods()} and {@link #postModInit()} and mark ourselves up and ready to run.
     */
    public void initializeMods()
    {
        modInit();
        postModInit();
        state = State.UP;
        log.info(String.format("Forge Mod Loader load complete, %d mods loaded", mods.size()));
    }

    /**
     * Query if we know of a mod named modname
     * 
     * @param modname
     * @return
     */
    public static boolean isModLoaded(String modname)
    {
        return instance().namedMods.containsKey(modname);
    }

    /**
     * @return
     */
    public File getConfigDir()
    {
        return canonicalConfigDir;
    }
}