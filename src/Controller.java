import com.jogamp.opengl.awt.GLCanvas;
import com.sun.deploy.util.StringUtils;
import javafx.scene.control.CheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import static java.lang.Math.abs;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * The Controller class handles the User Interface. It handles all the events by
 * user and communicates with the view and model classes and display the model
 * accordingly.
 */
public class Controller

{
    File curDir = new File("user.home");
    /*
    // Communication variables
    private CondVar               attentionRequest;
    private boolean               outputFlag;
    */

    // Internal UI variables
    private Container UI;  // The containing UI panel for the controller
    int wilsimWidth = 1200;
    int wilsimHeight = 1024;
    //private JPanel controlsHead; // contains the options, profiles and
    // hypsometric buttons

    // hypsometric
    // panels
    private JPanel options; // Contains the optionsHead and optionsHead panels
    // parameters Buttons
    private JPanel optionsBody; // Contains the two cards -> initial &
    // parameters
    private JPanel profiles; // panel to hold profiles
    private JPanel profileNorth; // panel to hold profiles
    private JPanel profileSouth; // panel to hold profiles
    private JPanel xsection;

    private JPanel cardPanel; // Panel to hold the main three panels
    private CardLayout card; // Main cardlayout to display one panel at a time
    // out of (options,profiles and hypsometric)
    private CardLayout optionsCard; // Options card layout to display one card
    // at a time out of (initial conditions and
    // parameters)

    // View widgets
    private JScrollBar viewHorScroll, viewVerScroll;

    // Controller widgets
    JButton startStopButton;
    private JRadioButton profilesButton;
    private JRadioButton DrawProfileButton;
    private JRadioButton XsectionButton;
    private JRadioButton optionsButton;
    private JRadioButton kstrong;
    private JRadioButton kfactor;

    // button
    // to be selected at a
    // time
    JProgressBar progressBar;
    private JLabel rate;
    private JLabel Myr;
    private JLabel factor;
    private JLabel strong;
    private JScrollBar timeBar;
    private JScrollBar kfactorBar;
    private JScrollBar kstrongBar;
    private JScrollBar Along_GW_Fault;
    private JLabel Along_GW_Fault_Text;
    public JLabel timeLegend;
    public ArrayList<String> sectionTimes;
    private boolean HGridBool;
    private boolean VGridBool;

    JButton saveBtn;


    private JScrollBar cliffRateScroll;

    // to set the color of main card panels
    private final Color inactiveColor = new Color(205, 133, 63);
    private final Color activeColor = new Color(227, 207, 87);
    private final Color grey = new Color (127,127,127);

    float duration;
    float kfctor;
    float kstrng;
    float cliffRate;
    float pauseValue;


    private JLabel siText;
    private JScrollBar siBar;

    // Output
    private Runnable outputRun;

    // XSection file chooser
    private JFileChooser xsfc;  // XSection file chooser
    private JFileChooser lpfc;   // long profile file chooser
    private JFileChooser topofc;   // long profile file chooser
    
    /*
    public Controller()
    {
   // Create communication locks
   attentionRequest = new CondVar(false);
   outputFlag = false;
    }
    */

    public boolean isHGridBool() {
        return HGridBool;
    }
    public boolean isVGridBool() {
        return VGridBool;
    }

    void createGUI(final Container c, GLCanvas glc) {

        UI = c;

        // Border Layout by default
        // Set up the graphics panel
        final JPanel viewPanel = new JPanel();
        viewPanel.setLayout(new BorderLayout());


        viewHorScroll = new JScrollBar(Adjustable.HORIZONTAL, 180, 1, 0, 360);
        viewVerScroll = new JScrollBar(Adjustable.VERTICAL, 45, 1, 1, 90);

        viewPanel.add(viewHorScroll, BorderLayout.SOUTH);
        viewPanel.add(viewVerScroll, BorderLayout.EAST);
        viewPanel.add(glc, BorderLayout.CENTER);


        // adding listeners to rotate the model
        viewHorScroll.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                float angle = viewHorScroll.getValue();
                Wilsim.v.horizontalRotate(angle);
            }
        });

        viewVerScroll.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                float angle = viewVerScroll.getValue();
                Wilsim.v.verticalRotate(angle);
            }
        });

        Font font = new Font(("SansSerif"), Font.PLAIN,12);
        // Set up the user interface controlsHead
        JPanel controlsHead = new JPanel(new GridLayout(2,2));
        final JPanel controlPanel = new JPanel(new BorderLayout());

        //JPanel controlsHead = new JPanel(new GridLayout(1, 3));

        // Initializing the buttons
        optionsButton = new JRadioButton("PARAMETERS");
        optionsButton.setMargin(new Insets(0, 0, 0, 0));
        optionsButton.setFocusPainted(false);
        optionsButton.setPreferredSize(new Dimension(110, 50));
        optionsButton.setMaximumSize(new Dimension(110, 50));
        optionsButton.setFont(font);



        profilesButton = new JRadioButton("DRAW");
        profilesButton.setMargin(new Insets(0, 0, 0, 0));
        profilesButton.setFocusPainted(false);
        profilesButton.setPreferredSize(new Dimension(70, 50));
        profilesButton.setMaximumSize(new Dimension(70, 50));
        profilesButton.setFont(font);


        XsectionButton = new JRadioButton("CROSS SECTION");
        XsectionButton.setPreferredSize(new Dimension(125, 50));
        XsectionButton.setMaximumSize(new Dimension(125, 50));
        XsectionButton.setFont(font);


        DrawProfileButton = new JRadioButton("PROFILE");
        DrawProfileButton.setPreferredSize(new Dimension(80, 50));
        DrawProfileButton.setMaximumSize(new Dimension(80, 50));
        DrawProfileButton.setFont(font);


        optionsButton.setSelected(true);
        optionsButton.setBackground(activeColor);
        profilesButton.setBackground(inactiveColor);
        XsectionButton.setBackground(inactiveColor);
        DrawProfileButton.setBackground(inactiveColor);

        // grouping the three buttons which helps in choosing a single button at
        // a time
        ButtonGroup mainOptions = new ButtonGroup();
        mainOptions.add(optionsButton);
        mainOptions.add(profilesButton);
        mainOptions.add(XsectionButton);
        mainOptions.add(DrawProfileButton);

        // Adding the buttons to controlsHead panel which is the head of the
        // control
        // panel


        controlsHead.add(optionsButton);
        controlsHead.add(profilesButton);
        controlsHead.add(XsectionButton);
        controlsHead.add(DrawProfileButton);
        controlsHead.setBackground(activeColor);
        controlsHead.setBorder(new EmptyBorder(0, 0, 0, 0));


        //controlsHead.setBorder(new Border);
        // controlsHead.setBackground(inactiveColor);
        //controlsHead.add(controlsHead);


        // Creating a card layout to display single panel out of the
        // two(parameters,profiles) at a time
        card = new CardLayout();
        cardPanel = new JPanel(card);

        // Initializing the buttons of options category
        JRadioButton parameterButton = new JRadioButton("Parameters");


        // Initializing the options panel
        options = new JPanel(new BorderLayout());
        options.setBackground(activeColor);
        options.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Initializing the optionsbody panel to cardlayout so that one
        // card(between initial and parameters) can be displayed at a time.
        optionsCard = new CardLayout();
        optionsBody = new JPanel(optionsCard);

        // Initializing and adding the initial conditions and parameters panels
        // (to hold their appropriate values) to optionsBody panel
        JPanel test = new JPanel (new FlowLayout());
        Box verticalBox = Box.createVerticalBox();

        JPanel parameter = new JPanel(new GridLayout(6, 1));
        parameterButton.setBackground(activeColor);
        parameter.setBackground(activeColor);
        optionsBody.add("Parameter", parameter);

        // Creating brown color
        Color brown = new Color(205, 133, 63);

        // Declaring Initial Conditions panel values and adding them to the
        // panel
        JLabel endTime = new JLabel("Simulation End Time :   ");
        endTime.setToolTipText(
                "<html>The time when the simulation will end<br />" +
                        "(the simulation starts 6 million years before<br />" +
                        "present).</html>");
        Myr = new JLabel("<html><br />0 Myr <br />(Present)</html>");
        //Myr.setBounds(2, 2, 2, 2);
        duration = 6000;

        timeBar = new JScrollBar(Adjustable.VERTICAL, 6, 1, 0, 7);

        // progress bar to display the model evolution in time
        timeBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // int timeValue = timeBar.getValue();
                int timeValue = 6 - timeBar.getValue();
                if (timeValue > 0)
                    Myr.setText("<html><br />" + String.valueOf(timeValue)
                            + " Myr <br />(in the future)</html>");
                if (timeValue == 0)
                    Myr.setText("<html><br /> 0 Myr <br />(Present)</html>");
                duration = 6000 + (1000 * timeValue);

            }
        });

        // creating each option in the paramter at a panel and adding border to
        // them
        verticalBox.add(Box.createVerticalGlue());
        verticalBox.add(Myr);
        verticalBox.add(Box.createVerticalGlue());
        verticalBox.setBackground(activeColor);
        //test.add(Myr, BorderLayout.SOUTH);
        JPanel time = new JPanel(new BorderLayout());
        time.add(endTime, BorderLayout.WEST);
        time.add(Myr,BorderLayout.CENTER);
        time.add(timeBar, BorderLayout.EAST);
        time.setBackground(activeColor);
        // adding border to time parameter
        Border timeLine = BorderFactory.createLineBorder(brown);
        time.setBorder(timeLine);

        // Declaring Cliff retreat
        cliffRate = 0.5f;
        JLabel cliff = new JLabel("Cliff Retreat Rate :");
        cliff.setToolTipText(
                "<html>The rate at which cliffs wear back laterally:<br />" +
                        "a value of 0.5m kyr\u207B\u00B9 means the cliff will<br />" +
                        "wear back 0.5m per 1000 years.</html>");
        rate = new JLabel("       0.5  m/kyr");
        cliffRateScroll = new JScrollBar(Adjustable.VERTICAL, 200, 25, 0, 251);
        cliffRateScroll.setValue(200);
        cliffRateScroll.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                float cliff = 250 - cliffRateScroll.getValue();
                cliffRate = cliff / 100;
                rate.setText("       " + String.valueOf(cliffRate)
                        + "  m/kyr");

            }
        });

        // making cliff retreat as a panel and adding border to it
        JPanel clif = new JPanel(new BorderLayout());
        clif.add(cliff, BorderLayout.WEST);
        clif.add(rate, BorderLayout.CENTER);
        clif.add(cliffRateScroll, BorderLayout.EAST);
        clif.setBackground(activeColor);
        Border cliffLine = BorderFactory.createLineBorder(brown);
        clif.setBorder(cliffLine);

        // Declaring kstrong and kfactor as panels and adding border to the
        // panel

       // JLabel subsidence = new JLabel("Subsidence rate<br>along faults");
        JPanel subsidencePan = new JPanel(new BorderLayout());
       // subsidencePan.add(subsidence, BorderLayout.WEST);
        subsidencePan.setBackground(activeColor);

        JPanel emptyPan = new JPanel(new BorderLayout());
        emptyPan.setBackground(activeColor);


        JLabel Along_GW_Fault_Label = new JLabel("<html>Subsidence Rate Along<br />Grand Wash Fault:<br /></html>");
        Along_GW_Fault_Label.setToolTipText(
                "<html>Subsidence rate along Grand Wash Fault<br />" +
                      "in meter per thousand years. This is essentially<br />" +
                        "a base level drop rate.</html>");
        Along_GW_Fault_Text = new JLabel("<html><br />1.7 m/kyr </html>");
        Along_GW_Fault = new JScrollBar(Adjustable.VERTICAL, 35, 0, 0, 226);
        Along_GW_Fault.setValue(81);
        Along_GW_Fault.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Wilsim.m.Along_Grant_Wash_Fault = ((251 - Along_GW_Fault.getValue()) / 100.0f) * -1;

                /*new = KStrong / 100.0f;*/
                // Have to fake division for proper printing
                // without flipping to scientific notation
                Along_GW_Fault_Text.setText("<html><br />"
                        + String.format("%.02f", abs(Wilsim.m.Along_Grant_Wash_Fault)) + " m/kyr  </html>");
            }
        });

        JLabel kStrong = new JLabel("Rock Erodibility :");
        kStrong.setToolTipText(
                "<html>How easy the rock layer can be eroded:<br />" +
                        "a value of 0.0001 kyr\u207B\u00B9 generates<br />" +
                        "0.1m of erosion per 1000 years for a fluvial<br />" +
                        "channel of slope equal to 1 (45\u00B0) and<br />" +
                        "drainage area equal to 1 km\u00B2.</html>");
        JLabel kFactor = new JLabel("Hard/Soft Contrast :");
        kFactor.setToolTipText(
                "<html>A dimensionless number that controls the<br />" +
                        "relative erosion rate between strong and weak<br />" +
                        "rock layers: a value of 5 means the soft rock<br />" +
                        "is 5 times more erodible than the hard rock<br />" +
                        "layer.</html>");

        strong = new JLabel("        0.00015 kyr\u207B\u00B9");
        factor = new JLabel("    5");
        kstrongBar = new JScrollBar(Adjustable.VERTICAL, 35, 1, 0, 51);
        kfactorBar = new JScrollBar(Adjustable.VERTICAL, 5, 1, 0, 10);
        // initial values
        kstrng = (float) 0.15;
        kfctor = 5;
        kfactorBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                int KFactor = 9 - kfactorBar.getValue() + 1;
                factor.setText("    " + String.valueOf(KFactor));
                kfctor = KFactor;

            }
        });

        kstrongBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                int KStrong = 50 - kstrongBar.getValue();

                kstrng = KStrong / 100.0f;
                // Have to fake division for proper printing
                // without flipping to scientific notation
                strong.setText("        "
                        + String.format("0.000%02d", KStrong)
                        + " kyr\u207B\u00B9");

            }
        });
        JPanel Along_GW_Pan = new JPanel(new BorderLayout());
        Along_GW_Pan.add(Along_GW_Fault_Label, BorderLayout.WEST);
        Along_GW_Pan.add(Along_GW_Fault_Text, BorderLayout.CENTER);
        Along_GW_Pan.add(Along_GW_Fault, BorderLayout.EAST);
        Along_GW_Pan.setBackground(activeColor);

        Border Along_GW_Line = BorderFactory.createLineBorder(brown);
        Along_GW_Pan.setBorder(Along_GW_Line);


        JPanel kstr = new JPanel(new BorderLayout());
        kstr.add(kStrong, BorderLayout.WEST);
        kstr.add(strong, BorderLayout.CENTER);
        kstr.add(kstrongBar, BorderLayout.EAST);
        kstr.setBackground(activeColor);

        Border kstrLine = BorderFactory.createLineBorder(brown);
        kstr.setBorder(kstrLine);

        JPanel kfact = new JPanel(new BorderLayout());
        kfact.add(kFactor, BorderLayout.WEST);
        kfact.add(factor, BorderLayout.CENTER);
        kfact.add(kfactorBar, BorderLayout.EAST);
        kfact.setBackground(activeColor);

        // Color b=Color(1,1,1);
        Border kfactLine = BorderFactory.createLineBorder(brown);
        kfact.setBorder(kfactLine);

        // Adding the options head and body panels(cards) to the options
        // Panel(card)
        options.add(optionsBody, BorderLayout.CENTER);

        // Initializing the profiles and profiles panels(Cards)
        profiles = new JPanel();
        profileNorth = new JPanel();
        profileSouth = new JPanel();
        profiles.setLayout(new BoxLayout(profiles, BoxLayout.PAGE_AXIS));
        xsection = new JPanel();
   /*
   // This button will switch model between profiles and spin modes
   // the counter changes the name on the button from profile to spin and
   // vice versa
   profButton = new JButton("Setup");
   count = 0;
   profButton.addActionListener(new ActionListener() {
      @Override
          public void actionPerformed(ActionEvent arg0) {
          // TODO Auto-generated method stub
          count++;
          if (count % 2 != 0) {
         profButton.setText("Finish");
         Wilsim.v.changeViewMode(Wilsim.v.PROFILE_MODE);
          } else {
         profButton.setText("Setup");
         Wilsim.v.changeViewMode(Wilsim.v.SPIN_MODE);
          }
      }
       });
   */

        //this button clears the drawn profiles on the model
        sectionTimes = new ArrayList<>(1);
        //sectionTimes.add("");

        JButton clear = new JButton("Clear");


        timeLegend = new JLabel();
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Wilsim.v.resetXSections();

            }
        });

        final Checkbox HGrid = new Checkbox("Horizontal Grid Lines");
        HGrid.setBackground(grey);
        final Checkbox VGrid = new Checkbox("Vertical Grid Lines");
        VGrid.setBackground(grey);

        HGrid.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (HGrid.getState() == true) {
                    HGridBool = true;
                }
                else {HGridBool = false;
                }
            }
        });

        VGrid.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (VGrid.getState() == true) {
                    VGridBool = true;
                }
                else {VGridBool = false;
                }
            }
        });


        // create XSection File Browser for future use
        xsfc = new JFileChooser();
        xsfc.setMultiSelectionEnabled(false);
        xsfc.setCurrentDirectory(new File("user.home"));
        xsfc.setDialogTitle("Save Cross Section File");


        // create long profile file browser for futre use
        lpfc = new JFileChooser();
        lpfc.setMultiSelectionEnabled(false);
        lpfc.setCurrentDirectory(new File("user.home"));
        lpfc.setDialogTitle("Save Long Profile File");


        topofc = new JFileChooser();
        topofc.setMultiSelectionEnabled(false);
        topofc.setCurrentDirectory(new File("user.home"));
        topofc.setDialogTitle("Save Topographic File");

/*        outputRun = new Runnable() {
            public void run() {
                outputXSections();
                outputProfiles();
                outputTopo();
            }
        };*/


        // Adding buttons to profiles tab
        // profiles.add(profButton);
        profileNorth.add(HGrid);
        profileNorth.add(VGrid);
        profileSouth.add(timeLegend);
        profiles.add(profileNorth);
        profiles.add(profileSouth);


        profiles.add(Box.createVerticalGlue());
        xsection.add(clear);
        /*
        profiles.add(initialize);
        profiles.add(display);*/
        // profiles.add(savetoText);

        // Adding the three main card panels to the main CardPanel
        cardPanel.add("Card1", options);
        cardPanel.add("Card2", profiles);
        cardPanel.add("Card3", xsection);
        //cardPanel.add("Card4", DrawProfile);

        // When options button is selected, the options panels card is displayed
        optionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                viewPanel.add(viewHorScroll, BorderLayout.SOUTH);
                viewPanel.add(viewVerScroll, BorderLayout.EAST);
                card.show(cardPanel, "Card1");
                options.setBackground(activeColor);
                optionsButton.setBackground(activeColor);
                profilesButton.setToolTipText("");
                profilesButton.setBackground(inactiveColor);
                XsectionButton.setBackground(inactiveColor);
                DrawProfileButton.setBackground(inactiveColor);
                Wilsim.v.changeViewMode(View.SPIN_MODE);
                // profButton.setText("Setup");

            }
        });

        // When hypsometric button is selected, the hypsometric panel card is
        // displayed
        profilesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {


                viewPanel.remove(viewHorScroll);
                viewPanel.remove(viewVerScroll);


                card.show(cardPanel, "Card3");
                profiles.setBackground(activeColor);
                xsection.setBackground(activeColor);
                profilesButton.setBackground(activeColor);
                optionsButton.setBackground(inactiveColor);
                XsectionButton.setBackground(inactiveColor);
                profilesButton.setToolTipText(
                        "<html>Left-click and drag a line where you want<br />" +
                                "to place the cross-section.</html>");
                DrawProfileButton.setBackground(inactiveColor);
                Wilsim.v.changeViewMode(View.PROFILE_MODE);

            }
        });

        XsectionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                viewPanel.remove(viewHorScroll);
                viewPanel.remove(viewVerScroll);
                card.show(cardPanel, "Card2");
                profiles.setBackground(grey);
                profileNorth.setBackground(grey);
                profileSouth.setBackground(grey);
                profilesButton.setToolTipText("");
                profilesButton.setBackground(inactiveColor);
                optionsButton.setBackground(inactiveColor);
                XsectionButton.setBackground(activeColor);
                DrawProfileButton.setBackground(inactiveColor);
                Wilsim.v.changeViewMode(View.XVISUALIZER_MODE);

            }
        });

        DrawProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewPanel.remove(viewHorScroll);
                viewPanel.remove(viewVerScroll);
                card.show(cardPanel, "Card2");
                profiles.setBackground(grey);
                profileNorth.setBackground(grey);
                profileSouth.setBackground(grey);
                profilesButton.setToolTipText("");
                profilesButton.setBackground(inactiveColor);
                optionsButton.setBackground(inactiveColor);
                XsectionButton.setBackground(inactiveColor);
                DrawProfileButton.setBackground(activeColor);
                Wilsim.v.changeViewMode(View.RIVER_PROFILE_MODE);

            }
        });


        // //When parameter button is selected, the parameter panels card is
        // displayed
        parameterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                optionsCard.show(optionsBody, "Parameter");

                kstrong.setSelected(true);

            }
        });

        // Adding the three control buttons and the three panels to the main
        // control panel
        JPanel controlsBody = new JPanel(new BorderLayout());



        controlsBody.add(controlsHead, "North");
        //controlsBody.add(controlsHeadBtm, "Center");


        controlsBody.add(cardPanel, "Center");
        controlPanel.add(controlsBody, "Center");

        // Button to start/pause the simulation
        startStopButton = new JButton();
        startStopButton.setText("Start");
        startStopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Wilsim.m.toggleExecution();

            }
        });
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        Wilsim.c.progressBar.setString("6M Years ago");
        JButton rset = new JButton("Reset");
        saveBtn = new JButton("Save");
        saveBtn.setEnabled(false);


        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCross();

            }
        });


        rset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Wilsim.m.resetCall();

            }
        });
        JPanel startPanel = new JPanel(new BorderLayout());
        startPanel.add(startStopButton, "West");
        startPanel.add(progressBar, "North");
        startPanel.add(rset, "Center");
        startPanel.add(saveBtn, "East");
        controlPanel.add(startPanel, BorderLayout.SOUTH);
        controlPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        //ImageIcon banner = new ImageIcon(getClass().getResource("bn.jpg"));
        //ImageIcon banner = new ImageIcon("res/bn.jpg");
        //JLabel bannerLabel = new JLabel(banner);
        JPanel bannerPanel = new JPanel(new GridLayout(1, 1));
       // bannerPanel.add(bannerLabel);

   /* // Pause points

   Pause = new JTextField(4);

   pause = new JLabel("Pause point : (In Million years)");

   JButton set = new JButton("set");
   // Because we do not want to pause betwen 0 and 2 unless the value is set
   pauseValue = 20;
   set.setPreferredSize(new Dimension(70, 20));
   set.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          int val = Integer.parseInt(Pause.getText());
          if (val == -1)
         pauseValue = 1;

          if (val == -2)
         pauseValue = 2;
          if (val == -3)
         pauseValue = 3;
          if (val == -4)
         pauseValue = 4;
          if (val == -5)
         pauseValue = 5;
          if (val == 0)
         pauseValue = 6;
          if (val == 1)
         pauseValue = 7;
          if (val == 2)
         pauseValue = 8;
          if (val == 3)
         pauseValue = 9;
          if (val == 4)
         pauseValue = 10;
          if (val == 5)
         pauseValue = 11;
          if (val == 6)
         pauseValue = 12;

          if (val > 6 || val < -5)
         JOptionPane.showMessageDialog(Pause,
                        "Enter a value in range -1 to 6");
      }
       });

   JPanel pas = new JPanel(new BorderLayout());

   JPanel pan = new JPanel();
   JPanel p = new JPanel();
   pan.setBackground(activeColor);
   pan.add(pause);
   p.add(pan);
   p.setBackground(activeColor);
   pas.add(p, BorderLayout.WEST);

   JPanel field = new JPanel();
   field.add(Pause);
   field.setBackground(activeColor);
   JPanel box = new JPanel();
   box.add(field);
   box.setBackground(activeColor);

   JPanel btn = new JPanel();
   JPanel bt = new JPanel();
   bt.add(set);
   bt.setBackground(activeColor);
   btn.setBackground(activeColor);
   btn.add(bt);

   pas.add(btn, BorderLayout.EAST);
   pas.add(box, BorderLayout.CENTER);
   pas.setBackground(activeColor);

   // Color m=Color(0,0,0);
   Line = BorderFactory.createLineBorder(brown);
   pas.setBorder(Line);
   */
        // Storage intervals
        JLabel siLabel = new JLabel("Visualization Intervals :");
        siLabel.setToolTipText(
                "<html>How often the long profile and cross-section will be saved:<br />" +
                        "a value of 1 means that they will be saved once (at the end<br />" +
                        "of simulation); a value of 6 means that they will be saved<br />" +
                        "at the end of 6 equal intervals over the simulation duration.</html>");
        siText = new JLabel("        6");
        Wilsim.m.storageIntervals = 6;
        siBar = new JScrollBar(Adjustable.VERTICAL,
                25 - Wilsim.m.storageIntervals, 1, 1, 25);
        // storInterval = 1;
        siBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                int siFactor = 25 - siBar.getValue();
                siText.setText("         " + String.valueOf(siFactor));
                Wilsim.m.storageIntervals = siFactor;
                //        storInterval = siBar.getValue();
            }
        });

        JPanel siPanel = new JPanel(new BorderLayout());
        siPanel.add(siLabel, BorderLayout.WEST);
        siPanel.add(siText, BorderLayout.CENTER);
        siPanel.add(siBar, BorderLayout.EAST);
        siPanel.setBackground(activeColor);

        Border siLine = BorderFactory.createLineBorder(brown);
        siPanel.setBorder(siLine);

        // Adding all the parameter panels to mail parameter panel
        parameter.add(Along_GW_Pan);
        parameter.add(kstr);
        parameter.add(kfact);
        parameter.add(clif);
        parameter.add(time);
        // parameter.add(pas);
        parameter.add(siPanel);


        //JPanel middle = new JPanel(new BorderLayout());

        //Create a split pane with the two scroll panes in it.
        JPanel middle = new JPanel(new BorderLayout());

        middle.add(viewPanel, BorderLayout.CENTER);
        middle.add(controlPanel, BorderLayout.EAST);

        UI.add(bannerPanel, BorderLayout.NORTH);
        UI.add(middle, BorderLayout.CENTER);

        middle.addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent evt) {
                Component c = (Component)evt.getSource();

                controlPanel.setPreferredSize(new Dimension(250, controlPanel.getSize().height));
            }
        });
        //viewPanel.setMinimumSize(new Dimension(400,600));
        // middle.add(viewPanel, BorderLayout.CENTER);
        // middle.add(controlPanel, BorderLayout.EAST);

        UI.add(bannerPanel, BorderLayout.NORTH);
        UI.add(middle, BorderLayout.CENTER);

        // for debugging
        // JPanel log = new JPanel();
        // log.add(Wilsim.i.bar);
        // UI.add(log, BorderLayout.SOUTH);

    } // createGUI

    private void saveCross() {
        //Wilsim.c.outputReady();
        //remember dir choosen
        //outputTopo(
                outputProfiles(
                        outputXSections(curDir)
                //)
        );

    }
    private File outputTopo(File lastDir) {
        boolean foundWrite = false;
        boolean toWrite = true;
        File file = lastDir;

        topofc.setCurrentDirectory(lastDir);

        if (Wilsim.m.storageIntervals < 0) return curDir;



        while (toWrite && !foundWrite) {
            try {
                int returnVal;
                returnVal = topofc.showSaveDialog(UI); // parent component to
                // // JFileChooser
                if (returnVal == JFileChooser.APPROVE_OPTION) { // OK button
                    // //
                    // pressed
                    // by user
                    file = topofc.getSelectedFile(); // get File
                    curDir = topofc.getCurrentDirectory();
                    System.out.println(curDir.toString());
                    // selected //
                    // by user

                    if (file.exists()) {
                        int overwrite;
                        overwrite = JOptionPane.showConfirmDialog(UI,
                                file.getName() + " already exists.  Overwrite?",
                                "Topographic File Confirmation",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (overwrite == JOptionPane.YES_OPTION)
                            foundWrite = true;
                    } else {
                        // New, unused file
                        foundWrite = true;
                    }
                } else {
                    // Cancel
                    toWrite = false;
                }
            } catch (Exception er) {
                er.getCause();
            }
        }
        if (toWrite) {

            try {

                float[][] arr = Wilsim.m.topoSave;


                if (!file.exists())
                    file.createNewFile();

                FileWriter fw = new FileWriter(file.getAbsoluteFile());

                BufferedWriter bw = new BufferedWriter(fw);
                int nIterations = Wilsim.m.storageIntervals;
                int j,i;
                for (j = 0; j < nIterations; j++) {
                for (i = 0; i < arr[0].length; i++) {

                        bw.write(String.valueOf(arr[j][i]) + ", ");
                }
                    bw.write("\n");
                }
                bw.close();
                // Wilsim.i.log.append("Done");
            } catch (Exception er) {
                er.getCause();
            }
        }

return curDir;
    }

    private File outputXSections(File lastDir) {
        boolean foundWrite = false;
        boolean toWrite = true;
        File file = lastDir;

        topofc.setCurrentDirectory(lastDir);

        if (Wilsim.m.storageIntervals < 0) return curDir;

        //Wilsim.i.log.append("XSectionManager.nXSections():  "
        //        + String.valueOf(XSectionManager.nXSections())+ "\n");

        while (toWrite && !foundWrite) {
            try {
                int returnVal;
                returnVal = xsfc.showSaveDialog(UI); // parent component to
                // // JFileChooser
                if (returnVal == JFileChooser.APPROVE_OPTION) { // OK button
                    // //
                    // pressed
                    // by user
                    file = xsfc.getSelectedFile(); // get File
                    curDir = xsfc.getCurrentDirectory();
                    // selected //
                    // by user

                    if (file.exists()) {
                        int overwrite;
                        overwrite = JOptionPane.showConfirmDialog(UI,
                                file.getName() + " already exists.  Overwrite?",
                                "Cross Section File Confirmation",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (overwrite == JOptionPane.YES_OPTION)
                            foundWrite = true;
                    } else {
                        // New, unused file
                        foundWrite = true;
                    }
                } else {
                    // Cancel
                    toWrite = false;
                }
            } catch (Exception er) {
                er.getCause();
            }
        }
        // Wilsim.i.log.append("File chosen\n");
        // Wilsim.i.log.append("toWrite: " + String.valueOf(toWrite) + "\n");
        if (toWrite) {
            // Wilsim.i.log.append("outputXSections:2\n");
            try {
                XSection xs = XSectionManager.getXSection(0);
                float[][] arr = xs.values;
                // Wilsim.i.log.append("Iterations: " + String.valueOf(arr.length) + "\n");
                // Wilsim.i.log.append("Profile length: " + String.valueOf(arr[0].length) + "\n");

                if (!file.exists())
                    file.createNewFile();
                // Wilsim.i.log.append("file: " + file.getAbsoluteFile() + "\n");
                FileWriter fw = new FileWriter(file.getAbsoluteFile());

                BufferedWriter bw = new BufferedWriter(fw);
          /*
          if(bw == null)
         Wilsim.i.log.append("null BufferedWriter\n");
          */
                int nIterations = xs.getNIterates();
                // Wilsim.i.log.append("nIterations: " + nIterations + "\n");
                for (int i = 0; i < arr[0].length; i++) {
                    int j;
                    for (j = 0; j < nIterations; j++) {
                        // Wilsim.i.log.append("[" + j + "][" + i + "]\n");
             /* 
             if(arr[j] == null)
            {
                Wilsim.i.log.append("output: null iteration " + j + "\n");
                continue;
            }
             if(i >= arr[j].length)
            {
                Wilsim.i.log.append("output: out of bounds [" + j + "][" + i + "]\n");
                continue;
            }
             */
                        bw.write(String.valueOf(arr[j][i]) + ", ");
                    }
         /*
         if(i >= arr[j].length)
             {
            Wilsim.i.log.append("output: out of bounds [" + j + "][" + i + "]\n");
            continue;
             }
         */
                    bw.write(String.valueOf(arr[j][i]) + "\n");
                }
                bw.close();
                // Wilsim.i.log.append("Done");
            } catch (Exception er) {
                er.getCause();
            }
        }
        return curDir;
    }

    private File outputProfiles(File lastDir) {
        boolean foundWrite = false;
        boolean toWrite = true;
        File file = lastDir;
        lpfc.setCurrentDirectory(lastDir);

        if (Wilsim.m.storageIntervals < 0) return curDir;

        while (toWrite && !foundWrite) {
            try {
                int returnVal;
                returnVal = lpfc.showSaveDialog(UI); // parent component to
                // // JFileChooser
                if (returnVal == JFileChooser.APPROVE_OPTION) { // OK button
                    // //
                    // pressed
                    // by user
                    file = lpfc.getSelectedFile(); // get File
                    curDir = lpfc.getCurrentDirectory();
                    // selected //
                    // by user

                    if (file.exists()) {
                        int overwrite;
                        overwrite = JOptionPane.showConfirmDialog(UI,
                                file.getName() + " already exists.  Overwrite?",
                                "Cross Section File Confirmation",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (overwrite == JOptionPane.YES_OPTION)
                            foundWrite = true;
                    } else {
                        // New, unused file
                        foundWrite = true;
                    }
                } else {
                    // Cancel
                    toWrite = false;
                }
            } catch (Exception er) {
                er.getCause();
            }
        }
        // Wilsim.i.log.append("File chosen\n");
        // Wilsim.i.log.append("toWrite: " + String.valueOf(toWrite) + "\n");
        if (toWrite) {

            // Wilsim.i.log.append("outputProfiles:2\n");
            try {
                Profile rv = Wilsim.m.river;

                // Last minute filter hack to get rid of spikes.  Ideally
                // shouldn't be needed.  To be investigated further
                final int thresh = 30;
                for (int k = 7; k >= 1; k -= 2) // Reduce wide peaks to narrow peaks
                    for (int j = 0; j < rv.getNIterates(); j++) {
                        for (int i = k; i < rv.n[j] - k; i++) {
                            float value;
                            if ((rv.values[j][i - k] - rv.values[j][i]) < -thresh
                                    && (rv.values[j][i + k] - rv.values[j][i]) < -thresh)
                                rv.values[j][i] = (rv.values[j][i + k] + rv.values[j][i - k]) / 2.0f;
                        }
                    }

                // Send to file
                if (!file.exists())
                    file.createNewFile();
                // Wilsim.i.log.append("file: " + file.getAbsoluteFile() + "\n");
                FileWriter fw = new FileWriter(file.getAbsoluteFile());

                BufferedWriter bw = new BufferedWriter(fw);
          /*
          if(bw == null)
         Wilsim.i.log.append("null BufferedWriter\n");
          */
                int nIterations = rv.getNIterates();
                //
                // This approach was taken due to initial uncertainty about the
                // equivalence of each iteration length
                //
                boolean doneFlag = false;
                int i = 0;
                // Wilsim.i.log.append("nIterations: " + nIterations + "\n");

                while (!doneFlag) {
                    doneFlag = true;
                    int j;
                    if (i < rv.n[0] - 1) doneFlag = false; // Another row exists

                    if (i < rv.n[0])
                        bw.write(String.valueOf(rv.distances[0][i]) + ", ");

                    for (j = 0; j < nIterations - 1; j++) {
                        // Wilsim.i.log.append("[" + j + "][" + i + "]\n");
                        //if(i < rv.n[j]-1) doneFlag = false ;  // Another row exists

                        if (i < rv.n[j]) {
                            // bw.write(String.valueOf(rv.distances[j][i]) + ", ");
                            bw.write(String.valueOf(rv.values[j][i]) + ", ");
                        } else
                            // bw.write(", , ");
                            bw.write(", ");
                    }

                    // bw.write(String.valueOf(rv.distances[j][i]) + ", ");
                    bw.write(String.valueOf(rv.values[j][i]) + "\n");

                    i++;
                }
                bw.close();
                // Wilsim.i.log.append("Done");
            } catch (Exception er) {
                er.getCause();
            }
        }
        return curDir;
    }
}