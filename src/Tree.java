
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
public class Tree extends IRBaseListener {

    private Class currClass;
    private Function currFunction;

    public Tree() {
        currClass = null;
        currFunction = null;
    }

    public Class getCurrClass() {
        return currClass;
    }

    @Override
    public void enterProgram(IRParser.ProgramContext ctx) {
        currClass = new Class(ctx.ID(0).getText());
    }

    @Override
    public void enterStatic_float_list(IRParser.Static_float_listContext ctx) {
        for (var varNode : ctx.var_dec()) {
            if (varNode.LB() == null)
                currClass.addStaticVar(new Variable(varNode.ID().getText(), Type.Float));
            else
                currClass.addStaticVar(new Array(varNode.ID().getText(), Type.Float,
                                                 Integer.valueOf(varNode.NUM().getText())));
        }
    }

    @Override
    public void enterStatic_int_list(IRParser.Static_int_listContext ctx) {
        for (var varNode : ctx.var_dec()) {
            if (varNode.LB() == null)
                currClass.addStaticVar(new Variable(varNode.ID().getText(), Type.Integer));
            else
                currClass.addStaticVar(new Array(varNode.ID().getText(), Type.Integer,
                                                 Integer.valueOf(varNode.NUM().getText())));
        }
    }

    @Override
    public void enterFunction(IRParser.FunctionContext ctx) {
        currFunction = new Function(ctx.ID(0).getText());
        currClass.addFunction(currFunction);
    }

    @Override
    public void enterArgs_list(IRParser.Args_listContext ctx) {
        for (int i = 0; i < ctx.ID().size(); i += 2) {
            boolean isInt = ctx.ID(i).getText().equals("int");
            String name = ctx.ID(i + 1).getText();

            Variable arg = isInt ? new Variable(name, Type.Integer) : new Variable(name, Type.Float);
            currFunction.addArgument(arg);
        }
    }

    @Override
    public void enterFloat_list(IRParser.Float_listContext ctx) {
        for (var varNode : ctx.var_dec()) {
            if (varNode.LB() == null)
                currFunction.addLocalVar(new Variable(varNode.ID().getText(), Type.Float));
            else
                currFunction.addLocalVar(new Array(varNode.ID().getText(), Type.Float,
                                                   Integer.valueOf(varNode.NUM().getText())));
        }
    }

    @Override
    public void enterInt_list(IRParser.Int_listContext ctx) {
        for (var varNode : ctx.var_dec()) {
            if (varNode.LB() == null)
                currFunction.addLocalVar(new Variable(varNode.ID().getText(), Type.Integer));
            else
                currFunction.addLocalVar(new Array(varNode.ID().getText(), Type.Integer,
                                                 Integer.valueOf(varNode.NUM().getText())));
        }
    }

    @Override
    public void enterGoto_branch_operator(IRParser.Goto_branch_operatorContext ctx) {
        String jumpLabel = ctx.ID().getText();
        IRCommand command = new GotoCommand(jumpLabel);

        currFunction.addCommand(command);
        currFunction.endBasicBlock(jumpLabel, false);
    }

    @Override
    public void exitCond_branch_operators(IRParser.Cond_branch_operatorsContext ctx) {
        String commandName = ctx.start.getText();
        String jumpLabel = ctx.ID().getText();
        String a = ctx.alnum(0).getText();
        String b = ctx.alnum(1).getText();
        IRCommand command = new ConditionalBranchCommand(commandName, currFunction.getVariable(a),
                currFunction.getVariable(b), jumpLabel);

        currFunction.addCommand(command);
        currFunction.endBasicBlock(jumpLabel, true);
    }

    @Override
    public void exitReturn_operators(IRParser.Return_operatorsContext ctx) {
        // TODO: Implement return command, cause don't remember shit atm
        IRCommand command = ctx.alnum() == null ?
                new ReturnCommand(currFunction.getVariable(ctx.alnum().getText())) :
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

        IRCommand command = new BinaryOperatorCommand(operator,
                currFunction.getVariable(a), currFunction.getVariable(b), currFunction.getVariable(dest));
        currFunction.addCommand(command);
    }

    @Override
    public void exitCall(IRParser.CallContext ctx) {
        List<Argument> args = ctx.alnum().stream().map(elem -> currFunction.getVariable(elem.getText())).toList();
        IRCommand command = new CallCommand(ctx.ID().getText(), args);

        currFunction.addCommand(command);
    }

    @Override
    public void exitCallr(IRParser.CallrContext ctx) {
        List<Argument> args = ctx.alnum().stream().map(elem -> currFunction.getVariable(elem.getText())).toList();
        IRCommand command = new CallRCommand(currFunction.getVariable(ctx.ID(0).getText()), ctx.ID(1).getText(), args);

        currFunction.addCommand(command);
    }

    @Override
    public void exitArray_load(IRParser.Array_loadContext ctx) {
        String variable = ctx.ID(0).getText();
        String array = ctx.ID(1).getText();
        String index = ctx.alnum().getText();
        IRCommand command = new ArrayLoadCommand(currFunction.getVariable(variable), (Array) currFunction.getVariable(array), currFunction.getVariable(index));

        currFunction.addCommand(command);
    }

    @Override
    public void exitArray_store(IRParser.Array_storeContext ctx) {
        String array = ctx.ID().getText();
        String index = ctx.alnum(0).getText();
        String value = ctx.alnum(1).getText();
        IRCommand command = new ArrayStoreCommand((Array) currFunction.getVariable(array),
                                                  currFunction.getVariable(index), currFunction.getVariable(value));

        currFunction.addCommand(command);
    }

    @Override
    public void exitAssignment_operator(IRParser.Assignment_operatorContext ctx) {
        IRCommand command;
        String variable = ctx.ID().getText();

        if (ctx.alnum().size() == 1) {
            String value = ctx.alnum(0).getText();
            command = new AssignmentCommand(currFunction.getVariable(variable), currFunction.getVariable(value));
        } else {
            String size = ctx.alnum(0).getText();
            String value = ctx.alnum(1).getText();
            command = new AssignmentCommand(currFunction.getVariable(variable),
                    Integer.valueOf(size), currFunction.getVariable(value));
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
