import java.awt.*;
import java.util.BitSet;
import java.util.Scanner;

public class Model implements Runnable {
    private int iterationCount = 0;
    private CondVar executeFlag;
    private CondVar resetFlag;
    private static CondVar[] needWork = new CondVar[Runtime.getRuntime().availableProcessors()];
    private static final float oneoversqrt2 = 0.70710678118F;
    private static final float sqrt2 = 1.414213562F;
    private static final int stacklimit = 1000000;
    private static final float fillincrement = 0.1F;
    private static float time;

    // Model grid parameters
    static final short lattice_size_x = 678;
    static final short lattice_size_y = 524;
    static final float gridHorizontalSpacingFactor = 360.0f;
    private static final float oneoverdeltax = (float) 1.0 / gridHorizontalSpacingFactor;
    private static float[] diag = new float[Runtime.getRuntime().availableProcessors()];
    private static float max;
    private int i;
    private int j;
    private final static short profileStartX = 660;
    private final static short profileStartY = 158;
    public static final int oneDimSize = (lattice_size_x + 1) * (lattice_size_y + 1);
    public Profile river;

    int visualizationIntervals;
    private int storeCount;

    // Time parameters
    private float duration;
    private static final float presentTime = 6000.0f;  // 6 MYr from start of simulation
    private float storeTime;

    // Data arrays
    private static float[] area21d;

    public static final float[] topo1dim = new float [oneDimSize];
    public static float[][] topoSave;
    public static Vec3[] vert_color2;
    public static Vec3[] vert_Normals;
    private static final float[] topoactual1d = new float [oneDimSize];
    private static byte[] channel1d = new byte [oneDimSize];
    private static final int[][] draindiri = new int[lattice_size_x + 1][lattice_size_y + 1];
    private static final int[][] draindirj = new int[lattice_size_x + 1][lattice_size_y + 1];

    //private static final int[] draindir = new int [oneDimSize];
    //private static final byte[] mask = new byte [oneDimSize];;
    private static BitSet mask = new BitSet(oneDimSize);
    private static int[] stacki;
    private static int[] stackj;
    private static float[] timecut;
    private static float[] rim;

    private static float[] wavespeed1d = new float[oneDimSize];
    private static float[][] topodrain;
    private static float[][] maskhurricane;
    private static float[] topodrain1d = new float[oneDimSize];

    private static float[] topoorig1d = new float [oneDimSize];
    private static final float[] slope1d = new float[oneDimSize];
    private static float[] U1d = new float [oneDimSize];


    // Score
    boolean scoreFlag;
    double score;

    private static int ic[] = new int[Runtime.getRuntime().availableProcessors()];
    private static int jc[] = new int[Runtime.getRuntime().availableProcessors()];

    private static int count;

    private Scanner fp0;
    private Scanner fp0b;
    private Scanner fp0c;
    private Scanner fp1;
    private Scanner fp1b;
    static float Along_Grant_Wash_Fault = -1.7F;
    static float timestep = 1;
    static float thresh = 0.25F;
    static float deltax = gridHorizontalSpacingFactor;

    public static Thread[] threads;

    public static void createThreads(){
        final int cores = Runtime.getRuntime().availableProcessors();
        threads = new Thread[cores];
        for (int thread = 0; thread < threads.length; thread++) {
            final int threadID = thread;
            threads[thread] = new Thread(new Runnable() {


                public void run() {
                    int[] end = new int[cores];
                   end[cores -1 ] = lattice_size_x;
                    if (!(threadID == cores - 1)){
                        end[threadID] = (threadID * (lattice_size_x / cores) + (lattice_size_x / cores)) + 1;
                    }



                    // start of time evolution
                    while (true) {
                        while (!needWork[threadID].boolVal) {
                            //System.out.println("thread " + threadID + " " + needWork[threadID].boolVal);
                            synchronized (needWork[threadID]) {
                                try {
                                    needWork[threadID].wait();
                                } catch (Exception e) {
                                }
                            }
                            continue;
                        }
                        //System.out.println("thread " + threadID + " has started");
                        //System.out.println("I am thread " + threadID + " and I am doing i " + (threadID * (lattice_size_x / cores) + 1) + " Through " + (threadID * (lattice_size_x / cores) + (lattice_size_x / cores)));
 /*                       if (time > 3000) {
                            for (int i = (threadID * (lattice_size_x / cores) + 1); i < end; i++)   {
                                for (int j = 1; j < lattice_size_y - 1; j++) {
                                    float min;
                                    count = 0;
                                    if (mask.get(i + j * lattice_size_x) && (ic[threadID] > 1) && (jc[threadID] > 1)){
                                        count++;
                                        stacki[(ic[threadID])] = i;
                                        stackj[(jc[threadID])] = j;
                                        while (count > 0) {
                                            //pop();

                                            ic[threadID] = stacki[count];
                                            jc[threadID] = stackj[count];
                                            count--;



                                            min = topo1dim[ic[threadID] + jc[threadID] * lattice_size_x];
                                            if (topo1dim[(ic[threadID] + 1) + jc[threadID] * lattice_size_x] < min)
                                                min = topo1dim[(ic[threadID] + 1) + jc[threadID] * lattice_size_x];
                                            if (topo1dim[(ic[threadID] - 1) + jc[threadID] * lattice_size_x] < min)
                                                min = topo1dim[(ic[threadID] - 1) + jc[threadID] * lattice_size_x];
                                            if (topo1dim[ic[threadID] + (jc[threadID] + 1) * lattice_size_x] < min)
                                                min = topo1dim[ic[threadID] + (jc[threadID] + 1) * lattice_size_x];
                                            if (topo1dim[ic[threadID] + (jc[threadID] - 1) * lattice_size_x] < min)
                                                min = topo1dim[ic[threadID] + (jc[threadID] - 1) * lattice_size_x];
                                            if (topo1dim[(ic[threadID] + 1) + (jc[threadID] + 1) * lattice_size_x] < min)
                                                min = topo1dim[(ic[threadID] + 1) + (jc[threadID] + 1) * lattice_size_x];
                                            if (topo1dim[(ic[threadID] - 1) + (jc[threadID] - 1) * lattice_size_x] < min)
                                                min = topo1dim[(ic[threadID] - 1) + (jc[threadID] - 1) * lattice_size_x];
                                            if (topo1dim[(ic[threadID] - 1) + (jc[threadID] + 1) * lattice_size_x] < min)
                                                min = topo1dim[(ic[threadID] - 1) + (jc[threadID] + 1) * lattice_size_x];
                                            if (topo1dim[(ic[threadID] + 1) + (jc[threadID] - 1) * lattice_size_x] < min)
                                                min = topo1dim[(ic[threadID] + 1) + (jc[threadID] - 1) * lattice_size_x];
                                            if ((topo1dim[ic[threadID] + jc[threadID] * lattice_size_x] <= min) && (ic[threadID] > 1) && (jc[threadID] > 1)
                                                    && (ic[threadID] < lattice_size_x) && (jc[threadID] < lattice_size_y)
                                                    && (count < stacklimit) && (topo1dim[ic[threadID] + jc[threadID] * lattice_size_x] > 0)) {
                                                topo1dim[ic[threadID] + jc[threadID] * lattice_size_x] = min + fillincrement;


                                                count++;
                                                stacki[(ic[threadID] + 1)] = i;
                                                stackj[(jc[threadID] - 1)] = j;

                                                count++;
                                                stacki[(ic[threadID] - 1)] = i;
                                                stackj[(jc[threadID] - 1)] = j;

                                                count++;
                                                stacki[(ic[threadID] + 1)] = i;
                                                stackj[(jc[threadID] + 1)] = j;

                                                count++;
                                                stacki[(ic[threadID] - 1)] = i;
                                                stackj[(jc[threadID] + 1)] = j;

                                                count++;
                                                stacki[(ic[threadID])] = i;
                                                stackj[(jc[threadID] - 1)] = j;

                                                count++;
                                                stacki[(ic[threadID])] = i;
                                                stackj[(jc[threadID] + 1)] = j;

                                                count++;
                                                stacki[(ic[threadID] + 1)] = i;
                                                stackj[(jc[threadID])] = j;

                                                count++;
                                                stacki[(ic[threadID] - 1)] = i;
                                                stackj[(jc[threadID])] = j;

*//*                                                count++;
                                                stacki[(ic[threadID])] = i;
                                                stackj[(jc[threadID])] = j;*//*
                                            }
                                        }
                                    }


                                }
                            }
                        }*/
                        if (time < 3000) {

                            // defines subsidence rate of lower Colorado River trough
                            // (defined as a triangular domain) of 1.7 m/kyr for 300 kyr
                            for (int i = (threadID * (lattice_size_x / cores) + 1); i < end[threadID]; i++) {

                                for (int j = 1; j <= 162; j++) {
                                    U1d[i + j * lattice_size_x] = 0.0F;
                                }
                            }


                            for (int i = (threadID * (lattice_size_x / cores) + 1); i < end[threadID]; i++)
                                for (int j = 164; j <= lattice_size_y; j++) {
                                    if ((0.4 * j + i < 150) && (time < 300))
                                        U1d[i + j * lattice_size_x] = Along_Grant_Wash_Fault;
                                    else {
                                        U1d[i + j * lattice_size_x] = 0.0F;
                                    }
                                }

                            // perform subsidence and initialize some arrays
                            // performs stream-power model erosion by upwind differencing
                            for (int i = (threadID * (lattice_size_x / cores) + 1); i < end[threadID]; i++)
                                for (int j = 1; j < lattice_size_y; j++) {
                                    topo1dim[i + j * lattice_size_x] += U1d[i + j * lattice_size_x] * timestep;
                                }


                        }


                        max = 0;
                        for (int x = (threadID * (lattice_size_x / cores) + 1); x < end[threadID]; x++)
                            for (int j = 1; j < lattice_size_y; j++){
                                int i = x + j * lattice_size_x;

                                if (mask.get(i)) {

                                    if (x + (j - 1) * lattice_size_x > 0 && x + (j + 1) * lattice_size_x <= oneDimSize) {
                                        float down = 0;
                                        draindiri[x][j] = x;
                                        draindirj[x][j] = j;
                                        diag[threadID] = 1;
                                        if ((topo1dim[(x + 1) + j * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[(x + 1) + j * lattice_size_x] - topo1dim[x + j * lattice_size_x]) < down)) {
                                            down = topo1dim[(x + 1) + j * lattice_size_x] - topo1dim[x + j * lattice_size_x];
                                            draindiri[x][j] = (short) (x + 1);
                                            draindirj[x][j] = j;
                                            diag[threadID] = 1;
                                        }
                                        if ((topo1dim[(x - 1) + j * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[(x - 1) + j * lattice_size_x] - topo1dim[x + j * lattice_size_x]) < down)) {
                                            down = topo1dim[(x - 1) + j * lattice_size_x] - topo1dim[x + j * lattice_size_x];
                                            draindiri[x][j] = (short) (x - 1);
                                            draindirj[x][j] = j;
                                            diag[threadID] = 1;
                                        }
                                        if ((topo1dim[x + (j + 1) * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[x + (j + 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) < down)) {
                                            down = topo1dim[x + (j + 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x];
                                            draindiri[x][j] = x;
                                            draindirj[x][j] = (short) (j + 1);
                                            diag[threadID] = 1;
                                        }


                                        if ((topo1dim[x + (j - 1) * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[x + (j - 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) < down)) {
                                            down = topo1dim[x + (j - 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x];
                                            draindiri[x][j] = x;
                                            draindirj[x][j] = (short) (j - 1);
                                            diag[threadID] = 1;
                                        }
                                        if ((topo1dim[(x + 1) + (j + 1) * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[(x + 1) + (j + 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2 < down)) {
                                            down = (topo1dim[(x + 1) + (j + 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2;
                                            draindiri[x][j] = (short) (x + 1);
                                            draindirj[x][j] = (short) (j + 1);
                                            diag[threadID] = sqrt2;
                                        }
                                        if ((topo1dim[(x - 1) + (j + 1) * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[(x - 1) + (j + 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2 < down)) {
                                            down = (topo1dim[(x - 1) + (j + 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2;
                                            draindiri[x][j] = (short) (x - 1);
                                            draindirj[x][j] = (short) (j + 1);
                                            diag[threadID] = sqrt2;
                                        }
                                        if ((topo1dim[(x + 1) + (j - 1) * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[(x + 1) + (j - 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2 < down)) {
                                            down = (topo1dim[(x + 1) + (j - 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2;
                                            draindiri[x][j] = (short) (x + 1);
                                            draindirj[x][j] = (short) (j - 1);
                                            diag[threadID] = sqrt2;
                                        }
                                        if ((topo1dim[(x - 1) + (j - 1) * lattice_size_x] < topo1dim[x + j * lattice_size_x])
                                                && ((topo1dim[(x - 1) + (j - 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2 < down)) {
                                            down = (topo1dim[(x - 1) + (j - 1) * lattice_size_x] - topo1dim[x + j * lattice_size_x]) * oneoversqrt2;
                                            draindiri[x][j] = (short) (x - 1);
                                            draindirj[x][j] = (short) (j - 1);
                                            diag[threadID] = sqrt2;
                                        }
                                        slope1d[x + j * lattice_size_x] = -oneoverdeltax * (down);
                                    }

                                    float[] erodeddepth = new float[Runtime.getRuntime().availableProcessors()];
                                            erodeddepth[threadID]= topoorig1d[i] - topo1dim[i];
                                    if (erodeddepth[threadID] > 50) {

                                        mask.set(i + 1);
                                        mask.set(i - 1);
                                        mask.set(i + lattice_size_x);
                                        mask.set(i - lattice_size_x);

                                        mask.set((i + 1) + lattice_size_x);
                                        mask.set((i + 1) - lattice_size_x);
                                        mask.set((i - 1) + lattice_size_x);
                                        mask.set((i - 1) - lattice_size_x);

                                        if (timecut[(x)] < 0.01) timecut[(x)] = time;
                                    }
                                    erodeddepth[threadID] = rim[(x)] - topo1dim[i];

                                    float k;
                                    if (erodeddepth[threadID] < 300)
                                        k = Wilsim.c.kstrng;
                                    else if (erodeddepth[threadID] < 700)
                                        k = Wilsim.c.kstrng * Wilsim.c.kfctor;
                                    else
                                        k = Wilsim.c.kstrng;

                                    float c = Wilsim.c.cliffRate;

                                    if (area21d[i] > 2.0f) {
                                        channel1d[i] = 1;
                                        wavespeed1d[i] = (float) (k * Math.sqrt(area21d[i]));
                                    } else {
                                        wavespeed1d[i] = c;
                                        channel1d[i] = 0;
                                    }
                                    if (wavespeed1d[i] > max)
                                        max = wavespeed1d[i];
                                    Wilsim.v.map_color_pre_calc(topo1dim[i], vert_color2[i]);

                                }
                            }

                        float[] erosion = new float [Runtime.getRuntime().availableProcessors()];

                        for (int i = (threadID * (lattice_size_x / cores) + 1); i < end[threadID]; i++)
                            for (int j = 1; j < lattice_size_y; j++)

                                if (mask.get(i + j * lattice_size_x)) {
                                    if ((channel1d[i + j * lattice_size_x] == 1)
                                            && (wavespeed1d[i + j * lattice_size_x] * slope1d[i + j * lattice_size_x] > thresh)) {
                                        topo1dim[i + j * lattice_size_x] -= timestep
                                                * (wavespeed1d[i + j * lattice_size_x] * slope1d[i + j * lattice_size_x] - thresh);
                                        erosion[threadID] += (wavespeed1d[i + j * lattice_size_x] * slope1d[i + j * lattice_size_x] - thresh);
                                    } else {
                                        topo1dim[i + j * lattice_size_x] -= timestep
                                                * (wavespeed1d[i + j * lattice_size_x] * slope1d[i + j * lattice_size_x]);
                                        erosion[threadID] += wavespeed1d[i + j * lattice_size_x] *slope1d[i + j * lattice_size_x];
                                    }
                                }




                        synchronized (needWork[threadID]) {
                            needWork[threadID].boolVal = false;
                            needWork[threadID].notify();
                            //System.out.println("test");
                        }
                        //System.out.println("thread " + threadID + " is done");
                    }
                    //run is next bracket
                }
            }
            );

        }
    }

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

    public void toggleErosionThreads(){
        for (int i = 0; i< Runtime.getRuntime().availableProcessors(); i++){
            synchronized (needWork[i]) {

                if (needWork[i].boolVal) {
                    needWork[i].boolVal = false;
                } else {
                    needWork[i].boolVal = true;
                    needWork[i].notify();
                }
            }
        }
    }

    private static float[] vector() {
        return new float[Model.lattice_size_x + 1];// Math.abs(nh - nl + 1 + NR_END)];
    }

    private static int[] ivector() {
        return new int[Model.stacklimit + 1];// [Math.abs(nh - nl + 1 + NR_END)];
    }

    private static short[] svector() {
        return new short[Model.stacklimit + 1];// [Math.abs(nh - nl + 1 + NR_END)];
    }

    private static float[][] matrix() {
        return new float[Model.lattice_size_x + 1][Model.lattice_size_y + 1];// Math.abs(nrh - nrl +
        // 1)][Math.abs(nch - ncl + 1)];
    }

    public float getTime() {
        return time;
    }




    float diagsingle;
    private void calculatedownhillslopeorig(short i, short j)
    // does the same as the above for a different copy of the topographic
    // surface grid
    {
        float down;
        down = 0;
        draindiri[i][j] = i;
        draindirj[i][j] = j;
        diagsingle = 1;
        if (topodrain[i + 1][j] - topodrain[i][j] < down) {
            down = topodrain[i + 1][j] - topodrain[i][j];
            draindiri[i][j] = (short) (i + 1);
            draindirj[i][j] = j;
            diagsingle = 1;
        }
        if (topodrain[i - 1][j] - topodrain[i][j] < down) {
            down = topodrain[i - 1][j] - topodrain[i][j];
            draindiri[i][j] = (short) (i - 1);
            draindirj[i][j] = j;
            diagsingle = 1;
        }
        if (topodrain[i][j + 1] - topodrain[i][j] < down) {
            down = topodrain[i][j + 1] - topodrain[i][j];
            draindiri[i][j] = i;
            draindirj[i][j] = (short) (j + 1);
            diagsingle = 1;
        }
        if (j > 0)
            if (topodrain[i][j - 1] - topodrain[i][j] < down) {
                down = topodrain[i][j - 1] - topodrain[i][j];
                draindiri[i][j] = i;
                draindirj[i][j] = (short) (j - 1);
                diagsingle = 1;
            }
        if ((topodrain[i + 1][j + 1] - topodrain[i][j]) * oneoversqrt2 < down) {
            down = (topodrain[i + 1][j + 1] - topodrain[i][j]) * oneoversqrt2;
            draindiri[i][j] = (short) (i + 1);
            draindirj[i][j] = (short) (j + 1);
            diagsingle = sqrt2;
        }
        if ((topodrain[i - 1][j + 1] - topodrain[i][j]) * oneoversqrt2 < down) {
            down = (topodrain[i - 1][j + 1] - topodrain[i][j])
                    * oneoversqrt2;
            draindiri[i][j] = (short) (i - 1);
            draindirj[i][j] = (short) (j + 1);
            diagsingle = sqrt2;
        }
        if (j > 0)
            if ((topodrain[i + 1][j - 1] - topodrain[i][j]) * oneoversqrt2 < down) {
                down = (topodrain[i + 1][j - 1] - topodrain[i][j])
                        * oneoversqrt2;
                draindiri[i][j] = (short) (i + 1);
                draindirj[i][j] = (short) (j - 1);
                diagsingle = sqrt2;
            }
        if (j > 0)
            if ((topodrain[i - 1][j - 1] - topodrain[i][j]) * oneoversqrt2 < down) {
                down = (topodrain[i - 1][j - 1] - topodrain[i][j])
                        * oneoversqrt2;
                draindiri[i][j] = (short) (i - 1);
                draindirj[i][j] = (short) (j - 1);
                diagsingle = sqrt2;
            }
        slope1d[i + j * lattice_size_x] = -oneoverdeltax * (down);
    }


    public class Vec3 {
        public float x, y, z;

        public Vec3() {
            x = y = z = 0f;
        }
    }

    private void initcolor() {
        vert_color2 = new Vec3[oneDimSize];

        for (int i = 0; i < vert_color2.length; i++) {
            vert_color2[i] = new Vec3();
        }
    }



    private void initNormals() {
        float [] v1 = new float[3];
        float [] v2 = new float[3];
        float [] norm = new float[3];


        v1[0] = 30.0f / 5.0f;  // Scale factor is a hack
        v1[1] = 0.0f;

        v2[0] = 0.0f;
        v2[1] = 30.0f / 5.0f;  // Ditto on hack
        vert_Normals = new Vec3[oneDimSize];

        for (int i = 1; i < vert_Normals.length - lattice_size_x; i++) {
            v1[2] = Model.topo1dim[i + 1] - Model.topo1dim[i - 1];
            v2[2] = Model.topo1dim[i + lattice_size_x] - Model.topo1dim[(i - 1) + lattice_size_x];
            vert_Normals[i] = new Vec3();


        }
    }

    private static void setupmatrices() {
        maskhurricane = matrix();

        topodrain = matrix();
        area21d = new float[oneDimSize];
        //mask = bimatrix();
        mask = new BitSet(oneDimSize);
        rim = vector();
        timecut = vector();
        stacki = ivector();
        stackj = ivector();
    }

    private void modelMain() {

        executeFlag = new CondVar(false);
        resetFlag = new CondVar(true);


        Wilsim.i.log.append("Model: modelMain() \n");

        // Default input files are stored in a .jar file
        // These need to be handled differently from user supplied
        // data files.

        openDefaultDataFiles();




        // model duration - time is in kyr, so 6000 kyr is the time since
        // the Colorado River became integrated with Grand Canyon and
        // started downcutting
        duration = Wilsim.c.duration;


        // automatically reduced if Courant stability
        // criterion is not satisfied
        float x = 2.0F;
        // channel incision on a pixel, below this threshold we do
        // cliff retreat. calibrated to modern drainage density
        // data from Grand Canyon region

        initcolor();
        initNormals();
        setupmatrices();

        for (i = 1; i <= lattice_size_x; i++) {
            rim[i] = fp0c.nextFloat();
            timecut[i] = 0;

        }

        for (j = 0; j < lattice_size_y; j++)
            for (i = 0; i< lattice_size_x; i++) {
                maskhurricane[i][j] = fp0.nextFloat();
                timecut[i]=0;
            }

/*        for(int i = 0; i <= lattice_size_y * lattice_size_x; i++) {
            topo1dim[i] = fp1.nextFloat();
        }*/

        for (j = 1; j <= lattice_size_y; j++) {
            for (i = 1; i <= lattice_size_x; i++) {


                topoorig1d[i + j * lattice_size_x] = topo1dim[i + j * lattice_size_x] = fp1.nextFloat();
                Wilsim.v.map_color_pre_calc(topo1dim[i + j * lattice_size_x], vert_color2[i + j * lattice_size_x]);
                Wilsim.v.precalc_Normals(i, j);

                topoactual1d[i + j * lattice_size_x] = fp1b.nextFloat();
                topodrain[i][j] = topo1dim[i + j * lattice_size_x];

                area21d[i + j * lattice_size_x] = fp0b.nextFloat();
            }
        }

        Wilsim.v.initModel(); // Let the view know that data matrices are ready

        createThreads();
        for (int thread = 0; thread < Runtime.getRuntime().availableProcessors(); thread++) {
            needWork[thread] = new CondVar(false);
            synchronized (needWork[thread]) {

                needWork[thread].notify();
            }
            threads[thread].start();

        }

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
                    } catch (Exception e) {
                    }
                }
                continue;
            }

            // starting the execution all over from the beginning
            if (resetFlag.boolVal) {
                // Wilsim.i.log.append("reset (1): \n");
                reset();
                // Reset long profile
                // Wilsim.i.log.append("reset (2): \n");
                river = new Profile();
                river.init(visualizationIntervals, 3 * lattice_size_x);
                topoSave = new float [visualizationIntervals][oneDimSize];
                resetFlag.boolVal = false;
                // Wilsim.i.log.append("reset (3): \n");
            }

            if (time >= duration) {
                // Save output
                //Wilsim.c.saveCross();      //<== make saving optional
                Wilsim.c.saveBtn.setEnabled(true);
                synchronized (executeFlag) {
                    executeFlag.boolVal = false;
                }
                continue;
            }

            // Some timing figures
            if (iterationCount % 10 == 0) {
                String str = String.valueOf(time);
            }


            // hydrologic correction



            for (int thread = 0; thread < Runtime.getRuntime().availableProcessors(); thread++) {
                needWork[thread].boolVal = true;
            }

            for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
                synchronized (needWork[i]) {
                    needWork[i].boolVal = true;
                    needWork[i].notify();
                }
            }

            for (int thread = 0; thread < Runtime.getRuntime().availableProcessors(); thread++) {

                while (needWork[thread].boolVal == true) {
                    synchronized (needWork[thread]) {
                        // System.out.println("thread " + thread + " " + needWork[thread].boolVal);
                        //System.out.println("spinlock");
                        //assert true;

                    }
                }
            }







            time += timestep;  // KD - Why here?

            // Courant stability criterion is checked here and timestep is
            // reduced if the criterion is not met
            if (max > 0.9 * deltax / timestep) {
                time -= timestep;
                timestep /= 2.0;

            } else {
                // fprintf(fp5,"%f %f\n",time,erosion);
				/*
				 * bw_fp5 = new BufferedWriter(fp5); bw_fp5.write(time + " " +
				 * erosion); bw_fp5.newLine();
				 */
                if (max < 0.6 * deltax / timestep)
                    timestep *= 1.2;
            }

            if (time >= storeTime) {
                //topoSave[storeCount].clone(topo1dim);
                System.arraycopy(topo1dim,0,topoSave[storeCount],0,topo1dim.length);

                Wilsim.i.log.append("Storing XSections\n");
                for (i = 0; i < XSectionManager.nXSections(); i++)
                    XSectionManager.getXSection(i).appendXSectionValues1d(topo1dim); //1d fix

                // computes and prints final long profiles for Colorado River
                i = profileStartX;
                j = profileStartY;
                int count = 0;
                float dist = 0.0f;
                float last = topo1dim[i + j * lattice_size_x];
                while (i > 1 && count < 522 && j < lattice_size_y) {
                    river.distances[storeCount][count] = dist;
                    river.values[storeCount][count] = last;
                    count++;

                    calculatedownhillslopeorig((short) i, (short) j);
                    //calculatedownhillslope(i, j);

                    dist += deltax * diagsingle;
                    int ikeep = i;
                    last = topo1dim[i + j * lattice_size_x];
                    i = draindiri[i][j];
                    j = draindirj[ikeep][j];
                }
                river.n[storeCount] = count;

                // Wilsim.i.log.append("storeTime: " + storeTime + "\n");

                storeCount++;  // Storage completed
                storeTime = ((float) (storeCount + 1) / visualizationIntervals) * duration;
                river.processedpp();

                String myTime;
                String set = "";

                Color myRGBColor = Color.getHSBColor((float) (storeCount - 1) / (Wilsim.m.visualizationIntervals + 1), 1.0f, 1.0f);

                if (Wilsim.m.getTime() < 6000) {
                    myTime = "<sup>_____</sup> " + String.valueOf((float) Math.round(((6000f - Wilsim.m.getTime()) / 10)) / 100) + " Million Years Ago</font><br>";
                } else if (Wilsim.m.getTime() < 6500) {
                    myTime = "<sup>_____</sup> Present</font><br>";
                } else {
                    myTime = "<sup>_____</sup> " + String.valueOf((float) Math.round(((Wilsim.m.getTime() - 6000f) / 10)) / 100) + " Million Years in the Future</font><br>";
                }
                Wilsim.c.sectionTimes.add("<font color =#" +
                        String.format("%02X", myRGBColor.getRed()) +
                        String.format("%02X", myRGBColor.getGreen()) +
                        String.format("%02X", myRGBColor.getBlue()) + ">" + myTime);
                for (String s : Wilsim.c.sectionTimes) {
                    set += s;
                }
                Wilsim.c.timeLegend.setText("<html>" + set + "</html>");
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
            if (!scoreFlag && time >= presentTime) {
                // Compute score
                score = 0.0f;
                double value;
                int score_count;
                score_count = 0;
                // Stay away from borders for now
                for (i = 2; i < lattice_size_x - 1; i++)
                    for (j = 2; j < lattice_size_y - 1; j++) {
                        if (!((j > 81) && (0.4 * j + i < 75))) {
                            value = topo1dim[i + j * lattice_size_x] - topoactual1d[i + j * lattice_size_x];
                            //value = topo1dim[i + j * lattice_size_x] - topoactual1d[i + j * lattice_size_x];
                            score += value * value;
                            score_count++;
                        }
                    }
                Wilsim.i.log.append("score_count: " + score_count + "\n");
                score /= score_count;
                score = Math.sqrt(score);
                scoreFlag = true;
            }

            //Wilsim.v.loadModel(topo);  // Notify view of data
            Wilsim.v.newComp();
            iterationCount++;
            Thread.yield();
            pause();
            // Wilsim.i.log.append(time + " \n");
        } // main simulation loop

        // print final topography
    }

    private void openDefaultDataFiles() {
        Wilsim.i.log.append("Model: openDefaultDataFiles() \n");

        // Default input files are stored in a .jar file
        // These need to be handled differently from user supplied
        // data files.

        fp0 = new Scanner(getClass().getResourceAsStream(
                "input_files/grandcanyonfaultmask.txt"));

        //Wilsim.i.log.append("Model: opened grandcanyonfaultmask.txt\n");

        fp0b = new Scanner(getClass().getResourceAsStream(
                "input_files/grandcanyonarea.txt"));

        //Wilsim.i.log.append("Model: opened grandcanyonarea.txt\n");

        fp0c = new Scanner(getClass().getResourceAsStream(
                "input_files/grandcanyonrim.txt"));

        //Wilsim.i.log.append("Model: opened grandcanyonrim-hr.txt\n");

        fp1 = new Scanner(getClass().getResourceAsStream(
                "input_files/grandcanyoninitialtopo.txt"));

        //Wilsim.i.log.append("Model: opened grandcanyoninitialtopo.txt\n");

        fp1b = new Scanner(getClass().getResourceAsStream(
                "input_files/grandcanyonactualtopo.txt"));

        //Wilsim.i.log.append("Model: opened grandcanyonactualtopo.txt\n");

        Scanner f1 = new Scanner(getClass().getResourceAsStream("input_files/text.txt"));
        //Wilsim.i.log.append("Model: opened text.txt \n");

    }

    void resetCall() {
/*        for (i = 0; i <= lattice_size_x; i++)
            System.arraycopy(topoorig[i], 0, topo1dim, i * lattice_size_y, lattice_size_y); //1d fix*/

        if (Wilsim.c.pauseValue == 0) {
            if(executeFlag.boolVal)
                toggleExecution();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (j = 1; j <= lattice_size_y; j++)
            for (i = 1; i <= lattice_size_x; i++) {
                //topo1dim[i + j * lattice_size_x] = topoorig[i][j];
                topo1dim[i + j * lattice_size_x] = topoorig1d[i + j * lattice_size_x];
                Wilsim.v.map_color_pre_calc(topoorig1d[i + j * lattice_size_x], vert_color2[i + j * lattice_size_x]);
                Wilsim.v.precalc_Normals(i, j);
            }
        Wilsim.c.sectionTimes.removeAll(Wilsim.c.sectionTimes);
        Wilsim.c.timeLegend.setText("<html></html>");
        if (river != null)
            if (river.getProcessed() > 0) {
                // Wilsim.v.rv.resetProcessed();
                // Wilsim.m.river.resetProcessed();
                Wilsim.m.river = null;
                // Wilsim.v.resetXSections();
                //XSectionManager.getXSection(0).clear();
                Wilsim.v.tempXSection = new XSection();
                Wilsim.v.newComp();
            }



        //Wilsim.v.loadModel(topo);
        Wilsim.v.newComp();

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

    private void reset() {



        time = 0;
        scoreFlag = false;
        //

        for (j = 1; j <= lattice_size_y; j++) {
            for (i = 1; i <= lattice_size_x; i++) {
                U1d[i + j * lattice_size_x] = 0;
                channel1d[i + j * lattice_size_x] = 0;
                topo1dim[i + j * lattice_size_x] = topoorig1d[i + j * lattice_size_x];
                Wilsim.v.map_color_pre_calc(topoorig1d[i + j * lattice_size_x], vert_color2[i + j * lattice_size_x]);
                Wilsim.v.precalc_Normals(i, j);

                if ((j > 81) && (0.4 * j + i < 75))
                    mask.set(i + j * lattice_size_x, true);
                else
                    /*mask[i][j] = 0; // defines the Lower Colorado trough as*/
                    mask.set(i + j * lattice_size_x,false);
                // a triangular block - this could be
                // made prettier with an input mask
                //if (area2[i][j] > 300000)
                if (area21d[i + j * lattice_size_x] > 300000)
                    /*mask[i][j] = 1; // this mask grid is used to speed up*/
                    mask.set(i + j * lattice_size_x, true);
                // the code by computing erosion only in
                // areas incised into the plateau or
                // adjacent to areas incised into the
                // plateau. this approach may break down
                // for some values of K and C (erosion
                // coefficients) but works fine for the
                // values used here
                //masknew[i][j] = mask[i][j];
                wavespeed1d[i + j * lattice_size_x] = 0;
            }
        }
        //Wilsim.v.loadModel(topo);
        Wilsim.v.newComp();

        max = 0;
        Wilsim.c.progressBar.setString("6M Years ago");

        // Reset XSections
        XSectionManager.clear(visualizationIntervals);
        storeCount = 0;
        storeTime = 1.0f / visualizationIntervals * duration;

    }

    public void readFile() {

    }

    private void pause() {
        if (Wilsim.c.pauseValue > 0) {

            float i = Wilsim.c.pauseValue * 1000;
            if (time > i && time < i + 2) {
                toggleExecution();
            }

        }

    }
}
