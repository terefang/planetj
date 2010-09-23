package planetj;

import java.util.Properties;

public interface IPlanet 
{
	public void init(Properties prop);
	public void setup();
	public void process();
	public void save();
}
