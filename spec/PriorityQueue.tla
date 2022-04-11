--------------------------- MODULE PriorityQueue ---------------------------
LOCAL INSTANCE Integers
LOCAL INSTANCE TLC

(* Priority Queue specification. *)
CONSTANT PQDomain \* The set of priority values for which the queue is defined.
CONSTANT PQRange  \* The set of values that the queue takes. 

PQSlotEmpty == -1   \* Represents an empty value in the queue (i.e., never inserted).          
PQSlotRemoved == -2 \* Represents a value removed from the queue.

(* PQSlotEmpty and PQSlotRemoved have a special meaning and can't be in PQRange. *)
ASSUME PQRange \intersect {PQSlotEmpty, PQSlotRemoved} = {}

(* Defines piority queue as a function from piority to the associated value. *)
PQueue == [priority \in PQDomain |-> PQSlotEmpty]

(* The priority of the head of the queue. It can be associated with an empty value. *)
PQHeadPriority(Q) == CHOOSE priority \in DOMAIN Q:
                       /\ Q[priority] # PQSlotRemoved
                       /\ \A p \in DOMAIN Q: (priority =< p \/ Q[p] = PQSlotRemoved)

(* The value at the head of the queue. It can be an empty value. *)
PQHead(Q) == Q[PQHeadPriority(Q)]

(*
 * Queue with value inserted at priority. The queue is unchanged if a non-empty value 
 * is already associated with the priority.
 *)
PQInsertAt(Q, priority, value) == IF Q[priority] = PQSlotEmpty
                                  THEN priority :> value @@ Q
                                  ELSE Q

(* The next available priority in the queue. It is always associated with an empty value. *)
PQNextPriority(Q) == CHOOSE priority \in DOMAIN Q:
               /\ Q[priority] = PQSlotEmpty
               /\ \A p \in DOMAIN Q: (priority =< p \/ Q[p] # PQSlotEmpty)

(* Queue with value inserted at the next available priority. *)
PQInsertAtNext(Q, value) == PQInsertAt(Q, PQNextPriority(Q), value)

(* Queue with value at priority removed. *)
PQRemoveAt(Q, priority) == PQInsertAt(Q, priority, PQSlotRemoved)

(* Whether a value exists in the queue. *)
PQExists(Q, value) == \E priority \in DOMAIN Q: Q[priority] = value

(*
 * Queue with an arbitrary instance of value removed. The queue is unchanged
 * if the value does not exist in the queue.
 *)
PQRemoveByValue(Q, value) == IF PQExists(Q, value)
                             THEN PQRemoveAt(Q, CHOOSE priority \in DOMAIN Q: Q[priority] = value)
                             ELSE Q

(* Type invariant. *)
PQTypeOK(Q) == Q \in [PQDomain -> PQRange \union {PQSlotEmpty, PQSlotRemoved}]
=============================================================================
\* Modification History
\* Last modified Wed Apr 14 18:56:27 WEST 2021 by hmz
\* Created Sat Apr 10 13:43:44 WEST 2021 by hmz
