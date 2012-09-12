/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/



#include "tcvm.h"

void updateScreen(Context currentContext);
void vmSetAutoOff(bool enable); // vm_c.h
static void pumpEvent(Context currentContext);

// Platform-specific code
#if defined(PALMOS)
 #include "palm/event_c.h"
#elif defined(WINCE) || defined(WIN32)
 #include "win/event_c.h"
#elif defined(darwin)
 #include "darwin/event_c.h"
#elif defined(__SYMBIAN32__)
 #include "symbian/event_c.h"
#elif defined(ANDROID)
 #include "android/event_c.h"
#else
 #include "linux/event_c.h"
#endif
//

static Method onMinimize;
static Method onRestore;

static void checkTimer(Context currentContext)
{
   if (nextTimerTick != 0)
   {
      int32 now = getTimeStamp();

      if (now >= nextTimerTick && _onTimerTick && _onTimerTick->code)
      {
         nextTimerTick = 0;
         executeMethod(currentContext, _onTimerTick, mainClass, true);
      }
   }
}

#ifdef PALMOS
#define INITIAL_TICK 500
#else
#define INITIAL_TICK 5000
#endif

#ifdef ENABLE_DEMO
static int32 demoTick = INITIAL_TICK;
#endif
extern bool wokeUp();
#ifdef WINCE
static int32 nextAutoOffTick;
#endif

static void pumpEvent(Context currentContext)
{
   if (currentContext != mainContext) // only pump events on the mainContext
      goto sleep;                     
#ifdef WINCE 
   {
    int32 ts;
	if (oldAutoOffValue != 0 && (ts=getTimeStamp()) >= nextAutoOffTick) // guich@tc120_67
	{
      keybd_event(VK_LBUTTON, 0, KEYEVENTF_SILENT, 0);
      keybd_event(VK_LBUTTON, 0, KEYEVENTF_KEYUP | KEYEVENTF_SILENT, 0);
      SystemIdleTimerReset();
      nextAutoOffTick = ts + 15 * 1000; // get next time
	}
   }
#endif
#if defined(PALMOS) || (defined(WINCE) && _WIN32_WCE >= 300)
   if (wokeUp())
      postEvent(currentContext, KEYEVENT_SPECIALKEY_PRESS, SK_POWER_ON, 0,0,-1);
#endif
#ifndef PALMOS // guich@tc100b4: Palm OS requires SysEventGet to be called always, otherwise incoming phone calls will not work
   if (privateIsEventAvailable())
#endif
      privatePumpEvent(currentContext);
   checkTimer(currentContext);
#ifdef ENABLE_DEMO
   if (--demoTick == 0) {demoTick = INITIAL_TICK; updateDemoTime();}
#endif
sleep:
   Sleep(1); // avoid 100% cpu
}

bool isEventAvailable()
{
   return privateIsEventAvailable();
}

void pumpEvents(Context currentContext)
{
   if (keepRunning)
      do
      {            
         pumpEvent(currentContext);
      } while (isEventAvailable() && keepRunning);

   if (!keepRunning && !appExitThrown)
   {
      appExitThrown = true;
      throwException(currentContext, AppExitException,null);
   }
}

void mainEventLoop(Context currentContext)
{
   // now that the Main class was load, it's safe to get these methods
   _postEvent = getMethod(OBJ_CLASS(mainClass), true, "_postEvent", 6, J_INT, J_INT, J_INT, J_INT, J_INT, J_INT);
   _onTimerTick = getMethod(OBJ_CLASS(mainClass), true, "_onTimerTick", 1, J_BOOLEAN);
   onMinimize = getMethod(OBJ_CLASS(mainClass), true, "onMinimize", 0);
   onRestore = getMethod(OBJ_CLASS(mainClass), true, "onRestore", 0);

   if (_onTimerTick == null || _postEvent == null || onMinimize == null || onRestore == null) // unlikely to occur...
      throwException(currentContext, RuntimeException, "Can't find event methods.");
#ifndef ANDROID      
   else
      while (keepRunning)
         pumpEvent(currentContext);
#endif         
}

void postEvent(Context currentContext, TotalCrossUiEvent type, int32 key, int32 x, int32 y, int32 mods)
{
   if (mainClass != null && _postEvent != null)
   {                       
      executeMethod(currentContext, _postEvent, mainClass, (int32)type, key, x, y, keyGetPortableModifiers(mods), getTimeStamp()); // events are always posted to the main execution line
#ifndef ANDROID      
      updateScreen(currentContext); // update the screen after the event was called, otherwise ListBox selection will not work
#endif      
   }
}

void postOnMinimizeOrRestore(bool isMinimized)
{
   if (mainClass != null)
      executeMethod(lifeContext, (isMinimized ? onMinimize : onRestore), mainClass); // events are always posted to the main execution line
}

bool initEvent()
{
   return privateInitEvent();
}

void destroyEvent()
{
#ifdef ENABLE_DEMO
   updateDemoTime();
#endif
   if (oldAutoOffValue != 0) // if user changed the state, restore the old value of the auto-off timer
      vmSetAutoOff(true);
   privateDestroyEvent();
   freeArray(interceptedSpecialKeys);
}