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

package tc.samples.ui.androidui;

import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.gfx.*;

public class AndroidUI extends MainWindow
{
   static
   {
      Settings.useNewFont = true;
   }

   public AndroidUI()
   {
      setUIStyle(Settings.Android);
      setBackColor(UIColors.controlsBack = Color.WHITE);
      UIColors.messageboxBack = Color.brighter(BaseContainer.BKGCOLOR,64);
      UIColors.messageboxFore = Color.WHITE;
      //ScrollPosition.AUTO_HIDE = false;
   }
   
   public void initUI()
   {
      new MainMenu().show();
   }
}
