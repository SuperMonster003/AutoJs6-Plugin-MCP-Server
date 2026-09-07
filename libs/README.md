# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifact named exactly:

- `common-plugin-api.aar`

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

Roadmap phase P1 adds the dedicated `mcp-server-api.aar` (Binder contract between AutoJs6 and
this plugin). Stage it the same way and extend the lock file in the same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
