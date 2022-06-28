import java.util.*;

public class InterferenceGraph {
    /* TODO: nodes for each variable
        if two variables are alive at same time there is interference between them
        (represented by edge between corresponding variables' nodes)
        also nodes need some kind of color indicator
    */
    Map<Argument, Node> nodes;

    public InterferenceGraph(LiveSet liveSet){
        init(liveSet);
    }

    private void init(LiveSet liveSet){
        for (int i = 0; i < liveSet.getSize(); ++i){
            List<Node> aliveNodes = new ArrayList<>();
            for(Argument var: liveSet.getSet(i)){
                aliveNodes.add(nodes.computeIfAbsent(var, Node::new));
            }
        }
    }

    private void addInterference(Node one, Node two){
        // TODO: add interference between two nodes
        one.addInterference(two);
        two.addInterference(one);
    }

    private void addInterference(List<Node> nodes){
        // TODO: add interference between all nodes in list
        for (int i = 0; i < nodes.size(); ++i){
            for (int j = 0; j < nodes.size(); ++j){
                if (i == j)
                    continue;
                nodes.get(i).addInterference(nodes.get(j));
                nodes.get(j).addInterference(nodes.get(i));
            }
        }
    }

    static public class Node {
        private final Argument name;
        private final Set<Node> interferences;

        public Node(Argument name) {
            this.name = name;
            this.interferences = new HashSet<>();
        }

        public Argument getName() {
            return name;
        }

        public Set<Node> getInterferences() {
            return interferences;
        }

        public void addInterference(Node other){
            interferences.add(other);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(name, node.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
