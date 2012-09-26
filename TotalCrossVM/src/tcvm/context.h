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



#ifndef CONTEXT_H
#define CONTEXT_H

#define STARTING_REGI_SIZE  2000  // * 4 = 8k
#define STARTING_REG64_SIZE 1000  // * 8 = 8k
#define STARTING_REGO_SIZE  2000  // * 4 = 8k for a 32-bit architecture
#define STARTING_STACK_SIZE 2000  // * 4 = 8k - each method call uses 2 positions

struct TContext
{
   Object thrownException;
   VoidPArray callStack;
   Int32Array  regI;  // start <= x < end
   ObjectArray regO;
   Value64Array reg64;
   Int32Array  regIStart, regIEnd;  // start <= x < end
   ObjectArray regOStart, regOEnd;
   Value64Array reg64Start, reg64End;
   // method stack
   VoidPArray callStackStart, callStackEnd;
   Code code;
   Heap heap;
   ThreadHandle thread; // the thread handle for this thread or null if its the main thread
   Object threadObj;
   TNMParams nmp;

   // global variables that can be changed by the thread
   // tcexception.c
   Object OutOfMemoryErrorObj;

   // tcexception.c
   char exmsg[1024];

   // graphicsprimitives.c
   // in a menu, we use often two colors only, so we cache them
   PixelConv aafroms1[16],aafroms2[16];
   PixelConv aatos1[16],aatos2[16];
   int32 aaFromColor1,aaToColor1,aaTextColor1;
   int32 aaFromColor2,aaToColor2,aaTextColor2;
   bool lastWas1; // used to switch from 1 to 2

   // PalmFont_c.h
   UserFont lastUF;
   Object lastFontObj;

   VoidP litebasePtr; // used by litebase
   VoidP sslPtr; // used by SSL
   VoidP rsaPtr; // used by RSACipher
   int32 sslPtrCount; // number of references to sslPtr.
   int32 rsaPtrCount; // number of references to rsaPtr.

   // Control access to this context in executeMethod
   DECLARE_MUTEX(usageLock);
   ThreadHandle usageOwner;
   int32 usageCount;

   // graphics
   bool fullDirty;
   int32 dirtyX1, dirtyY1, dirtyX2, dirtyY2;

   // IMPORTANT: ALL IFDEFS MUST BE PLACED AT THE END, otherwise, other native libraries that 
   // use this header that do not define the same #defines, will have problems.
   #ifdef ENABLE_TEST_SUITE
   VoidP *callStackForced;
   #endif
   #ifdef ENABLE_TRACE
   int32 ccon,depth;
   char spaces[200];
   #endif
   #ifdef TRACK_USED_OPCODES
   int8 usedOpcodes[256];
   #endif
   #ifdef ANDROID
   #define GL_COORD_COUNT 3000
   GLfloat glcoords[GL_COORD_COUNT*3];
   GLfloat glcolors[GL_COORD_COUNT*4];
   #endif
};

Context newContext(ThreadHandle thread, Object threadObj, bool bigContextSizes); // if bigContextSize is false, use STARTING_xxx_SIZE/10
void deleteContext(Context c, bool destroyThread);

bool contextIncreaseRegI(Context c, int32** r);
bool contextIncreaseRegO(Context c, Object** r);
bool contextIncreaseReg64(Context c, Value64* r);
bool contextIncreaseCallStack(Context c, VoidP** r);

Context initContexts();
void destroyContexts();

TC_API Context getMainContext();
typedef Context (*getMainContextFunc)();

#endif
