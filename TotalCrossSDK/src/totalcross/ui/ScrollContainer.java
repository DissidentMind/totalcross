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
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;

/**
 * ScrollContainer is a container with a horizontal only, vertical only, both or no
 * ScrollBars, depending on the control positions.
 * The default unit scroll is an Edit's height (for the vertical
 * scrollbar), and the width of '@' (for the horizontal scrollbar).
 * <p>
 * <b>Caution</b>: you must not use RIGHT, BOTTOM, CENTER and FILL when setting the control bounds,
 * unless you disable the corresponding ScrollBar!
 * <p>
 * Here is an example showing how it can be used:
 *
 * <pre>
 * public class MyProgram extends MainWindow
 * {
 * ScrollContainer sc;
 *
 * public void initUI()
 * {
       ScrollContainer sc;
       add(sc = new ScrollContainer());
       sc.setBorderStyle(BORDER_SIMPLE);
       sc.setRect(LEFT+10,TOP+10,FILL-20,FILL-20);
       int xx = new Label("Name99").getPreferredWidth()+2; // edit's alignment
       for (int i =0; i < 100; i++)
       {
          sc.add(new Label("Name"+i),LEFT,AFTER);
          sc.add(new Edit("@@@@@@@@@@@@@@@@@@@@"),xx,SAME);
          if (i % 3 == 0) sc.add(new Button("Go"), AFTER+2,SAME,PREFERRED,SAME);
       }
 * }
 *}
 * </pre>
 */

public class ScrollContainer extends Container implements Scrollable
{
   /** Returns the scrollbar for this ScrollContainer. With it, you can directly
    * set its parameters, like blockIncrement, unitIncrement and liveScrolling.
    * But be careful, don't mess with the minimum, maximum and visibleItems.
    */
   public ScrollBar sbH,sbV;

   /** The Flick object listens and performs flick animations on PenUp events when appropriate. */
   protected Flick flick;

   protected ClippedContainer bag;
   protected Container bag0; // used to make sure that the clipping will work
   boolean changed;
   private int lastV=-10000000, lastH=-10000000; // eliminate duplicate events
   /** Set to true, to make the surrounding container shrink to its size. */
   public boolean shrink2size;
   private boolean isScrolling;
   private boolean scScrolled;

   /** Standard constructor for a new ScrollContainer, with both scrollbars enabled.
     */
   public ScrollContainer()
   {
      this(true);
   }
   
   /** Constructor used to specify when both scrollbars are enabled or not. */
   public ScrollContainer(boolean allowScrollBars)
   {
      this(allowScrollBars, allowScrollBars);
   }
   
   /** Constructor used to specify when each scrollbar is enabled or not.
    * By disabling the horizontal scrollbar, you can use RIGHT and CENTER on the x parameter of a control that is added.
    * By disabling the vertical scrollbar, you can use BOTTOM and CENTER on the y parameter of a control that is added.
    * @since TotalCross 1.27 
    */
   public ScrollContainer(boolean allowHScrollBar, boolean allowVScrollBar)
   {
      super.add(bag0 = new Container());
      bag0.add(bag = new ClippedContainer());
      bag.ignoreOnAddAgain = bag.ignoreOnRemove = true;
      bag0.ignoreOnAddAgain = bag0.ignoreOnRemove = true;
      bag.setRect(0,0,4000,20000); // set an arbitrary size
      bag.setX = -100000000; // ignore this setX and use the next one
      if (allowHScrollBar)
      {
         sbH = Settings.fingerTouch ? new ScrollPosition(ScrollBar.HORIZONTAL) : new ScrollBar(ScrollBar.HORIZONTAL);
         sbH.setLiveScrolling(true);
         sbH.setMaximum(0);
      }
      if (allowVScrollBar)
      {
         sbV = Settings.fingerTouch ? new ScrollPosition(ScrollBar.VERTICAL) : new ScrollBar(ScrollBar.VERTICAL);
         sbV.setLiveScrolling(true);
         sbV.setMaximum(0);
      }
      if (Settings.fingerTouch)
         flick = new Flick(this);
   }
   
   public boolean flickStarted()
   {
      return true;//isScrolling; // flick1.robot fails with this
   }
   
   public void flickEnded(boolean atPenDown)
   {
   }
   
   public boolean canScrollContent(int direction, Object target)
   {
      if (direction == 4)
         direction = 4;
      boolean ret = false;
      if (Settings.fingerTouch)
         switch (direction)
         {
            case DragEvent.UP   : ret = sbV != null && sbV.value > sbV.minimum; break;
            case DragEvent.DOWN : ret = sbV != null && (sbV.value + sbV.visibleItems) < sbV.maximum; break;
            case DragEvent.LEFT : ret = sbH != null && sbH.value > sbH.minimum; break;
            case DragEvent.RIGHT: ret = sbH != null && (sbH.value + sbH.visibleItems) < sbH.maximum; break;
         }
      return ret;
   }
   
   public boolean scrollContent(int dx, int dy)
   {
      boolean scrolled = false;

      if (dx != 0 && sbH != null)
      {
         int oldValue = sbH.value;
         sbH.setValue(oldValue + dx);
         lastH = sbH.value;

         if (oldValue != lastH)
         {
            bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
            bag.setRect(LEFT - lastH, KEEP,KEEP,KEEP);
            bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
            scrolled = true;
         }
      }
      if (dy != 0 && sbV != null)
      {
         int oldValue = sbV.value;
         sbV.setValue(oldValue + dy);
         lastV = sbV.value;

         if (oldValue != lastV)
         {
            bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
            bag.setRect(KEEP, TOP - lastV, KEEP, KEEP);
            bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
            scrolled = true;
         }
      }

      if (scrolled)
      {
         Window.needsPaint = true;
         return true;
      }
      else
         return false;
   }

   public int getScrollPosition(int direction)
   {
      return direction == DragEvent.LEFT || direction == DragEvent.RIGHT ? lastH : lastV;
   }

   /** Adds a child control to the bag container. */
   public void add(Control control)
   {
      changed = true;
      bag.add(control);
   }
   
   /** Adds a control to the ScrollContainer itself. Used internally. */
   void addToSC(Control c)
   {
      bag0.add(c);
   }

   /**
   * Removes a child control from the bag container.
   */
   public void remove(Control control)
   {
      changed = true;
      bag.remove(control);
   }

   protected void onBoundsChanged(boolean screenChanged)
   {
      bag0.setRect(LEFT, TOP, FILL, FILL, null, screenChanged);
      if (sbH == null && sbV == null && shrink2size)
      {
         bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
         bag.setRect(LEFT, TOP, FILL, FILL, null, screenChanged);
         bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
      }
      else if (sbH == null || sbV == null)
      {
         int w = sbH != null ? 4000 : FILL - (sbV != null && !Settings.fingerTouch ? sbV.getPreferredWidth() : 0);
         int h = sbV != null ? 20000 : FILL - (sbH != null && !Settings.fingerTouch ? sbH.getPreferredHeight() : 0);
         bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
         bag.setRect(LEFT, TOP, w, h, null, screenChanged);
         bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
      }
   }

   protected void onColorsChanged(boolean colorsChanged)
   {
      super.onColorsChanged(colorsChanged);
      if (colorsChanged)
      {
         bag.setBackForeColors(backColor, foreColor);
         bag0.setBackForeColors(backColor, foreColor);
         if (sbV != null)
            sbV.setBackForeColors(backColor, foreColor);
         if (sbH != null)
            sbH.setBackForeColors(backColor,foreColor);
      }
   }

   /** This method resizes the control to the needed bounds, based on added childs. 
    * Must be called if you're controlling reposition by your own, after you repositioned the controls inside of it. */
   public void resize()
   {
      int maxX = 0;
      int maxY = 0;
      for (Control child = bag.children; child != null; child = child.next)
      {
         maxX = Math.max(maxX,child.x+child.width);
         maxY = Math.max(maxY,child.y+child.height);
      }
      resize(maxX == 0 ? FILL : maxX, maxY == 0 ? PREFERRED : maxY);
   }

   /** This method resizes the control to the needed bounds, based on the given maximum width and heights. */
   /** This method resizes the control to the needed bounds, based on the given maximum width and heights. */
   public void resize(int maxX, int maxY)
   {
      bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
      bag.setRect(bag.x, bag.y, maxX, maxY);
      bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
      if (sbV != null)
         super.remove(sbV);
      if (sbH != null)
         super.remove(sbH);
      // check if we need horizontal or vertical or both scrollbars
      boolean needX = false, needY = false, changed=false;
      Rect r = getClientRect();
      int availX = r.width;
      int availY = r.height;
      boolean finger = ScrollPosition.AUTO_HIDE && 
                       ((sbH != null && sbH instanceof ScrollPosition) ||
                        (sbV != null && sbV instanceof ScrollPosition));
      if (sbH != null || sbV != null)
         do
         {
            changed = false;
            if (!needY && maxY > availY)
            {
               changed = needY = true;
               if (finger && sbH != null && sbV != null) availX -= sbV.getPreferredWidth();
            }
            if (!needX && maxX > availX) // do we need an horizontal scrollbar?
            {
               changed = needX = true;
               if (finger && sbV != null && sbH != null) availY -= sbH.getPreferredHeight(); // remove the horizbar area from the avail Y area
            }
         } while (changed);

      if (sbH != null || sbV != null || !shrink2size)
         bag0.setRect(r.x,r.y,r.width-(!finger && needY && sbV != null ? sbV.getPreferredWidth() : 0), r.height-(!finger && needX && sbH != null ? sbH.getPreferredHeight() : 0));
      else
      {
         bag0.setRect(r.x,r.y,maxX,maxY);
         setRect(this.x,this.y,maxX,maxY);
      }
      if (needX && sbH != null)
      {
         super.add(sbH);
         sbH.setMaximum(maxX);
         sbH.setVisibleItems(bag0.width);
         sbH.setRect(LEFT,BOTTOM,FILL-(!finger && needY?sbV.getPreferredWidth():0),PREFERRED);
         sbH.setUnitIncrement(flick != null && flick.scrollDistance > 0 ? flick.scrollDistance : fm.charWidth('@'));
         lastH = -10000000;
      }
      else if (sbH != null) sbH.setMaximum(0); // kmeehl@tc100: drag-scrolling depends on this to determine the bounds
      if (needY && sbV != null)
      {
         super.add(sbV);
         sbV.setMaximum(maxY);
         sbV.setVisibleItems(bag0.height);
         sbV.setRect(RIGHT,TOP,PREFERRED,FILL);
         sbV.setUnitIncrement(flick != null && flick.scrollDistance > 0 ? flick.scrollDistance : fmH+Edit.prefH);
         lastV = -10000000;
      }
      else if (sbV != null) sbV.setMaximum(0); // kmeehl@tc100: drag-scrolling depends on this to determine the bounds
      Window.needsPaint = true;
   }

   public void reposition()
   {
      super.reposition();
      resize();
      if (sbH != null) 
         sbH.setValue(0);
      if (sbV != null) 
         sbV.setValue(0);
      bag.x = bag.y = 0;
   }
   
   /** Returns the preferred width AFTER the resize method was called.
    * If the ScrollBars are disabled, returns the maximum size of the container to hold all controls.
    */
   public int getPreferredWidth()
   {
      return sbV == null ? bag.width : sbH.maximum + (sbV.maximum == 0 ? 0 : sbV.getPreferredWidth());
   }

   /** Returns the preferred height AFTER the resize method was called. 
   * If the ScrollBars are disabled, returns the maximum size of the container to hold all controls.
   */
   public int getPreferredHeight()
   {
      return sbH == null ? bag.height : sbV.maximum + (sbH.maximum == 0 ? 0 : sbH.getPreferredWidth());
   }

   public void onPaint(Graphics g)
   {
      if (changed)
      {
         resize();
         changed = false;
      }
      super.onPaint(g);
   }

   public void onEvent(Event event)
   {
      switch (event.type)
      {
         case ControlEvent.PRESSED:
            if (event.target == sbV && sbV.value != lastV)
            {
               lastV = sbV.value;
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
               bag.setRect(bag.x,TOP-lastV,bag.width,bag.height);
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
            }
            else
            if (event.target == sbH && sbH.value != lastH)
            {
               lastH = sbH.value;
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
               bag.setRect(LEFT-lastH,bag.y,bag.width,bag.height);
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
            }
            break;
         case PenEvent.PEN_DOWN:
            scScrolled = false;
            break;
         case PenEvent.PEN_DRAG:
            if (event.target == sbV || event.target == sbH) break;
            if (Settings.fingerTouch)
            {
               DragEvent de = (DragEvent)event;
               int dx = -de.xDelta;
               int dy = -de.yDelta;
               if (isScrolling)
               {
                  scrollContent(dx, dy);
                  event.consumed = true;
               }
               else
               {
                  int direction = DragEvent.getInverseDirection(de.direction);
                  if (!flick.isValidDirection(direction))
                     break;
                  if (canScrollContent(direction, de.target) && scrollContent(dx, dy))
                     event.consumed = isScrolling = scScrolled = true;
               }
            }
            break;
         case PenEvent.PEN_UP:
            isScrolling = false;
            break;
         case ControlEvent.HIGHLIGHT_IN:
            if (event.target != this)
               scrollToControl((Control)event.target);
            break;
      }
   }

   /** Scrolls to the given control. */
   public void scrollToControl(Control c) // kmeehl@tc100
   {
      if (c != null && (sbH != null || sbV != null))
      {
         Rect r = c.getRect();
         Control f = c.parent;
         while (f.parent != this)
         {
            r.x += f.x;
            r.y += f.y;
            f = f.parent;
            if (f == null)
               return;// either c is not in this container, or it has since been removed from the UI
         }

         // horizontal
         if (r.x < 0 || r.x2() > bag0.width && sbH != null)
         {
            lastH = sbH.value;
            int val = lastH + (r.x <= 0 || r.width > bag0.width ? r.x : (r.x2()-bag0.width));
            if (val < sbH.minimum)
               val = sbH.minimum;
            sbH.setValue(val);
            if (lastH != sbH.value)
            {
               lastH = sbH.value;
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
               bag.setRect(LEFT-lastH,bag.y,bag.width,bag.height);
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
            }
         }
         // vertical
         if (r.y < 0 || r.y2() > bag0.height && sbV != null)
         {
            lastV = sbV.value;
            int val = lastV + (r.y <= 0 || r.height > bag0.height ? r.y : (r.y2() - bag0.height));
            if (val < sbV.minimum)
               val = sbV.minimum;
            sbV.setValue(val);
            if (lastV != sbV.value)
            {
               lastV = sbV.value;
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = false;
               bag.setRect(bag.x,TOP-lastV,bag.width,bag.height);
               bag.uiAdjustmentsBasedOnFontHeightIsSupported = true;
            }
         }
      }
   }
   
   public void setBorderStyle(byte border)
   {
      if (shrink2size)
         bag.setBorderStyle(border);
      else
         super.setBorderStyle(border);
   }
   
   public Flick getFlick()
   {
      return flick;
   }
   
   public boolean wasScrolled()
   {
      return scScrolled;
   }

   /**
    * Removes all controls from the ScrollContainer.
    */
    public void removeAll()
    {
       bag.removeAll();
    }

}
