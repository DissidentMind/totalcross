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



#include "tcvm.h"

#if defined (WINCE)
 #include "win/GPS_c.h"
#elif defined (ANDROID)
 #include "android/GPS_c.h"
#endif

//////////////////////////////////////////////////////////////////////////
TC_API void tidgGPS_startGPS(NMParams p) // totalcross/io/device/gps/GPS native private boolean startGPS() throws totalcross.io.IOException;
{
#if defined(WINCE) || defined(ANDROID)
   Err err;
   
   if ((err = nativeStartGPS()) > 0)
#ifdef ANDROID
      throwException(p->currentContext, IOException, err == 1 ? "No environment" : "GPS is disabled");
#else         
      throwExceptionWithCode(p->currentContext, IOException, err);
#endif      
   p->retI = (err == NO_ERROR);
#endif
}
//////////////////////////////////////////////////////////////////////////
TC_API void tidgGPS_updateLocation(NMParams p) // totalcross/io/device/gps/GPS native private int updateLocation();
{
#if defined(WINCE) || defined(ANDROID)
   int32 flags = 0;
   Err err;

   if ((err = nativeUpdateLocation(p->currentContext, p->obj[0], &flags)) > 0)
      throwExceptionWithCode(p->currentContext, IOException, err);
   p->retI = flags;
#endif
}
//////////////////////////////////////////////////////////////////////////
TC_API void tidgGPS_stopGPS(NMParams p) // totalcross/io/device/gps/GPS native private void stopGPS();
{
#if defined(WINCE) || defined(ANDROID)
   nativeStopGPS();
#endif
}

#ifdef ENABLE_TEST_SUITE
//#include "GPS_test.h"
#endif
