# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Joget DX9 marketplace plugin (OSGi bundle) that adds a single Datalist Column Formatter: **Options Template Datalist Formatter**. It takes a semicolon-separated field value, resolves each individual value to a label (via a static options grid or a configured Form Options Binder), and renders each resolved value through a user-configurable HTML template (`{id}` / `{label}` placeholders), joined by a configurable separator.

## Build

```bash
mvn clean install
```

Produces an OSGi bundle jar at `target/options-template-datalist-formatter-<version>.jar`, which is installed into a Joget DX9 instance as a plugin.

There are no automated tests in this repo (surefire integration-test execution is configured with `skipTests=true`).

## Architecture

- [Activator.java](src/main/java/org/joget/marketplace/Activator.java) — OSGi `BundleActivator`; registers `OptionsTemplateDatalistFormatter` as an OSGi service on bundle start, unregisters on stop. Also defines the plugin `VERSION` constant used by the formatter's `getVersion()`.
- [OptionsTemplateDatalistFormatter.java](src/main/java/org/joget/marketplace/OptionsTemplateDatalistFormatter.java) — extends `DataListColumnFormatDefault`, the actual formatter logic:
  - `format()` splits the incoming cell value on `;`, trims each token, looks up a label via `getOptionMap()`, and substitutes `{id}`/`{label}` into the configured `template` string, joining results with `separator`.
  - `getOptionMap()` resolves value→label pairs from two possible sources, in this order of precedence at runtime: first tries the static `options` grid property, then — if an `optionsBinder` is configured — loads a `FormLoadOptionsBinder` plugin instance via `PluginManager` and overwrites `optionMap` with rows loaded from `FormLoadBinder.load()`. The map is cached per-instance in the `optionMap` field (lazy-loaded once, not per-row).
  - The `customHeader` property (raw HTML/CSS) is emitted once per HTTP request, guarded via a `request.setAttribute(uniqueColumnIdentifier, ...)` flag keyed on `column.getProperty("id") + getClassName()`, so it isn't repeated for every row in the datalist. This mechanism was previously buggy for datalists with multiple instances of the formatter column — see recent commit history before changing it.
  - Plugin property options (form definition for the Joget admin UI) live in [OptionsTemplateDatalistFormatter.json](src/main/resources/properties/OptionsTemplateDatalistFormatter.json); labels/descriptions are externalized to [OptionsTemplateDatalistFormatter.properties](src/main/resources/messages/OptionsTemplateDatalistFormatter.properties) and referenced via `@@key@@` placeholders resolved by `AppPluginUtil.getMessage`.

## Key dependencies

- `wflow-core` (Joget core, `provided` scope, resolved from Joget's Maven repo — expects a `9.0-SNAPSHOT` build available locally/in the configured repositories).
- OSGi bundling is handled by `maven-bundle-plugin`; `Import-Package` in [pom.xml](pom.xml) explicitly whitelists the Joget packages this plugin depends on — when adding a new Joget API import, it must also be added to this `Import-Package` list or the bundle will fail to resolve at runtime.

## Versioning note

`Activator.VERSION` and the `<version>` in `pom.xml` are maintained independently — keep them in sync when cutting a release.
