import java.util.*;

public abstract class RegisterAllocator {
    // allocates registers and returns command list with [load and store commands]
    private Set<Register> registers;

    public RegisterAllocator(Set<Register> registers){
        this.registers = registers;
    }

    public abstract void allocate(Function func);
}

class NaiveAllocator extends RegisterAllocator {
    public NaiveAllocator(Set<Register> registers) {
        super(registers);
    }

    @Override
    public void allocate(Function func) {
        // should be empty, naive allocator does not allocate registers
        // TODO: recheck
//        Set<Register> freeRegisters = new HashSet<>(registers);
//        Map<String, Register> usedRegisters = new HashMap<>();
//
//        for(int i = 0; i < func.getNumCommands(); ){
//            BasicBlocks.Block currBlock = func.getCommand(i).getBlock();
//            List<Command> commandList = new ArrayList<>();
////            for()
//            i += currBlock.getCommands().size();
//        }
    }
}

