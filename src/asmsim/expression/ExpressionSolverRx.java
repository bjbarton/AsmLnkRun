package asmsim.expression;

import java.io.File;
import java.util.Stack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExpressionSolverRx
{

	public static void main(String[] args)throws Exception
	{
		ExpressionSolverRx es = new ExpressionSolverRx();
		Scanner sym = new Scanner(new File("symTable.txt"));

		HashMap<String, Integer> vars = new HashMap<String, Integer>();
		Stack<HashMap<String, Integer>> envir = new Stack<HashMap<String, Integer>>();


		while(sym.hasNextLine()){
			Scanner piece = new Scanner(sym.nextLine());
			String s = piece.next();
			Integer i = piece.nextInt();
			System.out.println(s + " " + i);
			vars.put(s, i);
		}
		envir.push(vars);

		String[] testData = {

			//just numbers
			"0","22", "+1",

			//Binary
			"0b11", "0b0101",

			//octal
			"022", "016",

			//hexidecimal
			"0 x A1", "0 x 10",

			//basic expressions
			//decimal only
			"1 / 2", "222 + 111", "20 / 13",
			"2 ^ 3", "~1",

			//alternate base
			"0b101 + 5", "0b101 * 0x5", "0x10 - 020",
			"021 & 2", "!3", "~3", "-3",

			//compund expressions
			"-21-023", "6 & 2 - 3 * 1", "5 % 3 - 0b11",
			"0x21 * (-221 + 0b11011101)",

			//with symbols
			"-21 % 30", "0b1101 && 2 - ace",
			"-2 + ace % (ace) ^ 7",

			//relational
			"22 > 1", "22 < 1", "(3 * 2) <= 5",
			"2 >= 4", "4 >= 2", "2 == 3", "3 == 3",

			//expected error conditions
			//"", //empty entry returns null

			"(3 - 2 * 3", //unbalancecd parenthesis.
			"3 - 2 * 3)",

			"2 -- 4",		//invalid operators.
			"3 &^ 3",
			"1 @ 3",
			"3 -% 7)",
			"11 * 7 ^ 54 %* 8",
			"11 * 7 ^ 54 % * 8 * (7 ^ 2)",

			"falseSymbol - 7", //invalid symbol
			"-77 * falseSymbol",
			"2 - a44",

			"$%@#",            //invalid chars
			"&^$",
			"51 + 0b11 # 0",
			"11 + 0xA -$ 6",

			"6(8)",			 // missing operator
			"a44b33",
			"77+9(7^1)",
			"55-(8)7",
			"12ace",

			"-",			//missing Operand
			"!",
			"+",
			">=2",
			"55-",
			"65 - 6 +",
			"23 + 8 * 2 ^ 3 & "
			};

			Integer q, a;
			for(int x = 0; x < testData.length; x++ ){
				//q = es.eval(testData[x], envir);
				//a = ansData[x];
				//if(q != a)
					System.out.println(testData[x]);
					System.out.println(es.eval(testData[x], envir) + "\n");
			}



		Scanner sc = new Scanner(System.in);
		String line = sc.nextLine();
		while(!line.equals("x")){
			System.out.println(es.eval(line, envir));
			line = sc.nextLine();
		}

	}


	//Offset is to keep track of the index for preprocess evaluation
	private int offset;

	//charactors that can appear in a symbol
	final String ARITH_UNI_OP;
	final String LOGICAL_UNI_OP;
	final String UNI_OP;

	final String UNI_OP_CASE;
/*
	final String ARITH_BIN_OP

	final String RELN_BIN_OP

	final String LOGICAL_BIN_OP
	final String BIN_OP
*/
	final String OP_CASE;

	final String SYMBOL;

	final String BINARY_NUM;
	final String HEX_NUM;
	final String OCTAL_NUM;
	final String DECIMAL_NUM;
	final String NUMBER;


/* Cannot declair due to forwaord reference. Used as a guid

	final String TERM		 = NUMBER + "|" + EXPRESSION;

	final String EXPRESSION	 = TERM + "|(" +  UNI_OP + TERM + ")|(" +
							   TERM + BIN_OP + TERM + ")";
*/	final String SUB_EXP_CAPT;

	//Divide the Expression up by operators and terms
	final String TOKEN;

	Pattern token;
	Matcher tknMatch;

	//StartExp is used for error handling and carrot placement
	String startExp;

	//The environment holds the symbol table(s)
	Stack<HashMap<String, Integer>> env = new Stack();

	public ExpressionSolverRx()
	{

		ARITH_UNI_OP	= "~+-";
		LOGICAL_UNI_OP	= "!";
		UNI_OP			=  "\\Q" + ARITH_UNI_OP +
								LOGICAL_UNI_OP + "\\E";

	/*
		ARITH_BIN_OP	=
			"[+ - * & / % | ^ ] | (>>) | (<<)";
		RELN_BIN_OP	=
			"(<=) | (==) | (!=) | (>=) | [<>]";
		LOGICAL_BIN_OP = "(&&)|(||)";
		BIN_OP 		=
		ARITH_BIN_OP + "|" + RELN_BIN_OP +
				"|" + LOGICAL_BIN_OP;
	*/
		UNI_OP_CASE	= "[" + UNI_OP + "]";
		OP_CASE		= "\\Q+-*/%&^|!~=<>\\E";

		SYMBOL		= "[A-Za-z][\\w\\Q_$\\E]*";

		BINARY_NUM	= "(0b[01]+)";
		HEX_NUM		= "(0x[\\dA-Fa-f]+)";
		OCTAL_NUM	= "(0[0-8]*)";
		DECIMAL_NUM	= "([1-9]\\d*)";
		NUMBER		= BINARY_NUM + "|" + HEX_NUM + "|" +
								  OCTAL_NUM + "|" + DECIMAL_NUM;

		SUB_EXP_CAPT = "\\([^\\(\\)]+\\)"; /*Use greedy Quantifyer
											* to capture inner parenthesis
											* untill none exist;
											*/

		TOKEN	= "[" + OP_CASE + "]+|[^" + OP_CASE + "\\)\\(]+|[\\)\\(]";
		token = Pattern.compile(TOKEN);

	}

	public Integer eval(String expression,
						Stack<HashMap<String, Integer>> en)
								throws Exception

	{
		offset = 0;
		env = en;

		TermToken treeStart = null;
		Integer ans = null;

		startExp = expression.replace(" ", "");

		//check for empty String
		if (expression.length() > 0) {

			//try{
				tknMatch = token.matcher(startExp);
				checkParen(startExp);
				treeStart = preProcess((startExp + ")"));
				ans = parse(treeStart);
			//}
			//catch(Exception exc){
			//	System.out.println(startExp + " : " +
			//						exc.getMessage());
			//}
		}
			return ans;
	}

	private TermToken preProcess(String expression)
	{
		StringBuilder exp = new StringBuilder(expression);
		ArrayList<Token> tokenList = new ArrayList<Token>();

		String tkn;
		int tknStart = 0;
		int tknEnd = 0;

		while (tknMatch.find())
		{
			//get the String that was matched
			tkn = tknMatch.group();
			System.out.println("TKN: " + tkn);

			//get the start and end index of this token.
			tknStart = tknMatch.start();
			tknEnd = tknMatch.end();

			Token thisToken = null;

			//Beginning of sub expression
			if (tkn.matches("[\\(]+")){

				offset = tknEnd;
				System.out.println("Open Paren");

				thisToken = preProcess(
									exp.substring(tknStart + 1)
								 );
				//System.out.println(tknMatch.group());

				//exp = new StringBuilder();

				//tknMatch = token.matcher(exp.substring(offset - 1));
			}
			//End of sub expression
			else if (tkn.matches("[\\)]+")){

				offset += tknEnd;
				//trigger end of loop for return
				//tknMatch.usePattern(Pattern.compile("@#"));
				break;
			}

			else if (tkn.matches(this.NUMBER)){
				thisToken = getNum(tkn);
			}
			else if (tkn.matches("[" + this.OP_CASE + "]+")){
				thisToken = getOp(tkn);
			}
			else if (tkn.matches(this.SYMBOL)){
				thisToken = getSymbol(tkn);
			}
			else
				throw new InputMismatchException(
								startExp + ": Invalid Charactor or symbol" + tkn +
								carrot(tknStart)
						 );

			if (thisToken != null && !tokenList.isEmpty()){

				Token lasTkn = tokenList.get(tokenList.size() - 1);

				if( lasTkn.getClass().isInstance(thisToken) ){
					throw new InputMismatchException(startExp +
													": Missing Operand" +
													carrot(tknStart));
				}
			}
			 tokenList.add(thisToken);

		}

		System.out.println(tokenList);

		int lstSize = tokenList.size();

		if((lstSize == 2) &&
			(tokenList.get(0) instanceof OpToken)){

			OpToken op = (OpToken)tokenList.get(0);

			if(op.getOper().matches(UNI_OP_CASE))
			{
				tokenList.add(0, new ValueToken(0));
				lstSize++;
			}
		}

		if( !(lstSize == 1 || lstSize >= 3) ||
				(tokenList.get(0) instanceof OpToken)
		)
			throw new InputMismatchException(startExp + ": Missing Operand" +
											 carrot(0));

		if ((tokenList.get(tokenList.size()-1) instanceof OpToken))
			throw new InputMismatchException(startExp + ": Missing Operand" +
											 carrot(tokenList.size() + 1));

		Token[] tkLst = new Token[tknEnd];
		tokenList.toArray(tkLst);

		return genTree(tkLst);
	}

	private ValueToken getNum(String num)
	{
		//System.out.println(partialExp);
		int result, off, base = 10;
		off = 0;

		if (num.matches(BINARY_NUM)){
			off = 2;
			base = 2;
		}
		else if (num.matches(OCTAL_NUM)){
			off = 0;
			base = 8;
		}
		else if (num.matches(HEX_NUM)){
			off = 2;
			base = 16;
		}
		else if(!num.matches(DECIMAL_NUM)){
			System.out.println("Invalid number format");
		}

		result = Integer.parseInt(num.substring(off), base);
		return new ValueToken(result);
	}

	private Integer parse(TermToken term)
	{
		Integer ans;

		if (term instanceof ValueToken){

			ValueToken valTok = (ValueToken)term;
			ans =  valTok.getValue();
		}
		else {
			ans = calc( parse( term.opdLeft),
						parse(term.opdRight),
						term.opr.getOper()
					  );
		}
		//System.out.println(term.opdLeft + " : " + term.opr.getOpNum()  + " : " + term.opdRight);

		return ans;
	}


	private boolean isLastTknType(ArrayList<Token> tknLst, Token type)
	{
		boolean isType = false;
		Token last;

		if (!tknLst.isEmpty()){

			last = tknLst.get( tknLst.size() - 1 );
			//debug
			//System.out.println(last);


			isType = type.getClass().isInstance(last);
		}

		return isType;
	}


	/*
	* Just as the name suggests this method will ensure that
	* all parenthesis are both opened and closed
	* otherwise throw an Exception.
	*/

	private void checkParen(String exp)throws Exception
	{
		Stack<Integer> pStack = new Stack<Integer>();

		for(int indx = 0; indx < exp.length(); indx++)
		{
			if(exp.charAt(indx) == '(') pStack.push(indx);

			else if(exp.charAt(indx) == ')'){

				if (pStack.isEmpty()){
					throw new Exception(startExp +
										": Unbalanced Parenthesis" +
										carrot(indx + 1));
				}
				else
					pStack.pop();
			}
		}
		if (!pStack.isEmpty()){
			throw new Exception(startExp +
							   ": Unbalanced Parenthesis" +
								carrot(pStack.pop()) );
		}
	}

	private TermToken genTree(Token[] tokenExp)
	{
		TermToken treeTop;

		//if the length of this tokenized expression is 1
		//then we have a single value token.
		if (tokenExp.length == 1){

			 treeTop = (TermToken)tokenExp[0];
		}
		else{
			//if the length is greater than 1 then we have an
			//expression that follows the pattern (Term) op (Term)
											 //left Operand
			treeTop = new TermToken((TermToken)tokenExp[0],
											//Right operand
										  (TermToken)tokenExp[2],
										  	//operator
										  (OpToken)tokenExp[1]
								     );


			/*At this point we have a termToken with value op value
			* and therefor if there are more terms we should start with
			* treeTop as the left Operand the current index as the operand
			* and index + 1 as the right operand.
			*/
			for(int indx = 3; indx < tokenExp.length; indx++)
			{
				treeTop = new TermToken((TermToken)treeTop,			//Left Operand
									 	(TermToken)tokenExp[indx+1], //Right Operand
									 	(OpToken)tokenExp[indx++]  //Operator
									   );
			}
		}
		return treeTop;
	}

	/*
	* This method takes a substring of the starting expression
	* and gets the operator charactors starting at index 0;
	* until the charactor is not in the opChars string
	*/

	private OpToken getOp(String oper)
	{
		OpToken op = null;

		//----WHY DID I DO THIS... THIS IS DUMB-----
		try
		{
			//DEBUG
			//System.out.println(oper);
			op = new OpToken(oper);
		}

		catch(InputMismatchException exc)
		{
			throw new InputMismatchException(
								startExp +
								": Invalid Operator: "
								+ oper);
		}
		//--------FIX THIS ASAP--------
		return op;
	}

	//retrieve the entirety of the Symbol and loook it up
	//return the Symbols value from the table, or throws and error
	//should the symbol not be found.
	private ValueToken getSymbol(String sym)
	{

		Integer result = null;

//		DEBUG
//		System.out.println(subExp);
//		System.out.println("Got to sym find");

		if(!env.isEmpty()){

			Stack<HashMap> envir = (Stack)this.env.clone();
			result = null;

//			DEBUG
//			System.out.println("[" + sym +"]");

			while(result == null && !envir.isEmpty()){

				//DEBUG
				//System.out.println(result == null && !env.isEmpty());

				//System.out.println("IsEmpty? " + env.peek());
				result = (Integer) envir.pop().get(sym);
			}
		}
		if(result == null)
		{
			throw new NoSuchElementException(startExp + ": Symbol not found: "
											+ sym);
		}

		return new ValueToken(result);
	}

/*	private int calc(Integer opdLeft, Integer opdRight, int opId)
	{	//   0    1    2    3    4    5    6    7    8    9    10    11
		//	"+", "-", "!", "~", "*", "/", "%", "&", "|", "^", "<<", ">>",
		//  12   13    14   15    16    17   18    19
		//	"<", "<=", "==","!=", ">=", ">", "&&", "||";
		int result = 0;

		switch(opId){
			//case 0: result = (opdLeft == null)? opdRight : opdLeft + opdRight; break;
			//case 1: result = (opdLeft == null)? -opdRight: opdLeft - opdRight; break;

			case 0: result = opdLeft + opdRight;		   break;
			case 1: result = opdLeft - opdRight;  		   break;
			case 2: result = (~opdRight);       	       break;
			case 3: result = (~opdRight);                  break;
			case 4: result = opdLeft * opdRight;           break;
			case 5: result = opdLeft / opdRight;           break;
			case 6: result = opdLeft % opdRight;           break;
			case 7: result = opdLeft & opdRight;           break;
			case 8: result = opdLeft | opdRight;           break;
			case 9: result = opdLeft ^ opdRight;           break;
			case 10: result = opdLeft << opdRight;         break;
			case 11: result = opdLeft >> opdRight;         break;
			case 12: result = (opdLeft < opdRight) ? 1 : 0;break;
			case 13: result = (opdLeft <= opdRight)? 1 : 0;break;
			case 14: result = (opdLeft == opdRight)? 1 : 0;break;
			case 15: result = (opdLeft != opdRight)? 1 : 0;break;
			case 16: result = (opdLeft >= opdRight)? 1 : 0;break;
			case 17: result = (opdLeft > opdRight) ? 1 : 0;break;
			case 18: result = (opdLeft & opdRight);        break;
			case 19: result = (opdLeft | opdRight);        break;
		}*/

	private int calc(Integer opdLeft, Integer opdRight, String oper)
	{	//   0    1    2    3    4    5    6    7    8    9    10    11
		//	"+", "-", "!", "~", "*", "/", "%", "&", "|", "^", "<<", ">>",
		//  12   13    14   15    16    17   18    19
		//	"<", "<=", "==","!=", ">=", ">", "&&", "||";
		int result = 0;

		switch(oper){
			//case 0: result = (opdLeft == null)? opdRight : opdLeft + opdRight; break;
			//case 1: result = (opdLeft == null)? -opdRight: opdLeft - opdRight; break;

			case "+":
				result = opdLeft + opdRight;
				break;
			case "-":
				result = opdLeft - opdRight;
				break;
			case "!":
				result = (~opdRight);
				break;
			case "~":
				result = (~opdRight);
				break;
			case "*":
				result = opdLeft * opdRight;
				break;
			case "/":
				result = opdLeft / opdRight;
				break;
			case "%":
				result = opdLeft % opdRight;
				break;
			case "&":
				result = opdLeft & opdRight;
				break;
			case "|":
				result = opdLeft | opdRight;
				break;
			case "^":
				result = opdLeft ^ opdRight;
				break;
			case "<<":
				result = opdLeft << opdRight;
				break;
			case ">>":
				result = opdLeft >> opdRight;
				break;
			case "<":
				result = (opdLeft < opdRight) ? 1 : 0;
				break;
			case "<=":
				result = (opdLeft <= opdRight)? 1 : 0;
				break;
			case "==":
				result = (opdLeft == opdRight)? 1 : 0;
				break;
			case "!=":
				result = (opdLeft != opdRight)? 1 : 0;
				break;
			case ">=":
				result = (opdLeft >= opdRight)? 1 : 0;
				break;
			case ">":
				result = (opdLeft > opdRight) ? 1 : 0;
				break;
			case "&&":
				result = (opdLeft != 0)? 1: 0;
				result = result & ((opdRight != 0)? 1 : 0);
				break;
			case "||":
				result = (opdLeft | opdRight);
				break;
			default:
				result = 0;
				break;
		}

//				DEBUG
		//		System.out.println("[" + opdLeft + " " + OpToken.ops[opId] + " " + opdRight + "]");
		return result;
	}

	public String carrot(int indx)
	{
		StringBuilder pad = new StringBuilder("\n");
		//System.out.println("-" + indx +"-");
		for(; indx > 1; indx-- )
		{ pad.append(" "); };
		pad.append("^");

		return  pad.toString();
	}
}