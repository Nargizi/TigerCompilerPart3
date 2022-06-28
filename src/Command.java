import java.util.*;
import java.util.stream.Collectors;

public interface Command {
    Set<Argument> getUsed();
    Set<Argument> getDecl();
    BasicBlocks.Block getBlock();
    void setBlock(BasicBlocks.Block block);

    static Set<Argument> extractVars(Set<Argument> args){
        return args.stream().filter(x -> !(x instanceof ConstantArgument)).collect(Collectors.toSet());
    }
}

class ConditionalBranchCommand implements Command {
    private String branchCommand;
    private Argument a;
    private Argument b;
    private String label;
    private BasicBlocks.Block block;

    public ConditionalBranchCommand(String branchCommand, Argument a, Argument b, String label) {
        this.branchCommand = branchCommand;
        this.a = a;
        this.b = b;
        this.label = label;
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(Set.of(a, b));
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of();
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionalBranchCommand that = (ConditionalBranchCommand) o;
        return Objects.equals(branchCommand, that.branchCommand) && Objects.equals(a, that.a) && Objects.equals(b, that.b) && Objects.equals(label, that.label) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchCommand, a, b, label, block);
    }
}

class GotoCommand implements Command {
    private String label;
    private BasicBlocks.Block block;

    public GotoCommand(String label) {
        this.label = label;
    }

    @Override
    public Set<Argument> getUsed() {
        return Set.of();
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GotoCommand that = (GotoCommand) o;
        return Objects.equals(label, that.label) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, block);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }
}

class ReturnCommand implements Command {
    private Argument returnValue;
    private BasicBlocks.Block block;

    public ReturnCommand(Argument returnValue) {
        this.returnValue = returnValue;
    }
    public ReturnCommand() {
        this(null);
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(Set.of(returnValue));
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of();
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnCommand that = (ReturnCommand) o;
        return Objects.equals(returnValue, that.returnValue) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnValue, block);
    }
}

enum BinaryOperator {
    ADD("add"),
    SUB("sub"),
    MUL("mult"),
    DIV("div"),
    AND("and"),
    OR("or");

    public final String value;
    private static final Map<String, BinaryOperator> lookup = new HashMap<String, BinaryOperator>();

    static {
        for (BinaryOperator op : BinaryOperator.values()) {
            lookup.put(op.getValue(), op);
        }
    }

    BinaryOperator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BinaryOperator get(String value) {
        return lookup.get(value);
    }
}

class BinaryOperatorCommand implements Command {
    private BinaryOperator op;
    private Argument a;
    private Argument b;
    private Argument dest;
    private BasicBlocks.Block block;

    public BinaryOperatorCommand(BinaryOperator op, Argument a, Argument b, Argument dest) {
        this.op = op;
        this.a = a;
        this.b = b;
        this.dest = dest;
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(Set.of(a, b));
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of(dest);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryOperatorCommand that = (BinaryOperatorCommand) o;
        return op == that.op && Objects.equals(a, that.a) && Objects.equals(b, that.b) && Objects.equals(dest, that.dest) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, a, b, dest, block);
    }
}

class CallCommand implements Command {
    private String func;
    private List<Argument> args;
    private BasicBlocks.Block block;

    public CallCommand(String func, List<Argument> args) {
        this.func = func;
        this.args = args;
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(new HashSet<>(args));
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of();
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallCommand that = (CallCommand) o;
        return Objects.equals(func, that.func) && Objects.equals(args, that.args) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(func, args, block);
    }
}

class CallRCommand implements Command {
    private Argument var;
    private String func;
    private List<Argument> args;
    private BasicBlocks.Block block;

    public CallRCommand(Argument var, String func, List<Argument> args) {
        this.var = var;
        this.func = func;
        this.args = args;
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(new HashSet<>(args));
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of(var);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallRCommand that = (CallRCommand) o;
        return Objects.equals(var, that.var) && Objects.equals(func, that.func) && Objects.equals(args, that.args) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var, func, args, block);
    }
}

class ArrayLoadCommand implements Command {
    private Argument var;
    private Argument arr;
    private Argument index;
    private ArrayElementArgument arrayElement;
    private BasicBlocks.Block block;

    public ArrayLoadCommand(Argument var, Argument arr, Argument index) {
        this.var = var;
        this.arr =  arr;
        this.index = index;
        this.arrayElement = new ArrayElementArgument(this.arr, this.index);
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(Set.of(arrayElement));
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of(var);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayLoadCommand that = (ArrayLoadCommand) o;
        return Objects.equals(var, that.var) && Objects.equals(arr, that.arr) && Objects.equals(index, that.index) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var, arr, index, block);
    }
}

class ArrayStoreCommand implements Command {
    private Argument arr;
    private Argument index;
    private Argument value;
    private BasicBlocks.Block block;

    public ArrayStoreCommand(Argument arr, Argument index, Argument value) {
        this.arr = arr;
        this.index = index;
        this.value = value;
    }

    @Override
    public Set<Argument> getUsed() {
        return Command.extractVars(Set.of(value, index));
    }

    @Override
    public Set<Argument> getDecl() {
        // Temp
        // not sure if array assignment counts as var (re)declaration
//        return Set.of(arr + "[" + index + "]");
        return Set.of();
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayStoreCommand that = (ArrayStoreCommand) o;
        return Objects.equals(arr, that.arr) && Objects.equals(index, that.index) && Objects.equals(value, that.value) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arr, index, value, block);
    }
}

class AssignmentCommand implements Command {
    private Argument var;
    private Argument size;
    private Argument value;
    private BasicBlocks.Block block;

    public AssignmentCommand(Argument var, Argument value) {
        this(var, new IntArgument(null), value);
    }
    public AssignmentCommand(Argument var, Argument size, Argument value) {
        this.var = var;
        this.size = size;
        this.value = value;
    }

    @Override
    public Set<Argument> getUsed() {
        HashSet<Argument> temp = new HashSet<>();
        temp.add(value);
        temp.add(size);
        return Command.extractVars(temp);
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of(var);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentCommand that = (AssignmentCommand) o;
        return Objects.equals(var, that.var) && Objects.equals(size, that.size) && Objects.equals(value, that.value) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var, size, value, block);
    }
}

class LabelCommand implements Command {
    private String label;
    private BasicBlocks.Block block;

    public LabelCommand(String label) {
        this.label = label;
    }

    @Override
    public Set<Argument> getUsed() {
        return Set.of();
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of();
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }
}

class LoadCommand implements Command {
    private Argument dest;
    private Argument origin;
    private BasicBlocks.Block block;

    public LoadCommand(Argument dest, Argument origin){
        this.dest = dest;
        this.origin = origin;
    }

    @Override
    public Set<Argument> getUsed() {
        return Set.of(dest, origin);
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of(dest);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }
}

class StoreCommand implements Command {
    private Argument dest;
    private Argument origin;
    private BasicBlocks.Block block;

    public StoreCommand(Argument dest, Argument origin){
        this.dest = dest;
        this.origin = origin;
    }

    @Override
    public Set<Argument> getUsed() {
        return Set.of(dest, origin);
    }

    @Override
    public Set<Argument> getDecl() {
        return Set.of(dest);
    }

    @Override
    public BasicBlocks.Block getBlock() {
        return block;
    }

    @Override
    public void setBlock(BasicBlocks.Block block) {
        this.block = block;
    }
}

