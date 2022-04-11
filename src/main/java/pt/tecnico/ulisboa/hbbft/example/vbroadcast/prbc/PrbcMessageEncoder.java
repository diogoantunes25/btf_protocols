package pt.tecnico.ulisboa.hbbft.example.vbroadcast.prbc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import pt.tecnico.ulisboa.hbbft.MessageEncoder;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;
import pt.tecnico.ulisboa.hbbft.vbroadcast.prbc.messages.DoneMessage;

import java.util.Base64;

public class PrbcMessageEncoder implements MessageEncoder<String> {

    private final Gson gson = new Gson();

    private final MessageEncoder<String> rbcEncoder;

    public PrbcMessageEncoder(MessageEncoder<String> rbcEncoder) {
        this.rbcEncoder = rbcEncoder;
    }

    @Override
    public String encode(ProtocolMessage message) {
        if (message instanceof DoneMessage) {
            return this.encodeDoneMessage((DoneMessage) message);

        } else {
            return this.rbcEncoder.encode(message);
        }
    }

    private String encodeDoneMessage(DoneMessage message) {
        JsonObject root = new JsonObject();
        root.addProperty("pid", message.getPid());
        root.addProperty("type", message.getType());
        root.addProperty("sender", message.getSender());
        root.addProperty("share", Base64.getEncoder().encodeToString(message.getShare().getSig().toByteArray()));
        return root.toString();
    }

    @Override
    public ProtocolMessage decode(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        final int type = root.get("type").getAsInt();

        if (type == DoneMessage.DONE) {
            final String pid = root.get("pid").getAsString();
            final int senderId = root.get("sender").getAsInt();
            final byte[] share = Base64.getDecoder().decode(root.get("share").getAsString().getBytes());
            return new DoneMessage(pid, senderId, new SigShare(senderId+1, share));

        } else {
            return rbcEncoder.decode(data);
        }
    }
}
