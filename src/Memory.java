import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public interface Memory {
    boolean declareVariable(Variable var);
    Register getAddress(Variable var);
    Variable getVariable(String name);
}

class RegisterMemory implements Memory {
    private final LinkedList<Register> freeRegisters;
    private final Map<Variable, Register> stored;
    private final Map<String, Variable> varStored;

    public RegisterMemory(List<Register> registers){
        freeRegisters = new LinkedList<>(registers);
        stored = new HashMap<>();
        varStored = new HashMap<>();
    }

    @Override
    public boolean declareVariable(Variable var){
        Register register = freeRegisters.pop();
        if (register == null)
            return false;
        stored.put(var, register);
        varStored.put(var.getName(), var);
        return true;
    }

    @Override
    public Register getAddress(Variable var){
        return stored.get(var);
    }

    @Override
    public Variable getVariable(String name) {
        return varStored.get(name);
    }

    public void deleteVariable(Variable var){
        Register register = stored.remove(var);
        varStored.remove(var.getName());
        freeRegister(register);
    }

    public Integer getNumFree(){
        return freeRegisters.size();
    }

    private void freeRegister(Register register){
        freeRegisters.add(register);
    }

}

class Stack implements Memory {
    private final Register stackRegister;
    private final Map<Variable, Integer> stored;
    private final Map<String, Variable> varStored;
    private Integer size;

    public Stack(Register memoryRegister){
        this.stackRegister = memoryRegister;
        this.size = 0;
        this.stored = new HashMap<>();
        this.varStored = new HashMap<>();
    }

    @Override
    public boolean declareVariable(Variable var){
        if(stored.containsKey(var))
            return false;

        varStored.put(var.getName(), var);
        stored.put(var, size);
        size += var.getSize();
        return true;
    }

    @Override
    public Address getAddress(Variable var){
        // in assembly stack is increasing from top to bottom.
        // last element is always at the start of the stack (meaning at address $sp(0))
        // instead of top of the stack, like in most programming languages (including java)
        // so here i am trying to mimic stack pointer behaviour by reversing object indices
        return new Address(stackRegister, size - stored.get(var) - var.getSize());
    }

    @Override
    public Variable getVariable(String name) {
        return varStored.get(name);
    }
}

class Address extends Register{
    private Integer offset;

    public Address(Register start, Integer offset) {
        super(start.getName(), start.getType());
        this.offset = offset;
    }

    public Register getStart() {
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return offset  + "(" + super.toString() + ")";
    }
}
