import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Project {
	public static String[] memory;
	public static int[] registerFile;
	public static int pc = 0;
	public static final int ZERO = 0;
	public static final int instructionStart = 0;
	public static final int dataStart = 1024;
	public static int numberOfInstructions;
	public int cycles = 0;

	// String between stages fetch decode
	public static String decode;
	// register between stages decode execute
	public static ArrayList<String> decex;
	// register between stages execute memory
	public static ArrayList<String> exme;
	// register between stages memory and write-back
	public static ArrayList<String> mewb;

	// counters for each stage
	public static int decodeCounter = 0;
	public static int executeCounter = 0;
	public static int memoryCounter = 0;
	public static int writeBackCounter = 0;

	// restart for jump (to have he impression of a restart after a jump)
	public static int restart = 1;

	// to flag that a jump has occured
	public static boolean jump = false;

	// a counter to flag the two exceptional cycles of the jump before the restart
	public static int exceptional = 0;

	// to handle the two consecutive cycles of decode and execute (note: each of
	// them is done in the first cycle)
	public static boolean stillInDecode = false;
	public static boolean stillInExecute = false;

	// a temp pc to store the value of the pc that we want after a jump (can't
	// override directly because the execute is done before fetch)
	public static int jumpPc = 0;

	public Project(String filepath) throws IOException {
		memory = new String[2048];
		
		for(int i=dataStart;i<memory.length;i++) {
			String zeroes = "00000000000000000000000000000000";
			memory[i]=zeroes;
		}
		registerFile = new int[31];
		decex = new ArrayList<String>();
		exme = new ArrayList<String>();
		mewb = new ArrayList<String>();
		readFile(filepath);
	}

	// reading the instruction from the text file
	@SuppressWarnings("resource")
	public static void readFile(String filepath) throws IOException {

		File file = new File(filepath);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		numberOfInstructions = instructionStart;
		while ((st = br.readLine()) != null) {
			memory[numberOfInstructions++] = makeInstruction(st);
		}
	}

	// Take a string as ADD R1 R2 R3 and returns 0000 00000 00001 00010 000000
	public static String makeInstruction(String st) {
		String[] splitted = st.split("\\s+");
		splitted[0] = splitted[0].toLowerCase();
		String instruction = "";
		String type = "";
		boolean shift = false;
		boolean emptyR2 = false;

		// getting the opcode
		switch (splitted[0]) {
		case "add": {
			instruction = "0000";
			type = "R";
			break;
		}
		case "sub": {
			instruction = "0001";
			type = "R";
			break;
		}
		case "mul": {
			instruction = "0010";
			type = "R";
			break;
		}
		case "movi": {
			instruction = "0011";
			type = "I";
			emptyR2 = true;
			break;
		}
		case "jeq": {
			instruction = "0100";
			type = "I";
			break;
		}
		case "and": {
			instruction = "0101";
			type = "R";
			break;
		}
		case "xori": {
			instruction = "0110";
			type = "I";
			break;
		}
		case "jmp": {
			instruction = "0111";
			type = "J";
			break;
		}
		case "lsl": {
			instruction = "1000";
			type = "R";
			shift = true;
			break;
		}
		case "lsr": {
			instruction = "1001";
			type = "R";
			shift = true;
			break;
		}
		case "movr": {
			instruction = "1010";
			type = "I";
			break;
		}
		case "movm": {
			instruction = "1011";
			type = "I";
			break;
		}
		default:
			System.out.println("INCORRECT INSTRUCTION!");
		}

		// Completing the instruction in case of R-type
		if (type.equalsIgnoreCase("R")) {
			for (int i = 1; i <= 2; i++) {
				instruction += getRegisterNumber(splitted[i]); // the name of the register ex: R1 , R2 , R18 ...
			}
			if (shift)
				instruction = instruction + "00000" + toBinary(Integer.parseInt(splitted[3]), 13);
			else
				instruction = instruction + getRegisterNumber(splitted[3]) + "0000000000000";
		}
		// Completing the instruction in case of I-type
		else if (type.equalsIgnoreCase("I")) {
			instruction += getRegisterNumber(splitted[1]); // for R1 (first reg)
			if (emptyR2)
				instruction = instruction + "00000" + toBinary(Integer.parseInt(splitted[2]), 18);
			else
				instruction = instruction + getRegisterNumber(splitted[2])
						+ toBinary(Integer.parseInt(splitted[3]), 18);
		}
		// Completing the instruction in case of J-type
		else if (type.equalsIgnoreCase("J")) {
			instruction += toBinary(Integer.parseInt(splitted[1]), 28);
		}

		return instruction;
	}

	// changes from R1 to 00001
	public static String getRegisterNumber(String registerName) {
		int registerNumber = Integer.parseInt(registerName.substring(1));
		return toBinary(registerNumber, 5);
	}

	// changes the integer to binary and adds zeroes to make the total length = len
	public static String toBinary(int x, int len) {

		if (len > 0) {
			// positive number
			if (x >= 0) {
				return String.format("%" + len + "s", Integer.toBinaryString(x)).replaceAll(" ", "0");
			}
			// negative number
			else {
				return Integer.toBinaryString(x).substring(Integer.toBinaryString(x).length() - len,
						Integer.toBinaryString(x).length());
			}
		}
		return null;
	}

	public static ArrayList<String> splitter(String instruction) {

		ArrayList<String> result = new ArrayList<>();
		result.add(instruction.substring(0, 4));
		String opcode = instruction.substring(0, 4);
		if (opcode.equals("0000") || opcode.equals("0001") || opcode.equals("0010") || opcode.equals("0101")
				|| opcode.equals("1000") || opcode.equals("1001")) {
			result.add(instruction.substring(4, 9));
			result.add(instruction.substring(9, 14));
			result.add(instruction.substring(14, 19));
			result.add(instruction.substring(19, 32));
		}

		if (opcode.equals("0011") || opcode.equals("0100") || opcode.equals("0110") || opcode.equals("1010")
				|| opcode.equals("1011")) {
			result.add(instruction.substring(4, 9));
			result.add(instruction.substring(9, 14));
			result.add(instruction.substring(14, 32));
		}

		if (opcode.equals("0111")) {
			result.add(instruction.substring(4, 32));

		}
		return result;
	}

	public static int getRegisterValue(String binaryIndex) {
		int index = Integer.parseInt(binaryIndex, 2);
		int result = registerFile[index - 1];
		return result;
	}

	// FETCH STAGE
	public static void fetch() {

		System.out.print("FETCH STAGE --> ");
		System.out.println("Instruction number: " + pc);
		decode = memory[pc++];
		System.out.println("Fetched instruction is: " + decode);
		System.out.println();
	}

	// DECODE STAGE
	public static void decode() {

		ArrayList<String> splittedInstruction = splitter(decode);
		System.out.print("DECODE STAGE --> ");
		System.out.println("Instruction number: " + decodeCounter);
		System.out.println("Stage input: {instruction=" + decode + "}");
		decex.clear();

		if (splittedInstruction.size() == 5) {
			// R-type
			decex.add(splittedInstruction.get(0));
			decex.add(splittedInstruction.get(1));
			// check for r0 as a source register
			if (splittedInstruction.get(2).equals("00000")) {
				decex.add(ZERO + "");
			} else {
				decex.add(getRegisterValue(splittedInstruction.get(2)) + "");
			}
			if (splittedInstruction.get(0).equals("1000") || splittedInstruction.get(0).equals("1001")) {
				decex.add(0 + "");
			} else {
				if (splittedInstruction.get(3).equals("00000")) {
					decex.add(ZERO + "");
				} else {
					decex.add(getRegisterValue(splittedInstruction.get(3)) + "");
				}
			}
			decex.add(Integer.parseInt(splittedInstruction.get(4), 2) + "");
			System.out.println("Opcode: " + decex.get(0));
			System.out.println("Destination register index: " + decex.get(1));
			System.out.println("First operand: " + decex.get(2));
			System.out.println("Second operand: " + decex.get(3));
			System.out.println("Shamt: " + decex.get(4));
			System.out.println();

		} else if (splittedInstruction.size() == 4) {
			// I-type
			decex.add(splittedInstruction.get(0));
			System.out.println("Opcode: " + decex.get(0));
			// cases to know if i want to add the index of R1 (destination) or its value (in
			// jeq & movm)
			if (splittedInstruction.get(0).equals("0100") || splittedInstruction.get(0).equals("1011")) {
				if (splittedInstruction.get(1).equals("00000")) {
					decex.add(ZERO + "");
				} else {
					decex.add(getRegisterValue(splittedInstruction.get(1)) + "");
				}
				System.out.println("Value of R1: " + decex.get(1));
			} else {
				decex.add(splittedInstruction.get(1));
				System.out.println("Destination register index: " + decex.get(1));
			}

			if (!splittedInstruction.get(0).equals("0011")) {
				if (splittedInstruction.get(2).equals("00000")) {
					decex.add(ZERO + "");
				} else {
					decex.add(getRegisterValue(splittedInstruction.get(2)) + "");
				}
			} else {
				decex.add(0 + "");
			}
			System.out.println("Value of R2: " + decex.get(2));
			decex.add(parse(splittedInstruction.get(3)) + "");
			System.out.println("Value of IMM: " + decex.get(3));
			System.out.println();

		} else if (splittedInstruction.size() == 2) {
			// J-type
			decex.add(splittedInstruction.get(0));
			System.out.println("Opcode: " + decex.get(0));
			decex.add(splittedInstruction.get(1));
			System.out.println("Jump address: " + decex.get(1));
			System.out.println();
		}

	}

	// EXECUTE STAGE
	public static void execute() {

		int res = 0;
		int r2 = 0;
		int r3 = 0;
		int shamt = 0;
		System.out.print("EXECUTE STAGE --> ");
		System.out.println("Instruction number: " + executeCounter);

		// populating the values of exme with those of decex and adding result
		exme.clear();
		for (String s : decex) {
			exme.add(s);
		}

		if (exme.get(0).equals("0000")) {
			// add : r1 = r2 + r3
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", R3=" + decex.get(3));
			System.out.print(", SHAMT=" + decex.get(4));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 + r3;
			exme.add(res + "");
			System.out.println("Addition result: " + exme.get(5));
			System.out.println("Destination register index:" + exme.get(1));
		} else if (exme.get(0).equals("0001")) {
			// sub : r1 = r2 - r3
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", R3=" + decex.get(3));
			System.out.print(", SHAMT=" + decex.get(4));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 - r3;
			exme.add(res + "");
			System.out.println("Subtraction result: " + exme.get(5));
			System.out.println("Destination register index:" + exme.get(1));
		} else if (exme.get(0).equals("0010")) {
			// mul : r1 = r2 * r3
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", R3=" + decex.get(3));
			System.out.print(", SHAMT=" + decex.get(4));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 * r3;
			exme.add(res + "");
			System.out.println("Multiplication result: " + exme.get(5));
			System.out.println("Destination register index:" + exme.get(1));
		} else if (exme.get(0).equals("0011")) {
			// movi : r1 = imm
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", IMM=" + decex.get(3));
			System.out.println("}");
			res = Integer.parseInt(exme.get(3));
			exme.add(res + "");
			System.out.println("Value of IMM: " + exme.get(4));
			System.out.println("Destination register index: " + exme.get(1));
		} else if (exme.get(0).equals("0100") && Integer.parseInt(exme.get(1)) == Integer.parseInt(exme.get(2))) {
			// jeq : (if r1 == r2 then pc = pc + 1 + imm)
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", IMM=" + decex.get(3));
			System.out.println("}");
			System.out.println("Value of the old pc: " + pc);
			// case for jeq upwards
			if (Integer.parseInt(decex.get(3)) < 0) {
				jumpPc = pc + Integer.parseInt(decex.get(3)) - 3;
				System.out.println(Integer.parseInt(decex.get(3)));
				System.out.println(numberOfInstructions);
				System.out.println("Value of the new pc: " + (jumpPc));
				numberOfInstructions = (numberOfInstructions * 2) - jumpPc;
			} else {
				jumpPc = pc + Integer.parseInt(decex.get(3)) - 1;
				System.out.println("Value of the new pc: " + (jumpPc));
				numberOfInstructions = numberOfInstructions - Integer.parseInt(decex.get(3)) + 2;
			}
			System.out.println("numberOfInstructions: " + numberOfInstructions);
			jump = true;
		} else if (exme.get(0).equals("0101")) {
			// and : r1 = r2 and r3
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", R3=" + decex.get(3));
			System.out.print(", SHAMT=" + decex.get(4));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 & r3;
			exme.add(res + "");
			System.out.println("Bitwise Anding result: " + exme.get(5));
			System.out.println("Destination register index: " + exme.get(1));
		} else if (exme.get(0).equals("0110")) {
			// xori : r1 = r2 ^ imm
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", IMM=" + decex.get(3));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 ^ r3;
			exme.add(res + "");
			System.out.println("Bitwise XORing result: " + exme.get(4));
			System.out.println("Destination register index: " + exme.get(1));
		} else if (exme.get(0).equals("0111")) {
			// jmp : PC = PC[31:28] || ADDRESS
			System.out.println("Value of the old pc: " + pc);
			String pcBits = toBinary(pc, 32);
			String temp = pcBits.substring(0, 4) + exme.get(1);
			jumpPc = Integer.parseInt(temp, 2);
			System.out.println("Value of the new pc: " + (jumpPc));
			numberOfInstructions = numberOfInstructions - (Integer.parseInt(decex.get(1), 2) - pc) + 1;
			System.out.println("numberOfInstructions: " + numberOfInstructions);
			jump = true;
		} else if (decex.get(0).equals("1000")) {
			// lsl : R1 = R2 << SHAMT
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", R3=" + decex.get(3));
			System.out.print(", SHAMT=" + decex.get(4));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			shamt = Integer.parseInt(exme.get(4));
			res = r2 << shamt;
			exme.add(res + "");
			System.out.println("Logical shift left result: " + exme.get(5));
			System.out.println("Destination register index:" + exme.get(1));
		} else if (exme.get(0).equals("1001")) {
			// lsr : R1 = R2 >>> SHAMT
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", R3=" + decex.get(3));
			System.out.print(", SHAMT=" + decex.get(4));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			shamt = Integer.parseInt(exme.get(4));
			res = r2 >>> shamt;
			exme.add(res + "");
			System.out.println("Logical shift right result: " + exme.get(5));
			System.out.println("Destination register index:" + exme.get(1));
		} else if (decex.get(0).equals("1010")) {
			// movr : R1 = MEM[R2 + IMM]
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", IMM=" + decex.get(3));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 + r3;
			exme.add(res + "");
			System.out.println("Addition result (Memory address): " + exme.get(4));
			System.out.println("Destination register index:" + exme.get(1));
		} else if (decex.get(0).equals("1011")) {
			// movm : MEM[R2 + IMM] = R1
			System.out.print("Stage Input: {");
			System.out.print("OPCODE=" + decex.get(0));
			System.out.print(", R1=" + decex.get(1));
			System.out.print(", R2=" + decex.get(2));
			System.out.print(", IMM=" + decex.get(3));
			System.out.println("}");
			r2 = Integer.parseInt(exme.get(2));
			r3 = Integer.parseInt(exme.get(3));
			res = r2 + r3;
			exme.add(res + "");
			System.out.println("Addition result (Memory address): " + exme.get(4));
			System.out.println("Register value:" + exme.get(1));
		}
		System.out.println();

	}

	// MEMORY STAGE
	public static void memory() {

		// use it only in movr and movm
		mewb.clear();
		for (String s : exme) {
			mewb.add(s);
		}
		System.out.print("MEMORY STAGE --> ");
		System.out.println("Instruction number: " + memoryCounter++);

		if (mewb.get(0).equals("1010")) {
			// movr : R1 = MEM[R2 + IMM]
			System.out.print("Stage Input: {");
			System.out.print("Addition result(Memory address): " + (Integer.parseInt(exme.get(4)) + dataStart));
			System.out.print(" , Destination register index:" + exme.get(1));
			System.out.println("}");
			String temp = memory[dataStart + Integer.parseInt(mewb.get(4))];
			mewb.add(Integer.parseInt(temp, 2) + "");
			System.out.println(
					"Value fetched from memory M[" + (dataStart + Integer.parseInt(exme.get(4))) + "]: " + mewb.get(5));
			System.out.println("Destination register index: " + mewb.get(1));
		} else if (mewb.get(0).equals("1011")) {
			// movm : MEM[R2 + IMM] = R1
			System.out.print("Stage Input: {");
			System.out.print("Addition result (Memory address)=" + (Integer.parseInt(exme.get(4)) + dataStart));
			System.out.print(", Register value=" + exme.get(1));
			System.out.println("}");
			System.out.println("Before writing in memory: M[" + (dataStart + Integer.parseInt(mewb.get(4))) + "]="
					+ (memory[dataStart + Integer.parseInt(mewb.get(4))]));
			memory[dataStart + Integer.parseInt(mewb.get(4))] = toBinary(Integer.parseInt(mewb.get(1)), 32);
			System.out.println("After writing in memory: M[" + (dataStart + Integer.parseInt(mewb.get(4))) + "]="
					+ memory[dataStart + Integer.parseInt(mewb.get(4))]);
		}
		System.out.println();
	}

	// WRITEBACK STAGE
	public static void writeBack() {

		System.out.print("WRITEBACK STAGE --> ");
		System.out.println("Instrcution number: " + writeBackCounter++);

		if (mewb.get(0).equals("0000") || mewb.get(0).equals("0001") || mewb.get(0).equals("0010")
				|| mewb.get(0).equals("1001") || mewb.get(0).equals("1010") || mewb.get(0).equals("1000")
				|| mewb.get(0).equals("0101")) {
			System.out.print("Stage Input: {");
			System.out.print("Operation result=" + mewb.get(5));
			System.out.print(", Register destination index =" + Integer.parseInt(mewb.get(1), 2));
			System.out.println("}");
			if (Integer.parseInt(mewb.get(1), 2) != 0) {
				System.out.println("Before writing in registerFile: r" + (Integer.parseInt(mewb.get(1), 2)) + "="
						+ (registerFile[Integer.parseInt(mewb.get(1), 2) - 1]));
				registerFile[Integer.parseInt(mewb.get(1), 2) - 1] = Integer.parseInt(mewb.get(5));
				System.out.println("After writing in registerFile: r" + (Integer.parseInt(mewb.get(1), 2)) + "="
						+ (registerFile[Integer.parseInt(mewb.get(1), 2) - 1]));
			} else {
				System.out.println("Before writing in registerFile: r0= " + ZERO);
				System.out.println("After writing in registerFile: r0= " + ZERO);
			}
		}

		if (mewb.get(0).equals("0011") || mewb.get(0).equals("0110")) {
			System.out.print("Stage Input: {");
			System.out.print("Operation result=" + mewb.get(4));
			System.out.print(", Register destination index =" + Integer.parseInt(mewb.get(1), 2));
			System.out.println("}");
			if (Integer.parseInt(mewb.get(1), 2) != 0) {
				System.out.println("Before writing in registerFile: r" + (Integer.parseInt(mewb.get(1), 2)) + "="
						+ (registerFile[Integer.parseInt(mewb.get(1), 2) - 1]));
				registerFile[Integer.parseInt(mewb.get(1), 2) - 1] = Integer.parseInt(mewb.get(4));
				System.out.println("After writing in registerFile: r" + (Integer.parseInt(mewb.get(1), 2)) + "="
						+ (registerFile[Integer.parseInt(mewb.get(1), 2) - 1]));
			} else {

				System.out.println("Before writing in registerFile: r0=" + ZERO);
				System.out.println("After writing in registerFile: r0=" + ZERO);
			}
		}
		System.out.println();

	}

	public static void displayMemory() {
		System.out.print("{");
		for (int i = 0; i < memory.length; i++) {
			if (memory[i] != "00000000000000000000000000000000" && memory[i]!=null) {
				System.out.print("index " + i + "= " + memory[i] + ", ");
			}
		}
		System.out.println("}");
	}

	public static void displayRegisterFile() {
		System.out.print("{");
		System.out.print("r0= " + ZERO + ", ");
		for (int i = 0; i < registerFile.length; i++) {
			System.out.print("r" + (i + 1) + "= " + registerFile[i] + ", ");
		}
		System.out.println("}");
	}

	public static void pipeline() {
		int numberOfCycles = 7 + (numberOfInstructions - 1) * 2;
		System.out.println("MEMORY ARRAY:");
		displayMemory();
		System.out.println("REGISTER FILE");
		displayRegisterFile();
		System.out.println();
		for (int i = 1; i <= numberOfCycles; i++) {

			System.out
					.println("-----------------------------------Cycle number " + i + "--------------------------------------");
			System.out.println();
			// the two exceptional cycles of after a jump
			if (exceptional > 0) {
				if (exceptional == 1) {
					// second exceptional cycle: 1. fetch of the instruction after jump 2. adjust
					// number of cycles 3. restart and return to normal
					writeBack();
					if (memory[pc] != null)
						fetch();
					// update counters to get the correct instruction number displayed afterwards
					decodeCounter = pc - 1;
					executeCounter = pc - 1;
					memoryCounter = pc - 1;
					writeBackCounter = pc - 1;
					numberOfCycles = 7 + (numberOfInstructions - 1) * 2;
					exceptional--;
					restart = 2;
				}
				if (exceptional == 2) {
					// first exceptional cycle: memory and flush
					memory(); // this is the memory stage of the jump instruction
					// flush
					decode = "";
					decex.clear();
					exceptional--;
				}

			} else {
				// normal pipeline attitude
				if (i % 2 == 1 && (i > 5 && restart > 5)) {
					writeBack();
				}
				if (i % 2 == 0 && (i > 4 && restart > 4) && i < numberOfCycles) {
					memory();
				}
				if (stillInExecute) {
					System.out.println("EXECUTE STAGE --> Instruction number: " + executeCounter++);
					System.out.println();
					stillInExecute = false;
					if (jump) {
						exceptional = 2;
					}
				}
				if (i % 2 == 0 && (i > 2 && restart > 2) && i < numberOfCycles - 1) {
					// check if still in execute
					execute();
					stillInExecute = true;
				}
				if (stillInDecode) {
					System.out.println("DECODE STAGE --> Instruction number: " + decodeCounter++);
					System.out.println();
					stillInDecode = false;
					if (jump) {
						exceptional = 2;
					}
				}
				if (i % 2 == 0 && i < numberOfCycles - 3) {
					decode();
					stillInDecode = true;
				}
				if (i % 2 == 1 && i < numberOfCycles - 5) {
					if (jump) {
						fetch();
						pc = jumpPc;
						jump = false;
					} else {
						fetch();
					}
				}
//			System.out.println(decode);
//			System.out.println("decex: ");
//			for(String s : decex) {
//				System.out.print(s+", ");
//			}
//			System.out.println();
//			System.out.println("exme: ");
//			for(String s : exme) {
//				System.out.print(s+", ");
//			}
//			System.out.println();
//			System.out.println("mewb: ");
//			for(String s : mewb) {
//				System.out.print(s+", ");
//			}
//			System.out.println();
				restart++;
			}
		}
		System.out.println("MEMORY ARRAY:");
		displayMemory();
		System.out.println("REGISTER FILE");
		displayRegisterFile();
	}

	public static int parse(String s) {
		if (s.charAt(0) == '1') {
			s = complement(s);
			return (Integer.parseInt(s, 2) + 1) * -1;
		}
		return Integer.parseInt(s, 2);
	}

	public static String complement(String s) {
		String res = "";
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '1')
				res += '0';
			else
				res += '1';
		}
		return res;
	}

	public static void main(String[] args) throws IOException {

		Project p = new Project("allOtherOperations.txt");
		memory[1036] = "00000000000000000000000000000011";
		memory[1026] = "00000000000000000000000000000010";
		pipeline();

	}

}
