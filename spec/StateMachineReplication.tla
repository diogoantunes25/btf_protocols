---------------------- MODULE StateMachineReplication ----------------------
EXTENDS Naturals, Sequences, FiniteSets, SystemModel, TLC
CONSTANTS Command
VARIABLES state, cbctx, bactx

ConsistentBroadcast ==
  INSTANCE ConsistentBroadcast
  WITH BroadcastId <- 1..Cardinality(Command),
       Message <- Command,
       broadcast <- cbctx

BinaryAgreement ==
  INSTANCE BinaryAgreement WITH BADomain <- 1..(Cardinality(Process)*Cardinality(Command)), context <- bactx

INSTANCE PriorityQueue WITH PQDomain <- 1..Cardinality(Command), PQRange <- Command
-----------------------------------------------------------------------------
fn == CHOOSE fn \in [1..n -> Process]:
          \A p \in Process: \E i \in 1..n: fn[i] = p  


Executed(p) == state[p].executed

(* The current sequence number at process p. *)
Sequence(p) == state[p].sequence

(* The current subject at process p as a function the current sequence. *)
\*Subject(p) == ((Sequence(p) - 1) % n) + 1
Subject(p) == fn[((Sequence(p) - 1) % n) + 1]
  
AllQueues(p) == state[p].queue

(* The queue of process q at process p. *)
Queue(p, q) == AllQueues(p)[q]

(* The queue of process p at process p. *)
LocalQueue(p) == Queue(p, p)

(* The queue of current subject at process p. *)
SubjectQueue(p) == Queue(p, Subject(p))

(* Whether a command has been executed. *)
HasExecuted(p, cmd) == \E i \in DOMAIN state[p].executed: state[p].executed[i] = cmd

(* Whether every command has been executed. *)
AllExecuted(p) == Len(Executed(p)) = Cardinality(Command)


State(sequence, executed, queue) ==
  [sequence |-> sequence, executed |-> executed, queue |-> queue]

UpdateState(p, sequence, executed, queue) ==
  state' = p :> State(sequence, executed, queue) @@ state
  
UpdateQueue(p, q, queue) ==
  UpdateState(p, Sequence(p), Executed(p), q :> queue @@ AllQueues(p))

(* 
 * Receives a command from a client. The command is inserted in the local
 * queue and is consistent broadcast.
 *)
RcvCommand(p, cmd) ==
  /\ ~HasExecuted(p, cmd)
  /\ \A q \in CorrectProcess: ~PQExists(Queue(p, q), cmd)
  /\ UpdateQueue(p, p, PQInsertAtNext(LocalQueue(p), cmd))
  /\ LET priority == PQNextPriority(LocalQueue(p))
     IN ConsistentBroadcast!Send(p, priority, cmd)
  /\ UNCHANGED bactx

(*
 * Receives a command from another process via consistent broadcast. The
 * command is inserted in the replica of process' queue.
 *)
RcvConsistentBroadcast(p, sender, priority) ==
  /\ LET cmd == ConsistentBroadcast!Out(sender, priority, p)
         queue == Queue(p, sender)
     IN /\ cmd \in Command
        /\ UpdateQueue(p, sender, PQInsertAt(queue, priority, cmd))
  /\ UNCHANGED <<cbctx, bactx>>

(* Propose a value to binary agreement *)
Propose(p) ==  
  /\ ~AllExecuted(p)
  (* Process p has not proposed a value yet. *) 
  /\ BinaryAgreement!Proposal(Sequence(p), p) \notin {0,1}
  (* There is a command at the head of the local replica of some process' queue. *)
  /\ \E q \in Process: PQHead(Queue(p, q)) \in Command
  (* Action: propose a value. *) 
  /\ LET proposal == IF PQHead(SubjectQueue(p)) \in Command
                     THEN 1
                     ELSE 0
     IN BinaryAgreement!Propose(Sequence(p), p, proposal) 
  /\ UNCHANGED <<state, cbctx>>

ExecuteCommand(p, cmd) == Append(Executed(p), cmd)

(* Decide the current binary agreement instance. *)
Decide(p) ==
  /\ UNCHANGED <<bactx, cbctx>>
  /\ ~AllExecuted(p)
  /\ LET decision == BinaryAgreement!Decision(Sequence(p), p)
     IN /\ decision \in {0,1}
        /\ IF decision = 1
           THEN LET cmd == ConsistentBroadcast!GetMessage(Subject(p), PQHeadPriority(SubjectQueue(p)))
                IN UpdateState(p,
                               Sequence(p) + 1,
                               ExecuteCommand(p, cmd), \* Append cmd to executed 
                               [q \in Process |-> PQRemoveByValue(Queue(p, q), cmd)]) \* Remove cmd from all queues
           ELSE UpdateState(p,
                            Sequence(p) + 1,
                            Executed(p),
                            AllQueues(p))
-----------------------------------------------------------------------------
Init ==
  /\ ConsistentBroadcast!Init
  /\ BinaryAgreement!Init
  /\ state = [p \in CorrectProcess |-> [
                (* The id of the current binary agreement instance. *)
                sequence |-> 1,
                (* The sequence of executed state machine commands. *)
                executed |-> << >>, 
                (* The piority queues, one for each process. *)
                queue |-> [q \in Process |-> PQueue]]]

Next ==
  \/ ConsistentBroadcast!Next /\ UNCHANGED <<state, bactx>>
  \/ BinaryAgreement!Next /\ UNCHANGED <<state, cbctx>>
  \/ \E cmd \in Command: RcvCommand(CHOOSE p \in CorrectProcess: TRUE, cmd)
  \* \/ \E cmd \in Command: \E p \in CorrectProcess: RcvCommand(p, cmd)
  \/ \E p \in CorrectProcess: \E sender \in Process: \E priority \in 1..Cardinality(Command): RcvConsistentBroadcast(p, sender, priority)
  \/ \E p \in CorrectProcess: Propose(p)
  \/ \E p \in CorrectProcess: Decide(p)
  
        
Spec == 
  /\ Init
  /\ [][Next]_<<state, cbctx, bactx>> 
=============================================================================
\* Modification History
\* Last modified Thu Apr 15 15:55:32 WEST 2021 by hmz
\* Created Thu Apr 08 13:52:51 WEST 2021 by hmz
