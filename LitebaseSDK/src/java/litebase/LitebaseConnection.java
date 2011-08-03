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

import totalcross.io.*;
import totalcross.sys.*;
import totalcross.util.*;

/**
 * This class is the one used to issue SQL commands. Read Litebase Companion chapters for more information.
 */
public class LitebaseConnection
{ 
   /**
    * English language.
    */
   public static final int LANGUAGE_EN = 1;

   // guich@223_10: the Litebase version is not declared as final anymore, otherwise the compiler replaces the constant by the value.
   /**
    * Portuguese language.
    */
   public static final int LANGUAGE_PT = 2;

   /**
    * The string corresponding to the current Litebase version.
    */
   public static String versionStr = "2.28";

   /**
    * The integer corresponding to the current Litebase version.
    */
   public static int version = 228;

   /** 
    * The maximum time (in seconds) that will be taken to sort a table before creating an index. Defaults to 20 seconds on <code>JavaSE</code> and
    * 40 seconds on other platforms.
    */
   static int indexSortMaxTime = Settings.onJavaSE? 20 : 40;
   
   /**
    * The key which identifies one Litebase connection instance.
    */
   private int key;

   /**
    * A hash table containing the connections to Litebase in use.
    */
   static Hashtable htDrivers = new Hashtable(10);

   /**
    * The logger.
    */
   public static Logger logger;

   // juliana@211_1: language is now a public field. It must be accessed directly.
   /**
    * The language of the Litebase messages.
    */
   public static int language = LANGUAGE_EN;

   /**
    * Given the table name, returns the Table object.
    */
   Hashtable htTables;

   /** 
    * A hash table of prepared statements.
    */
   private Hashtable htPS = new Hashtable(30); // guich@201_3
   
   /**
    * The creator id for the tables managed by Litebase.
    */
   String appCrid;

   /**
    * The source path, where the tables will be stored.
    */
   String sourcePath;
   
   /**
    * Indicates that the object can be collected.
    */
   boolean dontFinalize;
   
   /**
    * Indicates if the tables of this connection uses ascii or unicode strings.
    */
   private boolean isAscii; // juliana@210_2: now Litebase supports tables with ascii strings.
   
   // juliana@224_2: improved memory usage on BlackBerry.
   /**
    * A temporary date object.
    */
   Date tempDate = new Date();
   
   /**
    * A temporary time object.
    */
   static Time tempTime = new Time();
   
   /**
    * An auxiliary value.
    */
   SQLValue sqlv = new SQLValue();
   
   /**
    * A temporary value for index manipulation.
    */
   Value tempVal = new Value();
   
   /**
    * An object to check if the primary key was violated.
    */
   private CheckPK checkPK = new CheckPK();
 
   /**
    * A vector of ancestors of index nodes.
    */
   IntVector ancestors = new IntVector();
   
   /**
    * A temporary buffer for strings.
    */
   StringBuffer sBuffer = new StringBuffer();
   
   /**
    * An auxiliary single value for index manipulation.
    */
   private SQLValue[] oneValue = new SQLValue[1];
   
   /**
    * A buffer to store the value.
    */
   private byte[] valueBuf = new byte[Value.VALUERECSIZE];
   
   // juliana@230_13: removed some possible strange behaviours when using threads.
   /**
    * A byte for saving table meta data.
    */
   byte[] oneByte = new byte[1]; // juliana@226_4
   
   /**
    * A buffer used for reading ascii strings in <code>PlainDB.readValue()</code>.
    */
   byte[] buffer = new byte[1];
   
   /**
    * A buffer used for reading unicode strings in <code>PlainDB.readValue()</code>.
    */
   char[] valueAsChars = new char[1];
   
   /**
    * The lexical analizer.
    */
   LitebaseLex lexer = new LitebaseLex();
   
   static
   {
      if (Settings.deviceId == null) // juliana@lb201_30: fills Settings if its a headless application. 
         new totalcross.Launcher().fillSettings();
      
      if (Settings.platform.equals("BlackBerry"))
      {
         //$START:DEMO-VERSION$
         StringBuffer copyright = new StringBuffer("Mjufcbtf\n!!!!!!!EFNP!WFSTJPO\nDpqzsjhiu!2008.2009!TvqfsXbcb!Mueb");
         int i = copyright.length();
         while (--i >= 0)
         {
            char c = copyright.charAt(i);
            if (c != '\n' && !('0' <= c && c <= '9'))
               copyright.setCharAt(i, (char)(c - 1));
         }
         Vm.alert(copyright.toString());
         //$END:DEMO-VERSION$
         //$START:FULL-VERSION$
         totalcross.Launcher.checkLitebaseAllowed();
         //$END:FULL-VERSION$
      }
   }
   
   // juliana@230_11: Litebase public class constructors are now not public any more. 
   /**
    * The constructor.
    */
   private LitebaseConnection() {}
   
   // juliana@201_26: created a default getInstance() which creates a new Litebase connection with the current application id.
   /**
    * Creates a Litebase connection for the default creator id, storing the database as a flat file. This method avoids the creation of more than one 
    * instance with the same creator id, which would lead to performance and memory problems. Using this method, the strings are stored in the 
    * unicode format.
    *
    * @return A Litebase instance.
    */
   public static LitebaseConnection getInstance()
   {
      return getInstance(Settings.applicationId, null);
   }
   
   /**
    * Creates a Litebase connection for the given creator id, storing the database as a flat file. This method avoids the creation of more than one 
    * instance with the same creator id, which would lead to performance and memory problems. Using this method, the strings are stored in the 
    * unicode format.
    *
    * @param appCrid The creator id, which may (or not) be the same one of the current application and MUST be 4 characters long.
    * @return A Litebase instance.
    */
   public static LitebaseConnection getInstance(String appCrid)
   {
      return getInstance(appCrid, null);
   }

   /**
    * Creates a LitebaseConnection for the given creator id and with the given connection param list. This method avoids the creation of more than
    * one instance with the same creator id and parameters, which would lead to performance and memory problems.
    *
    * @param appCrid The creator id, which may be the same one of the current application and MUST be 4 characters long.
    * @param params Only the folder where it is desired to store the tables, <code>null</code>, if it is desired to use the current data 
    * path, or <code>chars_type = chars_format; path = source_path</code>, where <code>chars_format</code> can be <code>ascii</code> or 
    * <code>unicode</code>, and <code>source_path</code> is the folder where the tables will be stored. The params can be entered in any order. If
    * only the path is passed as a parameter, unicode is used. Notice that path must be absolute, not relative.
    * <p>If it is desired to store the database in the memory card (on Palm OS devices only), use the desired volume in the path given to the method.
    * <p>Most PDAs will only have one card, but others, like Tungsten T5, can have more then one. So it is necessary to specify the desired card 
    * slot.
    * <p>Note that databases belonging to multiple applications can be stored in the same path, since all tables are prefixed by the application's 
    * creator id.
    * <p>Also notice that to store Litebase files on card on Pocket PC, just set the second parameter to the correct directory path.
    * <p>It is not recommended to create the databases directly on the PDA. Memory cards are FIVE TIMES SLOWER than the main memory, so it will take
    * a long time to create the tables. Even if the NVFS volume is used, it can be very slow. It is better to create the tables on the desktop, and 
    * copy everything to the memory card or to the NVFS volume.
    * <p>Due to the slowness of a memory card and the NVFS volume, all queries will be stored in the main memory; only tables and indexes will be 
    * stored on the card or on the NVFS volume.
    * <p> An exception will be raised if tables created with an ascii kind of connection are oppened with an unicode connection and vice-versa.
    * @return A Litebase instance.
    * @throws DriverException If an <code>IOException</code> occurs or an application id with more or less than four characters is specified.
    */
   public static LitebaseConnection getInstance(String appCrid, String params) throws DriverException
   {
      try
      {         
         if (appCrid.length() != 4) // The application id must have 4 characters.
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_CRID));
         
         int key = (appCrid + '|' + params + '|' + Thread.currentThread()).hashCode(); // The key which identifies the Litebase connection.

         LitebaseConnection conn = (LitebaseConnection)htDrivers.get(key); // Tries to get a connection with the same key.
         
         if (conn == null) // If there is no connections with this key, creates a new one.
         {
            conn = new LitebaseConnection();
            if (logger != null)
               synchronized (logger)
               {
                  conn.sBuffer.setLength(0);
                  logger.logInfo(conn.sBuffer.append("new LitebaseConnection(").append(appCrid).append(",").append(params).append(")"));
               }
          
            // juliana@210_2: now Litebase supports tables with ascii strings.
            String path = null;
            if (params != null)
            {
               String[] paramsSeparated = Convert.tokenizeString(params, ';'); // Separates the parameters.
               String tempParam = null;
               int len = paramsSeparated.length;
              
               if (len == 1) // Things do not change if there is only one parameter.
                  path = params;
               else
                  while (--len >= 0) // The parameters order does not matter. 
                  {
                     tempParam = paramsSeparated[len].trim();
                     if (tempParam.startsWith("chars_type")) // Chars type param. 
                        conn.isAscii = tempParam.indexOf("ascii") != -1;
                     else if (tempParam.startsWith("path")) // Path param.
                           path = tempParam.substring(tempParam.indexOf('=') + 1).trim();
                  }
            }
           
            // juliana@214_1: relative paths can't be used with Litebase.
            validatePath(path = conn.sourcePath = (path != null)? path : Settings.dataPath != null && Settings.dataPath.length() != 0? Settings.dataPath : Settings.appPath);
            
            // If the source folder does not exist, it is created. This creation is recursive.
            if (path.length() > 0 && !new File(path).exists())
               new File(path).createDir();

            if (!path.endsWith("\\") && !path.endsWith("/")) // Appends a "/" if the datapath does not end with "\\" or "/".
               conn.sourcePath = path + '/';

            conn.appCrid = appCrid;
            conn.htTables = new Hashtable(10);
            conn.key = key;
            conn.lexer.nameToken = conn.sBuffer;
            
            synchronized(htDrivers) // juliana@230_13: removed some possible strange behaviours when using threads.
            {
               htDrivers.put(key, conn);
            }
         }
         return conn;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Returns the path where the tables created/opened by this connection are stored.
    *
    * @return A string representing the path.
    * @throws IllegalStateException If the driver is closed.
    */
   public String getSourcePath() throws IllegalStateException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      return sourcePath;
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
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
    * @param sql The SQL creation command.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    * @throws SQLParseException If the table name or a default string is too big, there is an invalid default value, or an unknown (on a create 
    * table) or repeated column name, or an <code>InvalidDateException</code> or an <code>InvalidNumberException</code> occurs.
    * @throws AlreadyCreatedException If the table or index is already created.
    */
   public void execute(String sql) throws IllegalStateException, DriverException, SQLParseException, AlreadyCreatedException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));

      if (logger != null)
         synchronized (logger)
         {
            logger.log(Logger.INFO, sql, false);
         }

      try
      {
         int i;
         LitebaseParser parser = new LitebaseParser();
         parser.tableList = new SQLResultSetTable[1];
         
         if ((sql.toLowerCase().trim()).startsWith("create table"))
            parser.fieldList = new SQLFieldDefinition[SQLElement.MAX_NUM_COLUMNS];
         parser.fieldNames = new String[SQLElement.MAX_NUM_COLUMNS];
         
         // juliana@224_2: improved memory usage on BlackBerry.
         LitebaseParser.parser(sql, parser, lexer); // Does de parsing.
        
         if (parser.command == SQLElement.CMD_CREATE_TABLE) // CREATE TABLE
         {
            String tableName = parser.tableList[0].tableName;
            
            // Verifies the length of the table name.
            if (tableName.length() > SQLElement.MAX_TABLE_NAME_LENGTH_AS_PLAIN_FILE) // rnovais@570_114: The table name can't be infinite.
               throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_MAX_TABLE_NAME_LENGTH));
               
            if (exists(tableName)) // guich@105: verifies if it is already created.
               throw new AlreadyCreatedException(LitebaseMessage.getMessage(LitebaseMessage.ERR_TABLE_ALREADY_CREATED) + tableName);
   
            // Counts the number of fields.
            int count = parser.fieldListSize + 1; // fieldListSize + rowid
   
            // Now gets the columns.
            String[] names = new String[count];
            int[] hashes = new int[count];
            short[] types = new short[count];
            int[] sizes = new int[count];
            int primaryKeyCol = Utils.NO_PRIMARY_KEY, 
                 composedPK = Utils.NO_PRIMARY_KEY;
            SQLValue[] defaultValues = new SQLValue[count];
            byte[] columnAttrs = new byte[count];
            SQLFieldDefinition field;
            Date tempDateAux = tempDate; // juliana@224_2: improved memory usage on BlackBerry.
   
            // Creates column 0 (rowid).
            names[0] = "rowid";
            types[0] = SQLElement.INT;
            hashes[0] = 108705909;
            
            i = count;
            while (--i > 0) // Creates the other columns.
            {
               field = parser.fieldList[i - 1];
               hashes[i] = (names[i] = field.fieldName).hashCode();
               types[i] = (short)field.fieldType;
               sizes[i] = field.fieldSize;
               
               if (field.isPrimaryKey) // Checks if there is a primary key definition.
                  primaryKeyCol = i; // Only one primary key can be defined per table: this is verified during the parsing.
   
               if (field.defaultValue != null) // Default values: default null has no effect. This is handled by the parser.
               {
                  defaultValues[i] = new SQLValue();
                  columnAttrs[i] |= Utils.ATTR_COLUMN_HAS_DEFAULT;  // Sets the default bit.
                  String defaultValue = field.defaultValue.trim();
                  
                  // juliana@222_9: Some string conversions to numerical values could return spourious values if the string range were greater than 
                  // the type range.
                  switch (field.fieldType) 
                  {
                     case SQLElement.CHARS:
                     case SQLElement.CHARS_NOCASE:
                        if (defaultValue.length() > sizes[i]) // The default value size can't be larger than the size of the field definition.
                           throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_LENGTH_DEFAULT_VALUE_IS_BIGGER));
                        defaultValues[i].asString = defaultValue;
                        break;
                     case SQLElement.DATE: // juliana@224_2: improved memory usage on BlackBerry.
                        defaultValues[i].asInt = tempDateAux.set(defaultValue, Settings.DATE_YMD);
                        break;
                     case SQLElement.DATETIME: // juliana@224_2: improved memory usage on BlackBerry.
                        int pos = defaultValue.lastIndexOf(' ');
                        if (pos == -1) // There is no time here.
                        {
                           defaultValues[i].asInt = tempDateAux.set(defaultValue, Settings.DATE_YMD);
                           defaultValues[i].asShort = 0;
                        }
                        else
                        {
                           defaultValues[i].asInt = tempDateAux.set(defaultValue.substring(0, pos), Settings.DATE_YMD);
                           defaultValues[i].asShort = Utils.testAndPrepareTime(defaultValue.substring(pos + 1).trim());
                        }
                        break;
                     case SQLElement.SHORT:
                        defaultValues[i].asShort = Convert.toShort(defaultValue);
                        break;
                     case SQLElement.INT:
                        defaultValues[i].asInt = Convert.toInt(defaultValue);
                        break;
                     case SQLElement.LONG:
                        defaultValues[i].asLong = Convert.toLong(defaultValue);
                        break;
                     case SQLElement.FLOAT:
                        defaultValues[i].asDouble = Utils.toFloat(defaultValue);
                        break;
                     case SQLElement.DOUBLE:
                        defaultValues[i].asDouble = Convert.toDouble(defaultValue);
                  } 
               }
   
               if (field.isNotNull) // Sets the 'not null' bit.
                  columnAttrs[i] |= Utils.ATTR_COLUMN_IS_NOT_NULL;
            }
   
            // Gets the composed primary keys.
            byte[] composedPKCols = null;
            int composedPKSize = parser.fieldNamesSize;
            String[] composedPK_Fields = parser.fieldNames;
            if (composedPKSize > 0)
            {
               composedPKCols = new byte[composedPKSize];
               int j, 
                   pos = -1;
               i = -1;
               while (++i < composedPKSize)
               {
                  pos = -1;
                  j = count;
                  
                  while (--j >= 0) // Checks if the name of a table column exist.
                     if (composedPK_Fields[i].equals(names[j]))
                     {
                        pos = j;
                        break;
                     }
                  if (pos == -1) // Column not found.
                     throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_UNKNOWN_COLUMN) + composedPK_Fields[i]);
   
                  
                  if (types[pos] == SQLElement.BLOB) // A blob can't be in a composed PK.
                     throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_BLOB_PRIMARY_KEY));
   
                  j = -1;
                  while (++j < i) // Verifies if there's a duplicate definition.
                     if (composedPKCols[j] == pos)
                        throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DUPLICATED_COLUMN_NAME) + composedPK_Fields[i]);
   
                  composedPKCols[i] = (byte)pos;
               }
               if (composedPKSize == 1)
               {
                  primaryKeyCol = pos;
                  composedPKCols = null;
               }
               else
                  composedPK = 0;
            }
            driverCreateTable(tableName, names, hashes, types, sizes, columnAttrs, defaultValues, primaryKeyCol, composedPK, composedPKCols);
         }
         else if (parser.command == SQLElement.CMD_CREATE_INDEX)
         {
            // indexTableName ignored - formed internally.
            String tableName = parser.tableList[0].tableName;
            String[] indexNames = new String[parser.fieldNamesSize];
            Hashtable names = new Hashtable(10);
            String indexName;
            
            // juliana@225_8: it was possible to create a composed index with duplicated column names.
            i = parser.fieldNamesSize;
            while (--i >= 0)
               if (names.get(indexName = indexNames[i] = parser.fieldNames[i]) == null)
                  names.put(indexName, indexName);
               else
                  throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DUPLICATED_COLUMN_NAME));
            
            driverCreateIndex(tableName, indexNames, null, false);
         }
         else
            throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_ONLY_CREATE_TABLE_INDEX_IS_ALLOWED));
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         throw new SQLParseException(exception);
      }
      catch (InvalidNumberException exception)
      {
         throw new SQLParseException(exception);
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Used to execute updates in a table (insert, delete, update, alter table, drop). E.g.:
    *
    * <p><code>driver.executeUpdate(&quot;drop table person&quot;);</code> will drop also the indices.
    * <p><code>driver.executeUpdate(&quot;drop index * on person&quot;);</code> will drop all indices but not the primary key index.
    * <p><code>driver.executeUpdate(&quot;drop index name on person&quot;);</code> will drop the index for the &quot;name&quot; column.
    * <p><code> driver.executeUpdate(&quot;ALTER TABLE person DROP primary key&quot;);</code> will drop the primary key.
    * <p><code>driver.executeUpdate(&quot;update person set age=44, salary=3200.5 where name = 'guilherme campos hazan'&quot;);</code> 
    * will update the table.
    * <p><code>driver.executeUpdate(&quot;delete person where name like 'g%'&quot;);</code> will delete records of the table.
    * <p><code> driver.executeUpdate(&quot;insert into person (age, salary, name, email)
    *  values (32, 2000, 'guilherme campos hazan', 'guich@superwaba.com.br')&quot;);</code> will insert a record in the table.
    *
    * @param sql The SQL update command.
    * @return The number of rows affected or <code>0</code> if a drop or alter operation was successful.
    * @throws IllegalStateException If the driver is closed.
    * @throws SQLParseException If an <code>InvalidDateException</code> or <code>InvalidNumberException</code> occurs.
    * @throws DriverException If an <code>IOException</code> occurs.
    */
   public int executeUpdate(String sql) throws IllegalStateException, SQLParseException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            logger.log(Logger.INFO, sql, false);
         }

      try
      {
         LitebaseParser parser = new LitebaseParser();
         
         // juliana@202_5: removed possible NPE if there is a blank in the beginning of the sql command.
         String tempSQL = sql.toLowerCase().trim();
         
         parser.tableList = new SQLResultSetTable[1];
         if (tempSQL.startsWith("insert") || tempSQL.startsWith("update"))
         {   
            parser.fieldValues = new String[SQLElement.MAX_NUM_COLUMNS];
            parser.fieldNames = new String[SQLElement.MAX_NUM_COLUMNS];
         }
         else
         if (tempSQL.startsWith("drop index"))
            parser.fieldNames = new String[SQLElement.MAX_NUM_COLUMNS];
         else
         if (tempSQL.startsWith("alter table")) 
            if (tempSQL.indexOf("rename") != -1)
               parser.fieldNames = new String[2];
            else
            if (tempSQL.indexOf("add primary key") != -1)
               parser.fieldNames = new String[SQLElement.MAX_NUM_COLUMNS];   
         
         // juliana@224_2: improved memory usage on BlackBerry.
         LitebaseParser.parser(sql, parser, lexer); // Does the parsing.
         
         switch (parser.command)
         {
            case SQLElement.CMD_DROP_TABLE: // DROP TABLE
               litebaseExecuteDropTable(parser);
               return 0;
            case SQLElement.CMD_DROP_INDEX: // DROP INDEX
               return litebaseExecuteDropIndex(parser);
            case SQLElement.CMD_INSERT: // INSERT
               new SQLInsertStatement(parser, this).litebaseBindInsertStatement().litebaseDoInsert(this);
               return 1;
            case SQLElement.CMD_DELETE: // DELETE
               return new SQLDeleteStatement(parser).litebaseBindDeleteStatement(this).litebaseDoDelete(this);
            case SQLElement.CMD_UPDATE: // UPDATE
               return new SQLUpdateStatement(parser).litebaseBindUpdateStatement(this).litebaseDoUpdate(this);
            case SQLElement.CMD_ALTER_DROP_PK: // DROP PRIMARY KEY
            case SQLElement.CMD_ALTER_ADD_PK: // ADD PRIMARY KEY
            case SQLElement.CMD_ALTER_RENAME_TABLE: // RENAME TABLE
            case SQLElement.CMD_ALTER_RENAME_COLUMN: // RENAME COLUMN
               litebaseExecuteAlter(parser); 
               return 0;
         }
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         throw new SQLParseException(exception);
      }
      catch (InvalidNumberException exception)
      {
         throw new SQLParseException(exception);
      }
      return -1;
   }

   /**
    * Executes an alter statement.
    *
    * @param parser The parser.
    * @throws DriverException If there is no primary key to be dropped, an invalid column name, or a new table name is already in use.
    * @throws AlreadyCreatedException If one tries to add another primary key or there is a duplicated column name in the primary key definition.
    * @throws SQLParseException If there is a blob in a primary key definition.
    * @throws IOException If an internal method throws it.
    * @throws InvalidDateException If an internal method throws it.
    */
   private void litebaseExecuteAlter(LitebaseParser parser) throws DriverException, AlreadyCreatedException, SQLParseException, IOException, 
                                                                                                                                InvalidDateException
   {
      String tableName = parser.tableList[0].tableName;
      Table table = getTable(tableName);
      int colIndex = -1;
      
      // juliana@226_4: now a table won't be marked as not closed properly if the application stops suddenly and the table was not modified since its 
      // last oppening. 
      if (!table.isModified) // Sets the table as not closed properly.
      {
         PlainDB plainDB = table.db;
         NormalFile dbFile = (NormalFile)plainDB.db;
         
         dbFile.setPos(6);
         
         // juliana@230_13: removed some possible strange behaviours when using threads.
         oneByte[0] = (byte)(plainDB.isAscii? Table.IS_ASCII : 0);
         dbFile.writeBytes(oneByte, 0, 1);
         
         dbFile.flushCache();
         table.isModified = true;
      }

      switch (parser.command)
      {
         case SQLElement.CMD_ALTER_DROP_PK: // DROP PRIMARY KEY
            if (table.primaryKeyCol != Utils.NO_PRIMARY_KEY) // Simple primary key.
            {
               colIndex = table.primaryKeyCol;
               table.primaryKeyCol = Utils.NO_PRIMARY_KEY;
               table.driverDropIndex(colIndex); // Drops the PK index.
            }
            else
            if (table.composedPK != Utils.NO_PRIMARY_KEY) // Composed primary key.
            {
               // juliana@230_17: solved a possible crash or exception if the table is not closed properly after dropping a composed primary key.
               table.numberComposedPKCols = 0;
               table.composedPK = Utils.NO_PRIMARY_KEY;
               table.driverDropComposedIndex(table.composedPrimaryKeyCols, -1, true); // The meta data is saved.
               table.composedPrimaryKeyCols = null;
            }
            else
               throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_TABLE_DOESNOT_HAVE_PRIMARY_KEY)); // There's no primary key.
            break;

         case SQLElement.CMD_ALTER_ADD_PK: // ADD PRIMARY KEY
            // There can't be two primary keys.
            if (table.primaryKeyCol != Utils.NO_PRIMARY_KEY || table.composedPK != Utils.NO_PRIMARY_KEY)
               throw new AlreadyCreatedException(LitebaseMessage.getMessage(LitebaseMessage.ERR_PRIMARY_KEY_ALREADY_DEFINED));

            int size = parser.fieldNamesSize, 
                j = -1,
                i = -1;
            byte[] composedPKCols = new byte[size];
            String colName = null;
            
            while (++i < size)
            {
               colIndex = table.htName2index.get((colName = parser.fieldNames[i]).hashCode(), -1);
               if (colIndex == -1)
                  throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_COLUMN_NAME) + colName);

               // Verifies if there's a duplicate definition.
               j = i;
               while (--j >= 0)
                  if (composedPKCols[j] == colIndex)
                     throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DUPLICATED_COLUMN_NAME) + parser.fieldNames[i]);
               composedPKCols[i] = (byte)colIndex;
               
               if (table.columnTypes[colIndex] == SQLElement.BLOB) // A blob cant be in a primary key.
                  throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_BLOB_PRIMARY_KEY));
            }
            if (size == 1) // Simple primary key.
            {
               if (table.columnIndices[table.primaryKeyCol = colIndex] == null) // If there is no index yet for the column, creates it.
               {
                  try
                  {
                     driverCreateIndex(tableName, new String[] {colName}, null, true);
                  }
                  catch (PrimaryKeyViolationException exception)
                  {
                     table.primaryKeyCol = -1;
                     throw exception;
                  }
               }
               else // The column already has an index.
                  table.tableSaveMetaData(Utils.TSMD_ONLY_PRIMARYKEYCOL); // guich@560_24
            }
            else // Composed primary key.
            {
               table.numberComposedPKCols = size;

               try
               {
                  driverCreateIndex(tableName, null, table.composedPrimaryKeyCols = composedPKCols, true);
               }
               catch (PrimaryKeyViolationException exception)
               {
                  table.numberComposedPKCols = 0;
                  table.composedPrimaryKeyCols = null;
                  table.composedPK = -1;
                  throw exception;
               }
            }
            break;

         case SQLElement.CMD_ALTER_RENAME_TABLE: // RENAME TABLE
            String newTableName = parser.fieldNames[0]; // The new table name is stored in the field list.

            if (exists(newTableName)) // The new table name can't be in use.
               throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_TABLE_ALREADY_EXIST) + newTableName);
            try
            {
               table.renameTable(this, tableName, newTableName);
            }
            catch (IOException exception) // A possible fail during rename will rename back all table files.
            {
               table.renameTable(this, newTableName, tableName);
               throw exception;
            }
            break;

         case SQLElement.CMD_ALTER_RENAME_COLUMN: // RENAME COLUMN
            table.renameTableColumn(parser.fieldNames[1], parser.fieldNames[0]);
      }
   }

   /**
    * Drops a table.
    * 
    * @param parser The parser.
    * @throws DriverException If the table does not exist.
    * @throws IOException If an internal method throws it.
    * @throws InvalidDateException If an internal method throws it.
    */
   private void litebaseExecuteDropTable(LitebaseParser parser) throws DriverException, IOException, InvalidDateException
   {
      String tableName = parser.tableList[0].tableName;
      Table table = (Table)htTables.remove(tableName); // Tries to get the table.

      // flsobral@224_4: workaround for bug with listFiles on BlackBerry 9000.
      if (table == null && Settings.platform.equals(Settings.BLACKBERRY) && Settings.deviceId.equals("9000"))
      {
         table = getTable(tableName);
         htTables.remove(tableName);
      }
      
      if (table != null) // The table is open.
      {
         Index idx;
         int i = table.columnCount;
         // Drops its simple indices.
         while (--i >= 0)
            if ((idx = table.columnIndices[i]) != null)
               idx.remove();
         // Drops its composed indices.
         if ((i = table.numberComposedIndices) > 0)
            while (--i >= 0)
               table.composedIndices[i].index.remove();
         table.db.remove(); // Drops the table.
      }
      else // The table is closed.
      {
         // juliana@220_12: drop table was dropping a closed table and all tables starting with the same name of the dropped one.
         // Lists the folder files.         
         String path = sourcePath,
                name = appCrid + '-' + tableName,
                nameSimpIdx = name + '$',
                nameCompIdx = name + '&';
         name += '.';
         File file = null;
         String[] listFiles = new File(path).listFiles();  // Lists all the path files.
         int numFiles = listFiles.length;

         while (--numFiles >=0)  // Erases the table files.
            if (listFiles[numFiles].startsWith(name) || listFiles[numFiles].startsWith(nameSimpIdx) || listFiles[numFiles].startsWith(nameCompIdx)) 
               (file = new File(path + listFiles[numFiles])).delete();

         if (file == null) // If there is no file to be erased, an exception must be raised.
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_TABLE_NAME_NOT_FOUND) + tableName);
      }
   }

   /**
    * Drops an index.
    * 
    * @param parser The parser.
    * @return The number of indices deleted.
    * @throws DriverException If a column does not have an index, is invalid, or if the columns to have the index dropped are from a primary key.
    * @throws IOException If an internal method throws it.
    * @throws InvalidDateException If an internal method throws it. 
    */
   private int litebaseExecuteDropIndex(LitebaseParser parser) throws DriverException, IOException, InvalidDateException
   {
      String tableName = parser.tableList[0].tableName;
      String colName = parser.fieldNames[0];
      Table table = getTable(tableName);
      int n = 1;
      
      // juliana@226_4: now a table won't be marked as not closed properly if the application stops suddenly and the table was not modified since its 
      // last oppening. 
      if (!table.isModified) // Sets the table as not closed properly.
      {
         PlainDB plainDB = table.db;
         NormalFile dbFile = (NormalFile)plainDB.db;
         
         dbFile.setPos(6);
         
         // juliana@230_13: removed some possible strange behaviours when using threads.
         oneByte[0] = (byte)(plainDB.isAscii? Table.IS_ASCII : 0);
         dbFile.writeBytes(oneByte, 0, 1);
         
         dbFile.flushCache();
         table.isModified = true;
      }
      
      if (colName.equals("*")) // Drops all the indices.
         n = table.deleteAllIndices();
      else // Drops an especific index.
      if (parser.fieldNamesSize == 1) // Simple index.
      {
         int column = table.htName2index.get(colName.hashCode(), -1);

         if (column == -1) // Unknown column.
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_COLUMN_NAME) + colName);
         
         if (column == table.primaryKeyCol) // Can't use drop index to drop a primary key.
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DROP_PRIMARY_KEY));
         table.driverDropIndex(column);
      }
      else // Composed index.
      
      {
         byte[] columns = new byte[parser.fieldNamesSize];
         byte[] keyCols = table.composedPrimaryKeyCols;
         byte column;
         int i = parser.fieldNamesSize;
         
         while (--i >= 0)
         {
            if ((column = (byte)table.htName2index.get((colName = parser.fieldNames[i]).hashCode(), -1)) == -1)
               throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_COLUMN_NAME) + colName);
            columns[i] = column;
         }
         
         if (keyCols != null) // Can't use drop index to drop a primary key.
         {
            // The columns passed can't be the same ones of the primary key.
            i = columns.length;
            while (--i >= 0)
               if (columns[i] != keyCols[i])
                  break;
            if (i < 0) // The columns of both arrays are equal.
               throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DROP_PRIMARY_KEY));
         }
         table.driverDropComposedIndex(columns, -1, true);
      }
      return n;
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
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
    * @param sql The SQL query command.
    * @return A result set with the values returned from the query.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    * @throws SQLParseException If an <code>InvalidDateException</code> or an <code>InvalidNumberException</code> occurs.
    */
   public ResultSet executeQuery(String sql) throws IllegalStateException, DriverException, SQLParseException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            logger.log(Logger.INFO, sql, false);
         }
      try
      {
         // Parses, creates and executes the select statement.
         LitebaseParser parser = new LitebaseParser();
         parser.tableList = new SQLResultSetTable[SQLElement.MAX_NUM_COLUMNS];
         parser.select = new SQLSelectClause();
         parser.tables = new IntHashtable(4);
         
         // juliana@224_2: improved memory usage on BlackBerry.
         LitebaseParser.parser(sql, parser, lexer); // Does de parsing.
         
         return new SQLSelectStatement(parser).litebaseBindSelectStatement(this).litebaseDoSelect(this);
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         throw new SQLParseException(exception);
      }
      catch (InvalidNumberException exception)
      {
         throw new SQLParseException(exception);
      }
   }
   
   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Creates a pre-compiled statement with the given sql. Prepared statements are faster for repeated queries. Instead of parsing the same query 
    * where only a few arguments change, it is better to create a prepared statement and the query is pre-parsed. Then, it is just needed to set the 
    * arguments (defined as ? in the sql) and run the sql.
    * 
    * @param sql The SQL query command.
    * @return A pre-compiled SQL statement.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    * @throws SQLParseException If an <code>InvalidDateException</code> or an <code>InvalidNumberException</code> occurs.
    */
   public PreparedStatement prepareStatement(String sql) throws IllegalStateException, SQLParseException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("prepareStatement ").append(sql));
         }
      
      // juliana@226_16: prepared statement is now a singleton.
      PreparedStatement ps = (PreparedStatement)htPS.get(sql); 
      if (ps != null)
      {
         ps.clearParameters();
         return ps;
      }
      
      ps = new PreparedStatement();
      try
      {
         ps.prepare(this, sql);
         htPS.put(sql, ps); // guich@201_28
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         throw new SQLParseException(exception);
      }
      catch (InvalidNumberException exception)
      {
         throw new SQLParseException(exception);
      }
      return ps;
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Returns the current rowid for a given table.
    * 
    * @param tableName The name of a table.
    * @return The current rowid for the table. -1 will never occur.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    */
   public int getCurrentRowId(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("getCurrentRowId ").append(tableName));
         }
      
      try
      {
         return getTable(tableName).currentRowId;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception) 
      {
         return -1;
      }  
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Returns the number of valid rows in a table. This may be different from the number of records if a row has been deleted.
    * 
    * @see #getRowCountDeleted(String)
    * @param tableName The name of a table.
    * @return The number of valid rows in a table. -1 will never occur.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    */
   public int getRowCount(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("getRowCount ").append(tableName));
         }
      try // juliana@201_31: LitebaseConnection.getRowCount() will now throw an exception if tableName is null or invalid instead of returning -1.
      {
         Table table = getTable(tableName);
         return table.db.rowCount - table.deletedRowsCount;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         return -1;
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Sets the row increment used when creating or updating big amounts of data. Using this method greatly increases the speed of bulk insertions 
    * (about 3x faster). To use it, it is necessary to call it (preferable) with the amount of rows that will be inserted. After the insertion is 
    * finished, it is <b>NECESSARY</b> to call it again, passing <code>-1</code> as the increment argument. Without doing this last step, data may 
    * be lost because some writes will be delayed until the method is called with -1. Another good optimization on bulk insertions is to drop the 
    * indexes and then create them afterwards. So, to correctly use <code>setRowInc()</code>, it is necessary to:
    * 
    * <pre>
    * driver.setRowInc(&quot;table&quot;, totalNumberOfRows);
    * // Fetches the data and insert them.
    * driver.setRowInc(&quot;table&quot;, -1);
    * </pre>
    * 
    * Using prepared statements on insertion makes it another a couple of times faster.
    * 
    * @param tableName The associated table name.
    * @param inc The increment value.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    */
   public void setRowInc(String tableName, int inc) throws DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("setRowInc ").append(tableName).append(' ').append(inc));
         }
      
      try
      {
         Table table = getTable(tableName);
         PlainDB db = table.db;
         Index[] columnIndices = table.columnIndices;
         ComposedIndex[] composedIndices = table.composedIndices;
         boolean setting = inc != -1;
         db.rowInc = setting? inc : Utils.DEFAULT_ROW_INC;
         int i = table.columnCount;
         while (--i >= 0) // Flushes the simple indices.
            if (columnIndices[i] != null)
               columnIndices[i].setWriteDelayed(setting);
         i = table.numberComposedIndices;
         while (--i >= 0) // juliana@202_18: The composed indices must also be written delayed when setting row increment to a value different to -1.
            composedIndices[i].index.setWriteDelayed(setting);
         
         NormalFile dbFile = (NormalFile)db.db,
         dboFile = (NormalFile)db.dbo;
         
         // juliana@227_3: improved table files flush dealing.
         if (inc == -1) // juliana@202_17: Flushs the files to disk when setting row increment to -1.
         {
            dbFile.dontFlush = dboFile.dontFlush = false;
            if (dbFile.cacheIsDirty)
               dbFile.flushCache(); // Flushs .db.
            if (dboFile.cacheIsDirty)
               dboFile.flushCache(); // Flushs .dbo.
         }
         else
            dbFile.dontFlush = dboFile.dontFlush = true;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception) {}
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Indicates if the given table already exists. This method can be used before a drop table.
    * 
    * @param tableName The name of a table.
    * @return <code>true</code> if a table exists; <code>false</code> othewise.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs or the driver is closed.
    */
   public boolean exists(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      String name = tableName.toLowerCase();
      
      if (htTables.exists(name)) // If the table is loaded, it exists.
         return true;

      try // Tests if the .db file exists.
      {
         sBuffer.setLength(0);
         return new File(sBuffer.append(sourcePath).append(appCrid).append('-').append(name).append(NormalFile.DB_EXT).toString()).exists();
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Releases the file handles (on the device) of a Litebase instance. Note that, after this is called, all <code>Resultset</code>s and 
    * <code>PreparedStatement</code>s created with this Litebase instance will be in an inconsistent state, and using them will probably reset the 
    * device. This method also deletes the active instance for this creator id from Litebase's internal table.
    *
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs or the driver is closed.
    */
   public void closeAll() throws IllegalStateException, DriverException // guich@109
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            logger.log(Logger.INFO, "closeAll", false);
         }
      try
      {
         litebaseClose();
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
   }

   /**
    * Releases the file handles (on the device) of a Litebase instance. Note that, after this is called, all <code>Resultset</code>s and 
    * <code>PreparedStatement</code>s created with this Litebase instance will be in an inconsistent state, and using them will probably reset the 
    * device.
    * 
     * @throws IOException</code> If an internal method throws it.
    */
   private void litebaseClose() throws IOException
   {
      dontFinalize = true;
      Vector v = htTables.getValues();
      int n = v.size();

      Index idx;
      Table table;
      int i;

      while (--n >= 0)
      {
         (table = (Table)v.items[n]).db.close(table.db.isAscii, true); // Closes the table files.
         table.db = null;

         // Closes the simple indices.
         i = table.columnCount;
         while (--i >= 0)
            if ((idx = table.columnIndices[i]) != null)
               idx.close();
              
         // Closes the composed indices.
         i = table.numberComposedIndices;
         while (--i >= 0)
            table.composedIndices[i].index.close();
      }
      
      htTables = null; // guich@564_9: makes sure that this instance will fail if someone tries to use it again.
      synchronized (htDrivers) // juliana@201_11: only sets the LitebaseConnection class as finalized if all its objects are finalized.
      {
         htDrivers.remove(key);
      }
      
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   // juliana@201_13: .dbo is now being purged.
   /**
    * Used to delete physically the records of the given table. Records are always deleted logically, to avoid the need of recreating the indexes.
    * When a new record is added, it doesn't uses the position of the previously deleted one. This can make the table big, if a table is created, 
    * filled and has a couple of records deleted. This method will remove all deleted records and recreate the indexes accordingly. Note that it 
    * can take some time to run.
    * <p>
    * Important: the rowid of the records is NOT changed with this operation.
    * 
    * @param tableName The table name to purge.
    * @return The number of purged records. -1 will never occur.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    */
   public int purge(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("purge ").append(tableName));
         }

      try
      {
         Table table = getTable(tableName);
         boolean updateAuxRowId = false; // rnovais@570_61

         // Removes the deleted records from the table.
         int deleted = table.deletedRowsCount;

         if (deleted > 0)
         {
            PlainDB plainDB = table.db;
            NormalFile dbFile = (NormalFile)plainDB.db;
            
            int i = -1, 
                j,
                rows = plainDB.rowCount,
                willRemain = rows - table.deletedRowsCount,
                columns = table.columnCount;
            
            // juliana@226_4: now a table won't be marked as not closed properly if the application stops suddenly and the table was not modified 
            // since its last oppening. 
            if (!table.isModified) // Sets the table as not closed properly.
            {
               dbFile.setPos(6);
               
               // juliana@230_13: removed some possible strange behaviours when using threads.
               oneByte[0] = (byte)(plainDB.isAscii? Table.IS_ASCII : 0);
               dbFile.writeBytes(oneByte, 0, 1);
               
               dbFile.flushCache();
               table.isModified = true;
            }
            
            if (willRemain == 0) // If no rows will remain, just deletes everyone.
            {
               // Shrinking the file is faster than deleting and recreating it.
               dbFile.f.setSize(0);
               ((NormalFile)plainDB.dbo).f.setSize(0);
               ((NormalFile)plainDB.dbo).finalPos = 0; // juliana@202_13: .dbo final position must be zeroed when purging all the table.

               dbFile.size = plainDB.dbo.size = plainDB.rowAvail = plainDB.rowCount = 0;
               updateAuxRowId = true; // Needs to update the auxRowId, because the last line was deleted.
            }
            else
            {
               // rnovais@570_75: inserts all records at once.
               PlainDB newdb = new PlainDB(table.name + "_", sourcePath, true);
               DataStreamLE oldBasds = plainDB.basds;
               int[] columnSizes = table.columnSizes;
               short[] columnTypes = table.columnTypes;
               byte[] columnNulls0 = table.columnNulls[0];

               SQLValue[] record = SQLValue.newSQLValues(table.columnCount);
               int length = columnNulls0.length;
               
               // juliana@220_13: The header size of the purged table must be equal to the header size of the table before purge. 
               newdb.headerSize = plainDB.headerSize;
               
               newdb.isAscii = plainDB.isAscii; // juliana@210_2: now Litebase supports tables with ascii strings.
               newdb.driver = this; 
               
               // rnovais@570_61: verifies if it needs to store the currentRowId.
               plainDB.read(rows - 1);
               if ((oldBasds.readInt() & Utils.ROW_ATTR_MASK) == Utils.ROW_ATTR_DELETED) // Is the last record deleted?
                  updateAuxRowId = true;
               if (updateAuxRowId) // rnovais@570_61
                  table.auxRowId = table.currentRowId;

               newdb.setRowSize(plainDB.rowSize, plainDB.basbuf);
               newdb.rowInc = willRemain;
               
               ByteArrayStream newBas = newdb.bas; 
               DataStreamLE newBasds = newdb.basds;
               byte[] oldBuffer = plainDB.bas.getBuffer();
               
               while (++i < rows)
               {
                  table.readRecord(record, i, 0, null, null, false, null); // juliana@220_3 juliana@227_20
                  if (((record[0].asInt = oldBasds.readInt()) & Utils.ROW_ATTR_MASK) != Utils.ROW_ATTR_DELETED) // Is record not deleted?
                  {    
                     j = -1;
                     while (++j < columns)
                        newdb.writeValue(columnTypes[j], record[j], newBasds, true, true, columnSizes[j], 0, false); // juliana@220_3
                     newBasds.writeBytes(columnNulls0, 0, length); 
                     
                     // juliana@223_8: corrected a bug on purge that would not copy the crc32 codes for the rows.
                     // juliana@220_4: added a crc32 code for every record. Please update your tables.
                     j = oldBuffer[3];
                     oldBuffer[3] = 0; // juliana@222_5: The crc was not being calculated correctly for updates.
                     
                     // Computes the crc for the record and stores at the end of the record.
                     newBasds.writeInt(Table.computeCRC32(oldBuffer, newBas.getPos())); 
                     
                     oldBuffer[3] = (byte)j;
                     
                     newdb.add();
                     newdb.write();
                  }
               }
               newdb.rowInc = Utils.DEFAULT_ROW_INC;
               String oldName = table.name;
               dbFile.f.delete();
               ((NormalFile)plainDB.dbo).f.delete();
               newdb.rename(oldName, sourcePath);
               table.db = newdb;
            }

            // juliana@115_8: saving metadata before recreating the indices does not let .db header become empty.

            // Empties the deletedRows and update the metadata.
            table.deletedRowsCount = 0;
            table.tableSaveMetaData(Utils.TSMD_EVERYTHING); // guich@560_24

            Vm.gc(); // Frees some memory.
            
            i = table.columnCount;
            while (--i >= 0) // Recreates the simple indices.
               if (table.columnIndices[i] != null)
                  table.tableReIndex(i, null, false);

            if (table.numberComposedIndices > 0) // Recreates the composed indices.
            {   
               i = table.numberComposedIndices;
               while (--i >= 0)
                  table.tableReIndex(i, table.composedIndices[i], false);
            }
         }
         return deleted;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         return -1;
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Returns the number of deleted rows.
    * 
    * @param tableName The name of a table.
    * @return The total number of deleted records of the given table. -1 will never occur.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs.
    */
   public int getRowCountDeleted(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("getRowCountDeleted ").append(tableName));
         }

      try
      {
         return getTable(tableName).deletedRowsCount;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         return -1;
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   /**
    * Gets an iterator for a table. With it, it is possible iterate through all the rows of a table in sequence and get
    * its attributes. This is good for synchronizing a table. While the iterator is active, it is not possible to do any
    * queries or updates because this can cause dada corruption.
    * 
    * @param tableName The name of a table.
    * @return A iterator for the given table. <code>null</code> will never occur.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs or the driver is closed.
    */
   public RowIterator getRowIterator(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("getRowIterator ").append(tableName));
         }
      try
      {
         return new RowIterator(this, tableName.toLowerCase());
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         return null;
      }
   }

   // juliana@210_3: LitebaseConnection.getLogger() and LitebaseConnection.setLogger() are no longer deprecated.
   /**
    * Gets the Litebase logger. The fields should be used unless using the logger within threads. 
    * 
    * @return The logger.
    */
   public static synchronized Logger getLogger()
   {
      return logger;
   }

   /**
    * Sets the litebase logger. This enables log messages for all queries and statements of Litebase and can be very useful to help finding bugs in 
    * the system. Logs take up memory space, so turn them on only when necessary. The fields should be used unless using the logger within threads.
    * 
    * @param logger The logger.
    */
   public static synchronized void setLogger(Logger logger)
   {
      LitebaseConnection.logger = logger;
   }

   // juliana@230_4: Litebase default logger is now a plain text file instead of a PDB file.                                                                
   /**                                                                                                                                                 
    * Gets the default Litebase logger. When this method is called for the first time, a new text file is created. In the subsequent calls, the same   
    * file is used.                                                                                                                                    
    *                                                                                                                                                  
    * @return The default Litebase logger.                                                                                                             
    * @throws DriverException If an <code>IOException</code> occurs.                                                                                    
    */                                                                                                                                                 
   public static synchronized Logger getDefaultLogger() throws DriverException                                                                         
   {                                                                                                                                                   
      Logger logger = Logger.getLogger("litebase", -1, null); // Gets the logger object.                                                               
                                                                                                                                                       
      try                                                                                                                                              
      {                                                                                                                                                
         if (logger.getOutputHandlers().length == 0) // Only gets a new default logger if no one exists.                                               
         {                                                                                                                                             
            LitebaseConnection.tempTime.update();                                                                                                      
            logger.addOutputHandler(new File(Convert.appendPath(Settings.dataPath != null && Settings.dataPath.length() > 0? Settings.dataPath                            
         : Settings.appPath, "LITEBASE_" + LitebaseConnection.tempTime.getTimeLong() + '.' + Settings.applicationId + ".LOGS"), File.CREATE_EMPTY, 1));
         }                                                                                                                                             
      }                                                                                                                                                
      catch (IOException exception)                                                                                                                   
      {                                                                                                                                                
         throw new DriverException(exception);                                                                                                        
      }                                                                                                                                                
                                                                                                                                                       
      logger.setLevel(Logger.INFO);                                                                                                                    
      return logger;                                                                                                                                   
   }                                                                                                                                                   
                                                                                                                                                       
   /**                                                                                                                                                 
    * Deletes all the log files with the default format found in the default device folder. If log is enabled, the current log file is not affected    
    * by this command.                                                                                                                                 
    *                                                                                                                                                  
    * @return the number of files deleted.                                                                                                             
    * @throws DriverException If an <code>IOException</code> occurs.                                                                                   
    */                                                                                                                                                 
   public static synchronized int deleteLogFiles() throws DriverException                                                                              
   {                                                                                                                                                   
      String path = Settings.dataPath != null && Settings.dataPath.length() > 0? Settings.dataPath : Settings.appPath;                                 
      int count = 0, // The number of log files.                                                                                                       
          i;                                                                                                                                           
                                                                                                                                                       
      try // Gets a list of closed log files.                                                                                                          
      {                                                                                                                                                
        String[] list = File.listFiles(path);                                                                                                          
                                                                                                                                                       
         if (list != null)                                                                                                                              
         {                                                                                                                                              
            String current;                                                                                                                             
                                                                                                                                                        
            i = list.length;                                                                                                                            
            while (--i >= 0)                                                                                                                            
            {                                                                                                                                           
            current = list[i];                                                                                                                         
               if (current.startsWith(Convert.appendPath(path, "LITEBASE_")) && current.endsWith(".LOGS"))                                              
               {                                                                                                                                        
                   try                                                                                                                                 
                   {                                                                                                                                   
                     new File(current, File.READ_WRITE).delete(); // Deletes all closed log files.                                                      
                     count++;                                                                                                                           
                   }                                                                                                                                   
                   catch (IOException exception) {}                                                                                                    
               }                                                                                                                                        
            }                                                                                                                                           
         }                                                                                                                                              
      }                                                                                                                                                
      catch (IOException exception)                                                                                                                    
      {                                                                                                                                                
         throw new DriverException(exception);                                                                                                         
      }                                                                                                                                                
      return count;                                                                                                                                    
   } 

   // guich@566_32 rnovais@570_77
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
    * @param sql The string array of SQL commands to be executed.
    * @param params The parameters to open a connection.
    * @param isDebug Indicates if debug information is to displayed on the debug console.
    * @return The LitebaseConnection instance created, or <code>null</code> if <code>closeAll</code> was the last command executed (or no commands 
    * were executed at all).
    * @throws DriverException If an exception occurs.
    */
   public static LitebaseConnection processLogs(String[] sql, String params, boolean isDebug) throws DriverException
   {
      LitebaseConnection driver = null;
      int i = -1,
          length = sql.length;
      String str;
      ResultSet rs;
      
      while (++i < length)
      {
         str = sql[i];
         if (isDebug)
            Vm.debug("running command #" + (i + 1));
         try
         {
            if (str.startsWith("new LitebaseConnection")) // Gets a new Litebase Connection.
               
               // juliana@115_3: corrected the start and the end position for the application id of the table.
               driver = getInstance(str.substring(23, 27), params);
            else
               
            if (str.startsWith("create")) // Create command.
               driver.execute(str);
            else
            if (str.equals("closeAll")) // closeAll() command.
            {
               driver.closeAll();
               driver = null;
            }
            else
            if (str.startsWith("select")) // Select command.
            {
               rs = driver.executeQuery(str);
               while (rs.next());
               rs.close();
            }
            else
            if (str.length() > 0) // Commands that update the table.
               driver.executeUpdate(str);
         }
         catch (RuntimeException exception)
         {
            if (isDebug)
               Vm.debug(str + " - " + exception);
            throw new DriverException(exception);
         }
      }
      return driver;
   }
   
   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   // juliana@220_5: added a method to recover possible corrupted tables, the ones that were not closed properly.
   /**
    * Tries to recover a table not closed properly by marking and erasing logically the records whose crc are not valid. The table must be closed in
    * order to use this method and will be closed after it.
    * When a table is not closed, a <code>TableNotClosedException</code> is thrown when one tries to access the table. One should, then, call this 
    * method to try to recover it.
    * 
    * @param tableName The table to be recovered.
    * @return <code>true</code> if it was in fact corrupted; <code>false</code>otherwise.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If an <code>IOException</code> occurs, it is not possible to read from the file, or the table was closed correctly.
    */
   public boolean recoverTable(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("recover table ").append(tableName));
         }

      try
      {
         sBuffer.setLength(0);
         
         // Opens the table file.
         File tableDb = new File(sBuffer.append(sourcePath).append(appCrid).append('-').append(tableName.toLowerCase()).append(".db").toString(), 
                                                                                                                        File.READ_WRITE, -1);
         
         byte[] buffer = new byte[1]; 
         
         // juliana@222_2: the table must be not closed properly in order to recover it.
         tableDb.setPos(6);
         if (tableDb.readBytes(buffer, 0, 1) == -1)
         {
            tableDb.close();
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_CANT_READ));
         }
         if ((buffer[0] & Table.IS_SAVED_CORRECTLY) == Table.IS_SAVED_CORRECTLY)
         {  
            tableDb.close(); 
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_TABLE_CLOSED));
         }
         tableDb.close();
         
         boolean recovered = false;
         Table table = new Table();
         
         // juliana@224_2: improved memory usage on BlackBerry.
         table.tempDate = tempDate;
         table.tempVal = tempVal;
         table.ancestors = ancestors;
         table.valueBuf = valueBuf;
         table.oneByte = oneByte;
         
         // Opens the table even if it was not cloded properly.
         table.tableCreate(sourcePath, appCrid + '-' + tableName.toLowerCase(), false, appCrid, this, isAscii, false);
         
         PlainDB plainDB = table.db;
         ByteArrayStream bas = plainDB.bas;
         DataStreamLE dataStream = plainDB.basds; 
         buffer = bas.getBuffer();
         int rows = plainDB.rowCount,
             crc32,
             rowid,
             i = -1,
             len = buffer.length - 4;

         table.deletedRowsCount = 0; // Invalidates the number of deleted rows.
         
         while (++i < rows) // Checks all table records.
         {
            plainDB.read(i);
            rowid = dataStream.readInt();
            
            if ((rowid & Utils.ROW_ATTR_MASK) == Utils.ROW_ATTR_DELETED) // Counts the number of deleted records.
               table.deletedRowsCount++;
            else
            {
               bas.reset();
               buffer[3] = 0; // Erases rowid information.
               crc32 = Table.computeCRC32(buffer, len);
               dataStream.skipBytes(len);
               if (crc32 != dataStream.readInt()) // Deletes and invalidates corrupted records.
               {
                  bas.reset();
                  dataStream.writeInt(Utils.ROW_ATTR_DELETED);
                  plainDB.rewrite(i);
                  table.deletedRowsCount++;
                  recovered = true;
               }
               else // juliana@224_3: corrected a bug that would make Litebase not use the correct rowid after a recoverTable().
                  table.auxRowId = (rowid & Utils.ROW_ID_MASK) + 1; 
            }
         }
           
         // Recreates the indices.
         // Simple indices.
         i = table.columnIndices.length;
         Index[] indices = table.columnIndices;
         while (--i >= 0)
            if (indices[i] != null)
               table.tableReIndex(i, null, false);
         
         // Composed indices.
         i = table.numberComposedIndices;
         ComposedIndex[] compIndices = table.composedIndices;
         while (--i >= 0)
            table.tableReIndex(i, compIndices[i], false);
         
         // juliana@224_3: corrected a bug that would make Litebase not use the correct rowid after a recoverTable().
         table.tableSaveMetaData(Utils.TSMD_ONLY_AUXROWID); // Saves information concerning deleted rows and the auxiliary rowid.
         
         // Closes the table.
         plainDB.close(plainDB.isAscii, false); // Closes the table files.
         table.db = null;
         Index idx;
         
         // Closes the simple indices.
         i = table.columnCount;
         while (--i >= 0)
            if ((idx = table.columnIndices[i]) != null)
               idx.close();
              
         // Closes the composed indices.
         i = table.numberComposedIndices;
         while (--i >= 0)
            table.composedIndices[i].index.close();
         
         return recovered;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception)
      {
         return true;
      }
   }

   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   // juliana@220_11: added a method to convert a table from the previous format to the current one being used.
   /**
    * Converts a table from the previous Litebase table version to the current one. If the table format is older than the previous table version, 
    * this method can't be used. It is possible to know if the table version is not compativel with the current version used in Litebase because 
    * an exception will be thrown if one tries to open a table with the old format. The table will be closed after using this method and must be
    * closed before calling it. Notice that the table .db file will be overwritten. 
    * 
    * @param tableName The name of the table to be converted.
    * @throws IllegalStateException If the driver is closed.
    * @throws DriverException If the table version is not the previous one (too old or the actual used by Litebase), it is not possible to read from
    * the file, or an <code>IllegalArgumentIOException</code>, <code>FileNotFoundException</code>, or <code>IOException</code> occurs.
    */
   public void convert(String tableName) throws IllegalStateException, DriverException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      if (logger != null)
         synchronized (logger)
         {
            sBuffer.setLength(0);
            logger.logInfo(sBuffer.append("convert ").append(tableName));
         }
      
      try
      {
         byte[] oneByte = new byte[1];
         Table table = new Table();
         byte rowid;
         
         sBuffer.setLength(0);
         
         // Opens the table file.
         File tableDb = new File(sBuffer.append(sourcePath).append(appCrid).append('-').append(tableName.toLowerCase()).append(".db").toString(), 
                                                                                                                        File.READ_WRITE, -1);
         
         // The version must be the previous of the current one.
         tableDb.setPos(7);
         if (tableDb.readBytes(oneByte, 0, 1) == -1)
         {
            tableDb.close();         
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_CANT_READ));
         }
         if (oneByte[0] != (byte)(Table.VERSION  - 1))
         {
            tableDb.close(); // juliana@222_4: The table files must be closed if convert() fails().
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_WRONG_PREV_VERSION) + tableName);
         }
         
         // Changes the version to be current one and closes it.
         tableDb.setPos(7);
         oneByte[0] = (byte)Table.VERSION;
         tableDb.writeBytes(oneByte, 0, 1);
         tableDb.close();
         
         // juliana@224_2: improved memory usage on BlackBerry.
         table.tempDate = tempDate;
         table.tempVal = tempVal;
         table.ancestors = ancestors;
         table.valueBuf = valueBuf;
         table.oneByte = oneByte;
         
         // Opens the table even if it was not cloded properly.
         table.tableCreate(sourcePath, appCrid + '-' + tableName.toLowerCase(), false, appCrid, this, isAscii, false);
         PlainDB plainDB = table.db;   
         NormalFile dbFile = (NormalFile)table.db.db;
         DataStreamLE dataStream = plainDB.basds; 
         ByteArrayStream bas = plainDB.bas;
         byte[] buffer = bas.getBuffer();
         int headerSize = plainDB.headerSize, 
             len = buffer.length - 4,
             rows = (dbFile.size - headerSize) / len;
               
         while (--rows >= 0) // Converts all the records adding a crc code to them.
         {
            dbFile.setPos(rows * len + headerSize);
            dbFile.readBytes(buffer, 0, len);
            rowid = buffer[3];
            buffer[3] = 0;
            bas.reset();
            dataStream.skipBytes(len);
            dataStream.writeInt(Table.computeCRC32(buffer, len));
            buffer[3] = rowid;
            plainDB.rewrite(rows);
         }   
            
         // Closes the table.
         plainDB.close(plainDB.isAscii, false); // Closes the table files.
         table.db = null;
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
      catch (InvalidDateException exception) {}
   }
   
   /**
    * Finalizes the <code>LitebaseConnection</code> object.
    * 
    * @throws IOException If an internal method throws it.
    */
   protected void finalize() throws IOException
   {
      litebaseClose();
   }

   /**
    * Creates an index.
    * 
    * @param tableName The table name whose index is to be created.
    * @param columnNames The names of the index columns.
    * @param numberColumns The column numbers of the index.
    * @param isPK Indicates if the index to be created is the primary key.
    * @throws IOException If an internal method throws it.
    * @throws DriverException If a column for the index does not exist.
    * @throws SQLParseException If a column for the index is of type blob.
    * @throws PrimaryKeyViolationException If an index already exist for a group or one column.
    * @throws InvalidDateException If an internal method throws it.
    */
   private void driverCreateIndex(String tableName, String[] columnNames, byte[] numberColumns, boolean isPK)
         throws IOException, DriverException, PrimaryKeyViolationException, InvalidDateException, SQLParseException
   {
      Table table = getTable(tableName);
      PlainDB plainDB = table.db;
      int indexCount = (numberColumns == null)? columnNames.length : numberColumns.length;
      int saveType,
          idx = -1,
          i = indexCount;
      byte[] columns = new byte[indexCount];
      int[] columnSizes = new int[indexCount];
      int[] columnTypes = new int[indexCount];
      
      // juliana@226_4: now a table won't be marked as not closed properly if the application stops suddenly and the table was not modified since its 
      // last oppening. 
      if (!table.isModified) // Sets the table as not closed properly.
      {
         NormalFile dbFile = (NormalFile)plainDB.db;
         
         dbFile.setPos(6);
         
         // juliana@230_13: removed some possible strange behaviours when using threads.
         oneByte[0] = (byte)(plainDB.isAscii? Table.IS_ASCII : 0);
         dbFile.writeBytes(oneByte, 0, 1);
         
         dbFile.flushCache();
         table.isModified = true;
      }
      
      while (--i >= 0)
      {
         // Column not found.
         if ((idx = columns[i] = (numberColumns == null)? (byte) table.htName2index.get(columnNames[i].hashCode(), -1) : numberColumns[i]) == -1) 
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_COLUMN_NOT_FOUND) + columnNames[i]);
         
         if (table.columnTypes[idx] == SQLElement.BLOB) // An index can't have a blob column.
            throw new SQLParseException(LitebaseMessage.getMessage(LitebaseMessage.ERR_BLOB_INDEX));

         columnSizes[i] = table.columnSizes[idx];
         columnTypes[i] = table.columnTypes[idx];
      }
      
      int newIndexNumber = table.verifyIfIndexAlreadyExists(columns);
      if (indexCount == 1)
      {
         table.indexCreateIndex(table.name, columns[0], columnSizes, columnTypes, appCrid, sourcePath, false, false);
         saveType = Utils.TSMD_EVERYTHING;
      }
      else
      {
         table.indexCreateComposedIndex(table.name, columns, columnSizes, columnTypes, newIndexNumber, isPK, appCrid, true, sourcePath, false, 
                                                                                                                                        false);
         saveType = Utils.TSMD_EVERYTHING;
      }

      if (plainDB.rowCount > 0) // luciana@570_49: fixed index creation when row count == 1.
      {
         try // Catchs the PrimaryKeyViolation exception to drop the recreated index and throws it again.
         {
            if (indexCount == 1)
               table.tableReIndex(idx, null, isPK);
            else
            {
               int id = (newIndexNumber < 0)? -newIndexNumber : newIndexNumber;
               table.tableReIndex(id - 1, table.composedIndices[id - 1], isPK);
            }
         }
         catch (PrimaryKeyViolationException exception)
         {
            if (indexCount == 1)
            {
               table.primaryKeyCol = Utils.NO_PRIMARY_KEY;
               table.driverDropIndex(idx);
            }
            else
            {
               table.composedPK = Utils.NO_PRIMARY_KEY;
               table.numberComposedPKCols = 0;
               table.driverDropComposedIndex(columns, table.numberComposedIndices - 1, true);
            }
            throw exception;
         }
         catch (DriverException exception)
         {
            if (indexCount == 1)
               table.driverDropIndex(idx);
            else
               table.driverDropComposedIndex(columns, table.numberComposedIndices - 1, true);
            if (isPK)
            {
               table.primaryKeyCol = table.composedPK = Utils.NO_PRIMARY_KEY;
               table.numberComposedPKCols = 0;
               table.composedPrimaryKeyCols = null;
            }
            throw exception;
         }
      }
      table.tableSaveMetaData(saveType); // guich@560_24
   }

   // Modified it to return the table that was created, to add columnNames, and primaryKeyCol to the parameters list.
   /**
    * Creates a table, which can be stored on disk or on memory (result set table).
    *
    * @param tableName The table name.
    * @param names The table column names.
    * @param hashes The table column hashes.
    * @param types The table column types.
    * @param sizes The table column sizes.
    * @param columnAttrs The table cxlumn attributes.
    * @param defaultValues The table column default values.
    * @param primaryKeyCol The primary key column.
    * @param composedPK The composed primary key index in the composed indices.
    * @param composedPKCols The columnns that are part of the composed primary key.
    * @return The table handle.
    * @throws IOException If an internal method throws it.
    * @throws InvalidDateException If an internal method throws it.
    */
   Table driverCreateTable(String tableName, String[] names, int[] hashes, short[] types, int[] sizes, byte[] columnAttrs, SQLValue[] defaultValues,
                                             int primaryKeyCol, int composedPK, byte[] composedPKCols) throws IOException, InvalidDateException
   {
      Table table = new Table();
      
      // juliana@224_2: improved memory usage on BlackBerry.
      table.oneByte = oneByte;
      
      table.tableCreate(sourcePath, tableName == null? null : appCrid + "-" + tableName, true, appCrid, this, isAscii, true); // rnovais@570_75 juliana@220_5 
      
      if (tableName == null) // juliana@201_14
      {
         table.db.headerSize = 0;
         table.tableSetMetaData(null, hashes, types, sizes, null, null, -1, -1, null, 0);
      }
      else
      {
         int numberComposedPKCols = composedPKCols != null? composedPKCols.length : 0;
         
         table.isModified = true;
         
         // Stores the meta data if the table is new.
         table.tableSetMetaData(names, hashes, types, sizes, columnAttrs, defaultValues, primaryKeyCol, composedPK, composedPKCols, 
                                                                                                                    numberComposedPKCols);
         htTables.put(tableName, table);
         
         // juliana@224_2: improved memory usage on BlackBerry.
         table.tempDate = tempDate;
         table.tempVal = tempVal;
         table.ancestors = ancestors;
         table.checkPK = checkPK;
         table.oneValue = oneValue;
         table.valueBuf = valueBuf;
         
         if (primaryKeyCol != Utils.NO_PRIMARY_KEY) // creates the index for the primary key.
            driverCreateIndex(tableName, new String[] {names[primaryKeyCol]}, null, false);
         if (numberComposedPKCols > 0) // Creates the composed index for the composed primary key.
            driverCreateIndex(tableName, null, composedPKCols, true);
      }

      return table;
   }
   
   /**
    * Fetches a table that already exists.
    * 
    * @param tableName The name of the table to be fetched.
    * @return The desired table.
    * @throws IOException If an internal method throws it.
    * @throws InvalidDateException If an internal method throws it.
    */
   Table getTable(String tableName) throws IOException, InvalidDateException
   {
      Table table = (Table)htTables.get(tableName = tableName.toLowerCase()); // Already open?

      if (table == null)
      {
         // Opens it.
         table = new Table();
         
         // juliana@224_2: improved memory usage on BlackBerry.
         table.tempDate = tempDate;
         table.tempVal = tempVal;
         table.ancestors = ancestors;
         table.checkPK = checkPK;
         table.oneValue = oneValue;
         table.valueBuf = valueBuf;
         table.oneByte = oneByte;
         
         table.tableCreate(sourcePath, appCrid + '-' + tableName, false, appCrid, this, isAscii, true); // juliana@220_5

         PlainDB plainDB = table.db;
         
         if (plainDB.db.size == 0) // Only valid if already created.
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_TABLE_NAME_NOT_FOUND) + tableName);
         
         htTables.put(tableName, table); // Puts the table in the table hashes.
      }

      return table;
   }
    
   /**
    * Tests if a path is valid. It can't be null or relative.
    * 
    * @param path The path to be tested.
    * @throws DriverException If the path is null or relative.
    */
   private static void validatePath(String path) throws DriverException
   {
      int pathLen;
      String auxP;
      
      if (path == null || (pathLen = path.length()) == 0) // The path can't be null or empty.
         throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_PATH) + path);
      
      // The path can't be relative.
      if (path.charAt(0) == '.')
      {
         if (pathLen == 1 || (pathLen == 2 && path.charAt(1) == '.'))
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_PATH) + path + '.');
         if (pathLen > 1 && (path.charAt(1) == '/' || path.charAt(1) == '\\'))
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_PATH) + path + '.');
         if (pathLen > 2 && path.charAt(1) == '.' && (path.charAt(2) == '/' || path.charAt(2) == '\\'))
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_PATH) + path + '.');
      }
      
      pathLen = path.indexOf("/.");
      if (pathLen >= 0 )
         auxP = path.substring(pathLen);
      else
         auxP = null;
      if (auxP != null && (auxP.length() == 2 || auxP.charAt(2) == '/' || (auxP.charAt(2) == '.' && (auxP.length() == 3  || auxP.charAt(3) == '/'))))
         throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_INVALID_PATH) + path + '.');
   }
   
   /**
    * Returns the slot where the tables are stored. Always return -1 except on palm.
    * 
    * @return The slot where the tables are stored.
    */
   public int getSlot() // juliana@223_1: added a method to get the current slot being used. Returns -1 except on palm.
   {
      return -1;     
   }
   
   
   // juliana@230_27: if a public method in now called when its object is already closed, now an IllegalStateException will be thrown instead of a 
   // DriverException.
   // juliana@226_6: added LitebaseConnection.isOpen(), which indicates if a table is open in the current connection.
   /**
    * Indicates if a table is open or not.
    * 
    * @param tableName The table name to be checked
    * @return <code>true</code> if the table is open in the current connection; <code>false</code>, otherwise.
    * @throws IllegalStateException If the driver is closed.
    */
   public boolean isOpen(String tableName) throws IllegalStateException
   {
      if (htTables == null) // The driver can't be closed.
         throw new IllegalStateException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DRIVER_CLOSED));
      
      return htTables.get(tableName = tableName.toLowerCase()) != null; 
   }
   
   // juliana@226_10: added LitebaseConnection.dropDatabase().
   /**
    * Drops all the tables from a database represented by its application id and path.
    * 
    * @param crid The application id of the database.
    * @param sourcePath The path where the files are stored.
    * @param slot The slot on Palm where the source path folder is stored. Ignored on other platforms.
    * @throws DriverException If the database is not found or an <code>IOException</code> occurs.
    */
   public static void dropDatabase(String crid, String sourcePath, int slot) throws DriverException
   {
      try
      {
         // Lists all the files of the folder.
         File dir = new File(sourcePath, File.DONT_OPEN, slot);
         String[] files = dir.listFiles();
         int i = files.length;
         
         // Deletes only the files of the chosen database.
         boolean deleted = false;
         while (--i >= 0)
         {
            if (files[i].startsWith(crid + '-'))
            {
               new File(sourcePath + files[i], File.DONT_OPEN, slot).delete();
               deleted = true;
            }
         }
         
         if (!deleted) // If the database has no files, there is an error.
            throw new DriverException(LitebaseMessage.getMessage(LitebaseMessage.ERR_DB_NOT_FOUND));
      }
      catch (IOException exception)
      {
         throw new DriverException(exception);
      }
   }
}
