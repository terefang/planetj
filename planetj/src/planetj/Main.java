package planetj;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception 
	{
		if(args.length==0)
		{
			doRandom();
		}
	}
	
	public static void doRandom() throws Exception
	{
		String id = UUID.randomUUID().toString().toUpperCase();
		
		Properties prop = new Properties();
		prop.setProperty("-i", "-.015");
		
		prop.setProperty("-l", "180.0");
		prop.setProperty("-L", "0.0");
		
		prop.setProperty("-m", "1.0");
		prop.setProperty("-s", Double.toString(new Random().nextDouble()));
		
		prop.setProperty("-g", "30.0");
		prop.setProperty("-G", "30.0");
		
		prop.setProperty("-p", "q");
		prop.setProperty("-a", "true");

		prop.setProperty("-w", "1024");
		prop.setProperty("-h", "512");

		PlanetJ planet = new PlanetJ();
		
		planet.init(prop);
		
		System.err.println("Processing ... ");
		planet.process();
		
		System.err.println("Saving ... ");
		planet.save(id+".gif");
		System.err.println("Saved ... ");
		prop.store(new FileOutputStream(id+".prop"), "planetj properties "+id);
	}

}
