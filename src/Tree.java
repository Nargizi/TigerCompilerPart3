
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
public class Tree extends IRBaseListener {

    private Class currClass;
    private Function currFunction;

    public Tree() {
        currClass = null;
        currFunction = null;
    }

    @Override
    public void enterProgram(IRParser.ProgramContext ctx) {
        currClass = new Class(ctx.ID(0).getText());
    }

    // TODO: ARRAYS
    @Override
    public void enterStatic_float_list(IRParser.Static_float_listContext ctx) {
        for (TerminalNode varNode : ctx.ID()) {
            currClass.addStaticVar(new IntArgument(varNode.getText()));
        }
    }

    @Override
    public void enterStatic_int_list(IRParser.Static_int_listContext ctx) {
        for (TerminalNode varNode : ctx.ID()) {
            currClass.addStaticVar(new FloatArgument(varNode.getText()));
        }
    }

    @Override
    public void enterFunction(IRParser.FunctionContext ctx) {
        currFunction = new Function(ctx.ID(0).getText());
        currClass.addFunction(currFunction);
    }

    // TODO: 4 args should be in registers
    @Override
    public void enterArgs_list(IRParser.Args_listContext ctx) {
        for (int i = 0; i < ctx.ID().size(); i += 2) {
            boolean isInt = ctx.ID(i).getText().equals("int");
            String name = ctx.ID(i + 1).getText();

            Argument arg = isInt ? new IntArgument(name) : new FloatArgument(name);
            currFunction.addLocalVar(arg);
        }
    }

    // TODO: arrays
    @Override
    public void enterFloat_list(IRParser.Float_listContext ctx) {
        for (TerminalNode varNode : ctx.ID()) {
            currFunction.addLocalVar(new IntArgument(varNode.getText()));
        }
    }

    @Override
    public void enterInt_list(IRParser.Int_listContext ctx) {
        for (TerminalNode varNode : ctx.ID()) {
            currFunction.addLocalVar(new FloatArgument(varNode.getText()));
        }
    }

    @Override
    public void enterGoto_branch_operator(IRParser.Goto_branch_operatorContext ctx) {
        String jumpLabel = ctx.ID().getText();
        Command command = new GotoCommand(jumpLabel);

        currFunction.addCommand(command);
        currFunction.endBasicBlock(jumpLabel, false);
    }

    @Override
    public void exitCond_branch_operators(IRParser.Cond_branch_operatorsContext ctx) {
        String commandName = ctx.start.getText();
        String jumpLabel = ctx.ID().getText();
        String a = ctx.alnum(0).getText();
        String b = ctx.alnum(1).getText();
        Command command = new ConditionalBranchCommand(commandName, currFunction.getArgument(a),
                currFunction.getArgument(b), jumpLabel);

        currFunction.addCommand(command);
        currFunction.endBasicBlock(jumpLabel, true);
    }

    @Override
    public void exitReturn_operators(IRParser.Return_operatorsContext ctx) {
        // TODO: Implement return command, cause don't remember shit atm
        Command command = ctx.alnum() == null ?
                new ReturnCommand(currFunction.getArgument(ctx.alnum().getText())) :
                new ReturnCommand();

        currFunction.addCommand(command);
        currFunction.endBasicBlock();
    }

    @Override
    public void exitBinary_operators(IRParser.Binary_operatorsContext ctx) {
        BinaryOperator operator = BinaryOperator.get(ctx.start.getText());
        String a = ctx.alnum(0).getText();
        String b = ctx.alnum(1).getText();
        String dest = ctx.ID().getText();

        Command command = new BinaryOperatorCommand(operator,
                currFunction.getArgument(a), currFunction.getArgument(b), currFunction.getArgument(dest));
        currFunction.addCommand(command);
    }

    @Override
    public void exitCall(IRParser.CallContext ctx) {
        List<Argument> args = ctx.alnum().stream().map(elem -> currFunction.getArgument(elem.getText())).toList();
        Command command = new CallCommand(ctx.ID().getText(), args);

        currFunction.addCommand(command);
    }

    @Override
    public void exitCallr(IRParser.CallrContext ctx) {
        List<Argument> args = ctx.alnum().stream().map(elem -> currFunction.getArgument(elem.getText())).toList();
        Command command = new CallRCommand(currFunction.getArgument(ctx.ID(0).getText()), ctx.ID(1).getText(), args);

        currFunction.addCommand(command);
    }

    @Override
    public void exitArray_load(IRParser.Array_loadContext ctx) {
        String variable = ctx.ID(0).getText();
        String array = ctx.ID(1).getText();
        String index = ctx.alnum().getText();
        Command command = new ArrayLoadCommand(currFunction.getArgument(variable),
                currFunction.getArgument(array), currFunction.getArgument(index));

        currFunction.addCommand(command);
    }

    @Override
    public void exitArray_store(IRParser.Array_storeContext ctx) {
        String array = ctx.ID().getText();
        String index = ctx.alnum(0).getText();
        String value = ctx.alnum(1).getText();
        Command command = new ArrayLoadCommand(currFunction.getArgument(array),
                currFunction.getArgument(index), currFunction.getArgument(value));

        currFunction.addCommand(command);
    }

    @Override
    public void exitAssignment_operator(IRParser.Assignment_operatorContext ctx) {
        Command command;
        String variable = ctx.ID().getText();

        if (ctx.alnum().size() == 1) {
            String value = ctx.alnum(0).getText();
            command = new AssignmentCommand(currFunction.getArgument(variable), currFunction.getArgument(value));
        } else {
            String size = ctx.alnum(0).getText();
            String value = ctx.alnum(1).getText();
            command = new AssignmentCommand(currFunction.getArgument(variable),
                    currFunction.getArgument(size), currFunction.getArgument(value));
        }

        currFunction.addCommand(command);
    }

    @Override
    public void enterLabel(IRParser.LabelContext ctx) {
        String label = ctx.ID().getText();

        currFunction.startBasicBlock(label);
        currFunction.addCommand(new LabelCommand(label));
    }

}
