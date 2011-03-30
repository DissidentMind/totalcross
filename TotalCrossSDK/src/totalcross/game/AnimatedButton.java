/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
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

// $Id: AnimatedButton.java,v 1.18 2011-01-04 13:19:17 guich Exp $

package totalcross.game;

import totalcross.sys.*;
import totalcross.ui.image.*;
import totalcross.ui.event.*;

/**
 * An animated button control.
 * <pre>
 * This control displays an animated button which can take 'S' different states
 * and each state is fades in or out in 'F' frames. 'S' and 'F' represent the
 * two first constructor arguments. The frames of this special animation have to be
 * ordered to be supported by this class. The states are numbered from 0 to 'S'-1
 * and the frames order is the following depending on the layoutType value:<br>
 * FADE_OUT_LAYOUT    :  S0F0,S0F1,S0F2,S1F0,S1F1,S1F2,S2F0,S2F1,S2F2<br>
 * FADE_IN_LAYOUT     :  S0F2,S0F1,S0F0,S1F2,S1F1,S1F0,S2F2,S2F1,S2F0<br>
 * FADE_OUT_IN_LAYOUT :  S0F0,S0F1,S1F1,S1F0,S1F1,S2F1,S2F0,S2F1,S0F1<br>
 * where S stands for state, F for frame and where S0F0, S1F0 and S2F0
 * are the full states and the others are transition frames.
 * Open the 'onOff.bmp' in the Scape game sample and you will understand ;-)
 * </pre>
 * @author Frank Diebolt
 * @author Guilherme Campos Hazan
 * @version 1.1
 */
public class AnimatedButton extends Animation
{
  /**
   * Defines the frames animation order. In the case of a "S" states button of "F"
   * frames per state, FADE_OUT_LAYOUT means that the frames are a "S" set of
   * "F" frames that are fading out the state, that means the first frame of
   * of each set is the full state image.
   * In the FADE_IN_LAYOUT layout, it's the opposite, namely the last frame of
   * each set represents the state ending position.
   * Finaly the FADE_OUT_IN_LAYOUT is a mix of the two others, because inter-frames
   * represent successively fading out from one state to fading in to next state.
   * See the game tutorial for samples.
   */
  public static final int FADE_OUT_LAYOUT   = 0;

  /** Frames fading in mode.  
   * @see #FADE_OUT_LAYOUT */
  public static final int FADE_IN_LAYOUT    = 1;

  /** Frames fading out then fading in mode. 
   * @see #FADE_OUT_LAYOUT */
  public static final int FADE_OUT_IN_LAYOUT = 2;

  /** current animated button state */
  protected int state;

  protected int layoutType;
  protected int fadeInState;
  protected int framesPerState;
  protected int maxStates;
  protected int statesIndexes[];

  private final static int IDLE = -1;

  /**
   * Animated button constructor.
   * @param frames button different states frames in multi-frame BMP format.
   * @param states number of states of the button.
   * @param framesPerState number of frames for each state.
   * @param layoutType FADE_OUT_LAYOUT, FADE_IN_LAYOUT or FADE_OUT_IN_LAYOUT.
   * @param transColor the transparency color
   * @param framePeriod delay in millisecconds between two frames
   * @see #FADE_OUT_LAYOUT
   * @see #FADE_IN_LAYOUT
   * @see #FADE_OUT_IN_LAYOUT
   */
  public AnimatedButton(Image frames,int states,int framesPerState,int layoutType, int transColor,int framePeriod) // fdie@341_2
  {
    super(frames,states * framesPerState,transColor,framePeriod);

    this.framesPerState = framesPerState;
    this.layoutType = layoutType;
    this.maxStates = states;
    statesIndexes = new int[states];
    for (int s=0; s<states; s++)
      statesIndexes[s] = (layoutType==FADE_IN_LAYOUT) ? ((s+1)*framesPerState)-1 : s*framesPerState;

    curFrame = statesIndexes[state=0];
    fadeInState = IDLE;
    eventsMask = eventFinish;
  }

  /**
   * Set the animated button state.
   * @param state value between 0 and states-1
   */
  public void setState(int state)
  {
    if (isPlaying)
    {
      stop();
      fadeInState=IDLE;
    }

    this.state=state;
    curFrame=statesIndexes[state];
    repaintNow();
  }

  /**
   * Get the animated button state.
   * @return value between 0 and states-1
   */
  public int getState()
  {
    return state;
  }

  /**
   * Animated button event handler.
   * @param event
   */
  public void onEvent(Event event)
  {
     switch (event.type)
     {
        case PenEvent.PEN_DOWN:
           if (fadeInState==IDLE)
              inc(((PenEvent)event).x >= (width>>1));
           break;
        case KeyEvent.SPECIAL_KEY_PRESS:
          if (fadeInState==IDLE)
          {
            int key = ((KeyEvent)event).key;
            if (key == SpecialKeys.ACTION || key == SpecialKeys.ENTER)
              inc(true);
          }
          break;
        case AnimationEvent.FINISH:
         if (fadeInState!=IDLE)
         {
           state=fadeInState;
           fadeInState=IDLE;
           if (layoutType==FADE_OUT_IN_LAYOUT)
           {
             postPressedEvent();
             return;
           }
           int dest=statesIndexes[state];
           if (layoutType!=FADE_IN_LAYOUT)
              start(dest+framesPerState-1,dest,-1,1);
           else
              start(dest-framesPerState+1,dest,1,1);
         }
         break;
        case ControlEvent.PRESSED:
           postPressedEvent();
          break;
        default: // pass timer events to the parent
           super.onEvent(event);
     }
  }

  /**
   * Increase/decrease the animated button state.
   * @param up boolean with a true value to increase the value, decrease otherwise
   */
  protected void inc(boolean up)
  {
    int dir=up ? 1:-1;
    int dest=(state+maxStates+dir) % maxStates;
    int src=statesIndexes[state];
    if (layoutType==FADE_OUT_IN_LAYOUT)
       start(src,statesIndexes[dest],dir,1);
    else
    if (layoutType!=FADE_IN_LAYOUT)
       start(src,src+framesPerState-1,1,1);
    else
       start(src,src-framesPerState+1,-1,1);
    fadeInState=dest;
  }
}