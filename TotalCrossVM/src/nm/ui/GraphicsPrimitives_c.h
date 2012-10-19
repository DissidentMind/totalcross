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
#include "PalmFont.h"
#include "GraphicsPrimitives.h"
#include "math.h"

#define TRANSITION_NONE  0
#define TRANSITION_OPEN  1
#define TRANSITION_CLOSE 2

#ifdef __gl2_h_
extern int appW,appH;
#endif


bool graphicsStartup(ScreenSurface screen, int16 appTczAttr);
bool graphicsCreateScreenSurface(ScreenSurface screen);
void graphicsUpdateScreen(Context currentContext, ScreenSurface screen, int32 transitionEffect);
void graphicsDestroy(ScreenSurface screen, bool isScreenChange);
#if !defined(NO_GRAPHICS_LOCK_NEEDED)
bool graphicsLock(ScreenSurface screen, bool on);
#endif
static bool createScreenSurface(Context currentContext, bool isScreenChange);

void updateScreen(Context currentContext);
void privateScreenChange(int32 w, int32 h);
void markWholeScreenDirty(Context currentContext);

// >>>>>>>>>
// DO NOT LOCK THE SCREEN ON THESE METHODS. THE CALLER MUST DO THAT.
static inline Pixel* getSurfacePixels(Object surf)
{
   Object pix;
   bool isImage=false;
   if (surf != null)
   {
      CharP name = OBJ_CLASS(surf)->name;
      if (name[0] == 't' && strEq(name,"totalcross.ui.gfx.Graphics")) // if the surface is a Graphics, get the target surface
         surf = Graphics_surface(surf);
      isImage = Surface_isImage(surf);
   }
   pix = isImage ? Image_pixels(surf) : screen.mainWindowPixels;
   return (Pixel*)ARRAYOBJ_START(pix);
}

static inline Pixel* getGraphicsPixels(Object g)
{
   Object surf = Graphics_surface(g);
   return getSurfacePixels(surf);
}
// <<<<<<<<<

void screenChange(Context currentContext, int32 newWidth, int32 newHeight, int32 hRes, int32 vRes, bool nothingChanged) // rotate the screen
{
   callingScreenChange = true;
   // IMPORTANT: this is the only place that changes tcSettings
   screen.screenW = *tcSettings.screenWidthPtr  = newWidth;
   screen.pitch = screen.screenW * screen.bpp / 8;
   screen.screenH = *tcSettings.screenHeightPtr = newHeight;
   screen.hRes = *tcSettings.screenWidthInDPIPtr = hRes;
   screen.vRes = *tcSettings.screenHeightInDPIPtr = vRes;
   markWholeScreenDirty(currentContext);
   privateScreenChange(newWidth, newHeight);
   if (!nothingChanged)
   {
      graphicsDestroy(&screen, true);
      createScreenSurface(currentContext, true);
   }
   // post the event to the vm
   if (mainClass != null)
      postEvent(currentContext, KEYEVENT_SPECIALKEY_PRESS, SK_SCREEN_CHANGE, 0,0,-1);
   callingScreenChange = false;
}

void repaintActiveWindows(Context currentContext)
{
   static Method repaintActiveWindows;
   if (repaintActiveWindows == null && mainClass != null)
      repaintActiveWindows = getMethod(OBJ_CLASS(mainClass), true, "repaintActiveWindows", 0);
   if (repaintActiveWindows != null)
      executeMethod(currentContext, repaintActiveWindows);
}

////////////////////////////////////////////////////////////////////////////
static bool translateAndClip(Object g, int32 *x, int32 *y, int32 *width, int32 *height);

Pixel makePixelA(int32 a, int32 r, int32 g, int32 b)
{
   PixelConv p;
   p.a = (uint8)(a & 0xFF);
   p.r = (uint8)(r & 0xFF);
   p.g = (uint8)(g & 0xFF);
   p.b = (uint8)(b & 0xFF);
   return p.pixel;
}
Pixel makePixel(int32 r, int32 g, int32 b)
{
   PixelConv p;
   p.a = 0;
   p.r = (uint8)(r & 0xFF);
   p.g = (uint8)(g & 0xFF);
   p.b = (uint8)(b & 0xFF);
   return p.pixel;
}
Pixel makePixelRGB(int32 rgb) // from Java's big endian to native format
{
   PixelConv p;
   p.a = 0;
   p.r = (uint8)((rgb >> 16) & 0xFF);
   p.g = (uint8)((rgb >> 8)  & 0xFF);
   p.b = (uint8)( rgb        & 0xFF);
   return p.pixel;
}
Pixel makePixelARGB(int32 rgb) // from Java's big endian to native format
{
   PixelConv p;
   p.a = (uint8)((rgb >> 24) & 0xFF);
   p.r = (uint8)((rgb >> 16) & 0xFF);
   p.g = (uint8)((rgb >> 8)  & 0xFF);
   p.b = (uint8)( rgb        & 0xFF);
   return p.pixel;
}

// Updates the dirty area (or extends the current one)
// DO NOT LOCK THE SCREEN ON THIS METHOD. THE CALLER MUST DO THAT.
static void markScreenDirty(Context currentContext, int32 x, int32 y, int32 w, int32 h)
{
   LOCKVAR(screen);
   if (w > 0 && h > 0 && x < screen.screenW && y < screen.screenH)
   {
      int32 x2,y2;
      /* Clip with the Screen surface */
      if (x < 0) x = 0;
      if (y < 0) y = 0;
      x2 = x+w;
      y2 = y+h;
      if (x2 > screen.screenW) x2 = screen.screenW;
      if (y2 > screen.screenH) y2 = screen.screenH;

      if (currentContext->dirtyX1 < x)
      {
         x2 = (currentContext->dirtyX2 > x2) ? currentContext->dirtyX2 : x2;
         x  = currentContext->dirtyX1;
      }
      else
      if (currentContext->dirtyX2 > x2)
         x2 = currentContext->dirtyX2;

      if (currentContext->dirtyY1 < y)
      {
         y2 = (currentContext->dirtyY2 > y2) ? currentContext->dirtyY2 : y2;
         y  = currentContext->dirtyY1;
      }
      else
      if (currentContext->dirtyY2 > y2)
         y2 = currentContext->dirtyY2;

      currentContext->dirtyX1 = x;
      currentContext->dirtyY1 = y;
      currentContext->dirtyX2 = x2;
      currentContext->dirtyY2 = y2;
      if (x == 0 && y == 0 && x2 == screen.screenW && y2 == screen.screenH)
         currentContext->fullDirty = true;
   }
   UNLOCKVAR(screen);
}

// This is the main routine that draws a surface (a Control or an Image) in the destination GfxSurface.
// Destination is always a Graphics object.
static void drawSurface(Context currentContext, Object dstSurf, Object srcSurf, int32 srcX, int32 srcY, int32 width, int32 height,
                       int32 dstX, int32 dstY, int32 doClip)
{
   uint32 i;
   Pixel * srcPixels;
   Pixel * dstPixels;
   int32 srcPitch, srcWidth, srcHeight;
   bool isSrcScreen = !Surface_isImage(srcSurf);
   dstPixels = getSurfacePixels(dstSurf);
   srcPixels = getSurfacePixels(srcSurf);
   if (Surface_isImage(srcSurf))
   {
      srcPitch = srcWidth = Image_width(srcSurf);
      srcHeight = Image_height(srcSurf);
   }
   else
   {
      srcPitch = srcWidth = screen.screenW;
      srcHeight = screen.screenH;
   }
   if (!doClip)
   {
      /*
      | Even if no clip is required, we still have to make sure that the
      | area of the bitmap that we want to copy is inside its area.
      */
      if (srcX <= -width  || srcX >= srcWidth || srcY <= -height || srcY >= srcHeight)
         goto end;
      dstX += Graphics_transX(dstSurf);
      dstY += Graphics_transY(dstSurf);
   }
   else
   {
      dstX += Graphics_transX(dstSurf);
      dstY += Graphics_transY(dstSurf);

      /* clip the source rectangle to the source surface */
      if (srcX < 0)
      {
         width += srcX;
         dstX -= srcX;
         srcX = 0;
      }
      i = srcWidth - srcX;
      if (width > (int32)i)
         width = i;
      if (srcY < 0)
      {
         height += srcY;
         dstY -= srcY;
         srcY = 0;
      }
      i = srcHeight - srcY;
      if (height > (int32)i)
         height = i;

      /* clip the destination rectangle against the clip rectangle */
      if (dstX < Graphics_clipX1(dstSurf))
      {
         i = Graphics_clipX1(dstSurf) - dstX;
         dstX = Graphics_clipX1(dstSurf);
         srcX += i;
         width -= i;
      }
      if ((dstX + width) > Graphics_clipX2(dstSurf))
         width = Graphics_clipX2(dstSurf) - dstX;
      if (dstY < Graphics_clipY1(dstSurf))
      {
         i = Graphics_clipY1(dstSurf) - dstY;
         dstY = Graphics_clipY1(dstSurf);
         srcY += i;
         height -= i;
      }
      if ((dstY + height) > Graphics_clipY2(dstSurf))
         height = Graphics_clipY2(dstSurf) - dstY;

      /* check the validity */
      if (width <= 0 || height <= 0)
         goto end;
   }

   srcPixels += srcY * srcPitch + srcX;
   dstPixels += dstY * Graphics_pitch(dstSurf) + dstX;
#ifdef __gl2_h_
   if (Graphics_useOpenGL(dstSurf) && !isSrcScreen)
   {
      if (Image_changed(srcSurf))
         applyChanges(srcSurf);
      glDrawTexture(Image_textureId(srcSurf), srcX,srcY,width,height, dstX,dstY, srcWidth,srcHeight);
   }
   else
#endif
   for (i=0; i < (uint32)height; i++) // in opengl, only case of image drawing on image
   {
      PixelConv *ps = (PixelConv*)srcPixels;
      PixelConv *pt = (PixelConv*)dstPixels;
      uint32 count = width;
      if (isSrcScreen)
         xmemmove(pt, ps, count<<2);
      else
         for (;count != 0; pt++,ps++, count--)
         {
            int32 a = ps->a;
            if (a == 0xFF)
               pt->pixel = ps->pixel;
            else
            if (a != 0)
            {
               int32 ma = 0xFF-a;
               int32 r = (a * ps->r + ma * pt->r);
               int32 g = (a * ps->g + ma * pt->g);
               int32 b = (a * ps->b + ma * pt->b);
               pt->r = (r+1 + (r >> 8)) >> 8; // fast way to divide by 255
               pt->g = (g+1 + (g >> 8)) >> 8;
               pt->b = (b+1 + (b >> 8)) >> 8;
            }
         }
      srcPixels += srcPitch;
      dstPixels += Graphics_pitch(dstSurf);
   }
#ifndef __gl2_h_
   if (!currentContext->fullDirty && !Surface_isImage(dstSurf)) markScreenDirty(currentContext, dstX, dstY, width, height);
#else
   currentContext->fullDirty |= !Surface_isImage(dstSurf);
#endif
end:
   ;
}

//   Device specific routine.
//   Gets the color value of the pixel, using the current translation
//   Returns -1 if error (out of clip bounds)
static int32 getPixel(Object g, int32 x, int32 y)
{
   int32 ret = -1;
   x += Graphics_transX(g);
   y += Graphics_transY(g);
   if (Graphics_clipX1(g) <= x && x < Graphics_clipX2(g) && Graphics_clipY1(g) <= y && y < Graphics_clipY2(g))
   {
      PixelConv p;
#ifdef __gl2_h_
      if (Graphics_useOpenGL(g))
         return glGetPixel(x,y);
      else
#endif
         p.pixel = getGraphicsPixels(g)[y * Graphics_pitch(g) + x];
      ret = (p.r << 16) | (p.g << 8) | p.b;
   }
   return ret;
}

static PixelConv getPixelConv(Object g, int32 x, int32 y)
{
   PixelConv p;
   p.pixel = -1;
   x += Graphics_transX(g);
   y += Graphics_transY(g);
   if (Graphics_clipX1(g) <= x && x < Graphics_clipX2(g) && Graphics_clipY1(g) <= y && y < Graphics_clipY2(g))
   {
#ifdef __gl2_h_
      if (Graphics_useOpenGL(g))
         p.pixel = glGetPixel(x,y);
      else
#endif
         p.pixel = getGraphicsPixels(g)[y * Graphics_pitch(g) + x];
   }
   return p;
}

// Device specific routine.
// Sets the pixel to the given color, translating and clipping
static inline void setPixel(Context currentContext, Object g, int32 x, int32 y, Pixel pixel)
{
   x += Graphics_transX(g);
   y += Graphics_transY(g);
   if (Graphics_clipX1(g) <= x && x < Graphics_clipX2(g) && Graphics_clipY1(g) <= y && y < Graphics_clipY2(g))
   {
#ifdef __gl2_h_
      if (Graphics_useOpenGL(g))
      {
         glDrawPixel(x,y,pixel,255);
         currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
      }
      else
#endif
      {
         getGraphicsPixels(g)[y * Graphics_pitch(g) + x] = pixel;
         if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, x, y, 1, 1);
      }
   }
}

static int32 interpolate(PixelConv c, PixelConv d, int32 factor)
{
   int m = 255-factor;
   c.r = (c.r*factor + d.r*m)/255;
   c.g = (c.g*factor + d.g*m)/255;
   c.b = (c.b*factor + d.b*m)/255;
   return c.pixel;
}

static inline bool surelyOutsideClip(Object g, int32 x1, int32 y1, int32 x2, int32 y2)
{
   int cx1 = Graphics_clipX1(g);
   int cx2 = Graphics_clipX2(g);
   int cy1 = Graphics_clipY1(g);
   int cy2 = Graphics_clipY2(g);
   x1 += Graphics_transX(g);
   x2 += Graphics_transX(g);
   y1 += Graphics_transY(g);
   y2 += Graphics_transY(g);
   return (x1 < cx1 && x2 < cx1) || (x1 > cx2 && x2 > cx2) || (y1 < cy1 && y2 < cy1) || (y1 > cy2 && y2 > cy2);
}

//   Device specific routine.
//   Draws a horizontal line from x to x+w-1, translating and clipping
static void drawHLine(Context currentContext, Object g, int32 x, int32 y, int32 width, Pixel pixel1, Pixel pixel2)
{
   x += Graphics_transX(g);
   y += Graphics_transY(g);
   /*
   | line must lie inside y clip bounds, must not end before clip x1
   | and must not start after clip x2
   */
   if (Graphics_clipY1(g) <= y && y < Graphics_clipY2(g) && Graphics_clipX1(g) <= (x+width) && x < Graphics_clipX2(g)) // NOPT
   {
      Pixel* pTgt;
      if (x < Graphics_clipX1(g))           // line start before clip x1
      {
         width -= Graphics_clipX1(g)-x;
         x = Graphics_clipX1(g);
      }
      if ((x+width) > Graphics_clipX2(g))   // line stops after clip x2
         width = Graphics_clipX2(g)-x;

      if (width <= 0)
         return;
#ifdef __gl2_h_
      if (Graphics_useOpenGL(g))
      {
         glDrawLine(x,y,x+width,y,pixel1,255);
         if (pixel1 != pixel2)
            for (x++; width > 0; width -= 2, x += 2)
               glDrawPixel(x,y, pixel2,255);
         currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
      }
      else
#endif
      {
         pTgt = getGraphicsPixels(g) + y * Graphics_pitch(g) + x;
         if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, x, y, width, 1);
         if (pixel1 == pixel2) // same color?
         {
            while (width-- > 0)
               *pTgt++ = pixel1;          // plot the pixel
         }
         else
         {
            int32 i=0;
            while (width-- > 0)
               *pTgt++ = (i++ & 1) ? pixel1 : pixel2;
         }
      }
   }
}

//   Device specific routine.
//   Draws a vertical line from y to y+h-1 using the given color
static void drawVLine(Context currentContext, Object g, int32 x, int32 y, int32 height, Pixel pixel1, Pixel pixel2)
{
   x += Graphics_transX(g);
   y += Graphics_transY(g);
   /*
   | line must lie inside x clip bounds, must not end before clip y1
   | and must not start after clip y2
   */
   if (Graphics_clipX1(g) <= x && x < Graphics_clipX2(g) && Graphics_clipY1(g) <= (y+height) && y < Graphics_clipY2(g)) // NOPT
   {
      Pixel * pTgt;
      int32 pitch = Graphics_pitch(g);
      uint32 n;
      if (y < Graphics_clipY1(g))           // line start before clip y1
      {
         height -= Graphics_clipY1(g)-y;
         y = Graphics_clipY1(g);
      }
      if ((y+height) > Graphics_clipY2(g))
         height = Graphics_clipY2(g)-y;           // line stops after clip y2

      if (height <= 0)
         return;
#ifdef __gl2_h_
      if (Graphics_useOpenGL(g))
      {
         glDrawLine(x,y,x,y+height,pixel1,255);
         if (pixel1 != pixel2)
            for (y++; height > 0; height -= 2, y += 2)
               glDrawPixel(x,y, pixel2,255);
         currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
      }
      else
#endif
      {
         pTgt = getGraphicsPixels(g) + y * pitch + x;
         n = (uint32)height;
         if (pixel1 == pixel2) // same color?
         {
            for (; n != 0; pTgt += pitch, n--)
               *pTgt = pixel1;          // plot the pixel
         }
         else
         {
            uint32 i=0;
            for (; n != 0; pTgt += pitch, n--)
               *pTgt = (i++ & 1) ? pixel1 : pixel2;          // plot the pixel
         }
         if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, x, y, 1, height);
      }
   }
}

static void drawDottedLine(Context currentContext, Object g, int32 x1, int32 y1, int32 x2, int32 y2, Pixel pixel1, Pixel pixel2)
{
    // guich@501_10: added pyInc and clipping support for this routine.
    // store the change in X and Y of the line endpoints
    int32 dX,dY;
    // DETERMINE "DIRECTIONS" TO INCREMENT X AND Y (REGARDLESS OF DECISION)
    int32 xInc,yInc,pyInc; // py incs by pixel, y incs by row
    // used in clipping
    int32 xMin,yMin;
    int32 tX = Graphics_transX(g), tY = Graphics_transY(g);

    if (surelyOutsideClip(g, x1,y1,x2,y2)) // guich@tc115_63
       return;

    // compute the values
    if (x1 <= x2)
    {
       dX = x2-x1;
       xMin = x1;
       xInc = 1;
    }
    else
    {
       dX = x1-x2;
       xMin = x2;
       xInc = -1;
    }
    if (y1 <= y2)
    {
       dY = y2-y1;
       yMin = y1;
       pyInc = 1;
    }
    else
    {
       dY = y1-y2;
       yMin = y2;
       pyInc = -1;
    }

    // guich: if its a pixel, draw only the pixel
    if (dX == 0 && dY == 0)
       setPixel(currentContext, g, xMin,yMin,pixel1);
    else
    if (dY == 0) // horizontal line?
       drawHLine(currentContext, g, min32(x1,x2),min32(y1,y2),dX+1,pixel1,pixel2);
    else
    if (dX == 0) // vertical line?
       drawVLine(currentContext, g, min32(x1,x2),min32(y1,y2),dY+1,pixel1,pixel2);
    else // guich@566_43: removed the use of drawH/VLine to make sure that it will draw the same of desktop
    {
       int32 currentX,currentY;  // saved for later markScreenDirty
       Pixel *row;
       int32 on = 1;
       int32 dx = dX+1, dy = dY+1; // fdie@580_37
       int32 clipX1 = Graphics_clipX1(g), clipY1 = Graphics_clipY1(g), clipX2 = Graphics_clipX2(g), clipY2 = Graphics_clipY2(g);
       bool dontClip = true; // the most common will be draw lines that do not cross the clip bounds, so we may speedup a little

       xMin += tX;
       yMin += tY;

       if (xMin < clipX1 || (xMin+dX) >= clipX2 || yMin < clipY1 || (yMin+dY) >= clipY2)
          dontClip = false;

#ifdef __gl2_h_  
       bool useOpenGL = Graphics_useOpenGL(g);
       if (useOpenGL) // draw a full line and then cover with pixel2 colors
       {
          if (!dontClip) glSetClip(clipX1,clipY1,clipX2,clipY2);
          glDrawLine(x1+tX,y1+tY,x2+tX,y2+tY,pixel1,255);
          if (!dontClip) glClearClip();
          currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
          if (pixel1 == pixel2)
             return;
       }
#endif

       currentX = x1+tX;
       currentY = y1+tY;

       row = getGraphicsPixels(g) + (pyInc>0 ? yMin : (yMin+dY)) * Graphics_pitch(g) + (xInc>0 ? xMin : (xMin+dX)); // currentX    | else start from the max value

       yInc = pyInc>0 ? Graphics_pitch(g) : -Graphics_pitch(g);  // now that we have active, correctly assign the y incrementing value

       // DETERMINE INDEPENDENT VARIABLE (ONE THAT ALWAYS INCREMENTS BY 1 (OR -1) )
       // AND INITIATE APPROPRIATE LINE DRAWING ROUTINE (BASED ON FIRST OCTANT
       // ALWAYS). THE X AND Y'S MAY BE FLIPPED IF Y IS THE INDEPENDENT VARIABLE.
       if (dX >= dY)   // if X is the independent variable
       {
          int32 dPr     = dY<<1;                         // amount to increment decision if right is chosen (always)
          int32 dPru    = dPr - (dX<<1);                 // amount to increment decision if up is chosen
          int32 p       = dPr - dX;                      // decision variable start value

          if (pixel1 == pixel2) // quick optimization
             for (; dX >= 0; dX--)                       // process each point in the line one at a time (just use dX)
             {
                if (dontClip || (clipX1 <= currentX && currentX < clipX2 && clipY1 <= currentY && currentY < clipY2))
                   *row = pixel1;                        // plot the pixel - never in opengl
                row      += xInc;                        // increment independent variable
                currentX += xInc;
                if (p > 0)                               // is the pixel going right AND up?
                {
                   currentY += pyInc;
                   p += dPru;                            // increment decision (for up)
                   row += yInc;                          // increment dependent variable
                }
                else                                     // is the pixel just going right?
                   p += dPr;                             // increment decision (for right)
             }
          else
             for (; dX >= 0; dX--)                       // process each point in the line one at a time (just use dX)
             {
                if (dontClip || (clipX1 <= currentX && currentX < clipX2 && clipY1 <= currentY && currentY < clipY2))
#ifdef __gl2_h_
                   if (useOpenGL && (on++ & 1) == 0)
                      glDrawPixel(currentX, currentY, pixel2, 255);
#else
                   *row = (on++ & 1) ? pixel1 : pixel2;  // plot the pixel
#endif
                row += xInc;                             // increment independent variable
                currentX += xInc;
                if (p > 0)                               // is the pixel going right AND up?
                {
                   currentY += pyInc;
                   p += dPru;                            // increment decision (for up)
                   row += yInc;                          // increment dependent variable
                }
                else                                     // is the pixel just going right?
                   p += dPr;                             // increment decision (for right)
             }
       }
       else            // if Y is the independent variable
       {
          int32 dPr     = dX<<1;                         // amount to increment decision if right is chosen (always)
          int32 dPru    = dPr - (dY<<1);                 // amount to increment decision if up is chosen
          int32 p       = dPr - dY;                      // decision variable start value

          if (pixel1 == pixel2) // quick optimization
             for (; dY >= 0; dY--)                       // process each point in the line one at a time (just use dY)
             {
                if (dontClip || (clipX1 <= currentX && currentX < clipX2 && clipY1 <= currentY && currentY < clipY2))
                   *row = pixel1;                        // plot the pixel
                row += yInc;                             // increment independent variable
                currentY += pyInc;
                if (p > 0)                               // is the pixel going up AND right?
                {
                   row += xInc;                          // increment dependent variable
                   currentX += xInc;
                   p += dPru;                            // increment decision (for up)
                }
                else                                     // is the pixel just going up?
                   p += dPr;                             // increment decision (for right)
             }
          else
             for (; dY >= 0; dY--)                       // process each point in the line one at a time (just use dY)
             {
                if (dontClip || (clipX1 <= currentX && currentX < clipX2 && clipY1 <= currentY && currentY < clipY2))
#ifdef __gl2_h_
                   if (useOpenGL && (on++ & 1) == 0)
                      glDrawPixel(currentX, currentY, pixel2,255);
#else
                   *row = (on++ & 1) ? pixel1 : pixel2;  // plot the pixel
#endif
                row += yInc;                             // increment independent variable
                currentY += pyInc;
                if (p > 0)                               // is the pixel going up AND right?
                {
                   row += xInc;                          // increment dependent variable
                   currentX += xInc;
                   p += dPru;                            // increment decision (for up)
                }
                else                                     // is the pixel just going up?
                   p += dPr;                             // increment decision (for right)
             }
       }
#ifndef __gl2_h_
       if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, xMin, yMin, dx, dy);
#endif
    }
}

static int32 abs32(int32 a)
{
   return a < 0 ? -a : a;
}

/* Code taken from http://www.codeproject.com/gdi/antialias.asp */
static void drawLineAA(Context currentContext, Object g, int32 x1, int32 y1, int32 x2, int32 y2, Pixel color_)
{
  // Calculate line params
  int32 dx = (x2 - x1);
  int32 dy = (y2 - y1);
  int32 temp;
  int32 k,z,yt,xt,distance,notdist;
  int32 xs,ys,DeltaX,DeltaY;
  int32 red, green, blue,rr,gg,bb;
  PixelConv bgColor,color;
  int32 tX = Graphics_transX(g), tY = Graphics_transY(g);

  if (surelyOutsideClip(g, x1,y1,x2,y2)) // guich@tc115_63
     return;

  DeltaX = abs32(dx);
  DeltaY = abs32(dy);

#ifdef __gl2_h_
  if (Graphics_useOpenGL(g))
  {
     if (DeltaX > DeltaY)
     {
        glDrawLine(x1+tX,y1+tY,x2+tX,y2+tY, color_, 255);
        glDrawLine(x1+tX,y1+tY+1,x2+tX,y2+tY+1, color_, 128);
     }
     else 
     {
        glDrawLine(x1+tX,y1+tY,x2+tX,y2+tY, color_, 255);
        glDrawLine(x1+tX+1,y1+tY,x2+tX+1,y2+tY, color_, 128);
     }
     return;
  }
#endif

  color.pixel = color_;
  rr = color.r;
  gg = color.g;
  bb = color.b;

  // Set start pixel
  setPixel(currentContext, g, x1, y1, color_);

  // X-dominant line
  if (DeltaX == 0 && DeltaY == 0) // the pixel was already drawn
     ;
  else
  if (DeltaY == 0 || DeltaX == 0 || DeltaX == DeltaY)
     drawDottedLine(currentContext, g,x1,y1,x2,y2, color_, color_);
  else
  if (DeltaX > DeltaY)
  {
     // Exchange line end points
     if (dx < 0)
     {
        temp = x1; x1 = x2; x2 = temp;
        temp = y1; y1 = y2; y2 = temp;
     }
     k = (dy<<16) / dx;
     // Set middle pixels
     yt = (y1<<16) + k;
     for (xs=x1+1; xs<x2; xs++)
     {
        z = (yt>>16);
        distance = yt - (z<<16);
        notdist = (1<<16) - distance;

        bgColor = getPixelConv(g,xs, z);
        red   = (distance*bgColor.r + notdist*rr) >> 16;
        green = (distance*bgColor.g + notdist*gg) >> 16;
        blue  = (distance*bgColor.b + notdist*bb) >> 16;
        setPixel(currentContext, g, xs, z, makePixel(red,green,blue));

        bgColor = getPixelConv(g, xs, z+1);
        red   = (notdist*bgColor.r + distance*rr) >> 16;
        green = (notdist*bgColor.g + distance*gg) >> 16;
        blue  = (notdist*bgColor.b + distance*bb) >> 16;
        setPixel(currentContext, g, xs, z+1, makePixel(red,green,blue));
        yt += k;
     }
  }
  // Y-dominant line
  else
  {
     // Exchange line end points
     if (dy < 0)
     {
        temp = x1; x1 = x2; x2 = temp;
        temp = y1; y1 = y2; y2 = temp;
     }
     k = (dx<<16) / dy;

     // Set middle pixels
     xt = (x1<<16) + k;
     for (ys=y1+1; ys<y2; ys++)
     {
        z = xt>>16;
        distance = xt - (z<<16);
        notdist = (1<<16) - distance;

        bgColor = getPixelConv(g,z, ys);
        red   = (distance*bgColor.r + notdist*rr) >> 16;
        green = (distance*bgColor.g + notdist*gg) >> 16;
        blue  = (distance*bgColor.b + notdist*bb) >> 16;
        setPixel(currentContext, g, z, ys, makePixel(red,green,blue));

        bgColor = getPixelConv(g,z+1, ys);
        red   = (notdist*bgColor.r + distance*rr) >> 16;
        green = (notdist*bgColor.g + distance*gg) >> 16;
        blue  = (notdist*bgColor.b + distance*bb) >> 16;
        setPixel(currentContext, g, z+1, ys, makePixel(red,green,blue));

        xt += k;
     }
  }

  // Set end pixel
  setPixel(currentContext, g, x2, y2, color_);
}

inline static void drawLine(Context currentContext, Object g, int32 x1, int32 y1, int32 x2, int32 y2, Pixel pixel)
{
   if (Graphics_useAA(g))
      drawLineAA(currentContext, g, x1,y1,x2,y2,pixel);
   else
      drawDottedLine(currentContext, g, x1, y1, x2, y2, pixel, pixel);
}

//   Draws a rectangle with the given color
static void drawRect(Context currentContext, Object g, int32 x, int32 y, int32 width, int32 height, Pixel pixel)
{
   drawHLine(currentContext, g, x, y, width, pixel, pixel);
   drawHLine(currentContext, g, x, y+height-1, width, pixel, pixel);
   drawVLine(currentContext, g, x, y, height, pixel, pixel);
   drawVLine(currentContext, g, x+width-1, y, height, pixel, pixel);
}

// Description:
//   Device specific routine.
//   Fills a rectangle with the given color
static void fillRect(Context currentContext, Object g, int32 x, int32 y, int32 width, int32 height, Pixel pixel)
{
   int32 clipX1 = Graphics_clipX1(g);
   int32 clipX2 = Graphics_clipX2(g);
   int32 clipY1 = Graphics_clipY1(g);
   int32 clipY2 = Graphics_clipY2(g);
   x += Graphics_transX(g);
   y += Graphics_transY(g);

   if (x < clipX1)             // line starts before clip x1
   {
      width -= clipX1-x;
      x = clipX1;
   }
   if ((x+width) > clipX2)     // line stops after clip x2
      width = clipX2-x;

   if (y < clipY1)             // line starts before clip y1
   {
      height -= clipY1-y;
      y = clipY1;
   }
   if ((y+height) > clipY2)    // line stops after clip y2
      height = clipY2-y;

#ifdef __gl2_h_
   if (x == 0 && width == appW && height == appH && Graphics_useOpenGL(g)) // in opengl is mandatory to do this at least once, per frame
   {
      PixelConv pc;
      pc.pixel = pixel;
      glClearColor((float)pc.r/(float)255,(float)pc.g/(float)255,(float)pc.b/(float)255,0);
      glClear(GL_COLOR_BUFFER_BIT);
      currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
      return;
   }
#endif

   if (height > 0 && width > 0)
   {
#ifdef __gl2_h_
      if (Graphics_useOpenGL(g))
      {
         glFillRect(x,y,width,height,pixel,255);
         currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
      }
      else
#endif
      {
         uint32 count;
         int32 pitch = Graphics_pitch(g);
         Pixel* to = getGraphicsPixels(g) + y * pitch + x;
         if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, x, y, width, height);
         if (x == 0 && width == pitch) // filling with full width?
         {
            int32* t = (int32*)to;
            int32 p2 = pixel;
            count = width*height;
            for (; count != 0; count--)
               *t++ = p2;
         }
         else
         {
            uint32 i = width, j = height;
            for (pitch -= width; j != 0;  to += pitch, i=width, j--)
               for (; i != 0; i--)
                  *to++ = pixel;
         }
      }
   }
}

#define INTERP(j,f) (j + (((f - j) * transparency) >> 4)) & 0xFF

static uint8 _ands8[8] = {0x80,0x40,0x20,0x10,0x08,0x04,0x02,0x01};
#ifdef __gl2_h_
extern GLfloat ftransp[16], f255[256];
extern GLfloat *glcoords, *glcolors;
#endif

static void drawText(Context currentContext, Object g, JCharP text, int32 chrCount, int32 x0, int32 y0, Pixel foreColor, int32 justifyWidth)
{
   Object fontObj = Graphics_font(g);
   int32 startBit,currentBit;
   uint8 *bitmapTable, *ands, *current, *start;
   uint16* bitIndexTable;
   int32 rowWIB,offset,xMin,xMax,yMin,yMax,x,y,yDif,width,height,spaceW=0,k,clipX2,pitch;
   Pixel transparency,*row0, *row;
   PixelConv *i;
   bool isNibbleStartingLow,isLowNibble,isAA;
   JChar ch,first,last;
   UserFont uf=null;
   PixelConv fc;
   int32 extraPixelsPerChar=0,extraPixelsRemaining=-1,rem;
   uint8 *ands8 = _ands8;
   int32 fcR,fcG,fcB;
#ifdef __gl2_h_
   GLfloat *glC, *glV;
#endif

   if (!text || chrCount == 0 || fontObj == null) return;

   fc.pixel = foreColor;
   fcR = fc.r;
   fcG = fc.g;
   fcB = fc.b;

#ifdef __gl2_h_
   flushPixels();
#endif

   uf = loadUserFontFromFontObj(currentContext, fontObj, ' ');
   if (uf == null) return;
   rowWIB = uf->rowWidthInBytes;
   bitIndexTable = uf->bitIndexTable;
   bitmapTable = uf->bitmapTable;
   first = uf->fontP.firstChar;
   last = uf->fontP.lastChar;

   isAA = uf->fontP.antialiased;
   height = uf->fontP.maxHeight;

   x0 += Graphics_transX(g);
   y0 += Graphics_transY(g);

   if (justifyWidth > 0)
   {
      while (text[chrCount-1] <= (JChar)' ')
         chrCount--;
      if (chrCount == 0) return;
      rem = justifyWidth - getJCharPWidth(currentContext, fontObj, text,chrCount);
      if (rem > 0)
      {
         extraPixelsPerChar   = rem / chrCount;
         extraPixelsRemaining = rem % chrCount;
      }
   }

   xMax = xMin = (x0 < Graphics_clipX1(g)) ? Graphics_clipX1(g) : x0;
   yMax = y0 + height;
   if (yMax >= Graphics_clipY2(g))
      yMax = Graphics_clipY2(g);
   yMin = (y0 < Graphics_clipY1(g))? Graphics_clipY1(g) : y0;
   row0 = getGraphicsPixels(g) + yMin * Graphics_pitch(g);
   yDif = yMin - y0;

   pitch = Graphics_pitch(g);
   clipX2 = Graphics_clipX2(g);

   for (k = 0; k < chrCount; k++) // guich@402
   {
      ch = *text++;
      if (ch <= ' ' || ch == 160)
      {
         if (ch == ' ' || ch == '\t' || ch == 160)
         {
            x0 += getJCharWidth(currentContext, fontObj, ch)+extraPixelsPerChar;
            if (k <= extraPixelsRemaining)
               x0++;
         }
         continue;
      }
      if (uf == null || ch < first || ch > last)
      {
         uf = loadUserFontFromFontObj(currentContext, fontObj, ch);
         if (uf == null || ch < uf->fontP.firstChar || ch > uf->fontP.lastChar) // invalid char - guich@tc122_23: must also check the font's range
         {
            x0 += spaceW ? spaceW : (spaceW=getJCharWidth(currentContext, fontObj, ' ')) + extraPixelsPerChar;
            if (k <= extraPixelsRemaining)
               x0++;
            continue;
         }
         rowWIB = uf->rowWidthInBytes;
         bitIndexTable = uf->bitIndexTable;
         bitmapTable = uf->bitmapTable;
         first = uf->fontP.firstChar;
         last = uf->fontP.lastChar;
#ifdef __gl2_h_
         checkGLfloatBuffer(currentContext, uf->fontP.maxHeight * uf->fontP.maxWidth);
#endif
      }
      // valid char, get its start
      offset = bitIndexTable[ch];
      width = bitIndexTable[ch+1] - offset;
      if ((xMax = x0 + width) > Graphics_clipX2(g))
         xMax = Graphics_clipX2(g);

      if (!isAA) // antialiased?
      {
         start     = bitmapTable + (offset >> 3) + rowWIB * yDif;
         startBit  = offset & 7;

         // draws the char, a row at a time
         for (y=yMin, row=row0; y < yMax; start+=rowWIB, x -= width, y++, row += pitch)
         {
            current = start;
            ands = ands8 + (currentBit = startBit);
            for (x=x0; x < xMax; x++)
            {
               if ((*current & *ands++) != 0 && x >= xMin)
                  row[x] = foreColor;
               if (++currentBit == 8)   // finished this uint8?
               {
                  currentBit = 0;       // reset counter
                  ands = ands8;         // reset test bit pointer
                  ++current;            // inc current uint8
               }
            }
         }
      }
      else
      {
         start = bitmapTable + (offset >> 1) + rowWIB * yDif;
         isNibbleStartingLow = (offset & 1) == 1;
#ifdef __gl2_h_
         // draws the char, a row at a time
         if (Graphics_useOpenGL(g))
         {
            glC = glcolors;
            glV = glcoords;
            for (y=yMin; y < yMax; start+=rowWIB, x -= width, y++)
            {
               current = start;
               isLowNibble = isNibbleStartingLow;
               for (x=x0; x < xMax; x++)
               {
                  transparency = isLowNibble ? (*current++ & 0xF) : ((*current >> 4) & 0xF);
                  isLowNibble = !isLowNibble;
                  if (transparency == 0 || x < xMin)
                     continue;

                  // alpha
                  *glC++ = ftransp[transparency];
                  // vertices
                  *glV++ = x;
                  *glV++ = y;
               }
            }
            if (glC != glcolors) // flush vertices buffer
               glDrawPixels(((int32)(glC-glcolors)),foreColor);
         }
         else
#endif
            for (y=yMin, row=row0; y < yMax; start+=rowWIB, x -= width, y++, row += pitch)
            {
               current = start;
               isLowNibble = isNibbleStartingLow;
               i = (PixelConv*)&row[x0];
               for (x=x0; x < xMax; x++,i++)
               {
                  transparency = isLowNibble ? (*current++ & 0xF) : ((*current >> 4) & 0xF);
                  isLowNibble = !isLowNibble;
                  if (transparency == 0 || x < xMin)
                     continue;
                  if (transparency == 0xF)
                     i->pixel = foreColor;
                  else
                  {
                     i->r = INTERP(i->r, fcR);
                     i->g = INTERP(i->g, fcG);
                     i->b = INTERP(i->b, fcB);
                  }
               }
            }
      }
      if (xMax >= clipX2)
      {
         xMax = clipX2;
         break;
      }
      x0 = xMax; // next character
      x0 += extraPixelsPerChar;
      if (k <= extraPixelsRemaining)
         x0++;
   }
#ifndef __gl2_h_
   if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, xMin, yMin, (xMax - xMin), (yMax - yMin));
#else
   currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
#endif
}

static SurfaceType getSurfaceType(Context currentContext, Object surface)
{
   // cache class pointers for performance
   return (surface != NULL && areClassesCompatible(currentContext, OBJ_CLASS(surface), "totalcross.ui.image.Image") == 1) == COMPATIBLE ? SURF_IMAGE : SURF_CONTROL;
}

////////////////////////////////////////////////////////////////////////////
inline static void quadPixel(Context currentContext, Object g, int32 xc, int32 yc, int32 x, int32 y, Pixel c)
{
   // draw 4 points using symetry
   setPixel(currentContext, g,xc + x, yc + y, c);
   setPixel(currentContext, g,xc + x, yc - y, c);
   setPixel(currentContext, g,xc - x, yc + y, c);
   setPixel(currentContext, g,xc - x, yc - y, c);
}

inline static void quadLine(Context currentContext, Object g, int32 xc, int32 yc, int32 x, int32 y, Pixel c)
{
   int32 w = x+x+1; // plus 1 for the drawHLine (draws to width-1)
   // draw 2 lines using symetry
   drawHLine(currentContext, g,xc - x, yc - y, w, c, c);
   drawHLine(currentContext, g,xc - x, yc + y, w, c, c);
}

// draws an ellipse incrementally
static void ellipseDrawAndFill(Context currentContext, Object g, int32 xc, int32 yc, int32 rx, int32 ry, Pixel c1, Pixel c2, bool fill, bool gradient)
{
   int32 numSteps=0, startRed=0, startGreen=0, startBlue=0, endRed=0, endGreen=0, endBlue=0, redInc=0, greenInc=0, blueInc=0, red=0, green=0, blue=0;
   PixelConv c;
   // intermediate terms to speed up loop
   int32 t1 = rx*rx, t2 = t1<<1, t3 = t2<<1;
   int32 t4 = ry*ry, t5 = t4<<1, t6 = t5<<1;
   int32 t7 = rx*t5, t8 = t7<<1, t9 = 0L;
   int32 d1 = t2 - t7 + (t4>>1);    // error terms
   int32 d2 = (t1>>1) - t8 + t5;
   int32 x = rx;      // ellipse points
   int32 y = 0;       // ellipse points
   if (rx < 0 || ry < 0) // guich@501_13
      return;

   if (gradient)
   {
      numSteps = ry + ry; // guich@tc110_11: support horizontal gradient
      startRed = (c1 >> 16) & 0xFF;
      startGreen = (c1 >> 8) & 0xFF;
      startBlue = c1 & 0xFF;
      endRed = (c2 >> 16) & 0xFF;
      endGreen = (c2 >> 8) & 0xFF;
      endBlue = c2 & 0xFF;
      redInc = ((endRed - startRed) << 16) / numSteps;
      greenInc = ((endGreen - startGreen) << 16) / numSteps;
      blueInc = ((endBlue - startBlue) << 16) / numSteps;
      red = startRed << 16;
      green = startGreen << 16;
      blue = startBlue << 16;
   }
   else c.pixel = c1;

   while (d2 < 0)          // til slope = -1
   {
      if (gradient)
      {
         c.r = (red >> 16) & 0xFF;
         c.g = (green >> 16) & 0xFF;
         c.b = (blue >> 16) & 0xFF;
         red += redInc;
         green += greenInc;
         blue += blueInc;
      }
      if (fill)
         quadLine(currentContext, g,xc,yc,x,y,c.pixel);
      else
         quadPixel(currentContext, g,xc,yc,x,y,c.pixel);
      y++;          // always move up here
      t9 += t3;
      if (d1 < 0)   // move straight up
      {
         d1 += t9 + t2;
         d2 += t9;
      }
      else        // move up and left
      {
         --x;
         t8 -= t6;
         d1 += t9 + t2 - t8;
         d2 += t9 + t5 - t8;
      }
   }

   do             // rest of top right quadrant
   {
      if (gradient)
      {
         c.r = (red >> 16) & 0xFF;
         c.g = (green >> 16) & 0xFF;
         c.b = (blue >> 16) & 0xFF;
         red += redInc;
         green += greenInc;
         blue += blueInc;
      }
      // draw 4 points using symmetry
      if (fill)
         quadLine(currentContext, g,xc,yc,x,y,c.pixel);
      else
         quadPixel(currentContext, g,xc,yc,x,y,c.pixel);
      --x;        // always move left here
      t8 -= t6;
      if (d2 < 0)  // move up and left
      {
         ++y;
         t9 += t3;
         d2 += t9 + t5 - t8;
      }
      else d2 += t5 - t8; // move straight left
   } while (x >= 0);
}

////////////////////////////////////////////////////////////////////////////
static void drawDottedRect(Context currentContext, Object g, int32 x, int32 y, int32 w, int32 h, Pixel c1, Pixel c2)
{
   if (w > 0 && h > 0)
   {
      int32 x2 = x+w-1;
      int32 y2 = y+h-1;
      drawDottedLine(currentContext, g,x, y, x2,y, c1, c2);
      drawDottedLine(currentContext, g,x, y, x ,y2, c1, c2);
      drawDottedLine(currentContext, g,x2, y, x2, y2, c1, c2);
      drawDottedLine(currentContext, g,x, y2, x2, y2, c1, c2);
   }
}
////////////////////////////////////////////////////////////////////////////
// Generalized Polygon Fill
static void qsortInts(int32 *items, int32 first, int32 last)
{
   int32 low = first;
   int32 high = last, mid;
   if (first >= last)
      return;
   mid = items[(first+last) >> 1];
   while (true)
   {
      while (high >= low && items[low] < mid) // guich@566_25: added "high > low" here and below - guich@568_5: changed to >=
         low++;
      while (high >= low && items[high] > mid)
         high--;
      if (low <= high)
      {
         int32 temp = items[low];
         items[low++] = items[high];
         items[high--] = temp;
      }
      else break;
   }
   if (first < high)
      qsortInts(items, first,high);
   if (low < last)
      qsortInts(items, low,last);
}

static Object growIntArray(Context currentContext, Object oldArrayObj, int32 newLen) // must unlock the returned obj
{
   Object newArrayObj = createArrayObject(currentContext, INT_ARRAY, newLen);
   int32 *newArray,*oldArray, oldLen;
   if (newArrayObj != null)
   {
      newArray = (int32*)ARRAYOBJ_START(newArrayObj);
      oldArray = (int32*)ARRAYOBJ_START(oldArrayObj);
      oldLen = ARRAYOBJ_LEN(oldArrayObj);
      xmemmove(newArray, oldArray, oldLen * 4);
   }
   return newArrayObj;
}

static void fillPolygon(Context currentContext, Object g, int32 *xPoints1, int32 *yPoints1, int32 nPoints1, int32 *xPoints2, int32 *yPoints2, int32 nPoints2, Pixel c1, Pixel c2, bool gradient)
{
   int32 x1, y1, x2, y2,y,n=0,temp, i,j, miny, maxy, a, numSteps=0, startRed=0, startGreen=0, startBlue=0, endRed=0, endGreen=0, endBlue=0, redInc=0, greenInc=0, blueInc=0, red=0, green=0, blue=0;
   int32 *yp;
   int32 *axPoints[2], *ayPoints[2], anPoints[2];
   Object *intsObj = &Graphics_ints(g);
   int32 *ints = *intsObj ? (int32*)ARRAYOBJ_START(*intsObj) : null;
   PixelConv c;

   if (!xPoints1 || !yPoints1 || nPoints1 < 2)
      return;

   axPoints[0] = xPoints1; ayPoints[0] = yPoints1; anPoints[0] = nPoints1;
   axPoints[1] = xPoints2; ayPoints[1] = yPoints2; anPoints[1] = nPoints2;

   yp = yPoints1;
   miny = maxy = *yp++;
   for (i = nPoints1; --i > 0; yp++)
   {
      if (*yp < miny) miny = *yp;
      if (*yp > maxy) maxy = *yp;
   }
   yp = yPoints2;
   for (i = nPoints2; --i >= 0; yp++)
   {
      if (*yp < miny) miny = *yp;
      if (*yp > maxy) maxy = *yp;
   }

   if (ints == null)
   {
      *intsObj = createArrayObject(currentContext, INT_ARRAY, 2); // 2 is the most used length
      if (*intsObj == null)
         return;
      setObjectLock(*intsObj, UNLOCKED);
      ints = (int32*)ARRAYOBJ_START(*intsObj);
   }
   if (gradient)
   {
      numSteps = maxy - miny; // guich@tc110_11: support horizontal gradient
      if (numSteps == 0) numSteps = 1; // guich@tc115_86: prevent divide by 0
      c.pixel = c1;
      startRed   = c.r;
      startGreen = c.g;
      startBlue  = c.b;
      c.pixel = c2;
      endRed   = c.r;
      endGreen = c.g;
      endBlue  = c.b;
      redInc = ((endRed - startRed) << 16) / numSteps;
      greenInc = ((endGreen - startGreen) << 16) / numSteps;
      blueInc = ((endBlue - startBlue) << 16) / numSteps;
      red = startRed << 16;
      green = startGreen << 16;
      blue = startBlue << 16;
   }
   else c.pixel = c1;
   for (y = miny; y <= maxy; y++)
   {
      n = 0;
      for (a = 0; a < 2; a++)
      {
         int32 nPoints = anPoints[a];
         int32* xPoints = axPoints[a];
         int32* yPoints = ayPoints[a];
         j = nPoints-1;
         for (i = 0; i < nPoints; j=i,i++)
         {
            y1 = yPoints[j];
            y2 = yPoints[i];
            if (y1 == y2)
               continue;
            if (y1 > y2) // invert
            {
               temp = y1;
               y1 = y2;
               y2 = temp;
            }
            // compute next x point
            if ( (y1 <= y && y < y2) || (y == maxy && y1 < y && y <= y2) )
            {
               if (n == (int32)ARRAYOBJ_LEN(*intsObj)) // have to grow the ints array?
               {
                  Object newIntsObj = growIntArray(currentContext, *intsObj, n * 2);
                  if (newIntsObj == null)
                     return;
                  *intsObj = newIntsObj;
                  setObjectLock(*intsObj, UNLOCKED);
                  ints = (int32*)ARRAYOBJ_START(*intsObj);
               }
               if (yPoints[j] < yPoints[i])
               {
                  x1 = xPoints[j];
                  x2 = xPoints[i];
               }
               else
               {
                  x2 = xPoints[j];
                  x1 = xPoints[i];
               }
               ints[n++] = (y - y1) * (x2 - x1) / (y2 - y1) + x1;
            }
         }
      }
      if (n >= 2)
      {
         if (gradient)
         {
            c.r = (red   >> 16) & 0xFF;
            c.g = (green >> 16) & 0xFF;
            c.b = (blue  >> 16) & 0xFF;
            red += redInc;
            green += greenInc;
            blue += blueInc;
         }
         if (n == 2) // most of the times
         {
            if (ints[1] > ints[0])
               drawHLine(currentContext, g,ints[0],y,ints[1]-ints[0]+1,c.pixel,c.pixel);
            else
               drawHLine(currentContext, g,ints[1],y,ints[0]-ints[1]+1,c.pixel,c.pixel);
         }
         else
         {
            qsortInts(ints, 0, n-1);
            for (n>>=1, yp = ints; --n >= 0; yp+=2)
               drawHLine(currentContext, g,yp[0],y,yp[1]-yp[0]+1,c.pixel,c.pixel);
         }
      }
   }
}

////////////////////////////////////////////////////////////////////////////
// draws a polygon. if the polygon is not closed, close it

static void drawPolygon(Context currentContext, Object g, int32 *xPoints1, int32 *yPoints1, int32 nPoints1, int32 *xPoints2, int32 *yPoints2, int32 nPoints2, Pixel pixel)
{
   int32 i;
   if (!xPoints1 || !yPoints1 || nPoints1 < 2)
      return;
   for (i=1; i < nPoints1; i++)
      drawLine(currentContext, g,xPoints1[i-1], yPoints1[i-1], xPoints1[i], yPoints1[i], pixel);
   for (i=1; i < nPoints2; i++)
      drawLine(currentContext, g,xPoints2[i-1], yPoints2[i-1], xPoints2[i], yPoints2[i], pixel);
}
////////////////////////////////////////////////////////////////////////////
// draw an elliptical arc from startAngle to endAngle.
// c is the fill color and c2 is the outline color
// (if in fill mode - otherwise, c = outline color)
static void arcPiePointDrawAndFill(Context currentContext, Object g, int32 xc, int32 yc, int32 rx, int32 ry, double startAngle, double endAngle, Pixel c, Pixel c2, bool fill, bool pie, bool gradient)
{
   int32 limitx1 = Graphics_clipX1(g) - Graphics_transX(g); // guich@501_13: store these for faster computation
   int32 limitx2 = Graphics_clipX2(g) - Graphics_transX(g);
   int32 limity1 = Graphics_clipY1(g) - Graphics_transY(g);
   int32 limity2 = Graphics_clipY2(g) - Graphics_transY(g);
   // this algorithm was created by Guilherme Campos Hazan
   double ppd;
   int32 startIndex,endIndex,index,i,nq,size=0,oldX1=0,oldY1=0,last,oldX2=0,oldY2=0;
   bool sameR, sameC;
   bool checkClipX = (bool)((xc+rx) > limitx2 || (xc-rx) < limitx1); // guich@340_3 - guich@401_2: added transX/Y
   bool checkClipY = (bool)((yc+ry) > limity2 || (yc-ry) < limity1);
   Object *xPointsObj = &Graphics_xPoints(g);
   Object *yPointsObj = &Graphics_yPoints(g);
   int32 *xPoints = *xPointsObj ? (int32*)ARRAYOBJ_START(*xPointsObj) : null;
   int32 *yPoints = *yPointsObj ? (int32*)ARRAYOBJ_START(*yPointsObj) : null;

   if (rx < 0 || ry < 0) // guich@501_13
      return;
   // make sure the values are -359 <= x <= 359
   while (startAngle <= -360) startAngle += 360;
   while (endAngle   <= -360) endAngle   += 360;
   while (startAngle >   360) startAngle -= 360;
   while (endAngle   >   360) endAngle   -= 360;

   if (startAngle == endAngle) // guich@501_13
      return;
   if (startAngle > endAngle) // eg 235 to 45
      startAngle -= 360; // set to -45 to 45 so we can handle it correctly
   if (startAngle >= 0 && endAngle <= 0) // eg 135 to -135
      endAngle += 360; // set to 135 to 225

   // step 0: correct angle values
   if (startAngle < 0.1 && endAngle > 359.9) // full circle? use the fastest routine instead
   {
      if (fill)
         ellipseDrawAndFill(currentContext, g,xc, yc, rx, ry, c, c2, true, gradient);
      ellipseDrawAndFill(currentContext, g,xc, yc, rx, ry, c, c, false, gradient);
      return;
   }

   // step 0: if possible, use cached results
   sameR = rx == Graphics_lastRX(g) && ry == Graphics_lastRY(g);
   sameC = xc == Graphics_lastXC(g) && yc == Graphics_lastYC(g);
   if (!sameR || !sameC)
   {
      // step 1: computes how many points the circle has (computes only 45 degrees and mirrors the rest)
      // intermediate terms to speed up loop
      int32 t1 = rx*rx, t2 = t1<<1, t3 = t2<<1;
      int32 t4 = ry*ry, t5 = t4<<1, t6 = t5<<1;
      int32 t7 = rx*t5, t8 = t7<<1, t9 = 0L;
      int32 d1 = t2 - t7 + (t4>>1);    // error terms
      int32 d2 = (t1>>1) - t8 + t5;
      int32 x = rx;                 // ellipse points
      int32 y = 0;                  // ellipse points

      if (sameR)
         size = (Graphics_lastSize(g)-2)/4;
      else
      {
         while (d2 < 0)              // til slope = -1
         {
            t9 += t3;
            if (d1 < 0)             // move straight up
            {
               d1 += t9 + t2;
               d2 += t9;
            }
            else                   // move up and left
            {
               --x;
               t8 -= t6;
               d1 += t9 + t2 - t8;
               d2 += t9 + t5 - t8;
            }
            ++size;
         }

         do             // rest of top right quadrant
         {
            --x;         // always move left here
            t8 -= t6;
            if (d2 < 0)  // move up and left
            {
               t9 += t3;
               d2 += t9 + t5 - t8;
            }
            else d2 += t5 - t8;  // move straight left
            ++size;
         } while (x >= 0);
      }
      nq = size;
      size *= 4;
      // step 2: computes how many points per degree
      ppd = (double)size / 360.0f;
      // step 3: create space in the buffer so it can save all the circle
      size+=2;
      if (xPoints == null || ARRAYOBJ_LEN(*xPointsObj) < (uint32)size)
      {
         *xPointsObj = createArrayObject(currentContext, INT_ARRAY, size);
         if (*xPointsObj == null)
            return;
         *yPointsObj = createArrayObject(currentContext, INT_ARRAY, size);
         if (*yPointsObj == null)
         {
            setObjectLock(*xPointsObj, UNLOCKED);
            return;
         }
         setObjectLock(*xPointsObj, UNLOCKED);
         setObjectLock(*yPointsObj, UNLOCKED);
      }
      xPoints = (int32*)ARRAYOBJ_START(*xPointsObj);
      yPoints = (int32*)ARRAYOBJ_START(*yPointsObj);

      // step 4: stores all the circle in the array. the odd arcs are drawn in reverse order
      // intermediate terms to speed up loop
      if (!sameR)
      {
         t2 = t1<<1;
         t3 = t2<<1;
         t8 = t7<<1;
         t9 = 0;
         d1 = t2 - t7 + (t4>>1); // error terms
         d2 = (t1>>1) - t8 + t5;
         x = rx;
      }
      i=0;
      while (d2 < 0)          // til slope = -1
      {
         // save 4 points using symmetry
         // guich@340_3: added clipping
         #define doClipX(x) ( ((x) < limitx1) ? limitx1 : ((x) >= limitx2) ? limitx2 : (x) ) // guich@401_2: added transX/Y
         #define doClipY(y) ( ((y) < limity1) ? limity1 : ((y) >= limity2) ? limity2 : (y) )

         index = nq*0+i;      // 0/3
         xPoints[index]=checkClipX?doClipX(xc+x):(xc+x);
         yPoints[index]=checkClipY?doClipY(yc-y):(yc-y);

         index = (nq<<1)-i-1;    // 1/3
         xPoints[index]=checkClipX?doClipX(xc-x):(xc-x);
         yPoints[index]=checkClipY?doClipY(yc-y):(yc-y);

         index = (nq<<1)+i;      // 2/3
         xPoints[index]=checkClipX?doClipX(xc-x):(xc-x);
         yPoints[index]=checkClipY?doClipY(yc+y):(yc+y);

         index = (nq<<2)-i-1;    // 3/3
         xPoints[index]=checkClipX?doClipX(xc+x):(xc+x);
         yPoints[index]=checkClipY?doClipY(yc+y):(yc+y);

         i++;
         y++;        // always move up here
         t9 += t3;
         if (d1 < 0)  // move straight up
         {
             d1 += t9 + t2;
             d2 += t9;
         }
         else      // move up and left
         {
             x--;
             t8 -= t6;
             d1 += t9 + t2 - t8;
             d2 += t9 + t5 - t8;
         }
      }

      do             // rest of top right quadrant
      {
         // save 4 points using symmetry
         // guich@340_3: added clipping
         index = nq*0+i;    // 0/3
         xPoints[index]=checkClipX?doClipX(xc+x):(xc+x);
         yPoints[index]=checkClipY?doClipY(yc-y):(yc-y);

         index = (nq<<1)-i-1;  // 1/3
         xPoints[index]=checkClipX?doClipX(xc-x):(xc-x);
         yPoints[index]=checkClipY?doClipY(yc-y):(yc-y);

         index = (nq<<1)+i;    // 2/3
         xPoints[index]=checkClipX?doClipX(xc-x):(xc-x);
         yPoints[index]=checkClipY?doClipY(yc+y):(yc+y);

         index = (nq<<2)-i-1;  // 3/3
         xPoints[index]=checkClipX?doClipX(xc+x):(xc+x);
         yPoints[index]=checkClipY?doClipY(yc+y):(yc+y);

         ++i;
         --x;        // always move left here
         t8 -= t6;
         if (d2 < 0)  // move up and left
         {
            ++y;
            t9 += t3;
            d2 += t9 + t5 - t8;
         }
         else d2 += t5 - t8;   // move straight left
      } while (x >= 0);
      // save last arguments
      Graphics_lastXC(g)   = xc;
      Graphics_lastYC(g)   = yc;
      Graphics_lastRX(g)   = rx;
      Graphics_lastRY(g)   = ry;
      Graphics_lastPPD(g)  = ppd;
      Graphics_lastSize(g) = size;
   }
   else
   {
      size = Graphics_lastSize(g);
      ppd = Graphics_lastPPD(g);
   }
   // step 5: computes the start and end indexes that will become part of the arc
   if (startAngle < 0)
      startAngle += 360;
   if (endAngle < 0)
      endAngle += 360;
   startIndex = (int32)(ppd * startAngle);
   endIndex = (int32)(ppd * endAngle);

   last = size-2;
   if (endIndex >= last) // 360?
      endIndex--;
   // step 6: fill or draw the polygons
   endIndex++;
   if (pie)
   {
      // connect two lines from the center to the two edges of the arc
      oldX1 = xPoints[endIndex];
      oldY1 = yPoints[endIndex];
      oldX2 = xPoints[endIndex+1];
      oldY2 = yPoints[endIndex+1];
      xPoints[endIndex] = xc;
      yPoints[endIndex] = yc;
      xPoints[endIndex+1] = xPoints[startIndex];
      yPoints[endIndex+1] = yPoints[startIndex];
      endIndex+=2;
   }

   if (startIndex > endIndex) // drawing from angle -30 to +30 ? (startIndex = 781, endIndex = 73, size=854)
   {
      int p1 = last-startIndex;
      if (fill)
         fillPolygon(currentContext, g, xPoints+startIndex, yPoints+startIndex, p1, xPoints, yPoints, endIndex, gradient ? c : c2, c2, gradient); // lower half, upper half
      if (!gradient) drawPolygon(currentContext, g, xPoints+startIndex, yPoints+startIndex, p1, xPoints, yPoints, endIndex, c);
   }
   else
   {
      if (fill)
         fillPolygon(currentContext, g, xPoints+startIndex, yPoints+startIndex, endIndex-startIndex, 0,0,0, gradient ? c : c2, c2, gradient);
      if (!gradient) drawPolygon(currentContext, g, xPoints+startIndex, yPoints+startIndex, endIndex-startIndex, 0,0,0, c);
   }
   if (pie)  // restore saved points
   {
      endIndex-=2;
      xPoints[endIndex]   = oldX1;
      yPoints[endIndex]   = oldY1;
      xPoints[endIndex+1] = oldX2;
      yPoints[endIndex+1] = oldY2;
   }
}
////////////////////////////////////////////////////////////////////////////
static void drawRoundRect(Context currentContext, Object g, int32 x, int32 y, int32 width, int32 height, int32 r, Pixel c)
{
   int32 x1, y1, x2, y2, dec, xx, yy;
   int32 w, h;
   r = min32(r,min32(width/2,height/2));
   w = width - 2*r;
   h = height - 2*r;
   x1 = x+r;
   y1 = y+r;
   x2 = x+width-r-1;
   y2 = y+height-r-1;
   dec = 3-2*r;

   drawHLine(currentContext, g,x+r, y, w, c, c); // top
   drawHLine(currentContext, g,x+r, y+height-1, w, c, c); // bottom
   drawVLine(currentContext, g,x, y+r, h, c, c); // left
   drawVLine(currentContext, g,x+width-1, y+r, h, c, c); // right

   // draw the round rectangles.
   for (xx = 0, yy = r; xx <= yy; xx++)
   {
      setPixel(currentContext, g,x2+xx, y2+yy, c);
      setPixel(currentContext, g,x2+xx, y1-yy, c);
      setPixel(currentContext, g,x1-xx, y2+yy, c);
      setPixel(currentContext, g,x1-xx, y1-yy, c);

      setPixel(currentContext, g,x2+yy, y2+xx, c);
      setPixel(currentContext, g,x2+yy, y1-xx, c);
      setPixel(currentContext, g,x1-yy, y2+xx, c);
      setPixel(currentContext, g,x1-yy, y1-xx, c);
      if (dec >= 0)
         dec += -4*(yy--)+4;
      dec += 4*xx+6;
   }
}
////////////////////////////////////////////////////////////////////////////
static void fillRoundRect(Context currentContext, Object g, int32 x, int32 y, int32 width, int32 height, int32 r, Pixel c)
{
   int32 x1, y1, x2, y2, xx, yy, dec;
   if (r > (width/2) || r > (height/2)) r = min32(width/2,height/2); // guich@200b4_6: correct bug that crashed the device.

   x1 = x+r;
   y1=y+r;
   x2=x+width-r-1;
   y2=y+height-r-1;
   dec = 3-2*r;

   height -= 2*r;
   y += r;
   while (height--)
      drawHLine(currentContext, g,x, y++, width, c, c);
   // fill the round rectangles
   for (xx = 0, yy = r; xx <= yy; xx++)
   {
      drawLine(currentContext, g,x1-xx, y1-yy, x2+xx, y1-yy, c);
      drawLine(currentContext, g,x1-xx, y2+yy, x2+xx, y2+yy, c);

      drawLine(currentContext, g,x1-yy, y1-xx, x2+yy, y1-xx, c);
      drawLine(currentContext, g,x1-yy, y2+xx, x2+yy, y2+xx, c);

      if (dec >= 0)
         dec += -4*(yy--)+4;
      dec += 4*xx+6;
   }
}

////////////////////////////////////////////////////////////////////////////
// sets the clip rect to (x,y,x+w-1,y+h-1), translated to the current translated origin
static void setClip(Object g, int32 x, int32 y, int32 w, int32 h)
{
   int32 clipX1 = x+Graphics_transX(g);
   int32 clipY1 = y+Graphics_transY(g);
   int32 clipX2 = clipX1+w;
   int32 clipY2 = clipY1+h;

   if (clipX1 < Graphics_minX(g)) clipX1 = Graphics_minX(g);
   if (clipY1 < Graphics_minY(g)) clipY1 = Graphics_minY(g);
   if (clipX1 > Graphics_maxX(g)) clipX1 = Graphics_maxX(g);
   if (clipY1 > Graphics_maxY(g)) clipY1 = Graphics_maxY(g);

   if (clipX2 < Graphics_minX(g)) clipX2 = Graphics_minX(g);
   if (clipY2 < Graphics_minY(g)) clipY2 = Graphics_minY(g);
   if (clipX2 > Graphics_maxX(g)) clipX2 = Graphics_maxX(g);
   if (clipY2 > Graphics_maxY(g)) clipY2 = Graphics_maxY(g);

   if (clipX2 > screen.screenW) clipX2 = screen.screenW;
   if (clipY2 > screen.screenH) clipY2 = screen.screenH;

   Graphics_clipX1(g) = clipX1;
   Graphics_clipY1(g) = clipY1;
   Graphics_clipX2(g) = clipX2;
   Graphics_clipY2(g) = clipY2;
}

// Translates the given coords and returns the intersection between
// the clip rect and the coords passed.
// Returns: 1 if OK, 0 if the coords are outside the clip rect
static bool translateAndClip(Object g, int32 *pX, int32 *pY, int32 *pWidth, int32 *pHeight)
{
   int32 x = *pX;
   int32 y = *pY;
   int32 w = *pWidth;
   int32 h = *pHeight;
   x += Graphics_transX(g);
   y += Graphics_transY(g);
   if (x < Graphics_clipX1(g))
   {
      if ((x+w) > Graphics_clipX2(g))
         w = Graphics_clipX2(g) - Graphics_clipX1(g);
      else
         w -= Graphics_clipX1(g)-x;
      x = Graphics_clipX1(g);
   }
   else
   if ((x+w) > Graphics_clipX2(g))
      w = Graphics_clipX2(g) - x;
   if (y < Graphics_clipY1(g))
   {
      if ((y+h) > Graphics_clipY2(g))
         h = Graphics_clipY2(g) - Graphics_clipY1(g);
      else
         h -= Graphics_clipY1(g)-y;
      y = Graphics_clipY1(g);
   }
   else
   if ((y+h) > Graphics_clipY2(g))
      h = Graphics_clipY2(g) - y;

   if (x < 0 || y < 0 || h <= 0 || w <= 0) return false; // guich@566_42: check the resulting w/h - guich@tc112_34: check also x and y

   *pX      = x;
   *pY      = y;
   *pWidth  = w;
   *pHeight = h;
   return true;
}

static void createGfxSurface(int32 w, int32 h, Object g, SurfaceType stype)
{
   Graphics_clipX2(g) = Graphics_width (g) = w;
   Graphics_clipY2(g) = Graphics_height(g) = h;
   if (stype == SURF_IMAGE)
      Graphics_pitch(g) = w;
   else
      Graphics_pitch(g) = screen.screenW;
}

#define BITMAP_PTR(p, dline, pitch)      (((uint8*)p) + (dline * pitch))
#define IS_PITCH_OPTIMAL(w, pitch, bpp)  (((uint32)w * (uint32)bpp / 8) == (uint32)pitch) // 240 * 32 / 8 == 960 ?

int32 *shiftYfield, *shiftHfield, *lastShiftYfield, lastShiftY=-1;
static bool firstUpdate = true;

#ifdef darwin
static int32 lastAppHeightOnSipOpen;
extern int keyboardH,realAppH;

static void checkKeyboardAndSIP(Context currentContext, int32 *shiftY, int32 *shiftH)
{
   int32 appHeightOnSipOpen = screen.screenH - keyboardH;
   if (appHeightOnSipOpen != lastAppHeightOnSipOpen)
   {
      lastAppHeightOnSipOpen = appHeightOnSipOpen;
      markWholeScreenDirty(currentContext);
   }
   if ((*shiftY + *shiftH) > screen.screenH)
      *shiftH = screen.screenH - *shiftY;

   if ((*shiftY + *shiftH) < appHeightOnSipOpen) // don't shift the screen if above
      *shiftY = 0;
   else
   {
      *shiftY -= appHeightOnSipOpen - *shiftH;
      *shiftH = appHeightOnSipOpen ;
   }
}
#elif defined(ANDROID)
extern int realAppH;
static int32 lastAppHeightOnSipOpen;
void markWholeScreenDirty(Context currentContext);
static int desiredShiftY=-1;
static void checkKeyboardAndSIP(Context currentContext, int32 *shiftY, int32 *shiftH)
{
   JNIEnv *env = getJNIEnv();
   if (env == null) return;

   if ((*env)->GetStaticBooleanField(env, applicationClass, jhardwareKeyboardIsVisible))
      *shiftY = *shiftH = 0;
   else
   {
      bool sipVisible = (*env)->GetStaticBooleanField(env, applicationClass, jsipVisible);
      // we know the height if:
      // 1. Portrait and Android 2.x
      // 2. Portrait/landscape and Android 3.x
      if (sipVisible && (screen.screenH > screen.screenW || *tcSettings.romVersionPtr >= 11))
      {
         int32 appHeightOnSipOpen = (*env)->CallStaticIntMethod(env, applicationClass, jgetHeight);
         int32 appTitleH = (*env)->GetStaticIntField(env, applicationClass, jappTitleH);
         // when application is in full screen, this function would erase a possibly valid shiftY value;
         // so, here i store the desired shiftY and restore it when the application is really not full screen
         bool isFullScreen = appTitleH != 0 && (appTitleH+appHeightOnSipOpen) == screen.screenH;
         if (!isFullScreen && desiredShiftY != -1)
         {
            *shiftY = desiredShiftY;
            desiredShiftY = -1;
         }
         else
         if (isFullScreen && desiredShiftY == -1)
            desiredShiftY = *shiftY;

         if (appHeightOnSipOpen != lastAppHeightOnSipOpen)
         {
            lastAppHeightOnSipOpen = appHeightOnSipOpen;
            markWholeScreenDirty(currentContext);
         }
         if ((*shiftY + *shiftH) > screen.screenH)
            *shiftH = screen.screenH - *shiftY;

         if ((*shiftY + *shiftH) < appHeightOnSipOpen) // don't shift the screen if above
            *shiftY = 0;
         else
         {
            *shiftY -= appHeightOnSipOpen - *shiftH;
            *shiftH = appHeightOnSipOpen ;
         }
      }
   }
}
#endif

// not used with opengl
static bool updateScreenBits(Context currentContext) // copy the 888 pixels to the native format
{
   int32 y, screenW, screenH, shiftY=0, shiftH=0;
   uint32 count;
   TCClass window;
   PixelConv gray;
   gray.pixel = *shiftScreenColorP;

   if (screen.mainWindowPixels == null || ARRAYOBJ_LEN(screen.mainWindowPixels) < (uint32)(screen.screenW * screen.screenH))
      return false;

   if (!graphicsLock(&screen, true))
   {
      if (firstUpdate)
         throwException(currentContext, RuntimeException, "Cannot lock screen");
      return false;
   }
   firstUpdate = false;

   if (shiftYfield == null && (window = loadClass(currentContext, "totalcross.ui.Window", false)) != null)
   {
      shiftYfield = getStaticFieldInt(window, "shiftY");
      shiftHfield = getStaticFieldInt(window, "shiftH");
      lastShiftYfield = getStaticFieldInt(window, "lastShiftY");
      if (shiftYfield == null)
         return false;
   }
   shiftY = *shiftYfield;
   shiftH = *shiftHfield;
#if defined ANDROID || defined darwin
   checkKeyboardAndSIP(currentContext, &shiftY,&shiftH);
#ifdef ANDROID
   if (*shiftYfield != shiftY && lastAppHeightOnSipOpen != screen.screenH)
#else
   if (*shiftYfield != shiftY && lastAppHeightOnSipOpen != realAppH)
#endif
   {
      *lastShiftYfield = *shiftYfield = shiftY;
      *shiftHfield = shiftH;
   }
#endif
   screenW = screen.screenW;
   screenH = screen.screenH;

   if ((shiftY+shiftH) > screen.screenH)
      shiftH = screen.screenH - shiftY;
   if (shiftY != 0 && shiftH <= 0)
      return false;

   if (!currentContext->fullDirty && shiftY != 0) // *1* clip dirty Y values to screen shift area
   {
      if (shiftY != lastShiftY) // the first time a shift is made, we must paint everything, to let the gray part be painted
      {
         lastShiftY = shiftY;
         markWholeScreenDirty(currentContext);
      }
      else
      {
         if (currentContext->dirtyY1 <   shiftY)         currentContext->dirtyY1 = shiftY;
         if (currentContext->dirtyY2 >= (shiftY+shiftH)) currentContext->dirtyY2 = shiftY+shiftH;
         currentContext->dirtyY1 -= shiftY;
         currentContext->dirtyY2 = currentContext->dirtyY1 + min32(currentContext->dirtyY2-(currentContext->dirtyY1+shiftY), shiftH);
      }
   }
   screen.shiftY = shiftY;

#ifndef darwin // in darwin, the temporary buffer already is in the target format
   // screen bytes must be aligned to a 4-byte boundary, but screen.g bytes don't
   if (screen.bpp == 16)
   {
      Pixel565 grayp = SETPIXEL565(gray.r,gray.g,gray.b);
      if (currentContext->fullDirty && IS_PITCH_OPTIMAL(screenW, screen.pitch, screen.bpp)) // fairly common: the MainWindow is often fully repainted, and Palm OS and Windows always have pitch=width
      {
         PixelConv *f = (PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels);
         Pixel565 *t = (Pixel565*)screen.pixels;
         if (shiftY == 0)
            for (count = screenH * screenW; count != 0; f++,count--)
               #if defined(WIN32)
               SETPIXEL565_(t, f->pixel)
               #else
               *t++ = (Pixel565)SETPIXEL565(f->r, f->g, f->b);
               #endif
         else
         {
            for (count = shiftH * screenW, f += shiftY * screenW; count != 0; f++,count--)
               #if defined(WIN32)
               SETPIXEL565_(t, f->pixel)
               #else
               *t++ = (Pixel565)SETPIXEL565(f->r, f->g, f->b);
               #endif
            if (screenH > shiftH)
               for (count = (screenH-shiftH)*screenW; count != 0; f++,count--)
                  *t++ = grayp;
         }
      }
      else
      {
         PixelConv *f = ((PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels)) + (currentContext->dirtyY1+shiftY) * screenW + currentContext->dirtyX1, *rowf, *pf;
         Pixel565 *t = ((Pixel565*)BITMAP_PTR(screen.pixels, currentContext->dirtyY1, screen.pitch)) + currentContext->dirtyX1, *rowt, *pt;
         for (pf=rowf=f, pt=rowt=t, y = currentContext->dirtyY1; y < currentContext->dirtyY2; y++, pt = (rowt = (Pixel565*)(((uint8*)rowt) + screen.pitch)), pf = (rowf += screenW))
            if (shiftY != 0 && y >= shiftH)
            {
               if (currentContext->fullDirty) // draw gray area only if first time (full dirty, set above *1*)
                  for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; count--)
                     *pt++ = grayp;
            }
            else
            {
               for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; pf++, count--)
                  #if defined(WIN32)
                  SETPIXEL565_(pt, pf->pixel)
                  #else
                  *pt++ = (Pixel565)SETPIXEL565(pf->r, pf->g, pf->b);
                  #endif
            }
      }
   }
   else
   if (screen.bpp == 8)
   {
      uint32 r,g,b;
      uint8* toR = lookupR;
      uint8* toG = lookupG;
      uint8* toB = lookupB;
      uint8* toGray = lookupGray;
      PixelPal grayp = toGray[gray.r];
      if (currentContext->fullDirty && IS_PITCH_OPTIMAL(screenW, screen.pitch, screen.bpp)) // fairly common: the MainWindow is often fully repainted, and Palm OS and Windows always have pitch=width
      {
         PixelConv *f = (PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels);
         PixelPal *t = (PixelPal*)screen.pixels;
         if (shiftY == 0)
            for (count = screenH * screenW; count != 0; f++, count--)
            {
               r = f->r; g = f->g; b = f->b;
               *t++ = (PixelPal)((g == r && g == b) ? toGray[r] : (toR[r] + toG[g] + toB[b]));
            }
         else
         {
            PixelPal grayp = toGray[gray.r];
            for (count = shiftH * screenW, f += shiftY * screenW; count != 0; f++, count--)
            {
               r = f->r; g = f->g; b = f->b;
               *t++ = (PixelPal)((g == r && g == b) ? toGray[r] : (toR[r] + toG[g] + toB[b]));
            }
            if (screenH > shiftH)
               for (count = (screenH-shiftH)*screenW; count != 0; f++, count--)
                  *t++ = grayp;
         }
      }
      else
      {
         PixelConv *f = ((PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels)) + (currentContext->dirtyY1+shiftY) * screenW + currentContext->dirtyX1, *rowf, *pf;
         PixelPal *t = ((PixelPal*)BITMAP_PTR(screen.pixels, currentContext->dirtyY1, screen.pitch)) + currentContext->dirtyX1, *rowt, *pt;
         for (pf=rowf=f, pt=rowt=t, y = currentContext->dirtyY1; y < currentContext->dirtyY2; y++, pt = (rowt = (PixelPal*)(((uint8*)rowt) + screen.pitch)), pf = (rowf += screenW))
            if (shiftY != 0 && y >= shiftH)
            {
               if (currentContext->fullDirty) // draw gray area only if first time (full dirty, set above *1*)
                  for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; count--)
                     *pt++ = grayp;
            }
            else
            {
               for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; pf++, count--)
               {
                  r = pf->r; g = pf->g; b = pf->b;
                  *pt++ = (PixelPal)((g == r && g == b) ? toGray[r] : (toR[r] + toG[g] + toB[b]));
               }
            }
      }
   }
   else
   if (screen.bpp == 32)
   {
#ifdef WIN32
      #define SETPIXEL_32(p) (((p)->pixel) >> 8)
#else
      #define SETPIXEL_32(p) SETPIXEL32((p)->r, (p)->g, (p)->b)
#endif

      Pixel32 grayp = SETPIXEL_32(&gray);
      if (currentContext->fullDirty && IS_PITCH_OPTIMAL(screenW, screen.pitch, screen.bpp)) // fairly common: the MainWindow is often fully repainted, and Palm OS and Windows always have pitch=width
      {
         PixelConv *f = (PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels);
         Pixel32 *t = (Pixel32*)screen.pixels;
         if (shiftY == 0)
            for (count = screenH * screenW; count != 0; f++, count--)
               *t++ = SETPIXEL_32(f);
         else
         {
            for (count = shiftH * screenW, f += shiftY * screenW; count != 0; f++,count--)
               *t++ = SETPIXEL_32(f);
            if (screenH > shiftH)
               for (count = (screenH-shiftH)*screenW; count != 0; f++, count--)
                  *t++ = grayp;
         }
      }
      else
      {
         PixelConv *f = ((PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels)) + (currentContext->dirtyY1+shiftY) * screenW + currentContext->dirtyX1, *rowf, *pf=f;
         Pixel32 *t = ((Pixel32*)BITMAP_PTR(screen.pixels, currentContext->dirtyY1, screen.pitch)) + currentContext->dirtyX1, *rowt, *pt=t;
         for (pf=rowf=f, pt=rowt=t, y = currentContext->dirtyY1; y < currentContext->dirtyY2; y++, pt = (rowt = (Pixel32*)(((uint8*)rowt) + screen.pitch)), pf = (rowf += screenW))
            if (shiftY != 0 && y >= shiftH)
            {
               if (currentContext->fullDirty) // draw gray area only if first time (full dirty, set above *1*)
                  for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; count--)
                     *pt++ = grayp;
            }
            else
            {
               for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; pf++, count--)
                  *pt++ = SETPIXEL_32(pf);
            }
      }
   }
   else
   if (screen.bpp == 24)
   {
      if (currentContext->fullDirty && IS_PITCH_OPTIMAL(screenW, screen.pitch, screen.bpp)) // fairly common: the MainWindow is often fully repainted, and Palm OS and Windows always have pitch=width
      {
         PixelConv *f = (PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels);
         Pixel24 *t = (Pixel24*)screen.pixels;
         if (shiftY == 0)
            for (count = screenH * screenW; count != 0; f++, t++, count--)
               SETPIXEL24(t,f)
         else
         {
            Pixel24 grayp;
            SETPIXEL24((&grayp),(&gray));
            for (count = shiftH * screenW, f += shiftY * screenW; count != 0; f++, t++, count--)
               SETPIXEL24(t,f)
            if (screenH > shiftH)
               for (count = (screenH-shiftH)*screenW; count != 0; count--)
                  *t++ = grayp;
         }
      }
      else
      {
         PixelConv *f = ((PixelConv*)ARRAYOBJ_START(screen.mainWindowPixels)) + (currentContext->dirtyY1+shiftY) * screenW + currentContext->dirtyX1, *rowf, *pf=f;
         Pixel24 *t = ((Pixel24*)BITMAP_PTR(screen.pixels, currentContext->dirtyY1, screen.pitch)) + currentContext->dirtyX1, *rowt, *pt=t;
         for (pf=rowf=f, pt=rowt=t, y = currentContext->dirtyY1; y < currentContext->dirtyY2; y++, pt = (rowt = (Pixel24*)(((uint8*)rowt) + screen.pitch)), pf = (rowf += screenW))
            for (count = currentContext->dirtyX2 - currentContext->dirtyX1; count != 0; pf++,pt++, count--)
               SETPIXEL24(pt, pf);
      }

   }
#endif
   graphicsLock(&screen, false);
   return true;
}

static bool createColorPaletteLookupTables()
{
   uint32 i, r,g,b;
   lookupR = (uint8*)xmalloc(256);
   lookupG = (uint8*)xmalloc(256);
   lookupB = (uint8*)xmalloc(256);
   lookupGray = (uint8*)xmalloc(256);
   if (!lookupR || !lookupG || !lookupB || !lookupGray)
   {
      xfree(lookupR);
      xfree(lookupG);
      xfree(lookupB);
      xfree(lookupGray);
      return false;
   }

   for (i = 0; i < 256; i++)
   {
      r = (i+1) * 6 / 256; if (r > 0) r--;
      g = (i+1) * 8 / 256; if (g > 0) g--;
      b = (i+1) * 5 / 256; if (b > 0) b--;
      lookupR[i] = (uint8)(r*40);
      lookupG[i] = (uint8)(g*5);
      lookupB[i] = (uint8)(b+16);
      lookupGray[i] = (uint8)(i / 0x11);
   }
   return true;
}

static void fillWith8bppPalette(uint32* ptr)
{
   uint32 R = 6, G = 8, B = 5,r,g,b,rr,gg,bb,k,kk;
   // gray values  0-15
   for (k =0; k <= 15; k++)
   {
      kk = k * 0x11;
      *ptr++ = SETPIXEL32(kk,kk,kk);
   }
   // color values  16 - 255
   for (r = 1; r <= R; r++)
   {
      rr = r * 256/R; if (rr > 255) rr = 255;
      for (g = 1; g <= G; g++)
      {
         gg = g * 256/G; if (gg > 255) gg = 255;
         for (b = 1; b <= B; b++)
         {
            bb = b * 256/B; if (bb > 255) bb = 255;
            *ptr++ = SETPIXEL32(rr, gg, bb);
         }
      }
   }
}

static void fillHatchedRect(Context currentContext, Object g, int32 x, int32 y, int32 w, int32 h, bool top, bool bottom, Pixel c)
{
   int32 x2 = x+w-1;
   int32 y2 = y+h-1;
   if (top && bottom)
   {
      fillRect(currentContext, g,x, y+2, w, h-4, c);  // middle
      drawLine(currentContext, g,x+2,y, x2-2, y, c);           // 1st line
      drawLine(currentContext, g,x+1,y+1, x2-1, y+1, c);       // 2nd line
      drawLine(currentContext, g,x+1, y2-1, x2-1, y2-1, c);    // last-1 line
      drawLine(currentContext, g,x+2, y2, x2-2, y2, c);        // last line
   }
   else
   if (top && !bottom)
   {
      drawLine(currentContext, g,x+2, y, x2-2, y, c);           // 1st line
      drawLine(currentContext, g,x+1, y+1, x2-1, y+1, c);       // 2nd line
      fillRect(currentContext, g,x, y+2, w, h-2, c);   // middle
   }
   else
   if (!top && bottom)
   {
      fillRect(currentContext, g,x, y, w, h-2, c);     // middle
      drawLine(currentContext, g,x+1,y2-1, x2-1, y2-1, c);      // last-1 line
      drawLine(currentContext, g,x+2, y2, x2-2, y2, c);         // last line
   }
}

static void drawHatchedRect(Context currentContext, Object g, int32 x, int32 y, int32 w, int32 h, bool top, bool bottom, Pixel c)
{
   int32 x2 = x+w-1;
   int32 y2 = y+h-1;
   if (top && bottom)
   {
      drawLine(currentContext, g,x, y+2, x, y2-2, c); // left
      drawLine(currentContext, g,x+2, y2, x2-2, y2, c); // bottom
      drawLine(currentContext, g,x2, y+2, x2, y2-2, c); // right
      drawLine(currentContext, g,x+2, y, x2-2, y, c); // top
      setPixel(currentContext, g,x+1, y+1, c); // top left
      setPixel(currentContext, g,x2-1, y+1, c); // top right
      setPixel(currentContext, g,x+1, y2-1, c); // bottom left
      setPixel(currentContext, g,x2-1, y2-1, c); // bottom right
   }
   else
   if (top && !bottom)
   {
      drawLine(currentContext, g,x, y+2, x, y2, c); // left
      drawLine(currentContext, g,x, y2, x2, y2, c); // bottom
      drawLine(currentContext, g,x2, y+2, x2, y2, c); // right
      drawLine(currentContext, g,x+2, y, x2-2, y, c); // top
      setPixel(currentContext, g,x+1, y+1, c); // top left
      setPixel(currentContext, g,x2-1, y+1, c); // top right
   }
   else
   if (!top && bottom)
   {
      drawLine(currentContext, g,x, y, x, y2-2, c); // left
      drawLine(currentContext, g,x+2, y2, x2-2, y2, c); // bottom
      drawLine(currentContext, g,x2, y, x2, y2-2, c); // right
      drawLine(currentContext, g,x, y, x2, y, c); // top
      setPixel(currentContext, g,x+1, y2-1, c); // bottom left
      setPixel(currentContext, g,x2-1, y2-1, c); // bottom right
   }
}

static void drawVistaRect(Context currentContext, Object g, int32 x, int32 y, int32 width, int32 height, Pixel topColor, Pixel rightColor, Pixel bottomColor, Pixel leftColor)
{
   int32 x1 = x+1;
   int32 y1 = y+1;
   int32 x2 = x+width-1;
   int32 y2 = y+height-1;
   drawLine(currentContext, g,x1,y,x2-1,y, topColor);
   drawLine(currentContext, g,x2,y1,x2,y2-1, rightColor);
   drawLine(currentContext, g,x1,y2,x2-1,y2, bottomColor);
   drawLine(currentContext, g,x,y1,x,y2-1, leftColor);
}

static void fillVistaRect(Context currentContext, Object g, int32 x, int32 y, int32 width, int32 height, bool invert, bool rotate, Object colors)
{
   int32 dim,y0,hh,incY,lineH,lineY=0,yy,k,c=0;
   int32 *vistaColors = (int32*)ARRAYOBJ_START(colors);
   dim = rotate ? width : height;
   y0 = rotate ? x : y;
   hh = rotate ? x+dim : y+dim;
   dim <<= 16;
   incY = dim/10;
   lineH = (incY>>16)+1;
   for (; lineY < dim; c++, lineY += incY)
   {
      Pixel backColor = makePixelRGB(vistaColors[invert ? 10-c : c]);
      yy = y0+(lineY>>16);
      k = hh-yy;
      if (!rotate)
         fillRect(currentContext, g, x,yy,width,k < lineH ? k : lineH, backColor);
      else
         fillRect(currentContext, g, yy,y,k < lineH ? k : lineH, height, backColor);
   }
}

inline static int getOffset(int radius, int y)
{
   return radius - (int32)sqrt(radius * radius - y * y);
}

static void drawFadedPixel(Context currentContext, Object g, int32 xx, int32 yy, int32 c) // guich@tc124_4
{
#ifdef __gl2_h_
   if (Graphics_useOpenGL(g))
      glDrawPixel(xx+Graphics_transX(g),yy+Graphics_transY(g),c,20*255/100);
   else
#endif
   {      
   PixelConv c1,c2;
   c1.pixel = c;
   c2 = getPixelConv(g, xx, yy);
   setPixel(currentContext, g, xx, yy, interpolate(c1, c2, 20*255/100));
   }
}

static void drawRoundGradient(Context currentContext, Object g, int32 startX, int32 startY, int32 endX, int32 endY, int32 topLeftRadius, int32 topRightRadius, int32 bottomLeftRadius, int32 bottomRightRadius, int32 startColor, int32 endColor, bool vertical)
{
   int32 numSteps = max32(1, vertical ? abs32(endY - startY) : abs32(endX - startX)); // guich@tc110_11: support horizontal gradient - guich@gc114_41: prevent div by 0 if numsteps is 0
   int32 startRed = (startColor >> 16) & 0xFF;
   int32 startGreen = (startColor >> 8) & 0xFF;
   int32 startBlue = startColor & 0xFF;
   int32 endRed = (endColor >> 16) & 0xFF;
   int32 endGreen = (endColor >> 8) & 0xFF;
   int32 endBlue = endColor & 0xFF;
   int32 redInc = ((endRed - startRed) << 16) / numSteps;
   int32 greenInc = ((endGreen - startGreen) << 16) / numSteps;
   int32 blueInc = ((endBlue - startBlue) << 16) / numSteps;
   int32 red = startRed << 16;
   int32 green = startGreen << 16;
   int32 blue = startBlue << 16;
   int32 i;
   int32 leftOffset=0;
   int32 rightOffset=0;
   bool hasRadius = (topLeftRadius+topRightRadius+bottomLeftRadius+bottomRightRadius) > 0;
   Pixel p;
   if (startX > endX)
   {
      int32 temp = startX;
      startX = endX;
      endX = temp;
   }
   if (startY > endY)
   {
      int32 temp = startY;
      startY = endY;
      endY = temp;
   }

   for (i = 0; i < numSteps; i++)
   {
      if (hasRadius)
      {
         leftOffset = rightOffset = 0;

         if (topLeftRadius > 0 && i < topLeftRadius)
            leftOffset = getOffset(topLeftRadius,topLeftRadius - i - 1) - 1;
         else
         if (bottomLeftRadius > 0 && i > numSteps - bottomLeftRadius)
            leftOffset = getOffset(bottomLeftRadius,bottomLeftRadius - (numSteps - i + 1)) - 1;

         if (topRightRadius > 0 && i < topRightRadius)
            rightOffset = getOffset(topRightRadius,topRightRadius - i - 1) - 1;
         else
         if (bottomRightRadius > 0 && i > numSteps - bottomRightRadius)
            rightOffset = getOffset(bottomRightRadius,bottomRightRadius - (numSteps - i + 1)) - 1;

         if (leftOffset < 0) leftOffset = 0;
         if (rightOffset < 0) rightOffset = 0;
      }
      p = makePixel(red >> 16,green >> 16, blue >> 16);
      if (vertical)
      {
         int32 fc = p;
         drawLine(currentContext, g, startX + leftOffset, startY+i, endX - rightOffset, startY+i, p);
         drawFadedPixel(currentContext, g, endX - rightOffset+1, startY+i, fc);
         drawFadedPixel(currentContext, g, startX+leftOffset-1, startY+i, fc);
      }
      else
         drawLine(currentContext, g, startX+i, startY + leftOffset, startX+i, endY - rightOffset, p);

      red += redInc;
      green += greenInc;
      blue += blueInc;
   }
}

static int getsetRGB(Context currentContext, Object g, Object dataObj, int32 offset, int32 x, int32 y, int32 w, int32 h, bool isGet)
{
   if (dataObj == null)
      throwException(currentContext, NullPointerException, "Argument 'data' can't be null");
   else
   if (!checkArrayRange(currentContext, dataObj, offset, w*h))
      ;
   else
   if (translateAndClip(g, &x, &y, &w, &h))
   {
      Pixel* data = ((Pixel*)ARRAYOBJ_START(dataObj)) + offset;
      int32 inc = Graphics_pitch(g), count = w * h;
      Pixel* pixels = getGraphicsPixels(g) + y * inc + x;
      bool markDirty = !currentContext->fullDirty && !Surface_isImage(Graphics_surface(g));
#ifdef __gl2_h_
      currentContext->fullDirty |= markDirty;
#endif

      if (isGet)
         for (; h-- > 0; pixels += inc, data += w)
            xmemmove(data, pixels, w<<2);
      else
         for (; h-- > 0; pixels += inc, data += w)
         {
            xmemmove(pixels, data, w<<2);
#ifndef __gl2_h_
            if (markDirty)
               markScreenDirty(currentContext, x, y++, w, 1);
#endif
         }
      return count;
   }
   return 0;
}

#define IN_BORDER 0
#define OUT_BORDER 0x100

static int32 windowBorderAlpha[3][7][7] =
{
   {  // thickness 1
      { 190, 190,  152,   89,  OUT_BORDER, OUT_BORDER, OUT_BORDER },
      { 255, 255,  255,  255,  220, OUT_BORDER, OUT_BORDER },
      {  IN_BORDER,  IN_BORDER,  -32, -110,  255, 174, OUT_BORDER },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,  -11,  255, 245,  62 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,   IN_BORDER, -110, 255,  81 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,   IN_BORDER, -26,  255, 152 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,   IN_BORDER,  IN_BORDER,  255, 190 },
   },
   {  // thickness 2
      { 255, 229,  163,   95,  OUT_BORDER, OUT_BORDER, OUT_BORDER },
      { 255, 255,  255,  255,  215, OUT_BORDER, OUT_BORDER },
      {  IN_BORDER, -36, -102, -197,  255, 215, OUT_BORDER },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,  -36, -191, 255, 122 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,   IN_BORDER,  -77, 255, 179 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,   IN_BORDER,  -32, 255, 229 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,   IN_BORDER,   IN_BORDER, 255, 255 },
   },
   {  // thickness 3
      { 255, 199,  128,   10,  OUT_BORDER, OUT_BORDER, OUT_BORDER },
      { 255, 255,  255,  223,   59, OUT_BORDER, OUT_BORDER },
      { 255, 255,  255,  255,  255,  81, OUT_BORDER },
      {  IN_BORDER, -79, -234,  255,  255, 245,  16 },
      {  IN_BORDER,  IN_BORDER,  -32, -215,  255, 255, 133 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,  -77,  255, 255, 207 },
      {  IN_BORDER,  IN_BORDER,   IN_BORDER,  -15,  255, 255, 245 },
   }
};

static void setPixelA(Context currentContext, Object g, int32 x, int32 y, PixelConv color, int32 alpha)
{
#ifdef __gl2_h_
   if (Graphics_useOpenGL(g))
      glDrawPixel(x+Graphics_transX(g),y+Graphics_transY(g),color.pixel,alpha);
   else
#endif
      setPixel(currentContext, g, x,y,interpolate(color, getPixelConv(g, x,y), alpha));
}

static void drawLineA(Context currentContext, Object g, int32 x1, int32 y1, int32 x2, int32 y2, PixelConv color, int32 alpha)
{
#ifdef __gl2_h_
   if (Graphics_useOpenGL(g))
      glDrawLine(x1+Graphics_transX(g),y1+Graphics_transY(g),x2+Graphics_transX(g),y2+Graphics_transY(g),color.pixel,alpha);
   else
#endif
      drawLine(currentContext, g, x1,y1,x2,y2,interpolate(color, getPixelConv(g, x1, y1), alpha)); // bottom
}

static void drawWindowBorder(Context currentContext, Object g, int32 xx, int32 yy, int32 ww, int32 hh, int32 titleH, int32 footerH, PixelConv borderColor, PixelConv titleColor, PixelConv bodyColor, PixelConv footerColor, int32 thickness, bool drawSeparators)
{
   int32 kx, ky, a, i, j, t0, ty, bodyH, rectX1, rectX2, rectW;
   int32 y2 = yy+hh-1;
   int32 x2 = xx+ww-1;
   int32 x1l = xx+7;
   int32 y1l = yy+7;
   int32 x2r = x2-6;
   int32 y2r = y2-6;
   PixelConv c;

   // horizontal and vertical lines
   for (i = 0; i < 3; i++)
   {
      a = windowBorderAlpha[thickness-1][i][0];
      if (a == OUT_BORDER || a == IN_BORDER)
         continue;
      kx = x1l;
      ky = yy+i;
      drawLineA(currentContext, g, kx,ky,x2r,yy+i,borderColor, a); // top

      ky = y2-i;
      drawLineA(currentContext, g, kx,ky,x2r,y2-i,borderColor, a); // bottom

      kx = xx+i;
      ky = y1l;
      drawLineA(currentContext, g, kx,ky,xx+i,y2r,borderColor, a); // left

      kx = x2-i;
      drawLineA(currentContext, g, kx,ky,x2-i,y2r,borderColor, a); // right
   }
   // round corners
   for (j = 0; j < 7; j++)
   {
      int32 top = yy+j, bot = y2r+j;
      for (i = 0; i < 7; i++)
      {
         int left = xx+i, right = x2r+i;
         // top left
         a = windowBorderAlpha[thickness-1][j][6-i];
         if (a != OUT_BORDER)
         {
            if (a == 0)
               setPixel(currentContext, g, left,top,titleColor.pixel);
            else
            if (a < 0)
               setPixel(currentContext, g, left,top,interpolate(borderColor, titleColor, -a));
            else
               setPixelA(currentContext, g, left,top,borderColor, a);
         }

         // top right
         a = windowBorderAlpha[thickness-1][j][i];
         if (a != OUT_BORDER)
         {
            if (a == 0)
               setPixel(currentContext, g, right,top,titleColor.pixel);
            else
            if (a < 0)
               setPixel(currentContext, g, right,top,interpolate(borderColor, titleColor, -a));
            else
               setPixelA(currentContext, g, right,top,borderColor, a);
         }
         // bottom left
         a = windowBorderAlpha[thickness-1][i][j];
         if (a != OUT_BORDER)
         {
            if (a == 0)
               setPixel(currentContext, g, left,bot,footerColor.pixel);
            else
            if (a < 0)
               setPixel(currentContext, g, left,bot,interpolate(borderColor, footerColor, -a));
            else
               setPixelA(currentContext, g, left,bot,borderColor, a);
         }
         // bottom right
         a = windowBorderAlpha[thickness-1][6-i][j];
         if (a != OUT_BORDER)
         {
            if (a == 0)
               setPixel(currentContext, g, right,bot,footerColor.pixel);
            else
            if (a < 0)
               setPixel(currentContext, g, right,bot,interpolate(borderColor, footerColor, -a));
            else
               setPixelA(currentContext, g, right,bot,borderColor, a);
         }
      }
   }
   // now fill text, body and footer
   t0 = thickness <= 2 ? 2 : 3;
   ty = t0 + yy;
   rectX1 = xx+t0;
   rectX2 = x2-t0;
   rectW = ww-t0*2;
   bodyH = hh - (titleH == 0 ? 7 : titleH) - (footerH == 0 ? 7 : footerH);
   // remove corners from title and footer heights
   titleH -= 7;  if (titleH < 0) titleH = 0;
   footerH -= 7; if (footerH < 0) footerH = 0;

   // text
   fillRect(currentContext, g, x1l,ty,x2r-x1l,7-t0, titleColor.pixel);    ty += 7-t0;   // corners
   fillRect(currentContext, g, rectX1,ty,rectW,titleH, titleColor.pixel); ty += titleH; // non-corners
   // separator
   if (drawSeparators && titleH > 0 && titleColor.pixel == bodyColor.pixel)
      drawLine(currentContext, g, rectX1,ty-1,rectX2,ty-1,interpolate(borderColor,titleColor,64));
   // body
   fillRect(currentContext, g, rectX1,ty,rectW,bodyH, bodyColor.pixel); ty += bodyH;
   // separator
   if (drawSeparators && footerH > 0 && bodyColor.pixel == footerColor.pixel)
      {drawLine(currentContext, g, rectX1,ty,rectX2,ty,interpolate(borderColor,titleColor,64)); ty++; footerH--;}
   // footer
   fillRect(currentContext, g, rectX1,ty,rectW,footerH,footerColor.pixel); ty += footerH; // non-corners
   fillRect(currentContext, g, x1l,ty,x2r-x1l,7-t0,footerColor.pixel);                    // corners
}

static inline void addError(PixelConv* pixel, int32 x, int32 y, int32 w, int32 h, int32 errR, int32 errG, int32 errB, int32 j, int32 k)
{
   int32 r,g,b;
   if (x >= w || y >= h || x < 0) return;
   r = pixel->r + j*errR/k;
   g = pixel->g + j*errG/k;
   b = pixel->b + j*errB/k;
   if (r > 255) r = 255; else if (r < 0) r = 0;
   if (g > 255) g = 255; else if (g < 0) g = 0;
   if (b > 255) b = 255; else if (b < 0) b = 0;
   pixel->r = r;
   pixel->g = g;
   pixel->b = b;
}

static void dither(Context currentContext, Object g, int32 x0, int32 y0, int32 w, int32 h)
{
   if (translateAndClip(g, &x0, &y0, &w, &h))
   {
      PixelConv *pixels;
      int32 oldR,oldG,oldB, newR,newG,newB, errR, errG, errB, pitch, x,y,yf=y0+h,xf=x0+w;
      pitch = Graphics_pitch(g);

      // based on http://en.wikipedia.org/wiki/Floyd-Steinberg_dithering
      for (y=y0; y < yf; y++)
      {
         pixels = (PixelConv*)(getGraphicsPixels(g) + y * pitch + x0);
         for (x=x0; x < xf; x++,pixels++)
         {
            // get current pixel values
            oldR = pixels->r;
            oldG = pixels->g;
            oldB = pixels->b;
            // convert to 565 component values
            newR = oldR >> 3 << 3;
            newG = oldG >> 2 << 2;
            newB = oldB >> 3 << 3;
            // compute error
            errR = oldR-newR;
            errG = oldG-newG;
            errB = oldB-newB;
            // set new pixel
            pixels->r = newR;
            pixels->g = newG;
            pixels->b = newB;

            addError(pixels+1      , x+1, y ,w,h, errR,errG,errB,7,16);
            addError(pixels-1+pitch, x-1,y+1,w,h, errR,errG,errB,3,16);
            addError(pixels  +pitch, x,y+1  ,w,h, errR,errG,errB,5,16);
            addError(pixels+1+pitch, x+1,y+1,w,h, errR,errG,errB,1,16);
         }
      }
#ifndef __gl2_h_
      if (!currentContext->fullDirty && !Surface_isImage(Graphics_surface(g))) markScreenDirty(currentContext, x0, y0, w, h);
#else
      currentContext->fullDirty |= !Surface_isImage(Graphics_surface(g));
#endif
   }
}

static void drawCylindricShade(Context currentContext, Object g, int32 startColor, int32 endColor, int32 startX, int32 startY, int32 endX, int32 endY)
{
   int32 numSteps = max32(1,min32((endY - startY)/2, (endX - startX)/2)); // guich@tc110_11: support horizontal gradient - guich@gc114_41: prevent div by 0 if numsteps is 0
   int32 startRed = (startColor >> 16) & 0xFF;
   int32 startGreen = (startColor >> 8) & 0xFF;
   int32 startBlue = startColor & 0xFF;
   int32 endRed = (endColor >> 16) & 0xFF;
   int32 endGreen = (endColor >> 8) & 0xFF;
   int32 endBlue = endColor & 0xFF;
   int32 redInc = (((endRed - startRed)*2) << 16) / numSteps;
   int32 greenInc = (((endGreen - startGreen)*2) << 16) / numSteps;
   int32 blueInc = (((endBlue - startBlue)*2) << 16) / numSteps;
   int32 red = startRed << 16;
   int32 green = startGreen << 16;
   int32 blue = startBlue << 16;
   int32 foreColor,rr,gg,bb,sx,sy,ii,i2,i;
   for (i = 0; i < numSteps; i++)
   {
      rr = ((red+i*redInc) >> 16) & 0xFFFFFF;     if (rr > endRed) rr = endRed;
      gg = ((green+i*greenInc) >> 16) & 0xFFFFFF; if (gg > endGreen) gg = endGreen;
      bb = ((blue+i*blueInc) >> 16) & 0xFFFFFF;   if (bb > endBlue) bb = endBlue;
      foreColor = (rr << 16) | (gg << 8) | bb;
      sx = startX+i;
      sy = startY+i;
      drawRect(currentContext, g,sx,sy,endX-i-sx,endY-i-sy,foreColor);
      ii = i-8;
      rr = ((red+ii*redInc) >> 16) & 0xFFFFFF;     if (rr > endRed) rr = endRed;
      gg = ((green+ii*greenInc) >> 16) & 0xFFFFFF; if (gg > endGreen) gg = endGreen;
      bb = ((blue+ii*blueInc) >> 16) & 0xFFFFFF;   if (bb > endBlue) bb = endBlue;
      foreColor = (rr << 16) | (gg << 8) | bb;
      i2 = i/8;
      drawLine(currentContext, g,sx-i2,sy+i2,sx+i2,sy-i2,foreColor);
      sx = endX-i; drawLine(currentContext, g,sx-i2,sy-i2,sx+i2,sy+i2,foreColor);
      sy = endY-i; drawLine(currentContext, g,sx-i2,sy+i2,sx+i2,sy-i2,foreColor);
      sx = startX+i; drawLine(currentContext, g,sx-i2,sy-i2,sx+i2,sy+i2,foreColor);
   }
   if (screen.bpp < 24) dither(currentContext, g, startX, startY, endX-startX, endY-startY);
}

/////////////// Start of Device-dependant functions ///////////////
static bool startupGraphics(int16 appTczAttr) // there are no threads running at this point
{
   return graphicsStartup(&screen, appTczAttr);
}

#ifdef darwin
static Object constPixels;
char* createPixelsBuffer(int width, int height) // called from childview.m
{
   if (constPixels != null)
      return constPixels;
   constPixels = createArrayObject(mainContext, INT_ARRAY, width*height);
   return ARRAYOBJ_START(constPixels);
}
#endif

static bool createScreenSurface(Context currentContext, bool isScreenChange)
{
   bool ret = false;
   if (screen.screenW <= 0 || screen.screenH <= 0)
      return false;

   if (graphicsCreateScreenSurface(&screen))
   {
      Object *screenObj;
      screenObj = getStaticFieldObject(loadClass(currentContext, "totalcross.ui.gfx.Graphics",false), "mainWindowPixels");
#ifdef darwin // in darwin, the pixels buffer is pre-initialized and never changed
      if (screen.mainWindowPixels == null)
      {
         controlEnableUpdateScreenPtr = getStaticFieldInt(loadClass(currentContext, "totalcross.ui.Control",false), "enableUpdateScreen");
         containerNextTransitionEffectPtr = getStaticFieldInt(loadClass(currentContext, "totalcross.ui.Container",false), "nextTransitionEffect");
      }
      *screenObj = screen.mainWindowPixels = constPixels;
      ret = true;
#else

      if (isScreenChange)
      {
         screen.mainWindowPixels = *screenObj = null;
         gc(currentContext); // let the gc collect the old screen object
      }
      else
      {
         controlEnableUpdateScreenPtr = getStaticFieldInt(loadClass(currentContext, "totalcross.ui.Control",false), "enableUpdateScreen");
         containerNextTransitionEffectPtr = getStaticFieldInt(loadClass(currentContext, "totalcross.ui.Container",false), "nextTransitionEffect");
      }

      *screenObj = screen.mainWindowPixels = createArrayObject(currentContext, INT_ARRAY, screen.screenW * screen.screenH);
      setObjectLock(*screenObj, UNLOCKED);
      ret = screen.mainWindowPixels != null && controlEnableUpdateScreenPtr != null;
#endif
   }
   return ret;
}

void markWholeScreenDirty(Context currentContext)
{
   LOCKVAR(screen);
   currentContext->dirtyX1 = currentContext->dirtyY1 = 0;
   currentContext->dirtyX2 = screen.screenW;
   currentContext->dirtyY2 = screen.screenH;
   currentContext->fullDirty = true;
   UNLOCKVAR(screen);
}

static bool checkScreenPixels()
{
   return screen.pixels != null;
}

void updateScreen(Context currentContext)
{
#ifdef darwin
   if (callingScreenChange) return;
#elif defined ANDROID
   if (appPaused) return;
#endif
   LOCKVAR(screen);
   if (keepRunning && checkScreenPixels() && controlEnableUpdateScreenPtr && *controlEnableUpdateScreenPtr && (currentContext->fullDirty || (currentContext->dirtyX1 != screen.screenW && currentContext->dirtyX2 != 0 && currentContext->dirtyY1 != screen.screenH && currentContext->dirtyY2 != 0)))
   {
      int32 transitionEffect = *containerNextTransitionEffectPtr;
#ifndef __gl2_h_
      if (updateScreenBits(currentContext)) // move the temporary buffer to the real screen
#endif
      {
         if (transitionEffect == -1)
            transitionEffect = TRANSITION_NONE;
#ifdef darwin
         UNLOCKVAR(screen); // without this, a deadlock can occur in iOS if the user minimizes the application, since another thread can trigger a markScreenDirty
#endif
         graphicsUpdateScreen(currentContext, &screen, transitionEffect);
#ifdef darwin
         LOCKVAR(screen);
#endif
      }
      *containerNextTransitionEffectPtr = TRANSITION_NONE;
      currentContext->dirtyX1 = screen.screenW;
      currentContext->dirtyY1 = screen.screenH;
      currentContext->dirtyX2 = currentContext->dirtyY2 = 0;
      currentContext->fullDirty = false;
   }
   UNLOCKVAR(screen);
}

void graphicsDestroyPrimitives()
{
   xfree(lookupR);
   xfree(lookupG);
   xfree(lookupB);
   xfree(lookupGray);
   fontDestroy();
}
/////////////// End of Device-dependant functions ///////////////
