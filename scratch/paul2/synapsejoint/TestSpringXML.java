import java.io.FileInputStream;

import java.io.InputStream;

import org.apache.synapse.RuleList;

public class TestSpringXML {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InputStream is;
		try {
			is = new FileInputStream("c:\\syntest\\repos\\synapserules.xml");
	
			RuleList rl = new RuleList(is, null);
			
			System.out.println(rl);
	

			

			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
			

	}

}
