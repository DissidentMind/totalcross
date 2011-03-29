/*********************************************************************************
 *  TotalCross Software Development Kit - Litebase                               *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/

// $Id: Node.c

/**
 * Defines functions to manipulate a B-Tree. It is used to store the table indices. It has some improvements for both memory usage, disk space, and 
 * speed, targeting the creation of indices, where the table's record is far greater than the index record.
 */

#include "Node.h"

/**
 * Creates a new node for an index.
 *
 * @param index The index of the node to be created.
 * @return The node created.
 */
Node* createNode(Index* index)
{
	TRACE("createNode")
   Heap heap = index->heap;
   Node* node = (Node*)TC_heapAlloc(heap, sizeof(Node));
   int32* types = index->types;
   int32* colSizes = index->colSizes;
   int32 i = (node->index = index)->btreeMaxNodes, 
         j,
         numberColumns = index->numberColumns;
   Key* keys = node->keys = (Key*)TC_heapAlloc(heap, sizeof(Key) * i);
   Key* key;

   node->idx = -1;
	node->children = (int16*)TC_heapAlloc(heap, (i + 1) << 1);

   while (--i >= 0)
   {
      key = &keys[i];
      key->index = index;
      key->keys = (SQLValue*)TC_heapAlloc(heap, numberColumns * sizeof(SQLValue));
      j = numberColumns;
      while (--j >= 0)
         if (types[j] == CHARS_TYPE || types[j] == CHARS_NOCASE_TYPE)
            key->keys[j].asChars = (JCharP)TC_heapAlloc(heap, (colSizes[j] << 1) + 2);
   }
   return node;
}

/**
 * Loads a node.
 * 
 * @param context The thread context where the function is being executed.
 * @param node A pointer to the node being loaded.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 */
bool nodeLoad(Context context, Node* node)
{
	TRACE("nodeLoad")
   Index* index = node->index;
   PlainDB* plainDB = index->table->db;
   XFile* fnodes = &index->fnodes;
   uint8* dataStream = index->basbuf;
   uint8* dsAux = index->table->db->basbuf;
   int16* children = node->children;
   int32 i = index->nodeRecSize,
         n = 0;
	
   plainDB->basbuf = index->basbufAux;

   // Reads all the record at once.
   nfSetPos(fnodes, node->idx * i);
   if (nfReadBytes(context, fnodes, dataStream, i) != i) 
      return false;

   // Loads the keys.
   xmove2(&n, dataStream);
   dataStream += 2;
   i = -1;
   while (++ i < n)
      dataStream = keyLoad(&node->keys[i], dataStream);

	xmemmove(children, dataStream, ((node->size = n) + 1) << 1); // Loads the node children.

	// juliana@202_3: Solved a bug that could cause a GPF when using composed indices.
	xmemset(&children[n + 1], 0xFF, (index->btreeMaxNodes - n) << 1); // Fills the non-used indexes with TERMINAL.
 
   node->isDirty = false;
   plainDB->basbuf = dsAux;
   return true;
}

/**
 * Saves a dirty key.
 *
 * @param context The thread context where the function is being executed.
 * @param node The node being saved.
 * @param currPos The current position in the file where the key should be saved.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 */
bool nodeSaveDirtyKey(Context context, Node* node, int32 currPos)
{
	TRACE("nodeSaveDirtyKey")
   Index* index = node->index;
   XFile* fnodes = &index->fnodes;
   // Positions the file pointer at the insert position.
   nfSetPos(fnodes, node->idx * index->nodeRecSize + 2 + index->keyRecSize * currPos + (index->keyRecSize - VALREC_SIZE)); 
   
   keySaveValRec(node->keys[currPos], index->basbuf);
   return nfWriteBytes(context, fnodes, index->basbuf, 4) == 4;
}

/**
 * Saves a node.
 *
 * @param context The thread context where the function is being executed.
 * @param node The node being saved.
 * @param isNew Indicates if it is a new node, not saved yet.
 * @param left The left child.
 * @param right The right child.
 * @return The position of this node.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If the index gets too large.
 */
int32 nodeSave(Context context, Node* node, bool isNew, int32 left, int32 right)
{
	TRACE("nodeSave")
   Index* index = node->index;
   XFile* fnodes = &index->fnodes;
   Key* keys = node->keys;
   uint8* dataStream = index->basbuf;
   int32 i = right - left,
         idx = node->idx,
         nodeRecSize = index->nodeRecSize;

   if (isNew)
   {
      if ((idx = index->nodeCount++) >= MAX_IDX) // The index got too large!
		{
			TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_INDEX_LARGE));
			return -1;
		}

      if (index->root->isWriteDelayed) // Grows more than 1 record per time.
      {
         if ((idx & (RECGROWSIZE - 1)) == 0 && !nfGrowTo(context, fnodes, (idx + RECGROWSIZE) * nodeRecSize))
            return -1;
      }
		else if (!nfGrowTo(context, fnodes, (idx + 1) * nodeRecSize)) // Opens space for the node.
         return -1;
   }

   nfSetPos(fnodes, idx * nodeRecSize); // Rewinds to insert position.
   xmove2(dataStream, &i);
   dataStream += 2;

   // Saves the keys.
   i = left - 1;
   while (++i < right)
      dataStream = keySave(&keys[i], dataStream);

   // juliana@225_2: corrected a possible index corruption when updating each node children.
   // Saves the children;
   xmemmove(dataStream, &node->children[left], i = ((right - left + 1) << 1));
   dataStream += i;

   xmemzero(dataStream, nodeRecSize - (dataStream - index->basbuf)); // Fills the rest with zeros.
   if (nfWriteBytes(context, fnodes, index->basbuf, nodeRecSize) != nodeRecSize)
      return -1;
   
   if (!isNew) // If the record and not a copy of it is being saved, then mark as saved
      node->isDirty = false;
   return idx;
}

/**
 * Constructs a B-Tree node with at most k keys, initially with one element, item, and two children: left and right.
 *
 * @param node The node being saved.
 * @param key The key to be saved.
 * @param left The left child.
 * @param right The right child.
 */
void nodeSet(Node* node, Key* key, int32 left, int32 right)
{
	TRACE("nodeSet")
   node->size = 1;
   keySetFromKey(node->keys, key);
   node->children[0] = left;
   node->children[1] = right;
}

/**
 * Returns the index of the leftmost element of this node that is not less than item, using a binary search.
 *
 * @param context The thread context where the function is being executed.
 * @param node The node being searched.
 * @param key The key to be found.
 * @param isInsert Indicates that the function is called by <code>indexInsert()</code>
 * @return The position of the key.
 */
int32 nodeFindIn(Context context, Node* node, Key* key, bool isInsert) // juliana@201_3
{
	TRACE("nodeFindIn")
   Index* index = node->index;
   PlainDB* plainDB = index->table->db;
   XFile* dbo = &plainDB->dbo;
   Key* keys = node->keys;
   Key* keyAux;
   SQLValue* sqlValues;
   SQLValue* sqlValue;
   int32* types = index->types;
   bool isAscii = plainDB->isAscii;
   CharP buffer,
         from, 
         to;
	int32 right = node->size - 1, 
         i,
         j,
         middle, 
         comp,
         length = 0,
         numberColumns = index->numberColumns,

   // juliana@201_3: If the insertion is ordered, the position being seached is the last.
         left = (isInsert && index->isOrdered && right > 0)? right : 0; 

   while (left <= right)
   {
      sqlValues = (keyAux = &keys[middle = (left + right) >> 1])->keys;
      i = numberColumns;

      while (--i >= 0) // A string may not be loaded.
      {
         sqlValue = &sqlValues[i];
			if (!sqlValue->length && (types[i] == CHARS_TYPE || types[i] == CHARS_NOCASE_TYPE))
			{
            nfSetPos(dbo, sqlValue->asInt); // Gets and sets the string position in the .dbo.
				if (!nfReadBytes(context, dbo, (uint8*)&length, 2)) // Reads the string length.
               return false;
            sqlValue->length = length;
				if (isAscii) // juliana@210_2: now Litebase supports tables with ascii strings.
				{
					if (nfReadBytes(context, dbo, (uint8*)(buffer = (CharP)sqlValue->asChars), length) != length) // Reads the string.
						return false;
					from = buffer + (j = length - 1);
					to = from + j;
					while (--j >= 0)
				   {
				      *to = *from;
				      *from-- = 0;
					   to -= 2;
				   }
				}
				else if (nfReadBytes(context, dbo, (uint8*)sqlValue->asChars, length << 1) != (length << 1)) // Reads the string.
               return false;
            sqlValue->asChars[length] = 0; // juliana@202_8
			}
      }

      if (!(comp = keyCompareTo(key, keyAux, numberColumns)))
         return middle;
      else if (comp < 0)
         right = middle - 1;
      else
         left = middle + 1;
   }
   return left;
}

/**
 * Inserts element item, with left and right children at the right position in this node.
 *
 * @param context The thread context where the function is being executed.
 * @param node The node where a key will be inserted.
 * @param key The key to be saved.
 * @param leftChild The left child of the node.
 * @param rightChild The right child of the node.
 * @param insPos The position where to insert the key.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 */
bool nodeInsert(Context context, Node* node, Key* key, int32 leftChild, int32 rightChild, int32 insPos)
{
	TRACE("nodeInsert")
   Key* keys = node->keys;
   int16* children = node->children;
   int32 i = node->size - insPos;
   if (i > 0)
   {
      xmemmove(&children[insPos + 2], &children[insPos + 1], i << 1); 
      while (--i >= 0)
         keySetFromKey(&keys[insPos + i + 1], &keys[insPos + i]);
   }

   keySetFromKey(&keys[insPos], key);
   children[insPos] = leftChild;
   children[insPos + 1] = rightChild;
   node->size++;
   if (node->isWriteDelayed) // Only saves the key if it is not to be saved later.
      node->isDirty = true;
   else
      return nodeSave(context, node, false, 0, node->size) >= 0;
   return true;
}

/**
 * Sets the flag that indicates if the not should have its write process delayed or not.
 *
 * @param context The thread context where the function is being executed.
 * @param node The node whose flag will be updated.
 * @param delayed The new value of the flag.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 */
bool nodeSetWriteDelayed(Context context, Node* node, bool delayed)
{
   TRACE(delayed ? "nodeSetWriteDelayed on" : "nodeSetWriteDelayed off")
   if (node)
   {
      if (node->isWriteDelayed && node->isDirty && nodeSave(context, node, false, 0, node->size) < 0) // Before changing the flag, flushs the node.
		   return false;
	   node->isWriteDelayed = delayed;
   }
   return true;
}
