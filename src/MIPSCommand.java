public abstract class MIPSCommand {


}

class BinaryMIPSCommand extends MIPSCommand{
    private Register dest, a, b;
    private BinaryOperator op;

    public BinaryMIPSCommand(Register dest, Register a, Register b, BinaryOperator op) {
        this.dest = dest;
        this.a = a;
        this.b = b;
        this.op = op;
    }

    @Override
    public String toString() {
        return op.toString().toLowerCase() + " " + dest + ", " + a + ", " + b;
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
        return op.toString().toLowerCase() + "i" + " " + dest + ", " + a + ", " + b;
    }
}

class LoadMIPSCommand extends MIPSCommand {
    private Register dest;
    private Address origin;

    public LoadMIPSCommand(Register dest, Address origin) {
        this.dest = dest;
        this.origin = origin;
    }

    @Override
    public String toString() {
        return "lw" + " " + dest + ", " + origin;
    }
}

class StoreMIPSCommand extends MIPSCommand {
    private Register origin;
    private Address dest;

    public StoreMIPSCommand(Register origin, Address dest) {
        this.origin = origin;
        this.dest = dest;
    }

    @Override
    public String toString() {
        return "sw" + " " + origin + ", " + dest;
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
        return "j " + label;
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
        return "jr" + addr;
    }
}

class CallMIPSCommand extends MIPSCommand {
    private String label;

    public CallMIPSCommand(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "jal " + label;
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
        return "li " + dest + " " + constant;
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
        return "move " + a + ", " + b;
    }

}

class LabelMIPSCommand extends MIPSCommand {
    private String label;

    public LabelMIPSCommand(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label + ":";
    }
}

