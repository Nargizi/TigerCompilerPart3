import java.util.HashSet;
import java.util.Set;

public class GraphToGraphvizParser {
    private GraphVizBuilder builder;
    private Set<Integer> visited;

    public GraphToGraphvizParser(GraphVizBuilder builder){
        this.builder = builder;
    }

    public void parse(Class c){
        builder.startDigraph(c.getClassName());
        for(Function function: c.getFunctions().values())
            parse(function);
        builder.endDigraph();
    }

    public void parse(Function function){
        builder.startSubgraph(function.getFuncName());
        visited = new HashSet<>();
        parse(function.getControlFlowGraph().getStartingBlock());
        builder.endSubgraph();
    }

    public void parse(BasicBlocks.Block block){
        if(visited.contains(block.hashCode()))
            return;
        visited.add(block.hashCode());
        builder.addAttribute(String.valueOf(block.hashCode()),"label", block.toString());
        for(BasicBlocks.Block b: block.getNextBlocks())
            builder.addArrow(String.valueOf(block.hashCode()), String.valueOf(b.hashCode()));
        for(BasicBlocks.Block b: block.getNextBlocks())
            parse(b);
    }
}
