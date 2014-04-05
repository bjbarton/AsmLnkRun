package asmsim.assembler;
import java.util.HashMap;
import java.util.Stack;

public abstract class Architecture
{


	public static Architecture getArch(String arch)
	{
		return null;
	}

	public abstract Integer formInstruction(SourceLine line)
												throws Exception;

}

