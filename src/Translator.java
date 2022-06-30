import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Translator {

    final static List<Register> intTempRegisters = List.of("t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatTempRegisters = List.of("f4", "f6", "f8", "f10", "f16", "f18")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());

    final static Register SP = new Register("sp", Type.Integer);
    final static Register FP = new Register("fp", Type.Integer);
    final static Register RA = new Register("ra", Type.Integer);

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

    public List<MIPSCommand> translate(Class c){
        List<MIPSCommand> commandList= new LinkedList<>();
        commandList.add(new AssemblerDirectiveCommand("data"));
        for(var add: c.getAllGlobalAddress().entrySet()) {
            if(add.getKey() instanceof Array)
                commandList.add(new DataTypeMIPSCommand(add.getValue().getName(), "space", String.valueOf(add.getKey().getSize())));
            else
                commandList.add(new DataTypeMIPSCommand(add.getValue().getName(), add.getKey().getType().equals(Type.Float) ? "float": "word"));
        }
        commandList.add(new AssemblerDirectiveCommand("text"));
        commandList.add(new AssemblerDirectiveCommand("global", List.of("main")));
        for(var func: c.getFunctions().values()){
            commandList.addAll(translate(func));
        }
        return commandList;
    }
    //TODO: save fp value
    private List<MIPSCommand> prolog(Function f){
        List<MIPSCommand> commandList= new LinkedList<>();
        int argStorage = f.getMaxArgument() * 4;
        int savedRegister = RegisterAllocator.intSavedRegisters.size() * 4 + RegisterAllocator.floatSavedRegisters.size() * 8;
        int returnAddress = 4;
        int stackSize = argStorage + savedRegister + returnAddress + f.getLocalMemorySize();
        commandList.add(new BinaryImmediateMIPSCommand(SP, SP, new Constant(String.valueOf(-stackSize)), BinaryOperator.ADD));
        commandList.add(new StoreMIPSCommand(RA, new Address(SP, stackSize - returnAddress), false));

        for(int i = 0; i < RegisterAllocator.intSavedRegisters.size(); ++i){
            commandList.add(new StoreMIPSCommand(RegisterAllocator.intSavedRegisters.get(i),
                                                 new Address(SP, argStorage + f.getLocalMemorySize() + i * 4), false));
        }

        for(int i = 0; i < RegisterAllocator.floatSavedRegisters.size(); ++i){
            commandList.add(new StoreMIPSCommand(RegisterAllocator.floatSavedRegisters.get(i),
                                                 new Address(SP, argStorage + f.getLocalMemorySize()
                                                         + i * 8 + RegisterAllocator.intSavedRegisters.size() * 4),
                                                 false));
        }
        return commandList;
    }

    public List<MIPSCommand> translate(Function f){
        List<MIPSCommand> commandList= new LinkedList<>();
        registerAllocator.reset(f);
        commandList.addAll(prolog(f));
        for(int i = 0; i < f.getNumCommands(); ++i){
            commandList.add(new CommentMIPSCommand(f.getCommand(i).toString()));
            commandList.addAll(translate(f.getCommand(i)));
        }
        return commandList;
    }

    //TODO: restore saved values
    private List<MIPSCommand> epilog(Function f){
        List<MIPSCommand> commandList= new LinkedList<>();

        return commandList;
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
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));
        commandList.add(new JumpMIPSCommand(command.getLabel()));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateLabelCommand(LabelCommand command){
        List<MIPSCommand> commandList = new LinkedList<>();
        commandList.add(new LabelMIPSCommand(command.getLabel()));
        commandList.addAll(registerAllocator.enterCommand(command));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    //TODO: Static Memory
    private List<MIPSCommand> translateArrayStoreCommand(ArrayStoreCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));

        Variable array = command.getArray();
        Variable tempIndexVar = null, tempSizeVar = null;
        Address arrayAddress = registerAllocator.func.getLocalAddress(command.getArray());
        if(command.getIndex() instanceof Constant) {
            int index = Integer.parseInt(((Constant) command.getIndex()).getValue());
            arrayAddress = new Address(arrayAddress.getStart(), index * array.getType().getSize() + arrayAddress.getOffset());
        } else {
            Register index = registerAllocator.getRegister((Variable) command.getIndex());
            if(index.getType().equals(Type.Float)){
                tempIndexVar = getTempVariable(Type.Integer);
                Register tempReg = load(tempIndexVar);
                commandList.add(new FloatToIntCommand(tempReg, index)); // convert index to int
                index = tempReg;
            }
            tempSizeVar = getTempVariable(Type.Integer);
            Register sizeVar = load(tempSizeVar);
            commandList.add(new LoadIntCommand(sizeVar, new Constant("4")));
            commandList.add(new BinaryMIPSCommand(index, index, sizeVar, BinaryOperator.MUL, false)); // index = index * 4
            commandList.add(new BinaryMIPSCommand(index, index, arrayAddress.getStart(), BinaryOperator.ADD,true)); // index = base + index
            arrayAddress = new Address(index, arrayAddress.getOffset());
        }
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
        if (tempIndexVar != null) store(tempIndexVar);
        if(tempSizeVar != null) store(tempSizeVar);

        commandList.add(new StoreMIPSCommand(valueRegister,
                arrayAddress,
                array.getType().equals(Type.Float)));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    //TODO: Static Memory
    private List<MIPSCommand> translateArrayLoadCommand(ArrayLoadCommand command){
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));

        Variable array = command.getArray();
        Address arrayAddress = registerAllocator.func.getLocalAddress(command.getArray());
        Variable tempIndexVar = null, tempSizeVar = null;

        if(command.getIndex() instanceof Constant) {
            int index = Integer.parseInt(((Constant) command.getIndex()).getValue());
            arrayAddress = new Address(arrayAddress.getStart(), index * array.getType().getSize() + arrayAddress.getOffset());
        } else {
            Register index = registerAllocator.getRegister((Variable) command.getIndex());
            if(index.getType().equals(Type.Float)){
                tempIndexVar = getTempVariable(Type.Integer);
                Register tempReg = load(tempIndexVar);
                commandList.add(new FloatToIntCommand(tempReg, index)); // convert index to int
                index = tempReg;
            }
            tempSizeVar = getTempVariable(Type.Integer);
            Register sizeVar = load(tempSizeVar);
            commandList.add(new LoadIntCommand(sizeVar, new Constant("4")));
            commandList.add(new BinaryMIPSCommand(index, index, sizeVar, BinaryOperator.MUL, false)); // index = index * 4
            commandList.add(new BinaryMIPSCommand(index, index, arrayAddress.getStart(), BinaryOperator.ADD,false)); // index = base + index
            arrayAddress = new Address(index, arrayAddress.getOffset());
        }
        Register variable = registerAllocator.getRegister(command.getVar());

        Register loadRegister;
        if (!variable.getType().equals(array.getType())) {
            Variable tempVariable = getTempVariable(array.getType());
            loadRegister = load(tempVariable);
            store(tempVariable);
        } else {
            loadRegister = variable;
        }

        commandList.add(new LoadMIPSCommand(loadRegister, arrayAddress, array.getType().equals(Type.Float)));

        if (!variable.getType().equals(array.getType()))
            commandList.add(variable.getType().equals(Type.Float) ?
                    new IntToFloatCommand(variable, loadRegister) :
                    new FloatToIntCommand(variable, loadRegister));

        if (tempIndexVar != null) store(tempIndexVar);
        if(tempSizeVar != null) store(tempSizeVar);

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateAssignmentCommand(AssignmentCommand command){
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));
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
        List<MIPSCommand> commandList = new LinkedList<>();

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

    //TODO: Static Memory
    private List<MIPSCommand> translateArrayArrayAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>();

        Array a = (Array)command.getVar();
        Array b = (Array)command.getValue();
        int size = a.getSize();
        Address aBaseAddress = registerAllocator.func.getLocalAddress(a);
        Address bBaseAddress = registerAllocator.func.getLocalAddress(b);

        for (int i = 0; i < size; i++) {
            Address destination = new Address(aBaseAddress.getStart(), aBaseAddress.getOffset() + i * a.getType().getSize());
            Address source = new Address(bBaseAddress.getStart(), bBaseAddress.getOffset() + i * b.getType().getSize());

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

    //TODO: Static Memory
    private List<MIPSCommand> translateArrayVarAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>();

        int size = command.getSize();
        Variable a = (Variable)command.getVar();
        Address aBaseAddress = registerAllocator.func.getLocalAddress(a);

        for (int i = 0; i < size; ++i) {
            Address destination = new Address(aBaseAddress.getStart(), aBaseAddress.getOffset() + i * a.getType().getSize());
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
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));

        Argument aVar = command.getA();
        Argument bVar = command.getB();
        Register a, b;
        Variable aTempVar = null;

        if (aVar instanceof Constant) {
            aTempVar = getTempVariable(aVar.getType());
            a = load(aTempVar);
            commandList.add(aVar.getType().equals(Type.Float) ?
                    new LoadFloatCommand(a, (Constant)aVar) :
                    new LoadIntCommand(a, (Constant)aVar));
        }else{
            a = registerAllocator.getRegister((Variable) aVar);
        }

        if (bVar instanceof Constant) {
            Variable bTempVar = getTempVariable(bVar.getType());
            b = load(bTempVar);
            commandList.add(bVar.getType().equals(Type.Float) ?
                    new LoadFloatCommand(b, (Constant)bVar) :
                    new LoadIntCommand(b, (Constant)bVar));

            store(bTempVar);
        } else {
            b = registerAllocator.getRegister((Variable) bVar);
        }
        if (aTempVar != null) store(aTempVar);

        commandList.add(new BranchMIPSCommand(a, b, command.getLabel(), command.getBranchCommand()));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateBinaryCommand(BinaryOperatorCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));
        Argument aVar = command.getA();
        Argument bVar = command.getB();
        Register a, b;
        Variable aTempVar = null;

        if (aVar instanceof Constant) {
            aTempVar = getTempVariable(aVar.getType());
            a = load(aTempVar);
            commandList.add(aVar.getType().equals(Type.Float) ?
                                    new LoadFloatCommand(a, (Constant)aVar) :
                                    new LoadIntCommand(a, (Constant)aVar));
        }else{
            a = registerAllocator.getRegister((Variable) aVar);
        }

        if (bVar instanceof Constant) {
            Variable bTempVar = getTempVariable(bVar.getType());
            b = load(bTempVar);
            commandList.add(bVar.getType().equals(Type.Float) ?
                                    new LoadFloatCommand(b, (Constant)bVar) :
                                    new LoadIntCommand(b, (Constant)bVar));

            store(bTempVar);
        } else {
            b = registerAllocator.getRegister((Variable) bVar);
        }

        Register dest = registerAllocator.getRegister(command.getDest());

        // conversion
        Variable aConTempVar = null;
        if(!a.getType().equals(dest.getType())){
            aConTempVar = getTempVariable(dest.getType());
            Register aConTempReg = load(aConTempVar);
            commandList.add(dest.getType().equals(Type.Float) ?
                                    new IntToFloatCommand(aConTempReg, a) :
                                    new FloatToIntCommand(aConTempReg, a));
        }
        if(!b.getType().equals(dest.getType())){
            Variable bConTempVar = getTempVariable(dest.getType());
            Register bConTempReg = load(bConTempVar);
            commandList.add(dest.getType().equals(Type.Float) ?
                                    new IntToFloatCommand(bConTempReg, b) :
                                    new FloatToIntCommand(bConTempReg, b));
            store(bConTempVar);
        }

        if(aConTempVar != null) store(aConTempVar);
        if (aTempVar != null) store(aTempVar);
        commandList.add(new BinaryMIPSCommand(dest, a, b, command.getOp(), a.getType().equals(Type.Float)));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }


}
