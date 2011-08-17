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
import totalcross.ui.image.*;

/** Group or matrix of pushbuttons in a single control. Is one of the most versatiles
 * controls of TotalCross.
  * Here is an example of constructor:
  * <pre>
  * new PushButtonGroup(new String[]{"Button1","Button2","Button3"},false,-1,-1,4,0,false,PushButtonGroup.NORMAL)
  * </pre>
  * <p>The width of each button is calculated based on its caption size plus the insideGap, if you use PREFERRED
  * as the width; otherwise, it uses the size you specified (E.G.: FILL, FIT, etc).
  * The height is calculated based on the font's size or on the height you specified.
  */

public class PushButtonGroup extends Control
{
   /** Normal: only one selected at a time */
   public static final byte NORMAL = 0;
   /** The button will be selected and unselected immediately, acting like a real button */
   public static final byte BUTTON = 1;
   /** One click in the button will select it and another click will unselect it. However, only one button can be selected at a time */
   public static final byte CHECK = 2;
   /** Set to true to make the CHECK mode appear as raised by default (default is: button lowered when not selected). */
   public boolean checkAppearsRaised;
   public String []names; // guich@401_36: made protected
   private int widths[];
   private int selectedIndex = -1;
   private int gap;
   /** Space between the text and the button border. The ideal is 4. If allSameWidth is true, it is only used to compute the preferred width and may be overriden; otherwise, it is used as the internal gap. */
   public int insideGap;
   private int rows, cols;
   private boolean atLeastOne,actLikeButton,actLikeCheck;
   private int cellH,rowH;
   private int lastSel=-1;
   private int maxWidth=-1;
   private boolean simpleBorder = uiPalm || uiFlat; // guich@552_22: added uiFlat
   /** The bounds of each of the buttons. Never change this directly. */
   public Rect rects[];
   private int count;
   private boolean allSameWidth;
   private int[] tX;
   private int dColor,fColor;
   private int fourColors[] = new int[4];
   private int userCursorColor=-1;
   private int []btnFColors,btnBColors;
   private int nullNames;
   private Rect clip;
   
   /** The boolean array that defines which buttons are hidden. If you want to hide a button,
    * just access this and set an array index to true. Note that you must also explicitly call the repaint
    * function to update the control. Sample:
    * <pre>
    * pbg.hidden[5] = true; // hides button 5
    * repaint();
    * </pre>
    * @since SuperWaba 5.0
    */
   public boolean hidden[];
   
   /** Span across multiple columns and rows. These cells that will be overriden must be null and the allSameWidth must be true.
    * This sample:
    * <pre>
      String []numerics = {"1","2","3","4","5","6","7","clear",null,"0",null,null};
      PushButtonGroup pbg=new PushButtonGroup(numerics,false,-1,4,0,4,true,PushButtonGroup.BUTTON);
      pbg.colspan[7] = 2;
      pbg.rowspan[7] = 2;
      add(pbg, LEFT+50,AFTER+50,FILL-50,FILL-50);
    * </pre> 
    * ... will show this:
    * <pre>
    * 1   2   3
    * 
    * 
    * 4   5   6
    * 
    * 
    * 7   
    *     clear
    * 
    * 0
    * </pre>
    * @since TotalCross 1.3
    */
   public int[] colspan, rowspan;

   /** Create the button matrix.
       @param names captions of the buttons. You can specify some names as null so the button is not displayed. This is good if you're creating a button matrix and want to hide some buttons definetively (you can hide some buttons temporarily setting the hiden array). You can also use the <code>hidden</code> property to dynamically show/hide buttons.
       @param atLeastOne if true, at least one button must be selected
       @param selected default index to appear selected, or -1 if none
       @param gap space between the buttons, -1 glue them.
       @param insideGap Space between the text and the button border. The ideal is 4. If allSameWidth is true, it is only used to compute the preferred width and may be overriden; otherwise, it is used as the internal gap.
       @param rows if > 1, creates a button matrix
       @param allSameWidth if true, all the buttons will have the width of the most large one.
       @param type can be NORMAL, BUTTON or CHECK
   */
   public PushButtonGroup(String[] names, boolean atLeastOne, int selected, int gap, int insideGap, int rows, boolean allSameWidth, byte type)
   {
      this.names = names;
      this.insideGap = insideGap;
      this.atLeastOne = atLeastOne;
      this.actLikeButton = type == BUTTON;
      this.actLikeCheck = type == CHECK;
      this.allSameWidth = allSameWidth;
      this.selectedIndex = selected;
      this.gap = gap;
      count = names.length;
      widths = new int[count];
      rects = new Rect[count];
      tX = new int[count];
      if (rows < 1) rows = 1;
      this.rows = rows;
      cols = count / rows;
      if (cols*rows < count) // guich@340_28: needs more rows than given - guich@tc100: use cols*rows
         this.cols++;
      hidden = new boolean[count];
      onFontChanged();
      if (uiAndroid)
         clip = new Rect();
      colspan = new int[count];
      rowspan = new int[count];
   }

   /** Create the button matrix, with insideGap = 4, selected = -1, atLeastOne = false, allSameWidth = true and type = BUTTON.
      @param names captions of the buttons. You can specify some names as null so the button is not displayed. This is good if you're creating a button matrix and want to hide some buttons definetively (you can hide some buttons temporarily setting the hiden array). You can also use the <code>hidden</code> property to dynamically show/hide buttons.
      @param gap space between the buttons, -1 glue them.
      @param rows if > 1, creates a button matrix
   */
   public PushButtonGroup(String[] names, int gap, int rows)
   {
      this(names, false, -1, gap, 4, rows, true, BUTTON);
   }

   /** Create the button matrix, with selected = -1, atLeastOne = false, allSameWidth = false and type = BUTTON.
      @param names captions of the buttons. You can specify some names as null so the button is not displayed. This is good if you're creating a button matrix and want to hide some buttons definetively (you can hide some buttons temporarily setting the hiden array). You can also use the <code>hidden</code> property to dynamically show/hide buttons.
      @param gap space between the buttons, -1 glue them.
      @param insideGap Space between the text and the button border. The ideal is 4. If allSameWidth is true, it is only used to compute the preferred width and may be overriden; otherwise, it is used as the internal gap.
      @param rows if > 1, creates a button matrix
   */
   public PushButtonGroup(String[] names, int gap, int insideGap, int rows)
   {
      this(names, false, -1, gap, insideGap, rows, false, BUTTON);
   }

   /** Sets the names. Note that it must have the same number of elements passed in the constructor,
    * and that the bounds are NOT recomputed. Repaint is called.
    */
   public void setNames(String[] newNames) // guich@573_38
   {
      if (newNames.length == names.length)
      {
         names = newNames;
         onFontChanged(); // guich@tc100b5_29 - have to update widths[]
         onBoundsChanged(false); // guich@tc100b5_29 - and rects[]
         Window.needsPaint = true;
      }
   }

   /** Sets a button's index color. The other buttons will remain with the default color.
    * Pass -1 to restore the default color. */
   public void setColor(int index, int foreColor, int backColor) // guich@573_37
   {
      if (0 <= index && index < count)
      {
         if (foreColor != -1 && btnFColors == null) // only initialize the colors array after the first use
         {
            btnFColors = new int[count];
            Convert.fill(btnFColors, 0, count, -1);
         }
         if (backColor != -1 && btnBColors == null)
         {
            btnBColors = new int[count];
            Convert.fill(btnBColors, 0, count, -1);
         }
         if (btnFColors != null)
            btnFColors[index] = foreColor;
         if (btnBColors != null)
            btnBColors[index] = backColor;
      }
   }

   /** Uses a border with a single line (not 3d and not Android's) */
   public void setSimpleBorder(boolean simple)
   {
      this.simpleBorder = simple || uiPalm || uiFlat || uiVista; // guich@552_22: added uiFlat - // guich@573_6: added uiVista
   }
   /** Returns the index of the selected button, or -1 if none is selected. */
   public int getSelectedIndex()
   {
      return selectedIndex;
   }
   /** Returns the caption of the selected button, or <code>null</code> if no button is selected
     * @since SuperWaba 4.01
     */
   public String getSelectedItem() // guich@401_1
   {
      return selectedIndex == -1 ? null : names[selectedIndex];
   }
   /** Sets the cursor color for this PushButtonGroup. The default is equal to the background slightly darker. Make sure you tested it in 2,4 and 8bpp devices. */
   public void setCursorColor(int color) // guich@210_19
   {
      this.userCursorColor = color;
      onColorsChanged(true); // jrissoto@220_26
   }

   public int getPreferredWidth()
   {
      int i,w = 0;
      int wc = 0;
      if (count == rows) // only one column?
      {
         for (i =count-1; i >= 0; i--)
            w = Math.max(w,widths[i]);
      }
      else
      {
         int n = count;
         for (i =0; i < n; i++) // compute the maximum size of each row
         {
            wc += ((maxWidth == -1)?widths[i]:maxWidth) +gap;
            if (i != 0 && ((i+1)%cols == 0))
            {
               wc -= gap;
               w = (wc > w)?wc:w;
               wc = 0;
            }
         }
         w = (wc > w)?wc:w;
      }
      return w;
   }

   public int getPreferredHeight()
   {
      return (fmH+(simpleBorder?0:2)+gap)*rows - gap; // remove the last gap
   }

   protected void onFontChanged()
   {
      int i;
      // computes the best width for all controls
      int avg = 0;
      nullNames = 0;
      maxWidth=-1;
      for (i =count-1; i >= 0; i--)
      {
         if (names[i] == null)
            nullNames++;
         else
            avg += (widths[i] = fm.stringWidth(names[i])+insideGap);
         if (allSameWidth) maxWidth = Math.max(maxWidth,widths[i]);
      }
      // guich@200b4: search again for possible null names and set its width as the average width
      if (nullNames > 0)
      {
         avg /= count-nullNames;
         for (i =count-1; i >= 0; i--)
            if (names[i] == null)
               widths[i] = avg;
      }
   }

   protected void onColorsChanged(boolean colorsChanged)
   {
      if (colorsChanged) // backColor has changed
         dColor = userCursorColor >= 0 ? userCursorColor : Color.getCursorColor(backColor);
      fColor = getForeColor();
      Graphics.compute3dColors(enabled,backColor,foreColor,fourColors);
   }

   protected void onBoundsChanged(boolean screenChanged)
   {
      rowH = (height+gap) / rows; // calculate through the given height
      cellH = rowH-gap; // guich@200b4: corrected if more than 1 row
      int desiredW=0, extraGaps=0,g; // guich@580_6
      if (allSameWidth)
      {
         g = gap > 0 ? gap : 0;
         desiredW = (width-gap*(cols-1)) / cols;
         extraGaps = width - (desiredW * cols + g * (cols-1));
      }

      // compute the rects
      int i =0;
      int c = cols;
      int x = 0;
      int y = 0;
      while (true)
      {
         int w = !allSameWidth ? widths[i] : (desiredW + (c <= extraGaps ? 1 : 0));
         int h = cellH;
         int span = colspan[i]-1;
         if (span > 0)
            w += span * (w+gap);
         span = rowspan[i]-1;
         if (span > 0)
            h += span*rowH;
         
         if (names[i] != null)
            rects[i] = new Rect(x,y,w,h);
         tX[i] = x+((w-widths[i]+insideGap) >> 1);
         if (++i >= count) break;
         if (--c == 0)
         {
            x = 0;
            y += rowH;
            c = cols;
         }
         else x += w + gap;
      }
   }

   public void onPaint(Graphics g)
   {
      onPaint(g, selectedIndex);
   }
   
   private Image getAndroidButton(int w, int h, int color, boolean selected) throws ImageException
   {
      Image img = NinePatch.getNormalInstance(NinePatch.BUTTON,w,h,color,true);
      if (selected)
         img = NinePatch.getPressedInstance(img, color, -1, true);
      return img;
   }

   private void onPaint(Graphics g, int sel) // guich@573_7: let the selected item be passed as parameter
   {
      int i;
      int n = count;
      Rect r;
      boolean uiAndroid = Control.uiAndroid && !simpleBorder;

      g.foreColor = fColor;
      boolean drawEachBack = nullNames > 0 || (btnBColors != null || uiCE || uiAndroid || (uiVista && enabled)) || (gap > 0 && parent != null && backColor != parent.backColor); // guich@230_34 - guich@tc110_16: consider nullNames
      if (!drawEachBack || uiAndroid)
      {
         if (!uiAndroid)
            g.backColor = backColor;
         else
         {
            g.getClip(clip);
            g.backColor = g.getPixel(clip.x,clip.y); // use color painted by the parent
         }
         g.fillRect(0,0,width,height);
      }
      g.backColor = backColor;
      for (i=0; i < n; i++)
         if ((r = rects[i]) != null && !hidden[i])
         {
            if (drawEachBack && !transparentBackground) // guich@tc120_36
               try
               {
                  int back;
                  // selects the background color
                  if (i == sel && userCursorColor >= 0)
                     back = userCursorColor;
                  else
                  if (btnBColors != null && btnBColors[i] >= 0) // guich@573_37
                     back = btnBColors[i];
                  else
                     back = backColor;
   
                  if (uiAndroid)
                  {
                     g.drawImage(getAndroidButton(r.width,r.height,enabled ? back : Color.interpolate(back,parent.backColor), i == sel), r.x,r.y);
                     continue;
                  }
                  else
                  if (uiVista)
                     g.fillVistaRect(r.x,r.y,r.width,r.height, back, i == sel,false);
                  else
                  {
                     g.backColor = back;
                     g.fillRect(r.x,r.y,r.width,r.height);
                     g.backColor = backColor;
                  }
               }
               catch (Exception e) {if (Settings.onJavaSE) e.printStackTrace();}
            if (simpleBorder)
               g.drawRect(r.x,r.y,r.width,r.height);
            else
               g.draw3dRect(r.x,r.y,r.width,r.height,actLikeCheck && !checkAppearsRaised?((uiCE || uiVista) && i==sel)?Graphics.R3D_RAISED:Graphics.R3D_CHECK:((uiCE || uiVista) && i==sel)?Graphics.R3D_LOWERED:Graphics.R3D_RAISED,false,false,fourColors);
         }
      g.foreColor = fColor;
      for (i=0; i < n; i++)
         if ((r = rects[i]) != null && !hidden[i])
         {
            int ty = (r.height-fmH) / 2; // nopt
            boolean useCustomColor = btnFColors != null && btnFColors[i] >= 0; // guich@573_37
            g.setClip(r.x+1,r.y+1,r.width-2,r.height-2);
            if (useCustomColor) g.foreColor = btnFColors[i];
            if (uiPalm || uiFlat || i != sel)
               g.drawText(names[i], tX[i], r.y+ty, textShadowColor != -1, textShadowColor); // tX[i]: if allSameWidth, center the label in the button
            else
            {
               int shift = uiAndroid ? 0 : (uiCE&&actLikeCheck && !checkAppearsRaised?-1:1);
               g.drawText(names[i], tX[i]+shift, r.y+ty+shift, textShadowColor != -1, textShadowColor);
            }
            if (useCustomColor) g.foreColor = fColor;
         }
      g.clearClip();
      if ((uiFlat || uiPalm) && selectedIndex != -1 && (r = rects[selectedIndex]) != null && !hidden[selectedIndex])
      {
         int k = simpleBorder?1:2;
         g.eraseRect(r.x+k,r.y+k,r.width-k-k,r.height-k-k,backColor,dColor,foreColor);
      }
   }

   /** Sets the selected button index. Note. if there are any null or hidden buttons, you must
    * consider them too to compute the correct index. */
   public void setSelectedIndex(int ind)
   {
      int min = atLeastOne?0:-1;
      if ((actLikeCheck || selectedIndex != ind) && min <= ind && ind < count)
      {
         selectedIndex = ind;
         lastSel = -100; // guich@510_24: reset this.
         repaintNow();
      }
   }

   private int findButtonAt(int px, int py)
   {
      boolean inside = 0 <= px && px < width && 0 <= py && py < height;
      if (Settings.fingerTouch || inside)
      {
         Rect r;
         if (inside)
         {
            int s = cols*(py / rowH);
            int e = Math.min(count,s+cols);
            for (int i =s; i < e; i++)
               if ((r=rects[i]) != null && !hidden[i] && r.contains(px,py))
                  return i;
         }
         else // guich@tc120_48
         {
            int minDist = Settings.touchTolerance;
            int sel = -1;
            for (int i =0; i < rects.length; i++)
               if ((r=rects[i]) != null && !hidden[i])
               {
                  int d = (int)(Convert.getDistancePoint2Rect(px,py, r.x,r.y,r.x+r.width,r.y+r.height)+0.5);
                  if (d < minDist)
                  {
                     minDist = d;
                     sel = i;
                  }
               }
            return sel;
         }
      }
      return -1;
   }

   public void onEvent(Event event)
   {
      int sel = 0;
      if (event instanceof PenEvent)
         sel = findButtonAt(((PenEvent)event).x,((PenEvent)event).y);

      switch (event.type)
      {
         case PenEvent.PEN_DRAG:
         case PenEvent.PEN_DOWN:
            if (sel != selectedIndex && (!atLeastOne || sel != -1))
               setSelectedIndex(sel);
            break;
         case KeyEvent.KEY_PRESS:
         case KeyEvent.SPECIAL_KEY_PRESS:
            KeyEvent ke = (KeyEvent)event;

            if (ke.key == '0' && !atLeastOne) // guich@573_47
               setSelectedIndex(-1,true);
            else
            if ('1' <= ke.key && ke.key <= '9' && (ke.key-'1') < count) // guich@573_47
               setSelectedIndex(ke.key - '1', true);
            else
            if (Settings.keyboardFocusTraversable && (ke.isPrevKey() || ke.isNextKey()))
            {
               int newIndex=selectedIndex;
               int max = count; // guich@tc123_1: limit the loop to the number of items in the PBG
               if (newIndex == -1)
                  ke.key = SpecialKeys.RIGHT;
               if (ke.isUpKey()) // guich@550_15: added support for navigate using all arrows
               {
                  do
                  {
                     newIndex -= cols;
                     if (newIndex < 0)
                        newIndex = count+newIndex;
                  } while (max-- > 0 && (names[newIndex] == null || names[newIndex].length() == 0)); // guich@573_40: also ignores (here and below) if name == ""
               }
               else
               if (ke.isDownKey())
               {
                  do
                  {
                     newIndex += cols;
                     if (newIndex >= count)
                        newIndex %= count;
                  } while (max-- > 0 && (names[newIndex] == null || names[newIndex].length() == 0));
               }
               else
               if (ke.key == SpecialKeys.RIGHT)
               {
                  do
                  {
                     newIndex = (newIndex+1) % count;
                  } while (max-- > 0 && (names[newIndex] == null || names[newIndex].length() == 0));
               }
               else
               if (ke.key == SpecialKeys.LEFT)
               {
                  do
                  {
                     newIndex = (newIndex==0) ? count-1 : newIndex-1;
                  } while (max-- > 0 && (names[newIndex] == null || names[newIndex].length() == 0));
               }
               if (newIndex != selectedIndex && max != 0)
                  setSelectedIndex(newIndex);
            }
            if (!ke.isActionKey())
               break;
         case PenEvent.PEN_UP:
            setSelectedIndex(sel,false);
            break;
         case KeyEvent.ACTION_KEY_PRESS:
             if (Settings.geographicalFocus)
             {
                int selected = selectedIndex;
                setSelectedIndex(selectedIndex, false);
                setSelectedIndex(selected);
             }
             break;
      }
   }

   private void setSelectedIndex(int sel, boolean selectIt)
   {
      if (selectIt)
         setSelectedIndex(sel);
      if (!atLeastOne || sel != -1)
         postPressedEvent();
      if (actLikeCheck)
      {
         if (lastSel != selectedIndex)
            lastSel = selectedIndex;
         else
         {
            lastSel = -1;
            setSelectedIndex(-1);
         }
      } else
      if ((actLikeButton || (sel == -1 && !atLeastOne)))
      {
         Vm.safeSleep(150);
         setSelectedIndex(-1);
      }
   }
   /** Clears this control, selecting index clearValueInt. Note that if atLeastOne
    * is true, setting clearValueInt to -1 will have no effect. */
   public void clear() // guich@572_19
   {
      setSelectedIndex(clearValueInt);
   }

   public Control handleGeographicalFocusChangeKeys(KeyEvent ke)
   {
       if ((ke.isUpKey() && selectedIndex-cols < 0) ||
           (ke.isDownKey() && selectedIndex+cols >= count) ||
           (ke.isNextKey() && !ke.isDownKey() && selectedIndex != -1 && (selectedIndex+1) % cols == 0) ||
           (ke.isPrevKey() && !ke.isUpKey() && selectedIndex % cols == 0))
          return null;

       _onEvent(ke);
       return this;
   }
}
