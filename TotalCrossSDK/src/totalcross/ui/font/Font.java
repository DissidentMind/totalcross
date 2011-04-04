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



package totalcross.ui.font;

import totalcross.Launcher;
import totalcross.sys.Settings;
import totalcross.util.Hashtable;

/**
 * Font is the character font used when drawing text on a surface.
 * Fonts can be antialiased, and usually range from size 6 to 22.
 * <ol>
 * <li> To see if the font you created is installed in the target device, query its name after
 * the creation. If the font is not found, its name is changed to match the default font.
 * <li> You may create new fonts based on the TrueType fonts using the tool tc.tools.FontGenerator.
 * </ol>
 */

public final class Font
{
   /** Read only field that contains the font's name. Note that changing this directly will have no effect. */
   public String name;
   /** Read only field that contains the font's style. Note that changing this directly will have no effect. For bold fonts, style == 1. */
   public int style;
   /** Read only field that contains the font's size. Note that changing this directly will have no effect. */
   public int size;
   // HOOK VARIABLES - can't be private - guich@350_20
   public Object hv_UserFont;
   public FontMetrics fm;

   /** Returns the default font size, based on the screen's size.
    * If Settings.fingerTouch is true, the default font size will be increased by 15%. 
    */
   public static int getDefaultFontSize()
   {
      if (Settings.isWindowsDevice())
         return 12; // added this exception to get the right font when running in the WM phone in landscape mode
      if (Settings.ANDROID.equals(Settings.platform)) // guich@tc126_69
         return 20 * Settings.deviceFontHeight / 14; 

      int fontSize; //flsobral@tc126_49: with the exception of WindowsCE and WinMo, the font size is now based on the screen resolution for all platforms to better support small phones and tablets.
      switch (Settings.screenWidth)
      {
         // some predefined device screen sizes
         case 480:
         case 360:
         case 320:
            if (Settings.screenHeight < 240)
               fontSize = 13;
            else if(Settings.screenHeight == 240)
               fontSize = 14;
            else
               fontSize = 18;
            break;
         case 640:
         case 240:
         case 220:
         case 200:
            fontSize = 12;
            break;
         case 176: fontSize = 11; break;
         default :
            if (Settings.screenWidth * Settings.screenHeight > 480000 /*800x600*/) // bigger font for tablets, final value will be 26 if the device is fingerTouch
               fontSize = 23;
            else
               fontSize = 9; // guich@tc123_13: pk doesn't like to have a size=20 for above 640
      }
      
      if (Settings.fingerTouch) // bigger font for fingerTouch
         fontSize *= 1.15;
      return fontSize;
   }

   /** A normal-sized font */
   public static final int NORMAL_SIZE = getDefaultFontSize();
   /** A big-sized font (2 above the normal size) */
   public static final int BIG_SIZE = NORMAL_SIZE+2;

   /** The default font name: "TCFont". If a specified font is not found, this one is used instead. */
   public static final String DEFAULT = "TCFont";
   /** The minimum font size: 6. */
   public static int MIN_FONT_SIZE = 6;
   /** The maximum font size: 22. */
   public static int MAX_FONT_SIZE = 34; // guich@tc122_17: 24 -> 30

   /** The tab size will be TAB_SIZE * font's max width. Defaults to 3, but you can change at any time. */
   public static int TAB_SIZE = 3;

   private static Hashtable htFonts = new Hashtable(13);
   private static StringBuffer sb = new StringBuffer(30);

   private Font(String name, boolean boldStyle, int size) // guich@580_10
   {
      this.name = name;
      this.style = boldStyle ? 1 : 0;
      this.size = size;
      fontCreate();
      fm = new FontMetrics(this); // guich@450_36: get out fontmetrics at once.
   }

   /**
    * Gets the instance of the default font, with the given style and size.
    * @param boldStyle If true, a bold font is used. Otherwise, a plain font is used.
    * @param size If you want a text bigger than the standard size, use Font.NORMAL_SIZE+x; or if you want
    * a text smaller than the standard size, use Font.NORMAL_SIZE-x. Size is adjusted to be in the range
    * <code>Font.MIN_FONT_SIZE ... Font.MAX_FONT_SIZE</code>.
    */
   public static Font getFont(boolean boldStyle, int size) // guich@580_10
   {
      return getFont(DEFAULT, boldStyle, size);
   }

   /**
    * Gets the instance of a font of the given name, style and size. Font styles are defined
    * in this class. BlackBerry supports the use of native system fonts, which are formed by
    * the font family name preceded by a '$' (e.g.: "$BBCasual"). You can also specify only
    * "$" for the font name, which means the default system font. "TCFont" will be used in
    * place of native fonts for all platforms that do not support them.
    * @param name "TCFont" is the default font. You must install other fonts if you want to use them.
    * @param boldStyle If true, a bold font is used. Otherwise, a plain font is used.
    * @param size If you want a text bigger than the standard size, use Font.NORMAL_SIZE+x; or if you want
    * a text smaller than the standard size, use Font.NORMAL_SIZE-x. Size is adjusted to be in the range
    * <code>Font.MIN_FONT_SIZE ... Font.MAX_FONT_SIZE</code>.
    */
   public static Font getFont(String name, boolean boldStyle, int size) // guich@580_10
   {
      sb.setLength(0);
      String key = sb.append(name).append('$').append(boldStyle?'B':'P').append(size).toString();
      Font f = (Font)htFonts.get(key);
      if (f == null)
         htFonts.put(key, f = new Font(name, boldStyle, size));
      return f;
   }

   /** Returns this font as Bold */
   public Font asBold()
   {
      return getFont(name,true,size); // guich@450_36: cache the bolded font - guich@580_10: cached now in the Hashtable.
   }

   ///// Native methods
   native void fontCreate4D();
   void fontCreate()
   {
      hv_UserFont = Launcher.instance.getFont(this, ' ');
   }
}