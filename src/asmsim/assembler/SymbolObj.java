package asmsim.assembler;

class SymbolObj implements Cloneable
{
	/*//FLAGS	 bit #,	value
	* extern	= 0,		1    (1)
	* Address	= 1,		2   (10)
	* global	= 2,		4  (100)

	*/

	//for
	Integer value;
	String section;
	int flags;
	int size;

	public SymbolObj()
	{
		this.flags	= 0;
		this.value	= null;
		this.size	= 0;
		this.section = "";

	}

	public SymbolObj(Integer val)
	{
		this.value = val;
		this.flags	= 0;
		this.size	= 0;
		this.section = "";
	}

	public SymbolObj(Integer val, int flag, String sect)
	{
		this.flags	= flag;
		this.value	= val;
		this.size	= 0;
		this.section = sect;
	}

	public SymbolObj(Integer val, int flag, int syze)
	{
		this.flags	= flag;
		this.value	= val;
		this.size	= syze;
		this.section = "";
 	}

 	public SymbolObj(Integer val, int flag, int syze, String sect)
 	{
		this.flags	= flag;
		this.value	= val;
		this.size	= syze;
		this.section = sect;
	}

 	public SymbolObj clone()
 	{
		return new SymbolObj(value, flags, size, section);
	}

}

