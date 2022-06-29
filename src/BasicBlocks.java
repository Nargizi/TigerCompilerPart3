import java.util.*;

public class BasicBlocks {
    private Block startingBlock;
    private Block currentBlock;
    private Map<String, Block> labeledBlocks;
    private Map<String, Set<Block>> labelListeners;

    public BasicBlocks() {
        startingBlock = null;
        currentBlock = null;
        labeledBlocks = new HashMap<>();
        labelListeners = new HashMap<>();
    }

    public void startBasicBlock(String label) {
        if (currentBlock == null || !currentBlock.isEmpty()) {
            Block newBlock = new Block();
            if (startingBlock == null) startingBlock = newBlock;
            if (currentBlock != null) currentBlock.addNextBlock(newBlock);
            currentBlock = newBlock;
        }

        labeledBlocks.put(label, currentBlock);

        for (Block listener : labelListeners.getOrDefault(label, new HashSet<>())) {
            listener.nextBlocks.add(currentBlock);
        }
    }

    public void endBasicBlock(String jumpLabel, boolean isConditional) {
        if (labeledBlocks.containsKey(jumpLabel)) {
            Block jumpBlock = labeledBlocks.get(jumpLabel);
            currentBlock.addNextBlock(jumpBlock);
        } else {
            labelListeners.computeIfAbsent(jumpLabel, k -> new HashSet<>());
            labelListeners.get(jumpLabel).add(currentBlock);
        }

        Block newBlock = new Block();
        if (isConditional) currentBlock.addNextBlock(newBlock);

        currentBlock = newBlock;
    }

    public void endBasicBlock() {
        currentBlock = new Block();
    }

    public void addCommand(IRCommand command) {
        assert currentBlock != null;
        currentBlock.addCommand(command);
    }

    public Block getCurrentBlock(){ return currentBlock;}

    public Block getStartingBlock() {
        return startingBlock;
    }

    public class Block {
        private Set<Block> nextBlocks;
        private List<IRCommand> commands;

        public Block() {
            nextBlocks = new HashSet<>();
            commands = new ArrayList<>();
        }

        public void setCommands(List<IRCommand> commands) {
            this.commands = commands;
        }

        public Set<Block> getNextBlocks() {
            return nextBlocks;
        }

        public void addNextBlock(Block nextBlock) {
            nextBlocks.add(nextBlock);
        }

        public List<IRCommand> getCommands() {
            return commands;
        }

        public void addCommand(IRCommand command) {
            commands.add(command);
        }

        public boolean isEmpty() {
            return commands.size() == 0;
        }

        public Map<Argument, Integer> getUsedVars(){
            Map<Argument, Integer> usedVars = new HashMap<>();
            for(var c: commands){
                for(var v: c.getUsed()){
                    usedVars.merge(v, 1, Integer::sum);
                }
            }
            return usedVars;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("\"");
            for (var command: commands)
                builder.append(command).append("\n");
            return builder.append("\"").toString();

        }
    }
}
