import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LiveSet {
    private Map<Integer, Set<Argument>> inSets;
    private Map<Integer, Set<Argument>> outSets;
    private Function function;

    public LiveSet(Function function){
        this.function = function;
        this.inSets = new HashMap<>();
        this.outSets = new HashMap<>();
        init();
    }

    public Set<Argument> getSet(Integer i){
        return inSets.getOrDefault(i, new HashSet<>());
    }

    public Integer getSize(){
        return inSets.size();
    }

    private void init(){
        boolean isUpdate;
        do {
            isUpdate = false;
            for (int i = 0; i < function.getNumCommands(); ++i) {
                Set<Argument> oldOut = outSets.computeIfAbsent(i, x -> new HashSet<>());
                Set<Argument> oldIn = inSets.computeIfAbsent(i, x -> new HashSet<>());
                update(i);
                if(!oldOut.equals(outSets.get(i)) || !oldIn.equals(inSets.get(i)))
                    isUpdate = true;
            }
        }while(isUpdate);
    }

    private void update(Integer i){
        outSets.put(i, getOutSet(function.getFollowSet(i)));
        inSets.put(i, getLiveSet(function.getCommand(i), i));
    }

    private Set<Argument> getLiveSet(Command i, Integer index){
        Set<Argument> set = new HashSet<>(outSets.getOrDefault(index, new HashSet<>()));
        set.removeAll(i.getDecl());
        set.addAll(i.getUsed());
        return set;
    }

    private Set<Argument> getOutSet(Set<Integer> successorCommands){
        Set<Argument> outSet = new HashSet<>();
        for(var i: successorCommands) {
            outSet.addAll(inSets.getOrDefault(i, new HashSet<>()));
        }
        return outSet;
    }

    @Override
    public String toString() {
        StringBuilder rep = new StringBuilder();
        rep.append("Function - ").append(function.getFuncName());
        rep.append(":\n");
        for(int i = 0; i < function.getNumCommands(); ++i){
            rep.append(i).append(":").append("INSET: ").append(inSets.get(i))
                    .append(" - ").append("OUTSET: ").append(outSets.get(i)).append("\n");
        }
        return rep.toString();
    }
}
