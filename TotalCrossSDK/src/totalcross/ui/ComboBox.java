/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2001 Daniel Tauchke                                            *
 *  Copyright (C) 2001-2012 SuperWaba Ltda.                                      *
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
import totalcross.util.*;

/**
 * ComboBox is an implementation of a ComboBox, with the drop down window implemented by the ComboBoxDropDown class.
 * <p>
 * Note: the color used in the setBackground method will be used in the button
 * only. The background color of the control will be a lighter version of the
 * given color.
 */

public class ComboBox extends Container
{
   protected ComboBoxDropDown pop;
   ArrowButton        btn;
   private boolean            armed;
   private boolean            opened;
   private int                btnW;
   private int                bColor, fColor;
   private int                fourColors[]     = new int[4];
   private Image npback,nparmed;
   private int selOnPopup = -2;
   /** If set to true, the popup window will have the height of the screen */
   public boolean             fullHeight;
   /** If set to true, the popup window will have the width of the screen */
   public boolean             fullWidth;                    // guich@550_20

   /** The default value that is set to the clearValueInt of all ComboBox.
    * Usually this value is 0, but sometimes you may wish set it to -1, to unselect the ComboBox when clear is called.
    */
   public static int defaultClearValueInt;
   /** The check color used to fill the radio button used in Android. Defaults to the fore color.
    * @since TotalCross 1.3 
    */
   public int checkColor = -1;

   /** The title of the PopupMenu when in Android user interface style. 
    * @since TotalCross 1.3
    */
   public String popupTitle;
   
   /** Set to false to don't use the PopupMenu when the user interface style is Android.
    * This affects all ComboBoxes. If you want to change a particular ComboBox to use the standard
    * popup list, but keep others with the PopupMenu, you can do something like:
    * <pre>
    * public class MyListBox extends ListBox
    * {
    *    public MyListBox(String[] items)
    *    {
    *       super(items);
    *    }
    * }
    * ...
    * ComboBox cb = new ComboBox(new MyListBox(items));
    * </pre>
    * @since TotalCross 1.5
    */
   public static boolean usePopupMenu = true;
   
   /** Creates an empty ComboBox */
   public ComboBox()
   {
      this((Object[]) null);
   }

   /** Creates a ComboBox with the given items */
   public ComboBox(Object[] items)
   {
      this(new ListBox(items));
   }

   /**
    * Creates a ComboBox with a PopList containing the given ListBox. You can
    * extend the ListBox to draw the items by yourself and use this constructor
    * so the PopList will use your class and not the default ListBox one. 
    * This constructor forces the ListBox.simpleBorder to true. Note: the
    * listbox items must be already set.
    */
   public ComboBox(ListBox userListBox)
   {
      this(new ComboBoxDropDown(userListBox)); // guich@340_36
   }

   /** Constructs a ComboBox with the given PopList. */
   public ComboBox(ComboBoxDropDown userPopList) // guich@340_36
   {
      clearValueInt = defaultClearValueInt;
      ignoreOnAddAgain = ignoreOnRemove = true;
      pop = userPopList;
      btn = new ArrowButton(Graphics.ARROW_DOWN, getArrowWidth(), Color.BLACK);
      if (!uiCE)
         btn.setBorder(Button.BORDER_NONE);
      if (uiVista)
      {
         btn.flatBackground = false;
         btn.setBackColor(Color.darker(backColor,32));
      }
      btn.focusTraversable = false;
      if (uiAndroid) btn.transparentBackground = true;
      super.add(btn);
      started = true; // avoid calling the initUI method
      this.focusTraversable = true; // kmeehl@tc100
   }
   
   private int getArrowWidth()
   {
      return fmH * 3 / 11;
   }

   /** Does nothing */
   public void add(Control control)
   {
   }

   void add(Control control, boolean dummy)
   {
      super.add(control);
   }

   /** Does nothing */
   public void remove(Control control)
   {
   }

   /** Adds an array of Objects to the Listbox */
   public void add(Object[] items)
   {
      pop.lb.add(items);
      Window.needsPaint = true;
   }

   /** Adds an array of Objects to the Listbox */
   public void add(Object[] items, int startAt, int size)
   {
      pop.lb.add(items, startAt, size);
      Window.needsPaint = true;
   }

   /**
    * Adds an Object to the Listbox. This method is very slow if used in loop; use the
    * <code>add(Object[])</code> to add a bunch of objects instead.
    */
   public void add(Object item)
   {
      pop.lb.add(item);
      Window.needsPaint = true;
   }
   
   /** Adds the given text to this ListBox, breaking the text if it goes beyond the ListBox' limits, and also breaking if it contains \n.
    * Returns the number of lines. Note that each part of the text is considered a new item. This method is slower than the other <code>add</code> methods.
    * @since TotalCross 1.24
    */
   public int addWrapping(String text) // guich@tc124_21
   {
      Window.needsPaint = true;
      return pop.lb.addWrapping(text);
   }

   /** Adds an Object to the Listbox at the given index */
   public void insert(Object item, int index)
   {
      pop.lb.insert(item, index);
      Window.needsPaint = true;
   }

   /** Empties the ListBox */
   public void removeAll() // guich@210_13
   {
      pop.lb.removeAll();
      Window.needsPaint = true;
   }

   /** Removes an Object from the Listbox */
   public void remove(Object item)
   {
      pop.lb.remove(item);
      Window.needsPaint = true;
   }

   /** Removes an Object from the Listbox at the given index. */
   public void remove(int itemIndex)
   {
      pop.lb.remove(itemIndex);
      Window.needsPaint = true;
   }

   /** Sets the Object at the given Index, starting from 0 */
   public void setItemAt(int i, Object s)
   {
      pop.lb.setItemAt(i, s);
      Window.needsPaint = true;
   }

   /** Get the Object at the given Index */
   public Object getItemAt(int i)
   {
      return pop.lb.getItemAt(i);
   }

   /** Returns the selected item of the ListBox */
   public Object getSelectedItem()
   {
      return pop.lb.getSelectedItem();
   }

   /** Returns the position of the selected item of the ListBox */
   public int getSelectedIndex()
   {
      return pop.lb.selectedIndex;
   }

   /** Returns all items in this ComboBox */
   public Object[] getItems()
   {
      return pop.lb.getItems();
   }

   /** Returns the index of the item specified by the name */
   public int indexOf(Object name)
   {
      return pop.lb.indexOf(name);
   }

   /**
    * Sets the cursor color for this ComboBox. The default is equal to the
    * background slightly darker. 
    */
   public void setCursorColor(int color) // guich@210_19
   {
      pop.lb.setCursorColor(color);
   }

   /**
    * Selects an item. If the name is not found, the currently selected item is
    * not changed.
    *
    * @since SuperWaba 4.01
    */
   public void setSelectedItem(Object name) // guich@401_25
   {
      int idx = pop.lb.selectedIndex;
      pop.lb.setSelectedItem(name);
      if (Settings.sendPressEventOnChange && pop.lb.selectedIndex != idx)
         postPressedEvent();
      Window.needsPaint = true;
   }

   /**
    * Select the given index. Choose "-1" if you want to blank the ComboBox view
    * box.
    */
   public void setSelectedIndex(int i)
   {
      setSelectedIndex(i,Settings.sendPressEventOnChange);
   }
   
   /**
    * Select the given index, and optionally sends a PRESSED event. Choose "-1" if you want to blank the ComboBox view
    * box.
    */
   public void setSelectedIndex(int i, boolean sendPressEvent)
   {
      int idx = pop.lb.selectedIndex;
      pop.lb.setSelectedIndex(i);
      if (sendPressEvent && pop.lb.selectedIndex != idx)
         postPressedEvent();
      Window.needsPaint = true;
   }

   /** Returns the number of items */
   public int size()
   {
      return pop.lb.itemCount;
   }

   public int getPreferredWidth()
   {
      return pop.getPreferredWidth() + 1 + insets.left+insets.right + (Settings.fingerTouch ? btn.getPreferredWidth()+4 : 0);
   }

   public int getPreferredHeight()
   {
      return (pop.lb.itemCount > 0 && !isSupportedListBox() ? pop.lb.getItemHeight(0) : fmH) + Edit.prefH + insets.top+insets.bottom;
   }

   /** Passes the font to the pop list */
   protected void onFontChanged() // guich@200b4_153
   {
      if (pop != null)
         pop.setFont(font);
      // guich@tc100b3: resize the arrow based on the font.
      int newWH = getArrowWidth();
      if (btn.prefWH != newWH)
      {
         btn.prefWH = newWH;
         onBoundsChanged(false);
      }
   }
   
   /** Sets the ihtForeColors and ihtBackColors for the ListBox used with this ComboBox.
    * Note that null is a valid value, used when you always want to use the default color.
    * @since TotalCross 1.0 beta 4
    */
   public void setBackForeItemColors(IntHashtable ihtFore, IntHashtable ihtBack)
   {
      pop.lb.ihtForeColors = ihtFore;
      pop.lb.ihtBackColors = ihtBack;
   }

   protected void onBoundsChanged(boolean screenChanged)
   {
      btnW = btn.getPreferredWidth();
      switch (Settings.uiStyle)
      {
         case Settings.Android:
            btn.setRect(width - btnW - 3, 2, btnW, height-4,null,screenChanged);
            break;
         case Settings.PalmOS:
            btn.setRect(0, 0, btnW, height, null, screenChanged); // move button to left - guich@572_13: -1 - guich@tc100: removed -1
            break;
         case Settings.WinCE:
            btn.setRect(width - btnW - 2, 2, btnW, height - 4, null, screenChanged);
            break;
         default: // guich@573_6: both Flat and Vista use this
            btn.setRect(width - btnW - 3, 1, btnW + 2, height - 2, null, screenChanged);
            break;
      }
      if (screenChanged && pop.isVisible()) // guich@tc100b4_29: reposition the pop too if its visible
         updatePopRect();
      npback = nparmed = null;
   }

   public void onEvent(Event event)
   {
      PenEvent pe;
      boolean inside;
      if (opened && event.type != ControlEvent.WINDOW_CLOSED)
         return;
      if (enabled)
      switch (event.type)
      {
         case PenEvent.PEN_DRAG:
            pe = (PenEvent) event;
            inside = isInsideOrNear(pe.x, pe.y);
            if (event.target == this && inside != armed && pop.lb.itemCount > 0)
            {
               armed = inside;
               if (uiPalm || uiAndroid)
                  Window.needsPaint = true; // guich@580_25: just call repaint instead of drawing the cursor
               else
                  btn.press(armed);
            }
            break;
         case PenEvent.PEN_DOWN:
            if (event.target == this && !armed && pop.lb.itemCount > 0)
            {
               if (uiPalm || uiAndroid)
                  Window.needsPaint = true; // guich@580_25: just call repaint instead of drawing the cursor
               else
                  btn.press(true);
               armed = true;
            }
            break;
         case PenEvent.PEN_UP:
            pe = (PenEvent) event;
            if (event.target == this && (armed || isActionEvent(event)))
            {
               if (uiPalm || uiAndroid)
                  Window.needsPaint = true; // guich@580_25: just call repaint instead of drawing the cursor
               else
                  btn.press(false);
               armed = false;
               inside = isInsideOrNear(pe.x, pe.y); //  is a method of class Control that uses absolute coords
               if (inside && (!Settings.fingerTouch || !hadParentScrolled()))
               {
                  opened = true;
                  selOnPopup  =pop.lb.selectedIndex;
                  popup();
               }
            }
            break;
         case ControlEvent.WINDOW_CLOSED:
            if (event.target == pop) // an item was selected?
            {
               opened = false;
               boolean isMulti = pop.lb instanceof MultiListBox;
               if (pop.lb.selectedIndex >= 0 && ((!isMulti && (!Settings.sendPressEventOnChange || pop.lb.selectedIndex != selOnPopup)) || (isMulti && ((MultiListBox)pop.lb).changed)))
                  postPressedEvent();
               selOnPopup = -2;
            }
            break;
         case ControlEvent.PRESSED:
            if (event.target == btn && pop.lb.itemCount > 0)
            {
               btn.appId = this.appId; // guich@502_3: make the button have the same appid than us.
               event.consumed = true; // kevinsmotherman@450_22: avoid passing this to the other controls.
               popup();
            }
            break;
         case ControlEvent.FOCUS_IN:
         case ControlEvent.FOCUS_OUT:
            if (event.target == btn) // guich@240_11: change the target focus so parents can catch the focus_in and focus_out event
               event.target = this;
            break;
         case KeyEvent.ACTION_KEY_PRESS:
            btn.simulatePress();
            popup();
            break;
      }
   }

   protected void updatePopRect()
   {
      Rect r = getAbsoluteRect();
      pop.fullHeight = fullHeight; // guich@330_52
      pop.fullWidth = fullWidth;
      pop.setRect(r.x, r.y, width, height);
   }
   
   private boolean isSupportedListBox()
   {
      String cl = pop.lb.getClass().getName();
      return cl.equals("totalcross.ui.ListBox") || cl.equals("litebase.ui.DBListBox");
   }
   
   /** Pops up the ComboBoxDropDown */
   public void popup()
   {
      requestFocus(); // guich@240_6: avoid opening the combobox when its popped up and the user presses the arrow again - guich@tc115_36: moved from the event handler to here
      boolean isMultiListBox = pop.lb instanceof MultiListBox;
      if (uiAndroid && usePopupMenu && !isMultiListBox && isSupportedListBox()) // we don't support yet user-defined ListBox types yet
      {
         if (pop.lb.itemCount > 0)
            try
            {
               PopupMenu pm = new PopupMenu(popupTitle != null ? popupTitle : " ",pop.lb.getItemsArray(), isMultiListBox);
               pm.makeUnmovable();
               pm.enableSearch = true;
               pm.itemCount = pop.lb.size();
               pm.dataCol = pop.lb.dataCol;
               pm.checkColor = checkColor;
               pm.setBackForeColors(pop.lb.backColor,pop.lb.foreColor);
               pm.setCursorColor(pop.lb.back1);
               pm.setSelectedIndex(pop.lb.selectedIndex);
               if (pm.itemCount > 100) Flick.defaultLongestFlick = pm.itemCount > 1000 ? 9000 : 6000; 
               pm.popup();
               Event.clearQueue(PenEvent.PEN_UP); // prevent problem when user selects an item that is at the top of this ComboBox
               Flick.defaultLongestFlick = 2500;
               opened = false;
               int sel = pm.getSelectedIndex();
               if (sel != -1)
               {
                  pop.lb.selectedIndex = sel;
                  if (sel != -1)
                     postPressedEvent();
                  Window.needsPaint = true;
               }
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
      }
      else
      {
         updatePopRect();
         if (pop.lb.hideScrollBarIfNotNeeded()) // guich@tc115_77
            updatePopRect();
         // guich@320_17
         if (!isMultiListBox)
         {
            int sel = pop.lb.selectedIndex;
            pop.lb.selectedIndex = -2;
            pop.lb.setSelectedIndex(sel);
         }
         pop.popupNonBlocking();
         pop.lb.requestFocus();
         isHighlighting = false; // kmeehl@tc100: allow immediate keyboard navigation of the dropdown
      }  
   }
   
   /** Unpops the ComboBoxDropDown.
    * @since TotalCross 1.2
    */
   public void unpop() // guich@tc120_64
   {
      if (pop.isVisible())
         pop.unpop();
   }

   protected void onColorsChanged(boolean colorsChanged)
   {
      npback = nparmed = null;
      bColor = UIColors.sameColors ? backColor : Color.brighter(getBackColor()); // guich@572_15
      fColor = getForeColor();
      if (colorsChanged)
      {
         btn.setBackForeColors(uiVista ? Color.darker(bColor,32) : uiFlat ? bColor : backColor, foreColor);
         btn.arrowColor = fColor;
         pop.lb.setBackForeColors(backColor, foreColor);
      }
      if (!uiAndroid && !uiPalm) Graphics.compute3dColors(enabled, backColor, foreColor, fourColors);
   }

   /** paint the combo's border and the current selected item */
   public void onPaint(Graphics g)
   {
      // guich@200b4_126: repaint the background.
      if (!transparentBackground) // guich@tc115_18
         if (!uiAndroid && uiVista && enabled) // guich@573_6
            g.fillVistaRect(0, 0, width, height, bColor, false, false);
         else
         if (uiPalm && armed) // guich@580_25: fill the rect as inverted if Palm and armed
         {
            g.backColor = btn.pressColor; // use the same color of the button when pressed.
            g.fillRect(btnW, 0, width - btnW, height);
         }
         else
         {
            g.backColor = uiAndroid ? parent.backColor : bColor;
            g.fillRect(0, 0, width, height);
         }
      if (uiAndroid)
         try
         {
            if (npback == null)
               npback = NinePatch.getInstance().getNormalInstance(NinePatch.COMBOBOX, width, height, enabled ? bColor : Color.interpolate(bColor,parent.backColor), false,true);
            if ((armed || btn.armed) && nparmed == null)
               nparmed = npback.getTouchedUpInstance((byte)25,(byte)0);
            Image img = armed || btn.armed ? nparmed : npback;
            g.drawImage(img, 0,0);
            Graphics gg = img.getGraphics();
            g.fillShadedRect(width-btnW-5,1,1,height-3,true,false,gg.getPixel(width/2,1),gg.getPixel(width/2,height-3),30); // draw the line - TODO: fix if this is inside a ScrollContainer (see Button.onPaint)
            g.setClip(2,2,width-btnW-8,height-4);
         }
         catch (ImageException e) {e.printStackTrace();}
      else
      if (uiPalm) // guich@200b4_112
         g.setClip(btnW, 0, width - btnW - 1, height - 1);
      else
      {
         g.draw3dRect(0, 0, width, height, Graphics.R3D_CHECK, false, false, fourColors);
         g.setClip(2, 2, width - btnW - 3, height - 4);
      }
      if (pop.lb.itemCount > 0 && pop.lb.selectedIndex >= 0) // guich@402_31: avoid drawing invalid index
         drawSelectedItem(g);
   }

   protected void drawSelectedItem(Graphics g)
   {
      g.foreColor = (uiPalm && armed) ? bColor : fColor; // guich@580_25: select the inverted color if Palm and armed
      boolean trickW = pop.lb.width == 0; // guich@tc125_35
      if (trickW) pop.lb.width = width - btnW;
      int ih = pop.lb.itemCount > 0 ? pop.lb.getItemHeight(0) : fmH;
      boolean isString = pop.lb.itemCount > 0 && pop.lb.items.items[0] instanceof String;
      pop.lb.drawSelectedItem(g, pop.lb.selectedIndex, uiPalm ? btnW + 3 : 3, height == fmH+Edit.prefH ? Edit.prefH/2 : isString && uiAndroid ? (height-(fmH+Edit.prefH))/2 : (height - ih)/2); // guich@200b4: let the listbox draw the item
      if (trickW) pop.lb.width = 0;
   }

   /** Sorts the items of this combobox, and then unselects the current item. */
   public void qsort() // guich@220_35
   {
      pop.lb.qsort();
      setSelectedIndex(-1);
   }

   /** Sorts the elements of this ListBox. The current selection is cleared.
    * @param caseless Pass true to make a caseless sort, if the items are Strings. 
    */
   public void qsort(boolean caseless) // guich@tc113_5
   {
      pop.lb.qsort(caseless);
      setSelectedIndex(-1);
   }
   
   /**
    * Adds support for horizontal scroll on this listbox. Two buttons will
    * appear below the vertical scrollbar. The add, replace and remove
    * operations will be a bit slower because the string's width will have to be
    * computed in order to correctly set the maximum horizontal scroll.
    *
    * @since SuperWaba 5.6
    */
   public void enableHorizontalScroll() // guich@560_9
   {
      pop.lb.enableHorizontalScroll();
   }

   /**
    * Selects the last item added to this combobox, doing a scroll if needed.
    * Calls repaintNow.
    *
    * @since SuperWaba 5.6
    */
   public void selectLast()
   {
      pop.lb.selectLast();
      repaintNow();
   }

   /** Clears this control, selecting index clearValueInt (0 by default). */
   public void clear() // guich@572_19
   {
      setSelectedIndex(clearValueInt);
   }

   public void getFocusableControls(Vector v) // kmeehl@tc100
   {
      if (visible && enabled) v.addElement(this);
   }

   public Control handleGeographicalFocusChangeKeys(KeyEvent ke) // kmeehl@tc100
   {
      if (armed)
      {
         _onEvent(ke);
         return this;
      }
      return null;
   }
   
   /** Returns the ListBox used when this combobox is opened. */
   public ListBox getListBox()
   {
      return pop.lb;
   }
   
   /** Selects the item that starts with the given text
    * @param text The text string to search for
    * @param caseInsensitive If true, the text and all searched strings are first converted to lowercase.
    * @return If an item was found and selected.
    * @since TotalCross 1.13
    */
   public boolean setSelectedItemStartingWith(String text, boolean caseInsensitive) // guich@tc113_2
   {
      int idx = pop.lb.selectedIndex;
      boolean b = pop.lb.setSelectedItemStartingWith(text, caseInsensitive);
      if (Settings.sendPressEventOnChange && pop.lb.selectedIndex != idx)
         postPressedEvent();
      return b;
   }

   /** Replaces the original ComboBoxDropDown by the given one.
    * @since TotalCross 1.5
    */
   public void setPop(ComboBoxDropDown lb)
   {
      pop = lb;
      setSelectedIndex(-1);
   }
}
