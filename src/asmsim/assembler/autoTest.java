package asmsim.assembler;

import java.util.ArrayList;
import java.util.Random;

public class autoTest
{
	public static void main(String[] args)
	{
		ArrayList<String> sourceList = new ArrayList<>();

		SparcArchitecture sa = new SparcArchitecture();
		//a list of registter arguments to test with.
		String[] registers = new String[30];
		for (int i=0; i < registers.length; i++) registers[i] = ("%r" + i);

		//A list of non register symbols to test with, not predefined in architecture.
		String[] nonRegSymbols = { "a", "b", "tsta", "tstb",
			"tstc", "tstd", "tste", "tstf", "tstg", "tsth"};

		//A list of numeric args to test with.
		String[] numericArgs = new String[30];

		for (int i = 0; i< numericArgs.length; i++){

			if(i % 4 == 1)
			numericArgs[i] = Integer.toHexString((int)java.lang.Math.random() * 1000);

			else if(i % 4 == 2)
			numericArgs[i] = Integer.toOctalString((int)java.lang.Math.random() * 1000);

			else if(i % 4 == 3)
			numericArgs[i] = Integer.toBinaryString((int)java.lang.Math.random() * 1000);

			else{
				int integer = (int)( java.lang.Math.random() * 1000 );
				numericArgs[i] = Integer.toString(integer);
			}
		}

		String[][] argTypes = new String[3][];
		argTypes[0] = registers;
		argTypes[1] = nonRegSymbols;
		argTypes[2] = numericArgs;

		//Genereate three address instructions
		Random indexGen = new Random();

		for (String opCode : sa.opMap.keySet()){
			Instruction currInstruction = sa.opMap.get(opCode);

			if (currInstruction instanceof ArithmeticInstruction) {
				int index;
				String testCase = opCode;
				for(int i = 0; i < argTypes.length; i++){

					index = indexGen.nextInt( argTypes[i].length );
					testCase = testCase + " " + argTypes[i][index];

					index = indexGen.nextInt( argTypes[( i + 1 ) % 3].length);
					testCase = testCase + ", " + argTypes[ (i + 1) % 3 ][index];

					index = indexGen.nextInt( argTypes[( i + 2 ) % 3].length);
					testCase = testCase + ", " + argTypes[ (i + 2) % 3 ][index];

					sourceList.add(testCase);
					testCase = opCode;

				}
			}

			else if(currInstruction instanceof BranchInstruction) {

			}

			else if(currInstruction instanceof CallInstruction) {

			}

			else if(currInstruction instanceof LoadInstruction) {

			}

			else if(currInstruction instanceof StoreInstruction) {

			}

		}

		for(String element : sourceList)
		System.out.println( element );


	}
}

