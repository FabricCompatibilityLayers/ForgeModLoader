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
package cpw.mods.fml.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cpw.mods.fml.common.modloader.ModLoaderModContainer;
import net.minecraft.src.BaseMod;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.toposort.ModSorter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class LoaderTests {

	private ModContainer mc1;
	private ModContainer mc2;
	private ModContainer mc3;
	private ModContainer mc4;
	private ModContainer mc5;
	private ModContainer mc6;
	private ModContainer mc7;
	private ModContainer mc8;

	@BeforeAll
	public void setUp() throws Exception {
		mc1 = new ModLoaderModContainer(Mod1.class, "Test1");
		mc1.preInit();
		mc2 = new ModLoaderModContainer(Mod2.class, "Test2");
		mc2.preInit();
		mc3 = new ModLoaderModContainer(Mod3.class, "Test3");
		mc3.preInit();
		mc4 = new ModLoaderModContainer(Mod4.class, "Test4");
		mc4.preInit();
		mc5 = new ModLoaderModContainer(Mod5.class, "Test5");
		mc5.preInit();
		mc6 = new ModLoaderModContainer(Mod5.class, "Test6");
		mc6.preInit();
		mc7 = new ModLoaderModContainer(Mod5.class, "Test7");
		mc7.preInit();
		mc8 = new ModLoaderModContainer(Mod5.class, "Test8");
		mc8.preInit();
	}

	@AfterAll
	public void tearDown() throws Exception {
	}

	@Test
	public void testModSorting() {
		HashMap<String, ModContainer> modList = new HashMap<String, ModContainer>();
		modList.put("Mod1", mc1);
		modList.put("Mod2", mc2);
		modList.put("Mod3", mc3);
		modList.put("Mod4", mc4);
		modList.put("Mod5", mc5);
		modList.put("Mod6", mc6);
		modList.put("Mod7", mc7);
		modList.put("Mod8", mc8);
		ModSorter ms = new ModSorter(Collections.list(Collections.enumeration(modList.values())), modList);
		List<ModContainer> mods = ms.sort();
		assertEquals(8, mods.size(), "Eight mods");
		System.out.printf("%s\n", mods);
	}

	@Test
	public void testModPrioritiesParsing() {
		// Mod 1
		assertTrue(mc1.getDependencies().isEmpty(), "Empty hard dependencies for Mod1");
		assertTrue(mc1.getPreDepends().contains("*"), "Mod1 predepends on *");

		// Mod 2
		assertTrue(mc2.getDependencies().isEmpty(), "Empty hard dependencies for Mod2");
		assertTrue(mc1.getPostDepends().contains("*"), "Mod2 postdepends on *");

		// Mod 3
		assertEquals(2, mc1.getDependencies().size(), "Two hard dependencies for Mod3");
		assertTrue(mc1.getDependencies().contains("Mod4"), "Hard dependencies for Mod3 contains Mod4");
		assertTrue(mc1.getDependencies().contains("Mod2"), "Hard dependencies for Mod3 contains Mod2");
		assertEquals(1, mc1.getPreDepends().size(), "One pre depends for Mod3");
		assertEquals(1, mc1.getPostDepends().size(), "One post depends for Mod3");

		// Mod 4
		assertTrue(mc1.getDependencies().isEmpty(), "Empty hard dependencies for Mod1");
		assertTrue(mc1.getPreDepends().contains("*"), "Mod1 predepends on *");

		// Mod 5
		assertTrue(mc1.getDependencies().isEmpty(), "Empty hard dependencies for Mod1");
		assertTrue(mc1.getPreDepends().contains("*"), "Mod1 predepends on *");
	}

	public static class Mod1 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "before:*";
		}
	}

	public static class Mod2 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "after:*";
		}
	}

	public static class Mod3 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "required-before:Mod4;required-after:Mod5";
		}
	}

	public static class Mod4 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "required-before:Mod2;after:Mod3;required-after:Mod1";
		}
	}

	public static class Mod5 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "";
		}
	}

	public static class Mod6 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "";
		}
	}

	public static class Mod7 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "";
		}
	}

	public static class Mod8 extends BaseMod {
		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public void load() {
		}

		@Override
		public String getPriorities() {
			return "before:Mod7";
		}
	}
}
