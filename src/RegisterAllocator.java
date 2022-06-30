
import java.util.*;
import java.util.stream.Collectors;

public abstract class RegisterAllocator {

    public final static List<Register> intSavedRegisters = List.of("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    public final static List<Register> floatSavedRegisters = List.of("f20", "f22", "f24", "f26", "f28", "f30")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());

    protected RegisterMemory savedIntRegisterMemory;
    protected RegisterMemory savedFloatRegisterMemory;
    protected Function func;

    public RegisterAllocator(){
    }

    public void reset(Function func){
        this.savedIntRegisterMemory = new RegisterMemory(intSavedRegisters);
        this.savedFloatRegisterMemory = new RegisterMemory(floatSavedRegisters);
        this.func = func;
    }

    public Function getFunc() {
        return func;
    }

    public void setFunc(Function func) {
        this.func = func;
    }

    public LoadMIPSCommand loadCommand(Register dest, Register origin, boolean isFloat){
        return new LoadMIPSCommand(dest, origin, isFloat);
    }

    public StoreMIPSCommand storeCommand(Register origin, Register dest, boolean isFloat) {
        return new StoreMIPSCommand(origin, dest, isFloat);
    }

    public List<Register> getUsedIntRegister(){
        return savedIntRegisterMemory.getAllAddress().values().stream().toList();
    }

    public List<Register> getUsedFloatRegister(){
        return savedFloatRegisterMemory.getAllAddress().values().stream().toList();
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

    @Override
    public List<MIPSCommand> enterCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();
        Set<Variable> decl = command.getDecl();
        Set<Variable> used = command.getUsed();
//        System.out.println("ENTER START: " + savedFloatRegisterMemory.getNumFree());
        for(Variable var: used) {
            if (inRegister(var)) continue;
            load(var);
            commandList.add(loadCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
        }

        for(Variable var: decl){
            if (inRegister(var)) continue;
//            System.out.println(var + " : " + load(var));
            load(var);
        }
//        System.out.println("ENTER END: " + savedFloatRegisterMemory.getNumFree());
        return commandList;
    }


    @Override
    public List<MIPSCommand> exitCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();
        Set<Variable> decl = command.getDecl();
        Set<Variable> used = command.getUsed();
//        System.out.println("EXIT START: " + savedFloatRegisterMemory.getNumFree());

        for(Variable var: decl){
            commandList.add(storeCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
            store(var);
        }

        for(Variable var: used) {
            store(var);
        }

//    System.out.println("EXIT END: " + savedFloatRegisterMemory.getNumFree());
        return commandList;
    }
}

class CFGAllocator extends RegisterAllocator {
    Set<Variable> currStored;
    BasicBlocks.Block currBlock;

    public CFGAllocator() {
        currStored = new HashSet<>();
        currBlock = null;
    }

    private List<MIPSCommand> enterBlock(BasicBlocks.Block block){
        List<MIPSCommand> commandList = new ArrayList<>();
        var usedVars = block.getUsedVars();
        while(!usedVars.isEmpty() && (savedFloatRegisterMemory.getNumFree() > 3 || savedIntRegisterMemory.getNumFree() > 3)){
            Variable curr = (Variable) usedVars.poll().getKey();
            if (inRegister(curr)){
                currStored.add(curr);
                continue;
            }
            if(curr.getType().equals(Type.Float) && savedFloatRegisterMemory.getNumFree() > 3) {
                Register reg = load(curr);
                currStored.add(curr);
                commandList.add(loadCommand(reg, func.getAddress(curr), true));
            }else if (curr.getType().equals(Type.Integer) && savedIntRegisterMemory.getNumFree() > 3){
                Register reg = load(curr);
                currStored.add(curr);
                commandList.add(loadCommand(reg, func.getAddress(curr), false));
            }
        }
        currBlock = block;
        return commandList;
    }

    private List<MIPSCommand> exitBlock(){
        List<MIPSCommand> commandList = new ArrayList<>();
        for(Variable var: currStored){
            commandList.add(storeCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
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
            commandList.addAll(exitBlock());
            commandList.addAll(enterBlock(block));
        }

        Set<Variable> decl = new HashSet<>(command.getDecl());
        Set<Variable> used = new HashSet<>(command.getUsed());

        decl.removeAll(currStored);
        used.removeAll(currStored);

        for(Variable var: used) {
            if (inRegister(var)) continue;
            load(var);
            commandList.add(loadCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
        }

        for(Variable var: decl){
            if (inRegister(var)) continue;
            load(var);
        }

        return commandList;
    }

    @Override
    public List<MIPSCommand> exitCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();

        Set<Variable> decl = new HashSet<>(command.getDecl());
        Set<Variable> used = new HashSet<>(command.getUsed());

        decl.removeAll(currStored);
        used.removeAll(currStored);

        for(Variable var: decl){
            commandList.add(storeCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
            store(var);
        }

        for(Variable var: used) {
            store(var);
        }

        BasicBlocks.Block currBlock = command.getBlock();
        if (currBlock.getCommands().get(currBlock.getCommands().size() - 1).equals(command)){
            commandList.addAll(exitBlock());
        }
        return commandList;
    }
}

class BriggsAllocator extends RegisterAllocator {
    private LivenessAnalysis livenessAnalysis;
    private InterferenceGraph graph;

    @Override
    public void reset(Function func){
        super.reset(func);
        graph = new InterferenceGraph(livenessAnalysis.getLiveSet(func.getFuncName()),
                                      intSavedRegisters.stream().limit(5).collect(Collectors.toSet()),
                                      floatSavedRegisters.stream().limit(3).collect(Collectors.toSet()));
    }

    public BriggsAllocator(LivenessAnalysis livenessAnalysis) {
        this.livenessAnalysis = livenessAnalysis;
    }

    @Override
    public List<MIPSCommand> enterCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();

        Set<Variable> decl = new HashSet<>(command.getDecl());
        Set<Variable> used = new HashSet<>(command.getUsed());

        for(Variable var: used) {
            if (inRegister(var)) continue;
            Register reg = graph.getRegister(var);
            if(reg != null){
                if(reg.getType().equals(Type.Float))
                    savedFloatRegisterMemory.declarePair(var, reg);
                else
                    savedIntRegisterMemory.declarePair(var, reg);
                continue;
            }
            load(var);
            commandList.add(loadCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
        }

        for(Variable var: decl){
            Register reg = graph.getRegister(var);
            if(reg != null){
                if(reg.getType().equals(Type.Float))
                    savedFloatRegisterMemory.declarePair(var, reg);
                else
                    savedIntRegisterMemory.declarePair(var, reg);
                continue;
            }
            if (inRegister(var)) continue;
            load(var);
        }

        return commandList;
    }

    @Override
    public List<MIPSCommand> exitCommand(IRCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();

        Set<Variable> decl = new HashSet<>(command.getDecl());
        Set<Variable> used = new HashSet<>(command.getUsed());

        for(Variable var: decl){
            if(graph.getRegister(var) != null) continue;
            commandList.add(storeCommand(getRegister(var), func.getAddress(var), var.getType().equals(Type.Float)));
            store(var);
        }

        for(Variable var: used) {
            if(graph.getRegister(var) != null) continue;
            store(var);
        }

        return commandList;
    }
}

