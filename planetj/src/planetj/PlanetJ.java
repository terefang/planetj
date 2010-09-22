package planetj;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import javax.imageio.ImageIO;

/* planet.c */
/* planet generating program */
/* Copyright 1988-2002 Torben AE. Mogensen */

/* version of June 19 2002 */

/* The program generates planet maps based on recursive spatial subdivision */
/* of a tetrahedron containing the globe. The output is a colour PPM bitmap. */

/* The colours may optionally be modified according to latitude to move the */
/* icecaps lower closer to the poles, with a corresponding change in land colours. */

/* The Mercator map at magnification 1 is scaled to fit the Width */
/* it uses the full height (it could extend infinitely) */
/* The orthographic projections are scaled so the full view would use the */
/* full Height. Areas outside the globe are coloured black. */
/* Stereographic and gnomic projections use the same scale as orthographic */
/* in the center of the picture, but distorts scale away from the center. */

/* It is assumed that pixels are square */
/* I have included procedures to print the maps as bmp or ppm */
/* bitmaps (portable pixel map) on standard output or specified files. */

/* I have tried to avoid using machine specific features, so it should */
/* be easy to port the program to any machine. Beware, though that due */
/* to different precision on different machines, the same seed numbers */
/* can yield very different planets. */
/* The primitive user interface is a result of portability concerns */

public class PlanetJ 
{
	public static void main(String[] args) 
	{
		Properties prop = new Properties();
		prop.setProperty("-i", "-.015");
		
		prop.setProperty("-l", "180.0");
		prop.setProperty("-L", "0.0");
		
		prop.setProperty("-m", "1.0");
		prop.setProperty("-s", "0.6");
		
		prop.setProperty("-g", "0.0");
		prop.setProperty("-G", "0.0");
		
		prop.setProperty("-p", "q");
		prop.setProperty("-a", "true");

		prop.setProperty("-w", "1024");
		prop.setProperty("-h", "768");

		PlanetJ planet = new PlanetJ();
		
		planet.init(prop);
		
		System.err.println("Processing ... ");
		planet.process();
		
		System.err.println("Saving ... ");
		planet.save("planet-map.gif");
		System.err.println("Saved ... ");
	}

	public static int min(int x, int y)
	{ 
		return(x<y ? x : y); 
	}

	public static int max(int x,int y)
	{ 
		return(x<y ? y : x); 
	}

	public static double rand2(double p, double q)
	{
		double r;
		r = (p+3.14159265)*(q+3.14159265);
		return(2.*(r-(int)r)-1.);
	}

	public static double log_2(double x)
	{ 
		return(Math.log(x)/Math.log(2.0)); 
	}

	public void save(String f)
	{
		BufferedImage bufferedImage = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
		for(int ix=0; ix<Width; ix++)
		{
			for(int iy=0; iy<Height; iy++)
			{
				int n = col[ix][iy];
				int[] c = new int [] { rtable[n], gtable[n], btable[n]  };
				if(doshade) 
				{
					int s = shades[ix][iy];
					for(int q=0; q<3; q++)
					{
						c[q] = s*c[q]/150;
						if (c[q]>255) c[q]=255;
					}
				}
				bufferedImage.getRaster().setPixel(ix, iy, c);
			}
		}
		
		String image_type = "jpg";
		if(f.toLowerCase().endsWith(".jpg") || f.toLowerCase().endsWith(".jpeg"))
		{
			image_type = "jpg";
		}
		else if(f.toLowerCase().endsWith(".png"))
		{
			image_type = "png";
		}
		else if(f.toLowerCase().endsWith(".gif"))
		{
			image_type = "gif";
		}
		else
		{
			throw new IllegalArgumentException("Unknown image extension: "+f);
		}

		try 
		{
			ImageIO.write(bufferedImage, image_type, new File(f));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void saveWLD(String f)
	{
	}

	public void saveTERRAGEN(String f)
	{
	}

	long start;
	
	public static final double PI = 3.14159265358979;
	public static final double DEG2RAD = 0.0174532918661; /* pi/180 */
	
	public static final int BLACK = 0;
	public static final int WHITE = 1;
	public static final int BLUE0 = 2;

	public static final int MAXCOL =	9;
	
	public static int colors[][] =
	{{0,0,255},	    /* Dark blue depths		*/
		{0,128,255},   /* Light blue shores	*/
		{0,255,0},	    /* Light green lowlands	*/
		{64,192,16},   /* Dark green highlands	*/
		{64,192,16},   /* Dark green Mountains	*/
		{128,128,32},  /* Brown stoney peaks	*/
		{255,255,255}, /* White - peaks		*/
		{0,0,0},	    /* Black - Space		*/
		{0,0,0}};	    /* Black - Lines		*/

	public static final int std_colors[][] =
	{{0,0,255},	    /* Dark blue depths		*/
		{0,128,255},   /* Light blue shores	*/
		{0,255,0},	    /* Light green lowlands	*/
		{64,192,16},   /* Dark green highlands	*/
		{64,192,16},   /* Dark green Mountains	*/
		{128,128,32},  /* Brown stoney peaks	*/
		{255,255,255}, /* White - peaks		*/
		{0,0,0},	    /* Black - Space		*/
		{0,0,0}};	    /* Black - Lines		*/

	public static final int alt_colors[][] =
	{{0,0,192},	    /* Dark blue depths		*/
		{0,128,255},   /* Light blue shores	*/
		{0,96,0},	    /* Dark green Lowlands	*/
		{0,224,0},	    /* Light green Highlands	*/
		{128,176,0},   /* Brown mountainsides	*/
		{128,128,128}, /* Grey stoney peaks	*/
		{255,255,255}, /* White - peaks		*/
		{0,0,0},	    /* Black - Space		*/
		{0,0,0}};	    /* Black - Lines		*/

	public static double moll_table[] = {0.0, 0.0685055811, 0.1368109534, 0.2047150027,
		0.2720147303, 0.3385041213, 0.4039727534,
		0.4682040106, 0.5309726991, 0.5920417499,
		0.6511575166, 0.7080428038, 0.7623860881,
		0.8138239166, 0.8619100185, 0.9060553621,
		0.9453925506, 0.9783738403, 1.0};


	/* these values can be changed to change world characteristica */

	//public static double M  = -.02;   /* initial altitude (slightly below sea level) */
	public double M  = -.01;   /* initial altitude (slightly below sea level) */
	public double dd1 = 0.4;   /* weight for altitude difference */
	public double dd2 = 0.03;  /* weight for distance */
	public boolean debug;
	public boolean altColors;
	public double longi,lat,scale;
	public double vgrid, hgrid;
	public char view;
	public int nocols = 256;
	public int lighter = 0; /* specifies lighter colours */
	public boolean latic; /* flag for latitude based colour */
	public boolean do_outline;
	public boolean do_bw;
	public boolean doshade;
	public int Depth; /* depth of subdivisions */
	public double r1,r2,r3,r4; /* seeds */
	public double cla, sla, clo, slo;


	public int Width = 800, Height = 600;

	public int col[][];
	public int heights[][];
	//	int cl0[60][30];

	public int shade;
	public int shades[][];
	public double shade_angle = 150.0; /* angle of "light" on bumpmap */
	public double rseed, increment = 0.00000001;	

	public int BLUE1, LAND0, LAND1, LAND2, LAND4;
	public int GREEN1, BROWN0, GREY0;

	public int BACK = BLACK;

	public int black_r = 0,    black_g = 0,    black_b = 0;
	public int white_r = 255,  white_g = 255,  white_b = 255;

	public int rtable[] = new int[256], gtable[] = new int[256], btable[] = new int[256];

	public int best = 500000;
	public int weight[]= new int[30];
	private String filename;
	private String colorsname;

	private String world_file;

	private String ter_file;

	public void init(Properties prop)
	{
		M   = Double.parseDouble(prop.getProperty("-i", prop.getProperty("initial-altitude", "-.015")));
		
		dd1 = Double.parseDouble(prop.getProperty("-v", prop.getProperty("altitude-weight",  "0.4")));
		
		dd2 = Double.parseDouble(prop.getProperty("-V", prop.getProperty("distance-weight",  "0.03")));

		debug = Boolean.parseBoolean(prop.getProperty("-X", prop.getProperty("debug", "false")));
		
		rseed = Double.parseDouble(prop.getProperty("-s", prop.getProperty("seed", ""+(new Random().nextDouble()))));

		Width=Integer.parseInt(prop.getProperty("-w", prop.getProperty("width", "512")));

		Height=Integer.parseInt(prop.getProperty("-h", prop.getProperty("height", "384")));
		
		scale = Double.parseDouble(prop.getProperty("-m", prop.getProperty("magnification", "1.0")));

		filename = prop.getProperty("-o", prop.getProperty("out-file", "planet-map.jpg"));

		altColors = Boolean.parseBoolean(prop.getProperty("-a", prop.getProperty("alt-colors", "false")));
		
		if(altColors)
		{ 
			copyColors(alt_colors);
		}
		else
		{
			copyColors(std_colors);
		}

		colorsname = prop.getProperty("-M", prop.getProperty("map-file", "false"));
		world_file = prop.getProperty("-W", prop.getProperty("world-file", "false"));
		ter_file = prop.getProperty("-T", prop.getProperty("ter-file", "false"));
		
		longi = Double.parseDouble(prop.getProperty("-l", prop.getProperty("longitude", "0.0")));

		lat = Double.parseDouble(prop.getProperty("-L", prop.getProperty("latitude", "0.0")));

		vgrid = Double.parseDouble(prop.getProperty("-g", prop.getProperty("vgrid", "0.0")));

		hgrid = Double.parseDouble(prop.getProperty("-G", prop.getProperty("hgrid", "0.0")));

		latic = Boolean.parseBoolean(prop.getProperty("-c", prop.getProperty("latitude-colors", "false")));
		
		if(Boolean.parseBoolean(prop.getProperty("-E", prop.getProperty("edge", "false"))))
		{
			do_outline = true;
		}

		if(Boolean.parseBoolean(prop.getProperty("-O", prop.getProperty("outline", "false"))))
		{
			do_outline = true;
			do_bw = true;
		}

		if(Boolean.parseBoolean(prop.getProperty("-C", prop.getProperty("lighter", "false"))))
		{
			lighter += 1;
		}

		nocols = Integer.parseInt(prop.getProperty("-N", prop.getProperty("color-number", "32")));
		
		if (nocols<5) nocols = 5;
		if (nocols>256) nocols = 256;
		
		doshade = Boolean.parseBoolean(prop.getProperty("-B", prop.getProperty("shade", "false")));
		
		shade_angle = Double.parseDouble(prop.getProperty("-A", prop.getProperty("shade-angle", "150.0")));

		view = prop.getProperty("-p", prop.getProperty("projection", "mercator")).charAt(0);
		
		if (longi>180) longi -= 360;
		
		longi = longi*DEG2RAD;
		lat = lat*DEG2RAD;

		sla = Math.sin(lat); 
		cla = Math.cos(lat);
		slo = Math.sin(longi); 
		clo = Math.cos(longi);

		heights = new int[Width][];
		col = new int[Width][];
		shades = new int[Width][];
		for (int i=0; i<Width; i++) 
		{
			heights[i] = new int[Height];
			col[i] = new int[Height];
			shades[i] = new int[Height];
		}


		if (view == 'c') {
			if (lat == 0.0) view = 'm';
			/* Conical approaches mercator when lat -> 0 */
			if (Math.abs(lat) >= PI - 0.000001) view = 's';
			/* Conical approaches stereo when lat -> +/- 90 */
		}

		setcolours();

		Depth = 3*((int)(log_2(scale*Height)))+6;

		r1 = rseed;

		r1 = rand2(r1,r1);
		r2 = rand2(r1,r1);
		r3 = rand2(r1,r2);
		r4 = rand2(r2,r3);
	}
	
	public void process()
	{
		start=System.currentTimeMillis();
		
		switch (view) {

		case 'm': /* Mercator projection */
			mercator();
			break;

		case 'p': /* Peters projection (area preserving cylindrical) */
			peter();
			break;

		case 'q': /* Square projection (equidistant latitudes) */
			squarep();
			break;

		case 'M': /* Mollweide projection (area preserving) */
			mollweide();
			break;

		case 'S': /* Sinusoid projection (area preserving) */
			sinusoid();
			break;

		case 's': /* Stereographic projection */
			stereo();
			break;

		case 'o': /* Orthographic projection */
			orthographic();
			break;

		case 'g': /* Gnomonic projection */
			gnomonic();
			break;

		case 'a': /* Area preserving azimuthal projection */
			azimuth();
			break;

		case 'c': /* Conical projection (conformal) */
			conical();
			break;

		}

		if (do_outline) makeoutline(do_bw);

		if (doshade) smoothshades();
	}
	
	void makeoutline(boolean doBw)
	{
		int i,j,k;

		int[] outx = new int[Width*Height];
		int[] outy = new int[Width*Height];
		k=0;
		for (i=1; i<Width-1; i++)
			for (j=1; j<Height-1; j++)
				if ((col[i][j] >= BLUE0 && col[i][j] <= BLUE1) &&
						(col[i-1][j] >= LAND0 || col[i+1][j] >= LAND0 ||
								col[i][j-1] >= LAND0 || col[i][j+1] >= LAND0 ||
								col[i-1][j-1] >= LAND0 || col[i-1][j+1] >= LAND0 ||
								col[i+1][j-1] >= LAND0 || col[i+1][j+1] >= LAND0)) {
					outx[k] = i; outy[k++] =j;
				}
		if (doBw)
			for (i=0; i<Width; i++)
				for (j=0; j<Height; j++)
					if (col[i][j] != BLACK) col[i][j] = WHITE;
		while (k-->0) col[outx[k]][outy[k]] = BLACK;
	}
	
	public void smoothshades()
	{
		int i,j;

		for (i=0; i<Width-2; i++)
			for (j=0; j<Height-2; j++)
				shades[i][j] = (4*shades[i][j]+2*shades[i][j+1]
				             +2*shades[i+1][j]+shades[i+1][j+2]+4)/9;
	}
	
	void copyColors(int cTable[][])
	{
		int x, y;

		for (x = 0; x< MAXCOL; x++)
		{
			for (y = 0; y < 3; y++)
			{
				colors[x][y] = cTable[x][y];
			}
		}
	}
	
	void setcolours()
	{
		int i;

		if (altColors) {
			int	    crow;

			if (nocols < 8)
				nocols = 8;

			/*
			 *	This color table tries to follow the coloring conventions of
			 *	several atlases.
			 *
			 *	The first two colors are reserved for black and white
			 *	1/4 of the colors are blue for the sea, dark being deep
			 *	3/4 of the colors are land, divided as follows:
			 *	 nearly 1/2 of the colors are greens, with the low being dark
			 *	 1/8 of the colors shade from green through brown to grey
			 *	 1/8 of the colors are shades of grey for the highest altitudes
			 *
			 *	The minimum color table is:
			 *	    0	Black
			 *	    1	White
			 *	    2	Blue
			 *	    3	Dark Green
			 *	    4	Green
			 *	    5	Light Green
			 *	    6	Brown
			 *	    7	Grey
			 *	and doesn't look very good. Somewhere between 24 and 32 colors
			 *	is where this scheme starts looking good. 256, of course, is best.
			 */

			LAND0 = max(nocols / 4, BLUE0 + 1);
			BLUE1 = LAND0 - 1;
			GREY0 = nocols - (nocols / 8);
			GREEN1 = min(LAND0 + (nocols / 2), GREY0 - 2);
			BROWN0 = (GREEN1 + GREY0) / 2;
			LAND1 = nocols - 1;

			rtable[BLACK] = colors[7][0];
			gtable[BLACK] = colors[7][0];
			btable[BLACK] = colors[7][0];

			rtable[WHITE] = colors[6][0];
			gtable[WHITE] = colors[6][1];
			btable[WHITE] = colors[6][2];

			rtable[BLUE0] = colors[0][0];
			gtable[BLUE0] = colors[0][1];
			btable[BLUE0] = colors[0][2];

			for (i=BLUE0+1;i<=BLUE1;i++) {
				rtable[i] = (colors[0][0]*(BLUE1-i)+colors[1][0]*(i-BLUE0))/(BLUE1-BLUE0);
				gtable[i] = (colors[0][1]*(BLUE1-i)+colors[1][1]*(i-BLUE0))/(BLUE1-BLUE0);
				btable[i] = (colors[0][2]*(BLUE1-i)+colors[1][2]*(i-BLUE0))/(BLUE1-BLUE0);
			}
			for (i=LAND0;i<GREEN1;i++) {
				rtable[i] = (colors[2][0]*(GREEN1-i)+colors[3][0]*(i-LAND0))/(GREEN1-LAND0);
				gtable[i] = (colors[2][1]*(GREEN1-i)+colors[3][1]*(i-LAND0))/(GREEN1-LAND0);
				btable[i] = (colors[2][2]*(GREEN1-i)+colors[3][2]*(i-LAND0))/(GREEN1-LAND0);
			}
			for (i=GREEN1;i<BROWN0;i++) {
				rtable[i] = (colors[3][0]*(BROWN0-i)+colors[4][0]*(i-GREEN1))/(BROWN0-GREEN1);
				gtable[i] = (colors[3][1]*(BROWN0-i)+colors[4][1]*(i-GREEN1))/(BROWN0-GREEN1);
				btable[i] = (colors[3][2]*(BROWN0-i)+colors[4][2]*(i-GREEN1))/(BROWN0-GREEN1);
			}
			for (i=BROWN0;i<GREY0;i++) {
				rtable[i] = (colors[4][0]*(GREY0-i)+colors[5][0]*(i-BROWN0))/(GREY0-BROWN0);
				gtable[i] = (colors[4][1]*(GREY0-i)+colors[5][1]*(i-BROWN0))/(GREY0-BROWN0);
				btable[i] = (colors[4][2]*(GREY0-i)+colors[5][2]*(i-BROWN0))/(GREY0-BROWN0);
			}
			for (i=GREY0;i<nocols;i++) {
				rtable[i] = (colors[5][0]*(nocols-i)+(colors[6][0]+1)*(i-GREY0))/(nocols-GREY0);
				gtable[i] = (colors[5][1]*(nocols-i)+(colors[6][1]+1)*(i-GREY0))/(nocols-GREY0);
				btable[i] = (colors[5][2]*(nocols-i)+(colors[6][2]+1)*(i-GREY0))/(nocols-GREY0);
			}
		} else {
			rtable[BLACK] = 0;
			gtable[BLACK] = 0;
			btable[BLACK] = 0;

			rtable[WHITE] = 255;
			gtable[WHITE] = 255;
			btable[WHITE] = 255;

			while (lighter-->0) {
				int r, c;
				double x;

				for (r =	0; r < 7; r++)
					for (c = 0; c < 3; c++) {
						x = Math.sqrt((double)colors[r][c]/256.0);
						colors[r][c] = (int)(240.0*x+16);
					}
			}

			BLUE1 = (nocols-4)/2+BLUE0;
			if (BLUE1==BLUE0) {
				rtable[BLUE0] = colors[0][0];
				gtable[BLUE0] = colors[0][1];
				btable[BLUE0] = colors[0][2];
			} else
				for (i=BLUE0;i<=BLUE1;i++) {
					rtable[i] = (colors[0][0]*(BLUE1-i)+colors[1][0]*(i-BLUE0))/(BLUE1-BLUE0);
					gtable[i] = (colors[0][1]*(BLUE1-i)+colors[1][1]*(i-BLUE0))/(BLUE1-BLUE0);
					btable[i] = (colors[0][2]*(BLUE1-i)+colors[1][2]*(i-BLUE0))/(BLUE1-BLUE0);
				}
			LAND0 = BLUE1+1; LAND2 = nocols-2; LAND1 = (LAND0+LAND2+1)/2;
			for (i=LAND0;i<LAND1;i++) {
				rtable[i] = (colors[2][0]*(LAND1-i)+colors[3][0]*(i-LAND0))/(LAND1-LAND0);
				gtable[i] = (colors[2][1]*(LAND1-i)+colors[3][1]*(i-LAND0))/(LAND1-LAND0);
				btable[i] = (colors[2][2]*(LAND1-i)+colors[3][2]*(i-LAND0))/(LAND1-LAND0);
			}
			if (LAND1==LAND2) {
				rtable[LAND1] = colors[4][0];
				gtable[LAND1] = colors[4][1];
				btable[LAND1] = colors[4][2];
			} else
				for (i=LAND1;i<=LAND2;i++) {
					rtable[i] = (colors[4][0]*(LAND2-i)+colors[5][0]*(i-LAND1))/(LAND2-LAND1);
					gtable[i] = (colors[4][1]*(LAND2-i)+colors[5][1]*(i-LAND1))/(LAND2-LAND1);
					btable[i] = (colors[4][2]*(LAND2-i)+colors[5][2]*(i-LAND1))/(LAND2-LAND1);
				}
			LAND4 = nocols-1;
			rtable[LAND4] = colors[6][0];
			gtable[LAND4] = colors[6][1];
			btable[LAND4] = colors[6][2];
		}
	}

	public int alt2color(double alt, double x, double y, double z)
	{
		int colour;

		if (altColors)
		{
			double snow = .125;
			double tree = snow * 0.5;
			double bare = (tree + snow) / 2.;

			if (latic) {
				snow -= (.13 * (y*y*y*y*y*y));
				bare -= (.12 * (y*y*y*y*y*y));
				tree -= (.11 * (y*y*y*y*y*y));
			}

			if (alt > 0) {		    /* Land */
				if (alt > snow) {		    /* Snow: White */
					colour = WHITE;
				} else if (alt > bare) {	    /* Snow: Grey - White */
					colour = GREY0+(int)((1+LAND1-GREY0) *
							(alt-bare)/(snow-bare));
					if (colour > LAND1) colour = LAND1;
				} else if (alt > tree) {	    /* Bare: Brown - Grey */
					colour = GREEN1+(int)((1+GREY0-GREEN1) *
							(alt-tree)/(bare-tree));
					if (colour > GREY0) colour = GREY0;
				} else {			    /* Green: Green - Brown */
					colour = LAND0+(int)((1+GREEN1-LAND0) *
							(alt)/(tree));
					if (colour > GREEN1) colour = GREEN1;
				}
			} else {			    /* Sea */
				alt = alt/2;
				if (alt > snow) {		    /* Snow: White */
					colour = WHITE;
				} else if (alt > bare) {
					colour = GREY0+(int)((1+LAND1-GREY0) *
							(alt-bare)/(snow-bare));
					if (colour > LAND1) colour = LAND1;
				} else {
					colour = BLUE1+(int)((BLUE1-BLUE0+1)*(25*alt));
					if (colour<BLUE0) colour = BLUE0;
				}
			}
		} else {
			/* calculate colour */
			if (alt <=0.) { /* if below sea level then */
				if (latic && y*y+alt >= 0.98)
					colour = LAND4;	 /* white if close to poles */
				else {
					colour = BLUE1+(int)((BLUE1-BLUE0+1)*(10*alt));	  /* blue scale otherwise */
					if (colour<BLUE0) colour = BLUE0;
				}
			}
			else {
				if (latic) alt += 0.10204*y*y;  /* altitude adjusted with latitude */
				if (alt >= 0.1) /* if high then */
					colour = LAND4;
				else {
					colour = LAND0+(int)((LAND2-LAND0+1)*(10*alt));
					/* else green to brown scale */
					if (colour>LAND2) colour = LAND2;
				}
			}
		}
		return(colour);
	}

	public double ssa,ssb,ssc,ssd, ssas,ssbs,sscs,ssds,
	ssax,ssay,ssaz, ssbx,ssby,ssbz, sscx,sscy,sscz, ssdx,ssdy,ssdz;

	public double planet1(double x, double y, double z)
	{
		double abx,aby,abz, acx,acy,acz, adx,ady,adz, apx,apy,apz;
		double bax,bay,baz, bcx,bcy,bcz, bdx,bdy,bdz, bpx,bpy,bpz;

		abx = ssbx-ssax; aby = ssby-ssay; abz = ssbz-ssaz;
		acx = sscx-ssax; acy = sscy-ssay; acz = sscz-ssaz;
		adx = ssdx-ssax; ady = ssdy-ssay; adz = ssdz-ssaz;
		apx = x-ssax; apy = y-ssay; apz = z-ssaz;
		if ((adx*aby*acz+ady*abz*acx+adz*abx*acy
				-adz*aby*acx-ady*abx*acz-adx*abz*acy)*
				(apx*aby*acz+apy*abz*acx+apz*abx*acy
						-apz*aby*acx-apy*abx*acz-apx*abz*acy)>0.0){
			/* p is on same side of abc as d */
			if ((acx*aby*adz+acy*abz*adx+acz*abx*ady
					-acz*aby*adx-acy*abx*adz-acx*abz*ady)*
					(apx*aby*adz+apy*abz*adx+apz*abx*ady
							-apz*aby*adx-apy*abx*adz-apx*abz*ady)>0.0){
				/* p is on same side of abd as c */
				if ((abx*ady*acz+aby*adz*acx+abz*adx*acy
						-abz*ady*acx-aby*adx*acz-abx*adz*acy)*
						(apx*ady*acz+apy*adz*acx+apz*adx*acy
								-apz*ady*acx-apy*adx*acz-apx*adz*acy)>0.0){
					/* p is on same side of acd as b */
					bax = -abx; bay = -aby; baz = -abz;
					bcx = sscx-ssbx; bcy = sscy-ssby; bcz = sscz-ssbz;
					bdx = ssdx-ssbx; bdy = ssdy-ssby; bdz = ssdz-ssbz;
					bpx = x-ssbx; bpy = y-ssby; bpz = z-ssbz;
					if ((bax*bcy*bdz+bay*bcz*bdx+baz*bcx*bdy
							-baz*bcy*bdx-bay*bcx*bdz-bax*bcz*bdy)*
							(bpx*bcy*bdz+bpy*bcz*bdx+bpz*bcx*bdy
									-bpz*bcy*bdx-bpy*bcx*bdz-bpx*bcz*bdy)>0.0){
						/* p is on same side of bcd as a */
						/* Hence, p is inside tetrahedron */
						return(planet(ssa,ssb,ssc,ssd, ssas,ssbs,sscs,ssds,
								ssax,ssay,ssaz, ssbx,ssby,ssbz,
								sscx,sscy,sscz, ssdx,ssdy,ssdz,
								x,y,z, 11));
					}
				}
			}
		} /* otherwise */
		return(planet(M,M,M,M,
				/* initial altitude is M on all corners of tetrahedron */
				r1,r2,r3,r4,
				/* same seed set is used in every call */
				0.0, 0.0, 3.01,
				0.0, Math.sqrt(8.0)+.01, -1.01,
				-Math.sqrt(6.0)-.01, -Math.sqrt(2.0)-.01, -1.01,
				Math.sqrt(6.0)-.005, -Math.sqrt(2.0)-.005, -1.005,
				/* coordinates of vertices */
				x,y,z,
				/* coordinates of point we want colour of */
				Depth));
		/* subdivision depth */

	}

	public double planet( 
			double a, double b, double c, double d, /* altitudes of the 4 verticess */  
			double as, double bs, double cs, double ds, /* seeds of the 4 verticess */
			double ax, double ay, double az, /* vertex coordinates */
			double bx, double by, double bz, 
			double cx, double cy, double cz, 
			double dx, double dy, double dz,
			double x, double y, double z, /* goal point */
			int level /* levels to go */)
	{
		double abx,aby,abz, acx,acy,acz, adx,ady,adz;
		double bcx,bcy,bcz, bdx,bdy,bdz, cdx,cdy,cdz;
		double lab, lac, lad, lbc, lbd, lcd;
		double ex, ey, ez, e, es;
		double eax,eay,eaz, epx,epy,epz;
		double ecx,ecy,ecz, edx,edy,edz;
		double x1,y1,z1,x2,y2,z2,l1,tmp;

		if (level>0) {
			if (level==11) {
				ssa=a; ssb=b; ssc=c; ssd=d; ssas=as; ssbs=bs; sscs=cs; ssds=ds;
				ssax=ax; ssay=ay; ssaz=az; ssbx=bx; ssby=by; ssbz=bz;
				sscx=cx; sscy=cy; sscz=cz; ssdx=dx; ssdy=dy; ssdz=dz;
			}
			abx = ax-bx; aby = ay-by; abz = az-bz;
			acx = ax-cx; acy = ay-cy; acz = az-cz;
			lab = abx*abx+aby*aby+abz*abz;
			lac = acx*acx+acy*acy+acz*acz;

			if (lab<lac)
				return(planet(a,c,b,d, as,cs,bs,ds,
						ax,ay,az, cx,cy,cz, bx,by,bz, dx,dy,dz,
						x,y,z, level));
			else {
				adx = ax-dx; ady = ay-dy; adz = az-dz;
				lad = adx*adx+ady*ady+adz*adz;
				if (lab<lad)
					return(planet(a,d,b,c, as,ds,bs,cs,
							ax,ay,az, dx,dy,dz, bx,by,bz, cx,cy,cz,
							x,y,z, level));
				else {
					bcx = bx-cx; bcy = by-cy; bcz = bz-cz;
					lbc = bcx*bcx+bcy*bcy+bcz*bcz;
					if (lab<lbc)
						return(planet(b,c,a,d, bs,cs,as,ds,
								bx,by,bz, cx,cy,cz, ax,ay,az, dx,dy,dz,
								x,y,z, level));
					else {
						bdx = bx-dx; bdy = by-dy; bdz = bz-dz;
						lbd = bdx*bdx+bdy*bdy+bdz*bdz;
						if (lab<lbd)
							return(planet(b,d,a,c, bs,ds,as,cs,
									bx,by,bz, dx,dy,dz, ax,ay,az, cx,cy,cz,
									x,y,z, level));
						else {
							cdx = cx-dx; cdy = cy-dy; cdz = cz-dz;
							lcd = cdx*cdx+cdy*cdy+cdz*cdz;
							if (lab<lcd)
								return(planet(c,d,a,b, cs,ds,as,bs,
										cx,cy,cz, dx,dy,dz, ax,ay,az, bx,by,bz,
										x,y,z, level));
							else {
								ex = 0.5*(ax+bx); ey = 0.5*(ay+by); ez = 0.5*(az+bz);
								es = rand2(as,bs);
								if (lab>1.0) lab = Math.pow(lab,0.75);
								e = 0.5*(a+b)+es*(dd1*Math.abs(a-b)+dd2*Math.pow(lab,0.45));
								eax = ax-ex; eay = ay-ey; eaz = az-ez;
								epx = x-ex; epy = y-ey; epz = z-ez;
								ecx = cx-ex; ecy = cy-ey; ecz = cz-ez;
								edx = dx-ex; edy = dy-ey; edz = dz-ez;
								if ((eax*ecy*edz+eay*ecz*edx+eaz*ecx*edy
										-eaz*ecy*edx-eay*ecx*edz-eax*ecz*edy)*
										(epx*ecy*edz+epy*ecz*edx+epz*ecx*edy
												-epz*ecy*edx-epy*ecx*edz-epx*ecz*edy)>0.0)
									return(planet(c,d,a,e, cs,ds,as,es,
											cx,cy,cz, dx,dy,dz, ax,ay,az, ex,ey,ez,
											x,y,z, level-1));
								else
									return(planet(c,d,b,e, cs,ds,bs,es,
											cx,cy,cz, dx,dy,dz, bx,by,bz, ex,ey,ez,
											x,y,z, level-1));
							}
						}
					}
				}
			} 
		}
		else {
			if (doshade) {
				x1 = 0.25*(ax+bx+cx+dx);
				x1 = a*(x1-ax)+b*(x1-bx)+c*(x1-cx)+d*(x1-dx);
				y1 = 0.25*(ay+by+cy+dy);
				y1 = a*(y1-ay)+b*(y1-by)+c*(y1-cy)+d*(y1-dy);
				z1 = 0.25*(az+bz+cz+dz);
				z1 = a*(z1-az)+b*(z1-bz)+c*(z1-cz)+d*(z1-dz);
				l1 = Math.sqrt(x1*x1+y1*y1+z1*z1);
				if (l1==0.0) l1 = 1.0;
				tmp = Math.sqrt(1.0-y*y);
				if (tmp<0.0001) tmp = 0.0001;
				x2 = x*x1+y*y1+z*z1;
				y2 = -x*y/tmp*x1+tmp*y1-z*y/tmp*z1;
				z2 = -z/tmp*x1+x/tmp*z1;
				shade =
					(int)((-Math.sin(PI*shade_angle/180.0)*y2-Math.cos(PI*shade_angle/180.0)*z2)
							/l1*48.0+128.0);
				if (shade<10) shade = 10;
				if (shade>255) shade = 255;
			}
			return((a+b+c+d)/4);
		}
	}

	private void tickH(int j) {
		long curr=System.currentTimeMillis();
		
		if((j % (Height/10))==0)
		{
			System.err.println("- "+(j*100/Height)+"% ETA:"+new Date(System.currentTimeMillis()+((curr-start)*(Height-j+1)/(j+1))));
		}
	}

	private void tickW(int j) {
		long curr=System.currentTimeMillis();

		if((j % (Width/10))==0)
		{
			System.err.println("- "+(j*100/Width)+"% ETA:"+new Date(System.currentTimeMillis()+((curr-start)*(Width-j+1)/(j+1))));
		}
	}

	public void mercator()
	{
		double y,scale1,cos2,theta1;
		int i,j,k;

		y = Math.sin(lat);
		y = (1.0+y)/(1.0-y);
		y = 0.5*Math.log(y);
		k = (int)(0.5*y*Width*scale/PI);
		for (j = 0; j < Height; j++) 
		{
			y = PI*(2.0*(j-k)-Height)/Width/scale;
			y = Math.exp(2.*y);
			y = (y-1.)/(y+1.);
			scale1 = scale*Width/Height/Math.sqrt(1.0-y*y)/PI;
			cos2 = Math.sqrt(1.0-y*y);
			Depth = 3*((int)(log_2(scale1*Height)))+3;
			for (i = 0; i < Width ; i++) 
			{
				theta1 = longi-0.5*PI+PI*(2.0*i-Width)/Width/scale;
				double alt = planet1(Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
				col[i][j] = alt2color(alt, Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2); 
				heights[i][j] = (int) alt; 
				if (doshade) shades[i][j] = shade;
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = Math.sin(DEG2RAD*theta1);
				y = (1.0+y)/(1.0-y);
				y = 0.5*Math.log(y);
				j = Height/2+(int)(0.5*y*Width*scale/PI)+k;
				if (j>=0 && j<Height) for (i = 0; i < Width ; i++) col[i][j] = BLACK;
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta1 = 0.0; theta1>-360.0; theta1-=vgrid);
			for (theta1 = theta1; theta1<360.0; theta1+=vgrid) {
				i = (int)(0.5*Width*(1.0+scale*(DEG2RAD*theta1-longi)/PI));
				if (i>=0 && i<Width) for (j = 0; j < Height; j++) col[i][j] = BLACK;
			} 
		}
	}

	public void peter()
	{
		double y,cos2,theta1,scale1;
		int k,i,j,water,land;

		y = 2.0*Math.sin(lat);
		k = (int)(0.5*y*Width*scale/PI);
		water = land = 0;
		for (j = 0; j < Height; j++) {
			y = 0.5*PI*(2.0*(j-k)-Height)/Width/scale;
			if (Math.abs(y)>1.0)
				for (i = 0; i < Width ; i++) {
					col[i][j] = BACK;
					if (doshade) shades[i][j] = 255;
				}
			else {
				cos2 = Math.sqrt(1.0-y*y);
				if (cos2>0.0) {
					scale1 = scale*Width/Height/cos2/PI;
					Depth = 3*((int)(log_2(scale1*Height)))+3;
					for (i = 0; i < Width ; i++) {
						theta1 = longi-0.5*PI+PI*(2.0*i-Width)/Width/scale;
						double alt = planet1(Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
						col[i][j] = alt2color(alt, Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
						heights[i][j] = (int) alt; 
						if (doshade) shades[i][j] = shade;
						if (col[i][j] < LAND0) water++; else land++;
					}
				}
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = 2.0*Math.sin(DEG2RAD*theta1);
				j = Height/2+(int)(0.5*y*Width*scale/PI)+k;
				if (j>=0 && j<Height) for (i = 0; i < Width ; i++) col[i][j] = BLACK;
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta1 = 0.0; theta1>-360.0; theta1-=vgrid);
			for (theta1 = theta1; theta1<360.0; theta1+=vgrid) {
				i = (int)(0.5*Width*(1.0+scale*(DEG2RAD*theta1-longi)/PI));
				if (i>=0 && i<Width)
					for (j = max(0,Height/2-(int)(Width*scale/PI)+k);
					j < min(Height,Height/2+(int)(Width*scale/PI)+k); j++)
						col[i][j] = BLACK;
			}
		}
	}
	
	public void mollweide()
	{
		double x,y,y1,zz,scale1,cos2,theta1,theta2;
		int i,j,i1=1,k;

		for (j = 0; j < Height; j++) {
			y1 = 2*(2.0*j-Height)/Width/scale;
			if (Math.abs(y1)>=1.0) for (i = 0; i < Width ; i++) {
				col[i][j] = BACK;
				if (doshade) shades[i][j] = 255;
			} else {
				zz = Math.sqrt(1.0-y1*y1);
				y = 2.0/PI*(y1*zz+Math.asin(y1));
				cos2 = Math.sqrt(1.0-y*y);
				if (cos2>0.0) {
					scale1 = scale*Width/Height/cos2/PI;
					Depth = 3*((int)(log_2(scale1*Height)))+3;
					for (i = 0; i < Width ; i++) {
						theta1 = PI/zz*(2.0*i-Width)/Width/scale;
						if (Math.abs(theta1)>PI) {
							col[i][j] = BACK;
							if (doshade) shades[i][j] = 255;
						} else {
							theta1 += longi-0.5*PI;
							double alt = planet1(Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
							col[i][j] = alt2color(alt, Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
							heights[i][j] = (int)alt;
							if (doshade) shades[i][j] = shade;
						}
					}
				}
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				theta2 = Math.abs(theta1);
				x = Math.floor(theta2/5.0); y = theta2/5.0-x;
				y = (1.0-y)*moll_table[(int)x]+y*moll_table[(int)x+1];
				if (theta1<0.0) y = -y;
				j = Height/2+(int)(0.25*y*Width*scale);
				if (j>=0 && j< Height) {
					for (i = max(0,Width/2-(int)(0.5*Width*scale*Math.sqrt(1.0-y*y)));
					i < min(Width,Width/2+(int)(0.5*Width*scale*Math.sqrt(1.0-y*y))); i++)
						col[i][j] = BLACK;
				}
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta1 = 0.0; theta1>-360.0; theta1-=vgrid);
			for (theta1 = theta1; theta1<360.0; theta1+=vgrid) {
				if (DEG2RAD*theta1-longi+0.5*PI>-PI &&
				DEG2RAD*theta1-longi+0.5*PI<=PI) {
					x = 0.5*(DEG2RAD*theta1-longi+0.5*PI)*Width*scale/PI;
					j = max(0,Height/2-(int)(0.25*Width*scale));
					y = 2*(2.0*j-Height)/Width/scale;
					i = (int) (Width/2.0+x*Math.sqrt(1.0-y*y));
					for (; j <= min(Height,Height/2+(int)(0.25*Width*scale)); j++) {
						y1 = 2*(2.0*j-Height)/Width/scale;
						if (Math.abs(y1)<=1.0) {
							i1 = (int) (Width/2+x*Math.sqrt(1.0-y1*y1));
							if (i1>=0 && i1<Width) col[i1][j] = BLACK;
						}
						if (Math.abs(y)<=1.0) {
							if (i<i1) {
								for (k=i+1; k<i1; k++)
									if (k>00 && k<Width) col[k][j] = BLACK;
							}
							else if (i>i1) {
								for (k=i-1; k>i1; k--)
									if (k>=0 && k<Width) col[k][j] = BLACK;
							}
						}
						y = y1;
						i = i1;
					}
				}
			}
		}
	}
	
	public void sinusoid()
	{
		double y,theta1,theta2,cos2,l1,i1,scale1;
		int k,i,j,l,c;

		k = (int)(lat*Width*scale/PI);
		for (j = 0; j < Height; j++) {
			y = (2.0*(j-k)-Height)/Width/scale*PI;
			if (Math.abs(y)>=0.5*PI) for (i = 0; i < Width ; i++) {
				col[i][j] = BACK;
				if (doshade) shades[i][j] = 255;
			} else {
				cos2 = Math.cos(y);
				if (cos2>0.0) {
					scale1 = scale*Width/Height/cos2/PI;
					Depth = 3*((int)(log_2(scale1*Height)))+3;
					for (i = 0; i<Width; i++) {
						l = i*12/Width;
						l1 = l*Width/12.0;
						i1 = i-l1;
						theta2 = longi-0.5*PI+PI*(2.0*l1-Width)/Width/scale;
						theta1 = (PI*(2.0*i1-Width/12)/Width/scale)/cos2;
						if (Math.abs(theta1)>PI/12.0) 
						{
							col[i][j] = BACK;
							if (doshade) shades[i][j] = 255;
						} 
						else 
						{
							double alt = planet1(Math.cos(theta1+theta2)*cos2,Math.sin(y),
									-Math.sin(theta1+theta2)*cos2);
							col[i][j] = alt2color(alt, Math.cos(theta1+theta2)*cos2,Math.sin(y),
									-Math.sin(theta1+theta2)*cos2);
							heights[i][j] = (int)alt;
							if (doshade) shades[i][j] = shade;
						}
					}
				}
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (; theta1<90.0; theta1+=hgrid) {
				y = DEG2RAD*theta1;
				cos2 = Math.cos(y);
				j = Height/2+(int)(0.5*y*Width*scale/PI)+k;
				if (j>=0 && j<Height)
					for (i = 0; i < Width ; i++) {
						l = i*12/Width;
						l1 = l*Width/12.0;
						i1 = i-l1;
						theta2 = (PI*(2.0*i1-Width/12)/Width/scale)/cos2;
						if (Math.abs(theta2)<=PI/12.0) col[i][j] = BLACK;
					}
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta1 = 0.0; theta1>-360.0; theta1-=vgrid);
			for (; theta1<360.0; theta1+=vgrid) {
				i = (int)(0.5*Width*(1.0+scale*(DEG2RAD*theta1-longi)/PI));
				if (i>=0 && i<Width)
					for (j = max(0,Height/2-(int)(0.25*PI*Width*scale/PI)+k);
					j < min(Height,Height/2+(int)(0.25*PI*Width*scale/PI)+k); j++) {
						y = (2.0*(j-k)-Height)/Width/scale*PI;
						cos2 = Math.cos(y);
						l = i*12/Width;
						l1 = l*Width/12.0+Width/24.0;
						i1 = i-l1;
						c = (int) (l1+i1*cos2);
						if (c>=0 && c<Width) col[c][j] = BLACK;
					}
			}
		}
	}
	
	public void stereo()
	{
		double x,y,ymin,ymax,z,zz,x1,y1,z1,theta1,theta2;
		int i,j;

		ymin = 2.0;
		ymax = -2.0;
		for (j = 0; j < Height; j++) {
			for (i = 0; i < Width ; i++) {
				x = (2.0*i-Width)/Height/scale;
				y = (2.0*j-Height)/Height/scale;
				z = x*x+y*y;
				zz = 0.25*(4.0+z);
				x = x/zz;
				y = y/zz;
				z = (1.0-0.25*z)/zz;
				x1 = clo*x+slo*sla*y+slo*cla*z;
				y1 = cla*y-sla*z;
				z1 = -slo*x+clo*sla*y+clo*cla*z;
				if (y1 < ymin) ymin = y1;
				if (y1 > ymax) ymax = y1;
				double alt = planet1(x1,y1,z1);
				col[i][j] = alt2color(alt, x1,y1,z1);
				heights[i][j] = (int)alt;
				if (doshade) shades[i][j] = shade;
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = Math.sin(DEG2RAD*theta1);
				if (ymin <= y && y <= ymax) {
					zz = Math.sqrt(1-y*y);
					for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
						x = Math.sin(theta2)*zz;
						z = Math.cos(theta2)*zz;
						x1 = clo*x+slo*z;
						y1 = slo*sla*x+cla*y-clo*sla*z;
						z1 = -slo*cla*x+sla*y+clo*cla*z;
						if (Math.abs(z1)<1.0){
							i = (int) (0.5*(Height*scale*2.0*x1*(1+z1)/(1.0-z1*z1)+Width));
							j = (int) (0.5*(Height*scale*2.0*y1*(1+z1)/(1.0-z1*z1)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
				y = Math.sin(theta2);
				if (ymin <= y && y <= ymax) {
					for (theta1 = 0.0; theta1<360.0; theta1+=vgrid) {
						x = Math.sin(DEG2RAD*theta1)*Math.cos(theta2);
						z = Math.cos(DEG2RAD*theta1)*Math.cos(theta2);
						x1 = clo*x+slo*z;
						y1 = slo*sla*x+cla*y-clo*sla*z;
						z1 = -slo*cla*x+sla*y+clo*cla*z;
						if (Math.abs(z1)<1.0){
							i = (int) (0.5*(Height*scale*2.0*x1*(1+z1)/(1-z1*z1)+Width));
							j = (int) (0.5*(Height*scale*2.0*y1*(1+z1)/(1-z1*z1)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
	}

	public void orthographic()
	{
		double x,y,z,x1,y1,z1,ymin,ymax,theta1,theta2,zz;
		int i,j;

		ymin = 2.0;
		ymax = -2.0;
		for (j = 0; j < Height; j++) {
			for (i = 0; i < Width ; i++) {
				x = (2.0*i-Width)/Height/scale;
				y = (2.0*j-Height)/Height/scale;
				if (x*x+y*y>1.0) 
				{
					heights[i][j]=0;
					col[i][j] = BACK;
					if (doshade) shades[i][j] = 255;
				} else {
					z = Math.sqrt(1.0-x*x-y*y);
					x1 = clo*x+slo*sla*y+slo*cla*z;
					y1 = cla*y-sla*z;
					z1 = -slo*x+clo*sla*y+clo*cla*z;
					if (y1 < ymin) ymin = y1;
					if (y1 > ymax) ymax = y1;
					double alt = planet1(x1,y1,z1);
					col[i][j] = alt2color(alt, x1,y1,z1);
					heights[i][j] = (int)alt;
					if (doshade) shades[i][j] = shade;
				}
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = Math.sin(DEG2RAD*theta1);
				if (ymin <= y && y <= ymax) {
					zz = Math.sqrt(1-y*y);
					for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
						x = Math.sin(theta2)*zz;
						z = Math.cos(theta2)*zz;
						x1 = clo*x+slo*z;
						y1 = slo*sla*x+cla*y-clo*sla*z;
						z1 = -slo*cla*x+sla*y+clo*cla*z;
						if (0.0>=z1){
							i = (int) (0.5*(Height*scale*x1+Width));
							j = (int) (0.5*(Height*scale*y1+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
				y = Math.sin(theta2);
				if (ymin <= y && y <= ymax) {
					for (theta1 = 0.0; theta1<360.0; theta1+=vgrid) {
						x = Math.sin(DEG2RAD*theta1)*Math.cos(theta2);
						z = Math.cos(DEG2RAD*theta1)*Math.cos(theta2);
						x1 = clo*x+slo*z;
						y1 = slo*sla*x+cla*y-clo*sla*z;
						z1 = -slo*cla*x+sla*y+clo*cla*z;
						if (0.0>=z1){
							i = (int) (0.5*(Height*scale*x1+Width));
							j = (int) (0.5*(Height*scale*y1+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
	}

	public void gnomonic()
	{
		double x,y,z,x1,y1,z1,zz,theta1,theta2,ymin,ymax;
		int i,j;

		ymin = 2.0;
		ymax = -2.0;
		for (j = 0; j < Height; j++) {
			for (i = 0; i < Width ; i++) {
				x = (2.0*i-Width)/Height/scale;
				y = (2.0*j-Height)/Height/scale;
				zz = Math.sqrt(1.0/(1.0+x*x+y*y));
				x = x*zz;
				y = y*zz;
				z = Math.sqrt(1.0-x*x-y*y);
				x1 = clo*x+slo*sla*y+slo*cla*z;
				y1 = cla*y-sla*z;
				z1 = -slo*x+clo*sla*y+clo*cla*z;
				if (y1 < ymin) ymin = y1;
				if (y1 > ymax) ymax = y1;
				double alt = planet1(x1,y1,z1);
				col[i][j] = alt2color(alt, x1,y1,z1);
				heights[i][j] = (int)alt;
				if (doshade) shades[i][j] = shade;
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = Math.sin(DEG2RAD*theta1);
				if (ymin <= y && y <= ymax) {
					zz = Math.sqrt(1-y*y);
					for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
						x = Math.sin(theta2)*zz;
						z = Math.cos(theta2)*zz;
						x1 = clo*x-slo*z;
						y1 = slo*sla*x+cla*y+clo*sla*z;
						z1 = slo*cla*x-sla*y+clo*cla*z;
						if (z1!=0.0){
							i = (int) (0.5*(Height*scale*x1/z1+Width));
							j = (int) (0.5*(Height*scale*y1/z1+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
				y = Math.sin(theta2);
				if (ymin <= y && y <= ymax) {
					for (theta1 = 0.0; theta1<360.0; theta1+=vgrid) {
						x = Math.sin(DEG2RAD*theta1)*Math.cos(theta2);
						z = Math.cos(DEG2RAD*theta1)*Math.cos(theta2);
						x1 = clo*x-slo*z;
						y1 = slo*sla*x+cla*y+clo*sla*z;
						z1 = slo*cla*x-sla*y+clo*cla*z;
						if (z1!=0.0){
							i = (int) (0.5*(Height*scale*x1/z1+Width));
							j = (int) (0.5*(Height*scale*y1/z1+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
	}
	
	public void azimuth()
	{
		double x,y,z,x1,y1,z1,zz,theta1,theta2,ymin,ymax;
		int i,j;

		ymin = 2.0;
		ymax = -2.0;
		for (j = 0; j < Height; j++) {
			for (i = 0; i < Width ; i++) {
				x = (2.0*i-Width)/Height/scale;
				y = (2.0*j-Height)/Height/scale;
				zz = x*x+y*y;
				z = 1.0-0.5*zz;
				if (z<-1.0) {
					col[i][j] = BACK;
					if (doshade) shades[i][j] = 255;
				} else {
					zz = Math.sqrt(1.0-0.25*zz);
					x = x*zz;
					y = y*zz;
					x1 = clo*x+slo*sla*y+slo*cla*z;
					y1 = cla*y-sla*z;
					z1 = -slo*x+clo*sla*y+clo*cla*z;
					if (y1 < ymin) ymin = y1;
					if (y1 > ymax) ymax = y1;
					double alt = planet1(x1,y1,z1);
					col[i][j] = alt2color(alt, x1,y1,z1);
					heights[i][j] = (int)alt;
					if (doshade) shades[i][j] = shade;
				}
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = Math.sin(DEG2RAD*theta1);
				if (ymin <= y && y <= ymax) {
					zz = Math.sqrt(1-y*y);
					for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
						x = Math.sin(theta2)*zz;
						z = Math.cos(theta2)*zz;
						x1 = clo*x-slo*z;
						y1 = slo*sla*x+cla*y+clo*sla*z;
						z1 = slo*cla*x-sla*y+clo*cla*z;
						if (z1!=-1.0){
							i = (int) (0.5*(Height*scale*x1/Math.sqrt(0.5+0.5*z1)+Width));
							j = (int) (0.5*(Height*scale*y1/Math.sqrt(0.5+0.5*z1)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta2=-PI; theta2<PI; theta2+=0.5/Width/scale) {
				y = Math.sin(theta2);
				if (ymin <= y && y <= ymax) {
					for (theta1 = 0.0; theta1<360.0; theta1+=vgrid) {
						x = Math.sin(DEG2RAD*theta1)*Math.cos(theta2);
						z = Math.cos(DEG2RAD*theta1)*Math.cos(theta2);
						x1 = clo*x-slo*z;
						y1 = slo*sla*x+cla*y+clo*sla*z;
						z1 = slo*cla*x-sla*y+clo*cla*z;
						if (z1!=-1.0){
							i = (int) (0.5*(Height*scale*x1/Math.sqrt(0.5+0.5*z1)+Width));
							j = (int) (0.5*(Height*scale*y1/Math.sqrt(0.5+0.5*z1)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
	}
	
	public void conical()
	{
		double k1,c,y2,x,y,zz,x1,y1,z1,theta1,theta2,ymin,ymax,cos2;
		int i,j;

		ymin = 2.0;
		ymax = -2.0;
		if (lat>0) {
			k1 = 1.0/Math.sin(lat);
			c = k1*k1;
			y2 = Math.sqrt(c*(1.0-Math.sin(lat/k1))/(1.0+Math.sin(lat/k1)));
			for (j = 0; j < Height; j++) {
				for (i = 0; i < Width ; i++) {
					x = (2.0*i-Width)/Height/scale;
					y = (2.0*j-Height)/Height/scale+y2;
					zz = x*x+y*y;
					if (zz==0.0) theta1 = 0.0; else theta1 = k1*Math.atan2(x,y);
					if (theta1<-PI || theta1>PI) {
						col[i][j] = BACK;
						if (doshade) shades[i][j] = 255;
					} else {
						theta1 += longi-0.5*PI; /* theta1 is longitude */
						theta2 = k1*Math.asin((zz-c)/(zz+c));
						/* theta2 is latitude */
						if (theta2 > 0.5*PI || theta2 < -0.5*PI) {
							col[i][j] = BACK;
							if (doshade) shades[i][j] = 255;
						} else {
							cos2 = Math.cos(theta2);
							y = Math.sin(theta2);
							if (y < ymin) ymin = y;
							if (y > ymax) ymax = y;
							double alt = planet1(Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
							col[i][j] = alt2color(alt, Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
							heights[i][j] = (int)alt;
							if (doshade) shades[i][j] = shade;
						}
					}
				}
				tickH(j);
			}
			if (hgrid != 0.0) { /* draw horisontal gridlines */
				for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
				for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
					y = Math.sin(DEG2RAD*theta1);
					if (ymin <= y && y <= ymax) {
						zz = Math.sqrt(c*(1.0+Math.sin(DEG2RAD*theta1/k1))
								/(1.0-Math.sin(DEG2RAD*theta1/k1)));
						for (theta2=-PI+longi; theta2<PI+longi; theta2+=0.5/Width/scale) {
							z1 = theta2-longi;
							x1 = zz*Math.sin(z1/k1);
							y1 = zz*Math.cos(z1/k1);
							i = (int) (0.5*(Height*scale*x1+Width));
							j = (int) (0.5*(Height*scale*(y1-y2)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
			if (vgrid != 0.0) { /* draw vertical gridlines */
				for (theta1=-0.5*PI; theta1<0.5*PI; theta1+=0.5/Width/scale) {
					y = Math.sin(theta1);
					if (ymin <= y && y <= ymax) {
						zz = Math.sqrt(c*(1.0+Math.sin(theta1/k1))
								/(1.0-Math.sin(theta1/k1)));
						for (theta2 = 0.0; theta2>-180.0+longi/DEG2RAD; theta2-=vgrid);
						for (theta2 = theta2; theta2<180.0+longi/DEG2RAD; theta2+=vgrid) {
							z1 = DEG2RAD*theta2-longi;
							x1 = zz*Math.sin(z1/k1);
							y1 = zz*Math.cos(z1/k1);
							i = (int) (0.5*(Height*scale*x1+Width));
							j = (int) (0.5*(Height*scale*(y1-y2)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
		else {
			k1 = 1.0/Math.sin(lat);
			c = k1*k1;
			y2 = Math.sqrt(c*(1.0-Math.sin(lat/k1))/(1.0+Math.sin(lat/k1)));
			for (j = 0; j < Height; j++) {
				for (i = 0; i < Width ; i++) {
					x = (2.0*i-Width)/Height/scale;
					y = (2.0*j-Height)/Height/scale-y2;
					zz = x*x+y*y;
					if (zz==0.0) theta1 = 0.0; else theta1 = -k1*Math.atan2(x,-y);
					if (theta1<-PI || theta1>PI) {
						col[i][j] = BACK;
						if (doshade) shades[i][j] = 255;
					} else {
						theta1 += longi-0.5*PI; /* theta1 is longitude */
						theta2 = k1*Math.asin((zz-c)/(zz+c));
						/* theta2 is latitude */
						if (theta2 > 0.5*PI || theta2 < -0.5*PI) {
							col[i][j] = BACK;
							if (doshade) shades[i][j] = 255;
						} else {
							cos2 = Math.cos(theta2);
							y = Math.sin(theta2);
							if (y < ymin) ymin = y;
							if (y > ymax) ymax = y;
							double alt = planet1(Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
							col[i][j] = alt2color(alt, Math.cos(theta1)*cos2,y,-Math.sin(theta1)*cos2);
							heights[i][j] = (int)alt;
							if (doshade) shades[i][j] = shade;
						}
					}
				}
				tickH(j);
			}
			if (hgrid != 0.0) { /* draw horisontal gridlines */
				for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
				for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
					y = Math.sin(DEG2RAD*theta1);
					if (ymin <= y && y <= ymax) {
						zz = Math.sqrt(c*(1.0+Math.sin(DEG2RAD*theta1/k1))
								/(1.0-Math.sin(DEG2RAD*theta1/k1)));
						for (theta2=-PI+longi; theta2<PI+longi; theta2+=0.5/Width/scale) {
							z1 = theta2-longi;
							x1 = -zz*Math.sin(z1/k1);
							y1 = -zz*Math.cos(z1/k1);
							i = (int) (0.5*(Height*scale*x1+Width));
							j = (int) (0.5*(Height*scale*(y1+y2)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
			if (vgrid != 0.0) { /* draw vertical gridlines */
				for (theta1=-0.5*PI; theta1<0.5*PI; theta1+=0.5/Width/scale) {
					y = Math.sin(theta1);
					if (ymin <= y && y <= ymax) {
						zz = Math.sqrt(c*(1.0+Math.sin(theta1/k1))
								/(1.0-Math.sin(theta1/k1)));
						for (theta2 = 0.0; theta2>-180.0+longi/DEG2RAD; theta2-=vgrid);
						for (theta2 = theta2; theta2<180.0+longi/DEG2RAD; theta2+=vgrid) {
							z1 = DEG2RAD*theta2-longi;
							x1 = -zz*Math.sin(z1/k1);
							y1 = -zz*Math.cos(z1/k1);
							i = (int) (0.5*(Height*scale*x1+Width));
							j = (int) (0.5*(Height*scale*(y1+y2)+Height));
							if (0<=i && i<Width && 0<=j && j<Height) col[i][j] = BLACK;
						}
					}
				}
			}
		}
	}

	public void squarep()
	{
		double y,scale1,theta1,cos2;
		int k,i,j;

		k = (int)(lat*Width*scale/PI);
		for (j = 0; j < Height; j++) {
			y = (2.0*(j-k)-Height)/Width/scale*PI;
			if (Math.abs(y)>=0.5*PI) for (i = 0; i < Width ; i++) {
				col[i][j] = BACK;
				if (doshade) shades[i][j] = 255;
			} else {
				cos2 = Math.cos(y);
				if (cos2>0.0) {
					scale1 = scale*Width/Height/cos2/PI;
					Depth = 3*((int)(log_2(scale1*Height)))+3;
					for (i = 0; i < Width ; i++) {
						theta1 = longi-0.5*PI+PI*(2.0*i-Width)/Width/scale;
						double alt = planet1(Math.cos(theta1)*cos2,Math.sin(y),-Math.sin(theta1)*cos2);
						col[i][j] = alt2color(alt, Math.cos(theta1)*cos2,Math.sin(y),-Math.sin(theta1)*cos2);
						heights[i][j] = (int)alt;
						if (doshade) shades[i][j] = shade;
					}
				}
			}
			tickH(j);
		}
		if (hgrid != 0.0) { /* draw horisontal gridlines */
			for (theta1 = 0.0; theta1>-90.0; theta1-=hgrid);
			for (theta1 = theta1; theta1<90.0; theta1+=hgrid) {
				y = DEG2RAD*theta1;
				j = Height/2+(int)(0.5*y*Width*scale/PI)+k;
				if (j>=0 && j<Height) for (i = 0; i < Width ; i++) col[i][j] = BLACK;
			}
		}
		if (vgrid != 0.0) { /* draw vertical gridlines */
			for (theta1 = 0.0; theta1>-360.0; theta1-=vgrid);
			for (theta1 = theta1; theta1<360.0; theta1+=vgrid) {
				i = (int)(0.5*Width*(1.0+scale*(DEG2RAD*theta1-longi)/PI));
				if (i>=0 && i<Width)
					for (j = max(0,Height/2-(int)(0.25*PI*Width*scale/PI)+k);
					j < min(Height,Height/2+(int)(0.25*PI*Width*scale/PI)+k); j++)
						col[i][j] = BLACK;
			}
		}
	}
		
}
