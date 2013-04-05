package tc.samples.ui.androidui;

import totalcross.ui.*;
import totalcross.ui.dialog.*;
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;

public class CheckRadioSamples extends BaseContainer
{
   public CheckRadioSamples()
   {
      helpMessage = "These are Check and Radio box samples in the Android user interface style. Press back to go to the main menu.";
   }
   
   Control c0,c1,c2,c3,c4,c5;
   public void initUI()
   {
      try
      {
         super.initUI();
         setTitle("Check and Radio");

         ScrollContainer sc = new ScrollContainer(false, true);
         sc.setInsets(gap,gap,gap,gap);
         Check c;
         
         sc.add(c0 = c = new Check("Check box / cyan check"),LEFT,AFTER,PREFERRED+gap,PREFERRED+gap); 
         c.checkColor = Color.CYAN;
         c.setChecked(true);

         sc.add(c1 = c = new Check("Check box / yellow background"),LEFT,AFTER+gap,PREFERRED+gap,PREFERRED+gap);
         c.setBackColor(Color.YELLOW);
         c.textColor = Color.BLUE;
         c.checkColor = Color.YELLOW;

         sc.add(c2 = c = new Check("Check box / green foreground"),LEFT,AFTER+gap,PREFERRED+gap,PREFERRED+gap); 
         c.setForeColor(Color.darker(Color.GREEN));
         c.checkColor = Color.GREEN;

         RadioGroupController rg = new RadioGroupController();
         
         Radio r;
         sc.add(c3 = r = new Radio("Radio / cyan check",rg),LEFT,AFTER+gap*2,PREFERRED+gap,PREFERRED+gap); 
         r.checkColor = Color.CYAN;
         r.setChecked(true);

         sc.add(c4 = r = new Radio("Radio / yellow background",rg),LEFT,AFTER+gap,PREFERRED+gap,PREFERRED+gap);
         r.setBackColor(Color.YELLOW);
         r.textColor = Color.BLUE;
         r.checkColor = Color.YELLOW;

         sc.add(c5 = r = new Radio("Radio / green foreground",rg),LEFT,AFTER+gap,PREFERRED+gap,PREFERRED+gap); 
         r.setForeColor(Color.darker(Color.GREEN));
         r.checkColor = Color.GREEN;
         
         c0.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               boolean b = ((Check)c0).isChecked();
               c1.setEnabled(b);
               c2.setEnabled(b);
               c3.setEnabled(b);
               c4.setEnabled(b);
               c5.setEnabled(b);
            }
         });
         add(sc,LEFT,TOP,FILL,FILL);
         setInfo("Click Info button for help.");
      }
      catch (Exception ee)
      {
         MessageBox.showException(ee,true);
      }
   }
}