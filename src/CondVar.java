class CondVar {
	// Wrapper around boolean value - needed for locking and communicating
	// state conditions between execution threads

	public boolean boolVal;
	protected boolean resetVal;

	CondVar(boolean b) {
		boolVal = b;
		
	}
	protected CondVar()
	{
		boolVal=false;
	}
	
}
