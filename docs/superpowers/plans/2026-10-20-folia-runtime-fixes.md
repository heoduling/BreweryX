# BreweryX Folia runtime fixes

Goal: repair the supplied container watchdog path and verified puke failures on Shiroha 26.2, based on upstream 0ceed4a (3.7.1).

Scope: user explicitly excluded recipes. Keep all recipe/config/data formats and existing data untouched. Do not deploy to production. Existing BreweryX-3.7.0-custom checkout remains untouched.

1. Reproduce holder snapshots with real CraftBukkit inventories, invoking the actual click, drag and close listeners. Replace identity-only PaperLib holder calls with native getHolder(false). Keep snapshot consumers that modify block state unchanged.
2. Exercise upstream puke on isolated matching core: disabled/enabled, player/console entry, multiple distant players, automatic drunkenness, despawn and logout. Only repair demonstrated failures. Verify touched temporary item metadata lifecycle.
3. Run clean check and shadowJar, run the isolated regression against the deliverable, verify JAR independently, check persistence code unchanged, package source and evidence, commit/push the fork branch.

Review focus: ordinary chest/barrel contents preserved; custom barrel/sealer identity preserved; cross-region player commands; task retirement/logout; no recipe or data migration.

Execution: inline; continue within explicit repair authorization. Regression harness and observations live under integration/ and the external audit/breweryx-20261020 directory. Upstream Gradle test task currently has zero discovered tests (legacy src/test is not a JUnit source root).

Completed: holder regression failed on both baselines and passed on fixed; 3.7.0 cross-region puke failed and upstream 3.7.1/fixed passed; one-tick and unused metadata defects reproduced and repaired. Fixed runtime: 16 assertions + 10 compatibility/identity assertions passed, no errors. Old-JAR fixture generation: three real serialized potions survived normal restart, version replacement and re-save unchanged in all recorded fields. Clean build and independent JAR validation passed. Fresh read-only reviewer found no new defects; its custom identity coverage caveat was subsequently resolved by real-object tests. Actual production database, LogBlock integration and third-party metadata consumers remain unverified. No recipe changes or production deployment.
