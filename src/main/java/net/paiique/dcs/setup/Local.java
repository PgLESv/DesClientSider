package net.paiique.dcs.setup;

import net.paiique.dcs.Main;
import net.paiique.dcs.util.TextFileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Local {
    private static final Logger LOGGER = Logger.getLogger(Local.class.getName());

    private List<String> listFilesUsingFilesList(Path modsPath) {

        File[] fileList = modsPath.toFile().listFiles();
        if (fileList == null) return null;

        List<String> modList = new ArrayList<>();

        for (File file : fileList) {
            modList.add(file.getName());
        }
        return modList;
    }

    public boolean execute(boolean skipVerification) throws URISyntaxException {
        Path path;
        Scanner reader = new Scanner(System.in);

        if (!skipVerification){
            System.out.print("Path to mods folder (Example: /home/paique/server/mods or C:\\Users\\paique\\server\\mods): ");
            path = Path.of(reader.next());
        } else {
            path = Path.of(new File(Local.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "/mods/");
        }

        System.out.println(path);
        if (Files.exists(path) && Files.isDirectory(path)) {
            List<String> mods = listFilesUsingFilesList(path);

            if (mods == null) {
                LOGGER.log(Level.SEVERE, "Error! The mod array is null!");
                System.out.println("Restarting...");
                return false;
            }

            List<String> keywords = new TextFileUtils().read(new Keywords().getKeywordFile());

            List<String> clientSideMods = new ArrayList<>();

            keywords.forEach(keyword -> mods.forEach(mod -> {
                if (mod.toLowerCase().contains(keyword)) {
                    clientSideMods.add(mod);
                }
            }));

            if (!clientSideMods.isEmpty()) {
                System.out.println(keywords.size() + " keywords loaded.");
                System.out.println(mods.size() + " mods detected.");


                System.out.println(clientSideMods.size() + " client-side mods detected!");

                String remove = "";

                if (!skipVerification) {
                    System.out.println(clientSideMods);
                    System.out.print("\nDisable mods? (y/n): ");
                    remove = reader.next();
                }

                List<String> notRemoved = new ArrayList<>();
                if (remove.equalsIgnoreCase("y") || skipVerification) {
                    remove = "";
                    try {
                        Path clientFolder = Path.of(path + "/client");
                        if (!Files.exists(clientFolder)) Files.createDirectory(clientFolder);
                        for (String mod : clientSideMods) {
                            System.out.println("Moving to client folder: " + mod);
                            Path target = Path.of(clientFolder + "/" + mod);
                            Path source = Path.of(path + "/" + mod);
                            if (Files.exists(target)) {
                                if (!skipVerification) {
                                    System.out.print("File " + mod + " already exists in the client folder, delete it? (y/n/a) (a for all): ");
                                    remove = reader.next().toLowerCase();
                                }
                                if (remove.equals("a") || remove.equals("all")) skipVerification = true;
                                if (remove.equals("y") || remove.equals("yes") || skipVerification) {
                                    Files.delete(source);
                                    System.out.println("File " + mod + " deleted.");
                                } else {
                                    System.out.println("File not removed!!!");
                                    notRemoved.add(mod);
                                }
                                continue;
                            }
                            Files.move(source, target);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (notRemoved.isEmpty()) {
                        System.out.println("All detected client-side mods are moved to a new folder named client, or deleted if prompted.");
                    } else {
                        System.out.println(notRemoved.size() + " file(s) not removed:");
                        notRemoved.forEach(System.out::println);
                    }

                }
            } else {
                LOGGER.log(Level.SEVERE, "The directory is empty, or any client-side mod was detected.");
            }
            return true;

        } else {
            LOGGER.log(Level.SEVERE, "The path " + path + " does not exist, or it's not a directory.");
            return false;
        }
    }

}
