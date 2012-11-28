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

#include "gfx_ex.h"

#ifdef ANDROID
#include <android/native_window.h> // requires ndk r5 or newer
#include <android/native_window_jni.h> // requires ndk r5 or newer
#include <android/log.h>
#define debug(...) ((void)__android_log_print(ANDROID_LOG_INFO, "TotalCross", __VA_ARGS__))
#endif

void checkGlError(const char* op, int line)
{
   GLint error;
   int c=0;
   if (!op)
      glGetError();
   else
   for (error = glGetError(); error; error = glGetError())
   {
      char* msg = "???";
      switch (error)
      {
         case GL_INVALID_ENUM     : msg = "INVALID ENUM"; break;
         case GL_INVALID_VALUE    : msg = "INVALID VALUE"; break;
         case GL_INVALID_OPERATION: msg = "INVALID OPERATION"; break;
         case GL_OUT_OF_MEMORY    : msg = "OUT OF MEMORY"; break;
      }
      debug("glError %s at %s (%d)\n", msg, op, line);
      c++;
   }
}

#ifdef ANDROID
static void setProjectionMatrix(GLfloat w, GLfloat h);
static ANativeWindow *window,*lastWindow;
static EGLDisplay _display;
static EGLSurface _surface;
static EGLContext _context;
#endif
static void destroyEGL();
static bool surfaceWillChange;

VoidPs* imgTextures;
int32 realAppH,appW,appH,glShiftY;
GLfloat ftransp[16], f255[256];
int32 flen;
GLfloat* glcoords;//[flen*2]; x,y
GLfloat* glcolors;//[flen];   alpha
static GLfloat texcoords[16], lrcoords[8], shcolors[24],shcoords[8];
static int32 *pixcoords, *pixcolors, *pixEnd;

// http://www.songho.ca/opengl/gl_projectionmatrix.html
//////////// texture
#define TEXTURE_VERTEX_CODE  \
      "attribute vec4 vertexPoint;" \
      "attribute vec2 aTextureCoord;" \
      "uniform mat4 projectionMatrix; " \
      "varying vec2 vTextureCoord;" \
      "void main()" \
      "{" \
      "    gl_Position = vertexPoint * projectionMatrix;" \
      "    vTextureCoord = aTextureCoord;" \
      "}"

#define TEXTURE_FRAGMENT_CODE \
      "precision mediump float;" \
      "varying vec2 vTextureCoord;" \
      "uniform sampler2D sTexture;" \
      "void main() {gl_FragColor = texture2D(sTexture, vTextureCoord);}"

static GLuint textureProgram;
static GLuint texturePoint;
static GLuint textureCoord,textureS;

//////////// points (text)

#define POINTS_VERTEX_CODE \
      "attribute vec4 a_Position; uniform vec4 a_Color; varying vec4 v_Color; attribute float alpha;" \
      "uniform mat4 projectionMatrix; " \
      "void main() {gl_PointSize = 1.0; v_Color = vec4(a_Color.x,a_Color.y,a_Color.z,alpha); gl_Position = a_Position * projectionMatrix;}"

#define POINTS_FRAGMENT_CODE \
      "precision mediump float;" \
      "varying vec4 v_Color;" \
      "void main() {gl_FragColor = v_Color;}"

static GLuint pointsProgram;
static GLuint pointsPosition;
static GLuint pointsColor;
static GLuint pointsAlpha;

///////////// line, rect, point

#define LRP_VERTEX_CODE \
      "attribute vec4 a_Position;" \
      "uniform mat4 projectionMatrix;" \
      "void main() {gl_PointSize = 1.0; gl_Position = a_Position*projectionMatrix;}"

#define LRP_FRAGMENT_CODE \
      "precision mediump float;" \
      "uniform vec4 a_Color;" \
      "void main() {gl_FragColor = a_Color;}"

static GLuint lrpProgram;
static GLuint lrpPosition;
static GLuint lrpColor;
static GLubyte rectOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

///////////// shaded rect

#define SHADE_VERTEX_CODE \
      "attribute vec4 a_Position; attribute vec4 a_Color; varying vec4 v_Color;" \
      "uniform mat4 projectionMatrix;" \
      "void main() {gl_PointSize = 1.0; v_Color = a_Color; gl_Position = a_Position*projectionMatrix;}"

#define SHADE_FRAGMENT_CODE \
      "precision mediump float;" \
      "varying vec4 v_Color;" \
      "void main() {gl_FragColor = v_Color;}"

static GLuint shadeProgram;
static GLuint shadePosition;
static GLuint shadeColor;

GLuint loadShader(GLenum shaderType, const char* pSource)
{
   GLint ret=1;               
   GLuint shader = glCreateShader(shaderType); GL_CHECK_ERROR
   glShaderSource(shader, 1, &pSource, NULL); GL_CHECK_ERROR
   glCompileShader(shader); GL_CHECK_ERROR

   glGetShaderiv(shader, GL_COMPILE_STATUS, &ret); GL_CHECK_ERROR
   if(!ret)
   {
      glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &ret); GL_CHECK_ERROR
      GLchar buffer[ret];
      glGetShaderInfoLog(shader, ret, &ret, buffer); GL_CHECK_ERROR
      debug("Shader compiler error: %s",buffer);
   }
   return shader;
}

static GLint lastProg=-1;
static void setCurrentProgram(GLint prog)
{
   if (prog != lastProg)
   {
      glUseProgram(lastProg = prog); GL_CHECK_ERROR
   }
}

static GLuint createProgram(char* vertexCode, char* fragmentCode)
{
   GLint ret;
   GLuint p = glCreateProgram();
   glAttachShader(p, loadShader(GL_VERTEX_SHADER, vertexCode)); GL_CHECK_ERROR
   glAttachShader(p, loadShader(GL_FRAGMENT_SHADER, fragmentCode)); GL_CHECK_ERROR
   glLinkProgram(p); GL_CHECK_ERROR
   //glValidateProgram(p);
   glGetProgramiv(p, GL_LINK_STATUS, &ret); GL_CHECK_ERROR
   if (!ret)
   {
      glGetProgramiv(p, GL_INFO_LOG_LENGTH, &ret); GL_CHECK_ERROR
      GLchar buffer[ret];
      glGetProgramInfoLog(p, ret, &ret, buffer); GL_CHECK_ERROR
      debug("Link error: %s",buffer);
   }
   return p;
}

bool initGLES(ScreenSurface screen); // in iOS, implemented in mainview.m
void recreateTextures(VoidPs* imgTextures); // imagePrimitives_c.h

void setTimerInterval(int32 t);  
int32 desiredglShiftY;
bool setShiftYonNextUpdateScreen;
#ifdef ANDROID           
void JNICALL Java_totalcross_Launcher4A_nativeInitSize(JNIEnv *env, jobject this, jobject surface, jint width, jint height) // called only once
{                    
   if (!screen.extension)           
      screen.extension = newX(ScreenSurfaceEx);

   if (surface == null) // passed null when the surface is destroyed
   {       
      if (width == -999)
      {                
         if (needsPaint != null)
         {
            //debug("changing sip. appH: %d, heigth: %d, glShiftY: %d",appH,height,glShiftY);
            desiredglShiftY = height == 0 ? 0 : appH - height; // change only after the next screen update, since here we are running in a different thread
            setShiftYonNextUpdateScreen = true;
            *needsPaint = true; // schedule a screen paint to update the shiftY values
            setTimerInterval(1);      
         }
      }
      else
         surfaceWillChange = true; // block all screen updates
      return;
   }  
   desiredglShiftY = glShiftY = 0;         
   setShiftYonNextUpdateScreen = true;
   appW = width;
   appH = height;
   surfaceWillChange = false;
   window = ANativeWindow_fromSurface(env, surface);
   realAppH = (*env)->CallStaticIntMethod(env, applicationClass, jgetHeight);
   if (lastWindow && lastWindow != window)
   {  
      destroyEGL();
      initGLES(&screen);
      recreateTextures(imgTextures);
   }
   lastWindow = window;
}
#endif

static void initPoints()
{
   pointsProgram = createProgram(POINTS_VERTEX_CODE, POINTS_FRAGMENT_CODE);
   setCurrentProgram(lrpProgram);
   pointsColor = glGetUniformLocation(pointsProgram, "a_Color"); GL_CHECK_ERROR
   pointsAlpha = glGetAttribLocation(pointsProgram, "alpha"); GL_CHECK_ERROR
   pointsPosition = glGetAttribLocation(pointsProgram, "a_Position"); GL_CHECK_ERROR // get handle to vertex shader's vPosition member
   glEnableVertexAttribArray(pointsAlpha); GL_CHECK_ERROR // Enable a handle to the colors - since this is the only one used, keep it enabled all the time
   glEnableVertexAttribArray(pointsPosition); GL_CHECK_ERROR // Enable a handle to the vertices - since this is the only one used, keep it enabled all the time
}

static int pixLastRGB = -1;
void glDrawPixels(int32 n, int32 rgb)
{
   setCurrentProgram(pointsProgram);
   if (pixLastRGB != rgb)
   {
      PixelConv pc;
      pc.pixel = pixLastRGB = rgb;
      glUniform4f(pointsColor, f255[pc.r], f255[pc.g], f255[pc.b], 0); GL_CHECK_ERROR
   }                               
   glVertexAttribPointer(pointsAlpha, 1, GL_FLOAT, GL_FALSE, 0, glcolors); GL_CHECK_ERROR
   glVertexAttribPointer(pointsPosition, 2, GL_FLOAT, GL_FALSE, 0, glcoords); GL_CHECK_ERROR
   glDrawArrays(GL_POINTS, 0,n); GL_CHECK_ERROR
}

static void initShade()
{         
   shadeProgram = createProgram(SHADE_VERTEX_CODE, SHADE_FRAGMENT_CODE);
   setCurrentProgram(shadeProgram);
   shadeColor = glGetAttribLocation(shadeProgram, "a_Color"); GL_CHECK_ERROR
   shadePosition = glGetAttribLocation(shadeProgram, "a_Position"); GL_CHECK_ERROR // get handle to vertex shader's vPosition member
   glEnableVertexAttribArray(shadeColor); GL_CHECK_ERROR // Enable a handle to the colors - since this is the only one used, keep it enabled all the time
   glEnableVertexAttribArray(shadePosition); GL_CHECK_ERROR // Enable a handle to the vertices - since this is the only one used, keep it enabled all the time
   shcolors[3] = shcolors[7] = shcolors[11] = shcolors[15] = shcolors[19] = shcolors[23] = 1; // note: last 2 colors are not used by opengl
}

void glFillShadedRect(Object g, int32 x, int32 y, int32 w, int32 h, PixelConv c1, PixelConv c2, bool horiz)
{
   if (pixcolors != (int32*)glcolors) flushPixels(4);
   setCurrentProgram(shadeProgram);
   glVertexAttribPointer(shadeColor, 4, GL_FLOAT, GL_FALSE, 0, shcolors); GL_CHECK_ERROR
   glVertexAttribPointer(shadePosition, 2, GL_FLOAT, GL_FALSE, 0, shcoords); GL_CHECK_ERROR
   
   y += glShiftY;
   
   shcoords[0] = shcoords[2] = x;
   shcoords[1] = shcoords[7] = y;
   shcoords[3] = shcoords[5] = y+h;
   shcoords[4] = shcoords[6] = x+w;

   if (!horiz)
   {
      shcolors[0] = shcolors[12] = f255[c2.r]; // upper left + upper right
      shcolors[1] = shcolors[13] = f255[c2.g];
      shcolors[2] = shcolors[14] = f255[c2.b];
      
      shcolors[4] = shcolors[8]  = f255[c1.r]; // lower left + lower right
      shcolors[5] = shcolors[9]  = f255[c1.g];
      shcolors[6] = shcolors[10] = f255[c1.b];
   }
   else
   {
      shcolors[0] = shcolors[4] = f255[c2.r];  // upper left + lower left
      shcolors[1] = shcolors[5] = f255[c2.g];
      shcolors[2] = shcolors[6] = f255[c2.b];
      
      shcolors[8]  = shcolors[12] = f255[c1.r]; // lower right + upper right
      shcolors[9]  = shcolors[13] = f255[c1.g];
      shcolors[10] = shcolors[14] = f255[c1.b];
   }
    
   glSetClip(Graphics_clipX1(g),Graphics_clipY1(g),Graphics_clipX2(g),Graphics_clipY2(g)); GL_CHECK_ERROR
   glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, rectOrder); GL_CHECK_ERROR
   glClearClip();
}

void glDrawCylindricShade(Object g, int32 x, int32 y, int32 w, int32 h, PixelConv ul, PixelConv ll, PixelConv lr, PixelConv ur)
{                
   if (pixcolors != (int32*)glcolors) flushPixels(5);
   setCurrentProgram(shadeProgram);
   glVertexAttribPointer(shadeColor, 4, GL_FLOAT, GL_FALSE, 0, shcolors); GL_CHECK_ERROR
   glVertexAttribPointer(shadePosition, 2, GL_FLOAT, GL_FALSE, 0, shcoords); GL_CHECK_ERROR

   y += glShiftY;
   
   shcoords[0] = shcoords[2] = x;
   shcoords[1] = shcoords[7] = y;
   shcoords[3] = shcoords[5] = y+h;
   shcoords[4] = shcoords[6] = x+w;

   shcolors[0] = f255[ul.r]; // upper left
   shcolors[1] = f255[ul.g];
   shcolors[2] = f255[ul.b];

   shcolors[4] = f255[ll.r]; // lower left
   shcolors[5] = f255[ll.g];
   shcolors[6] = f255[ll.b];

   shcolors[8]  = f255[lr.r]; // lower right
   shcolors[9]  = f255[lr.g];
   shcolors[10] = f255[lr.b];

   shcolors[12] = f255[ur.r]; // upper right
   shcolors[13] = f255[ur.g];
   shcolors[14] = f255[ur.b];

   glSetClip(Graphics_clipX1(g),Graphics_clipY1(g),Graphics_clipX2(g),Graphics_clipY2(g));
   glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, rectOrder); GL_CHECK_ERROR
   glClearClip();
}

void initTexture()
{         
   textureProgram = createProgram(TEXTURE_VERTEX_CODE, TEXTURE_FRAGMENT_CODE);
   setCurrentProgram(textureProgram);
   textureS     = glGetUniformLocation(textureProgram, "sTexture"); GL_CHECK_ERROR
   texturePoint = glGetAttribLocation(textureProgram, "vertexPoint"); GL_CHECK_ERROR
   textureCoord = glGetAttribLocation(textureProgram, "aTextureCoord"); GL_CHECK_ERROR

   glEnableVertexAttribArray(textureCoord); GL_CHECK_ERROR
   glEnableVertexAttribArray(texturePoint); GL_CHECK_ERROR
}

void glLoadTexture(Object img, int32* textureId, Pixel *pixels, int32 width, int32 height, bool updateList)
{
   int32 i;
   PixelConv* pf = (PixelConv*)pixels;
   PixelConv* pt = (PixelConv*)xmalloc(width*height*4), *pt0 = pt;
   bool textureAlreadyCreated = *textureId != 0;
   if (!pt)
      return;

   if (!textureAlreadyCreated) {glGenTextures(1, (GLuint*)textureId); GL_CHECK_ERROR}
   // OpenGL ES provides support for non-power-of-two textures, provided that the s and t wrap modes are both GL_CLAMP_TO_EDGE.
   glBindTexture(GL_TEXTURE_2D, *textureId); GL_CHECK_ERROR
   if (!textureAlreadyCreated)
   {
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); GL_CHECK_ERROR
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); GL_CHECK_ERROR
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); GL_CHECK_ERROR
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); GL_CHECK_ERROR
//      glUniform1i(textureS, 0); GL_CHECK_ERROR - was giving INVALID OPERATION
   }
   // must invert the pixels from ARGB to RGBA
   for (i = width*height; --i >= 0;pt++,pf++) {pt->a = pf->r; pt->b = pf->g; pt->g = pf->b; pt->r = pf->a;}
   if (textureAlreadyCreated)
   {
      glTexSubImage2D(GL_TEXTURE_2D, 0,0,0,width,height, GL_RGBA,GL_UNSIGNED_BYTE, pt0); GL_CHECK_ERROR
   }
   else
   {
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA,GL_UNSIGNED_BYTE, pt0); GL_CHECK_ERROR
      if (updateList)
         imgTextures = VoidPsAdd(imgTextures, img, null);
   }
   glBindTexture(GL_TEXTURE_2D, 0); GL_CHECK_ERROR
   xfree(pt0);
}

void glDeleteTexture(Object img, int32* textureId, bool updateList)
{         
   glDeleteTextures(1,(GLuint*)textureId); GL_CHECK_ERROR
   *textureId = 0;                               
   if (updateList)
      imgTextures = VoidPsRemove(imgTextures, img, null);
}

void glDrawTexture(int32 textureId, int32 x, int32 y, int32 w, int32 h, int32 dstX, int32 dstY, int32 imgW, int32 imgH)
{         
   GLfloat* coords = texcoords;
   if (pixcolors != (int32*)glcolors) flushPixels(6);
   setCurrentProgram(textureProgram);
   glBindTexture(GL_TEXTURE_2D, textureId); GL_CHECK_ERROR

   dstY += glShiftY;

   // destination coordinates
   coords[0] = coords[6] = dstX;
   coords[1] = coords[3] = dstY+h;
   coords[2] = coords[4] = dstX+w;
   coords[5] = coords[7] = dstY;
   glVertexAttribPointer(texturePoint, 2, GL_FLOAT, false, 0, coords); GL_CHECK_ERROR

   // source coordinates
   GLfloat left = (float)x/(float)imgW,top=(float)y/(float)imgH,right=(float)(x+w)/(float)imgW,bottom=(float)(y+h)/(float)imgH; // 0,0,1,1
   coords[ 8] = coords[14] = left;
   coords[ 9] = coords[11] = bottom;
   coords[10] = coords[12] = right;
   coords[13] = coords[15] = top;
   glVertexAttribPointer(textureCoord, 2, GL_FLOAT, false, 0, &coords[8]); GL_CHECK_ERROR

   glDrawArrays(GL_TRIANGLE_FAN, 0, 4); GL_CHECK_ERROR
   glBindTexture(GL_TEXTURE_2D, 0); GL_CHECK_ERROR
}

void initLineRectPoint()
{         
   lrpProgram = createProgram(LRP_VERTEX_CODE, LRP_FRAGMENT_CODE);
   setCurrentProgram(lrpProgram);
   lrpColor = glGetUniformLocation(lrpProgram, "a_Color"); GL_CHECK_ERROR
   lrpPosition = glGetAttribLocation(lrpProgram, "a_Position"); GL_CHECK_ERROR
   glEnableVertexAttribArray(lrpPosition); GL_CHECK_ERROR
}

void glSetLineWidth(int32 w)
{         
   setCurrentProgram(lrpProgram);
   glLineWidth(w); GL_CHECK_ERROR
}

#define IS_PIXEL (1<<28)
#define IS_DIAGONAL  (1<<27)

static void clearPixels()
{
   pixcoords = (int32*)glcoords;
   pixcolors = (int32*)glcolors;
}

void flushPixels(int q)
{         
   if (pixcolors != (int32*)glcolors)
   {
      int32 n = pixcolors-(int32*)glcolors, i;
      PixelConv pc;
      GLfloat* coords = lrcoords;
      setCurrentProgram(lrpProgram);
      pixcoords = (int32*)glcoords;
      pixcolors = (int32*)glcolors;
      glVertexAttribPointer(lrpPosition, 2, GL_FLOAT, GL_FALSE, 0, coords); GL_CHECK_ERROR
      int32 lastRGBA = ~*pixcolors;
      int32 x,y,w,h,x2,y2;
      int32 ty = glShiftY;
      for (i = 0; i < n; i++)
      {  
         // color
         int32 rgba = *pixcolors++;
         if (lastRGBA != rgba) // prevent color change = performance x2 in galaxy tab2
         {
            pc.pixel = lastRGBA = rgba;
            glUniform4f(lrpColor, f255[pc.r],f255[pc.g],f255[pc.b],f255[pc.a]); GL_CHECK_ERROR
         }
         // coord
         x = *pixcoords++;
         y = *pixcoords++;
         y += ty;
         if (x & IS_DIAGONAL)
         {                    
            x2 = *pixcoords++;
            y2 = *pixcoords++ + ty;
            coords[0] = x & ~IS_DIAGONAL;
            coords[1] = y;
            coords[2] = x2;
            coords[3] = y2;
            glDrawArrays(GL_LINES, 0,2); GL_CHECK_ERROR
         }
         else
         {
            if (x & IS_PIXEL)
            {
               x &= ~IS_PIXEL;
               w = h = 1;
            }
            else
            {
               w = *pixcoords++;
               h = *pixcoords++;
            }
            coords[0] = coords[2] = x;
            coords[1] = coords[7] = y;
            coords[3] = coords[5] = y+h;
            coords[4] = coords[6] = x+w;
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, rectOrder); GL_CHECK_ERROR
         }
      }
      clearPixels();
   }
}

static void add2pipe(int32 x, int32 y, int32 w, int32 h, int32 rgb, int32 a)
{
   bool isPixel = (x & IS_PIXEL) != 0;
   if ((pixcoords+(isPixel ? 2 : 4)) > pixEnd)
      flushPixels(7);
   *pixcoords++ = x;
   *pixcoords++ = y;
   if (!isPixel)
   {
      *pixcoords++ = w;
      *pixcoords++ = h;
   }
   PixelConv pc;
   pc.pixel = rgb;
   pc.a = a;
   *pixcolors++ = pc.pixel;
}

void glDrawPixel(int32 x, int32 y, int32 rgb, int32 a)
{   
   add2pipe(x|IS_PIXEL,y,1,1,rgb,a);
}

void glDrawThickLine(int32 x1, int32 y1, int32 x2, int32 y2, int32 rgb, int32 a)
{
   add2pipe(x1|IS_DIAGONAL,y1,x2,y2,rgb,a);
}

void glDrawLine(int32 x1, int32 y1, int32 x2, int32 y2, int32 rgb, int32 a)
{
   // The Samsung Galaxy Tab 2 (4.0.4) has a bug in opengl for drawing horizontal/vertical lines: it draws at wrong coordinates, and incomplete sometimes. so we use fillrect, which always work
   if (x1 == x2)
      add2pipe(min32(x1,x2),min32(y1,y2),1,abs32(y2-y1),rgb,a);
   else
   if (y1 == y2) 
      add2pipe(min32(x1,x2),min32(y1,y2),abs32(x2-x1),1,rgb,a);
   else              
      add2pipe(x1|IS_DIAGONAL,y1,x2,y2,rgb,a);
}

void glFillRect(int32 x, int32 y, int32 w, int32 h, int32 rgb, int32 a)
{
   add2pipe(x,y,w,h,rgb,a);
}

typedef union
{
   struct{ GLubyte r, g, b, a; };
   Pixel pixel;
} glpixel;

int32 glGetPixel(int32 x, int32 y)
{                
   glpixel gp;
   if (pixcolors != (int32*)glcolors) flushPixels(8);
   glReadPixels(x, appH-y-1, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, &gp); GL_CHECK_ERROR
   return (((int32)gp.r) << 16) | (((int32)gp.g) << 8) | (int32)gp.b;
}

void glGetPixels(Pixel* dstPixels,int32 srcX,int32 srcY,int32 width,int32 height,int32 pitch)
{          
   Pixel* p;
   PixelConv pc;
   glpixel gp;
   int32 i;
   if (pixcolors != (int32*)glcolors) flushPixels(9);
   for (; height-- > 0; srcY++,dstPixels += pitch)
   {
      glReadPixels(srcX, appH-srcY-1, width, 1, GL_RGBA, GL_UNSIGNED_BYTE, dstPixels); GL_CHECK_ERROR
      p = dstPixels;
      for (i = 0; i < width; i++)
      {
         gp.pixel = *p;
         pc.a = 255;//gp.a; - with this, the transition effect causes a fade-out when finished in UIGadgets
         pc.r = gp.r;
         pc.g = gp.g;
         pc.b = gp.b;
         *p++ = pc.pixel;       
      }
   }   
}

// note: glSetClip cannot be used for points, lines and rectangles, since they are cached and drawn later
void glSetClip(int32 x1, int32 y1, int32 x2, int32 y2) 
{  
   if (x1 == 0 && y1 == 0 && x2 == appW && y2 == appH) // set clip to whole screen disables it
      {glClearClip(); GL_CHECK_ERROR}
   else
   {
      y1 += glShiftY;
      y2 += glShiftY;
      glEnable(GL_SCISSOR_TEST); GL_CHECK_ERROR
      if (x1 < 0) x1 = 0; else if (x1 > appW) x1 = appW;
      if (x2 < 0) x2 = 0; else if (x2 > appW) x2 = appW;
      if (y1 < 0) y1 = 0; else if (y1 > appH) y1 = appH;
      if (y2 < 0) y2 = 0; else if (y2 > appH) y2 = appH;
      int32 h = y2-y1, w = x2-x1;
      if (h < 0) h = 0;
      if (w < 0) w = 0;
      glScissor(x1,appH-y2,w,h); GL_CHECK_ERROR
   }
}

void glClearClip()
{            
   glDisable(GL_SCISSOR_TEST); GL_CHECK_ERROR
}   

void flushAll()
{
   flushPixels(10);
   glFlush(); GL_CHECK_ERROR
}

static void setProjectionMatrix(GLfloat w, GLfloat h)
{                              
   GLfloat mat[16] =
   {
      2.0/w, 0.0, 0.0, -1.0,
      0.0, -2.0/h, 0.0, 1.0,
      0.0, 0.0, -1.0, 0.0,
      0.0, 0.0, 0.0, 1.0
   };
   setCurrentProgram(textureProgram); glUniformMatrix4fv(glGetUniformLocation(textureProgram, "projectionMatrix"), 1, 0, mat); GL_CHECK_ERROR
   setCurrentProgram(lrpProgram);     glUniformMatrix4fv(glGetUniformLocation(lrpProgram    , "projectionMatrix"), 1, 0, mat); GL_CHECK_ERROR
   setCurrentProgram(pointsProgram);  glUniformMatrix4fv(glGetUniformLocation(pointsProgram , "projectionMatrix"), 1, 0, mat); GL_CHECK_ERROR
   setCurrentProgram(shadeProgram);   glUniformMatrix4fv(glGetUniformLocation(shadeProgram  , "projectionMatrix"), 1, 0, mat); GL_CHECK_ERROR
   glViewport(0, 0, w, h); GL_CHECK_ERROR
}

/////////////////////////////////////////////////////////////////////////
bool checkGLfloatBuffer(Context c, int32 n)
{
   if (n > flen)
   {
      xfree(glcoords);
      xfree(glcolors);
      flen = n*3/2;
      int len = flen*2;
      glcoords = (GLfloat*)xmalloc(sizeof(GLfloat)*len); pixcoords = (int32*)glcoords;
      glcolors = (GLfloat*)xmalloc(sizeof(GLfloat)*flen); pixcolors = (int32*)glcolors;
      pixEnd = pixcoords + len;
      if (!glcoords || !glcolors)
      {
         throwException(c, OutOfMemoryError, "Cannot allocate buffer for drawPixels");
         xfree(glcoords);
         xfree(glcolors);
         flen = 0;
         return false;
      }
   }
   return true;
}

bool setupGL(int width, int height)
{
    int i;
    appW = width;
    appH = height;

    initTexture();
    initLineRectPoint();
    initPoints();
    initShade();
    setProjectionMatrix(appW,appH);

    glPixelStorei(GL_PACK_ALIGNMENT, 1); GL_CHECK_ERROR
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1); GL_CHECK_ERROR

    glDisable(GL_CULL_FACE); GL_CHECK_ERROR
    glDisable(GL_DEPTH_TEST); GL_CHECK_ERROR
    glEnable(GL_BLEND); GL_CHECK_ERROR // enable color alpha channel
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); GL_CHECK_ERROR

    for (i = 0; i <= 15; i++)
        ftransp[i] = (GLfloat)((i<<4)|0xF) / (GLfloat)255;
    for (i = 0; i <= 255; i++)
        f255[i] = (GLfloat)i/(GLfloat)255;

    return checkGLfloatBuffer(mainContext,10000);
}

#ifdef ANDROID
bool initGLES(ScreenSurface /*screen*/unused)
{
   int32 i;
   const EGLint attribs[] =
   {
       EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
       EGL_BLUE_SIZE, 8,
       EGL_GREEN_SIZE, 8,
       EGL_RED_SIZE, 8,
       EGL_ALPHA_SIZE, 8,
       EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
       EGL_NONE
   };
   EGLint context_attribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
   EGLDisplay display;
   EGLConfig config;
   EGLint numConfigs;
   EGLint format;
   EGLSurface surface;
   EGLContext context;
   EGLint width;
   EGLint height;
   
   if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY)    {debug("eglGetDisplay() returned error %d", eglGetError()); return false;}
   if (!eglInitialize(display, 0, 0))                                       {debug("eglInitialize() returned error %d", eglGetError()); return false;}
   if (!eglChooseConfig(display, attribs, &config, 1, &numConfigs))         {debug("eglChooseConfig() returned error %d", eglGetError()); destroyEGL(); return false;}
   if (!eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &format)) {debug("eglGetConfigAttrib() returned error %d", eglGetError()); destroyEGL(); return false;}

   ANativeWindow_setBuffersGeometry(window, 0, 0, format);

   if (!(surface = eglCreateWindowSurface(display, config, window, 0)))     {debug("eglCreateWindowSurface() returned error %d", eglGetError()); destroyEGL(); return false;}
   if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, context_attribs))) {debug("eglCreateContext() returned error %d", eglGetError()); destroyEGL(); return false;}
   if (!eglMakeCurrent(display, surface, surface, context))                 {debug("eglMakeCurrent() returned error %d", eglGetError()); destroyEGL(); return false;}
   if (!eglQuerySurface(display, surface, EGL_WIDTH, &width) || !eglQuerySurface(display, surface, EGL_HEIGHT, &height)) {debug("eglQuerySurface() returned error %d", eglGetError()); destroyEGL(); return false;}

   _display = display;
   _surface = surface;
   _context = context;
   return setupGL(width,height);
}

static void destroyEGL()
{         
   eglMakeCurrent(_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
   eglDestroyContext(_display, _context);
   eglDestroySurface(_display, _surface);
   eglTerminate(_display);

   _display = EGL_NO_DISPLAY;
   _surface = EGL_NO_SURFACE;
   _context = EGL_NO_CONTEXT;
}
#endif

void privateScreenChange(int32 w, int32 h)
{
   appW = w;
   appH = h;                           
   clearPixels();
   setProjectionMatrix(w,h); 
}

bool graphicsStartup(ScreenSurface screen, int16 appTczAttr)
{
   screen->bpp = 32;
   screen->screenX = screen->screenY = 0;
   screen->screenW = lastW;
   screen->screenH = lastH;
   screen->hRes = ascrHRes;
   screen->vRes = ascrVRes;
   return initGLES(screen);
}

bool graphicsCreateScreenSurface(ScreenSurface screen)
{
#ifndef ANDROID
   screen->extension = deviceCtx;
#endif
   screen->pitch = screen->screenW * screen->bpp / 8;
   screen->pixels = (uint8*)1;
   return screen->pixels != null;
}

void graphicsDestroy(ScreenSurface screen, bool isScreenChange)
{
#ifdef ANDROID
   if (!isScreenChange)
   {
      destroyEGL();
      xfree(screen->extension);
      xfree(glcoords);
      xfree(glcolors);
   }
#else
   if (isScreenChange)
       screen->extension = NULL;
   else
   {
      if (screen->extension)
         free(screen->extension);
      deviceCtx = screen->extension = NULL;
      xfree(glcoords);
      xfree(glcolors);
   }
#endif
}

void setTimerInterval(int32 t);
void setShiftYgl()
{
#ifdef ANDROID           
   if (setShiftYonNextUpdateScreen && needsPaint != null)
   {       
      setShiftYonNextUpdateScreen = false;
      glShiftY = desiredglShiftY - desiredScreenShiftY;     // set both at once
      screen.shiftY = desiredScreenShiftY;
      *needsPaint = true; // now that the shifts has been set, schedule another window update to paint at the given location
      setTimerInterval(1);      
   }
#endif    
}
extern int32 desiredScreenShiftY;
void graphicsUpdateScreenIOS();
void graphicsUpdateScreen(Context currentContext, ScreenSurface screen)
{ 
   if (surfaceWillChange) {clearPixels(); return;}
   if (pixcolors != (int32*)glcolors) flushPixels(11);
#ifdef ANDROID
   eglSwapBuffers(_display, _surface);
#else
   graphicsUpdateScreenIOS();
#endif
   // erase buffer with keyboard's background color
   PixelConv gray;
   gray.pixel = shiftScreenColorP ? *shiftScreenColorP : 0xFFFFFF;
   glClearColor(f255[gray.r],f255[gray.g],f255[gray.b],1); GL_CHECK_ERROR
   glClear(GL_COLOR_BUFFER_BIT); GL_CHECK_ERROR
}
