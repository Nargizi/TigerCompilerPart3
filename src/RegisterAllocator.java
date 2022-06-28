import java.util.*;

public class RegisterAllocator {
    // allocates registers and returns command list with [load and store commands]
    private Set<Register> registers;

    public RegisterAllocator(Set<Register> registers){
        this.registers = registers;
    }

    public void naiveAllocate(Function func){
        Set<Register> freeRegisters = new HashSet<>(registers);
        Map<String, Register> usedRegisters = new HashMap<>();

        for(int i = 0; i < func.getNumCommands(); ){
            BasicBlocks.Block currBlock = func.getCommand(i).getBlock();
            List<Command> commandList = new ArrayList<>();
//            for()
            i += currBlock.getCommands().size();
        }
    }





}
