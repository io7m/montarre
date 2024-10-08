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


package com.io7m.montarre.api;

import java.util.Comparator;
import java.util.Objects;

/**
 * A platform.
 *
 * @param architecture    The architecture
 * @param operatingSystem The operating system name
 */

public record MPlatform(
  MArchitectureName architecture,
  MOperatingSystemName operatingSystem)
  implements Comparable<MPlatform>
{
  @Override
  public String toString()
  {
    return "%s:%s".formatted(this.architecture, this.operatingSystem);
  }

  /**
   * A platform.
   *
   * @param architecture    The architecture
   * @param operatingSystem The operating system name
   */

  public MPlatform
  {
    Objects.requireNonNull(architecture, "architecture");
    Objects.requireNonNull(operatingSystem, "operatingSystem");
  }

  @Override
  public int compareTo(
    final MPlatform other)
  {
    return Comparator.comparing(MPlatform::architecture)
      .thenComparing(MPlatform::operatingSystem)
      .compare(this, other);
  }
}
