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



package litebase;

import totalcross.sys.*;

/**
 * This class has useful constants, fields, and methods for the other Litebase classes.
 */
class Utils
{
   /**
    * The default row increment, that is, how many empty rows are inserted in a .db file at once.
    */
   static final int DEFAULT_ROW_INC = 10;


   // juliana@114_9: the absence of primary key can't be zero because the rowid may be a primary key or it can have an index.
   /**
    * Indicates if table has primary key.
    */
   static final int NO_PRIMARY_KEY = -1;

   // rnovais@567_2: added more sizes.
   /**
    * The sizes of each type in the record in the .db file.
    */
   static final int[] typeSizes = {4, 2, 4, 8, 4, 8, 4, -1, 4, 8, 4};

   /**
    * Indicates if the column has an index.
    */
   static final int ATTR_COLUMN_HAS_INDEX = 1;
   
   /**
    * Indicates if a row has an auxiliary rowid.
    */
   static final int ATTR_DEFAULT_AUX_ROWID = -1; // rnovais@570_61
   
   /**
    * Indicates if the column has a default value.
    */
   static final int ATTR_COLUMN_HAS_DEFAULT = 2;
   
   /**
    * Indicates if a column is defined as <code>NOT NULL</code>.
    */
   static final int ATTR_COLUMN_IS_NOT_NULL = 4;
   
   /**
    * Indicates if the column has an .idr file.
    */
   static final int ATTR_COLUMN_HAS_IDR = 8;
   
   /**
    * Indicates thar the column has an index with repetition.
    */
   static final int ATTR_COLUMN_HAS_IDX_IDR = Utils.ATTR_COLUMN_HAS_INDEX | Utils.ATTR_COLUMN_HAS_IDR;
   
   /**
    * Indicates that the column has no indice.
    */
   static final int ATTR_COLUMN_HAS_NO_INDICE = ~(Utils.ATTR_COLUMN_HAS_INDEX | Utils.ATTR_COLUMN_HAS_IDR); // juliana@227_6

   /**
    * When saving the meta data, indicates that only the deleted rows count was changed.
    */
   static final int TSMD_ONLY_DELETEDROWSCOUNT = 1;

   /**
    * When saving the meta data, indicates that only the primary key was changed.
    */
   static final int TSMD_ONLY_PRIMARYKEYCOL = 2;

   /**
    * When saving the meta data, indicates that everything must be saved.
    */
   static final int TSMD_EVERYTHING = 3;

   /**
    * When saving the meta data, indicates that only the auxiliary rowid was changed.
    */
   static final int TSMD_ONLY_AUXROWID = 4;

   // guich@300: added support for basic synchronization.
   /**
    * Indicates if a row is synced.
    */
   static final int ROW_ATTR_SYNCED = 0X00000000; // 0

   /**
    * Indicates if a row is new.
    */
   static final int ROW_ATTR_NEW = 0X40000000; // 1

   /**
    * Indicates if a row is updated.
    */
   static final int ROW_ATTR_UPDATED = 0x80000000; // 2
   
   /**
    * Indicates if a row is deleted.
    */
   static final int ROW_ATTR_DELETED = 0XC0000000; // 3

   /**
    * The rowid mask.
    */
   static final int ROW_ID_MASK = 0x3FFFFFFF;

   /**
    * The row attributes mask.
    */
   static final int ROW_ATTR_MASK = 0xC0000000;

   /**
    * 'AND' of different result sets.
    */
   static int WC_TYPE_AND_DIFF_RS = 0;

   /**
    * 'OR' of different result sets.
    */
   static int WC_TYPE_OR_DIFF_RS = 1;
   
   /**
    * Finds the next bit set from an b-tree.
    *
    * @param items The index bitmap.
    * @param start The first value to search.
    * @return The position of the next bit set.
    */
   static int findNextBitSet(int[] items, int start)
   {
      int index = start >> 5, // Converts from bits to int.
          n,
          b;
      
      start &= 31;
      while (true)
      {
         n = items.length - index;
         if (n > 0 && items[index] == 0)
         {
            start = 0;
            while (n > 0 && items[index] == 0) // Finds the next int with any bit set.
            {
               n--;
               index++;
            }
         }
         if (n > 0) // Found?
         {
            b = items[index];
            while (start < 32 && (b & (1 << start)) == 0)
               start++;
            if (start == 32)
            {
               start = 0;
               index++; // No more bits in this int? Yests the next ints.
               continue;
            }
            return start + (index << 5);
         }
         return -1;
      }
   }
   
   /**
    * Finds the previous bit set from an b-tree.
    *
    * @param items The index bitmap.
    * @param start The first value to search.
    * @return The position of the previous bit set.
    */
   static int findPrevBitSet(int[] items, int start)
   {
      int index = start >> 5, // Converts from bits to int.
          b;
      
      start &= 31;
      while (true)
      {
         if (index >= 0 && items[index] == 0)
         {
            start = 31;            
            while (index >= 0 && items[index] == 0) // Finds the next int with any bit set.
               index--;
         }
         if (index >= 0) // Found?
         {
            b = items[index];
            while (start >= 0 && (b & (1 << start)) == 0)
               start--;
            if (start < 0)
            {
               start = 31;
               index--; // No more bits in this int? Tests next ints.
               continue;
            }
            return start + (index << 5);
         }
         return -1;
      }
   }
   
   /**
    * Compares 2 arrays of values.
    *
    * @param v1 The first array of values.
    * @param v2 The second array of values.
    * @param types The types of the values being compared.
    * @return 0 if the arrays are identical; a positive number if <code>v1[]</code> is greater than <code>v2[]</code>; otherwise, a negative number.
    */
   static int arrayValueCompareTo(SQLValue[] v1, SQLValue[] v2, int[] types)
   {
      int size = v1.length, 
           r,
           i = -1;
      while (++i < size) // juliana@210a_12: corrected wrong comparison order.
         if ((r = v1[i].valueCompareTo(v2[i], types[i], false, false)) != 0)
            return r;
      return 0;
   }

   /**
    * Compares the two records, using the sort column list.
    * 
    * @param record1 The first record to be compared.
    * @param record2 The second record to be compared.
    * @param nullsRecord1 The null values of the first record.
    * @param nullsRecord2 The null values of the second record.
    * @param sortFieldList The order of evaluation of the record.
    * @return 0 if the arrays are identical in the comparison order; a positive number if <code>record1[]</code> is greater than 
    * <code>record2[]</code>; otherwise, a negative number.
    */ 
   static int compareRecords(SQLValue[] record1, SQLValue[] record2, byte[] nullsRecord1, byte[] nullsRecord2, SQLResultSetField[] sortFieldList)
   {
      
      int i = -1,
          n = sortFieldList.length,
          result,
          index;
      SQLResultSetField field;
      SQLValue value1,
               value2;
      
      while (++i < n) // Compares the records, using the sequence used by the sort column List.
      {
         field = sortFieldList[i];
         index = field.tableColIndex;
         value1 = record1[index];
         value2 = record2[index];
         
         // Compares the elements checking if they are null.
         result = value1.valueCompareTo(value2, field.dataType, (nullsRecord1[index >> 3] & (1 << (index & 7))) != 0, (nullsRecord2[index >> 3] & (1 << (index & 7))) != 0);
         if (!field.isAscending)
            result = -result;
         if (result != 0)
            return result;
      }
      return 0;
   }

   // rnovais@101_1: this methods verifies the datapath
   /**
    * Gets the full name of a file: path + file name.
    * 
    * @param fileName The file name.
    * @param dataPath The path where the table is stored.
    * @return path + file name if path is not empty, null or the root; otherwise, only the table name.
    */
   static String getFullFileName(String fileName, String sourcePath)
   {
      if (sourcePath.length() > 0 && !sourcePath.equals("./")) // guich@102a_4: better logic, and fix.
         return sourcePath.concat(fileName); 
      return fileName;
   }
   
   /**
    * Verifies if a string is a valid Time and transforms it into a correspondent int datetime. The time ranges from 00:00:00:000 to 23:59:59:9999 
    * (it accepts dots and colons). This method is very flexible. For instance: 2:-:8:10 is the same as 2:0:8:10 and returns 20008010; 
    * 02:.:8:1 is the same as 02:0.0:8 and returns 20000008; :4:8:19 is the same as 0:4:8:19 and returns; 408019 2.4.a.876 is the same as 2.4.0.876 
    * and returns 20400876.
    * 
    * @param strTime A string in a time format.
    * @returns A correspondent int datetime.
    * @throws SQLParseException If the value is not a valid time.
    */
   static int testAndPrepareTime(String strTime) throws SQLParseException // rnovais@567_2 
   {
      int n = strTime.length(), 
          j = 2, 
          hh, 
          mm, 
          ss, 
          ms,
          multiplier = 1;
      int p[] = new int[3];
      char c;
      
      if (n > 0 && n <= 13)
      {
         int value = 0;
         while (--n >= 0) // Going back to front and computing the value.
         {
            c = strTime.charAt(n);
            if ('0' <= c && c <= '9')
            {
               value += (c - '0') * multiplier;
               multiplier *= 10;
            }
            else
            {
               p[j--] = value;
               value = 0;
               multiplier = 1;
            }
         }
         hh = value; // There's at least one number.
         mm = ++j < 3? p[j] : 0;
         ss = ++j < 3? p[j] : 0;
         ms = ++j < 3? p[j] : 0;

         if (!(hh > 23 || mm > 59 || ss > 59 || ms > 999))
            return hh * 10000000 + mm * 100000 + ss * 1000 + ms;
      }
      throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_VALUE_ISNOT_DATETIME) + strTime);
   }

   /**
    * Formats an int "intTime" into a TIME hh:mm:ss:mmm and appends it to a <code>StringBuffer</code>.
    * 
    * @param sBuffer The string buffer parameter.
    * @param intTime An integer representing a time.
    * @return A string in the format hh:mm:ss:mmm.
    */
   static void formatTime(StringBuffer sBuffer, int intTime)
   {
      int mills = intTime % 1000;
      int second = (intTime /= 1000) % 100;
      int minute = (intTime /= 100) % 100;
      int hour = (intTime / 100) % 100;
      boolean useAmPm = !Settings.is24Hour;
      int h;

      if (useAmPm) // guich@566_40
         if (hour == 0 || hour == 12)
            h = 12;
         else
            h = hour < 12? hour : (hour - 12);
      else
         h = hour;

      sBuffer.append(h / 10).append(h % 10).append(Settings.timeSeparator).append(minute / 10).append(minute % 10).append(Settings.timeSeparator)
             .append(second / 10).append(second % 10).append(Settings.timeSeparator).append(mills / 100).append(mills / 10 % 10).append(mills % 10);
      
      if (useAmPm)
         sBuffer.append(' ').append(hour >= 12? 'P' : 'A').append('M');
   }
   
   /**
    * Formats an pecific integer date as a string into a <code>StringBuffer</code>.
    *
    * @param sBuffer The string buffer parameter.
    * @param intDate The date as an integer.
    */
   static void formatDate(StringBuffer sBuffer, int intDate)
   {
      sBuffer.append(intDate / 10000000).append(intDate / 1000000 % 10).append(intDate / 100000 % 10).append(intDate / 10000 % 10)
             .append('/').append(intDate / 1000 % 10).append(intDate / 100 % 10).append('/').append(intDate / 10 % 10).append(intDate % 10);
   }
   
   /**
    * Formats a date or a datetime as integers as a string using a <code>StringBuffer</code>. 
    *
    * @param sBuffer The string buffer parameter.
    * @param type The type of the value, DATE or DATETIME.
    * @param sqlValue The record which stores the DATE, DATETIME, and the resulting string.
    * @return The string representing the date or datetime.
    */
   static String formatDateDateTime(StringBuffer sBuffer, int type, SQLValue sqlValue)
   {
      if (sqlValue.asString == null)
      {
         if (type == SQLElement.DATE)
         {
            sBuffer.setLength(0);
            Utils.formatDate(sBuffer, sqlValue.asInt);
            return sqlValue.asString = sBuffer.toString();
         }
         if (type == SQLElement.DATETIME)
         {
            sBuffer.setLength(0);
            Utils.formatDate(sBuffer, sqlValue.asInt);
            sBuffer.append(' ');
            Utils.formatTime(sBuffer, sqlValue.asShort);
            return sqlValue.asString = sBuffer.toString();
         }
      }
      return sqlValue.asString;
   }

   /**
    * Verifies if the function can be applied to a data type field.
    * 
    * @param parameterDataType The data type of the function parameter.
    * @param sqlFunction The function code.
    * @return <code>true</code> If the function can be applied to a data type field; <code>false</code>, otherwise.
    */
   static boolean bindFunctionDataType(int parameterDataType, int sqlFunction) // rnovais@568_10
   {
      byte functions[] = SQLElement.function_x_datatype[parameterDataType];
      int j = functions.length;
      while(--j >= 0)
         if (functions[j] == sqlFunction)
            return true;
      return false;
   }

   /**
    * Sets and resets one bit in an array of bytes.
    * @param items The array of bytes. 
    * @param index The bit index to be set or reseted. 
    * @param on A boolean that defines whether the bit will be set or reseted.
    */
   static void setBit(byte[] items, int index, boolean on)
   {
      if (on)
         items[index >> 3] |= (1 << (index & 7)); // set
      else
         items[index >> 3] &= ~(1 << (index & 7)); // reset
   }
   
   // juliana@222_9: Some string conversions to numerical values could return spourious values if the string range were greater than the type range.
   /** 
    * Converts the a string into a float.
    * 
    * @param string The string to be converted.
    * @return the float represented by the string.
    * @throws InvalidNumberException If the string passed is not a valid float.
    */
   static float toFloat(String string) throws InvalidNumberException
   {
      try
      {
         return Float.valueOf(string).floatValue();
      }
      catch (NumberFormatException exception)
      {
         throw new InvalidNumberException("Error: " + string + " is not a valid float value.");
      }
   }
   
   /**
    * Calculates the hash code of a substring of a string.
    * 
    * @param string The string.
    * @param initialIdx The initial index of the string to calculate the hash code.
    * @return The hash code of the substring.
    */
   static int subStringHashCode(String string, int initialIdx)
   {
      int hashCode = 0,
          length = string.length() - initialIdx;
  
      while (--length >= 0)
         hashCode = (hashCode << 5) - hashCode + (int)string.charAt(initialIdx++);
      return hashCode;
   }
} 