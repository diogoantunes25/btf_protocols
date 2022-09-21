package pt.tecnico.ulisboa.hbbft.abc.alea.state;

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
        // logger.info("[EPOCH - {}] - Entered Proposing State.", epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        Step<Block> step = new Step<>();

        if (this.canProgress()) {
            PriorityQueue queue = this.getQueue();
            Optional<Slot> slot = queue.peek();

            IBinaryAgreement instance = this.getBaInstance();
            final boolean proposal = slot.isPresent();
            Step<Boolean> baStep = instance.handleInput(slot.isPresent());
            step.add(baStep.getMessages());

            // logger.info("[EPOCH - {}] - Proposed={}", epoch, proposal);

            AgreementState nextState = new OngoingState(alea, epoch);
            step.add(this.alea.setAgreementState(nextState));
        }
        return step;
    }

    private boolean canProgress() {
        return this.alea.getQueues().values().stream()
                .anyMatch(queue -> queue.peek().isPresent());
    }
}
