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
            if (labelListeners.get(jumpLabel) == null)
                labelListeners.put(jumpLabel, new HashSet<>());
            labelListeners.get(jumpLabel).add(currentBlock);
        }

        Block newBlock = new Block();
        if (isConditional) currentBlock.addNextBlock(newBlock);

        currentBlock = newBlock;
    }

    public void endBasicBlock() {
        currentBlock = new Block();
    }

    public void addCommand(Command command) {
        assert currentBlock != null;
        currentBlock.addCommand(command);
    }

    public Block getStartingBlock() {
        return startingBlock;
    }

    public class Block {
        private Set<Block> nextBlocks;
        private List<Command> commands;

        public Block() {
            nextBlocks = new HashSet<>();
            commands = new ArrayList<>();
        }

        public Set<Block> getNextBlocks() {
            return nextBlocks;
        }

        public void addNextBlock(Block nextBlock) {
            nextBlocks.add(nextBlock);
        }

        public List<Command> getCommands() {
            return commands;
        }

        public void addCommand(Command command) {
            commands.add(command);
        }

        public boolean isEmpty() {
            return commands.size() == 0;
        }
    }
}
