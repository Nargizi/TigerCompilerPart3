import java.util.HashMap;
import java.util.Map;

public class Memory {
    private Register memoryRegister;
    private Integer size;
    private Map<Argument, Address> storedVarAddresses;
    private Map<String, Argument> storedVars;

    public Memory(Register memoryRegister){
        this.memoryRegister = memoryRegister;
        this.size = 0;
        this.storedVarAddresses = new HashMap<>();
        this.storedVars = new HashMap<>();
    }

    public void declareVariable(Argument argument){
        if(argument.isEmpty() || argument instanceof ConstantArgument)
            return;

        if(storedVarAddresses.containsKey(argument))
            return;

        storedVarAddresses.put(argument, new Address(memoryRegister, size));
        storedVars.put(argument.getValue(), argument);
        size += argument.getSize();
    }

    public Address getAddress(Argument argument){
        return storedVarAddresses.get(argument);
    }

    public Argument getArgument(String value){
        return storedVars.getOrDefault(value, new ConstantArgument(value));
    }

    static class Address {
        private Register start;
        private Integer offset;

        public Address(Register start, Integer offset) {
            this.start = start;
            this.offset = offset;
        }

        public Register getStart() {
            return start;
        }

        public void setStart(Register start) {
            this.start = start;
        }

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }
    }

}
