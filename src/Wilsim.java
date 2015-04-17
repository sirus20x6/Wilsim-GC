import javax.swing.JFrame;
import java.awt.*;

class Wilsim extends JFrame {
	/*
	 * The Wilsim erosion simulation application.
	 * 
	 * Implemented as a classic Model View Controller (MVC) program. Three
	 * threads of execution set up. Since Swing et al are not thread safe, all
	 * UI event handling remains in the applet thread.
	 */

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */

	public static Model m;
	public static View v;
	public static Controller c;
	
	public static InfoPanel i;

	private Container ui;
	private Thread t1;
	private Thread t2;

	private void init() {
		// Called once at beginning of program

		// Get things going here

		v = new View();
		m = new Model();
		c = new Controller();

		i=new InfoPanel();
		i.init();
		ui = rootPane.getContentPane();

		c.createGUI(ui, v.canvas);

		setVisible(true);

		// Set up threads and get started

		t1 = new Thread(m);
		t2 = new Thread(v);
		// Controller thread needs to stay in this thread of execution
		// in order to trap UI input events

		t1.start(); // Model
		t2.start(); // View

		// c.run();    // Start monitoring of thread communication variables
		Thread.yield();
	}

	public static void main(String args[]) {
		Wilsim wilsim = new Wilsim();
		wilsim.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		wilsim.setSize(1024, 800); // Change to something more intelligent
		wilsim.init();
	}

}

