/********************************************************************************* *  TotalCross Software Development Kit                                          * *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      * *  All Rights Reserved                                                          * *                                                                               * *  This library and virtual machine is distributed in the hope that it will     * *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    * *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         * *                                                                               * *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    * *  A copy of this license is located in file license.txt at the root of this    * *  SDK or can be downloaded here:                                               * *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     * *                                                                               * *********************************************************************************/package tc.samples.api.io.device;import tc.samples.api.*;import totalcross.io.device.*;import totalcross.sys.*;import totalcross.ui.*;import totalcross.ui.event.*;public class PortConnectorSample extends BaseContainer{   PushButtonGroup portSelect, operationSelect;   Button sendButton;   Button receiveButton;   Check flow, endRead;   PortConnector port;   byte readBuf[];   String portNames[] = {"Serial", "IrCOMM", "SIR", "USB", "Bluetooth"};   String operationNames[] = {"Send", "Receive"};   int ports[];   int helloCount = 1;   StringBuffer sb = new StringBuffer(100);   StringBuffer sb2 = new StringBuffer(100);   public void initUI()   {      super.initUI();      if (!Settings.platform.equals(Settings.WIN32) && !Settings.onJavaSE)      {         add(new Label("This sample works only on Win32"),CENTER,CENTER);         return;      }      readBuf = new byte[100];      ports = new int[] {PortConnector.DEFAULT, PortConnector.IRCOMM, PortConnector.SIR, PortConnector.USB, PortConnector.BLUETOOTH};      portSelect = new PushButtonGroup(portNames, false, -1, -1, 6, 2, true, PushButtonGroup.NORMAL);      add(portSelect);      portSelect.setRect(CENTER, TOP + 2, PREFERRED, PREFERRED + 4);      operationSelect = new PushButtonGroup(operationNames, false, -1, 4, 20, 1, true, PushButtonGroup.BUTTON);      add(operationSelect);      operationSelect.setRect(CENTER + 4, AFTER + 2, PREFERRED, PREFERRED + 4);      add(flow = new Check("Use flow control"), LEFT, AFTER + 2);      add(endRead = new Check("End write on check timeout"), LEFT, AFTER + 2);      endRead.setChecked(true);      addLog(LEFT, AFTER, FILL, FILL,null);   }   private int lastSelectedPort = -1; // fabio: PushButtonGroup.NORMAL is sending a pressed event when a pressed button is clicked. This should be temporary.   public void onEvent(Event event)   {      switch (event.type)      {         case ControlEvent.PRESSED:         {            if (event.target == portSelect)            {               int selectedPort = portSelect.getSelectedIndex();               if (selectedPort == -1 || selectedPort != lastSelectedPort)               {                  if (port != null)                  {                     try                     {                        port.close();                     }                     catch (totalcross.io.IOException e)                     {                        log("Exception in port.close: " + e.getMessage());                        e.printStackTrace();                     }                     finally                     {                        port = null;                     }                  }                  if (selectedPort != -1)                  {                     log("Opening port " + portNames[selectedPort]);                     try                     {                        port = new PortConnector(ports[selectedPort], 9600);                        flow.setEnabled(selectedPort != 2);                        if (selectedPort == 2)                           log("SIR has no flow control");                        port.stopWriteCheckOnTimeout = endRead.isChecked();                        port.setFlowControl(flow.isChecked());                        port.readTimeout = 2000;                        lastSelectedPort = selectedPort;                     }                     catch (totalcross.io.IOException e)                     {                        port = null;                        portSelect.setSelectedIndex(-1);                        log("Exception in PC constructor: " + e.getMessage());                        e.printStackTrace();                     }                  }               }            }            else if (event.target == operationSelect)            {               if (port == null)               {                  log("No connection available for this operation");               }               else               {                  switch (operationSelect.getSelectedIndex())                  {                     case 0: // send button                     {                        String s = "Hello " + helloCount++;                        byte[] buf = s.getBytes();                        int n;                        try                        {                           n = port.writeBytes(buf, 0, buf.length);                           if (n == buf.length)                              log("Sent " + s);                           else                              log("Sent only " + new String(buf, 0, n));                        }                        catch (totalcross.io.IOException e)                        {                           log(">>>Write failed: " + e.getMessage());                           e.printStackTrace();                        }                     } break;                     case 1: // receive button                     {                        int count;                        try                        {                           count = port.readBytes(readBuf, 0, 100);                           if (count == 0)                           {                              log("No data received");                           }                           else                           {                              int b;                              sb.setLength(0);                              sb2.setLength(0);                              for (int i = 0; i < count; i++)                              {                                 b = readBuf[i] & 0xFF;                                 if (b != 255)                                 {                                    sb.append((char) b);                                    sb2.append(b).append(' ');                                 }                              }                              log("Asc " + sb.toString());                              log("Dec " + sb2.toString());                           }                        }                        catch (totalcross.io.IOException e)                        {                           log(">>>Read Failed: " + e.getMessage());                           e.printStackTrace();                        }                     } break;                  }               }            }         } break;      }   }}