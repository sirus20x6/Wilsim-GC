import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.*;
import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.Font;

public class View implements Runnable, GLEventListener
{
    public GLCanvas               canvas;
    private GLCapabilities        caps;
    private GL2                   gl;

    private static float[][]      topo;  // Local copy of the height grid
    private static int            latticeSizeX;
    private static int            latticeSizeY;

    private CondVar               newComputation;
    private boolean               newData;
    private boolean               newParams;
    private boolean               newMode;
    private boolean               newUI;

    // Modes
    final static public int SPIN_MODE = 0;
    final static public int PROFILE_MODE = 1;
    final static public int FLY_MODE = 2;  // Someday
	final static public int XVISUALIZER_MODE = 3;

    private int viewMode = SPIN_MODE;

    // Camera parameters
    private float[] cameraEye = {1, 0, 0};
    private float[] cameraAt  = {0, 0, 0};
    private float[] cameraUp  = {0, 1, 0};
    private float cameraFOV;
    private float cameraNear;
    private float cameraFar;
    final private float cameraRadius = 18000.0F;

    private float [] world2cam = new float[16];

    private float [] lightPosition = new float[4];

    // X and Y screen extents at cameraNear distance
    // Useful for positioning UI widgets
    float screenX, screenY;

    // Material parameters
    private float[] backgroundColor = {1.0f, 1.0f, 1.0f, 1.0f};
    private float[] directLight = {0.8f, 0.8f, 0.8f, 0.8f};
    private float[] ambientLight = {0.2f, 0.2f, 0.2f, 1.0f};

    final private float gridHorizontalScaleFactor = 30.0f;
    // for visualization - change later to take into account true
    // ground scale factor and then throw in proper vertical scaling.

    final private float gridHorizontalSpacingFactor = 720.0f;
    // Spacing between points on ground
    // Needed for cross section output.

    // Intermediate viewing control parameters

    private float viewLongitude;
    private float viewLatitude;

    // For text labels
    private TextRenderer vertScaleTextEngine;
    private TextRenderer scoreTextEngine;
    private TextRenderer CompassTextEngine;

    final float COLOR_MAX_HEIGHT = 3000.0f;
    final float COLOR_MIN_HEIGHT = 1400.0f;
    
    private float [] vertScaleSample = {1400.0f, 2000.0f, 2500.0f, 3000.0f};
    private int nVertScaleSamples = 4;


    // Mouse state
    private boolean buttonDownFlag = false;
    private int mouseStartX, mouseStartY;
    private int mouseEndX, mouseEndY;

    private float [] mouse2Data = new float[4];

    private XSection tempXSection = new XSection();

    // XSection information
    final private float profileScale = 1.1f;  // For borders in profile view

    private XSectionManager xSMgr;

    public View()
    {
	// System.out.println("View: View()\n");
	// Log not available here

	GLProfile glp = GLProfile.getDefault();

	caps = new GLCapabilities(glp);

	caps.setDoubleBuffered(true);
	caps.setRedBits(8);
	caps.setGreenBits(8);
	caps.setBlueBits(8);
	caps.setAlphaBits(8);
	caps.setDepthBits(32);
	caps.setHardwareAccelerated(true);

	canvas = new GLCanvas(caps);
	ViewEventHandler v = new ViewEventHandler();
	canvas.addMouseListener(v);
	canvas.addMouseMotionListener(v);

	// Initialize camera parameters
	viewLongitude = 180.0f;
	viewLatitude = 45.0f;
	
	cameraEye[0] = 0.0f;  cameraEye[1] = -cameraRadius; cameraEye[2] = 0.0f;
	cameraAt[0]  = 0.0f;  cameraAt[1]  = 0.0f;  cameraAt[2]  = 0.0f;
	cameraUp[0]  = 0.0f;  cameraUp[1]  = 0.0f;  cameraUp[2]  = 1.0f;
	cameraFOV = 50;
	cameraNear = 2000.0f;
	cameraFar = 50000.0f;
	
	lightPosition[0] = cameraRadius;
	lightPosition[1] = cameraRadius;
	lightPosition[2] = cameraRadius;
	lightPosition[3] = 0.0f;

	// Create communication locks
	newComputation = new CondVar(false);
	newData = false;
	newParams = true;
	newMode = true;
	newUI = false;
    }

    // inner class for handling events
    private class ViewEventHandler extends ViewAdapter
    {
    @Override
	public void mousePressed(MouseEvent m)
	{
	    /* Wilsim.i.log.append("View : button_down() (" + m.getX() + ", "
				+ m.getY() + ")\n");
	    */

	    mouseEndX = mouseStartX = m.getX();
	    mouseEndY = mouseStartY = m.getY();

	    switch(viewMode)
		{
		case SPIN_MODE: return;
		case PROFILE_MODE: 
		    tempXSection.startX = 
			(int)(mouseStartX * mouse2Data[0] + mouse2Data[1]);
		    tempXSection.startY = 
			(int)(mouseStartY * mouse2Data[2] + mouse2Data[3]);
		    // Check for bounds

		    if(tempXSection.startX < 0 
		       || tempXSection.startX >= latticeSizeX)
			return;
		    if(tempXSection.startY < 0
		       || tempXSection.startY >= latticeSizeY)
			return;

		    // For future implementation -- clipping of line outside
		    // bounds would be better.

		    /* Wilsim.i.log.append("View : button_down() mouse(" 
					+ m.getX() + ", "+ m.getY() + ")\n");
		    Wilsim.i.log.append("View : button_down() data(" 
					+ tempXSection.startX + ", "
					+ tempXSection.startY + ")\n");
		    */
		    break;
		default: return;
		}

	    buttonDownFlag = true;

	    synchronized(newComputation)
		{
		    newUI = true;
		    newComputation.boolVal = true;
		    newComputation.notify();
		}

	}

    @Override
	public void mouseReleased(MouseEvent m)
	{
	    /* Wilsim.i.log.append("View : button_up() (" + m.getX() + ", "
	       + m.getY() + ")\n"); */
	    	    
	    mouseEndX = m.getX();
	    mouseEndY = m.getY();

	    switch(viewMode)
		{
		case SPIN_MODE: 
		    break;
		case PROFILE_MODE: 
		    if(! buttonDownFlag ) return;  // Invalid start point
		    tempXSection.endX = 
			(int)(mouseEndX * mouse2Data[0] + mouse2Data[1]);
		    tempXSection.endY = 
			(int)(mouseEndY * mouse2Data[2] + mouse2Data[3]);
		    // Check for bounds
		    if(tempXSection.startX < 0 
		       || tempXSection.endX >= latticeSizeX)
			break;
		    if(tempXSection.startY < 0
		       || tempXSection.endY >= latticeSizeY)
			break;

		    // For future implementation -- clipping of line outside
		    // bounds would be better.

		    /* 
		    Wilsim.i.log.append("View : button_up() mouse(" 
					+ m.getX() + ", "+ m.getY() + ")\n");
		    Wilsim.i.log.append("View : button_up() data(" 
					+ tempXSection.endX + ", "
					+ tempXSection.endY + ")\n");
		    */

		    // For future implementation -- clipping of line outside
		    // bounds would be better.

		    // Make a profile
		    int p_index = XSectionManager.addXSection();
		    XSection p = XSectionManager.getXSection(p_index);

		    p.startX = tempXSection.startX;
		    p.startY = tempXSection.startY;
		    p.endX = tempXSection.endX;
		    p.endY = tempXSection.endY;
		    
		    break;
		default:
		    break;
		}

	    buttonDownFlag = false;
	    synchronized(newComputation)
		{
		    newUI = true;
		    newComputation.boolVal = true;
		    newComputation.notify();
		}
	}
    @Override
	public void mouseDragged(MouseEvent m)
	{
	    mouseEndX = m.getX();
	    mouseEndY = m.getY();

	    switch(viewMode)
		{
		case SPIN_MODE: 
		    return;
		case PROFILE_MODE: 
		    /* Wilsim.i.log.append("View : move() (" + m.getX() + ", "
						+ m.getY() + ")\n");
		    */
		    if(!buttonDownFlag)
			{
			    // Shouldn't happen unless events are processed
			    // out of order
			    Wilsim.i.log.append("View : move() : out of order\n");
			    return;  
			}
		    
			{
	    
			    // Check for bounds
			    // For future implementation -- clipping of line outside
			    // bounds would be better.
			}
		    break;
		default:
		    return;
		}

	    synchronized(newComputation)
		{
		    newUI = true;
		    newComputation.boolVal = true;
		    newComputation.notify();
		}
	}
    }

    void init()
    {
	// Wilsim.i.log.append("View : init()\n");
	gl = canvas.getGL().getGL2();

	gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

	float matDiffuse[] = {0.8f, 0.8f, 0.8f, 1.0f};
	float matSpecular[] = {0.01f, 0.01f, 0.01f, 1.0f};
	float matShininess[] = {30.0f, 30.0f, 30.0f, 1.0f};
	
	gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
	gl.glMaterialfv(gl.GL_FRONT_AND_BACK, GLLightingFunc.GL_DIFFUSE,
			matDiffuse, 0);
	gl.glMaterialfv(gl.GL_FRONT_AND_BACK, GLLightingFunc.GL_SPECULAR, 
			matSpecular, 0);
	gl.glMaterialfv(gl.GL_FRONT_AND_BACK, GLLightingFunc.GL_SHININESS, 
			matShininess, 0);
	gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, 
		     directLight, 0);
	gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, 
		     directLight, 0);
	
	gl.glLightModeli(gl.GL_LIGHT_MODEL_TWO_SIDE, 1);
	gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, 
		     ambientLight, 0);

	gl.glEnable(gl.GL_NORMALIZE);
	//	gl.glEnable(gl.GL_LIGHTING);
	gl.glEnable(gl.GL_LIGHT0);
	gl.glEnable(gl.GL_DEPTH_TEST);

	// Set up text rendering capability
	vertScaleTextEngine = new TextRenderer(new Font("SansSerif", Font.PLAIN, 14));
	scoreTextEngine = new TextRenderer(new Font("SanSerif", Font.PLAIN, 28));
    CompassTextEngine = new TextRenderer(new Font("SanSerif", Font.PLAIN, 20));

	// Initialize XSection Manager
	XSectionManager.init();
    }

    public void initModel()
    {
    	// Wilsim.i.log.append("View: initModel()\n");
	latticeSizeX = Wilsim.m.lattice_size_x;
	latticeSizeY = Wilsim.m.lattice_size_y;
	topo = new float[latticeSizeX+1][latticeSizeY+1];
	String str = latticeSizeX + "x" + latticeSizeY;
	// Wilsim.i.log.append("View: initModel(): " + str + '\n');
	newComputation.boolVal = false;
    }

    public void loadModel(float[][] array)
    {
	// System.out.println("View: loadModel()\n");

	for(int i = 0; i < array.length; i++)
	    for(int j = 0; j < array[i].length; j++)
		topo[i][j] = array[i][j];

	synchronized(newComputation)
	    {
		newData = true;
		newComputation.boolVal = true;
		newComputation.notify();
	    }
    }

    public void changeViewMode(int v)
    {
	synchronized(newComputation)
	    {
		switch(v)
		    {
		    case SPIN_MODE:
			viewMode = SPIN_MODE;
			break;
		    case PROFILE_MODE:
			viewMode = PROFILE_MODE;
//		    case XVISUALIZER_MODE:
//		    viewMode = XVISUALIZER_MODE;
			break;
		    default:  //Unrecognized view mode - do nothing
			break;
		    }

		newMode = true;
		newComputation.boolVal = true;
		newComputation.notify();
	    }
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
	// System.out.println("View : display(drawable)\n");
	draw(drawable);

    }

    public void draw(GLAutoDrawable draw)
    {
	// System.out.println("View: draw()\n");
	
	gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
	gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
	gl.glColor3f(0.8f, 0.8f, 0.8f);

	// Move more and more into separate drawing modes
	// as functionality diverges
	if(viewMode == PROFILE_MODE)
	    {
		drawXSectionMode();
	    gl.glEnable(gl.GL_LIGHTING);

		// Position lights
		gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

		// Recenter and resposition grid
		// gl.glRotatef(180f, 1.0f, 1.0f, .0f);
		gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
		gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
		// Z translation is currently a hack based on the grid.  Roughly 1800 m for
		// the Grand Canyon

		drawTerrain();

		gl.glDisable(gl.GL_LIGHTING);

		drawXSections();

		// Now draw UI stuff on top
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity();

		drawUI();
	    }
	else if(viewMode == SPIN_MODE)
	    {
		// Spin mode
		drawSpinMode();
			gl.glEnable(gl.GL_LIGHTING);

			// Position lights
			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

			// Recenter and resposition grid
			// gl.glRotatef(180f, 1.0f, 1.0f, .0f);
			gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
			gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
			// Z translation is currently a hack based on the grid.  Roughly 1800 m for
			// the Grand Canyon

			drawTerrain();

			gl.glDisable(gl.GL_LIGHTING);

			drawXSections();

			// Now draw UI stuff on top
			gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
			gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
			gl.glLoadIdentity();

			drawUI();
	    }
	else
		{
		drawXVISUALIZER_MODE();
			gl.glEnable(gl.GL_LIGHTING);

			// Position lights
			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

			// Recenter and resposition grid
			// gl.glRotatef(180f, 1.0f, 1.0f, .0f);
			gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
			gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
			// Z translation is currently a hack based on the grid.  Roughly 1800 m for
			// the Grand Canyon

			drawTerrain();

			gl.glDisable(gl.GL_LIGHTING);

			drawXSections();

			// Now draw UI stuff on top
			gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
			gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
			gl.glLoadIdentity();

			drawUI();
		}



	synchronized(newComputation)
	    {
		newData = false;
		newComputation.boolVal = false; // ?? Should this be elsewhere?
	    }
	gl.glFlush();
    }

    private void drawSpinMode()
    {
	// Move more and more in here as draw mode methods diverge
	if(newParams || newMode)
	    {
		computeView(viewLongitude, viewLatitude);

		// Cleanup
		newParams = false;
		newMode = false;
	    }

	// Set up 3D view
	gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
	gl.glLoadIdentity();
	gl.glFrustum(-screenX, screenX, -screenY, screenY,
		     cameraNear, cameraFar);

	// Set up 3D rendering
	gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
	gl.glLoadIdentity();
	gl.glMultMatrixf(world2cam, 0);


    }
    
    private void drawXSectionMode()
    {
	// Move more and more in here as draw mode methods diverge

	if(newParams || newMode)
	    {
		float aspect = ((float) canvas.getWidth())/ canvas.getHeight();
		float data_aspect = ((float)latticeSizeX+1) / (latticeSizeY+1);

		if(aspect > data_aspect)
		    {
			// Wide screen
			screenY = ((float)latticeSizeY + 1) / 2.0f 
			    * gridHorizontalScaleFactor * profileScale;
			screenX = screenY * aspect;
		    }
		else
		    {
			// Tall screen
			screenX = ((float)latticeSizeX + 1) / 2.0f
			    * gridHorizontalScaleFactor * profileScale;
			screenY = screenX / aspect;
		    }
		computeXSectionView();
		newParams = true;
		newMode = true;
	    }



	// Set up orthogonal view
	gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
	gl.glLoadIdentity();
	gl.glOrtho(-screenX, screenX, -screenY, screenY,
		     cameraNear, cameraFar);

	// Set up 3D rendering
	gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
	gl.glLoadIdentity();
	gl.glMultMatrixf(world2cam, 0);
    }

	private void drawXVISUALIZER_MODE()
	{

	}

    private void drawXSections()
    {
	int n = XSectionManager.nXSections();

	// Wilsim.i.log.append("View : drawXSections() : " + n + "\n");

	gl.glEnable(gl.GL_BLEND);
	gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
	gl.glBegin(gl.GL_QUADS);
	for(int i = 0; i < n; i++)
	    {
		XSection p = XSectionManager.getXSection(i);
		
		gl.glColor4f(0.5f, 0.5f, 0.5f, 0.6f);
		gl.glVertex3f(p.startX, p.startY, COLOR_MIN_HEIGHT);
		gl.glVertex3f(p.endX, p.endY, COLOR_MIN_HEIGHT);
		gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);
		gl.glVertex3f(p.startX, p.startY, COLOR_MAX_HEIGHT);
		
	    }
	gl.glEnd();
	gl.glDisable(gl.GL_BLEND);
	
	// Draw top highlights
	gl.glLineWidth(3.0f);
	gl.glBegin(gl.GL_LINES);
	gl.glColor3f(0.0f, 0.0f, 0.0f);
	for(int i = 0; i < n; i++)
	    {
		XSection p = XSectionManager.getXSection(i);
		
		gl.glVertex3f(p.startX, p.startY, COLOR_MAX_HEIGHT);
		gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);

		// Draw arrow

		float dx, dy, dbarb;
		dx = p.endX - p.startX;
		dy = p.endY - p.startY;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		// Some hackish numbers to keep barb size sane
		dbarb = distance * 0.06f;  // 10% / sqrt(3)
		dbarb = (float) Math.min(dbarb, 15.0f);  // but no greater than 26. 
		dbarb = (float) Math.max(dbarb, 2.0f); // but no smaller than 3

		dx = dx * dbarb / distance;
		dy = dy * dbarb / distance;
		gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);
		gl.glVertex3f(p.endX - 2.0f * dx + dy, p.endY - dx - 2.0f * dy, COLOR_MAX_HEIGHT);
		gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);
		gl.glVertex3f(p.endX - 2.0f * dx - dy, p.endY + dx - 2.0f * dy, COLOR_MAX_HEIGHT);
	    }
    	gl.glEnd();
	
	gl.glLineWidth(1.0f);
    }

    private void drawTerrain()
    {
	// Need a separate vertex normal for each quad -- this implementation is inefficient
	// Should be computed once and stored when grid is loaded

	float [] v1 = new float[3];
	float [] v2 = new float[3];
	float [] norm = new float[3];
	float [] color = new float[3];

	v1[0] = gridHorizontalScaleFactor / 5.0f;  // Scale factor is a hack
	v1[1] = 0.0f;

	v2[0] = 0.0f;
	v2[1] = gridHorizontalScaleFactor / 5.0f;  // Ditto on hack

	// Get ready for varying material properties
	gl.glEnable(gl.GL_COLOR_MATERIAL);
	gl.glColorMaterial(gl.GL_FRONT_AND_BACK,
			   GLLightingFunc.GL_AMBIENT_AND_DIFFUSE);

	for(int x = 2; x < latticeSizeX-1; x++)
	    {
		for(int y = 2; y < latticeSizeY-1; y++)
		    {
			gl.glBegin(gl.GL_TRIANGLES);
			v1[2] = topo[x+1][y] - topo[x-1][y];
			v2[2] = topo[x][y+1] - topo[x][y-1];
			
			cross(norm, v2, v1);

			gl.glNormal3fv(norm, 0);

			map_color(topo[x][y], color);
			gl.glColor3f(color[0], color[1], color[2]);
			// debug_color(x, y);
			gl.glVertex3f(x,   y,   topo[x][y]);

			map_color(topo[x+1][y], color);
			// gl.glColor3fv(color, 0);
			gl.glColor3f(color[0], color[1], color[2]);
			// debug_color(x+1, y);
			gl.glVertex3f(x+1, y,   topo[x+1][y]);

			map_color(topo[x][y+1], color);
			// gl.glColor3fv(color, 0);
			gl.glColor3f(color[0], color[1], color[2]);
			// debug_color(x, y+1);
			gl.glVertex3f(x,   y+1, topo[x][y+1]);

			v1[2] = topo[x+2][y+1] - topo[x][y+1];
			v2[2] = topo[x+1][y+2] - topo[x+1][y];

			cross(norm, v2, v1);

			map_color(topo[x+1][y+1], color);
			// gl.glColor3fv(color, 0);
			gl.glColor3f(color[0], color[1], color[2]);
			// debug_color(x+1, y+1);
			gl.glVertex3f(x+1, y+1, topo[x+1][y+1]);

			map_color(topo[x][y+1], color);
			// gl.glColor3fv(color, 0);
			gl.glColor3f(color[0], color[1], color[2]);
			// debug_color(x, y+1);
			gl.glVertex3f(x,   y+1, topo[x][y+1]);

			map_color(topo[x+1][y], color);
			// gl.glColor3fv(color, 0);
			gl.glColor3f(color[0], color[1], color[2]);
			// debug_color(x+1, y);
			gl.glVertex3f(x+1, y,   topo[x+1][y]);

			gl.glEnd();
		    }
	    }
	gl.glDisable(gl.GL_COLOR_MATERIAL);
    }

    private void drawUI()
    {
	if(viewMode == PROFILE_MODE && buttonDownFlag)
	    {
		/*
		Wilsim.i.log.append("View : drawUI() (" + mouseStartX + ", "
				    + mouseStartY + ")-("
				    + mouseEndX + ", "
				    + mouseEndY + ")\n");
		*/
		// Draw mouse line
		gl.glLineWidth(3.0f);
		gl.glBegin(gl.GL_LINES);
		gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glVertex3f(mouseStartX, 
			      canvas.getHeight() - mouseStartY, -1.2f);
		gl.glVertex3f(mouseEndX, 
			      canvas.getHeight() - mouseEndY, -1.2f);
		gl.glEnd();
		gl.glLineWidth(1.0f);
	    }
	newUI = false;
	drawVertScale();
		//drawScaleBar();
	if(viewMode == SPIN_MODE) drawCompass();
	//drawCompass();
	if(viewMode == SPIN_MODE && Wilsim.m.scoreFlag)
	    //drawScore();   // Temporarily removed until sane values emerge
	    ;
    }

    private void drawVertScale()
    {
	// Figure out size, location
	// Maybe this should be done only once and recomputed when the shape changes
	// Size is expandable up to a point.  Fraction of screen until screen gets so large.
	// For now min( 300 pixels high, height of screen) 
	// Use lower left origin pixel coordinate system

	float scaleHeight = canvas.getHeight();
	if(scaleHeight > 300.0f) scaleHeight = 300.0f;

	float scaleBorder = 0.03f; // Fraction of screen height
	scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight ; // Give a little border
	float scaleWidth = 0.05f * scaleHeight; // Seems like a good ratio
	float scalePosX;
	float scalePosY;
	//	scalePosX = canvas.getHeight() * scaleBorder;
	//	scalePosY = canvas.getHeight() * (1.0f - scaleBorder) - scaleHeight;
	scalePosX = 10.0f;
	scalePosY = canvas.getHeight() - 10.0f - scaleHeight;


	//	Wilsim.i.log.append("View : scaleHeight :" + String.valueOf(scaleHeight) + "\n");
	//	Wilsim.i.log.append("View : scaleWidth :" + String.valueOf(scaleWidth) + "\n");
	//	Wilsim.i.log.append("View : scalePosX :" + String.valueOf(scalePosX) + "\n");
	//	Wilsim.i.log.append("View : scalePosY :" + String.valueOf(scalePosY) + "\n");

	// Calculate divisions and division locations
	// Hardwired for now

	// Draw colored rectangular scale
	float [] color = new float[3];

	gl.glBegin(gl.GL_QUAD_STRIP);
	for(int i = 0; i <= 12; i++)
	    {
		map_color((i / 12.0f) * (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT)
			  + COLOR_MIN_HEIGHT, color);
		gl.glColor3f(color[0], color[1], color[2]);
		gl.glVertex3f(scalePosX, 
			      scalePosY + (i / 12.0f) * scaleHeight, -1.7f);
		gl.glVertex3f(scalePosX + scaleWidth, 
			      scalePosY + (i / 12.0f) * scaleHeight, -1.7f);
	    }

	gl.glEnd();
	// Outline the scale
	
	gl.glColor3f(0.0f, 0.0f, 0.0f);
	gl.glBegin(gl.GL_LINE_LOOP);
	gl.glVertex3f(scalePosX, scalePosY, -1.5f);
	gl.glVertex3f(scalePosX + scaleWidth, scalePosY, -1.5f);
	gl.glVertex3f(scalePosX + scaleWidth, scalePosY + scaleHeight, -1.5f);
	gl.glVertex3f(scalePosX, scalePosY + scaleHeight, -1.5f);
	gl.glEnd();
	
	// Draw label tics
	gl.glColor3f(0.0f, 0.0f, 0.0f);
	gl.glBegin(gl.GL_LINES);
	for(int i = 0; i < nVertScaleSamples; i++)
	    {
		float y;
		y = (vertScaleSample[i] - COLOR_MIN_HEIGHT) 
		    / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);
		y = y * scaleHeight + scalePosY;
		
		gl.glVertex3f(scalePosX + scaleWidth, y, -1.5f);
		gl.glVertex3f(scalePosX + scaleWidth + 5.0f, y, -1.5f);
	    }
	gl.glEnd();

	// Draw labels

	vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
	vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
	
	float textPosX, textPosY;
	textPosX = scalePosX + scaleWidth + 10.0f;

	for(int i = 0; i < nVertScaleSamples; i++)
	    {
		textPosY = scalePosY + 
		    ((vertScaleSample[i] - COLOR_MIN_HEIGHT) 
		     / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT)) 
		    * scaleHeight - 5.0f;
		
		String Label = Float.toString(vertScaleSample[i]) + " m";
		vertScaleTextEngine.draw(Label, (int)textPosX, (int)textPosY);
	    }

	vertScaleTextEngine.endRendering();
    }

	private void drawScaleBar()
	{

		float scaleHeight = canvas.getHeight();
		if(scaleHeight > 300.0f) scaleHeight = 300.0f;

		float scaleBorder = 0.03f; // Fraction of screen height
		scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight ; // Give a little border
		float scaleWidth = 0.05f * scaleHeight; // Seems like a good ratio
		float scalePosX;
		float scalePosY;
		//	scalePosX = canvas.getHeight() * scaleBorder;
		//	scalePosY = canvas.getHeight() * (1.0f - scaleBorder) - scaleHeight;
		scalePosX = 10.0f;
		scalePosY = canvas.getHeight() - 100.0f - scaleHeight;


		// Calculate divisions and division locations
		// Hardwired for now

		// Draw colored rectangular scale
		float [] color = new float[3];


		/********************************************/
		//Attempt to use objreader.java
		/********************************************/




		/********************************************/

		gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		gl.glVertex3f(scalePosX * 5, scalePosY + 30.0f, -1.5f);
		gl.glVertex3f(scalePosX * 8 + scaleHeight, scalePosY + 30.0f, -1.5f);
		gl.glVertex3f(scalePosX * 5, scalePosY + 30.0f, -1.5f);
		// gl.glVertex3f(scalePosX + scaleWidth, scaleHeight , -1.5f);
		//gl.glVertex3f(scalePosX, scalePosY + scaleHeight, -1.5f);
		gl.glEnd();

		// Draw label tics
		gl.glColor3f(0.0f, 0.0f, 0.0f);
		gl.glBegin(GL2.GL_LINES);
		int nVertScaleSamples = 4;



		gl.glVertex3f(scalePosX * 5, scalePosY +30.0f, -1.5f);
		gl.glVertex3f(scalePosX * 5, scalePosY +35.0f, -1.5f);
		gl.glVertex3f(scalePosX * 5, scalePosY +30.0f, -1.5f);


		gl.glVertex3f(128, scalePosY +30.0f, -1.5f);
		gl.glVertex3f(128, scalePosY +35.0f, -1.5f);
		gl.glVertex3f(128, scalePosY +30.0f, -1.5f);

		gl.glVertex3f(206, scalePosY +30.0f, -1.5f);
		gl.glVertex3f(206, scalePosY +35.0f, -1.5f);
		gl.glVertex3f(206, scalePosY +30.0f, -1.5f);

		gl.glVertex3f(284, scalePosY +30.0f, -1.5f);
		gl.glVertex3f(284, scalePosY +35.0f, -1.5f);
		gl.glVertex3f(284, scalePosY +30.0f, -1.5f);

		gl.glVertex3f(362, scalePosY +30.0f, -1.5f);
		gl.glVertex3f(362, scalePosY +35.0f, -1.5f);
		gl.glVertex3f(362, scalePosY +30.0f, -1.5f);


		gl.glEnd();

		// Draw labels


		vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
		gl.glMatrixMode(5888);
		gl.glPushMatrix();

		gl.glRotatef(90, 0, 0, 1);
		vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);

		float textPosX, textPosY;
		textPosX = scalePosX + scaleWidth + 10.0f;

		float total = 244800;
		//61200

		for(int i = 0; i < 5; i++)
		{
			textPosY = 78 * i;

			String Label = Float.toString(total) ;
			total = total - 61200.0f;
			vertScaleTextEngine.draw(Label, (int)textPosX -30, (int)textPosY - 362);
		}

		vertScaleTextEngine.endRendering();
	}
    private void drawCompass()
    {
        // Positions the compass in the upper right corner
        float compassHeight = canvas.getHeight();
        float compassWidth = 0.05f;
        float icompasswidth = canvas.getWidth()*compassWidth;
        float ccx;
        float ccy;
        if (icompasswidth<20.0f) icompasswidth=20.0f;
        ccx = canvas.getWidth()-icompasswidth-20;
        ccy = canvas.getHeight()-icompasswidth-20;

        // Rotates the compass with the model, points to North
        gl.glPushMatrix();
        gl.glTranslatef(ccx, ccy, 0);
        gl.glRotatef(viewLongitude-180, 0, 0, 1);

        //Draw compass border
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(gl.GL_LINE_LOOP);
        gl.glVertex3f(icompasswidth*(-2/3.0f), -icompasswidth, -1.5f);
        gl.glVertex3f(0, icompasswidth, -1.0f);
        gl.glVertex3f(icompasswidth*(2/3.0f), -icompasswidth, -1.5f);
        gl.glVertex3f(0, icompasswidth*(-1/3.0f), -1.5f);
        gl.glEnd();

//    	//Draw left half of compass arrow
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        gl.glColor3f(0.9f, 0.7f, 0.1f);
        gl.glVertex3f(icompasswidth*(-2/3.0f), -icompasswidth, -1.5f);
        gl.glColor3f(0.7f, 0.2f, 0.0f);
        gl.glVertex3f(0, icompasswidth, -1.5f);
        gl.glColor3f(1.0f, 0.8f, 0.1f);
        gl.glVertex3f(0, icompasswidth*(-1/3.0f), -1.5f);
        gl.glEnd();

        //Draw right half of compass arrow
        gl.glBegin(gl.GL_TRIANGLE_STRIP);
        gl.glColor3f(0.9f, 0.7f, 0.1f);
        gl.glVertex3f(icompasswidth*(2/3.0f), -icompasswidth, -1.5f);
        gl.glColor3f(0.7f, 0.2f, 0.0f);
        gl.glVertex3f(0, icompasswidth, -1.5f);
        gl.glColor3f(1.0f, 0.8f, 0.1f);
        gl.glVertex3f(0, icompasswidth*(-1/3.0f), -1.5f);
        gl.glEnd();

        CompassTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
        CompassTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Writes an "N" in the middle of the compass
        float textPosX, textPosY;
        textPosX = ccx-6.0f;
        textPosY = ccy-10.0f;

        String Label = "N";
        CompassTextEngine.draw(Label, (int)textPosX, (int)textPosY);

        CompassTextEngine.endRendering();

        gl.glPopMatrix();
    }

    private void drawScore()
    {
	scoreTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
	scoreTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);

	float textPosX, textPosY;
	
	// Need something a little more intelligent
	textPosX = canvas.getWidth() * 0.30f; 
	textPosY =  + 50;

	scoreTextEngine.draw(String.format("Score: %7.2f", Wilsim.m.score),
			     (int)textPosX, (int)textPosY);
	scoreTextEngine.endRendering();
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
	init();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
    {
    	Wilsim.i.log.append("View : reshape()\n");
	Wilsim.i.log.append(String.format("x: %d\ny: %d\nw: %d\nh: %d\n",
					  x, y, w, h));
	gl.glViewport(0, 0, w, h);

	newParams = true;
	newComputation.boolVal = true;
    }

    @Override
    public void run()
    {
	canvas.requestFocus();
	canvas.addGLEventListener(this);

	// Wilsim.i.log.append("View: run()\n");

	while(true)
	    {
		if(!newComputation.boolVal)
		    {
			synchronized(newComputation)
			    {
				try{
				    newComputation.wait();
				} catch (Exception e) {}
			    }
			continue;
		    }

		// Render an image here
		canvas.repaint();
	    }
    }

    void computeXSectionView()
    {
	// View from above and centered over terrain

	world2cam[0]  = 1.0f; world2cam[1]  = 0.0f; 
	world2cam[2]  = 0.0f; world2cam[3]  = 0.0f;

	world2cam[4]  = 0.0f; world2cam[5]  = 1.0f;
	world2cam[6]  = 0.0f; world2cam[7]  = 0.0f;

	world2cam[8]  = 0.0f; world2cam[9]  = 0.0f;
	world2cam[10] = 1.0f; world2cam[11] = 0.0f;

	world2cam[12] = 0.0f; world2cam[13] = 0.0f;
	world2cam[14] = -cameraRadius; world2cam[15] = 1.0f;

	// Transform from canvas/mouse coords to data grid
	mouse2Data[0] = (2.0f * screenX) 
	    / (canvas.getWidth() * gridHorizontalScaleFactor);
	mouse2Data[1] = latticeSizeX / 2.0f 
	    - screenX / gridHorizontalScaleFactor;

	mouse2Data[2] = (2.0f * screenY)
	    / (canvas.getHeight() * gridHorizontalScaleFactor);
	mouse2Data[3] = latticeSizeY / 2.0f
	    - screenY / gridHorizontalScaleFactor;
	
    }

    void computeView(float longitude, float latitude)
    {
	// System.out.println("View: computeView()\n");
	float theta = cameraFOV/2.0f;
	float ttheta = (float) Math.tan(theta * Math.PI / 180.0f);
	float aspect = ((float) canvas.getWidth())/ canvas.getHeight();

	if(canvas.getWidth() > canvas.getHeight())
	    {
		// Horizontal window
		screenX = ttheta * cameraNear;
		screenY = (screenX * canvas.getHeight()) / canvas.getWidth(); 
	    }
	else
	    {
		// Vertical window
		screenY = ttheta * cameraNear;
		screenX = (screenY * canvas.getWidth()) / canvas.getHeight();
	    }

	// Compute new eye position
	cameraEye[0] = cameraRadius * (float) Math.sin(longitude*Math.PI/180.0f)
	    * (float) Math.sin(latitude*Math.PI/180.0f);
	cameraEye[1] = cameraRadius * (float) Math.cos(longitude*Math.PI/180.0f)
	    * (float) Math.sin(latitude*Math.PI/180.0f);
	cameraEye[2] = cameraRadius * (float) Math.cos(latitude*Math.PI/180.0f);

	calculateWorldToCamera(world2cam);
    }


    // Vector manipulation methods

    private void cross(float [] c, float [] a, float [] b)
    {
	// C = A X B

	c[0] = a[1] * b[2] - b[1] * a[2];
	c[1] = -(a[0] * b[2] - b[0] * a[2]);
	c[2] = a[0] * b[1] - b[0] * a[1];
    }

    private float dot(float [] a, float [] b)
    {
	return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private void normalize(float [] v)
    {
	// In place
	float mag2 = v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
	mag2 = (float) Math.sqrt(mag2);
	v[0] /= mag2; v[1] /= mag2; v[2] /= mag2;
    }
    private void calculateWorldToCamera(float [] m)
    {
	float [] A = new float[3];
	A[0] = cameraAt[0] - cameraEye[0];
	A[1] = cameraAt[1] - cameraEye[1];
	A[2] = cameraAt[2] - cameraEye[2];

	normalize(A);

	float [] V = new float[3];
	
	cross(V, A, cameraUp);

	normalize(V);

	float [] U = new float[3];

	cross(U, V, A);

	// GL uses column major order for matrices - Ugh!

	m[0] = V[0] ; m[1] = U[0] ; m[2]  = -A[0]; m[3]  = 0.0f;
	m[4] = V[1] ; m[5] = U[1] ; m[6]  = -A[1]; m[7]  = 0.0f;
	m[8] = V[2] ; m[9] = U[2] ; m[10] = -A[2]; m[11] = 0.0f;
	m[12] = -dot(cameraEye, V);
	m[13] = -dot(cameraEye, U);
	m[14] =  dot(cameraEye, A);
	m[15] = 1.0f;
    }
    
    //Horizontal Rotate
    public void horizontalRotate(float angle)
    {
	viewLongitude = angle;

	synchronized(newComputation)
	    {
		newParams = true;
		newComputation.boolVal = true;
		newComputation.notify();
	    }
	
    }
 
    public void verticalRotate(float angle)
    {
	viewLatitude = angle;
	
	synchronized(newComputation)
	    {
		newParams = true;
		newComputation.boolVal = true;
		newComputation.notify();
	    }
	
    }

    private void debug_color(int x, int y)
    {
	if(x == 19 && y == 146)
	    {    
	      gl.glColor3f(0.5f, 1.0f, 0.5f);
	    }
	
    }

    private void map_color(float height, float color[])
    {

	float t = (height - COLOR_MIN_HEIGHT) / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);

	// Clamp to range, just in case
	if(t < 0.0) t = 0.0f;
	if(t > 1.0) t = 1.0f;

	/*
	// Light green through red

	if (t <= 0.33)
	    {
		color[0] = (t) / 0.33f;
		color[1] = 1.0f;
		color[2] = (0.33f - t) / 0.33f;
	    }
	else
	    {
		color[0] = 1.0f;
		color[1] = (1.0f - t) / 0.67f;
		color[2] = 0.0f;
	    }
	*/

	// Brown yellow map
	if (t < 0.5)
	    {
		color[0] = 0.488f + t * 0.836f;
		color[1] = 0.164f + t * 0.953f;
		color[2] = 0.094f + t * 0.172f;
	    }
	else 
	    {
		color[0] = 0.906f + (t - 0.5f) * 0.180f;
		color[1] = 0.640f + (t - 0.5f) * 0.680f;
		color[2] = 0.180f + (t - 0.5f) * 0.773f;
	    }

    }

    public void resetXSections()
    {
	XSectionManager.reset();
	
	// Refresh view
	synchronized(newComputation)
	    {
		newUI = true;
		newComputation.boolVal = true;
		newComputation.notify();
	    }
    }
    /*
    public int getXSectionSize(int index)
    {
	
	if(index < 0 || index >= XSectionManager.nXSections())
	    return 0;

	XSection p = XSectionManager.getXSection(index);

	int dx, dy;

	if(p.startX < p.endX)
	    dx = p.endX - p.startX;
	else
	    dx = p.startX - p.endX;

	if(p.startY < p.endY)
	    dy = p.endY - p.startY;
	else
	    dy = p.startY - p.endY;

	int value;
	if(dx > dy)
	    value = dx + 1;  // include endpoints
	else
	    value = dy + 1;

	return value;
    }
    */

    /*    
    public float [][] getXSection(int index)
    {
	
	float [][] array;
	array = new float[2][];

	if(index < 0 || index >= XSectionManager.nXSections())
	    return null;

	XSection pf = XSectionManager.getXSection(index);

	float distance;

	// Do a Brensenham-like line stepping
	// Nearest neighbor sampling

	int dx, dy, sx, sy, stepX, stepY;
	int p;

	sx = pf.endX - pf.startX;
	sy = pf.endY - pf.startY;

	if(sx < 0) dx = -sx; else dx = sx;  // dx = abs(sx)
	if(sy < 0) dy = -sy; else dy = sy;

	if(sx < 0) stepX = -1; else stepX = 1;
	if(sy < 0) stepY = -1; else stepY = 1;

	distance = (float) Math.sqrt(dx * dx + dy * dy);

	int x, y, i;

	x = pf.startX; y = pf.startY;

	if(dx > dy)
	    {
		array[0] = new float [dx+1];
		array[1] = new float [dx+1];
		// Conditional step in y
		p = 2 * dy - dx;
		for(i = 0; i < dx; i++)
		    {
			array[0][i] = ((float)i) / dx * distance
			    * gridHorizontalSpacingFactor;
			array[1][i] = topo[x][y];
			x += stepX;
			if(p < 0)
			    { p += 2 * dy; }
			else
			    { p += 2 * (dy - dx); y += stepY; }
		    }
		array[0][i] = distance * gridHorizontalSpacingFactor;
		array[1][i] = topo[x][y];
	    }
	else
	    {
		array[0] = new float [dy+1];
		array[1] = new float [dy+1];
		// Conditional step in x
		p = 2 * dx - dy;
		for(i = 0; i < dy; i++)
		    {
			array[0][i] = ((float)i) / dy * distance
			    * gridHorizontalSpacingFactor;
			array[1][i] = topo[x][y];
			y += stepY;
			if(p < 0)
			    { p += 2 * dx; }
			else
			    { p += 2 * (dx - dy); x += stepX; }
		    }
		array[0][i] = distance * gridHorizontalSpacingFactor;
		array[1][i] = topo[x][y];
	    }

	return array;
    }
    */
}