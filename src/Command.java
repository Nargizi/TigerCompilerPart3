import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Command {

}

class ConditionalBranchCommand implements Command {
    public ConditionalBranchCommand(String branchCommand, String a, String b, String label) {}
}

class GotoCommand implements Command {
    public GotoCommand(String label) {}
}

class ReturnCommand implements Command {
    public ReturnCommand(String returnValue) {}
    public ReturnCommand() {}
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
    public BinaryOperatorCommand(BinaryOperator op, String a, String b, String dest) {}
}

class CallCommand implements Command {
    public CallCommand(String func, List<String> args) {}
}

class CallRCommand implements Command {
    public CallRCommand(String var, String func, List<String> args) {}
}

class ArrayLoadCommand implements Command {
    public ArrayLoadCommand(String var, String arr, String index) {}
}

class ArrayStoreCommand implements Command {
    public ArrayStoreCommand(String arr, String index, String value) {}
}

class AssignmentCommand implements Command {
    public AssignmentCommand(String var, String value) {}
    public AssignmentCommand(String var, String size, String value) {}
}

class LabelCommand implements Command {
    public LabelCommand(String label) {}
}