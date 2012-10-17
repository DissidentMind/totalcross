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



extern Object testfont;
extern Object pngImage, jpegImage;

#define TEST_SLEEP 200
void blank(Context currentContext, Object g)
{
   Sleep(TEST_SLEEP);
   fillRect(g, 0,0,screen.screenW, screen.screenH, makePixel(255,255,255));
   updateScreen(mainContext);
}

static void testDrawHline(Context currentContext, Object g)
{
   int32 y;
   for (y = 0; y < screen.screenH; y++)
   {
      Pixel color = makePixel(255,y>255?255:y,255);
      drawHLine(g, y, y, screen.screenW-y, color,color);
      updateScreen(mainContext);
   }
}

static void testDrawVline(Context currentContext, Object g)
{
   int32 x;
   for (x = 0; x < screen.screenW; x++)
   {
      Pixel color = makePixel(x>255?255:x,255,255);
      drawVLine(g, x, 0, screen.screenH-x, color,color);
      updateScreen(mainContext);
   }
}

static void testFillRect(Context currentContext, Object g)
{
   int32 x,k;
   PixelConv rr,gg,bb;
   rr.pixel = gg.pixel = bb.pixel = 0;
   k = (screen.screenH - 20) / 3;

   for (x = 0; x < 256; x+=2)
   {
      bb.b = gg.g = rr.r = (uint8)x;
      fillRect(g, 10, 10+k*0, screen.screenW-20, k, rr.pixel);
      fillRect(g, 10, 10+k*1, screen.screenW-20, k, gg.pixel);
      fillRect(g, 10, 10+k*2, screen.screenW-20, k, bb.pixel);
      updateScreen(mainContext);
   }
}

static void testFillCircle(Context currentContext, Object g)
{
   Pixel color;
   int32 r,mx,my;
   mx = screen.screenW >> 1;
   my = screen.screenH >> 1;

   for (r = 160; r >= 0; r-=3)
   {
      color = makePixel(r,r,0xAA);
      ellipseDrawAndFill(g, mx, my, r, r, color, color, true, false);
      updateScreen(mainContext);
   }
}

static void testDrawCircle(Context currentContext, Object g)
{
   Pixel color;
   int32 r,mx,my;
   mx = screen.screenW >> 1;
   my = screen.screenH >> 1;

   for (r = 0; r < 160; r++)
   {
      color = makePixel(0x55,r,0x88);
      ellipseDrawAndFill(g, mx, my, r, r, color, color, false, false);
      updateScreen(mainContext);
   }
}

static void testDrawEllipse(Context currentContext, Object g)
{
   Pixel color;
   int32 r,mx,my;
   mx = screen.screenW >> 1;
   my = screen.screenH >> 1;

   for (r = 0; r < 160; r++)
   {
      color = makePixel(0xAA,0xFF,r);
      ellipseDrawAndFill(g, mx, my, r/2, r, color, color, false, false);
      updateScreen(mainContext);
   }
}

static void testFillEllipse(Context currentContext, Object g)
{
   Pixel color;
   int32 r,mx,my;
   mx = screen.screenW >> 1;
   my = screen.screenH >> 1;

   for (r = 160; r >= 0; r -= 2)
   {
      color = makePixel(0xAA,r,0xFF);
      ellipseDrawAndFill(g, mx, my, r, r/2, color, color, true, false);
      updateScreen(mainContext);
   }
}

static void testPie(Context currentContext, Object g)
{
   Pixel color,white,black;
   int32 r=40,d,mx,my;
   mx = screen.screenW >> 1;
   my = screen.screenH >> 1;
   white = makePixel(0xFF, 0xFF, 0xFF);
   black = makePixel(0,0,0);

   color = makePixel(r,0xAA,0xFF);
   for (d = 0; d <= 360; d+=2)
   {
      arcPiePointDrawAndFill(g, mx, my, r, r, d-45, d, black, color, true, true, false, currentContext);
      updateScreen(mainContext);
      fillRect(g, mx-r-1,my-r-1,r+r+2,r+r+2, white);
   }
}

static void testFillCursor(Context currentContext, Object g)
{
   int32 y;
   int32 h = 30;
   for (y = 0; y < screen.screenH; y+=2)
   {
      fillCursor(g, 5, y, screen.screenW-10, h);
      updateScreen(mainContext);
#if defined WIN32 && !defined WINCE
//      Sleep(2);
#endif
      fillCursor(g, 5, y, screen.screenW-10, h);
   }
}

static void drawImage(Context currentContext, Object g, Object img)
{
   Object gimg;
   int32 w = Image_width(img);
   int32 h = Image_height(img);
   Pixel fore,back;
   fore = makePixel(0,255,255);
   back = makePixel(255,255,0);
   gimg = createObjectWithoutCallingDefaultConstructor(currentContext, "totalcross.ui.gfx.Graphics");
   setObjectLock(gimg, UNLOCKED);
   Graphics_surface(gimg) = img;
   createGfxSurface(w, h, gimg, SURF_IMAGE);
   drawSurface(g, gimg, 0, 0, w, h, 0,0, 0, fore, back, true);
   updateScreen(mainContext);
}

static void testJpegImage(Context currentContext, Object g)
{
   drawImage(currentContext, g, jpegImage);
}

static void testPngImage(Context currentContext, Object g)
{
   drawImage(currentContext, g, pngImage);
}

static void testPalette(Context currentContext, Object g)
{
   int32 x=0,y=0,i,wh;
   uint32 c;
   uint32 pal[256];
   fillWith8bppPalette(pal);
   wh = min32(screen.screenW / 16, screen.screenH / 16);
   for (i = 0; i < 256; i++)
   {
      c = pal[i];
#ifdef PALMOS // other platforms have the r,g,b in the right order, Palm OS is BBGGRR00
     {uint32 r = (c>>8)&0xFF, g = (c>>16)&0xFF, b = (c >> 24)&0xFF; c = (r << 16) | (g << 8) | b;}
#endif
      if (i && (i % 16) == 0)
      {
         x = 0; y += wh;
      }
      fillRect(g, x,y,wh,wh,makePixelRGB(c));
      x += wh;
   }
   updateScreen(mainContext);
}

static void testText(Context currentContext, Object g)
{
   JChar text[50];
   int32 x,x1,y1,x2,y2,y,dy,i;
   Pixel white = makePixel(255,255,255);
   Pixel p = makePixel(0,0,255);
   Graphics_font(g) = testfont;
   dy = 12;
   CharP2JCharPBuf("Barbara",7, text, true);
   for (x = -50; x < (50+screen.screenW); x++)
   {
      for (i=0,y = 10; i++ < 10 && y < (screen.screenH-20); y+=dy) // max 10 lines
         drawText(currentContext, g, text, 7, x, y, p,0);
      x1 = x; x2 = currentContext->dirtyX2; y1 = currentContext->dirtyY1; y2 = currentContext->dirtyY2; // save dirty area to erase it
      updateScreen(mainContext);
      fillRect(g, x1,y1,x2-x1,y2-y1, white); // erase dirty area
   }
   Graphics_clipX1(g) = 0;
   Graphics_clipX2(g) = screen.screenW;
}

//#define DUMP_TIME

static void debugTime(int32 n, int32 ini)
{
#ifdef DUMP_TIME
   debug(" %02d.Elapsed: %d ms",n, getTimeStamp()-ini);
#else
   UNUSED(n)
   UNUSED(ini)
#endif
}

TESTCASE(Graphics) // #DEPENDS(tuiI_imageLoad_s)
{
   Object g;
   int32 s;
   TNMParams p;
   Object obj[2];

   ASSERT1_EQUALS(NotNull, screen.pixels);
   // create a graphics object and call its native constructor
   g = createObjectWithoutCallingDefaultConstructor(currentContext, "totalcross.ui.gfx.Graphics");
   setObjectLock(g, UNLOCKED);
   ASSERT1_EQUALS(NotNull, g);
   p.currentContext = currentContext;
   p.obj = obj;
   obj[0] = g;
   obj[1] = null;
   tugG_create_g(&p);

   fillRect(g, 0,0,screen.screenW, screen.screenH, makePixel(0xFF,0xFF,0xFF));
   updateScreen(mainContext);

   ASSERT1_EQUALS(NotNull, defaultFont);
   ASSERT1_EQUALS(NotNull, pngImage);
   ASSERT1_EQUALS(NotNull, jpegImage);

   s = getTimeStamp();  testJpegImage(currentContext, g);  debugTime( 1,s);  blank(g);
   s = getTimeStamp();  testPngImage(currentContext,  g);  debugTime( 2,s);  blank(g);
   s = getTimeStamp();  testText(currentContext,      g);  debugTime( 3,s);  blank(g);
   s = getTimeStamp();  testPalette(                  g);  debugTime( 4,s);  blank(g);
   s = getTimeStamp();  testPie(currentContext,       g);  debugTime( 5,s);  blank(g);
   s = getTimeStamp();  testDrawEllipse(              g);  debugTime( 6,s);  blank(g);
   s = getTimeStamp();  testFillEllipse(              g);  debugTime( 7,s);  blank(g);
   s = getTimeStamp();  testFillCircle(               g);  debugTime( 8,s);  blank(g);
   s = getTimeStamp();  testDrawCircle(               g);  debugTime( 9,s);  blank(g);
   s = getTimeStamp();  testDrawVline(                g);  debugTime(10,s);  blank(g);
   s = getTimeStamp();  testDrawHline(                g);  debugTime(11,s);  blank(g);
   s = getTimeStamp();  testFillRect(                 g);  debugTime(12,s);  Sleep(TEST_SLEEP); // no blank
   s = getTimeStamp();  testFillCursor(               g);  debugTime(13,s);  blank(g);
   finish: ;
}
