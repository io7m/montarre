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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.montarre.api.MManifest;
import com.io7m.montarre.api.MManifestItemType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import static com.io7m.montarre.xml.internal.v1.Mx1.element;

/**
 * A parser.
 */

public final class Mx1Manifest
  implements BTElementHandlerType<MManifestItemType, MManifest>
{
  private final ArrayList<MManifestItemType> items;

  /**
   * A parser.
   *
   * @param context The context
   */

  public Mx1Manifest(
    final BTElementParsingContextType context)
  {
    this.items = new ArrayList<>();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends MManifestItemType>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Resource"),
        Mx1Resource::new
      ),
      Map.entry(
        element("Module"),
        Mx1Module::new
      ),
      Map.entry(
        element("PlatformDependentModule"),
        Mx1PlatformDependentModule::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final MManifestItemType result)
  {
    this.items.add(result);
  }

  @Override
  public MManifest onElementFinished(
    final BTElementParsingContextType context)
  {
    return MManifest.builder()
      .addAllItems(
        this.items.stream()
          .sorted(Comparator.comparing(MManifestItemType::file))
          .toList())
      .build();
  }
}
