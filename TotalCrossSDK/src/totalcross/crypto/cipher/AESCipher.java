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

// $Id: AESCipher.java,v 1.7 2011-01-04 13:19:16 guich Exp $

package totalcross.crypto.cipher;

import java.security.GeneralSecurityException;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import totalcross.crypto.CryptoException;

/**
 * This class implements the AES cryptographic cipher.
 *  <p>
    If you get a <code>totalcross.crypto.CryptoException: Illegal key size</code>, you must
    download the strong cryptography files from <a href='http://www.totalcross.com/etc/securejars.zip' class=mail>here</a> <b>AFTER</b>
    understanding that you are elligible to do so as stated in <a href='http://java.sun.com/j2se/1.4.2/jre/README' class=mail>here</a> 
    (search for 'Unlimited Strength Java Cryptography Extension' - installation instructions are inside that topic).
 */
public class AESCipher extends Cipher
{
   public final String getAlgorithm()
   {
      return "AES";
   }

   public final int getBlockLength()
   {
      // Applet may support 16 or 32, like axtls
      // javax.crypto.Cipher cipher = (javax.crypto.Cipher)cipherRef;
      // return (cipher != null) ? cipher.getBlockSize() : 16;
      return 16;
   }
   
   protected final void doReset() throws CryptoException
   {
      String transf = "AES";
      switch (chaining)
      {
         case CHAINING_NONE:
            transf += "/NONE";
            break;
         case CHAINING_ECB:
            transf += "/ECB";
            break;
         case CHAINING_CBC:
            transf += "/CBC";
            break;
      }
      switch (padding)
      {
         case PADDING_NONE:
            transf += "/NoPadding";
            break;
         case PADDING_PKCS1:
            transf += "/PKCS1Padding";
            break;
         case PADDING_PKCS5:
            transf += "/PKCS5Padding";
      }
      
      try
      {
         javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(transf);
         cipherRef = cipher;
         
         keyRef = new SecretKeySpec(((AESKey)key).getData(), "AES");
         
         int mode = operation == OPERATION_ENCRYPT ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE;
         cipher.init(mode, (java.security.Key)keyRef, iv == null ? null : new IvParameterSpec(iv));
         if (iv == null)
            iv = cipher.getIV();
      }
      catch (GeneralSecurityException e)
      {
         throw new CryptoException(e.getMessage());
      }
   }
   
   protected final byte[] process(byte[] data) throws CryptoException
   {
      try
      {
         javax.crypto.Cipher cipher = (javax.crypto.Cipher)cipherRef;
         return cipher.doFinal(data);
      }
      catch (GeneralSecurityException e)
      {
         throw new CryptoException(e.getMessage());
      }
   }
   
   protected final boolean isKeySupported(Key key, int operation)
   {
      return key instanceof AESKey;
   }
   
   protected final boolean isChainingSupported(int chaining)
   {
      return true;
   }
   
   protected final boolean isPaddingSupported(int padding)
   {
      return padding == PADDING_PKCS5;
   }
}
