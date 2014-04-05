/*
*@author Brian Hufsmith
* This is a generic XMLParser. to help create structured
* Documents 
 */
package asmsim.XMLParser;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.File; 
import java.io.IOException;
import java.util.List;

public class XMLTree {
    
    //Any properties that will reside in the Head of this 
    //XML Element
    private List<String> properties;
    
    //A list of all the subElements for this XMLTree
    private List<XMLTree> subElements;
    
    //The tabSpacing for a single level.
    private final String tabSpacing = "    ";
    //The tag for this object. i.e. "XML" 
    //will be printed as <XML> ... </XML>
    private String XMLTag;
    //Any text that should be printed between the start and end tags
    //this text will be printed on the first line, and will be printed
    //with a newLine after it.
    private List<String> text;
    //The parent of this XMLTree
    private XMLTree parent;
    //what level is this node on. 
    private int level; 
    
    
    //Default
    XMLTree(){
        
        this.properties = new LinkedList<>();
        this.subElements = new LinkedList<>();
        this.text = new LinkedList<>();
        this.XMLTag = "XMLObject";
        this.parent = null;
        setLevel();
    }
    
    XMLTree(XMLTree parent, String tag)
    {
        
        this.properties = new LinkedList<>();
        this.subElements = new LinkedList<>();
        
        this.XMLTag = tag;
        this.parent = parent;
        
        setLevel();
    }
            
    XMLTree(XMLTree parent, String tag, 
                Collection properties, Collection subElements){
        
        this.properties = properties;
        this.subElements = subElements;
        
        this.XMLTag = tag;
        this.parent = parent;
        setLevel();
    }
    
    //Sets the level of this node
    //This is for the tab spacing before each XML node in the tree
    public void setLevel()
    {
        if(parent == null){
            this.level = 1;
        }
        else {
            this.level = parent.getLevel() + 1;
        }
    }
    
    public int getLevel()
    { return this.level; }
    
    public List<String> getProperties() { return properties; }
    
    public List<XMLTree> getSubElements() { return subElements; }
    
    public void setTag(String tag)
    { this.XMLTag = tag;  }
    
    private String getTab(){
        String tab = "";
        for(int i = 0; i < this.level; i++){
            tab += tabSpacing;
        }
        return tab;
    }
        
    public String toString()
    {
        String printString = "<" + this.XMLTag + "  ";
        String prop = "";
        for(String xmlProp : properties){
            prop += xmlProp + "  ";
        }
        printString  += prop + ">\n";
        
        for(XMLTree xml : subElements){
            printString += xml.toString();
        }
                
       printString += "</" + this.XMLTag +">\n" + tabSpacing;
       return printString;
    }
    
    
    //This method will take an XMLTree root and print the xml
    //Tree into an XML Document. 
    public static void toXMLFile(String fileName, XMLTree root)
    {
    }
    
    //This method will take a source line from the input XML
    //File and convert it to an XMLTree representation
    private static XMLTree srcToXML()
    {
        return null;
    }
    
    //This method will read xml data into an XMLTree Class Structure. 
    public static XMLTree fromXMLFile(String fileName) 
    {
        XMLTree currentParent = new XMLTree();
        return null; 
    }
    
    //This method will read from an XML file and append the XML onto a 
    //specified node. 
    public static XMLTree appendFromXMLFile(String fileName, XMLTree currentParent)
    {
       return null; 
    }
}
