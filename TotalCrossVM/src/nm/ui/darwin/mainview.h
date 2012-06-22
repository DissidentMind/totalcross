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



#ifndef MAINVIEW_H
#define MAINVIEW_H

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <UIKit/UITextView.h>

#include "GraphicsPrimitives.h"
#import "childview.h"
#import "sipargs.h"

/*
 * fdie@ add iPhone full screen support.
 * The UIView can't be created during the app launching insofar its creation have to be deferred until
 * we started the video mode where the we choose between fullscreen and normal mode.
 * But it also requires to call the video switching code in the app main thread. The app mainloop() is
 * executed in a event dispatching thread that is not able to successfully call into the UIKit framework :-(
 */

@interface SSize : NSObject
{
   CGSize ssize;
}

- (id)set:(CGSize)size;
- (CGSize)get;

@end

@interface MainView : UIViewController
{
   NSMutableArray* _events;
   NSLock* _lock;
   ChildView *child_view;
   UITextView* kbd;
   NSRange lastRange;
}

- (void)initEvents;
- (void)addEvent:(NSDictionary*)event;
- (bool)isEventAvailable;
- (NSArray*)getEvents;
- (void)showSIP:(SipArguments*)args;
- (void)destroySIP;
- (BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text;
- (void) keyboardDidShow: (NSNotification *)notif;
- (void) keyboardDidHide: (NSNotification *)notif;

@end

typedef struct
{
   __unsafe_unretained UIWindow  *_window;
   __unsafe_unretained MainView  *_mainview;
   __unsafe_unretained ChildView *_childview;
} TScreenSurfaceEx, *ScreenSurfaceEx;

#endif
