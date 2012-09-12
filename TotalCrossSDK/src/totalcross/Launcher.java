/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 1998, 1999 Wabasoft <www.wabasoft.com>                         *
 *  Copyright (C) 2000 Dave Slaughter                                            *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    *
 *  A copy of this license is located in file license.txt at the root of this    *
 *  SDK or can be downloaded here:                                               *
 *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     *
 *                                                                               *
 *********************************************************************************/



package totalcross;

/*
 * Note: Everything that calls TotalCross code in these classes must be
 * synchronized with respect to the Applet uiLock object to allow TotalCross
 * programs to be single threaded. This is because of the multi-threaded
 * nature of Java and because timers use multiple threads.
 *
 * Because all calls into TotalCross are synchronized and users can't call this code,
 * they can't deadlock the program in any way. If we moved the synchronization
 * into TotalCross code, we would have the possibility of deadlock.
 */

import java.awt.*;
import java.awt.Insets;
import java.awt.event.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.awt.image.*;
import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.zip.*;

import totalcross.io.*;
import totalcross.io.IOException;
import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.event.*;
import totalcross.ui.event.KeyEvent;
import totalcross.util.*;
import totalcross.util.zip.*;

/** Represents the applet or application used as a Java Container to make possible run TotalCross at the desktop. */

public class Launcher extends java.applet.Applet implements WindowListener, KeyListener, java.awt.event.MouseListener, MouseMotionListener, ComponentListener
{
   public static Launcher instance;
   public static boolean isApplication;
   public static boolean terminateIfMainClass = true;
   public String commandLine = "";
   public int threadCount;
   public Hashtable htOpenedAt = new Hashtable(31); // guich@200b4_82
   public IntHashtable keysPressed = new IntHashtable(129);
   public MainWindow mainWindow;
   public boolean showKeyCodes;
   public Hashtable htAttachedFiles = new Hashtable(5); // guich@566_28
   public static int userFontSize = -1;

   private int toBpp = -1;
   private int toWidth = -1;
   private int toHeight = -1;
   private String className;
   private boolean appletInitialized; // guich@500_1
   private LauncherFrame frame;
   private int toUI=-1; // guich@573_6: since now we have 4 styles, select the target one directly.
   private double toScale = -1;
   private int toX=-1,toY=-1;
   private WinTimer winTimer;
   private boolean started; // guich@120
   private boolean destroyed; // guich@230_24
   private boolean settingsFilled;
   private int[] screenPixels = new int[0];
   private int lookupR[], lookupG[], lookupB[], lookupGray[];
   private int pal685[];
   private Class _class; // used by the openInputStream method.
   protected MemoryImageSource screenMis;
   protected java.awt.Image screenImg;
   private AlertBox alert;
   private String frameTitle;
   private String crid4settings; // prevent from having two different crids for loading and storing the settings.
   private StringBuffer mmsb = new StringBuffer(32);
   private TCEventThread eventThread;
   private boolean isMainClass;
   private boolean isDemo;

   public Launcher()
   {
      totalcross.sys.Settings.showDesktopMessages = false; // guich@500_1: avoid messages when calling retroguard
      instance  = this;
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
      try {Runtime.runFinalizersOnExit(true);} catch (Throwable t) {}
      //try {System.runFinalizersOnExit(true);} catch (Throwable t) {} // guich@300_31
   }

   public void destroy()
   {
      if (mainWindow == null || destroyed)
         return;
      destroyed = true;
      eventThread.invokeInEventThread(true, new Runnable()
      {
        public void run()
        {
          mainWindow.appEnding();
          System.runFinalization();
          storeSettings();
        }
      });
      winTimer.stopGracefully(); // timer must be running when appEnding is called
   }

   private void runtimeInstructions()
   {
      System.out.println("Current path: "+System.getProperty("user.dir"));
      // print instructions
      System.out.println("===================================");
      System.out.println("Device key emulations:");
      System.out.println("F1-F4 : HARD1 to HARD4");
      System.out.println("F5 : COMMAND");
      System.out.println("F6 : MENU");
      System.out.println("F7 : CALC");
      System.out.println("F8 : FIND");
      System.out.println("F9 : CHANGE ORIENTATION");
      System.out.println("F10: LAUNCH (HOME)");
      System.out.println("F11: OPEN KEYBOARD");
      System.out.println("F12: ACTION (Center button press)"); // guich@400
      System.out.println("===================================");
   }

   public void init()
   {
      boolean showInstructionsOnError = true;
      appletInitialized = true; // guich@500_1
      totalcross.sys.Settings.showDesktopMessages = true; // guich@500_1: redo the messages.
      try
      {
         alert = new AlertBox();
         // NOTE: getParameter() and size() don't work in a
         // java applet constructor, so we need to call them here
         if (!isApplication)
         {
            String arguments = getParameter("arguments");
            if (arguments == null)
               throw new Exception("Error: you must suply an 'arguments' property with all the argments to create the application");
            String []args = tokenizeString(arguments,' ');
            parseArguments(args);
         }

         fillSettings();

         try
         {
            _class = getClass(); // guich@500_1: we can use ourselves
            // if the user pass: tc/samples/ui/image/test/ImageTest.class, change to tc.samples.ui.image.test.ImageTest
            if (className.endsWith(".class"))
               className = className.substring(0,className.length()-6);
            className = className.replace('/','.');

            Class c = _class.forName(className); // guich@200b2: applets dont let you specify the path. it must be set in the codebase param - guich@520_9: changed from Class. to getClass
            showInstructionsOnError = false;
            isMainClass = checkIfMainClass(c); // guich@tc122_4
            if (!isMainClass)
               runtimeInstructions();
            Object o = c.newInstance();
            if (o instanceof MainClass && !(o instanceof MainWindow))
            {
               ((MainClass)o).appStarting(0);
               ((MainClass)o).appEnding();
               if (terminateIfMainClass)
                  System.exit(0); // currently we just exit after the constructor is called in a Non-GUI (headless) application
               else
                  return;
            }
            mainWindow = (MainWindow)o;
            // NOTE: java will call a partially constructed object if show() is called before all the objects are constructed
            if (isApplication)
            {
               frame = new LauncherFrame();
               requestFocus();
            }
            else
               setLayout(new java.awt.BorderLayout());
            if (toUI != -1) // now is safe
               mainWindow.setUIStyle((byte)toUI);
         }
         catch (LinkageError le)
         {
            System.out.println("Fatal Error when running applet: there is an error in the constructor of the class "+className+" and it could not be instantiated. Stack trace: ");
            le.printStackTrace();
            exit(0);
         }
         catch (ClassCastException cce)
         {
            System.out.println("Error: class "+className+" does not extend MainClass nor MainWindow!");
            cce.printStackTrace();
            exit(-1);
         }
         catch (ClassNotFoundException cnfe)
         {
            System.out.println("The MainWindow class specified was not found: "+className+"\n\nCommon causes are:");
            System.out.println(". The name is misspelled: java is case sensitive, so UIGadgets is not the same of uigadgets");
            if (className.indexOf('.') < 0) System.out.println(". The package name is incorrect: if you declared a class like: \n     package com.foo.bar;\n     public class "+className+"\n  then you must specify com.foo.bar."+className+" as the main class; only specifying "+className+" is not enough.");
            System.out.println(". Its location was not added to the classpath: if you're running from the prompt, be sure to add the path where your application is to the CLASSPATH argument. For example, if the class is in the current path, add a . specifying the current path: java -classpath .;tc.jar totalcross.Launcher "+className);
            exit(-1);
         }
      }
      catch (Exception ee)
      {
         if (showInstructionsOnError) showInstructions();
         ee.printStackTrace();
      } // guich@120
   }

   private static boolean checkIfMainClass(Class c)
   {
      Class []interfaces = c.getInterfaces();
      if (interfaces != null)
         for (int i = 0; i < interfaces.length; i++)
            if (interfaces[i].getName().equals("totalcross.MainClass"))
               return true;
      return false;
   }

   private class LauncherFrame extends Frame
   {
      private Insets insets;

      public LauncherFrame()
      {
         setBackground(new java.awt.Color(getScreenColor(mainWindow.getBackColor())));
         setResizable(Settings.resizableWindow); // guich@570_54
         setLayout(null);
         add(instance);
         addNotify(); // without this, the insets will not be correctly set.
         insets = getInsets();
         if (insets == null)
            insets = new Insets(0,0,0,0);
         setFrameSize(toWidth, toHeight, true);
         setLocation(toX,toY);
         super.setTitle(frameTitle != null ? frameTitle : mainWindow.getClass().getName());
         setVisible(true);
         addWindowListener(instance);
         addComponentListener(instance);
      }

      public void update(java.awt.Graphics g) {}

      public void setFrameSize(int toWidth, int toHeight, boolean set)
      {
         if (set)
            setSize((int)(toWidth*toScale) + insets.left + insets.right, (int)(toHeight*toScale )+ insets.top + insets.bottom);
         instance.setBounds(insets.left,insets.top,(int)(toWidth*toScale), (int)(toHeight*toScale));
      }
   };

   private class WinTimer extends java.lang.Thread
   {
      private int interval;
      private boolean shouldStop;

      public void run()
      {
        // NOTE: because we have created an official event queue/thread, which now
        // resembles the device event queue much more closely, we must be
        // sure that all timers and TC threads are run in that event thread.  This
        // will ensure that such things as blinking cursors will continue to work
        // if there is a blocking modal dialog open.  This also means that TC JDK
        // threads will act much more like the device threads... in that, threads
        // will not run unless a message pump is running.
         while (!shouldStop)
         {
            boolean doTick = true;
            int millis = interval;
            if (millis <= 0)
            {
               // NOTE: Netscape navigator doesn't support interrupt()
               // so we sleep here less than we would normally need to
               // (1 second) if we're not doing anything to check if
               // the timer should start in case interrupt didn't work
               millis = 1 * 1000;
               doTick = false;
            }
            // guich@200b4_84: implement the simple thread
            long first = System.currentTimeMillis();
            while ((System.currentTimeMillis()-first) < millis)
            {
               try
               {
                  sleep(millis);
                  doTick = true; // guich@230_3
                  break; // guich@230_3
               }
               catch (InterruptedException e)
               {
                  doTick = false;
                  break; // guich@230_4
               }
            }
            if (doTick && eventThread != null)
            {
                eventThread.invokeInEventThread(false, new Runnable() {
                public void run()
                {
                   synchronized (instance) // guich@510_2: synchronize the repaint with the timer
                   {
                      mainWindow._onTimerTick(true);
                   }
                }
              });
            }
         }
      }

      void setInterval(int millis)
      {
         //System.out.println("setInterval "+millis);
         interval = millis<55 ? 55 : millis; // guich@230_3
         interrupt();
      }

      void stopGracefully()
      {
         // NOTE: It's not a good idea to call stop() on threads since
         // it can cause the JVM to crash.
         shouldStop = true;
         interrupt();
      }
   }

   public boolean eventIsAvailable()
   {
      return eventThread.eventAvailable();
   }

   void startApp()
   {
      eventThread = new TCEventThread(mainWindow);
      if (!started) // guich@120 - make sure that the component is available for drawing when starting the application. called by paint.
      {
         try
         {
           eventThread.invokeInEventThread(true, new Runnable()
           {
             public void run() { while (mainWindow == null) Vm.sleep(1); mainWindow.appStarting(isDemo ? 80 : -1); } // guich@200b4_107 - guich@570_3: check if mainWindow is not null to avoid problems when running on Linux. seems that the paint event is being generated before the start one.
           });
         } catch (Throwable e) {e.printStackTrace();}
         started = true;
      }
   }

   private static void showInstructions()
   {
      System.out.println("Possible Arguments (in any order and case insensitive). Default is marked as *");
      System.out.println("   /scr WIDTHxHEIGHT     : sets the width and height");
      System.out.println("   /scr WIDTHxHEIGHTxBPP : sets the width, height and bits per pixel (8, 16, 24 or 32)");
      System.out.println("   /scr PalmLo      : Palm OS low     (same of /scr 160x160x8)");
      System.out.println("*  /scr PalmHI      : Palm OS high    (same of /scr 320x320x16)");
      System.out.println("   /scr PalmTall    : Palm OS tall    (same of /scr 320x480x16)");
      System.out.println("   /scr PalmWide    : Palm OS wide    (same of /scr 480x320x16)");
      System.out.println("   /scr WinCE       : Windows CE      (same of /scr 240x320x16)");
      System.out.println("   /scr Win32       : Windows 32      (same of /scr 240x320x24)");
      System.out.println("   /scr bbLo        : BlackBerry low  (same of /scr 320x240x16)");
      System.out.println("   /scr bbBold      : BlackBerry Bold (same of /scr 480x360x16)");
      System.out.println("   /scr bbStorm     : BlackBerry Storm(same of /scr 480x320x16)");
      System.out.println("   /scr iPhone      : iPhone          (same of /scr 320x480x24)");
      System.out.println("   /scr android     : Android         (same of /scr 320x480x16)");
      System.out.println("   /pos x,y         : Sets the openning position of the application");
      System.out.println("*  /uiStyle WinCE   : Windows CE user interface style");
      System.out.println("   /uiStyle PalmOS  : Palm OS user interface style");
      System.out.println("   /uiStyle Flat    : Flat user interface style");
      System.out.println("   /uiStyle Vista   : Vista user interface style");
      System.out.println("   /uiStyle Android : Android user interface style");
      System.out.println("   /penlessDevice   : acts as a device that has no touchscreen.");
      System.out.println("   /fingerTouch     : acts as a device that uses a finger instead of a pen.");
      System.out.println("   /unmovablesip    : acts as a device whose SIP is unmovable (like in Android and iPhone).");
      System.out.println("   /geofocus        : enables geographical focus.");
      System.out.println("   /keypadOnly      : acts as a device that has only the 0-9*# keys");
      System.out.println("   /virtualKeyboard : shows the virtual keyboard when in an Edit or a MultiEdit");
      System.out.println("   /showmousepos    : shows the mouse position.");
      System.out.println("   /bpp 8           : emulates 8  bits per pixel screens (256 colors)");
      System.out.println("   /bpp 16          : emulates 16 bits per pixel screens (64K colors)");
      System.out.println("   /bpp 24          : emulates 24 bits per pixel screens (16M colors)");
      System.out.println("   /bpp 32          : emulates 32 bits per pixel screens (16M colors without transparency)");
      System.out.println("   /scale <0.1 to 4>: scales the screen, magnifying the contents using a smooth scale.");
      System.out.println("   /dataPath <path> : sets where the PDB and media files are stored");
      System.out.println("   /cmdLine <...>   : the rest of arguments-1 are passed as the command line");
      System.out.println("   /fontSize <size> : set the default font size to the one passed as parameter");
      System.out.println("The class name that extends MainWindow must always be the last argument");
   }

   public static void main(String args[])
   {
      if (args.length == 0)
      {
         showInstructions();
         return;
      }
      isApplication = true;
      Launcher app = new Launcher();
      app.parseArguments(args);
      app.init();
   }

   private int toInt(String s) // Convert.toInt can't be used here, otherwise, the settings will be set too early!
   {
      try {return Integer.parseInt(s);} catch (Exception e) {return 0;}
   }
   private double toDouble(String s) 
   {
      try {return Double.parseDouble(s);} catch (Exception e) {return 0;}
   }

   protected void parseArguments(String[] args)
   {
      int n = args.length-1,i=0;
      String newDataPath = null;
      try
      {
         className = args[n];
         for (i = 0; i < n; i++)
         {
            if (args[i].equalsIgnoreCase("/fontsize"))
               userFontSize = toInt(args[++i]);
            else
            if (args[i].equalsIgnoreCase("/dataPath"))
            {
               newDataPath = args[++i];
               System.out.println("Data path is "+newDataPath);
            }
            else
            if (args[i].equalsIgnoreCase("/scr")) /* /scr 320x320  or  /scr 320x320x8 */
            {
               String next = args[++i];
               if (next.equalsIgnoreCase("palmLo"))
               {
                  toWidth = toHeight = 160; toBpp = 8;
               }
               else
               if (next.equalsIgnoreCase("palmHi"))
               {
                  toWidth = toHeight = 320; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("bbLo"))
               {
                  toWidth = 320; toHeight = 240; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("bbBold"))
               {
                  toWidth = 480; toHeight = 320; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("bbStorm"))
               {
                  toWidth = 480; toHeight = 360; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("palmTall"))
               {
                  toWidth = 320; toHeight = 480; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("palmWide"))
               {
                  toWidth = 480; toHeight = 320; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("wince"))
               {
                  toWidth = 240; toHeight = 320; toBpp = 16;
               }
               else
               if (next.equalsIgnoreCase("win32"))
               {
                  toWidth = 240; toHeight = 320; toBpp = 24;
               }
               else
               if (next.equalsIgnoreCase("iPhone"))
               {
                  toWidth = 320; toHeight = 480; toBpp = 24;
               }
               else
               if (next.equalsIgnoreCase("android"))
               {
                  toWidth = 320; toHeight = 480; toBpp = 16;
               }
               else
               {
                  String []scr = tokenizeString(next.toLowerCase(),'x');
                  if (scr.length == 1)
                     throw new Exception();
                  toWidth = toInt(scr[0]);
                  toHeight = toInt(scr[1]);
                  if (scr.length == 3)
                     toBpp = toInt(scr[2]);
               }
               System.out.println("Screen is "+toWidth+"x"+toHeight+"x"+toBpp);
            }
            else
            if (args[i].equalsIgnoreCase("/pos")) /* x,y */
            {
               String []scr = tokenizeString(args[++i].toLowerCase(),',');
               if (scr.length == 1)
                  throw new Exception();
               toX = toInt(scr[0]);
               toY = toInt(scr[1]);
            }
            else
            if (args[i].equalsIgnoreCase("/cmdline"))
            {
               commandLine = "";
               while (++i < n)
                  commandLine += args[i] + " ";
               commandLine = commandLine.trim();
               System.out.println("Command line is '"+commandLine+"'");
            }
            else
            if (args[i].equalsIgnoreCase("/uiStyle"))
            {
               String next = args[++i];
               if (next.equalsIgnoreCase("PalmOS"))
                  toUI = Settings.PalmOS;
               else
               if (next.equalsIgnoreCase("Flat"))
                  toUI = Settings.Flat;
               else
               if (next.equalsIgnoreCase("Vista"))
                  toUI = Settings.Vista;
               else
               if (next.equalsIgnoreCase("WinCE")) // guich@580_33
                  toUI = Settings.WinCE;
               else
               if (next.equalsIgnoreCase("Android")) // guich@580_33
                  toUI = Settings.Android;
               else
                  throw new Exception();
               System.out.println("UI style is "+toUI);
            }
            else
            if (args[i].equalsIgnoreCase("/penlessDevice")) // guich@573_20
            {
               Settings.keyboardFocusTraversable = true;
               System.out.println("Penless device is on");
            }
            else
            if (args[i].equalsIgnoreCase("/fingertouch")) // guich@573_20
            {
               Settings.fingerTouch = true;
               System.out.println("Finger touch is on");
            }
            else
            if (args[i].equalsIgnoreCase("/unmovablesip")) // guich@573_20
            {
               Settings.unmovableSIP = true;
               System.out.println("Unmovable SIP is on");
            }
            else
            if (args[i].equalsIgnoreCase("/geofocus")) // guich@tc114_31
            {
               Settings.geographicalFocus = Settings.keyboardFocusTraversable = true;
               System.out.println("Geographical focus is on");
            }
            else
            if (args[i].equalsIgnoreCase("/keypadOnly")) // guich@573_20
            {
               Settings.keypadOnly = true;
               System.out.println("Keypad only is on");
            }
            else
            if (args[i].equalsIgnoreCase("/virtualKeyboard")) // bruno@tc110
            {
               Settings.virtualKeyboard = true;
               System.out.println("Virtual keyboard is on");
            }
            else
            if (args[i].equalsIgnoreCase("/bpp"))
            {
               toBpp = toInt(args[++i]);
               if (toBpp != 4 && toBpp != 8 && toBpp != 16 && toBpp != 24 && toBpp != 32) // guich@450_4
                  throw new Exception();
               System.out.println("Bpp is "+toBpp);
            }
            else
            if (args[i].equalsIgnoreCase("/scale"))
            {
               toScale = toDouble(args[++i]); // guich@tc126_74: use a 
               if (toScale < 0 || toScale > 4)
                  throw new Exception();
               System.out.println("Scale is "+toScale);
            }
            else
            if (args[i].equalsIgnoreCase("/showmousepos"))
               Settings.showMousePosition = true;
            else 
            if (args[i].equalsIgnoreCase("/demo"))
               isDemo = true;
            else
               throw new Exception();
         }
      }
      catch (Exception e)
      {
         showInstructions();
         System.out.println("Invalid or incomplete argument at position "+i+": "+args[i]);
         String s = "";
         for (i = 0; i < args.length; i++)
            s += " "+args[i];
         System.out.println("Full command line:\n"+s.trim());
         exit(-1);
         return;
      }

      // verify the parameters
      if (toWidth == -1 || toHeight == -1) // if no width specified, use the lowest one
      {
         if (isApplication)
            toWidth = toHeight = 320; // guich@tc100b5_35: now default is palm hi
         else
         {
            toWidth = getSize().width;
            toHeight = getSize().height;
         }
      }
      if (toScale == -1) // if no scale specified, adjust depending on the resolution
         toScale = toWidth < 240 ? 2 : 1;
      if (toBpp == -1)
         toBpp = isApplication ? 16 : 32;

      Settings.dataPath = newDataPath;
   }

   private String[] tokenizeString(String string, char c)
   {
      java.util.StringTokenizer st = new java.util.StringTokenizer(string, ""+c);
      String []ret = new String[st.countTokens()];
      for (int i =0; i < ret.length; i++)
         ret[i] = st.nextToken();
      return ret;
   }

   public void start()
   {
      mainWindow = MainWindow.getMainWindow();
   }

   ///////// guich@200b2: to make the vm easier to port, i removed all methods from the TotalCross classes that uses the jdk classes /////////
   public void registerMainWindow(totalcross.ui.MainWindow main)
   {
      (winTimer = new WinTimer()).start(); // guich@510_2: start the timer only after we had added the others
   }

   public void setTimerInterval(int milliseconds)
   {
	   winTimer.setInterval(milliseconds);
   }

   public void exit(int exitCode)
   {
      destroy(); // guich@230_24
      if (isApplication) System.exit(exitCode);
   }

   public void minimize()
   {
      if (frame != null)
         frame.setExtendedState(Frame.ICONIFIED);
   }

   public void restore()
   {
      if (frame != null)
         frame.setExtendedState(Frame.NORMAL);
   }

   public void print(java.awt.Graphics g)
   {
   }

   public boolean isFocusTraversable() // guich@512_1: inform that we want to handle tab
   {
      return true;
   }

   private int modifiers;

   private boolean isIntercepting(int key)
   {
      int[] k = Vm.keysBeingIntercepted;
      for (int i = (k == null ? 0 : k.length)-1; i >= 0; i--)
         if (k[i] == key)
            return true;
      return false;
   }

   private void updateModifiers(java.awt.event.KeyEvent event)
   {
      if (event.isShiftDown())    
      {
         keysPressed.put(SpecialKeys.SHIFT,1); 
         modifiers |= SpecialKeys.SHIFT;   
      }
      else 
      {
         keysPressed.put(SpecialKeys.SHIFT,0); 
         modifiers &= ~SpecialKeys.SHIFT;
      }
      if (event.isControlDown())  
      {
         keysPressed.put(SpecialKeys.CONTROL,1); 
         modifiers |= SpecialKeys.CONTROL;   
      }
      else 
      {
         keysPressed.put(SpecialKeys.CONTROL,0); 
         modifiers &= ~SpecialKeys.CONTROL;
      }
      if (event.isAltDown())
      {
         keysPressed.put(SpecialKeys.ALT,1); 
         modifiers |= SpecialKeys.ALT;   
      }
      else 
      {
         keysPressed.put(SpecialKeys.ALT,0); 
         modifiers &= ~SpecialKeys.ALT;
      }
   }

   public void keyPressed(final java.awt.event.KeyEvent event)
   {
      if (event.getKeyChar() == '1' && event.isControlDown())
         totalcross.ui.Window.onRobotKey();
      updateModifiers(event);
      if (event.isActionKey())
      {
         updateModifiers(event);
         int key = 0;

         switch (event.getKeyCode())
         {
            case java.awt.event.KeyEvent.VK_HOME:       key = SpecialKeys.HOME; break;
            case java.awt.event.KeyEvent.VK_END:        key = SpecialKeys.END; break;
            case java.awt.event.KeyEvent.VK_UP:         key = SpecialKeys.UP; break;
            case java.awt.event.KeyEvent.VK_DOWN:       key = SpecialKeys.DOWN; break;
            case java.awt.event.KeyEvent.VK_LEFT:       key = SpecialKeys.LEFT; break;
            case java.awt.event.KeyEvent.VK_RIGHT:      key = SpecialKeys.RIGHT; break;
            case java.awt.event.KeyEvent.VK_INSERT:     key = SpecialKeys.INSERT; break;
            case java.awt.event.KeyEvent.VK_ENTER:      key = SpecialKeys.ENTER; break;
            case java.awt.event.KeyEvent.VK_TAB:        key = SpecialKeys.TAB; break;
            case java.awt.event.KeyEvent.VK_BACK_SPACE: key = SpecialKeys.BACKSPACE; break;
            case java.awt.event.KeyEvent.VK_ESCAPE:     key = SpecialKeys.ESCAPE; break;
            case java.awt.event.KeyEvent.VK_DELETE:     key = SpecialKeys.DELETE; break;
            case java.awt.event.KeyEvent.VK_PAGE_UP:    key = SpecialKeys.PAGE_UP;    keysPressed.put(key,1); keysPressed.put(java.awt.event.KeyEvent.VK_PAGE_DOWN,0); break; // don't let down/up simultanealy
            case java.awt.event.KeyEvent.VK_PAGE_DOWN:  key = SpecialKeys.PAGE_DOWN;  keysPressed.put(key,1); keysPressed.put(java.awt.event.KeyEvent.VK_PAGE_UP,0);   break;
            // guich@120 - emulate more keys
            case java.awt.event.KeyEvent.VK_F1:         if (isIntercepting(SpecialKeys.HARD1)) {key = SpecialKeys.HARD1; keysPressed.put(key,1);} break;
            case java.awt.event.KeyEvent.VK_F2:         if (isIntercepting(SpecialKeys.HARD2)) {key = SpecialKeys.HARD2; keysPressed.put(key,1);} break;
            case java.awt.event.KeyEvent.VK_F3:         if (isIntercepting(SpecialKeys.HARD3)) {key = SpecialKeys.HARD3; keysPressed.put(key,1);} break;
            case java.awt.event.KeyEvent.VK_F4:         if (isIntercepting(SpecialKeys.HARD4)) {key = SpecialKeys.HARD4; keysPressed.put(key,1);} break;
            case java.awt.event.KeyEvent.VK_F5:         key = SpecialKeys.COMMAND; break;
            case java.awt.event.KeyEvent.VK_F6:         key = SpecialKeys.MENU; break;
            case java.awt.event.KeyEvent.VK_F7:         if (isIntercepting(SpecialKeys.CALC)) key = SpecialKeys.CALC; break;
            case java.awt.event.KeyEvent.VK_F8:         if (isIntercepting(SpecialKeys.FIND)) key = SpecialKeys.FIND; break;
            case java.awt.event.KeyEvent.VK_F10:        if (isIntercepting(SpecialKeys.LAUNCH)) key = SpecialKeys.LAUNCH; break;
            case java.awt.event.KeyEvent.VK_F11:        key = SpecialKeys.KEYBOARD_123; break;
            case java.awt.event.KeyEvent.VK_F12:        key = SpecialKeys.ACTION; break; // guich@400_64
            case java.awt.event.KeyEvent.VK_F9:
               if (isApplication && !Settings.disableScreenRotation && Settings.screenWidth != Settings.screenHeight && eventThread != null) // guich@tc: changed orientation?
               {
                  int t = toWidth;
                  toWidth = toHeight;
                  toHeight = t;
                  screenResized(Settings.screenHeight,Settings.screenWidth,true);
                  key = 0;
               }
               break;
            default: key = 0; break;
         }
         if (key != 0 && eventThread != null) // sometimes, when debugging in applet, eventThread can be null
         {
            eventThread.pushEvent(KeyEvent.SPECIAL_KEY_PRESS, key, 0, 0, modifiers, Vm.getTimeStamp());
         }
         if (showKeyCodes && eventThread != null)
         {
            final String msg = "Key code: " + (key == 0 ? event.getKeyCode() : key) + ", Modifier: " + modifiers;
            new Thread() {public void run() {Vm.alert(msg);}}.start(); // must place this in a separate thread, or the vm dies
         }
      }
   }
   
   private void screenResized(int w, int h, boolean setframe)
   {
      if (screenMis == null || (Settings.screenWidth == w && Settings.screenHeight == h)) return;
      Settings.screenWidth = w;
      Settings.screenHeight = h;
      frame.setFrameSize(w,h,setframe);
      screenMis = null; // force the creation of a new screen image
      eventThread.pushEvent(KeyEvent.SPECIAL_KEY_PRESS, SpecialKeys.SCREEN_CHANGE, 0, 0, modifiers, Vm.getTimeStamp());
   }

   public void transferFocus() // guich@512_1: handle the tab key.
   {
      super.transferFocus();
      if (eventThread != null) // sometimes, when debugging in applet, eventThread can be null
         eventThread.pushEvent(KeyEvent.SPECIAL_KEY_PRESS, SpecialKeys.TAB, 0, 0, modifiers, Vm.getTimeStamp());
   }

   public void keyReleased(java.awt.event.KeyEvent event)
   {
      updateModifiers(event);
      if (event.isActionKey())
         switch (event.getKeyCode())
         {
            case java.awt.event.KeyEvent.VK_F1:        keysPressed.put(SpecialKeys.HARD1,0); break;
            case java.awt.event.KeyEvent.VK_F2:        keysPressed.put(SpecialKeys.HARD2,0); break;
            case java.awt.event.KeyEvent.VK_F3:        keysPressed.put(SpecialKeys.HARD3,0); break;
            case java.awt.event.KeyEvent.VK_F4:        keysPressed.put(SpecialKeys.HARD4,0); break;
            case java.awt.event.KeyEvent.VK_PAGE_UP:   keysPressed.put(SpecialKeys.PAGE_UP,0); break;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN: keysPressed.put(SpecialKeys.PAGE_DOWN,0); break;
         }
   }

   public void keyTyped(java.awt.event.KeyEvent event)
   {
      updateModifiers(event);
      if (!event.isActionKey() && eventThread != null)
      {
         int key = event.getKeyChar(), orig = key;
         switch (key)
         {
            case 8  : key = SpecialKeys.BACKSPACE; break;
            case 10 : key = SpecialKeys.ENTER; break;
            case 127: key = SpecialKeys.DELETE; break;
            case 27 : key = SpecialKeys.ESCAPE; break; // guich@tc110_79
         }
         eventThread.pushEvent(orig < 32 ? KeyEvent.SPECIAL_KEY_PRESS : KeyEvent.KEY_PRESS, key, 0, 0, modifiers, Vm.getTimeStamp());
      }
   }

   public void mousePressed(java.awt.event.MouseEvent event)
   {
      if (eventThread != null) // sometimes, when debugging in applet, eventThread can be null
         eventThread.pushEvent(PenEvent.PEN_DOWN, 0, (int)(event.getX()/toScale), (int)(event.getY()/toScale), modifiers, Vm.getTimeStamp());
   }

   public void mouseReleased(java.awt.event.MouseEvent event)
   {
      if (eventThread != null) // sometimes, when debugging in applet, eventThread can be null
         eventThread.pushEvent(PenEvent.PEN_UP, 0, (int)(event.getX()/toScale), (int)(event.getY()/toScale), modifiers, Vm.getTimeStamp());
   }

   public void mouseDragged(java.awt.event.MouseEvent event)
   {
      if (eventThread != null) // sometimes, when debugging in applet, eventThread can be null
         eventThread.pushEvent(PenEvent.PEN_DRAG, 0, (int)(event.getX()/toScale), (int)(event.getY()/toScale), modifiers, Vm.getTimeStamp()); // guich@580_40: changed from 201 to 203; PenEvent.PEN_MOVE is deprecated
   }

   public void windowClosing(java.awt.event.WindowEvent event)
   {
      destroy();
      exit(0);
   }

   public void mouseEntered(java.awt.event.MouseEvent event)
   {
      if (frame != null && frame.getFocusOwner() != this && !destroyed) // guich@320_39
         requestFocus();  // guich@200b4: correct a bug that sometimes key events was not being sent anymore to the canvas.
   }
      
   public void mouseClicked(java.awt.event.MouseEvent event) {}
   public void mouseExited(java.awt.event.MouseEvent event)  {}
   public void windowActivated(java.awt.event.WindowEvent event) {}
   public void windowClosed(java.awt.event.WindowEvent event)     {}
   public void windowDeactivated(java.awt.event.WindowEvent event) {}
   public void windowDeiconified(java.awt.event.WindowEvent event)
   {
      if (mainWindow != null)
         mainWindow.onRestore();
   }
   public void windowIconified(java.awt.event.WindowEvent event)
   {
      if (mainWindow != null)
         mainWindow.onMinimize();
   }
   public void windowOpened(java.awt.event.WindowEvent event)      {}
   public void mouseMoved(java.awt.event.MouseEvent event) 
   {
      if (eventThread != null) // sometimes, when debugging in applet, eventThread can be null
         eventThread.pushEvent(totalcross.ui.event.MouseEvent.MOUSE_MOVE, 0, (int)(event.getX()/toScale), (int)(event.getY()/toScale), modifiers, Vm.getTimeStamp());
      if (frame != null && Settings.showMousePosition) // guich@tc115_48
      {
         mmsb.setLength(0);
         if (frameTitle != null)
            mmsb.append(frameTitle).append(" (");
         mmsb.append(event.getX()).append(",").append(event.getY());
         if (frameTitle != null)
            mmsb.append(")");
         frame.setTitle(mmsb.toString());
      }
   }

   public void paint(java.awt.Graphics g)
   {
      if (!started) // guich@120 - only call initUI after the component is valid
         startApp();
      else
         eventThread.invokeInEventThread(false, new Runnable()
         {
            public void run()
            {
               try
               {
                  totalcross.ui.Window.repaintActiveWindows();
               }
               catch (Exception e) {System.out.println("Exception in Launcher.paint"); e.printStackTrace();}
           }
         });
   }

   public void pumpEvents()
   {
      eventThread.pumpEvents();
   }
   
   public void update(java.awt.Graphics g) {}

   public void setNewMainWindow(MainWindow newInstance, String args) // called on Vm.exec
   {
      commandLine = args; // guich@200b3: added command line support for desktop classes.
      winTimer.stopGracefully(); // guich@120
      MainWindow.destroyZStack();
      mainWindow = newInstance;
      mainWindow.initUI(); // ps: since we are being called from an app, we cannot use the synchronized method
   }

   /** Calls System.out.println. TotalCross system debugging uses this method. See also debug(String s). */
   public static void print(String s)
   {
      if (totalcross.sys.Settings.showDesktopMessages) System.err.println(s);
   }
   //// Graphics ////////////////////////////////////////////////////////////////////

   private void createColorPaletteLookupTables()
   {
      int i,r,g,b;
      lookupR = new int[256];
      lookupG = new int[256];
      lookupB = new int[256];
      lookupGray = new int[256];

      for (i = 0; i < 256; i++)
      {
         r = (i+1) * 6 / 256; if (r > 0) r--;
         g = (i+1) * 8 / 256; if (g > 0) g--;
         b = (i+1) * 5 / 256; if (b > 0) b--;
         lookupR[i] = r*40;
         lookupG[i] = g*5;
         lookupB[i] = b+16;
         lookupGray[i] = i / 0x11;
      }
      pal685 = totalcross.ui.gfx.Graphics.getPalette();
   }

   private int getScreenColor(int p)
   {
      int r = (p >> 16) & 0xFF;
      int g = (p >> 8) & 0xFF;
      int b = p & 0xFF;
      switch (toBpp)
      {
         case 4:
            return ((((r << 5) + (g << 6) + (b << 2)) / 100) >> 4) * 0x111111;
         case 8:
            if (lookupR == null)
               createColorPaletteLookupTables();
            return pal685[(g == r && g == b) ? lookupGray[r] : (lookupR[r] + lookupG[g] + lookupB[b])];
         case 16:
            return (((r) >> 3) << 19) | (((g) >> 2) << 10) | (((b >> 3) << 3));
         default: return p;
      }
   }

   public void updateScreen(int transitionEffect)
   {
      if (transitionEffect == -1)
         transitionEffect = totalcross.ui.Container.TRANSITION_NONE;

      //int ini = totalcross.sys.Vm.getTimeStamp();
      int[] pixels = (int[])totalcross.ui.gfx.Graphics.mainWindowPixels;
      int n = Settings.screenWidth * Settings.screenHeight;
      if (toBpp >= 24)
         screenPixels = pixels;
      else
      if (screenPixels.length < n)
         screenPixels = new int[n];
      // convert to the target bpp on-the-fly
      switch (toBpp)
      {
         case 4:
         {
            while (--n >= 0)
            {
               int p = pixels[n];
               int r = (p >> 16) & 0xFF;
               int g = (p >> 8) & 0xFF;
               int b = p & 0xFF;
               screenPixels[n] = ((((r << 5) + (g << 6) + (b << 2)) / 100) >> 4) * 0x111111;
            }
            break;
         }
         case 8:
         {
            if (lookupR == null)
               createColorPaletteLookupTables();
            int[] pal = pal685;
            int[] toR = lookupR;
            int[] toG = lookupG;
            int[] toB = lookupB;
            int[] toGray = lookupGray;
            while (--n >= 0)
            {
               int p = pixels[n];
               int r = (p >> 16) & 0xFF;
               int g = (p >> 8) & 0xFF;
               int b = p & 0xFF;
               screenPixels[n] = pal[(g == r && g == b) ? toGray[r] : (toR[r] + toG[g] + toB[b])];
            }
            break;
         }
         case 16:
         {
            while (--n >= 0)
               screenPixels[n] = pixels[n] & 0xF8FCF8; // guich@tc100b4_2: use a direct and instead of a bunch of shifts. note: using a DirectColorModel(32,0xF80000,0x00FC00,0x0000F8,0) is 5x SLOWER than doing the mapping by ourselves.
            break;
         }
      }
      int w = totalcross.sys.Settings.screenWidth;
      int h = totalcross.sys.Settings.screenHeight;
      if (screenMis == null)
      {
         screenMis = new MemoryImageSource(w, h, new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0), screenPixels, 0, w);
         screenMis.setAnimated(true);
         screenMis.setFullBufferUpdates(true);
         screenImg = Toolkit.getDefaultToolkit().createImage(screenMis);
      }
      screenMis.newPixels();
      Graphics g = getGraphics();
      int ww = (int)(w*toScale);
      int hh = (int)(h*toScale);
      int shiftY = totalcross.ui.Window.shiftY;
      int shiftH = totalcross.ui.Window.shiftH;
      if ((shiftY+shiftH) > h)
         totalcross.ui.Window.shiftY = shiftY = h - shiftH;
      if (shiftY != 0)
      {
         g.setColor(new Color(UIColors.shiftScreenColor));
         int yy = (int)(shiftH*toScale);
         g.fillRect(0, yy, ww, hh-yy); // erase empty area
         g.setClip(0,0,ww,yy);         // limit drawing area
         g.translate(0,-(int)(shiftY*toScale));
      }
      switch (transitionEffect)
      {
         case totalcross.ui.Container.TRANSITION_CLOSE:
         case totalcross.ui.Container.TRANSITION_OPEN:
         {
            n = Math.min(w,h);
            int mx = w/2,mw=1,mh=1;
            int my = h/2;
            float incX=1,incY=1;
            if (w > h)
               {incX = (float)w/h; mw = (int)incX+1;}
             else
               {incY = (float)h/w; mh = (int)incY+1;}
            int i0 = transitionEffect == totalcross.ui.Container.TRANSITION_CLOSE ? n : 0;
            int iinc = transitionEffect == totalcross.ui.Container.TRANSITION_CLOSE ? -1 : 1;
            for (int i =i0; --n >= 0; i+=iinc)
            {
               int minx = (int)(mx - i*incX);
               int miny = (int)(my - i*incY);
               int maxx = (int)(mx + i*incX);
               int maxy = (int)(my + i*incY);
               drawImageLine(g,minx-mw,miny-mh,maxx+mw,miny+mh);
               drawImageLine(g,minx-mw,miny-mh,minx+mw,maxy+mh);
               drawImageLine(g,maxx-mw,miny-mh,maxx+mw,maxy+mh);
               drawImageLine(g,minx-mw,maxy-mh,maxx+mw,maxy+mh);
               Vm.sleep(1);
            }
            if (toScale == 1)
               break;
         }
         case totalcross.ui.Container.TRANSITION_NONE:
            if (toScale != 1) // guich@tc126_74 - guich@tc130 
            {
               Image img = screenImg.getScaledInstance(ww, hh, toScale != (int)toScale ? Image.SCALE_AREA_AVERAGING : Image.SCALE_FAST);
               g.drawImage(img, 0, 0, this); // this is faster than use img.getScaledInstance
               img.flush();
            }
            else
            if (g != null)
               g.drawImage(screenImg, 0, 0, ww, hh, 0,0,w,h, this); // this is faster than use img.getScaledInstance
            break;
      }
      if (shiftY != 0)
      {
         g.translate(0,(int)(shiftY*toScale));
         g.setClip(0,0,ww,hh);
      }
      //System.out.println(++count+" total in "+(totalcross.sys.Vm.getTimeStamp()-ini)+"ms");
      //try {throw new Exception();} catch (Exception e) {e.printStackTrace();}
   }
   private void drawImageLine(Graphics g, int minx, int miny, int maxx, int maxy)
   {
      g.drawImage(screenImg, (int)(minx*toScale),(int)(miny*toScale),(int)(maxx*toScale),(int)(maxy*toScale), minx,miny,maxx,maxy, this); // this is faster than use img.getScaledInstance
   }

   //static int count;
   ///////////////////////        I/O        /////////////////////////////////////
   private File[] getClassPathDirectories() throws Exception
   {
      char dirSeparator = File.pathSeparatorChar;
      File[] classPath;
      String pathstr = System.getProperty("java.class.path");
      // Count the number of path separators
      int i=0;
      int n=0;
      int j=0;
      while ((i = pathstr.indexOf(dirSeparator, i)) != -1)
      {
         n++;
         i++;
      }
      // Build the class path
      File[] path = new File[n+1];
      int len = pathstr.length();
      for (i = n = 0; i < len; i = j + 1)
      {
         if ((j = pathstr.indexOf(dirSeparator, i)) == -1)
            j = len;
         if (i != j)
         {
            String p = pathstr.substring(i, j);
            File file = new File(p);
            if (!file.isDirectory())
               file = new File(getPathOf(p)); // add the parent path of the file
            if (file.isDirectory())
               path[n++]=file;
         }
      }
      // Trim class path to exact size
      classPath = new File[n];
      System.arraycopy(path, 0, classPath, 0, n);
      return classPath;
   }

   private InputStream readJavaInputStream(java.io.InputStream is)
   {
      if (is == null) return null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      byte []buf = new byte[128];
      int len;
      while (true)
      {
         try
         {
            len = is.read(buf);
         }
         catch (java.io.IOException e) {break;}
         if (len > 0)
            baos.write(buf,0,len);
         else
            break;
      }
      return new ByteArrayInputStream(baos.toByteArray());
   }

   private String getPathOf(String pathAndFileName)
   {
      char []chars = pathAndFileName.toCharArray();
      for (int i = chars.length-1; i >=0; i--)
         if (chars[i] == '\\' || chars[i] == '/')
            return new String(chars,0,i);
      return ""; // no path
   }

   public String getDataPath() // guich@420_11 - this now is needed because the user may change the datapath anywhere in the program
   {
      String path = totalcross.sys.Settings.dataPath;
      if (path != null)
      {
         path = path.replace('\\','/');
         if (!path.endsWith("/")) path += "/";
         // don't check for folder to keep compatibility with win32 vm
         //java.io.File f = new java.io.File(newDataPath);
         //if (!f.isDirectory())
         //   System.out.println("ERROR: dataPath specified is not a directory or does not exist! "+newDataPath);
      }
      return path;
   }

   private String getMainWindowPath()
   {
      if (MainWindow.getMainWindow() == null)
         return null;
      String main = MainWindow.getMainWindow().getClass().getName().replace('.','/');
      return getPathOf(main)+"/";
   }
   /** used in some classes so they can correctly open files. now can open jar files. */
   public InputStream openInputStream(String path)
   {
      String sread = "\nopening for read "+path+"\n";
      String dataPath = getDataPath();
      InputStream stream = null;
      String mainpath = getMainWindowPath();
      try
      {
         try // guich@tc100: removed the nonGuiApp flag
         {
            sread += "#0 - the file given: "+path+"\n";
            stream = new FileInputStream(path); // guich@421_72
         } catch (Exception e) {stream = null;}
         if (stream == null && isApplication)
         {
            // search in the Settings.dataPath
            try
            {
               String p = isOk(dataPath)?(dataPath+path):path;
               sread += "#1 - dataPath: "+p+"\n";
               stream = new FileInputStream(p);
               htOpenedAt.put(path, getPathOf(p)); // guich@200b4_82 - jr: i changed getPathOf(path) to getPathOf(p)
            } catch (Exception e) {stream = null;}
            if (stream == null && mainpath != null)
               try
               {
                  String p = mainpath + path;
                  sread += "#2 - MainWindow's path from current folder: "+p+"\n";
                  stream = new FileInputStream(p);
                  htOpenedAt.put(path, getPathOf(p)); // guich@200b4_82 - jr: i changed getPathOf(path) to getPathOf(p)
               } catch (Exception e) {stream = null;}
            // search in the classpath
            if (stream == null)
            {
               sread += "#3 - classpath\n";
               File []dirs = getClassPathDirectories();
               File f = null;
               for (int i = 0; i < dirs.length; i++)
                  try
                  {
                     f = new File(dirs[i],path);
                     if (!f.isFile() && mainpath != null)
                        f = new File(dirs[i],mainpath+path); // guich@tc100: search in the path of the main window
                     if (f.isFile())
                     {
                        String ff = getPathOf(f.getAbsolutePath());
                        htOpenedAt.put(path,ff); // guich@200b4_82 - jr: changed dirs[i].getAbsolutePath - guich@tc112_20: using f.getAbsolutePath instead of dirs[i].getAbsolutePath
                        break;
                     }
                     else f = null; // guich@400_8: fixed problem when file was not found so the #3 can be tried below
                  } catch (Exception e) {f = null;}
               if (f != null)
                  stream = new FileInputStream(f);
            }
            if (stream == null && _class != null) // guich@400_6: now the resources can be read from the jar file
            {
               sread += "#4 - jar file\n";
               try
               {
                  InputStream is = (InputStream)_class.getResourceAsStream("/"+path);
                  if (is != null)
                  {
                     stream = readJavaInputStream(is);
                  }
               } catch (Throwable tt) {if (tt.getMessage() != null) System.out.println(tt.getMessage());}
            }
            if (stream == null && htAttachedFiles.size() > 0) // guich@tc100: load from attached libraries too
            {
               sread += "#5 - attached libraries\n";
               totalcross.io.ByteArrayStream bas = (totalcross.io.ByteArrayStream)htAttachedFiles.get(path.toLowerCase());
               if (bas != null)
                  stream = new ByteArrayInputStream(bas.getBuffer()); // buffer is the same size of the loaded file.
            }
         }
         else
         if (stream == null)
         {
            URL url;
            // zero in the jar file (normal way)
            InputStream is=null;
            try
            {
               is = (InputStream)_class.getResourceAsStream("/"+path);
            } catch (Throwable tt) {if (tt.getMessage() != null) System.out.println(tt.getMessage());}
            sread += "#1 - resource: "+is+"\n"; // guich@200b4_59
            if (is != null)
               stream = readJavaInputStream(is);
            // first in the jar file
            // guich@200b4: using this in Internet makes the archive be fetched from the server at each call of this function.
            if (stream == null)
            {
               String archive = getParameter("archive");
               sread += "#2 - archive: "+archive+"\n";
               if (isOk(archive) && !archive.equals("null"))
               {
                  String[] archives = tokenizeString(archive,','); // guich@580_39: if there are more than one file, split them
                  for (int i=0; i < archives.length; i++)
                  {
                     archive = archives[i];
                     if(archive.startsWith("null"))
                        archive = archive.substring(4);
                     URL codeBase = getCodeBase();
                     url = new URL(codeBase+"/"+archive);
                     try
                     {
                        ZipInputStream zIn = new ZipInputStream(url.openStream());
                        java.util.zip.ZipEntry zEntry = zIn.getNextEntry();
                        while(!zEntry.getName().equals(path))
                        {
                           zEntry = zIn.getNextEntry();
                           if (zEntry == null)
                              throw new Exception("doh");
                        }
                        // guich@200b2: ok. the zIn.available() returns 1 and not the real size of the zip entry. so, here we read all into a byte stream
                        stream = readJavaInputStream(zIn);
                     } catch (Exception e){if (!e.getMessage().equals("doh")) e.printStackTrace();/* doh didn't find it in the jar thing */}
                  }
               }
            }
            // second under the codebase
            if (stream == null)
            try
            {
               URL codeBase = getCodeBase();
               String cb = codeBase.toString();
               char lastc = cb.charAt(cb.length() - 1);
               char firstc = path.charAt(0);
               if (lastc != '/' && firstc != '/')
                  cb += "/";
               sread += "#3 - url: "+cb+path+"\n";
               url = new URL(cb + path);
               stream = url.openStream();
            }
            catch (FileNotFoundException ee) {}
            catch (Exception e) {e.printStackTrace();/* neither in the codebase */}
            // third in the localhost
            if (stream == null)
            try
            {
               sread += "#4- url: file://localhost/"+dataPath+path+"\n";
               url = new URL("file://localhost/"+dataPath + path); // guich@120
               stream = url.openStream();
            } catch (Exception e) {};
            if (stream == null && htAttachedFiles.size() > 0) // guich@tc100: load from attached libraries too
            {
               sread += "#5 - attached libraries\n";
               totalcross.io.ByteArrayStream bas = (totalcross.io.ByteArrayStream)htAttachedFiles.get(path.toLowerCase());
               if (bas != null)
                  stream = new ByteArrayInputStream(bas.getBuffer()); // buffer is the same size of the loaded file.
            }
         }
         if (stream == null) print(sread+"file not found\n");
      }
      catch (FileNotFoundException ee)
      {
         print("file not found");
      }
      catch (Exception e) // guich@120
      {
         if (isOk(e.getMessage())) // guich@500_something: only show message if something is to be displayed
            print("error in JavaBridge.openInputStream: "+e.getMessage());
         return null;
      }
      return stream;
   }
   private OutputStream openOutputUrl(URL url)
   {
      try
      {
         URLConnection con = url.openConnection();
         con.setUseCaches(false);
         con.setDoOutput(true);
         con.setDoInput(false);
         return con.getOutputStream();
      }
      catch (Exception u) // try another way
      {
         try
         {
            String path = url+"";
            return new FileOutputStream(isOk(totalcross.sys.Settings.dataPath)?(getDataPath()+path):path);
         } catch (Exception ee) {return null;}
      }
   }
   /** used in some classes so they can correctly open files. used internally by readBytes. */
   public OutputStream openOutputStream(String path)
   {
      print("\nopening for write "+path);
      String dataPath = getDataPath();
      OutputStream stream = null;
      String readPath = (String)htOpenedAt.get(path); // guich@tc112_20
      try
      {
         try // guich@tc100: removed the nonGuiApp flag
         {
            String pp = isOk(dataPath) ? (dataPath+path) : isOk(readPath) ? totalcross.sys.Convert.appendPath(readPath,path) : path; // guich@tc112_20: use readPath if not null
            stream = new FileOutputStream(pp); // guich@421_11: added support for dataPath
         } catch (Exception e) {stream = null;}

         if (stream == null && isApplication)
         {
            // search in the place where it was read - guich@200b4_82
            if (readPath != null) // guich@400_58 - guich@421_10: changed to != instead of ==
            try
            {
               print("#1 - read path");
               stream = new FileOutputStream(new java.io.File(readPath,path));
               print("found in "+readPath);
            }
            catch (Exception e) {stream = null;}
            if (stream != null)
               return stream;
            // search in the Settings.dataPath
            try
            {
               String p = isOk(dataPath)?(dataPath+path):path;
               print("#2 - Settings.dataPath");
               stream = new FileOutputStream(p);
               print("found in "+p);
            }
            catch (Exception e) {stream = null;}
            // search in the classpath
            if (stream == null)
            {
               print("#3 - classpath");
               File []dirs = getClassPathDirectories();
               File f = null;
               for (int i = 0; i < dirs.length; i++)
                  try
                  {
                     f = new File(dirs[i],path);
                     if (f.isFile())
                     {
                        print("found in "+dirs[i]);
                        break;
                     }
                  } catch (Exception e) {f = null;}
               if (f == null)
                  print("could not find file in the classpath");
               else
                  stream = new FileOutputStream(f);
            }
         }
         else
         if (stream == null)
         {
            URL url;
            // first under the codebase
            if (stream == null)
            try
            {
               URL codeBase = getCodeBase();
               print("#1- codeBase: "+codeBase);
               String cb = codeBase.toString();
               char lastc = cb.charAt(cb.length() - 1);
               char firstc = path.charAt(0);
               if (lastc != '/' && firstc != '/')
                  cb += "/";
               url = new URL(cb + path);
               stream = openOutputUrl(url);
               print("found under codebase: "+url);
            }
            catch (Exception e) {e.printStackTrace();/* neither in the codebase */}
            // third in the localhost
            if (stream == null)
            try
            {
               print("#2- url: file://localhost/" + dataPath + path);
               url = new URL("file://localhost/" + dataPath + path); // guich@120
               stream = openOutputUrl(url);
               print("found under localhost: "+url);
            } catch (Exception e) {};
         }
         if (stream == null) print("file not found");
      }
      catch (FileNotFoundException ee) {print("file not found");}
      catch (Exception e) {/*if (!msgShowed) */print("error in Vm.openOutputStream: "+e.getMessage()); return null;} // guich@200
      return stream;
   }
   /** read the available bytes from the stream getted with openInputStream.
     * called by totalcross.ui.image.Image and totalcross.io.PDBFile
     */
   public byte [] readBytes(String path)
   {
      byte [] bytes = null;
      try
      {
         InputStream is = openInputStream(path);
         if (is != null)
         {
            int n = is.available();
            bytes = new byte[n];
            is.read(bytes);
            is.close();
         }
      } catch (Exception e) {e.printStackTrace();}
      return bytes;
   }
   /** write the available bytes to the stream getted with openOutputStream.
     * called by totalcross.io.PDBFile
     */
   public boolean writeBytes(String path, byte []buf, int len)
   {
      boolean ret = true;
      try
      {
         OutputStream os = openOutputStream(path);
         if (os != null)
         {
            if (buf != null)
            {
               os.write(buf,0,len);
               os.close(); // pietj@330_1
            }
            else
               print("ATT: you sent to stream.writeBytes a null buffer!");
         }
      } catch (Exception e) {e.printStackTrace(); ret = false;}
      return ret;
   }

   /** return true is the string is valid. called by openInputStream and openOutputStream in this class. */
   private boolean isOk(String s)
   {
      return s != null && s.length() > 0;
   }

   String getDefaultCrid(String name)
   {
      if (name == null)
         return null;

      if (name.indexOf('.') != -1)
         name = name.substring(name.lastIndexOf('.')+1);
      int i;
      int n = name.length();
      int hash = 0;
      byte[] creat=new byte[4];
      for (i = 0; i < n; i++)
         hash += (byte)name.charAt(i);
      for (i = 0; i < 4; i++)
      {
         creat[i] = (byte)((hash % 26) + 'a');
         if ((hash & 64)>0)
            creat[i] += ('A'-'a');
         hash = hash / 2;
      }
      return new String(creat);
   }

   void storeSettings()
   {
      try
      {
         String crid = crid4settings;//totalcross.sys.Settings.applicationId;
         // first verify if the PDBFile is created but the String is null
         totalcross.sys.Settings.showDesktopMessages = false; // guich@340_49
         boolean saveSettings = totalcross.sys.Settings.appSettings != null || totalcross.sys.Settings.appSecretKey != null || totalcross.sys.Settings.appSettingsBin != null; // guich@570_9: also check if appSecretKey is null

         totalcross.io.PDBFile cat;

         if (!saveSettings)
         {
            try
            {
               cat = new totalcross.io.PDBFile("Settings4"+crid+".TCVM."+crid,totalcross.io.PDBFile.READ_WRITE); // guich@241_17: changed READ_ONLY to READ_WRITE to fix "operation invalid" error
               cat.delete();
            }
            catch(totalcross.io.FileNotFoundException e)
            {
            }
         }
         else
         {
            cat = new totalcross.io.PDBFile("Settings4"+crid+".TCVM."+crid,totalcross.io.PDBFile.CREATE);
            totalcross.io.ResizeRecord rs = new totalcross.io.ResizeRecord(cat,256);
            totalcross.io.DataStream ds = new totalcross.io.DataStream(rs);

            try
            {
               cat.setRecordPos(1);
               cat.deleteRecord();
            } catch (totalcross.io.IOException e) {}
            try
            {
               cat.setRecordPos(0);
               cat.deleteRecord();
            } catch (totalcross.io.IOException e) {}
            rs.startRecord();
            // store the appSettings record
            ds.writeString(totalcross.sys.Settings.appSettings);
            ds.writeString(totalcross.sys.Settings.appSecretKey);
            rs.endRecord();
            // guich@573_16: store the bin in another record
            if (totalcross.sys.Settings.appSettingsBin != null)
            {
               int len = totalcross.sys.Settings.appSettingsBin.length;
               cat.addRecord(len);
               cat.writeBytes(totalcross.sys.Settings.appSettingsBin,0,len);
            }
            cat.close();

         }
         totalcross.sys.Settings.showDesktopMessages = true;
      }
      catch (Throwable t) {System.out.println("Settings can't be stored: "+t.toString());}
   }

   private void getAppSettings()
   {
      String crid = crid4settings = totalcross.sys.Settings.applicationId;
      totalcross.sys.Settings.showDesktopMessages = false; // guich@340_49
      try
      {
         totalcross.io.PDBFile cat = new totalcross.io.PDBFile("Settings4"+crid+".TCVM."+crid,totalcross.io.PDBFile.READ_WRITE);
         totalcross.io.DataStream ds = new totalcross.io.DataStream(cat);
         cat.setRecordPos(0);
         String s;
         s = ds.readString();
         if (!"".equals(s))
            totalcross.sys.Settings.appSettings = s;
         try
         {
            s = ds.readString();
            if (!"".equals(s))
               totalcross.sys.Settings.appSecretKey = s;
         } catch (Throwable t) {System.out.println("Reading an old settings file; no appSecretKey available.");}

         if (cat.getRecordCount() > 1) // guich@573_16
         {
            cat.setRecordPos(1);
            byte[] buf = new byte[cat.getRecordSize()];
            cat.readBytes(buf,0,buf.length);
            totalcross.sys.Settings.appSettingsBin = buf;
         }

         cat.close();
      }
      catch (Throwable t) {}
      totalcross.sys.Settings.showDesktopMessages = true; // guich@340_49
   }
   private char getFirstSymbol(String s)
   {
      char []c = s.toCharArray();
      for (int i =0; i < c.length; i++)
         if (c[i] != ' ' && !('0' <= c[i] && c[i] <= '9'))
            return c[i];
      return ' ';
   }
   /** called by totalcross.Launcher.init() */
   public void fillSettings()
   {
      if (settingsFilled)
         return;
      settingsFilled = true;
      java.util.Calendar cal = java.util.Calendar.getInstance();
      // guich@340_34: since java can't provide us good methods to return these values, we use parse the return of some formatting methods
      cal.set(2002,11,25,20,0,0); // guich@401_32
      java.text.DateFormat df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT); // guich@401_32: fixed wrong results in some systems
      String d = df.format(cal.getTime());
      totalcross.sys.Settings.dateFormat = d.startsWith("25") ? totalcross.sys.Settings.DATE_DMY
                                   : d.startsWith("12") ? totalcross.sys.Settings.DATE_MDY
                                   : totalcross.sys.Settings.DATE_YMD;
      totalcross.sys.Settings.dateSeparator = getFirstSymbol(d);
      df = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT); // guich@401_32
      d = df.format(cal.getTime());

      totalcross.sys.Settings.is24Hour = d.toLowerCase().indexOf("am") == -1 && d.toLowerCase().indexOf("pm") == -1;
      totalcross.sys.Settings.timeSeparator = getFirstSymbol(d);
      //

      totalcross.sys.Settings.weekStart = (byte) (cal.getFirstDayOfWeek() - 1);
      settingsRefresh(false);

      java.text.DecimalFormatSymbols dfs = new java.text.DecimalFormatSymbols();
      totalcross.sys.Settings.thousandsSeparator = dfs.getGroupingSeparator();
      totalcross.sys.Settings.decimalSeparator = dfs.getDecimalSeparator();
      totalcross.sys.Settings.screenBPP = toBpp;
      try
      {
         totalcross.sys.Settings.screenWidthInDPI = totalcross.sys.Settings.screenHeightInDPI = Toolkit.getDefaultToolkit().getScreenResolution();
      }
      catch (Throwable t) 
      {
         totalcross.sys.Settings.screenWidthInDPI = 96;
      }
      totalcross.sys.Settings.romVersion = 0x02000000;
      totalcross.sys.Settings.uiStyle = totalcross.sys.Settings.WinCE;
      totalcross.sys.Settings.screenWidth = toWidth;
      totalcross.sys.Settings.screenHeight = toHeight;
      totalcross.sys.Settings.onJavaSE = true;
      totalcross.sys.Settings.platform = Settings.JAVA;
      totalcross.sys.Settings.applicationId = getDefaultCrid(className); // dhaysmith@420_4
      totalcross.sys.Settings.deviceId = "Desktop"; // guich@568_2
      if (totalcross.sys.Settings.applicationId != null)
         getAppSettings(); // guich@330_47
      try
      {
         // Fill all paths
         String basePath = System.getProperty("user.dir");
         totalcross.sys.Settings.vmPath = basePath;
         totalcross.sys.Settings.appPath = basePath;
         // guich@tc112_21: commented - if (totalcross.sys.Settings.dataPath == null) totalcross.sys.Settings.dataPath = basePath; // flsobral@tc100b5_51: Settings.dataPath was being overwritten if set before the Launcher was initialized.

         if (totalcross.sys.Settings.appPath != null) // guich@582_17: make sure that it ends with a slash
         {
            if (totalcross.sys.Settings.appPath.indexOf('/') >= 0 && !totalcross.sys.Settings.appPath.endsWith("/"))
               totalcross.sys.Settings.appPath += "/";
            else
            if (totalcross.sys.Settings.appPath.indexOf('\\') >= 0 && !totalcross.sys.Settings.appPath.endsWith("\\"))
               totalcross.sys.Settings.appPath += "\\";
         }
         totalcross.sys.Settings.userName = !isApplication?null:java.lang.System.getProperty("user.name");
      } catch (SecurityException se) {totalcross.sys.Settings.userName = null;}
   }

   public void settingsRefresh(boolean callStoreSettings) // guich@tc115_81
   {
      java.util.Calendar cal = java.util.Calendar.getInstance();
      totalcross.sys.Settings.daylightSavings = cal.get(java.util.Calendar.DST_OFFSET) != 0; // guich@tc112_1
      java.util.TimeZone tz = java.util.TimeZone.getDefault(); // guich@340_33
      totalcross.sys.Settings.timeZone = tz.getRawOffset() / (60*60*1000);
      totalcross.sys.Settings.timeZoneStr = java.util.TimeZone.getDefault().getID(); //flsobral@tc115_54: added field Settings.timeZoneStr
      if (callStoreSettings)
         try
         {
            storeSettings();
         } catch (Exception e) {}
   }

   ////  font and font metrics //////////////////////////////////////////////////////
   private totalcross.util.Hashtable htLoadedFonts = new totalcross.util.Hashtable(31);
   private UserFont loadUF(String fontName, String suffix)
   {
      try
      {
         return new UserFont(fontName, suffix);
      }
      catch (Exception e) 
      {
         String msg = ""+e.getMessage();
         if (!msg.startsWith("name") || !msg.endsWith("not found"))
            if (Settings.onJavaSE) e.printStackTrace();
      }
      return null;
   }

   public UserFont getFont(totalcross.ui.font.Font f, char c)
   {
      UserFont uf=null;
      try
      {
         // verify if its in the cache.
         String fontName = f.name;
         int size = Math.max(f.size,totalcross.ui.font.Font.MIN_FONT_SIZE); // guich@tc122_15: don't check for the maximum font size here
         char faceType = c < 0x3000 && f.style == 1 ? 'b' : 'p';
         int uIndex = ((int)c >> 8) << 8;
         String suffix = "$"+faceType+size+"u"+uIndex;
         String key = fontName+suffix;
         uf = (UserFont)htLoadedFonts.get(key);
         if (uf != null)
            return uf;

         if (fontName.charAt(0) == '$') // bruno@tc114_37: native fonts always start with '$'
            print("Native fonts are not supported on Desktop");
         else
         {
            // first, try to load the font itself using the current font pattern
            uf = loadUF(fontName, suffix);
            if (uf == null) // try now as a plain font
               uf = loadUF(fontName, "$p"+size+"u"+uIndex); // guich@tc122_15: ... check only here
            if (uf == null && f.size != totalcross.ui.font.Font.NORMAL_SIZE)
            {
               int t = f.size;
               while (uf == null && --t >= 5) // try to find the nearest size
                  uf = loadUF(fontName, "$p"+t+"u"+uIndex);
            }
            if (uf == null) // try now as the default size original face
               uf = loadUF(fontName, "$"+faceType+totalcross.ui.font.Font.NORMAL_SIZE+"u"+uIndex);
            if (uf == null && faceType != 'p') // try now as the default size plain font
               uf = loadUF(fontName, "$p"+totalcross.ui.font.Font.NORMAL_SIZE+"u"+uIndex);
         }
         
         // at last, use the default font
         if (uf == null)
            uf = loadUF(totalcross.ui.font.Font.DEFAULT, suffix);
         if (uf == null && fontName.charAt(0) != '$') // check if there's a font of any size - maybe the file has only one font?
            for (int i = totalcross.ui.font.Font.MIN_FONT_SIZE; i <= totalcross.ui.font.Font.MAX_FONT_SIZE; i++)
               if ((uf = loadUF(fontName,"$p"+i+"u"+uIndex)) != null)
                  break;
         if (uf == null) // check if there's a font of any size - at least with the default font
            for (int i = totalcross.ui.font.Font.MIN_FONT_SIZE; i <= totalcross.ui.font.Font.MAX_FONT_SIZE; i++)
               if ((uf = loadUF(totalcross.ui.font.Font.DEFAULT,"$p"+i+"u"+uIndex)) != null)
                  break;
         
         if (uf != null)
         {
            htLoadedFonts.put(key,uf); // note that we will use the original key to avoid entering all exception handlers.
            f.name = uf.fontName; // update the name, the font may have been replaced.
         }
         else
         if (htLoadedFonts.size() > 0)
            return c == ' ' ? null : getFont(f, ' '); // probably the index was outside the available ranges at this font - guich@tc110_28: if space, just return null
         else
         if (appletInitialized) // guich@500_1: when retroguard is loaded, Applet.init is never called, so we just skip here.
         {
            System.err.println("No fonts found! be sure to place the file "+totalcross.ui.font.Font.DEFAULT+".tcz in the same directory from where you're running your application"+(isApplication ? " or put a reference to TotalCrossSDK/etc folder in the classpath!" : "or in your applet's codebase or in a jar file!"));
            System.exit(2);
         }
      } catch (Exception e) {System.out.println(""+e);}
      return uf;
   }

   /** Represents the internal font structure, read from a pdb file. used internally. */
   // created by guich@200b2
   public static class CharBits         // pgr@402_50 - describe the bitmap for a given character
   {
      public int rowWIB;          // width in bytes
      public byte[] charBitmapTable;
      public int offset;          // offset relative to the bitmap table
      public int width;
   }

   private static Hashtable loadedTCZs = new Hashtable(31);
   public class UserFont
   {
      public Object nativeFont;    // stores the system font in some platforms
      public boolean antialiased;  // true if its antialiased
      public int firstChar;        // ASCII code of first character
      public int lastChar;         // ASCII code of last character
      public int spaceWidth;       // width of the space char
      public int maxWidth;         // width of font rectangle
      public int maxHeight;        // height of font rectangle
      public int owTLoc;           // offset to offset/width table
      public int ascent;           // ascent
      public int descent;          // descent
      public int rowWords;         // row width of bit image / 2

      public int   rowWidthInBytes;
      public int   bitmapTableSize;
      public byte  []bitmapTable;
      public int []bitIndexTable;
      public String fontName;
      public int numberWidth;

      private UserFont(String fontName, String sufix) throws Exception
      {
         this.fontName = fontName;
         String fileName = fontName+".tcz";
         TCZ z = (TCZ)loadedTCZs.get(fileName.toLowerCase());
         if (z == null)
         {
            InputStream is = openInputStream(fileName);
            if (is == null)
            {
               is = openInputStream("vm/"+fileName); // for the release sdk, there's no etc/fonts. the tcfont.tcz is located at dist/vm/tcfont.tcz
               if (is == null)
                  throw new Exception("file "+fileName+" not found"); // loaded = false
            }
            z = new TCZ(new IS2S(is));
            totalcross.io.ByteArrayStream fontChunks[];
            fontChunks = new totalcross.io.ByteArrayStream[z.numberOfChunks];
            for (int i =0; i < fontChunks.length; i++)
            {
               int s = z.getNextChunkSize();
               fontChunks[i] = new totalcross.io.ByteArrayStream(s);
               z.readNextChunk(fontChunks[i]);
            }
            z.bag = fontChunks;
            loadedTCZs.put(fileName.toLowerCase(), z);
         }
         fontName += sufix;
         int index  = z.findNamePosition(fontName.toLowerCase());
         if (index == -1)
            throw new Exception("name "+fontName+" not found"); // loaded = false

         totalcross.io.ByteArrayStream bas = ((totalcross.io.ByteArrayStream[])z.bag)[index];
         bas.reset();
         totalcross.io.DataStreamLE ds = new totalcross.io.DataStreamLE(bas);
         antialiased = ds.readUnsignedShort()==1;
         firstChar   = ds.readUnsignedShort();
         lastChar    = ds.readUnsignedShort();
         spaceWidth  = ds.readUnsignedShort();
         maxWidth    = ds.readUnsignedShort();
         maxHeight   = ds.readUnsignedShort();
         owTLoc      = ds.readUnsignedShort();
         ascent      = ds.readUnsignedShort();
         descent     = ds.readUnsignedShort();
         rowWords    = ds.readUnsignedShort();

         rowWidthInBytes = rowWords << (antialiased ? 3 : 1);
         bitmapTableSize = (int)rowWidthInBytes * (int)maxHeight;

         bitmapTable     = new byte[bitmapTableSize];
         ds.readBytes(bitmapTable);
         bitIndexTable   = new int[lastChar - firstChar + 1 + 1];
         for (int i=0; i < bitIndexTable.length; i++)
            bitIndexTable[i] = ds.readUnsignedShort();
         //
         if (firstChar <= '0' && '0' <= lastChar)
         {
            index = (int)'0' - (int)firstChar;
            numberWidth = bitIndexTable[index+1] - bitIndexTable[index];
         }
      }

      // Get the source x coordinate and width of the character
      public void setCharBits(char ch, CharBits bits)
      {
         if (firstChar <= ch && ch <= lastChar)
         {
            int index = (int)ch - (int)firstChar;
            bits.rowWIB = rowWidthInBytes;
            bits.charBitmapTable = bitmapTable;
            bits.offset = bitIndexTable[index];
            bits.width = bitIndexTable[index+1] - bits.offset;
         }
         else
         {
            bits.width = spaceWidth;
            bits.offset = -1;
         }
      }
   }
   
   public int getCharWidth(totalcross.ui.font.Font f, char ch) // guich@tc122_16: moved to outside UserFont, because each char may be in a different UserFont
   {
      UserFont font = (UserFont)f.hv_UserFont;
      if (ch < font.firstChar || ch > font.lastChar)
         f.hv_UserFont = font = Launcher.instance.getFont(f, ch);
      if (ch == 160)
         return font.numberWidth;
      if (ch < ' ')
         return (ch == '\t') ? font.spaceWidth * totalcross.ui.font.Font.TAB_SIZE : 0; // guich@tc100: handle tabs
      int index = (int)ch - (int)font.firstChar;
      return (font.firstChar <= ch && ch <= font.lastChar) ? font.bitIndexTable[index+1] - font.bitIndexTable[index] : font.spaceWidth;
   }
   

   private class AlertBox extends Frame implements java.awt.event.ActionListener
   {
      private java.awt.Button ok;
      private java.awt.TextArea ta;

      public AlertBox()
      {
         super("Alert");
         setLayout(new BorderLayout());
         add("Center",ta = new java.awt.TextArea());
         Panel p = new Panel();
         p.setLayout(new FlowLayout());
         p.add(ok = new java.awt.Button("Ok"));
         ok.addActionListener(this);
         add("South",p);
         pack();
         Dimension d = getToolkit().getScreenSize();
         setLocation(d.width/3,d.height/3);
      }

      public void actionPerformed(java.awt.event.ActionEvent ae)
      {
         if (ae.getSource() == ok)
            setVisible(false);
      }

      public void setText(String s)
      {
         ta.setText(s);
      }
   }

   public void alert(String msg)
   {
      if (!started)
         System.out.println(msg);
      else
      {
         alert.setText(msg);
         alert.setVisible(true);
         while (alert.isVisible())
            try {Thread.sleep(10);} catch (Exception e) {}
      }
   }
   /** Converts a java.io.InputStream into a totalcross.io.Stream */
   public static class IS2S extends totalcross.io.Stream
   {
      InputStream is;

      public IS2S(InputStream is)
      {
         this.is = is;
      }
      public void close()
      {
         try {is.close();} catch (Exception e) {}
         is = null;
      }
      public int readBytes(byte[] buf, int start, int count)
      {
         try
         {
            return is.read(buf, start, count);
         } catch (Exception e) {return -1;}
      }
      public int writeBytes(byte[] buf, int start, int count)
      {
         return 0; // not supported
      }
   }

   public static class S2IS extends java.io.InputStream
   {
      private Stream s;
      private byte[] oneByte = new byte[1];
      private int left;
      private boolean closeUnderlying;

      public S2IS(Stream s)
      {
         this(s, -1, true);
      }

      public S2IS(Stream s, int max)
      {
         this(s, max, true);
      }

      public S2IS(Stream s, int max, boolean closeUnderlying)
      {
         this.s = s;
         this.left = max;
         this.closeUnderlying = closeUnderlying;
      }

      public int read() throws java.io.IOException
      {
         if (left == 0)
            return -1;

         try
         {
            int r = s.readBytes(oneByte, 0, 1);

            if (left != -1 && r == 1)
               left--;

            return r > 0 ? ((int)oneByte[0] & 0xFF) : -1;
         }
         catch (IOException e)
         {
            throw new java.io.IOException(e.getMessage());
         }
      }

      public int read(byte[] buf, int off, int len) throws java.io.IOException
      {
         if (left == 0)
            return -1;

         try
         {
            if (left != -1 && len > left)
               len = left;

            int r = s.readBytes(buf, off, len);

            if (left != -1 && r > 0)
               left -= r;

            return r;
         }
         catch (IOException e)
         {
            throw new java.io.IOException(e.getMessage());
         }
      }

      public void close() throws java.io.IOException
      {
         if (closeUnderlying)
         {
            try
            {
               s.close();
            }
            catch (IOException e)
            {
               throw new java.io.IOException(e.getMessage());
            }
         }
      }
   }

   public static class S2OS extends java.io.OutputStream
   {
      private Stream s;
      private byte[] oneByte = new byte[1];
      private int count;
      private boolean closeUnderlying;

      public S2OS(Stream s)
      {
         this(s, true);
      }

      public S2OS(Stream s, boolean closeUnderlying)
      {
         this.s = s;
         this.closeUnderlying = closeUnderlying;
      }

      public int count()
      {
         return count;
      }

      public void write(int b) throws java.io.IOException
      {
         try
         {
            oneByte[0] = (byte)(b & 0xFF);

            int c = s.writeBytes(oneByte, 0, 1);
            if (c < 0)
               throw new java.io.IOException("Unknown error when writing to stream");
            count++;
         }
         catch (IOException e)
         {
            throw new java.io.IOException(e.getMessage());
         }
      }

      public void write(byte[] b, int off, int len) throws java.io.IOException
      {
         try
         {
            int c = s.writeBytes(b, off, len);
            if (c < 0)
               throw new java.io.IOException("Unknown error when writing to stream");
            count += c;
         }
         catch (IOException e)
         {
            throw new java.io.IOException(e.getMessage());
         }
      }

      public void close() throws java.io.IOException
      {
         if (closeUnderlying)
         {
            try
            {
               s.close();
            }
            catch (IOException e)
            {
               throw new java.io.IOException(e.getMessage());
            }
         }
      }
   }

   public void setTitle(String title)
   {
      if (isApplication)
      {
         frameTitle = title;
         if (frame != null)
            frame.setTitle(title);
      }
   }
   
   public void vibrate(final int millis)
   {
      if (isApplication)
      {
         new Thread() {
            public void run()
            {
               Point p = frame.getLocation();
               int x = p.x, y = p.y;
               
               int[] xPoints = { x - 3, x, x + 3, x, x + 3, x, x - 3, x};
               int[] yPoints = { y - 3, y, y + 3, y, y - 3, y, y + 3, y};
               int i = 0;
               int j = 0;
               
               int t = Vm.getTimeStamp();
               do
               {
                  frame.setLocation(xPoints[i], yPoints[j]);
                  
                  i = ++i % xPoints.length;
                  if (i == 0)
                     j = ++j % yPoints.length;
                  
                  Thread.yield();// give some time for the other threads to execute
               }
               while (Vm.getTimeStamp() - t < millis);
               
               frame.setLocation(x, y); // restore original location
            }
         }.start();
      }
   }
   
   public void setSIP(int option, Control edit, boolean secret)
   {
   }
   public static void checkLitebaseAllowed()
   {
   }
   public void componentHidden(ComponentEvent arg0)
   {
   }
   public void componentMoved(ComponentEvent arg0)
   {
   }

   public void componentShown(ComponentEvent arg0)
   {
   }
   {
      int w = frame.getWidth()-frame.insets.left-frame.insets.right;
      int h = frame.getHeight()-frame.insets.top-frame.insets.bottom;
      if (w < toWidth || h < toHeight)
         screenResized(w >= toWidth ? w : toWidth,h >= toHeight ? h : toHeight,true);
      else
         screenResized(w,h,false);
   }
}