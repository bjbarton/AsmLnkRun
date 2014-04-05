package asmsim.assembler;

import asmsim.expression.ExpressionSolver;

public abstract class Instruction
{
    protected final int OPCode; //Used to store "binary" base value for each instruction type

    ExpressionSolver  expSolver;
//    String[] argArr;

    public  Instruction()
    {
        OPCode = 0;
        expSolver = new ExpressionSolver();
    }

    public  Instruction(int base)
    {
        OPCode = base;
        expSolver = new ExpressionSolver();
    }

	private static final int MASK13 = (1 << 13) - 1;
    int _simm13(int n)
    {   return (n & MASK13);    }

    private static final int MASK22 = (1 << 22) - 1;
    int _imm22(int n)
    {   return (n & MASK22); }

    int _rs1(int n)
    { return n << 14; }

	private static final int I = 1 << 13;
    int _i()
    { return I; } //return the i bit.

    int _rd(int n)
    { return n << 25; }

	private static final int MASK30 = 1 << 30 - 1;
    int _disp30(int n)
    {   return n & MASK30; }

    int _reg(String reg)
    	throws IllegalArgumentException
    {
        int regNum = 0;
        try {
            regNum = Integer.parseInt(reg.substring(1));
		}
        catch(NumberFormatException exc) {
            throw new IllegalArgumentException("Register must be a number: " + reg);
        }

        if(regNum < 0 || 32 <= regNum)
            throw new IllegalArgumentException("Outside register range:[0, 32) : " + regNum);

        return regNum;
    }

    //protected abstract Integer formInstructionCode(String... args)
    //	throws Exception;
    protected abstract Integer formInstructionCode(SourceLine line)
    	throws Exception;
}


//OP_00
class BranchInstruction extends Instruction
{
    BranchInstruction()
    {
        super();
    }


    BranchInstruction(int base)
    {
        super(base);
    }
    protected Integer formInstructionCode(SourceLine line)
    	throws IllegalArgumentException, Exception
    {
        int ins = OPCode;
        String[] argArr = line.args.split(",\\s");

            //if this is a sethi
            final int SETHI_BIT = 1 << 24;
            if((OPCode & SETHI_BIT) == SETHI_BIT){

                if(argArr.length == 2){
                    if(argArr[1].charAt(0) != '%'){
                        throw new IllegalArgumentException("Invalid arg need register");
					}
                }
                else{
                    throw new IllegalArgumentException("Invalid number of args for setHi" );
				}


                int reg = _reg( argArr[1] );


                ins = ins | _rd(reg);
                //System.out.println("reg : " + _rd(reg) );
			//	System.out.println(ins);
               ins  = ins | _imm22( (expSolver.eval(argArr[0] , null) - line.address) >> 2);
              // System.out.println("After Expression Parse : " + ins);
               // ins = imm22 | ins;
            }

             else if(argArr.length != 1)
                throw new IllegalArgumentException("Illegal number of args");
             else{

            //get the first arg. should be a signed displacement
            int arg = _imm22(Integer.parseInt(argArr[0]) >> 2);
            ins = (arg | ins);
        }

      	return ins;
    }

}

//OP_01
class CallInstruction extends Instruction
{

    CallInstruction()
    {
        super();
    }


    CallInstruction(int base)
    {
        super(base);
	}

    protected Integer formInstructionCode(SourceLine line)throws Exception
    {
//        super();
		String[] argArr = line.args.split(",\\s");
        if(argArr.length != 1){
				//System.out.println("Call-Exception: " + argArr.length);
                          throw new Exception("Illegal number of args");
		}

         //call is the only instruction of type 01
        //we will get a 30 bit shifted address.

        return (OPCode | _disp30(Integer.parseInt(argArr[0]) ));
    }
}

//OP_10
class ArithmeticInstruction extends Instruction
{

    ArithmeticInstruction()
    {
        super();
    }


    ArithmeticInstruction(int base)
    {
        super(base);
    }
    protected Integer formInstructionCode(SourceLine line)
    	throws IllegalArgumentException, Exception
    {
		String[] argArr = line.args.split(",\\s");
		//of form
		//op rs1, rs1/simm13Expression, rd
		int instruction = OPCode;


		if(argArr.length != 3){
		//	System.out.println("NOT RIGHT NUM ARGS");
			throw new IllegalArgumentException("Illegal number of Args");
		}

		//if the second arg is not a register
		if(argArr[1].charAt(0) != '%'){
			instruction += _i() + _simm13(expSolver.eval(argArr[1], null ));
		}
		else{
			instruction += _reg(argArr[1]);
		}

		/*
			ins = ins | (mask &
						Integer.parseInt(argArr[1].substring(1) ) );
		*/
			if(argArr[0].charAt(0) != '%' ||
				argArr[2].charAt(0) != '%')
					throw new IllegalArgumentException("Illegal Argument, needed regester");

		//Debug
		//System.out.println("Made it past tst for registers in Arithmetic");
			//for rs1 and rd
			//mask = 0b11111 << 14;
			//ins = ins | (mask & (Integer.parseInt(
			//argArr[0].substring(1)) << 14) );
			instruction += _rs1( _reg(argArr[0]) ) + _rd( _reg(argArr[2]) );

		//System.out.println("here is the instruction: " + instruction);
        return instruction;
    }

}

class MemoryAccessInstruction extends Instruction
{

	public MemoryAccessInstruction()
	{	super();	}

	public MemoryAccessInstruction(int base){

		super(base);
	}
		protected String[] eliminateSquares(String squareArg)
			throws IllegalArgumentException
		{
			assert(squareArg.charAt(0) == '[');
			assert(squareArg.charAt(squareArg.length() - 1) == ']');

			//RS1 at index 0 and RS2 or simm13 at index 1
			String rs1, oprnd2;

			//assume first ->[  ]<- last
			squareArg = squareArg.replace("\\s", "");

			int operInd = 0;
			char curr;
			for(int i = 0; i < squareArg.length(); i++){
				curr = squareArg.charAt(i);
				if (curr == '+' || curr == '-'){
					operInd = i;
					i = squareArg.length();
				}
			}
			if(operInd == 0)
				throw new IllegalArgumentException("missing + or - : " + squareArg);

			else
			//take from after the [ until the first plus or minus sign.

			rs1 = squareArg.substring(1, operInd).trim();

			//Eliminate the ending bracket ']'and assign the
			//expression or register to oprnd2

			//               will end on ] (substring ignors the last index)
			oprnd2 = squareArg.substring
								(operInd + 1, squareArg.length() - 1)
									.trim();

			String[] parts = {rs1, oprnd2};
			return parts;
	}

	//LOOK AT RDIndx

	protected Integer formInstructionCode(SourceLine line)
		throws IllegalArgumentException, Exception
	{
		String[] argArr = line.args.split(",\\s");
		int sqrIndx = 0;
		//square index is 0 or 1
		if(this instanceof StoreInstruction)
			sqrIndx = 1;

		int rdIndx = 1 ^ sqrIndx; //set for load or store ;0 is load
								  //					1 is store.
		System.out.println("Inside memoryAccessInstruction");
		//TWO arg
		//Load [rs1+rs2], rd
		//Load [rs1 + (sim13 Expression)], rd

		//THREE arg
		//Load rs1, rs2, rd
		//Load rs1, sim13 Expression, rd
		String rs1, oprnd2, rd;

		if(argArr.length == 2){

			String[] squareParts;

			if(! ( (argArr[ sqrIndx ].charAt(0) == '[') &&
				argArr[ sqrIndx ].charAt(
									argArr[sqrIndx].length() - 1)
													== ']')){
				//System.out.println("--Square:" + argArr[sqrIndx]);
				throw new IllegalArgumentException ("missing Square Barackets");
			}

			squareParts = eliminateSquares( argArr[ sqrIndx ] );
//Debug
			//System.out.println("-\nSquareParts: " + squareParts[0] + ", " + squareParts[1]);
//EndDebug
			//take parts from the square brackets and place in
			//proper container.
			rs1 = squareParts[0];
			oprnd2 = squareParts[1];

			rd = argArr[ rdIndx ];

		}
		else if(argArr.length == 3){
			System.out.println("argLength 3");
			rs1		= argArr[0];
			oprnd2	= argArr[1];
			rd		= argArr[2];
		}
		else
			throw new IllegalArgumentException("Invalid number of args: ");



		//check to make sure source1 and destination are registers.
		if(rs1.charAt(0) != '%'  || rd.charAt(0) != '%')
			throw new IllegalArgumentException("Invalid arg, need register: "
									+ argArr[0]);


		int instruction = this.OPCode;

		//if operand 2 is a register handle accordingly.
		if(oprnd2.charAt(0) == '%'){
			instruction +=  _reg(oprnd2);
		}

		else{
			 instruction += _i() + _simm13(expSolver.eval(oprnd2, null));
		}

		System.out.println("rs1: " + rs1 + "\nrd: " + rd);
	    return instruction + _rs1(_reg(rs1))
       	     				+ _rd(_reg(rd));
	}

}

//OP_11
class LoadInstruction extends MemoryAccessInstruction
{

    LoadInstruction()
    {
        super();
    }


    LoadInstruction(int base)
    {
        super(base);
    }

    protected Integer formInstructionCode(SourceLine line) throws Exception
    {
		//return super.formInstructionCode(argArr, new String("0") );
		return super.formInstructionCode(line);
	}
    /*
    protected Integer formInstructionCode(SourceLine line)throws Exception
    {
		//TWO arg
		//Load [rs1+rs2], rd
		//Load [rs1 + (sim13 Expression)], rd

		//THREE arg
		//Load rs1, rs2, rd
		//Load rs1, sim13 Expression, rd
		String rs1, oprnd2, rd;

		if(argArr.length == 2){

			String[] squareParts;

			if(! ( (argArr[0].charAt(0) == '[') &&
				argArr[0].charAt( argArr[0].length() - 1) == ']')){

				throw new Exception ("invalid argument list for load");
			}

			squareParts = eliminateSquares(argArr[0]);
			//take parts from the square brackets and place in
			//proper container.
			rs1 = squareParts[0];
			oprnd2 = squareParts[2];

			rd = argArr[1];
		}
		else if(argArr.length == 3){
			rs1		= argArr[0];
			oprnd2	= argArr[1];
			rd		= argArr[2];
		}
		else
			throw new Exception("Invalid number of args for load: ");


		//check to make sure source1 and destination are registers.
		if(rs1.charAt(0) != '%'  || rd.charAt(0) != '%')
			throw new Exception("Invalid arg, need register: "
									+ argArr[0]);


		int instruction = OPCode;

		//if operand 2 is a register handle accordingly.
		if(oprnd2.charAt(0) == '%'){
			instruction +=  _reg(oprnd2);
		}

		else{
			 instruction += _i() + _simm13(expSolver.eval(oprnd2, null));
		}


        return instruction += _rs1(Integer.parseInt(rs1))
        					+ _rd(Integer.parseInt(rd));
    }
    */

}

class StoreInstruction extends MemoryAccessInstruction
{

    StoreInstruction()
    {
        super();
    }


    StoreInstruction(int base)
    {
        super(base);
    }
    protected Integer formInstructionCode(SourceLine line)throws Exception
    {	//									args   index for optional square brackets.
    	//										   this is a store
        return super.formInstructionCode(line);
        //ST %rd, [%rs1 + %rs2/Simm13Expression]
       	//    0			1
    }

}