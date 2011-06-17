/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 1998, 1999 Wabasoft <www.wabasoft.com>                         *
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
import totalcross.ui.font.*;
import totalcross.ui.gfx.*;
import totalcross.ui.image.*;
import totalcross.ui.media.*;
import totalcross.util.*;

/**
 * TabbedContainer is a bar of text or image tabs.
 * It is assumed that all images will have the same height, but they may have different widths.
 * <br>
 * A scroll is automatically added when the total width of the titles is bigger than the control's width.
 * <br>
 * The containers are created automatically and switched when the user press the corresponding tab.
 * <p>
 * Here is an example showing a tab bar being used:
 *
 * <pre>
 * public class MyProgram extends MainWindow
 * {
 *    TabbedContainer tab;
 *
 *    public void initUI()
 *    {
 *       String names[] = {"Edition","Report"};
 *       tab = new TabbedContainer(names);
 *       add(tab);
 *       tab.setGaps(2,2,2,2); // set it before setting the rect
 *       tab.setRect(LEFT,TOP,FILL,FILL);
 *       tab.setContainer(0,new Edition()); // replace container 1 by a class that extends Container.
 *       tab.getContainer(1).add(new Label("Not implemented"),CENTER,CENTER);
 *    }
 *
 *    public void onEvent(Event event)
 *    {
 *       if (event.type == ControlEvent.PRESSED && event.target == tp)
 *       {
 *          int activeIndex = tp.getActiveTab();
 *          ... handle tab being pressed
 *       }
 *    }
 * }
 * </pre>
 * Here's another sample that will show two TabbedContainers, one with images and another one with scrolling tabs.
 * Note that you must create img1.png and img2.png.
 * <pre>
 * TabbedContainer tp1 = new TabbedContainer(new Image[]{new Image("img1.png"), new Image("img2.png")}, null);
 * add(tp1);
 * tp1.setRect(LEFT,TOP,Settings.screenWidth/2,Settings.screenHeight/2);
 * tp1.activeTabBackColor = Color.getRGB(222,222,222);
 *
 * TabbedContainer tp2 = new TabbedContainer(new String[]{"verinha","marcelo","denise","guilherme","renato","michelle","rafael","barbara","lucas","ronaldo","nenem",});
 * add(tp2);
 * tp2.setRect(LEFT,AFTER+2,FILL,FILL);
 * </pre>
 */

public class TabbedContainer extends Container implements Scrollable
{
   private int activeIndex=-1;
   private String []strCaptions;
   private Image imgCaptions[],imgDis[];
   private boolean isTextCaption=true;
   private Container containers[];
   private int count;
   private int tabH;
   private int captionColor = Color.BLACK;
   private boolean atTop=true;
   private Rect [] rects,rSel,rNotSel;
   private int fColor,cColor;
   private int fadedColor[];
   private Rect clientRect;
   private ArrowButton btnLeft, btnRight;
   private static final byte FOCUSMODE_OUTSIDE = 0;
   private static final byte FOCUSMODE_CHANGING_TABS = 1;
   private static final byte FOCUSMODE_INSIDE_CONTAINERS = 2;
   private byte focusMode;
   private boolean brightBack;
   /** Set to true to enable the beep when a tab is clicked */
   public  boolean beepOn; // guich@230_37
   /** Set the arrows color right after the constructor and after calling setCaptionsColor, which also change this property. */
   public int arrowsColor = Color.BLACK;
   private Font bold;
   private int btnX;
   private int transpColor=-1;
   private int style = Window.RECT_BORDER;
   private boolean []disabled; // guich@tc110_58
   // flick support
   private boolean isScrolling;
   private boolean flickTimerStarted=true;

   /** This color is the one used to paint the background of the active tab.
    * This is specially useful for image tabs.
    * @since SuperWaba 5.64
    */
   public int activeTabBackColor=-1; // guich@564_14

   /** Sets the tabs with the same colors of the container.
    * @since SuperWaba 5.72
    */
   public boolean useOnTabTheContainerColor; // guich@572_12

   /** Stores the last active tab index, or -1 if none was previously selected.
     * @since SuperWaba 4.21
     */
   public int lastActiveTab = -1; // guich@421_30: changed name to conform with getActiveIndex

   /** To be used on the setType method: specifies that the tabs will be placed on the top. */
   public static final byte TABS_TOP = 0;
   /** To be used on the setType method: specifies that the tabs will be placed on the bottom. */
   public static final byte TABS_BOTTOM = 1;

   /** The Flick object listens and performs flick animations on PenUp events when appropriate. */
   protected Flick flick;

   private TabbedContainer(int count)
   {
      ignoreOnAddAgain = ignoreOnRemove = true;
      this.count = count;
      this.focusTraversable = true; // kmeehl@tc100
      started = true;
      focusHandler = true;
      containers = new Container[count];
      if (Settings.fingerTouch)
      {
         flick = new Flick(this);
         flick.forcedFlickDirection = Flick.HORIZONTAL_DIRECTION_ONLY;
         flick.maximumAccelerationMultiplier = 1;
      }
      // create the rects since we want to reuse them
      rects = new Rect[count];
      for (int i = count-1; i >= 0; i--)
      {
         rects[i] = new Rect();
         Container c = containers[i] = new Container();
         flick.addEventSource(c);
         c.ignoreOnAddAgain = c.ignoreOnRemove = true;
      }
      disabled = new boolean[count];
   }
   
   public void initUI()
   {
      onBoundsChanged(false);
   }
   
   /** Returns the number of tabs.
    * @since TotalCross 1.15
    */
   public int getTabCount()
   {
      return count;
   }

   /** Sets the given tab index as enabled or not. When a tab is disabled, it is displayed faded,
    * and if the user clicks on it, nothing happens. However, you still can activate it by calling
    * setActiveTab. If there are no tabs enabled, the current tab will be made active and the controls will
    * also be enabled. So, if you plan to disable all tabs, better disable the TabbedContainer control instead.
    * @param on If true, the tab is enabled, if false it is disabled.
    * @param tabIndex The tab's index (0 to count-1)
    * @since TotalCross 1.01
    * @see #setActiveTab
    */
   public void setEnabled(int tabIndex, boolean on) // guich@tc110_58
   {
      disabled[tabIndex] = !on;
      int back = useOnTabTheContainerColor ? containers[tabIndex].backColor : backColor;
      if (!on && !isTextCaption && (imgDis[tabIndex] == null || fadedColor[tabIndex] != back))
         try
         {
            imgDis[tabIndex] = imgCaptions[tabIndex].getFadedInstance(fadedColor[tabIndex] = back);
         }
         catch (ImageException e)
         {
            imgDis[tabIndex] = imgCaptions[tabIndex];
         }
      if (!on && activeIndex == tabIndex) // move to next tab
         setActiveTab(nextEnabled(activeIndex,true));
      Window.needsPaint = true;
   }

   /** Returns if the given tab index is enabled.
    * @since TotalCross 1.01
    */
   public boolean isEnabled(int tabIndex)
   {
      return !disabled[tabIndex];
   }

   /** Constructs a tab bar control with Strings as captions. */
   public TabbedContainer(String []strCaptions)
   {
      this(strCaptions.length);
      this.strCaptions = strCaptions;
      onFontChanged();
   }

   /** Constructs a tab bar control with images as captions, using the given color as transparent color.
    * If you don't want to use transparent colors, just pass -1 to the color. */
   public TabbedContainer(Image []imgCaptions, int transparentColor) // guich@564_13
   {
      this(imgCaptions.length);
      this.imgCaptions = imgCaptions;
      if (transparentColor >= 0)
      {
         this.transpColor = transparentColor;
         for (int i = count-1; i >= 0; i--)
            imgCaptions[i].transparentColor = transparentColor;
      }
      imgDis = new Image[count];
      fadedColor = new int[count];
      Convert.fill(fadedColor, 0, count-1, -1);
      isTextCaption = false;
      onFontChanged();
   }

   /** Sets the position of the tabs. use constants TABS_TOP or TABS_BOTTOM.
     * Since the tabs are not changed dinamicaly, this method must be called
     * after the constructor. */
   public void setType(byte type)
   {
      atTop = type == TABS_TOP;
      onFontChanged();
   }

   /** Returns the Container for tab i */
   public Container getContainer(int i)
   {
      return containers[i];
   }

   /** Sets the type of border. Currently, only the Window.NO_BORDER and Window.RECT_BORDER are supported. NO_BORDER only draws the line under the tabs. */
   public void setBorderStyle(byte style)
   {
      this.style = style;
   }

   /** Replaces the default created Container with the given one. This way you can
    * avoid adding a container to a container and, as such, waste memory.
    * Note that you must do this before the first setRect for this TabbedContainer; otherwise,
    * you must explicitly call setRect again to update the added container bounds
    */
   public void setContainer(int i, Container container)
   {
      if (containers != null && i >= 0 && i < containers.length)
      {
         Container old = containers[i];
         containers[i] = container;
         if (i == activeIndex) // guich@300_34: fixed problem when the current tab was changed
         {
            remove(old);
            if (flick != null)
               flick.removeEventSource(old);
            add(container);
            tabOrder.removeAllElements(); // don't let the cursor keys get into our container
            container.requestFocus();
         }
         if (!container.started) // guich@340_58: set the container's rect
         {
            add(container);
            container.setRect(old.getRect());
            if (flick != null)
               flick.addEventSource(container);
            container.setBackColor(container.getBackColor());
         }
      }
      if (Settings.keyboardFocusTraversable) // otherwise, in an app where there's only a TabbedContainer, the last added container would remain highlighted
         requestFocus();
   }

   /**
    * Sets the currently active tab. A PRESSED event will be posted to
    * the given tab if it is not the currently active tab; then, the containers will be switched.
    */
   public void setActiveTab(int tab)
   {
      if (tab != activeIndex && tab >= 0)
      {
         boolean firstTabChange = activeIndex == -1;
         if (!firstTabChange && flick == null) 
            remove(containers[activeIndex]);
         lastActiveTab = activeIndex; // guich@402_4
         activeIndex = tab;
         if (!Settings.fingerTouch)
            add(containers[activeIndex]);
         tabOrder.removeAllElements(); // don't let the cursor keys get into our container
         computeTabsRect();
         scrollTab(activeIndex);
         Window.needsPaint = true;
         if (!firstTabChange) // guich@200b4_87
            postPressedEvent();
      }
   }

   /** Returns the index of the selected tab */
   public int getActiveTab()
   {
      return activeIndex;
   }
   
   /** Returns the container of the active tab. 
    * @since TotalCross 1.2
    */
   public Container getActiveContainer() // guich@tc120_16
   {
      return containers[activeIndex];
   }

   /** Returns the caption height for this TabbedContainer. Note that it is not possible to compute the correct height of 
    * each container, since they will be added AFTER this TabbedContainer has their bounds set. So, you should actually use some
    * other way to specify the bounds, like FILL or FIT; using PREFERRED in the height of setRect will make your application abort. */
   public int getPreferredHeight()
   {
      return tabH /* guich@564_12: + 20 */ + insets.top+insets.bottom;
   }
   /** Returns the minimum width (based on the sizes of the captions) for this TabbedContainer */
   public int getPreferredWidth()
   {
      int sum = 0;
      if (count > 0)
      {
         // the max size is the size of the biggest bolded title plus the size of the plain titles
         int maxw = 0, maxi = 0;
         for (int i = count-1; i >= 0; i--)
         {
            int w = rSel[i].width;
            if (w > maxw)
            {
               maxi = i;
               maxw = w;
            }
            sum += rNotSel[i].width-1;
         }
         sum += maxw - rNotSel[maxi].width; // add the diff between the bold and the plain fonts of the biggest title
      }
      return sum+2+(uiCE?1:0) + insets.left+insets.right; // guich@573_11: changed from 3 to 2
   }

   /** Returns the index of the next/prev enabled tab, or the current tab if there's none. */
   private int nextEnabled(int from, boolean forward)
   {
      for (int i =0; i < count; i++)
      {
         boolean limitsReached = (forward && from == containers.length-1) || (!forward && from == 0);
         if (limitsReached)
            from = forward? 0 : count-1;
         else
            from = forward?from+1:from-1;
         if (!disabled[from])
            break;
      }
      return from < 0 ? 0 : from;
   }

   /** Used internally. resizes all the containers and add the arrows if scroll is needed. */
   protected void onBoundsChanged(boolean screenChanged)
   {
      int i;
      onFontChanged();
      computeTabsRect();
      int borderGap = style==Window.NO_BORDER ? 0 : 1; // guich@400_89
      int xx = insets.left+borderGap;
      int yy = (atTop?tabH:borderGap)+insets.top;
      int ww = width-insets.left-insets.right-(borderGap<<1);
      int hh = height-insets.top-insets.bottom-(borderGap<<1)-(atTop?yy:tabH);
      clientRect = new Rect(xx,yy,ww,hh);
      for (i = 0; i < count; i++)
      {
         Container c = containers[i];
         if (c.parent == null)
            add(c);
         c.setRect(xx,yy,ww,hh,null,screenChanged);
         c.reposition();
         System.out.println(i+": "+(xx-clientRect.x));
         xx += width;
      }
      if (Settings.fingerTouch)
      {
         flick.setScrollDistance(width);
         flick.setDistanceToAbortScroll(0);
      }
      if (activeIndex == -1) setActiveTab(nextEnabled(-1,true)); // fvincent@340_40
      addArrows();
   }

   private boolean mustScroll()
   {
      return count > 1 && getPreferredWidth() > this.width; // guich@564_10: support scroll - guich@573_2: only add arrows if there's more than one tab
   }

   private void addArrows()
   {
      boolean scroll = mustScroll();
      if (scroll)
      {
         int c = parent != null ? parent.backColor : UIColors.controlsBack; // guich@573_4
         if (btnLeft == null)
         {
            int hh = Math.max(fmH/2,tabH/4); // guich@tc110_90: use tab height if its larger than font's height
            btnRight = new ArrowButton(Graphics.ARROW_RIGHT, hh, arrowsColor);
            btnRight.setBackColor(c);
            btnRight.setBorder(Button.BORDER_NONE);
            btnLeft  = new ArrowButton(Graphics.ARROW_LEFT,  hh, arrowsColor);
            btnLeft.setBackColor(c);
            btnLeft.setBorder(Button.BORDER_NONE);
            int yy = (tabH+btnRight.getPreferredHeight()) >> 1;
            super.add(btnRight,RIGHT,atTop ? (tabH-yy) : (this.height-yy));
            super.add(btnLeft,BEFORE-2,SAME);
            btnLeft.setEnabled(false);
            btnLeft.setFocusLess(true); // guich@570_39
            btnRight.setFocusLess(true); // guich@570_39
            btnRight.autoRepeat = btnLeft.autoRepeat = true; // guich@tc122_46
            btnRight.AUTO_DELAY = btnLeft.AUTO_DELAY = 500;
         }
         btnX = btnLeft.x-(uiCE?3:2);
      }
      else btnX = this.width;
      if (btnLeft != null)
      {
         btnRight.setVisible(scroll);
         btnLeft.setVisible(scroll);
      }
   }

   public void setEnabled(boolean b)
   {
      super.setEnabled(b);
      if (btnLeft != null)
      {
         boolean canGoLeft = activeIndex > 0;
         boolean canGoRight = activeIndex < count-1;
         btnLeft.setEnabled(enabled && canGoLeft);
         btnRight.setEnabled(enabled && canGoRight);
      }
   }

   /** compute the rects that represents each tab on the screen. */
   private void computeTabsRect()
   {
      int x0 = 1;
      int y0 = atTop?0:(height-tabH);
      int n = count;
      for (int i =0; i < n; i++)
      {
         Rect r = rects[i];
         Rect r0 = i == activeIndex ? rSel[i] : rNotSel[i];
         r.x = x0;
         r.y = r0.y + y0;
         r.width = r0.width;
         r.height = r0.height;
         x0 += r.width-1;
         rects[i] = r;
      }
   }

   /** Scroll the TabbedContainer to the given tab */
   private void scrollTab(int toIdx) // guich@564_10
   {
      if (btnLeft != null && mustScroll())
      {
         boolean canGoLeft = toIdx > 0;
         boolean canGoRight = toIdx < count-1;
         btnLeft.setEnabled(canGoLeft);
         btnRight.setEnabled(canGoRight);
         if (canGoLeft || canGoRight)
         {
            int xOfs;
            if (toIdx == 0)
               xOfs = 0;
            else
            {
               xOfs = 7*fmH/11; // keep part of the previous tab on screen
               for (int i =0; i < toIdx; i++)
                  xOfs -= rNotSel[i].width-1;
            }
            offsetRects(xOfs);
            // make sure that the last tab is near the left button
            if (rects[count-1].x2() < btnX || toIdx == count-1)
            {
               int dif = btnX - rects[count-1].x2();
               offsetRects(-xOfs);
               xOfs += dif;
               offsetRects(xOfs);
            }
            Window.needsPaint = true;
         }
      }
   }

   /** Offsets all rectangles by the given value */
   private void offsetRects(int xOfs)
   {
      // offset the rectangles
      for (int i = count-1; i >= 0; i--)
         rects[i].x += xOfs;
   }

   /** Compute the rectangles of the tabs based on the selected
    * (bolded) and unselected (plain) titles. */
   protected void onFontChanged() // guich@564_11
   {
      boolean isText = isTextCaption;
      int wplain,wbold;
      tabH = isText ? (fmH + 4) : (imgCaptions[0].getHeight() + 4);
      int y0 = atTop?2:0;
      bold = font.asBold();
      FontMetrics fmb = bold.fm;
      rSel = new Rect[count];
      rNotSel = new Rect[count];
      for (int i =count-1; i >= 0; i--)
      {
         wplain =  isText ? fm.stringWidth(strCaptions[i]) : imgCaptions[i].getWidth();
         wbold  =  isText ?fmb.stringWidth(strCaptions[i]) : wplain;
         rSel[i]    = new Rect(0,0,wbold+5,tabH);
         rNotSel[i] = new Rect(0,y0,wplain+4,tabH-2);
      }
   }

   protected void onColorsChanged(boolean colorsChanged)
   {
      if (colorsChanged)
         brightBack = Color.getAlpha(foreColor) > 128;
      fColor = (enabled || !brightBack) ? getForeColor()    : Color.darker(foreColor);
      cColor = (enabled || !brightBack) ? getCaptionColor() : Color.darker(captionColor);
      if (colorsChanged && btnLeft != null)
      {
         btnRight.arrowColor = btnLeft.arrowColor = arrowsColor;
         btnRight.backColor = btnLeft.backColor = parent != null ? parent.backColor : UIColors.controlsBack; // guich@573_4
      }
   }

   /** Called by the system to draw the tab bar. */
   public void onPaint(Graphics g)
   {
      Rect r;
      boolean isFlat = uiFlat;
      int n = count;
      int y = atTop?(tabH-1):0;
      int h = atTop?(height-y):(height-tabH+1);
      int yl = atTop?y:(y+h-1);
      // erase area with parent's color
      int containerColor = containers[activeIndex].backColor; // guich@580_26: use current container's backcolor instead of TabbedContainer's backcolor
      g.backColor = parent.backColor;
      if (!transparentBackground)
      {
         if (parent.backColor == containerColor) // same color? fill the whole rect
            g.fillRect(0,0,width,height);
         else
         {
            // otherwise, erase tab area...
            if (atTop)
               g.fillRect(0,0,width,y);
            else
               g.fillRect(0,yl,width,height-yl);
            // ...and erase containers area
            g.backColor = containerColor;
            g.fillRect(0,y,width,h);
         }
      }
      g.foreColor = fColor;
      if (style != Window.NO_BORDER)
         g.drawRect(0,y,width,h); // guich@200b4: now the border is optional
      else
         g.drawLine(0,yl,width,yl);

      int back = backColor;
      g.backColor = backColor;
      if (btnLeft != null && mustScroll()) // if we have scroll, don't let the title be drawn over the arrow buttons
         g.setClip(1,0,btnX+(uiCE?1:0),height);
      // draw the tabs
      if (!transparentBackground && (useOnTabTheContainerColor || parent.backColor != backColor || uiVista)) // guich@400_40: now we need to first fill, if needed, and at last draw the border, since the text will overlap the last pixel (bottom-right or top-right) - guich@tc100b4_10: uivista also needs this
         for (int i = 0; i < n; i++)
         {
            r = rects[i];
            g.backColor = back = useOnTabTheContainerColor && containers[i].backColor != -1 ? containers[i].backColor : backColor; // guich@580_7: use the container's color if containersColor was not set - guich@tc110_59: use default back color if container was not yet shown.

            if (isFlat) // the flat style has rect borders instead of hatched ones.
               g.fillRect(r.x,r.y,r.width,r.height);
            else
            if (uiVista && enabled)
               g.fillVistaRect(r.x+1,r.y+1,r.width-2,r.height-2, back, atTop,false);
            else
               g.fillHatchedRect(r.x,r.y,r.width,r.height,atTop,!atTop); // (*)
         }
      if (!transparentBackground && activeTabBackColor >= 0) // draw again for the selected tab if we want to use a different color
      {
         g.backColor = activeTabBackColor;
         r = rects[activeIndex];
         if (isFlat) // the flat style has rect borders instead of hatched ones.
            g.fillRect(r.x,r.y,r.width,r.height);
         else
            g.fillHatchedRect(r.x,r.y,r.width,r.height,atTop,!atTop); // (*)
         g.backColor = backColor;
      }
      // draw text
      boolean isText = isTextCaption;
      if (!isText && transpColor >= 0) // guich@564_13
      {
         g.drawOp = Graphics.DRAW_SPRITE;
         g.backColor = transpColor;
      }

      for (int i =0; i < n; i++)
      {
         r = rects[i];
         if (isText)
         {
            g.foreColor = disabled[i] ? Color.getCursorColor(cColor) : cColor; // guich@200b4_156
            if (i != activeIndex)
               g.drawText(strCaptions[i],r.x+3, r.y+1, textShadowColor != -1, textShadowColor);
            else
            {
               g.setFont(bold); // guich@564_11
               g.drawText(strCaptions[i],r.x+3, r.y+1, textShadowColor != -1, textShadowColor);
               g.setFont(font);
            }
            if (disabled[i])
               g.foreColor = Color.getCursorColor(cColor);
         }
         else
         {
            g.drawImage(disabled[i] ? imgDis[i] : imgCaptions[i], r.x+3, r.y+1);
         }
         if (isFlat)
            g.drawRect(r.x,r.y,r.width,r.height);
         else
         {
            g.drawHatchedRect(r.x,r.y,r.width,r.height,atTop,!atTop); // guich@400_40: moved from (*) to here
            if (uiCE && i+1 != activeIndex) // guich@100b4_9
            {
               int nextColor = useOnTabTheContainerColor ? containers[i].backColor : backColor;
               g.foreColor = Color.interpolate(cColor,nextColor);
               g.drawLine(r.x+r.width,r.y+(atTop?2:1),r.x+r.width,r.y+r.height+(atTop?-1:-3));
               g.foreColor = cColor;
            }
         }
      }
      if (!isText && transpColor >= 0)
      {
         g.backColor = backColor;
         g.drawOp = Graphics.DRAW_PAINT;
      }
      // guich@200b4: remove the underlaying line of the active tab.
      r = rects[activeIndex];
      g.foreColor = useOnTabTheContainerColor ? containers[activeIndex].backColor : backColor; // guich@580_7: use the container's back color
      g.drawLine(r.x,yl,r.x2(),yl);
      g.drawLine(r.x+1,yl,r.x2()-1,yl);

      if (Settings.keyboardFocusTraversable && focusMode == FOCUSMODE_CHANGING_TABS) // draw the focus around the current tab - guich@580_52: draw the cursor only when changing tabs
      {
         g.drawDottedCursor(r.x+1,r.y+1,r.width-2,r.height-2);
         if (Settings.screenWidth == 320)
            g.drawDottedCursor(r.x+2,r.y+2,r.width-4,r.height-4);
      }
   }

   /** Sets the text color of the captions */
   public void setCaptionColor(int capColor)
   {
      this.captionColor = this.arrowsColor = capColor;
      onColorsChanged(true); // guich@200b4_169
   }
   /** Gets the text color of the captions. return a grayed value if this control is not enabled. */
   public int getCaptionColor()
   {
      return enabled?captionColor:Color.brighter(captionColor);
   }
   /** Returns the area excluding the tabs and borders for this TabbedContainer.
     * Note: do not change the returning rect object ! */
   public Rect getClientRect() // guich@340_27
   {
      return clientRect;
   }

   /** Returns the area excluding the tabs and borders for this TabbedContainer.
    * In this version, you provide the created Rect to be filled with the coords.*/
   protected void getClientRect(Rect r) // guich@450_36
   {
      r.set(clientRect);
   }

   /** Called by the system to pass events to the tab bar control. */
   public void onEvent(Event event)
   {
      if (event.target != this)
      {
         if (event.type == ControlEvent.PRESSED && (event.target == btnLeft || event.target == btnRight))
            setActiveTab(nextEnabled(activeIndex,event.target == btnRight));
         if (!(Settings.fingerTouch && (event.type == PenEvent.PEN_DRAG || event.type == PenEvent.PEN_UP))) 
            return;
      }

      switch (event.type)
      {
         case PenEvent.PEN_UP:
            if (!flickTimerStarted)
               flickEnded();
            isScrolling = false;
            break;
         case PenEvent.PEN_DRAG:
            if (Settings.fingerTouch)
            {
               DragEvent de = (DragEvent)event;
               if (isScrolling)
               {
                  scrollContent(-de.xDelta, 0);
                  event.consumed = true;
               }
               else
               {
                  int direction = DragEvent.getInverseDirection(de.direction);
                  if (canScrollContent(direction, de.target) && scrollContent(-de.xDelta, 0))
                  {
                     flickTimerStarted = false;
                     event.consumed = isScrolling = true;
                  }
               }
            }
            break;
         case ControlEvent.FOCUS_IN: // guich@580_53: when focus is set, activate tab changing mode.
            if (Settings.keyboardFocusTraversable)
               focusMode = FOCUSMODE_CHANGING_TABS;
            break;
         case PenEvent.PEN_DOWN:
            PenEvent pe = (PenEvent)event;
            if (pe.x < btnX && (Settings.fingerTouch || (rects[0].y <= pe.y && pe.y <= rects[0].y2()))) // guich@tc100b4_7 - guich@tc120_48: when fingerTouch, the y position may be below the tabbed container
            {
               int sel = -1;
               if (Settings.fingerTouch) // guich@tc120_48
               {
                  int minDist = Settings.touchTolerance;
                  for (int i = count-1; i >= 0; i--)
                  {
                     Rect r = rects[i];
                     int d = (int)(Convert.getDistancePoint2Rect(pe.x,pe.y, r.x,r.y,r.x+r.width,r.y+r.height)+0.5);
                     if (d < minDist)
                     {
                        minDist = d;
                        sel = i;
                     }
                  }
               }
               else
               {
                  for (int i = count-1; i >= 0; i--)
                     if (rects[i].contains(pe.x,pe.y))
                     {
                        sel = i;
                        break;
                     }
               }
               if (sel != activeIndex && sel >= 0 && !disabled[sel])
               {
                  if (beepOn && !Settings.onJavaSE) Sound.beep(); // guich@300_7
                  setActiveTab(sel);
               }
            }
            break;
         case KeyEvent.ACTION_KEY_PRESS:
            focusMode = FOCUSMODE_CHANGING_TABS;
            // guich@573_23 - super.drawHighlight(); // remove the highlight around the TabbedContainer
            Window.needsPaint = true; // guich@573_23
            break;
         case KeyEvent.SPECIAL_KEY_PRESS:
            if (Settings.keyboardFocusTraversable)
            {
               KeyEvent ke = (KeyEvent)event;
               int key = ke.key;
               if (focusMode == FOCUSMODE_CHANGING_TABS)
               {
                  if (key == SpecialKeys.LEFT || key == SpecialKeys.RIGHT)
                     setActiveTab(nextEnabled(activeIndex, key == SpecialKeys.RIGHT));
                  else
                  if (ke.isUpKey() || ke.isDownKey())
                  {
                     focusMode = FOCUSMODE_INSIDE_CONTAINERS;
                     Window.needsPaint = true; // guich@573_23 - drawHighlight();
                     containers[activeIndex].changeHighlighted(containers[activeIndex],ke.isDownKey());
                     isHighlighting = true;
                  }
               }
               if (ke.isActionKey())
               {
                  focusMode = FOCUSMODE_OUTSIDE;
                  //getParent().requestFocus(); - guich@580_54
                  isHighlighting = true;
                  Window.needsPaint = true; // guich@573_23 - drawHighlight();
               }
            }
            break;
      }
   }

   /** Tranfer the focus between the containers on this TabbedContainer */
   public void changeHighlighted(Container p, boolean forward)
   {
      Window w = getParentWindow();
      if (w != null)
         switch (focusMode)
         {
            case FOCUSMODE_OUTSIDE: // focus just got here
               if (w.getHighlighted() != this)
                  w.setHighlighted(this);
               else
                  super.changeHighlighted(p,forward);
               break;
            case FOCUSMODE_INSIDE_CONTAINERS: // was changing a control and the limits has been reached
               focusMode = FOCUSMODE_CHANGING_TABS;
               w.setHighlighted(this); // remove the focus from the last control
               Window.needsPaint = true; // guich@573_23 - drawHighlight();
               requestFocus();
               isHighlighting = false;
               break;
            default:
               super.changeHighlighted(p,forward);
         }
   }

   /** Only return to highlighting when we want */
   public void setHighlighting()
   {
      isHighlighting = false;
   }

   public void reposition()
   {
      super.reposition();
      computeTabsRect();
      addArrows(); // this is needed because the btnX was not yet repositioned when onBounds called addArrows.
      if (mustScroll())
         scrollTab(activeIndex);
   }

   public void getFocusableControls(Vector v)
   {
      if (visible && enabled) v.addElement(this);
      super.getFocusableControls(v);
   }

   public Control handleGeographicalFocusChangeKeys(KeyEvent ke)
   {
      if (MainWindow.mainWindowInstance._focus ==  this)
      {
         if ((atTop && ke.isUpKey()) || (!atTop && ke.isDownKey()))
            return null;

          if ((atTop && ke.isDownKey()) || (!atTop && ke.isUpKey()))
          {
              Control c = containers[activeIndex].children;
              while (c != null && !c.focusTraversable)
                  c = c.next;
              return c;
          }
          if ((ke.isNextKey() && activeIndex == containers.length-1) || (ke.isPrevKey() && activeIndex == 0))
             return null;
          ke.target = this;
          _onEvent(ke);
          return this;
      }

      int direction = 0;
      if (ke.isUpKey()) direction = SpecialKeys.UP;             // this order must
      else if (ke.isDownKey()) direction = SpecialKeys.DOWN;    // be preserved
      else if (ke.isNextKey()) direction = SpecialKeys.RIGHT;
      else if (ke.isPrevKey()) direction = SpecialKeys.LEFT;
      else return null;

      Control c = findNextFocusControl(MainWindow.mainWindowInstance._focus, direction);
      if (c == null)
      {
         boolean prev = direction == SpecialKeys.UP || direction == SpecialKeys.LEFT;
         c = (prev == atTop) ? this : MainWindow.mainWindowInstance.findNextFocusControl(this, direction);
      }
      return c;
   }

   /** Returns true of the type is set to TABS_TOP. */
   public boolean isAtTop()
   {
      return atTop;
   }

   /** Resizes the height of each added container and sets the height of this TabbedContainer to the maximum height of the containers. */
   public void resizeHeight() // guich@tc120_12
   {
      int h = 0;
      for (int i=0; i < containers.length; i++)
      {
         containers[i].resizeHeight();
         h = Math.max(h, containers[i].getHeight());
      }
      setRect(KEEP,KEEP,KEEP,getPreferredHeight() + h + 3);
   }

   public boolean flickStarted()
   {
      flickTimerStarted = true;
      return isScrolling;
   }
   
   public void flickEnded()
   {
      for (int i = 0; i < containers.length; i++)
      {
         System.out.println(i+" "+(containers[i].x - clientRect.x));
         if (containers[i].x == clientRect.x)
         {
            setActiveTab(i);
            break;
         }
      }
      System.out.println("============ ");
   }
   
   public boolean canScrollContent(int direction, Object target)
   {
      return (direction == DragEvent.LEFT && activeIndex > 0) ||
             (direction == DragEvent.RIGHT && activeIndex < containers.length-1);
   }

   public boolean scrollContent(int xDelta, int yDelta)
   {      
      if (containers.length == 1)
         return false;
      for (int i = containers.length; --i >= 0;)
         containers[i].x -= xDelta;
      Window.needsPaint = true;
      return true;
   }
   
   public int getScrollPosition(int direction)
   {
      return containers[0].getX() - clientRect.x;
   }
   
   public Flick getFlick()
   {
      return flick;
   }
}
