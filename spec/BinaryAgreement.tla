-------------------------- MODULE BinaryAgreement --------------------------
CONSTANTS BADomain,
          Process
VARIABLE context

LOCAL INSTANCE Integers
LOCAL INSTANCE SystemModel
LOCAL INSTANCE FiniteSets


(* The nil value represents the absence of input or output value. *)
nil == -1

Decision(baid, p) == 
  IF p \in context[baid].Decided
  THEN context[baid].choice
  ELSE nil
  
Proposal(baid, p) == context[baid].proposal[p]

UpdateContext(baid, ctx) ==
  context' = [context EXCEPT ![baid] = ctx]

UpdateProposal(baid, p, proposal) ==
  LET ctx == context[baid]
  IN UpdateContext(baid, [ctx EXCEPT !.proposal = proposal])

SetProposal(baid, p, v) ==
  LET proposal == context[baid].proposal
  IN UpdateProposal(baid, p, [proposal EXCEPT ![p] = v])

(* Correct process p proposes value v. *)
Propose(baid, p, v) ==
  /\ p \in CorrectProcess 
  /\ v \in {0,1}
  /\ Proposal(baid, p) = nil
  /\ SetProposal(baid, p, v)

(* Correct process p decides chosen value. *)
Decide(baid, p) ==
  /\ p \in CorrectProcess
  /\ context[baid].choice \in {0,1}
  /\ context' = [context EXCEPT ![baid] = [context[baid] EXCEPT !.Decided = context[baid].Decided \union {p}]]

(* Whether a value v is a valid decision value. *)
Valid(baid, v) ==
  (* At least n-2f correct processes proposed a value. *)
  /\ LET Proposed == {p \in CorrectProcess: Proposal(baid, p) # nil}  
     IN Cardinality(Proposed) >= n-2*f
  (* If f+1 correct processes proposed 1, then decision must be 1. *) 
  /\ (\E CorrectSubset \in SUBSET CorrectProcess:
       /\ Cardinality(CorrectSubset) = f+1
       /\ \A p \in CorrectSubset: Proposal(baid, p) = 1) => v = 1
 
(* Chooses decision value for agreement instance baid. *)
Choose(baid) ==
  /\ context[baid].choice \notin {0,1}
  /\ \E v \in {0,1}:
       /\ Valid(baid, v)
       /\ context' = [context EXCEPT ![baid] = [context[baid] EXCEPT !.choice = v]]
       
------------------------------------------------------------------------------------


Init ==
  context = [baid \in BADomain |-> [proposal |-> [p \in CorrectProcess |-> nil], choice |-> nil, Decided |-> {}]]

Next == 
  \E baid \in DOMAIN context:
    \/ Choose(baid)
    \/ \E p \in CorrectProcess: Decide(baid, p)

Spec == Init /\ [][Next]_context
=============================================================================
\* Modification History
\* Last modified Thu Apr 15 15:37:05 WEST 2021 by hmz
\* Created Sat Apr 10 10:08:48 WEST 2021 by hmz
