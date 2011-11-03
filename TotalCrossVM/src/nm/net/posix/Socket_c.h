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



#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>

#if defined(ANDROID) || defined(__SYMBIAN32__)
#include <netinet/in.h>
#endif

typedef int SOCKET;
#define INVALID_SOCKET 0

static Err socketClose(SOCKET* socketHandle);

/*****   socketCreate   *****
 *
 * struct sockaddr_in
 * struct hostent
 * fd_set
 * struct timeval
 *
 * FD_ZERO
 * FD_SET
 * FD_ISSET
 *
 * socket
 * gethostbyname
 * htons
 * fcntl
 * connect
 * select
 * errno
 *
 * OS Versions: POSIX compliant systems.
 * Header: sys/socket.h.
 * Link Library: libc.
 *
 *************************************/

#if defined(darwin) // fdie@iphone_DNS_issue fix, see http://www.saurik.com/id/3
#include <mach-o/nlist.h>
#include <stdbool.h>
#endif

#if defined(darwin)
#ifdef __cplusplus
extern "C" {
#endif

int iphoneSocket(char* hostname, struct sockaddr *in_addr);

#ifdef __cplusplus
}
#endif
#endif

static Err socketCreate(SOCKET* socketHandle, CharP hostname, int32 port, int32 timeout, bool noLinger, bool *isUnknownHost)
{
   Err err;
   int hostSocket;
   int res, valopt;
   struct sockaddr_in destination_sin;
   struct hostent *phostent;
   long arg;
   fd_set fdWriteSet;
   struct timeval timeout_val;
   socklen_t lon;

#if defined(darwin) // fdie@iphone_DNS_issue
   static bool fix_installed = false;
   if (!fix_installed)
   {
      struct nlist nl[2];
      memset(nl, 0, sizeof(nl));
      nl[0].n_un.n_name = (char *) "_useMDNSResponder";
      nlist("/usr/lib/libc.dylib", nl);
      if (nl[0].n_type != N_UNDF) * (bool *) nl[0].n_value = false;
      fix_installed = true;
   }
#endif

   // Create socket
   if ((hostSocket = socket(AF_INET, SOCK_STREAM, 0)) < 0)
      goto Error;

   // Set non-blocking
   arg = fcntl(hostSocket, F_GETFL, NULL);
   arg |= O_NONBLOCK;
   fcntl(hostSocket, F_SETFL, arg);

#if defined (darwin)
   res = iphoneSocket(hostname, (struct sockaddr*) &destination_sin);
   if (res < 0)
   {
      debug("res: %d", res);
      *isUnknownHost = true;
      goto Error;
   }
#else   
   // Fill out the server socket's address information.
   destination_sin.sin_family = AF_INET;
   //destination_sin.sin_addr.s_addr = htonl(INADDR_ANY);
   if ((destination_sin.sin_addr.s_addr = inet_addr(hostname)) == -1)
   {
      if ((phostent = gethostbyname(hostname)) == null)
      {
         if (h_errno == HOST_NOT_FOUND || h_errno == NO_ADDRESS || h_errno == NO_DATA)
            *isUnknownHost = true;
         goto Error;
      }
      else
      {
         destination_sin.sin_family = phostent->h_addrtype;
         xmemmove(&(destination_sin.sin_addr.s_addr), phostent->h_addr, phostent->h_length);
      }
   }
#endif   
   // Convert to network ordering.
   destination_sin.sin_port = htons((uint16) port);

//   destination_sin.sin_family = AF_INET;
//   destination_sin.sin_port = htons(2000);
//   destination_sin.sin_addr.s_addr = inet_addr("192.168.0.1");

   res = connect(hostSocket, (struct sockaddr *)&destination_sin, sizeof(destination_sin));
   if (res < 0)
   {
      if (errno != EINPROGRESS)
         goto Error;

      timeout_val.tv_sec = (timeout>=1000 ? timeout/1000 : 0 );
      timeout_val.tv_usec = (timeout<1000 ? timeout : timeout%1000)*1000;
      FD_ZERO(&fdWriteSet);
      FD_SET(hostSocket, &fdWriteSet);
      if (select(hostSocket+1, NULL, &fdWriteSet, NULL, &timeout_val) <= 0)
         goto Error;
      if (!FD_ISSET(hostSocket, &fdWriteSet))
         goto Error;
      lon = sizeof(int);
      getsockopt(hostSocket, SOL_SOCKET, SO_ERROR, (void*)(&valopt), &lon);
      if (valopt)
      {
         errno = valopt;
         goto Error;
      }
   }

   // flsobral@tc100b4: keep the socket in non-blocking mode.
   /*
      // Set to blocking mode again...
      arg = fcntl(hostSocket, F_GETFL, NULL);
      arg &= (~O_NONBLOCK);
      fcntl(hostSocket, F_SETFL, arg);
   */

   *socketHandle = hostSocket;
   return NO_ERROR;

Error: // Close the socket.
   err = errno;
   socketClose(&hostSocket);
   return err;
}

/*****   socketClose   *****
 *
 * shutdown
 * close
 * errno
 *
 * OS Versions: POSIX compliant systems.
 * Header: sys/socket.h.
 * Link Library: libc.
 *
 *************************************/
static Err socketClose(SOCKET* socketHandle)
{
   if (shutdown(*socketHandle, SHUT_WR) < 0)
      goto error;
   if (close(*socketHandle) < 0)
      goto error;

   *socketHandle = INVALID_SOCKET;
   return NO_ERROR;

error:
   return errno;
}

/*****   socketReadWriteBytes   *****
 *
 * fd_set
 * struct timeval
 *
 * FD_ZERO
 * FD_SET
 *
 * select
 * recv
 * send
 * errno
 *
 * OS Versions: POSIX compliant systems.
 * Header: sys/socket.h.
 * Link Library: libc.
 *
 *************************************/
static Err socketReadWriteBytes(SOCKET socketHandle, int32 timeoutMillis, CharP buf, int32 start, int32 count, int32* retCount, bool isRead)
{
   fd_set fdSet;
   struct timeval timeout;
   int32 result;
   int32 timeoutLeft = timeoutMillis >= 0 ? timeoutMillis : 0; // we should enter the read/write loop at least once, even if timeout <= 0
   int32 timestamp;
   *retCount = 0; // clear bytes count

   timestamp = getTimeStamp();

   FD_ZERO(&fdSet);
   FD_SET(socketHandle, &fdSet);

   timeout.tv_sec  = timeoutMillis / 1000;
   timeout.tv_usec = (timeoutMillis % 1000) * 1000;

   if (isRead)
      result = select(socketHandle+1, &fdSet, null, null, &timeout); //Read
   else
      result = select(socketHandle+1, null, &fdSet, null, &timeout); //Write

   if (result == 0)  // select timed out, update retCount and return
   {
      *retCount = -2; // indicating it was a timeout.
      return NO_ERROR; // do not display platform specific error.
   }
   else if (result < 0 || !FD_ISSET(socketHandle, &fdSet)) // select failed or socket is not set, return the error code.
      goto Error;

   do // flsobral@tc113_33: loop back only on EWOULDBLOCK, respecting the timeout.
   {
      if (isRead)
      {
         result = recv(socketHandle, buf + start + *retCount, count - *retCount, 0); //Read
         if (result == 0) // flsobral@tc110_2: if result is 0, the connection was gracefully closed by the remote host.
            return NO_ERROR;
      }
      else
         result = send(socketHandle, buf + start + *retCount, count - *retCount, 0); //Write
      *retCount += result; // update the number of bytes write/read
   } while (result < 0 && errno == EWOULDBLOCK && (getTimeStamp() - timestamp < timeoutMillis));

   if (result < 0 && errno != EWOULDBLOCK)
      goto Error;

   return NO_ERROR;
Error:
   return errno;
}
