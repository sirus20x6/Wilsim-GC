public class XSection
{
    // Stores data values of a cross section line through a terrain
    // over multiple iterations.  Iteration 0 is interpreted as cumulative distance
    // along the line.  

    // Line endpoints
    int startX, startY, endX, endY;

    private int maxNIterates, nIterates, nValues;
    float [][] values;

    XSection()
    {
	maxNIterates = 0; nIterates = 0;
    }

    void clear()
    {
	Wilsim.i.log.append("XSection::clear()\n");
	nIterates = 0;
	maxNIterates = 0;
	values = null;
    }

    void init(int nI)
    {
	Wilsim.i.log.append("XSection::init(" + String.valueOf(nI) + ")\n");
	maxNIterates = nI;
	nIterates = 0;
	
	values = new float [nI+1][];

	computenValues();
	computeDistances();
    }

    int getNIterates() { return nIterates; }

    private void computenValues()
    {
	// Bresenham routine

	int dx, dy;

	if(startX < endX)
	    dx = endX - startX;
	else
	    dx = startX - endX;

	if(startY < endY)
	    dy = endY - startY;
	else
	    dy = startY - endY;

	if(dx > dy)
	    nValues = dx + 1;   // include endpoints
	else
	    nValues = dy + 1;
    }
    
    private void computeDistances()
    {
	values[0] = new float[nValues];

	float distance;

	// Do a Bresenham-like line stepping

	int dx, dy, sx, sy, stepX, stepY;
	int p;

	sx = endX - startX;
	sy = endY - startY;

	if(sx < 0) dx = -sx; else dx = sx;  // dx = abs(sx)
	if(sy < 0) dy = -sy; else dy = sy;

	if(sx < 0) stepX = -1; else stepX = 1;
	if(sy < 0) stepY = -1; else stepY = 1;

	distance = (float) Math.sqrt(dx * dx + dy * dy);

	int x, y, i;

	x = startX; y = startY;

	if(dx > dy)
	    {
		// Conditional step in y
		p = 2 * dy - dx;
		for(i = 0; i < dx; i++)
		    {
			values[0][i] = ((float)i) / dx * distance
			    * Model.gridHorizontalSpacingFactor;
			x += stepX;
			if(p < 0)
			    { p += 2 * dy; }
			else
			    { p += 2 * (dy - dx); y += stepY; }
		    }
		values[0][i] = distance * Model.gridHorizontalSpacingFactor;
	    }
	else
	    {
		// Conditional step in x
		p = 2 * dx - dy;
		for(i = 0; i < dy; i++)
		    {
			values[0][i] = ((float)i) / dy * distance
			    * Model.gridHorizontalSpacingFactor;
			y += stepY;
			if(p < 0)
			    { p += 2 * dx; }
			else
			    { p += 2 * (dx - dy); x += stepX; }
		    }
		values[0][i] = distance * Model.gridHorizontalSpacingFactor;
	    }

	if(nIterates == 0)
	    nIterates = 1;

    } // computeDistances

    void appendXSectionValues(UnsafeMemory terrain)
    {
	Wilsim.i.log.append("Xsection::appendXSectionValues()\n");
	if(nIterates > maxNIterates) return;
	
	values[nIterates] = new float[nValues];

	// Do a Bresenham-like line stepping

	int dx, dy, sx, sy, stepX, stepY;
	int p;

	sx = endX - startX;
	sy = endY - startY;

	if(sx < 0) dx = -sx; else dx = sx;  // dx = abs(sx)
	if(sy < 0) dy = -sy; else dy = sy;

	if(sx < 0) stepX = -1; else stepX = 1;
	if(sy < 0) stepY = -1; else stepY = 1;

	int x, y, i;

	x = startX; y = startY;

	if(dx > dy)
	    {
		// Conditional step in y
		p = 2 * dy - dx;
		for(i = 0; i < dx; i++)
		    {

			values[nIterates][i] = terrain.getFloatAt(x * y + y);
			x += stepX;
			if(p < 0)
			    { p += 2 * dy; }
			else
			    { p += 2 * (dy - dx); y += stepY; }
		    }
		values[nIterates][i] = terrain.getFloatAt(x * y + y);
	    }
	else
	    {
		// Conditional step in x
		p = 2 * dx - dy;
		for(i = 0; i < dy; i++)
		    {
			values[nIterates][i] = terrain.getFloatAt(x * y + y);
			y += stepY;
			if(p < 0)
			    { p += 2 * dx; }
			else
			    { p += 2 * (dx - dy); x += stepX; }
		    }
		values[nIterates][i] = terrain.getFloatAt(x * y + y);
	    }

	nIterates++;
    }
}