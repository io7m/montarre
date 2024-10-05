/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.montarre.xml.internal.v1;

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.montarre.api.MArchitectureName;
import com.io7m.montarre.api.MFileName;
import com.io7m.montarre.api.MHash;
import com.io7m.montarre.api.MHashAlgorithm;
import com.io7m.montarre.api.MHashValue;
import com.io7m.montarre.api.MOperatingSystemName;
import com.io7m.montarre.api.MPlatformDependentModule;
import org.xml.sax.Attributes;

/**
 * A parser.
 */

public final class Mx1PlatformDependentModule
  implements BTElementHandlerType<Object, MPlatformDependentModule>
{
  private MPlatformDependentModule data;

  /**
   * A parser.
   *
   * @param context The context
   */

  public Mx1PlatformDependentModule(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.data = new MPlatformDependentModule(
      new MFileName(attributes.getValue("File")),
      new MHash(
        new MHashAlgorithm(
          attributes.getValue("HashAlgorithm")
        ),
        new MHashValue(
          attributes.getValue("HashValue")
        )
      ),
      new MOperatingSystemName(
        attributes.getValue("OperatingSystem")
      ),
      new MArchitectureName(
        attributes.getValue("Architecture")
      )
    );
  }

  @Override
  public MPlatformDependentModule onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.data;
  }
}
