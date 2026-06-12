#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const POM_PATH = path.join(__dirname, "..", "oid4vp-java", "pom.xml");

// ANSI color codes for colored output
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

function parsePomVersion(pomContent) {
  const match = pomContent.match(
    /<artifactId>oid4vp<\/artifactId>\s*\n\s*<version>([^<]+)<\/version>/
  );
  if (!match) {
    throw new Error("Could not find project version in oid4vp-java/pom.xml");
  }

  const rawVersion = match[1].trim();
  const isSnapshot = rawVersion.endsWith("-SNAPSHOT");
  const baseVersion = isSnapshot
    ? rawVersion.slice(0, -"-SNAPSHOT".length)
    : rawVersion;

  return { rawVersion, baseVersion, isSnapshot };
}

function getCurrentVersion() {
  const pomContent = fs.readFileSync(POM_PATH, "utf8");
  return parsePomVersion(pomContent);
}

function updatePomVersion(newVersion) {
  let pomContent = fs.readFileSync(POM_PATH, "utf8");
  pomContent = pomContent.replace(
    /(<artifactId>oid4vp<\/artifactId>\s*\n\s*<version>)[^<]+(<\/version>)/,
    `$1${newVersion}$2`
  );
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
    lines.splice(insertIndex, endOfUnreleasedIndex - insertIndex, newEntry);
  } else {
    if (insertIndex === -1) insertIndex = lines.length;
    lines.splice(insertIndex, 0, newEntry);
  }

  fs.writeFileSync(changelogPath, lines.join("\n"));
  log(`✓ Updated CHANGELOG.md with version ${version}`, "green");
}

function bumpVersion(baseVersion, type) {
  const parts = baseVersion.split(".").map(Number);

  switch (type) {
    case "patch":
      parts[2]++;
      break;
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

function resolveReleaseVersion(current, type) {
  if (current.isSnapshot) {
    return current.baseVersion;
  }
  return bumpVersion(current.baseVersion, type);
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
  } catch (error) {
    log("⚠ Git operations failed. Please commit and tag manually:", "yellow");
    log(`  git add oid4vp-java/pom.xml CHANGELOG.md`, "cyan");
    log(`  git commit -m "chore: release version ${version}"`, "cyan");
    log(`  git tag -a v${version} -m "Release version ${version}"`, "cyan");
    log(`  git push && git push --tags`, "cyan");
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
    log("  npm run release patch    # 0.1.0-SNAPSHOT -> 0.1.0", "cyan");
    log("  npm run release patch    # 0.1.0 -> 0.1.1", "cyan");
    log("  npm run release minor    # 0.1.0 -> 0.2.0", "cyan");
    log("  npm run release major    # 0.1.0 -> 1.0.0", "cyan");
    process.exit(1);
  }

  const current = getCurrentVersion();
  const newVersion = resolveReleaseVersion(current, versionType);

  log(`Current version: ${current.rawVersion}`, "blue");
  log(`New version: ${newVersion}`, "green");
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
    log("", "cyan");
    log("### Changed", "cyan");
    log("- Changed feature description", "cyan");
    log("", "cyan");
    log("### Fixed", "cyan");
    log("- Bug fix description", "cyan");
    process.exit(1);
  }

  log("📝 Found unreleased changes:", "green");
  log(changes, "cyan");
  log("");

  updatePomVersion(newVersion);
  updateChangelog(newVersion, changes);

  log("\n📋 Release Summary:", "bright");
  log(`Version: ${current.rawVersion} -> ${newVersion}`, "blue");
  log("Files updated:", "blue");
  log("  ✓ oid4vp-java/pom.xml", "green");
  log("  ✓ CHANGELOG.md", "green");

  if (!skipGit) {
    log("\n📝 Creating git commit and tag...", "yellow");
    commitAndTag(newVersion);
  }

  log("\n🎉 Release preparation complete!", "bright");
  log("\nNext steps:", "yellow");
  if (!skipGit) {
    log("1. The changes and tags have been pushed to remote", "green");
    log("2. The release workflow will automatically:", "green");
    log("   - Publish the artifact to Maven Central", "cyan");
    log("   - Create a GitHub release with changelog content", "cyan");
  } else {
    log("1. Review the updated files", "cyan");
    log("2. Commit, tag, and push when ready", "cyan");
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
