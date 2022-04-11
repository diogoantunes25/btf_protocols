# Honey Badger BFT

This repository contains a [Java](https://docs.oracle.com/en/java/) library of the Honey Badger Byzantine Fault Tolerant (BFT) consensus algorithm. The research and protocols for this algorithm are explained in detail in "[The Honey Badger of BFT Protocols](https://eprint.iacr.org/2016/199.pdf)" by Miller et al., 2016.

_**Note:** This library is a work in progress and parts of the algorithm are still in development._


## Algorithms

- **[Honey Badger](src/main/java/pt/tecnico/ulisboa/hbbft/abc/honeybadger):** Each node inputs transactions. The protocol outputs a sequence of batches of transactions.

- **[Subset](src/main/java/pt/tecnico/ulisboa/hbbft/subset):** Each node inputs data. The nodes agree on a subset of suggested data.

- **[Broadcast](src/main/java/pt/tecnico/ulisboa/hbbft/broadcast):** A proposer node inputs data and every node receives this output.

- **[Binary Agreement](src/main/java/pt/tecnico/ulisboa/hbbft/binaryagreement):** Each node inputs a binary value. The nodes agree on a value that was input by at least one correct node.


# Algorithms

## Reliable Broadcast

## Consistent Broadcast

- **[Avid](src/main/java/pt/tecnico/ulisboa/hbbft/broadcast/bracha)** Reliable 
- **[Bracha](src/main/java/pt/tecnico/ulisboa/hbbft/broadcast/bracha):** A reliable broadcast protocol by Bracha.
- **[Echo](src/main/java/pt/tecnico/ulisboa/hbbft/broadcast/echo):** A consistent broadcast protocol by Reiter based on digital signatures.


## Binary Agreement

## Asynchronous Common Subset

## Atomic Broadcast


## Getting Started

### Requirements:
- [JDK-11](https://www.oracle.com/pt/java/technologies/javase-jdk11-downloads.html) - Java SE Development Kit 11
- [Maven](https://maven.apache.org/) - Dependency Management
   

### Installation:
In order to install hbbft-java the first step is to download the source code by typing:
```sh
$ git clone https://github.com/nosofa/ao-master-thesis.git
```

Afterwards navigate into the recently cloned repository...
```sh
$ cd ao-master-thesis
```

And install all the required dependencies using Maven...
```sh
$ mvn compile
```


### Configuration:
TODO


### Running:
TODO


### Testing:
```sh
$ mvn test
```


## References
- [The Honey Badger of BFT Protocols](https://eprint.iacr.org/2016/199.pdf)

- [BEAT: Asynchronous BFT Made Practical](https://www.csee.umbc.edu/~hbzhang/files/beat.pdf)

- Other implementations:
    - [Python](https://github.com/initc3/HoneyBadgerBFT-Python)
    - [Go](https://github.com/anthdm/hbbft)
    - [Rust](https://github.com/poanetwork/hbbft/)
    

## Protocol Architecture:
![hbbft-acs.png](https://i.postimg.cc/CLnkvRcZ/hbbft-acs.png)
The structure of ACS in HoneyBadgerBFT ([source](https://eprint.iacr.org/2020/841.pdf)).

[![dumbo1-acs.png](https://i.postimg.cc/Jn2kRjCR/dumbo1-acs.png)](https://postimg.cc/JDjnqDhF)
The structure of ACS in Dumbo1 ([source](https://eprint.iacr.org/2020/841.pdf)).