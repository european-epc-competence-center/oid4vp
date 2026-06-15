#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const JAVA_ROOT = path.join(__dirname, "..", "oid4vp-java");
const PARENT_POM = path.join(JAVA_ROOT, "pom.xml");
const MODULE_POMS = [
  path.join(JAVA_ROOT, "oid4vp-core", "pom.xml"),
  path.join(JAVA_ROOT, "oid4vp-spring", "pom.xml"),
  path.join(JAVA_ROOT, "oid4vp-spring-boot-starter", "pom.xml"),
];
const ALL_POMS = [PARENT_POM, ...MODULE_POMS];
const POM_GIT_PATHS = ALL_POMS.map((pomPath) =>
  path.relative(path.join(__dirname, ".."), pomPath)
);

const PARENT_VERSION_PATTERN =
  /(<artifactId>oid4vp-parent<\/artifactId>\s*\n\s*<version>)[0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?(<\/version>)/;
const PARENT_VERSION_READ_PATTERN =
  /<artifactId>oid4vp-parent<\/artifactId>\s*\n\s*<version>([0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?)<\/version>/;

const colors = {
  reset: "\x1b[0m",
  bright: "\x1b[1m",
  red: "\x1b[31m",
  green: "\x1b[32m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  cyan: "\x1b[36m",
};

function log(message, color = "reset") {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function getCurrentVersion() {
  const pomContent = fs.readFileSync(PARENT_POM, "utf8");
  const versionMatch = pomContent.match(PARENT_VERSION_READ_PATTERN);
  if (!versionMatch) {
    throw new Error("Could not find project version in oid4vp-java/pom.xml");
  }
  return versionMatch[1];
}

function updatePomVersion(newVersion) {
  for (const pomPath of ALL_POMS) {
    let pomContent = fs.readFileSync(pomPath, "utf8");
    let versionUpdated = false;

    pomContent = pomContent.replace(
      PARENT_VERSION_PATTERN,
      (match, prefix, suffix) => {
        versionUpdated = true;
        return prefix + newVersion + suffix;
      }
    );

    if (!versionUpdated) {
      throw new Error(`Could not update version in ${pomPath}`);
    }

    fs.writeFileSync(pomPath, pomContent);
    log(`✓ Updated ${path.relative(path.join(__dirname, ".."), pomPath)} to ${newVersion}`, "green");
  }
}

function updateChangelog(version, changes) {
  const changelogPath = path.join(__dirname, "..", "CHANGELOG.md");
  const today = new Date().toISOString().split("T")[0];

  let changelog = "";
  if (fs.existsSync(changelogPath)) {
    changelog = fs.readFileSync(changelogPath, "utf8");
  } else {
    changelog = `# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

`;
  }

  const newEntry = `## [${version}] - ${today}

${changes}

`;

  const lines = changelog.split("\n");
  let insertIndex = lines.findIndex((line) => line.startsWith("## ["));

  if (insertIndex !== -1 && lines[insertIndex].includes("[Unreleased]")) {
    let endOfUnreleasedIndex = insertIndex + 1;
    while (
      endOfUnreleasedIndex < lines.length &&
      !lines[endOfUnreleasedIndex].startsWith("## [")
    ) {
      endOfUnreleasedIndex++;
    }
    lines.splice(insertIndex + 1, endOfUnreleasedIndex - insertIndex - 1);
    lines.splice(insertIndex + 1, 0, "", newEntry.trimEnd());
  } else {
    if (insertIndex === -1) insertIndex = lines.length;
    lines.splice(insertIndex, 0, "## [Unreleased]", "", newEntry.trimEnd());
  }

  fs.writeFileSync(changelogPath, lines.join("\n"));
  log(`✓ Updated CHANGELOG.md with version ${version}`, "green");
}

function compareSemver(a, b) {
  const partsA = a.split(".").map(Number);
  const partsB = b.split(".").map(Number);

  for (let i = 0; i < 3; i++) {
    const diff = (partsA[i] || 0) - (partsB[i] || 0);
    if (diff !== 0) {
      return diff;
    }
  }

  return 0;
}

function getLastReleasedVersion() {
  try {
    execSync("git status", { stdio: "ignore" });
  } catch {
    return null;
  }

  try {
    const tags = execSync('git tag -l "v*"', { encoding: "utf8" })
      .trim()
      .split("\n")
      .filter(Boolean)
      .map((tag) => tag.replace(/^v/, ""))
      .sort(compareSemver);

    return tags.length > 0 ? tags[tags.length - 1] : null;
  } catch {
    return null;
  }
}

function calculateReleaseVersion(currentVersion, type, lastReleasedVersion) {
  const baseVersion = currentVersion.replace(/-SNAPSHOT$/, "");

  switch (type) {
    case "patch":
      return baseVersion;
    case "minor": {
      const base = lastReleasedVersion || baseVersion;
      const parts = base.split(".").map(Number);
      parts[1]++;
      parts[2] = 0;
      return parts.join(".");
    }
    case "major": {
      const base = lastReleasedVersion || baseVersion;
      const parts = base.split(".").map(Number);
      parts[0]++;
      parts[1] = 0;
      parts[2] = 0;
      return parts.join(".");
    }
    default:
      throw new Error(
        `Invalid version type: ${type}. Use 'patch', 'minor', or 'major'`
      );
  }
}

function commitAndTag(version) {
  try {
    execSync("git status", { stdio: "ignore" });

    execSync(`git add ${POM_GIT_PATHS.join(" ")} CHANGELOG.md`);

    execSync(`git commit -m "chore: release version ${version}"`, {
      stdio: "inherit",
    });

    execSync(`git tag -a v${version} -m "Release version ${version}"`, {
      stdio: "inherit",
    });

    log(`✓ Created git commit and tag v${version}`, "green");

    log("📤 Pushing changes to remote...", "yellow");
    execSync("git push && git push --tags", {
      stdio: "inherit",
    });

    log("✓ Successfully pushed changes and tags to remote", "green");
    return true;
  } catch (error) {
    log("⚠ Git operations failed. Please commit and tag manually:", "yellow");
    log(`  git add ${POM_GIT_PATHS.join(" ")} CHANGELOG.md`, "cyan");
    log(`  git commit -m "chore: release version ${version}"`, "cyan");
    log(`  git tag -a v${version} -m "Release version ${version}"`, "cyan");
    log(`  git push && git push --tags`, "cyan");
    return false;
  }
}

function setNextSnapshotVersion(releaseVersion) {
  const parts = releaseVersion.split(".").map(Number);
  parts[2]++;
  const snapshotVersion = `${parts.join(".")}-SNAPSHOT`;

  log(`\n📝 Setting next development version to ${snapshotVersion}...`, "yellow");

  updatePomVersion(snapshotVersion);

  try {
    execSync(`git add ${POM_GIT_PATHS.join(" ")}`);
    execSync(
      `git commit -m "chore: prepare next development iteration ${snapshotVersion}"`,
      { stdio: "inherit" }
    );

    log(`✓ Set next development version to ${snapshotVersion}`, "green");

    log("📤 Pushing snapshot version to remote...", "yellow");
    execSync("git push", { stdio: "inherit" });

    log("✓ Successfully pushed snapshot version to remote", "green");
    return true;
  } catch (error) {
    log("\n⚠ Release was tagged successfully, but snapshot version setup failed.", "yellow");
    log(`Set all pom.xml files to ${snapshotVersion} manually and push.`, "yellow");
    return false;
  }
}

function extractUnreleasedChanges() {
  const changelogPath = path.join(__dirname, "..", "CHANGELOG.md");

  if (!fs.existsSync(changelogPath)) {
    return null;
  }

  const changelog = fs.readFileSync(changelogPath, "utf8");
  const lines = changelog.split("\n");

  const unreleasedIndex = lines.findIndex(
    (line) => line.startsWith("## [") && line.includes("[Unreleased]")
  );

  if (unreleasedIndex === -1) {
    return null;
  }

  let endIndex = unreleasedIndex + 1;
  while (endIndex < lines.length && !lines[endIndex].startsWith("## [")) {
    endIndex++;
  }

  const unreleasedLines = lines.slice(unreleasedIndex + 1, endIndex);

  while (unreleasedLines.length > 0 && unreleasedLines[0].trim() === "") {
    unreleasedLines.shift();
  }
  while (
    unreleasedLines.length > 0 &&
    unreleasedLines[unreleasedLines.length - 1].trim() === ""
  ) {
    unreleasedLines.pop();
  }

  const changes = unreleasedLines.join("\n");
  return changes.trim() || null;
}

function main() {
  log("🚀 oid4vp Release Tool", "bright");
  log("======================\n", "bright");

  const args = process.argv.slice(2);
  const versionType = args[0];
  const skipGit = args.includes("--skip-git");

  if (!versionType || !["patch", "minor", "major"].includes(versionType)) {
    log("Usage: npm run release <patch|minor|major> [--skip-git]", "red");
    log("\nExamples:", "cyan");
    log("  npm run release patch    # 0.1.0-SNAPSHOT -> 0.1.0 -> 0.1.1-SNAPSHOT", "cyan");
    log("  npm run release minor    # 0.1.0-SNAPSHOT -> 0.2.0 -> 0.2.1-SNAPSHOT", "cyan");
    log("  npm run release major    # 0.1.0-SNAPSHOT -> 1.0.0 -> 1.0.1-SNAPSHOT", "cyan");
    log("\nThis will:", "yellow");
    log("  • Update all oid4vp-java pom.xml files to the release version", "cyan");
    log("  • Update CHANGELOG.md", "cyan");
    log("  • Commit and tag the release", "cyan");
    log("  • Set next development version with -SNAPSHOT suffix", "cyan");
    log("  • Commit (without tag) and push", "cyan");
    process.exit(1);
  }

  const currentVersion = getCurrentVersion();
  const lastReleasedVersion = getLastReleasedVersion();
  const releaseVersion = calculateReleaseVersion(
    currentVersion,
    versionType,
    lastReleasedVersion
  );

  log(`Current version: ${currentVersion}`, "blue");
  if (lastReleasedVersion) {
    log(`Last released version: ${lastReleasedVersion}`, "blue");
  }
  if (
    lastReleasedVersion &&
    versionType !== "patch" &&
    compareSemver(
      currentVersion.replace(/-SNAPSHOT$/, ""),
      lastReleasedVersion
    ) > 0
  ) {
    log(
      `Note: ${versionType} release is based on last git tag (v${lastReleasedVersion}), not the SNAPSHOT in pom.xml`,
      "yellow"
    );
  }
  log(`Release version: ${releaseVersion}`, "green");
  log(`Release type: ${versionType}`, "cyan");
  log("");

  const changes = extractUnreleasedChanges();

  if (!changes) {
    log("⚠ No unreleased changelog entries found in CHANGELOG.md", "yellow");
    log(
      "Please add entries under an '## [Unreleased]' section first.",
      "yellow"
    );
    log("\nExample format:", "cyan");
    log("## [Unreleased]", "cyan");
    log("", "cyan");
    log("### Added", "cyan");
    log("- New feature description", "cyan");
    process.exit(1);
  }

  log("📝 Found unreleased changes:", "green");
  log(changes, "cyan");
  log("");

  updatePomVersion(releaseVersion);
  updateChangelog(releaseVersion, changes);

  log("\n📋 Release Summary:", "bright");
  log(`Version: ${currentVersion} -> ${releaseVersion}`, "blue");
  log("Files updated:", "blue");
  for (const pomPath of POM_GIT_PATHS) {
    log(`  ✓ ${pomPath}`, "green");
  }
  log("  ✓ CHANGELOG.md", "green");

  if (!skipGit) {
    log("\n📝 Creating git commit and tag for release...", "yellow");
    const releaseSuccess = commitAndTag(releaseVersion);

    if (!releaseSuccess) {
      process.exit(1);
    }

    setNextSnapshotVersion(releaseVersion);
  }

  log("\n🎉 Release preparation complete!", "bright");
  log("\nNext steps:", "yellow");
  if (!skipGit) {
    log("1. The release commit and tag have been pushed to remote", "green");
    log("2. The release workflow will automatically:", "green");
    log("   - Publish the artifact to Maven Central", "cyan");
    log("   - Create a GitHub release with changelog content", "cyan");
    log("3. Development continues with the next SNAPSHOT version", "green");
  } else {
    log("1. Review the updated files", "cyan");
    log("2. Commit, tag, and push when ready", "cyan");
    const parts = releaseVersion.split(".").map(Number);
    parts[2]++;
    log(`3. Set all pom.xml files to ${parts.join(".")}-SNAPSHOT for next development`, "cyan");
  }
}

process.on("SIGINT", () => {
  log("\n\n❌ Release cancelled by user", "red");
  process.exit(130);
});

try {
  main();
} catch (error) {
  log(`\n❌ Error: ${error.message}`, "red");
  process.exit(1);
}
