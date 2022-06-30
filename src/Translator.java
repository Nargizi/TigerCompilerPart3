import com.sun.source.doctree.ReturnTree;

import java.util.ArrayList;
import java.util.Collection;
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
//        if(command instanceof ReturnCommand)
//            return translateReturnCommand((ReturnCommand) command);
//        if(command instanceof CallCommand)
//            return translateCallCommand((CallCommand) command);
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

    // TODO: non-constant index
    private List<MIPSCommand> translateArrayStoreCommand(ArrayStoreCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));

        Variable array = command.getArray();
        Address arrayAddress = registerAllocator.func.getAddress(command.getArray());
        int index = Integer.parseInt(((Constant)command.getIndex()).getValue());
        Argument value = command.getValue();

        Register valueRegister;
        Variable tempVariable = null;
        if (value instanceof Constant) {
            tempVariable = getTempVariable(value.getType());
            valueRegister = load(tempVariable);
            commandList.add(value.getType().equals(Type.Float) ?
                    new LoadFloatCommand(valueRegister, ((Constant)value)) :
                    new LoadIntCommand(valueRegister, (Constant)value));
        } else {
            valueRegister = registerAllocator.getRegister((Variable)value);
        }

        if (!valueRegister.getType().equals(array.getType())) {
            Variable tempTypeVariable = getTempVariable(array.getType());
            Register tempTypeRegister = load(tempTypeVariable);

            commandList.add(valueRegister.getType().equals(Type.Float) ?
                    new FloatToIntCommand(tempTypeRegister, valueRegister) :
                    new IntToFloatCommand(tempTypeRegister, valueRegister));

            store(tempTypeVariable);
            valueRegister = tempTypeRegister;
        }

        if (tempVariable != null) store(tempVariable);

        commandList.add(new StoreMIPSCommand(valueRegister,
                new Address(arrayAddress, arrayAddress.getOffset() + index * array.getType().getSize()),
                array.getType().equals(Type.Float)));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    // TODO: non-constant index
    private List<MIPSCommand> translateArrayLoadCommand(ArrayLoadCommand command){
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));

        Variable array = command.getArray();
        Address arrayAddress = registerAllocator.func.getAddress(command.getArray());
        int index = Integer.parseInt(((Constant)command.getIndex()).getValue());
        Register variable = registerAllocator.getRegister(command.getVar());

        Register loadRegister;
        if (!variable.getType().equals(array.getType())) {
            Variable tempVariable = getTempVariable(array.getType());
            loadRegister = load(tempVariable);
            store(tempVariable);
        } else {
            loadRegister = variable;
        }

        commandList.add(new LoadMIPSCommand(loadRegister,
                new Address(arrayAddress, arrayAddress.getOffset() + index * array.getType().getSize()),
                array.getType().equals(Type.Float)));

        if (!variable.getType().equals(array.getType()))
            commandList.add(variable.getType().equals(Type.Float) ?
                    new IntToFloatCommand(variable, loadRegister) :
                    new FloatToIntCommand(variable, loadRegister));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    // TODO: support static variables (Giorgio what does this mean?)
    private List<MIPSCommand> translateAssignmentCommand(AssignmentCommand command){
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));
        int num = command.getSize();

        if(num == 0) {
            if (!(command.getVar() instanceof Array))
                commandList.addAll(translateVariableAssignment(command));
            else
                commandList.addAll(translateArrayArrayAssignment(command));
        }
        else commandList.addAll(translateArrayVarAssignment(command));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateVariableAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();

        Register a = registerAllocator.getRegister((Variable) command.getVar());
        Argument b = command.getValue();

        if (b instanceof Constant && b.getType().equals(a.getType())) {
            commandList.add(a.getType().equals(Type.Float) ?
                    new LoadFloatCommand(a, (Constant)b) :
                    new LoadIntCommand(a, (Constant)b));
        } else if (b instanceof Constant || !b.getType().equals(a.getType())) {
            Register bRegister;

            if (b instanceof Constant) {
                Variable bVariable = getTempVariable(b.getType());
                bRegister = load(bVariable);
                store(bVariable);
            } else {
                bRegister = registerAllocator.getRegister((Variable)b);
            }

            commandList.add(b.getType().equals(Type.Float) ?
                    new FloatToIntCommand(a, bRegister) :
                    new IntToFloatCommand(a, bRegister));
        } else {
            Register bRegister = registerAllocator.getRegister((Variable)b);
            commandList.add(new MoveMIPSCommand(a, bRegister));
        }

        return commandList;
    }

    private List<MIPSCommand> translateArrayArrayAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();

        Array a = (Array)command.getVar();
        Array b = (Array)command.getValue();
        int size = a.getSize();
        Address aBaseAddress = registerAllocator.func.getAddress(a);
        Address bBaseAddress = registerAllocator.func.getAddress(b);

        for (int i = 0; i < size; i++) {
            Address destination = new Address(aBaseAddress, aBaseAddress.getOffset() + i * a.getType().getSize());
            Address source = new Address(bBaseAddress, bBaseAddress.getOffset() + i * b.getType().getSize());

            Variable bTemp = getTempVariable(b.getType());
            Register bRegister = load(bTemp);
            commandList.add(new LoadMIPSCommand(bRegister, source, b.getType().equals(Type.Float)));

            if (!b.getType().equals(a.getType())) {
                Variable bTypeTemp = getTempVariable(b.getType());
                Register bTypeRegister = load(bTypeTemp);

                commandList.add(b.getType().equals(Type.Float) ?
                        new FloatToIntCommand(bTypeRegister, bRegister) :
                        new IntToFloatCommand(bTypeRegister, bRegister));

                bRegister = bTypeRegister;
                store(bTypeTemp);
            }

            commandList.add(new StoreMIPSCommand(bRegister, destination, a.getType().equals(Type.Float)));

            store(bTemp);
        }

        return commandList;
    }

    private List<MIPSCommand> translateArrayVarAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new ArrayList<>();

        int size = command.getSize();
        Variable a = (Variable)command.getVar();
        Address aBaseAddress = registerAllocator.func.getAddress(a);

        for (int i = 0; i < size; ++i) {
            Address destination = new Address(aBaseAddress, aBaseAddress.getOffset() + i * a.getType().getSize());
            Argument b = command.getValue();

            Register bRegister;
            Variable bVariable = null;
            if (b instanceof Constant) {
                bVariable = getTempVariable(b.getType());
                bRegister = load(bVariable);
            } else {
                bRegister = registerAllocator.getRegister((Variable)b);
            }

            if (!bRegister.getType().equals(destination.getType())) {
                Variable tempVariable = getTempVariable(destination.getType());
                Register tempRegister = load(tempVariable);

                commandList.add((bRegister.getType().equals(Type.Float) ?
                        new FloatToIntCommand(tempRegister, bRegister) :
                        new IntToFloatCommand(tempRegister, bRegister)));

                bRegister = tempRegister;
                store(tempVariable);
            }

            if (bVariable != null) store(bVariable);

            commandList.add(new StoreMIPSCommand(bRegister, destination, bRegister.getType().equals(Type.Float)));
        }

        return  commandList;
    }

    // TODO: too much effort
    private List<MIPSCommand> translateReturnCommand(ReturnCommand command){
        return null;
    }

    private List<MIPSCommand> translateCallCommand(CallCommand command){
        return null;
    }

    // TODO: float type operations
    // TODO: float branching a need different MIPS command
    private List<MIPSCommand> translateBranchCommand(ConditionalBranchCommand command){
        List<MIPSCommand> commandList = new ArrayList<>(registerAllocator.enterCommand(command));
        Register a, b;

        // FIXME: DUUUDE WTF IS THIS WHY NOT THE COMMENTED WAY BELOW BRO?
        if(command.getA() instanceof Constant && command.getB() instanceof Constant) {
            Variable tempVar = getTempVariable(Type.Integer);
            Variable tempVar2 = getTempVariable(Type.Integer);
            a = load(tempVar);
            b = load(tempVar2);
            store(tempVar);
            store(tempVar2);
            commandList.add(new LoadIntCommand(a, (Constant) command.getA()));
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

//        Argument aVar = command.getA();
//        Argument bVar = command.getB();
//        Variable aTempVar = null;
//
//        if (aVar instanceof Constant) {
//            aTempVar = getTempVariable(aVar.getType());
//            a = load(aTempVar);
//            commandList.add(aVar.getType().equals(Type.Float) ?
//                    new LoadFloatCommand(a, (Constant)aVar) :
//                    new LoadIntCommand(a, (Constant)aVar));
//        }
//
//        if (bVar instanceof Constant) {
//            Variable bTempVar = getTempVariable(bVar.getType());
//            b = load(bTempVar);
//            commandList.add(bVar.getType().equals(Type.Float) ?
//                    new LoadFloatCommand(b, (Constant)bVar) :
//                    new LoadIntCommand(b, (Constant)bVar));
//
//            store(bTempVar);
//        }
//        if (aTempVar != null) store(aTempVar);

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
