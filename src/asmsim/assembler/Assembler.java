package asmsim.assembler;

import asmsim.expression.ExpressionSolver;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.EOFException;
import java.io.IOException;
import java.util.regex.*;
//import asmsim.XMLParser;


public class Assembler
{
	public static void main(String[] args) throws Exception
	{
		Assembler cp = new Assembler(null, args);
		cp.process("tstSrc.asm");
	}

	Stack< Map< String, SymbolObj > > environment;
	Stack< Map< String, Integer > > valueStack;

	Set<String> macros = new HashSet<String>();

	char commentIdent = ';';

	//List<File>	fileList;	//For imported or included files.
	List<String> listingFile;	//The version to be sent to object file.
	Set<String> globalSym;		//And to be used for list file
	Set<String> relocate;
	//Set<String> externalSym;

	Architecture architecture;
	List<SourceLine> scannedSrc;
	Map<String, Integer> sections;
	Map<String, SourceLine> macroMap;
	int numMacroCalls;

//	Architecture architecture;


	public Assembler(String archType, String[] fList) throws Exception
	{
	//	relocate = new HashSet<String>();
//		externalSym = new HashSet<String>();
		//globalSym = new HashSet<String>();
	//	listingFile = new LinkedList<String>();
		environment = new Stack <>();
	//	valueStack = new Stack<>();
		scannedSrc = new LinkedList<>();
		architecture = new SparcArchitecture();

		sections = new HashMap<String, Integer>();
		macroMap = new HashMap<String, SourceLine>();
		numMacroCalls = 0;

		sections.put("text", 0);
		sections.put("data", 0);
		sections.put("rodata", 0);


		Map<String, SymbolObj> initial = new HashMap<>();

		initial.put("r0", new SymbolObj(0));
		initial.put("r1", new SymbolObj(1));
		initial.put("r2", new SymbolObj(2));
		initial.put("r3", new SymbolObj(3));
		initial.put("r4", new SymbolObj(4));
		initial.put("r5", new SymbolObj(5));
		initial.put("r6", new SymbolObj(6));
		initial.put("r7", new SymbolObj(7));
		initial.put("r8", new SymbolObj(8));
		initial.put("r9", new SymbolObj(9));
		initial.put("r10", new SymbolObj(10));
		initial.put("r11", new SymbolObj(11));
		initial.put("r12", new SymbolObj(12));
		initial.put("r13", new SymbolObj(13));
		initial.put("r14", new SymbolObj(14));
		initial.put("r15", new SymbolObj(15));

		environment.push(initial);

		for(String file : fList) this.process(file);

		//architecture = Architecture.getArch(archType);
	}

	public void process(String file) throws Exception
	{
		File thisFile = new File(file);
		List<SourceLine> fileSrc = readFile(thisFile);


			firstPass(fileSrc);
			secondPass(scannedSrc);
			thirdPass(scannedSrc);
			replaceVars(scannedSrc);

			formInstruction(scannedSrc);
			generateListFile(fileSrc);
			printSymTable();

//			XMLParser toXML = new XMLParser();
//			toXML.sourceToXML(file, scannedSrc, environment);
		//}
		//catch(Exception exc){
		//	System.out.println(exc.getMessage());
		//}

	}

	private void printSymTable()
	{
		System.out.println("-- Symbol Tab --");
		Map<String, SymbolObj> table;
		while(!environment.isEmpty()){
			table = environment.pop();
			Set<String> curr = table.keySet();
			Iterator<String> currIt = curr.iterator();

			while(currIt.hasNext()){
				String key  = currIt.next();
				System.out.println("Key : " + key + " Val: " + 
                                        Integer.toHexString(table.get(key).value));
			}
		}
	}

	private LinkedList<SourceLine> readFile(File curr)
	{
		//The lines in current file
		LinkedList<SourceLine> fileContent = new LinkedList<SourceLine>();

		try(Scanner fScan = new Scanner(curr))
		{
			String line;
			int lNum = 1;

			while(fScan.hasNextLine()){
				line = fScan.nextLine();
				fileContent.add( breakdown(line) );
			}
		}

		catch(FileNotFoundException fnfExc)
		{
			System.out.println("Could not find file: " + curr);
		}

		return fileContent;
	}


/*	private SourceLine toLineObj(String src, int lnNum)
	{
		SourceLine thisLine = null;

		if( !src.trim().equals("") ){

			src.replaceAll("\\s", " ");
			String srcTxt	= src;

			int comInd = src.indexOf(commentIdent);
			String comm = "";

			if(comInd >= 0 ){
				comm	= src.substring(comInd);
				src 	= src.substring(0, comInd) + '\n';
			}

			Scanner line = new Scanner(src);

			String ident	= ( src.charAt(0) == ' ') ? "" : line.next();

			String instr	= ( !line.hasNext() ) ? "" : line.next();

			String args		= line.nextLine();

		}
		else{
			thisLine = new SourceLine("");
		}

		return thisLine;
	}
*/
	public SourceLine breakdown(String src)
	{
		SourceLine thisLine;

		if ( !src.trim().equals("") )
		{
			//Set up parts of a line
			String ident 	= "";
			String srcTxt	= src;
			String instr	= "";
			String args		= "";
			String comm		= "";

			//If there is a comment, store it and remove it from the line.
			int comIndx = src.indexOf(';');
			if (comIndx >= 0) {
				comm = src.substring(comIndx);
				src = src.substring(0, comIndx);
			}

			Scanner line = new Scanner(src);

			//Get optional identifier, must start at the beginning of a line
			ident = (src.charAt(0) != ' ')? line.next() : "";


			/*
			* If there is more than an Identifier left then we have
			* either an instruction (e.g. OpCode, directive), or
			* an instruction with arguments.
			*/
			if(line.hasNext()){
				instr = line.next().trim();
				//everything after the instruction is a comma separated list of args
			//	System.out.println( ident + " : " + instr );
				args  = line.nextLine().trim();
			}

			thisLine = toLineObj(srcTxt, ident, instr, args, comm);
		}
		else
			thisLine = new SourceLine("");

		return thisLine;
	}



	private SourceLine toLineObj(String srcTxt, String ident,
								   String instr, String args, String comm)
	{
		SourceLine thisLine;

		
                String[] objTypes = {".struct", ".union", ".macro"};
		for(String curr : objTypes){

			if(instr.equals(curr)){
                            String type = curr.substring(1);
				thisLine = new ObjectLine(srcTxt, ident, instr, 
                                            args, comm, type,  null);
			}			
		}

		if(instr.equals(".end")){
			thisLine = new ObjectLine(srcTxt, ident, instr, args, comm, ".end " + args, null);
		}

		else if(instr.equals(".include")){
			File incSrc = new File(args);
			LinkedList<SourceLine> includeSrc = readFile(incSrc);

			thisLine = new SourceLine(srcTxt, ident, instr, args,
			comm, includeSrc);
		}

		else{
			thisLine = new SourceLine(srcTxt, ident, instr, args,
			comm, null);
		}

		return thisLine;
	}

	/*
	* Folllow directives that are independent of memory address
	* and other directives. requires only the directive or the directive
	* and its arguments.
	*/
	private void firstPass(List<SourceLine> lineLst)
		throws EOFException
	{
		HashMap<String, SymbolObj> vars = new HashMap<String, SymbolObj>();

		ListIterator<SourceLine> srcList = lineLst.listIterator();
		String section = "text";

		while(srcList.hasNext()){
			SourceLine currLine = srcList.next();
			String[] currLineArgs = currLine.args.split(",\\s");

			switch(currLine.OpCode){

				//directives do not need addresses.

				case ".extern" :
					currLine.address = null;
					for(String arg : currLineArgs){
						//If this identifier is external
						//then it has no local value
						vars.put( arg, new SymbolObj(0, 0b0001, "") );
					}

				break;
				case ".equ" :
				currLine.address = null;
					//equate the identifier to a single Integer argument.
					vars.put( currLine.getIdentifier(),
								new SymbolObj( Integer.parseInt(
												currLine.getArgs()), 0,
												section) );
				break;
				case ".global" :
					currLine.address = null;

					for(String arg : currLineArgs){
						//If this identifier has been defined already
						//we do not want to lose its value.
						SymbolObj symVal = vars.get(arg);

						if(symVal == null){
							symVal = new SymbolObj(null, 0b0000, null);
						}
						vars.put(arg, symVal);
					}

					break;

				case ".include" :
					currLine.address = null;

					firstPass( currLine.getInclude() );
					scannedSrc.addAll(currLine.getInclude());
					break;

				case ".macro":
					currLine.address = null;
					buildMacro(currLine, srcList);
					//firstPass(currLine.includes);
					break;
				case ".section":
					section = currLine.args;

				default:
					scannedSrc.add(currLine);
					System.out.println(currLine);
					break;

			}
		}
		environment.push(vars);
	}

	/*
	* expand struct, union, and macro prototypes.
	*/
	private void secondPass(List<SourceLine> lineLst)
	throws EOFException
	{
		ListIterator<SourceLine> srcList = lineLst.listIterator();
		SourceLine curr;

		while(srcList.hasNext()){
			curr = srcList.next();
			//check to see if this is a macro
			String name = curr.OpCode;
			SourceLine temp = macroMap.get(name);

			if(temp != null){
				curr.address = null;
				srcList.remove();
				macroExpand(curr, temp, srcList);
			}


		}

	}

	/*
	* Execute directives that depend on memory address -
	* section, byte, half, word, dword, align, ascii[z]
	* expand macros and objects
	* build structs and unions in memory.
	*/
	private void thirdPass(List<SourceLine> lineLst)
	{
		int progCount = 0;

		String sect = "text";
		ListIterator<SourceLine> lst = lineLst.listIterator();
		SourceLine currLine;
		String[] currLineArgs;

		HashMap<String, SymbolObj> labels = new HashMap<String, SymbolObj>();

		while(lst.hasNext()){
			currLine = lst.next();
			currLine.address = progCount;

			currLineArgs = currLine.args.split(",\\s");

			if(! currLine.getIdentifier().equals("")){
								//add this label and note address, not value

				labels.put(currLine.getIdentifier(),
									  //address   address-Flag section
						new SymbolObj( progCount, 0b010,       sect ) );
			}

			switch(currLine.OpCode){

				case ".align":
					if (currLineArgs.length != 1)
						throw new IllegalArgumentException("Illegal number of arguments: " +
							currLine.getSrc());
					else{
						currLine.address = null;
						int boundary =
						Integer.parseInt(currLineArgs[0]);

						if((progCount & (boundary - 1)) != 0 ){
							progCount = progCount + boundary;
							progCount = progCount & ( -boundary );
						}
					}
					break;
				case ".char"  :
				case ".byte"  :
				case ".short" :
				case ".half"  :
				case ".int"   :
				case ".word"  :
				case ".long":
				case ".dword" :
					progCount =
						(genData (currLine,
								//points to current spot in the list
								//genData will insert new lines
								//based on the args.and directive.
								lst ));

					break;
				case ".section":
						currLine.address = null;
						sections.put(sect, progCount);

						sect = currLine.args;

						Integer thisSectionValue = sections.get(sect);

						//if this section has not been previously defined
						//then we need to start at 0;
						if(thisSectionValue == null)  thisSectionValue = 0;
						//subtract 4 so that the loop will make it correct.

						//directive does not need address
						//currLine.address = null;

						lst.remove();
						progCount = thisSectionValue;
					break;
			}
			//If this is not a directive then it is an instruction
			//Or empty line.
			if(!currLine.OpCode.equals("")){
				if(currLine.OpCode.charAt(0) != '.')
					progCount += 4;
			}
			currLine.section = sect;
		}
		environment.push(labels);
	}

	private void buildMacro(SourceLine macro,
							ListIterator<SourceLine> srcList)
		throws EOFException
	{
		boolean inMacro = true;
		SourceLine curr = srcList.next();
		macro.includes = new LinkedList<SourceLine>();

		while(inMacro){
			curr.address = null;
			if(curr.OpCode.equalsIgnoreCase(".end")){
				if(curr.args.equalsIgnoreCase("macro"))
					inMacro = false;
			}
			else{
				macro.includes.add(curr);
			}

			if(srcList.hasNext())
				curr = srcList.next();
			else
				throw new EOFException(
					"Reached end of source file without ending macro - line:" +
					macro.lineNumber + ": " + macro.args);
		}
		srcList.previous();
		String name = macro.args.substring(0, macro.args.indexOf(' '));
		macroMap.put(name, macro);

	}

	private void macroExpand(SourceLine callMacro, SourceLine macro,
							ListIterator<SourceLine> srcList)
		throws IllegalArgumentException
	{
		/*the args inside the macro source line
		are the parameters for the macro.
		the name of the macro is from index 0 to the first space
		We do not need it in the args and have matched the name
		with the macro call already. This method takes the sourceLine that
		was generated when the macro was created as an argument.
		*/
		String tmp = macro.args.substring(macro.args.indexOf(' ') + 1);
		String[] param	= tmp.split(",\\s");
		String[] arg	= callMacro.args.split(",\\s");

		//System.out.println("In Macro Expand Method.");

		if(arg.length != param.length)
			throw new IllegalArgumentException(
						"Invalid number of arguments for macro - line: " +
						macro.lineNumber + " : " + macro.args);

		for(int index = 0; index < param.length; index++){

			if(param[index].charAt(0) == '%')
				if(arg[index].charAt(0) != '%'){
					throw new IllegalArgumentException(
							"Invalid argument list for macro - line: " +
							macro.lineNumber + " : " + macro.args);
				}
		}

		//set up to add the modified macro text to the included src
		//of the calling line
		callMacro.includes = new LinkedList<SourceLine>();

		//list to keep track of identifiers so they can be appended with an
		//index number for this macro instance.
		List<String> identList = new LinkedList<>();

		for(SourceLine curr : macro.includes){

			SourceLine newCurr = new SourceLine( curr );
			callMacro.includes.add( newCurr );

			//if there are identifiers, they will need to be
			//renamed to prevent duplicates. Collect them while we add
			// the macro text to the source list.
			if(! curr.identifier.equals("") ){
				identList.add(curr.identifier);
                newCurr.identifier = curr.identifier + numMacroCalls;
			}

		}
		//Is this character non alphanumeric
		String nonAlphaNumChar = "(?=[\\D&\\W])";

		for(String currIdent : identList){

			String identRegex = nonAlphaNumChar + currIdent + nonAlphaNumChar;
			String sequencedIdent = currIdent + numMacroCalls;



			for(SourceLine curr : callMacro.includes){

				curr.args = curr.args.replaceAll(identRegex, sequencedIdent);
				curr.srcText = curr.srcText.replaceAll(identRegex, sequencedIdent);
				System.out.println("replace args and Source: " + curr);
			}

		}

		for(int index = 0; index < param.length; index++){
			/*
			* here we need to match the parameter but make sure it is not
			* simply part of a larger parameter.
			* (e.g .macro test a, ab)
			* We need to make sure we do not replace
			* the 'a' in "ab" with argument 0 so that
			* we can replace "ab" with argument 2 without causing an error
			*/
			//here we make sure that the parameter is isolated by
			//non alpha-numeric charators,
			String regex = nonAlphaNumChar + param[index] + nonAlphaNumChar;

			//for each souceLine in this macro we need to replace the
			//parameter with the argument text.
			for(SourceLine curr : callMacro.includes){
				curr.args = curr.args.replaceAll(regex, arg[index]);
			}
		}


		for(SourceLine curr : callMacro.includes){
			srcList.add(curr);
			System.out.println("-" + curr);
		}


	}

	/*
	* Executed when we come across a data directive such as (.word, ,byte, .long)
	* returns the program count after adding the propper data lines to the included source
	* list of the Sourceline that envoked the data Directive (dataDirective)
	*/
	private int genData(SourceLine dataDirective, ListIterator<SourceLine> lst)

	{
		int pcOffset = 0;
		int size = 0;
		String type = dataDirective.OpCode;
		String[] args = dataDirective.args.split(",\\s");

		switch(type)
		{
			case ".long"	:
			case ".dword"	: size = 8;
							break;
			case ".int"		:
			case ".word"	: size = 4;
							break;
			case ".short"	:
			case ".half"	: size = 2;
							break;
			case ".char"	:
			case ".byte"	: size = 1;
							break;
		}

		//DataLine datLine = new DataLine();
		SourceLine datLine = dataDirective;

		//if(args.length > 1)
			dataDirective.includes = new LinkedList<SourceLine>();

		//eliminate the sourceLine for the directive
		//so we can add a DataLine in stead.
		//int currCount = lst.previous().address;

		 //adjust currCount so it is aligned
		int currCount = (dataDirective.address % size != 0)?
				(dataDirective.address + size) : dataDirective.address;

		for(int argInd = 0; argInd< args.length; argInd++){
			String arg = args[argInd];
			datLine.address = currCount;



			if(arg.matches("\\d*")){
				//if this arg is a number then get the low order
				//bits associated with this size and save it in
				//datLine to be added to the list.
				Long insInt = Long.parseLong(arg);

				/*
				* Create a mask of the apropriate size.
				* (e.g.  11111111 for byte (or size 1) )
				* ex.size = 1
				* total_bits = (size * 8) = (1 * 8) = 8
				* 1 << 8 = 0b1_0000_0000
				* (1 << 32) - 1 = 0b1111_1111
				*
				*/
				int mask = 0b1111_1111;
				for(int i = 1; ((i < size) & (i < 4)); i++)
					mask = ( (mask << 8) | 0b11111111 );

				if(size == 8){
					//take the low order 32 bits of a double word
					int tmp = (int)(insInt & mask);

					//add the high order 32 bits to the list first;

					datLine.instructionCode = (int)(insInt >> 32);
					//MOD lst.add(datLine);
					if(argInd == 0)
						dataDirective = datLine;
					else
						dataDirective.includes.add(datLine);

					currCount += 4;
					//pcOffset += 4;

					//set up next line for low order 32 bits
					datLine = new SourceLine("");
					datLine.address = currCount;
					insInt = (long)tmp;
				}
				datLine.instructionCode = (int)(insInt & mask);
				//lst.add(datLine);
				//System.out.println(dataDirective.includes.size());
				if(argInd == 0)
					dataDirective = datLine;
				else
					dataDirective.includes.add(datLine);

				//dataDirective.includes.add(datLine);
				pcOffset += size;

				datLine = new SourceLine("");
			}
			else{
				SourceLine tempData = new SourceLine("");
				tempData.errors = "Must use integer arguments: " + arg;
				dataDirective.includes = new LinkedList<SourceLine>();

			}
			currCount += size;
		}
		currCount = (currCount % 4 != 0)? (currCount + size) & size : currCount;

		return currCount;
	}


	private void replaceVars(List<SourceLine> lineLst)throws Exception
	{
		//ExpressionSolver ex = new ExpressionSolver();

		Integer ans;
		SourceLine currLine;
		//int hi = 0b1111111111111111111111_0000000000;
		//String maskHi = Integer.toString(hi) + " & (";
		String maskLo = "1023 & (";

		ListIterator<SourceLine> lines = lineLst.listIterator();

		while (lines.hasNext()){

			currLine = lines.next();

			//-----DEBUG
			//System.out.println("{-- " + currLine);
			if(currLine.getArgs() != null){

				if(currLine.OpCode.equals(".include"))
					replaceVars( currLine.getInclude() );

					String args = currLine.getArgs();

					//Take care of %hi and %lo
					int hiStrt = args.indexOf("%hi");

					if( hiStrt >= 0){
						//Possible Exception if no comma is
						//used between second and third argument
						String replace = args.substring(
										hiStrt,
										args.indexOf(',', hiStrt+3));

						args = args.replace(replace,
										replace.substring(3) + " >> 10");

					}
						args = args.replaceAll("%lo\\(", maskLo);
								//currLine.args = args;
					try{
						SymbolSwapResult swapRes = symbolMatch(args);
						currLine.args = swapRes.augmentedArgs;

						int flags = 0;

						for(SymbolObj symbol : swapRes.symbolList){
							System.out.println(symbol.section + " == " + currLine.section + " = " + symbol.section.equalsIgnoreCase(currLine.section));
							if(symbol.section.equalsIgnoreCase(currLine.section)){
								symbol.flags = symbol.flags & 1101;
							}
							else{
								currLine.section = symbol.section;
							}

							flags = flags | symbol.flags;
						}
						currLine.flags = flags;

					}
					catch(Exception exc){
						currLine.errors = currLine.errors + exc.getMessage();
						System.out.println("CurrLine.errors: " + currLine.errors);
					}
			}
		}
	}

	private class SymbolSwapResult
	{
		List<SymbolObj> symbolList;
		String augmentedArgs;

		SymbolSwapResult(List<SymbolObj> syms, String args)
		{
			symbolList = syms;
			augmentedArgs = args;
		}
	}

	//change to private after testing.
	public String elimBinAndHex(String args) throws Exception
	{
		//String PrefBin = "0b";
		//String PrefHex = "0x";
		//String PrefOct = "0";

		String pref = "((0b)|(0x))";
		String number = "[0-9[a-fA-F]]*";
		String numbers = pref 	+ number;
		Pattern numberPattern 	= Pattern.compile(numbers);
		Matcher numberMatch		= numberPattern.matcher(args);
		ExpressionSolver expSlv = new ExpressionSolver();
		int val;
		while(numberMatch.find())
		{
			val = expSlv.eval(numberMatch.group(), null);
			args = args.replaceAll( numberMatch.group(), "" + val);
			System.out.println(args);
			numberMatch = numberPattern.matcher(args);

		}
		return args;
	}

	private SymbolSwapResult symbolMatch(String args)throws Exception
	{
		Pattern symbol = Pattern.compile("[a-zA-Z]\\w*");
		Matcher symMatch = symbol.matcher(args);


		@SuppressWarnings("unchecked")
		Stack<Map<String, SymbolObj>> envClone =
				(Stack<Map<String, SymbolObj>>) environment.clone();
		//Object envClone_ = environment.clone();
		//Stack<Map<String, SymbolObj>> envClone =  envClone_;


		Map<String, SymbolObj> currMap;

		String identifier;
		int flags = 0;
		SymbolObj currSym = null;

		List<SymbolObj> symbolObjList = new LinkedList<>();
		List<String> possibleVars = new LinkedList<>();

		//while the environment is not empty
		//continue to pop off the layered symbol tables

		//NEEDS WORDK Identifies Hex and binary as a symbol.
		args = elimBinAndHex(args);

		while(symMatch.find()){
			possibleVars.add( symMatch.group() );
		}

		while(! ( environment.isEmpty() )){

			currMap = environment.pop();
			ListIterator<String> varsIt = possibleVars.listIterator();
			String sym = "";

			while(varsIt.hasNext()){

				sym = varsIt.next();
				currSym = currMap.get(sym);

				if(currSym != null){
					flags = (flags | currSym.flags);

					args = args.replaceFirst(sym, "" + currSym.value);

					varsIt.remove();
					symbolObjList.add(currSym);
				}
			}

		}
		//reset the environment.
		environment = envClone;

		if( !possibleVars.isEmpty() )
		{
			String vars = "";

			for(String notFound : possibleVars)
			{
				vars = vars + notFound + ": ";
			}

			throw new Exception("Symbol not found : " + vars);
		}

		SymbolSwapResult result = new SymbolSwapResult(symbolObjList, args);
		return result;
	}


	private void generateListFile(List<SourceLine> processedSource)
	{
		//File srcListing = new File("srcListing.lst");
		String curLine = "";
		//String format = "%1$08X    %2$08X %3$-4S %4$-30S";
		//String no_address_fmt = "%1$-8S    %2$08X %3$-4S %4$-30S";
		boolean expand = true;

		String sect = ".text";
		//int address = 0;

		for(String key : sections.keySet()){
			sections.put(key, 0);
		}

		Iterator<SourceLine> lineList = processedSource.iterator();


		while(lineList.hasNext()){

			SourceLine currLine = lineList.next();

			boolean noAddress = (currLine.address == null);
			boolean noInsCode = (currLine.instructionCode == null);
			boolean noAttrib  = (currLine.flags == 0);
			String addressFmt	= noAddress? "            " : "%1$08X    ";
			String insCodeFmt	= noInsCode? "         " : "%2$08X ";
			String attribFmt	= noAttrib? "     " : "%3$-4S ";
			String sourceFmt	= "%4$-30S";
			String format		= addressFmt + insCodeFmt + attribFmt + sourceFmt;

			curLine = String.format(format, currLine.address,
									currLine.instructionCode,
									currLine.getFlagString(),
									currLine.getSrc());
				System.out.println(curLine);

				//If this is not a directive, and it has expandable source
				//Expand and show the source if ".expand" is set.
				//if(currLine.OpCode.indexOf('.') == -1){
					if (currLine.includes != null && expand == true){

						for (SourceLine curr : currLine.includes){
							noAddress = (curr.address == null);
							noInsCode = (curr.instructionCode == null);
							noAttrib  = (curr.flags == 0);
							addressFmt	= noAddress? "            " : "%1$08X    ";
							insCodeFmt	= noInsCode? "         " : "%2$08X ";
							attribFmt	= noAttrib? "     " : "%3$-4S ";
							sourceFmt	= "%4$-30S";
							format		= addressFmt + insCodeFmt + attribFmt + sourceFmt;

							System.out.println(String.format(format,
													curr.address,
													curr.instructionCode,
													curr.getFlagString(),
													curr.OpCode + " " +
													curr.args));
						}
					}

				if (! currLine.errors.equals("") ){
					System.out.println(currLine.errors);
				}
		}
	}

	private void formInstruction(List<SourceLine> srcList)throws Exception
	{

		SourceLine currLine;

		Iterator<SourceLine> line = srcList.iterator();
		//StringBuilder sb;

		int address = 0;

		while (line.hasNext()){
		//	sb = new StringBuilder("");
			currLine = line.next();

			//sb.append(Integer.toHexString(address)).append(" ");

			String opCode = currLine.getOpCode();

			if ( (opCode != null) && (opCode.length() >= 1) ){
				if( opCode.charAt(0) != '.'){
					try{
						//System.out.println("Try");
						currLine.instructionCode =
			 							this.architecture.formInstruction(currLine);
						//System.out.println(currLine.getOpCode() + "  " + currLine.getArgs());
						//System.out.println(Integer.toHexString( currLine.instructionCode ) );
					}
					catch(Exception exc){
						//System.out.println("This Was an ERROR: " + exc.getMessage());
						currLine.errors = currLine.errors + exc.getMessage();

					}
				}
			}
		}
	}
}
