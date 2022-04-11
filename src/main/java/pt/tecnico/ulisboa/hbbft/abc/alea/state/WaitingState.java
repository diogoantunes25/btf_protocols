package pt.tecnico.ulisboa.hbbft.abc.alea.state;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.alea.Alea;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.Slot;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class WaitingState extends AgreementState {

    public WaitingState(Alea alea, Long epoch) {
        super(alea, epoch);
        //logger.info("[EPOCH - {}] - Entered Waiting State.", epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        Step<Block> step = new Step<>();

        if (this.canProgress()) {
            Slot slot = this.getQueue().peek().orElseThrow();

            Collection<byte[]> blockContents;
            if (this.alea.getParams().getFault(this.getQueue().getId()) == Alea.Params.Fault.BYZANTINE) {
                blockContents = new ArrayList<>();
            } else if (!alea.getParams().isBenchmark()) {
                blockContents = Alea.decodeBatchEntries(slot.getValue());
            } else {
                blockContents = new ArrayList<>();
                for (int i=0; i<alea.getParams().getBatchSize(); i++) blockContents.add(new byte[0]);
            }

            // Assemble block
            Block block = new Block(epoch, blockContents);
            block.setProposers(Collections.singleton(this.getQueue().getId()));
            step.add(block);

            logger.info("[EPOCH - {}] - Delivered={}.", epoch, blockContents.size());

            // Remove decided value from all queues
            if (blockContents.size() > 1) {
                this.getQueue().dequeue(slot.getId());
            } else {
                for (PriorityQueue q: this.alea.getQueues().values()) {
                    q.dequeue(slot.getValue());
                }
            }

            // Progress to next state
            AgreementState nextState = new ProposingState(alea, epoch+1);
            step.add(this.alea.setAgreementState(nextState));
        }
        return step;
    }

    private boolean canProgress() {
        return this.getQueue().peek().isPresent();
    }
}
