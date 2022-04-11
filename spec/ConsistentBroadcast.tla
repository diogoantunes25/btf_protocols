------------------------ MODULE ConsistentBroadcast ------------------------
CONSTANTS BroadcastId,
          Message,
          Process
VARIABLE broadcast

LOCAL INSTANCE Integers
LOCAL INSTANCE SystemModel

nil == -1

ASSUME Message \intersect {nil} = {}

State(message, Delivered, Output) ==
  [message |-> message, Delivered |-> Delivered, Output |-> Output]

Out(sender, bid, receiver) == broadcast[sender][bid].Output[receiver]
GetMessage(sender, bid) == broadcast[sender][bid].message

UpdateState(sender, bid, msg, Delivered, Output) ==
  broadcast' = [broadcast EXCEPT ![sender] = [broadcast[sender] EXCEPT ![bid] = State(msg, Delivered, Output)]] 

Send(sender, bid, message) ==
  /\ broadcast[sender][bid].message = nil
  /\ UpdateState(sender,
                 bid,
                 message,
                 {}, \* Delivered
                 [p \in CorrectProcess |-> nil]) \* Output

ConsistentDeliver(sender, bid, receiver) ==
  LET b == broadcast[sender][bid]
  IN /\ b.message # nil
     /\ receiver \notin b.Delivered
     /\ UpdateState(sender,
                    bid,
                    b.message,
                    b.Delivered \union {receiver}, \* Delivered
                    [b.Output EXCEPT ![receiver] = b.message]) \* Output

FakeDeliver(sender, bid, receiver) ==
  LET b == broadcast[sender][bid]
  IN /\ sender \in ByzantineProcess
     /\ b.message # nil
     /\ receiver \notin b.Delivered
     /\ UpdateState(sender, bid, b.message, b.Delivered \union {receiver}, b.Output)

TypeOK ==
  broadcast \in [Process ->
                  [BroadcastId ->
                    [message: Message \union {nil},
                     Delivered: SUBSET CorrectProcess,
                     Output: [CorrectProcess -> Message \union {nil}]]]]
  
Init ==
  broadcast = [sender \in Process |-> 
                [bid \in BroadcastId |->
                  State(nil, \* message,
                        {}, \* Delivered
                        [p \in CorrectProcess |-> nil])]] \* Output

Next == 
  \E sender \in DOMAIN broadcast:
    \E bid \in DOMAIN broadcast[sender]:
      \E receiver \in CorrectProcess:
        \/ ConsistentDeliver(sender, bid, receiver)
        \/ FakeDeliver(sender, bid, receiver)

Spec == Init /\ [][Next]_broadcast

(* Invariants. *)
Termination ==
  \E sender \in DOMAIN broadcast:
    \E bid \in DOMAIN broadcast[sender]:
      LET b == broadcast[sender][bid]
      IN <>(b.sender \in CorrectProcess => \A p \in CorrectProcess: b.Output[p] # nil)
          
Agreement ==
  \E sender \in DOMAIN broadcast:
    \E bid \in DOMAIN broadcast[sender]:
      LET b == broadcast[sender][bid]
      IN \A p \in CorrectProcess: b.Output[p] # nil => b.Output[p] = b.msg 
=============================================================================
\* Modification History
\* Last modified Thu Apr 15 15:37:20 WEST 2021 by hmz
\* Created Sat Apr 10 13:44:00 WEST 2021 by hmz
