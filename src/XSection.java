public class XSection {
	// Stores data values of a cross section line through a terrain
	// over multiple iterations.  Iteration 0 is interpreted as cumulative distance
	// along the line.

	// Line endpoints
	int startX, startY, endX, endY;
	private int dMaxValues = -1;
	private int dMinValues = -1;
	private float crossSectionMaxX;
	private float crossSectionMinX;
	private float crossSectionMaxY;
	private float crossSectionMinY;

	public int getMaxNIterates() {
		return maxNIterates;
	}

	private int maxNIterates, nIterates, nValues;
	float[][] values;

	XSection() {
		maxNIterates = 0;
		nIterates = 0;
	}

	void clear() {
		Wilsim.i.log.append("XSection::clear()\n");
		nIterates = 0;
		maxNIterates = 0;
		values = null;
		startX = 0;
		startY = 0;
		endX = 0;
		endY = 0;
	}

	void init(int nI) {
		Wilsim.i.log.append("XSection::init(" + String.valueOf(nI) + ")\n");
		maxNIterates = nI;
		nIterates = 0;
/*		startX = 0;
		startY = 0;
		endX = 0;
		endY = 0;*/

		values = new float[nI + 1][];

		computenValues();
		computeDistances();
	}

	int getNIterates() {
		return nIterates;
	}

	private void computenValues() {
		// Bresenham routine

		int dx, dy;

		if (startX < endX)
			dx = endX - startX;
		else
			dx = startX - endX;

		if (startY < endY)
			dy = endY - startY;
		else
			dy = startY - endY;

		if (dx > dy)
			nValues = dx + 1;   // include endpoints
		else
			nValues = dy + 1;
	}

	private void computeDistances() {
		values[0] = new float[nValues];

		float distance;

		// Do a Bresenham-like line stepping

		int dx, dy, sx, sy, stepX, stepY, dMax, dMin;
		int p;

		sx = endX - startX;
		sy = endY - startY;

		dx = Math.abs(sx);
		dy = Math.abs(sy);

		if (sx < 0) stepX = -1;
		else stepX = 1;
		if (sy < 0) stepY = -1;
		else stepY = 1;

		distance = (float) Math.sqrt(dx * dx + dy * dy);

		int x, y, i;

		x = startX;
		y = startY;

		if (dx > dy) {
			dMax = dx;
			dMin = dy;
		} else {
			dMax = dy;
			dMin = dx;
		}
		// Conditional step in y
		p = 2 * dMin - dMax;
		for (i = 0; i < dMax; i++) {
			values[0][i] = ((float) i) / dMax * distance
					* Model.gridHorizontalSpacingFactor;
			x += stepX;
			if (p < 0) {
				p += 2 * dMin;
			} else {
				p += 2 * (dMin - dMax);
				y += stepY;
			}

			if (values[0][i] > crossSectionMaxX) {
				crossSectionMaxX = values[0][i];
			}
			if (values[0][i] < crossSectionMinX) {
				crossSectionMinX = values[0][i];
			}


		}
		values[0][i] = distance * Model.gridHorizontalSpacingFactor;

	} // computeDistances*/

	void appendXSectionValues(float[][] terrain) {
		Wilsim.i.log.append("Xsection::appendXSectionValues()\n");
		if (nIterates + 1 > maxNIterates) return;

		values[nIterates + 1] = new float[nValues];

		// Do a Bresenham-like line stepping

		int dx, dy, sx, sy, dMax, dMin;
		double stepX, stepY;
		sx = endX - startX;
		sy = endY - startY;

		dx = Math.abs(sx);
		dy = Math.abs(sy);

		if (sx < 0) stepX = -1;
		else stepX = 1;
		if (sy < 0) stepY = -1;
		else stepY = 1;

		double x, y;

		x = startX;
		y = startY;
		//if (startX > 339) startX = 339; // bounds check

		if (dx > dy) {
			dMax = dx;
			dMin = dy;
			stepY = (double) dMin / dMax * stepY;
		} else {
			dMax = dy;
			dMin = dx;
			stepX =  (double) dMin / dMax * stepX;
		}
		//if (startY + dMax > 262) startY = 262; //Bounds check
		dMaxValues = dMax;
		dMinValues = dMin;
		// Conditional step in y

		{
			int i = 0; // block scope for whatever reason the command after the for loop is.
			for (i = 0; i < dMax; i++) {
				//System.out.println("X is " + x + " Y is " + y);
				//System.out.println("p is " + p );
				values[nIterates + 1][i] = terrain[Math.round( (float) x)][Math.round( (float) y)];

				if (values[0][i] > crossSectionMaxY) {
					crossSectionMaxY = values[0][i];
				}
				if (values[0][i] < crossSectionMinY) {
					crossSectionMinY = values[0][i];
				}
/*				System.out.println("X is " + x);
				System.out.println("Y is " + y);*/

				x += stepX;
				y += stepY;

			}
			//System.out.println("X is " + x + " Y is " + y);
			values[nIterates + 1][i] = terrain[Math.round( (float) x)][Math.round( (float) y)];
		}

		nIterates++;
	}
	void appendXSectionValues1d(float[] terrain) {
		Wilsim.i.log.append("Xsection::appendXSectionValues()\n");
		if (nIterates > maxNIterates) return;

		values[nIterates + 1] = new float[nValues];

		// Do a Bresenham-like line stepping

		int dx, dy, sx, sy, dMax, dMin;
		double stepX, stepY;
		sx = endX - startX;
		sy = endY - startY;

		dx = Math.abs(sx);
		dy = Math.abs(sy);

		if (sx < 0) stepX = -1;
		else stepX = 1;
		if (sy < 0) stepY = -1;
		else stepY = 1;

		double x, y;

		x = startX;
		y = startY;
		//if (startX > 339) startX = 339; // bounds check

		if (dx > dy) {
			dMax = dx;
			dMin = dy;
			stepY = (double) dMin / dMax * stepY;
		} else {
			dMax = dy;
			dMin = dx;
			stepX =  (double) dMin / dMax * stepX;
		}
		//if (startY + dMax > 262) startY = 262; //Bounds check
		dMaxValues = dMax;
		dMinValues = dMin;
		// Conditional step in y

		{
			int i = 0; // block scope for whatever reason the command after the for loop is.
			for (i = 0; i < dMax; i++) {
				//System.out.println("X is " + x + " Y is " + y);
				//System.out.println("p is " + p );
				values[nIterates + 1][i] = terrain[Math.round( (float) x) + Math.round( (float) y) * 339];

				if (values[0][i] > crossSectionMaxY) {
					crossSectionMaxY = values[0][i];
				}
				if (values[0][i] < crossSectionMinY) {
					crossSectionMinY = values[0][i];
				}
/*				System.out.println("X is " + x);
				System.out.println("Y is " + y);*/

				x += stepX;
				y += stepY;

			}
			//System.out.println("X is " + x + " Y is " + y);
			values[nIterates + 1][i] = terrain[Math.round( (float) x) + Math.round( (float) y) * 339];
		}

		nIterates++;
	}
}
