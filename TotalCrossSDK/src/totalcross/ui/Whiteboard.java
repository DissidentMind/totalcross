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



package totalcross.ui;

import totalcross.ui.dialog.*;
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;
import totalcross.ui.image.*;

/** This is a whiteboard that can be used to draw something. 
 * It uses a special event flag in order to improve the accuracy.
 */

public class Whiteboard extends Control
{
   //to draw Lines
   private int oldX;
   private int oldY;
   private Image img;
   private Graphics gImg;
   private Graphics gScr;
   /** Set this to some color so a frame can be drawn around the image */
   public int borderColor=-1;
   /** Set to true to enable antialiase on the line drawing. It must be set right after the constructor. */
   public boolean useAA;
   private boolean isEmpty=true;
   
   /** Set to true to draw a thick line.
    * @since TotalCross 1.14
    */
   public boolean thick; // guich@tc114_78
   
   private int desiredPenColor = -1;

   /** Constructs a new whiteboard, setting the back color to white. */
   public Whiteboard()
   {
      backColor = Color.WHITE;
      focusTraversable = false; // guich@tc123_13
   }

   /** Now that we know our bounds, we can create the image that will hold the drawing */
   public void onBoundsChanged(boolean screenChanged)
   {
      //if (!screenChanged)
      if (isEmpty)
         setImage(null); // resize to width and height
   }

   /** Returns the image where the drawing is taking place. */
   public Image getImage()
   {
      return this.img;
   }

   /** Returns the preferred width: FILL */
   public int getPreferredWidth()
   {
      return FILL;
   }

   /** Returns the preferred height: FILL */
   public int getPreferredHeight()
   {
      return FILL;
   }

   /** Sets the image for this WhiteBoard. Pass null to create an empty image. */
   public void setImage(Image image)
   {
      try
      {
         isEmpty = image == null;
         this.img = image == null ? new Image(width,height) : image;
         this.gImg = img.getGraphics();
         if (desiredPenColor != -1) gImg.foreColor = desiredPenColor;
         gImg.useAA = useAA;
         int lastColor = gImg.foreColor;
         if (image == null)
         {
            gImg.backColor = backColor;
            gImg.fillRect(0,0,width,height);
         }
         if (borderColor != -1)
         {
            gImg.foreColor = borderColor;
            gImg.drawRect(0,0,width,height);
         }
         gImg.foreColor = lastColor;
         Window.needsPaint = true;
      } catch (ImageException e) {new MessageBox("Error","Not enough memory to create image").popup();}
   }

   /** Clears the WhiteBoard to the current background color. */
   public void clear()
   {
      int lastColor = gImg.foreColor;
      gImg.backColor = backColor;
      gImg.fillRect(0,0,width,height);
      if (borderColor != -1)
      {
         gImg.foreColor = borderColor;
         gImg.drawRect(0,0,width,height);
      }
      gImg.foreColor = lastColor;
      Window.needsPaint = true;
   }

   /** Sets the drawing pen color */
   public void setPenColor(int c) // guich@300_65
   {
      desiredPenColor = c;
      if (gImg != null)
         gImg.foreColor = gScr.foreColor = c;
   }
   
   /** Returns the drawing pen color. */
   public int getPenColor()
   {
      return desiredPenColor;
   }

   public void onPaint(Graphics g)
   {
      if (gScr == null)
      {
         gScr = getGraphics(); // create the graphics object that will be used to repaint the image
         gScr.setClip(0,0,width,height);
         gScr.useAA = useAA;
         if (desiredPenColor != -1)
            gScr.foreColor = desiredPenColor;
      }
      g.drawImage(img,0,0); // draw the image...
   }

   public void onEvent(Event event)
   {
      PenEvent pe;
      switch (event.type)
      {
         case PenEvent.PEN_DOWN:
            pe = (PenEvent)event;
            oldX = pe.x;
            oldY = pe.y;
            gScr.setPixel(pe.x,pe.y);
            gImg.setPixel(pe.x,pe.y);
            getParentWindow().setGrabPenEvents(this); // guich@tc100: redirect all pen events to here, bypassing other processings
            break;
         case PenEvent.PEN_DRAG:
            pe = (PenEvent)event;
            gScr.drawLine(oldX,oldY,pe.x,pe.y); // guich@580_34: draw directly on screen
            gImg.drawLine(oldX,oldY,pe.x,pe.y);
            if (thick)
            {
               gScr.drawLine(oldX+1,oldY+1,pe.x+1,pe.y+1);
               gScr.drawLine(oldX-1,oldY-1,pe.x-1,pe.y-1);
               gScr.drawLine(oldX+1,oldY+1,pe.x-1,pe.y-1);
               gScr.drawLine(oldX-1,oldY-1,pe.x+1,pe.y+1);
               
               gImg.drawLine(oldX+1,oldY+1,pe.x+1,pe.y+1);
               gImg.drawLine(oldX-1,oldY-1,pe.x-1,pe.y-1);
               gImg.drawLine(oldX+1,oldY+1,pe.x-1,pe.y-1);
               gImg.drawLine(oldX-1,oldY-1,pe.x+1,pe.y+1);
            }
            oldX = pe.x;
            oldY = pe.y;
            Window.updateScreen(); // important at desktop!
            break;
         case PenEvent.PEN_UP:
            getParentWindow().setGrabPenEvents(null);
            break;
      }
   }
}
