import java.util.*;
import java.util.stream.Collectors;

public abstract class IRCommand {
    abstract Set<Variable> getUsed();
    abstract Set<Variable> getDecl();
    abstract void setBlock(BasicBlocks.Block block);
    abstract BasicBlocks.Block getBlock();


    static Set<Variable> extractVars(Set<Argument> args){
        return args.stream().filter(Objects::nonNull).filter(Variable.class::isInstance)
                .map(Variable.class::cast).collect(Collectors.toSet());
    }
}

class ConditionalBranchCommand extends IRCommand {
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

    public String getBranchCommand() {
        return branchCommand;
    }

    public Argument getA() {
        return a;
    }

    public Argument getB() {
        return b;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Set<Variable> getUsed() {
        return IRCommand.extractVars(Set.of(a, b));
    }

    @Override
    public Set<Variable> getDecl() {
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

    @Override
    public String toString() {
        return branchCommand + ", " + a + ", " + b + ", " + label;
    }

}

class GotoCommand extends IRCommand {
    private final String label;
    private BasicBlocks.Block block;

    public GotoCommand(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Set<Variable> getUsed() {
        return Set.of();
    }

    @Override
    public Set<Variable> getDecl() {
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

    @Override
    public String toString() {
        return "goto" + ", " + label + ", ,";
    }
}

class ReturnCommand extends IRCommand {
    private final Argument returnValue;
    private BasicBlocks.Block block;

    public ReturnCommand(Argument returnValue) {
        this.returnValue = returnValue;
    }
    public ReturnCommand() {
        this(null);
    }

    @Override
    public Set<Variable> getUsed() {
        Set<Argument> set = new HashSet<>();
        set.add(returnValue);
        return IRCommand.extractVars(set);
    }

    public Argument getReturnValue() {
        return returnValue;
    }

    @Override
    public Set<Variable> getDecl() {
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
    @Override
    public String toString() {
        if (returnValue != null)
            return "return, " + returnValue + ", ,";
        return "return, , ,";
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

class BinaryOperatorCommand extends IRCommand {
    private BinaryOperator op;
    private Argument a;
    private Argument b;
    private Variable dest;
    private BasicBlocks.Block block;

    public BinaryOperatorCommand(BinaryOperator op, Argument a, Argument b, Argument dest) {
        this.op = op;
        this.a = a;
        this.b = b;
        this.dest = (Variable) dest;
    }

    public BinaryOperator getOp() {
        return op;
    }

    public Argument getA() {
        return a;
    }

    public Argument getB() {
        return b;
    }

    public Variable getDest() {
        return dest;
    }

    @Override
    public Set<Variable> getUsed() {
        Set<Argument> set = new HashSet<>();
        set.add(a);
        set.add(b);
        return IRCommand.extractVars(set);
    }

    @Override
    public Set<Variable> getDecl() {
        return IRCommand.extractVars(Set.of(dest));
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

    @Override
    public String toString() {
        return op.getValue() + ", " + a + ", " + b + ", " + dest;
    }


}

class CallCommand extends IRCommand {
    private final String func;
    private final List<Argument> args;
    private BasicBlocks.Block block;

    public CallCommand(String func, List<Argument> args) {
        this.func = func;
        this.args = args;
    }

    @Override
    public Set<Variable> getUsed() {
        return extractVars(new HashSet<>(args));
    }

    @Override
    public Set<Variable> getDecl() {
        return Set.of();
    }

    public String getFunc() {
        return func;
    }

    public List<Argument> getArgs() {
        return args;
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
        return Objects.equals(func, that.func) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(func, block);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("call, ").append(func);
        for(Argument arg: args)
            builder.append(", ").append(arg);
        return builder.toString();
    }

}

class CallRCommand extends CallCommand {
    private final Argument var;
    private BasicBlocks.Block block;

    public CallRCommand(Argument var, String func, List<Argument> args) {
        super(func, args);
        this.var = var;
    }

    public Argument getVar() {
        return var;
    }

    @Override
    public Set<Variable> getDecl() {
        return extractVars(Set.of(var));
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        CallRCommand that = (CallRCommand) o;
        return Objects.equals(var, that.var) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), var, block);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("callr, ").append(var).append(", ").append(getFunc());
        for(Argument arg: getArgs())
            builder.append(", ").append(arg);
        return builder.toString();
    }
}

class ArrayLoadCommand extends IRCommand {
    private final Array arr;
    private final Variable var;
    private final Argument index;
    private BasicBlocks.Block block;

    public ArrayLoadCommand(Argument var, Array arr, Argument index) {
        this.var = (Variable)var;
        this.arr =  arr;
        this.index = index;
    }

    public Array getArray() {
        return arr;
    }

    public Variable getVar() {
        return var;
    }

    public Argument getIndex() {
        return index;
    }

    @Override
    public Set<Variable> getUsed() {
        return Set.of();
    }

    @Override
    public Set<Variable> getDecl() {
        return extractVars(Set.of(var));
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

    @Override
    public String toString() {
        return "array_load, " + var + ", " +  arr+ ", " + index;
    }
}

class ArrayStoreCommand extends IRCommand {
    private final Array arr;
    private final Argument index;
    private final Argument value;
    private BasicBlocks.Block block;

    public ArrayStoreCommand(Array arr, Argument index, Argument value) {
        this.value = value;
        this.arr = arr;
        this.index = index;
    }

    public Array getArray() {
        return arr;
    }

    public Argument getIndex() {
        return index;
    }

    public Argument getValue() {
        return value;
    }

    @Override
    public Set<Variable> getUsed() {
        return extractVars(Set.of(index, value));
    }

    @Override
    public Set<Variable> getDecl() {
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ArrayStoreCommand that = (ArrayStoreCommand) o;
        return Objects.equals(arr, that.arr) && Objects.equals(index, that.index) && Objects.equals(value, that.value) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arr, index, value, block);
    }

    @Override
    public String toString() {
        return "array_store, " + arr+ ", " + index + ", " + value;
    }
}

class AssignmentCommand extends IRCommand {
    private Argument var;
    private Integer size;
    private Argument value;
    private BasicBlocks.Block block;

    public AssignmentCommand(Argument var, Argument value) {
        this(var, 0, value);
    }
    public AssignmentCommand(Argument var, Integer size, Argument value) {
        this.var = var;
        this.size = size;
        this.value = value;
    }

    public Argument getVar() {
        return var;
    }

    public Integer getSize() {
        return size;
    }

    public Argument getValue() {
        return value;
    }

    @Override
    public Set<Variable> getUsed() {
        HashSet<Argument> temp = new HashSet<>();
        temp.add(value);
        return extractVars(temp);
    }

    @Override
    public Set<Variable> getDecl() {
        return extractVars(Set.of(var));
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

    @Override
    public String toString() {
        return "assign, " +  var + (size == 0?  "" :  ", " + size) + ", " + value;
    }

}

class LabelCommand extends IRCommand {
    private String label;
    private BasicBlocks.Block block;

    public LabelCommand(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Set<Variable> getUsed() {
        return Set.of();
    }

    @Override
    public Set<Variable> getDecl() {
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
    public String toString() {
        return label + ": ";
    }
}


