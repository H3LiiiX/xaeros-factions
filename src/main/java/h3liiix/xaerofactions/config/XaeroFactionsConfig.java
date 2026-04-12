package h3liiix.xaerofactions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class XaeroFactionsConfig {
    private static final File file = FabricLoader.getInstance().getConfigDir().resolve("xaerofactions.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static XaeroFactionsConfig INSTANCE;

    public boolean hideNeutralClaims = false;
    public boolean hideEnemyClaims = false;
    public boolean hideAllyClaims = false;

    public boolean trackSameFaction = true;
    public boolean trackAlly = true;
    public boolean trackEnemy = false;
    public boolean trackNeutral = false;

    public static void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                INSTANCE = new XaeroFactionsConfig();
                FileWriter writer = new FileWriter(file);
                GSON.toJson(INSTANCE, writer);
                writer.close();
            } else {
                INSTANCE = GSON.fromJson(new FileReader(file), XaeroFactionsConfig.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new XaeroFactionsConfig();
        }
    }
}