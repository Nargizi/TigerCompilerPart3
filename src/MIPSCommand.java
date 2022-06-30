import java.util.List;

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

    public BranchMIPSCommand(Register a, Register b, String label, String op) {
        this.a = a;
        this.b = b;
        this.label = label;
        this.op = op;
    }

    @Override
    public String toString() {
        if (a.getType().equals(Type.Float))
            // TODO: we need to discuss this
            return "";
        else
            return op + " " + a + ", " + b + ", " + label;
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
        return "\t\t" +  "li " + dest + " " + constant;
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
        return "\t\t" +  "li.s " + dest + " " + constant;
    }
}

class MoveMIPSCommand extends MIPSCommand {
    private Register a, b;

    public MoveMIPSCommand(Register a, Register b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return "\t\t" +  "move " + a + ", " + b;
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
        return "\t\t" +  "cvt.s.w " + floatRegister + ", " + intRegister;
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
        return "\t\t" +  "cvt.w.s " + intRegister + ", " + floatRegister;
    }
}

class DataTypeMIPSCommand extends MIPSCommand{
    private String name, type, initializer;

    public DataTypeMIPSCommand(String name, String type, String initializer) {
        this.name = name;

        this.type = type;
        this.initializer = initializer;
    }

    public DataTypeMIPSCommand(String name, String type) {
        this(name, type, "");
    }

    @Override
    public String toString(){
        return name + ":  ." + type + "  " + initializer ;
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

