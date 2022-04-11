package pt.tecnico.ulisboa.hbbft.abc.honeybadger;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.IAtomicBroadcast;

public interface IHoneyBadger extends IAtomicBroadcast {

    Epoch getEpoch(Long epochId);

    Step<Block> handleEpochStep(Step<Batch> epochStep);
}
