#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const POM_PATH = path.join(__dirname, "..", "oid4vp-java", "pom.xml");

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
  const pomContent = fs.readFileSync(POM_PATH, "utf8");
  const versionMatch = pomContent.match(
    /<artifactId>oid4vp-parent<\/artifactId>\s*\n\s*<version>([0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?)<\/version>/
  );
  if (!versionMatch) {
    throw new Error("Could not find project version in oid4vp-java/pom.xml");
  }
  return versionMatch[1];
}

function updatePomVersion(newVersion) {
  let pomContent = fs.readFileSync(POM_PATH, "utf8");
  let versionUpdated = false;
  pomContent = pomContent.replace(
    /(<artifactId>oid4vp-parent<\/artifactId>\s*\n\s*<version>)[0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?(<\/version>)/,
    (match, prefix, suffix) => {
      versionUpdated = true;
      return prefix + newVersion + suffix;
    }
  );

  if (!versionUpdated) {
    throw new Error("Could not update version in oid4vp-java/pom.xml");
  }

  fs.writeFileSync(POM_PATH, pomContent);
  log(`✓ Updated oid4vp-java/pom.xml version to ${newVersion}`, "green");
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

function calculateReleaseVersion(currentVersion, type) {
  const baseVersion = currentVersion.replace(/-SNAPSHOT$/, "");
  const parts = baseVersion.split(".").map(Number);

  switch (type) {
    case "patch":
      return baseVersion;
    case "minor":
      parts[1]++;
      parts[2] = 0;
      break;
    case "major":
      parts[0]++;
      parts[1] = 0;
      parts[2] = 0;
      break;
    default:
      throw new Error(
        `Invalid version type: ${type}. Use 'patch', 'minor', or 'major'`
      );
  }

  return parts.join(".");
}

function commitAndTag(version) {
  try {
    execSync("git status", { stdio: "ignore" });

    execSync("git add oid4vp-java/pom.xml CHANGELOG.md");

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
    log(`  git add oid4vp-java/pom.xml CHANGELOG.md`, "cyan");
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
    execSync("git add oid4vp-java/pom.xml");
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
    log(`Set oid4vp-java/pom.xml to ${snapshotVersion} manually and push.`, "yellow");
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
    log("  • Update oid4vp-java/pom.xml to the release version", "cyan");
    log("  • Update CHANGELOG.md", "cyan");
    log("  • Commit and tag the release", "cyan");
    log("  • Set next development version with -SNAPSHOT suffix", "cyan");
    log("  • Commit (without tag) and push", "cyan");
    process.exit(1);
  }

  const currentVersion = getCurrentVersion();
  const releaseVersion = calculateReleaseVersion(currentVersion, versionType);

  log(`Current version: ${currentVersion}`, "blue");
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
  log("  ✓ oid4vp-java/pom.xml", "green");
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
    log(`3. Set oid4vp-java/pom.xml to ${parts.join(".")}-SNAPSHOT for next development`, "cyan");
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
