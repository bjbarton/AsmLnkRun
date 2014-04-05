package asmsim.expression;

import java.util.InputMismatchException;

//	Token for expression tree
public abstract class Token
{
	OpToken opr;

	public OpToken getOpr()
	{	return this.opr;	}
}

//	A term is anything at expression level or lower.
class TermToken
	extends Token
{
	//	Can be a full term or a value
	//	Used for recursive evaluation of terms.
	TermToken opdLeft, opdRight;

	public TermToken(TermToken opdL, TermToken opdR, OpToken opr)
	{
		this.opdLeft = opdL;
		this.opdRight = opdR;
		this.opr = opr;
	}
	public TermToken()
	{
		this.opdLeft = null;
		this.opdRight = null;
		this.opr = null;
	}

	public TermToken getLeft()
	{	return this.opdLeft;	}

	public TermToken getRight()
	{	return this.opdRight;	}

	@Override
	public String toString()
	{	return("Term");	}
}

//A value token is a part of a Term, and will act as ther terminal
//case for the recursive evaluation.
//Once we have reached a valueToken, no more computation will be needed
//so we can return this Token which will be a leaf.
class ValueToken
	extends TermToken
{
	private int value;

	public ValueToken(int v)
	{
		super();
		this.value = v;
	}

	public int getValue()
	{	return this.value;}

	@Override
	public String toString()
	{	return "" + this.value;	}
}

//	OpToken is for valid operators.
class OpToken
	extends Token
{
	static final String[] ops = {
			//	Unary operators (arithmetic and logical).
		"+", "-", "!", "~",
			//	Binary operators (aritmetic)
		"*", "/", "%",
			//	Binary operators (bit-wise)
		"&", "|", "^",
			//	Binary operators (shift)
		"<<", ">>",
			//	Binary operators (relational)
		"<", "<=", "==","!=", ">=", ">",
			//	Binary operators (logical)
		"&&", "||",
	};

	private String operStr;

	public OpToken(String opStr)
	{
		boolean isValid = false;

		//Is opStr a valid operator?
		//Try to find it in ops
		for (String oper : ops) {

			if(opStr.equals(oper)) isValid = true;
		}

		//System.out.println(ops[opNum]);

		//If opStr not found indicate that this is a invalid operator.
		if (!isValid) {
			throw new InputMismatchException("Invalid Operater: " + opStr);
		}

		this.operStr = opStr;
	}

	public String getOper()
	{	return this.operStr;	}

	@Override
	public String toString()
	{	return this.operStr;  }
}