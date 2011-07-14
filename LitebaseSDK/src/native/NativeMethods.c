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

/**
 * Defines Litebase native methods. 
 */

#include "NativeMethods.h"

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
/**
 * Moves to the next record and fills the data members.
 *
 * @param p->obj[0] The row iterator. 
 * @param p->retI Receives <code>true</code> if it is possible to iterate to the next record. Otherwise, it will return <code>false</code>.
 * @throws IllegalStateException If the row iterator or driver are closed.
 */
LB_API void lRI_next(NMParams p) // litebase/RowIterator public native boolean next() throws IllegalStateException;
{
	TRACE("lRI_next")
   Object rowIterator = p->obj[0];
	Table* table = (Table*)OBJ_RowIteratorTable(rowIterator);
   
   MEMORY_TEST_START

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   // juliana@225_14: RowIterator must throw an exception if its driver is closed.
   if (OBJ_LitebaseDontFinalize(OBJ_RowIteratorDriver(rowIterator))) // The driver is closed.
   {
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      goto finish;
   }

   if (table) 
   {
      int32 rowNumber = OBJ_RowIteratorRowNumber(rowIterator),
         id;
      PlainDB* plainDB = table->db; 
      uint8* basbuf = plainDB->basbuf;

	   if (++rowNumber < plainDB->rowCount && plainRead(p->currentContext, plainDB, rowNumber))
      {
         xmove4(&id, basbuf);
         xmemmove((uint8*)ARRAYOBJ_START(OBJ_RowIteratorData(rowIterator)), basbuf, plainDB->rowSize);
         OBJ_RowIteratorRowid(rowIterator) = id & ROW_ID_MASK;
         OBJ_RowIteratorAttr(rowIterator) = ((id & ROW_ATTR_MASK) >> ROW_ATTR_SHIFT) & 3; // Masks out the attributes.
         p->retI = true;
      }
      else
         p->retI = false;
      OBJ_RowIteratorRowNumber(rowIterator) = rowNumber;

      // juliana@223_5: now possible null values are treated in RowIterator.
      xmemmove(table->columnNulls[0], basbuf + table->columnOffsets[table->columnCount], NUMBEROFBYTES(table->columnCount));
   }
   else // The row iterator is closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_ROWITERATOR_CLOSED));

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
/**
 * Moves to the next record with an attribute different of SYNCED.
 *
 * @param p->obj[0] The row iterator. 
 * @param p->retI Receives <code>true</code> if it is possible to iterate to a next record not synced. Otherwise, it will return <code>false</code>.
 * @throws IllegalStateException If the row iterator or driver are closed.
 */
LB_API void lRI_nextNotSynced(NMParams p) // litebase/RowIterator public native boolean nextNotSynced() throws IllegalStateException;
{
	TRACE("lRI_nextNotSynced")
   Object rowIterator = p->obj[0];
   Table* table = (Table*)OBJ_RowIteratorTable(rowIterator);
   Context context = p->currentContext;
    
   MEMORY_TEST_START

   // juliana@225_14: RowIterator must throw an exception if its driver is closed.
   if (OBJ_LitebaseDontFinalize(OBJ_RowIteratorDriver(rowIterator))) // The driver is closed.
   {
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      goto finish;
   }

   if (table) 
   {
      PlainDB* plainDB = table->db;
      uint8* basbuf = plainDB->basbuf;
      int32 rowNumber = OBJ_RowIteratorRowNumber(rowIterator),
            rowSize = plainDB->rowSize,
            id; 
      bool ret = false;

	   while (++rowNumber < plainDB->rowCount && plainRead(context, plainDB, rowNumber))
      {
         xmove4(&id, basbuf);
         if ((id & ROW_ATTR_MASK) == ROW_ATTR_SYNCED)
            continue;
         xmemmove((uint8*)ARRAYOBJ_START(OBJ_RowIteratorData(rowIterator)), basbuf, rowSize);
         OBJ_RowIteratorRowid(rowIterator) = id & ROW_ID_MASK;
         OBJ_RowIteratorAttr(rowIterator) = ((id & ROW_ATTR_MASK) >> ROW_ATTR_SHIFT) & 3; // Masks out the attributes.
         ret = true;
         break;
      }
      OBJ_RowIteratorRowNumber(rowIterator) = rowNumber;
      
      // juliana@223_5: now possible null values are treated in RowIterator.
      xmemmove(table->columnNulls[0], basbuf + table->columnOffsets[table->columnCount], NUMBEROFBYTES(table->columnCount));
      
      p->retI = ret;
   }
   else // The row iterator is closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_ROWITERATOR_CLOSED));
     
finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
/**
 * If the attribute is currently NEW or UPDATED, this method sets them to SYNCED. Note that if the row is DELETED, the change will be ignored.
 *
 * @param p->obj[0] The row iterator. 
 * @throws IllegalStateException If the row iterator or driver are closed.
 */
LB_API void lRI_setSynced(NMParams p) // litebase/RowIterator public native void setSynced() throws IllegalStateException;
{
	TRACE("lRI_setSynced")
   Object rowIterator = p->obj[0];
   Table* table = (Table*)OBJ_RowIteratorTable(rowIterator);
   
   MEMORY_TEST_START

   // juliana@225_14: RowIterator must throw an exception if its driver is closed.
   if (OBJ_LitebaseDontFinalize(OBJ_RowIteratorDriver(rowIterator))) // The driver is closed.
   {
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      goto finish;
   }

   if (table) // The row iterator is closed.
   {
      PlainDB* plainDB = table->db; 
      uint8* basbuf = plainDB->basbuf;
      int32 rowNumber = OBJ_RowIteratorRowNumber(rowIterator),
            id,
            oldAttr,
            newAttr;

      // The record is assumed to have been already read.
      xmove4(&id, basbuf);
      
      // guich@560_19 // juliana@230_16: solved a bug with row iterator.
      if ((newAttr = OBJ_RowIteratorAttr(rowIterator) = ROW_ATTR_SYNCED_MASK) != (oldAttr = ((id & ROW_ATTR_MASK) >> ROW_ATTR_SHIFT) & 3) 
       && oldAttr != (int32)ROW_ATTR_DELETED_MASK)
      {
         id = (id & ROW_ID_MASK) | newAttr; // Sets the new attribute.
         xmove4(basbuf, &id); 
		   plainRewrite(p->currentContext, plainDB, rowNumber);
      }
   }
   else
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_ROWITERATOR_CLOSED));

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
/**
 * Closes this iterator.
 *
 * @param p->obj[0] The row iterator.
 * @throws IllegalStateException If the row iterator or driver are closed.
 */
LB_API void lRI_close(NMParams p) // litebase/RowIterator public native void close() throws IllegalStateException;
{
	TRACE("lRI_close")
   Object rowIterator = p->obj[0];
   Table* table = (Table*)OBJ_RowIteratorTable(rowIterator);

   MEMORY_TEST_START

   // juliana@225_14: RowIterator must throw an exception if its driver is closed.
   if (OBJ_LitebaseDontFinalize(OBJ_RowIteratorDriver(rowIterator))) // The driver is closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
   else if (table) 
   {
      // juliana@227_22: RowIterator.close() now flushes the setSynced() calls.
      XFile* dbFile = &table->db->db;
      if (dbFile->cacheIsDirty)
         flushCache(p->currentContext, dbFile);

      OBJ_RowIteratorTable(rowIterator) = null;
	   OBJ_RowIteratorData(rowIterator) = null;
   }
   else // The row iterator is closed.
     TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_ROWITERATOR_CLOSED));

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Resets the counter to zero so it is possible to restart to fetch records.
 *
 * @param p->obj[0] The row iterator.
 */
LB_API void lRI_reset(NMParams p) // litebase/RowIterator public native void reset();
{
	TRACE("lRI_reset")
   MEMORY_TEST_START
   OBJ_RowIteratorRowNumber(p->obj[0]) = -1;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a short contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The short column index, starting from 1.
 * @param p->retI Receives the value of the column or 0 if the column is <code>null</code>.
 */
LB_API void lRI_getShort_i(NMParams p) // litebase/RowIterator public native short getShort(int column);
{
	TRACE("lRI_getShort_i")
   MEMORY_TEST_START
   getByIndex(p, SHORT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns an integer contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The integer column index, starting from 1.
 * @param p->retI Receives the value of the column or 0 if the column is <code>null</code>.
 */
LB_API void lRI_getInt_i(NMParams p) // litebase/RowIterator public native int getInt(int column);
{
	TRACE("lRI_getInt_i")
   MEMORY_TEST_START
   getByIndex(p, INT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a long integer contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The long integer column index, starting from 1.
 * @param p->retL Receives the value of the column or 0 if the column is <code>null</code>.
 */
LB_API void lRI_getLong_i(NMParams p) // litebase/RowIterator public native long getLong(int column);
{
	TRACE("lRI_getLong_i")
   MEMORY_TEST_START
   getByIndex(p, LONG_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a floating point number contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The floating point number column index, starting from 1.
 * @param p->retD Receives the value of the column or 0 if the column is <code>null</code>.
 */
LB_API void lRI_getFloat_i(NMParams p) // litebase/RowIterator public native double getFloat(int column);
{
	TRACE("lRI_getFloat_i")
   MEMORY_TEST_START
   getByIndex(p, FLOAT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a double precision floating point number contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The double precision floating point number column index, starting from 1.
 * @param p->retD Receives the value of the column or 0 if the column is <code>null</code>.
 */
LB_API void lRI_getDouble_i(NMParams p) // litebase/RowIterator public native double getDouble(int column);
{
	TRACE("lRI_getDouble_i")
   MEMORY_TEST_START
   getByIndex(p, DOUBLE_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a string contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The string column index, starting from 1.
 * @param p->retO Receives the value of the column or <code>null</code> if the column is <code>null</code>.
 */
LB_API void lRI_getString_i(NMParams p) // litebase/RowIterator public native String getString(int column);
{
	TRACE("lRI_getString_i")
   MEMORY_TEST_START
   getByIndex(p, CHARS_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a blob contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The blob column index, starting from 1.
 * @param p->retO Receives the value of the column or <code>null</code> if the column is <code>null</code>.
 */
LB_API void lRI_getBlob_i(NMParams p) // litebase/RowIterator public native byte[] getBlob(int column);
{
	TRACE("lRI_getBlob_i")
   MEMORY_TEST_START
   getByIndex(p, BLOB_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a date contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The date column index, starting from 1.
 * @param p->retO Receives the value of the column or <code>null</code> if the column is <code>null</code>.
 */
LB_API void lRI_getDate_i(NMParams p) // litebase/RowIterator public native totalcross.util.Date getDate(int column);
{
	TRACE("lRI_getDate_i")
   MEMORY_TEST_START
   getByIndex(p, DATE_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_5: now possible null values are treated in RowIterator.
/**
 * Returns a datetime contained in the current row.
 *
 * @param p->obj[0] The row iterator.
 * @param p->i32[0] The datetime column index, starting from 1.
 * @param p->retO Receives the value of the column or <code>null</code> if the column is <code>null</code>.
 */
LB_API void lRI_getDateTime_i(NMParams p) // litebase/RowIterator public native totalcross.sys.Time getDateTime(int column);
{
	TRACE("lRI_getDateTime_i")
   MEMORY_TEST_START
   getByIndex(p, DATETIME_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// juliana@230_28: if a public method receives an invalid argument, now an IllegalArgumentException will be thrown instead of a DriverException.
// juliana@223_5: now possible null values are treated in RowIterator.
// litebase/RowIterator public native boolean isNull(int column) IllegalStateException, IllegalArgumentException;
/**
 * Indicates if this column has a <code>NULL</code>.
 *
 * @param p->i32[0] The column index, starting from 1.
 * @param p->retI Receives <code>true</code> if the value is SQL <code>NULL</code>; <code>false</code>, otherwise.
 * @throws IllegalStateException If the row iterator or the driver is closed.
 * @throws IllegalArgumentException If the column index is invalid.
 */
LB_API void lRI_isNull_i(NMParams p) 
{
   TRACE("lRI_isNull_i")
   Object rowIterator = p->obj[0];
   Table* table = (Table*)OBJ_RowIteratorTable(rowIterator);
   int32 column = p->i32[0];
   
   MEMORY_TEST_START
	
   // juliana@225_14: RowIterator must throw an exception if its driver is closed.	
   if (!table) // The row iterator is closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_ROWITERATOR_CLOSED));
	else if (OBJ_LitebaseDontFinalize(OBJ_RowIteratorDriver(rowIterator))) // The driver is closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
   else if (column < 0 || column >= table->columnCount) // Checks if the column index is within range.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalArgumentException", getMessage(ERR_INVALID_COLUMN_NUMBER), column);
   else
      p->retI = isBitSet(table->columnNulls[0], column); // juliana@223_5: now possible null values are treated in RowIterator.
   
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Creates a Litebase connection for the default creator id, storing the database as a flat file. This method avoids the creation of more than one 
 * instance with the same creator id, which would lead to performance and memory problems. Using this method, the strings are stored in the 
 * unicode format. 
 *
 * @param p->retO Receives a Litebase instance.
 */
LB_API void lLC_privateGetInstance(NMParams p) // litebase/LitebaseConnection public static native litebase.LitebaseConnection privateGetInstance();
{
	TRACE("lLC_privateGetInstance")
	MEMORY_TEST_START
   TC_setObjectLock(p->retO = create(p->currentContext, TC_getApplicationId(), null), UNLOCKED);
	MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public static native litebase.LitebaseConnection privateGetInstance(String appCrid) throws DriverException, 
//                                                                                                                        NullPointerException;
/**
 * Creates a Litebase connection for the given creator id, storing the database as a flat file. This method avoids the creation of more than one 
 * instance with the same creator id, which would lead to performance and memory problems. Using this method, the strings are stored in the 
 * unicode format.
 *
 * @param p->obj[0] The creator id, which may (or not) be the same one of the current application and MUST be 4 characters long.
 * @param p->retO Receives a Litebase instance.
 * @throws DriverException If an application id with more or less than four characters is specified.
 * @throws NullPointerException If <code>appCrid == null</code>.
 */
LB_API void lLC_privateGetInstance_s(NMParams p) 
{
	TRACE("lLC_privateGetInstance_s")
	char strAppId[5];
   Object appCrid = p->obj[0];

   MEMORY_TEST_START
	
   if (!appCrid) // The application can't be null.
		TC_throwNullArgumentException(p->currentContext, "appCrid");
   else if (String_charsLen(appCrid) != 4) // The application id must have 4 characters.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException",  getMessage(ERR_DRIVER_CLOSED));
   else
   {
      TC_JCharP2CharPBuf(String_charsStart(appCrid), 4, strAppId);
	   TC_setObjectLock(p->retO = create(p->currentContext, getAppCridInt(strAppId), null), UNLOCKED);
   }

	MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public static native litebase.LitebaseConnection privateGetInstance(String appCrid, String params) 
//                                                                                                 throws DriverException, NullPointerException;
/**
 * Creates a connection with Litebase.
 *
 * @param p->obj[0] The creator id, which may be the same one of the current application.
 * @param p->obj[1] Only the folder where it is desired to store the tables, <code>null</code>, if it is desired to use the current data path, or 
 * <code>chars_type = chars_format; path = source_path</code>, where <code>chars_format</code> can be <code>ascii</code> or <code>unicode</code>, 
 * and <code>source_path</code> is the folder where the tables will be stored. The params can be entered in any order. If only the path is passed as
 * a parameter, unicode is used. Notice that path must be absolute, not relative. 
 * <p>If it is desired to store the database in the memory card (on Palm OS devices only), use the desired volume in the path given to the method.
 * <p>Most PDAs will only have one card, but others, like Tungsten T5, can have more then one. So it is necessary to specify the desired card slot.
 * <p>Note that databases belonging to multiple applications can be stored in the same path, since all tables are prefixed by the application's 
 * creator id.
 * <p>Also notice that to store Litebase files on card on Pocket PC, just set the second parameter to the correct directory path.
 * <p>It is not recommended to create the databases directly on the PDA. Memory cards are FIVE TIMES SLOWER than the main memory, so it will take 
 * a long time to create the tables. Even if the NVFS volume is used, it can be very slow. It is better to create the tables on the desktop, and copy 
 * everything to the memory card or to the NVFS volume.
 * <p>Due to the slowness of a memory card and the NVFS volume, all queries will be stored in the main memory; only tables and indexes will be stored 
 * on the card or on the NVFS volume.
 * <p> An exception will be raised if tables created with an ascii kind of connection are oppened with an unicode connection and vice-versa. 
 * @param p->retO Receives a Litebase instance.
 * @throws DriverException If an application id with more or less than four characters is specified.
 * @throws NullPointerException If <code>appCrid == null</code>.
 */
LB_API void lLC_privateGetInstance_ss(NMParams p) 
{
	TRACE("lLC_privateGetInstance_ss")
	char strAppId[5];
   Object appCrid = p->obj[0],
          params = p->obj[1];

   MEMORY_TEST_START
	
   if (!appCrid) // The application can't be null.
      TC_throwNullArgumentException(p->currentContext, "appCrid"); 
	else if (String_charsLen(appCrid) != 4) // The application id must have 4 characters.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      TC_JCharP2CharPBuf(String_charsStart(appCrid), 4, strAppId);
	   TC_setObjectLock(p->retO = create(p->currentContext, getAppCridInt(strAppId), params), UNLOCKED);
   }

	MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Returns the path where the tables created/opened by this connection are stored.
 *
 * @param p->obj[0] The connection with Litebase.
 * @param p->retO Receives a string representing the path.
 * @throws DriverException If the driver is closed.
 */
LB_API void lLC_getSourcePath(NMParams p) // litebase/LitebaseConnection public native String getSourcePath() throws DriverException;
{
	TRACE("lLC_getSourcePath")
   Object driver = p->obj[0];

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
	   TC_setObjectLock(p->retO = TC_createStringObjectFromCharP(p->currentContext, (CharP)OBJ_LitebaseSourcePath(driver), -1), UNLOCKED);

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native void execute(String sql) throws DriverException, NullPointerException;
/**
 * Used to execute a <code>create table</code> or <code>create index</code> SQL commands.
 * 
 * <p>Examples:
 * <ul>
 *     <li><code>driver.execute("create table PERSON (NAME CHAR(30), SALARY DOUBLE, AGE INT, EMAIL CHAR(50))");</code>
 *     <li><code>driver.execute("CREATE INDEX IDX_NAME ON PERSON(NAME)");</code>
 * </ul>
 * 
 * <p>When creating an index, its name is ignored but must be given. The index can be created after data was added to the table.
 *
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The SQL creation command.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If the sql command is null.
 */
LB_API void lLC_execute_s(NMParams p) 
{
	TRACE("lLC_execute_s")
	Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

	if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object sqlString = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];

      if (logger)
		{
			LOCKVAR(log);
			TC_executeMethod(context, loggerLog, logger, 16, sqlString, false);
			UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
		}
      
      if (sqlString)
         litebaseExecute(context, driver, String_charsStart(sqlString), String_charsLen(sqlString));
      else 
		   TC_throwNullArgumentException(context, "sql"); // The string can't be null.
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native int executeUpdate(String sql) throws DriverException, NullPointerException;
/**
 * Used to execute updates in a table (insert, delete, update, alter table, drop). E.g.:
 *
 * <p><code>driver.executeUpdate(&quot;drop table person&quot;);</code> will drop also the indices.
 * <p><code>driver.executeUpdate(&quot;drop index * on person&quot;);</code> will drop all indices but not the primary key index.
 * <p><code>driver.executeUpdate(&quot;drop index name on person&quot;);</code> will drop the index for the &quot;name&quot; column.
 * <p><code> driver.executeUpdate(&quot;ALTER TABLE person DROP primary key&quot;);</code> will drop the primary key.
 * <p><code>driver.executeUpdate(&quot;update person set age=44, salary=3200.5 where name = 'guilherme campos hazan'&quot;);</code> will update the
 * table.
 * <p><code>driver.executeUpdate(&quot;delete person where name like 'g%'&quot;);</code> will delete records of the table.
 * <p><code> driver.executeUpdate(&quot;insert into person (age, salary, name, email)
 * values (32, 2000, 'guilherme campos hazan', 'guich@superwaba.com.br')&quot;);</code> will insert a record in the table.
 *
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The SQL update command.
 * @param p->retI Receives the number of rows affected or <code>0</code> if a drop or alter operation was successful.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If the sql command is null.
 */
LB_API void lLC_executeUpdate_s(NMParams p) 
{
	TRACE("lLC_executeUpdate_s")
   Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object sqlString = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];

		if (logger)
		{
			LOCKVAR(log);
			TC_executeMethod(context, loggerLog, logger, 16, sqlString, false);
			UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
		}
      if (sqlString)
         p->retI = litebaseExecuteUpdate(context, driver, String_charsStart(sqlString), String_charsLen(sqlString));
      else 
         TC_throwNullArgumentException(context, "sql"); // The string can't be null. 
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native litebase.ResultSet executeQuery(String sql) throws DriverException, NullPointerException;
/**
 * Used to execute queries in a table. Example:
 * 
 * <pre>
 * ResultSet rs = driver.executeQuery(&quot;select rowid, name, salary, age from person where age != 44&quot;);
 * rs.afterLast();
 * while (rs.prev())
 *    Vm.debug(rs.getString(1) + &quot;. &quot; + rs.getString(2) + &quot; - &quot; + rs.getInt(&quot;age&quot;) + &quot; years&quot;);
 * </pre>
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The SQL query command.
 * @param p->retO Receives a result set with the values returned from the query.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If the sql command is null.
 */
LB_API void lLC_executeQuery_s(NMParams p) 
{
	TRACE("lLC_executeQuery_s")
   Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object sqlString = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];

      if (logger)
      {
	      LOCKVAR(log);
	      TC_executeMethod(context, loggerLog, logger, 16, sqlString, false);
	      UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
      }

      if (sqlString)
         TC_setObjectLock(p->retO = litebaseExecuteQuery(context, driver, String_charsStart(sqlString), String_charsLen(sqlString)), UNLOCKED);
      else
         TC_throwNullArgumentException(context, "sql"); // The string can't be null.
   }
      
finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native litebase.PrepareStatement prepareStatement(String sql) throws DriverException, NullPointerException, OutOfMemoryError;
/**
 * Creates a pre-compiled statement with the given sql. Prepared statements are faster for repeated queries. Instead of parsing the same query 
 * where only a few arguments change, it is better to create a prepared statement and the query is pre-parsed. Then, it is just needed to set the 
 * arguments (defined as ? in the sql) and run the sql.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The SQL query command.
 * @param p->retO Receives a pre-compiled SQL statement.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If the sql command is null.
 * @throws OutOfMemoryError If there is not enough memory to create the preparedStatement.
 */
LB_API void lLC_prepareStatement_s(NMParams p) 
{
	TRACE("lLC_prepareStatement_s")
   Object driver = p->obj[0],
          sqlObj = p->obj[1],
          oldSqlObj,
          logger = litebaseConnectionClass->objStaticValues[1],
          prepStmt = null;
   Context context = p->currentContext;
   LitebaseParser* parse;
   Hashtable* htPS;
	JCharP sqlChars,
          sqlCharsAux;
   char command[MAX_RESERVED_SIZE];
	int32 sqlLength,
         sqlLengthAux,
         numParams = 0,
         i,
         hashCode;

   MEMORY_TEST_START

   if (!sqlObj) // The string can't be null.
   {
      TC_throwNullArgumentException(context, "sql");
      goto finish;
   }

	if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
   {
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      goto finish;
   }

   sqlLength = String_charsLen(sqlObj);
   sqlChars = String_charsStart(sqlObj);

   if (logger)
	{
		Object logStr = litebaseConnectionClass->objStaticValues[2];
		int32 oldLength,
            newLength = sqlLength + 17;
      JCharP logChars;
      
      LOCKVAR(log);

		if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
      {
         if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
         {
            UNLOCKVAR(log);
            goto finish;
         }
         TC_setObjectLock(logStr, UNLOCKED);
      }

      // Builds the logger string contents.
		TC_CharP2JCharPBuf("prepareStatement ", 17, logChars = String_charsStart(logStr), false);
		xmemmove(&logChars[17], sqlChars, sqlLength << 1); 
		if (oldLength > newLength)
         xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
      
      TC_executeMethod(context, loggerLog, logger, 16, logStr, false);  
      UNLOCKVAR(log);
      if (context->thrownException)
         goto finish;
	}
   
   // juliana@226_16: prepared statement is now a singleton.
   // juliana@226a_21: solved a problem which could cause strange errors when using prepared statements.
   htPS = (Hashtable*)OBJ_LitebaseHtPS(driver);
   if ((prepStmt = p->retO = p->obj[0] = TC_htGetPtr(htPS, hashCode = TC_JCharPHashCode(sqlChars, sqlLength))) 
    && !OBJ_PreparedStatementDontFinalize(prepStmt) && (oldSqlObj = OBJ_PreparedStatementSqlExpression(prepStmt))
    && TC_JCharPEqualsJCharP(String_charsStart(oldSqlObj), sqlChars, String_charsLen(oldSqlObj), sqlLength))
   {
      lPS_clearParameters(p);
      goto finish;
   }

   // The prepared statement.
	if (!(prepStmt = p->retO = TC_createObject(context, "litebase.PreparedStatement")))
		goto finish;
	OBJ_PreparedStatementDriver(prepStmt) = driver;
	OBJ_PreparedStatementSqlExpression(prepStmt) = sqlObj;
   
   // Only parses commands that create statements.
   sqlLengthAux = sqlLength;
   sqlCharsAux = str16LeftTrim(sqlChars, &sqlLengthAux);
   TC_CharPToLower(TC_JCharP2CharPBuf(sqlCharsAux, 8, command));
   if (!sqlLengthAux) // juliana@230_20
   {
      TC_throwExceptionNamed(context, "litebase.SQLParseException", getMessage(ERR_SYNTAX_ERROR));
      goto finish;
   }
   
   if (xstrstr(command, "create"))
      OBJ_PreparedStatementType(p->retO) = CMD_CREATE_TABLE;
   else if (xstrstr(command, "delete") || xstrstr(command, "insert") || xstrstr(command, "select") || xstrstr(command, "update"))
   {
      Heap heapParser = heapCreate();
      bool locked = false;
      Table* table;

	   IF_HEAP_ERROR(heapParser)
      {
		   if (locked)
            UNLOCKVAR(parser);
         heapDestroy(heapParser);
		   TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
         goto finish;
      }

      // Parses the sql string.
	   locked = true;
	   LOCKVAR(parser);
	   parse = initLitebaseParser(context, sqlChars, sqlLength, heapParser);
      UNLOCKVAR(parser);
	   locked = false;
      
      // Error checking.
      if (!parse)
      {
         heapDestroy(heapParser);
         goto finish;
      }
      IF_HEAP_ERROR(heapParser)
      {
         TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
         heapDestroy(heapParser);
         goto finish;
      }

      // juliana@226_15: corrected a bug that would make a prepared statement with where clause and indices not work correctly after the first 
      // execution.
      switch (parse->command) // Gets the command in the SQL expression and creates the apropriate statement.
      {
         case CMD_DELETE:
         {
            SQLDeleteStatement* deleteStmt = initSQLDeleteStatement(parse, true);  
            
            if (litebaseBindDeleteStatement(context, driver, deleteStmt))
			   {
				   SQLBooleanClause* whereClause = deleteStmt->whereClause;

               if (whereClause)
                  whereClause->expressionTreeBak = cloneTree(whereClause->expressionTree, null, heapParser);
               
               table = deleteStmt->rsTable->table;
				   IF_HEAP_ERROR(table->heap)
               {
                  TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                  goto finish;
               }
               OBJ_PreparedStatementType(prepStmt) = CMD_DELETE;
			      OBJ_PreparedStatementStatement(prepStmt) = (int64)deleteStmt;
			      table->preparedStmts = TC_ObjectsAdd(table->preparedStmts, prepStmt, table->heap);
			   }
			   else
            {
               heapDestroy(heapParser);
               goto finish;
            }
            break;
         }

         case CMD_INSERT:
         {
            SQLInsertStatement* insertStmt = initSQLInsertStatement(context, driver, parse);
            
            if (!insertStmt || !litebaseBindInsertStatement(context, insertStmt))
            {
               heapDestroy(heapParser);
               goto finish;
            }

			   OBJ_PreparedStatementType(prepStmt) = CMD_INSERT;
			   table = insertStmt->table;
            IF_HEAP_ERROR(table->heap)
            {
               TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
               goto finish;
            }
		      OBJ_PreparedStatementStatement(prepStmt) = (int64)insertStmt;
			   table->preparedStmts = TC_ObjectsAdd(table->preparedStmts, prepStmt, table->heap);
            break;
         }

         case CMD_SELECT:
         {
            SQLSelectStatement* selectStmt = initSQLSelectStatement(parse, true);

            if (litebaseBindSelectStatement(context, driver, selectStmt))
			   {
               SQLSelectClause* selectClause = selectStmt->selectClause;
				   SQLResultSetTable** tableList = selectClause->tableList;
               int32 len = selectClause->tableListSize;
               SQLBooleanClause* whereClause = selectStmt->whereClause;
               SQLColumnListClause* orderByClause = selectStmt->orderByClause;
               SQLColumnListClause* groupByClause = selectStmt->groupByClause;
               SQLResultSetField** fieldList;
               uint8* fieldTableColIndexesBak;
               Heap heap = selectClause->heap;
               int32 count;

               IF_HEAP_ERROR(heap)
               {
                  heapDestroy(heapParser);
                  TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                  goto finish;
               }

               if (orderByClause)
               {
                  fieldList = orderByClause->fieldList;
                  count = orderByClause->fieldsCount;
                  fieldTableColIndexesBak = orderByClause->fieldTableColIndexesBak = TC_heapAlloc(heap, count);
                  while (--count >= 0)
                     fieldTableColIndexesBak[count] = fieldList[count]->tableColIndex;
               }

               // juliana@226_14: corrected a bug that would make a prepared statement with group by not work correctly after the first execution.
               if (groupByClause)
               {
                  fieldList = groupByClause->fieldList;
                  count = groupByClause->fieldsCount;
                  fieldTableColIndexesBak = groupByClause->fieldTableColIndexesBak = TC_heapAlloc(heap, count);
                  while (--count >= 0)
                     fieldTableColIndexesBak[count] = fieldList[count]->tableColIndex;
               }

               if (whereClause)
                  whereClause->expressionTreeBak = cloneTree(whereClause->expressionTree, null, heapParser);

				   OBJ_PreparedStatementType(prepStmt) = CMD_SELECT;
				   OBJ_PreparedStatementStatement(prepStmt) = (int64)selectStmt;
			      selectStmt->selectClause->sqlHashCode = TC_JCharPHashCode(sqlChars, sqlLength);
				   while (--len >= 0)
				   {
					   table = tableList[len]->table;
                  IF_HEAP_ERROR(table->heap)
                  {
                     TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                     goto finish;
                  }
				      table->preparedStmts = TC_ObjectsAdd(table->preparedStmts, prepStmt, table->heap);
				   }
			   }
			   else
            {
               heapDestroy(heapParser);
               goto finish;
            }
            break;
         }

         case CMD_UPDATE:
         {
            SQLUpdateStatement* updateStmt = initSQLUpdateStatement(context, driver, parse, true);
            SQLBooleanClause* whereClause;

            if (!updateStmt || !litebaseBindUpdateStatement(context, updateStmt))
            {
               heapDestroy(heapParser);
               goto finish;
            }

            if ((whereClause = (updateStmt->whereClause)))
               whereClause->expressionTreeBak = cloneTree(whereClause->expressionTree, null, heapParser);

            OBJ_PreparedStatementType(prepStmt) = CMD_UPDATE;
			   table = updateStmt->rsTable->table;
            IF_HEAP_ERROR(table->heap)
            {
               TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
               goto finish;
            }
		      OBJ_PreparedStatementStatement(prepStmt) = (int64)updateStmt;
		      table->preparedStmts = TC_ObjectsAdd(table->preparedStmts, prepStmt, table->heap);
            break;
         }
      }
   }

   if ((i = sqlLength)) // Tokenizes the sql string looking for '?'.
      while (--i)
         if (sqlChars[i] == '?')
            numParams++;

   // juliana@222_8: an array to hook the prepared statement object parameters.
   if (!(OBJ_PreparedStatementObjParams(prepStmt) = TC_createArrayObject(context, "[java.lang.Object", numParams)))
      goto finish;
   TC_setObjectLock(OBJ_PreparedStatementObjParams(prepStmt), UNLOCKED);
   
   // If the statement is to be used as a prepared statement, it is possible to use log.
   if (OBJ_PreparedStatementStatement(prepStmt) && logger)
   {
      int32* paramsPos;
      int32* paramsLength;
      JCharP* paramsAsStrs;
      
      if (numParams > 0)
      {
         // Creates the array of parameters.
         paramsAsStrs = (JCharP*)xmalloc(numParams << 2);
         if (!(OBJ_PreparedStatementParamsAsStrs(prepStmt) = (int64)paramsAsStrs))
         {
            TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
            goto finish;
         }

         // Creates the array of the parameters length
         paramsLength = (int32*)xmalloc(numParams << 2);
         if (!(OBJ_PreparedStatementParamsLength(prepStmt) = (int64)paramsLength))
         {
            TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
            goto finish;
         }

         i = numParams;

			// juliana@201_15: The prepared statement parameters for logging must be set as "unfilled" when creating it.
			while (--i >= 0)
         {
            if (!(paramsAsStrs[i] = TC_CharP2JCharP("unfilled", 8)))
            {
               TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
               goto finish;
            }
            paramsLength[i] = 8;
         }

         OBJ_PreparedStatementStoredParams(prepStmt) = numParams;
      }

      // The array of positions of the '?' in the sql.
      paramsPos = (int32*)xmalloc((numParams + 1) << 2);
      if (!(OBJ_PreparedStatementParamsPos(prepStmt) = (int64)paramsPos))
      {
         TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
         goto finish;
      }

      // Marks the positions of the '?'.
      paramsPos[numParams] = sqlLength;
      while (--sqlLength >= 0)
         if (sqlChars[sqlLength] == '?')
            paramsPos[--numParams] = sqlLength;
   }
   if (!TC_htPutPtr(htPS, hashCode, prepStmt))
      TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);

finish: ;

   // juliana@230_19: removed some possible memory problems with prepared statements and ResultSet.getStrings().
   if (context->thrownException && prepStmt)
      freePreparedStatement(prepStmt);

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native int getCurrentRowId(String tableName) throws DriverException, NullPointerException;
/**
 * Returns the current rowid for a given table.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of a table.
 * @param p->retI Receives the current rowid for the table.
 * @throws DriverException If the driver is closed.
 * @trows NullPointerException If table name is null.
 */
LB_API void lLC_getCurrentRowId_s(NMParams p) 
{
	TRACE("lLC_getCurrentRowId_s")
	Object driver = p->obj[0];  
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object tableName = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];
      Table* table;

	   if (logger)
	   {
		   Object logStr = litebaseConnectionClass->objStaticValues[2];
	      int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 16 : 20;
         JCharP logChars;
      
         LOCKVAR(log);

	      if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
		   // Builds the logger string contents.
	      TC_CharP2JCharPBuf("getCurrentRowId ", 16, logChars = String_charsStart(logStr), false);
	      if (tableName)
            xmemmove(&logChars[16], String_charsStart(tableName), String_charsLen(tableName) << 1); 
         else
            TC_CharP2JCharPBuf("null", 4, &logChars[16], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false);  
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
	   }
      if (tableName)
      {
         if ((table = getTableFromName(context, driver, tableName)))
            p->retI = table->currentRowId;
      }
      else // The table name can't be null.
         TC_throwNullArgumentException(context, "tableName");
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native int getRowCount(String tableName) throws DriverException, NullPointerException;
/**
 * Returns the number of valid rows in a table. This may be different from the number of records if a row has been deleted.
 * 
 * @see #getRowCountDeleted(String)
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of a table.
 * @param p->retI Receives the number of valid rows in a table.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If table name is null.
 */
LB_API void lLC_getRowCount_s(NMParams p) 
{
	TRACE("lLC_getRowCount_s")
   Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object tableName = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];
      Table* table;

		if (logger)
		{
			Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 12 : 16;
         JCharP logChars;
      
         LOCKVAR(log);

		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("getRowCount ", 12, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[12], String_charsStart(tableName), String_charsLen(tableName) << 1); 
		   else
            TC_CharP2JCharPBuf("null", 4, &logChars[12], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false);  
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
		}
      if (tableName)
      {
         if ((table = getTableFromName(context, driver, tableName)))
		      p->retI = table->db->rowCount - table->deletedRowsCount;
      }
      else // The table name can't be null.
         TC_throwNullArgumentException(context, "tableName");
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native void setRowInc(String tableName, int inc) throws DriverException, NullPointerException;
/**
 * Sets the row increment used when creating or updating big amounts of data. Using this method greatly increases the speed of bulk insertions 
 * (about 3x faster). To use it, it is necessary to call it (preferable) with the amount of lines that will be inserted. After the insertion is 
 * finished, it is <b>NECESSARY</b> to call it again, passing <code>-1</code> as the increment argument. Without doing this last step, data may be
 * lost because some writes will be delayed until the method is called with -1. Another good optimization on bulk insertions is to drop the indexes
 * and then create them afterwards. So, to correctly use <code>setRowInc()</code>, it is necessary to:
 *
 * <pre>
 * driver.setRowInc(&quot;table&quot;, totalNumberOfRows);
 * // Fetch the data and insert them.
 * driver.setRowInc(&quot;table&quot;, -1);
 * </pre>
 *
 * Using prepared statements on insertion makes it another a couple of times faster.
 *
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of a table.
 * @param p->i32[0] The increment value.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If table name is null.
 */
LB_API void lLC_setRowInc_si(NMParams p) 
{
	TRACE("lLC_setRowInc_si")
   Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object tableName = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];
      Table* table;
      int32 inc = p->i32[0];

      if (logger)
		{
			Object logStr = litebaseConnectionClass->objStaticValues[2];
         IntBuf intBuf;
         CharP incStr = TC_int2str(inc, intBuf);
		   int32 oldLength,
               incLen = xstrlen(incStr),
               nameLength = tableName? String_charsLen(tableName) : 4,
               newLength = nameLength + incLen + 11;
         JCharP logChars;
      
         LOCKVAR(log);

		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("setRowInc ", 10, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[10], String_charsStart(tableName), nameLength << 1); 
         else
            TC_CharP2JCharPBuf("null", 4, &logChars[10], true);
         logChars[10 + nameLength] = ' ';
         TC_CharP2JCharPBuf(incStr, incLen, &logChars[11 + nameLength], false);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false); 
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
		}

      if (tableName)
      {      
         if ((table = getTableFromName(context, driver, tableName)))
         {
            bool setting = inc != -1;
            int32 i = table->columnCount;
			   PlainDB* plainDB = table->db;
            Index** columnIndexes = table->columnIndexes;
			   ComposedIndex** composedIndexes = table->composedIndexes;
            XFile* dbFile = &plainDB->db;
            XFile* dboFile = &plainDB->dbo;

            plainDB->rowInc = setting? inc : DEFAULT_ROW_INC;
            while (--i >= 0) // Flushes the simple indices.
               if (columnIndexes[i])
                  indexSetWriteDelayed(context,columnIndexes[i], setting);
   			
			   // juliana@202_18: The composed indices must also be written delayed when setting row increment to a value different to -1.
			   i = table->numberComposedIndexes;
			   while (--i >= 0)
				   indexSetWriteDelayed(context, composedIndexes[i]->index, setting);

            // juliana@227_3: improved table files flush dealing.
			   if (inc == -1) // juliana@202_17: Flushs the files to disk when setting row increment to -1.
			   {
               dbFile->dontFlush = dboFile->dontFlush = false;
               if (dbFile->cacheIsDirty)
				      flushCache(context, dbFile); // Flushs .db.
               if (dboFile->cacheIsDirty)
				      flushCache(context, dboFile); // Flushs .dbo.
			   }
            else
               dbFile->dontFlush = dboFile->dontFlush = true;
         }
      }
      else // The table name can't be null.
         TC_throwNullArgumentException(context, "tableName");
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native boolean exists(String tableName) throws DriverException, NullPointerException; 
/**
 * Indicates if the given table already exists. This method can be used before a drop table.
 *
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of a table.
 * @param p->retI Receives <code>true</code> if a table exists; <code>false</code> othewise.
 * @throws DriverException If tableName is too big or the driver is closed.
 * @throws NullPointerException If table name is null.
 */
LB_API void lLC_exists_s(NMParams p)
{
	TRACE("lLC_exists_s")
   Object driver = p->obj[0],
          tableNameObj = p->obj[1];
   char tableNameCharP[DBNAME_SIZE],
        bufName[DBNAME_SIZE];
   TCHAR fullName[MAX_PATHNAME];

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else if (tableNameObj) 
      if (String_charsLen(tableNameObj) > MAX_TABLE_NAME_LENGTH)
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_MAX_TABLE_NAME_LENGTH));
      else
      {
         TC_JCharP2CharPBuf(String_charsStart(tableNameObj), String_charsLen(tableNameObj), tableNameCharP);
         getDiskTableName(p->currentContext, OBJ_LitebaseAppCrid(driver), tableNameCharP, bufName);
         xstrcat(bufName, DB_EXT);
         getFullFileName(bufName, (CharP)OBJ_LitebaseSourcePath(driver), fullName);
         p->retI = fileExists(fullName, OBJ_LitebaseSlot(driver));
      }
   else // The table name can't be null.
      TC_throwNullArgumentException(p->currentContext, "tableName");

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Releases the file handles (on the device) of a Litebase instance. Note that, after this is called, all <code>Resultset</code>s and 
 * <code>PreparedStatement</code>s created with this Litebase instance will be in an inconsistent state, and using them will probably reset the 
 * device. This method also deletes the active instance for this creator id from Litebase's internal table.
 *
 * @param p->obj[0] The connection with Litebase.
 * @throws DriverException If the driver is closed.
 */
LB_API void lLC_closeAll(NMParams p) // litebase/LitebaseConnection public native void closeAll() throws DriverException;
{
	TRACE("lLC_closeAll")
	Object driver = p->obj[0];
   Context context = p->currentContext;
   
   MEMORY_TEST_START
   
   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object logger = litebaseConnectionClass->objStaticValues[1];

      if (logger)
	   {
		   Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength;
         JCharP logChars;
         
         LOCKVAR(log);

	      if ((oldLength = String_charsLen(logStr)) < 8) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectFromCharP(context, "closeAll", 8)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         else
         {
		      // Builds the logger string contents.
	         TC_CharP2JCharPBuf("closeAll", 8, logChars = String_charsStart(logStr), false);
            xmemzero(&logChars[8], (oldLength - 8) << 1);
         }

         TC_executeMethod(context, loggerLog, logger, 16, logStr, false);  
         UNLOCKVAR(log);
	   }

finish: // juliana@214_7: must free Litebase even if the log string creation fails.
      freeLitebase(context, (int32)driver);
      xfree(context->litebasePtr);	   
   }
   	
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@201_13: .dbo is now being purged.
// litebase/LitebaseConnection public native int purge(String tableName) throws DriverException, NullPointerException, OutOfMemoryError;
/**
 * Used to delete physically the records of the given table. Records are always deleted logically, to avoid the need of recreating the indexes. When 
 * a new record is added, it doesn't uses the position of the previously deleted one. This can make the table big, if a table is created, filled and 
 * has a couple of records deleted. This method will remove all deleted records and recreate the indexes accordingly. Note that it can take some time 
 * to run.
 * <p>
 * Important: the rowid of the records is NOT changed with this operation.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The table name to purge.
 * @param p->retI Receives the number of purged records.
 * @throws DriverException If the driver is closed or a row can't be read or written.
 * @throws NullPointerException if table name is null. 
 * @throws OutOfMemoryError If there is not enough memory to purge the table.
 */
LB_API void lLC_purge_s(NMParams p) 
{
	TRACE("lLC_purge_s")
   Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object tableName = p->obj[1], 
             logger = litebaseConnectionClass->objStaticValues[1];

      if (logger)
	   {
		   Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 6 : 10;
         JCharP logChars;
      
         LOCKVAR(log);

		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("purge ", 6, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[6], String_charsStart(tableName), String_charsLen(tableName) << 1); 
		   else
            TC_CharP2JCharPBuf("null", 4, &logChars[6], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false);
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
	   }

      if (tableName)
      {
         Table* table = getTableFromName(context, driver, tableName);
         int32 deleted = 0;

         if (table && (deleted = table->deletedRowsCount) > 0) // Removes the deleted records from the table.
         {
            PlainDB* plainDB = table->db;
            XFile* dbFile = &plainDB->db;
            Index** columnIndexes = table->columnIndexes;
            ComposedIndex** composedIndexes = table->composedIndexes;
            int32 willRemain = plainDB->rowCount - deleted,
                  columnCount = table->columnCount,
                  i;
            bool updateAuxRowId = false; // rnovais@570_61

            // juliana@226_4: now a table won't be marked as not closed properly if the application stops suddenly and the table was not modified 
            // since its last opening. 
            if (!table->isModified)
            {
               i = (plainDB->isAscii? IS_ASCII : 0);
	            nfSetPos(dbFile, 6);
	            if (nfWriteBytes(context, dbFile, (uint8*)&i, 1) && flushCache(context, dbFile)) // Flushs .db.
                  table->isModified = true;
	            else
                  goto finish;
            }

            if (willRemain) 
            {
               // rnovais@570_75: inserts all records at once.
               // juliana@223_12: purge now only recreates the .dbo file, reusing .db file on Windows 32, Windows CE, Palm, iPhone, and Android.
               char buffer[DBNAME_SIZE];
               XFile newdbo,
                     olddbo;
               int32* columnTypes = table->columnTypes;
               int32* columnSizes = table->columnSizes;
               uint16* columnOffsets = table->columnOffsets;
               uint8* basbuf = plainDB->basbuf;
               uint8* columnNulls0 = table->columnNulls[0];
               int32 rowCount = plainDB->rowCount,
                     id,
                     j,
                     crc32, 
                     length = table->columnOffsets[columnCount] + NUMBEROFBYTES(columnCount),
                     remain = 0,
                     slot = table->slot;
               CharP sourcePath = (CharP)OBJ_LitebaseSourcePath(driver);
	            SQLValue** record;
               Heap heap = heapCreate(); 

               IF_HEAP_ERROR(heap)
               {
                  TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                  heapDestroy(heap);
                  goto finish;
               }

               // Allocates the temporary records.
               i = columnCount;
               record = (SQLValue**)TC_heapAlloc(heap, columnCount * PTRSIZE);
			      while (--i >= 0)
			      {
				      record[i] = (SQLValue*)TC_heapAlloc(heap, sizeof(SQLValue));
				      if (columnTypes[i] == CHARS_TYPE || columnTypes[i] == CHARS_NOCASE_TYPE)
					      record[i]->asChars = (JCharP)TC_heapAlloc(heap, (columnSizes[i] << 1) + 2); 
                  else if (columnTypes[i] == BLOB_TYPE)
					      record[i]->asBlob = (uint8*)TC_heapAlloc(heap, columnSizes[i]);
			      }

               // rnovais@570_61: verifies if it needs to store the currentRowId.
			      if (plainRead(context, plainDB, rowCount - 1))
               {
                  xmove4(&id, basbuf); 
                  if ((id & ROW_ATTR_MASK) == ROW_ATTR_DELETED) // Is the last record deleted?
                     updateAuxRowId = true;
               }
               else
               {
                  heapDestroy(heap);
                  goto finish;
               }

               if (updateAuxRowId) // rnovais@570_61
                  table->auxRowId = table->currentRowId;
               
               // Creates the temporary .dbo file.
               xstrcpy(buffer, plainDB->dbo.name);
               xstrcat(buffer, "_");
               if (!nfCreateFile(context, buffer, true, sourcePath, slot, &newdbo, -1)) // Creates the new .dbo file.
                  goto finish;

			      plainDB->rowInc = willRemain;
               i = -1;
               while (++i < rowCount)
               {
				      if (!readRecord(context, table, record, i, 0, null, 0, false, null, null)) // juliana@227_20
                  {
                     heapDestroy(heap);
                     goto finish;
                  }

				      xmove4(&record[0]->asInt, plainDB->basbuf); 
                  if ((record[0]->asInt & ROW_ATTR_MASK) != ROW_ATTR_DELETED) // is record ok?
                  {
                     xmemmove(&olddbo, &plainDB->dbo, sizeof(XFile));
                     xmemmove(&plainDB->dbo, &newdbo, sizeof(XFile));
                     
                     // juliana@225_3: corrected a possible "An attempt was made to move the file pointer before the beginning of the file." on 
                     // some Windows CE devices when doing a purge.
                     j = -1;
                     while (++j < columnCount)
						      if (!writeValue(context, plainDB, record[j], &basbuf[columnOffsets[j]], columnTypes[j], columnSizes[j], true, true, false, 
                                                                                                                                            false))
                        {
                           heapDestroy(heap);
                           nfRemove(context, &newdbo, sourcePath, slot);
                           xmemmove(&plainDB->dbo, &olddbo, sizeof(XFile));
                           goto finish;
                        }
   						
					      xmemmove(&basbuf[columnOffsets[j]], columnNulls0, NUMBEROFBYTES(j)); 
   						
                     // juliana@223_8: corrected a bug on purge that would not copy the crc32 codes for the rows.
                     // juliana@220_4: added a crc32 code for every record. Please update your tables.
                     j = basbuf[3];
                     basbuf[3] = 0; // juliana@222_5: The crc was not being calculated correctly for updates.
                     crc32 = computeCRC32(basbuf, length);
                     xmove4(&basbuf[length], &crc32); // Computes the crc for the record and stores at the end of the record.
                     basbuf[3] = j;

					      if (!plainRewrite(context, plainDB, remain++))
                     {
                        heapDestroy(heap);
                        goto finish;
                     }
                     xmemmove(&newdbo, &plainDB->dbo, sizeof(XFile));
                     xmemmove(&plainDB->dbo, &olddbo, sizeof(XFile));
                  }
               }
               
               if (!nfRemove(context, &olddbo, sourcePath, slot) || !nfRename(context, &newdbo, olddbo.name, sourcePath, slot))
               {
                  heapDestroy(heap);
                  goto finish;
               }
			      xmemmove(&plainDB->dbo, &newdbo, sizeof(XFile));
               plainDB->rowInc = DEFAULT_ROW_INC;
               plainDB->rowCount = remain;
               heapDestroy(heap);
            }
            else // If no rows will remain, just deletes everyone.
            {
               XFile* dbo = &plainDB->dbo;

               if ((i = fileSetSize(&dbFile->file, 0)) || (i = fileSetSize(&dbo->file, 0)))
               {
                  fileError(context, i, dbFile->name);
                  goto finish;
               }
               
               dbo->finalPos = dbFile->finalPos = dbFile->size = dbo->size = plainDB->rowAvail = plainDB->rowCount = 0;
               updateAuxRowId = true; // Needs to update the auxRowId, because the last line was deleted.
            }

            table->deletedRowsCount = 0; // Empties the deletedRows.  

            // Recreates the simple indices.
            i = table->columnCount;
            while (--i >= 0)

               // juliana@202_14: Corrected the simple index re-creation when purging the table. 
               if (columnIndexes[i] && !tableReIndex(context, table, i, 0, null))
               {
                  table->deletedRowsCount = deleted;
                  goto finish;
               }

            // recreate the composed indexes
            if ((i = table->numberComposedIndexes) > 0)
               while (--i >= 0)
                  if (!tableReIndex(context, table, -1, 0, composedIndexes[i]))
                  {
                     table->deletedRowsCount = deleted;
                     goto finish;
                  }

            // juliana@115_8: saving metadata before recreating the indices does not let .db header become empty.
            // Updates the metadata.
            if (!tableSaveMetaData(context, table, TSMD_EVERYTHING)) // guich@560_24 table->saveOnExit = 1;
               goto finish; 

            // juliana@227_16: purge must truncate the .db file and flush .dbo file in order to ensure that a future recoverTable() won't corrupt the 
            // table.
            if (plainDB->dbo.cacheIsDirty && !flushCache(context, &plainDB->dbo)) // Flushs .dbo.
               goto finish;
            if ((i = fileSetSize(&dbFile->file, dbFile->size = plainDB->rowCount * plainDB->rowSize + plainDB->headerSize))
             || (i = fileFlush(dbFile->file)))
            {
               fileError(context, i, dbFile->name);
               goto finish;
            }
         }
         p->retI = deleted;
	      
      }
      else // The table name can't be null.
         TC_throwNullArgumentException(context, "tableName");
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native int getRowCountDeleted(String tableName) throws DriverException, NullPointerException;
/**
 * Returns the number of deleted rows.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of a table.
 * @param p->retI Receives the total number of deleted records of the given table.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If table name is null.
 */
LB_API void lLC_getRowCountDeleted_s(NMParams p) 
{
	TRACE("lLC_getRowCountDeleted_s")
	Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object tableName = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];
      Table* table;

		if (logger)
		{
			Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 19 : 23;
         JCharP logChars;
      
         LOCKVAR(log);

		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("getRowCountDeleted ", 19, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[19], String_charsStart(tableName), String_charsLen(tableName) << 1); 
		   else
            TC_CharP2JCharPBuf("null", 4, &logChars[19], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false); 
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
		}

      if (tableName)
      {
         if ((table = getTableFromName(context, driver, tableName)))
		      p->retI = table->deletedRowsCount;
      }
      else // The table name can't be null.
         TC_throwNullArgumentException(context, "tableName");
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native litebase.RowIterator getRowIterator(String tableName) throws DriverException, NullPointerException;
/**
 * Gets an iterator for a table. With it, it is possible iterate through all the rows of a table in sequence and get its attributes. This is good for
 * synchronizing a table. While the iterator is active, it is not possible to do any queries or updates because this can cause dada corruption.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of a table.
 * @param p->retO receives a iterator for the given table.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If table name is null.
 */
LB_API void lLC_getRowIterator_s(NMParams p) 
{
	TRACE("lLC_getRowIterator_s")
   Object driver = p->obj[0];
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      Object tableName = p->obj[1],
	          logger = litebaseConnectionClass->objStaticValues[1];

	   if (logger)
	   {
		   Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 15 : 19;
         JCharP logChars;
      
         LOCKVAR(log);

		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("getRowIterator ", 15, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[15], String_charsStart(tableName), String_charsLen(tableName) << 1); 
		   else
            TC_CharP2JCharPBuf("null", 4, &logChars[15], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false); 
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
	   }

      if (tableName)
      {
         Table* table;

         if ((table = getTableFromName(context, driver, tableName)))
         {
            Object rowIterator;

            // Creates and populates the row iterator object.
            if ((rowIterator = p->retO = TC_createObject(context, "litebase.RowIterator")))
            {
	            TC_setObjectLock(rowIterator, UNLOCKED);
               OBJ_RowIteratorTable(rowIterator) = (int64)table;
               OBJ_RowIteratorRowNumber(rowIterator) = -1;
               OBJ_RowIteratorData(rowIterator) = TC_createArrayObject(context, BYTE_ARRAY, table->db->rowSize);
               OBJ_RowIteratorDriver(rowIterator) = driver;
               TC_setObjectLock(OBJ_RowIteratorData(rowIterator), UNLOCKED);
            }

         }
      }
      else // The table name can't be null.
         TC_throwNullArgumentException(context, "tableName");
   }
finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Gets the Litebase logger. The fields should be used unless using the logger within threads. 
 * 
 * @param p->retO receives the logger.
 * @throws DriverException if an <code>IOException</code> occurs.
 */
// litebase/LitebaseConnection public static native totalcross.util.Logger getLogger() throws DriverException; 
LB_API void lLC_privateGetLogger(NMParams p) 
{
	TRACE("lLC_privateGetLogger")
	MEMORY_TEST_START
	LOCKVAR(log); 
   p->retO = litebaseConnectionClass->objStaticValues[1];
	UNLOCKVAR(log);
	MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Sets the litebase logger. This enables log messages for all queries and statements of Litebase and can be very useful to help finding bugs in 
 * the system. Logs take up memory space, so turn them on only when necessary. The fields should be used unless using the logger within threads.
 * 
 * @param p->obj[0] The logger.
 */
LB_API void lLC_privateSetLogger_l(NMParams p) // litebase/LitebaseConnection public static native void setLogger(totalcross.util.Logger logger);
{
	TRACE("lLC_privateSetLogger_l")
   MEMORY_TEST_START
	LOCKVAR(log); 
	litebaseConnectionClass->objStaticValues[1] = p->obj[0];
	UNLOCKVAR(log);
	MEMORY_TEST_END
}

// juliana@230_4: Litebase default logger is now a plain text file instead of a PDB file.                                                                                             
//////////////////////////////////////////////////////////////////////////                                                                           
/**                                                                                                                                                  
 * Gets the default Litebase logger. When this method is called for the first time, a new text file is created. In the subsequent calls, the same    
 * file is used.                                                                                                                                     
 *                                                                                                                                                   
 * @param p->retI receives the number of files deleted.                                                                                              
 */                                                                                                                                                  
LB_API void lLC_privateGetDefaultLogger(NMParams p) // litebase/LitebaseConnection public static native totalcross.util.Logger getDefaultLogger();   
{                                                                                                                                                    
	TRACE("lLC_privateGetDefaultLogger")                                                                                                               
   Context context = p->currentContext;                                                                                                              
   Object nameStr,                                                                                                                                   
          logger,                                                                                                                                    
          file = null;                                                                                                                                      
   char nameCharP[MAX_PATHNAME];                                                                                                                     
                                                                                                                                                     
   MEMORY_TEST_START                                                                                                                                 
	LOCKVAR(log);                                                                                                                                      
	                                                                                                                                                   
   // Creates the logger string.                                                                                                                     
   // juliana@225_10: Corrected a possible crash when using the default logger.                                                                      
   if (!(nameStr = TC_createStringObjectFromCharP(context, "litebase", 8)))                                                                          
      goto finish;                                                                                                                                                                                                                                                 
                                                                                                                                                     
   // Gets the logger object.                                                                                                                        
   if (!(p->retO = logger = TC_executeMethod(context, getLogger, nameStr, -1, null).asObj))                                                          
      goto finish;                                                                                                                                   
   if (context->thrownException)                                                                                                                     
      goto finish;                                                                                                                                   
                                                                                                                                                     
	if (!FIELD_I32(FIELD_OBJ(logger, loggerClass, 1), 0)) // Only gets a new default logger if no one exists.                                          
	{                                                                                                                                                  
		LongBuf timeLong;                                                                                                                                
		char strAppId[5];                                                                                                                                
      int32 year,                                                                                                                                    
            month,                                                                                                                                   
            day,                                                                                                                                     
            hour,                                                                                                                                    
            minute,                                                                                                                                  
            second,                                                                                                                                  
            millis;                                                                                                                                  
                                                                                                                                                     
      getCurrentPath(nameCharP);                                                                                                                     
		xstrcat(nameCharP, "/LITEBASE_");                                                                                                                
      TC_getDateTime(&year, &month, &day, &hour, &minute, &second, &millis);                                                                         
		xstrcat(nameCharP, TC_long2str(getTimeLong(year, month, day, hour, minute, second), timeLong));                                                  
		xstrcat(nameCharP, ".");                                                                                                                         
      TC_int2CRID(TC_getApplicationId(), strAppId);                                                                                                  
		xstrcat(nameCharP, strAppId);                                                                                                                    
		xstrcat(nameCharP, ".LOGS");
		TC_setObjectLock(nameStr, UNLOCKED);
		nameStr = null;                                                                                                                     
		if (!(file = TC_createObjectWithoutCallingDefaultConstructor(context, "totalcross.io.File"))                                                     
       || !(nameStr = TC_createStringObjectFromCharP(context, nameCharP, -1)))                                                                       
         goto finish;                                                                                                                                
                                                                                                                                                     
		TC_executeMethod(context, newFile, file, nameStr, 8, 1); // CREATE_EMPTY                                                                         
      if (context->thrownException)                                                                                                                  
         goto finish;                                                                                                                                
                                                                                                                                                     
		TC_executeMethod(context, addOutputHandler, logger, file);                                                                                       
      if (context->thrownException)                                                                                                                  
         goto finish;                                                                                                                                
                                                                                                                                                     
	}                                                                                                                                                  
                                                                                                                                                     
	FIELD_I32(logger, 0) = 16;                                                                                                                         
   p->retO = logger;                                                                                                                                 
                                                                                                                                                     
finish: ;                                                                                                                                            
   TC_setObjectLock(file, UNLOCKED);                                                                                                                 
   TC_setObjectLock(nameStr, UNLOCKED); 
   
   // juliana@230_23: now LitebaseConnection.getDefaultLogger() will throw a DriverException instead of an IOException if a file error occurs.
   if (context->thrownException && TC_areClassesCompatible(context, OBJ_CLASS(context->thrownException), "totalcross.io.IOException"))                                                                                                             
   {
      Object exception = context->thrownException,
			    exceptionMsg = FIELD_OBJ(exception, throwableClass, 0);
      char msgError[1024];
      
      if (exceptionMsg)
      {
         int32 length = String_charsLen(exceptionMsg);
         TC_JCharP2CharPBuf(String_charsStart(exceptionMsg), length < 1024? length : 1023, msgError);
	   }
	   else
	      xstrcpy(msgError, "null");
      
      context->thrownException = null;
      TC_throwExceptionNamed(context, "litebase.DriverException", msgError);

      if (strEq(OBJ_CLASS(context->thrownException)->name, "litebase.DriverException"))
		   OBJ_DriverExceptionCause(context->thrownException) = exception;
   }
   UNLOCKVAR(log);                                                                                                                                   
	MEMORY_TEST_END                                                                                                                                    
}                                                                                                                                                    
                                                                                                                                                     
//////////////////////////////////////////////////////////////////////////                                                                           
/**                                                                                                                                                  
 * Deletes all the log files with the default format found in the default device folder. If log is enabled, the current log file is not affected by 
 * this command.
 *                                                                                                                                                   
 * @param p->retI receives the number of files deleted.                                                                                              
 */                                                                                                                                                  
LB_API void lLC_privateDeleteLogFiles(NMParams p) // litebase/LitebaseConnection public static native int deleteLogFiles();                          
{                                                                                                                                                    
	TRACE("lLC_privateDeleteLogFiles")                                                                                                                 
   Context context = p->currentContext;                                                                                                              
   char pathCharP[MAX_PATHNAME];                                                                                                                     
   TCHARPs* list = null;                                                                                                                             
   TCHAR fullPath[MAX_PATHNAME];
   char name[MAX_PATHNAME];
   int32 count = 0,                                                                                                                                  
         i = 0,
         ret;
   Heap heap = heapCreate();                                                                                                                         
                                                                                                                                                     
#ifdef WINCE // A file name in char for Windows CE, which uses TCHAR.                                                                                
   char value[DBNAME_SIZE];                                                                                                                          
   JChar pathTCHARP[MAX_PATHNAME];                                                                                                                   
#else                                                                                                                                                
   CharP value;                                                                                                                                      
   CharP pathTCHARP;                                                                                                                                 
#endif                                                                                                                                               
	                                                                                                                                                   
   MEMORY_TEST_START                                                                                                                                 
   LOCKVAR(log);                                                                                                                                     
   IF_HEAP_ERROR(heap)                                                                                                                               
   {                                                                                                                                                 
      heapDestroy(heap);                                                                                                                             
      TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);                                                                           
      goto finish;                                                                                                                                   
   }                                                                                                                                                 
   getCurrentPath(pathCharP);                                                                                                                        
                                                                                                                                                     
#ifdef WINCE                                                                                                                                         
   TC_CharP2JCharPBuf(pathCharP, -1, pathTCHARP, true);                                                                                              
#else                                                                                                                                                
   pathTCHARP = pathCharP;                                                                                                                           
#endif                                                                                                                                               
	                                                                                                                                                   
   if ((ret = TC_listFiles(pathTCHARP, 1, &list, &count, heap, 0))) // Lists all the files of the folder.                                              
   {                                                                                                                                                 
      fileError(context, ret, "");                                                                                                                     
      goto finish;                                                                                                                                   
   }                                                                                                                                                 
	        
   name[0] = 0;
   if (count)
   {
      Object logger = litebaseConnectionClass->objStaticValues[1],
             nameObj;

      if (logger)
      {
		   nameObj = FIELD_OBJ(((Object*)ARRAYOBJ_START(FIELD_OBJ(FIELD_OBJ(logger, loggerClass, 1), vectorClass, 0)))[0], fileClass, 0);
         TC_JCharP2CharPBuf(String_charsStart(nameObj), String_charsLen(nameObj), name);
      }
   }
                                                                                                                                           
   while (--count >= 0)                                                                                                                              
   {                                                                                                                                                 
#ifndef WINCE                                                                                                                                        
      value = list->value;                                                                                                                           
#else                                                                                                                                                
      TC_JCharP2CharPBuf(list->value, -1, value);                                                                                                    
#endif                                                                                                                                               
                                                                                                                                                     
      if (xstrstr(value, "LITEBASE") == value && xstrstr(value, ".LOGS") && !xstrstr(name, value)) // Deletes only the closed log files.                                      
      {  
         getFullFileName(value, pathCharP, fullPath);                                                                                                
         if ((ret = fileDelete(null, fullPath, 1, false)))                                                                                                  
         {                                                                                                                                                 
            fileError(context, ret, "");                                                                                                                     
            goto finish;                                                                                                                                   
         }        
         i++;
      }                                                                                                                                              
                                                                                                                                                     
      list = list->next;                                                                                                                             
   }                                                                                                                                                 
	 
   p->retI = i; // The number of log files deleted.

finish: ;                                                                                                                                            
   heapDestroy(heap);                                                                                                                                
	UNLOCKVAR(log);                                                                                                                                    
	MEMORY_TEST_END                                                                                                                                    
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public static native litebase.LitebaseConnection processLogs(String []sql, String params, boolean isDebug) 
// throws DriverException, NullPointerException, OutOfMemoryError;
/**
 * This is a handy method that can be used to reproduce all commands of a log file. This is intended to be used by the development team only. 
 * Here's a sample on how to use it:
 * 
 * <pre>
 * String []sql =
 * {
 *    &quot;new LitebaseConnection(MBSL,null)&quot;,
 *    &quot;create table PRODUTO (IDPRODUTO int, IDPRODUTOERP char(10), IDGRUPOPRODUTO int, IDSUBGRUPOPRODUTO int, IDEMPRESA char(20), 
 *                                DESCRICAO char(100), UNDCAIXA char(10), PESO float, UNIDADEMEDIDA char(3),
 *                                EMBALAGEM char(10), PORCTROCA float, PERMITETROCA int)&quot;,
 *    &quot;create index IDX_PRODUTO_1 on PRODUTO(IDPRODUTO)&quot;,
 *    &quot;create index IDX_PRODUTO_2 on PRODUTO(IDGRUPOPRODUTO)&quot;,
 *    &quot;create index IDX_PRODUTO_3 on PRODUTO(IDEMPRESA)&quot;,
 *    &quot;create index IDX_PRODUTO_4 on PRODUTO(DESCRICAO)&quot;,
 *    &quot;closeAll&quot;,
 *    &quot;new LitebaseConnection(MBSL,null)&quot;,
 *    &quot;insert into PRODUTO values(1,'19132', 2, 1, '1', 2, '3', 'ABSORVENTE SILHO ABAS', '5', 13, 'PCT', '20X30', 10, 0)&quot;,
 *  };
 *  LitebaseConnection.processLogs(sql, true);
 * </pre>
 * 
 * @param p->obj[0] The string array of SQL commands to be executed.
 * @param p->obj[1] The parameters to open a connection.
 * @param p->i32[0] Indicates if debug information is to displayed on the debug console.
 * @param p->retO Receives the LitebaseConnection instance created, or <code>null</code> if <code>closeAll</code> was the last command executed (or 
 * no commands were executed at all).
 * @throws DriverException If an exception occurs.
 * @throws NullPointerException If <code>p->obj[0]</code> is null.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lLC_privateProcessLogs_Ssb(NMParams p) 
{
	TRACE("lLC_privateProcessLogs_Ssb")
	Object driver = null,
	       sql = p->obj[0],
	       params = p->obj[1],
          string,
          resultSetObj;
   Object* sqlArray = (Object*)ARRAYOBJ_START(sql);
   Context context = p->currentContext;
	bool isDebug = p->i32[0];
	int32 i,
         j = -1,
		   length = ARRAYOBJ_LEN(sql),
         sqlLen;
   JCharP sqlStr;
	ResultSet* resultSet;

	MEMORY_TEST_START
   i = j;

   if (sql)
	   while (++i < length)
	   {
         if (isDebug)
			   TC_debug("running command # %d", (i + 1));
         string = sqlArray[i];

         // Gets a new Litebase Connection.
         if (JCharPStartsWithCharP(sqlStr = String_charsStart(string), "new LitebaseConnection", sqlLen = String_charsLen(string), 22))
			   TC_setObjectLock(p->retO = driver = create(context, getAppCridInt(&sqlStr[23]), params), UNLOCKED);
		   
         // Create command.
         else if (JCharPStartsWithCharP(sqlStr, "create", sqlLen, 6))
			   litebaseExecute(context, driver, sqlStr, sqlLen);
		   
         // closeAll() command.
         else if (JCharPEqualsCharP(sqlStr, "closeAll", sqlLen, 8, true))
		   {
            freeLitebase(context, (int32)driver);
			   p->retO = driver = null;
		   }

         // Select command.
		   else if (JCharPStartsWithCharP(sqlStr, "select", sqlLen, 6))
		   {
			   if ((resultSetObj = litebaseExecuteQuery(context, driver, sqlStr, sqlLen)))
            {
               resultSet = (ResultSet*)OBJ_ResultSetBag(resultSetObj);
			      while (resultSetNext(context, resultSet));
			      freeResultSet(resultSet);
               TC_setObjectLock(resultSetObj, UNLOCKED);
            }
		   }

         // Commands that update the table.
		   else if (sqlLen > 0)
			   litebaseExecuteUpdate(context, driver, sqlStr, sqlLen);

		   if (context->thrownException)
		   {
			   Object exception = context->thrownException,
			          exceptionMsg = FIELD_OBJ(exception, throwableClass, 0);
            char msgError[1024];
            
            if (exceptionMsg)
            {
               int32 length = String_charsLen(exceptionMsg);
               TC_JCharP2CharPBuf(String_charsStart(exceptionMsg), length < 1024? length : 1023, msgError);
			   }
			   else
			      xstrcpy(msgError, "null");
            
            if (isDebug)
            {
               char sqlErr[1024];
               TC_JCharP2CharPBuf(sqlStr, sqlLen < 1024? sqlLen : 1023, sqlErr);
               TC_debug("%s - %s", sqlErr, msgError);
            }

            context->thrownException = null;
            TC_throwExceptionNamed(context, "litebase.DriverException", msgError);

            if (strEq(OBJ_CLASS(context->thrownException)->name, "litebase.DriverException"))
				   OBJ_DriverExceptionCause(context->thrownException) = exception;
			   break;
		   }
         else
            j++;
	   }
   else
      TC_throwNullArgumentException(context, "sql");

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@220_5: added a method to recover possible corrupted tables, the ones that were not closed properly.
// litebase/LitebaseConnection public native boolean recoverTable(String tableName) throws DriverException, NullPointerException, OutOfMemoryError;
/**
 * Tries to recover a table not closed properly by marking and erasing logically the records whose crc are not valid.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of the table to be converted.
 * @param p->retI Receives the number of purged records.
 * @throws DriverException If the driver is closed or the table name is too big.
 * @throws NullPointerException If table name is null.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lLC_recoverTable_s(NMParams p) 
{
   TRACE("lLC_recoverTable_s")
   Object driver = p->obj[0],
          tableName = p->obj[1],
	       logger = litebaseConnectionClass->objStaticValues[1];
   Context context = p->currentContext;
	char name[DBNAME_SIZE];
   CharP sourcePath = (CharP)OBJ_LitebaseSourcePath(driver);
   TCHAR buffer[MAX_PATHNAME];
   Heap heap = null;
   Table* table = null;
	PlainDB* plainDB;
   uint8* basbuf;
   Index** columnIndexes;
   NATIVE_FILE tableDb;
   int32 crid = OBJ_LitebaseAppCrid(driver),
         slot = OBJ_LitebaseSlot(driver),
         i,
         j,
         read,
         rows,
			columnCount,
         crcPos,
	      crc32Lido = 0,
         deleted,
         ret = 0;

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else if (!tableName) // The table name can't be null.
      TC_throwNullArgumentException(context, "tableName");
   else if (String_charsLen(tableName) > MAX_TABLE_NAME_LENGTH)
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_MAX_TABLE_NAME_LENGTH));
   else
   {
      if (logger)
	   {
		   Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 14 : 18;
         JCharP logChars;
      
         LOCKVAR(log);
		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("recover table ", 14, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[14], String_charsStart(tableName), String_charsLen(tableName) << 1); 
		   else
            TC_CharP2JCharPBuf("null", 4, &logChars[14], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false); 
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
	   }

      // Opens the table file.
	   TC_JCharP2CharPBuf(String_charsStart(tableName), String_charsLen(tableName), &name[5]);
	   TC_CharPToLower(&name[5]); // juliana@227_19: corrected a bug in convert() and recoverTable() which could not find the table .db file. 
      TC_int2CRID(crid, name);
      name[4] = '-';
      xstrcat(name, ".db");
      getFullFileName(name, sourcePath, buffer);
      if ((j = fileCreate(&tableDb, buffer, READ_WRITE, &slot))) // Opens the .db table file.
	   {
		   fileError(context, j, name);
		   goto finish;
	   }

      // juliana@222_2: the table must be not closed properly in order to recover it.
	   if ((j = fileSetPos(tableDb, 6)) || (j = fileReadBytes(tableDb, (uint8*)&crc32Lido, 0, 1, &read)))
      {
		   fileError(context, j, name);
         fileClose(&tableDb);
		   goto finish;
	   }
      if (read != 1) // juliana@226_8: a table without metadata (with an empty .db, for instance) can't be recovered: it is corrupted.
      {
         fileError(context, j, name);
         fileClose(&tableDb);
         TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_TABLE_CORRUPTED), name);
         goto finish;
      }
	   if ((crc32Lido & IS_SAVED_CORRECTLY) == IS_SAVED_CORRECTLY) 
	   {
		   TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_TABLE_CLOSED), name);
         fileClose(&tableDb);
		   goto finish;
      }
	   fileClose(&tableDb);

      heap = heapCreate();
	   IF_HEAP_ERROR(heap)
	   {
		   TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
         heapDestroy(heap);
         goto finish;
	   }

      name[xstrlen(name) - 3] = 0;

	   // Opens the table even if it was not cloded properly.
	   if (!(table = tableCreate(context, name, sourcePath, slot, crid, false, (bool)OBJ_LitebaseIsAscii(driver), false, heap)))
         goto finish;

	   rows = (plainDB = table->db)->rowCount;
	   table->deletedRowsCount = 0; // Invalidates the number of deleted rows.
      columnCount = table->columnCount;
	   basbuf = plainDB->basbuf;
      columnIndexes = table->columnIndexes;
      crcPos = (int32)table->columnOffsets[columnCount] + NUMBEROFBYTES(columnCount);
      deleted = 0;
      i = -1;

	   while (++i < rows) // Checks all table records.
	   {
		   if (!plainRead(context, plainDB, i))
			   goto finish;
		   xmove4(&read, basbuf);
		   if ((read & ROW_ATTR_MASK) == ROW_ATTR_DELETED) // Counts the number of deleted records.
            deleted++;
		   else 
		   {
			   xmove4(&crc32Lido, &basbuf[crcPos]);
			   basbuf[3] = 0; // Erases rowid information.
			   if (computeCRC32(basbuf, crcPos) != crc32Lido) // Deletes and invalidates corrupted records.
			   {
               j = ROW_ATTR_DELETED;
               xmove4(basbuf, &j);
				   if (!plainRewrite(context, plainDB, i))
					   goto finish;
				   ret = ++deleted;
			   }
            else // juliana@224_3: corrected a bug that would make Litebase not use the correct rowid after a recoverTable().
               table->auxRowId = (read & ROW_ID_MASK) + 1; 
		   }
	   }

      table->deletedRowsCount = deleted;

      // Recreates the indices.
      // Simple indices.
      while (--columnCount >= 0)
			if (columnIndexes[columnCount] && !tableReIndex(context, table, columnCount, false, null))
            goto finish;

      // Recreates the composed indexes.
      if ((i = table->numberComposedIndexes))
	   {
         ComposedIndex** compIndexes = table->composedIndexes;
         while (--i >= 0)
            if (!tableReIndex(context, table, -1, false, compIndexes[i]))
               goto finish;
	   }

      plainDB->wasNotSavedCorrectly = false;

      // juliana@224_3: corrected a bug that would make Litebase not use the correct rowid after a recoverTable().
	   tableSaveMetaData(context, table, TSMD_ONLY_AUXROWID); // Saves information concerning deleted rows.
      p->retI = ret;
   }

finish: 
	if (table) 
      freeTable(context, table, false, true); // Closes the table.
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native void convert(String tableName) throws DriverException, NullPointerException, OutOfMemoryError;
/**
 * Converts a table from the previous Litebase table version to the current one. If the table format is older than the previous table version, this 
 * method can't be used. It is possible to know if the table version is not compativel with the current version used in Litebase because an exception
 * will be thrown if one tries to open a table with the old format. The table will be closed after using this method. Notice that the table .db file 
 * will be overwritten. 
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The name of the table to be converted.
 * @throws DriverException If the table version is not the previous one (too old or the actual used by Litebase), the driver is closed or the table 
 * name is too big.
 * @throws NullPointerException If table name is null.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lLC_convert_s(NMParams p) 
{
	TRACE("lLC_convert_s")
	Object driver = p->obj[0],
          tableName = p->obj[1],
          logger = litebaseConnectionClass->objStaticValues[1];
   Context context = p->currentContext;
   Heap heap;
   char name[DBNAME_SIZE];
   CharP sourcePath = (CharP)OBJ_LitebaseSourcePath(driver);
   TCHAR buffer[MAX_PATHNAME];
   Table* table = null;
	PlainDB* plainDB;
   uint8* basbuf;
   XFile dbFile;
   NATIVE_FILE tableDb;
	int32 crid = OBJ_LitebaseAppCrid(driver),
         slot = OBJ_LitebaseSlot(driver),
         i,
         j = 0,
         length,
			rows,
         rowSize,
			headerSize,
         read;

	MEMORY_TEST_START
	
   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else if (!tableName) // The table name can't be null.
      TC_throwNullArgumentException(context, "tableName");
   else if (String_charsLen(tableName) > MAX_TABLE_NAME_LENGTH)
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_MAX_TABLE_NAME_LENGTH));
   else
   {
      if (logger)
	   {
		   Object logStr = litebaseConnectionClass->objStaticValues[2];
		   int32 oldLength,
               newLength = tableName? String_charsLen(tableName) + 8 : 12;
         JCharP logChars;
      
         LOCKVAR(log);
		   if ((oldLength = String_charsLen(logStr)) < newLength) // Reuses the logger string whenever possible.
         {
            if (!(logStr = litebaseConnectionClass->objStaticValues[2] = TC_createStringObjectWithLen(context, newLength)))
            {
               UNLOCKVAR(log);
               goto finish;
            }
            TC_setObjectLock(logStr, UNLOCKED);
         }
         
			// Builds the logger string contents.
		   TC_CharP2JCharPBuf("convert ", 8, logChars = String_charsStart(logStr), false);
		   if (tableName)
            xmemmove(&logChars[8], String_charsStart(tableName), String_charsLen(tableName) << 1); 
		   else
            TC_CharP2JCharPBuf("null", 4, &logChars[8], true);
         if (oldLength > newLength)
            xmemzero(&logChars[newLength], (oldLength - newLength) << 1);   
         
         TC_executeMethod(context, loggerLog, logger, 16, logStr, false); 
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
	   }
 
      // Opens the .db table file.
	   TC_JCharP2CharPBuf(String_charsStart(tableName), String_charsLen(tableName), &name[5]);
	   TC_CharPToLower(&name[5]); // juliana@227_19: corrected a bug in convert() and recoverTable() which could not find the table .db file. 
      TC_int2CRID(crid, name);
      name[4] = '-';
      xstrcat(name, ".db");

      // juliana@225_11: corrected possible memory leaks and crashes when LitebaseConnection.convert() fails.
      heap = heapCreate();
	   IF_HEAP_ERROR(heap)
	   {
		   TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
         heapDestroy(heap);
         goto finish;
	   }
	   
      getFullFileName(name, sourcePath, buffer);
	   if ((i = fileCreate(&tableDb, buffer, READ_WRITE, &slot))) // Opens the .db table file.
	   {
		   fileError(context, i, name);
         heapDestroy(heap);
         goto finish;
	   }

	   // The version must be the previous of the current one.
	   if ((i = fileSetPos(tableDb, 7)) || (i = fileReadBytes(tableDb, (uint8*)&j, 0, 1, &read))) 
      {
		   fileError(context, i, name);
         fileClose(&tableDb);
         heapDestroy(heap);
         goto finish;
	   }
	   if (j != VERSION_TABLE - 1) 
	   {
		   TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_WRONG_PREV_VERSION), name);
         fileClose(&tableDb);
         heapDestroy(heap);
		   goto finish;
      }

      // Changes the version to be current one and closes it.
	   j = VERSION_TABLE;
      if ((i = fileSetPos(tableDb, 7)) || (i = fileWriteBytes(tableDb, (uint8*)&j, 0, 1, &read)))
      {
		   fileError(context, i, name);
         fileClose(&tableDb);
         heapDestroy(heap);
         goto finish;
	   }
	   fileClose(&tableDb);

	   name[xstrlen(name) - 3] = 0;

	   // Opens the table even if it was not cloded properly.
	   if (!(table = tableCreate(context, name, sourcePath, slot, crid, false, (bool)OBJ_LitebaseIsAscii(driver), false, heap)))
         goto finish;

	   dbFile = (plainDB = table->db)->db;
	   headerSize = plainDB->headerSize;
	   basbuf = plainDB->basbuf;
	   rows = (dbFile.size - headerSize) / (length = (rowSize = plainDB->rowSize) - 4);
	   plainDB->rowCount = rows;

	   while (--rows >= 0) // Converts all the records adding a crc code to them.
	   {
		   nfSetPos(&dbFile, rows * length + headerSize);
		   if (nfReadBytes(context, &dbFile, basbuf, length) != length)
            goto finish;
		   j = basbuf[3];
		   basbuf[3] = 0;
         i = computeCRC32(basbuf, length);
         xmove4(&basbuf[length], &i);
		   basbuf[3] = j;
		   nfSetPos(&dbFile, rows * rowSize + headerSize);
		   nfWriteBytes(context, &dbFile, basbuf, rowSize);
	   }
   }

finish:
   if (table)
	   freeTable(context, table, false, false); // Closes the table.
	MEMORY_TEST_END
}

// juliana@223_1: added a method to get the current slot being used.
//////////////////////////////////////////////////////////////////////////
/**
 * Returns the slot where the tables are stored. Always return -1 except on palm. 
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->retI receives -1 except on palm, where returns the current slot being used.
 */
LB_API void lLC_getSlot(NMParams p) // litebase/LitebaseConnection public native int getSlot(); 
{
   TRACE("lLC_getSlot")
   p->retI = OBJ_LitebaseSlot(p->obj[0]);
}

// juliana@226_6: added LitebaseConnection.isOpen(), which indicates if a table is open in the current connection.
//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native boolean isOpen(String tableName) throws DriverException, NullPointerException; 
/**
 * Indicates if a table is open or not.
 * 
 * @param p->obj[0] The connection with Litebase.
 * @param p->obj[1] The table name to be checked.
 * @param p->retI receives <code>true</code> if the table is open in the current connection; <code>false</code>, otherwise.
 * @throws DriverException If the driver is closed.
 * @throws NullPointerException If the table name is null.
 */
LB_API void lLC_isOpen_s(NMParams p) 
{
   TRACE("lLC_isOpen_s")
   Object driver = p->obj[0],
          tableName = p->obj[1];
   Hashtable* htTables = (Hashtable*)OBJ_LitebaseHtTables(driver);

   MEMORY_TEST_START

   if (OBJ_LitebaseDontFinalize(driver)) // The driver can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else if (!tableName) // The table name can't be null.
      TC_throwNullArgumentException(p->currentContext, "tableName");
   else
   {
      int32 length = String_charsLen(tableName);
      char nameCharP[DBNAME_SIZE];

      // Checks if the table name hash code is in the driver hash table.
      TC_JCharP2CharPBuf(String_charsStart(tableName), length, nameCharP);
      TC_CharPToLower(nameCharP);
      p->retI = TC_htGetPtr(htTables, TC_hashCode(nameCharP)) != null; 
   }

   MEMORY_TEST_END
}

// juliana@226_10: added LitebaseConnection.dropDatabase().
//////////////////////////////////////////////////////////////////////////
// litebase/LitebaseConnection public native static void dropDatabase(String crid, String sourcePath, int slot) throws DriverException, 
// NullPointerException;
/**
 * Drops all the tables from a database represented by its application id and path.
 * 
 * @param p->obj[0] The application id of the database.
 * @param p->obj[1] The path where the files are stored.
 * @param p->i32[0] The slot on Palm where the source path folder is stored. Ignored on other platforms.
 * @throws DriverException If the database is not found or a file error occurs.
 */
LB_API void lLC_dropDatabase_ssi(NMParams p)
{
   TRACE("lLC_dropDatabase_ssi")
   Object cridObj = p->obj[0],
          pathObj = p->obj[1];
   
   MEMORY_TEST_START
   if (cridObj) 
      if (pathObj)
         if (String_charsLen(pathObj) >= MAX_PATHNAME - 4 - DBNAME_SIZE) // The path length can't be greater than the buffer size.
            TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_INVALID_PATH));
         else
         {
            TCHARPs* list = null;

#ifdef WINCE
            JCharP cridStr;
            JChar pathStr[MAX_PATHNAME]; // juliana@230_6
            char value[DBNAME_SIZE],
            fullPath[MAX_PATHNAME];
#else
            char cridStr[5],
                 pathStr[MAX_PATHNAME]; 
            CharP fullPath;
            CharP value;
#endif

            int32 i,
                  count = 0,
                  slot = p->i32[0];
            bool deleted = false;
            Heap heap = heapCreate();
            TCHAR buffer[MAX_PATHNAME];

#ifdef PALMOS
            if (slot == -1)
               slot = TC_getLastVolume();
#endif

            IF_HEAP_ERROR(heap)
            {
               TC_throwExceptionNamed(p->currentContext, "java.lang.OutOfMemoryError", null);
               heapDestroy(heap);
               goto finish;
            }

#ifdef WINCE
            cridStr = String_charsStart(cridObj);
            
            // juliana@230_6: corrected LitebaseConnection.dropDatabase() not working properly on Windows CE.
            xmemmove(pathStr, String_charsStart(pathObj), (i = String_charsLen(pathObj)) << 1);
            pathStr[i] = 0;
            TC_JCharP2CharPBuf(pathStr, i, fullPath);
#else
            TC_JCharP2CharPBuf(String_charsStart(cridObj), 4, cridStr);
            TC_JCharP2CharPBuf(String_charsStart(pathObj), String_charsLen(pathObj), fullPath = pathStr);
#endif

            if ((i = TC_listFiles(pathStr, slot, &list, &count, heap, 0))) // Lists all the files of the folder. 
            {
               fileError(p->currentContext, i, "");
               heapDestroy(heap);
               goto finish;
            }

            while (--count >= 0) // Deletes only the files of the chosen database.
            {
#ifndef WINCE         
               value = list->value;
               if (xstrstr(value, cridStr) == value)
#else
               TC_JCharP2CharPBuf(list->value, -1, value);
               if (str16StartsWith(list->value, cridStr, TC_JCharPLen(list->value), 4, 0, false))
#endif
               {
                  getFullFileName(value, fullPath, buffer);
                  if ((i = fileDelete(null, buffer, slot, false)))
                  {
                     fileError(p->currentContext, i, value);
                     heapDestroy(heap);
                     goto finish;
                  }
                  deleted = true;
               }

               list = list->next;
            }
            
            heapDestroy(heap);
            if (!deleted)
               TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DB_NOT_FOUND));
         }  
      else // The string argument can't be null.
         TC_throwNullArgumentException(p->currentContext, "sourcePath");
   else // The string argument can't be null.
      TC_throwNullArgumentException(p->currentContext, "crid");

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/ResultSet public native litebase.ResultSetMetaData getResultSetMetaData() throws DriverException;
/**
 * Returns the metadata for this result set.
 *
 * @param p->obj[0] The result set.
 * @param p->retO receives the metadata for this result set.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_getResultSetMetaData(NMParams p) 
{
	TRACE("lRS_getResultSetMetaData")
   Object resultSet = p->obj[0],
          rsMetaData;
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(resultSet);
	
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));   
      else
         if ((p->retO = rsMetaData = TC_createObject(p->currentContext, "litebase.ResultSetMetaData")))
         {
            TC_setObjectLock(rsMetaData, UNLOCKED);
            OBJ_ResultSetMetaData_ResultSet(rsMetaData) = resultSet;	   
         }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Releases all memory allocated for this object. Its a good idea to call this when you no longer needs it, but it is also called by the GC when the 
 * object is no longer in use.
 *
 * @param p->obj[0] The result set.
 * @throws DriverException If the result set is closed.
 */
LB_API void lRS_close(NMParams p) // litebase/ResultSet private native void rsClose() throws DriverException;
{
	TRACE("lRS_close")
   Object resultSet = p->obj[0];
	ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(resultSet);
	
   MEMORY_TEST_START
	if (rsBag) // juliana@211_4: solved bugs with result set dealing.
   {
      freeResultSet(rsBag);
      OBJ_ResultSetBag(resultSet) = null;
      OBJ_ResultSetDontFinalize(resultSet) = true;
   }
   else // The result set can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Places the cursor before the first record.
 *
 * @param p->obj[0] The result set.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_beforeFirst(NMParams p) // litebase/ResultSet public native void beforeFirst() throws DriverException;
{
	TRACE("lRS_beforeFirst")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag) // The result set can't be closed.
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));   
      else
         rsBag->pos = -1;
   }
   else
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Places the cursor after the last record.
 *
 * @param p->obj[0] The result set.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_afterLast(NMParams p) // litebase/ResultSet public native void afterLast() throws DriverException;
{
	TRACE("lRS_afterLast")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag) // The result set can't be closed.
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
         rsBag->pos = rsBag->table->db->rowCount;
   }
   else
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Places the cursor in the first record of the result set.
 *
 * @param p->obj[0] The result set.
 * @param p->retI Receives <code>true</code> if it was possible to place the cursor in the first record; <code>false</code>, otherwise.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_first(NMParams p) // litebase/ResultSet public native bool first() throws DriverException;
{
	TRACE("lRS_first")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag) // The result set can't be closed.
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         int32 ok;
         rsBag->pos = -1; // Sets the position before the first record.
         if (!(ok = resultSetNext(p->currentContext, rsBag))) // Reads the first record. 
            rsBag->pos = -1; // guich@_105: sets the record to -1 if it can't read the first position.
         p->retI = ok;
      }
   }   
   else
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Places the cursor in the last record of the result set.
 *
 * @param p->obj[0] The result set.
 * @param p->retI Receives <code>true</code> if it was possible to place the cursor in the last record; <code>false</code>, otherwise.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_last(NMParams p) // litebase/ResultSet public native bool last() throws DriverException;
{
	TRACE("lRS_last")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         int32 ok;
         rsBag->pos = rsBag->table->db->rowCount; // Sets the position after the last record.
         if (!(ok = resultSetPrev(p->currentContext, rsBag))) // Reads the last record. 
            rsBag->pos = -1; // guich@_105: sets the record to -1 if it can't read the last position.
         p->retI = ok;
      }
   }
   else
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Gets the next record of the result set.
 *
 * @param p->obj[0] The result set.
 * @param p->retI Receives <code>true</code> if there is a next record to go to in the result set; <code>false</code>, otherwise.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_next(NMParams p) // litebase/ResultSet public native bool next() throws DriverException;
{
	TRACE("lRS_next")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag) // The ResultSet can't be closed.
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else   
         p->retI = resultSetNext(p->currentContext, rsBag);
   }
   else
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Returns the previous record of the result set.
 *
 * @param p->obj[0] The result set.
 * @param p->retI Receives <code>true</code> if there is a previous record to go to in the result set; <code>false</code>, otherwise.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_prev(NMParams p) // litebase/ResultSet public native bool prev() throws DriverException;
{
	TRACE("lRS_prev")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag) // The ResultSet can't be closed.
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
         p->retI = resultSetPrev(p->currentContext, rsBag);
   }
   else
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a short value that is represented by this column. Note that it is only possible to request this 
 * column as short if it was created with this precision.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retI receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0</code>.
 */
LB_API void lRS_getShort_i(NMParams p) // litebase/ResultSet public native short getShort(int col) throws DriverException;
{
   TRACE("lRS_getShort_i");
   MEMORY_TEST_START
   rsGetByIndex(p, SHORT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a short value that is represented by this column. Note that it is only possible to request this 
 * column as short if it was created with this precision. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retI receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0</code>.
 */
LB_API void lRS_getShort_s(NMParams p) // litebase/ResultSet public native short getShort(String colName) throws DriverException;
{
   TRACE("lRS_getShort_s");
   MEMORY_TEST_START
   rsGetByName(p, SHORT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns an integer value that is represented by this column. Note that it is only possible to request this 
 * column as integer if it was created with this precision.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retI receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0</code>.
 */
LB_API void lRS_getInt_i(NMParams p) // litebase/ResultSet public native int getInt(int col) throws DriverException;
{
   TRACE("lRS_getInt_i");
   MEMORY_TEST_START
   rsGetByIndex(p, INT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns an integer value that is represented by this column. Note that it is only possible to request this 
 * column as integer if it was created with this precision. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retI receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0</code>.
 */
LB_API void lRS_getInt_s(NMParams p) // litebase/ResultSet public native int getInt(String colName) throws DriverException;
{
   TRACE("lRS_getInt_s");
   MEMORY_TEST_START
   rsGetByName(p, INT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a long value that is represented by this column. Note that it is only possible to request this 
 * column as long if it was created with this precision.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retL receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0</code>.
 */
LB_API void lRS_getLong_i(NMParams p) // litebase/ResultSet public native long getLong(int col) throws DriverException;
{
   TRACE("lRS_getLong_i");
   MEMORY_TEST_START
   rsGetByIndex(p, LONG_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a long value that is represented by this column. Note that it is only possible to request this 
 * column as long if it was created with this precision. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retL receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0</code>.
 */
LB_API void lRS_getLong_s(NMParams p) // litebase/ResultSet public native long getLong(String colName) throws DriverException;
{
   TRACE("lRS_getLong_s");
   MEMORY_TEST_START
   rsGetByName(p, LONG_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a float value that is represented by this column. Note that it is only possible to request this 
 * column as float if it was created with this precision.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retD receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0.0</code>.
 */
LB_API void lRS_getFloat_i(NMParams p) // litebase/ResultSet public native double getFloat(int col) throws DriverException;
{
   TRACE("lRS_getFloat_i");
   MEMORY_TEST_START
   rsGetByIndex(p, FLOAT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a float value that is represented by this column. Note that it is only possible to request this 
 * column as float if it was created with this precision. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retD receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0.0</code>.
 */
LB_API void lRS_getFloat_s(NMParams p) // litebase/ResultSet public native double getFloat(String colName) throws DriverException;
{
   TRACE("lRS_getFloat_s");
   MEMORY_TEST_START
   rsGetByName(p, FLOAT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a double value that is represented by this column. Note that it is only possible to request this 
 * column as double if it was created with this precision.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retD receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0.0</code>.
 */
LB_API void lRS_getDouble_i(NMParams p) // litebase/ResultSet public native double getDouble(int col) throws DriverException;
{
   TRACE("lRS_getDouble_i");
   MEMORY_TEST_START
   rsGetByIndex(p, DOUBLE_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a double value that is represented by this column. Note that it is only possible to request this 
 * column as double if it was created with this precision. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retD receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>0.0</code>.
 */
LB_API void lRS_getDouble_s(NMParams p) // litebase/ResultSet public native double getDouble(String colName) throws DriverException;
{
   TRACE("lRS_getDouble_s");
   MEMORY_TEST_START
   rsGetByName(p, DOUBLE_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a char array that is represented by this column. Note that it is only possible to request this 
 * column as a char array if it was created as a string.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getChars_i(NMParams p) // litebase/ResultSet public native char[] getChars(int col) throws DriverException;
{
   TRACE("lRS_getChars_i");
   MEMORY_TEST_START
   rsGetByIndex(p, CHARS_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a char array that is represented by this column. Note that it is only possible to request this 
 * column as a char array if it was created as a string. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getChars_s(NMParams p) // litebase/ResultSet public native char[] getChars(String colName) throws DriverException;
{
   TRACE("lRS_getChars_s");
   MEMORY_TEST_START
   rsGetByName(p, CHARS_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a string that is represented by this column. Any column type can be returned as a string. 
 * <code>Double</code>/<code>float</code> values formatting will use the precision set with the <code>setDecimalPlaces()</code> method.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>
 */
LB_API void lRS_getString_i(NMParams p) // litebase/ResultSet public native String getString(int col) throws DriverException;
{
   TRACE("lRS_getString_i");
   MEMORY_TEST_START
   rsGetByIndex(p, UNDEFINED_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a string that is represented by this column. Any column type can be returned as a string. 
 * <code>Double</code>/<code>float</code> values formatting will use the precision set with the <code>setDecimalPlaces()</code> method. This 
 * method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column index.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>
 */
LB_API void lRS_getString_s(NMParams p) // litebase/ResultSet public native String getString(String colName) throws DriverException;
{
   TRACE("lRS_getString_s");
   MEMORY_TEST_START
   rsGetByName(p, -1);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a byte (blob) array that is represented by this column. Note that it is only possible to request 
 * this column as a blob if it was created this way.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getBlob_i(NMParams p) // litebase/ResultSet public native uint8[] getBlob(int col) throws DriverException;
{
   TRACE("lRS_getBlob_i");
   MEMORY_TEST_START
   rsGetByIndex(p, BLOB_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a byte array (blob) that is represented by this column. Note that it is only possible to request 
 * this column as a blob if it was created this way. This method is slightly slower then the method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getBlob_s(NMParams p) // litebase/ResultSet public native uint8[] getBlob(String colName) throws DriverException;
{
   TRACE("lRS_getBlob_s");
   MEMORY_TEST_START
   rsGetByName(p, BLOB_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Starting from the current cursor position, it reads all result set rows that are being requested. <code>first()</code>,  <code>last()</code>, 
 * <code>prev()</code>, or <code>next()</code> must be used to set the current position, but not  <code>beforeFirst()</code> or 
 * <code>afterLast()</code>. It doesn't return BLOB values. <code>null</code> is returned in their places instead.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The number of rows to be fetched, or -1 for all. 
 * @param p->retO receives a matrix, where <code>String[0]<code> is the first row, and <code>String[0][0], String[0][1]...</code> are the column 
 * elements of the first row. Returns <code>null</code> if here's no more element to be fetched. Double/float values will be formatted using the 
 * <code>setDecimalPlaces()</code> settings. If the value is SQL <code>NULL</code> or a <code>blob</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getStrings_i(NMParams p) // litebase/ResultSet public native String[][] getStrings(int count);
{  
	TRACE("lRS_getStrings_i")
   MEMORY_TEST_START
   getStrings(p, p->i32[0]); // juliana@201_2: corrected a bug that would let garbage in the number of records parameter.
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Starting from the current cursor position, it reads all result set rows of the result set. <code>first()</code>,  <code>last()</code>, 
 * <code>prev()</code> or <code>next()</code> must be used to set the current position, but not <code>beforeFirst()</code> or 
 * <code>afterLast()</code>. It doesn't return BLOB values. <code>null</code> is returned in their places instead. 
 *
 * @param p->obj[0] The result set.
 * @param p->retO receives a matrix, where <code>String[0]<code> is the first row, and <code>String[0][0], String[0][1]...</code> are the column 
 * elements of the first row. Returns <code>null</code> if here's no more element to be fetched. Double/float values will be formatted using the 
 * <code>setDecimalPlaces()</code> settings. If the value is SQL <code>NULL</code> or a <code>blob</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getStrings(NMParams p) // litebase/ResultSet public native String[][] getStrings();
{  
	TRACE("lRS_getStrings")
   MEMORY_TEST_START
   getStrings(p, -1); // juliana@201_2: corrected a bug that would let garbage in the number of records parameter.
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a <code>Date</code> value that is represented by this column. Note that it is only possible 
 * to request this column as a date if it was created this way (DATE or DATETIME).
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getDate_i(NMParams p) // litebase/ResultSet public native totalcross.util.Date getDate(int col);
{
	TRACE("lRS_getDate_i")
	MEMORY_TEST_START
	rsGetByIndex(p, DATE_TYPE);
   rsGetDate(p);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), returns a <code>Date</code> value that is represented by this column. Note that it is only possible 
 * to request this column as a date if it was created this way (DATE or DATETIME). This method is slightly slower then the method that accepts a 
 * column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retO receives the column value; if the value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 */
LB_API void lRS_getDate_s(NMParams p) // litebase/ResultSet public native totalcross.util.Date getDate(String colName);
{
	TRACE("lRS_getDate_s")
	MEMORY_TEST_START
	rsGetByName(p, DATE_TYPE);
   rsGetDate(p);
	MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), returns a <code>Time</code> (correspondent to a DATETIME data type) value that is represented by this 
 * column. Note that it is only possible to request this column as a date if it was created this way.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The colum index.
 * @param p->retO receives the time of the DATETIME. If the DATETIME value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_getDateTime_i(NMParams p) // litebase/ResultSet public native totalcross.sys.Time getDateTime(int colIdx) throws DriverException;
{
	TRACE("lRS_getDateTime_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   Context context = p->currentContext;

	MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
         rsGetDateTime(context, &p->retO, rsBag, p->i32[0] - 1);
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/ResultSet public native totalcross.sys.Time getDateTime(String colName) throws DriverException, NullPointerException;
/**
 * Given the column name (case insensitive), returns a <code>Time</code> (correspondent to a DATETIME data type) value that is represented by this
 * column. Note that it is only possible to request this column as a date if it was created this way. This method is slightly slower then the 
 * method that accepts a column index.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[0] The colum name.
 * @param p->retO receives the time of the DATETIME. If the DATETIME value is SQL <code>NULL</code>, the value returned is <code>null</code>.
 * @throws DriverException If the result set or the driver is closed.
 * @throws NullPointerException If the column name is null.
 */
LB_API void lRS_getDateTime_s(NMParams p) 
{
	TRACE("lRS_getDateTime_s")
   Object colName = p->obj[1];
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         if (colName)
            rsGetDateTime(context, &p->retO, rsBag, TC_htGet32Inv(&rsBag->intHashtable, identHashCode(colName)));
         else  
            TC_throwNullArgumentException(context, "colName");
      }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Places this result set cursor at the given absolute row. This is the absolute physical row of the table. This method is usually used to restore
 * the row at a previous row got with the <code>getRow()</code> method.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The row to set the cursor.
 * @param p->retI receives <code>true</code> whenever this method does not throw an exception.
 * @throws DriverException If the result set or the driver is closed, or it is not possible to set the cursor at the given row.
 */
LB_API void lRS_absolute_i(NMParams p) // litebase/ResultSet public native bool absolute(int row) throws DriverException;
{
	TRACE("lRS_absolute_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   Context context = p->currentContext;
   int32 row = p->i32[0],
         i;
   
   MEMORY_TEST_START

   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else   
      {
         Table* table = rsBag->table;
         PlainDB* plainDB = table->db;
         int32 rowCountLess1 = plainDB->rowCount - 1;

		   if (table->deletedRowsCount > 0) // juliana@210_1: select * from table_name does not create a temporary table anymore.
         {
            int32 rowCount = 0;
            
            // Continues searching the position until finding the right row or the end of the result set table.
            while (rowCount <= rowCountLess1 && rowCount <= row)
            {   
               // Reads the next row.
               rsBag->pos = rowCount;
				   if (!plainRead(context, plainDB, rowCount++))	
					   goto finish;
               xmove4(&i, plainDB->basbuf);
               
				   if ((i & ROW_ATTR_MASK) == ROW_ATTR_DELETED) // If it was deleted, one more row will be read in total.
                  row++;
            }
            xmemmove(table->columnNulls[0], plainDB->basbuf + table->columnOffsets[table->columnCount], NUMBEROFBYTES(table->columnCount));
         } 
         else if (0 <= row && row <= rowCountLess1)
         {
            rsBag->pos = row;
			   if ((p->retI = plainRead(context, plainDB, row)))				
               xmemmove(table->columnNulls[0], plainDB->basbuf + table->columnOffsets[table->columnCount], NUMBEROFBYTES(table->columnCount));
			   else 
               TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_CANT_READ_RS), row);
		   }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Moves the cursor <code>rows</code> in distance. The value can be greater or lower than zero.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The distance to move the cursor.
 * @param p->retI receives <code>true</code> whenever this method does not throw an exception.
 * @throws DriverException If the result set or the driver is closed, or it is not possible to set the cursor at the given row.
 */
LB_API void lRS_relative_i(NMParams p) // litebase/ResultSet public native bool relative(int rows) throws DriverException;
{
	TRACE("lRS_relative_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   Context context = p->currentContext;
   
   MEMORY_TEST_START

   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         Table* table = rsBag->table;
         PlainDB* plainDB = table->db;
         int32 rows = p->i32[0];
   		
		   if (table->deletedRowsCount > 0) // juliana@210_1: select * from table_name does not create a temporary table anymore.
         {
            // Continues searching the position until finding the right row or the end or the beginning of the result set table.
            if (rows > 0)
               while (--rows >= 0)
					   resultSetNext(context, rsBag);
            else
               while (++rows <= 0)
                  resultSetPrev(context, rsBag);
            if (rsBag->pos <= 0)
               resultSetNext(context, rsBag);
            if (rsBag->pos >= plainDB->rowCount - 1)
               resultSetPrev(context, rsBag);
            xmemmove(table->columnNulls[0], plainDB->basbuf + table->columnOffsets[table->columnCount], NUMBEROFBYTES(table->columnCount));
         } 
		   else
		   {
            // The new pos is pos + rows or 0 (if pos + rows < 0) or bag.lastRecordIndex (if pos + rows > bag.lastRecordIndex).
			   int32 newPos = MAX(0, MIN(plainDB->rowCount - 1, rsBag->pos + rows));
			   if (rsBag->pos != newPos) // If there are no deleted rows, just reads the row in the right position.
			   {
				   rsBag->pos = newPos;
				   if ((p->retI = plainRead(context, plainDB, newPos)))
					   xmemmove(table->columnNulls[0], plainDB->basbuf + table->columnOffsets[table->columnCount], NUMBEROFBYTES(table->columnCount));
				   else
					   TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_CANT_READ_RS), newPos);
			   }
		   }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Returns the current physical row of the table where the cursor is. It must be used with <code>absolute()</code> method.
 *
 * @param p->obj[0] The result set.
 * @param p->retI receives the current physical row of the table where the cursor is.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_getRow(NMParams p) // litebase/ResultSet public native int getRow() throws DriverException;
{
	TRACE("lRS_getRow")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
         p->retI = rsBag->pos; // Returns the current position of the cursor.
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Sets the number of decimal places that the given column (starting from 1) will have when being converted to <code>String</code>.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column.
 * @param p->i32[1] The number of decimal places.
 * @throws DriverException If the result set or the driver is closed, the column index is invalid, or the value for decimal places is invalid.
 */
LB_API void lRS_setDecimalPlaces_ii(NMParams p) // litebase/ResultSet public native void setDecimalPlaces(int col, int places) throws DriverException;
{
	TRACE("lRS_setDecimalPlaces_ii")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag)
	{
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
	      int32 column = rsBag->isSimpleSelect? p->i32[0] : p->i32[0] - 1,
               places = p->i32[1];

         if (column < 0 || column >= rsBag->columnCount) // The columns given by the user ranges from 1 to n.
            TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_INVALID_COLUMN_NUMBER), column);
         else
         {
            int32 type = rsBag->table->columnTypes[column]; // Gets the column type.

            if (places < -1 || places > 40) // Invalid value for decimal places.
               TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RS_DEC_PLACES_START), places);
            else
            if (type == FLOAT_TYPE || type == DOUBLE_TYPE) // Only sets the decimal places if the type is FLOAT or DOUBLE.
               rsBag->decimalPlaces[column] = places;
            else
               TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_INCOMPATIBLE_TYPES));
         }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Returns the number of rows of the result set.
 *
 * @param p->obj[0] The result set.
 * @param p->retI receives the number of rows.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_getRowCount(NMParams p) // litebase/ResultSet public native int getRowCount() throws DriverException;
{
	TRACE("lRS_getRowCount")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
         p->retI = rsBag->table->db->rowCount - rsBag->table->deletedRowsCount; // juliana@114_10: Removes the deleted rows. 
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column index (starting from 1), indicates if this column has a <code>NULL</code>.
 *
 * @param p->obj[0] The result set.
 * @param p->i32[0] The column index.
 * @param p->retI receives <code>true</code> if the value is SQL <code>NULL</code>; <code>false</code>, otherwise.
 * @throws DriverException If the result set or the driver is closed.
 */
LB_API void lRS_isNull_i(NMParams p) // litebase/ResultSet public native boolean isNull(int col) throws DriverException;
{
	TRACE("lRS_isNull_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);
   
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         // juliana@227_14: corrected a DriverException not being thrown when fetching in some cases when trying to fetch data from an invalid result 
         // set column.
         // juliana@210_1: select * from table_name does not create a temporary table anymore.
		   int32 givenColumn = p->i32[0],
            column = rsBag->isSimpleSelect? givenColumn + 1 : givenColumn; 
        
         if (verifyRSState(p->currentContext, rsBag, givenColumn))
            p->retI = isBitSet(rsBag->table->columnNulls[0], column - 1); 
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Given the column name (case insensitive), indicates if this column has a <code>NULL</code>.
 *
 * @param p->obj[0] The result set.
 * @param p->obj[1] The column name.
 * @param p->retI receives <code>true</code> if the value is SQL <code>NULL</code>; <code>false</code>, otherwise.
 * @throws DriverException If the result set or the driver is closed.
 * @throws NullPointerException If the column name is null.
 */
LB_API void lRS_isNull_s(NMParams p) // litebase/ResultSet public native boolean isNull(String colName) throws DriverException, NullPointerException;
{
	TRACE("lRS_isNull_s")
   Object colName = p->obj[1];
   
   MEMORY_TEST_START
   if (colName)
   {
      ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(p->obj[0]);

      if (rsBag)
      {
         if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
            TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
         else
         {
            // juliana@227_14: corrected a DriverException not being thrown when fetching in some cases when trying to fetch data from an invalid 
            // result set column.
		      // juliana@210_1: select * from table_name does not create a temporary table anymore.
            int32 givenColumn = TC_htGet32Inv(&rsBag->intHashtable, identHashCode(colName)),
                  column = rsBag->isSimpleSelect? givenColumn + 1 : givenColumn;

            if (verifyRSState(p->currentContext, rsBag, givenColumn + 1))
               p->retI = isBitSet(rsBag->table->columnNulls[0], column); 
         }
      }
      else // The ResultSet can't be closed.
         TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_RESULTSET_CLOSED));
   }
   else
      TC_throwNullArgumentException(p->currentContext, "colName");
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
/**
 * Gets the number of columns for this <code>ResultSet</code>.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->retI receives the number of columns for this <code>ResultSet</code>.
 * @throws IllegalStateException If the result set or the driver is closed.
 */
LB_API void lRSMD_getColumnCount(NMParams p) // litebase/ResultSetMetaData public native int getColumnCount() throws IllegalStateException;
{
	TRACE("lRSMD_getColumnCount")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
         // juliana@210_1: select * from table_name does not create a temporary table anymore.
		   p->retI = rsBag->isSimpleSelect? rsBag->columnCount - 1 : rsBag->columnCount;
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// juliana@230_28: if a public method receives an invalid argument, now an IllegalArgumentException will be thrown instead of a DriverException.
// litebase/ResultSetMetaData public native int getColumnDisplaySize(int column) throws IllegalStateException, IllegalArgumentException;
/**
 * Given the column index (starting at 1), returns the display size. For chars, it will return the number of chars defined; for primitive types, it 
 * will return the number of decimal places it needs to be displayed correctly. Returns 0 if an error occurs.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->i32[0] The column index (starting at 1).
 * @param p->retI receives the display size or -1 if a problem occurs.
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws IllegalArgumentException If the column index is invalid.
 */
LB_API void lRSMD_getColumnDisplaySize_i(NMParams p) 
{
	TRACE("lRSMD_getColumnDisplaySize_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   
   MEMORY_TEST_START

   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         int32 column = p->i32[0],
               columnCount = rsBag->columnCount;
         bool isSimpleSelect = rsBag->isSimpleSelect;

		   // juliana@213_5: Now a DriverException is thrown instead of returning an invalid value.
		   if (column <= 0 || (isSimpleSelect && column >= columnCount) || (!isSimpleSelect && column > columnCount)) 
            TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalArgumentException", getMessage(ERR_INVALID_COLUMN_NUMBER));
         else
         {
		      if (!isSimpleSelect) // juliana@210_1: select * from table_name does not create a temporary table anymore.
			      column--;

            switch (rsBag->table->columnTypes[column])
            {
               case SHORT_TYPE:  
                  p->retI = 6; 
                  break;
               case INT_TYPE:    
                  p->retI = 11; 
                  break;
               case LONG_TYPE:   
                  p->retI = 20; 
                  break;
               case FLOAT_TYPE:  
                  p->retI = 13; 
                  break;
               case DOUBLE_TYPE: 
                  p->retI = 21; 
                  break;
               case CHARS_TYPE:
               case CHARS_NOCASE_TYPE: 
                  p->retI = rsBag->table->columnSizes[column]; 
                  break;
               case DATE_TYPE: // rnovais@570_12 
                  p->retI = 11; 
                  break; 
               case DATETIME_TYPE: // rnovais@570_12
                  p->retI = 31; // (10 + 19) 
                  break; 
               case BLOB_TYPE:     
                  p->retI = 0; 
            }
         }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// juliana@230_28: if a public method receives an invalid argument, now an IllegalArgumentException will be thrown instead of a DriverException.
// litebase/ResultSetMetaData public native String getColumnLabel(int column) throws IllegalStateException, IllegalArgumentException;
/**
 * Given the column index (starting at 1), returns the column name. Note that if an alias is used to the column, the alias will be returned instead. 
 * If an error occurs, an empty string is returned. Note that LitebaseConnection 2.x tables must be recreated to be able to return this label 
 * information.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->i32[0] The column index (starting at 1).
 * @param p->retO receives the name or alias of the column, which can be an empty string if an error occurs.
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws IllegalArgumentException If the column index is invalid.
 */
LB_API void lRSMD_getColumnLabel_i(NMParams p) 
{
	TRACE("lRSMD_getColumnLabel_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   
   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         int32 column = p->i32[0],
               columnCount = rsBag->columnCount;
         bool isSimpleSelect = rsBag->isSimpleSelect;

		   // juliana@213_5: Now a DriverException is thrown instead of returning an invalid value.
		   if (column <= 0 || (isSimpleSelect && column >= columnCount) || (!isSimpleSelect && column > columnCount)) 
            TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalArgumentException", getMessage(ERR_INVALID_COLUMN_NUMBER));
         else
         {
            CharP* columnNames = rsBag->table->columnNames;

            if (columnNames)

			      // juliana@210_1: select * from table_name does not create a temporary table anymore.
               TC_setObjectLock(p->retO = TC_createStringObjectFromCharP(p->currentContext, columnNames[isSimpleSelect? column: column - 1], -1), 
                                                                                                                                             UNLOCKED);
            
            else
               p->retO = null;
         }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));

   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// juliana@230_28: if a public method receives an invalid argument, now an IllegalArgumentException will be thrown instead of a DriverException.
// litebase/ResultSetMetaData public native int getColumnType(int column) throws IllegalStateException, IllegalArgumentException;
/**
 * Given the column index (starting at 1), returns the column type.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->i32[0] The column index (starting at 1).
 * @param p->retI receives the column type, which can be: <b><code>SHORT_TYPE</b></code>, <b><code>INT_TYPE</b></code>, 
 * <b><code>LONG_TYPE</b></code>, <b><code>FLOAT_TYPE</b></code>, <b><code>DOUBLE_TYPE</b></code>, <b><code>CHAR_TYPE</b></code>, 
 * <b><code>CHAR_NOCASE_TYPE</b></code>, <b><code>DATE_TYPE</b></code>, <b><code>DATETIME_TYPE</b></code>, or <b><code>BLOB_TYPE</b></code>.
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws IllegalArgumentException If the column index is invalid.
 */
LB_API void lRSMD_getColumnType_i(NMParams p) 
{
	TRACE("lRSMD_getColumnType_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   
   MEMORY_TEST_START
   
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         int32 column = p->i32[0],
               columnCount = rsBag->columnCount;
         bool isSimpleSelect = rsBag->isSimpleSelect;

         // juliana@213_5: Now a DriverException is thrown instead of returning an invalid value.
		   if (column <= 0 || (isSimpleSelect && column >= columnCount) || (!isSimpleSelect && column > columnCount)) 
			   TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalArgumentException", getMessage(ERR_INVALID_COLUMN_NUMBER), column);
         else

		   // juliana@210_1: select * from table_name does not create a temporary table anymore.
         p->retI = rsBag->table->columnTypes[isSimpleSelect? column: column - 1];
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/ResultSetMetaData public native String getColumnTypeName(int column) throws DriverException;
/**
 * Given the column index (starting at 1), returns the name of the column type.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->i32[0] The column index (starting at 1).
 * @param p->retO receives the name of the column type, which can be: <b><code>chars</b></code>, <b><code>short</b></code>, <b><code>int</b></code>, 
 * <b><code>long</b></code>, <b><code>float</b></code>, <b><code>double</b></code>, <b><code>date</b></code>, <b><code>datetime</b></code>, 
 * <b><code>blob</b></code>, or null if an error occurs.
 */
LB_API void lRSMD_getColumnTypeName_i(NMParams p) 
{
	TRACE("lRSMD_getColumnTypeName_i")
   CharP ret = "";

   lRSMD_getColumnType_i(p);
   MEMORY_TEST_START

   switch (p->retI)
   {
      case CHARS_TYPE:
      case CHARS_NOCASE_TYPE:
         ret = "chars";
         break;
      case SHORT_TYPE:
         ret = "short";
         break;
      case INT_TYPE:
         ret = "int";
         break;
      case LONG_TYPE:
         ret = "long";
         break;
      case FLOAT_TYPE:
         ret = "float";
         break;
      case DOUBLE_TYPE:
         ret = "double";
         break;
      case DATE_TYPE:
         ret = "date";
         break;
      case DATETIME_TYPE:
         ret = "datetime";
         break;
      case BLOB_TYPE:
         ret = "blob";
         break;
   } 

   TC_setObjectLock(p->retO = TC_createStringObjectFromCharP(p->currentContext, ret, -1), UNLOCKED);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/ResultSetMetaData public native String getColumnTableName(int columnIdx) throws IllegalStateException, IllegalArgumentException;
/**
 * Given the column index, (starting at 1) returns the name of the table it came from.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->i32[0] The column index.
 * @param p->retO receives the name of the table it came from or <code>null</code> if the column index does not exist.
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws IllegalArgumentException If the column index is invalid.
 */
LB_API void lRSMD_getColumnTableName_i(NMParams p) 
{
	TRACE("lRSMD_getColumnTableName_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         SQLResultSetField** fields = rsBag->selectClause->fieldList;
         int32 column = p->i32[0],
               columnCount = rsBag->columnCount;
         bool isSimpleSelect = rsBag->isSimpleSelect;

         p->retO = null;

		   // juliana@213_5: Now a DriverException is thrown instead of returning an invalid value.
		   if (column <= 0 || (isSimpleSelect && column >= columnCount) || (!isSimpleSelect && column > columnCount)) 
			   TC_throwExceptionNamed(context, "java.lang.IllegalArgumentException", getMessage(ERR_INVALID_COLUMN_NUMBER), column);
         else

		      // null is a valid return value.
            TC_setObjectLock(p->retO = fields[--column]->tableName? TC_createStringObjectFromCharP(context, fields[column]->tableName, -1) : null, 
                                                                                                                                             UNLOCKED);
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// litebase/ResultSetMetaData public native String getColumnTableName(String columnName) throws IllegalStateException, DriverException, 
// NullPointerException;
/**
 * Given the column name or alias, returns the name of the table it came from.
 *
 * @param p->obj[0] The result set meta data.
 * @param p->obj[1] The column name.
 * @param p->retO receives the name of the table it came from or <code>null</code> if the column name does not exist.
 * @throws IllegalStateException If the result set or the driver is closed. 
 * @throws DriverException If the column was not found.
 * @throws NullPointerException if the column name is null.
 */
LB_API void lRSMD_getColumnTableName_s(NMParams p) 
{
	TRACE("lRSMD_getColumnTableName_s")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         SQLResultSetField** fields = rsBag->selectClause->fieldList;
         int32 i = -1,
               length = rsBag->selectClause->fieldsCount;
         Object columnNameStr = p->obj[1];

         if (columnNameStr)
         {
            JCharP columnNameJCharP = String_charsStart(columnNameStr);
            int32 columnNameLength = String_charsLen(columnNameStr);
            CharP tableColName,
                  alias,
                  tableName;

            p->retO = null;
            
            while (++i < length) // Gets the name of the table or its alias given the column name.
            {
               if ((((tableColName = fields[i]->tableColName) 
                  && JCharPEqualsCharP(columnNameJCharP, tableColName, columnNameLength, xstrlen(tableColName), true))) 
                 || ((alias = fields[i]->alias) 
                  && JCharPEqualsCharP(columnNameJCharP, alias, columnNameLength, xstrlen(alias), true)))
               {
                  TC_setObjectLock(p->retO = (tableName = fields[i]->tableName)? TC_createStringObjectFromCharP(context, tableName, -1) : null, 
                                                                                                                                          UNLOCKED);
                  break;
               }
            }
            if (i == length) // Column name or alias not found.
            {
               CharP columnNameCharP = TC_JCharP2CharP(columnNameJCharP, columnNameLength);
               TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_COLUMN_NOT_FOUND), columnNameCharP? columnNameCharP : "");
               xfree(columnNameCharP);
            }
         }
         else // The column name can't be null.
            TC_throwNullArgumentException(context, "columnName");
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

// juliana@227_2: added methods to indicate if a column of a result set is not null or has default values.
//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// juliana@227_2: added methods to indicate if a column of a result set is not null or has default values.
// litebase/ResultSetMetaData public native boolean hasDefaultValue(int columnIndex) throws IllegalStateException, DriverException;
/**
 * Indicates if a column of the result set has default value.
 * 
 * @param p->i32[0] The column index.
 * @param p->retI receives <code>true</code> if the column has a default value; <code>false</code>, otherwise. 
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws DriverException If the column does not have an underlining table.
 */
LB_API void lRSMD_hasDefaultValue_i(NMParams p) 
{
   TRACE("lRSMD_hasDefaultValue_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         Object nameObj;
         char nameCharP[DBNAME_SIZE];

         lRSMD_getColumnTableName_i(p);
         if ((nameObj = p->retO))
         {
            Table* table;
            
            // Gets the table column info.
            TC_JCharP2CharPBuf(String_charsStart(nameObj), String_charsLen(nameObj), nameCharP);
            if ((table = getTable(context, rsBag->driver, nameCharP)))
            {
               SQLResultSetField* field = rsBag->selectClause->fieldList[p->i32[0] - 1];
               p->retI = (table->columnAttrs[field->tableColIndex < 129? field->tableColIndex : field->parameter->tableColIndex] 
                                                                                             & ATTR_COLUMN_HAS_DEFAULT) != 0;
            }
         }
         else
         {
            IntBuf buffer;
            TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_COLUMN_NOT_FOUND), TC_int2str(p->i32[0], buffer)); 
         }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// litebase/ResultSetMetaData public native boolean hasDefaultValue(String columnName) throws IllegalStateException, DriverException, 
// NullPointerException;
/**
 * Indicates if a column of the result set has default value.
 * 
 * @param p->obj[1] The column name.
 * @param p->retI receives <code>true</code> if the column has a default value; <code>false</code>, otherwise. 
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws DriverException If the column was not found or does not have an underlining table.
 * @throws NullPointerException if the column name is null.
 */
LB_API void lRSMD_hasDefaultValue_s(NMParams p) 
{
   TRACE("lRSMD_hasDefaultValue_s")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         Object columnNameStr = p->obj[1];

         if (columnNameStr)
         {
            SQLResultSetField** fields = rsBag->selectClause->fieldList; 
            SQLResultSetField* field; 
            JCharP columnNameJCharP = String_charsStart(columnNameStr);
            int32 i = -1,
                  length = rsBag->selectClause->fieldsCount,
                  columnNameLength = String_charsLen(columnNameStr);
            CharP tableColName,
                  alias;

            p->retO = null;
            
            while (++i < length) // Gets the name of the table or its alias given the column name.
            {
               if ((((tableColName = (field = fields[i])->tableColName) 
                  && JCharPEqualsCharP(columnNameJCharP, tableColName, columnNameLength, xstrlen(tableColName), true))) 
                 || ((alias = fields[i]->alias) 
                  && JCharPEqualsCharP(columnNameJCharP, alias, columnNameLength, xstrlen(alias), true)))
               {
                  if (field->tableName)
                  {
                     Table* table;
                     if ((table = getTable(context, rsBag->driver, field->tableName)))
                        p->retI = (table->columnAttrs[field->tableColIndex < 129? field->tableColIndex : field->parameter->tableColIndex] 
                                                                                                      & ATTR_COLUMN_HAS_DEFAULT) != 0;
                  }
                  else
                     i = length;
                  break;
               }
            }
            if (i == length) // Column name or alias not found.
            {
               CharP columnNameCharP = TC_JCharP2CharP(columnNameJCharP, columnNameLength);
               TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_COLUMN_NOT_FOUND), columnNameCharP? columnNameCharP : "");
               xfree(columnNameCharP);
            }
         }
         else // The column name can't be null.
            TC_throwNullArgumentException(context, "columnName");
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// litebase/ResultSetMetaData public native boolean isNotNull(int columnIndex) throws IllegalStateException, DriverException;
/**
 * Indicates if a column of the result set is not null.
 * 
 * @param p->i32[0] The column index.
 * @param p->retI receives <code>true</code> if the column is not null; <code>false</code>, otherwise. 
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws DriverException If the column does not have an underlining table.
 */
LB_API void lRSMD_isNotNull_i(NMParams p) 
{
   TRACE("lRSMD_isNotNull_i")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         Object nameObj;
         char nameCharP[DBNAME_SIZE];

         lRSMD_getColumnTableName_i(p);
         if ((nameObj = p->retO))
         {
            Table* table;
            
            // Gets the table column info.
            TC_JCharP2CharPBuf(String_charsStart(nameObj), String_charsLen(nameObj), nameCharP);
            if ((table = getTable(context, rsBag->driver, nameCharP)))
            {
               SQLResultSetField* field = rsBag->selectClause->fieldList[p->i32[0] - 1];
               p->retI = (table->columnAttrs[field->tableColIndex < 129? field->tableColIndex : field->parameter->tableColIndex] 
                                                                                             & ATTR_COLUMN_IS_NOT_NULL) != 0;
            }
         }
         else
         {
            IntBuf buffer;
            TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_COLUMN_NOT_FOUND), TC_int2str(p->i32[0], buffer)); 
         }
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
// DriverException.
// litebase/ResultSetMetaData public native boolean isNotNull(String columnName) throws IllegalStateException, DriverException, 
// NullPointerException;
/**
 * Indicates if a column of the result set is not null.
 * 
 * @param p->obj[1] The column name.
 * @param p->retI receives <code>true</code> if the column is not null; <code>false</code>, otherwise. 
 * @throws IllegalStateException If the result set or the driver is closed.
 * @throws DriverException If the column was not found or does not have an underlining table.
 * @throws NullPointerException if the column name is null.
 */
LB_API void lRSMD_isNotNull_s(NMParams p) 
{
   TRACE("lRSMD_isNotNull_s")
   ResultSet* rsBag = (ResultSet*)OBJ_ResultSetBag(OBJ_ResultSetMetaData_ResultSet(p->obj[0]));
   Context context = p->currentContext;

   MEMORY_TEST_START
   if (rsBag)
   {
      if (OBJ_LitebaseDontFinalize(rsBag->driver)) // juliana@227_4: the connection where the result set was created can't be closed while using it.
         TC_throwExceptionNamed(p->currentContext, "java.lang.IllegalStateException", getMessage(ERR_DRIVER_CLOSED));
      else
      {
         Object columnNameStr = p->obj[1];

         if (columnNameStr)
         {
            SQLResultSetField** fields = rsBag->selectClause->fieldList; 
            SQLResultSetField* field; 
            JCharP columnNameJCharP = String_charsStart(columnNameStr);
            int32 i = -1,
                  length = rsBag->selectClause->fieldsCount,
                  columnNameLength = String_charsLen(columnNameStr);
            CharP tableColName,
                  alias;

            p->retO = null;
            
            while (++i < length) // Gets the name of the table or its alias given the column name.
            {
               if ((((tableColName = (field = fields[i])->tableColName) 
                  && JCharPEqualsCharP(columnNameJCharP, tableColName, columnNameLength, xstrlen(tableColName), true))) 
                 || ((alias = fields[i]->alias) 
                  && JCharPEqualsCharP(columnNameJCharP, alias, columnNameLength, xstrlen(alias), true)))
               {
                  if (field->tableName)
                  {
                     Table* table;
                     if ((table = getTable(context, rsBag->driver, field->tableName)))
                        p->retI = (table->columnAttrs[field->tableColIndex < 129? field->tableColIndex : field->parameter->tableColIndex] 
                                                                                                      & ATTR_COLUMN_IS_NOT_NULL) != 0;          
                  }
                  else
                     i = length;
                  break;
               }
            }
            if (i == length) // Column name or alias not found.
            {
               CharP columnNameCharP = TC_JCharP2CharP(columnNameJCharP, columnNameLength);
               TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_COLUMN_NOT_FOUND), columnNameCharP? columnNameCharP : "");
               xfree(columnNameCharP);
            }
         }
         else // The column name can't be null.
            TC_throwNullArgumentException(context, "columnName");
      }
   }
   else // The ResultSet can't be closed.
      TC_throwExceptionNamed(context, "java.lang.IllegalStateException", getMessage(ERR_RESULTSETMETADATA_CLOSED));
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// guich@550_43: fixed problem when reusing the statement.
// litebase/PreparedStatement public native litebase.ResultSet executeQuery() throws DriverException, OutOfMemoryError;
/**
 * This method executes a prepared SQL query and returns its <code>ResultSet</code>.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->retO receives the <code>ResultSet</code> of the SQL statement.
 * @throws DriverException If the statement to be execute is not a select, there are undefined parameters or the driver is closed.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lPS_executeQuery(NMParams p) 
{
	TRACE("lPS_executeQuery")
	Object stmt = p->obj[0],
          logger = litebaseConnectionClass->objStaticValues[1],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;

   MEMORY_TEST_START
 
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else if (OBJ_PreparedStatementType(stmt) != CMD_SELECT) // The statement must be a select.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_QUERY_DOESNOT_RETURN_RESULTSET));
   else 
   {
      SQLSelectStatement* selectStmt = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt)); // The select statement.

      if (!allParamValuesDefinedSel(selectStmt)) // All the parameters of the select statement must be defined.
         TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_NOT_ALL_PARAMETERS_DEFINED));
      else
      {
         MemoryUsageEntry* memUsageEntry;
         ResultSet* resultSetBag;
         SQLSelectClause* selectClause = selectStmt->selectClause;
         Heap heap = selectClause->heap;
         bool locked = false;
         PlainDB* plainDB;
       
         if (logger) // If log is on, adds information to it.
         { 
            LOCKVAR(log);
            TC_executeMethod(context, loggerLog, logger, 16, toString(context, stmt, true), false);
            UNLOCKVAR(log);
            if (context->thrownException)
               goto finish;
         }

	      resetWhereClause(selectStmt->whereClause, heap);

         IF_HEAP_ERROR(heap)
         {
            TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
            goto finish;
         }

         // guich@554_37: tableColIndex may change between runs of a prepared statement with a sort field so we have to cache the tableColIndex of 
         // the order by fields.
         resetColumnListClause(selectStmt->orderByClause);

         // juliana@226_14: corrected a bug that would make a prepared statement with group by not work correctly after the first execution.
         resetColumnListClause(selectStmt->groupByClause);

         selectClause->isPrepared = true;
         TC_setObjectLock(p->retO = litebaseDoSelect(context, driver, selectStmt), UNLOCKED);

         if (p->retO)
         {
            // Gets the query result table size and stores it.
            IF_HEAP_ERROR(hashTablesHeap)
            {
               if (locked)
                  UNLOCKVAR(parser);
               TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
               goto finish;
            }
            locked = true;
	         LOCKVAR(parser);
            if (!(memUsageEntry = TC_htGetPtr(&memoryUsage, selectStmt->selectClause->sqlHashCode)))
            {
               memUsageEntry = (MemoryUsageEntry*)TC_heapAlloc(hashTablesHeap, sizeof(MemoryUsageEntry));
               TC_htPutPtr(&memoryUsage, selectStmt->selectClause->sqlHashCode, memUsageEntry);
            }
            resultSetBag = (ResultSet*)OBJ_ResultSetBag(p->retO);
            memUsageEntry->dbSize = (plainDB = resultSetBag->table->db)->db.size;
            memUsageEntry->dboSize = plainDB->dbo.size;
            TC_htPutPtr(&memoryUsage, selectClause->sqlHashCode, memUsageEntry);
	         UNLOCKVAR(parser);
	         locked = false;
         }
      }
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * This method executes a SQL <code>INSERT</code>, <code>UPDATE</code>, or <code>DELETE</code> statement. SQL statements that return nothing such as
 * SQL DDL statements can also be executed.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->retI receives the result is either the row count for <code>INSERT</code>, <code>UPDATE</code>, or <code>DELETE</code> statements; or 0 
 * for SQL statements that return nothing.
 * @throws DriverException If the query does not update the table, there are undefined parameters or the driver is closed.
 */
LB_API void lPS_executeUpdate(NMParams p) // litebase/PreparedStatement public native int executeUpdate() throws DriverException;
{
	TRACE("lPS_executeUpdate")
   Object stmt = p->obj[0],
          logger = litebaseConnectionClass->objStaticValues[1],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;

   MEMORY_TEST_START

   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else if (OBJ_PreparedStatementType(stmt) == CMD_SELECT) // The statement must be a select.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_QUERY_DOESNOT_PERFORM_UPDATE));
   else 
   {
      if (logger) // If log is on, adds information to it.
      {
         LOCKVAR(log);
         TC_executeMethod(context, loggerLog, logger, 16, toString(context, stmt, true), false);
         UNLOCKVAR(log);
         if (context->thrownException)
            goto finish;
      }
     
      // juliana@226_15: corrected a bug that would make a prepared statement with where clause and indices not work correctly after the first 
      // execution.
      switch (OBJ_PreparedStatementType(stmt)) // Returns the number of rows affected or if the command was successfully executed.
      {
         case CMD_INSERT:
         {
            SQLInsertStatement* insertStmt = (SQLInsertStatement*)(OBJ_PreparedStatementStatement(stmt));
			   
            rearrangeNullsInTable(insertStmt->table, insertStmt->record, insertStmt->storeNulls, insertStmt->paramDefined, 
                                                     insertStmt->paramIndexes, insertStmt->nFields, insertStmt->paramCount);
            if (convertStringsToValues(context, insertStmt->table, insertStmt->record, insertStmt->nFields))
               p->retI = litebaseDoInsert(context, insertStmt);
            break;
         }
         case CMD_UPDATE:
         {
            SQLUpdateStatement* updateStmt = (SQLUpdateStatement*)(OBJ_PreparedStatementStatement(stmt));
         
            resetWhereClause(updateStmt->whereClause, updateStmt->heap); // guich@554_13            
            rearrangeNullsInTable(updateStmt->rsTable->table, updateStmt->record, updateStmt->storeNulls, updateStmt->paramDefined, 
                                                              updateStmt->paramIndexes, updateStmt->nValues, updateStmt->paramCount); 
            if (allParamValuesDefinedUpd(updateStmt) 
            && convertStringsToValues(context, updateStmt->rsTable->table, updateStmt->record, updateStmt->nValues))
               p->retI = litebaseDoUpdate(context, updateStmt);
            break;
         }
         case CMD_DELETE:
         {
            SQLDeleteStatement* deleteStmt = (SQLDeleteStatement*)(OBJ_PreparedStatementStatement(stmt));
            
            resetWhereClause(deleteStmt->whereClause, deleteStmt->heap); // guich@554_13
            if (allParamValuesDefinedDel(deleteStmt))
               p->retI = litebaseDoDelete(context, deleteStmt);
            break;
         }
         case CMD_CREATE_TABLE:
         {
            Object sqlExpression = OBJ_PreparedStatementSqlExpression(stmt);
            
            litebaseExecute(context, driver, String_charsStart(sqlExpression), String_charsLen(sqlExpression));
            p->retI = 0;
            break;
         }
         default:
         {
            Object sqlExpression = OBJ_PreparedStatementSqlExpression(stmt);
            
            p->retI = litebaseExecuteUpdate(context, driver, String_charsStart(sqlExpression), String_charsLen(sqlExpression));
         }
      }
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setShort(int index, short value);
/**
 * This method sets the specified parameter from the given Java <code>short</code> value.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->i32[1] The value of the parameter.
 */
LB_API void lPS_setShort_is(NMParams p) 
{
	TRACE("lPS_setShort_is")
   MEMORY_TEST_START
   psSetNumericParamValue(p, SHORT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setInt(int index, int value);
/**
 * This method sets the specified parameter from the given Java <code>int</code> value.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->i32[1] The value of the parameter.   
 */
LB_API void lPS_setInt_ii(NMParams p) 
{
	TRACE("lPS_setInt_ii")
	MEMORY_TEST_START
   psSetNumericParamValue(p, INT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setLong(int index, long value);
/**
 * This method sets the specified parameter from the given Java <code>long</code> value.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->i64[0] The value of the parameter.
 */
LB_API void lPS_setLong_il(NMParams p) 
{
	TRACE("lPS_setLong_il")
   MEMORY_TEST_START
   psSetNumericParamValue(p, LONG_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setFloat(int index, float value);
/**
 * This method sets the specified parameter from the given Java <code>float</code> value.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->dbl[0] The value of the parameter.
 */
LB_API void lPS_setFloat_id(NMParams p) 
{
	TRACE("lPS_setFloat_id")
   MEMORY_TEST_START
   psSetNumericParamValue(p, FLOAT_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setDouble(int index, double value);
/**
 * This method sets the specified parameter from the given Java <code>double</code> value.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->dbl[0] The value of the parameter.
 */
LB_API void lPS_setDouble_id(NMParams p) 
{
	TRACE("lPS_setDouble_id")
   MEMORY_TEST_START
   psSetNumericParamValue(p, DOUBLE_TYPE);
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
 // litebase/PreparedStatement public native void setString(int index, String value) throws DriverException, OutOfMemoryError;
/**
 * This method sets the specified parameter from the given Java <code>String</code> value.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->obj[1] The value of the parameter. DO NOT SURROUND IT WITH '!.
 * @throws DriverException If the driver is closed.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lPS_setString_is(NMParams p)
{
	TRACE("lPS_setString_is")
	Object stmt = p->obj[0],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;
 
   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      SQLSelectStatement* statement = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt));
      
      if (statement) // Only sets the parameter if the statement is not null.
      {
         Object string = p->obj[1];
         Object* objParams = (Object*)ARRAYOBJ_START(OBJ_PreparedStatementObjParams(stmt));
         JCharP stringChars = null;
         int32 index = p->i32[0],
               stringLength = 0;
      
         if (string)
         {
            stringChars = String_charsStart(string);
            stringLength = String_charsLen(string);
         }
         
         switch (statement->type) // Sets the parameter.
         {
            case CMD_DELETE:
               if (!setParamValueStringDel(context, (SQLDeleteStatement*)statement, index, stringChars, stringLength))
                  goto finish;
               break;
            case CMD_INSERT:
               if (!setStrBlobParamValueIns(context, (SQLInsertStatement*)statement, index, stringChars, stringLength, true))
                  goto finish;
               break;
            case CMD_SELECT:
               if (!setParamValueStringSel(context, statement, index, stringChars, stringLength))
                  goto finish;
               break;
            case CMD_UPDATE:
               if (!setStrBlobParamValueUpd(context, (SQLUpdateStatement*)statement, index, stringChars, stringLength, true))
                  goto finish;
         }

         objParams[index] = p->obj[1]; // juliana@222_8: stores the object so that it won't be collected.

         if (OBJ_PreparedStatementStoredParams(stmt)) // Only stores the parameter if there are parameters to be stored.
         {
            JCharP* paramsAsStrs = (JCharP*)OBJ_PreparedStatementParamsAsStrs(stmt);
            JCharP paramAsStr = paramsAsStrs[index];
            int32* paramsLength = (int32*)OBJ_PreparedStatementParamsLength(stmt);

            if (string) // The parameter is not null.
            {
               if (stringLength + 2 > paramsLength[index]) // Reuses the buffer whenever possible
               {
                  xfree(paramAsStr);
                  if (!(paramAsStr = paramsAsStrs[index] = (JCharP)xmalloc((stringLength + 3) << 1)))      
                  {
                     TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                     goto finish;
                  }
                  paramsLength[index] = stringLength + 2;
               }
               paramAsStr[0] = '\'';
               xmemmove(&paramAsStr[1], stringChars, stringLength << 1);
               paramAsStr[stringLength + 1] = '\'';
               paramAsStr[stringLength + 2] = 0;
            }
            else // The parameter is null;
               TC_CharP2JCharPBuf("null", 4, paramAsStr, true);
         }
      }
   }
   
finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setBlob(int index, uint8 []value) throws DriverException, SQLParseException;
/**
 * This method sets the specified parameter from the given array of bytes as a blob.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->obj[1] The value of the parameter.
 * @throws SQLParseException If the parameter to be set is in the where clause.
 * @throws DriverException If the driver is closed.
 */
LB_API void lPS_setBlob_iB(NMParams p) 
{
	TRACE("lPS_setBlob_iB")
	Object stmt = p->obj[0],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;
 
   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      SQLSelectStatement* statement = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt));
      
      if (statement) // Only sets the parameter if the statement is not null.
      {
         Object blob = p->obj[1];
         Object* objParams = (Object*)ARRAYOBJ_START(OBJ_PreparedStatementObjParams(stmt));
         uint8* blobArray = null;
         int32 index = p->i32[0],
               blobLength = 0;
  
         if (blob != null)
         {
            blobLength = ARRAYOBJ_LEN(p->obj[1]);
            blobArray = (uint8*)ARRAYOBJ_START(p->obj[1]);
         }
        
         switch (statement->type) // Sets the parameter.
         {
            case CMD_INSERT:
               if (!setStrBlobParamValueIns(context, (SQLInsertStatement*)statement, index, blobArray, blobLength, false))
                  goto finish;
               break;
            case CMD_UPDATE:
               if (!setStrBlobParamValueUpd(context, (SQLUpdateStatement*)statement, index, blobArray, blobLength, false))
                  goto finish;
               break;

            // A blob can't be used in a where clause.
            case CMD_SELECT:
            case CMD_DELETE:
               TC_throwExceptionNamed(context, "litebase.SQLParseException", getMessage(ERR_BLOB_WHERE));
               goto finish;
         }

         objParams[index] = p->obj[1]; // juliana@222_8: stores the object so that it won't be collected.

         if (OBJ_PreparedStatementStoredParams(stmt)) // Only stores the parameter if there are parameters to be stored.
         {
            JCharP* paramsAsStrs = (JCharP*)OBJ_PreparedStatementParamsAsStrs(stmt);

            if (blob) // The parameter is not null.
               TC_CharP2JCharPBuf("[BLOB]", 6, paramsAsStrs[index], true);
            else // The parameter is null;
               TC_CharP2JCharPBuf("null", 4, paramsAsStrs[index], true);
         }
      }
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setDate(int index, totalcross.Util.Date) throws DriverException, OutOfMemoryError;
/**
 * This method sets the specified parameter from the given Java <code>Date</code> value formated as "YYYY/MM/DD" <br>
 * <b>IMPORTANT</b>: The constructor <code>new Date(string_date)</code> must be used with care. Some devices can construct different dates, according
 * to the device's date format. For example, the constructor <code>new Date("12/09/2006")</code>, depending on the device's date format, can generate 
 * a date like "12 of September of 2006" or "09 of December of 2006". To avoid this, use the constructor
 * <code>new Date(string_date, totalcross.sys.Settings.DATE_XXX)</code> instead, where <code>totalcross.sys.Settings.DATE_XXX</code> is a date format 
 * parameter that must be one of the <code>totalcross.sys.Settings.DATE_XXX</code> constants.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->obj[1] The value of the parameter.
 * @throws DriverException If the driver is closed.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lPS_setDate_id(NMParams p) 
{
	TRACE("lPS_setDate_id")
	Object stmt = p->obj[0],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;
 
   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      SQLSelectStatement* statement = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt));
      
      if (statement) // Only sets the parameter if the statement is not null.
      {
   	   Object date = p->obj[1];
         Object* objParams = (Object*)ARRAYOBJ_START(OBJ_PreparedStatementObjParams(stmt));
         JCharP stringChars = null;
         int32 index = p->i32[0],
               stringLength = 0;

         if (date)
         {
		      Object dateBufObj = objParams[index];
            char dateBuf[11]; 

            if (!dateBufObj || String_charsLen(dateBufObj) < 10)
            {
               if (!(dateBufObj = TC_createStringObjectWithLen(context, 11)))
		            goto finish;
               TC_setObjectLock(dateBufObj, UNLOCKED);
               objParams[index] = dateBufObj; // juliana@222_8: stores the object so that it won't be collected.
            }
		      xstrprintf(dateBuf, "%04d/%02d/%02d", FIELD_I32(date, 2), FIELD_I32(date, 1), FIELD_I32(date, 0)); 
		      TC_CharP2JCharPBuf(dateBuf, stringLength = 10, stringChars = String_charsStart(dateBufObj), true);
         }
           
         switch (statement->type) // Sets the parameter.
         {
            case CMD_DELETE:
               if (!setParamValueStringDel(context, (SQLDeleteStatement*)statement, index, stringChars, stringLength))
                  goto finish;
               break;
            case CMD_INSERT:
               if (!setStrBlobParamValueIns(context, (SQLInsertStatement*)statement, index, stringChars, stringLength, true))
                  goto finish;
               break;
            case CMD_SELECT:
               if (!setParamValueStringSel(context, statement, index, stringChars, stringLength))
                  goto finish;
               break;
            case CMD_UPDATE:
               if (!setStrBlobParamValueUpd(context, (SQLUpdateStatement*)statement, index, stringChars, stringLength, true))
                  goto finish;
               break;
         }

         if (OBJ_PreparedStatementStoredParams(stmt)) // Only stores the parameter if there are parameters to be stored.
         {
            JCharP* paramsAsStrs = (JCharP*)OBJ_PreparedStatementParamsAsStrs(stmt);
            JCharP paramAsStr = paramsAsStrs[index];
            int32* paramsLength = (int32*)OBJ_PreparedStatementParamsLength(stmt);

            if (date) // The parameter is not null.
            {
               if (stringLength + 2 > paramsLength[index]) // Reuses the buffer whenever possible
               {
                  xfree(paramAsStr);
                  if (!(paramAsStr = paramsAsStrs[index] = (JCharP)xmalloc((stringLength + 3) << 1)))      
                  {
                     TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                     goto finish;
                  }
                  paramsLength[index] = stringLength + 2;
               }
               paramAsStr[0] = '\'';
               xmemmove(&paramAsStr[1], stringChars, stringLength << 1);
               paramAsStr[stringLength + 1] = '\'';
               paramAsStr[stringLength + 2] = 0;
            }
            else // The parameter is null;
               TC_CharP2JCharPBuf("null", 4, paramAsStr, true);
         }
      }
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * This method sets the specified parameter from the given Java <code>DateTime</code> value formated as "YYYY/MM/DD HH:MM:SS:ZZZ". <br>
 * <b>IMPORTANT</b>: The constructor <code>new Date(string_date)</code> must be used with care. Some devices can construct different dates, according 
 * to the device's date format. For example, the constructor <code>new Date("12/09/2006")</code>, depending on the device's date format, can generate 
 * a date like "12 of September of 2006" or "09 of December of 2006". To avoid this, use the constructor 
 * <code>new Date(string_date, totalcross.sys.Settings.DATE_XXX)</code> instead, where <code>totalcross.sys.Settings.DATE_XXX</code> is a date format 
 * parameter that must be one of the <code>totalcross.sys.Settings.DATE_XXX</code> constants.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->obj[1] The value of the parameter.
 */
LB_API void lPS_setDateTime_id(NMParams p) // litebase/PreparedStatement public native void setDate(int index, totalcross.Util.Date);
{
	TRACE("lPS_setDateTime_id")
   lPS_setDate_id(p);
}

//////////////////////////////////////////////////////////////////////////
// litebase/PreparedStatement public native void setDateTime(int index, totalcross.sys.Time) throws DriverException, OutOfMemoryError;
/**
 * Formats the <code>Time</code> t into a string "YYYY/MM/DD HH:MM:SS:ZZZ"
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set, starting from 0.
 * @param p->obj[1] The value of the parameter.
 * @throws DriverException If the driver is closed.
 * @throws OutOfMemoryError If a memory allocation fails.
 */
LB_API void lPS_setDateTime_it(NMParams p) 
{
	TRACE("lPS_setDateTime_it")
	Object stmt = p->obj[0],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;
 
   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      SQLSelectStatement* statement = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt));
      
      if (statement) // Only sets the parameter if the statement is not null.
      {
   	   Object time = p->obj[1];
         Object* objParams = (Object*)ARRAYOBJ_START(OBJ_PreparedStatementObjParams(stmt));
         JCharP stringChars = null;
         int32 index = p->i32[0],
               stringLength = 0;

         if (time)
         {
		      Object dateTimeBufObj = objParams[index];
            char dateTimeBuf[24]; 

            if (!dateTimeBufObj || String_charsLen(dateTimeBufObj) < 23)
            {
               if (!(dateTimeBufObj = TC_createStringObjectWithLen(context, 23)))
		            goto finish;
               TC_setObjectLock(dateTimeBufObj, UNLOCKED);
               objParams[index] = dateTimeBufObj; // juliana@222_8: stores the object so that it won't be collected.
            }
		      xstrprintf(dateTimeBuf, "%04d/%02d/%02d", Time_year(time), Time_month(time), Time_day(time)); 
            xstrprintf(&dateTimeBuf[11], "%02d:%02d:%02d:%03d", Time_hour(time), Time_minute(time), Time_second(time), Time_millis(time));
		      dateTimeBuf[10] = ' ';
		      TC_CharP2JCharPBuf(dateTimeBuf, stringLength = 23, stringChars = String_charsStart(dateTimeBufObj), true);
         }
           
         switch (statement->type) // Sets the parameter.
         {
            case CMD_DELETE:
               if (!setParamValueStringDel(context, (SQLDeleteStatement*)statement, index, stringChars, stringLength))
                  goto finish;
               break;
            case CMD_INSERT:
               if (!setStrBlobParamValueIns(context, (SQLInsertStatement*)statement, index, stringChars, stringLength, true))
                  goto finish;
               break;
            case CMD_SELECT:
               if (!setParamValueStringSel(context, statement, index, stringChars, stringLength))
                  goto finish;
               break;
            case CMD_UPDATE:
               if (!setStrBlobParamValueUpd(context, (SQLUpdateStatement*)statement, index, stringChars, stringLength, true))
                  goto finish;
               break;
         }

         if (OBJ_PreparedStatementStoredParams(stmt)) // Only stores the parameter if there are parameters to be stored.
         {
            JCharP* paramsAsStrs = (JCharP*)OBJ_PreparedStatementParamsAsStrs(stmt);
            JCharP paramAsStr = paramsAsStrs[index];
            int32* paramsLength = (int32*)OBJ_PreparedStatementParamsLength(stmt);

            if (time) // The parameter is not null.
            {
               if (stringLength + 2 > paramsLength[index]) // Reuses the buffer whenever possible
               {
                  xfree(paramAsStr);
                  if (!(paramAsStr = paramsAsStrs[index] = (JCharP)xmalloc((stringLength + 3) << 1)))      
                  {
                     TC_throwExceptionNamed(context, "java.lang.OutOfMemoryError", null);
                     goto finish;
                  }
                  paramsLength[index] = stringLength + 2;
               }
               paramAsStr[0] = '\'';
               xmemmove(&paramAsStr[1], stringChars, stringLength << 1);
               paramAsStr[stringLength + 1] = '\'';
               paramAsStr[stringLength + 2] = 0;
            }
            else // The parameter is null;
               TC_CharP2JCharPBuf("null", 4, paramAsStr, true);
         }
      }
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
// juliana@223_3: PreparedStatement.setNull() now works for blobs.
// litebase/PreparedStatement public native void setNull(int index) throws DriverException, SQLParseException;
/**
 * Sets null in a given field. This can be used to set any column type as null. It must be just remembered that a parameter in a where clause can't 
 * be set to null.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->i32[0] The index of the parameter value to be set as null, starting from 0.
 * @throws DriverException If the driver is closed.]
 * @throws SQLParseException If the parameter to be set as null is in the where clause.
 */
LB_API void lPS_setNull_i(NMParams p) 
{
	TRACE("lPS_setNull_i")
	Object stmt = p->obj[0],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;
 
   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      SQLSelectStatement* statement = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt));
      
      if (statement) // Only sets the parameter if the statement is not null.
      {
         int32 index = p->i32[0];

         switch (statement->type)
         {
            case CMD_INSERT:
               if (!setNullIns(context, (SQLInsertStatement*)statement, index))
                  goto finish;
               break;
            case CMD_DELETE:
            case CMD_SELECT:
               TC_throwExceptionNamed(context, "litebase.SQLParseException", getMessage(ERR_PARAM_NULL)); 
               goto finish;
               break;
            case CMD_UPDATE:
               if (!setNullUpd(context, (SQLUpdateStatement*)statement, index))
                  goto finish;
               break;
         }
         if (OBJ_PreparedStatementStoredParams(stmt))
            TC_CharP2JCharPBuf("null", 4, ((JCharP*)OBJ_PreparedStatementParamsAsStrs(stmt))[index], true);
      }
   }

finish: ;
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * This method clears all of the input parameters that have been set on this statement.
 * 
 * @param p->obj[0] The prepared statement.
 * @throws DriverException If the driver is closed.
 */
LB_API void lPS_clearParameters(NMParams p) // litebase/PreparedStatement public native void clearParamValues();
{
	TRACE("lPS_clearParameters")
   Object stmt = p->obj[0],
          driver = OBJ_PreparedStatementDriver(stmt);
   Context context = p->currentContext;
 
   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(stmt)) // Prepared Statement Closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(driver)) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(context, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
   {
      SQLSelectStatement* statement = (SQLSelectStatement*)(OBJ_PreparedStatementStatement(stmt));
      
      if (statement) // Only clears the parameter if the statement is not null.
      {
         int32 length = OBJ_PreparedStatementStoredParams(stmt);

         if (length)
         {
            JCharP* paramsAsStrs = ((JCharP*)OBJ_PreparedStatementParamsAsStrs(stmt));
            
            while (--length >= 0)
               TC_CharP2JCharPBuf("unfilled", 8, paramsAsStrs[length], true);
         }

         switch (statement->type)
         {
            case CMD_DELETE:
               clearParamValuesDel((SQLDeleteStatement*)statement);
               break;
            case CMD_INSERT:
               clearParamValuesIns((SQLInsertStatement*)statement);
               break;
            case CMD_SELECT:
               clearParamValuesSel(statement);
               break;
            case CMD_UPDATE:
               clearParamValuesUpd((SQLUpdateStatement*)statement);
               break;
         }
      }
   }
   MEMORY_TEST_END
}

//////////////////////////////////////////////////////////////////////////
/**
 * Returns the sql used in this statement. If logging is disabled, returns the sql without the arguments. If logging is enabled, returns the real 
 * sql, filled with the arguments.
 *
 * @param p->obj[0] The prepared statement.
 * @param p->obj[0] receives the sql used in this statement.
 */
LB_API void lPS_toString(NMParams p) // litebase/PreparedStatement public native String toString();
{
	TRACE("lPS_toString")
   Object statement = p->obj[0];

   MEMORY_TEST_START
   if (OBJ_PreparedStatementDontFinalize(statement)) // Prepared Statement Closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_PREPARED_STMT_CLOSED));
   else if (OBJ_LitebaseDontFinalize(OBJ_PreparedStatementDriver(statement))) // The connection with Litebase can't be closed.
      TC_throwExceptionNamed(p->currentContext, "litebase.DriverException", getMessage(ERR_DRIVER_CLOSED));
   else
      TC_setObjectLock(p->retO = toString(p->currentContext, statement, false), UNLOCKED);
   MEMORY_TEST_END
}

// juliana@230_19: removed some possible memory problems with prepared statements and ResultSet.getStrings().
