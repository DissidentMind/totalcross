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

// $Id: tcexception.c,v 1.72 2011-02-17 18:05:37 guich Exp $

#include "tcvm.h"

#define STRING_BUILDER_MSG "\nTotalCross is not compatible\nwith Java 1.5, so you must\ncompile your application passing\n-source 1.2 -target 1.1\nto instruct JavaC to replace\nStringBuilder by StringBuffer."

TC_API void throwException(Context currentContext, Throwable t, CharP message, ...) // throw an exception based on the Throwable enumeration
{
   Object exception;
   // note: code is duplicated here because the ... cannot be passed along different routines
   CharP exceptionClassName = throwableAsCharP[(int32)t];

   if (currentContext->thrownException != null) // do not overwrite a first exception, maybe the second one is caused by the first.
      return;

   exception = t == OutOfMemoryError ? null : createObject(currentContext, exceptionClassName); // guich@tc110_40: use the already-created OOME object
   if (exception == null)
   {
      currentContext->thrownException = exception = currentContext->OutOfMemoryErrorObj;
      *Throwable_trace(exception) = null; // let a new trace be generated
   }
   else
   {
      currentContext->thrownException = exception;
      setObjectLock(currentContext->thrownException, UNLOCKED);
   }

   if (message && strEq(exceptionClassName, throwableAsCharP[ClassNotFoundException]) && strEq(message,"java.lang.StringBuilder"))
   {
      *Throwable_msg(exception) = createStringObjectFromCharP(currentContext, STRING_BUILDER_MSG,-1);
      setObjectLock(*Throwable_msg(exception), UNLOCKED);
   }
   else
   if (message)
   {
      va_list args;
      va_start(args, message);
      vsprintf(currentContext->exmsg, message, args);
      va_end(args);
      *Throwable_msg(exception) = createStringObjectFromCharP(currentContext, currentContext->exmsg,-1);
      setObjectLock(*Throwable_msg(exception), UNLOCKED);
   }
   fillStackTrace(currentContext, exception, -1, currentContext->callStack);
}

TC_API Object createException(Context currentContext, Throwable t, bool fillStack, CharP message, ...)
{
   Object exception;

   if (currentContext->thrownException != null)
      return currentContext->thrownException;

#ifdef ENABLE_TRACE
   if (traceOn) debug("creating exception %s\n",throwableAsCharP[(int32)t]);
#endif

   exception = createObject(currentContext, throwableAsCharP[(int32)t]);
   if (exception == null)
   {
      currentContext->thrownException = exception = currentContext->OutOfMemoryErrorObj;
      *Throwable_trace(exception) = null; // let a new trace be generated
   }
   else
   {
      currentContext->thrownException = exception;
      setObjectLock(currentContext->thrownException, UNLOCKED);
   }
   if (message && t == ClassNotFoundException && strEq(message,"java.lang.StringBuilder"))
   {
      *Throwable_msg(exception) = createStringObjectFromCharP(currentContext, STRING_BUILDER_MSG,-1);
      setObjectLock(*Throwable_msg(exception), UNLOCKED);
   }
   else
   if (message)
   {
      va_list args;
      va_start(args, message);
      vsprintf(currentContext->exmsg, message, args);
      va_end(args);
      *Throwable_msg(exception) = createStringObjectFromCharP(currentContext, currentContext->exmsg,-1);
      setObjectLock(*Throwable_msg(exception), UNLOCKED);
   }
   if (fillStack) fillStackTrace(currentContext, exception, -1, currentContext->callStack);
   return exception;
}

TC_API void throwExceptionNamed(Context currentContext, CharP exceptionClassName, CharP message, ...) // throw an exception
{
   Object exception;

   if (currentContext->thrownException != null) // do not overwrite a first exception, maybe the second one is caused by the first.
      return;

   exception = createObject(currentContext, exceptionClassName);

   if (exception == null)
   {
      currentContext->thrownException = exception = currentContext->OutOfMemoryErrorObj;
      *Throwable_trace(exception) = null; // let a new trace be generated
   }
   else
   {
      currentContext->thrownException = exception;
      setObjectLock(currentContext->thrownException, UNLOCKED);
   }
   if (message && strEq(exceptionClassName, throwableAsCharP[ClassNotFoundException]) && strEq(message,"java.lang.StringBuilder"))
   {
      *Throwable_msg(exception) = createStringObjectFromCharP(currentContext, STRING_BUILDER_MSG,-1);
      setObjectLock(*Throwable_msg(exception), UNLOCKED);
   }
   else
   if (message)
   {
      va_list args;
      va_start(args, message);
      vsprintf(currentContext->exmsg, message, args);
      va_end(args);
      *Throwable_msg(exception) = createStringObjectFromCharP(currentContext, currentContext->exmsg,-1);
      setObjectLock(*Throwable_msg(exception), UNLOCKED);
   }
   fillStackTrace(currentContext, exception, -1, currentContext->callStack);
}

TC_API void throwExceptionWithCode(Context currentContext, Throwable t, int32 errorCode)
{
   CharP text;
   char errMsg[256];

   if (currentContext->thrownException != null) // do not overwrite a first exception, maybe the second one is caused by the first.
      return;

   text = getErrorMessage(errorCode, errMsg, sizeof(errMsg)-1);
   if (text != null)
      createException(currentContext, t, true, "Error Code: %d - %s", errorCode, text);
   else
      createException(currentContext, t, true, errorCode < -10000 ? "Error Code: %X" : "Error Code: %d", errorCode);
}

TC_API void throwIllegalArgumentExceptionI(Context currentContext, CharP argName, int32 illegalValue)
{
   char msg[128];
   sprintf(msg, "Invalid value '%d' for argument '%s'", illegalValue, argName);
   throwException(currentContext, IllegalArgumentException, msg);
}

TC_API void throwIllegalArgumentException(Context currentContext, CharP argName)
{
   char msg[128];
   sprintf(msg, "Invalid value for argument '%s'", argName);
   throwException(currentContext, IllegalArgumentException, msg);
}

TC_API void throwIllegalArgumentIOException(Context currentContext, CharP argName, CharP argValue)
{
   char msg[128];
   if (argValue != null)
      sprintf(msg, "Invalid value for argument '%s': %s", argName, argValue);
   else
      sprintf(msg, "Invalid value for argument '%s'", argName);
   throwException(currentContext, IllegalArgumentIOException, msg);
}

TC_API void throwFileNotFoundException(Context currentContext, TCHARP path)
{
   char msg[MAX_PATHNAME];
   xstrcpy(msg, "File not found: ");
   TCHARP2CharPBuf(path, msg + xstrlen(msg));
   //sprintf(msg, "File not found: %s", path);
   throwException(currentContext, FileNotFoundException, msg);
}

TC_API void throwNullArgumentException(Context currentContext, CharP argName)
{
   char msg[128];
   sprintf(msg, "Argument '%s' cannot have a null value", argName);
   throwException(currentContext, NullPointerException, msg);
}

static CharP dumpMethodInfo(CharP c, Method m, int32 line, CharP end) // waba.applet.Applet.registerMainWindow(Applet.java:441)
{
   CharP s,b;
   // totalcross.Launcher
   s = m->class_->name;
   while (c < end && *s) *c++ = *s++;
   // .
   if (c < end) *c++ = '.';
   // registerMainWindow
   s = m->name;
   if (s)
      while (c < end && *s) *c++ = *s++;
   if ((c+6) < end && line >= 0)
   {
      IntBuf ib;
      b = int2str(line, ib);
      *c++ = ' ';
      while (*b) *c++ = *b++;
   }
   // \n
   if (c < end) *c++ = '\n';
   return c;
}

int32 locateLine(Method m, int32 pc)
{
   if (pc >= 0)
   {
      int32 i = ARRAYLENV(m->lineNumberLine);
      UInt16Array lines = m->lineNumberStartPC + i - 1;
      for (; --i >= 0; lines--)
         if (*lines <= pc) // guich@tc100b5_32: changed < to <=
            return m->lineNumberLine[i];
   }
   return -1;
}
void fillStackTrace(Context currentContext, Object exception, int32 pc0, VoidPArray callStack)
{
   Method m=null;
   int32 line;
   char *c=currentContext->exmsg;
   bool first = true;
   Code oldpc;

   while (callStack > currentContext->callStackStart)
   {
      callStack -= 2;
      //int2hex((int32)callStack, 6, c); c += 6; *c++ = ' '; - used when debugging
      m = (Method)callStack[0];
      oldpc = (Code)callStack[1];
      line = (m->lineNumberLine != null) ? locateLine(m, first ? pc0 : ((int32)(oldpc - m->code))) : -1;
      c = dumpMethodInfo(c, m, line, currentContext->exmsg + sizeof(currentContext->exmsg) - 2);
      first = false;
   }
   *c = 0;
   if (exception != null)
   {
      if (c != currentContext->exmsg) // was something filled in?
      {
         *Throwable_trace(exception) = createStringObjectFromCharP(currentContext, currentContext->exmsg, (int32)(c-currentContext->exmsg));
         if (*Throwable_trace(exception))
            setObjectLock(*Throwable_trace(exception), UNLOCKED);
         else
            debug(currentContext != gcContext ? "Not enough memory to create the stack trace string. Dumping to here:\n%s" : "Exception thrown in finalize:\n%s", currentContext->exmsg); // guich@tc126_63
      }
      else
         *Throwable_trace(exception) = null; // the trace may not be null if we're reusing OutOfMemoryErrorObj
   }
}

void printStackTrace(Context currentContext)
{
   fillStackTrace(currentContext, null, -1, currentContext->callStack); 
   debug(currentContext->exmsg);
}
void showUnhandledException(Context context, bool useAlert)
{
   Object o;
   CharP msg=null, throwableTrace=null;
   o = *Throwable_msg(context->thrownException);
   if (o) msg = String2CharP(o);
   o = *Throwable_trace(context->thrownException);
   if (o && String_charsStart(o))
      throwableTrace = String2CharP(o);
   if (useAlert)
      alert("Unhandled exception:\n%s:\n %s\n\nStack trace:\n%s\nAborting program.", OBJ_CLASS(context->thrownException)->name, msg==null?"":msg, throwableTrace==null?"":throwableTrace);
   debug("Unhandled exception:\n%s:\n %s\n\nStack trace:\n%s\nAborting %s.", OBJ_CLASS(context->thrownException)->name, msg==null?"":msg, throwableTrace==null?"":throwableTrace,useAlert?"program":"thread"); // always dump to the console
   xfree(msg);
   xfree(throwableTrace);

}
void initException()
{
   throwableAsCharP[ArithmeticException           ] = "java.lang.ArithmeticException";
   throwableAsCharP[ArrayIndexOutOfBoundsException] = "java.lang.ArrayIndexOutOfBoundsException";
   throwableAsCharP[ArrayStoreException           ] = "java.lang.ArrayStoreException";
   throwableAsCharP[ClassCastException            ] = "java.lang.ClassCastException";
   throwableAsCharP[ClassNotFoundException        ] = "java.lang.ClassNotFoundException";
   throwableAsCharP[ErrorClass                    ] = "java.lang.Error";
   throwableAsCharP[ExceptionClass                ] = "java.lang.Exception";
   throwableAsCharP[IllegalAccessException        ] = "java.lang.IllegalAccessException";
   throwableAsCharP[IllegalArgumentException      ] = "java.lang.IllegalArgumentException";
   throwableAsCharP[ImageException                ] = "java.lang.ImageException";
   throwableAsCharP[IndexOutOfBoundsException     ] = "java.lang.IndexOutOfBoundsException";
   throwableAsCharP[InstantiationException        ] = "java.lang.InstantiationException";
   throwableAsCharP[NoSuchFieldError              ] = "java.lang.NoSuchFieldError";
   throwableAsCharP[NoSuchMethodError             ] = "java.lang.NoSuchMethodError";
   throwableAsCharP[NullPointerException          ] = "java.lang.NullPointerException";
   throwableAsCharP[OutOfMemoryError              ] = "java.lang.OutOfMemoryError";
   throwableAsCharP[RuntimeException              ] = "java.lang.RuntimeException";
   throwableAsCharP[IOException                   ] = "totalcross.io.IOException";
   throwableAsCharP[FileNotFoundException         ] = "totalcross.io.FileNotFoundException";
   throwableAsCharP[IllegalArgumentIOException    ] = "totalcross.io.IllegalArgumentIOException";
   throwableAsCharP[UnknownHostException          ] = "totalcross.net.UnknownHostException";
   throwableAsCharP[SocketTimeoutException        ] = "totalcross.net.SocketTimeoutException";
   throwableAsCharP[ZipException                  ] = "totalcross.util.zip.ZipException";
   throwableAsCharP[AppExitException              ] = "totalcross.sys.AppExitException";
   throwableAsCharP[InvalidNumberException        ] = "totalcross.sys.InvalidNumberException";
   throwableAsCharP[ElementNotFoundException      ] = "totalcross.util.ElementNotFoundException";
   throwableAsCharP[CryptoException               ] = "totalcross.crypto.CryptoException";
}
