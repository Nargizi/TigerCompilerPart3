
import java.util.*;
import java.util.stream.Collectors;

public abstract class RegisterAllocator {

    final static List<Register> intSavedRegisters = List.of("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatSavedRegisters = List.of("f20", "f22", "f24", "f26", "28", "f30")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());

    private RegisterMemory savedIntRegisterMemory;
    private RegisterMemory savedFloatRegisterMemory;
    protected Function func;

    public RegisterAllocator(Function func){
        this.savedIntRegisterMemory = new RegisterMemory(intSavedRegisters);
        this.savedFloatRegisterMemory = new RegisterMemory(floatSavedRegisters);
        this.func = func;
    }

    public void reset(){
        this.savedIntRegisterMemory = new RegisterMemory(intSavedRegisters);
        this.savedFloatRegisterMemory = new RegisterMemory(floatSavedRegisters);
    }

    public Function getFunc() {
        return func;
    }

    public void setFunc(Function func) {
        this.func = func;
    }

    public LoadMIPSCommand loadCommand(Register dest, Address origin){
        return new LoadMIPSCommand(dest, origin);
    }

    public StoreMIPSCommand storeCommand(Register origin, Address dest){
        return new StoreMIPSCommand(origin, dest);
    }

    public Register load(Variable var){
        if(var.getType().equals(Type.Float))
            if(savedFloatRegisterMemory.declareVariable(var))
                return savedFloatRegisterMemory.getAddress(var);
            else
                return null;
        else
        if(savedIntRegisterMemory.declareVariable(var))
            return savedIntRegisterMemory.getAddress(var);
        else
            return null;
    }

    public void store(Variable var){
        if(var.getType().equals(Type.Float))
            savedFloatRegisterMemory.deleteVariable(var);
        else
            savedIntRegisterMemory.deleteVariable(var);
    }

    public Register getRegister(Variable var){
        Register reg;
        if(var.getType().equals(Type.Float)) {
            reg = savedFloatRegisterMemory.getAddress(var);
        }
        else {
            reg = savedIntRegisterMemory.getAddress(var);
        }
        if (reg != null)
            return reg;
        return func.getRegister(var);
    }

    public boolean inRegister(Variable var){
        return getRegister(var) != null;
    }

    public abstract List<MIPSCommand> enterCommand(IRCommand command);
    public abstract List<MIPSCommand> exitCommand(IRCommand command);

}

class NaiveAllocator extends RegisterAllocator {
    public NaiveAllocator(Function func) {
        super(func);
    }

    @Override
    public List<MIPSCommand> enterCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();
        Set<Variable> decl = command.getDecl();
        Set<Variable> used = command.getUsed();

        for(Variable var: decl){
            load(var);
        }

        for(Variable var: used) {
            load(var);
            commandList.add(loadCommand(getRegister(var), func.getAddress(var)));
        }

        return commandList;
    }

    @Override
    public List<MIPSCommand> exitCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();
        Set<Variable> decl = command.getDecl();
        Set<Variable> used = command.getUsed();

        for(Variable var: decl){
            commandList.add(storeCommand(getRegister(var), func.getAddress(var)));
            store(var);
        }

        for(Variable var: used) {
            store(var);
        }

        return commandList;
    }
}

class CFGAllocator extends RegisterAllocator {


    public CFGAllocator(Function func) {
        super(func);
    }

    @Override
    public List<MIPSCommand> enterCommand(IRCommand command) {
        return null;
    }

    @Override
    public List<MIPSCommand> exitCommand(IRCommand command) {
        return null;
    }
}

class BriggsAllocator extends RegisterAllocator {


    public BriggsAllocator(Function func) {
        super(func);
    }

    @Override
    public List<MIPSCommand> enterCommand(IRCommand command) {
        return null;
    }

    @Override
    public List<MIPSCommand> exitCommand(IRCommand command) {
        return null;
    }
}

