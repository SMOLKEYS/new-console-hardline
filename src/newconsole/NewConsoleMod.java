package newconsole;

import arc.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import newconsole.game.*;
import newconsole.js.*;
import newconsole.ui.*;
import newconsole.ui.dialogs.*;

public class NewConsoleMod extends Mod {

    public NewConsoleMod() {
        Vars.loadLogger();

        Events.run(EventType.ClientLoadEvent.class, () -> {
            NCJSLink.importPackage(
                    "newconsole", "newconsole.game", "newconsole.io",
                    "newconsole.js", "newconsole.ui"
            );

            CStyles.loadSync();
            initConsole();

            Events.fire(new NewConsoleInitEvent());

            ConsoleVars.consoles.each(cons -> {
                cons.scripts.load();
                cons.autorun.load();
            });
        });
    }

    public static void executeStartup() {
        try {
            var file = Vars.tree.get(ConsoleVars.startup);
            if (!file.exists()) {
                Log.warn("Startup script not found.");
                return;
            }

            Log.info("Executing startup script...");
            Time.mark();
            Vars.mods.getScripts().runConsole(file.readString());
            Log.info("Startup script executed in [blue]" + Time.elapsed() + "[] ms.");
        } catch (Throwable e) {
            Log.err("Failed to execute startup script!", e);
        }
    }

    public void initConsole() {
        ConsoleVars.group = new WidgetGroup();
        ConsoleVars.group.setFillParent(true);
        ConsoleVars.group.touchable = Touchable.childrenOnly;
        ConsoleVars.group.visible(() -> ConsoleVars.consoleEnabled);
        Core.scene.add(ConsoleVars.group);

        Seq<CompiledScript> compiled = new Seq<>();
        ConsoleVars.consoles.add(new Console(new JsCodeArea("", CStyles.monoArea), "JS", code -> {
            if(Core.settings.getBool("newconsole.reuse-compiled-script", false)) {
                // don't compile empty scripts
                if(code.isBlank()) return "<no code evaluated>";

                CompiledScript scr = compiled.find(c -> c.isMatching(code));
                if(scr == null){
                    scr = CompiledScript.with(code);
                    compiled.add(scr);
                }
                return scr.execWithResult();
            } else {
                return Vars.mods.getScripts().runConsole(code);
            }
        }, (script, variable, eventObj) -> {
            Vars.mods.getScripts().scope.put(variable, Vars.mods.getScripts().scope, eventObj);

            String finalScript = script.replaceAll("_autorun_event", variable);
            String res = Vars.mods.getScripts().runConsole(finalScript);

            Vars.mods.getScripts().scope.delete(variable);

            return res;
        }));

        ConsoleVars.saves = new SavesDialog();
        ConsoleVars.copypaste = new CopypasteDialog();
        ConsoleVars.fileBrowser = new FileBrowser();
        ConsoleVars.autorun = new AutorunDialog();

        ConsoleVars.floatingWidget = new FloatingWidget();

        ImageButton b = ConsoleVars.floatingWidget.button(Icon.terminal, Styles.defaulti, () ->
                ConsoleVars.getCurrentConsole().show()
        ).uniformX().uniformY().fill().get();

        ConsoleVars.floatingWidget.row();

        ConsoleVars.floatingWidget.button(Icon.left, Styles.defaulti, () -> {
            if(ConsoleVars.selectConsole > 0){
                ConsoleVars.selectConsole--;
                b.replaceImage(new Image(ConsoleVars.getCurrentConsole().buttonIcon));
            }
        }).uniformX().uniformY().fill().visible(() -> ConsoleVars.consoles.size > 1);

        ConsoleVars.floatingWidget.button(Icon.right, Styles.defaulti, () -> {
            if(ConsoleVars.selectConsole == ConsoleVars.consoles.size - 1) return;

            ConsoleVars.selectConsole++;
            b.replaceImage(new Image(ConsoleVars.getCurrentConsole().buttonIcon));
        }).uniformX().uniformY().fill().visible(() -> ConsoleVars.consoles.size > 1);

        ConsoleVars.group.addChild(ConsoleVars.floatingWidget);

        Time.run(10, () -> {
            // try to restore the position of the button
            var oldPosition = ConsoleSettings.getLastButtonPosition();

            ConsoleVars.floatingWidget.setPosition(
                    oldPosition.x != -1 ? oldPosition.x : ConsoleVars.group.getWidth() / 2f,
                    oldPosition.y != -1 ? oldPosition.y : ConsoleVars.group.getHeight() / 1.5f
            );
        });

        var lastSavedPosition = new Vec2(-1, -1);
        Timer.schedule(() -> {
            // Save the position of the floating button, if necessary
            var newPosition = Tmp.v1.set(
                    ConsoleVars.floatingWidget.x,
                    ConsoleVars.floatingWidget.y
            );
            if (newPosition.equals(lastSavedPosition)) return;

            lastSavedPosition.set(newPosition);
            ConsoleSettings.setLastButtonPosition(newPosition);
        }, 2f, 2f);

        ConsoleSettings.init();
        executeStartup();
    }

    public static class NewConsoleInitEvent{}
}
