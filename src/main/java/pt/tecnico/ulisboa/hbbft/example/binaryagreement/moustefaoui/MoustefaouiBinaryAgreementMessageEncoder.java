package pt.tecnico.ulisboa.hbbft.example.binaryagreement.moustefaoui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pt.tecnico.ulisboa.hbbft.MessageEncoder;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.BoolSet;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.*;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class MoustefaouiBinaryAgreementMessageEncoder implements MessageEncoder<String> {

    private Gson gson = new Gson();

    @Override
    public String encode(ProtocolMessage message) {
        switch (message.getType()) {
            case BValMessage.BVAL:
                return this.encodeBValMessage((BValMessage) message);
            case AuxMessage.AUX:
                return this.encodeAuxMessage((AuxMessage) message);
            case ConfMessage.CONF:
                return this.encodeConfMessage((ConfMessage) message);
            case CoinMessage.COIN:
                return this.encodeCoinMessage((CoinMessage) message);
            case TermMessage.TERM:
                return this.encodeTermMessage((TermMessage) message);
            default:
                return null;
        }
    }

    private JsonObject encodeBinaryAgreementMessage(BinaryAgreementMessage message) {
        JsonObject root = new JsonObject();
        root.addProperty("pid", message.getPid());
        root.addProperty("type", message.getType());
        root.addProperty("sender", message.getSender());
        root.addProperty("round", message.getRound());
        return root;
    }

    public String encodeBValMessage(BValMessage message) {
        JsonObject root = encodeBinaryAgreementMessage(message);
        root.addProperty("value", message.getValue());
        return root.toString();
    }

    public String encodeAuxMessage(AuxMessage message) {
        JsonObject root = encodeBinaryAgreementMessage(message);
        root.addProperty("value", message.getValue());
        return root.toString();
    }

    public String encodeCoinMessage(CoinMessage message) {
        JsonObject root = encodeBinaryAgreementMessage(message);
        root.addProperty("value", Base64.getEncoder().encodeToString(message.getValue()));
        return root.toString();
    }

    public String encodeConfMessage(ConfMessage message) {
        JsonObject root = encodeBinaryAgreementMessage(message);
        JsonArray boolValues = new JsonArray();
        for (Boolean b : message.getValue().getValues())
            boolValues.add(b);
        root.add("value", boolValues);
        return root.toString();
    }

    public String encodeTermMessage(TermMessage message) {
        JsonObject root = encodeBinaryAgreementMessage(message);
        root.addProperty("value", message.getValue());
        return root.toString();
    }

    @Override
    public BinaryAgreementMessage decode(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        int type = root.get("type").getAsInt();
        switch (type) {
            case BValMessage.BVAL:
                return this.decodeBValMessage(data);
            case AuxMessage.AUX:
                return this.decodeAuxMessage(data);
            case ConfMessage.CONF:
                return this.decodeConfMessage(data);
            case CoinMessage.COIN:
                return this.decodeCoinMessage(data);
            case TermMessage.TERM:
                return this.decodeTermMessage(data);
            default:
                return null;
        }
    }

    public BValMessage decodeBValMessage(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String pid = root.get("pid").getAsString();
        int senderId = root.get("sender").getAsInt();
        long round = root.get("round").getAsLong();

        Boolean value = root.get("value").getAsBoolean();

        return new BValMessage(pid, senderId, round, value);
    }

    public AuxMessage decodeAuxMessage(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String pid = root.get("pid").getAsString();
        int senderId = root.get("sender").getAsInt();
        long round = root.get("round").getAsLong();

        Boolean value = root.get("value").getAsBoolean();

        return new AuxMessage(pid, senderId, round, value);
    }

    public CoinMessage decodeCoinMessage(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String pid = root.get("pid").getAsString();
        int senderId = root.get("sender").getAsInt();
        long round = root.get("round").getAsLong();

        byte[] value = Base64.getDecoder().decode(root.get("value").getAsString());

        return new CoinMessage(pid, senderId, round, value);
    }

    public ConfMessage decodeConfMessage(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String pid = root.get("pid").getAsString();
        int senderId = root.get("sender").getAsInt();
        long round = root.get("round").getAsLong();

        Set<Boolean> boolValues = new HashSet<>();
        for (JsonElement element: root.getAsJsonArray("value"))
            boolValues.add(element.getAsBoolean());
        BoolSet value = new BoolSet(boolValues);

        return new ConfMessage(pid, senderId, round, value);
    }

    public TermMessage decodeTermMessage(String data) {
        JsonObject root = gson.fromJson(data, JsonObject.class);
        String pid = root.get("pid").getAsString();
        int senderId = root.get("sender").getAsInt();
        long round = root.get("round").getAsLong();

        Boolean value = root.get("value").getAsBoolean();

        return new TermMessage(pid, senderId, round, value);
    }
}
