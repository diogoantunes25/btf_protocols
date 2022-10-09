package pt.tecnico.ulisboa.hbbft.abc.alea.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.alea.Alea;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.Slot;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;

import java.time.ZonedDateTime;
import java.util.Optional;

public class ProposingState extends AgreementState {

    public ProposingState(Alea alea, Long epoch) {
        super(alea, epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        logger.info("tryPropose on ProposingState");
        Step<Block> step = new Step<>();

        if (this.canProgress()) {
            PriorityQueue queue = this.getQueue();
            Optional<Slot> slot = queue.peek();

            IBinaryAgreement instance = this.getBaInstance();
            Step<Boolean> baStep = instance.handleInput(slot.isPresent());
            step.add(baStep.getMessages());

            // logger.info("[EPOCH - {}] - Proposed={}", epoch, proposal);

            AgreementState nextState = new OngoingState(alea, epoch);
            step.add(this.alea.setAgreementState(nextState));
        } else {
            logger.info("tryPropose on ProposingState aborted - there's nothing to decide on");
        }
        return step;
    }

    /**
     * Check if any queue is non-empty
     */
    private boolean canProgress() {
        return this.alea.getQueues().values().stream()
                .anyMatch(queue -> queue.peek().isPresent());
    }

    public String toString() {
        return "ProposingState";
    }
}
