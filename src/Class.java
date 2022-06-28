import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Class {
    private String className;
    private Map<String, Function> functions;
    private Memory staticMemory;

    public Class(String name){
        this.className = name;
        this.functions = new HashMap<>();
        this.staticMemory = new Memory(new Register("gp"));
    }

    public void addFunction(Function func){
        functions.put(func.getFuncName(), func);
        func.setCurrClass(this);
    }

    public void addStaticVar(Argument arg){
        staticMemory.declareVariable(arg);
    }

    public Memory.Address getAddress(Argument arg){
        return staticMemory.getAddress(arg);
    }

    public Map<String, Function> getFunctions(){
        return functions;
    }

    public Argument getArgument(String value) {
        return staticMemory.getArgument(value);
    }
}
