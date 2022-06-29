import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LiveSet {
    private final Map<Integer, Set<Variable>> inSets;
    private final Map<Integer, Set<Variable>> outSets;
    private final Function function;

    public LiveSet(Function function){
        this.function = function;
        this.inSets = new HashMap<>();
        this.outSets = new HashMap<>();
        init();
    }

    public Set<Variable> getSet(Integer i){
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
                Set<Variable> oldOut = outSets.computeIfAbsent(i, x -> new HashSet<>());
                Set<Variable> oldIn = inSets.computeIfAbsent(i, x -> new HashSet<>());
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

    private Set<Variable> getLiveSet(IRCommand i, Integer index){
        Set<Variable> set = new HashSet<>(outSets.getOrDefault(index, new HashSet<>()));
        set.removeAll(i.getDecl());
        set.addAll(i.getUsed());
        return set;
    }

    private Set<Variable> getOutSet(Set<Integer> successorCommands){
        Set<Variable> outSet = new HashSet<>();
        for(var i: successorCommands) {
            outSet.addAll(inSets.getOrDefault(i, new HashSet<>()));
        }
        return outSet;
    }

    @Override
    public String toString() {
        StringBuilder rep = new StringBuilder();
        rep.append("Function Start - ").append(function.getFuncName());
        rep.append(":\n");
        for(int i = 0; i < function.getNumCommands(); ++i){
            rep.append(i).append(":").append("INSET: ").append(inSets.get(i))
                    .append(" - ").append("OUTSET: ").append(outSets.get(i)).append("\n");
        }
        return rep.append("Function End - ").append(function.getFuncName()).append("\n\n").toString();
    }

}
