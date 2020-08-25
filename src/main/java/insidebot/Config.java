package insidebot;

import arc.Files;
import arc.files.Fi;
import arc.util.Log;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

public class Config {
    private JsonObject object;

    public Config() {
        try{
            Fi config = new Fi("config.json", Files.FileType.classpath);
            object = JsonValue.readJSON(config.readString()).asObject();
        } catch(Exception e) {
            Log.err(e);
        }
    }

    public String get(String key){
        try {
            return object.get(key).asString();
        }catch (Exception e){
            return "";
        }
    }
}
