/*********************************************************************************
 *  TotalCross Software Development Kit - Litebase                               *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/



package litebase;

/**
 * Contains error messages with multi-language support. By now, only English and Portuguese are implemented.
 */
class LitebaseMessage
{
   // General errors.
   /**
    * "Error: "
    */
   static final int ERR_MESSAGE_START = 0;

   /**
    * " Near position "
    */
   static final int ERR_MESSAGE_POSITION = 1;

   /**
    * "Syntax error."
    */
   static final int ERR_SYNTAX_ERROR = 2;

   // Limit errors.
   /**
    * "Table name too big: must be <= 23."
    */
   static final int ERR_MAX_TABLE_NAME_LENGTH = 3;

   /**
    * "The maximum number of fields in a SELECT clause was exceeded."
    */
   static final int ERR_FIELDS_OVERFLOW = 4;

   // Column errors.
   /**
    * "Unknown column "
    */
   static final int ERR_UNKNOWN_COLUMN = 5;

   /**
    * "Invalid column name: "
    */
   static final int ERR_INVALID_COLUMN_NAME = 6;

   /**
    * "Invalid column number: "
    */
   static final int ERR_INVALID_COLUMN_NUMBER = 7;

   /**
    * "The following column(s) does (do) not have an associated index "
    */
   static final int ERR_COLUMN_DOESNOT_HAVE_AN_INDEX = 8;

   /**
    * "Column name in field list is ambiguous: "
    */
   static final int ERR_AMBIGUOUS_COLUMN_NAME = 9;

   /**
    * "Column not found: "
    */
   static final int ERR_COLUMN_NOT_FOUND = 10;

   /**
    * "Duplicated column name: "
    */
   static final int ERR_DUPLICATED_COLUMN_NAME = 11;

   // Primary key errors.
   /**
    * "A primary key was already defined for this table."
    */
   static final int ERR_PRIMARY_KEY_ALREADY_DEFINED = 12;

   /**
    * "Table does not have a primary key."
    */
   static final int ERR_TABLE_DOESNOT_HAVE_PRIMARY_KEY = 13;

   /**
    * "Statement creates a duplicated primary key in "
    */
   static final int ERR_STATEMENT_CREATE_DUPLICATED_PK = 14;

   // Type errors.
   /**
    * "Incompatible types."
    */
   static final int ERR_INCOMPATIBLE_TYPES = 15;

   /**
    * "Field size must be a positive interger value."
    */
   static final int ERR_FIELD_SIZE_IS_NOT_INT = 16;

   // Number of fields errors.
   /**
    * "The number of fields does not match the number of values "
    */
   static final int ERR_NUMBER_FIELDS_AND_VALUES_DOES_NOT_MATCH = 17;

   /**
    * "The given number of values does not match the table definition."
    */
   static final int ERR_NUMBER_VALUES_DIFF_TABLE_DEFINITION = 18;

   // Default value errors.
   /**
    * "Length of default value is bigger than column size."
    */
   static final int ERR_LENGTH_DEFAULT_VALUE_IS_BIGGER = 19;

   // Driver errors.
   /**
    * "This driver instance was closed and can't be used anymore. Please get a new instance of it."
    */
   static final int ERR_DRIVER_CLOSED = 20;

   /**
    * "ResultSet already closed!"
    */
   static final int ERR_RESULTSET_CLOSED = 21;
   
   /**
    * "RowIterator already closed!"
    */
   static final int ERR_ROWITERATOR_CLOSED = 22;
   
   /**
    * "ResultSetMetaData can't be used after the ResultSet is closed."
    */
   static final int ERR_RESULTSETMETADATA_CLOSED = 23;

   /**
    * "Cant't find native methods implementation for LitebaseConnection. Please install Litebase.dll/prc file."
    */
   static final int ERR_LITEBASEPRCDLL_NOT_FOUND = 24;

   /**
    * "The application id must be four characters long."
    */
   static final int ERR_INVALID_CRID = 25;
   
   /**
    * "The increment must be greater than 0 or -1."
    */
   static final int ERR_INVALID_INC = 26;

   // Table errors.
   /**
    * "Table name not found: "
    */
   static final int ERR_TABLE_NAME_NOT_FOUND = 27;

   /**
    * "Table already created: "
    */
   static final int ERR_TABLE_ALREADY_CREATED = 28;
   
   /**
    * "It is not possible to open a table within a connection with a different string format."
    */
   static final int ERR_WRONG_STRING_FORMAT = 29; // juliana@210_2: now Litebase supports tables with ascii strings.

   // ROWID error.
   /**
    * "ROWID can't be changed by the user!"
    */
   static final int ERR_ROWID_CANNOT_BE_CHANGED = 34;

   // Prepared Statement errors.
   /**
    * "SQL statement does not return result set."
    */
   static final int ERR_QUERY_DOESNOT_RETURN_RESULTSET = 35;

   /**
    * "SQL statement does not perform updates in the database."
    */
   static final int ERR_QUERY_DOESNOT_PERFORM_UPDATE = 36;

   /**
    * "Not all parameters of the query had their values defined."
    */
   static final int ERR_NOT_ALL_PARAMETERS_DEFINED = 37;

   /**
    * "A value was not defined for the parameter "
    */
   static final int ERR_PARAMETER_NOT_DEFINED = 38;

   /**
    * "Invalid parameter index."
    */
   static final int ERR_INVALID_PARAMETER_INDEX = 39;

   // Rename errors.
   /**
    * "Can't rename table. This table already exists: "
    */
   static final int ERR_TABLE_ALREADY_EXIST = 40;

   /**
    * "Column already exists: "
    */
   static final int ERR_COLUMN_ALREADY_EXIST = 41;

   // Alias errors.
   /**
    * "Not unique table/alias: "
    */
   static final int ERR_NOT_UNIQUE_ALIAS_TABLE = 42;

   /**
    * "This alias is already being used in this expression: "
    */
   static final int ERR_DUPLICATE_ALIAS = 43;

   /**
    * "An alias is required for the aggregate function column."
    */
   static final int ERR_REQUIRED_ALIAS = 44;

   // Litebase.execute() error.
   /**
    * "Only CREATE TABLE and CREATE INDEX can be used in Litebase.execute()."
    */
   static final int ERR_ONLY_CREATE_TABLE_INDEX_IS_ALLOWED = 45;

   // Order by and group by errors.
   /**
    * "ORDER BY and GROUP BY clauses must match."
    */
   static final int ERR_ORDER_GROUPBY_MUST_MATCH = 46;

   /**
    * "No support for virtual columns in SQL queries with GROUP BY clause."
    */
   static final int ERR_VIRTUAL_COLUMN_ON_GROUPBY = 47;

   // Function errors.
   /**
    * "All non-aggregation function columns in the SELECT clause must also be in the GROUP BY clause."
    */
   static final int ERR_AGGREG_FUNCTION_ISNOT_ON_SELECT = 48;

   /**
    * " is not an aggregation function. All fields present in a HAVING clause must be listed in the SELECT clause as
    * aliased aggregation functions."
    */
   static final int ERR_IS_NOT_AGGREG_FUNCTION = 49;

   /**
    * "Can't mix aggregation functions with real columns in the SELECT clause without a GROUP BY clause."
    */
   static final int ERR_CANNOT_MIX_AGGREG_FUNCTION = 50;

   /**
    * "Can't have aggregation functions with ORDER BY clause and no GROUP BY clause."
    */
   static final int ERR_CANNOT_HAVE_AGGREG_AND_NO_GROUPBY = 51;

   /**
    * " was not listed in the SELECT clause. All fields present in a HAVING clause must be listed in the SELECT clause as aliased aggregation 
    * funtions."
    */
   static final int ERR_WAS_NOT_LISTED_ON_AGGREG_FUNCTION = 52;

   /**
    * "SUM and AVG aggregation functions are not used with DATE and DATETIME type fields."
    */
   static final int ERR_SUM_AVG_WITH_DATE_DATETIME = 53;

   // DATETIME error.
   /**
    * "Value is not a DATETIME: "
    */
   static final int ERR_VALUE_ISNOT_DATETIME = 54;

   // Index errors.
   /**
    * "Index already created for column "
    */
   static final int ERR_INDEX_ALREADY_CREATED = 55;

   /**
    * "Can't drop a primary key index with drop index."
    */
   static final int ERR_DROP_PRIMARY_KEY = 56;
   
   /**
    * "Index too large. It can't have more than 65534 nodes."
    */
   static final int ERR_INDEX_LARGE = 57;

   // NOT NULL errors.
   /**
    * "Primary key can't have null."
    */
   static final int ERR_PK_CANT_BE_NULL = 58;

   /**
    * "Field can't be null: "
    */
   static final int ERR_FIELD_CANT_BE_NULL = 59;
   
   /**
    * "A parameter in a where clause can't be null."
    */
   static final int ERR_PARAM_NULL = 60;

   // Result set errors.
   /**
    * "ResultSet in invalid record position."
    */
   static final int ERR_RS_INV_POS = 61;

   /**
    * "Invalid value for decimal places: "
    */
   static final int ERR_RS_DEC_PLACES_START = 62;

   /**
    * ". Must be in the range -1 to 40."
    */
   static final int ERR_RS_DEC_PLACES_END = 63;

   // File errors.
   /**
    * "Can't read from table."
    */
   static final int ERR_CANT_READ = 64;

   /**
    * "Can't load node: index corrupted."
    */
   static final int ERR_CANT_LOAD_NODE = 65;

   /**
    * "Table is corrupted: "
    */
   static final int ERR_TABLE_CORRUPTED = 66;
   
   /**
    * "Table not closed properly: "
    */
   static final int ERR_TABLE_NOT_CLOSED = 67; // juliana@220_2

   /**
    * "A properly closed table can't be used in recoverTable(): "
    */
   static final int ERR_TABLE_CLOSED = 68; // juliana@222_2
   
   /**
    * "Can't find index record position on delete."
    */
   static final int ERR_IDX_RECORD_DEL = 69;
   
   /**
    * "The table format is incompatible with Litebase version. Please update your tables."
    */
   static final int ERR_WRONG_VERSION = 70;
   
   /**
    * "The table format is not the previous one: "
    */
   static final int ERR_WRONG_PREV_VERSION = 71; // juliana@220_11

   /**
    * "Invalid path: " 
    */
   static final int ERR_INVALID_PATH = 72; // juliana@214_1
   
   /**
    * "Database not found."
    */
   static final int ERR_DB_NOT_FOUND = 73; // juliana@226_10
   
   // BLOB errors.
   /**
    * "The total size of a blob can't be greater then 10 Mb."
    */
   static final int ERR_BLOB_TOO_BIG = 74;

   /**
    * "This is not a valid size multiplier."
    */
   static final int ERR_INVALID_MULTIPLIER = 75;

   /**
    * "A blob type can't be part of a primary key."
    */
   static final int ERR_BLOB_PRIMARY_KEY = 76;

   /**
    * "A BLOB column can't be indexed."
    */
   static final int ERR_BLOB_INDEX = 77;

   /**
    * "A BLOB can't be in the where clause."
    */
   static final int ERR_BLOB_WHERE = 78;

   /**
    * "A BLOB can't be converted to a string."
    */
   static final int ERR_BLOB_STRING = 79;

   /**
    * "Blobs types can't be in ORDER BY or GROUP BY clauses.
    */
   static final int ERR_BLOB_ORDER_GROUP = 80;

   /**
    * "It is not possible to compare BLOBs."
    */
   static final int ERR_COMP_BLOBS = 81;

   /**
    * "It is only possible to insert or update a BLOB through prepared statements."
    */
   static final int ERR_BLOBS_PREPARED = 82;

   /**
    * Total Litebase possible errors.
    */
   static final int TOTAL_ERRORS = 83;

   // Error tables
   static final String[] errorMsgs_en = new String[TOTAL_ERRORS];
   static final String[] errorMsgs_pt = new String[TOTAL_ERRORS];

   static
   {
      // Some errors have space at the end. This can't be changed.

      // English messages.
      // General errors.
      errorMsgs_en[ERR_MESSAGE_START] = "Error: ";
      errorMsgs_en[ERR_MESSAGE_POSITION] = " Near position ";
      errorMsgs_en[ERR_SYNTAX_ERROR] = "Syntax error.";

      // Limit errors.
      errorMsgs_en[ERR_MAX_TABLE_NAME_LENGTH] = "Table name too big: must be <= 23.";
      errorMsgs_en[ERR_FIELDS_OVERFLOW] = "The maximum number of fields in a SELECT clause was exceeded.";

      // Column errors.
      errorMsgs_en[ERR_UNKNOWN_COLUMN] = "Unknown column: ";
      errorMsgs_en[ERR_INVALID_COLUMN_NAME] = "Invalid column name: ";
      errorMsgs_en[ERR_INVALID_COLUMN_NUMBER] = "Invalid column number: ";
      errorMsgs_en[ERR_COLUMN_DOESNOT_HAVE_AN_INDEX] = "The following column(s) does (do) not have an associated index ";
      errorMsgs_en[ERR_AMBIGUOUS_COLUMN_NAME] = "Column name in field list is ambiguous: ";
      errorMsgs_en[ERR_COLUMN_NOT_FOUND] = "Column not found: ";
      errorMsgs_en[ERR_DUPLICATED_COLUMN_NAME] = "Duplicated column name ";

      // Primary key errors.
      errorMsgs_en[ERR_PRIMARY_KEY_ALREADY_DEFINED] = "A primary key was already defined for this table.";
      errorMsgs_en[ERR_TABLE_DOESNOT_HAVE_PRIMARY_KEY] = "Table does not have a primary key.";
      errorMsgs_en[ERR_STATEMENT_CREATE_DUPLICATED_PK] = "Statement creates a duplicated primary key in ";

      // Type errors.
      errorMsgs_en[ERR_INCOMPATIBLE_TYPES] = "Incompatible types";
      errorMsgs_en[ERR_FIELD_SIZE_IS_NOT_INT] = "Field size must be a positive interger value.";

      // Number of fields errors.
      errorMsgs_en[ERR_NUMBER_FIELDS_AND_VALUES_DOES_NOT_MATCH] = "The number of fields does not match the number of values ";
      errorMsgs_en[ERR_NUMBER_VALUES_DIFF_TABLE_DEFINITION] = "The given number of values does not match the table definition.";

      // Default value errors.
      errorMsgs_en[ERR_LENGTH_DEFAULT_VALUE_IS_BIGGER] = "Length of default value is bigger than column size.";

      // Driver errors.
      errorMsgs_en[ERR_DRIVER_CLOSED] = "This driver instance was closed and can't be used anymore. Please get a new instance of it.";
      errorMsgs_en[ERR_RESULTSET_CLOSED] = "ResultSet already closed!";
      errorMsgs_en[ERR_ROWITERATOR_CLOSED] = "RowIterator already closed!";
      errorMsgs_en[ERR_RESULTSETMETADATA_CLOSED] = "ResultSetMetaData can't be used after the ResultSet is closed.";
      errorMsgs_en[ERR_LITEBASEPRCDLL_NOT_FOUND] = "Can't find native methods implementation for LitebaseConnection. Please install Litebase.dll/prc " 
                                                 + "file.";
      errorMsgs_en[ERR_INVALID_CRID] = "The application id must be 4 characters long.";
      errorMsgs_en[ERR_INVALID_INC] = "The increment must be greater than 0 or -1.";
      
      // Table errors.
      errorMsgs_en[ERR_TABLE_NAME_NOT_FOUND] = "Table name not found: ";
      errorMsgs_en[ERR_TABLE_ALREADY_CREATED] = "Table already created: ";
      errorMsgs_en[ERR_WRONG_STRING_FORMAT] =  "It is not possible to open a table within a connection with a different string format.";

      // ROWID error.
      errorMsgs_en[ERR_ROWID_CANNOT_BE_CHANGED] = "ROWID can't be changed by the user!";

      // Prepared Statement errors.
      errorMsgs_en[ERR_QUERY_DOESNOT_RETURN_RESULTSET] = "SQL statement does not return result set.";
      errorMsgs_en[ERR_QUERY_DOESNOT_PERFORM_UPDATE] = "SQL statement does not perform updates in the database.";
      errorMsgs_en[ERR_NOT_ALL_PARAMETERS_DEFINED] = "Not all parameters of the query had their values defined.";
      errorMsgs_en[ERR_PARAMETER_NOT_DEFINED] = "A value was not defined for the parameter ";
      errorMsgs_en[ERR_INVALID_PARAMETER_INDEX] = "Invalid parameter index.";

      // Rename errors.
      errorMsgs_en[ERR_TABLE_ALREADY_EXIST] = "Can't rename table. This table already exists: ";
      errorMsgs_en[ERR_COLUMN_ALREADY_EXIST] = "Column already exists: ";

      // Alias errors.
      errorMsgs_en[ERR_NOT_UNIQUE_ALIAS_TABLE] = "Not unique table/alias: ";
      errorMsgs_en[ERR_DUPLICATE_ALIAS] = "This alias is already being used in this expression: ";
      errorMsgs_en[ERR_REQUIRED_ALIAS] = "An alias is required for the aggregate function column.";

      // Litebase.execute() error.
      errorMsgs_en[ERR_ONLY_CREATE_TABLE_INDEX_IS_ALLOWED] = "Only CREATE TABLE and CREATE INDEX can be used in Litebase.execute().";

      // Order by and group by errors.
      errorMsgs_en[ERR_ORDER_GROUPBY_MUST_MATCH] = "ORDER BY and GROUP BY clauses must match.";
      errorMsgs_en[ERR_VIRTUAL_COLUMN_ON_GROUPBY] = "No support for virtual columns in SQL queries with GROUP BY clause.";

      // Function errors.
      errorMsgs_en[ERR_AGGREG_FUNCTION_ISNOT_ON_SELECT] = "All non-aggregation function columns in the SELECT clause must also be in the GROUP BY " 
                                                        + "clause.";
      errorMsgs_en[ERR_IS_NOT_AGGREG_FUNCTION] = " is not an aggregation function. All fields present in a HAVING "
            + "clause must be listed in the SELECT clause as aliased aggregation funtions.";
      errorMsgs_en[ERR_CANNOT_MIX_AGGREG_FUNCTION] = "Can't mix aggregation functions with real columns in the SELECT clause without a GROUP BY " 
                                                   + "clause.";
      errorMsgs_en[ERR_CANNOT_HAVE_AGGREG_AND_NO_GROUPBY] = "Can't have aggregation functions with ORDER BY clause and no GROUP BY clause.";
      errorMsgs_en[ERR_WAS_NOT_LISTED_ON_AGGREG_FUNCTION] = " was not listed in the SELECT clause. All fields present in a HAVING "
            + "clause must be listed in the SELECT clause as aliased aggregation funtions.";
      errorMsgs_en[ERR_SUM_AVG_WITH_DATE_DATETIME] = "SUM and AVG aggregation functions are not used with DATE and DATETIME type fields.";

      // DATETIME error.
      errorMsgs_en[ERR_VALUE_ISNOT_DATETIME] = "Value is not a DATETIME: ";

      // Index error.
      errorMsgs_en[ERR_INDEX_ALREADY_CREATED] = "Index already created for column ";
      errorMsgs_en[ERR_DROP_PRIMARY_KEY] = "Can't drop a primary key index with drop index.";
      errorMsgs_en[ERR_INDEX_LARGE] = "Index too large. It can't have more than 65534 nodes.";
      
      // NOT NULL errors.
      errorMsgs_en[ERR_PK_CANT_BE_NULL] = "Primary key can't have null.";
      errorMsgs_en[ERR_FIELD_CANT_BE_NULL] = "Field can't be null: ";
      errorMsgs_en[ERR_PARAM_NULL] = "A parameter in a where clause can't be null.";
      
      // Result set errors.
      errorMsgs_en[ERR_RS_INV_POS] = "ResultSet in invalid record position.";
      errorMsgs_en[ERR_RS_DEC_PLACES_START] = "Invalid value for decimal places: ";
      errorMsgs_en[ERR_RS_DEC_PLACES_END] = ". Must be in the range -1 to 40.";

      // File errors.
      errorMsgs_en[ERR_CANT_READ] = "Can't read from table.";
      errorMsgs_en[ERR_CANT_LOAD_NODE] = "Can't load node: index corrupted.";
      errorMsgs_en[ERR_TABLE_CORRUPTED] = "Table is corrupted: ";
      errorMsgs_en[ERR_TABLE_NOT_CLOSED] = "Table not closed properly: ";
      errorMsgs_en[ERR_TABLE_CLOSED] = "A properly closed table can't be used in recoverTable(): "; // juliana@222_2
      errorMsgs_en[ERR_IDX_RECORD_DEL] = "Can't find index record position on delete.";
      errorMsgs_en[ERR_WRONG_VERSION] = "The table format is incompatible with Litebase version. Please update your tables.";
      errorMsgs_en[ERR_WRONG_PREV_VERSION] = "The table format is not the previous one: "; // juliana@220_11
      errorMsgs_en[ERR_INVALID_PATH] = "Invalid path: "; // juliana@214_1
      errorMsgs_en[ERR_DB_NOT_FOUND] = "Database not found."; // juliana@226_10
      
      // BLOB errors.
      errorMsgs_en[ERR_BLOB_TOO_BIG] = "The total size of a blob can't be greater then 10 Mb.";
      errorMsgs_en[ERR_INVALID_MULTIPLIER] = "This is not a valid size multiplier.";
      errorMsgs_en[ERR_BLOB_PRIMARY_KEY] = "A blob type can't be part of a primary key.";
      errorMsgs_en[ERR_BLOB_INDEX] = "A BLOB column can't be indexed.";
      errorMsgs_en[ERR_BLOB_WHERE] = "A BLOB can't be in the where clause.";
      errorMsgs_en[ERR_BLOB_STRING] = "A BLOB can't be converted to a string.";
      errorMsgs_en[ERR_BLOB_ORDER_GROUP] = "Blobs types can't be in ORDER BY or GROUP BY clauses.";
      errorMsgs_en[ERR_COMP_BLOBS] = "It is not possible to compare BLOBs.";
      errorMsgs_en[ERR_BLOBS_PREPARED] = "It is only possible to insert or update a BLOB through prepared statements using setBlob().";

      // Portuguese messages.
      // General errors.
      errorMsgs_pt[ERR_MESSAGE_START] = "Erro: ";
      errorMsgs_pt[ERR_MESSAGE_POSITION] = ". Pr�ximo � posi��o ";
      errorMsgs_pt[ERR_SYNTAX_ERROR] = "Erro de sintaxe.";

      // Limit errors.
      errorMsgs_pt[ERR_MAX_TABLE_NAME_LENGTH] = "Nome da tabela muito grande: deve ser <= 23.";
      errorMsgs_pt[ERR_FIELDS_OVERFLOW] = "O n�mero m�ximo de campos na cl�usula SELECT foi excedido.";

      // Column errors.
      errorMsgs_pt[ERR_UNKNOWN_COLUMN] = "Coluna desconhecida ";
      errorMsgs_pt[ERR_INVALID_COLUMN_NAME] = "Nome de coluna inv�lido: ";
      errorMsgs_pt[ERR_INVALID_COLUMN_NUMBER] = "N�mero de coluna inv�lido: ";
      errorMsgs_pt[ERR_COLUMN_DOESNOT_HAVE_AN_INDEX] = "A(s) coluna(s) a seguir n�o tem (t�m) um ind�ce associado ";
      errorMsgs_pt[ERR_AMBIGUOUS_COLUMN_NAME] = "Nome de coluna amb�guo: ";
      errorMsgs_pt[ERR_COLUMN_NOT_FOUND] = "Coluna n�o encontrada: ";
      errorMsgs_pt[ERR_DUPLICATED_COLUMN_NAME] = "Nome de coluna duplicado ";

      // Primary key errors.
      errorMsgs_pt[ERR_PRIMARY_KEY_ALREADY_DEFINED] = "Uma chave primaria j� foi definida para esta tabela.";
      errorMsgs_pt[ERR_TABLE_DOESNOT_HAVE_PRIMARY_KEY] = "Tabela n�o tem chave prim�ria.";
      errorMsgs_pt[ERR_STATEMENT_CREATE_DUPLICATED_PK] = "Comando cria uma chave prim�ria duplicada em ";

      // Type errors.
      errorMsgs_pt[ERR_INCOMPATIBLE_TYPES] = "Tipos incompat�veis";
      errorMsgs_pt[ERR_FIELD_SIZE_IS_NOT_INT] = "Tamanho do campo deve ser um valor inteiro positivo.";

      // Number of fields errors.
      errorMsgs_pt[ERR_NUMBER_FIELDS_AND_VALUES_DOES_NOT_MATCH] = "O n�mero de campos � diferente do n�mero de valores ";
      errorMsgs_pt[ERR_NUMBER_VALUES_DIFF_TABLE_DEFINITION] = "O n�mero de valores dado n�o coincide com a defini��o da tabela.";

      // Default value errors.
      errorMsgs_pt[ERR_LENGTH_DEFAULT_VALUE_IS_BIGGER] = "Tamanho do valor padr�o � maior que o tamanho definido para a coluna.";

      // Driver errors.
      errorMsgs_pt[ERR_DRIVER_CLOSED] = "Esta inst�ncia do driver est� fechada e n�o pode ser mais utilizada. Por favor, obtenha uma nova " 
                                      + "inst�ncia.";
      errorMsgs_pt[ERR_RESULTSET_CLOSED] = "ResultSet j� est� fechado!";
      errorMsgs_pt[ERR_ROWITERATOR_CLOSED] = "RowIterator j� est� fechado!";
      errorMsgs_pt[ERR_RESULTSETMETADATA_CLOSED] = "ResultSetMetaData n�o pode ser usado depois que o ResultSet estiver fechado.";
      errorMsgs_pt[ERR_LITEBASEPRCDLL_NOT_FOUND] = "N�o � poss�vel encontrar a implementa��o dos m�todos nativos "
            + "para o LitebaseConnection. Por favor, instale o arquivo Litebase.dll/prc.";
      errorMsgs_pt[ERR_INVALID_CRID] = "O id da aplica��o de ter 4 characteres.";
      errorMsgs_pt[ERR_INVALID_INC] = "O incremento deve ser maior do que 0 ou -1.";
      
      // Table errors.
      errorMsgs_pt[ERR_TABLE_NAME_NOT_FOUND] = "Nome da tabela n�o encontrado: ";
      errorMsgs_pt[ERR_TABLE_ALREADY_CREATED] = "Tabela j� existe: ";
      errorMsgs_pt[ERR_WRONG_STRING_FORMAT] =  "N�o � poss�vel abrir uma tabela com uma conex�o com um tipo de strings diferente.";

      // ROWID error.
      errorMsgs_pt[ERR_ROWID_CANNOT_BE_CHANGED] = "ROWID n�o pode ser mudado pelo usu�rio!";

      // Prepared Statement errors.
      errorMsgs_pt[ERR_QUERY_DOESNOT_RETURN_RESULTSET] = "Comando SQL n�o retorna um ResultSet.";
      errorMsgs_pt[ERR_QUERY_DOESNOT_PERFORM_UPDATE] = "Comando SQL n�o executa uma atualiza��o no banco de dados.";
      errorMsgs_pt[ERR_NOT_ALL_PARAMETERS_DEFINED] = "Nem todos os par�metros da consulta tiveram seus valores definidos.";
      errorMsgs_pt[ERR_PARAMETER_NOT_DEFINED] = "N�o foi definido um valor para o par�metro ";
      errorMsgs_pt[ERR_INVALID_PARAMETER_INDEX] = "�ndice de par�metro inv�lido.";

      // Rename errors.
      errorMsgs_pt[ERR_TABLE_ALREADY_EXIST] = "N�o � poss�vel renomear a tabela. Esta tabela j� existe: ";
      errorMsgs_pt[ERR_COLUMN_ALREADY_EXIST] = "Coluna j� existe: ";

      // Alias errors.
      errorMsgs_pt[ERR_NOT_UNIQUE_ALIAS_TABLE] = "Nome de tabela/alias repetido: ";
      errorMsgs_pt[ERR_DUPLICATE_ALIAS] = "Este alias j� est� sendo utilizado no sql: ";
      errorMsgs_pt[ERR_REQUIRED_ALIAS] = "Um alias � necess�rio para colunas com fun��o de agrega��o.";

      // Litebase.execute() error.
      errorMsgs_pt[ERR_ONLY_CREATE_TABLE_INDEX_IS_ALLOWED] = "Apenas CREATE TABLE e CREATE INDEX s�o permitidos no Litebase.execute().";

      // Order by and group by errors.
      errorMsgs_pt[ERR_ORDER_GROUPBY_MUST_MATCH] = "Cl�usulas ORDER BY e GROUP BY devem coincidir.";
      errorMsgs_pt[ERR_VIRTUAL_COLUMN_ON_GROUPBY] = "SQL com cl�usula GROUP BY n�o tem suporte para colunas virtuais.";

      // Function errors.
      errorMsgs_pt[ERR_AGGREG_FUNCTION_ISNOT_ON_SELECT] = "Todas colunas que n�os�o fun��es de "
            + "agrega��o na cl�usula SELECT devem estar na cl�usula GROUP BY.";
      errorMsgs_pt[ERR_IS_NOT_AGGREG_FUNCTION] = " n�o � uma fun��o de agrega��o. Todos as colunas "
            + "da cl�usula HAVING devem ser listadas no SELECT utilizando alias.";
      errorMsgs_pt[ERR_CANNOT_MIX_AGGREG_FUNCTION] = "N�o � possivel misturar colunas reais e de agrega��o no SELECT sem cl�usula GROUP BY.";
      errorMsgs_pt[ERR_CANNOT_HAVE_AGGREG_AND_NO_GROUPBY] = "N�o � poss�vel ter fun��es de agrega��o com cl�usula ORDER BY sem cl�usula GROUP BY.";
      errorMsgs_pt[ERR_WAS_NOT_LISTED_ON_AGGREG_FUNCTION] = " n�o foi listado no SELECT. Todas "
            + "as colunas da cl�usula HAVING devem ser listadas no SELECT utilizando alias.";
      errorMsgs_pt[ERR_SUM_AVG_WITH_DATE_DATETIME] = "Fun��es de agrega��o SUM e AVG n�o s�o usadas com colunas do tipo DATE e DATETIME.";

      // DATETIME error.
      errorMsgs_pt[ERR_VALUE_ISNOT_DATETIME] = "Valor n�o � um tipo DATETIME v�lido: ";

      // Index error.
      errorMsgs_pt[ERR_INDEX_ALREADY_CREATED] = "�ndice j� criado para a coluna ";
      errorMsgs_pt[ERR_DROP_PRIMARY_KEY] = "N�o � poss�vel remover uma chave prim�ria usando drop index.";
      errorMsgs_pt[ERR_INDEX_LARGE] = "�ndice muito grande. Ele n�o pode ter mais do que 65534 n�s.";
      
      // NOT NULL errors.
      errorMsgs_pt[ERR_PK_CANT_BE_NULL] = "Chave prim�ria n�o pode ter NULL.";
      errorMsgs_pt[ERR_FIELD_CANT_BE_NULL] = "Coluna n�o pode ser NULL: ";
      errorMsgs_pt[ERR_PARAM_NULL] = "Um par�metro em uma where clause n�o pode ser NULL.";
      
      // Result set errors.
      errorMsgs_pt[ERR_RS_INV_POS] = "ResultSet em uma posi��o de registro inv�lida.";
      errorMsgs_pt[ERR_RS_DEC_PLACES_START] = "Valor inv�lido para casas decimais: ";
      errorMsgs_pt[ERR_RS_DEC_PLACES_END] = ". Deve ficar entre - 1 e 40.";

      // File errors.
      errorMsgs_pt[ERR_CANT_READ] = "N�o � poss�vel ler da tabela.";
      errorMsgs_pt[ERR_CANT_LOAD_NODE] = "N�o � poss�vel carregar o n�: �ndice corrompido.";
      errorMsgs_pt[ERR_TABLE_CORRUPTED] = "Tabela est� corrompida: ";
      errorMsgs_pt[ERR_TABLE_NOT_CLOSED] = "Tabela n�o foi fechada corretamente: ";
      errorMsgs_pt[ERR_TABLE_CLOSED] = "Uma tabela fechada corretamente n�o pode ser usada no recoverTable(): "; // juliana@222_2
      errorMsgs_pt[ERR_IDX_RECORD_DEL] = "N�o � poss�vel achar a posi��o de registro no �ndice na exclus�o.";
      errorMsgs_pt[ERR_WRONG_VERSION] = "O formato de tabela n�o � compat�vel com a vers�o do Litebase. Por favor, atualize suas tabelas.";
      errorMsgs_pt[ERR_WRONG_PREV_VERSION] = "O formato de tabela n�o � o anterior: "; // juliana@220_11
      errorMsgs_pt[ERR_INVALID_PATH] = "Caminho inv�lido: "; // juliana@214_1
      errorMsgs_pt[ERR_DB_NOT_FOUND] = "Base de dados n�o encontrada."; // juliana@226_10
      
      // BLOB errors.
      errorMsgs_pt[ERR_BLOB_TOO_BIG] = "O tamanho total de um BLOB n�o pode ser maior do que 10 Mb.";
      errorMsgs_pt[ERR_INVALID_MULTIPLIER] = "O multiplicador de tamanho n�o � v�lido.";
      errorMsgs_pt[ERR_BLOB_PRIMARY_KEY] = "Um tipo BLOB n�o pode ser parte de uma chave prim�ria.";
      errorMsgs_pt[ERR_BLOB_INDEX] = "Uma coluna do tipo BLOB n�o pode ser indexada.";
      errorMsgs_pt[ERR_BLOB_WHERE] = "Um BLOB n�o pode estar na cl�usula WHERE.";
      errorMsgs_pt[ERR_BLOB_STRING] = "Um BLOB n�o pode ser convertido em uma string.";
      errorMsgs_pt[ERR_BLOB_ORDER_GROUP] = "Tipos BLOB n�o podem estar em cl�usulas ORDER BY ou GROUP BY.";
      errorMsgs_pt[ERR_COMP_BLOBS] = "N�o � poss�vel comparar BLOBs.";
      errorMsgs_pt[ERR_BLOBS_PREPARED] = "S� � poss�vel inserir ou atualizar um BLOB atrav�s prepared statements usando setBlob().";
   }

   /**
    * Gets the correct error message.
    * 
    * @param messageNumber The error message code.
    * @return The string with the desired error message.
    */
   static String getMessage(int messageNumber)
   {
      if (LitebaseConnection.language == LitebaseConnection.LANGUAGE_PT)
         return errorMsgs_pt[messageNumber];
      return errorMsgs_en[messageNumber];
   }
}
