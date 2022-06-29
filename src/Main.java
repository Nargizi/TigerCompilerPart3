import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class Main {
    /*
    TODO: HOW TO ALLOCATE REGISTERS??
        change variables to register names
        variable to register map

        max - max number of registers that program can use
        stack - free registers
        map <variable, register> - used registers


     */

    public static void compile(File file, boolean graphViz, boolean liveness) throws IOException {
        CharStream codePointCharStream = CharStreams.fromPath(Path.of(file.getAbsolutePath()));
        IRLexer lexer = new IRLexer(codePointCharStream);
        IRParser parser = new IRParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();
        Tree t = new Tree();
        walker.walk(t, tree);
        Class c = t.getCurrClass();
        Function f = c.getFunctions().get("main");
        CFGAllocator allocator = new CFGAllocator(f);
        Translator translator = new Translator(allocator);
        for(int i = 0; i < f.getNumCommands(); ++i){
            for(var command: translator.translate(f.getCommand(i)))
                System.out.println(command);
        }

        String fileName = file.getName();
        fileName = fileName.substring(0, fileName.lastIndexOf("ir"));
        File folder = file.getParentFile();
        LivenessAnalysis livenessAnalysis = new LivenessAnalysis(c);
        if(graphViz){
            GraphVizBuilder builder = new GraphVizBuilder();
            GraphToGraphvizParser graphvizParser = new GraphToGraphvizParser(builder);
            graphvizParser.parse(c);
            builder.toFile(Path.of(folder.getAbsolutePath(), fileName + "cfg.gv").toString());
        }

        if(liveness){
            livenessAnalysis.toFile(Path.of(folder.getAbsolutePath(), fileName + "liveness").toString());
        }

//        File folder = file.getParentFile();
//        String name = file.getName();
//        name = name.substring(0, name.lastIndexOf("tiger"));
//        SemanticChecking semanticChecking = new SemanticChecking(save_symbol_table, Path.of(folder.getAbsolutePath(), name + "st"));
//        walker.walk(semanticChecking, tree);

    }

    public static void test(){
//        Function func = new Function("Test Func");
//        func.startBasicBlock("START");
//        func.addCommand(new ConditionalBranchCommand("BLE", "c", "0", "IF1"));
//        func.endBasicBlock("IF1", true);
//        func.addCommand(new BinaryOperatorCommand(BinaryOperator.ADD, "y", "1", "x"));
//        func.addCommand(new BinaryOperatorCommand(BinaryOperator.MUL, "2", "z", "y"));
//        func.addCommand(new ConditionalBranchCommand("BLE", "d", "0", "IF2"));
//        func.endBasicBlock("IF2", true);
//        func.addCommand(new BinaryOperatorCommand(BinaryOperator.ADD, "y", "z", "x"));
//        func.endBasicBlock("IF2", false);
//        func.startBasicBlock("IF2");
//        func.addCommand(new AssignmentCommand("z", "1"));
//        func.endBasicBlock("START", false);
//        func.startBasicBlock("IF1");
//        func.addCommand(new AssignmentCommand("z", "x"));
//        System.out.println(new LiveSet(func));
    }

    public static void main(String[] args) throws IOException {
        String ir_source = null;
        boolean graphViz = false, liveness = false;
        for(int i = 0; i < args.length; ++i){
            if(args[i].equals("-r")){
                ir_source = args[i + 1];
            }
            if(args[i].equals("--cfg")){
                graphViz = true;
            }
            if(args[i].equals("--liveness")){
                liveness = true;
            }
        }
        if (ir_source == null){
            // TODO ERROR
        }
        compile(new File(ir_source), graphViz, liveness);
    }
}
