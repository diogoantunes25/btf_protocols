package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.*;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoustefaouiBinaryAgreement implements IMoustefaouiBinaryAgreement {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String pid;

    // Our ID.
    private final Integer replicaId;

    // The network configuration.
    private final NetworkInfo networkInfo;

    // The Binary Agreement algorithm round.
    private long round = 0L;

    // Maximum number of future epochs for which incoming messages are accepted.
    private long maxFutureRounds = 100L;

    // The set of values for which _2 f + 1_ `BVal`s have been received.
    private BoolSet binValues = BoolSet.NONE();

    // The nodes that sent us a `BVal(b)`, by `b`.
    private BoolMultimap receivedBVal = new BoolMultimap();

    // The values `b` for which we already sent `BVal(b)`.
    private BoolSet sentBVal = BoolSet.NONE();

    // The nodes that sent us an `Aux(b)`, by `b`.
    private BoolMultimap receivedAux = new BoolMultimap();

    // Whether the sbv broadcast has already output.
    private Boolean sbvTerminated = false;

    // This round's common coin.
    private Coin coin;

    // Received `Conf` messages. Reset on every epoch update.
    private Map<Integer, ConfMessage> receivedConf = new TreeMap<>();

    // Received `Term` messages. Kept throughout epoch updates. These count as `BVal`, `Aux` and
    // `Conf` messages for all future epochs.
    private Map<Integer, TermMessage> receivedTerm = new TreeMap<>();

    // The values we found in the first _N - f_ `Aux` messages that were in `bin_values`.
    private BoolSet confValues = BoolSet.NONE();

    // The estimate of the decision value in the current epoch.
    private Boolean estimate;

    // A permanent, latching copy of the output value.
    private Boolean decision = null;

    // A cache for messages for future epochs that cannot be handled yet.
    private Map<Long, Map<Integer, ReceivedMessages>> incomingQueue = new TreeMap<>();

    private final MoustefaouiBinaryAgreementMessageFactory messageFactory;

    public MoustefaouiBinaryAgreement(
            String pid,
            Integer replicaId,
            NetworkInfo networkInfo
    ) {
        this.pid = pid;
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;

        this.coin = new Coin(pid, networkInfo.getGroupKey(), networkInfo.getKeyShare());

        this.messageFactory = new MoustefaouiBinaryAgreementMessageFactory(pid, replicaId, round);
    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public Long getRound() {
        return round;
    }

    @Override
    public Coin getCoin() {
        return coin;
    }

    @Override
    public Boolean getEstimate() {
        return estimate;
    }

    @Override
    public Step<Boolean> handleInput(Boolean input) {
        Step<Boolean> step = new Step<>();
        if (!this.canPropose()) {
            step.addFault(pid, "CANNOT PROPOSE");
            return step;
        }
        // Set the initial estimated value to the input value.
        this.estimate = input;

        // Record the value `b` as sent. If it was already there, don't send it again.
        if (this.sentBVal.insert(input)) {
            BValMessage bValMessage = messageFactory.createBValMessage(input);
            step.add(this.sendBValMessage(bValMessage));
        }
        return step;
    }

    @Override
    public Step<Boolean> handleMessage(BinaryAgreementMessage message) {
        Step<Boolean> step = new Step<>();
        Long round = message.getRound();

        if (!message.getPid().equals(pid)) {
            // Message pid does not match protocol instance
            step.addFault(pid, "INVALID PID");

        } else if (this.hasTerminated() || (round < this.getRound() && message.canExpire())) {
            // Message is obsolete: We are already in a later epoch or terminated.
            step.addFault(pid, "MESSAGE OBSOLETE");

        } else if (round > this.getRound() + this.maxFutureRounds) {
            // Message outside of the valid agreement round range
            step.addFault(pid, "MESSAGE OUTSIDE ROUND RANGE");

        } else if (round > this.getRound()) {
            // Message is for a later epoch. We can't handle that yet.
            Map<Integer, ReceivedMessages> epochState = incomingQueue.computeIfAbsent(round, r -> new TreeMap<>());
            ReceivedMessages received = epochState.computeIfAbsent(message.getSender(), s -> new ReceivedMessages());
            received.insert(message);

        } else {
            // Handle message content
            Integer type = message.getType();
            switch (type) {
                case BValMessage.BVAL: {
                    step.add(handleBValMessage((BValMessage) message));
                    break;
                }
                case AuxMessage.AUX: {
                    step.add(handleAuxMessage((AuxMessage) message));
                    break;
                }
                case ConfMessage.CONF: {
                    step.add(handleConfMessage((ConfMessage) message));
                    break;
                }
                case CoinMessage.COIN: {
                    step.add(handleCoinMessage((CoinMessage) message));
                    break;
                }
                case TermMessage.TERM: {
                    step.add(handleTermMessage((TermMessage) message));
                    break;
                }
            }
        }

        return step;
    }

    @Override
    public boolean hasTerminated() {
        return this.decision != null;
    }

    @Override
    public Optional<Boolean> deliver() {
        return Optional.ofNullable(decision);
    }

    // Whether we can still input a value. It is not an error to input if this returns `false`,
    // but it will have no effect on the outcome.
    private Boolean canPropose() {
        return this.getRound() == 0L && this.getEstimate() == null;
    }

    // Handles a `BVal(b)` message.
    //
    // Upon receiving _f + 1_ `BVal(b)`, multicasts `BVal(b)`. Upon receiving _2 f + 1_ `BVal(b)`,
    // updates `bin_values`. When `bin_values` gets its first entry, multicasts `Aux(b)`.
    @Override
    public Step<Boolean> handleBValMessage(BValMessage message) {
        final boolean value = message.getValue();

        Step<Boolean> step = new Step<>();
        if (!this.receivedBVal.getIndex(value).add(message.getSender())) {
            step.addFault(pid, "DUPLICATE BVAL MESSAGE");
            return step;
        }

        // Upon receiving `2*f + 1` valid `BVAL`s for the same value
        int countBVal = this.receivedBVal.getIndex(value).size();
        if (countBVal == (2*networkInfo.getF() + 1)) {
            // Add value to bin values set
            this.binValues.insert(value);

            if (!this.binValues.equals(BoolSet.BOTH())) {
                // First entry: send `Aux` for the value.
                AuxMessage auxMessage = messageFactory.createAuxMessage(value);
                step.add(this.sentAuxMessage(auxMessage));

            } else {
                // Otherwise just check for `Conf` condition.
                step.add(this.tryOutputSbv());
            }
        }

        if (countBVal == this.networkInfo.getF() + 1 && this.sentBVal.insert(value)) {
            BValMessage bValMessage = messageFactory.createBValMessage(value);
            step.add(this.sendMessage(bValMessage, false));
        }
        return step;
    }

    private Step<Boolean> sendBValMessage(BValMessage bValMessage) {
        Step<Boolean> step = new Step<>();
        for (int id=0; id<this.networkInfo.getN(); id++) {
            if (id == this.replicaId) {
                step.add(this.handleBValMessage(bValMessage));
            } else {
                step.add(bValMessage, id);
            }
        }
        return step;
    }

    // Handles an `Aux` message.
    public Step<Boolean> handleAuxMessage(AuxMessage auxMessage) {
        boolean b = auxMessage.getValue();
        if (!this.receivedAux.getIndex(b).add(auxMessage.getSender())) {
            return new Step<>();
        }
        return this.tryOutputSbv();
    }

    private Step<Boolean> sentAuxMessage(AuxMessage auxMessage) {
        Step<Boolean> step = new Step<>();
        for (int id=0; id<this.networkInfo.getN(); id++) {
            if (id == this.replicaId) {
                step.add(this.handleAuxMessage(auxMessage));
            } else {
                step.add(auxMessage, id);
            }
        }
        return step;
    }

    // Handles a `Conf` message. When _N - f_ `Conf` messages with values in `bin_values` have
    // been received, updates the epoch or decides.
    public Step<Boolean> handleConfMessage(ConfMessage message) {
        this.receivedConf.putIfAbsent(message.getSender(), message);
        return this.tryFinishConfRound();
    }

    // Multicast a `Conf(values)` message, and handle it.
    private Step<Boolean> sendConfMessage(ConfMessage message) {
        Step<Boolean> step = new Step<>();
        if (!this.confValues.equals(BoolSet.NONE())) {
            // Only one `Conf` message is allowed in an epoch.
            return step;
        }

        // Trigger the start of the `Conf` round.
        this.confValues = message.getValue();
        if (!this.networkInfo.isValidator()) {
            return step;
        }

        for (int id=0; id<this.networkInfo.getN(); id++) {
            if (id == this.replicaId) {
                step.add(this.handleConfMessage(message));
            } else {
                step.add(message, id);
            }
        }
        return step;
    }

    /// Handles a `ThresholdSign` message. If there is output, starts the next epoch. The function
    /// may output a decision value.
    public Step<Boolean> handleCoinMessage(CoinMessage message) {
        Step<Boolean> step = new Step<>();
        if (this.getCoin().hasDecided()) return step;

        final int senderId = message.getSender();
        final byte[] share = message.getValue();
        this.getCoin().addShare(senderId, share);

        if (this.getCoin().hasDecided()) {
            step.add(this.tryUpdateRound());
        }
        return step;
    }

    // Handles a `Term(v)` message. If we haven't yet decided on a value and there are more than
    // _f_ such messages with the same value from different nodes, performs expedite termination:
    // decides on `v`, broadcasts `Term(v)` and terminates the instance.
    @Override
    public Step<Boolean> handleTermMessage(TermMessage message) {
        Step<Boolean> step = new Step<>();
        this.receivedTerm.putIfAbsent(message.getSender(), message);

        if (this.decision != null) return step;

        Boolean b  = message.getValue();

        if (this.receivedTerm.values().stream().filter(t -> t.getValue() == b).count() > this.networkInfo.getF()) {
            // Check for the expedite termination condition.
            step.add(this.decide(b));

        } else {
            // Otherwise handle the `Term` as a `BVal`, `Aux` and `Conf`.
            step.add(this.handleBValMessage(new BValMessage(pid, message.getSender(), message.getRound(), b)));
            step.add(this.handleAuxMessage(new AuxMessage(pid, message.getSender(), message.getRound(), b)));
            step.add(this.handleConfMessage(new ConfMessage(pid, message.getSender(), message.getRound(), new BoolSet(Stream.of(b).collect(Collectors.toCollection(HashSet::new))))));
        }

        return step;
    }

    private Step<Boolean> sendTermMessage(TermMessage termMessage) {
        Step<Boolean> step = new Step<>();
        for (int id=0; id<this.networkInfo.getN(); id++) {
            if (id == this.replicaId) {
                step.add(this.handleTermMessage(termMessage));
            } else {
                step.add(termMessage, id);
            }
        }
        return step;
    }

    private Step<Boolean> tryOutputSbv() {
        Step<Boolean> step = new Step<>();
        if (this.sbvTerminated || this.binValues.equals(BoolSet.NONE())) return step;

        BoolSet aux_values = BoolSet.NONE();
        int aux_count = 0;
        for (Boolean b : this.binValues.getValues()) {
            if (!this.receivedAux.getIndex(b).isEmpty()) {
                aux_values.insert(b);
                aux_count += this.receivedAux.getIndex(b).size();
            }
        }

        if (aux_count < this.networkInfo.getNumCorrect()) return step;
        this.sbvTerminated = true;

        if (!this.confValues.equals(BoolSet.NONE())) {
            // The `Conf` round has already started.
            //System.out.println("CONF ROUND ALREADY STARTED");
            return step;
        }

        if (this.coin.hasDecided()) {
            this.confValues = this.binValues;
            step.add(this.tryUpdateRound());
        } else {
            // Start the `Conf` message round.
            ConfMessage confMessage = messageFactory.createConfMessage(this.binValues);
            step.add(this.sendConfMessage(confMessage));
        }
        return step;
    }

    // Checks whether the _N - f_ `Conf` messages have arrived, and if so, activates the coin.
    private Step<Boolean> tryFinishConfRound() {
        Step<Boolean> step = new Step<>();
        if (this.confValues.equals(BoolSet.NONE()) || !this.countConf().equals(this.networkInfo.getNumCorrect())) {
            //System.out.println("UNABLE TO FINISH CONF ROUND");
            return step;
        }

        // Invoke the coin
        byte[] share = coin.getMyShare();
        CoinMessage coinMessage = messageFactory.createCoinMessage(share);
        step.add(this.sendCoinMessage(coinMessage));

        step.add(this.tryUpdateRound());
        return step;
    }

    private Step<Boolean> sendCoinMessage(CoinMessage coinMessage) {
        Step<Boolean> step = new Step<>();
        for (int id=0; id<this.networkInfo.getN(); id++) {
            if (id == this.replicaId) {
                step.add(this.handleCoinMessage(coinMessage));
            } else {
                step.add(coinMessage, id);
            }
        }
        return step;
    }

    // Counts the number of received `Conf` messages with values in `bin_values`.
    private Integer countConf() {
        return (int) this.receivedConf.values().stream()
                .filter(cm -> cm.getValue().isSubset(this.binValues)).count();
    }

    // If this round's coin value or conf values are not known yet, does nothing, otherwise
    // updates the round or decides.
    //
    // With two conf values, the next round's estimate is the coin value. If there is only one conf
    // value and that disagrees with the coin, the conf value is the next round's estimate. If
    // the unique conf value agrees with the coin, terminates and decides on that value.
    @Override
    public Step<Boolean> tryUpdateRound() {
        Step<Boolean> step = new Step<>();
        if (this.decision != null) return step;
        if (!this.coin.hasDecided()) return step; // Still waiting for coin value.
        if (this.confValues.equals(BoolSet.NONE())) return step; // Still waiting for conf value.

        Set<Boolean> defBinValues = confValues.getValues();
        Boolean coinValue = this.coin.getValue();
        if (defBinValues.size() == 1) {
            if (defBinValues.contains(coinValue)) {
                step.add(this.decide(coinValue));
            } else {
                step.add(this.updateRound(defBinValues.iterator().next()));
            }
        } else {
            step.add(this.updateRound(coinValue));
        }
        return step;
    }

    private Step<Boolean> updateRound(Boolean estimate) {
        //System.out.println("UPDATED TO ROUND - " + (round+1));
        this.round += 1;
        this.binValues = BoolSet.NONE();
        this.receivedBVal = new BoolMultimap();
        this.sentBVal = BoolSet.NONE();
        this.receivedAux = new BoolMultimap();
        this.sbvTerminated = false;
        this.coin.reset(round);
        this.receivedConf = new TreeMap<>();
        this.confValues = BoolSet.NONE();
        this.messageFactory.setRound(round);

        Step<Boolean> step = new Step<>();

        // TODO move to propose method
        this.estimate = estimate;
        if (this.sentBVal.insert(estimate)) {
            BValMessage bValMessage = messageFactory.createBValMessage(estimate);
            step.add(this.sendBValMessage(bValMessage));
        }

        // Deliver pending messages
        Map<Integer, ReceivedMessages> pending = incomingQueue.computeIfAbsent(round, r -> new TreeMap<>());
        for (ReceivedMessages receivedMessages: pending.values()) {
            for (BinaryAgreementMessage message: receivedMessages.getMessages()) {
                step.add(this.handleMessage(message));
            }
        }
        return step;
    }

    // Decides on a value and broadcasts a `Term` message with that value.
    private Step<Boolean> decide(Boolean value) {
        Step<Boolean> step = new Step<>();
        if (this.decision != null) return step;

        // Latch the decided state.
        this.decision = value;
        step.add(decision);

        TermMessage message = messageFactory.createTermMessage(value);
        step.add(this.sendTermMessage(message));
        return step;
    }

    private Step<Boolean> sendMessage(BinaryAgreementMessage message, Boolean includeSelf) {
        Step<Boolean> step = new Step<>();
        for (int target=0; target < this.networkInfo.getN(); target++) {
            if (target == this.replicaId) {
                if (includeSelf) step.add(this.handleMessage(message));
            } else {
                step.add(message, target);
            }
        }
        return step;
    }
}
