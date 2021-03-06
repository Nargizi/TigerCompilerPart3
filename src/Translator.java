import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

    final static List<Register> intTempRegisters = Stream
            .of("t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9")
            .map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatTempRegisters = Stream
            .of("f4", "f6", "f8", "f10", "f16", "f18")
            .map(s -> new Register(s, Type.Float)).collect(Collectors.toList());

    final static Register SP = new Register("sp", Type.Integer);
    final static Register FP = new Register("fp", Type.Integer);
    final static Register RA = new Register("ra", Type.Integer);
    final static Register RETURN_INT = new Register("v0", Type.Integer);
    final static Register RETURN_FLOAT = new Register("f0", Type.Integer);

    private final RegisterMemory savedIntTempMemory;
    private final RegisterMemory savedFloatTempMemory;
    private Integer tempVariableCounter;
    private final RegisterAllocator registerAllocator;


    public Translator(RegisterAllocator registerAllocator){
        this.savedIntTempMemory = new RegisterMemory(intTempRegisters);
        this.savedFloatTempMemory = new RegisterMemory(floatTempRegisters);
        this.registerAllocator = registerAllocator;
        tempVariableCounter = 0;
    }

    public Variable getTempVariable(Type type){
        return new Variable(tempVariableCounter++ + "_INTERNAL_VAR", type);
    }

    public String getFunctionExitLabel(Function func) {
        return "__EXIT__" + func.getFuncName();
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
                if(add.getValue().getType().equals(Type.Float))
                    commandList.add(new DataTypeMIPSCommand(add.getValue().getName(), "float", "0.0"));
                else
                    commandList.add(new DataTypeMIPSCommand(add.getValue().getName(),  "word", "0"));
        }
        commandList.add(new AssemblerDirectiveCommand("text"));
        commandList.add(new AssemblerDirectiveCommand("globl", List.of("main")));
        for(var func: c.getFunctions().values()){
            commandList.addAll(translate(func));
        }
        return commandList;
    }
    
    private List<MIPSCommand> prolog(Function f){
        List<MIPSCommand> commandList= new LinkedList<>();
        commandList.add(new CommentMIPSCommand("PROLOG"));
        int argStorage = f.getMaxArgumentSize();
        int savedRegister = RegisterAllocator.intSavedRegisters.size() * 4 + RegisterAllocator.floatSavedRegisters.size() * 8;
        int returnAddress = 4;
        int frameRegister = 4;
        int stackSize = argStorage + savedRegister + returnAddress + f.getLocalMemorySize() + frameRegister;

        Variable temp = getTempVariable(Type.Integer);
        Register tempReg = load(temp);
        commandList.add(new MoveMIPSCommand(tempReg, SP, false));

        commandList.add(new BinaryImmediateMIPSCommand(SP, SP, new Constant(String.valueOf(-stackSize)), BinaryOperator.ADD));
        commandList.add(new StoreMIPSCommand(RA, new Address(SP, stackSize - returnAddress), false));
        commandList.add(new StoreMIPSCommand(FP, new Address(SP, stackSize - returnAddress - frameRegister), false));

        commandList.add(new MoveMIPSCommand(FP, tempReg, false));
        store(temp);

        for(int i = 0; i < RegisterAllocator.intSavedRegisters.size(); ++i){
            commandList.add(new StoreMIPSCommand(RegisterAllocator.intSavedRegisters.get(i),
                                                 new Address(SP, argStorage + f.getLocalMemorySize() + i * 4), false));
        }

        for(int i = 0; i < RegisterAllocator.floatSavedRegisters.size(); ++i){
            commandList.add(new StoreMIPSCommand(RegisterAllocator.floatSavedRegisters.get(i),
                                                 new Address(SP, argStorage + f.getLocalMemorySize()
                                                         + i * 8 + RegisterAllocator.intSavedRegisters.size() * 4),
                                                 true));
        }
        return commandList;
    }

    public List<MIPSCommand> translate(Function f){
        List<MIPSCommand> commandList= new LinkedList<>();
        registerAllocator.reset(f);
        commandList.add(new LabelMIPSCommand(f.getFuncName()));
        commandList.addAll(prolog(f));
        for(int i = 1; i < f.getNumCommands(); ++i){
            commandList.add(new CommentMIPSCommand(f.getCommand(i).toString()));
            commandList.addAll(translate(f.getCommand(i), f));
        }
        commandList.addAll(epilogue(f));
        return commandList;
    }

    private List<MIPSCommand> epilogue(Function f){
        List<MIPSCommand> commandList= new LinkedList<>();
        commandList.add(new CommentMIPSCommand("EPILOGUE"));
        commandList.add(new LabelMIPSCommand(getFunctionExitLabel(f)));

        int argStorage = f.getMaxArgumentSize();
        int savedRegister = RegisterAllocator.intSavedRegisters.size() * 4 + RegisterAllocator.floatSavedRegisters.size() * 8;
        int returnAddress = 4;
        int frameRegister = 4;
        int stackSize = argStorage + savedRegister + returnAddress + f.getLocalMemorySize() + frameRegister;


        commandList.add(new LoadMIPSCommand(RA, new Address(SP, stackSize - returnAddress), false));
        commandList.add(new LoadMIPSCommand(FP, new Address(SP, stackSize - returnAddress - frameRegister), false));


        for(int i = 0; i < RegisterAllocator.intSavedRegisters.size(); ++i){
            commandList.add(new LoadMIPSCommand(RegisterAllocator.intSavedRegisters.get(i),
                                                 new Address(SP, argStorage + f.getLocalMemorySize() + i * 4), false));
        }

        for(int i = 0; i < RegisterAllocator.floatSavedRegisters.size(); ++i){
            commandList.add(new LoadMIPSCommand(RegisterAllocator.floatSavedRegisters.get(i),
                                                 new Address(SP, argStorage + f.getLocalMemorySize()
                                                         + i * 8 + RegisterAllocator.intSavedRegisters.size() * 4),
                                                 true));
        }

        commandList.add(new BinaryImmediateMIPSCommand(SP, SP, new Constant(String.valueOf(stackSize)), BinaryOperator.ADD));
        commandList.add(new ReturnMIPSCommand(RA));

        return commandList;
    }

    public List<MIPSCommand> translate(IRCommand command, Function f){
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
            return translateReturnCommand((ReturnCommand) command, f);
        if(command instanceof CallRCommand)
            return translateCallRCommand((CallRCommand) command, f);
        if(command instanceof CallCommand)
            return translateCallCommand((CallCommand) command, f);
        if(command instanceof ArrayStoreCommand)
            return translateArrayStoreCommand((ArrayStoreCommand) command);
        if(command instanceof ArrayLoadCommand)
            return translateArrayLoadCommand((ArrayLoadCommand) command);
        return List.of();
    }

    private List<MIPSCommand> translateJumpCommand(GotoCommand command){
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));
        commandList.addAll(registerAllocator.exitCommand(command));
        commandList.add(new JumpMIPSCommand(command.getLabel()));
        return commandList;
    }

    private List<MIPSCommand> translateLabelCommand(LabelCommand command){
        List<MIPSCommand> commandList = new LinkedList<>();
        commandList.add(new LabelMIPSCommand(command.getLabel()));
        commandList.addAll(registerAllocator.enterCommand(command));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateArrayStoreCommand(ArrayStoreCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));

        Variable array = command.getArray();
        Variable tempIndexVar = null, tempSizeVar = null;
        Address arrayAddress = registerAllocator.func.getLocalAddress(command.getArray());

        Variable tempAddressVar = null;
        if (arrayAddress == null) {
            DataAddress dataAddress = registerAllocator.func.getGlobalAddress(array);
            tempAddressVar = getTempVariable(Type.Integer);

            arrayAddress = new Address(load(tempAddressVar), 0);
            commandList.add(new LoadLabelAddressCommand(arrayAddress.getStart(), dataAddress));
        }

        if(command.getIndex() instanceof Constant) {
            int index = Integer.parseInt(((Constant) command.getIndex()).getValue());
            int existingOffset =arrayAddress.getOffset();
            arrayAddress = new Address(arrayAddress.getStart(), index * array.getType().getSize() + existingOffset);
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

        if (tempAddressVar != null) store(tempAddressVar);
        if (tempVariable != null) store(tempVariable);
        if (tempIndexVar != null) store(tempIndexVar);
        if(tempSizeVar != null) store(tempSizeVar);

        commandList.add(new StoreMIPSCommand(valueRegister,
                arrayAddress,
                array.getType().equals(Type.Float)));

        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateArrayLoadCommand(ArrayLoadCommand command){
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));

        Variable array = command.getArray();
        Address arrayAddress = registerAllocator.func.getLocalAddress(command.getArray());
        Variable tempIndexVar = null, tempSizeVar = null;

        Variable tempAddressVar = null;
        if (arrayAddress == null) {
            DataAddress dataAddress = registerAllocator.func.getGlobalAddress(array);
            tempAddressVar = getTempVariable(Type.Integer);
            arrayAddress = new Address(load(tempAddressVar), 0);
            commandList.add(new LoadLabelAddressCommand(arrayAddress.getStart(), dataAddress));
        }

        if(command.getIndex() instanceof Constant) {
            int index = Integer.parseInt(((Constant) command.getIndex()).getValue());
            arrayAddress = new Address(arrayAddress.getStart(), index * array.getType().getSize() + arrayAddress.getOffset());
        } else {
            Register index = registerAllocator.getRegister((Variable) command.getIndex());
//            System.out.println(command + " " + command.getIndex());
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

        if (tempAddressVar != null) store(tempAddressVar);
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
                commandList.add(b.getType().equals(Type.Float) ?
                                        new LoadFloatCommand(bRegister, (Constant) b) :
                                        new LoadIntCommand(bRegister, (Constant) b));
            } else {
                bRegister = registerAllocator.getRegister((Variable)b);
            }

            commandList.add(b.getType().equals(Type.Float) ?
                    new FloatToIntCommand(a, bRegister) :
                    new IntToFloatCommand(a, bRegister));
        } else {
            Register bRegister = registerAllocator.getRegister((Variable)b);
            commandList.add(new MoveMIPSCommand(a, bRegister, a.getType().equals(Type.Float)));
        }

        return commandList;
    }

    private List<MIPSCommand> translateArrayArrayAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>();

        Array a = (Array)command.getVar();
        Array b = (Array)command.getValue();
        int size = a.getSize() / a.getType().getSize();
        Address aBaseAddress = registerAllocator.func.getLocalAddress(a);
        Address bBaseAddress = registerAllocator.func.getLocalAddress(b);

        Variable aTempAddressVar = null;
        if(aBaseAddress == null) {
            DataAddress dataAddress = registerAllocator.func.getGlobalAddress(a);
            aTempAddressVar = getTempVariable(Type.Integer);
            aBaseAddress = new Address(load(aTempAddressVar), 0);
            commandList.add(new LoadLabelAddressCommand(aBaseAddress.getStart(), dataAddress));
        }

        Variable bTempAddressVar = null;
        if(bBaseAddress == null) {
            DataAddress dataAddress = registerAllocator.func.getGlobalAddress(b);
            bTempAddressVar = getTempVariable(Type.Integer);
            bBaseAddress = new Address(load(bTempAddressVar), 0);
            commandList.add(new LoadLabelAddressCommand(bBaseAddress.getStart(), dataAddress));
        }

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

            if (aTempAddressVar != null) store(aTempAddressVar);
            if (bTempAddressVar != null) store(bTempAddressVar);
            store(bTemp);
        }

        return commandList;
    }

    private List<MIPSCommand> translateArrayVarAssignment(AssignmentCommand command) {
        List<MIPSCommand> commandList = new LinkedList<>();
        int size = command.getSize();
        Variable a = (Variable)command.getVar();
        Address aBaseAddress = registerAllocator.func.getLocalAddress(a);

        Variable aTempAddressVar = null;
        if(aBaseAddress == null) {
            DataAddress dataAddress = registerAllocator.func.getGlobalAddress(a);
            aTempAddressVar = getTempVariable(Type.Integer);
            aBaseAddress = new Address(load(aTempAddressVar), 0);
            commandList.add(new LoadLabelAddressCommand(aBaseAddress.getStart(), dataAddress));
        }

        for (int i = 0; i < size; ++i) {
            Address destination = new Address(aBaseAddress.getStart(), aBaseAddress.getOffset() + i * a.getType().getSize());
            Argument b = command.getValue();

            Register bRegister;
            Variable bVariable = null;
            if (b instanceof Constant) {
                bVariable = getTempVariable(b.getType());
                bRegister = load(bVariable);
                commandList.add((bRegister.getType().equals(Type.Float) ?
                        new LoadFloatCommand(bRegister, (Constant) b) :
                        new LoadIntCommand(bRegister, (Constant) b)));
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

            if (aTempAddressVar != null) store(aTempAddressVar);
            if (bVariable != null) store(bVariable);
            commandList.add(new StoreMIPSCommand(bRegister, destination, bRegister.getType().equals(Type.Float)));
        }
        return  commandList;
    }


    private List<MIPSCommand> translateReturnCommand(ReturnCommand command, Function f){
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));
        if(command.getReturnValue() != null){
            if(command.getReturnValue() instanceof Constant){
                commandList.add(command.getReturnValue().getType().equals(Type.Float) ?
                                        new LoadFloatCommand(RETURN_FLOAT, (Constant) command.getReturnValue()) :
                                        new LoadIntCommand(RETURN_INT, (Constant) command.getReturnValue()));
            } else {
                commandList.add(command.getReturnValue().getType().equals(Type.Float) ?
                                        new MoveMIPSCommand(RETURN_FLOAT, registerAllocator.getRegister((Variable) command.getReturnValue()), true):
                                        new MoveMIPSCommand(RETURN_INT, registerAllocator.getRegister((Variable) command.getReturnValue()), false));
            }
        }
        commandList.addAll(registerAllocator.exitCommand(command));
        commandList.add(new JumpMIPSCommand(getFunctionExitLabel(f)));
        return commandList;
    }



    private List<MIPSCommand> functionCall(CallCommand command, Function f) {
        List<MIPSCommand> commandList = new LinkedList<>(registerAllocator.enterCommand(command));
        int offset = 0, numInt = 0, numFloat = 0;
        for(int i = 0; i < command.getArgs().size(); ++i){
            Variable  arg;
            Register r;
            if (command.getArgs().get(i) instanceof Constant constant){
                arg = getTempVariable(constant.getType());
                r = load(arg);
                commandList.add(constant.getType().equals(Type.Float) ?
                                        new LoadFloatCommand(r, constant) :
                                        new LoadIntCommand(r, constant));
            }else {
                arg = (Variable) command.getArgs().get(i);
                r = registerAllocator.getRegister(arg);
                if(r == null) {
                    r = load(arg);
                    commandList.add(new LoadMIPSCommand(r, f.getAddress(arg), r.getType().equals(Type.Float)));
                }
            }

            commandList.add(new StoreMIPSCommand(r, new Address(SP, offset), arg.getType().equals(Type.Float)));

            if(arg.getType().equals(Type.Float) && numFloat < 2){
                commandList.add(new MoveMIPSCommand(new Register("f" + ((2 * numFloat++) + 12), Type.Float), r, true));
            } else if(arg.getType().equals(Type.Integer) && numInt < 4){
                commandList.add(new MoveMIPSCommand(new Register("a" + numInt++,  Type.Integer), r, false));
            }
            offset += arg.getSize();
            store(arg);
        }
        switch (command.getFunc()) {
            case "printi" -> {
                commandList.add(new LoadIntCommand(RETURN_INT, new Constant("1")));
                commandList.add(new SystemMIPSCommand());
                commandList.add(new LoadIntCommand(RETURN_INT, new Constant("11")));
                commandList.add(new LoadIntCommand(new Register("a0", Type.Integer), new Constant("10")));
                commandList.add(new SystemMIPSCommand());
            }
            case "printf" -> {
                Argument arg = command.getArgs().get(0);
                if(!arg.getType().equals(Type.Float)){
                    commandList.add(new IntToFloatCommand(new Register("f12", Type.Float), new Register("a0", Type.Integer)));
                }
                commandList.add(new LoadIntCommand(RETURN_INT, new Constant("2")));
                commandList.add(new SystemMIPSCommand());
                commandList.add(new LoadIntCommand(RETURN_INT, new Constant("11")));
                commandList.add(new LoadIntCommand(new Register("a0", Type.Integer), new Constant("10")));
                commandList.add(new SystemMIPSCommand());
            }
            case "exit" -> {
                commandList.add(new LoadIntCommand(RETURN_INT, new Constant("17")));
                commandList.add(new SystemMIPSCommand());
            }
            case "not" -> {
                Register arg = new Register("a0", Type.Integer);
                commandList.add(new BinaryMIPSCommand(RETURN_INT, arg, arg, BinaryOperator.NOR, false));
            }
            default -> commandList.add(new CallMIPSCommand(command.getFunc()));
        }
        return commandList;
    }
    private List<MIPSCommand> translateCallCommand(CallCommand command, Function f){
        List<MIPSCommand> commandList =  functionCall(command, f);
        commandList.addAll(functionExit(f));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> translateCallRCommand(CallRCommand command, Function f){
        List<MIPSCommand> commandList = functionCall(command, f);
        Variable var = (Variable) command.getVar();
        commandList.addAll(functionExit(f));
        if(var.getType().equals(Type.Float))
            commandList.add(new MoveMIPSCommand(registerAllocator.getRegister(var), RETURN_FLOAT, true));
        else
            commandList.add(new MoveMIPSCommand(registerAllocator.getRegister(var), RETURN_INT, false));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }

    private List<MIPSCommand> functionExit(Function f){
        List<MIPSCommand> commandList = new LinkedList<>();
        var intArguments = f.getIntArguments();
        var floatArguments = f.getFloatArguments();
        for(var arg: intArguments.entrySet()){
            commandList.add(new LoadMIPSCommand(arg.getValue(), f.getLocalAddress(arg.getKey()), false));
        }
        for(var arg: floatArguments.entrySet()){
            commandList.add(new LoadMIPSCommand(arg.getValue(), f.getLocalAddress(arg.getKey()), true));
        }
        return commandList;
    }


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

        if(!a.getType().equals(b.getType())){
            Variable tempVar = getTempVariable(Type.Float);
            Register tempReg = load(tempVar);
            if(!a.getType().equals(Type.Float)){
                commandList.add(new IntToFloatCommand(tempReg, a));
                a = tempReg;
            } else {
                commandList.add(new IntToFloatCommand(tempReg, b));
                b = tempReg;
            }
            store(tempVar);
        }
        if(a.getType().equals(Type.Float))
            commandList.add(new FloatBranchMIPSCommand(a, b, command.getLabel(), command.getBranchCommand()));
        else
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
            a = aConTempReg;
        }
        if(!b.getType().equals(dest.getType())){
            Variable bConTempVar = getTempVariable(dest.getType());
            Register bConTempReg = load(bConTempVar);
            commandList.add(dest.getType().equals(Type.Float) ?
                                    new IntToFloatCommand(bConTempReg, b) :
                                    new FloatToIntCommand(bConTempReg, b));
            b = bConTempReg;
            store(bConTempVar);
        }

        if(aConTempVar != null) store(aConTempVar);
        if (aTempVar != null) store(aTempVar);
        commandList.add(new BinaryMIPSCommand(dest, a, b, command.getOp(), a.getType().equals(Type.Float)));
        commandList.addAll(registerAllocator.exitCommand(command));
        return commandList;
    }


}
