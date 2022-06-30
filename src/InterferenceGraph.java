import java.util.*;
import java.util.stream.Collectors;

public class InterferenceGraph {

    Map<Argument, Node> intNodes;
    Map<Argument, Node> floatNodes;

    public InterferenceGraph(LiveSet liveSet, final Set<Register> intRegisters, final Set<Register> floatRegisters) {
        init(liveSet);
        color(intRegisters, true);
        color(floatRegisters, false);
    }

    private void init(LiveSet liveSet){
        for (int i = 0; i < liveSet.getSize(); ++i){
            List<Node> aliveNodes = new ArrayList<>();
            for(Argument var: liveSet.getSet(i)){
                Map<Argument, Node> varNodes = var.getType().equals(Type.Float) ? floatNodes : intNodes;
                aliveNodes.add(varNodes.computeIfAbsent(var, Node::new));
            }
        }
    }

    private void addInterference(Node one, Node two){
        one.addInterference(two);
        two.addInterference(one);
    }

    private void addInterference(List<Node> nodes){
        for (int i = 0; i < nodes.size(); ++i){
            for (int j = 0; j < nodes.size(); ++j){
                if (i == j)
                    continue;
                nodes.get(i).addInterference(nodes.get(j));
                nodes.get(j).addInterference(nodes.get(i));
            }
        }
    }

    public Register getRegister(Argument arg) {
        Node n;
        if (arg.getType().equals(Type.Integer))
            n = intNodes.get(arg);
        else
            n = floatNodes.get(arg);
        if (n != null)
            return n.getRegister();
        return null;
    }

    private void color(final Set<Register> registers, boolean isInt) {
        Map<Argument, Node> nodes = new HashMap<>(isInt ? intNodes : floatNodes);

        PriorityQueue<Node> pqueue = new PriorityQueue<>(Comparator.comparingInt(Node::getSpillCost).reversed());
        for (Node node: nodes.values()) { pqueue.add(node); }

        java.util.Stack<Node> stack = new java.util.Stack<>();
        while (!pqueue.isEmpty()) {
            Node node = pqueue.poll();
            node.onStack = true;

            UpdateNeighbourSpillCosts(node, pqueue);
            stack.push(node);
        }

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            node.onStack = false;
            colorNode(node, registers);
        }
    }

    private void colorNode(Node node, Set<Register> registers) {
        Set<Register> usedRegisters = node.interferences.stream()
                .filter(n -> node.register != null)
                .map(Node::getRegister)
                .collect(Collectors.toSet());

        Set<Register> freeRegisters = new HashSet<>(registers);
        freeRegisters.removeAll(usedRegisters);

        node.register = freeRegisters.isEmpty() ? null : freeRegisters.iterator().next();
    }

    private void UpdateNeighbourSpillCosts(Node node, PriorityQueue<Node> pqueue) {
        for (Node neighbour: node.interferences) {
            if (!neighbour.onStack) {
                pqueue.remove(neighbour);
                pqueue.add(neighbour);
            }
        }
    }

    static public class Node {
        private final Argument name;
        private final Set<Node> interferences;
        private Register register;
        private boolean onStack;

        public Node(Argument name) {
            this.name = name;
            this.interferences = new HashSet<>();
            this.register = null;
            this.onStack = false;
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

        public int getSpillCost() {
            return interferences.stream().reduce(0, (total, node) -> total + (node.onStack ? 0 : 1), Integer::sum);
        }

        public Register getRegister() {
            return register;
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
