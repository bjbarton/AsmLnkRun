package asmsim.assembler;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;


public class SparcArchitecture extends Architecture
{

	Map<String, Instruction> opMap;
	public SparcArchitecture()
	{
		opMap = new HashMap<String, Instruction>();

        //Type One Instruction
		                    //OP   Disp30
		opMap.put("call",  new CallInstruction( 0b01_000000000000000000000000000000));

		//Type Two Instructions (Branching) never anull, no delay slot
								//OP   cond op2  Disp22
		opMap.put("bn",		new BranchInstruction(
								0b00_00000_010_0000000000000000000000));
		opMap.put("be",		new BranchInstruction(
								0b00_00001_010_0000000000000000000000));
		opMap.put("ble",	new BranchInstruction(
								0b00_00010_010_0000000000000000000000));
		opMap.put("bl", 	new BranchInstruction(
								0b00_00011_010_0000000000000000000000));
		opMap.put("bleu",	new BranchInstruction(
								0b00_00100_010_0000000000000000000000));
		opMap.put("bcs", 	new BranchInstruction(
								0b00_00101_010_0000000000000000000000));
		opMap.put("bneg",	new BranchInstruction(
								0b00_00110_010_0000000000000000000000));
		opMap.put("bvs",	new BranchInstruction(
								0b00_00111_010_0000000000000000000000));
		opMap.put("ba", 	new BranchInstruction(
								0b00_01000_010_0000000000000000000000));
		opMap.put("bne",	new BranchInstruction(
								0b00_01001_010_0000000000000000000000));
		opMap.put("bg",		new BranchInstruction(
								0b00_01010_010_0000000000000000000000));
		opMap.put("bge",	new BranchInstruction(
								0b00_01011_010_0000000000000000000000));
		opMap.put("bgu",	new BranchInstruction(
								0b00_01100_010_0000000000000000000000));
		opMap.put("bcc",	new BranchInstruction(
								0b00_01101_010_0000000000000000000000));
		opMap.put("bpos",	new BranchInstruction(
								0b00_01110_010_0000000000000000000000));
		opMap.put("bvc", 	new BranchInstruction(
								0b00_01111_010_0000000000000000000000));

		opMap.put("sethi",  new BranchInstruction(
								0b00_00000_100_0000000000000000000000));

		//Type three instructions (arithmetic)
		    					//OP  RD    OP3    RS1  i  Operand2
		opMap.put("add",    new ArithmeticInstruction(
								0b10_00000_000000_00000_0_0000000000000));
		opMap.put("and",    new ArithmeticInstruction(
								0b10_00000_000001_00000_0_0000000000000));
		opMap.put("or",     new ArithmeticInstruction(
								0b10_00000_000010_00000_0_0000000000000));
		opMap.put("xor",    new ArithmeticInstruction(
								0b10_00000_000011_00000_0_0000000000000));
		opMap.put("sub",    new ArithmeticInstruction(
								0b10_00000_000100_00000_0_0000000000000));
		opMap.put("andn",   new ArithmeticInstruction(
								0b10_00000_000101_00000_0_0000000000000));
		opMap.put("orn",    new ArithmeticInstruction(
								0b10_00000_000110_00000_0_0000000000000));
		opMap.put("xnor",   new ArithmeticInstruction(
								0b10_00000_000111_00000_0_0000000000000));
		opMap.put("addcc",  new ArithmeticInstruction(
								0b10_00000_010000_00000_0_0000000000000));
		opMap.put("andcc",  new ArithmeticInstruction(
								0b10_00000_010001_00000_0_0000000000000));
		opMap.put("orcc",   new ArithmeticInstruction(
								0b10_00000_010010_00000_0_0000000000000));
		opMap.put("subcc",  new ArithmeticInstruction(
								0b10_00000_010100_00000_0_0000000000000));
		opMap.put("andncc",   new ArithmeticInstruction(
								0b10_00000_010101_00000_0_0000000000000));
		opMap.put("orncc",  new ArithmeticInstruction(
								0b10_00000_010110_00000_0_0000000000000));

		//Type Three Insstructions (Load / Store)
								//OP   RD    OP3    RS1 i   Operand2
		opMap.put("ld",     new LoadInstruction(
								0b11_00000_000000_00000_0_0000000000000));
		opMap.put("ldub",   new LoadInstruction(
								0b11_00000_000001_00000_0_0000000000000));
		opMap.put("lduh",   new LoadInstruction(
								0b11_00000_000010_00000_0_0000000000000));
		opMap.put("ldd",    new LoadInstruction(
								0b11_00000_000011_00000_0_0000000000000));
		opMap.put("ldsb",   new LoadInstruction(
								0b11_00000_001001_00000_0_0000000000000));
		opMap.put("ldsh",   new LoadInstruction(
								0b11_00000_001010_00000_0_0000000000000));

		opMap.put("st",		new StoreInstruction(
								0b11_00000_000100_00000_0_0000000000000));
		opMap.put("stb",    new StoreInstruction(
								0b11_00000_000101_00000_0_0000000000000));
		opMap.put("sth",    new StoreInstruction(
								0b11_00000_000110_00000_0_0000000000000));
		opMap.put("std",    new StoreInstruction(
								0b11_00000_000111_00000_0_0000000000000));


	}

	public Integer formInstruction(SourceLine line) throws Exception

	{
		//ExpressionSolver expSolver = new ExpressionSolver();
		//ExpressionSolverRx solver = new ExpressionSolverRx();
		//String instruction = null;
		//System.out.println("before asking the hash table");

		Instruction instr = opMap.get(line.OpCode);

		//System.out.println("Enter Arch formInst : " + ins);
//		System.out.println("Args: " + args);

		//String[] argArr = args.split(", ");

		if(instr == null){
			throw new UnsupportedOperationException("Unknown mnemonic: " + line.OpCode);
		}
		//System.out.println(opCode + " : " + argArr.length);


		//System.out.println("this is the INSCODE: " + x);
		return instr.formInstructionCode(line);
	}

}