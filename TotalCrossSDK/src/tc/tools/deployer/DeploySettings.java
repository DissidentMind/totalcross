/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/



package tc.tools.deployer;

import totalcross.io.File;
import totalcross.sys.*;
import totalcross.ui.font.*;
import totalcross.util.*;

public class DeploySettings
{
   public static final String UnknownVendor = "Unknown Vendor"; // fdie@570_96

   // constants for including the vm and/or litebase in a package 
   public static final int PACKAGE_DEMO = 1;
   public static final int PACKAGE_RELEASE = 2;
   public static final int PACKAGE_LITEBASE = 4;
   public static int packageType;
   public static String folderTotalCrossSDKDistVM, folderTotalCrossVMSDistVM,
                        folderLitebaseSDKDistLIB, folderLitebaseVMSDistLIB;
   
   public static String tczFileName;
   public static String targetDir;
   public static String filePrefix;
   public static Vector entriesList;
   public static Bitmaps bitmaps;
   public static String appTitle;
   public static String applicationId,appVersion,companyInfo,companyContact;
   public static String mainClassName;
   public static String commandLine = "";
   public static boolean isMainWindow;
   public static boolean isJarOrZip;
   public static boolean testClass; // guich@tc114_54
   public static boolean isFullScreen;
   public static String  fullScreenPlatforms;
   public static String fontTCZ = Font.OLD_FONT_SET+".tcz";
   
   public static boolean autoStart;

   public static byte[] rasKey;
   public static boolean autoSign;
   public static String autoSignPassword;
   public static boolean quiet=true; // to set to false, pass /v(erbose) to tc.Deploy

   public static final char DIRSEP = java.io.File.pathSeparatorChar;
   public static final char SLASH = java.io.File.separatorChar;

   public static String [] classPath; // environment variable
   public static String [] path; // guich@tc111_19 environment variable
   public static byte[] defaultArgument = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890".getBytes();
   public static String homeDir, etcDir, binDir, distDir;
   public static String currentDir;
   public static String baseDir;
   public static String mainClassDir;
   public static Vector exclusionList = new Vector(10);
   public static IntVector appletFontSizes = new IntVector();
   public static String javaVersion = System.getProperty("java.version");
   public static String osName = System.getProperty("os.name").toLowerCase();
   public static boolean isBuggyJDKVersionForSynchronizedKeyword; // guich@tc120_0

   public static boolean appIdSpecifiedAsArgument;
   public static boolean inputFileWasTCZ;

   public static boolean excludeOptionSet;
   public static String showBBPKGName,showBBPKGRoot;

   public static String mainPackage;

   public static boolean isTotalCrossJarDeploy;

   /////////////////////////////////////////////////////////////////////////////////////
   public static void init() throws Exception
   {
      String completeVersion = javaVersion;
      // guich@tc114_6: force java version 1.6 or above
      int firstDot = javaVersion.indexOf('.');
      int secondDot = javaVersion.indexOf('.',firstDot+1);
      if (secondDot != firstDot)
         javaVersion = javaVersion.substring(0,secondDot);
      float ver = Float.parseFloat(javaVersion);
      if (ver < 1.6)
         throw new DeployerException("Error: the Deployer requires JDK 1.6 or above!");
      // guich@tc120_0: check the minor version and make sure no one uses 1.6.0_06
      int subver = 100;
      try {subver = Integer.parseInt(completeVersion.substring(completeVersion.indexOf('_')+1));} catch (Exception e) {}
      isBuggyJDKVersionForSynchronizedKeyword = ver < 1.7 && ver > 1.5 && subver <= 6; // 1.6 and <= 6 ?
      
      exclusionList.addElement("totalcross/");
      exclusionList.addElement("java/");
      exclusionList.addElement("[");
      exclusionList.addElement("tc/tools/");
      exclusionList.addElement("litebase/");
      exclusionList.addElement("ras/");
      exclusionList.addElement("net/rim/");
      appletFontSizes.addElement(12);

      currentDir = System.getProperty("user.dir").replace('\\','/');
      // parse the classpath environment variable
      String cp = tc.Deploy.bootClassPath != null ? tc.Deploy.bootClassPath : System.getProperty("java.class.path");
      classPath = Convert.tokenizeString(cp,DIRSEP);
      if (classPath == null)
         classPath = new String[]{"."};
      Utils.removeQuotes(classPath);
      // parse the path environment variable
      cp = System.getenv("path"); // guich@tc111_19
      if (cp == null)
         cp = System.getenv("PATH"); // guich@tc122_29: linux is case-sensitive
      if (cp != null)
         path  = Convert.tokenizeString(cp,DIRSEP);
      if (path != null)
         Utils.removeQuotes(path);
      // setup the etc directory
      if (new File(currentDir+"/etc").exists())
         etcDir = currentDir+"/etc";
      else
      if (new File(System.getenv("GIT_HOME")+"/TotalCross/TotalCrossSDK/etc").exists()) // check first at p:
         etcDir = System.getenv("GIT_HOME")+"/TotalCross/TotalCrossSDK/etc";
      else
      if ((etcDir = Utils.findPath("etc",false)) == null)
      {
         String tchome = System.getenv("TOTALCROSS_HOME");
         if (tchome != null)
            etcDir = tchome.replace('\\','/')+"/etc";
         else
         if (isWindows()) // if in windows, search in all drives
         {
            for (char i = 'c'; i <= 'z'; i++)
               if (new File(i+":/TotalCrossSDK/etc").exists())
               {
                  etcDir = i+":/TotalCrossSDK/etc";
                  break;
               }
         }
         else
         if (new File("/TotalCrossSDK/etc").exists()) // check on the root of the current drive
            etcDir = "/TotalCrossSDK/etc";
         if (etcDir == null || !new File(etcDir).exists())
            throw new DeployerException("Can't find path for etc folder. Add TotalCrossSDK to the classpath or set the TOTALCROSS_HOME environment variable.");
      }
      if (!etcDir.endsWith("/"))
         etcDir += "/";
      etcDir = Convert.replace(etcDir, "//","/").replace('\\','/');
      homeDir = Convert.replace(etcDir, "/etc/","/");
      binDir = Convert.replace(etcDir, "/etc/","/bin/");
      distDir = Convert.replace(etcDir, "/etc/", "/dist/");
      System.out.println("TotalCross SDK version "+Settings.versionStr+" running on "+osName+" with JDK "+javaVersion);
      System.out.println("Etc directory: "+etcDir); // keep this always visible, its a very important information

      // find the demo and release folders for totalcross and litebase
      String f;
      f = System.getenv("TOTALCROSS_DEMO");
      if (f != null)
         folderTotalCrossSDKDistVM = Convert.appendPath(f, "dist/vm/");
      f = System.getenv("TOTALCROSS_RELEASE");
      if (f != null)
         folderTotalCrossVMSDistVM = Convert.appendPath(f, "dist/vm/");
      f = System.getenv("LITEBASE_DEMO");
      if (f != null)
         folderLitebaseSDKDistLIB = Convert.appendPath(f, "dist/lib/");
      f = System.getenv("LITEBASE_RELEASE");
      if (f != null)
         folderLitebaseVMSDistLIB = Convert.appendPath(f, "dist/lib/");

      if (folderTotalCrossSDKDistVM == null)
         folderTotalCrossSDKDistVM = distDir+"vm/";
      if (folderTotalCrossVMSDistVM == null)
         folderTotalCrossVMSDistVM = Convert.replace(folderTotalCrossSDKDistVM, "SDK","VMS");
      String lbhome = System.getenv("LITEBASE_HOME");
      if (lbhome == null)
         lbhome = Utils.getParent(Utils.getParentFolder(etcDir))+"/LitebaseSDK";
      lbhome = lbhome.replace('\\','/');
      if (folderLitebaseSDKDistLIB == null)
         folderLitebaseSDKDistLIB = lbhome + "/dist/lib/";
      if (folderLitebaseVMSDistLIB == null)
         folderLitebaseVMSDistLIB = Convert.replace(folderLitebaseSDKDistLIB, "SDK","VMS");
      // check if folders exist
      if (!new File(folderLitebaseSDKDistLIB).exists()) 
         folderLitebaseSDKDistLIB = null;
      if (!new File(folderLitebaseVMSDistLIB).exists()) 
         folderLitebaseVMSDistLIB = null;
      if (!new File(folderTotalCrossSDKDistVM).exists()) 
         folderTotalCrossSDKDistVM = null;
      if (!new File(folderTotalCrossVMSDistVM).exists()) 
         folderTotalCrossVMSDistVM = null;
      
      Utils.fillExclusionList(); //flsobral@tc115: exclude files contained in jar files in the classpath.
   }

   /** From 5.21, return 5 */
   public static int getAppVersionHi()
   {
      if (appVersion == null)
         return 1;
      try
      {
         if (appVersion.indexOf('.') > 0)
            return Convert.toInt(appVersion.substring(0, appVersion.indexOf('.')));
         return Convert.toInt(appVersion);
      } catch (InvalidNumberException ine) {return 1;}
   }

   /** From 5.21, returns 21 */
   public static int getAppVersionLo()
   {
      if (appVersion == null || appVersion.indexOf('.') < 0)
         return 0;
      try
      {
         return Convert.toInt(appVersion.substring(0, appVersion.indexOf('.')));
      } catch (InvalidNumberException ine) {return 0;}
   }
   
   public static boolean isWindows()
   {
      return osName.indexOf("windows") >= 0;
   }
   
   public static boolean isUnix()
   {
      return osName.indexOf("linux") >= 0 || osName.indexOf("unix") >= 0;
   }
   
   public static boolean isMac()
   {
      return osName.indexOf("mac") >= 0;
   }
   
   public static String appendDotExe(String s) // guich@tc115_85
   {
      return isUnix() || isMac() ? s : (s+".exe");
   }

   public static boolean isFullScreenPlatform(String plat) // guich@tc120_59
   {
      if (fullScreenPlatforms == null)
         return isFullScreen; // guich@tc126_32: return what user decided for all platforms
      return fullScreenPlatforms.indexOf(plat) >= 0;
   }
   
   public static String pathAddQuotes(String p)
   {
      return isWindows() ? '"'+p+'"' : p;
   }
}
