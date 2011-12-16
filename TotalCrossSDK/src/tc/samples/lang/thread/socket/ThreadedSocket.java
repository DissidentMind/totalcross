/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
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



package tc.samples.lang.thread.socket;

import totalcross.net.*;
import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.dialog.*;
import totalcross.ui.event.*;

public class ThreadedSocket extends MainWindow implements Runnable
{
   static
   {
      Settings.useNewFont = true;
   }

   static interface SetX
   {
      public void incX(int x);
      public void setX(int x);
   }
   
   Button directionButton;
   Button pauseButton,unpauseButton;
   static boolean paused,paused0;
   SetX[] containers;
   int direction = Settings.platform.equals(Settings.JAVA) || Settings.platform.equals(Settings.WIN32) ? 1 : 4;  //      moving right
   Thread slideThread;
   boolean running,finished;
   Label lmem;

   public ThreadedSocket()
   {
      setTitle("Threaded Socket");
   }

   public void initUI()
   {
      Label l;
      Vm.tweak(Vm.TWEAK_DUMP_MEM_STATS,true);
      if (Settings.platform.equals(Settings.PALMOS)) // palm require netlib to be loaded in the main execution line
      {
         add(l = new Label("WAIT, OPENING SOCKET\nBEFORE STARTING THREADS.\nSEE THE DIALOG BELOW"),CENTER,TOP);
         repaintNow();
         try
         {
            new Socket("www.google.com",80,10000).close();
         }
         catch (Exception e)
         {
            new MessageBox("Error","Error when connecting. Aborting the sample... ("+e+")").popup();
            exit(1);
            return;
         }
         remove(l);
         Vm.setAutoOff(false); // palm halts if using sockets in a thread and the device turns off
      }

      directionButton = new Button("Switch Direction");
      containers = new SetX[3];
      containers[0] = new TypingContainer(true);
      containers[1] = new HTTPContainer();
      containers[2] = new TypingContainer(false);
      add(directionButton,LEFT,TOP);
      add(pauseButton = new Button("Pause"),RIGHT,TOP);
      add(unpauseButton = new Button("Unpause"),RIGHT,TOP);
      unpauseButton.setVisible(false);
      add(lmem = new Label("",CENTER),AFTER,SAME,FIT,PREFERRED,directionButton);

      for (int i = 0; i < containers.length; i++)
         add((Container)containers[i], i == 0 ? LEFT : AFTER,i==0 ? AFTER : SAME,SCREENSIZE,FILL);

      slideThread = new Thread(this);
      slideThread.start();
   }

   public void pause()
   {
      paused = !paused;
      pauseButton.setVisible(!paused);
      unpauseButton.setVisible(paused);
   }

   public void onEvent(Event event)
   {
      if (event.type == ControlEvent.PRESSED)
      {
         if (event.target == directionButton)
            restartThread(true);
         else
         if (event.target == pauseButton || event.target == unpauseButton)
            pause();
      }
      super.onEvent(event);
   }
   
   private void restartThread(boolean changeDir)
   {
      running = false;
      while (!finished)
         Vm.sleep(1);
      if (changeDir)
         direction = -direction;
      slideThread = new Thread(this);
      slideThread.start();
   }

   public void run()
   {
      running = true;
      finished = false;
      while (running)
      {
         SetX temp;
         if (direction > 0)
         {
            temp = containers[2];
            containers[2] = containers[1];
            containers[2].setX(0);

            containers[1] = containers[0];
            containers[1].setX(-width);

            containers[0] = temp;
            containers[0].setX(-width*2);
         }
         else
         {
            temp = containers[0];
            containers[0] = containers[1];
            containers[0].setX(0);

            containers[1] = containers[2];
            containers[1].setX(width);

            containers[2] = temp;
            containers[2].setX(width*2);
         }
         while (running && ((Container)containers[1]).getX() != 0)
         {
            if (!paused)
            {
               for (int i = 0; i < containers.length; i++)
                  containers[i].incX(direction);
               lmem.setText(Convert.toString(Vm.getFreeMemory()));
               //try {repaintNow();} catch (Throwable t) {t.printStackTrace();}
            }
            Vm.sleep(5); // without this, events are blocked in Palm OS.
         }
         if (running)
         {
            paused0 = true;
            Vm.sleep(1000);
            paused0 = false;
         }
      }
      finished = true;
   }
   
   public void reposition()
   {
      super.reposition();
      restartThread(false);
   }
}