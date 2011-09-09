/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/



#ifndef GLOBALS_H
#define GLOBALS_H

// tcclass.c
extern Hashtable htLoadedClasses;
extern ClassArray vLoadedClasses;

// tcexception.c
extern CharP throwableAsCharP[(int32)ThrowableCount];

// event_c.h
extern int32 lastPenX, lastPenY, actionStart;
extern int32 lastW,lastH;
extern int32 ascrHRes,ascrVRes;
#if defined(WIN32)
extern uint8 keyIsDown[256];
extern bool dontPostOnChar;
#elif defined(PALMOS)
extern bool isGETreo650;
extern bool supportsDIA;
#elif defined(ANDROID)
extern jmethodID jeventIsAvailable,jpumpEvents;
extern bool appPaused;
extern int32 deviceFontHeight;
#endif

// GoogleMaps.c
#ifdef ANDROID
extern jmethodID jshowGoogleMaps;
#endif

// startup.c
extern bool traceOn;
extern char commandLine[256];
extern int32 exitCode;
extern bool rebootOnExit;
extern bool destroyingApplication;
extern Object mainClass;  // the instance being executed
extern bool isMainWindow;   // extends MainWindow ?
#if defined PALMOS
extern void *pealLoadLibrary68K, *pealUnloadLibrary68K, *pealGetProcAddress68K;
#elif defined WIN32 || defined linux || defined __SYMBIAN32__
extern TCHAR exeName[MAX_PATHNAME];
#elif defined(ANDROID)
JavaVM* androidJVM;
extern jobject applicationObj, applicationContext;
extern jclass applicationClass;
extern jfieldID jshowingAlert,jhardwareKeyboardIsVisible;
extern jfieldID jsipVisible;
extern jmethodID jgetHeight;
#endif

// graphicsprimitives.c
extern uint8 *lookupR, *lookupG, *lookupB, *lookupGray; // on 8 bpp screens
extern int32* controlEnableUpdateScreenPtr;
extern int32* containerNextTransitionEffectPtr;
extern TScreenSurface screen;
#ifdef ANDROID
extern jmethodID jupdateScreen;
#endif
extern Class uiColorsClass;
extern int32* shiftScreenColorP;

// mem.c
extern uint32 maxAvail; // in bytes
extern bool warnOnExit;
extern bool leakCheckingEnabled;
extern VoidPs* createdHeaps;
extern int32 totalAllocated, maxAllocated, allocCount, freeCount;
extern bool showMemoryMessagesAtExit;

// PalmFont_c.h
extern int32 maxFontSize, minFontSize, normalFontSize;
extern FontFile defaultFont;
extern int32 *tabSizeField;
extern Hashtable htUF;
extern VoidPs* openFonts;
extern Heap fontsHeap;

// win/gfx_Graphics_c.h
#ifdef WIN32
extern HWND mainHWnd;
extern bool bSipUp; //flsobral@tc114_50: fixed the SIP keyboard button not being properly displayed on some WinCE devices.
#endif

// Settings.c
extern Class settingsClass;
extern TTCSettings tcSettings;
#if defined (WINCE)
extern TVirtualKeyboardSettings vkSettings;
#endif

// demo.c
#ifdef ANDROID
extern jmethodID jsetElapsed;
#endif

// objectmemorymanager.c
extern bool runningGC,runningFinalizer;
extern ObjectArray freeList; // the array with lists of free objects
extern ObjectArray usedList; // the array with lists of used objects (allocated after the last GC)
extern ObjectArray lockList; // locked objects list
extern uint32 markedAsUsed; // starts as 1
extern uint32 objCreated,skippedGC,objLocked; // a few counters
extern int32 lastGC;
extern Heap ommHeap;
extern Heap chunksHeap;
extern Stack objStack;
#if defined(ENABLE_TEST_SUITE)
// The garbage collector tests requires that no objects are created, so we cache the state, then restore it when the test finishes
extern bool canTraverse;
extern ObjectArray freeList2; // the array with lists of free objects
extern ObjectArray usedList2; // the array with lists of used objects (allocated after the last GC)
extern ObjectArray lockList2; // locked objects list
extern uint32 markedAsUsed2; // starts as 1
extern uint32 gcCount2,objCreated2,skippedGC2,objLocked2; // the current gc count
extern Heap ommHeap2,chunksHeap2;
extern Stack objStack2;
#endif

// context.c
extern VoidPs* contexts;
extern Context mainContext,gcContext;

// tcvm.c
extern int32 vmTweaks;
extern bool showKeyCodes;
extern int32 profilerMaxMem;
extern Class lockClass;

// linux/graphicsprimitives.c, linux/event_c.h, darwin/event.m, tcview.m
#if !defined(PALMOS) && !defined(WIN32)
extern void *deviceCtx; // The device context points a structure containing platform specific data that have to handled in platform specific code only, that's why we don't define a structure here insofar some platform specific data can't be defined in plain C (such as SymbianOS C++ classes, iPhone objC data structures, ...) Currently this pointer is mirrored in ScreenSurface in the extension field but this may change sooner or later.
#endif

// utils.c
extern int32 firstTS;

// file.c
#ifdef ANDROID
extern jmethodID jgetSDCardPath;
#endif

// vm.c
#ifdef ANDROID
extern jmethodID jvmFuncI,jvmExec;
#endif

// debug.c
extern CharP debugstr;
extern bool consoleAllocated;
#ifdef ANDROID
extern jmethodID jalert;
#endif

// nativelib.c
extern VoidPs* openNativeLibs;

// tcz.c
extern VoidPs* openTCZs;

// event.c
extern bool appExitThrown;
extern bool keepRunning;
extern bool eventsInitialized;
extern int32 nextTimerTick;
extern bool isDragging;
extern Method _onTimerTick, _postEvent;
extern Int32Array interceptedSpecialKeys;

// Vm_c.h
extern int32 oldAutoOffValue; // if not 0, the device is in NEVER-SLEEP mode, and the old value will be restored when the vm quits

// media_Sound.c
extern TSoundSettings soundSettings;
#ifdef ANDROID
extern jmethodID jtone,jsoundEnable;
#endif

// ConnectionManager.c
extern Class connMgrClass;

// win/Socket_c.h
#ifdef WIN32
extern int32 WSACount;
#endif

// xml/xml_Tokenizer.c
extern bool xmlInitialized;

// ssl_SSL.c
extern Hashtable htSSLSocket;
extern Heap heapSSLSocket;

#ifdef PALMOS
// palm/Socket_c.h, ServerSocket_c.h
extern VoidP gNETLink;
// palm/media_Camera_c.h
extern VoidP gpalmOneCameraLink;

// palm/debug_c.h
extern void *pealAlert68K;

extern const void *gEmulStateP;
extern Call68KFuncType *gCall68KFuncP;
#elif defined ANDROID
extern jmethodID jshowCamera;

// android/GPS_c.h
extern jmethodID jgpsFunc,jcellinfoUpdate;

// android/Dial_c.h
extern jmethodID jdial;
#endif

// tcthread.c
extern int32 threadCount;

// These are set in the application's constructor
extern uint32 applicationId;
extern char applicationIdStr[5];

// These are set when the VM is initialized, and their values are copied into totalcross.sys.Settings.
extern CharP platform; // always a constant
extern char userName[42];
extern char appPath[MAX_PATHNAME];
extern char vmPath[MAX_PATHNAME];
extern char dataPath[MAX_PATHNAME];
extern char mainClassName[MAX_PATHNAME];
extern bool isMotoQ;
extern bool isWindowsMobile;

/***********************   METHODS THAT CAN BE USED BY LIBRARIES TO ACCESS THE GLOBALS  *************************/

#ifdef WIN32
TC_API HWND getMainWindowHandle();
typedef HWND (*getMainWindowHandleFunc)();
#endif

TC_API UInt32 getApplicationId();
typedef UInt32 (*getApplicationIdFunc)();
TC_API CharP getApplicationIdStr();
typedef CharP (*getApplicationIdStrFunc)();
TC_API Object getMainClass();
typedef Object (*getMainClassFunc)();
TC_API CharP getVMPath();
typedef CharP (*getVMPathFunc)();
TC_API CharP getAppPath();
typedef CharP (*getAppPathFunc)();
TC_API CharP getUserName();
typedef CharP (*getUserNameFunc)();

#ifdef WINCE
extern HINSTANCE aygshellDll, coreDll, cellcoreDll;
#endif

#if defined (WIN32)
 extern bool initWinsock();
 extern void closeWinsock();
#endif

bool initGlobals();
void destroyGlobals();

#endif
