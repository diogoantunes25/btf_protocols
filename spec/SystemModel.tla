---------------------------- MODULE SystemModel ----------------------------
LOCAL INSTANCE Integers
LOCAL INSTANCE FiniteSets

CONSTANT Process \* The set of all processes

(*
The total number of processes.
*)
n == Cardinality(Process)

(*
The upper bound on the number of Byzantine processes as a function of n. This
is NOT the same as the cardinality of the set of Byzantine processes.
*) 
f == CHOOSE f \in 1..n: n = 3*f+1 \* TODO: Fix this

(*
The set of correct processes.
*)
CorrectProcess == CHOOSE S \in SUBSET Process: Cardinality(S) = n-f

(*
The set of Byzantine processes.
*)
ByzantineProcess == Process \ CorrectProcess 

(*
The set of correct processes is disjoint from the set of Byzantine processes,
and the number of correct processes is more than half the number of Byzantine
processes (i.e., n > 3f). 
*)
ASSUME /\ CorrectProcess \intersect ByzantineProcess = {}
       /\ Cardinality(CorrectProcess) > 2 * Cardinality(ByzantineProcess)
=============================================================================
\* Modification History
\* Last modified Thu Apr 15 16:15:48 WEST 2021 by hmz
\* Created Sat Apr 10 08:51:53 WEST 2021 by hmz
