import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MIPSCommand {


}

class BinaryMIPSCommand extends MIPSCommand{
    private Register dest, a, b;
    private BinaryOperator op;
    private boolean isFloat;

    public BinaryMIPSCommand(Register dest, Register a, Register b, BinaryOperator op, boolean isFloat) {
        this.dest = dest;
        this.a = a;
        this.b = b;
        this.op = op;
        this.isFloat = isFloat;
    }

    @Override
    public String toString() {
        return "\t\t" + op.toString().toLowerCase() + ( isFloat ? ".s " : " ") + dest + ", " + a + ", " + b;
    }
}

class BinaryImmediateMIPSCommand extends MIPSCommand{
    private Register dest, a;
    private Constant b;
    private BinaryOperator op;

    public BinaryImmediateMIPSCommand(Register dest, Register a, Constant b, BinaryOperator op) {
        this.dest = dest;
        this.a = a;
        this.b = b;
        this.op = op;
    }

    @Override
    public String toString() {
        return "\t\t" +  op.toString().toLowerCase() + "iu" + " " + dest + ", " + a + ", " + b;
    }
}

class LoadMIPSCommand extends MIPSCommand {
    private Register dest;
    private Variable origin;
    private boolean isFloat;

    public LoadMIPSCommand(Register dest, Variable origin, boolean isFloat) {
        this.dest = dest;
        this.origin = origin;
        this.isFloat = isFloat;
    }

    @Override
    public String toString() {
        return "\t\t" +  "l" + (isFloat ? ".s" : "w") + " " + dest + ", " + origin;
    }
}

class StoreMIPSCommand extends MIPSCommand {
    private Register origin;
    private Register dest;
    private boolean isFloat;

    public StoreMIPSCommand(Register origin, Register dest, boolean isFloat) {
        this.origin = origin;
        this.dest = dest;
        this.isFloat = isFloat;
    }

    @Override
    public String toString() {
        return "\t\t" +  "s" + (isFloat ? ".s" : "w") + " " + origin + ", " + dest;
    }
}
class BranchMIPSCommand extends MIPSCommand {
    private Register a, b;
    private String label, op;
    private final static Map<String, String> INT_MAP = new HashMap<>();
    static{
        INT_MAP.put("brneq", "bne");
        INT_MAP.put("breq", "beq");
        INT_MAP.put("brgt", "bgt");
        INT_MAP.put("brgeq", "bge");
        INT_MAP.put("brlt", "blt");
        INT_MAP.put("brleq", "ble");
    }

    public BranchMIPSCommand(Register a, Register b, String label, String op) {
        this.a = a;
        this.b = b;
        this.label = label;
        this.op = INT_MAP.get(op);
    }

    @Override
    public String toString() {
        return op + " " + a + ", " + b + ", " + label;
    }

}

class FloatBranchMIPSCommand extends MIPSCommand{
    private Register a, b;
    private List<String> ops;
    private String label;

    private final static Map<String, List<String>> STRING_MAP = new HashMap<>();
    static{
        STRING_MAP.put("brneq", List.of("c.eq.s", "bc1f"));
        STRING_MAP.put("breq", List.of("c.eq.s", "bc1t"));
        STRING_MAP.put("brgt", List.of("c.le.s", "bc1f"));
        STRING_MAP.put("brgeq", List.of("c.lt.s", "bc1f"));
        STRING_MAP.put("brlt", List.of("c.lt.s", "bc1t"));
        STRING_MAP.put("brleq", List.of("c.le.s", "bc1t"));
    }

    public FloatBranchMIPSCommand(Register a, Register b, String label, String op) {
        this.a = a;
        this.b = b;
        this.label = label;
        this.ops = STRING_MAP.get(op);
    }
    @Override
    public String toString() {
        return ops.get(0) + " " + a + ", " + b + "\n" + ops.get(1) + " " + label;
    }

}

class JumpMIPSCommand extends MIPSCommand {
    private String label;

    public JumpMIPSCommand(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "\t\t" +  "j " + label;
    }
}

class ReturnMIPSCommand extends MIPSCommand {
    private Register addr;

    public ReturnMIPSCommand(){
        this(new Register("ra", Type.Integer));
    }

    public ReturnMIPSCommand(Register addr) {
        this.addr = addr;
    }

    @Override
    public String toString() {
        return "\t\t" +  "jr " + addr;
    }
}

class CallMIPSCommand extends MIPSCommand {
    private String label;

    public CallMIPSCommand(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "\t\t" +  "jal " + label;
    }
}

class LoadIntCommand extends MIPSCommand {
    private Register dest;
    private Constant constant;

    public LoadIntCommand(Register dest, Constant constant) {
        this.dest = dest;
        this.constant = constant;
    }

    @Override
    public String toString() {
        return "\t\t" +  "li " + dest + ", " + constant;
    }
}

class LoadFloatCommand extends MIPSCommand {
    private Register dest;
    private Constant constant;

    public LoadFloatCommand(Register dest, Constant constant) {
        this.dest = dest;
        this.constant = constant;
    }

    @Override
    public String toString() {
        return "\t\t" +  "li.s " + dest + ", " + constant;
    }
}

class LoadLabelAddressCommand extends MIPSCommand {
    private Register dest;
    private DataAddress label;

    public LoadLabelAddressCommand(Register dest, DataAddress label) {
        this.dest = dest;
        this.label = label;
    }

    @Override
    public String toString() {
        return "\t\t" +  "la " + dest + ", " + label;
    }
}

class MoveMIPSCommand extends MIPSCommand {
    private Register a, b;
    private boolean isFloat;

    public MoveMIPSCommand(Register a, Register b, boolean isFloat) {
        this.a = a;
        this.b = b;
        this.isFloat = isFloat;
    }

    @Override
    public String toString() {
        return "\t\t" +  "mov" +  (isFloat ? ".s ": "e ") + a + ", " + b;
    }

}

class LabelMIPSCommand extends MIPSCommand {
    private String label;

    public LabelMIPSCommand(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "\t\t" +  label + ":";
    }
}

class IntToFloatCommand extends MIPSCommand {
    private Register intRegister, floatRegister;

    public IntToFloatCommand(Register floatRegister, Register intRegister) {
        this.intRegister = intRegister;
        this.floatRegister = floatRegister;
    }

    @Override
    public String toString() {
        return new MoveToFloatCommand(intRegister, floatRegister) + "\n" + "\t\t" +  "cvt.s.w " + floatRegister + ", " + floatRegister;
    }
}

class FloatToIntCommand extends MIPSCommand {
    private Register intRegister, floatRegister;

    public FloatToIntCommand(Register intRegister, Register floatRegister) {
        this.intRegister = intRegister;
        this.floatRegister = floatRegister;
    }

    @Override
    public String toString() {
        return "\t\t" +  "cvt.w.s " + floatRegister + ", " + floatRegister + "\n" + new MoveFromFloatCommand(intRegister, floatRegister);
    }
}

class MoveToFloatCommand extends MIPSCommand {
    private Register intRegister, floatRegister;

    public MoveToFloatCommand(Register intRegister, Register floatRegister) {
        this.intRegister = intRegister;
        this.floatRegister = floatRegister;
    }

    @Override
    public String toString() {
        return "\t\t" +  "mtc1 " + intRegister + ", " + floatRegister;
    }
}

class MoveFromFloatCommand extends MIPSCommand {
    private Register intRegister, floatRegister;

    public MoveFromFloatCommand(Register intRegister, Register floatRegister) {
        this.intRegister = intRegister;
        this.floatRegister = floatRegister;
    }

    @Override
    public String toString() {
        return "\t\t" +  "mfc1 " + intRegister + ", " + floatRegister;
    }
}

class DataTypeMIPSCommand extends MIPSCommand{
    private String name, type, initializer;

    public DataTypeMIPSCommand(String name, String type, String initializer) {
        this.name = name;

        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public String toString(){
        return name + ":  ." + type + ", " + initializer ;
    }

}

class CommentMIPSCommand extends MIPSCommand{
    private String comment;

    public CommentMIPSCommand(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString(){
        return "#" + comment;
    }
}

class AssemblerDirectiveCommand extends MIPSCommand {
    private String directive;
    private List<String> arguments;

    public AssemblerDirectiveCommand(String directive, List<String> arguments) {
        this.directive = directive;
        this.arguments = arguments;
    }

    public AssemblerDirectiveCommand(String directive) {
        this(directive, List.of());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('.').append(directive).append(" ");
        for(var arg: arguments){
            builder.append(arg).append(" ");
        }
        return builder.toString();
    }

}

class SystemMIPSCommand extends  MIPSCommand {
    @Override
    public String toString() {
        return "syscall";
    }
}

