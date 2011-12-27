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
 * ListBox is a complete implementation of a Listbox.
 * You can use the up/down keys to scroll and enter the first
 * letter of an item to select it.
 * <p>
 * Note: the color used in the setBackground method will be used in the scrollbar
 * only. The background color of the control will be a lighter version of the
 * given color.
 * <p>
 * Here is an example showing how it can be used:
 *
 * <pre>
 * import totalcross.ui.*;
 *
 * public class MyProgram extends MainWindow
 * {
 * ListBox lb;
 *
 * public void initUI()
 * {
 *   lb = new ListBox();
 *   add(lb);
 *   lb.add(new String[]{"Daniel","Jenny","Helge","Sandra"});
 *   lb.add("Marc");
 *   // you may set the rect by using PREFERRED only after the items were added.
 *   lb.setRect(LEFT,TOP,PREFERRED,PREFERRED); // use control's preferred width based on the size of the elements
 * }
 *
 * public void onEvent(Event event)
 * {
 *    switch (event.type)
 *    {
 *       case ControlEvent.PRESSED:
 *          if (event.target == lb)
 *             Object element = lb.getSelectedItem(); // in most cases, this is just a String and may be casted to such
 *    }
 * }
 * }
 * </pre>
 * The first item has index 0.
 */

public class ListBox extends Container implements Scrollable
{
   protected Vector items = new Vector();
   protected int offset;
   protected int selectedIndex=-1;
   protected int itemCount;
   protected int visibleItems;
   protected int btnX,btnX0;
   protected ScrollBar sbar;
   protected boolean simpleBorder; // used by PopList
   protected int xOffset; // guich@500_16
   protected int back0,back1;
   private int fColor;
   private int fourColors[] = new int[4];
   protected int customCursorColor=-1;
   private IntVector ivWidths;
   private int xOffsetMin;
   private ArrowButton btnLeft, btnRight;
   private int dragDistanceY,dragDistanceX; // kmeehl@tc100
   private boolean isScrolling;
   private Image npback;
   private boolean scScrolled;

   /** When the ListBox has horizontal buttons and its height divided by the button height is greater
    * than this value (10), the horizontal button heights are increased.
    * @see #extraHorizScrollButtonHeight 
    * @see #enableHorizontalScroll()
    */
   public static int EXTRA_HEIGHT_FACTOR = 10;
   /** IntHashtable used to specify different background colors for some items. Example:
    * <pre>
    * list.ihtBackColor = new IntHashtable(10);
    * ihtBackColors.put(10,0xAABBCC); // will make line number 10 with back color 0xAABBCC.
    * </pre>
    * Specify a null value if you want to use the default back color (this also makes drawing faster).
    * Note that its up to you to update the hashtable if an item is inserted or removed.
    * @since TotalCross 1.0 beta 4 
    */
   public IntHashtable ihtBackColors;

   /** IntHashtable used to specify different foreground colors for some items. Example:
    * <pre>
    * list.ihtForeColor = new IntHashtable(10);
    * ihtForeColors.put(10,Color.RED); // will make line number 10 with fore color RED.
    * </pre>
    * Specify a null value if you want to use the default fore color (this also makes drawing faster).
    * Note that its up to you to update the hashtable if an item is inserted or removed.
    * @since TotalCross 1.0 beta 4 
    */
   public IntHashtable ihtForeColors;

   /** The extra height of the horizontal scroll buttons. Defaults 2 in 160x160 or a multiple of it 
    * in other resolutions.
    * @see #EXTRA_HEIGHT_FACTOR
    * @see #enableHorizontalScroll()
    */
   public int extraHorizScrollButtonHeight = Settings.screenHeight*2/160; // guich@560_11: now depends on the resolution
   
   /** The Flick object listens and performs flick animations on PenUp events when appropriate. */
   protected Flick flick;
   
   /** Sets the number of visible lines, used to make PREFERRED height return the given number of lines as the grid height.
    * This method must be called before setRect.
    * @since TotalCross 1.13
    */
   public int visibleLines = -1;
   
   /** If true, all ListBox will have the selection bar drawn in
    * the full width instead of the selected's text width
    * @since SuperWaba 5.5
    */
   public static boolean useFullWidthOnSelection; // guich@550_21
   
   /** Used by the DBListBox to store the data column that is displayed. */
   protected int dataCol;

   /** Creates an empty Listbox. */
   public ListBox()
   {
      this(null);
   }

   /** Creates a Listbox with the given items. */
   public ListBox(Object []items)
   {
      started = true; // avoid calling the initUI method
      ignoreOnAddAgain = ignoreOnRemove = true;
      sbar = Settings.fingerTouch ? new ScrollPosition() : new ScrollBar();
      sbar.focusTraversable = false;
      super.add(sbar);
      sbar.setLiveScrolling(true);
      if (items != null)
      {
         this.items = new Vector(items);
         itemCount = items.length;
      }
      sbar.setMaximum(itemCount);
      this.focusTraversable = true; // kmeehl@tc100
      if (Settings.fingerTouch)
         flick = new Flick(this);
   }
   
   public boolean flickStarted()
   {
      dragDistanceX = dragDistanceY = 0;
      return isScrolling; // only start flick if already scrolling
   }
   
   public void flickEnded(boolean atPenDown)
   {
   }
   
   public boolean canScrollContent(int direction, Object target)
   {
      if (Settings.fingerTouch)
         switch (direction)
         {
            case DragEvent.UP: return sbar.getValue() > sbar.getMinimum();
            case DragEvent.DOWN: return (sbar.getValue() + sbar.getVisibleItems()) < sbar.getMaximum();
            case DragEvent.LEFT: return xOffset < 0;
            case DragEvent.RIGHT: return xOffset > xOffsetMin;
         }
      return false;
   }
   
   public boolean scrollContent(int xDelta, int yDelta)
   {
      boolean hFlick = xDelta != 0 && ivWidths != null;
      boolean vFlick = yDelta != 0;
      int itemH = getItemHeight(0);
      
      if (hFlick)
      {
         if ((xDelta < 0 && xOffset >= 0) || (xDelta > 0 && xOffset <= xOffsetMin))
            hFlick = false;
         else
         {
            dragDistanceX += xDelta;
            if (dragDistanceX <= -itemH || dragDistanceX >= itemH)
            {
               int offsetDelta = dragDistanceX / itemH;
               dragDistanceX %= itemH;
               
               xOffset += -offsetDelta * itemH; // invert signal to follow weird onPaint implementation
               if (xOffset < xOffsetMin)
                  xOffset = xOffsetMin;
               else if (xOffset > 0)
                  xOffset = 0;
               
               enableButtons();
               Window.needsPaint = true;
            }
         }
      }
      if (vFlick)
      {
         int cur = sbar.getValue();
         
         if ((yDelta < 0 && cur <= sbar.getMinimum()) || (yDelta > 0 && cur >= sbar.getMaximum())) // already at the top/bottom of the view window
            vFlick = false;
         else
         {
            dragDistanceY += yDelta;
            if (dragDistanceY <= -itemH || dragDistanceY >= itemH)
            {
               int offsetDelta = dragDistanceY / itemH;
               dragDistanceY %= itemH;
               
               sbar.setValue(offset + offsetDelta);
               int newOffset = sbar.getValue();
               
               if (newOffset == offset) // did not scroll
                  vFlick = false;
               else
               {
                  offset = newOffset;
                  Window.needsPaint = true;
               }
            }
         }
      }
      
      return hFlick || vFlick;
   }

   public int getScrollPosition(int direction)
   {
      if (direction == DragEvent.LEFT || direction == DragEvent.RIGHT)
         return xOffset;
      return offset;
   }

   /** Adds support for horizontal scroll on this listbox. Two buttons will appear below
    * the vertical scrollbar. The add, replace and remove operations will be a bit slower
    * because the string's width will have to be computed in order to correctly set the max
    * horizontal scroll.
    * @since SuperWaba 5.6
    * @see #extraHorizScrollButtonHeight
    */
   public void enableHorizontalScroll() // guich@560_9
   {
      if (itemCount > 0)
      {
         int n = itemCount,m=0,w;
         int []widths = new int[n];
         for (int i = 0; i < n; i++)
            if ((w = widths[i] = fm.stringWidth(items.items[i].toString())) > m)
               m = w;
         ivWidths = new IntVector(widths);
         verifyItemWidth(m);
      }
      else
      {
         ivWidths = new IntVector();
         xOffsetMin = 0;
         enableButtons();
      }
   }
   
   private void enableButtons()
   {
      if (xOffset < xOffsetMin) // shift to the left if there's no more need to shift and it is shifted
         xOffset = xOffsetMin;
      if (btnLeft != null)
      {
         btnLeft.setEnabled(enabled && xOffset < 0);
         btnRight.setEnabled(enabled && xOffset > xOffsetMin);
      }
   }

   /** Adds an array of Objects to the Listbox */
   public void add(Object []moreItems)
   {
      add(moreItems, 0, moreItems.length);
   }

   /** Adds a range of an array of Objects to the Listbox */
   public void add(Object []moreItems, int startAt, int size)
   {
      int realSize = moreItems.length < startAt + size ? moreItems.length - startAt : size;
      if (itemCount == 0) // guich@310_5: directly assign the array if this listbox is empty
      {
         Object[] array = new Object[realSize];
         Vm.arrayCopy(moreItems, startAt, array, 0, realSize);
         this.items = new Vector(array);
         itemCount = realSize;
         if (ivWidths != null) // guich@560_9
            enableHorizontalScroll(); // just recompute for all items
      }
      else
      {
         int n = realSize; //moreItems.length; // guich@450_36
         itemCount += n;
         for (int i = startAt; i < n; i++)
            items.addElement(moreItems[i]);
         if (ivWidths != null) // guichich@560_9
         {
            int w,m=0,mx = -xOffsetMin;
            for (int i = 0; i < n; i++)
            {
               ivWidths.addElement(w=fm.stringWidth(moreItems[i].toString()));
               if (w > mx)
                  m = w;
            }
            verifyItemWidth(m);
         }
      }
      sbar.setEnabled(enabled && visibleItems < itemCount);
      sbar.setMaximum(itemCount); // guich@210_12: forgot this line!
   }

   /** Adds an Object to the Listbox */
   public void add(Object item)
   {
      items.addElement(item);
      if (ivWidths != null) // guich@560_9
      {
         int w = fm.stringWidth(item.toString());
         ivWidths.addElement(w);
         verifyItemWidth(w);
      }
      itemCount++;
      sbar.setEnabled(enabled && visibleItems < itemCount);
      sbar.setMaximum(itemCount);
   }
   
   /** Adds the given text to this ListBox, breaking the text if it goes beyond the ListBox' limits, and also breaking if it contains \n.
    * Returns the number of lines. Note that each part of the text is considered a new item. This method is slower than the other <code>add</code> methods.
    * @since TotalCross 1.24
    */
   public int addWrapping(String text) // guich@tc124_21
   {
      if (fm.stringWidth(text) <= btnX && text.indexOf('\n') < 0)
      {
         add(text);
         return 1;
      }
      String[] lines = Convert.tokenizeString(Convert.insertLineBreak(btnX, fm, text),'\n');
      add(lines);
      return lines.length;
   }   

   /** Adds an Object to the Listbox at the given index */
   public void insert(Object item, int index)
   {
      items.insertElementAt(item, index);
      if (ivWidths != null) // guich@560_9
      {
         int w = fm.stringWidth(item.toString());
         ivWidths.insertElementAt(w, index);
         verifyItemWidth(w);
      }
      itemCount++;
      sbar.setEnabled(enabled && visibleItems < itemCount);
      sbar.setMaximum(itemCount);
   }

   /** Empties this ListBox, setting all elements of the array to <code>null</code>
       so they can be garbage collected.
       <b>Attention!</b> If you used the same object array
       to initialize two ListBoxes (or ComboBoxes), this method will null both ListBoxes
       (because they use the same array reference),
       and you'll get a null pointer exception!
    */
   public void removeAll() // guich@210_13
   {
      items.removeAllElements();
      sbar.setMaximum(0);
      itemCount = 0;
      offset=0;  // wolfgang@330_23
      xOffset = xOffsetMin = 0;
      if (ivWidths != null) // guich@560_9
      {
         ivWidths.removeAllElements();
         enableButtons();
      }
      selectedIndex = -1; // seanwalton@401.26
      Window.needsPaint = true;
   }

   /** Removes the Object at the given index from the Listbox */
   public void remove(int itemIndex) // guich@200final_12: new method
   {
      if (0 <= itemIndex && itemIndex < itemCount)
      {
         items.removeElementAt(itemIndex);
         itemCount--;
         if (ivWidths != null) // guich@560_9
         {
            int old = ivWidths.items[itemIndex];
            ivWidths.removeElementAt(itemIndex);
            if (old == -xOffsetMin) // was this the max offset? recompute the remaining ones
            {
               int m = 0;
               int []widths = ivWidths.items;
               int n = ivWidths.size();
               for (int i =0; i < n; i++)
                  if (widths[i] > m)
                     m = widths[i];
               verifyItemWidth(m);
            }
         }
         sbar.setMaximum(itemCount);
         sbar.setEnabled(enabled && visibleItems < itemCount);

         if (selectedIndex == itemCount) // last item was removed?
            setSelectedIndex(selectedIndex-1);
         if ( itemCount == 0 )  // olie@200b4_196: if after removing the list has 0 items, setSelectedIndex( -1 ) is called, which does nothing (see there), then selectedIndex keeps being 0 which is wrong, it has to be -1
            selectedIndex = -1;
         if (itemCount <= visibleItems && offset != 0) // guich@200final_13
            offset = 0;
         Window.needsPaint = true;
      }
   }

   /** Removes an Object from the Listbox */
   public void remove(Object item)
   {
      int index;
      if (itemCount > 0 && (index=items.indexOf(item)) != -1)
         remove(index);
   }

   /** Replace the Object at the given index, starting from 0 */
   public void setItemAt(int i, Object s)
   {
      if (0 <= i && i < itemCount)
      {
         items.items[i] = s;
         if (ivWidths != null) // guich@560_9
            verifyItemWidth(ivWidths.items[i] = fm.stringWidth(s.toString()));
         Window.needsPaint = true;
      }
   }

   /** Get the Object at the given Index. Returns an empty string if the index is outside of range. */
   public Object getItemAt(int i)
   {
      if (0 <= i && i < itemCount)
         return items.items[i];
      return "";
   }

   /** Returns the selected item of the Listbox or an empty String Object if none is selected */
   public Object getSelectedItem()
   {
      int sel = getSelectedIndex();
      return sel >= 0 ? items.items[sel] : ""; // guich@200b4: handle no selected index yet.
   }

   /** Returns the position of the selected item of the Listbox or -1 if the listbox has no selected index yet. */
   public int getSelectedIndex()
   {
      return selectedIndex;
   }

   /** Returns all items in this ListBox. If the elements are Strings, the array
     * can be casted to String[].
     */
   public Object []getItems()
   {
      return items.toObjectArray();
   }

   /** Used internally
    */
   protected Object []getItemsArray()
   {
      return items.items;
   }

   /** Returns the index of the item specified by the name, or -1 if not found. */
   public int indexOf(Object name)
   {
      return items.indexOf(name);
   }

   /** Selects the given name. If the name is not found, the current selected item is not changed.
     * @since SuperWaba 4.01
     */
   public void setSelectedItem(Object name) // guich@401_25
   {
      int idx = indexOf(name);
      if (idx != -1)
         setSelectedIndex(idx);
   }

   /** Select the given index and scroll to it if necessary. */
   public void setSelectedIndex(int i)
   {
      if (0 <= i && i < itemCount && i != selectedIndex/* && height != 0*/) // guich@tc100: commented height!=0 otherwise Watch's combobox will not be set properly
      {
         int vi = sbar.getVisibleItems();
         int ma = sbar.getMaximum();
         if (offset+vi > ma) // astein@200b4_195: fix list items from being lost when the comboBox.setSelectedIndex() method is used
           offset=Math.max(ma-vi,0); // guich@220_4: fixed bug when the listbox is greater than the current item count

         selectedIndex = i;

         if (selectedIndex >= offset + vi) // kmeehl@tc100
         {
            offset = selectedIndex - vi + 1;
            sbar.setValue(offset);
         }
         else
         if (selectedIndex < offset)
         {
            offset = selectedIndex;
            sbar.setValue(offset);
         }
         Window.needsPaint = true;
      }
      else
      if (i == -1) // guich@200b4_191: unselect all items
      {
         offset = 0;
         selectedIndex = -1;
         if (height != 0)
         {
            sbar.setValue(0);
            Window.needsPaint = true;
         }
      }
   }

   /** Selects the last item added to this listbox, doing a scroll if needed. Calls repaintNow.
    * @since SuperWaba 5.6
    */
   public void selectLast()
   {
      if (itemCount > 0)
      {
         setSelectedIndex(itemCount-1);
         repaintNow();
      }
   }

   /** Returns the number of items */
   public int size()
   {
      return itemCount;
   }

   /** Do nothing. Adding a control to a ListBox is nonsense. */
   public void add(Control control)
   {
      Vm.warning("add(Control) cannot be used in the ListBox class!");
   }

   /** Do nothing. Removing a control from a ListBox is nonsense. */
   public void remove(Control control)
   {
      Vm.warning("remove(Control) cannot be used in the ListBox class!");
   }

   /** Returns the preferred width, ie, the size of the largest item plus the size of the scrollbar. */
   public int getPreferredWidth()
   {
      int extra = (simpleBorder?4:6);
      if (!Settings.fingerTouch && sbar.isVisible()) // guich@tc115_77: only include sbar if its visible
         extra += sbar.getPreferredWidth();
      int maxWidth = 0;
      for (int i = itemCount-1; i >= 0; i--)
      {
         int w = getItemWidth(i);
         if (w > maxWidth)
            maxWidth = w;
      }

      return maxWidth + extra + insets.left+insets.right;
   }

   /** Returns the number of items multiplied by the font metrics height */
   public int getPreferredHeight()
   {
      int n = visibleLines == -1 ? itemCount : visibleLines; // guich@tc113_11: use visibleLines if set
      int lineH = getItemHeight(0);
      int h = Math.max(lineH*n,sbar.getPreferredHeight())+(simpleBorder?4:6);
      if (ivWidths != null && h < 4*lineH) // guich@tc114_64
         h = 4*lineH;
      return (n==1 ? h-1 : h) + insets.top+insets.bottom;
   }

   /** This is needed to recalculate the box size for the selected item if the control is resized by the main application */
   protected void onBoundsChanged(boolean screenChanged)
   {
      npback = null;
      int btnW = sbar.getPreferredWidth();
      int extraHB = 0;
      if ((this.height/btnW > EXTRA_HEIGHT_FACTOR)) // guich@tc100b4_28 - guich@tc100b5_21: now using a proportion of the button. 
         extraHB = Settings.screenHeight*4/160;
      int m = uiFlat?0:simpleBorder?1:2,n=0;
      visibleItems = (height-m-2) / getItemHeight(0);
      btnX = width - m - btnW;
      if (uiPalm)
         {btnX--; m++;}
      btnX0 = btnX;
      if (!sbar.isVisible())
         btnX = width-1;

      if (ivWidths != null) // guich@560_9: handle horiz scroll?
      {
         if (btnRight == null)
         {
            int hh = 3 * fmH / 11;
            super.add(btnRight = new ArrowButton(Graphics.ARROW_RIGHT, hh, foreColor));
            super.add(btnLeft  = new ArrowButton(Graphics.ARROW_LEFT,  hh, foreColor));
            btnRight.focusTraversable = btnLeft.focusTraversable = false;
            tabOrder.removeElement(btnRight); // guich@572_6: remove them from the tabOrder, otherwise it will block the control navigation in some situations (AllTests)
            tabOrder.removeElement(btnLeft);
            if (uiPalm) // guich@554_31
            {
               btnRight.setBorder(Button.BORDER_NONE);
               btnLeft.setBorder(Button.BORDER_NONE);
            }
            onColorsChanged(true); // guich@tc111_6
         }
         n = (btnRight.getPreferredHeight() + extraHorizScrollButtonHeight + extraHB) << 1;
         if (uiFlat) n-=2; // in flat, make the buttons overlap a bit
         enableButtons();
      }
      sbar.setMaximum(itemCount);
      sbar.setVisibleItems(visibleItems);
      sbar.setEnabled(visibleItems < itemCount);
      if (Settings.fingerTouch)
      {
         sbar.setRect(RIGHT-1,m,PREFERRED,FILL, null, screenChanged);
         if (ivWidths != null)
         {
            btnLeft.setVisible(false);
            btnRight.setVisible(false);
         }
      }
      else
         sbar.setRect(btnX,m,btnW,height-(m<<1)-n, null, screenChanged);
      if (Settings.keyboardFocusTraversable) sbar.setFocusLess(true); // guich@570_39

      if (ivWidths != null) // guich@560_9: handle horiz scroll?
      {
         n = uiFlat ? 1 : 0; // in flat, make the buttons overlap a bit
         // add the two horizontal scroll buttons below the scrollbar
         btnLeft.setRect (SAME, AFTER-n, SAME, PREFERRED+extraHorizScrollButtonHeight+extraHB, null, screenChanged);
         btnRight.setRect(SAME, AFTER-n, SAME, PREFERRED+extraHorizScrollButtonHeight+extraHB, null, screenChanged);
         btnLeft.repositionAllowed = btnRight.repositionAllowed = false; // we'll handle the reposition ourselves
      }
      if (visibleItems >= itemCount) // volkernigge@572_7: fixed listbox problem when an index was selected before the bounds were set.
         offset = 0;
   }

   /** Searches this ListBox for an item with the first letter matching the given char. The search is made case insensitive. Note: if you override this class you must implement this method. */
   protected void find(char c)
   {
      int i;
      c = Convert.toUpperCase(c); // dbeers@570_103: make sure that the letter is uppercased.
      int foundIndex = -1; // guich@450_30: fix search when exist repeating letters (cat, chicken, cow - pressing C 3 times)
      // first search from the next item
      for (i =selectedIndex+1; i < itemCount; i++)
      {
         String s = items.items[i].toString(); // guich@220_37
         if (s.length() > 0 && Convert.toUpperCase(s.charAt(0)) == c) // first letter matches?
         {
            foundIndex = i;
            break;
         }
      }
      if (foundIndex == -1 && selectedIndex >= 0) // if didnt found, search from the beginning.
         for (i =0; i < selectedIndex; i++)
         {
            String s = items.items[i].toString(); // guich@220_37
            if (s.length() > 0 && Convert.toUpperCase(s.charAt(0)) == c) // first letter matches?
            {
               foundIndex = i;
               break;
            }
         }
      if (foundIndex != -1)
      {
         setSelectedIndex(foundIndex);
         Window.needsPaint = true;
      }
   }

   /** Handles the events for this control. */
   public void onEvent(Event event)
   {
      PenEvent pe;
      if (enabled)
      switch (event.type)
      {
         case ControlEvent.PRESSED:
            if (event.target == sbar)
            {
               int newOffset = sbar.getValue();
               if (newOffset != offset) // guich@200final_3: avoid unneeded repaints
               {
                  offset = newOffset;
                  Window.needsPaint = true;
               }
            }
            else 
            if (event.target == btnLeft || event.target == btnRight)
               horizontalScroll(event.target == btnLeft);
            break;
         case KeyEvent.KEY_PRESS:
            find(Convert.toUpperCase((char)((KeyEvent)event).key));
            break;
         case KeyEvent.SPECIAL_KEY_PRESS:
            KeyEvent ke = (KeyEvent)event;
            if (Settings.keyboardFocusTraversable && ke.isActionKey()) // guich@550_15
               postPressedEvent();
            else
            if (ke.isPrevKey() || ke.isNextKey()) // guich@220_19 - guich@330_45
            {
               if (Settings.keyboardFocusTraversable)
               {
                  if (ke.isUpKey())
                     setSelectedIndex(Settings.circularNavigation ? (selectedIndex==0 ? itemCount-1 : (selectedIndex-1)) : Math.max(selectedIndex-1,0));
                  else
                  if (ke.isDownKey())
                     setSelectedIndex(Settings.circularNavigation ? (selectedIndex==itemCount-1 ? 0 : (selectedIndex+1)) : Math.min(selectedIndex+1,itemCount-1));
                  else
                  if (ke.key == SpecialKeys.LEFT)
                  {
                     if (!horizontalScroll(true))
                        leftReached();
                  }
                  else
                  if (ke.key == SpecialKeys.RIGHT) // guich@560_9
                     horizontalScroll(false);
               }
               else
               if (ke.key == SpecialKeys.LEFT || ke.key == SpecialKeys.RIGHT) // guich@560_9
                  horizontalScroll(ke.key == SpecialKeys.LEFT);
               else
                  sbar._onEvent(event);
            }
            break;
         case PenEvent.PEN_UP:
            if (event.target == this && !isScrolling) // if scrolling, do not end selection
            {
               pe = (PenEvent)event;
               if (Settings.fingerTouch)
                  handleSelection(((pe.y- (simpleBorder?3:4)) / getItemHeight(0)) + offset); // guich@200b4: corrected line selection
               // Post the event
               int newSelection = ((pe.y- (simpleBorder?3:4)) / getItemHeight(0)) + offset; // guich@200b4_2: corrected line selection
               if (isInsideOrNear(pe.x,pe.y) && pe.x < btnX && newSelection < itemCount)
                  postPressedEvent();
               endSelection();
            }
            isScrolling = false;
            break;
         case PenEvent.PEN_DRAG:
            DragEvent de = (DragEvent)event;
            
            if (Settings.fingerTouch)
            {
               if (isScrolling)
               {
                  scrollContent(-de.xDelta, -de.yDelta);
                  event.consumed = true;
               }
               else
               {
                  int direction = DragEvent.getInverseDirection(de.direction);
                  if (canScrollContent(direction, de.target) && scrollContent(-de.xDelta, -de.yDelta))
                     event.consumed = isScrolling = scScrolled = true;
               }
            }
            break;
         case KeyEvent.ACTION_KEY_PRESS: // guich@tc113_9
            if (!(this instanceof MultiListBox) && selectedIndex >= 0)
            {
               boolean old = isHighlighting;
               postPressedEvent();
               isHighlighting = old;
            }
            break;
         case PenEvent.PEN_DOWN:
            scScrolled = false;
            pe = (PenEvent)event;
            if (!Settings.fingerTouch && event.target == this && pe.x < btnX && isInsideOrNear(pe.x,pe.y))
               handleSelection(((pe.y- (simpleBorder?3:4)) / getItemHeight(0)) + offset); // guich@200b4: corrected line selection
            break;
      }
   }
   
   protected void leftReached()
   {
   }
   
   protected void endSelection()
   {
   }
   
   protected void handleSelection(int newSelection)
   {
      if (newSelection != selectedIndex && newSelection < itemCount)
      {
         if (transparentBackground) // guich@tc115_18: on transparent backgrounds, we must repaint everything
         {
            selectedIndex = newSelection;
            Window.needsPaint = true;
         }
         else
         {
            Graphics g = getGraphics();
            if (selectedIndex >= 0)
               drawCursor(g,selectedIndex,false);
            selectedIndex = newSelection;
            drawCursor(g,selectedIndex,true);
         }
      }
   }

   public void setEnabled(boolean enabled)
   {
      if (enabled != this.enabled)
      {
         this.enabled = enabled;
         onColorsChanged(false);
         sbar.setEnabled(enabled && visibleItems < itemCount);
         if (btnLeft != null)
            enableButtons();
         Window.needsPaint = true; // now the controls have different l&f for disabled states
      }
   }

   protected void onColorsChanged(boolean colorsChanged)
   {
      npback = null;
      fColor = getForeColor();
      back0  = Color.brighter(getBackColor());
      back1  = customCursorColor!=-1 ? customCursorColor : (back0 != Color.WHITE) ? backColor : Color.getCursorColor(back0);//guich@300_20: use backColor instead of: back0.getCursorColor(); // guich@210_19
      if (fColor == back1) // guich@200b4_206: ops! same color?
         fColor = foreColor;
      Graphics.compute3dColors(enabled,backColor,foreColor,fourColors);
      if (btnRight != null)
      {
         btnRight.setBackForeColors(uiVista?back0:backColor, foreColor);
         btnLeft.setBackForeColors(uiVista?back0:backColor, foreColor);
      }
      if (colorsChanged)
         sbar.setBackForeColors(uiVista?back0:backColor,foreColor);
   }

   public void onPaint(Graphics g)
   {
      int i;
      // Draw background and borders
      g.backColor = uiAndroid ? parent.backColor : back0;
      if (!transparentBackground) // guich@tc115_18
         g.fillRect(0,0,width,height); // guich@tc115_77: fill till end because the scrollbar may not being shown
      if (uiAndroid)
      {
         if (npback == null)
            try
            {
               npback = NinePatch.getInstance().getNormalInstance(NinePatch.LISTBOX, width, height, enabled ? back0 : Color.interpolate(back0,parent.backColor), false,true);
            }
         catch (ImageException e) {}
         g.drawImage(npback, 0,0);
      }
      g.foreColor = foreColor;
      if (!uiAndroid)
         if (simpleBorder && uiCE)
            g.drawRect(0,0,width,height);
         else
            g.draw3dRect(0,0,width,height,uiPalm?Graphics.R3D_SHADED:Graphics.R3D_CHECK,false,false,fourColors);
      g.foreColor = fColor;

      int dx = 2; // guich@580_41: changed from 3 to 2
      int dy = 3;
      if (uiPalm || uiFlat) dy--;
      if (simpleBorder) {dx--; dy--;}

      setTextAreaClip(g,dx,dy); // guich@tc100b4_5
      dx += xOffset;
      int greatestVisibleItemIndex = Math.min(itemCount, visibleItems+offset); // code corrected by Bjoem Knafla
      dx++;
      for (i = offset; i < greatestVisibleItemIndex; dy += getItemHeight(i++))
         drawItem(g,i,dx,dy); // guich@200b4: let the user extend ListBox and draw the items himself
      drawSelectedItem(g, offset, greatestVisibleItemIndex);
   }
   
   protected int getItemHeight(int i)
   {
      return uiAndroid?fmH*3/2:fmH;
   }

   protected void setTextAreaClip(Graphics g, int dx, int dy) // guich@tc100b4_5: use a common routine to prevent errors
   {
      int yy = dy-(uiCE?1:0);
      g.setClip(dx+1,yy,btnX-dx-2,Math.min(height-(uiCE||uiPalm||uiAndroid?2:1)-yy,getItemHeight(0)*visibleItems + (uiPalm||uiAndroid?1:2))); // guich@tc100b5_20: don't let get over the border - guich@tc115_77: if scrollbar is not shown, use the whole area
   }
   
   protected void drawSelectedItem(Graphics g, int from, int to)
   {
      if (selectedIndex >= 0) drawCursor(g,selectedIndex,true);
   }

   /** Sets the cursor color for this ListBox. The default is equal to the background slightly darker. Make sure you tested it in 2,4 and 8bpp devices. */
   public void setCursorColor(int color)
   {
      this.customCursorColor = color;
      onColorsChanged(true);
   }
   /** You can extend ListBox and overide this method to draw the items */
   protected void drawItem(Graphics g, int index, int dx, int dy)
   {
      String s = items.items[index].toString();
      // guich@tc100b4: allow change of back/fore colors
      int f = g.foreColor;
      if (ihtForeColors != null)
         g.foreColor = ihtForeColors.get(index,f);
      if (ihtBackColors != null)
      {
         int b = g.backColor;
         g.backColor = ihtBackColors.get(index,b);
         g.fillRect(dx,dy,useFullWidthOnSelection ? btnX : fm.stringWidth(s), getItemHeight(index));
         g.backColor = b;
      }
      g.drawText(s,dx,dy+(!uiAndroid?0:(getItemHeight(index)-fmH)/2), textShadowColor != -1, textShadowColor); // guich@402_31: don't test for index out of bounds. this will be catched in the caller
      g.foreColor = f;
   }
   /** You can extend ListBox and overide this method to draw the items */
   protected void drawSelectedItem(Graphics g, int index, int dx, int dy)
   {
      g.drawText(getText(),dx,dy, textShadowColor != -1, textShadowColor);
   }
   /** Returns the width of the given item index with the current fontmetrics. Note: if you overide this class you must implement this method. */
   protected int getItemWidth(int index)
   {
      return fm.stringWidth(items.items[index].toString());
   }

   int getIndexY(int sel)
   {
      int dy = 3;
      if (uiPalm || uiFlat) dy--;
      if (simpleBorder) dy--;
      int ih = getItemHeight(sel);
      dy += (sel-offset) * ih;
      return dy + ih + fm.descent;
   }

   /** This method is used to draw the cursor around the desired item */
   protected void drawCursor(Graphics g, int sel, boolean on)
   {
      if (offset <= sel && sel < visibleItems+offset && sel < itemCount) // guich@555_10: fixed in all ui styles
      {
         g.foreColor = fColor; // guich@520_15: by using fillRect we must set to the textcolor
         g.backColor = on?getCursorColor(sel):back0;

         int dx = 3; // guich@580_41: cursor must be drawn at 3 or will overwrite the border on a combobox with PalmOS style
         int dy = 3;
         if (uiPalm || uiFlat) dy--;
         if (simpleBorder) {dx--; dy--;}

         setTextAreaClip(g,dx-1,dy); // guich@tc100b4_5

         int ih = getItemHeight(sel);
         dx += xOffset; // guich@552_24: added this to make scroll apply to the item
         dy += (sel-offset) * ih;
         int sw;
         if (useFullWidthOnSelection || (sw = getItemWidth(sel)) == 0) // guich@500_16: use the full text width due to the horizontal scroll - guich@200final_5: if item is a zero length string, invert the complete row.
            sw = btnX-4;
         g.fillRect(dx-1, dy-1, sw+2, ih+fm.descent); // pgr@520_4: if this is an image or an antialiased font, using eraseRect will make it ugly. - guich@552_7: added -1 to fix cursor not overwriting border.
         drawItem(g, sel, dx, dy); // pgr@520_4
         if (on && getParentWindow() instanceof ComboBoxDropDown && !(this instanceof MultiListBox)) Window.updateScreen(); // guich@tc114_80: update screen before the combobox closes. not comparing with ComboBoxDropDown results in screen FLICKERing - guich@tc115_89: prevent flicker in MultiListBox
      }
   }
   
   protected int getCursorColor(int index)
   {
      return back1;
   }

   /** Sets the border of the listbox to be not 3d if flag is true. */
   public void setSimpleBorder(boolean simpleBorder) // guich@200b4_93
   {
      this.simpleBorder = simpleBorder;
   }

   /** Sorts the elements of this ListBox. The current selection is cleared. */
   public void qsort() // guich@220_35
   {
      items.qsort();
      setSelectedIndex(-1);
   }

   /** Sorts the elements of this ListBox. The current selection is cleared.
    * @param caseless Pass true to make a caseless sort, if the items are Strings. 
    */
   public void qsort(boolean caseless) // guich@tc113_5
   {
      if (size() > 0)
      {
         Convert.qsort(items.items, 0, items.size()-1, (caseless && items.items[0] instanceof String) ? Convert.SORT_STRING_NOCASE : Convert.SORT_AUTODETECT, true);
         setSelectedIndex(-1);
      }
   }

   /** Check if it is needed to change the xOffsetMin based in the given item's text width */
   private void verifyItemWidth(int w)
   {
      int newxOffsetMin = -Math.max(w-width+sbar.width+5,0);
      if (newxOffsetMin < xOffsetMin)
      {
         xOffsetMin = newxOffsetMin;
         enableButtons();
      }
   }
   /** Scrolls the grid horizontaly as needed */
   private boolean horizontalScroll(boolean toLeft)
   {
      int step = this.width >> 1;
      int newOffset = toLeft ? Math.min(xOffset + step, 0) : Math.max(xOffset - step, xOffsetMin);
      if (newOffset != xOffset)
      {
         xOffset = newOffset;
         enableButtons();
         Window.needsPaint = true;
         return true;
      }
      return false;
   }

   /** Clears this control, selecting index clearValueInt. */
   public void clear() // guich@572_19
   {
      setSelectedIndex(clearValueInt);
   }

   public void getFocusableControls(Vector v)
   {
      if (visible && enabled) v.addElement(this);
   }

   public Control handleGeographicalFocusChangeKeys(KeyEvent ke) // any change here must synchronize with MultiListBox'
   {
      if ((ke.isPrevKey() && !ke.isUpKey()) || (ke.isNextKey() && !ke.isDownKey()))
      {
         int oldXOffset = xOffset;
         _onEvent(ke);
         return (oldXOffset != xOffset) ? this : null;
      }
      if ((ke.isUpKey() && selectedIndex <= 0) || (ke.isDownKey() && selectedIndex == itemCount -1))
         return null;
      _onEvent(ke);
      return this;
   }
   
   /** Returns the string of the selected item or "" if none is selected. */
   public String getText()
   {
      return selectedIndex < 0 ? "" : getSelectedItem().toString();
   }

   /** Selects the item that starts with the given text
    * @param text The text string to search for
    * @param caseInsensitive If true, the text and all searched strings are first converted to lowercase.
    * @return If an item was found and selected.
    * @since TotalCross 1.13
    */
   public boolean setSelectedItemStartingWith(String text, boolean caseInsensitive) // guich@tc113_2
   {
      if (caseInsensitive) text = text.toLowerCase();
      for (int i =0; i < itemCount; i++)
      {
         String s = items.items[i].toString();
         if (caseInsensitive) s = s.toLowerCase();
         if (s.startsWith(text))
         {
            setSelectedIndex(i);
            return true;
         }
      }
      return false;
   }

   /** This method hides the scrollbar if its not needed, i.e., if horizontal scroll is disabled and the preferred
    * height is smaller than the actual height. You may have to call <code>reposition</code> if this method returns true.
    * You can call this method after all items were added.
    * @return True if the scrollbar was hidden.
    * @since TotalCross 1.15
    */
   public boolean hideScrollBarIfNotNeeded() // guich@tc115_77
   {
      boolean showSB = ivWidths != null || getPreferredHeight() > getHeight();
      if (sbar.isVisible() != showSB)
      {
         sbar.setVisible(showSB);
         btnX = showSB ? btnX0 : width-1;
         return true;
      }
      return false;
   }
   
   public Flick getFlick()
   {
      return flick;
   }

   public boolean wasScrolled()
   {
      return scScrolled;
   }
}
