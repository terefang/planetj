package planetj;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

public class Main 
{

	public static void main(String[] args) throws Exception 
	{
		Properties prop;
		
		if(args.length==0)
		{
			prop = doRandom();
		}
		else
		{
			prop = procArgs(args);
		}

		PlanetJ planet = new PlanetJ();
		
		planet.init(prop);
		
		System.err.println("Processing ... ");
		planet.process();
		
		System.err.println("Saving ... ");
		planet.save();
		System.err.println("Saved ... ");
	}
	
	static Properties procArgs(String[] args) {
		Properties prop = new Properties();
		Vector<String> left = new Vector();
		
		for(int i=0; i<args.length; i++)
		{
			if(args[i].startsWith("--") && args[i].indexOf('=')>0)
			{
				prop.setProperty(args[i].substring(2, args[i].indexOf('=')), args[i].substring(args[i].indexOf('=')+1));
			}
			else if(args[i].startsWith("--"))
			{
				prop.setProperty(args[i].substring(2), args[i+1]);
				i++;
			}
			else if(args[i].startsWith("-") && args[i].length()==2)
			{
				prop.setProperty(args[i], args[i+1]);
				i++;
			}
			else if(args[i].startsWith("-") && args[i].length()>2)
			{
				prop.setProperty(args[i].substring(0,2), args[i].substring(2));
			}
			else
			{
				left.add(args[i]);
			}
		}
		
		if(left.size()>0)
		{
			prop.setProperty("-o", left.firstElement());
		}
		
		return prop;
	}

	static Properties doRandom() throws Exception
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

		prop.setProperty("-o", id+".gif");

		prop.store(new FileOutputStream(id+".prop"), "planetj properties "+id);
		
		return prop;
	}
}
