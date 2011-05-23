/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
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

package totalcross.ui;

import totalcross.sys.*;
import totalcross.ui.gfx.*;
import totalcross.util.concurrent.*;

/** Spinner is a control that shows an image indicating that something is running in
 * the background. It has two styles: IPHONE and ANDROID. Its used in the ProgressBox.
 * 
 * To start the spin call the start method, and to stop it call the stop method.
 * 
 * @since TotalCross 1.3
 */
public class Spinner extends Control implements Runnable
{
   /** Used in the type field */
   public static final int IPHONE = 1;
   /** Used in the type field */
   public static final int ANDROID = 2;
   
   /** Defines the type of spinner for all instances. Defaults for IPHONE when running in iPhone and
    * ANDROID for all other platforms. 
    */
   public static int spinnerType = Settings.IPHONE.equals(Settings.platform) ? IPHONE : ANDROID;
   
   private Coord []coords;
   private int []colors;
   private int slots, slot0, size, type;
   private boolean running;
   private Lock lock;
   
   public Spinner()
   {
      type = spinnerType;
      slots = 8;
      if (UIColors.spinnerBack != -1) backColor = UIColors.spinnerBack;
      foreColor = UIColors.spinnerFore;
      lock = new Lock();
   }
   
   public void onBoundsChanged(boolean screenChanged)
   {
      size = width < height ? width : height;
      if ((size % 2) == 0) size--;
      
      if (!screenChanged || coords == null)
      {
         int xyc = size/2;
         // find the number of slots
         int bestn=0;
         switch (type)
         {
            case IPHONE: 
               bestn = height >= 24 ? 16 : 12;
               break;
            case ANDROID: 
               bestn = height >= 21 ? 12 : 8; 
               break;
         }
         if (slots != bestn)
         {
            slots = bestn;
            onColorsChanged(true);
         }
         
         if (type == ANDROID)
            return;
         
         int astep = 360 / slots;
         int a = 0;
         
         Graphics g = getGraphics();
         coords = new Coord[slots*3];
         for (int i = 0; i < coords.length;)
         {
            g.getAnglePoint(xyc,xyc,xyc+1,xyc+1,a-1,coords[i++] = new Coord());
            g.getAnglePoint(xyc,xyc,xyc,  xyc,  a,  coords[i++] = new Coord());
            g.getAnglePoint(xyc,xyc,xyc+1,xyc+1,a+1,coords[i++] = new Coord());
            a += astep;
         }
      }
   }
   
   public void onColorsChanged(boolean changed)
   {
      if (changed || colors == null)
      {
         if (colors == null || colors.length != slots)
            colors = new int[slots];
         for (int i = 0; i < slots; i++) 
            colors[i] = Color.interpolate(backColor,foreColor,100*i/slots);
      }
   }
   
   public void onPaint(Graphics g)
   {
      synchronized (lock)
      {
         g.useAA = true;
         
         int astep = 360/slots;
         int a = 360;
         
         int div = type == IPHONE ? 3 : 1;
         int xyc = size/2;
         for (int i = slots * div; --i >= 0;)
         {
            int idx = (i/div+slot0)%slots;
            switch (type)
            {
               case ANDROID:
                  g.backColor = colors[idx];
                  g.foreColor = backColor;
                  g.fillPie(xyc,xyc,xyc,a-astep,a);
                  g.drawPie(xyc,xyc,xyc,a-astep,a);
                  a -= astep;
                  break;
               case IPHONE:
                  g.backColor = g.foreColor = colors[idx];
                  g.drawLine(coords[i].x,coords[i].y,xyc,xyc);
            }
            g.backColor = backColor;
            g.fillCircle(xyc,xyc,xyc/2);
         }
      }
   }
   
   /** Starts the spinning thread. */
   public void start()
   {
      if (running)
         return;
      running = true;
      new Thread(this).start();
   }
   
   /** Stops the spinning thread. */
   public void stop()
   {
      running = false;
   }

   public void run()
   {
      while (running)
      {
         slot0++;
         repaintNow();
         Vm.sleep(120);
      }      
   }
}
