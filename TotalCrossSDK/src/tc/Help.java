package tc;

import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.event.*;
import totalcross.ui.gfx.*;

public class Help extends MainWindow
{
   public Help()
   {
      setUIStyle(Settings.Android);
      setBackColor(UIColors.controlsBack = Color.WHITE);
      Settings.uiAdjustmentsBasedOnFontHeight = true;
   }
   
   private static final int COLOR = 0x05B6EE;
   private boolean isEn=true;
   private String x(String en, String pt)
   {
      return isEn || pt == null ? en : pt; 
   }
   
   TabbedContainer tc;
   Label lstatus;
   Edit edclass,edkey;
   Radio rEn, rPt;
   
   public void initUI()
   {
      removeAll();
      RadioGroupController rg = new RadioGroupController();
      add(new Label(x("Helper application to Run/Deploy", "Aplica��o de ajuda para Executar / Empacotar"),CENTER,Color.BLUE,true),LEFT,TOP,FILL,PREFERRED);
      add(new Label(x("Language: ","Linguagem: ")), LEFT,AFTER+25);
      add(rEn = new Radio("English", rg), AFTER+25, SAME);    tip(rEn,"Click here to set the user interface to English","Clique aqui para mudar a linguagem para ingl�s");  
      add(rPt = new Radio("Portugu�s", rg), AFTER+25, SAME);  tip(rPt,"Click here to set the user interface to Portuguese","Clique aqui para mudar a linguagem para portugu�s");
      rg.setSelectedIndex(isEn ? 0 : 1,false);
      add(new Label(x("Class name: ","Nome da classe: ")),LEFT,AFTER+25);
      add(edclass = new Edit(),AFTER,SAME); tip(edclass, "Type the full class name of the class that extends MainWindow. Don't forget to include the package.", "Digite o nome (com o pacote) da classe que estende MainWindow"); 
      add(new Label(x("Key: ","Chave: ")),LEFT,AFTER+25);
      add(edkey = new Edit(),AFTER,SAME);  tip(edkey, "Type the registration key that you received by email", "Digite a chave que voc� recebeu por email");
      
      lstatus = new Label("",CENTER);
      lstatus.setBackForeColors(COLOR,0);
      lstatus.setFont(font.asBold());
      lstatus.autoSplit = true;
      add(lstatus,LEFT,BOTTOM,FILL,fmH*2);
      
      tc = new TabbedContainer(new String[]{x("Run","Executar"),x("Deploy","Empacotar")});
      tc.setBackColor(COLOR);
      tc.allSameWidth = true;
      add(tc, LEFT,AFTER+25,FILL,FIT,edkey);
      tc.setContainer(0,new RunContainer());
      tc.setContainer(1,new DepContainer());
   }
   
   private void tip(Control c, String en, String pt)
   {
      final String s = x(en,pt);
      c.addMouseListener(new MouseListener()
      {
         public void mouseWheel(MouseEvent e) {}
         public void mouseMove(MouseEvent e)  {}         
         public void mouseOut(MouseEvent e)   {lstatus.setText("");}
         public void mouseIn(MouseEvent e)    {lstatus.setText(s);}
      });
   }

   public void onEvent(Event e)
   {
      switch (e.type)
      {
         case ControlEvent.PRESSED:
            if (e.target == rEn || e.target == rPt)
            {
               isEn = e.target == rEn;
               initUI();
            }
            break;
      }
   }
   
   class RunContainer extends ScrollContainer
   {
      Edit edX, edY, edW, edH, edFS, edCmd;
      ComboBox cbSc, cbBpp;
      Switch swSc;
      Radio rdA, rdI, rd32, rdCE;
      Check chM;
      
      public RunContainer()
      {
         super(false,true);
      }
      public void initUI()
      {
         add(new Label(x("Screen settings","Configura��es da janela")),LEFT,TOP);
         RadioGroupController rg = new RadioGroupController();
         add(new Label(x("Position - X: ","Posi��o - X: ")),LEFT,AFTER+25);
         add(edX = new Edit("99999"),AFTER,SAME); tip(edX, "Type the X position for the window. Leave blank to let the system set it", "Digite a posi��o X para a janela. Deixe em branco pro sistema posicionar");
         add(new Label("Y: "),AFTER+50,SAME);
         add(edY = new Edit("99999"),AFTER,SAME); tip(edY, "Type the Y position for the window. Leave blank to let the system set it", "Digite a posi��o Y para a janela. Deixe em branco pro sistema posicionar");
         
         add(new Label(x("Select one to populate the edits below", "Selecione um para popular os edits abaixo")),LEFT,AFTER+25);
         add(rd32 = new Radio("Win32",rg),LEFT,AFTER);     tip(rd32, "Width=240, height=320, bpp=24", "Largura=240, altura=320, bpp=24");
         add(rdCE = new Radio("WinCE",rg),AFTER+50,SAME);  tip(rdCE, "Width=240, height=320, bpp=8", "Largura=240, altura=320, bpp=8");
         add(rdA = new Radio("Android",rg),AFTER+50,SAME); tip(rdA,  "Width=320, height=480, bpp=24", "Largura=320, altura=480, bpp=24");
         add(rdI = new Radio("iOS",rg),AFTER+50,SAME);     tip(rdI,  "Width=640, height=960, bpp=24, scale=0.75", "Largura=640, altura=960, bpp=24, escala=0.75");
         add(new Label(x("Width: ","Largura: ")),LEFT,AFTER+25);
         add(edW = new Edit("99999"),AFTER,SAME);   tip(edW, "Type the width for the window", "Digite a largura da janela");
         add(new Label(x("Height: ","Altura: ")),AFTER+50,SAME); 
         add(edH = new Edit("99999"),AFTER,SAME);   tip(edH, "Type the height for the window", "Digite a altura da janela");
         add(new Label(x("BitsPerPixel: ","BitsPorPixel: ")),LEFT,AFTER+25);
         add(cbBpp = new ComboBox(new String[]{"8","16","32"}),AFTER,SAME); tip(cbBpp, "Select the bits per pixel to be used in the Window. All platforms uses 24bpp, except WinCE, which uses 8bpp", "Selecione os bits por pixel usado na janela. Todas as plataformas, menos WinCE, usam 24bpp");
         add(new Label(x("Font size: ","Tam da letra: ")),LEFT,AFTER+25);
         add(edFS = new Edit("99999"),AFTER,SAME);  tip(edFS, "Type the font size, which is the most important attribute after the window's size", "Digite o tamanho da fonte, que � o atributo mais importante ap�s o tamanho da janela");
         add(new Label(x("Scale: ","Escala: ")),LEFT,AFTER+25);
         add(cbSc = new ComboBox(new String[]{"0.25","0.5","0.75","1","2","4","6","8"}),AFTER,SAME); tip(cbSc, "Select the scale to apply in the Window. Useful when you test screen sizes above your monitor's resolution", "Selecione a escala pra aplicar na janela. �til quando usar tamanho de janela maior que a resolu��o do seu monitor");
         cbSc.setSelectedItem("1");
         
         swSc = new Switch(true); tip(swSc, "Scale slow has a good appearance, while fast scale has a worst appearance", "Escala lenta tem boa apar�ncia, e escala devagar tem uma pior apar�ncia");
         swSc.textBackOn = x(" fast","r�pida ");
         swSc.textBackOff = x(" slow","lenta ");
         add(swSc, AFTER+50,SAME);
         
         add(new Label(x("Miscelaneous","Miscel�nia")),LEFT,AFTER+50);
         add(new Label(x("Command line to pass to application: ","Linha de comandos para passar � aplica��o")),LEFT,AFTER+25);
         add(edCmd = new Edit(),LEFT,AFTER);   tip(edCmd, "You can pass extra arguments to application and retrieve it using MainWindow.getCommandLine()", "Voc� pode passar argumentos extras pra aplica��o e recuper�-los usando MainWindow.getCommandLine()");
         add(chM = new Check(x("Show mouse position","Mostrar posi��o do mouse")),LEFT,AFTER+25); tip(chM, "Show the mouse position on window's title area", "Mostra a posi��o do mouse no t�tulo da janela");
      }
   }
   
   class DepContainer extends ScrollContainer
   {
      public DepContainer()
      {
         super(false,true);
      }
      public void initUI()
      {
      }
   }
}
