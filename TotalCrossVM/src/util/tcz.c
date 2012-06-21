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
#include "tcz.h"

/**
    A tcz file has the following format:
    . version
    . attributes
    . length
    . size of compressed offsets
    . size of compressed names
    . compressed offsets array (length+1)
    . uncompressed sizes array (length)
    . compressed names array (length)
    . compressed data chunks

    The first record is the class that implements totalcross.MainClass.
*/

void destroyTCZ() // no threads are running at this point
{
   while (openTCZs != null)  // the tcz is removed from the list on tczClose
      tczClose((TCZFile)openTCZs->value);
}

static bool tczReadMore(TCZFile f)
{
   int32 n;
   FILE* fin = fopen(f->header->path, "rb");
   fseek(fin, f->expectedFilePos, SEEK_SET);
   n = fread(f->buf, 1, sizeof(f->buf), fin);
   f->expectedFilePos += n;
   fclose(fin);
   if (n <= 0)
      return false; // no more data
   f->zs.next_in = f->buf;
   f->zs.avail_in = n;
   return true;
}

int32 tczRead(TCZFile f, void* outBuf, int32 count)
{
   int32 err=0;
   z_stream *zs = &f->zs;
   zs->avail_out = count;
   zs->next_out = outBuf;
   if (count == 0)
      return 0;

   while (true)
   {
      if (zs->avail_in == 0 && !tczReadMore(f))
         break;
      err = inflate(&f->zs, Z_NO_FLUSH);
      if (err == Z_STREAM_END)
      {
         int32 ret = count - zs->avail_out;
         inflateEnd(&f->zs);
         return ret;
      }
      else
      if (err < 0 || zs->avail_out == 0)
         break;
   }
   if (zs->avail_out > 0)
   {
      if (f->tempHeap != null)
         HEAP_ERROR(f->tempHeap, HEAP_ZIP_ERROR);
      else
         alert("Error on zip (in a heapless tcz): %d. Remain %d bytes",(int)err, (int)zs->avail_out);
   }
   return count;
}

int32 tczRead32(TCZFile f)
{
   int32 i=0;
   tczRead(f, &i, 4);
   return i;
}

int32 tczRead32BE(TCZFile f)
{
   int32 i=0;
   tczRead(f, &i, 4);
   i = SWAP32_FORCED(i);
   return i;
}

int16 tczRead16(TCZFile f)
{
   int16 i=0;
   tczRead(f, &i, 2);
   return i;
}

int16 tczRead16BE(TCZFile f)
{
   int16 i=0;
   tczRead(f, &i, 2);
   i = SWAP16_FORCED(i);
   return i;
}

int8 tczRead8(TCZFile f)
{
   int8 i=0;
   tczRead(f, &i, 1);
   return i;
}

static TCZFile tczNewInstance(TCZFile parent)
{
   volatile Heap hheap = null;
   TCZFile ntcz;
   int32 err;
   ntcz = newX(TCZFile); // don't use the heap
   if (!ntcz)
      return null;
   if (parent) // not first instance?
   {
      ntcz->header = parent->header;
      IF_HEAP_ERROR(ntcz->header->hheap)
         goto error;
   }
   else
   {
      hheap = heapCreate();
      IF_HEAP_ERROR(hheap)
         goto error;
      ntcz->header = newXH(TCZFileHeader, hheap);
      ntcz->header->hheap = hheap;
      //heapSetFinalizer(hheap, tczFinalizer, ntcz->header);
   }
   //debug("tczNewInstance tcz %d from header %d - %d",(int32)ntcz, (int32)ntcz->header, ntcz->header->instanceCount);
   ntcz->zs.opaque = ntcz->header->hheap;
   err = inflateInit(&ntcz->zs);
   if (err != Z_OK)
      goto error;
   ntcz->header->instanceCount++;
   return ntcz;
error:
   if (hheap) heapDestroy(hheap);
   xfree(ntcz);
   return null;
}

void tczClose(TCZFile tcz)
{
   if (tcz)
   {
      //debug("closing tcz %d from header %d - %d",(int32)tcz, (int32)tcz->header, tcz->header->instanceCount);
      inflateEnd(&tcz->zs);
      // remove the tcz from the list. Note that the first tcz added is usually the last one deleted; the exception to this is when we get an error while loading the constant pool.
      openTCZs = VoidPsRemove(openTCZs, tcz, null);
      if (--tcz->header->instanceCount == 0) // if there are no more instances, destroy the heap
      {
         // destroy the heaps
         if (tcz->header->cp)
            heapDestroy(tcz->header->cp->heap);
         heapDestroy(tcz->header->hheap);
      }
      xfree(tcz);
   }
}

static int32 findNamePosition(TCZFile tcz, CharP name)
{
   CharPArray names = tcz->header->names;
   if (strEq(names[0], name)) // is it the first file (extends MainClass)?
      return 0;
   else
   {
      int32 inf = 1,sup = ARRAYLEN(names)-1,half,res;
      while (inf<=sup)
      {
         half = (inf+sup)>>1;
         res = xstrcmp(name, names[half]);
         if (res == 0)
            return half;
         if (res < 0)
            sup=half-1;
         else
            inf=half+1;
      }
      return -1;
   }
}

TCZFile tczFindName(TCZFile tcz, CharP name) // locates the name and also positions the stream at the place to start reading it
{
   TCZFile ntcz;
   int32 pos = findNamePosition(tcz, name);
   if (pos == -1)
      return null;
   ntcz = tczNewInstance(tcz);
   if (!ntcz)
      return null;

   ntcz->expectedFilePos = ntcz->header->offsets[pos];
   ntcz->uncompressedSize = tcz->header->uncompressedSizes[pos];
   return ntcz;
}

/** Reads a TCZ file and place the informations in the public members available in this "class". */
TCZFile tczOpen(FILE* fin, CharP fullpath, CharP fileName)
{
   int32 baseOffset,n,i;
   CharPArray names;
   int32 *offsets;
   TCZFile tcz;
   int16 version, attr;
   volatile Heap heap;
   // assuming that current fin position is 0
   version    = fread16(fin);
   attr       = fread16(fin);
   baseOffset = fread32(fin);
   fclose(fin);
   if (version == 0)
   {
      alert("Invalid TCZ version.\nFile probably corrupted");
      return null;
   }
   if (fileName != null && version != TCZ_VERSION) // guich@tc110_70: check for version mismatch. font files doesn't care if the tcz changed
   {
      alert("TCZ version mismatch for %s. Recompile and deploy your app with the new SDK.",fileName);
      return null;
   }
   // the instance can only be created after the initial part has been read
   tcz = tczNewInstance(null);
   if (!tcz)
      return null;

   tcz->expectedFilePos = 8;
   xstrcpy(tcz->header->path, fullpath);
   tcz->header->version = version;
   tcz->header->attr = attr;
   heap = tcz->tempHeap = tcz->header->hheap;
   IF_HEAP_ERROR(heap)
   {
      heapDestroy(heap);
      //alert("opentcz ERROR: %d\nat %s (%d)",heap->errorCode, heap->errorFile, heap->errorLine);
      tczClose(tcz);
      return null;
   }
   n = tczRead32(tcz);

   // allocate space for names and offsets
   names   = tcz->header->names   = newPtrArrayOf(CharP, n, heap);
   offsets = tcz->header->offsets = newPtrArrayOf(Int32,  n+1, heap);
   tcz->header->uncompressedSizes = newPtrArrayOf(Int32,  n, heap);
   // decompress the offsets
   tczRead(tcz, tcz->header->offsets, (n+1) * 4);
   tczRead(tcz, tcz->header->uncompressedSizes, n * 4);
   for (i = n; i >= 0; i--) // add the baseOffset
      *offsets++ += baseOffset;

   // decompress the names
   for (i = 0; i < n; i++, names++)
   {
      int32 len = (uint8)tczRead8(tcz);
      *names = (CharP)heapAlloc(heap, len+1);
      tczRead(tcz, *names, len);
   }
   tcz->tempHeap = null; // not necessary, but safe
   inflateEnd(&tcz->zs);
   return tcz; // do NOT close this tcz here! it will be closed later
}

extern void readConstantPool(Context currentContext, ConstantPool t, TCZFile tcz, Heap heap); // class.c

TCZFile tczLoad(Context currentContext, CharP tczName)
{
   FILE* f;
   volatile TCZFile t=null,t2=null;
   char fullpath[256];

#ifdef PALMOS
   CharP dot;
   if ((dot=xstrstr(tczName, ".tcz")) != null) // in Palm OS, we must cut off the extension
      *dot = 0;
#endif

   f = findFile(tczName,fullpath);
   if (f != null && (t = tczOpen(f, fullpath, tczName)) != null)
   {
      VoidPs* temp;
      // enqueue the file in the TCZ list
      //debug("enqueuing tcz %d from header %d - %d",(int32)t, (int32)t->header, t->header->instanceCount);
      temp = VoidPsAdd(openTCZs, t, null); // cannot use heap here!
      if (temp == null)
      {
         tczClose(t);
         return null;
      }
      openTCZs = temp;
      // load the constant pool from inside this file
      if ((t2=tczFindName(t, "ConstantPool")) != null) // classes, not just resources?
      {
         volatile Heap cpHeap = heapCreate();
         IF_HEAP_ERROR(cpHeap)
         {
            alert("Error when loading constant pool of file\n%s", tczName);
            heapDestroy(cpHeap);
            t->header->cp = null; // already destroyed
            tczClose(t);
            tczClose(t2);
            return null;
         }
         t->header->cp = newXH(ConstantPool, cpHeap);
         readConstantPool(currentContext, t->header->cp, t2, cpHeap);
         tczClose(t2);
      }
   }
   return t;
}

TCZFile tczGetFile(CharP filename, bool strict) // fdie@ strict mode prevents mainClass package name based resolving
{
   TCZFile found = null;
   VoidPs *list, *head;
   list = head = openTCZs;
   if (head != null)
   do
   {
      TCZFile tcz = (TCZFile)list->value;
      found = tczFindName(tcz, filename);
      list = list->next;
   } while (head != list && found == null);

   // if the resource has not been found and "strict" mode is disabled, try to prefix the resource with the mainClass package name
   if (found == null && !strict)
   {
      char fullpath[MAX_PATHNAME];
      char *lastdot;
      xstrcpy(fullpath, mainClassName);
      lastdot = xstrrchr(fullpath, '.');
      if (lastdot != null)
      {
         lastdot[1] = 0;
         replaceChar(fullpath, '.', '/'); // now contains the package name with '/' separator
         xstrcpy(lastdot + 1, filename); // append the resource name and try again in "strict" mode
         found = tczGetFile(fullpath, true);
      }
   }

   return found;
}
