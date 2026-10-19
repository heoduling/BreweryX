# Isolated regression harness

This probe is test infrastructure; never put it on production. It grants operator status to joining test clients, creates test blocks/items, and changes in-memory test settings. The server must bind only to 127.0.0.1.

Tested core: Shiroha/Mizuki 26.2-DEV-f10fd16, Java 25.0.3. Compile `probe/probe/BreweryProbe.java` against that server's libraries and the BreweryX shaded JAR, then package the classes with `probe/plugin.yml` as `BreweryRegressionProbe.jar`. Run it beside BreweryX on an isolated offline server, port 25871.

Set `NODE_PATH` to an installation with mineflayer 4.39.0 and minecraft-data, then run `node integration/clients.cjs`. The harness uses the available 26.1 packet schema with handshake protocol 776. Packet parser trailing-byte warnings are a client limitation; assertions and all inventory/potion checks run on the server. Do not use client-side item decoding as evidence of item data compatibility.

- Default scenario: two players 2048 blocks apart, native barrel click/drag/close snapshot counts, contents preserved, real custom barrel/sealer/brewer identity, direct and scheduled puke, one-tick despawn boundary, 128 removed items per player checked for metadata retention, automatic drunk cycle and cross-player commands. The one-tick configuration mutation runs only on the first player.
- `BREWERY_SCENARIO=fixture-write`: run against the supplied 3.7.0 JAR to save three quality variants and their decoded fields under the probe's test data folder.
- Stop normally, replace only the BreweryX test JAR with the fixed version, restart with the same test data.
- `BREWERY_SCENARIO=fixture-read,identities`: verifies all three old items load and re-save with the same quality, alcohol, distillation count, age, wood, recipe and flags, plus real inventory holder identities.

Inspect server logs for every `PASS`, `FAIL`, `PROBE_EXCEPTION`, `PROBE_FAILURES`, and unexpected BreweryX errors. Client exit status alone does not indicate server assertions passed. The legacy upstream `src/test` tree contains no Gradle-discovered JUnit tests; `check` is therefore only a build/configuration check. The executable regressions are this real-server harness.
