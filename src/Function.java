import java.util.*;

public class Function {
    private List<Command> commandList;
    private Map<Command, Integer> commandMap;
    private BasicBlocks controlFlowGraph;
    private Memory localMemory;
    private String funcName;
    private Class currClass;

    public Function(String name) {
        this.localMemory = new Memory(new Register("sp"));
        this.commandList = new ArrayList<>();
        this.controlFlowGraph = new BasicBlocks();
        this.commandMap = new HashMap<>();
        this.funcName = name;
        this.currClass = null;
    }

    public String getFuncName() {
        return funcName;
    }

    public void addLocalVar(Argument a){
        localMemory.declareVariable(a);
    }


    public void addCommand(Command c){
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

    public Command getCommand(Integer i){
        return commandList.get(i);
    }

    public Memory.Address getAddress(Argument arg){
        Memory.Address add;
        add = localMemory.getAddress(arg);
        // search for variable address in static memory
        if(add == null && currClass != null){
            add = currClass.getAddress(arg);
        }
        return add;
    }

    public Argument getArgument(String value){
        Argument arg;
        arg = localMemory.getArgument(value);
        // search for variable in static memory
        if(arg instanceof ConstantArgument && currClass != null){
            arg = currClass.getArgument(value);
        }
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
        Command curr = commandList.get(i);
        BasicBlocks.Block currBlock = curr.getBlock();
        List<Command> commands = currBlock.getCommands();

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
