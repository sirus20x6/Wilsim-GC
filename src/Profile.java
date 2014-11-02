public class Profile
{
    // Stores data values of a river profile through a terrain
    // over multiple iterations.  


    private int nIterates, nValues;
    // Initially unsure whether or not profile follows same path every iteration
    // In hindsight, appears to be the same

    public int [] n;
    public float [][] values;
    public float [][] distances;

    public Profile()
    {
        nIterates = 0; nValues = 0;
    }

    public void init(int nI, int nV)
    {
	// Wilsim.i.log.append("Profile::init(" + nI + ", " + nV + ")\n");
	nIterates = nI;
	nValues = nV;

	values = new float [nI][];
	distances = new float [nI][];
	n = new int [nI];

	for(int i = 0; i < nI; i++)
	    {
		values[i] = new float[nV];
		distances[i] = new float[nV];
		n[i] = 0;
	    }

	
    }

    public int getNIterates()
    {
	return nIterates;
    }
}
