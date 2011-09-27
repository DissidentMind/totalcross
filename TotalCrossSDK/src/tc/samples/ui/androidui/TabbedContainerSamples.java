package tc.samples.ui.androidui;

import totalcross.ui.*;
import totalcross.ui.dialog.*;
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;
import totalcross.ui.image.*;

public class TabbedContainerSamples extends BaseContainer
{
   public TabbedContainerSamples()
   {
      helpMessage = "These are TabbedContainer samples in the Android user interface style. Press back to go to the main menu.";
   }
   
   public void initUI()
   {
      try
      {
         super.initUI();
         String[] caps = {"nak","andr�","babi"};
         Image [] icons = {new Image("images/ic_dialog_alert.png"),new Image("images/ic_dialog_usb.png"),new Image("images/ic_dialog_info.png")};
         final TabbedContainer tc = new TabbedContainer(caps);
         tc.setBackColor(Color.DARK);
         tc.getContainer(0).setBackColor(0x080C84);
         tc.getContainer(1).setBackColor(0x840C08);
         tc.getContainer(2).setBackColor(0x088608);
         tc.setIcons(icons);
         tc.pressedColor = Color.ORANGE;
         tc.activeTabBackColor = 0xDDDDDD;
         tc.allSameWidth = true;
         tc.extraTabHeight = fmH*2;
         add(tc,LEFT,TOP+fmH,FILL,SCREENSIZE+30);

         final TabbedContainer tc2 = new TabbedContainer(caps);
         tc2.setType(TabbedContainer.TABS_BOTTOM);
         tc2.setBackColor(Color.DARK);
         tc2.getContainer(0).setBackColor(0x080C84);
         tc2.getContainer(1).setBackColor(0x840C08);
         tc2.getContainer(2).setBackColor(0x088608);
         tc2.useOnTabTheContainerColor = true;
         tc2.pressedColor = Color.ORANGE;
         tc2.allSameWidth = true;
         tc2.extraTabHeight = fmH/2;
         add(tc2,LEFT,BOTTOM-fmH,FILL,SCREENSIZE+30);
         
         final Check ch = new Check("Disable last tab");
         add(ch, LEFT+2,CENTER);
         ch.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               tc.setEnabled(2,!ch.isChecked());
               tc2.setEnabled(2,!ch.isChecked());
            }
         });
         setInfo("Click Info button for help.");
      }
      catch (Exception ee)
      {
         MessageBox.showException(ee,true);
      }
   }
}