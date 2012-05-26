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



void privateScreenChange(int32 w, int32 h)
{
#ifndef WINCE // windows ce already does this for us
   w += GetSystemMetrics(SM_CXFIXEDFRAME)*2;
   h += GetSystemMetrics(SM_CYCAPTION) + GetSystemMetrics(SM_CYFIXEDFRAME)*2;
   MoveWindow(mainHWnd, 0, 0, w, h, TRUE);
#endif
}

#if defined (WINCE)
RECT defaultWorkingArea;
#endif

void restoreTaskbar()
{
#if defined (WINCE)
   HWND hWndTaskBar;

   if (!isWindowsMobile)
   {
      hWndTaskBar = FindWindow(TEXT("HHTaskBar"), TEXT(""));
      ShowWindow(hWndTaskBar, SW_SHOWNORMAL);
      SystemParametersInfo(SPI_SETWORKAREA, 0, &defaultWorkingArea, SPIF_SENDCHANGE); // flsobral@tc113_25: fixed bug - instead of restoring the taskbar, it was changing the default window size to the size of the taskbar.
   }
#endif
}

void getScreenSize(int32 *w, int32* h)
{
#ifdef WINCE
   RECT rect;
   SystemParametersInfo(SPI_GETWORKAREA, 0, &rect, 0);
   *w = GetSystemMetrics(SM_CXSCREEN);
   *h = GetSystemMetrics(SM_CYSCREEN);
   if (!*tcSettings.isFullScreenPtr)
   {
      // given the size of the client area, figure out the window size needed
      *w -= rect.left;
      if (isWindowsMobile)
         *h -= rect.top;
      else
         *h = rect.bottom;
   }
#else // guich@tc130: use the default values for win32
   *w = screen.screenW;
   *h = screen.screenH;
#endif
}

#if !defined(WINCE)
extern int32 defScrX,defScrY,defScrW,defScrH;
#endif

bool graphicsStartup(ScreenSurface screen)
{
   DWORD style;
   int32 width, height;
   RECT rect;
   TCHAR main[MAX_PATHNAME];
   HANDLE instance = GetModuleHandle(0);
   char* dot;
   HDC deviceContext;

   screen->extension = (TScreenSurfaceEx*)xmalloc(sizeof(TScreenSurfaceEx));

   SystemParametersInfo(SPI_GETWORKAREA, 0, &rect, 0);
   style = WS_VISIBLE;
#if !defined (WINCE)
   deviceContext = GetDC(mainHWnd);
   screen->bpp = GetDeviceCaps(deviceContext,BITSPIXEL) * GetDeviceCaps(deviceContext,PLANES);
   DeleteDC(deviceContext);

   width = defScrW == -1 ? 240 : defScrW;
   height = defScrH == -1 ? 320 : defScrH;

#ifdef DESIRED_SCREEN_WIDTH          // tweak to work in IBGE's NETBOOK
   width = DESIRED_SCREEN_WIDTH;
#endif
#ifdef DESIRED_SCREEN_HEIGHT
   height = DESIRED_SCREEN_HEIGHT;
#endif

   rect.left = defScrX == -1 ? 0 : defScrX == -2 ? (rect.left+(rect.right -width )/2) : defScrX;
   rect.top  = defScrY == -1 ? 0 : defScrY == -2 ? (rect.top +(rect.bottom-height)/2) : defScrY;
   rect.bottom = height + GetSystemMetrics(SM_CYCAPTION) + GetSystemMetrics(SM_CYFIXEDFRAME)*2;
   rect.right = width  + GetSystemMetrics(SM_CXFIXEDFRAME)*2;

   style |= WS_SYSMENU | WS_CAPTION | WS_MINIMIZEBOX;
#else
   SystemParametersInfo(SPI_GETWORKAREA, 0, &defaultWorkingArea, 0);
   deviceContext = GetDC(mainHWnd);
   screen->bpp = GetDeviceCaps(deviceContext, BITSPIXEL);

   width = GetSystemMetrics(SM_CXSCREEN);
   height = GetSystemMetrics(SM_CYSCREEN);

   // given the size of the client area, figure out the window size needed
   rect.right = width = width - rect.left;
   if (isWindowsMobile)
      rect.bottom = height = height - rect.top;
   else
      height = rect.bottom;
   AdjustWindowRectEx(&rect, style, FALSE, 0);
#endif
   dot = xstrrchr(mainClassName, '.');
   CharP2TCHARPBuf(dot ? dot+1 : mainClassName, main); // remove the package from the name
   mainHWnd = CreateWindow(exeName, main, style, rect.left, rect.top, rect.right, rect.bottom, NULL, NULL, instance, NULL ); // guich@400_62: move window to desired user position
   if (!mainHWnd)
      return false;

   // store the x, y, width, height, hRes and vRes
   screen->screenY = rect.top;
   GetClientRect(mainHWnd, &rect);
   screen->screenX = rect.left;
   screen->screenW = width;
   screen->screenH = height;
   screen->hRes = GetDeviceCaps(deviceContext, LOGPIXELSX);
   screen->vRes = GetDeviceCaps(deviceContext, LOGPIXELSY);

#if !defined (WINCE)
   DeleteDC(deviceContext);
#else
   ReleaseDC(mainHWnd, deviceContext);
#endif

   return true;
}

struct
{
   BITMAPINFO bi;
	RGBQUAD	 bmiColors[256];
} dibInfo;
struct
{
   LOGPALETTE lp;
   PALETTEENTRY pe[256];
} curPal;
HPALETTE hPalette;

void applyPalette()
{
   SelectPalette(SCREEN_EX(&screen)->dc, hPalette, 0);
   RealizePalette(SCREEN_EX(&screen)->dc);
}

bool graphicsCreateScreenSurface(ScreenSurface screen)
{
   uint32 *ptr;

   screen->pitch = screen->screenW * screen->bpp / 8;
   SCREEN_EX(screen)->dc = GetDC(mainHWnd);

   ptr = (uint32 *)dibInfo.bi.bmiColors;

	//3.1 Initilize DIBINFO structure
   xmemzero(&dibInfo,sizeof(dibInfo));
	dibInfo.bi.bmiHeader.biBitCount = (uint16)screen->bpp;
   dibInfo.bi.bmiHeader.biCompression = (screen->bpp == 16) ? BI_BITFIELDS : BI_RGB;
	dibInfo.bi.bmiHeader.biPlanes = 1;
	dibInfo.bi.bmiHeader.biSize = 40;
	dibInfo.bi.bmiHeader.biWidth = screen->screenW;
	dibInfo.bi.bmiHeader.biHeight = -(int32)screen->screenH;
	dibInfo.bi.bmiHeader.biSizeImage = screen->screenW * screen->screenH * screen->bpp / 8;
   if (screen->bpp == 16)
   {
      // setup the bit masks
      dibInfo.bi.bmiHeader.biClrUsed = dibInfo.bi.bmiHeader.biClrImportant = 3;
      ptr[0] = 0xf800;
      ptr[1] = 0x07e0;
      ptr[2] = 0x001F;
   }
   else
   if (screen->bpp == 8) // apply our 485 palette to the screen
   {
      // create the custom 685 palette
      dibInfo.bi.bmiHeader.biClrUsed = dibInfo.bi.bmiHeader.biClrImportant = 256;
      fillWith8bppPalette(ptr);

      curPal.lp.palNumEntries = 256;
      curPal.lp.palVersion = 0x0300;
      xmemmove(&curPal.lp.palPalEntry, ptr, 1024);
      hPalette = CreatePalette(&curPal.lp);
      SelectPalette(SCREEN_EX(screen)->dc, hPalette, 0);
      RealizePalette(SCREEN_EX(screen)->dc);
   }

	//3.2 Create bitmap and receive pointer to points into pBuffer
   SCREEN_EX(screen)->hbmp = CreateDIBSection(SCREEN_EX(screen)->dc, &dibInfo.bi, DIB_RGB_COLORS, (void**)&screen->pixels, NULL, 0);
   if (!SCREEN_EX(screen)->hbmp || !screen->pixels)
      return false; // put @err,hr in your watch window to see GetLastError()
   return true;
}

inline static void drawImageLine(ScreenSurface screen, HDC targetDC, int32 minx, int32 miny, int32 maxx, int32 maxy)
{
   BitBlt(SCREEN_EX(screen)->dc, minx,miny, maxx-minx, maxy-miny, targetDC, minx,miny, SRCCOPY);
}

void graphicsUpdateScreen(ScreenSurface screen, int32 transitionEffect) // screen's already locked
{
   HDC targetDC = CreateCompatibleDC(NULL);
   HBITMAP hOldBitmap = (HBITMAP)SelectObject(targetDC, SCREEN_EX(screen)->hbmp);
   switch (transitionEffect)
   {
      case TRANSITION_NONE:
         BitBlt(SCREEN_EX(screen)->dc, screen->dirtyX1, screen->dirtyY1, screen->dirtyX2-screen->dirtyX1, screen->dirtyY2-screen->dirtyY1, targetDC, screen->dirtyX1, screen->dirtyY1, SRCCOPY);
         break;
      case TRANSITION_CLOSE:
      case TRANSITION_OPEN:
      {       
         int32 i0,iinc,i;
         int32 w = screen->screenW;
         int32 h = screen->screenH;
         float incX=1,incY=1;
         int32 n = min32(w,h);
         int32 mx = w/2,ww=1,hh=1;
         int32 my = h/2;
         if (w > h)
            {incX = (float)w/h; ww = (int)incX+1;}
          else
            {incY = (float)h/w; hh = (int)incY+1;}
         i0 = transitionEffect == TRANSITION_CLOSE ? n : 0;
         iinc = transitionEffect == TRANSITION_CLOSE ? -1 : 1;
         for (i =i0; --n >= 0; i+=iinc)
         {
            int32 minx = (int32)(mx - i*incX);
            int32 miny = (int32)(my - i*incY);
            int32 maxx = (int32)(mx + i*incX);
            int32 maxy = (int32)(my + i*incY);
            drawImageLine(screen,targetDC,minx-ww,miny-hh,maxx+ww,miny+hh);
            drawImageLine(screen,targetDC,minx-ww,miny-hh,minx+ww,maxy+hh);
            drawImageLine(screen,targetDC,maxx-ww,miny-hh,maxx+ww,maxy+hh);
            drawImageLine(screen,targetDC,minx-ww,maxy-hh,maxx+ww,maxy+hh);
         }
         break;
      }
   }
   SelectObject(targetDC, hOldBitmap);
   DeleteDC(targetDC);
#ifdef WINCE // guich@tc113_20
   if (oldAutoOffValue != 0) // guich@450_33: since the autooff timer function don't work on wince, we must keep resetting the idle timer so that the device will never go sleep - guich@554_7: reimplemented this feature
      SystemIdleTimerReset();
#endif
}

void graphicsDestroy(ScreenSurface screen, bool isScreenChange)
{
   DeleteObject(SCREEN_EX(screen)->hbmp);
   if (!isScreenChange)
   {
      xfree(screen->extension);
      screen->extension = null;
      DestroyWindow(mainHWnd);
      mainHWnd = null;
      restoreTaskbar();
   }
   UnregisterClass(exeName, null);
}
