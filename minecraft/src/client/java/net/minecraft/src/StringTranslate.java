package net.minecraft.src;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;

public class StringTranslate {
    private static final StringTranslate instance = new StringTranslate();
    private Properties translateTable = new Properties();
    private final TreeMap<String, String> languageList= new TreeMap<>();
    @Getter
    private String currentLanguage;
    private boolean isUnicode;

    private StringTranslate() {
        this.loadLanguageList();
        this.setLanguage("en_US");
    }

    public static StringTranslate getInstance() {
        return instance;
    }

    private void loadLanguageList() {
        try (InputStream stream = StringTranslate.class.getResourceAsStream("/lang/languages.txt")) {
            if (stream == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

            for(String var3 = reader.readLine(); var3 != null; var3 = reader.readLine()) {
                String[] var4 = var3.split("=");
                if (var4.length == 2) {
                    languageList.put(var4[0], var4[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TreeMap<String, String> getLanguageList() {
        return this.languageList;
    }

    private void loadLanguage(Properties properties, String par2Str) throws IOException {
        try (InputStream stream = StringTranslate.class.getResourceAsStream("/lang/" + par2Str + ".lang")) {
            if (stream == null) return;
            BufferedReader var3 = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

            for (String var4 = var3.readLine(); var4 != null; var4 = var3.readLine()) {
                var4 = var4.trim();
                if (!var4.startsWith("#")) {
                    String[] var5 = var4.split("=");
                    if (var5.length == 2) {
                        properties.setProperty(var5[0], var5[1]);
                    }
                }
            }
        }
    }

    public void setLanguage(String par1Str) {
        if (!par1Str.equals(this.currentLanguage)) {
            Properties var2 = new Properties();

            try {
                this.loadLanguage(var2, "en_US");
            } catch (IOException ignored) {
            }

            this.isUnicode = false;
            if (!"en_US".equals(par1Str)) {
                try {
                    this.loadLanguage(var2, par1Str);
                    Enumeration<?> var3 = var2.propertyNames();

                    while(var3.hasMoreElements() && !this.isUnicode) {
                        Object var4 = var3.nextElement();
                        Object var5 = var2.get(var4);
                        if (var5 != null) {
                            String var6 = var5.toString();

                            for(int var7 = 0; var7 < var6.length(); ++var7) {
                                if (var6.charAt(var7) >= 256) {
                                    this.isUnicode = true;
                                    break;
                                }
                            }
                        }
                    }
                } catch (IOException var9) {
                    var9.printStackTrace();
                    return;
                }
            }

            this.currentLanguage = par1Str;
            this.translateTable = var2;
        }
    }

    public boolean isUnicode() {
        return this.isUnicode;
    }

    public String translateKey(String par1Str) {
        return this.translateTable.getProperty(par1Str, par1Str);
    }

    public String translateKeyFormat(String string, Object... par2ArrayOfObj) {
        String var3 = this.translateTable.getProperty(string, string);
        return String.format(var3, par2ArrayOfObj);
    }

    public String translateNamedKey(String par1Str) {
        return this.translateTable.getProperty(par1Str + ".name", "");
    }

    public static boolean isBidrectional(String par0Str) {
        return "ar_SA".equals(par0Str) || "he_IL".equals(par0Str);
    }

    // Server
    public Properties getCurrentLanguageTable() {
        return this.getTranslationTable();
    }

    // Client
    public Properties getTranslationTable() {
        return this.translateTable;
    }
}
