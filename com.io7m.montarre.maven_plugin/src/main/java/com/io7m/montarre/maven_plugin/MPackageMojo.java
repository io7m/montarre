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


package com.io7m.montarre.maven_plugin;

import com.io7m.lanark.core.RDottedName;
import com.io7m.montarre.api.MArchitectureName;
import com.io7m.montarre.api.MCategoryName;
import com.io7m.montarre.api.MException;
import com.io7m.montarre.api.MFileFilter;
import com.io7m.montarre.api.MFileName;
import com.io7m.montarre.api.MHash;
import com.io7m.montarre.api.MHashAlgorithm;
import com.io7m.montarre.api.MHashValue;
import com.io7m.montarre.api.MManifest;
import com.io7m.montarre.api.MMetadata;
import com.io7m.montarre.api.MModule;
import com.io7m.montarre.api.MOperatingSystemName;
import com.io7m.montarre.api.MPackageDeclaration;
import com.io7m.montarre.api.MPackageName;
import com.io7m.montarre.api.MPlatform;
import com.io7m.montarre.api.MPlatformDependentModule;
import com.io7m.montarre.api.MPlatformFileFilter;
import com.io7m.montarre.api.MResource;
import com.io7m.montarre.api.MShortName;
import com.io7m.montarre.api.MVendorName;
import com.io7m.montarre.api.io.MPackageWriterType;
import com.io7m.montarre.io.MPackageWriters;
import com.io7m.seltzer.api.SStructuredErrorType;
import com.io7m.verona.core.VersionException;
import com.io7m.verona.core.VersionParser;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

/**
 * The "package" mojo.
 */

@Mojo(
  name = "package",
  defaultPhase = PACKAGE,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public final class MPackageMojo extends AbstractMojo
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MPackageMojo.class);

  @Parameter(
    required = false,
    name = "skip",
    property = "montarre.skip",
    defaultValue = "false"
  )
  private boolean skip;

  /**
   * The output package.
   */

  @Parameter(
    defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}.mpk"
  )
  private String outputFile;

  /**
   * The package name.
   */

  @Parameter(defaultValue = "${project.artifactId}")
  private String packageName;

  /**
   * The package version.
   */

  @Parameter(defaultValue = "${project.version}")
  private String packageVersion;

  /**
   * The main module.
   */

  @Parameter(required = true)
  private String mainModule;

  /**
   * The package description.
   */

  @Parameter(defaultValue = "${project.description}")
  private String description;

  /**
   * The package copyright.
   */

  @Parameter(defaultValue = "")
  private String copyright = "";

  /**
   * The package site.
   */

  @Parameter(defaultValue = "${project.url}")
  private String site;

  /**
   * The package name.
   */

  @Parameter(required = true)
  private String shortName;

  /**
   * The vendor name.
   */

  @Parameter(required = true)
  private String vendorName;

  /**
   * The package required JDK version.
   */

  @Parameter(required = true)
  private long requiredJDKVersion;

  /**
   * The SPDX license identifier.
   */

  @Parameter(required = true)
  private String license;

  /**
   * The platform library filters.
   */

  @Parameter()
  private List<PlatformLibrary> platformLibraries = new ArrayList<>();

  /**
   * The library filter.
   */

  @Parameter()
  private Library libraries = new Library();

  /**
   * The resource files.
   */

  @Parameter()
  private List<Resource> resources = new ArrayList<>();

  /**
   * The categories.
   */

  @Parameter()
  private List<String> categories = new ArrayList<>();

  /**
   * Access to the Maven project.
   */

  @Parameter(
    defaultValue = "${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  /**
   * Access to the Maven session.
   */

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Component
  private DependencyGraphBuilder dependencyGraphBuilder;

  @Component
  private MavenProjectHelper mavenProjectHelper;

  private List<MPlatformFileFilter> platformFileFilters;
  private MFileFilter libraryFileFilter;
  private MPackageDeclaration packageV;
  private Set<Artifact> collectedLibraries;
  private SortedMap<MPlatform, Set<Artifact>> collectedPlatformDependentArtifacts;

  /**
   * The "package" mojo.
   */

  public MPackageMojo()
  {
    this.collectedLibraries =
      new HashSet<>();
    this.collectedPlatformDependentArtifacts =
      new TreeMap<>();
  }

  private static MHash hashOf(
    final File file)
    throws IOException, NoSuchAlgorithmException
  {
    try (var stream = Files.newInputStream(file.toPath())) {
      final var digest =
        MessageDigest.getInstance("SHA-256");
      try (var dStream = new DigestInputStream(stream, digest)) {
        dStream.transferTo(OutputStream.nullOutputStream());
      }
      return new MHash(
        new MHashAlgorithm("SHA-256"),
        new MHashValue(HexFormat.of().formatHex(digest.digest()))
      );
    }
  }

  static void logStructuredError(
    final Logger logger,
    final SStructuredErrorType<?> error)
  {
    logger.error("{}: {}", error.errorCode(), error.message());
    for (final var entry : error.attributes().entrySet()) {
      logger.error("  {}: {}", entry.getKey(), entry.getValue());
    }
    error.exception()
      .ifPresent(throwable -> logger.error("  Exception: ", throwable));
  }

  @Override
  public void execute()
    throws MojoExecutionException
  {
    try {
      this.libraryFileFilter =
        this.createFileFilters();
      this.platformFileFilters =
        this.createPlatformFileFilters();

      this.collectAllArtifacts();
      this.buildPackageDeclaration();

      final var output =
        Paths.get(this.outputFile);
      final var outputTmp =
        Paths.get(this.outputFile + ".tmp");

      final var writers = new MPackageWriters();
      try (var writer =
             writers.create(output, outputTmp, this.packageV)) {
        this.writeFiles(writer);
      }

      this.mavenProjectHelper.attachArtifact(
        this.project,
        "mpk",
        output.toFile()
      );
    } catch (final Exception e) {
      if (e instanceof SStructuredErrorType<?> s) {
        logStructuredError(LOG, s);
      } else {
        LOG.error("{}", e.getMessage());
      }
      throw new MojoExecutionException(e);
    }
  }

  private void writeFiles(
    final MPackageWriterType writer)
    throws MException
  {
    this.writeResources(writer);
    this.writeModules(writer);
    this.writePlatformDependentModules(writer);
  }

  private void writeResources(
    final MPackageWriterType writer)
    throws MException
  {
    for (final var resource : this.resources) {
      final var entryName =
        "meta/%s".formatted(resource.getEntryName());
      final var resourcePath =
        Paths.get(resource.getFile());

      writer.addFile(new MFileName(entryName), resourcePath);
    }
  }

  private void writePlatformDependentModules(
    final MPackageWriterType writer)
    throws MException
  {
    for (var platform : this.collectedPlatformDependentArtifacts.keySet()) {
      final var sorted =
        this.collectedPlatformDependentArtifacts.get(platform)
          .stream()
          .sorted(Comparator.comparing(o -> o.getFile().getName()))
          .toList();

      for (var artifact : sorted) {
        final var file = artifact.getFile();
        final var fileName = file.getName();
        final var entryName = "lib/" + fileName;
        writer.addFile(new MFileName(entryName), file.toPath());
      }
    }
  }

  private void writeModules(
    final MPackageWriterType writer)
    throws MException
  {
    final var sorted =
      this.collectedLibraries.stream()
        .sorted(Comparator.comparing(o -> o.getFile().getName()))
        .toList();

    for (var artifact : sorted) {
      final var file = artifact.getFile();
      final var fileName = file.getName();
      final var entryName = "lib/" + fileName;
      writer.addFile(new MFileName(entryName), file.toPath());
    }
  }

  private void buildPackageDeclaration()
    throws VersionException, IOException, NoSuchAlgorithmException
  {
    final var builder =
      MPackageDeclaration.builder();

    final var metaBuilder =
      MMetadata.builder()
        .setCopyright(this.copyright.trim())
        .setDescription(this.description.trim())
        .setLicense(this.license.trim())
        .setMainModule(this.mainModule.trim())
        .setPackageName(new MPackageName(new RDottedName(this.packageName.trim())))
        .setRequiredJDKVersion(this.requiredJDKVersion)
        .setSiteURI(URI.create(this.site.trim()))
        .setVersion(VersionParser.parse(this.packageVersion.trim()))
        .setShortName(new MShortName(this.shortName.trim()))
        .setVendorName(new MVendorName(this.vendorName.trim()));

    for (var category : this.categories) {
      metaBuilder.addCategories(new MCategoryName(category));
    }

    builder.setMetadata(metaBuilder.build());

    final var manifestBuilder = MManifest.builder();
    this.buildManifestForResources(manifestBuilder);
    this.buildManifestForModules(manifestBuilder);
    this.buildManifestForPlatformDependentModules(manifestBuilder);

    builder.setManifest(manifestBuilder.build());
    this.packageV = builder.build();
  }

  private void buildManifestForResources(
    final MManifest.Builder manifestBuilder)
    throws IOException, NoSuchAlgorithmException
  {
    for (final var resource : this.resources) {
      final var entryName =
        "meta/%s".formatted(resource.getEntryName());
      final var sha256 =
        hashOf(Paths.get(resource.getFile()).toFile());

      manifestBuilder.addItems(
        new MResource(new MFileName(entryName), sha256, resource.getRole()));
    }
  }

  private void buildManifestForPlatformDependentModules(
    final MManifest.Builder manifestBuilder)
    throws IOException, NoSuchAlgorithmException
  {
    for (var platform : this.collectedPlatformDependentArtifacts.keySet()) {
      for (var artifact : this.collectedPlatformDependentArtifacts.get(platform)) {
        final var file = artifact.getFile();
        final var fileName = file.getName();
        final var entryName = "lib/" + fileName;
        final var sha256 = hashOf(file);

        manifestBuilder.addItems(
          new MPlatformDependentModule(
            new MFileName(entryName),
            sha256,
            platform.operatingSystem(),
            platform.architecture()
          )
        );
      }
    }
  }

  private void buildManifestForModules(
    final MManifest.Builder manifestBuilder)
    throws IOException, NoSuchAlgorithmException
  {
    for (var artifact : this.collectedLibraries) {
      final var file = artifact.getFile();
      final var fileName = file.getName();
      final var entryName = "lib/" + fileName;
      final var sha256 = hashOf(file);
      manifestBuilder.addItems(new MModule(new MFileName(entryName), sha256));
    }
  }

  private List<MPlatformFileFilter> createPlatformFileFilters()
  {
    return this.platformLibraries.stream()
      .map(x -> {
        return MPlatformFileFilter.builder()
          .setOperatingSystem(new MOperatingSystemName(x.getOperatingSystem()))
          .setArchitecture(new MArchitectureName(x.getArchitecture()))
          .addAllIncludes(
            x.getIncludes()
              .stream()
              .map(Pattern::compile)
              .toList()
          )
          .addAllExcludes(
            x.getExcludes()
              .stream()
              .map(Pattern::compile)
              .toList()
          )
          .build();
      })
      .toList();
  }

  private MFileFilter createFileFilters()
  {
    return MFileFilter.builder()
      .addAllIncludes(
        this.libraries.getIncludes()
          .stream()
          .map(Pattern::compile)
          .toList()
      )
      .addAllExcludes(
        this.libraries.getExcludes()
          .stream()
          .map(Pattern::compile)
          .toList()
      )
      .build();
  }

  private void collectAllArtifacts()
    throws MojoExecutionException
  {
    this.classifyArtifact(this.project.getArtifact());

    for (final Artifact artifact : this.project.getArtifacts()) {
      this.classifyArtifact(artifact);
    }

    for (final Artifact artifact : this.project.getAttachedArtifacts()) {
      this.classifyArtifact(artifact);
    }

    final ProjectBuildingRequest request =
      new DefaultProjectBuildingRequest(this.session.getProjectBuildingRequest());

    request.setProject(this.project);

    try {
      Objects.requireNonNull(request, "request");

      final DependencyNode node =
        this.dependencyGraphBuilder.buildDependencyGraph(request, artifact -> {
          return true;
        });

      this.collectDependencies(node);
    } catch (final DependencyGraphBuilderException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private boolean classifyArtifact(
    final Artifact artifact)
  {
    final var file = artifact.getFile();
    if (file == null) {
      return false;
    }

    final var fileName = file.getName();
    if (this.libraryFileFilter.evaluate(fileName)) {
      LOG.info(
        "Included: Artifact {} ({}) marked as platform-independent library",
        artifact,
        fileName
      );
      this.collectedLibraries.add(artifact);
      return true;
    }

    for (var filter : this.platformFileFilters) {
      if (filter.evaluate(fileName)) {
        final var platform =
          new MPlatform(filter.architecture(), filter.operatingSystem());

        var existing = this.collectedPlatformDependentArtifacts.get(platform);
        if (existing == null) {
          existing = new HashSet<>();
        }
        existing.add(artifact);
        this.collectedPlatformDependentArtifacts.put(platform, existing);

        LOG.info(
          "Included: Artifact {} ({}) marked as platform {} library",
          artifact,
          fileName,
          platform
        );
        return true;
      }
    }

    LOG.info(
      "Excluded: Artifact {} ({}) is either excluded, or not included",
      artifact,
      fileName
    );
    return false;
  }

  private void collectDependencies(
    final DependencyNode node)
  {
    if (!this.classifyArtifact(node.getArtifact())) {
      return;
    }

    for (final DependencyNode child : node.getChildren()) {
      this.collectDependencies(child);
    }
  }
}
