package xyz.kaijiieow.statusup.core;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

public class RequirementChecker {

    private final boolean papiEnabled;
    private final ScriptEngine scriptEngine;

    public RequirementChecker(boolean papiEnabled) {
        this.papiEnabled = papiEnabled;
        // JavaScript engine (Nashorn) มีอยู่ใน Java 8-14, GraalJS ใน 15+
        this.scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        if (this.scriptEngine == null) {
            Bukkit.getLogger().severe("[StatusUp] JavaScript ScriptEngine not found! Requirements will fail.");
        }
    }

    public boolean check(Player player, List<String> requirements) {
        if (!papiEnabled) {
            // ถ้า PAPI ไม่มี, ให้ผ่านไปเลย (หรือจะให้ fail ก็ได้ แต่ส่วนใหญ่เลือกผ่าน)
            return true;
        }
        
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        
        if (this.scriptEngine == null) {
            return false; // ถ้า Engine พัง, ไม่ให้ผ่าน
        }

        for (String req : requirements) {
            if (!evaluate(player, req)) {
                return false;
            }
        }
        
        return true;
    }

    private boolean evaluate(Player player, String expression) {
        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, expression);
            Object result = scriptEngine.eval(parsed);

            if (result instanceof Boolean) {
                return (Boolean) result;
            } else {
                Bukkit.getLogger().warning("[StatusUp] Expression did not return a boolean: " + parsed);
                return false;
            }
        } catch (ScriptException e) {
            Bukkit.getLogger().warning("[StatusUp] Error evaluating expression: " + expression);
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            Bukkit.getLogger().warning("[StatusUp] NullPointerException during evaluation. Is PlaceholderAPI hooked?");
            return false;
        }
    }
}