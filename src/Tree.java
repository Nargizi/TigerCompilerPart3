import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tree extends IRBaseListener {

    private Map<String, BasicBlocks> functionBasicBlock;
    private BasicBlocks currentBasicBlocks;

    public Tree() {
        functionBasicBlock = new HashMap<>();
        currentBasicBlocks = null;
    }

    @Override
    public void enterFunction(IRParser.FunctionContext ctx) {
        currentBasicBlocks = new BasicBlocks();
        functionBasicBlock.put(ctx.ID(0).getText(), currentBasicBlocks);
    }

    @Override
    public void enterGoto_branch_operator(IRParser.Goto_branch_operatorContext ctx) {
        String jumpLabel = ctx.ID().getText();
        Command command = new GotoCommand(jumpLabel);

        currentBasicBlocks.addCommand(command);
        currentBasicBlocks.endBasicBlock(jumpLabel, false);
    }

    @Override
    public void exitCond_branch_operators(IRParser.Cond_branch_operatorsContext ctx) {
        String commandName = ctx.start.getText();
        String jumpLabel = ctx.ID().getText();
        String a = ctx.alnum(0).getText();
        String b = ctx.alnum(1).getText();
        Command command = new ConditionalBranchCommand(commandName, a, b, jumpLabel);

        currentBasicBlocks.addCommand(command);
        currentBasicBlocks.endBasicBlock(jumpLabel, true);
    }

    @Override
    public void exitReturn_operators(IRParser.Return_operatorsContext ctx) {
        // TODO: Implement return command, cause don't remember shit atm
        Command command = ctx.alnum() == null ?
                new ReturnCommand(ctx.alnum().getText()) :
                new ReturnCommand();

        currentBasicBlocks.addCommand(command);
        currentBasicBlocks.endBasicBlock();
    }

    @Override
    public void exitBinary_operators(IRParser.Binary_operatorsContext ctx) {
        BinaryOperator operator = BinaryOperator.get(ctx.start.getText());
        String a = ctx.alnum(0).getText();
        String b = ctx.alnum(1).getText();
        String dest = ctx.ID().getText();

        Command command = new BinaryOperatorCommand(operator, a, b, dest);
        currentBasicBlocks.addCommand(command);
    }

    @Override
    public void exitCall(IRParser.CallContext ctx) {
        List<String> args = ctx.alnum().stream().map(elem -> elem.getText()).toList();
        Command command = new CallCommand(ctx.ID().getText(), args);

        currentBasicBlocks.addCommand(command);
    }

    @Override
    public void exitCallr(IRParser.CallrContext ctx) {
        List<String> args = ctx.alnum().stream().map(elem -> elem.getText()).toList();
        Command command = new CallRCommand(ctx.ID(0).getText(), ctx.ID(1).getText(), args);

        currentBasicBlocks.addCommand(command);
    }

    @Override
    public void exitArray_load(IRParser.Array_loadContext ctx) {
        String variable = ctx.ID(0).getText();
        String array = ctx.ID(1).getText();
        String index = ctx.alnum().getText();
        Command command = new ArrayLoadCommand(variable, array, index);

        currentBasicBlocks.addCommand(command);
    }

    @Override
    public void exitArray_store(IRParser.Array_storeContext ctx) {
        String array = ctx.ID().getText();
        String index = ctx.alnum(0).getText();
        String value = ctx.alnum(1).getText();
        Command command = new ArrayLoadCommand(array, index, value);

        currentBasicBlocks.addCommand(command);
    }

    @Override
    public void exitAssignment_operator(IRParser.Assignment_operatorContext ctx) {
        Command command;
        String variable = ctx.ID().getText();

        if (ctx.alnum().size() == 1) {
            String value = ctx.alnum(0).getText();
            command = new AssignmentCommand(variable, value);
        } else {
            String size = ctx.alnum(0).getText();
            String value = ctx.alnum(1).getText();
            command = new AssignmentCommand(variable, size, value);
        }

        currentBasicBlocks.addCommand(command);
    }

    @Override
    public void enterLabel(IRParser.LabelContext ctx) {
        String label = ctx.ID().getText();

        currentBasicBlocks.startBasicBlock(label);
        currentBasicBlocks.addCommand(new LabelCommand(label));
    }

}
