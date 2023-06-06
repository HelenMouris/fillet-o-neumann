# fillet-o-neumann
A java simulation for a Von Neumann based Computer Architecture.

## Main Memory Architecture 
A von neumann Architecture where program data and instruction data are stored in the same memory<br>
Main Memory Size: 2048 (from 0 to 1023 instruction data and from 1024 to 2047 program data) 

## Registers 
- Size: 32 bits
- 31 General purpose Registers
- 1 Zero Register
- 1 program counter

</details>

## Instruction Set Architecture

<details>
<summary>Instruction Types</summary>

### R Format

| Opcode | R1 | R2 | R3 | shamt|
| ------ | -- | -- | -- | ---- |
| 4 bits | 5 bits | 5 bits | 5 bits | 13 bits |

### I Format

| Opcode | R1 | R2 | immediate |
| ------ | -- | -- | --------- |
| 4 bits | 5 bits | 5 bits | 18 bits |

### J Format

| Opcode | address |
| ------ | ------- |
| 4 bits | 28 bits |

</details>


<details>
	<summary> Instruction Set </summary>

|Name | Opcode| Type| Format| Operation|
|-----|-----|-----|-----|-----|
|ADD| 0000| R| ADD R1 R2 R3|R1 = R2 + R3|
|SUB| 0001| R| SUB R1 R2 R3| R1 = R2 - R3|
|MUL| 0010| R| MUL R1 R2 R3|R1 = R2 * R3|
|MOVI| 0011| I| MOVI R1 IMM|R1 = IMM|
|JEQ| 0100| I| JEQ R1 R2 IMM|IF(R1 == R2) {PC = PC+1+IMM}|
|AND| 0101| R| AND R1 R2 R3|R1 = R2 & R3|
|XORI| 0110| I| XORI R1 R2 IMM|R1 = R2 ⊕ IMM|
|JMP| 0111| J| JMP ADDRESS|PC = concatenate PC[31:28] and ADDRESS|
|LSL| 1000| R| LSL R1 R2 SHAMT|R1 = R2 << SHAMT|
|LSR| 1001| R| LSR R1 R2 SHAMT|R1 = R2 >>> SHAMT|
|MOVR| 1010| I| MOVR R1 R2 IMM|R1 = MEM[R2 + IMM]|
|MOVM| 1011| I| MOVM R1 R2 IMM|MEM[R2 + IMM] = R1|
	
</details>

## Datapath
<b> Stages: </b> 5
 - Instruction Fetch (IF)
 - Instruction Decode (ID)
 - Execute (EX)
 - Memory (MEM)
 - Write Back (WB)
 
<b> Pipeline: </b> maximum 4 instructions running in parallel

## Contributors

- [Helen Mouris](https://github.com/HelenMouris)
- [George Elhamy](https://github.com/George-Elhamy)
- [Youssef Magdy](https://github.com/YoussefPoulis)
- [Youhanna Mentias](https://github.com/youhaa77)
- [Maria Reda](https://github.com/mariareda)
