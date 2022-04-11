package pt.tecnico.ulisboa.hbbft.abc.byzness.state;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.Byzness;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.Slot;

public class WaitingState extends AgreementState {

    public WaitingState(Byzness byzness, Long epoch) {
        super(byzness, epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        Step<Block> step = new Step<>();

        if (this.canProgress()) {
            Slot slot = this.getQueue().peek().orElseThrow();

            // Remove decided value from all queues
            for (PriorityQueue q: this.byzness.getQueues().values())
                q.dequeue(slot.getValue());

            // Assemble block
            step.add(new Block(epoch, slot.getValue()));

            // Progress to next state
            AgreementState nextState = new ProposingState(byzness, epoch+1);
            step.add(this.byzness.setAgreementState(nextState));
        }
        return step;
    }

    private boolean canProgress() {
        return this.getQueue().peek().isPresent();
    }
}
