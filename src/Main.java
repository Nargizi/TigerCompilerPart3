import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void toFile(String path, List<MIPSCommand> commandList) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        for(MIPSCommand c: commandList)
            writer.append(c.toString() + "\n");
        writer.flush();
    }

    public static void compile(File file, File tiger_file, boolean graphViz, boolean liveness, boolean naive, boolean cfg, boolean briggs) throws IOException {
        CharStream codePointCharStream = CharStreams.fromPath(Path.of(file.getAbsolutePath()));
        IRLexer lexer = new IRLexer(codePointCharStream);
        IRParser parser = new IRParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();
        Tree t = new Tree();
        walker.walk(t, tree);
        Class c = t.getCurrClass();
        String fileName = tiger_file.getName();
        fileName = fileName.substring(0, fileName.lastIndexOf("tiger"));
        File folder = tiger_file.getParentFile();

        if(cfg) {
            CFGAllocator allocator = new CFGAllocator();
            Translator translator = new Translator(allocator);
            toFile(Path.of(folder.getAbsolutePath(), fileName + "ib.s").toString(), translator.translate(c));
        }
        if(naive || !(cfg || briggs)) {
            NaiveAllocator allocator = new NaiveAllocator();
            Translator translator = new Translator(allocator);
            toFile(Path.of(folder.getAbsolutePath(), fileName + "naive.s").toString(), translator.translate(c));
        }
        if(briggs) {
            BriggsAllocator allocator = new BriggsAllocator();
            Translator translator = new Translator(allocator);
            toFile(Path.of(folder.getAbsolutePath(), fileName + "briggs.s").toString(), translator.translate(c));
        }



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

    }


    public static void main(String[] args) throws IOException {
        String ir_source = null, tiger_source = null;
        boolean naive = false, briggs = false, cfg = false;
        boolean graphViz = false, liveness = false;
        for(int i = 0; i < args.length; ++i){
            if(args[i].equals("-r")){
                ir_source = args[i + 1];
            }
            if(args[i].equals("-i")){
                tiger_source = args[i + 1];
            }
            if(args[i].equals("--cfg")){
                graphViz = true;
            }
            if(args[i].equals("--liveness")){
                liveness = true;
            }
            if(args[i].equals("-n")){
                naive = true;
            }
            if(args[i].equals("-b")){
                cfg = true;
            }
            if(args[i].equals("-g")){
                briggs = true;
            }
        }
        if (ir_source == null){
            // TODO ERROR
        }
        compile(new File(ir_source), new File(tiger_source) , graphViz, liveness, naive, cfg, briggs);
    }
}
