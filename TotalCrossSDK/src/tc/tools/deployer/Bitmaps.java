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

package tc.tools.deployer;

import totalcross.io.ByteArrayStream;
import totalcross.io.Stream;
import totalcross.ui.gfx.Color;
import totalcross.ui.gfx.Graphics;
import totalcross.ui.image.Image;
import totalcross.ui.image.ImageException;
import totalcross.util.*;

public class Bitmaps
{
   static int palette256[] =  // web safe palette
   {
      0xFFFFFF,0xFFCCFF,0xFF99FF,0xFF66FF,0xFF33FF,0xFF00FF,0xFFFFCC,0xFFCCCC, // 0-7
      0xFF99CC,0xFF66CC,0xFF33CC,0xFF00CC,0xFFFF99,0xFFCC99,0xFF9999,0xFF6699, // 8-16
      0xFF3399,0xFF0099,0xCCFFFF,0xCCCCFF,0xCC99FF,0xCC66FF,0xCC33FF,0xCC00FF, // 16-23
      0xCCFFCC,0xCCCCCC,0xCC99CC,0xCC66CC,0xCC33CC,0xCC00CC,0xCCFF99,0xCCCC99, // 24-31
      0xCC9999,0xCC6699,0xCC3399,0xCC0099,0x99FFFF,0x99CCFF,0x9999FF,0x9966FF, // 32-39
      0x9933FF,0x9900FF,0x99FFCC,0x99CCCC,0x9999CC,0x9966CC,0x9933CC,0x9900CC, // 40-47
      0x99FF99,0x99CC99,0x999999,0x996699,0x993399,0x990099,0x66FFFF,0x66CCFF, // 48-55
      0x6699FF,0x6666FF,0x6633FF,0x6600FF,0x66FFCC,0x66CCCC,0x6699CC,0x6666CC, // 56-63
      0x6633CC,0x6600CC,0x66FF99,0x66CC99,0x669999,0x666699,0x663399,0x660099, // 64-71
      0x33FFFF,0x33CCFF,0x3399FF,0x3366FF,0x3333FF,0x3300FF,0x33FFCC,0x33CCCC, // 72-79
      0x3399CC,0x3366CC,0x3333CC,0x3300CC,0x33FF99,0x33CC99,0x339999,0x336699, // 80-87
      0x333399,0x330099,0x00FFFF,0x00CCFF,0x0099FF,0x0066FF,0x0033FF,0x0000FF, // 88-95
      0x00FFCC,0x00CCCC,0x0099CC,0x0066CC,0x0033CC,0x0000CC,0x00FF99,0x00CC99, // 96-103
      0x009999,0x006699,0x003399,0x000099,0xFFFF66,0xFFCC66,0xFF9966,0xFF6666, // 104-111
      0xFF3366,0xFF0066,0xFFFF33,0xFFCC33,0xFF9933,0xFF6633,0xFF3333,0xFF0033, // 112-119
      0xFFFF00,0xFFCC00,0xFF9900,0xFF6600,0xFF3300,0xFF0000,0xCCFF66,0xCCCC66, // 120-127
      0xCC9966,0xCC6666,0xCC3366,0xCC0066,0xCCFF33,0xCCCC33,0xCC9933,0xCC6633, // 128-135
      0xCC3333,0xCC0033,0xCCFF00,0xCCCC00,0xCC9900,0xCC6600,0xCC3300,0xCC0000, // 136-143
      0x99FF66,0x99CC66,0x999966,0x996666,0x993366,0x990066,0x99FF33,0x99CC33, // 144-151
      0x999933,0x996633,0x993333,0x990033,0x99FF00,0x99CC00,0x999900,0x996600, // 152-159
      0x993300,0x990000,0x66FF66,0x66CC66,0x669966,0x666666,0x663366,0x660066, // 160-167
      0x66FF33,0x66CC33,0x669933,0x666633,0x663333,0x660033,0x66FF00,0x66CC00, // 168-175
      0x669900,0x666600,0x663300,0x660000,0x33FF66,0x33CC66,0x339966,0x336666, // 176-183
      0x333366,0x330066,0x33FF33,0x33CC33,0x339933,0x336633,0x333333,0x330033, // 184-191
      0x33FF00,0x33CC00,0x339900,0x336600,0x333300,0x330000,0x00FF66,0x00CC66, // 192-199
      0x009966,0x006666,0x003366,0x000066,0x00FF33,0x00CC33,0x009933,0x006633, // 200-207
      0x003333,0x000033,0x00FF00,0x00CC00,0x009900,0x006600,0x003300,0x111111, // 208-215
      0x222222,0x444444,0x555555,0x777777,0x888888,0xAAAAAA,0xBBBBBB,0xDDDDDD, // 216-223
      0xEEEEEE,0xC0C0C0,0x800000,0x800080,0x008000,0x008080,0x000000,0x000000, // 224-231
      0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000, // 232-239
      0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000, // 240-247
      0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x060003  // 248-255
   };

   // file bytes
   Bmp bmp15x9x8;
   Bmp bmp30x18x8;
   Bmp bmp22x22x8;
   Bmp bmp44x44x8;
   Bmp bmp32x32x8;
   Bmp bmp16x16x8;
   Bmp bmp48x48x8;
   byte []bytes15x9x8;
   byte []bytes30x18x8;
   byte []bytes22x22x8;
   byte []bytes44x44x8;
   byte []bytes32x32x8;
   byte []bytes16x16x8;
   byte []bytes48x48x8;
   byte []bytes20x16x8;

   Image bmpTemplate;
   byte []bytesTemplate;
   String prefix;

   private int loadBmps(String prefix, boolean showMsg)
   {
      if (bytesTemplate == null) bytesTemplate= Utils.findAndLoadFile(prefix+"appicon.gif", showMsg);
      int lost = 8;
      // search with the prefix
      if (bytes15x9x8  == null) bytes15x9x8  = Utils.findAndLoadFile(prefix+"icon15x9x8.bmp", showMsg); if (bytes15x9x8  != null) lost--;
      if (bytes30x18x8 == null) bytes30x18x8 = Utils.findAndLoadFile(prefix+"icon30x18x8.bmp",showMsg); if (bytes30x18x8 != null) lost--;
      if (bytes22x22x8 == null) bytes22x22x8 = Utils.findAndLoadFile(prefix+"icon22x22x8.bmp",showMsg); if (bytes22x22x8 != null) lost--;
      if (bytes44x44x8 == null) bytes44x44x8 = Utils.findAndLoadFile(prefix+"icon44x44x8.bmp",showMsg); if (bytes44x44x8 != null) lost--;
      if (bytes16x16x8 == null) bytes16x16x8 = Utils.findAndLoadFile(prefix+"icon16x16x8.bmp",showMsg); if (bytes16x16x8 != null) lost--;
      if (bytes32x32x8 == null) bytes32x32x8 = Utils.findAndLoadFile(prefix+"icon32x32x8.bmp",showMsg); if (bytes32x32x8 != null) lost--;
      if (bytes48x48x8 == null) bytes48x48x8 = Utils.findAndLoadFile(prefix+"icon48x48x8.bmp",showMsg); if (bytes48x48x8 != null) lost--;
      if (bytes20x16x8 == null) bytes20x16x8 = Utils.findAndLoadFile(prefix+"icon20x16x8.bmp",showMsg); if (bytes20x16x8 != null) lost--;
      return lost;
   }

   public Bitmaps(String prefix) throws Exception // guich@330_48: added a prefix
   {
      this.prefix = prefix;
      boolean showMsg=prefix.length() > 0; // guich@503_8: show only if we're giving an icon prefix
      // first, search with prefix
      int lost = loadBmps(prefix, showMsg);
      // if some was lost, search with the prefix_
      if (prefix.length() > 0 && lost > 0)
         lost = loadBmps(prefix+"_", showMsg);
      // if some was lost, search without the prefix
      if (prefix.length() > 0 && lost > 0)
         lost = loadBmps("", showMsg);
      // if still lost, get from the etc/images folder
      if (lost > 0 && prefix.length() > 0)
         lost = loadBmps(DeploySettings.etcDir+"images/"+prefix+"_", showMsg);
      if (bytesTemplate == null) // only if the template was not found
      {
         if (lost > 0)
            lost = loadBmps(DeploySettings.etcDir+"images/", false);
         if (lost > 0)
            throw new DeployerException(lost+" icons not found, neither in classpath nor in the working directory. Be sure to put their location in the classpath or copy them to the working directory.");
      }
      // now we're sure that everything was found
      int oldW = totalcross.sys.Settings.screenWidth;
      int oldH = totalcross.sys.Settings.screenHeight;
      totalcross.sys.Settings.screenWidth = 1024; totalcross.sys.Settings.screenHeight = 1024; // let Image work correctly
      if (bytesTemplate != null)
      {
         bmpTemplate = new Image(bytesTemplate);
         if (bmpTemplate.getWidth() != bmpTemplate.getHeight())
            throw new IllegalArgumentException("Error: the appicon.gif file must be square (width = height)!");
      }
      bmp15x9x8   = bytes15x9x8  != null ? new Bmp(bytes15x9x8)  : new Bmp(15,-9);  // palm os
      bmp30x18x8  = bytes30x18x8 != null ? new Bmp(bytes30x18x8) : new Bmp(30,-18); // palm os
      bmp22x22x8  = bytes22x22x8 != null ? new Bmp(bytes22x22x8) : new Bmp(22,-22); // palm os
      bmp44x44x8  = bytes44x44x8 != null ? new Bmp(bytes44x44x8) : new Bmp(44,-44); // palm os
      bmp32x32x8  = bytes32x32x8 != null ? new Bmp(bytes32x32x8) : new Bmp(32,32);  // windows
      bmp16x16x8  = bytes16x16x8 != null ? new Bmp(bytes16x16x8) : new Bmp(16,16);  // windows
      bmp48x48x8  = bytes48x48x8 != null ? new Bmp(bytes48x48x8) : new Bmp(48,48);  // windows

      if (oldW > 0) // restore only if screen had valid dimensions
      {
         totalcross.sys.Settings.screenWidth = oldW;
         totalcross.sys.Settings.screenHeight = oldH;
      }
   }

   public class Bmp
   {
      byte []pixels;
      int []palette;
      private byte []wholeImage;
      private boolean shouldInvertY;

      public Bmp(int w, int h) throws ImageException
      {
         shouldInvertY = false;
         int back = bmpTemplate.getGraphics().getPixel(0,0); // use pixel at 0,0 as the background color
         boolean invertY = h < 0;
         h = Math.abs(h);
         int k = Math.min(w,h);
         Image sized = bmpTemplate.getSmoothScaledInstance(k, k, back);
         if (w > h) // center the icon horizontally
         {
            Image img = new Image(w,h);
            Graphics gg = img.getGraphics();
            gg.backColor = back;
            gg.fillRect(0,0,w,h);
            gg.drawImage(sized,(w-k)/2,0);
            sized = img;
         }
         // the sized image is 24bpp. create the palette, reading all pixels in the image;
         palette = new int[palette256.length];
         totalcross.sys.Vm.arrayCopy(palette256, 0, palette, 0, palette256.length);
         //palette[0] = 0xFFFFFF; palette[255] = 0;
         int realW = ((w+3)>>2)<<2;
         pixels = new byte[realW * h];
         Graphics g = sized.getGraphics();

         // map the resized image into the web safe palette

         // ok, here's a very dirty dithering algorithm.
         // I really don't care for performance nor memory!
         // taken from Wikipedia
         int oldpixel, newpixel;
         int [][]pix = new int[w][h];
         // get all r,g,b components for all pixels
         for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
               pix[x][y] = g.getPixel(x,y);
         // now dither them
         for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
               oldpixel = pix[x][y];
               newpixel = findNearest(palette256, oldpixel);
               newpixel = palette256[newpixel];
               pix[x][y] = newpixel;
               // compute the quantization errors
               int or = (oldpixel >> 16) & 0xFF;
               int og = (oldpixel >>  8) & 0xFF;
               int ob =  oldpixel        & 0xFF;
               int nr = (newpixel >> 16) & 0xFF;
               int ng = (newpixel >>  8) & 0xFF;
               int nb =  newpixel        & 0xFF;

               int er = or - nr;
               int eg = og - ng;
               int eb = ob - nb;
               if (x < w-1)            pix[x+1][y]   = computePixel(pix[x+1][y],   7, er, eg, eb);
               if (x > 0 && y < h-1)   pix[x-1][y+1] = computePixel(pix[x-1][y+1], 3, er, eg, eb);
               if (y < h-1)            pix[x][y+1]   = computePixel(pix[x][y+1],   5, er, eg, eb);
               if (x < w-1 && y < h-1) pix[x+1][y+1] = computePixel(pix[x+1][y+1], 1, er, eg, eb);
            }
         for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
               pixels[(invertY?(h-y-1):y)*realW+x] = (byte)findExact(palette256, pix[x][y]);
      }

      private int computePixel(int p, int dif, int er, int eg, int eb)
      {
         int r = (p >> 16) & 0xFF;
         int g = (p >> 8)  & 0xFF;
         int b = p & 0xFF;
         r += dif/16 * er;
         g += dif/16 * eg;
         b += dif/16 * eb;
         if (r < 0) r = 0; else if (r > 255) r = 255;
         if (g < 0) g = 0; else if (g > 255) g = 255;
         if (b < 0) b = 0; else if (b > 255) b = 255;
         return (r << 16) | (g << 8) | b;
      }

      public Bmp(byte[] bytes) throws Exception
      {
         shouldInvertY = true;
         wholeImage = new byte[bytes.length - 14];
         System.arraycopy(bytes, 14, wholeImage, 0, wholeImage.length);
         wholeImage[22-14] *= 2; // guich@340_60: icons have the height doubled
         // header (54 bytes)
         // 0-1   magic chars 'BM'
         // 2-5   uint32 filesize (not reliable)
         // 6-7   uint16 0
         // 8-9   uint16 0
         // 10-13 uint32 bitmapOffset
         // 14-17 uint32 info size    --- icon starts here
         // 18-21 int32  width
         // 22-25 int32  height
         // 26-27 uint16 nplanes
         // 28-29 uint16 bits per pixel
         // 30-33 uint32 compression flag
         // 34-37 uint32 image size in bytes
         // 38-41 int32  biXPixelsPerMeter
         // 42-45 int32  biYPixelsPerMeter
         // 46-49 uint32 colors used
         // 50-53 uint32 important color count
         // 54-   uchar  bitmap bytes depending to type
         // Each scan line of image data is padded to the next four byte boundary

         /* guich@340 notes:
            An icon differs from a bmp in another way: the icon has a mask at the
            end of the file. Also, the height is doubled (for a 32x32 icon, the
            stored height is 32*2 = 64)

            Here is how to compute the icon's size.

            icon     - 32x32x8
            40       - header
            4*256    - palette: 256 colors
            32*32/1  - pixels / 1 pixel per byte
            32*32/8  - mask
            -------
            2216 bytes

            icon     - 32x32x4
            40       - header
            4*16     - palette: 16 colors
            32*32/2  - pixels / 2 pixels per byte
            32*32/8  - mask
            -------
            744 bytes

            icon     - 32x32x1
            40       - header
            4*2      - palette: 2 colors
            32*32/8  - pixels / 8 pixels per byte
            32*32/8  - mask
            -------
            304 bytes

            icon     - 16x16x4
            40       - header
            4*16     - palette: 16 colors
            20*16/2  - pixels / 2 pixels per byte
            16*16/8  - mask
            -------
            296 bytes

          */
         int width;
         int height;

         short bitsPerPixel;

         int compression;
         int imageSize;
         int numberColors;

         if (bytes[0] != 'B' || bytes[1] != 'M')
            return;
         int offset = 0;
         /*headerSize  = */readInt(offset);   offset += 4;
         width           = readInt(offset);   offset += 4;
         height          = readInt(offset);   offset += 4;
         /*planes      = */readShort(offset); offset += 2;
         bitsPerPixel    = readShort(offset); offset += 2;
         if (bitsPerPixel > 8) throw new IllegalArgumentException("Bitmaps cannot be 24bpp!");
         compression     = readInt(offset);   offset += 4;
         if (compression != 0)
           new Exception("The bitmap used to create icons can't be compressed (RLE)!");
         imageSize       = readInt(offset);   offset += 4;
         if (imageSize == 0) // fdiebolt@341_12: if image size is not defined, compute a default size
         {
            int div   = 32 / bitsPerPixel;
            int rowW  = ( (width + div - 1) / div) * div;
            imageSize = (rowW/(8/bitsPerPixel)) * height;
         }
         /*xPixels     = */readInt(offset);   offset += 4;
         /*yPixels     = */readInt(offset);   offset += 4;
         numberColors    = readInt(offset);   offset += 4;
         /*colorsImport= */readInt(offset);   offset += 4;
         //System.out.println(width+"x"+height+"x"+bitsPerPixel);
         if (numberColors <= 0)
            numberColors = 1 << bitsPerPixel;
         palette = loadPalette(wholeImage, offset, numberColors); offset += 4*numberColors;
         pixels = new byte[imageSize];
         System.arraycopy(wholeImage, offset, pixels, 0, imageSize);
         if (palette[0] == 0xFFFFFF && width != 22 && width != 15 && width != 30 && width != 44 && width != 20)
            System.out.println("This image won't appear correct in WinCE. Palette index 0 must be black (it is white): "+width+"x"+(height>>1)+"x"+bitsPerPixel+". Please read the instructions.");

         //Utils.println(""+width+'x'+height+'x'+bitsPerPixel+" ("+totalcross.sys.Convert.unsigned2hex(imageSize,4)+")");
      }
      private int readInt(int offset)
      {
         return (((wholeImage[offset+3]&0xFF) << 24) | ((wholeImage[offset+2]&0xFF) << 16) | ((wholeImage[offset+1]&0xFF) << 8) | (wholeImage[offset+0]&0xFF));
      }
      private short readShort(int offset)
      {
         return (short)(((wholeImage[offset+1]&0xFF) << 8) | (wholeImage[offset]&0xFF));
      }
   }

   /** Returns the nearest color in the given palette */
   private int[]lastPal;
   private int lastColor=-1,lastnc=-1;
   public int findNearest(int []palette, int color)
   {
      if (palette == lastPal && color == lastColor) // same of the last one?
         return lastnc;
      int nc=0;
      int sdist = 255 * 255 * 255 + 1;
      int r = (color >> 16) & 0xFF;
      int g = (color >>  8) & 0xFF;
      int b = color         & 0xFF;
      for (int i =0; i < palette.length; i++)
      {
         int c = palette[i];
         int cr = (c >> 16) & 0xFF;
         int cg = (c >>  8) & 0xFF;
         int cb = c         & 0xFF;
         int dist = (r-cr)*(r-cr) + (g-cg)*(g-cg) + (b-cb)*(b-cb);
         if (dist < sdist)
         {
            nc = i;
            sdist = dist;
         }
      }
      lastPal = palette;
      lastColor = color;
      lastnc = nc;
      return nc;
   }
   /** Returns the exact index for the matching color in the given palette, or -1 if not found */
   public int findExact(int []palette, int color)
   {
      for (int i =0; i < palette.length; i++)
         if (palette[i] == color)
            return i;
      return -1;
   }

   /** Copy pixel bits from src to dest, converting the palette
    *  If maskOffset != -1, the mask is created and stored in that location (the most bright pixel is used as background - usually, white)
    */
   private void copyBits(byte []src, int []paletteFrom, int[]paletteTo,
                         int srcWidthInBytes, byte []dest, int destOffset,
                         int destWidthInBytes, int rows, int bpp, int maskOffset, boolean inverseMask, boolean shouldInvertY) // TODO use the maskOffset in the other platforms too
   {
      IntHashtable ht = new IntHashtable(511);
      int color, newColor, idx, white=0,x,y;
      int maskWidthInBytes = ((srcWidthInBytes+31)/32) << 2;
      int conv[]=null;

      if (maskOffset != -1)
      {
         conv = inverseMask ? new int[]{1,2,4,8,16,32,64,128} : new int[]{128,64,32,16,8,4,2,1}; // epoc uses the inverse mask
         white = paletteFrom[findNearest(paletteFrom, 0xFFFFFF)];
      }
      // invert mono image?
      boolean doInvert = bpp == 1 && paletteFrom[0] != paletteTo[0];
      boolean invertY = shouldInvertY && rows < 0;
      rows = Math.abs(rows);

      for (y = 0; y < rows; y++)
      {
         int soff = (invertY ? y : rows-y-1)     * srcWidthInBytes;
         int doff = destOffset + y * destWidthInBytes;
         int moff = maskOffset + y * maskWidthInBytes;

         for (x = 0; x < destWidthInBytes; x++, doff++, soff++)
         {
            idx = src[soff] & 0xFF;
            if (doInvert || bpp != 8)
               dest[doff] = doInvert?(byte)(src[soff]^0xFF):src[soff];
            else
            {
               color = paletteFrom[idx];
               try
               {
                  newColor=ht.get(color);
               } catch (ElementNotFoundException e)
               {
                  newColor = findExact(paletteTo, color);
                  if (newColor == -1)
                     newColor = findNearest(paletteTo, color);
                  ht.put(color, newColor);
               }
               dest[doff] = (byte)newColor; // is just the index
            }
            if (maskOffset != -1) // also create and store the mask
            {
               color = paletteFrom[idx & 0xFF];
               int bit = x&7;
               if (color == white)
                  dest[moff] |= conv[bit];
               else
                  dest[moff] &= ~conv[bit];
               if (bit == 7)
                  moff++;
            }
         }
      }
   }

   /** In PalmOS prc file, the icon is stored as an icon-family, with the first
     * icon being the 1bpp and the 2nd icon being the 256 color icon. First there
     * is a 16 bytes header.<p>
     * Id: 1000 - 22x22
     *   first icon  (1bpp)
     *   second icon (8bpp)
     *   third  icon (1bpp/double density)
     *   fourth icon (8bpp/double density)
     * Id: 1001 - 15x9
     *   first icon  (1bpp)
     *   gap: 2 bytes
     *   second icon (8bpp)
     *   third icon  (1bpp/double density)
	 *   gap: 2 bytes
	 *   fourth icon (8bpp/double density)
     */
   public void savePalmOSIcons(byte []bytes, int offset1000, int offset1001)
   {
      int realSize;

      // get the 22x22x8
      offset1000 += 16; // skip next header
      realSize   = 22 * 22; // tc note: pilrc uses 22x22 as the real size, while codewarrior uses 22x32
      if (bmp22x22x8 != null)
         copyBits(bmp22x22x8.pixels, bmp22x22x8.palette, palette256, 24, bytes, offset1000, 22, 22, 8, -1,false, bmp22x22x8.shouldInvertY);
	  // get the 44x44x8
     offset1000 += realSize; // skip first icon size
	  offset1000 += 40; // skip next header
	  if (bmp44x44x8 != null)
        copyBits(bmp44x44x8.pixels, bmp44x44x8.palette, palette256, 44, bytes, offset1000, 44, 44, 8, -1,false, bmp44x44x8.shouldInvertY);

     // get the 15x9x8
     offset1001 += 16; // skip next header
     realSize   = 16 * 9;
     if (bmp15x9x8 != null)
        copyBits(bmp15x9x8.pixels, bmp15x9x8.palette, palette256, 16, bytes, offset1001, 16, 9, 8, -1,false, bmp15x9x8.shouldInvertY);
	  // get the 30x18x8
     offset1001 += realSize; // skip first icon size
	  offset1001 += 40; // skip next header
	  if (bmp30x18x8 != null)
	     copyBits(bmp30x18x8.pixels, bmp30x18x8.palette, palette256, 32, bytes, offset1001, 30, 18, 8, -1,false, bmp30x18x8.shouldInvertY);
   }

   public void saveWinCEIcons(byte []bytes, int bitmap16x16x8_Offset, int bitmap32x32x8_Offset, int bitmap48x48x8_Offset)
   {
      if (bmp16x16x8 != null && bitmap16x16x8_Offset != -1)
         copyBits(bmp16x16x8.pixels, bmp16x16x8.palette, loadPalette(bytes, bitmap16x16x8_Offset+40, 256), 16, bytes, bitmap16x16x8_Offset+40+1024, 16, -16, 8, bitmap16x16x8_Offset+16*16+40+1024, false, bmp16x16x8.shouldInvertY);
      if (bmp32x32x8 != null && bitmap32x32x8_Offset != -1)
         copyBits(bmp32x32x8.pixels, bmp32x32x8.palette, loadPalette(bytes, bitmap32x32x8_Offset+40, 256), 32, bytes, bitmap32x32x8_Offset+40+1024, 32, -32, 8, bitmap32x32x8_Offset+32*32+40+1024, false, bmp32x32x8.shouldInvertY);
      if (bmp48x48x8 != null && bitmap48x48x8_Offset != -1)
         copyBits(bmp48x48x8.pixels, bmp48x48x8.palette, loadPalette(bytes, bitmap48x48x8_Offset+40, 256), 48, bytes, bitmap48x48x8_Offset+40+1024, 48, -48, 8, bitmap48x48x8_Offset+48*48+40+1024, false, bmp48x48x8.shouldInvertY);
   }

   public void saveWin32Icon(byte[] bytes, int iconOffset)
   {
      if (bmp32x32x8 != null && iconOffset != -1)
         copyBits(bmp32x32x8.pixels, bmp32x32x8.palette, loadPalette(bytes, iconOffset+40, 256), 32, bytes, iconOffset+40+1024, 32, -32, 8, iconOffset+32*32+40+1024,false, bmp32x32x8.shouldInvertY);
   }

   public void saveAndroidIcon(java.util.zip.ZipOutputStream zos) throws Exception // 72x72 png
   {
      byte[] imgbytes = findIconFile("icon72x72.png");
      if (imgbytes != null) // guich@tc126_65
      {
         Image img = new Image(imgbytes);
         if (img.getWidth() != 72 || img.getHeight() != 72)
            throw new IllegalArgumentException("The Android icon must be 72x72 in size!");
         zos.write(imgbytes, 0, imgbytes.length);
      }
      else
      {
         imgbytes = bytesTemplate;
         if (imgbytes == null) imgbytes = bytes48x48x8;
         if (imgbytes == null) imgbytes = bytes44x44x8;
         if (imgbytes == null) imgbytes = bytes32x32x8;
   
         if (imgbytes != null)
         {
            Image img = new Image(imgbytes);
            if (img.getWidth() != 72)
               img = img.getSmoothScaledInstance(72,72,img.transparentColor);
            ByteArrayStream s = new ByteArrayStream(4096);
            img.createPng(s);
            zos.write(s.getBuffer(), 0, s.getPos());
         }
      }
   }

   private byte[] findIconFile(String suffix)
   {
      byte[] imgbytes = Utils.findAndLoadFile(prefix+suffix,false);
      if (imgbytes == null) imgbytes = Utils.findAndLoadFile(prefix+"_"+suffix,false);
      if (imgbytes == null) imgbytes = Utils.findAndLoadFile(suffix,false);
      return imgbytes;
   }
   
   public boolean saveBlackBerryIcon(Stream s) throws Exception
   {
      // guich@tc113_35: search for the dedicated blackberry icon.
      byte[] imgbytes = findIconFile("icon80x80.png");
      if (imgbytes != null)
      {
         Image img = new Image(imgbytes);
         if (img.getWidth() != 80 || img.getHeight() != 80)
            throw new IllegalArgumentException("The BlackBerry icon must be 80x80 in size!");
         
         s.writeBytes(imgbytes,0,imgbytes.length); // already a png file
         return true;
      }
      
      // guich@tc113_35: now try with the template icon
      if (bytesTemplate != null)
      {
         Image img = new Image(bytesTemplate);
         if (img.getWidth() > 80)
            img = img.getSmoothScaledInstance(80,80,img.transparentColor);
         img.createPng(s);
         return true;
      }
      
      // else, use the 32x32
      if (bmp32x32x8 == null)
         return false;

      try
      {
         Image img = new Image(32, 32);
         byte[] pixels = bmp32x32x8.pixels;
         int[] palette = bmp32x32x8.palette;
         Graphics g = img.getGraphics();
         int n = pixels.length-1;

         if (bmp32x32x8.shouldInvertY) // are icons upside down?
            for (int i = 0; i <= n; i++)
            {
               int x = i & 31, y = i >> 5; // guich@tc100b4_17: must use x/y so we can turn the icon upside-down
               g.foreColor = (int)palette[(int)(pixels[(((31-y)<<5)+x)] & 0xFF)];
               g.setPixel(x,y);
            }
         else
            for (int i = 0; i <= n; i++)
            {
               g.foreColor = (int)palette[(int)(pixels[i] & 0xFF)];
               g.setPixel(i % 32, i / 32);
            }

         img.createPng(s);
         return true;
      }
      catch (ImageException ex)
      {
         return false;
      }
   }

   private static final int TOLERANCE = 16;
   private static IntVector fillCoords = new IntVector(10000); // guich@tc110_57: use a stack and static variables instead of a method recursion to avoid stack overflows
   private static boolean fillVisited[];
   private static int[] fillPixels;
   private static int fillW,fillH, fillNewColor,fillOldColor;

   private static int colorDist(int rgb1, int rgb2)
   {
      int r1 = Color.getRed(rgb1);
      int g1 = Color.getGreen(rgb1);
      int b1 = Color.getBlue(rgb1);
      int r2 = Color.getRed(rgb2);
      int g2 = Color.getGreen(rgb2);
      int b2 = Color.getBlue(rgb2);
      return (Math.abs(r1-r2) + Math.abs(g1-g2) + Math.abs(b1-b2)) / 3;
   }

   private static void floodFill() throws Exception
   {
      do
      {
         int c = fillCoords.pop();
         int x = c & 0xFFFF;
         int y = (c >> 16) & 0xFFFF;
         int dist;
         int index = x + y * fillW;

         fillVisited[index] = true;
         c = fillPixels[index];
         if ((dist=colorDist(c, fillOldColor)) < TOLERANCE && c != fillNewColor)
         {
            fillPixels[index] = dist == 0 ? fillNewColor : Color.interpolate(fillNewColor,c);

            addFillPoint(x + 1, y);
            addFillPoint(x - 1, y);
            addFillPoint(x, y + 1);
            addFillPoint(x, y - 1);
         }
      }
      while (!fillCoords.isEmpty());
   }

   private static void addFillPoint(int x, int y)
   {
      int index = x + y * fillW;
      if (x >= 0 && x < fillW && y >= 0 && y < fillH && !fillVisited[index])
         fillCoords.push((y << 16) | x);
   }

   private static void floodFill(Image img, int color)
   {
      fillCoords.removeAllElements();
      fillW = img.getWidth();
      fillH = img.getHeight();
      fillPixels = (int[])img.getPixels();
      fillOldColor = img.transparentColor != Image.NO_TRANSPARENT_COLOR ? img.transparentColor : fillPixels[0];
      fillNewColor = 0;
      fillVisited = new boolean[fillW*fillH];

      addFillPoint(0,0);
      addFillPoint(fillW-1,0);
      addFillPoint(0,fillH-1);
      addFillPoint(fillW-1,fillH-1);
      try
      {
         floodFill();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   static final int IPHONE_DEB = 60;

   /**
    * Gets an image of size 60x60. Searches from the available sizes, and does a resize if needed. Creates a png and
    * returns it.
    */
   public byte[] getIPhoneIcon(int size)
   {
      byte[] imgbytes = null;
      try
      {
         // search for the dedicated iphone icon.
         imgbytes = findIconFile("icon" + size + "x" + size + ".png");
         if (imgbytes != null)
         {
            Image img = new Image(imgbytes);
            if (img.getWidth() != size || img.getHeight() != size)
               throw new IllegalArgumentException("The iPhone icon must be " + size + "x" + size + " in size!");
            return imgbytes; // use this icon as-is
         }

         // otherwise, use the other ones provided
         imgbytes = bytesTemplate;
         if (imgbytes == null) imgbytes = bytes48x48x8;
         if (imgbytes == null) imgbytes = bytes44x44x8;
         if (imgbytes == null) imgbytes = bytes32x32x8;
         if (imgbytes == null) imgbytes = bytes22x22x8;
         if (imgbytes == null) imgbytes = bytes16x16x8;
         if (imgbytes != null)
         {
            Image img = new Image(imgbytes);
            // guich@tc100b5_11: flood fill with tolerance
            floodFill(img,0);
            // resize to 60x60
            ByteArrayStream bas = new ByteArrayStream(8192);
            img = img.getSmoothScaledInstance(size, size, 0);
            img.transparentColor = 0; // not really necessary
            img.createPng(bas);
            imgbytes = bas.toByteArray();
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return imgbytes;
   }

   private int[] loadPalette(byte[] bytes, int offset, int numberColors)
   {
      int[] palette = new int[numberColors];
      for (int i =0; i < numberColors; i++, offset+=4)
      {
         palette[i] = (((bytes[offset+3]&0xFF) << 24) | ((bytes[offset+2]&0xFF) << 16) | ((bytes[offset+1]&0xFF) << 8) | (bytes[offset+0]&0xFF));
         //Utils.println(i+" - "+totalcross.sys.Convert.unsigned2hex(palette[i],6));
      }
      return palette;
   }
}