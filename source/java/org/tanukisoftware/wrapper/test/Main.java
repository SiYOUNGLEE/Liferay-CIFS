package org.tanukisoftware.wrapper.test;

/*
 * Copyright (c) 1999, 2009 Tanuki Software, Ltd.
 * http://www.tanukisoftware.com
 * All rights reserved.
 *
 * This software is the proprietary information of Tanuki Software.
 * You shall use it only in accordance with the terms of the
 * license agreement you entered into with Tanuki Software.
 * http://wrapper.tanukisoftware.org/doc/english/licenseOverview.html
 * 
 * 
 * Portions of the Software have been derived from source code
 * developed by Silver Egg Technology under the following license:
 * 
 * Copyright (c) 2001 Silver Egg Technology
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sub-license, and/or 
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 */

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import org.tanukisoftware.wrapper.WrapperActionServer;
import org.tanukisoftware.wrapper.WrapperManager;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.event.WrapperEventListener;

/**
 * This is a Test / Example program which can be used to test the
 *  main features of the Wrapper.
 * <p>
 * It is also an example of Integration Method #3, where you implement
 *  the WrapperListener interface manually.
 * <p>
 * <b>NOTE</b> that in most cases you will want to use Method #1, using the
 *  WrapperSimpleApp helper class to integrate your application.  Please
 *  see the <a href="http://wrapper.tanukisoftware.org/doc/english/integrate.html">integration</a>
 *  section of the documentation for more details.
 *
 * @author Leif Mortenson <leif@tanukisoftware.com>
 */
public class Main
    extends AbstractActionApp
    implements WrapperListener
{
    private WrapperActionServer m_actionServer;
    
    private MainFrame m_frame;
    
    private ActionRunner m_actionRunner;
    
    private List m_listenerFlags;
    private TextField m_serviceName;
    
    /*---------------------------------------------------------------
     * Constructors
     *-------------------------------------------------------------*/
    private Main() {
    }
    
    /*---------------------------------------------------------------
     * Inner Classes
     *-------------------------------------------------------------*/
    private class MainFrame extends Frame implements ActionListener, WindowListener
    {
        /**
         * Serial Version UID.
         */
        private static final long serialVersionUID = -3847376282833547574L;

        MainFrame()
        {
            super( "Wrapper Test Application" );
            
            init();
            
            setLocation( 10, 10 );
            setSize( 750, 480 );
            
            setResizable( true );
        }
        
        private void init()
        {
            GridBagLayout gridBag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            
            Panel panel = new Panel();
            panel.setLayout( gridBag );
            
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.add( panel );
            scrollPane.getHAdjustable().setUnitIncrement( 20 );
            scrollPane.getVAdjustable().setUnitIncrement( 20 );
            
            setLayout( new BorderLayout() );
            add( scrollPane, BorderLayout.CENTER );
            
            buildCommand( panel, gridBag, c, "Stop(0)", "stop0",
                "Calls WrapperManager.stop( 0 ) to shutdown the JVM and Wrapper with a success exit code." );
            
            buildCommand( panel, gridBag, c, "Stop(1)", "stop1",
                "Calls WrapperManager.stop( 1 ) to shutdown the JVM and Wrapper with a failure exit code." );
            
            buildCommand( panel, gridBag, c, "Exit(0)", "exit0",
                "Calls System.exit( 0 ) to shutdown the JVM and Wrapper with a success exit code." );
            
            buildCommand( panel, gridBag, c, "Exit(1)", "exit1",
                "Calls System.exit( 1 ) to shutdown the JVM and Wrapper with a failure exit code." );
            
            buildCommand( panel, gridBag, c, "StopImmediate(0)", "stopimmediate0",
                "Calls WrapperManager.stopImmediate( 0 ) to immediately shutdown the JVM and Wrapper with a success exit code." );
            
            buildCommand( panel, gridBag, c, "StopImmediate(1)", "stopimmediate1",
                "Calls WrapperManager.stopImmediate( 1 ) to immediately shutdown the JVM and Wrapper with a failure exir code." );
            
            buildCommand( panel, gridBag, c, "StopAndReturn(0)", "stopandreturn0",
                "Calls WrapperManager.stopAndReturn( 0 ) to shutdown the JVM and Wrapper with a success exit code." );
            
            buildCommand( panel, gridBag, c, "Nested Exit(1)", "nestedexit1",
                "Calls System.exit(1) within WrapperListener.stop(1) callback." );
            
            buildCommand( panel, gridBag, c, "Halt(0)", "halt0",
                "Calls Runtime.getRuntime().halt(0) to kill the JVM, the Wrapper will restart it." );
            
            buildCommand( panel, gridBag, c, "Halt(1)", "halt1",
                "Calls Runtime.getRuntime().halt(1) to kill the JVM, the Wrapper will restart it." );
            
            buildCommand( panel, gridBag, c, "Restart()", "restart",
                "Calls WrapperManager.restart() to shutdown the current JVM and start a new one." );
            
            buildCommand( panel, gridBag, c, "RestartAndReturn()", "restartandreturn",
                "Calls WrapperManager.restartAndReturn() to shutdown the current JVM and start a new one." );
            
            buildCommand( panel, gridBag, c, "Access Violation", "access_violation",
                "Attempts to cause an access violation within the JVM, relies on a JVM bug and may not work." );
            
            buildCommand( panel, gridBag, c, "Native Access Violation", "access_violation_native",
                "Causes an access violation using native code, the JVM will crash and be restarted." );
            
            buildCommand( panel, gridBag, c, "Simulate JVM Hang", "appear_hung",
                "Makes the JVM appear to be hung as viewed from the Wrapper, it will be killed and restarted." );
            
            buildCommand( panel, gridBag, c, "Ignore Control Events", "ignore_events",
                "Makes this application ignore control events.  It will not shutdown in response to CTRL-C.  The Wrapper will still respond." );
            
            buildCommand( panel, gridBag, c, "Request Thread Dump", "dump",
                "Calls WrapperManager.requestThreadDump() to cause the JVM to dump its current thread state." );
            
            buildCommand( panel, gridBag, c, "System.out Deadlock", "deadlock_out",
                "Simulates a failure mode where the System.out object has become deadlocked." );
            
            buildCommand( panel, gridBag, c, "Poll Users", "users",
                "Begins calling WrapperManager.getUser() and getInteractiveUser() to monitor the current and interactive users." );
            
            buildCommand( panel, gridBag, c, "Poll Users with Groups", "groups",
                "Same as above, but includes information about the user's groups." );
            
            buildCommand( panel, gridBag, c, "Console", "console", "Prompt for Actions in the console." );
            
            buildCommand( panel, gridBag, c, "Idle", "idle", "Run idly." );
            
            buildCommand( panel, gridBag, c, "Dump Properties", "properties",
                "Dumps all System Properties to the console." );
            
            buildCommand( panel, gridBag, c, "Dump Configuration", "configuration",
                "Dumps all Wrapper Configuration Properties to the console." );
            
            
            m_listenerFlags = new List( 2, true );
            m_listenerFlags.add( "Service" );
            m_listenerFlags.add( "Control" );
            m_listenerFlags.add( "Logging" );
            m_listenerFlags.add( "Core" );
            
            Panel flagPanel = new Panel();
            flagPanel.setLayout( new BorderLayout() );
            flagPanel.add( new Label( "Event Flags: " ), BorderLayout.WEST );
            flagPanel.add( m_listenerFlags, BorderLayout.CENTER );
            flagPanel.setSize( 100, 10 );
            
            Panel flagPanel2 = new Panel();
            flagPanel2.setLayout( new BorderLayout() );
            flagPanel2.add( flagPanel, BorderLayout.WEST );
            
            buildCommand( panel, gridBag, c, "Update Event Listener", "listener", flagPanel2 );
            
            buildCommand( panel, gridBag, c, "Service List", "service_list", "Displays a list of registered services on Windows." );
            
            m_serviceName = new TextField( "testwrapper" );
            
            Panel servicePanel = new Panel();
            servicePanel.setLayout( new BorderLayout() );
            servicePanel.add( new Label( "Interrogate Service.  Service name: " ), BorderLayout.WEST );
            servicePanel.add( m_serviceName, BorderLayout.CENTER );
            
            Panel servicePanel2 = new Panel();
            servicePanel2.setLayout( new BorderLayout() );
            servicePanel2.add( servicePanel, BorderLayout.WEST );
            
            buildCommand( panel, gridBag, c, "Service Interrogate", "service_interrogate", servicePanel2 );
            
            buildCommand( panel, gridBag, c, "Service Start", "service_start", "Starts the above service." );
            
            buildCommand( panel, gridBag, c, "Service Stop", "service_stop", "Stops the above service." );
            
            buildCommand( panel, gridBag, c, "Service User Code", "service_user", "Sends a series of user codes to the above service." );
            
            buildCommand( panel, gridBag, c, "GC", "gc", "Performs a GC sweep." );
            
            buildCommand( panel, gridBag, c, "Is Professional?", "is_professional", "Prints true if this is a Professional Edition." );
            
            buildCommand( panel, gridBag, c, "Is Standard?", "is_standard", "Prints true if this is a Standard Edition." );
            
            addWindowListener( this );
        }
        
        private void buildCommand( Container container,
                                   GridBagLayout gridBag,
                                   GridBagConstraints c,
                                   String label,
                                   String command,
                                   Object description )
        {
            Button button = new Button( label );
            button.setActionCommand( command );
            
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 1;
            gridBag.setConstraints( button, c );
            container.add( button );
            button.addActionListener(this);
            
            c.gridwidth = GridBagConstraints.REMAINDER;
            Component desc;
            if ( description instanceof String )
            {
                desc = new Label( (String)description );
            }
            else if ( description instanceof Component )
            {
                desc = (Component)description;
            }
            else
            {
                desc = new Label( description.toString() );
            }
            
            gridBag.setConstraints( desc, c );
            container.add( desc );
        }
        
        /**************************************************************************
         * ActionListener Methods
         *************************************************************************/
        public void actionPerformed( ActionEvent event )
        {
            String action = event.getActionCommand();
            if ( action.equals( "listener" ) )
            {
                // Create the mask.
                long mask = 0;
                String[] flags = m_listenerFlags.getSelectedItems();
                for ( int i = 0; i < flags.length; i++ )
                {
                    String flag = flags[i];
                    if ( flag.equals( "Service" ) )
                    {
                        mask |= WrapperEventListener.EVENT_FLAG_SERVICE;
                    }
                    else if ( flag.equals( "Control" ) )
                    {
                        mask |= WrapperEventListener.EVENT_FLAG_CONTROL;
                    }
                    else if ( flag.equals( "Logging" ) )
                    {
                        mask |= WrapperEventListener.EVENT_FLAG_LOGGING;
                    }
                    else if ( flag.equals( "Core" ) )
                    {
                        mask |= WrapperEventListener.EVENT_FLAG_CORE;
                    }
                }
                
                setEventMask( mask );
            }
            
            setServiceName( m_serviceName.getText() );
            
            Main.this.doAction( action );
        }
        
        /**************************************************************************
         * WindowListener Methods
         *************************************************************************/
        public void windowOpened( WindowEvent e )
        {
        }
        
        public void windowClosing( WindowEvent e )
        {
            WrapperManager.stopAndReturn( 0 );
        }
        
        public void windowClosed( WindowEvent e )
        {
        }
        
        public void windowIconified( WindowEvent e )
        {
        }
        
        public void windowDeiconified( WindowEvent e )
        {
        }
        
        public void windowActivated( WindowEvent e )
        {
        }
        
        public void windowDeactivated( WindowEvent e )
        {
        }
    }

    private class ActionRunner implements Runnable {
        private String m_action;
        private boolean m_alive;
        
        public ActionRunner(String action) {
            m_action = action;
            m_alive = true;
        }
    
        public void run() {
            // Wait for a second so that the startup will complete.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            
            if (!Main.this.doAction(m_action)) {
                printHelp("\"" + m_action + "\" is an unknown action.");
                WrapperManager.stop(0);
                return;
            }
    
            while (m_alive) {
                // Idle some
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    
        public void endThread( ) {
            m_alive = false;
        }
    }
    
    /*---------------------------------------------------------------
     * WrapperListener Methods
     *-------------------------------------------------------------*/
    public Integer start( String[] args )
    {
        String command;
        
        System.out.println( "TestWrapper: start()" );

        prepareSystemOutErr();
        
        if ( args.length <= 0 )
        {
            System.out.println( "TestWrapper: An action was not specified.  Default to \"dialog\".  Use \"help\" for list of actions." );
            command = "dialog";
        }
        else
        {
            command = args[0];
        }
        
        if ( command.equals( "help" ) ) {
            printHelp( null );
            return null;
        }
        
        try
        {
            int port = 9999;
            m_actionServer = new WrapperActionServer( port );
            m_actionServer.enableShutdownAction( true );
            m_actionServer.enableHaltExpectedAction( true );
            m_actionServer.enableRestartAction( true );
            m_actionServer.enableThreadDumpAction( true );
            m_actionServer.enableHaltUnexpectedAction( true );
            m_actionServer.enableAccessViolationAction( true );
            m_actionServer.enableAppearHungAction( true );
            m_actionServer.start();
            
            System.out.println( "TestWrapper: ActionServer Enabled. " );
            System.out.println( "TestWrapper:   Telnet localhost 9999" );
            System.out.println( "TestWrapper:   Commands: " );
            System.out.println( "TestWrapper:     S: Shutdown" );
            System.out.println( "TestWrapper:     H: Expected Halt" );
            System.out.println( "TestWrapper:     R: Restart" );
            System.out.println( "TestWrapper:     D: Thread Dump" );
            System.out.println( "TestWrapper:     U: Unexpected Halt (Simulate crash)" );
            System.out.println( "TestWrapper:     V: Access Violation (Actual crash)" );
            System.out.println( "TestWrapper:     G: Make the JVM appear to be hung." );
            System.out.println( "TestWrapper:" );
        }
        catch ( java.io.IOException e )
        {
            System.out.println( "TestWrapper: Unable to open the action server socket: " + e.getMessage() );
            System.out.println( "TestWrapper:" );
        }
        
        if ( command.equals( "dialog" ) )
        {
            System.out.println( "TestWrapper: Showing dialog..." );
            
            try
            {
                m_frame = new MainFrame();
                m_frame.setVisible( true );
            }
            catch ( java.lang.InternalError e )
            {
                System.out.println( "TestWrapper: " );
                System.out.println( "TestWrapper: ERROR - Unable to display the GUI:" );
                System.out.println( "TestWrapper:           " + e.toString() );
                System.out.println( "TestWrapper: " );
                System.out.println( "TestWrapper: Fall back to the \"console\" action." );
                command = "console";
            }
            catch ( java.awt.AWTError e )
            {
                System.out.println( "TestWrapper: " );
                System.out.println( "TestWrapper: ERROR - Unable to display the GUI:" );
                System.out.println( "TestWrapper:           " + e.toString() );
                System.out.println( "TestWrapper: " );
                System.out.println( "TestWrapper: Fall back to the \"console\" action." );
                command = "console";
            }
           catch ( java.lang.UnsupportedOperationException e )
            {
                // java.awt.HeadlessException does not exist in Java versions prior to 1.4
                if ( e.getClass().getName().equals( "java.awt.HeadlessException" ) ) 
                {
                    System.out.println( "TestWrapper: " );
                    System.out.println( "TestWrapper: ERROR - Unable to display the GUI:" );
                    System.out.println( "TestWrapper:           " + e.toString() );
                    System.out.println( "TestWrapper: " );
                    System.out.println( "TestWrapper: Fall back to the \"console\" action." );
                    command = "console";
                }
                else 
                {
                    throw e;
                }
            }
        }
        
        if ( !command.equals( "dialog" ) )
        {
            // * * Start the action thread
            m_actionRunner = new ActionRunner( command );
            Thread actionThread = new Thread( m_actionRunner );
            actionThread.start();
        }
        
        return null;
    }
    
    public int stop( int exitCode )
    {
        System.out.println( "TestWrapper: stop(" + exitCode + ")" );
        
        if ( m_actionServer != null )
        {
            try
            {
                m_actionServer.stop();
            }
            catch ( Exception e )
            {
                System.out.println( "TestWrapper: Unable to stop the action server: " + e.getMessage() );
            }
        }
        
        if ( m_frame != null )
        {
            if ( !WrapperManager.hasShutdownHookBeenTriggered() )
            {
                m_frame.setVisible( false );
                m_frame.dispose();
            }
            m_frame = null;
        }
        
        if ( isNestedExit() )
        {
            System.out.println( "TestWrapper: calling System.exit(" + exitCode + ") within stop." );
            System.exit( exitCode );
        }
        
        return exitCode;
    }
    
    public void controlEvent( int event )
    {
        System.out.println( "TestWrapper: controlEvent(" + event + ")" );
        
        if ( event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT )
        {
            if ( WrapperManager.isLaunchedAsService() || WrapperManager.isIgnoreUserLogoffs() )
            {
                System.out.println( "TestWrapper:   Ignoring logoff event" );
                // Ignore
            }
            else if ( !ignoreControlEvents() )
            {
                WrapperManager.stop( 0 );
            }
        }
        else if ( event == WrapperManager.WRAPPER_CTRL_C_EVENT )
        {
            if ( !ignoreControlEvents() ) {
                //WrapperManager.stop(0);
                
                // May be called before the runner is started.
                if (m_actionRunner != null) {
                    m_actionRunner.endThread();
                }
            }
        }
        else
        {
            if ( !ignoreControlEvents() )
            {
                WrapperManager.stop( 0 );
            }
        }
    }
    
    /*---------------------------------------------------------------
     * Static Methods
     *-------------------------------------------------------------*/
    /**
     * Prints the usage text.
     *
     * @param error_msg Error message to write with usage text
     */
    private static void printHelp( String errorMsg )
    {
        System.err.println( "USAGE" );
        System.err.println( "" );
        System.err.println( "TestWrapper <action>" );
        printActions();
        System.err.println( "  Interactive:" );
        System.err.println( "   dialog                   : Shows the dialog interface" );
        System.err.println( "[EXAMPLE]" );
        System.err.println( "   TestAction access_violation_native " );
        System.err.println( "" );
        if ( errorMsg != null )
        {
            System.err.println( "ERROR: " + errorMsg );
            System.err.println( "" );
        }
        
        System.exit( 1 );
    }
    
    /*---------------------------------------------------------------
     * Main Method
     *-------------------------------------------------------------*/
    /**
     * IMPORTANT: Please read the Javadocs for this class at the top of the
     *  page before you start to use this class as a template for integrating
     *  your own application.  This will save you a lot of time.
     */
    public static void main( String[] args )
    {
        System.out.println( "TestWrapper: Initializing..." );
        
        // Start the application.  If the JVM was launched from the native
        //  Wrapper then the application will wait for the native Wrapper to
        //  call the application's start method.  Otherwise the start method
        //  will be called immediately.
        WrapperManager.start( new Main(), args );
    }
}

