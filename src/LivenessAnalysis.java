import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LivenessAnalysis {
    private Map<String, LiveSet> liveSets;

    public LivenessAnalysis(Class c){
        liveSets = new HashMap<>();
        for(var func: c.getFunctions().entrySet()){
            liveSets.put(func.getKey(), new LiveSet(func.getValue()));
        }
    }

    public LiveSet getLiveSet(String funcName){
        return liveSets.get(funcName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (var set: liveSets.values())
            builder.append(set);
        return builder.toString();
    }

    public void toFile(String path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.append(this.toString());
        writer.flush();
    }
}
