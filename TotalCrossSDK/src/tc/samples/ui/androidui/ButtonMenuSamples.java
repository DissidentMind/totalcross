package tc.samples.ui.androidui;

import totalcross.ui.*;
import totalcross.ui.dialog.*;
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;
import totalcross.ui.image.*;

public class ButtonMenuSamples extends BaseContainer
{
   private ScrollContainer sc;
   
   public ButtonMenuSamples()
   {
      helpMessage = "These are ButtonMenu samples in the Android user interface style. Press back to go to the main menu.";
   }
   
   public void initUI()
   {
      try
      {
         super.initUI();
         sc = new ScrollContainer(false, true);
         sc.setInsets(0,0,gap/2,gap/2);
         add(sc,LEFT,TOP,FILL,FILL);
         
         final UpdateMatrix um = new UpdateMatrix();
         um.p = sc;
         Image[] icons =
         {
            new Image("images/ic_dialog_usb.png"   ),
            new Image("images/ic_dialog_alert.png" ),
            new Image("images/ic_dialog_dialer.png"),
            new Image("images/ic_dialog_email.png" ),
            new Image("images/ic_dialog_info.png"  ),
            new Image("images/ic_dialog_map.png"   ),
            new Image("images/ic_dialog_time.png"  ),
         };
         String[] names =
         {
            "usb",
            "alert",
            "dialer",
            "email",
            "info",
            "map",
            "time",
         };
         um.oldtit = getTitle();
         
         // single-row
         um.ib = new ButtonMenu(icons, names, ButtonMenu.SINGLE_ROW);
         um.ib.textPosition = BOTTOM;
         um.ib.buttonHorizGap = um.ib.buttonVertGap = 50;
         um.ib.setBackForeColors(Color.brighter(BKGCOLOR), Color.WHITE);
         um.ib.pressedColor = Color.CYAN;
         sc.add(new Ruler(), LEFT,TOP,FILL,0); // this ruler makes the ScrollContainer have the same width always. otherwise, when changing the UpdateMatrix, it will be shrinked in 10 pixels at the width 
         sc.add(um.ib,LEFT+gap,TOP,FILL-gap,PREFERRED);
         um.ib.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               setTitle(um.oldtit+" - Button: "+um.ib.getSelectedIndex());
            }
         });
         
         int hs = fmH/2;
         sc.add(new Label("Text pos: "),LEFT+gap,AFTER+gap,PREFERRED,PREFERRED+hs);
         sc.add(um.cbtp = new ComboBox(new String[]{"left","right","top","bottom","right_of"}),AFTER,SAME,PREFERRED+hs,PREFERRED+hs);
         um.cbtp.setSelectedIndex(0);
         um.cbtp.addPressListener(um);
         sc.add(new Label("Border: "),LEFT+gap,AFTER+gap,PREFERRED,PREFERRED+hs);
         sc.add(um.cbnb = new ComboBox(new String[]{"3D Horiz Gradient","3D Vert Gradient","3D Border", "No border"}),SAME,AFTER+gap,PREFERRED+hs,PREFERRED+hs,um.cbtp);
         um.cbnb.setSelectedIndex(0);
         um.cbnb.addPressListener(um);
         RadioGroupController rg = new RadioGroupController();
         sc.add(um.rdh = new Radio("horizontal",rg),SAME,AFTER+gap,PREFERRED+hs,PREFERRED+hs);
         sc.add(um.rdv = new Radio("vertical",rg),AFTER+gap,SAME,PREFERRED+hs,PREFERRED+hs);
         // since a label does not have the same height of a Radio, we have to place the radio before
         sc.add(new Label("Scroll: "),LEFT+gap,SAME,PREFERRED,SAME);
         
         um.rdh.setChecked(true);
         um.rdh.addPressListener(um);
         um.rdv.addPressListener(um);
         
         // multiple-row - replicate our previous items
         um.icons2 = new Image[icons.length*10];
         um.names2 = new String[um.icons2.length];
         int nn = um.icons2.length/icons.length;
         for (int i = 0, k=0; i < icons.length; i++)
            for (int j = 0; j < nn; j++)
            {
               um.icons2[j*icons.length+i] = icons[i];
               um.names2[j*icons.length+i] = names[i]+" "+ k++;
            }
         um.controlPressed(null);
         
         setInfo("Click Info button for help.");
      }
      catch (Exception ee)
      {
         MessageBox.showException(ee,true);
      }
   }

   // scroll to the ButtonMenu if the user uses it. 
   Control lastParent;
   public void onEvent(Event e)
   {
      if (e.type == ControlEvent.FOCUS_IN)
      {
         Control par = ((Control)e.target).getParent();
         boolean isSC = par.getClass().getName().startsWith("totalcross.ui.ScrollContainer");
         if (isSC && par != lastParent)
         {
            lastParent = par;
            sc.scrollToControl(par instanceof ScrollContainer ? par : par.getParent());
         }
      }
   }
   
   static byte buttonTypes[] = {Button.BORDER_3D_VERTICAL_GRADIENT, Button.BORDER_3D_HORIZONTAL_GRADIENT, Button.BORDER_3D, Button.BORDER_NONE};
   
   class UpdateMatrix implements PressListener
   {
      Container p;
      ComboBox cbtp;
      ComboBox cbnb;
      Radio rdh,rdv;
      Image[] icons2;
      String[] names2;
      ButtonMenu ib2,ib;
      String oldtit;
      
      public void controlPressed(ControlEvent e)
      {
         int tp = cbtp.getSelectedIndex();
         byte type = buttonTypes[cbnb.getSelectedIndex()];
         boolean vert = rdv.isChecked();
         if (ib2 != null)
            p.remove(ib2);
         ib2 = new ButtonMenu(icons2, names2, vert ? ButtonMenu.MULTIPLE_VERTICAL : ButtonMenu.MULTIPLE_HORIZONTAL);
         ib2.borderType = type;
         ib2.textPosition = tp == 0 ? LEFT : tp == 1 ? RIGHT : tp == 2 ? TOP : tp == 3 ? BOTTOM : RIGHT_OF;
         ib2.setForeColor(Color.WHITE);
         ib2.setBackColor(SELCOLOR);
         p.add(ib2,LEFT+10,AFTER+10,FILL-10,SCREENSIZE+50,rdv);
         ib2.addPressListener(new PressListener()
         {
            public void controlPressed(ControlEvent e)
            {
               setInfo(oldtit+" - Button: "+ib2.getSelectedIndex());
            }
         });
      }
   }
}