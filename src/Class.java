import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Class {
    private final Map<String, Function> functions;
    private final Stack staticMemory;
    private final String className;

    public Class(String name){
        this.className = name;
        this.functions = new HashMap<>();
        this.staticMemory = new Stack(new Register("gp", Type.Integer));
    }

    public void addFunction(Function func){
        functions.put(func.getFuncName(), func);
        func.setCurrClass(this);
    }

    public void addStaticVar(Variable arg){
        staticMemory.declareVariable(arg);
    }

    public Address getAddress(Variable arg){
        return staticMemory.getAddress(arg);
    }

    public Map<String, Function> getFunctions(){
        return functions;
    }

    public Argument getVariable(String value) {
        return staticMemory.getVariable(value);
    }

    public String getClassName() {
        return className;
    }
}
