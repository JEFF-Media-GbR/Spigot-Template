package de.jeff_media.PluginName;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class ConfigUpdater {

    private static String[] nodesPrefixNeedingDoubleQuotes = {"message-"};
    private static String[] nodesPrefixNeedingSingleQuotes = {"test-"};
    private static String[] nodesStringLists = {"disabled-worlds"};
    private static String[] nodesIgnored = {"config-version", "plugin-version"};


    private static final boolean debug = true;

    private static final void debug( Logger logger,String message) {
        logger.warning(message);
    }

    public static void updateConfig(Main main) {
        Logger logger = main.getLogger();
        debug(logger,"Newest config version  = "+getNewConfigVersion(main));
        debug(logger,"Current config version = "+main.getConfig().getLong(Config.CONFIG_VERSION));
        if(main.getConfig().getLong(Config.CONFIG_VERSION) >= getNewConfigVersion(main)) {
            debug(logger,"The config currently used has an equal or newer version than the one shipped with this release.");
            return;
        }

        logger.info("===========================================");
        logger.info("You are using an outdated config file.");
        logger.info("Your config file will now be updated to the");
        logger.info("newest version. You changes will be kept.");
        logger.info("===========================================");

        backupCurrentConfig(main);
        main.saveDefaultConfig();

        Set<String> oldConfigNodes = main.getConfig().getKeys(false);
        ArrayList<String> newConfig = new ArrayList<>();

        for(String line : getNewConfigAsArrayList(main)) {

            String newLine = line;

            if(lineContaintsIgnoredNode(line)) {
                debug(logger,"Not updating this line: " + line);
            }
            else if(lineIsStringList(line)) {
                newLine = null;
                newConfig.add(line);
                String node = newLine.split(":")[0];
                for(String entry : main.getConfig().getStringList(node)) {
                    newConfig.add("- " + entry);
                }
            }
            else {
                for(String node : oldConfigNodes) {
                    if (line.startsWith(node+":")) {
                        String quotes = getQuotes(node);
                        newLine = node + ": " + quotes + main.getConfig().get(node).toString() + quotes;
                    }
                }
            }

            if(newLine != null) {
                newConfig.add(newLine);
            }
        }

        saveArrayListToConfig(main, newConfig);
    }

    private static String getQuotes(String line) {
        for(String test : nodesPrefixNeedingDoubleQuotes) {
            if (line.equals(test)) {
                return "\"";
            }
        }
        for(String test : nodesPrefixNeedingSingleQuotes) {
            if(line.equals(test)) {
                return "'";
            }
        }
        return "";
    }

    private static boolean lineIsStringList(String line) {
        for(String test : nodesStringLists) {
            if(line.startsWith(test+":")) {
                return true;
            }
        }
        return false;
    }

    private static boolean lineContaintsIgnoredNode(String line) {
        for(String test : nodesIgnored) {
            if(line.startsWith(test+":")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> getNewConfigAsArrayList(Main main) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(getFilePath(main,"config.yml")), StandardCharsets.UTF_8);
            return lines;
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    private static void saveArrayListToConfig(Main main, List<String> lines) {
        try {
            FileWriter writer = new FileWriter(getFilePath(main,"config.yml"));
            for(String line : lines) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static String getFilePath(Main main, String fileName) {
        return main.getDataFolder() + File.separator + fileName;
    }

    private static long getNewConfigVersion(Main main) {
        InputStream in = main.getClass().getResourceAsStream("/config-version.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            return Long.parseLong(reader.readLine());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return 0;
        }

    }

    private static void updateList(ArrayList<String> newConfig, String node, ArrayList<String> oldValues) {
        newConfig.add(node+":");
        for(String oldValue : oldValues) {
            newConfig.add("- " + oldValue);
        }
     }

    private static void backupCurrentConfig(Main main) {
        File oldFile = new File(getFilePath(main,"config.yml"));
        File newFile = new File(getFilePath(main,"config-backup-"+main.getConfig().getString(Config.CONFIG_PLUGIN_VERSION)+".yml"));
        if(newFile.exists()) newFile.delete();
        oldFile.getAbsoluteFile().renameTo(newFile.getAbsoluteFile());
    }
}