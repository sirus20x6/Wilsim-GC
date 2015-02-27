import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Scanner;

import javax.swing.JProgressBar;

public class Model implements Runnable {
    private int iterationCount = 0;
    CondVar executeFlag;
    CondVar resetFlag;
    private static final float oneoversqrt2 = 0.70710678118F;
    private static final float sqrt2 = 1.414213562F;
    private static final int stacklimit = 1000000;
    private static final float fillincrement = 0.1F;
    private float time;
    
    // Model grid parameters
    protected static int lattice_size_x = 339;
    protected static int lattice_size_y = 262;

    static final float gridHorizontalSpacingFactor = 720.0f;
    
    private static float oneoverdeltax;
    
    private static float deltax; // Grid spacing in meters
    private static float diag;
    private float K;
    // model parameters
    float last, erosion, C, area, erodeddepth, dist, thresh, capacity, max;
    int printinterval, i, j, ikeep, dir;
    
    protected final static int profileStartX = 330;
    protected final static int profileStartY = 79;
    public Profile river;

    int storageIntervals;
    private int storeCount;

    // Time parameters
    float duration;
    private float timestep;
    private static final float presentTime = 6000.0f;  // 6 MYr from start of simulation
    private float storeTime;

    // Auxillary data arrays
    private static int[] iup;
    private static int[] idown;
    private static int[] jdown;
    private static int[] jup;

    // Data arrays
    private static float[][] area2;
    private static float[][] flow;
    private static float[][] topo;
    private static float[][] topoold;
    private static float[][] topoactual;
    private static int[][] maskhurricane;
    private static int[][] channel;
    private static int[][] masknew;
    private static int[][] draindiri;
    private static int[][] draindirj;
    private static int[][] mask;
    private static int[] stacki;
    private static int[] stackj;
    private static float[][] fac;
    private static float[] timecut;
    private static float[] rim;
    
    private static float[][] wavespeed;
    private static float[][] topodrain;
    
    private static float[][] topoorig;
    private static float[][] slope;
    private static float[][] U;
    

    // Score
    boolean scoreFlag;
    double score;

    // Model parameters
    private float X;
    
    private static int ic;
    private static int jc;
    
    private static int count;
    
    // Data files
    Scanner fp0, fp0b, fp0c, fp1, fp1b, f1;
    float Along_Grant_Wash_Fault = -1.7F;
    float Along_Hurricane_Fault = -.15F;
    float Along_Toroweap_Fault = -.05f;
    
	@Override
	public void run() {
		try {
			modelMain();
		} catch (Exception e) {
			// Replace this with something to go to the log widget
			e.printStackTrace();
		}
	}

	public void toggleExecution() {
		synchronized (executeFlag) {

			if (executeFlag.boolVal) {
				executeFlag.boolVal = false;
				Wilsim.c.startStopButton.setText("Continue");
			} else {
				executeFlag.boolVal = true;
				Wilsim.c.startStopButton.setText("Pause");
				executeFlag.notify();
			}
		}
	}

	static float[] vector(int nh, int nl) {
		return new float[nl + 1];// Math.abs(nh - nl + 1 + NR_END)];
	}

	static int[] ivector(int nl, int nh) {
		return new int[nh + 1];// [Math.abs(nh - nl + 1 + NR_END)];
	}

	static float[][] matrix(int nrl, int nrh, int ncl, int nch) {
		return new float[nrh + 1][nch + 1];// Math.abs(nrh - nrl +
											// 1)][Math.abs(nch - ncl + 1)];
	}

	static int[][] imatrix(int nrl, int nrh, int ncl, int nch) {
		return new int[nrh + 1][nch + 1];// [Math.abs(nrh - nrl +
											// 1)][Math.abs(nch - ncl + 1)];
	}

	static void push(int i, int j) {
		count++;
		stacki[count] = i;
		stackj[count] = j;
	}

	static void pop() {
		ic = stacki[count];
		jc = stackj[count];
		count--;
	}

	private static void fillinpitsandflats(int i, int j) {
		float min;
		count = 0;
		push(i, j);
		while (count > 0) {
			pop();
			if(mask[ic][jc] != 1) 
			    continue;  // 

			min = topo[ic][jc];
			if (topo[iup[ic]][jc] < min)
				min = topo[iup[ic]][jc];
			if (topo[idown[ic]][jc] < min)
				min = topo[idown[ic]][jc];
			if (topo[ic][jup[jc]] < min)
				min = topo[ic][jup[jc]];
			if (topo[ic][jdown[jc]] < min)
				min = topo[ic][jdown[jc]];
			if (topo[iup[ic]][jup[jc]] < min)
				min = topo[iup[ic]][jup[jc]];
			if (topo[idown[ic]][jdown[jc]] < min)
				min = topo[idown[ic]][jdown[jc]];
			if (topo[idown[ic]][jup[jc]] < min)
				min = topo[idown[ic]][jup[jc]];
			if (topo[iup[ic]][jdown[jc]] < min)
				min = topo[iup[ic]][jdown[jc]];
			if ((topo[ic][jc] <= min) && (ic > 1) && (jc > 1)
					&& (ic < lattice_size_x) && (jc < lattice_size_y)
					&& (count < stacklimit) && (topo[ic][jc] > 0)) {
				topo[ic][jc] = min + fillincrement;

				push(iup[ic], jdown[jc]);
				push(idown[ic], jup[jc]);
				push(idown[ic], jdown[jc]);
				push(iup[ic], jup[jc]);
				push(ic, jdown[jc]);
				push(ic, jup[jc]);
				push(idown[ic], jc);
				push(iup[ic], jc);
				push(ic, jc);
			}
		}
	}

	private static void calculatedownhillslope(int i, int j)
	// this routine computes topographic slope in the direction of steepest
	// descent
	{
		float down;
		down = 0;
		draindiri[i][j] = i;
		draindirj[i][j] = j;
		diag = 1;
		if ((topo[iup[i]][j] < topo[i][j])
				&& ((topo[iup[i]][j] - topo[i][j]) < down)) {
			down = topo[iup[i]][j] - topo[i][j];
			draindiri[i][j] = iup[i];
			draindirj[i][j] = j;
			diag = 1;
		}
		if ((topo[idown[i]][j] < topo[i][j])
				&& ((topo[idown[i]][j] - topo[i][j]) < down)) {
			down = topo[idown[i]][j] - topo[i][j];
			draindiri[i][j] = idown[i];
			draindirj[i][j] = j;
			diag = 1;
		}
		if ((topo[i][jup[j]] < topo[i][j])
				&& ((topo[i][jup[j]] - topo[i][j]) < down)) {
			down = topo[i][jup[j]] - topo[i][j];
			draindiri[i][j] = i;
			draindirj[i][j] = jup[j];
			diag = 1;
		}
		if ((topo[i][jdown[j]] < topo[i][j])
				&& ((topo[i][jdown[j]] - topo[i][j]) < down)) {
			down = topo[i][jdown[j]] - topo[i][j];
			draindiri[i][j] = i;
			draindirj[i][j] = jdown[j];
			diag = 1;
		}
		if ((topo[iup[i]][jup[j]] < topo[i][j])
				&& ((topo[iup[i]][jup[j]] - topo[i][j]) * oneoversqrt2 < down)) {
			down = (topo[iup[i]][jup[j]] - topo[i][j]) * oneoversqrt2;
			draindiri[i][j] = iup[i];
			draindirj[i][j] = jup[j];
			diag = sqrt2;
		}
		if ((topo[idown[i]][jup[j]] < topo[i][j])
				&& ((topo[idown[i]][jup[j]] - topo[i][j]) * oneoversqrt2 < down)) {
			down = (topo[idown[i]][jup[j]] - topo[i][j]) * oneoversqrt2;
			draindiri[i][j] = idown[i];
			draindirj[i][j] = jup[j];
			diag = sqrt2;
		}
		if ((topo[iup[i]][jdown[j]] < topo[i][j])
				&& ((topo[iup[i]][jdown[j]] - topo[i][j]) * oneoversqrt2 < down)) {
			down = (topo[iup[i]][jdown[j]] - topo[i][j]) * oneoversqrt2;
			draindiri[i][j] = iup[i];
			draindirj[i][j] = jdown[j];
			diag = sqrt2;
		}
		if ((topo[idown[i]][jdown[j]] < topo[i][j])
				&& ((topo[idown[i]][jdown[j]] - topo[i][j]) * oneoversqrt2 < down)) {
			down = (topo[idown[i]][jdown[j]] - topo[i][j]) * oneoversqrt2;
			draindiri[i][j] = idown[i];
			draindirj[i][j] = jdown[j];
			diag = sqrt2;
		}
		slope[i][j] = -oneoverdeltax * (down);
	}

	private static void calculatedownhillslopeorig(int i, int j)
	// does the same as the above for a different copy of the topographic
	// surface grid
	{
		float down;
		down = 0;
		draindiri[i][j] = i;
		draindirj[i][j] = j;
		diag = 1;
		if (topodrain[iup[i]][j] - topodrain[i][j] < down) {
			down = topodrain[iup[i]][j] - topodrain[i][j];
			draindiri[i][j] = iup[i];
			draindirj[i][j] = j;
			diag = 1;
		}
		if (topodrain[idown[i]][j] - topodrain[i][j] < down) {
			down = topodrain[idown[i]][j] - topodrain[i][j];
			draindiri[i][j] = idown[i];
			draindirj[i][j] = j;
			diag = 1;
		}
		if (topodrain[i][jup[j]] - topodrain[i][j] < down) {
			down = topodrain[i][jup[j]] - topodrain[i][j];
			draindiri[i][j] = i;
			draindirj[i][j] = jup[j];
			diag = 1;
		}
		if (topodrain[i][jdown[j]] - topodrain[i][j] < down) {
			down = topodrain[i][jdown[j]] - topodrain[i][j];
			draindiri[i][j] = i;
			draindirj[i][j] = jdown[j];
			diag = 1;
		}
		if ((topodrain[iup[i]][jup[j]] - topodrain[i][j]) * oneoversqrt2 < down) {
			down = (topodrain[iup[i]][jup[j]] - topodrain[i][j]) * oneoversqrt2;
			draindiri[i][j] = iup[i];
			draindirj[i][j] = jup[j];
			diag = sqrt2;
		}
		if ((topodrain[idown[i]][jup[j]] - topodrain[i][j]) * oneoversqrt2 < down) {
			down = (topodrain[idown[i]][jup[j]] - topodrain[i][j])
					* oneoversqrt2;
			draindiri[i][j] = idown[i];
			draindirj[i][j] = jup[j];
			diag = sqrt2;
		}
		if ((topodrain[iup[i]][jdown[j]] - topodrain[i][j]) * oneoversqrt2 < down) {
			down = (topodrain[iup[i]][jdown[j]] - topodrain[i][j])
					* oneoversqrt2;
			draindiri[i][j] = iup[i];
			draindirj[i][j] = jdown[j];
			diag = sqrt2;
		}
		if ((topodrain[idown[i]][jdown[j]] - topodrain[i][j]) * oneoversqrt2 < down) {
			down = (topodrain[idown[i]][jdown[j]] - topodrain[i][j])
					* oneoversqrt2;
			draindiri[i][j] = idown[i];
			draindirj[i][j] = jdown[j];
			diag = sqrt2;
		}
		slope[i][j] = oneoverdeltax * down;
	}

	static void setupmatrices() {
		int i, j;
		// the "up" and "down" vectors are pointers that point to themselves on
		// the grid boundaries, otherwise they point to their neighbors
		idown = ivector(1, lattice_size_x);
		iup = ivector(1, lattice_size_x);
		jup = ivector(1, lattice_size_y);
		jdown = ivector(1, lattice_size_y);
		for (i = 1; i <= lattice_size_x; i++) {
			idown[i] = i - 1;
			iup[i] = i + 1;
		}
		idown[1] = 1;
		iup[lattice_size_x] = lattice_size_x;
		for (j = 1; j <= lattice_size_y; j++) {
			jdown[j] = j - 1;
			jup[j] = j + 1;
		}
		jdown[1] = 1;
		jup[lattice_size_y] = lattice_size_y;
		topo = matrix(1, lattice_size_x, 1, lattice_size_y);
		topoorig = matrix(1, lattice_size_x, 1, lattice_size_y);
		topoold = matrix(1, lattice_size_x, 1, lattice_size_y);
		topodrain = matrix(1, lattice_size_x, 1, lattice_size_y);
		topoactual = matrix(1, lattice_size_x, 1, lattice_size_y);
		wavespeed = matrix(1, lattice_size_x, 1, lattice_size_y);
		slope = matrix(1, lattice_size_x, 1, lattice_size_y);
		fac = matrix(1, lattice_size_x, 1, lattice_size_y);
		area2 = matrix(1, lattice_size_x, 1, lattice_size_y);
		mask = imatrix(1, lattice_size_x, 1, lattice_size_y);
		masknew = imatrix(1, lattice_size_x, 1, lattice_size_y);
		maskhurricane = imatrix(1, lattice_size_x, 1, lattice_size_y);
		channel = imatrix(1, lattice_size_x, 1, lattice_size_y);
		U = matrix(1, lattice_size_x, 1, lattice_size_y);
		flow = matrix(1, lattice_size_x, 1, lattice_size_y);
		draindiri = imatrix(1, lattice_size_x, 1, lattice_size_y);
		draindirj = imatrix(1, lattice_size_x, 1, lattice_size_y);
		rim = vector(1, lattice_size_x);
		timecut = vector(1, lattice_size_x);
		stacki = ivector(1, stacklimit);
		stackj = ivector(1, stacklimit);
	}

	void modelMain() {

		executeFlag = new CondVar(false);
		resetFlag = new CondVar(true);
		river = new Profile();

		Wilsim.i.log.append("Model: modelMain() \n");

		// Default input files are stored in a .jar file
		// These need to be handled differently from user supplied
		// data files.

		openDefaultDataFiles();

		deltax = gridHorizontalSpacingFactor; // m
		oneoverdeltax = (float) 1.0 / deltax;

		// model duration - time is in kyr, so 6000 kyr is the time since
		// the Colorado River became integrated with Grand Canyon and
		// started downcutting
		duration = Wilsim.c.duration;

		timestep = 1; // this is the initial timestep only - the timestep is
						// automatically reduced if Courant stability
						// criterion is not satisfied
		X = 2.0F; // defines stream power threshold for imposing bedrock
					// channel incision on a pixel, below this threshold we do
					// cliff retreat. calibrated to modern drainage density
					// data from Grand Canyon region
		thresh = 0.25F; // threshold of detachment in the stream power model
		setupmatrices();
		for (i = 1; i <= lattice_size_x; i++) {
			rim[i] = fp0c.nextFloat();
			timecut[i] = 0;

		}

		for (j = 1; j <= lattice_size_y; j++) {
			for (i = 1; i <= lattice_size_x; i++) {

				// fscanf(fp1,"%f",&topo[i][j]);
				topoorig[i][j] = topo[i][j] = fp1.nextFloat();
				
				// fscanf(fp1b,"%f",&topoactual[i][j]);
				topoactual[i][j] = fp1b.nextFloat();
				topodrain[i][j] = topo[i][j];

				// fscanf(fp0b,"%f",&area2[i][j]);
				area2[i][j] = fp0b.nextFloat();
			}
		}

		Wilsim.v.initModel(); // Let the view know that data matrices are ready

		reset();  // Don't set resetFlag to false here.  reset() does some necessary
		// initialization here for viewing,  but data values may be changed before
		// execution is started by user, necessitating another reset() in loop below
		
		// start of time evolution
		while (true) {
			duration = Wilsim.c.duration;
			if (!executeFlag.boolVal) {
				synchronized (executeFlag) {
					try {

						// Thread.yield();
						executeFlag.wait();
					} catch (Exception e) {	}
				}
				continue;
			}

			// starting the execution all over from the beginning
			if (resetFlag.boolVal) {
			    // Wilsim.i.log.append("reset (1): \n");
			    reset();
			    // Reset long profile
			    // Wilsim.i.log.append("reset (2): \n");
			    river.init(storageIntervals, 2 * lattice_size_x);
			    resetFlag.boolVal = false;
			    // Wilsim.i.log.append("reset (3): \n");
			}

			if (time >= duration) {
			    // Save output
			    Wilsim.c.outputReady();
				synchronized (executeFlag) {
					executeFlag.boolVal = false;
				}
				continue;
			}

			// Some timing figures
			if (iterationCount % 10 == 0) {
				String str = String.valueOf(time);
			}

			// make a copy of topo grid
			for (i = 1; i <= lattice_size_x; i++)
			    for (j = 1; j <= lattice_size_y; j++) {
				topoold[i][j] = topo[i][j];
			    }
			// hydrologic correction
			if (time > 3000)
			    for (j = 1; j <= lattice_size_y; j++) {
				for (i = 1; i <= lattice_size_x; i++)
				    fillinpitsandflats(i, j);
			    }
			
			if (time < 3000) {
				// defines subsidence rate of lower Colorado River trough
				// (defined as a triangular domain) of 1.7 m/kyr for 300 kyr
			    for (i = 1; i <= lattice_size_x; i++)
				for (j = 1; j <= lattice_size_y; j++) {
				    if ((j > 81) && (0.4 * j + i < 75) && (time < 300))
					U[i][j] = Along_Grant_Wash_Fault;
				    else
					U[i][j] = 0.0F;
				}
			} else {
				// defines subsidence rate associated with Quaternary
				// faulting along the Hurricane and Toroweap Faults from 3
				// Ma to present - the Gaussian functions cause a realistic
				// decay in faulting North and South of the main zone of
				// slip
			    for (i = 1; i <= lattice_size_x; i++) 
				for (j = 1; j <= lattice_size_y; j++) {


                    //commented per instructions feb 2015
/*				    if ((j > 81) && (0.4 * j + i < 75))
					U[i][j] = -.15f;
				    else
					U[i][j] = 0.0F;*/
				    if (maskhurricane[i][j] == 2)
					//U[i][j] += -0.10 * Math.exp(-(j - 140) * (j - 140) / 900.0) - 0.05 * Math.exp(-(j - 140) * (j - 140) / 900.0);
                    U[i][j] += Along_Hurricane_Fault * Math.exp(-(j-140)*(j-140)/900.0);
				    if (maskhurricane[i][j] == 1)
					U[i][j] += Along_Toroweap_Fault
					    * Math.exp(-(j - 140) * (j - 140) / 900.0);
				}
			}

			// perform subsidence and initialize some arrays
			// performs stream-power model erosion by upwind differencing
			for (i = 1; i <= lattice_size_x; i++)
			    for (j = 1; j <= lattice_size_y; j++) {
				topoold[i][j] += U[i][j] * timestep;
				topo[i][j] += U[i][j] * timestep;
				mask[i][j] = masknew[i][j];
			    }
			// System.out.println("time : " + time + "\n");
			max = 0;
			for (i = 1; i <= lattice_size_x; i++)
			    for (j = 1; j <= lattice_size_y; j++)
				if (mask[i][j] == 1) // mask grid defines areas of
				    // "active" erosion so that model
				    // does not waste time caluclating
				    // erosion on the plateau
				    {
					calculatedownhillslope(i, j);
					area = area2[i][j];
					capacity = area;
					erodeddepth = topoorig[i][j] - topo[i][j];
					if ((maskhurricane[i][j] == 2) && (time > 3000))
					    erodeddepth -= (time - 3000) * U[i][j];
					if ((maskhurricane[i][j] == 1) && (time > 3000))
					    erodeddepth -= (time - 3000) * U[i][j];
					if (erodeddepth > 50) {
					    if (mask[iup[i]][j] == 0)          masknew[iup[i]][j] = 1;
					    if (mask[idown[i]][j] == 0)	       masknew[idown[i]][j] = 1;
					    if (mask[i][jup[j]] == 0)          masknew[i][jup[j]] = 1;
					    if (mask[i][jdown[j]] == 0)	       masknew[i][jdown[j]] = 1;
					    if (mask[iup[i]][jup[j]] == 0)     masknew[iup[i]][jup[j]] = 1;
					    if (mask[iup[i]][jdown[j]] == 0)   masknew[iup[i]][jdown[j]] = 1;
					    if (mask[idown[i]][jup[j]] == 0)   masknew[idown[i]][jup[j]] = 1;
					    if (mask[idown[i]][jdown[j]] == 0) masknew[idown[i]][jdown[j]] = 1;
					    if (timecut[i] < 0.01) timecut[i] = time;
					}
					erodeddepth = rim[i] - topo[i][j];
					if ((maskhurricane[i][j] == 2) && (time > 3000))
					    erodeddepth -= (time - 3000) * U[i][j];
					if ((maskhurricane[i][j] == 1) && (time > 3000))
					    erodeddepth -= (time - 3000) * U[i][j];
					// see GSA paper for a discussion of how the K and C
					// values were calbrated. we will want students to
					// change these values, but we probably want the
					// relative values of different strong/weak layers
					// to remain the same.
					if (erodeddepth < 300)
					    K = Wilsim.c.kstrng;
					else if (erodeddepth < 700)
					    K = Wilsim.c.kstrng * Wilsim.c.kfctor;
					else
					    K = Wilsim.c.kstrng;
						// if (erodeddepth < 200)
						// 	C = (float) (Wilsim.c.cliffRate.getValue()) / 100;
						// else if (erodeddepth < 300)
						// 	C = (float) (Wilsim.c.cliffRate.getValue()) / 100;
						// if (erodeddepth < 600)
						// 	C = (float) (Wilsim.c.cliffRate.getValue()) / 100;
						// else if (erodeddepth < 700)
						// 	C = (float) (Wilsim.c.cliffRate.getValue()) / 100;
						// else
						// 	C = (float) (Wilsim.c.cliffRate.getValue()) / 100;

						if (erodeddepth < 200)
							C = Wilsim.c.cliffRate;
						else if (erodeddepth < 300)
							C = Wilsim.c.cliffRate;
						if (erodeddepth < 600)
							C = Wilsim.c.cliffRate;
						else if (erodeddepth < 700)
							C = Wilsim.c.cliffRate;
						else
							C = Wilsim.c.cliffRate;

						if (capacity > X) {
							channel[i][j] = 1;
							wavespeed[i][j] = (float) (K * Math.sqrt(area));
						} else {
							wavespeed[i][j] = C;
							channel[i][j] = 0;
						}
						if (wavespeed[i][j] > max)
							max = wavespeed[i][j];
					}

			erosion = 0;

			for (i = 1; i <= lattice_size_x; i++)
				for (j = 1; j <= lattice_size_y; j++)
					if (mask[i][j] == 1) {
						if ((channel[i][j] == 1)
								&& (wavespeed[i][j] * slope[i][j] > thresh)) {
							topo[i][j] -= timestep
									* (wavespeed[i][j] * slope[i][j] - thresh);
							erosion += (wavespeed[i][j] * slope[i][j] - thresh);
						} else {
							topo[i][j] -= timestep
									* (wavespeed[i][j] * slope[i][j]);
							erosion += wavespeed[i][j] * slope[i][j];
						}
					}
			
			
			time += timestep;  // KD - Why here?

			// Courant stability criterion is checked here and timestep is
			// reduced if the criterion is not met
			if (max > 0.9 * deltax / timestep) {
				time -= timestep;
				timestep /= 2.0;

				for (i = 1; i <= lattice_size_x; i++)
				    for (j = 1; j <= lattice_size_y; j++)
					topo[i][j] = topoold[i][j] - U[i][j] * timestep;
			} else {
				// fprintf(fp5,"%f %f\n",time,erosion);
				/*
				 * bw_fp5 = new BufferedWriter(fp5); bw_fp5.write(time + " " +
				 * erosion); bw_fp5.newLine();
				 */
				if (max < 0.6 * deltax / timestep)
					timestep *= 1.2;
			}

			if (time >= printinterval) {
				printinterval += 500;

				// output final topography every 500 kyr
				for (j = lattice_size_y; j >= 1; j--)
					for (i = 1; i <= lattice_size_x; i++) {
						// fprintf(fp2a,"%f\n",topo[i][j]);
						//
						// bw_fp2a = new BufferedWriter(fp2a);
						// bw_fp2a.write(String.valueOf(topo[i][j]));
						// bw_fp2a.newLine();
						//
					}

				for (j = lattice_size_y; j >= 1; j--)
					for (i = 1; i <= lattice_size_x; i++)
						calculatedownhillslopeorig(i, j);
				// computes and plots long profiles for Colorado River every
				// 500 kyr
				/*  KD Not needed every iteration ?   12 May 2014 
				i = 330;
				j = 79;
				dist = 0.0f;
				last = topo[i][j];
				while (i > 1) {
					// KD Output printint not yet implemented
					// fprintf(fp3, "%f %f\n", dist, topo[i][j]);
					calculatedownhillslopeorig(i, j);
					dist += deltax * diag;
					ikeep = i;
					last = topo[i][j];
					i = draindiri[i][j];
					j = draindirj[ikeep][j];
				}
				*/
			}
			for (j = lattice_size_y; j >= 1; j--) {
				for (i = 1; i <= lattice_size_x; i++)
					calculatedownhillslopeorig(i, j);
			}


			// computes and prints final long profiles for Colorado River
			// // Move to storage tick block - 14 May 2014 KD
			// i = 330;
			// j = 79;
			// dist = 0.0f;
			// last = topo[i][j];
			// while (i > 1) {
			// 	// print not yet implemented
			// 	calculatedownhillslopeorig(i, j);
			// 	dist += deltax * diag;
			// 	ikeep = i;
			// 	last = topo[i][j];
			// 	i = draindiri[i][j];
			// 	j = draindirj[ikeep][j];
			// }
			// // Wilsim.i.log.append("storeTime: " + storeTime + "\n");

			if(time >= storeTime)
			    {
				Wilsim.i.log.append("Storing XSections\n");
				for(i = 0; i < XSectionManager.nXSections(); i++)
				    XSectionManager.getXSection(i).appendXSectionValues(topo);

				// computes and prints final long profiles for Colorado River
				i = profileStartX;
				j = profileStartY;
				int count = 0;
				dist = 0.0f;
				last = topo[i][j];
				while (i > 1) {
				    river.distances[storeCount][count] = dist;
				    river.values[storeCount][count] = last;
				    count++;

				    calculatedownhillslopeorig(i, j);
				    dist += deltax * diag;
				    ikeep = i;
				    last = topo[i][j];
				    i = draindiri[i][j];
				    j = draindirj[ikeep][j];
				}
				river.n[storeCount] = count;

				// Wilsim.i.log.append("storeTime: " + storeTime + "\n");

				storeCount++;  // Storage completed
				storeTime = ((float)(storeCount+1) / storageIntervals) * duration;
			    }

			Wilsim.c.progressBar.setMaximum((int) duration - 1);
			Wilsim.c.progressBar.setValue((int) time);
			if (time < 1000)
				Wilsim.c.progressBar.setString("6M Years ago");
			else if (time < 2000)
				Wilsim.c.progressBar.setString("5M Years ago");
			else if (time < 3000)
				Wilsim.c.progressBar.setString("4M Years ago");
			else if (time < 4000)
				Wilsim.c.progressBar.setString("3M Years ago");
			else if (time < 5000)
				Wilsim.c.progressBar.setString("2M Years ago");
			else if (time < 6000)
				Wilsim.c.progressBar.setString("1M Years ago");
			else if (time < 7000)
				Wilsim.c.progressBar.setString("Present");
			else if (time < 8000)
				Wilsim.c.progressBar.setString("1M Years in future");
			else if (time < 9000)
				Wilsim.c.progressBar.setString("2M Years in future");
			else if (time < 10000)
				Wilsim.c.progressBar.setString("3M Years in future");
			else if (time < 11000)
				Wilsim.c.progressBar.setString("4M Years in future");
			else if (time < 12000)
				Wilsim.c.progressBar.setString("5M Years in future");
			else // (time < 13000)
				Wilsim.c.progressBar.setString("6M Years in future");

			Wilsim.c.progressBar.setIndeterminate(false);

			// Check score
			if(!scoreFlag && time >= presentTime)
			    {
				// Compute score
				score = 0.0f;
				double value;
				int score_count;
				score_count = 0;
				// Stay away from borders for now
				for (i = 2; i < lattice_size_x-1; i++)
				    for (j = 2; j < lattice_size_y-1; j++){
					if (!((j > 81) && (0.4 * j + i < 75)))
					    {
						value = topo[i][j] - topoactual[i][j];
						score += value * value;
						score_count++;
					    }
				    }
				Wilsim.i.log.append("score_count: " + score_count + "\n");
				score /= score_count;
				score = Math.sqrt(score);
				scoreFlag = true;
			    }

			Wilsim.v.loadModel(topo);  // Notify view of data
			iterationCount++;
			Thread.yield();
			pause();
			// Wilsim.i.log.append(time + " \n");
		} // main simulation loop

		// print final topography
	}

	void openDefaultDataFiles() {
		Wilsim.i.log.append("Model: openDefaultDataFiles() \n");

		// Default input files are stored in a .jar file
		// These need to be handled differently from user supplied
		// data files.

		fp0 = new Scanner(getClass().getResourceAsStream(
				"input_files/grandcanyonfaultmask.txt"));

		Wilsim.i.log.append("Model: opened grandcanyonfaultmask.txt\n");

		fp0b = new Scanner(getClass().getResourceAsStream(
				"input_files/grandcanyonarea.txt"));

		Wilsim.i.log.append("Model: opened grandcanyonarea.txt\n");

		fp0c = new Scanner(getClass().getResourceAsStream(
				"input_files/grandcanyonrim.txt"));

		Wilsim.i.log.append("Model: opened grandcanyonrim.txt\n");

		fp1 = new Scanner(getClass().getResourceAsStream(
				"input_files/grandcanyoninitialtopo.txt"));

		Wilsim.i.log.append("Model: opened grandcanyoninitialtopo.txt\n");

		fp1b = new Scanner(getClass().getResourceAsStream(
				"input_files/grandcanyonactualtopo.txt"));

		Wilsim.i.log.append("Model: opened grandcanyonactualtopo.txt\n");

		f1 = new Scanner(getClass().getResourceAsStream("input_files/text.txt"));
		Wilsim.i.log.append("Model: opened text.txt \n");

	}

	protected void resetCall() {

		synchronized (resetFlag) {
			try {

				resetFlag.boolVal = true;
				if (executeFlag.boolVal)
					Wilsim.c.startStopButton.setText("Pause");
				else
					Wilsim.c.startStopButton.setText("Start");
			} catch (Exception e) {
			}

		}
	}

	void reset() {
		time = 0;
		scoreFlag = false;

		for (j = 1; j <= lattice_size_y; j++) {
			for (i = 1; i <= lattice_size_x; i++) {
				U[i][j] = 0;
				channel[i][j] = 0;
				topo[i][j] = topoorig[i][j];

				if ((j > 81) && (0.4 * j + i < 75))
					mask[i][j] = 2;
				else
					mask[i][j] = 0; // defines the Lower Colorado trough as
									// a triangular block - this could be
									// made prettier with an input mask
				if (area2[i][j] > 300000)
					mask[i][j] = 1; // this mask grid is used to speed up
									// the code by computing erosion only in
									// areas incised into the plateau or
									// adjacent to areas incised into the
									// plateau. this approach may break down
									// for some values of K and C (erosion
									// coefficients) but works fine for the
									// values used here
				masknew[i][j] = mask[i][j];
				wavespeed[i][j] = 0;
			}
		}

		Wilsim.v.loadModel(topo);

		printinterval = 1000;
		max = 0;
		Wilsim.c.progressBar.setString("6M Years ago");

		// Reset XSections
		XSectionManager.clear(storageIntervals);
		storeCount = 0;
		storeTime = 1.0f / storageIntervals * duration;
	}

	public void readFile() {

	}

	public void pause() {
		if (Wilsim.c.pauseValue > 0) {

			float i = Wilsim.c.pauseValue * 1000;
			if (time > i && time < i + 2) {
				toggleExecution();
			}

		}

	}
}
