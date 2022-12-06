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
        // logger.info("tryPropose on WaitingState");
        Step<Block> step = new Step<>();

        synchronized (this.getQueue()) {
            // Wait until I know of the value that was agreed upon
            if (this.canProgress()) {
                // Deliver
                Slot slot = this.getQueue().peek().orElseThrow();

                Collection<byte[]> blockContents;
                if (this.alea.getParams().getFault(this.getQueue().getId()) == Alea.Params.Fault.BYZANTINE) {
                    // TODO: (dsa) check if this makes sense
                    blockContents = new ArrayList<>();
                } else {
                    blockContents = Alea.decodeBatchEntries(slot.getValue());
                }

                // Assemble block
                Block block = new Block(epoch, blockContents);
                block.setProposers(Collections.singleton(this.getQueue().getId()));
                step.add(block);

                // logger.info("[EPOCH - {}] - Delivered = ({}) {}.", epoch, blockContents.size(), blockContents);

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
            } else {
                // logger.info("tryPropose on WaitingState aborted - nothing to decide on this round");
            }
        }
        return step;
    }

    private boolean canProgress() {
        return this.getQueue().peek().isPresent();
    }

    public String toString() {
        return "WaitingState";
    }

}
