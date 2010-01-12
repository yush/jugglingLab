package jugglinglab.jml;

//import jugglinglab.jml.*;

public class JMLPatternList {
	
	
	public JMLPatternList(){
		
	}
	
	public void writePatternList(){
		StringBuffer result = new StringBuffer();
        for (int i = 0; i < JMLDefs.jmlprefix.length; i++)
            result.append(JMLDefs.jmlprefix[i]);
		System.out.print(result);
	}
}