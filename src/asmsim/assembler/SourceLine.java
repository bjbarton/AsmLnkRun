package asmsim.assembler;

import java.util.Scanner;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

class SourceLine
	implements Cloneable
{
	int lineNumber;
	Integer address;
	String srcText;		//What the compiler read in
	String identifier;	//label or name for struct or union
	String OpCode;	//Tells us what to do.(e.g. OpCode, Directive)
	String args;		//The arguments for the Instruction
	String comment;
	String section;
	String errors;
	Integer instructionCode;
	int flags;
	/*
	external	0b0001 X
	relocate	0b0010 R

	*/

	LinkedList<SourceLine> includes;

	public SourceLine(String src)
	{
		srcText = src;
		identifier	= "";
		OpCode		= "";
		args		= "";
		comment		= "";
		includes	= null;
		errors		= "";
		instructionCode = null;
		section		= null;
		flags = 0;
		address = 0;
	}

	public SourceLine(String src,
					  String ident, String instr,
					  String arg, String comm,
					  LinkedList<SourceLine> srcLst)
	{
		address 	=	0;
		srcText		=	src;
		identifier	=	ident;
		OpCode		=	instr;
		args		=	arg;
		comment		=	comm;
		includes	=	srcLst;
		section 	=	null;
		errors		= 	"";
		instructionCode = null;
		flags = 0;

	}

	public SourceLine (SourceLine copy)
	{
		this.address 	=	copy.address;
		this.srcText	=	copy.srcText;
		this.identifier	=	copy.identifier;
		this.OpCode		=	copy.OpCode;
		this.args		=	copy.args;
		this.comment	=	copy.comment;
		this.includes	=	copy.includes;
		this.section 	=	copy.section ;
		this.errors		=	copy.errors	;
		this.instructionCode = copy.instructionCode;
		this.flags 		=	copy.flags;

	}

	public String getOpCode()
	{ return this.OpCode; }

	public String getSrc()
	{ return this.srcText; }

	public String getArgs()
	{ return this.args; }

	public String getIdentifier()
	{ return this.identifier; }

	public List<SourceLine> getInclude()
	{ return this.includes; }

	public Integer getInstructionCode()
	{ return this.instructionCode; }

	public String getSection()
	{ return this.section;	}

	public String getFlagString()
	{
		StringBuilder flagString = new StringBuilder("");
		//external
		if( (this.flags & 0b0001) == 0b0001) flagString.append('X');
		//relocate
		if( (this.flags & 0b0010) == 0b0010) flagString.append('R');

		//System.out.println(this.getSrc() + "\nflags: " + flagString.toString());

		return flagString.toString();
	}

	public String toString()
	{
		return (srcText + "\n " + identifier + " : " +
		OpCode + " : " +  args + " : " + comment + " : \n" );
	}

	//public Object clone()
	//{
	//	return this;
	//}
}

class DataLine extends SourceLine
{
	Long value;
	int size;
	//int offset; //if inside an object, otherwise, 0;

	public DataLine(String src, int size, long val)
	{
		super(src);
		this.size = size;
		instructionCode = (int)(val);
	}

	public DataLine(String txt,
					String ident, String instr,
					String arg, String comm,
					long val, int off)
	{
		super(txt, ident, instr, arg, comm, null);
		value = val;
//		offset = off;
	}

	public int getSize()
	{	return this.size; }
}

class ObjectLine extends SourceLine
{
    String type;
    public ObjectLine(String src)
    {
        super(src);
    }
    
    public ObjectLine(String txt,  String ident, String instr,
					  String arg, String comm,
					  LinkedList<SourceLine> srcLst)
    {
        super(txt, ident, instr, arg, comm, srcLst);
    }

    public ObjectLine(String txt,  String ident, String instr,
					  String arg, String comm,
                                          String type,
					  LinkedList<SourceLine> srcLst)
    {
        super(txt, ident, instr, arg, comm, srcLst);
        this.type = type;
    }
        
    public ObjectLine (ObjectLine copy)
    {
        super(copy);
    }
    
}

/*
abstract class Directive extends SourceLine
{
	int priority;
	//LinkedList<SourceLine> includes;
	public Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	//returns the offset for the program count
	abstract int evaluate(int programCount, Object listOrMap);
}

class Section_Directive extends Directive{

	public Section_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Include_Directive extends Directive{

	public Include_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}


class Global_Directive extends Directive{

	public Global_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object varMap){

		HashMap<String, SymbolObj> vars = (HashMap<String, SymbolObj>)varMap;
		for(String arg : this.args.split(",\\s*")){
			//If this identifier has been defined already
			//we do not want to lose its value.
			SymbolObj symVal = vars.get(arg);

			if(symVal == null){
				symVal = new SymbolObj(null, 0b0000, null);
			}
			vars.put(arg, symVal);
		}
		return programCount;
	}
}

class Extern_Directive extends Directive{

	public Extern_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object varMap)
	{
		HashMap<String, SymbolObj> vars = (HashMap<String, SymbolObj>)varMap;
		for(String arg : this.args.split(",\\s*")){
			//If this identifier is external
			//then it has no local value
			vars.put( arg, new SymbolObj(0, 0b0001, "") );
		}

		return 0;
	}
}

class Byte_Directive extends Directive{

	public Byte_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Half_Directive extends Directive{

	public Half_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Word_Directive extends Directive{

	public Word_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Dword_Directive extends Directive{

	public Dword_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Equ_Directive extends Directive{

	public Equ_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object varMap)
	{
		HashMap<String, SymbolObj> vars = (HashMap<String, SymbolObj>)varMap;
		vars.put( this.identifier,
			new SymbolObj( Integer.parseInt(args)));

		return programCount;
	}
}

class Align_Directive extends Directive{

	public Align_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap)
	{
		return 0;
	}
}

class Ascii_Directive extends Directive{

	public Ascii_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Asciiz_Directive extends Ascii_Directive{

	public Asciiz_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class End_Directive extends Directive{

	public End_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class If_Directive extends Directive{

	public If_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}
	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Else_Directive extends Directive{

	public Else_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Struct_Directive extends Directive{

	public Struct_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}

class Union_Directive extends Directive{

	public Union_Directive(String txt, String ident,
					String instr, String arg,
					String comm,  LinkedList<SourceLine> srcLst)
	{
		super(txt, ident, instr, arg, comm, srcLst);
	}

	int evaluate(int programCount, Object listOrMap){
		return 0;
	}
}


*/