import com.sun.source.doctree.ReturnTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    final static List<Register> intTempRegisters = List.of("t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatTempRegisters = List.of("f4", "f6", "f8", "f10", "f16", "f18")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());

    private RegisterMemory savedIntTempMemory;
    private RegisterMemory savedFloatTempMemory;
    private Integer tempVariableCounter;
    private RegisterAllocator registerAllocator;


    public Translator(RegisterAllocator registerAllocator){
        this.savedIntTempMemory = new RegisterMemory(intTempRegisters);
        this.savedFloatTempMemory = new RegisterMemory(floatTempRegisters);
        this.registerAllocator = registerAllocator;
        tempVariableCounter = 0;
    }

    public Variable getTempVariable(Type type){
        return new Variable(tempVariableCounter++ + "_INTERNAL_VAR", type);
    }

    public Register load(Variable var){
        if(var.getType().equals(Type.Float))
             if(savedFloatTempMemory.declareVariable(var))
                 return savedFloatTempMemory.getAddress(var);
             else
                 return null;
        else
        if(savedIntTempMemory.declareVariable(var))
            return savedIntTempMemory.getAddress(var);
        else
            return null;
    }

    public void store(Variable var){
        if(var.getType().equals(Type.Float))
            savedFloatTempMemory.deleteVariable(var);
        else
            savedIntTempMemory.deleteVariable(var);
    }



    public List<MIPSCommand> translate(IRCommand command){
        if(command instanceof BinaryOperatorCommand)
            return translateBinaryCommand((BinaryOperatorCommand) command);
        if(command instanceof ConditionalBranchCommand)
            return translateBranchCommand((ConditionalBranchCommand) command);
        if(command instanceof GotoCommand)
            return translateJumpCommand((GotoCommand) command);
        if(command instanceof LabelCommand)
            return translateLabelCommand((LabelCommand) command);
        if(command instanceof AssignmentCommand)
            return translateAssignmentCommand((AssignmentCommand) command);
        if(command instanceof ReturnCommand)
            return translateReturnCommand((ReturnCommand) command);
        if(command instanceof CallCommand)
            return translateCallCommand((CallCommand) command);
        if(command instanceof ArrayStoreCommand)
            return translateArrayStoreCommand((ArrayStoreCommand) command);
        if(command instanceof ArrayLoadCommand)
            return translateArrayLoadCommand((ArrayLoadCommand) command);
        return List.of();
    }

    private List<MIPSCommand> translateJumpCommand(GotoCommand command){
        return List.of(new JumpMIPSCommand(command.getLabel()));
    }

    private List<MIPSCommand> translateLabelCommand(LabelCommand command){
        return List.of(new LabelMIPSCommand(command.getLabel()));
    }

    // TODO: ARRAY? WIE KANN ICH DAS MACHEN?
    private List<MIPSCommand> translateArrayStoreCommand(ArrayStoreCommand command){
        return null;
    }

    private List<MIPSCommand> translateArrayLoadCommand(ArrayLoadCommand command){
        return null;
    }

    // TODO: support static variables
    private List<MIPSCommand> translateAssignmentCommand(AssignmentCommand command){
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));
        int num = command.getSize();
        if(num == 0){
            Register a = registerAllocator.getRegister((Variable) command.getVar());
            if (command.getValue() instanceof Constant) {
                commandList.add(new LoadIntCommand(a, (Constant) command.getValue()));
            } else {
                Register b = registerAllocator.getRegister((Variable) command.getValue());
                commandList.add(new MoveMIPSCommand(a, b));
            }
        }else {
            for (int i = 0; i < num; ++i) {
                // TODO: Array assignment
                if (command.getVar() instanceof Constant) {

                }
            }
        }
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    // TODO: too much effort
    private List<MIPSCommand> translateReturnCommand(ReturnCommand command){
        return null;
    }

    private List<MIPSCommand> translateCallCommand(CallCommand command){
        return null;
    }

    // TODO: float type operations
    private List<MIPSCommand> translateBranchCommand(ConditionalBranchCommand command){
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));
        Register a, b;
        if(command.getA() instanceof Constant && command.getB() instanceof Constant) {
            Variable tempVar = getTempVariable(Type.Integer);
            Variable tempVar2 = getTempVariable(Type.Integer);
            a = load(tempVar);
            b = load(tempVar2);
            store(tempVar);
            store(tempVar2);
            commandList.add(new LoadIntCommand(b, (Constant) command.getA()));
            commandList.add(new LoadIntCommand(b, (Constant) command.getB()));
        } else if(command.getA() instanceof Constant) {
            Variable tempVar = getTempVariable(Type.Integer);
            a = load(tempVar);
            b = registerAllocator.getRegister((Variable) command.getB());
            store(tempVar);
            commandList.add(new LoadIntCommand(b, (Constant) command.getA()));
        } else if(command.getB() instanceof Constant) {
            Variable tempVar = getTempVariable(Type.Integer);
            b = load(tempVar);
            a = registerAllocator.getRegister((Variable) command.getA());
            store(tempVar);
            commandList.add(new LoadIntCommand(b, (Constant) command.getB()));
        } else {
            a = registerAllocator.getRegister((Variable) command.getA());
            b = registerAllocator.getRegister((Variable) command.getB());
        }
        commandList.add(new BranchMIPSCommand(a, b, command.getLabel(), command.getBranchCommand()));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    // TODO: float type operations
    private List<MIPSCommand> translateBinaryCommand(BinaryOperatorCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));
        if(command.getA() instanceof Constant && command.getB() instanceof Constant) {
            Variable tempVar = getTempVariable(Type.Integer);
            Variable tempVar2 = getTempVariable(Type.Integer);
            Register tempReg = load(tempVar);
            Register tempReg2 = load(tempVar2);
            commandList.add(new LoadIntCommand(tempReg, (Constant) command.getA()));
            commandList.add(new LoadIntCommand(tempReg2, (Constant) command.getB()));
            Register reg = registerAllocator.getRegister((Variable) command.getDest());
            commandList.add(new BinaryMIPSCommand(reg, tempReg, tempReg2, command.getOp()));
            store(tempVar2);
            store(tempVar);
        }else if(command.getA() instanceof Constant){
            Variable tempVar = getTempVariable(Type.Integer);
            Register tempReg = load(tempVar);
            commandList.add(new LoadIntCommand(tempReg, (Constant) command.getA()));
            Register reg = registerAllocator.getRegister((Variable) command.getDest());
            commandList.add(new BinaryMIPSCommand(reg, tempReg, registerAllocator.getRegister((Variable) command.getB()), command.getOp()));
            store(tempVar);
        }else if(command.getB() instanceof Constant){
            Variable tempVar = getTempVariable(Type.Integer);
            Register tempReg = load(tempVar);
            commandList.add(new LoadIntCommand(tempReg, (Constant) command.getB()));
            Register reg = registerAllocator.getRegister((Variable) command.getDest());
            commandList.add(new BinaryMIPSCommand(reg, tempReg, registerAllocator.getRegister((Variable) command.getA()), command.getOp()));
            store(tempVar);
        }else{
            commandList.add(new BinaryMIPSCommand(registerAllocator.getRegister((Variable) command.getDest()),
                    registerAllocator.getRegister((Variable) command.getA()), registerAllocator.getRegister((Variable) command.getB()), command.getOp()));
        }

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }


}
