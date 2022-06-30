import java.util.*;
import java.util.stream.Collectors;


public class Function {

    private final String funcName;
    private Class currClass;

    // memory for functions
    private final Stack localMemory;
    private final Stack argumentMemory;
    private final RegisterMemory intArgumentMemory;
    private final RegisterMemory floatArgumentMemory;

    private final Map<IRCommand, Integer> commandMap;
    private List<IRCommand> commandList;
    private final BasicBlocks controlFlowGraph;
    private int maxArgumentSize;


    // predefined registers
    final static List<Register> intArgumentRegisters = List.of("a0", "a1", "a2", "a3")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatArgumentRegisters = List.of("f12", "f14")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());



    public Function(String name) {
        this.funcName = name;
        this.currClass = null;
        this.maxArgumentSize = 4 * 4 + 2 * 8; // 4 int argument register + 2 float argument register

        // memory initialization
        this.localMemory = new Stack(new Register("sp", Type.Integer), true);
        this.argumentMemory = new Stack(new Register("fp", Type.Integer), false);
        this.intArgumentMemory = new RegisterMemory(intArgumentRegisters);
        this.floatArgumentMemory = new RegisterMemory(floatArgumentRegisters);

        this.commandList = new ArrayList<>();
        this.controlFlowGraph = new BasicBlocks();
        this.commandMap = new HashMap<>();

    }

    public String getFuncName() {
        return funcName;
    }

    public void addLocalVar(Variable v){
        if(argumentMemory.getAddress(v) == null)
            localMemory.declareVariable(v);
    }

    public void addArgument(Variable v) {
        if (v.getType().equals(Type.Integer)){
            intArgumentMemory.declareVariable(v);
        } else {
            floatArgumentMemory.declareVariable(v);
        }
        argumentMemory.declareVariable(v);
    }

    public void addCommand(IRCommand c){
        if (c instanceof CallCommand){
            int size = 0;
            for(var arg: ((CallCommand) c).getArgs()){
                size += arg.getType().getSize();
            }
            maxArgumentSize = Math.max(maxArgumentSize, size);
        } else if (c instanceof CallRCommand){
            int size = 0;
            for(var arg: ((CallRCommand) c).getArgs()){
                size += arg.getType().getSize();
            }
            maxArgumentSize = Math.max(maxArgumentSize, size);
        }
        c.setBlock(controlFlowGraph.getCurrentBlock());
        commandList.add(c);
        commandMap.put(c, commandMap.size());
        controlFlowGraph.addCommand(c);
    }

    public void startBasicBlock(String label){
        controlFlowGraph.startBasicBlock(label);
    }

    public void endBasicBlock(){
        controlFlowGraph.endBasicBlock();
    }

    public void endBasicBlock(String label, Boolean isConditional){
        controlFlowGraph.endBasicBlock(label, isConditional);
    }

    public BasicBlocks getControlFlowGraph() {
        return controlFlowGraph;
    }

    public IRCommand getCommand(Integer i){
        return commandList.get(i);
    }

    public int getMaxArgumentSize() {
        return maxArgumentSize;
    }

    public int getLocalMemorySize() {
        return localMemory.getSize();
    }

    public Address getLocalAddress(Variable arg){
        Address add = localMemory.getAddress(arg);
        if(add != null)
            add = new Address(add.getStart(), add.getOffset() + getMaxArgumentSize());
        else
            add = argumentMemory.getAddress(arg);
        return add;
    }

    public DataAddress getGlobalAddress(Variable arg){
        // search for variable address in static memory
        return currClass.getAddress(arg);
    }

    public Register getAddress(Variable arg){
        Address add = getLocalAddress(arg);
        if(add == null)
            return getGlobalAddress(arg);
        return add;
    }

    public Register getRegister(Variable arg){
        if (arg.getType().equals(Type.Float))
            return floatArgumentMemory.getAddress(arg);
        return intArgumentMemory.getAddress(arg);
    }

    public Argument getVariable(String name){
        Argument arg = localMemory.getVariable(name);
        if (arg == null)
            arg = argumentMemory.getVariable(name);
        if (arg == null)
            arg = currClass.getVariable(name);
        if (arg == null)
            arg = new Constant(name);
        return arg;
    }

    public Integer getNumCommands(){
        return commandList.size();
    }

    public Class getCurrClass() {
        return currClass;
    }

    public void setCurrClass(Class currClass) {
        this.currClass = currClass;
    }

    public Set<Integer> getFollowSet(Integer i){
        IRCommand curr = commandList.get(i);
        BasicBlocks.Block currBlock = curr.getBlock();
        List<IRCommand> commands = currBlock.getCommands();

        // get indices of commands which might follow current command in program flow
        if (commands.indexOf(curr) == commands.size() - 1){
            Set<Integer> followSet = new HashSet<>();

            for(var blocks: currBlock.getNextBlocks()){
                followSet.add(commandMap.get(blocks.getCommands().get(0)));
            }

            return followSet;
        }

        return Set.of(commandMap.get(curr) + 1);
    }
}
