// Copyright (C) 2000-2013 SuperWaba Ltda.
// Copyright (C) 2014-2020 TotalCross Global Mobile Platform Ltda.
//
// SPDX-License-Identifier: LGPL-2.1-only
package tc.tools.converter.bytecode;

public class BC040_dload_2 extends LoadLocal {
  public BC040_dload_2() {
    super(2, DOUBLE);
    stackInc = 2;
  }
}
