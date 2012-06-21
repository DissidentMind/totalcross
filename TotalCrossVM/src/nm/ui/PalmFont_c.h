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



#include "PalmFont.h"

static char defaultFontName[16];

bool fontInit(Context currentContext)
{
   int32 *maxfs=null, *minfs=null, *normal = null;
   Object *defaultFontNameObj;
   TCClass c;
   c = loadClass(currentContext, "totalcross.ui.font.Font",false);
   if (c)
   {
      maxfs = getStaticFieldInt(c, "MAX_FONT_SIZE");
      minfs = getStaticFieldInt(c, "MIN_FONT_SIZE");
      normal= getStaticFieldInt(c, "NORMAL_SIZE");
      tabSizeField = getStaticFieldInt(c, "TAB_SIZE");
      defaultFontNameObj = getStaticFieldObject(c, "DEFAULT");
   }
   if (!maxfs || !minfs || !normal || !tabSizeField || !defaultFontNameObj)
      return false;
      
   JCharP2CharPBuf(String_charsStart(*defaultFontNameObj), String_charsLen(*defaultFontNameObj), defaultFontName);
   maxFontSize = *maxfs;
   minFontSize = *minfs;
   normalFontSize = *normal;
   fontsHeap = heapCreate();
   if (fontsHeap == null)
      return false;
   htUF = htNew(23,null);
   if (!htUF.items)
   {
      heapDestroy(fontsHeap);
      return false;
   }
   defaultFont = loadFontFile(defaultFontName);
   if (defaultFont == null)
   {
      #ifdef PALMOS
      alert("Font file is missing.\nPlease install TCFont.pdb (or TCFontOld.pdb)");
      #else
      alert("Font file is missing.\nPlease install TCFont.tcz (or TCFontOld.tcz)");
      #endif
      heapDestroy(fontsHeap);
      htFree(&htUF,null);
   }
   return defaultFont != null;
}

void fontDestroy()
{
   VoidPs *list, *head;
   list = head = openFonts;
   if (head != null)
      do
      {
         FontFile ff = (FontFile)list->value;
         tczClose(ff->tcz);
         list = list->next;
      } while (list != head);
   openFonts = null;
   heapDestroy(fontsHeap);
   htFree(&htUF,null);
}

static FontFile findFontFile(char* fontName)
{
   VoidPs *list, *head;
   list = head = openFonts;
   if (head != null)     
   {
      int32 len = xstrlen(fontName);
      do
      {
         FontFile ff = (FontFile)list->value;
         if (strCaseEqn(fontName, ff->name, len))
            return ff;
         list = list->next;
      } while (list != head);
   }
   return null;
}

FontFile loadFontFile(char *fontName)
{
   FontFile ff;
   TCZFile tcz;
   FILE* f;
   char fullpath[256];

   IF_HEAP_ERROR(fontsHeap)
   {
      heapDestroy(fontsHeap);
      return null;
   }

   // first, check if its already loaded
   ff = findFontFile(fontName);
   if (ff == null)
   {
      f = findFile(fontName,fullpath);
      if (f == null)
      {
         char fullName[150];
         xstrprintf(fullName,"%s.tcz",fontName); // append a tcz to the font name
         f = findFile(fullName,fullpath);
      }
      #ifndef WIN32 // win32 file system is not case sensitive
      if (f == null && 'a' <= fontName[0] && fontName[0] <= 'z')
      {
         fontName[0] = toUpper(fontName[0]); // the user may have created the font with uppercase, like Arial
         return loadFontFile(fontName);
      }
      #endif
      if (f != null)
      {
         tcz = tczOpen(f,fullpath,null);
         if (tcz != null)
         {
            ff = newXH(FontFile, fontsHeap);
            xstrncpy(ff->name,fontName,min32(xstrlen(fontName),31));
            CharPToLower(ff->name); // fonts are stored in lowercase inside the tcz file
            ff->tcz = tcz;
            openFonts = VoidPsAdd(openFonts, ff, fontsHeap);
         }
      }
   }
   return ff;
}

UserFont loadUserFont(FontFile ff, bool bold, int32 size, JChar c)
{
   char fullname[100];
   UserFont uf;
   uint32 bitmapTableSize, bitIndexTableSize,numberOfChars,uIndex,hash;
   int32 nlen,vsize,i;
   TCZFile uftcz;
   char faceType;

   IF_HEAP_ERROR(fontsHeap)
   {
      //heapDestroy(fontsHeap); - guich@tc114_63 - not a good idea; just return null
      return null;
   }
   nlen=0;
   vsize = (size == -1) ? normalFontSize : max32(size,minFontSize); // guich@tc122_15: don't check for the maximum font size here

   faceType = c < 0x3000 && bold ? 'b' : 'p';
   uIndex = ((int32)c >> 8) << 8;
   xstrprintf(fullname, "%s$%c%du%d", ff->name, faceType, vsize, uIndex);

   // verify if its in the cache.
   hash = hashCode(fullname);
   uf = htGetPtr(&htUF, hash);
   if (uf != null)
      return uf;

   // first, try to load it
   uftcz = tczFindName(ff->tcz, fullname);
   if (uftcz == null)
   {
      nlen = xstrlen(ff->name);
      // try now as a plain font
      xstrprintf(&fullname[nlen], "$p%du%d", vsize, uIndex);
      uftcz = tczFindName(ff->tcz, fullname);
   }
   if (uftcz == null && vsize != normalFontSize) // guich@tc122_15: ... check only here
   {
      i = vsize;
      while (uftcz == null && --i >= 5) // try to find the nearest size
      {
         xstrprintf(&fullname[nlen], "$%c%du%d", faceType, i, uIndex);
         uftcz = tczFindName(ff->tcz, fullname);
      }
   }
   if (uftcz == null)
   {
      // try now as the default size original face
      xstrprintf(&fullname[nlen], "$%c%du%d", faceType, normalFontSize, uIndex);
      uftcz = tczFindName(ff->tcz, fullname);
   }
   if (uftcz == null && faceType != 'p')
   {
      // try now as the default size plain font
      xstrprintf(&fullname[nlen], "$p%du%d", normalFontSize, uIndex);
      uftcz = tczFindName(ff->tcz, fullname);
   }
   // at last, use the default font - guich@tc123_11: fixed these font checks
   if (uftcz == null && defaultFont != null)
   {
      xstrprintf(fullname, "tcfont$%c%du%d", faceType, vsize, uIndex);
      uftcz = tczFindName(defaultFont->tcz, fullname);
   }
   if (uftcz == null) // check if there's a font of any size - maybe the file has only one font?
      for (i = minFontSize; i <= maxFontSize; i++)
      {
         xstrprintf(fullname, "%s$p%du%d", ff->name, i, uIndex);
         if ((uftcz = tczFindName(ff->tcz, fullname)) != null)
            break;
      }
   if (uftcz == null) // check if there's a font of any size - at least with the default font
      for (i = minFontSize; i <= maxFontSize; i++)
      {
         xstrprintf(fullname, "tcfont$p%du%d", i, uIndex);
         if ((uftcz = tczFindName(defaultFont->tcz, fullname)) != null)
            break;
      }
   
   if (uftcz == null) // probably the index was outside the available ranges at this font
      return c == ' ' ? null : loadUserFont(ff, bold, size, ' '); // guich@tc110_28: if space, just return null

   uf = newXH(UserFont, fontsHeap);
   uftcz->tempHeap = fontsHeap; // guich@tc114_63: use the fontsHeap
   tczRead(uftcz, &uf->fontP, 2*10);

   uf->rowWidthInBytes = ((uint32)uf->fontP.rowWords) << (uf->fontP.antialiased ? 3 : 1);
   numberOfChars = uf->fontP.lastChar - uf->fontP.firstChar + 1;
   bitmapTableSize = ((uint32)uf->rowWidthInBytes) * uf->fontP.maxHeight;
   bitIndexTableSize = (numberOfChars+1) * 2;
   uf->bitmapTable = newPtrArrayOf(UInt8, bitmapTableSize, fontsHeap);
   uf->bitIndexTable = newPtrArrayOf(UInt16, bitIndexTableSize>>1, fontsHeap);
   tczRead(uftcz, uf->bitmapTable, bitmapTableSize);
   tczRead(uftcz, uf->bitIndexTable, bitIndexTableSize);
   uf->bitIndexTable -= uf->fontP.firstChar; // instead of doing "bitIndexTable[ch-firstChar]", this trick will allow use "bitIndexTable[ch]

   tczClose(uftcz);
   htPutPtr(&htUF, hash, uf);
   return uf;
}

UserFont loadUserFontFromFontObj(Context currentContext, Object fontObj, JChar ch)
{
   if (fontObj == currentContext->lastFontObj && ch == ' ')
      return currentContext->lastUF;
   else
   {
      FontFile ff=null;
      int32 style = Font_style(fontObj);
      int32 size  = Font_size(fontObj);
      UserFont uf;
      xmoveptr(&ff, ARRAYOBJ_START(Font_hvUserFont(fontObj)));
      uf = loadUserFont(ff, (style & 1) == 1, size, ch);
      if (uf == null) // guich@tc123_11: use the last available font
         uf = currentContext->lastUF;
      if (uf != null && ch == ' ') // only cache ' ', usually used to get a pointer to the user font
      {
         currentContext->lastUF = uf;
         currentContext->lastFontObj = fontObj;
      }
      return uf;
   }
}

int32 getJCharWidth(Context currentContext, Object fontObj, JChar ch)
{
   UserFont uf = loadUserFontFromFontObj(currentContext, fontObj, ch);
   if (ch == 160) // guich@tc153: now the char 160 have the same width of a number
      ch = '0';
   if (ch < ' ') // guich@tc126_22: since enter can be inside the range of this font, we have to handle it before and make sure its width is 0.
      return (ch == '\t') ? uf->fontP.spaceWidth * *tabSizeField : 0; // guich@tc100: handle tabs
   if (uf == null || ch < uf->fontP.firstChar || ch > uf->fontP.lastChar) // invalid char - guich@tc122_23: must also check the font's range
      return ch == ' ' ? 0 : getJCharWidth(currentContext, fontObj, ' ');
   return (uf->fontP.firstChar <= ch && ch <= uf->fontP.lastChar) ? uf->bitIndexTable[ch+1] - uf->bitIndexTable[ch] : uf->fontP.spaceWidth;
}

int32 getJCharPWidth(Context currentContext, Object fontObj, JCharP s, int32 len)
{
   int sum = 0;
   while (len-- > 0)
      sum += getJCharWidth(currentContext, fontObj, *s++);
   return sum;
}
