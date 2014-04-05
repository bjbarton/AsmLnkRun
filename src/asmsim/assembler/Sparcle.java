package asmsim.assembler;

import java.io.File;
import java.util.LinkedList;


public class Sparcle
{
	public static void main(String[] args)
	{

		LinkedList<File> fileList = new LinkedList<>();
		for(String arg : args)
		{
			if(arg.charAt(0) == '-'){
				//Switch on switch char
			}
			else{
				//assume we have a file. If not throw an exeption.
				try{
					fileList.add( new File(arg));
				}
				catch(Exception exc){
					System.err.println("Could not access file: " + arg);
				}

			}

		}
	}
}