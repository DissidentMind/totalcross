package tc.samples.ui.controls;

import totalcross.res.*;
import totalcross.ui.*;
import totalcross.ui.dialog.*;
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;
import totalcross.ui.image.*;

public class MessageBoxSample extends BaseContainer
{
   public void initUI()
   {
      try
      {
         super.initUI();
         setTitle("MessageBox");
         ScrollContainer sc = new ScrollContainer(false, true);
         sc.setInsets(gap,gap,gap,gap);
         add(sc,LEFT,TOP,FILL,FILL);
         
         Button btn;
         
         sc.add(btn = new Button("Title only"), CENTER, TOP+fmH,PREFERRED+gap,PREFERRED+gap);
         btn.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               MessageBox mb = new MessageBox("Message","This is a MessageBox with title, in the Android user interface style.",new String[]{"Close"});
               mb.popup();
            }
         });
         sc.add(btn = new Button("No title"), CENTER, AFTER+fmH,PREFERRED+gap,PREFERRED+gap);
         btn.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               MessageBox mb = new MessageBox("","This is a MessageBox without title, in the Android user interface style.",new String[]{"Close"});
               mb.popup();
            }
         });
         sc.add(btn = new Button("Title and Icon\nTop separator"), CENTER, AFTER+fmH,PREFERRED+gap,PREFERRED+gap);
         btn.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               MessageBox mb = new MessageBox("Message","This is a MessageBox with title and icon with top separator, in the Android user interface style.",new String[]{"Close"});
               mb.headerColor = UIColors.messageboxBack;
               mb.footerColor = 0xAAAAAA;
               try
               {
                  mb.setIcon(Resources.warning);
               }
               catch (Exception ee) {ee.printStackTrace();}
               mb.popup();
            }
         });
         sc.add(btn = new Button("Title and Icon\nTop/bottom separators"), CENTER, AFTER+fmH,PREFERRED+gap,PREFERRED+gap);
         btn.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               MessageBox mb = new MessageBox("Message","This is a MessageBox with title and icon with top and bottom separators, in the Android user interface style.",new String[]{"Close"});
               mb.footerColor = mb.headerColor = UIColors.messageboxBack;
               try
               {
                  // paint a copy of the image with the yellow color
                  Image img = Resources.warning.getFrameInstance(0);
                  img.applyColor2(Color.YELLOW);
                  mb.setIcon(img);
               }
               catch (Exception ee) {ee.printStackTrace();}
               mb.popup();
            }
         });
      }
      catch (Exception ee)
      {
         MessageBox.showException(ee,true);
      }
   }
}