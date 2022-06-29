
import java.util.*;
import java.util.stream.Collectors;

public abstract class RegisterAllocator {

    final static List<Register> intSavedRegisters = List.of("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatSavedRegisters = List.of("f20", "f22", "f24", "f26", "28", "f30")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());

    protected RegisterMemory savedIntRegisterMemory;
    protected RegisterMemory savedFloatRegisterMemory;
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
    Set<Variable> currStored;
    BasicBlocks.Block currBlock;

    public CFGAllocator(Function func) {
        super(func);
        currStored = new HashSet<>();
        currBlock = null;
    }

    //TODO: return load commands for each stored variable
    private List<MIPSCommand> enterBlock(BasicBlocks.Block block){
        List<MIPSCommand> commandList = new ArrayList<>();
        var usedVars = block.getUsedVars();
        while(!usedVars.isEmpty() && (savedFloatRegisterMemory.getNumFree() > 3 || savedIntRegisterMemory.getNumFree() > 3)){
            Variable curr = (Variable) usedVars.poll().getKey();
            if(curr.getType().equals(Type.Float) && savedFloatRegisterMemory.getNumFree() > 3) {
                Register reg = load(curr);
                currStored.add(curr);
                //TODO: get load command for float
            }else if (curr.getType().equals(Type.Integer) && savedIntRegisterMemory.getNumFree() > 3){
                Register reg = load(curr);
                currStored.add(curr);
                commandList.add(loadCommand(reg, func.getAddress(curr)));
            }
        }
        currBlock = block;
        return commandList;
    }

    //TODO: return store commands for each freed variable
    private List<MIPSCommand> exitBlock(){
        List<MIPSCommand> commandList = new ArrayList<>();
        for(Variable var: currStored){
            if(var.getType().equals(Type.Float)){
                //TODO: get store command for float
            }else{
                commandList.add(storeCommand(getRegister(var), func.getAddress(var)));
            }
            store(var);
        }
        currStored.clear();
        return commandList;
    }


    @Override
    public List<MIPSCommand> enterCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();
        BasicBlocks.Block block = command.getBlock();
        if (currBlock == null || !currBlock.equals(block)){
            System.out.println("EXIT BLOCK");
            commandList.addAll(exitBlock());
            System.out.println("ENTER BLOCK");
            commandList.addAll(enterBlock(block));
        }

        Set<Variable> decl = new HashSet<>(command.getDecl());
        Set<Variable> used = new HashSet<>(command.getUsed());

        decl.removeAll(currStored);
        used.removeAll(currStored);

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
        BasicBlocks.Block currBlock = command.getBlock();
        if (currBlock.getCommands().get(currBlock.getCommands().size() - 1).equals(command)){
            System.out.println("EXIT BLOCK");
            commandList.addAll(exitBlock());
        }

        Set<Variable> decl = new HashSet<>(command.getDecl());
        Set<Variable> used = new HashSet<>(command.getUsed());

        decl.removeAll(currStored);
        used.removeAll(currStored);

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

