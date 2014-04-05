package asmsim.expression;

import java.io.File;
import java.util.Stack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;


public class ExpressionSolver
{

	//Offset is to keep track of the index into the expression String
	//for preprocess evaluation
	private int offset;

	//Charactors that can appear in a symbol
	public final String symbolChars =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_0123456789.";

	//Chars that can be in an operater
	String opChars     = "+-><=!~%&^*/";
	String uniOpChars     = "+-!~";

	//StartExp is used for error handling and carrot placement
	String baseExp;

	//The environment holds the symbol table(s)
	Stack<HashMap<String, Integer>> environment;

	public Integer eval(String expression,
						Stack<HashMap<String, Integer>> symbolTable)
								throws Exception
	{
		offset = 0;
		environment = symbolTable;

		TermToken treeStart = null;
		Integer ans = null;

		baseExp = expression.replace(" ", "");

		//check for empty String
		if (expression.length() > 0) {

	//		try{
				checkParen(baseExp);
				treeStart = preProcess((baseExp + ')'));
				ans = parse(treeStart);
	//		}
	//		catch(Exception exc){
	//			System.out.println(baseExp + " : " + exc.getMessage());
	//		}
		}
			return ans;
	}

	private ValueToken getNum(String partialExp)
	{
		//System.out.println(partialExp);
		int result, base = 10;
		int index = 0;
		char next = ' ';
		StringBuilder num = new StringBuilder("");

		//DEBUG---
		//System.out.println(partialExp);

		if(partialExp.length() > 1 && partialExp.charAt(0) == '0'){
			switch(partialExp.charAt(1)){
				case 'b': base = 2; break;
				case 'x': base = 16; break;
				default:  base = 8;
			}
			index = (base == 8)? 0 : 2;
		}

		next = partialExp.charAt(index);
		while(Character.digit(next, base) > -1 ) //end of expression will have ')' so no need to check for end
		{
			num.append(next);
			next = partialExp.charAt(++index);
		}

		this.offset += index;

		//DEBUG---
		//System.out.println("offset: "+ offset);
		//System.out.println(num + " " + base);

		result = Integer.parseInt(num.toString(), base);

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


	//Break up into Array of Tokens
	//Assure pattern Term operator Term operator... Term
	private TermToken preProcess(String expression)throws Exception
	{
		StringBuilder exp = new StringBuilder(expression.replace(" ", ""));

		ArrayList<Token> tokenList = new ArrayList<Token>();
		//System.out.println("exp start pre: " + expression);

		char curr = ' ';
		int indx = 0;
		curr = exp.charAt(indx);

		//check for -, ~, + (UniOp)
		//if((curr == '-'  || curr == '+' ||
		//	curr == '~' || curr == '!')	&& exp.length() > 1)
		if ((uniOpChars.indexOf(curr) != -1))
		{
			if ( exp.length() > 2 ){

				String oper =  getOp(expression).getOper();
				int opdRight = getNum(exp.substring(1)).getValue();

				int value = calc(0, opdRight, oper );

				tokenList.add(new ValueToken(value));

				indx += offset;
				offset = 0;
			}
			else
			{	throw new InputMismatchException(
							"Missing operand" + carrot(0));
			}
		}

		for(; indx < exp.length(); indx ++){
//			System.out.println("index: " + indx);
			curr = exp.charAt(indx);

//			DEBUG
//			System.out.println("curr: " + curr);
//			System.out.println("Length: " + exp.length());

			//if we have a sub expression evaluate by itself.

			if( curr == '('){

				TermToken nxtTerm = preProcess(	exp.substring(++indx) );

				if ( isLastTknType(tokenList, nxtTerm) ){
					throw new InputMismatchException(
							   			"Missing Operator "+
													carrot(indx));
				}

				tokenList.add(nxtTerm);
				/*
				Because we are at the first condition we can add
				the length of the expression so we can start at the
				next charater without waiting for the loop.
				*/
				indx += offset;
				offset = 0;
			}
			//if we reach a close paren then return and get a tree.
			else if(curr ==')'){
			//Debug
			//System.out.println("---");
				offset += indx;

				indx = expression.length(); // exit the loop
			}
			/*
			* If we have a digit then call getNum to retrieve the
			* whole number and take care of the base
			* then add the number as the next token.
			* (all numbers start with a decimal digit.
			*  e.g 0b, 0, 0x,
			*/
			else if(Character.isDigit(curr)){

				ValueToken val = getNum(	exp.substring(indx));

				if ( isLastTknType(tokenList, val))
				{	throw new InputMismatchException(
										"Missing Operator"+
													carrot(indx));
				}
				//create a new token and
				//add the number starting with "curr' to the list.
				//System.out.println(val);
				tokenList.add(val);

				indx += (offset - 1);
				offset = 0;
			}

			//get the operator if we have found one.
			//should be a max of 2 chars at the time this
			//is written, but may be expanded to include other operators

			else if(opChars.indexOf(curr) != -1){

				OpToken op = getOp(exp.substring(indx));

				indx += offset - 1;
				offset = 0;
				tokenList.add(op);
			}
			//get and eval a symbol, starts with a Charactor
			else if(symbolChars.indexOf(curr) != -1){

				ValueToken vt = new ValueToken(	getSymbol(exp.substring(indx)));

				if ( isLastTknType(tokenList, vt) )
				{
					throw new InputMismatchException(
										"Missing Operator" +
													carrot(indx));
				}
				tokenList.add(vt);
				indx += offset - 1;
				offset = 0;

				//op = new StringBuilder("");
			}
			else{
				throw new InputMismatchException(curr + " : " +
												"Invalid char" +
													 carrot(indx + 1));
			}
		}

		Token[] tkLst = new Token[tokenList.size()];
		tokenList.toArray(tkLst);

		/*
		* Is there a missing operand at the beginning or end.?
		* E.g. ( * 7 - 6) or (7 - 6 +)
		* In the case where there is a missing operand in the
		* middle of the expression, we will identify a false operator
		*/
		if ( tkLst[0] instanceof OpToken )
			throw new InputMismatchException(
					  "Missing Operand" + carrot(0));

		else if ( tkLst[ tkLst.length - 1 ] instanceof OpToken )
				throw new InputMismatchException(
					  "Missing Operand" + carrot(exp.length() - 1));


		return genTree(tkLst);
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

				if (pStack.isEmpty())
					throw new Exception("Unbalanced Parenthesis" + carrot(indx + 1));
				else
					pStack.pop();
			}
		}
		if (!pStack.isEmpty()) throw new Exception("Unbalanced Parenthesis" +
													carrot(pStack.pop()) );
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
	private TermToken genTree(Token[] tknExp)
	{
		//may cause trouble with misplaced token order
		if(tknExp.length == 1){
			return (TermToken)tknExp[0];
		}

		//CURR
		TermToken curr = new TermToken();

		TermToken tree = curr;

		int ind = tknExp.length - 1;
		//System.out.println(tknExp.length);

		curr.opdRight = (TermToken)tknExp[ind];
		//TermToken opdRight = tknExp[ind];

		curr.opr = (OpToken)tknExp[--ind];
		//opToken op = tknExp[--ind];

		ind--;

		for(; ind >= 2; ind--){

			curr.opdLeft = new TermToken();
			curr = curr.opdLeft;

			//Debug--
			//System.out.println(curr + ", " + ind);

			curr.opdRight = (TermToken)tknExp[ind];
			curr.opr = (OpToken)tknExp[-- ind];

		}
		TermToken end = new TermToken();
		end.opdRight = (TermToken)tknExp[0];
		end.opr = new OpToken("+");
		end.opdLeft = new ValueToken(0);

		curr.opdLeft = end;
		return tree;
	}*/

	/*
	* This method takes a substring of the starting expression
	* and gets the operator charactors starting at index 0;
	* until the charactor is not in the opChars string
	*/

	private OpToken getOp(String subExp)
	{
		char curr = subExp.charAt(0);

		int index = 0;

		while(opChars.indexOf(curr) != -1 )
		{	curr = subExp.charAt( ++index );	}

		offset += index;

		OpToken op;
		//Debug
		//System.out.println(curr);

		try
		{ op = new OpToken(subExp.substring(0, index )); }

		catch(InputMismatchException exc)
		{ throw new InputMismatchException(
						exc.getMessage() +
			  			carrot( (baseExp.length() - subExp.length()) + 2));
		}
		return op;
	}

	//retrieve the entirety of the Symbol and loook it up
	//return the Symbols value from the table, or throws and error
	//should the symbol not be found.
	private int getSymbol(String subExp)
	{
		String sym = "";
		Integer result = null;

//		DEBUG
//		System.out.println(subExp);
//		System.out.println("Got to sym find");

		if(!environment.isEmpty() && environment != null){

			Stack<HashMap> envir = (Stack)this.environment.clone();
			result = null;
			int index = 0;
			char curr = subExp.charAt(index);

			while (symbolChars.indexOf(curr) != -1){
				index ++;
				curr = subExp.charAt(index);
			}

			sym = subExp.substring(0, index);
			offset += index;

//			DEBUG
//			System.out.println("[" + sym +"]");

			while(result == null && !envir.isEmpty()){

				//DEBUG
				//System.out.println(result == null && !environment.isEmpty());

				//System.out.println("IsEmpty? " + environment.peek());
				result = (Integer) envir.pop().get(sym);
			}
		}
		if(result == null)
		{ throw new NoSuchElementException("Symbol not found: "  + sym +
											carrot( baseExp.length() - subExp.length() + 2 ));
		}

		return result;
	}

	private int calc(Integer opdLeft, Integer opdRight, String oper)
	{	//   0    1    2    3    4    5    6    7    8    9    10    11
		//  "+", "-", "!", "~", "*", "/", "%", "&", "|", "^", "<<", ">>",
		//  12   13    14   15    16    17   18    19
		//  "<", "<=", "==","!=", ">=", ">", "&&", "||";
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

			case ">>>":
				result = (opdLeft >>> opdRight);
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