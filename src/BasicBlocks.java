import java.util.*;

public class BasicBlocks {
    Block startingBlock;
    Block currentBlock;
    Map<String, Block> labeledBlocks;

    public BasicBlocks() {
        startingBlock = null;
        currentBlock = null;
        labeledBlocks = new HashMap<>();
    }

    public void startBasicBlock(String label) {
        if (currentBlock != null && currentBlock.isEmpty()) {
            labeledBlocks.put(label, currentBlock);
            return;
        }

        Block newBlock = getLabeledBlock(label);
        if (startingBlock == null) startingBlock = newBlock;
        currentBlock.addNextBlock(newBlock);
        currentBlock = newBlock;
    }

    public void endBasicBlock(String jumpLabel, boolean isConditional) {
        Block jumpBlock = getLabeledBlock(jumpLabel);
        currentBlock.addNextBlock(jumpBlock);

        Block newBlock = new Block();
        if (isConditional) currentBlock.addNextBlock(newBlock);

        currentBlock = newBlock;
    }

    public void addCommand(Command command) {
        assert currentBlock != null;
        currentBlock.addCommand(command);
    }

    private Block getLabeledBlock(String label) {
        if (!labeledBlocks.containsKey(label))
            labeledBlocks.put(label, new Block());

        return labeledBlocks.get(label);
    }

    private class Block {
        private Set<Block> nextBlocks;
        private List<Command> commands;

        public Block() {
            nextBlocks = new HashSet<>();
            commands = new ArrayList<>();
        }

        public void addNextBlock(Block nextBlock) {
            nextBlocks.add(nextBlock);
        }

        public void addCommand(Command command) {
            commands.add(command);
        }

        public boolean isEmpty() {
            return commands.size() == 0;
        }
    }
}
