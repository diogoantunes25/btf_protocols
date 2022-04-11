package pt.tecnico.ulisboa.hbbft.example.agreement.vba;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import pt.tecnico.ulisboa.hbbft.MessageEncoder;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.agreement.vba.ValidatedBinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.agreement.vba.abba.messages.CoinMessage;
import pt.tecnico.ulisboa.hbbft.agreement.vba.abba.messages.MainVoteMessage;
import pt.tecnico.ulisboa.hbbft.agreement.vba.abba.messages.PreProcessMessage;
import pt.tecnico.ulisboa.hbbft.agreement.vba.abba.messages.PreVoteMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

import java.util.Base64;

public class VbaMessageEncoder implements MessageEncoder<String> {

    private final Gson gson = new Gson();

    @Override
    public String encode(ProtocolMessage message) {
        assert (message instanceof ValidatedBinaryAgreementMessage);
        switch (message.getType()) {
            case PreProcessMessage.PRE_PROCESS:
                return this.encodePreProcessMessage((PreProcessMessage) message);
            case PreVoteMessage.PRE_VOTE:
                return this.encodePreVoteMessage((PreVoteMessage) message);
            case MainVoteMessage.MAIN_VOTE:
                return this.encodeMainVoteMessage((MainVoteMessage) message);
            case CoinMessage.COIN:
                return this.encodeCoinMessage((CoinMessage) message);
            default:
                return null;
        }
    }

    private JsonObject encodeVbaMessage(ValidatedBinaryAgreementMessage message) {
        JsonObject root = new JsonObject();
        root.addProperty("pid", message.getPid());
        root.addProperty("type", message.getType());
        root.addProperty("sender", message.getSender());
        root.addProperty("round", message.getRound());
        return root;
    }

    private String encodePreProcessMessage(PreProcessMessage message) {
        JsonObject root = encodeVbaMessage(message);
        root.addProperty("value", message.getValue());
        root.addProperty("justification", Base64.getEncoder().encodeToString(message.getJustification()));
        root.addProperty("share", Base64.getEncoder().encodeToString(message.getShare().getSig().toByteArray()));
        return root.toString();
    }

    private String encodePreVoteMessage(PreVoteMessage message) {
        JsonObject root = encodeVbaMessage(message);
        root.addProperty("value", message.getValue());
        root.addProperty("justification", Base64.getEncoder().encodeToString(message.getJustification()));
        root.addProperty("share", Base64.getEncoder().encodeToString(message.getShare().getSig().toByteArray()));
        return root.toString();
    }

    private String encodeMainVoteMessage(MainVoteMessage message) {
        // FIXME multiple possible justifications
        JsonObject root = encodeVbaMessage(message);
        root.addProperty("value", message.getValue());
        root.addProperty("justification", Base64.getEncoder().encodeToString(message.getJustification()));
        root.addProperty("share", Base64.getEncoder().encodeToString(message.getShare().getSig().toByteArray()));
        return root.toString();
    }

    private String encodeCoinMessage(CoinMessage message) {
        JsonObject root = encodeVbaMessage(message);
        root.addProperty("share", Base64.getEncoder().encodeToString(message.getShare().getSig().toByteArray()));
        return root.toString();
    }

    @Override
    public ProtocolMessage decode(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        final String pid = root.get("pid").getAsString();
        final int type = root.get("type").getAsInt();
        final int sender = root.get("sender").getAsInt();
        final long round = root.get("round").getAsLong();

        if (type == CoinMessage.COIN) {
            byte[] share = Base64.getDecoder().decode(root.get("share").getAsString().getBytes());
            return new CoinMessage(pid, sender, round, new SigShare(sender + 1, share));
        }
        boolean value = root.get("value").getAsBoolean();
        byte[] justification = Base64.getDecoder().decode(root.get("justification").getAsString().getBytes());
        byte[] share = Base64.getDecoder().decode(root.get("share").getAsString().getBytes());
        switch (type) {
            case PreProcessMessage.PRE_PROCESS:
                return new PreProcessMessage(pid, sender, round, value, justification, new SigShare(sender + 1, share));
            case PreVoteMessage.PRE_VOTE:
                return new PreVoteMessage(pid, sender, round, value, justification, new SigShare(sender + 1, share));
            case MainVoteMessage.MAIN_VOTE:
                return new MainVoteMessage(pid, sender, round, value, justification, new SigShare(sender + 1, share));
        }

        return null;
    }
}
