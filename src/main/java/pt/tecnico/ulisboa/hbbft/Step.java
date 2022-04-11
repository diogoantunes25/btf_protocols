package pt.tecnico.ulisboa.hbbft;

import java.util.List;
import java.util.Vector;

public class Step<O> {

    private Vector<O> output = new Vector<>();
    private Vector<TargetedMessage> messages = new Vector<>();
    private Vector<String> faults = new Vector<>();

    public Step() {
    }

    public Step(Vector<TargetedMessage> messages) {
        this.messages = messages;
    }

    public void add(Step<O> other) {
        this.output.addAll(other.getOutput());
        this.messages.addAll(other.getMessages());
        this.faults.addAll(other.getFaults());
    }

    public void add(O output) {
        this.output.add(output);
    }

    public void add(Vector<TargetedMessage> messages) {
        this.messages.addAll(messages);
    }

    public void add(ProtocolMessage message, Integer target) {
        this.messages.add(new TargetedMessage(message, target));
    }

    public void add(ProtocolMessage message, List<Integer> targets) {
        this.messages.add(new TargetedMessage(message, targets));
    }

    public void addFault(String pid, String label) {
        this.faults.add(String.format("%s:%s", pid, label));
    }

    public Vector<O> getOutput() {
        return output;
    }

    public Vector<TargetedMessage> getMessages() {
        return messages;
    }

    public Vector<String> getFaults() {
        return faults;
    }
}
