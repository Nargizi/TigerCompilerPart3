import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Memory {
    boolean declareVariable(Variable var);
    Register getAddress(Variable var);
    Variable getVariable(String name);
    Map<Variable, Register> getAllAddress();
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
        Register register = freeRegisters.poll();
        if (register == null)
            return false;
        stored.put(var, register);
        varStored.put(var.getName(), var);
        return true;
    }

    public boolean declarePair(Variable var, Register reg){
        stored.put(var, reg);
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

    @Override
    public Map<Variable, Register> getAllAddress() {
        return stored;
    }

    public void deleteVariable(Variable var){
        Register register = stored.remove(var);
        if(register == null) return;
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
    private boolean reversed;
    private Integer size;

    public Stack(Register memoryRegister, boolean reversed){
        this.stackRegister = memoryRegister;
        this.size = 0;
        this.stored = new HashMap<>();
        this.varStored = new HashMap<>();
        this.reversed = reversed;
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
        if(!stored.containsKey(var))
            return null;
        try {
            return getAddress(stored.get(var), var.getSize());
        } catch (NullPointerException e){
            return null;
        }
    }

    private Address getAddress(Integer index, Integer varSize){
        if(reversed)
            return new Address(stackRegister, size - index - varSize);
        return new Address(stackRegister, index);
    }

    public Integer getSize() {
        return size;
    }

    @Override
    public Variable getVariable(String name) {
        return varStored.get(name);
    }

    @Override
    public Map<Variable, Register> getAllAddress() {
        return stored
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          entry -> getAddress(entry.getValue(),
                                                              entry.getKey().getSize())));
    }
}

class GlobalMemory implements Memory{
    private final Map<String, Variable> varStored;

    GlobalMemory() {
        this.varStored = new HashMap<>();
    }

    @Override
    public boolean declareVariable(Variable var) {
        varStored.put(var.getName(), var);
        return true;
    }

    @Override
    public DataAddress getAddress(Variable var) {
        if(varStored.containsKey(var.getName()))
            return new DataAddress(var.getName(), var.getType());
        return null;
    }

    @Override
    public Variable getVariable(String name) {
        return varStored.get(name);
    }

    @Override
    public Map<Variable, Register> getAllAddress() {
        return varStored
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue,
                                          entry -> new DataAddress(entry.getKey(),
                                                                   entry.getValue().getType())));
    }
}

class Address extends Register{
    private Integer offset;
    private Register register;

    public Address(Register start, Integer offset) {
        super(start.getName(), start.getType());
        this.offset = offset;
        this.register = start;
    }

    public Register getStart() {
        return register;
    }

    public Integer getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return offset  + "(" + super.toString() + ")";
    }
}

class DataAddress extends Register {

    public DataAddress(String name, Type type){
        super(name, type);
    }

    @Override
    public String toString() {
        return getName();
    }
}