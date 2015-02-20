public class XSectionManager
{
    static private int n; // Number of profiles stored

    // Currently limited to 1 for simplicity.
    static private XSection p;

    public XSectionManager()
    {
    }

    static void init()
    {
	Wilsim.i.log.append("XSectionManager::init()\n");

	n = 0;
	p = new XSection();
    }

    static public int addXSection()
    {
	// Returns an index to a XSection which can be manipulated
	// as needed

	Wilsim.i.log.append("XSectionManager::addXSection()\n");
	// Only one profile for now
	n = 1;

	return 0;
    }

    static public XSection getXSection(int index)
    {
	//Wilsim.i.log.append("XSectionManager::getXSection()\n");

	if(index < n)
	    return p;

	else
	    return null;
    }

    static public void reset()
    {
	Wilsim.i.log.append("XSectionManager::reset()\n");
	
	// Get rid of all XSections
	n = 0;
    }

    static public void clear(int nIntervals)
    {
	// Wilsim.i.log.append("XSectionManager::clear(" + nIntervals + ")\n");
	// Clear out all Xsection data values, while retaining the cross section
	// extent
	// Wilsim.i.log.append("XSectionManager::n: " + n + "\n");
	if (n == 0) return;

	p.init(nIntervals);
    }

    static public int nXSections()
    {
	return n;
    }
    
}

