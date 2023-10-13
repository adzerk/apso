#!/usr/bin/env npx zx --install

import {readFileSync} from "fs";

const CHANGELOG_VERSION_PREFIX = "#".repeat(2);
const CHANGELOG_VERSION_PATTERN = /^## \[\d+\.\d+\.\d+\] - \d{4}-\d{2}-\d{2}$/;
const EXAMPLE_VERSION_LINE = "## [Unreleased]";
const CHANGELOG_VERSION_SECTION_PREFIX = "#".repeat(3);
const CHANGELOG_VERSION_VALID_SECTIONS = Object.freeze([
  "Added",
  "Changed",
  "Deprecated",
  "Removed",
  "Fixed",
  "Security"
]);

const changelog = readFileSync("CHANGELOG.md").toString();
const changelogLines = changelog.split("\n");

// Validate consistency across Changelog section
const invalidChangelogSections = changelogLines
  .filter(line => line.startsWith(CHANGELOG_VERSION_SECTION_PREFIX))
  .filter(line => !CHANGELOG_VERSION_VALID_SECTIONS.includes(line.substring(CHANGELOG_VERSION_SECTION_PREFIX.length + 1)));

if (invalidChangelogSections.length > 0) {
  echo("Found invalid changelog sections! The invalid sections are:");
  echo(invalidChangelogSections.join("\n"));
  process.exit(1);
}

// Validate if versions have a date in the 'YYYY-MM-DD' format
const invalidVersionHeaders = changelogLines
  .filter(line => line.startsWith(`${CHANGELOG_VERSION_PREFIX} `))
  .filter(line => line !== EXAMPLE_VERSION_LINE)
  .filter(line => !CHANGELOG_VERSION_PATTERN.test(line));

if (invalidVersionHeaders.length > 0) {
  echo("Found invalid changelog version headers! The invalid version headers are:");
  echo(invalidVersionHeaders.join("\n"));
  process.exit(1);
}

echo("Changelog is valid.");
