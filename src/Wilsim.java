import javax.swing.*;
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


    void init() {
		// Called once at beginning of program

		// Get things going here
        //System.out.println(unsafe.floatArrayOffset);



		v = new View();
		m = new Model();
		c = new Controller();

		i=new InfoPanel();
		i.init();
        Container ui = rootPane.getContentPane();

		c.createGUI(ui, v.canvas);

		setVisible(true);

		// Set up threads and get started

        Thread t1 = new Thread(m);
        Thread t2 = new Thread(v);
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
		wilsim.setSize(800, 600); // Change to something more intelligent
		wilsim.init();
	}

}

