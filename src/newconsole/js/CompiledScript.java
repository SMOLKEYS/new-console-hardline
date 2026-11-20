package newconsole.js;

import arc.func.*;
import arc.util.*;
import mindustry.*;
import rhino.*;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/** A compiled RhinoJS script snippet. Probably preferable over {@link mindustry.mod.Scripts#runConsole(String)} in some cases. */
public class CompiledScript implements Runnable{
    public final String script;
    public final Script compiledScript;
    public Prov<Context> contextProv = () -> Vars.mods.getScripts().context;
    public Prov<Scriptable> scopeProv = () -> Vars.mods.getScripts().scope;

    public CompiledScript(String script, @Nullable Prov<Context> contextProv, @Nullable Prov<Scriptable> scopeProv){
        this.script = script;
        if(contextProv != null) this.contextProv = contextProv;
        this.compiledScript = this.contextProv.get().compileString(script, Integer.toHexString(hashCode()) + "-compiled-script.js", 1);
        if(scopeProv != null) this.scopeProv = scopeProv;
    }

    public Object exec(){
        return compiledScript.exec(contextProv.get(), scopeProv.get());
    }

    public boolean isMatching(String otherScript){
        return Objects.equals(script, otherScript);
    }

    public String execWithResult() {
        try {
            Object o = exec();
            if (o instanceof NativeJavaObject n) {
                o = n.unwrap();
            }

            if (o == null) {
                o = "null";
            } else if (o instanceof Undefined) {
                o = "undefined";
            } else if (o instanceof Object[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof int[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof float[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof byte[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof double[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof long[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof char[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof boolean[] arr) {
                o = Arrays.toString(arr);
            } else if (o instanceof NativeArray arr) {
                o = Arrays.toString(arr.toArray());
            }

            String out = o.toString();
            return out == null ? "null" : out;
        } catch (Throwable t) {
            return this.getError(t, false);
        }
    }

    private String getError(Throwable t, boolean log) {
        if (log) {
            Log.err(t);
        }

        String err = t.getClass().getSimpleName();
        return err + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }

    @Override
    public void run(){
        exec();
    }

    public static CompiledScript with(String script){
        return with(script, null, null);
    }

    public static CompiledScript with(String script, @Nullable Prov<Context> contextProv, @Nullable Prov<Scriptable> scopeProv){
        return new CompiledScript(script, contextProv, scopeProv);
    }
}
