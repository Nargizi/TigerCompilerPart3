import java.util.*;
import java.util.stream.Collectors;


public class Function {

    private final String funcName;
    private Class currClass;

    // memory for functions
    private final Stack localMemory;
    private final RegisterMemory intArgumentMemory;
    private final RegisterMemory floatArgumentMemory;

    private final Map<IRCommand, Integer> commandMap;
    private List<IRCommand> commandList;
    private final BasicBlocks controlFlowGraph;


    // predefined registers
    final static List<Register> intArgumentRegisters = List.of("a0", "a1", "a2", "a3")
            .stream().map(s -> new Register(s, Type.Integer)).collect(Collectors.toList());

    final static List<Register> floatArgumentRegisters = List.of("f12", "f14")
            .stream().map(s -> new Register(s, Type.Float)).collect(Collectors.toList());



    public Function(String name) {
        this.funcName = name;
        this.currClass = null;

        // memory initialization
        this.localMemory = new Stack(new Register("sp", Type.Integer));
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
        localMemory.declareVariable(v);
    }

    public void addArgument(Variable v) {
        if (v.getType().equals(Type.Integer)){
            if(!intArgumentMemory.declareVariable(v))
                localMemory.declareVariable(v);
        } else {
            if(!floatArgumentMemory.declareVariable(v))
                localMemory.declareVariable(v);
        }
    }

    public void addCommand(IRCommand c){
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

    public Address getAddress(Variable arg){
        Address add;
        add = localMemory.getAddress(arg);
        // search for variable address in static memory
        if(add == null && currClass != null){
            add = currClass.getAddress(arg);
        }
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
            arg = floatArgumentMemory.getVariable(name);
        if (arg == null)
            arg = intArgumentMemory.getVariable(name);
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
