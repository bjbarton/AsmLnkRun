package asmsim.assembler;

import asmsim.expression.ExpressionSolver;
import java.util.Scanner;
import java.util.Random;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class tst
{

	public static void main(String[] args)
	{
		//System.out.println("\nHI\n\nthis is two line lower");
		List<Integer> c = new LinkedList<>();
		c.add(1);
		c.add(2);
		c.add(3);
		c.add(4);

		System.out.println(c);

	}
	/*public static void main(String[] args)
	{

		String testSrc = "This is a +varTest varTest222";
		String regex = "(?=[\\D&\\W])varTest(?=[\\D&\\W])";
		Pattern tokens = Pattern.compile(regex);
		Matcher pattrnMatch = tokens.matcher(testSrc);


		testSrc.replaceAll(regex, "[replaced]");

		//while(pattrnMatch.find()){
		//	testSrc = ( testSrc.substring(0, pattrnMatch.start(1)) +
		//			"[Replaced]" + testSrc.substring(pattrnMatch.end(1)));
//
		//}


		testSrc = testSrc.replaceAll(regex, "[replaced]");
		System.out.println(testSrc);
	}*/

	/*public static void main(String[] args) throws Exception
	{

		LinkedList<Integer> numbers = new LinkedList<Integer>();

		numbers.add(0);
		numbers.add(1);

		numbers.add(2);
		numbers.add(3);

		ListIterator<Integer> lstIt = (ListIterator<Integer>)numbers.iterator();
		System.out.println(lstIt.next());
		System.out.println(lstIt.next());
		System.out.println(lstIt.next());
		System.out.println(lstIt.previous());
		System.out.println(lstIt.next());
	}*/

	public static void tstExpression() throws Exception
	{
		ExpressionSolver expSolve = new ExpressionSolver();
		Scanner ExprIn = new Scanner(System.in);

		for(String x = ExprIn.nextLine(); !x.equals("x");){
			System.out.println(expSolve.eval(x, null));
			x = ExprIn.nextLine();
		}
	}

/*	public static void tstFormInstruction() throws Exception
	{
		SparcArchitecture sa = new SparcArchitecture();
		Scanner ExprIn = new Scanner(System.in);
		String opCode, args;
		int ind;
		for(String x = ExprIn.nextLine(); !x.equals("x");){
			ind = x.indexOf(' ');
			opCode = x.substring(0, ind);
			args = x.substring( ++ind ).trim();
			System.out.println(Integer.toHexString(
									sa.formInstruction(opCode, args) ));
			x = ExprIn.nextLine();
		}

	}*/

/*	public static void tstElimNonDeciNums()
	{
		CodeCompiler cc = new CodeCompiler("", new String[10]);

		Scanner sc = new Scanner(System.in);
		String in = sc.nextLine();
		while(! (in.equals("x"))){
			cc.elimNonDecimalBase(in);
			in = sc.nextLine();

		}

	}*/
}