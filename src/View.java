
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.glsl.ShaderState;

import java.awt.*;
import java.awt.event.MouseEvent;

public class View implements Runnable, GLEventListener {
    public final GLCanvas canvas;
    private final GLCapabilities caps;
    private final ImmModeSink immModeSink = ImmModeSink.createFixed(3 * 3,
            3, GL.GL_FLOAT, // vertex
            3, GL.GL_FLOAT, // color
            0, GL.GL_FLOAT, // normal
            0, GL.GL_FLOAT, // texCoords
            GL.GL_STATIC_DRAW);
    private GL2 gl;

    private static float[][][] topoColor;
    private static int latticeSizeX;
    private static int latticeSizeY;

    private final CondVar newComputation;
    private boolean newData;
    private boolean newParams;
    private boolean newMode;
    private boolean newUI;
    private GLUT glut;

    // Modes
    final static public int SPIN_MODE = 0;
    final static public int PROFILE_MODE = 1;
    final static public int FLY_MODE = 2;  // Someday
    final static public int XVISUALIZER_MODE = 3;
    final static public int RIVER_PROFILE_MODE = 4;

    private int viewMode = SPIN_MODE;

    // Camera parameters
    private final float[] cameraEye = {1, 0, 0};
    private final float[] cameraAt = {0, 0, 0};
    private final float[] cameraUp = {0, 1, 0};
    private final float cameraFOV;
    private final float cameraNear;
    private final float cameraFar;
    final private float cameraRadius = 18000.0F;
    private ShaderState st;
    private GLArrayDataServer colors;

    private final float[] world2cam = new float[16];

    private final float[] lightPosition = new float[4];

    // X and Y screen extents at cameraNear distance
    // Useful for positioning UI widgets
    private float screenX;
    private float screenY;

    // Material parameters
    private final float[] backgroundColor = {1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] directLight = {0.8f, 0.8f, 0.8f, 0.8f};
    private final float[] ambientLight = {0.2f, 0.2f, 0.2f, 1.0f};

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
    private boolean repaint;

    private final float COLOR_MAX_HEIGHT = 3000.0f;
//    private final float COLOR_MIN_HEIGHT = 1200.0f;
private final float COLOR_MIN_HEIGHT = 1400.0f;

    private final float[] vertScaleSample = {1400.0f, 2000.0f, 2500.0f, 3000.0f};
    private final int nVertScaleSamples = 4;


    // Mouse state
    private boolean buttonDownFlag = false;
    private int mouseStartX, mouseStartY;
    private int mouseEndX, mouseEndY;

    private final float[] mouse2Data = new float[4];

    public XSection tempXSection = new XSection();;

    // XSection information
    final private float profileScale = 1.1f;  // For borders in profile view

    public View() {
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

        cameraEye[0] = 0.0f;
        cameraEye[1] = -cameraRadius;
        cameraEye[2] = 0.0f;
        cameraAt[0] = 0.0f;
        cameraAt[1] = 0.0f;
        cameraAt[2] = 0.0f;
        cameraUp[0] = 0.0f;
        cameraUp[1] = 0.0f;
        cameraUp[2] = 1.0f;
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
    private class ViewEventHandler extends ViewAdapter {
        @Override
        public void mousePressed(MouseEvent m) {
        /* Wilsim.i.log.append("View : button_down() (" + m.getX() + ", "
                + m.getY() + ")\n");
	    */

            mouseEndX = mouseStartX = m.getX();
            mouseEndY = mouseStartY = m.getY();

            switch (viewMode) {
                case SPIN_MODE:
                    return;
                case PROFILE_MODE:
                    tempXSection.startX =
                            (int) (mouseStartX * mouse2Data[0] + mouse2Data[1]);
                    tempXSection.startY =
                            (int) (mouseStartY * mouse2Data[2] + mouse2Data[3]);
                    // Check for bounds

                    if (tempXSection.startX < 0
                            || tempXSection.startX >= latticeSizeX)
                        return;
                    if (tempXSection.startY < 0
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
                default:
                    return;
            }

            buttonDownFlag = true;

            synchronized (newComputation) {
                newUI = true;
                newComputation.boolVal = true;
                newComputation.notify();
            }

        }

        @Override
        public void mouseReleased(MouseEvent m) {
        /* Wilsim.i.log.append("View : button_up() (" + m.getX() + ", "
	       + m.getY() + ")\n"); */

            mouseEndX = m.getX();
            mouseEndY = m.getY();

            switch (viewMode) {
                case SPIN_MODE:
                    break;
                case PROFILE_MODE:
                    if (!buttonDownFlag) return;  // Invalid start point
                    tempXSection.endX =
                            (int) (mouseEndX * mouse2Data[0] + mouse2Data[1]);
                    tempXSection.endY =
                            (int) (mouseEndY * mouse2Data[2] + mouse2Data[3]);
                    // Check for bounds
                    if (tempXSection.startX < 0
                            || tempXSection.endX >= latticeSizeX)
                        break;
                    if (tempXSection.startY < 0
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
            synchronized (newComputation) {
                newUI = true;
                newComputation.boolVal = true;
                newComputation.notify();
            }
        }

        @Override
        public void mouseDragged(MouseEvent m) {
            mouseEndX = m.getX();
            mouseEndY = m.getY();

            switch (viewMode) {
                case SPIN_MODE:
                    return;
                case PROFILE_MODE:
		    /* Wilsim.i.log.append("View : move() (" + m.getX() + ", "
						+ m.getY() + ")\n");
		    */
                    if (!buttonDownFlag) {
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

            synchronized (newComputation) {
                newUI = true;
                newComputation.boolVal = true;
                newComputation.notify();
            }
        }
    }

    public void init(final GLAutoDrawable glautodrawable) {
        // Wilsim.i.log.append("View : init()\n");
        glut = new GLUT();
        repaint = false;

        gl = glautodrawable.getGL().getGL2();
        //gl = glautodrawable.getGL().getGL2ES1();


        gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        if( !gl.hasGLSL() ) {
           /* System.err.println("No GLSL available, no rendering.");
            st = new ShaderState();
            st.setVerbose(true);
            final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader",
                    "shader/bin", "RedSquareShader", true);
            final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader",
                    "shader/bin", "RedSquareShader", true);
            vp0.defaultShaderCustomization(gl, true, true);
            fp0.defaultShaderCustomization(gl, true, true);
            final ShaderProgram sp0 = new ShaderProgram();
            sp0.add(gl, vp0, System.err);
            sp0.add(gl, fp0, System.err);
            st.attachShaderProgram(gl, sp0, true);

            // Allocate Color Array
            colors= GLArrayDataServer.createGLSL("mgl_Color", 4, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
            colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
            colors.putf(0); colors.putf(0); colors.putf(1); colors.putf(1);
            colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
            colors.putf(1); colors.putf(0); colors.putf(0); colors.putf(1);
            colors.seal(gl, true);
            st.ownAttribute(colors, true);
            colors.enableBuffer(gl, false);*/
        }
        else
            System.err.println("GLSL available");


        float matDiffuse[] = {0.8f, 0.8f, 0.8f, 1.0f};
        float matSpecular[] = {0.01f, 0.01f, 0.01f, 1.0f};
        float matShininess[] = {30.0f, 30.0f, 30.0f, 1.0f};

        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl.glMaterialfv(GL2.GL_FRONT, GLLightingFunc.GL_DIFFUSE,
                matDiffuse, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GLLightingFunc.GL_SPECULAR,
                matSpecular, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GLLightingFunc.GL_SHININESS,
                matShininess, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE,
                directLight, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR,
                directLight, 0);

        //gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
        gl.glLightModelf(GL2.GL_LIGHT_MODEL_TWO_SIDE, 1f);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT,
                ambientLight, 0);

        gl.glEnable(GLLightingFunc.GL_NORMALIZE);
        //	gl.glEnable(gl.GL_LIGHTING);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glEnable(GLLightingFunc.GL_LIGHT0);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        // Set up text rendering capability
        vertScaleTextEngine = new TextRenderer(new Font("SansSerif", Font.PLAIN, 14));
        scoreTextEngine = new TextRenderer(new Font("SanSerif", Font.PLAIN, 28));
        CompassTextEngine = new TextRenderer(new Font("SanSerif", Font.PLAIN, 20));

        // Initialize XSection Manager
        XSectionManager.init();
    }

    public void initModel() {
        // Wilsim.i.log.append("View: initModel()\n");
        latticeSizeX = Model.lattice_size_x;
        latticeSizeY = Model.lattice_size_y;
        //topoColor = new float[latticeSizeX + 1][latticeSizeY + 1][3];
        /*String str = latticeSizeX + "x" + latticeSizeY;*/
        // Wilsim.i.log.append("View: initModel(): " + str + '\n');
        newComputation.boolVal = false;
    }

    public void loadModel(float[][] array) {
        // System.out.println("View: loadModel()\n");
// Don't coppy an array in a for loop. Very slow!
/*	for(int i = 0; i < array.length; i++)
	    for(int j = 0; j < array[i].length; j++)
		topo[i][j] = array[i][j];*/

        //topo = array.clone(); // <== this should be 4x faster




        //for(int j = 0; j <= latticeSizeX; j++)
        //System.arraycopy(array[j], 0, topo[j], 0, array[0].length); //even faster





 /*       for (int j = 1; j <= Model.lattice_size_y; j++) {
            for (int i = 1; i <= Model.lattice_size_x; i++) {
                float t = (topo[i][j] - COLOR_MIN_HEIGHT) / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);

                // Clamp to range, just in case
                if (t < 0.0) t = 0.0f;
                if (t > 1.0) t = 1.0f;
                if (t < 0.5 *//*&& t > 0.01*//*) {
                    topoColor[i][j][0] = 0.488f + t * 0.836f;
                    topoColor[i][j][1] = 0.164f + t * 0.953f;
                    topoColor[i][j][2] = 0.094f + t * 0.172f;
                }
                else {
                    topoColor[i][j][0] = 0.906f + (t - 0.5f) * 0.180f;
                    topoColor[i][j][1] = 0.640f + (t - 0.5f) * 0.680f;
                    topoColor[i][j][2] = 0.180f + (t - 0.5f) * 0.773f;
                }
            }
            }*/

/*        synchronized (newComputation) {
            newData = true;
            newComputation.boolVal = true;
            newComputation.notify();
        }*/
    }

    public void newComp(){
        synchronized (newComputation) {
            newData = true;
            newComputation.boolVal = true;
            newComputation.notify();
        }
    }

    public void changeViewMode(int v) {
        synchronized (newComputation) {
            switch (v) {
                case SPIN_MODE:
                    viewMode = SPIN_MODE;
                    break;
                case PROFILE_MODE:
                    viewMode = PROFILE_MODE;
                    break;
                case XVISUALIZER_MODE:
                    viewMode = XVISUALIZER_MODE;
                    break;
                case RIVER_PROFILE_MODE:
                    viewMode = RIVER_PROFILE_MODE;
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
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // System.out.println("View : display(drawable)\n");
        draw(drawable);

    }

    private void drawprofileMode() {
        drawXSectionMode();
        gl.glEnable(GLLightingFunc.GL_LIGHTING);

        // Position lights
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

        // Recenter and resposition grid
        // gl.glRotatef(180f, 1.0f, 1.0f, .0f);
        gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor +2, 1.0f);
        gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
        // Z translation is currently a hack based on the grid.  Roughly 1800 m for
        // the Grand Canyon

        drawTerrain();

        gl.glDisable(GLLightingFunc.GL_LIGHTING);

        drawXSections();

        // Now draw UI stuff on top
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawUI();

    }


    private void draw(GLAutoDrawable draw) {
        // System.out.println("View: draw()\n");

        gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3f(0.8f, 0.8f, 0.8f);

        // Move more and more into separate drawing modes
        // as functionality diverges

        if (viewMode == PROFILE_MODE) drawprofileMode();

        else if (viewMode == SPIN_MODE) drawSpinMode();

        else if (viewMode == RIVER_PROFILE_MODE) drawRP();

        else drawXVISUALIZER_MODE();


        synchronized (newComputation) {
            newData = false;
            newComputation.boolVal = false; // ?? Should this be elsewhere?
        }
        gl.glFlush();
    }

    private void drawSpinMode() {
        // Move more and more in here as draw mode methods diverge
        if (newParams || newMode) {
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

        // Spin mode

        gl.glEnable(GLLightingFunc.GL_LIGHTING);

        // Position lights
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);



        // Recenter and resposition grid
        // gl.glRotatef(180f, 1.0f, 1.0f, .0f);
        gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
        gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
        // Z translation is currently a hack based on the grid.  Roughly 1800 m for
        // the Grand Canyon


        drawTerrain();



        gl.glDisable(GLLightingFunc.GL_LIGHTING);

        drawXSections();


        // Now draw UI stuff on top
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawVertScale();
        drawUI();
        immModeSink.draw(gl, true);



    }

    private void drawXSectionMode() {
        // Move more and more in here as draw mode methods diverge

        if (newParams || newMode) {
            float aspect = ((float) canvas.getWidth()) / canvas.getHeight();
            float data_aspect = ((float) latticeSizeX + 1) / (latticeSizeY + 1);

            if (aspect > data_aspect) {
                // Wide screen
                screenY = ((float) latticeSizeY + 1) / 2.0f
                        * gridHorizontalScaleFactor * profileScale;
                screenX = screenY * aspect;
            } else {
                // Tall screen
                screenX = ((float) latticeSizeX + 1) / 2.0f
                        * gridHorizontalScaleFactor * profileScale;
                screenY = screenX / aspect;
            }
            computeXSectionView();
            newParams = true;
            newMode = true;
        }
        drawVertScale();

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

    private void drawXVISUALIZER_MODE() {
        gl.glEnable(GLLightingFunc.GL_LIGHTING);

        // Position lights
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

        // Recenter and resposition grid
        // gl.glRotatef(180f, 1.0f, 1.0f, .0f);
        gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
        gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
        // Z translation is currently a hack based on the grid.  Roughly 1800 m for
        // the Grand Canyon

        //drawTerrain();


        gl.glDisable(GLLightingFunc.GL_LIGHTING);

        //drawXSections();

        // Now draw UI stuff on top
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawScaleBar();
        drawXSectionVertScale();

        drawUI();

    }

    private void drawRP() {
        gl.glEnable(GLLightingFunc.GL_LIGHTING);

        // Position lights
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

        // Recenter and resposition grid
        // gl.glRotatef(180f, 1.0f, 1.0f, .0f);
        gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
        gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
        // Z translation is currently a hack based on the grid.  Roughly 1800 m for
        // the Grand Canyon

        //drawTerrain();


        gl.glDisable(GLLightingFunc.GL_LIGHTING);

        drawXSections();

        // Now draw UI stuff on top
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawRIVER_PROFILE_MODE();

        drawUI();

    }

    private void drawRIVER_PROFILE_MODE() {
        gl.glEnable(GLLightingFunc.GL_LIGHTING);

        // Position lights
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

        // Recenter and resposition grid
        // gl.glRotatef(180f, 1.0f, 1.0f, .0f);
        gl.glScalef(gridHorizontalScaleFactor, -gridHorizontalScaleFactor, 1.0f);
        gl.glTranslatef(-latticeSizeX / 2, -latticeSizeY / 2, -1800);
        // Z translation is currently a hack based on the grid.  Roughly 1800 m for
        // the Grand Canyon

        //drawTerrain();


        gl.glDisable(GLLightingFunc.GL_LIGHTING);

        drawXSections();

        // Now draw UI stuff on top
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0f, canvas.getWidth(), 0.0f, canvas.getHeight(), 1.0f, 2.0f);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        drawProfileView();
        drawRiverProfileVertScale();

        drawUI();

    }

    private void drawXSections() {

        int n = XSectionManager.nXSections();

        // Wilsim.i.log.append("View : drawXSections() : " + n + "\n");

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBegin(GL2GL3.GL_QUADS);
        for (int i = 0; i < n; i++) {
            XSection p = XSectionManager.getXSection(i);

            gl.glColor4f(0.6f, 0.5f, 0.5f, 0.6f);
            gl.glVertex3f(p.startX, p.startY, COLOR_MIN_HEIGHT);
            gl.glVertex3f(p.endX, p.endY, COLOR_MIN_HEIGHT);
            gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);
            gl.glVertex3f(p.startX, p.startY, COLOR_MAX_HEIGHT);
        }
        gl.glEnd();
        gl.glDisable(GL.GL_BLEND);

        // Draw top highlights
        gl.glLineWidth(3.0f);
        gl.glBegin(GL.GL_LINES);
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        for (int i = 0; i < n; i++) {
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
            dbarb = Math.min(dbarb, 15.0f);  // but no greater than 26.
            dbarb = Math.max(dbarb, 2.0f); // but no smaller than 3

            dx = dx * dbarb / distance;
            dy = dy * dbarb / distance;
            gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);
            gl.glVertex3f(p.endX - 2.0f * dx + dy, p.endY - dx - 2.0f * dy, COLOR_MAX_HEIGHT);
            gl.glVertex3f(p.endX, p.endY, COLOR_MAX_HEIGHT);
            gl.glVertex3f(p.endX - 2.0f * dx - dy, p.endY + dx - 2.0f * dy, COLOR_MAX_HEIGHT);


            gl.glEnd();
            vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
            vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
            gl.glMatrixMode(5888);
            gl.glPushMatrix();
/*            vertScaleTextEngine.draw("tempXSection.startX= " + Integer.toString(p.startX), 10, 12);
            vertScaleTextEngine.draw("tempXSection.startY= " + Integer.toString(p.startY), 10, 0);

            vertScaleTextEngine.draw("tempXSection.endX= " + Integer.toString(p.endX), 300, 12);
            vertScaleTextEngine.draw("tempXSection.endY= " + Integer.toString(p.endY), 300, 0);*/

            vertScaleTextEngine.endRendering();
        }
        gl.glEnd();


        gl.glLineWidth(1.0f);
    }
/*
    private void drawTerrain2() {
        // Need a separate vertex normal for each quad -- this implementation is inefficient
        // Should be computed once and stored when grid is loaded

        float[] v1 = new float[3];
        float[] v2 = new float[3];
        float[] norm = new float[3];
        float[] color = new float[4];

        v1[0] = gridHorizontalScaleFactor / 5.0f;  // Scale factor is a hack
        v1[1] = 0.0f;

        v2[0] = 0.0f;
        v2[1] = gridHorizontalScaleFactor / 5.0f;  // Ditto on hack

        // Get ready for varying material properties
        gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK,
                GLLightingFunc.GL_AMBIENT_AND_DIFFUSE);
        int interp = 2;

        float mux = (2 % interp) * (1 / (float) (interp - 1));
        float mux2 = (float) (1 - Math.cos(mux * Math.PI)) / 2;

        for (int x = 2; x < (latticeSizeX - 2) * interp; x++) {



            float muxb = ((x + 1) % interp) * (1 / (float) (interp - 1));
            float muxb2 = (float) (1 - Math.cos(muxb * Math.PI)) / 2;


            for (int y = 2; y < (latticeSizeY - 2) * interp; y++) {

                float answerx = (
                        topo[x / interp][y / interp] * (1 - mux2) + topo[(x / interp) + 1][(y / interp)] * mux2);
                float answerx2 = (
                        topo[x / interp][y / interp + 1] * (1 - mux2) + topo[(x / interp) + 1][(y / interp) + 1] * mux2);

                float answerxb = (
                        topo[x / interp][y / interp] * (1 - muxb2) + topo[(x / interp) + 1][(y / interp)] * muxb2);

                float answerxb2 = (
                        topo[x / interp][y / interp + 1] * (1 - muxb2) + topo[(x / interp) + 1][(y / interp) + 1] * muxb2);

                float muy = (y % interp) * (1 / (float) (interp - 1));
                float muy2 = (float) (1 - Math.cos(muy * Math.PI)) / 2;

                float ybetweenx0x1 = (
                        answerx * (1 - muy2) + answerx2 * muy2);
                float ybetweenx1x2 = (
                        answerxb * (1 - muy2) + answerxb2 * muy2);

                float muyb = ((y + 1) % interp) * (1 / (float) (interp - 1));
                float muyb2 = (float) (1 - Math.cos(muyb * Math.PI)) / 2;

                float y1betweenx0x1 = (
                        answerx * (1 - muyb2) + answerx2 * muyb2);

                float y1betweenx1x2 = (
                        answerxb * (1 - muyb2) + answerxb2 * muyb2);




                gl.glBegin(GL.GL_TRIANGLE_STRIP);
                v1[2] = topo[(x / interp) + 1][(y / interp)] - topo[(x / interp) - 1][(y / interp)];
                v2[2] = topo[(x / interp)][(y / interp) + 1] - topo[(x / interp)][(y / interp) - 1];

                cross(norm, v2, v1);

                gl.glNormal3fv(norm, 0);

                map_color(ybetweenx0x1, color);
                gl.glColor3f(color[0], color[1], color[2]);
                gl.glVertex3f(x / interp + mux, y / interp + muy, ybetweenx0x1);

                map_color(ybetweenx1x2, color);
                gl.glColor3f(color[0], color[1], color[2]);
                gl.glVertex3f(x / interp + mux + mux, y / interp + muy, ybetweenx1x2);

                map_color(y1betweenx0x1, color);
                gl.glColor3f(color[0], color[1], color[2]);
                gl.glVertex3f(x / interp + mux, y / interp + muy + muy, y1betweenx0x1);

*//*                v1[2] = topo[(x / interp) + 2][(y / interp) + 1] - topo[x / interp][y / interp + 1];
                v2[2] = topo[(x / interp) + 1][(y / interp) + 2] - topo[(x / interp) + 1][y / interp];

                cross(norm, v2, v1);*//*

                map_color(y1betweenx1x2, color);
                gl.glColor3f(color[0], color[1], color[2]);
                gl.glVertex3f(x / interp + mux + mux, y / interp + muy + muy, y1betweenx1x2);


                gl.glEnd();


            }
            mux = muxb;
            mux2 = muxb2;
        }
        float km = 13.888888888f;
        int offset = 2;
        int verty = 270;





*//*        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(0.0f, 0.0f, 0.0f);

        gl.glVertex3f(offset, 271, 1800.0f);
        gl.glVertex3f(km * 5 + offset, 271, 1800.0f);
        gl.glVertex3f(offset, 261, 1800.0f);

        gl.glVertex3f(offset, 261, 1800.0f);
        gl.glVertex3f(km * 5 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 5 + offset, 261, 1800.0f);


        gl.glColor3f(1.75f, 1.75f, 1.75f);
        gl.glVertex3f(km * 5 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 7 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 5 + offset, 261, 1800.0f);

        gl.glVertex3f(km * 5 + offset, 261, 1800.0f);
        gl.glVertex3f(km * 7 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 7 + offset, 261, 1800.0f);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glVertex3f(km * 7 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 8 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 7 + offset, 261, 1800.0f);

        gl.glVertex3f(km * 7 + offset, 261, 1800.0f);
        gl.glVertex3f(km * 8 + offset, 271, 1800.0f);
        gl.glVertex3f(km * 8 + offset, 261, 1800.0f);

        gl.glEnd();*//*

        gl.glLineWidth(1.0f);
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(offset, verty - 5.0f, 1820.0f);
        gl.glVertex3f(km * 10 + offset, verty - 5.0f, 1820.0f);
        gl.glEnd();

        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        //gl.glColor4f(0.0f, 0.0f, 0.0f, .5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(offset, verty - 10.0f, 1820.0f);
        //System.out.println("Line 50 * drawScaleX " + (50 * drawScaleX));
        gl.glVertex3f(km * 2.5f + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 2.5f + offset, verty - 10.0f, 1820.0f);
        //System.out.println("Line 20050 * drawScaleX " + (20050 * drawScaleX));
        gl.glVertex3f(km * 5 + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 5 + offset, verty - 10.0f, 1820.0f);
        //System.out.println("Line 30050 * drawScaleX " + (30050 * drawScaleX));
        gl.glVertex3f(km * 10 + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 10 + offset, verty - 10.0f, 1820.0f);

*//*        gl.glVertex3f(km * 15 + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 15 + offset, verty - 10.0f, 1820.0f);*//*
        //System.out.println("Line 40050 * drawScaleX " + (40050 * drawScaleX));


        gl.glEnd();


        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());

*//*        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) " , 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) " , 5, 5);*//*
*//*        if (viewMode == SPIN_MODE) {*//*
        vertScaleTextEngine.draw("0 km ", 5, 25);
        vertScaleTextEngine.draw("25 km ", 55, 25);
        vertScaleTextEngine.draw("50 km ", 105, 25);
        vertScaleTextEngine.draw("100 km ", 205, 25);
//        }
 *//*       else {*//*
*//*            vertScaleTextEngine.draw("0 km ", 50, 25);
            vertScaleTextEngine.draw("25 km ", 155, 25);
            vertScaleTextEngine.draw("50 km ", 205, 25);
            vertScaleTextEngine.draw("100 km ", 305, 25);
            vertScaleTextEngine.draw("200 km ", 515, 25);*//*

        //       }
        vertScaleTextEngine.endRendering();
        gl.glDisable(GLLightingFunc.GL_COLOR_MATERIAL);
    }*/


    private void drawTerrainasdf()
    {
        float [] v1 = new float[3];
        float [] v2 = new float[3];
        float [] norm = new float[3];


        v1[0] = 30.0f / 5.0f;  // Scale factor is a hack
        v1[1] = 0.0f;

        v2[0] = 0.0f;
        v2[1] = 30.0f / 5.0f;  // Ditto on hack

        // Get ready for varying material properties
        gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK,
                GLLightingFunc.GL_AMBIENT_AND_DIFFUSE);


        //gl.glBegin(GL.GL_TRIANGLE_STRIP);
        immModeSink.glBegin(GL.GL_TRIANGLE_STRIP);


        for(int x = 1; x < latticeSizeX; x++) {

            for(int y = 1; y < latticeSizeY; y++)
            {

                v1[2] = Model.topo1dim[x+1 + y * latticeSizeX] - Model.topo1dim[x-1 + y * latticeSizeX];
                v2[2] = Model.topo1dim[x + (y+1) * latticeSizeX] - Model.topo1dim[x + (y-1) * latticeSizeX];

                cross(norm, v2, v1);
                immModeSink.glNormal3f(0, 0, 0);


                immModeSink.glColor3f(Model.vert_color2[x + y * Model.lattice_size_x].x, Model.vert_color2[(x + y * Model.lattice_size_x)].y, Model.vert_color2[(x + y * Model.lattice_size_x)].z);
                immModeSink.glVertex3f(x, y, Model.topo1dim[x + y * latticeSizeX]);
                immModeSink.glColor3f(Model.vert_color2[x + y * Model.lattice_size_x].x, Model.vert_color2[(x + y * Model.lattice_size_x)].y, Model.vert_color2[(x + y * Model.lattice_size_x)].z);
                immModeSink.glVertex3f(x + 1, y, Model.topo1dim[(x + 1) + y * latticeSizeX]);
            }

            //degenerate triangles

            //eff
            immModeSink.glVertex3f(x, latticeSizeY, Model.topo1dim[x + latticeSizeY * latticeSizeX]);
            immModeSink.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            immModeSink.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);

            //FFG
            immModeSink.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            immModeSink.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);

            //FGG
            immModeSink.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);

            //GGH
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            immModeSink.glVertex3f(x + 1, 1, Model.topo1dim[(x + 1) + latticeSizeX]);

            //GHG
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            immModeSink.glVertex3f(x + 1, 1, Model.topo1dim[(x + 1) + latticeSizeX]);
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);

            //GGH
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            immModeSink.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            immModeSink.glVertex3f(x + 1, 1, Model.topo1dim[(x + 1) + latticeSizeX]);
        }

        immModeSink.glEnd(gl);


    }

    private void drawTerrain()
    {
        // Need a separate vertex normal for each quad -- this implementation is inefficient
        // Should be computed once and stored when grid is loaded

        float [] v1 = new float[3];
        float [] v2 = new float[3];
        float [] norm = new float[3];


        v1[0] = 30.0f / 5.0f;  // Scale factor is a hack
        v1[1] = 0.0f;

        v2[0] = 0.0f;
        v2[1] = 30.0f / 5.0f;  // Ditto on hack

        // Get ready for varying material properties
        gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK,
                GLLightingFunc.GL_AMBIENT_AND_DIFFUSE);

//degenerate triangles info
        //http://www.gamedev.net/page/resources/_/technical/graphics-programming-and-theory/concatenating-triangle-strips-r1871
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        //immModeSink.glBegin(GL.GL_TRIANGLE_STRIP);


        for(int x = 1; x < latticeSizeX; x++) {

            for(int y = 1; y < latticeSizeY; y++)
            {
                v1[2] = Model.topo1dim[x+1 + y * latticeSizeX] - Model.topo1dim[x-1 + y * latticeSizeX];
                v2[2] = Model.topo1dim[x + (y+1) * latticeSizeX] - Model.topo1dim[x + (y-1) * latticeSizeX];
                cross(norm, v2, v1);
                gl.glNormal3fv(norm, 0);
                gl.glColor3f(Model.vert_color2[x + y * Model.lattice_size_x].x, Model.vert_color2[(x + y * Model.lattice_size_x)].y, Model.vert_color2[(x + y * Model.lattice_size_x)].z);
                gl.glVertex3f(x, y, Model.topo1dim[x + y * latticeSizeX]);
                gl.glColor3f(Model.vert_color2[x + y * Model.lattice_size_x].x, Model.vert_color2[(x + y * Model.lattice_size_x)].y, Model.vert_color2[(x + y * Model.lattice_size_x)].z);
                gl.glVertex3f(x + 1, y, Model.topo1dim[(x + 1) + y * latticeSizeX]);
            }

            //degenerate triangles

            //eff
            gl.glVertex3f(x, latticeSizeY, Model.topo1dim[x + latticeSizeY * latticeSizeX]);
            gl.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            gl.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);

            //FFG
            gl.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            gl.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);

            //FGG
            gl.glVertex3f(x + 1, latticeSizeY, Model.topo1dim[(x + 1) + latticeSizeY * latticeSizeX]);
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);

            //GGH
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            gl.glVertex3f(x + 1, 1, Model.topo1dim[(x + 1) + latticeSizeX]);

            //GHG
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            gl.glVertex3f(x + 1, 1, Model.topo1dim[(x + 1) + latticeSizeX]);
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);

            //GGH
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            gl.glVertex3f(x, 1, Model.topo1dim[x + latticeSizeX]);
            gl.glVertex3f(x + 1, 1, Model.topo1dim[(x + 1) + latticeSizeX]);
        }

        gl.glEnd();

        float km = 13.888888888f;
        int offset = 1;
        int verty = 270;

        gl.glLineWidth(1.0f);
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(offset, verty - 5.0f, 1820.0f);
        gl.glVertex3f(km * 10 + offset, verty - 5.0f, 1820.0f);
        gl.glEnd();

        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        //gl.glColor4f(0.0f, 0.0f, 0.0f, .5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(offset, verty - 10.0f, 1820.0f);
        //System.out.println("Line 50 * drawScaleX " + (50 * drawScaleX));
        gl.glVertex3f(km * 2.5f + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 2.5f + offset, verty - 10.0f, 1820.0f);
        //System.out.println("Line 20050 * drawScaleX " + (20050 * drawScaleX));
        gl.glVertex3f(km * 5 + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 5 + offset, verty - 10.0f, 1820.0f);
        //System.out.println("Line 30050 * drawScaleX " + (30050 * drawScaleX));
        gl.glVertex3f(km * 10 + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 10 + offset, verty - 10.0f, 1820.0f);

 /*       gl.glVertex3f(km * 15 + offset, verty + 0.0f, 1820.0f);
        gl.glVertex3f(km * 15 + offset, verty - 10.0f, 1820.0f);*/
        //System.out.println("Line 40050 * drawScaleX " + (40050 * drawScaleX));


        gl.glEnd();


        gl.glColor3f(0f, 0f, 0f);
        gl.glLineWidth(2.0f);
        gl.glPushMatrix();
        gl.glTranslatef(-10f, 270 + 10f, 1820.f);
        gl.glScalef(0.07f, -0.07f, 0.0f);
        renderStrokeString(GLUT.STROKE_MONO_ROMAN, "0km");
        gl.glTranslatef(130f, 270 - 275f, 1820.f);
        renderStrokeString(GLUT.STROKE_MONO_ROMAN, "25km 50km    100km");
gl.glPopMatrix();




  /*      vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());

*//*        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) " , 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) " , 5, 5);*//*
*//*        if (viewMode == SPIN_MODE) {*//*
        vertScaleTextEngine.draw("0 km ", 5, 25);
        vertScaleTextEngine.draw("25 km ", 55, 25);
        vertScaleTextEngine.draw("50 km ", 105, 25);
        vertScaleTextEngine.draw("100 km ", 205, 25);
//        }
 *//*       else {*//*
*//*            vertScaleTextEngine.draw("0 km ", 50, 25);
            vertScaleTextEngine.draw("25 km ", 155, 25);
            vertScaleTextEngine.draw("50 km ", 205, 25);
            vertScaleTextEngine.draw("100 km ", 305, 25);
            vertScaleTextEngine.draw("200 km ", 515, 25);*//*

        //       }
        vertScaleTextEngine.endRendering();
        gl.glDisable(GLLightingFunc.GL_COLOR_MATERIAL);*/
    }


    private void drawUI() {
        if (viewMode == PROFILE_MODE && buttonDownFlag) {
		/*
		Wilsim.i.log.append("View : drawUI() (" + mouseStartX + ", "
				    + mouseStartY + ")-("
				    + mouseEndX + ", "
				    + mouseEndY + ")\n");
		*/
            // Draw mouse line
            gl.glLineWidth(3.0f);
            gl.glBegin(GL.GL_LINES);
            gl.glColor3f(0.0f, 0.0f, 0.0f);
            gl.glVertex3f(mouseStartX,
                    canvas.getHeight() - mouseStartY, -1.2f);
            gl.glVertex3f(mouseEndX,
                    canvas.getHeight() - mouseEndY, -1.2f);
            gl.glEnd();
            gl.glLineWidth(1.0f);
        }
        newUI = false;

        if (viewMode == SPIN_MODE) drawCompass();

        if (viewMode == SPIN_MODE && Wilsim.m.scoreFlag)
            //drawScore();   // Temporarily removed until sane values emerge
            ;
    }

    private void drawVertScale() {
        // Figure out size, location
        // Maybe this should be done only once and recomputed when the shape changes
        // Size is expandable up to a point.  Fraction of screen until screen gets so large.
        // For now min( 300 pixels high, height of screen)
        // Use lower left origin pixel coordinate system

        float scaleHeight = canvas.getHeight();
        if (scaleHeight > 300.0f) scaleHeight = 300.0f;

        float scaleBorder = 0.03f; // Fraction of screen height
        scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight; // Give a little border
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
        float[] color = new float[3];

        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= 12; i++) {
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
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3f(scalePosX, scalePosY, -1.5f);
        gl.glVertex3f(scalePosX + scaleWidth, scalePosY, -1.5f);
        gl.glVertex3f(scalePosX + scaleWidth, scalePosY + scaleHeight, -1.5f);
        gl.glVertex3f(scalePosX, scalePosY + scaleHeight, -1.5f);
        gl.glEnd();

        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL.GL_LINES);
        for (int i = 0; i < nVertScaleSamples; i++) {
            float y;
            y = (vertScaleSample[i] - COLOR_MIN_HEIGHT)
                    / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);
            y = y * scaleHeight + scalePosY;

            gl.glVertex3f(scalePosX + scaleWidth, y, -1.5f);
            gl.glVertex3f(scalePosX + scaleWidth + 5.0f, y, -1.5f);
        }
        gl.glEnd();
        float km = 13.888888888f;
        km *= 1.5;
        int offset = 10;
/*
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(0.0f, 0.0f, 0.0f);
*/

        int verty = 15;

/*
        gl.glVertex3f(offset, verty, -1.5f);
        gl.glVertex3f(km * 5 + offset, verty, -1.5f);
        gl.glVertex3f(offset, verty - 15, -1.5f);

        gl.glVertex3f(offset, verty - 15, -1.5f);
        gl.glVertex3f(km * 5 + offset, verty, -1.5f);
        gl.glVertex3f(km * 5 + offset, verty - 15, -1.5f);

        gl.glColor3f(.75f, .75f, .75f);
        gl.glVertex3f(km * 5 + offset, verty, -1.5f);
        gl.glVertex3f(km * 7 + offset, verty, -1.5f);
        gl.glVertex3f(km * 5 + offset, verty - 15, -1.5f);

        gl.glVertex3f(km * 5 + offset, verty - 15, -1.5f);
        gl.glVertex3f(km * 7 + offset, verty, -1.5f);
        gl.glVertex3f(km * 7 + offset, verty - 15, -1.5f);


        gl.glColor3f(.5f, .5f, .5f);
        gl.glVertex3f(km * 7 + offset, verty, -1.5f);
        gl.glVertex3f(km * 8 + offset, verty, -1.5f);
        gl.glVertex3f(km * 7 + offset, verty - 15, -1.5f);

        gl.glVertex3f(km * 7 + offset, verty - 15, -1.5f);
        gl.glVertex3f(km * 8 + offset, verty, -1.5f);
        gl.glVertex3f(km * 8 + offset, verty - 15, -1.5f);
        gl.glEnd();



        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(50 + (scalePosX * 5), scalePosY + 5.0f, -1.5f);
        gl.glVertex3f(100 + (40000 * drawScaleX), scalePosY + 5.0f, -1.5f);
        gl.glEnd();*/

/*        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(offset, verty - 5.0f, -1.5f);
        gl.glVertex3f(km * 10 + offset, verty - 5.0f, -1.5f);
        gl.glEnd();*/

/*
        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        //gl.glColor4f(0.0f, 0.0f, 0.0f, .5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(offset, verty + 0.0f, -1.5f);
        gl.glVertex3f(offset, verty - 10.0f, -1.5f);
        //System.out.println("Line 50 * drawScaleX " + (50 * drawScaleX));
        gl.glVertex3f(km * 2.5f + offset, verty + 0.0f, -1.5f);
        gl.glVertex3f(km * 2.5f + offset, verty - 10.0f, -1.5f);
        //System.out.println("Line 20050 * drawScaleX " + (20050 * drawScaleX));
        gl.glVertex3f(km * 5 + offset, verty + 0.0f, -1.5f);
        gl.glVertex3f(km * 5 + offset, verty - 10.0f, -1.5f);
        //System.out.println("Line 30050 * drawScaleX " + (30050 * drawScaleX));
        gl.glVertex3f(km * 10 + offset, verty + 0.0f, -1.5f);
        gl.glVertex3f(km * 10 + offset, verty - 10.0f, -1.5f);
*/

        //System.out.println("Line 40050 * drawScaleX " + (40050 * drawScaleX));


       // gl.glEnd();
        // Draw labels

        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);

        float textPosX, textPosY;
        textPosX = scalePosX + scaleWidth + 10.0f;

        for (int i = 0; i < nVertScaleSamples; i++) {
            textPosY = scalePosY +
                    ((vertScaleSample[i] - COLOR_MIN_HEIGHT)
                            / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT))
                            * scaleHeight - 5.0f;

            String Label = Integer.toString((int) vertScaleSample[i]) + " m";
            vertScaleTextEngine.draw(Label, (int) textPosX, (int) textPosY);
        }

        vertScaleTextEngine.endRendering();
    }
    private void renderStrokeString(int font, String string) {
        // Center Our Text On The Screen
        float width = glut.glutStrokeLength(font, string);
        //gl.glTranslatef(width / 40f, 0, 0);
        // Render The Text
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            glut.glutStrokeCharacter(font, c);
        }

    }
    private void drawScaleBar() {
        gl.glClearColor(.5f, .5f, .5f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);


        float scaleHeight = canvas.getHeight();
        if (scaleHeight > 300.0f) scaleHeight = 300.0f;


        float scaleBorder = 0.03f; // Fraction of screen height
        scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight; // Give a little border
        float scaleWidth = 0.05f * scaleHeight; // Seems like a good ratio
        float scalePosX;
        float scalePosY;
        //	scalePosX = canvas.getHeight() * scaleBorder;
        //	scalePosY = canvas.getHeight() * (1.0f - scaleBorder) - scaleHeight;
        scalePosX = 10.0f;
        scalePosY = 320.0f - scaleHeight;

        float textPosX, textPosY;


        // Calculate divisions and division locations
        // Hardwired for now

        float drawScaleX = 0.02f; // defaults
        float drawScaleY = 0.23871753f;
        drawScaleY = 0.1763f;
        float saturation = 1.0f; //saturation
        float brightness = 1.0f; //brightness



//for (int x = XSectionManager.getXSection(0).getNIterates(); x < XSectionManager.getXSection(0).getMaxNIterates(); x++) {


       // gl.glEnd();





        if (XSectionManager.getXSection(0) != null && XSectionManager.getXSection(0).getNIterates() > 0) {
            double scalef = /*.00109 * */((double) (canvas.getWidth() - 75) / ((XSectionManager.getXSection(0).values[0][(XSectionManager.getXSection(0).values[0].length) -1]) *1.005));
            drawScaleX = (float) scalef;
            for (int x = 1; x <= XSectionManager.getXSection(0).getNIterates(); x++) {

                Color myRGBColor = Color.getHSBColor((float) (x - 1) / (Wilsim.m.storageIntervals + 1), saturation, brightness);

                gl.glEnable(GL.GL_LINE_SMOOTH);
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glColor3f(myRGBColor.getRed() / 255.0f, myRGBColor.getGreen() / 255.0f, myRGBColor.getBlue() / 255.0f);
                gl.glLineWidth(3.5f);



                if (XSectionManager.getXSection(0).values[x] != null) {
                    for (int i = 0; i < (XSectionManager.getXSection(0).values[x].length - 1) * 5; i++) {

                        /*5x cosine interpolation*/
                        double mu = (i % 5) * .25;

                        double mu2 = (1 - Math.cos(mu * Math.PI)) / 2;
                        double answer = (XSectionManager.getXSection(0).values[x][i / 5] * (1 - mu2) +
                                XSectionManager.getXSection(0).values[x][(i / 5) + 1] * mu2);

                        gl.glVertex3f((XSectionManager.getXSection(0).values[0][i / 5] * drawScaleX + (720 * (float) mu) * drawScaleX) + 75,
                                (float) answer * drawScaleY + 87,
                                -1.55f + (x * .001f));
                    }
                }
                gl.glEnd();
            }
        }

//}

        //System.out.println(canvas.getHeight());


        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(25 + (scalePosX * 5), scalePosY + 5.0f, -1.5f);
        gl.glVertex3f(100 + (4100000 * drawScaleX), scalePosY + 5.0f, -1.5f);
        gl.glEnd();


        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        //gl.glColor4f(0.0f, 0.0f, 0.0f, .5f);
        gl.glBegin(GL2.GL_LINES);

        gl.glVertex3f(75, scalePosY + 0.0f, -1.5f);
        gl.glVertex3f(75, scalePosY + 2000.0f, -1.5f);

        for (int x = 0; x < 41; x++) {
            gl.glVertex3f(75 + x * (10000 * drawScaleX), scalePosY + 0.0f, -1.5f);
            if (Wilsim.c.isVGridBool() == true){
                gl.glVertex3f(75 + x * (10000 * drawScaleX), 5000f, -1.5f);
                lastGridV = false;
            }
            else if(Wilsim.c.isVGridBool() == false) {
                gl.glVertex3f(75 + x * (10000 * drawScaleX), scalePosY + 10.0f, -1.5f);
                lastGridV = true;
            }
        }

        gl.glEnd();

        // Draw labels

        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());

/*        gl.glMatrixMode(5888);
        gl.glPushMatrix();*/

/*        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);*/

        // vertScaleTextEngine.draw("tempXSection.startX= " + Integer.toString(tempXSection.startX), 10, 10);
        // vertScaleTextEngine.draw("tempXSection.startY= " + Integer.toString( tempXSection.startY), 10, 0);
        // gl.glRotatef(270, 0, 0, 1);
        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);


        int total = 0;
        textPosX = 0;
        String Label = Integer.toString(total);
        vertScaleTextEngine.draw(Label + " km", (int) textPosX + 68, (int) 25.0f);
        vertScaleTextEngine.draw("Distance", (canvas.getWidth() / 2) - 25, 6);

        for (int i = 1; i < 41; i++) {
            textPosX = 10000 * i * drawScaleX;
            total += 10;
            Label = Integer.toString(total);

            vertScaleTextEngine.draw(Label + " km", (int) textPosX + 68, (int) 25.0f);
        }

        vertScaleTextEngine.endRendering();


        if (lastGridV != Wilsim.c.isVGridBool()){
            canvas.repaint();
        }
    }

    private boolean lastGridH = false;
    private boolean lastGridV = false;

    private void drawXSectionVertScale() {
        // Figure out size, location
        // Maybe this should be done only once and recomputed when the shape changes
        // Size is expandable up to a point.  Fraction of screen until screen gets so large.
        // For now min( 300 pixels high, height of screen)
        // Use lower left origin pixel coordinate system

        float scaleHeight = canvas.getHeight();
        if (scaleHeight > 300.0f) scaleHeight = 300.0f;

        float scaleBorder = 0.03f; // Fraction of screen height
        scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight; // Give a little border
        float scaleWidth = 0.05f * scaleHeight; // Seems like a good ratio
        float scalePosX;
        float scalePosY;
        //	scalePosX = canvas.getHeight() * scaleBorder;
        //	scalePosY = canvas.getHeight() * (1.0f - scaleBorder) - scaleHeight;
        scalePosX = 10.0f;
        scalePosY = canvas.getHeight() - scaleHeight;
        //System.out.println(scalePosY);
        scalePosY = 334f;




        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL.GL_LINES);
        int total = 3000;
        int tick = 250;
        for (int i = 0; i < 14; i++) {
            float y;
            y = ((total - (tick * i)) - COLOR_MIN_HEIGHT)
                    / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);
            y = y * scaleHeight + scalePosY;
            gl.glVertex3f(scalePosX + scaleWidth + 46, y, -1.5f);

            if (Wilsim.c.isHGridBool() == true){
                gl.glVertex3f(50000f, y, -1.5f);
                lastGridH = false;
            }
            else if(Wilsim.c.isHGridBool() == false) {
                gl.glVertex3f(scalePosX + scaleWidth + 56, y, -1.5f);
                lastGridH = true;
            }

        }
            gl.glEnd();






        // Draw labels

        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        float textPosX, textPosY;
        textPosX = scalePosX + scaleWidth + 10.0f;
        vertScaleTextEngine.draw("Elevation", 10
                , (int) (scalePosY +
                ((total - COLOR_MIN_HEIGHT)
                        / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT))
                        * scaleHeight + 24.0f));
        for (int i = 0; i < 14; i++) {
            textPosY = scalePosY +
                    (((total - (tick * i)) - COLOR_MIN_HEIGHT)
                            / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT))
                            * scaleHeight - 5.0f;

            String Label = Integer.toString(total - (tick * i)) + " m";
            vertScaleTextEngine.draw(Label, (int) textPosX - 20, (int) textPosY);
        }

        vertScaleTextEngine.endRendering();

        if (lastGridH != Wilsim.c.isHGridBool()){
            canvas.repaint();
           // repaint = false;
        }
    }

    private void drawRiverProfileVertScale() {
        // Figure out size, location
        // Maybe this should be done only once and recomputed when the shape changes
        // Size is expandable up to a point.  Fraction of screen until screen gets so large.
        // For now min( 300 pixels high, height of screen)
        // Use lower left origin pixel coordinate system

        float scaleHeight = canvas.getHeight();
        if (scaleHeight > 300.0f) scaleHeight = 300.0f;

        float scaleBorder = 0.03f; // Fraction of screen height
        scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight; // Give a little border
        float scaleWidth = 0.05f * scaleHeight; // Seems like a good ratio
        float scalePosX;
        float scalePosY;
        //	scalePosX = canvas.getHeight() * scaleBorder;
        //	scalePosY = canvas.getHeight() * (1.0f - scaleBorder) - scaleHeight;
        scalePosX = 10.0f;
        scalePosY = canvas.getHeight() - scaleHeight;
        scalePosY = 334f;


        //	Wilsim.i.log.append("View : scaleHeight :" + String.valueOf(scaleHeight) + "\n");
        //	Wilsim.i.log.append("View : scaleWidth :" + String.valueOf(scaleWidth) + "\n");
        //	Wilsim.i.log.append("View : scalePosX :" + String.valueOf(scalePosX) + "\n");
        //	Wilsim.i.log.append("View : scalePosY :" + String.valueOf(scalePosY) + "\n");

        // Calculate divisions and division locations
        // Hardwired for now

        // Draw colored rectangular scale
        float[] color = new float[3];


        //final float[] vertScaleSample2 = {1250.0f, 1500.0f, 1750.0f, 2000.0f, 2250.0f, 2500.0f, 2750.0f, 3000.0f};
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL.GL_LINES);
        int total = 3000;
        int tick = 250;
        for (int i = 0; i < 14; i++) {
            float y;
            y = ((total - (tick * i)) - COLOR_MIN_HEIGHT)
                    / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);
            y = y * scaleHeight + scalePosY;

            gl.glVertex3f(scalePosX + scaleWidth + 46, y, -1.5f);
            if (Wilsim.c.isHGridBool() == true){
                gl.glVertex3f(50000f, y, -1.5f);
                lastGridH = false;
            }
            else if(Wilsim.c.isHGridBool() == false) {
                gl.glVertex3f(scalePosX + scaleWidth + 56, y, -1.5f);
                lastGridH = true;
            }
        }
        gl.glEnd();

        // Draw labels

        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        float textPosX, textPosY;
        textPosX = scalePosX + scaleWidth + 10.0f;
        vertScaleTextEngine.draw("Elevation", 10
                , (int) (scalePosY +
                ((total - COLOR_MIN_HEIGHT)
                        / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT))
                        * scaleHeight + 24.0f));
        for (int i = 0; i < 14; i++) {
            textPosY = scalePosY +
                    (((total - (tick * i)) - COLOR_MIN_HEIGHT)
                            / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT))
                            * scaleHeight - 5.0f;

            String Label = Integer.toString(total - (tick * i)) + " m";
            vertScaleTextEngine.draw(Label, (int) textPosX - 20, (int) textPosY);
        }

        vertScaleTextEngine.endRendering();

        if (lastGridH != Wilsim.c.isHGridBool()){
            canvas.repaint();
            // repaint = false;
        }
    }

    private void drawProfileView() {
        gl.glClearColor(.5f, .5f, .5f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);


        float scaleHeight = canvas.getHeight();
        if (scaleHeight > 300.0f) scaleHeight = 300.0f;


        float scaleBorder = 0.03f; // Fraction of screen height
        scaleHeight = (1.0f - 2 * scaleBorder) * scaleHeight; // Give a little border
        float scaleWidth = 0.05f * scaleHeight; // Seems like a good ratio
        float scalePosX;
        float scalePosY;
        //	scalePosX = canvas.getHeight() * scaleBorder;
        //	scalePosY = canvas.getHeight() * (1.0f - scaleBorder) - scaleHeight;
        scalePosX = 10.0f;
        scalePosY = 320.0f - scaleHeight;

        float textPosX, textPosY;
        textPosX = scalePosX + scaleWidth + 10.0f;


        // Calculate divisions and division locations
        // Hardwired for now

        double scalef = .00102 * ((double) canvas.getWidth() / 522);
        float drawScaleX = (float) scalef;
        float drawScaleY = 0.23871753f;
        drawScaleY = 0.17636f;
        float saturation = 1.0f; //saturation
        float brightness = 1.0f; //brightness


//for (int x = XSectionManager.getXSection(0).getNIterates(); x < XSectionManager.getXSection(0).getMaxNIterates(); x++) {

//to calibrate scale
/*        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(0,0,0);
        gl.glLineWidth(3.5f);
        for (int i = 0; i <= 3000; i+=250) {
            gl.glVertex3f(0, i* drawScaleY + 87, -1.55f);
            gl.glVertex3f(1000, i* drawScaleY + 87, -1.55f);
        }
        gl.glEnd();*/


        if (Wilsim.m.river != null && Wilsim.m.river.getProcessed() > 0) {
        // Last minute filter hack to get rid of spikes.  Ideally
        // shouldn't be needed.  To be investigated further
        final int thresh = 30;
        for (int k = 7; k >= 1; k -= 2) // Reduce wide peaks to narrow peaks
            for (int j = 0; j < Wilsim.m.river.getNIterates(); j++) {
                for (int i = k; i < Wilsim.m.river.n[j] - k; i++) {
                    float value;
                    if ((Wilsim.m.river.values[j][i - k] - Wilsim.m.river.values[j][i]) < -thresh
                            && (Wilsim.m.river.values[j][i + k] - Wilsim.m.river.values[j][i]) < -thresh)
                        Wilsim.m.river.values[j][i] = (Wilsim.m.river.values[j][i + k] + Wilsim.m.river.values[j][i - k]) / 2.0f;
                }
            }



            scalef = .00102 * ((double) canvas.getWidth() / Wilsim.m.river.n[0]);
            drawScaleX = (float) scalef;


            for (int x = 0; x < Wilsim.m.river.getProcessed(); x++) {



                Color myRGBColor = Color.getHSBColor((float) (x) / (Wilsim.m.storageIntervals + 1), saturation, brightness);

                gl.glEnable(GL.GL_LINE_SMOOTH);
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glColor3f(myRGBColor.getRed() / 255.0f, myRGBColor.getGreen() / 255.0f, myRGBColor.getBlue() / 255.0f);
                gl.glLineWidth(3.5f);

                if (Wilsim.m.river.values[x] != null) {

                    for (int i = 1; i < Wilsim.m.river.n[0] - 1; i++) {

                        gl.glVertex3f((Wilsim.m.river.distances[0][i] * drawScaleX) + 74,
                                (Wilsim.m.river.values[x][i] * drawScaleY) + 87,
                                -1.55f + (x * .001f));

                    }
                }
                gl.glEnd();
            }
        }

//}

        //System.out.println(canvas.getHeight());


        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(25 + (scalePosX * 5), scalePosY + 5.0f, -1.5f);
        gl.glVertex3f(75 + (400000 * drawScaleX), scalePosY + 5.0f, -1.5f);
        gl.glEnd();


        // Draw label tics
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        //gl.glColor4f(0.0f, 0.0f, 0.0f, .5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(75, scalePosY + 0.0f, -1.5f);
        gl.glVertex3f(75, scalePosY + 2000.0f, -1.5f);

        for (int x = 0; x < 41; x++) {
            gl.glVertex3f(75 + x * (100000 * drawScaleX), scalePosY + 0.0f, -1.5f);

            if (Wilsim.c.isVGridBool() == true){
                gl.glVertex3f(75 + x * (100000 * drawScaleX), 5000f, -1.5f);
                lastGridV = false;
            }
            else if(Wilsim.c.isVGridBool() == false) {
                gl.glVertex3f(75 + x * (100000 * drawScaleX), scalePosY + 10.0f, -1.5f);
                lastGridV = true;
            }
        }

        gl.glEnd();

        // Draw labels

        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        vertScaleTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());

/*        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);
        vertScaleTextEngine.draw("Horizontal Distance (km) ", 5, 5);*/

        vertScaleTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);


        int total = 0;
        textPosX = 0;
        String Label = Integer.toString(total);
        vertScaleTextEngine.draw(Label + " km", (int) textPosX + 71, (int) 25.0f);
        vertScaleTextEngine.draw("Distance", (canvas.getWidth() / 2) - 25, 6);

        for (int i = 1; i < 5; i++) {
            textPosX = 100000 * i * drawScaleX;
            total += 100;
            Label = Integer.toString(total);
            vertScaleTextEngine.draw(Label + " km", (int) textPosX + 63, (int) 25.0f);
        }

        vertScaleTextEngine.endRendering();

        if (lastGridV != Wilsim.c.isVGridBool()){
            canvas.repaint();
            // repaint = false;
        }

    }

    private void drawCompass() {
        // Positions the compass in the upper right corner
        float compassScaleFactor = 0.05f;
        float iCompassWidth = canvas.getWidth() * compassScaleFactor;
        float ccx;
        float ccy;
        if (iCompassWidth < 20.0f) iCompassWidth = 20.0f;
        ccx = canvas.getWidth() - iCompassWidth - 20;
        ccy = canvas.getHeight() - iCompassWidth - 20;

        // Rotates the compass with the model, points to North
        gl.glPushMatrix();
        gl.glTranslatef(ccx, ccy, 0);
        gl.glRotatef(viewLongitude - 180, 0, 0, 1);

        //Draw compass border
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3f(iCompassWidth * (-2 / 3.0f), -iCompassWidth, -1.5f);
        gl.glVertex3f(0, iCompassWidth, -1.0f);
        gl.glVertex3f(iCompassWidth * (2 / 3.0f), -iCompassWidth, -1.5f);
        gl.glVertex3f(0, iCompassWidth * (-1 / 3.0f), -1.5f);
        gl.glEnd();

//    	//Draw left half of compass arrow
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        gl.glColor3f(0.9f, 0.7f, 0.1f);
        gl.glVertex3f(iCompassWidth * (-2 / 3.0f), -iCompassWidth, -1.5f);
        gl.glColor3f(0.7f, 0.2f, 0.0f);
        gl.glVertex3f(0, iCompassWidth, -1.5f);
        gl.glColor3f(1.0f, 0.8f, 0.1f);
        gl.glVertex3f(0, iCompassWidth * (-1 / 3.0f), -1.5f);
        gl.glEnd();

        //Draw right half of compass arrow
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        gl.glColor3f(0.9f, 0.7f, 0.1f);
        gl.glVertex3f(iCompassWidth * (2 / 3.0f), -iCompassWidth, -1.5f);
        gl.glColor3f(0.7f, 0.2f, 0.0f);
        gl.glVertex3f(0, iCompassWidth, -1.5f);
        gl.glColor3f(1.0f, 0.8f, 0.1f);
        gl.glVertex3f(0, iCompassWidth * (-1 / 3.0f), -1.5f);
        gl.glEnd();

        CompassTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
        CompassTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Writes an "N" in the middle of the compass
        float textPosX, textPosY;
        textPosX = ccx - 6.0f;
        textPosY = ccy - 10.0f;

        String Label = "N";
        CompassTextEngine.draw(Label, (int) textPosX, (int) textPosY);

        CompassTextEngine.endRendering();

        gl.glPopMatrix();
    }

    private void drawScore() {
        scoreTextEngine.beginRendering(canvas.getWidth(), canvas.getHeight());
        scoreTextEngine.setColor(0.0f, 0.0f, 0.0f, 1.0f);

        float textPosX, textPosY;

        // Need something a little more intelligent
        textPosX = canvas.getWidth() * 0.30f;
        textPosY = +50;

        scoreTextEngine.draw(String.format("Score: %7.2f", Wilsim.m.score),
                (int) textPosX, (int) textPosY);
        scoreTextEngine.endRendering();
    }



    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        Wilsim.i.log.append("View : reshape()\n");
        Wilsim.i.log.append(String.format("x: %d\ny: %d\nw: %d\nh: %d\n",
                x, y, w, h));
        gl.glViewport(0, 0, w, h);

        newParams = true;
        newComputation.boolVal = true;
    }

    @Override
    public void run() {
        canvas.requestFocus();
        canvas.addGLEventListener(this);

        // Wilsim.i.log.append("View: run()\n");

        while (true) {
            if (!newComputation.boolVal) {
                synchronized (newComputation) {
                    try {
                        newComputation.wait();
                    } catch (Exception e) {
                    }
                }
                continue;
            }

            // Render an image here
            canvas.repaint();
        }
    }

    private void computeXSectionView() {
        // View from above and centered over terrain

        world2cam[0] = 1.0f;
        world2cam[1] = 0.0f;
        world2cam[2] = 0.0f;
        world2cam[3] = 0.0f;

        world2cam[4] = 0.0f;
        world2cam[5] = 1.0f;
        world2cam[6] = 0.0f;
        world2cam[7] = 0.0f;

        world2cam[8] = 0.0f;
        world2cam[9] = 0.0f;
        world2cam[10] = 1.0f;
        world2cam[11] = 0.0f;

        world2cam[12] = 0.0f;
        world2cam[13] = 0.0f;
        world2cam[14] = -cameraRadius;
        world2cam[15] = 1.0f;

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

    private void computeView(float longitude, float latitude) {
        // System.out.println("View: computeView()\n");
        float theta = cameraFOV / 2.0f;
        float ttheta = (float) Math.tan(theta * Math.PI / 180.0f);
        float aspect = ((float) canvas.getWidth()) / canvas.getHeight();

        if (canvas.getWidth() > canvas.getHeight()) {
            // Horizontal window
            screenX = ttheta * cameraNear;
            screenY = (screenX * canvas.getHeight()) / canvas.getWidth();
        } else {
            // Vertical window
            screenY = ttheta * cameraNear;
            screenX = (screenY * canvas.getWidth()) / canvas.getHeight();
        }

        // Compute new eye position
        cameraEye[0] = cameraRadius * (float) Math.sin(longitude * Math.PI / 180.0f)
                * (float) Math.sin(latitude * Math.PI / 180.0f);
        cameraEye[1] = cameraRadius * (float) Math.cos(longitude * Math.PI / 180.0f)
                * (float) Math.sin(latitude * Math.PI / 180.0f);
        cameraEye[2] = cameraRadius * (float) Math.cos(latitude * Math.PI / 180.0f);

        calculateWorldToCamera(world2cam);
    }


    // Vector manipulation methods

    private void cross(float[] c, float[] a, float[] b) {
        // C = A X B

        c[0] = a[1] * b[2] - b[1] * a[2];
        c[1] = -(a[0] * b[2] - b[0] * a[2]);
        c[2] = a[0] * b[1] - b[0] * a[1];
    }

    private float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private void normalize(float[] v) {
        // In place
        float mag2 = v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
        mag2 = (float) Math.sqrt(mag2);
        v[0] /= mag2;
        v[1] /= mag2;
        v[2] /= mag2;
    }

    private void calculateWorldToCamera(float[] m) {
        float[] A = new float[3];
        A[0] = cameraAt[0] - cameraEye[0];
        A[1] = cameraAt[1] - cameraEye[1];
        A[2] = cameraAt[2] - cameraEye[2];

        normalize(A);

        float[] V = new float[3];

        cross(V, A, cameraUp);

        normalize(V);

        float[] U = new float[3];

        cross(U, V, A);

        // GL uses column major order for matrices - Ugh!

        m[0] = V[0];
        m[1] = U[0];
        m[2] = -A[0];
        m[3] = 0.0f;
        m[4] = V[1];
        m[5] = U[1];
        m[6] = -A[1];
        m[7] = 0.0f;
        m[8] = V[2];
        m[9] = U[2];
        m[10] = -A[2];
        m[11] = 0.0f;
        m[12] = -dot(cameraEye, V);
        m[13] = -dot(cameraEye, U);
        m[14] = dot(cameraEye, A);
        m[15] = 1.0f;
    }

    //Horizontal Rotate
    public void horizontalRotate(float angle) {
        viewLongitude = angle;

        synchronized (newComputation) {
            newParams = true;
            newComputation.boolVal = true;
            newComputation.notify();
        }

    }

    public void verticalRotate(float angle) {
        viewLatitude = angle;

        synchronized (newComputation) {
            newParams = true;
            newComputation.boolVal = true;
            newComputation.notify();
        }

    }

    private void debug_color(int x, int y) {
        if (x == 19 && y == 146) {
            gl.glColor3f(0.5f, 1.0f, 0.5f);
        }

    }
    public void map_color_pre_calc(float height, Model.Vec3 color){
        float t = (height - COLOR_MIN_HEIGHT) / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);

        // Clamp to range, just in case
        if (t < 0.0) t = 0.0f;
        if (t > 1.0) t = 1.0f;


        // Light green through red


/*	else
	    {
		color[0] = 1.0f;
		color[1] = (1.0f - t) / 0.67f;
		color[2] = 0.0f;
	    }*/

        if (t < 0.5 /*&& t > 0.01*/) {
            color.x = 0.488f + t * 0.836f;
            color.y = 0.164f + t * 0.953f;
            color.z = 0.094f + t * 0.172f;
        }
/*        else if (t <= 0.01)
        {
            color[0] = (t) / 0.33f;
            color[1] = .50f;
            color[2] = (0.33f - t) / 0.33f;
        }*/
        // Brown yellow map
        else {
            color.x = 0.906f + (t - 0.5f) * 0.180f;
            color.y = 0.640f + (t - 0.5f) * 0.680f;
            color.z = 0.180f + (t - 0.5f) * 0.773f;
        }
    }

    private void map_color(float height, float color[]) {

        float t = (height - COLOR_MIN_HEIGHT) / (COLOR_MAX_HEIGHT - COLOR_MIN_HEIGHT);

        // Clamp to range, just in case
        if (t < 0.0) t = 0.0f;
        if (t > 1.0) t = 1.0f;


	// Light green through red


/*	else
	    {
		color[0] = 1.0f;
		color[1] = (1.0f - t) / 0.67f;
		color[2] = 0.0f;
	    }*/

        if (t < 0.5 /*&& t > 0.01*/) {
            color[0] = 0.488f + t * 0.836f;
            color[1] = 0.164f + t * 0.953f;
            color[2] = 0.094f + t * 0.172f;
        }
/*        else if (t <= 0.01)
        {
            color[0] = (t) / 0.33f;
            color[1] = .50f;
            color[2] = (0.33f - t) / 0.33f;
        }*/
        // Brown yellow map
        else {
            color[0] = 0.906f + (t - 0.5f) * 0.180f;
            color[1] = 0.640f + (t - 0.5f) * 0.680f;
            color[2] = 0.180f + (t - 0.5f) * 0.773f;
        }

    }


    public void resetXSections() {
        XSectionManager.reset();
        XSectionManager.init();
        // Refresh view
        synchronized (newComputation) {
            newUI = true;
            newComputation.boolVal = true;
            newComputation.notify();
        }
    }


}