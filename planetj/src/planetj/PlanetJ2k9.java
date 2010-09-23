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
/* Copyright 1988--2009 Torben AE. Mogensen */

/* version of January 2009 */

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
/* I have included procedures to print the maps as bmp (Windows) or */
/* ppm(portable pixel map) bitmaps  on standard output or specified files. */

/* I have tried to avoid using machine specific features, so it should */
/* be easy to port the program to any machine. Beware, though that due */
/* to different precision on different machines, the same seed numbers */
/* can yield very different planets. */

/* The primitive user interface is primarily a result of portability concerns */


public class PlanetJ2k9 extends PlanetJ
{

	public static double fmin(double x, double y)
	{ 
		return(x<y ? x : y); 
	}

	public static double fmax(double x, double y)
	{ 
		return(x<y ? y : x); 
	}
	
	public static final int BLACK = 0;
	public static final int WHITE = 1;
	public static final int BACK = 2;
	public static final int GRID = 3;
	public static final int OUTLINE1 = 4;
	public static final int OUTLINE2 = 5;
	public static final int LOWEST = 6;
	public static final int SEA = 7;
	public static final int LAND = 8;
	public static final int HIGHEST = 9;

	public int rtable[] = new int[65536], gtable[] = new int[65536], btable[] = new int[65536];

	public int contourstep = 0;

	public double shade_angle2 = 20.0;
	
	public static final double POW = 0.47;
	
	public int doshade = 0;
	
	public void init(Properties prop)
	{
		super.init(prop);

		dd1 = Double.parseDouble(prop.getProperty("-v", prop.getProperty("altitude-weight",  "0.45")));
		
		dd2 = Double.parseDouble(prop.getProperty("-V", prop.getProperty("distance-weight",  "0.035")));
		
		altColors=false;
		
		if(Boolean.parseBoolean(prop.getProperty("-E", prop.getProperty("edge", "false")).trim()))
		{
			do_outline = true;
			if(!prop.getProperty("-E", prop.getProperty("edge", "false")).trim().equalsIgnoreCase("true"))
			{
				contourstep = Integer.parseInt(prop.getProperty("-E", prop.getProperty("edge", "false")).trim());
			}
		}

		if(Boolean.parseBoolean(prop.getProperty("-O", prop.getProperty("outline", "false")).trim()))
		{
			do_outline = true;
			do_bw = true;
			if(!prop.getProperty("-O", prop.getProperty("outline", "false")).trim().equalsIgnoreCase("true"))
			{
				contourstep = Integer.parseInt(prop.getProperty("-O", prop.getProperty("outline", "false")).trim());
			}
		}

		nocols = Integer.parseInt(prop.getProperty("-N", prop.getProperty("color-number", "65536")));
		
		if (nocols<9) nocols = 9;
		if (nocols>65536) nocols = 65536;
		
		shade_angle = Double.parseDouble(prop.getProperty("-a", prop.getProperty("shade-angle", "150.0")));
		
		shade_angle2 = Double.parseDouble(prop.getProperty("-A", prop.getProperty("shade-angle2", "20.0")));

		doshade = Boolean.parseBoolean(prop.getProperty("-B", prop.getProperty("shade-bumpmap", "false"))) ? 1 : doshade;
		doshade = Boolean.parseBoolean(prop.getProperty("-b", prop.getProperty("shade-bumpmap-land", "false"))) ? 2 : doshade;
		doshade = Boolean.parseBoolean(prop.getProperty("-d", prop.getProperty("shade-daylight", "false"))) ? 3 : doshade;
	}
	
	
	void makeoutline(boolean doBw)
	{
		int i,j,k,t;

		int[] outx = new int[Width*Height];
		int[] outy = new int[Width*Height];

		k=0;
		for (i=1; i<Width-1; i++)
			for (j=1; j<Height-1; j++)
				if ((col[i][j] >= LOWEST && col[i][j] <= SEA) &&
						(col[i-1][j] >= LAND || col[i+1][j] >= LAND ||
								col[i][j-1] >= LAND || col[i][j+1] >= LAND ||
								col[i-1][j-1] >= LAND || col[i-1][j+1] >= LAND ||
								col[i+1][j-1] >= LAND || col[i+1][j+1] >= LAND)) {
					/* if point is sea and any neighbour is not, add to outline */
					outx[k] = i; outy[k++] = j;
				}

		if (contourstep>0) {

			for (i=1; i<Width-1; i++)
				for (j=1; j<Height-1; j++) {
					t = (col[i][j] - LAND) / contourstep;
					if (t>=0 &&
							((col[i-1][j]-LAND) / contourstep > t ||
									(col[i+1][j]-LAND) / contourstep > t ||
									(col[i][j-1]-LAND) / contourstep > t ||
									(col[i][j+1]-LAND) / contourstep > t)) {
						/* if point is at countour line and any neighbour is higher */
						outx[k] = i; outy[k++] = j;
					}
				}
		}
		if (do_bw) /* if outline only, clear colours */
			for (i=0; i<Width; i++)
				for (j=0; j<Height; j++) {
					if (col[i][j] >= LOWEST)
						col[i][j] = WHITE;
					else col[i][j] = BLACK;
				}
		/* draw outline (in black if outline only) */
		while (k-->0) {
			if (do_bw) t = BLACK;
			else if (contourstep == 0 || col[outx[k]][outy[k]]<LAND ||
					((col[outx[k]][outy[k]]-LAND)/contourstep)%2 == 1)
				t = OUTLINE1;
			else t = OUTLINE2;
			col[outx[k]][outy[k]] = t;
		}
	
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

		/* calculate colour */
		if (alt <=0.) { /* if below sea level then */
			if (latic && y*y*y*y+alt >= 0.98)
				colour = HIGHEST;	 /* icecap if close to poles */
			else {
				colour = SEA+(int)((SEA-LOWEST+1)*(10*alt));
				if (colour<LOWEST) colour = LOWEST;
			}
		}
		else {
			if (latic) alt += 0.1*y*y*y*y;  /* altitude adjusted with latitude */
			if (alt >= 0.1) /* if high then */
				colour = HIGHEST;
			else {
				colour = LAND+(int)((HIGHEST-LAND+1)*(10*alt));
				if (colour>HIGHEST) colour = HIGHEST;
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
				-Math.sqrt(3.0)-0.20, -Math.sqrt(3.0)-0.22, -Math.sqrt(3.0)-0.23,
				-Math.sqrt(3.0)-0.19,  Math.sqrt(3.0)+0.18,  Math.sqrt(3.0)+0.17,
				Math.sqrt(3.0)+0.21, -Math.sqrt(3.0)-0.24,  Math.sqrt(3.0)+0.15,
				Math.sqrt(3.0)+0.24,  Math.sqrt(3.0)+0.22, -Math.sqrt(3.0)-0.25,
				/* coordinates of vertices of tetrahedron*/
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
		double ex, ey, ez, e, es, es1, es2, es3;
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

			/* reorder vertices so ab is longest edge */
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
							else { /* ab is longest, so cut ab */
								es = rand2(as,bs);
								es1 = rand2(es,es);
								es2 = 0.5+0.1*rand2(es1,es1);
								es3 = 1.0-es2;
								if (ax<bx) {
									ex = es2*ax+es3*bx; ey = es2*ay+es3*by; ez = es2*az+es3*bz;
								} else if (ax>bx) {
									ex = es3*ax+es2*bx; ey = es3*ay+es2*by; ez = es3*az+es2*bz;
								} else { /* ax==bx, very unlikely to ever happen */
									ex = 0.5*ax+0.5*bx; ey = 0.5*ay+0.5*by; ez = 0.5*az+0.5*bz;
								}
								if (lab>1.0) lab = Math.pow(lab,0.5);
								/* decrease contribution for very long distances */

								/* new altitude is: */
								e = 0.5*(a+b) /* average of end points */
								+ es*dd1*Math.abs(a-b) /* plus contribution for altitude diff */
								+ es1*dd2*Math.pow(lab,POW); /* plus contribution for distance */
								eax = ax-ex; eay = ay-ey; eaz = az-ez;
								epx =  x-ex; epy =  y-ey; epz =  z-ez;
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
			if (doshade==1 || doshade==2) {
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
				if (doshade==2 && (a+b+c+d)<0.0) shade = 150;
			}
			else if (doshade==3) {
				if ((a+b+c+d)<0.0) {
					x1 = x; y1 = y; z1 = z;
				} else {
					l1 = 50.0/
					Math.sqrt((ax-bx)*(ax-bx)+(ay-by)*(ay-by)+(az-bz)*(az-bz)+
							(ax-cx)*(ax-cx)+(ay-cy)*(ay-cy)+(az-cz)*(az-cz)+
							(ax-dx)*(ax-dx)+(ay-dy)*(ay-dy)+(az-dz)*(az-dz)+
							(bx-cx)*(bx-cx)+(by-cy)*(by-cy)+(bz-cz)*(bz-cz)+
							(bx-dx)*(bx-dx)+(by-dy)*(by-dy)+(bz-dz)*(bz-dz)+
							(cx-dx)*(cx-dx)+(cy-dy)*(cy-dy)+(cz-dz)*(cz-dz));
					x1 = 0.25*(ax+bx+cx+dx);
					x1 = l1*(a*(x1-ax)+b*(x1-bx)+c*(x1-cx)+d*(x1-dx)) + x;
					y1 = 0.25*(ay+by+cy+dy);
					y1 = l1*(a*(y1-ay)+b*(y1-by)+c*(y1-cy)+d*(y1-dy)) + y;
					z1 = 0.25*(az+bz+cz+dz);
					z1 = l1*(a*(z1-az)+b*(z1-bz)+c*(z1-cz)+d*(z1-dz)) + z;
				}
				l1 = Math.sqrt(x1*x1+y1*y1+z1*z1);
				if (l1==0.0) l1 = 1.0;
				x2 = Math.cos(PI*shade_angle/180.0-0.5*PI)*Math.cos(PI*shade_angle2/180.0);
				y2 = -Math.sin(PI*shade_angle2/180.0);
				z2 = -Math.sin(PI*shade_angle/180.0-0.5*PI)*Math.cos(PI*shade_angle2/180.0);
				shade = (int)((x1*x2+y1*y2+z1*z2)/l1*300.0+10);
				if (shade<10) shade = 10;
				if (shade>255) shade = 255;
			}
			return((a+b+c+d)/4);
		}
	}



}
