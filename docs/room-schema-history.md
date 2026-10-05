# Room: exported schemas and the gaps in them

`app/schemas/com.ajrpachon.chatapp.data.local.ChatDatabase/` holds one JSON per DB version, written by
Room's KSP processor on every build. Today it has versions **1-12, 15-21, 26 and 32-40**. The missing
ones are **13, 14, 22-25 and 27-31**, and they cannot be recovered.

## Why they are missing

- The June 2026 feature branches were merged in two batches (`e5d9e30`, `d10e732`), jumping the DB
  from 21 to 26 and then to 32. No commit ever held the intermediate states 22-25 and 27-31.
- Version numbers were reused across branches before being linearised. The entities at the commits
  that say "v13" and "v14" are not the ones the final migration chain calls v13 and v14: `isDeleted`
  is added by `migration12To13` in the chain, but at those commits it arrives at a later number.
  Regenerating 13.json and 14.json from those commits (building Room's KSP step against each commit's
  `data/local` sources reproduces committed schemas byte for byte, ignoring line endings) gives
  genuine schemas that the chain's `12 -> 13` and `14 -> 15` steps do not produce. They were not added.
- 16.json and 26.json were regenerated that way and do match the chain, so they were added.

## What guards this now

`ChatDatabaseMigrationTest` (androidTest, run with `connectedDebugAndroidTest`):

- `migrateBetweenEveryPairOfConsecutiveExportedSchemas` replays the chain from each exported version
  to the next and validates against that version's schema. Steps across a gap (12 -> 15, 21 -> 26,
  26 -> 32) are replayed together.
- `everyVersionFromFirstFullyExportedToCurrentHasASchema` fails if a new `Migration(X, Y)` ships
  without its schema export from v32 on.
- `migrate1To40_realDataSurvivesTheFullChain` checks the end state and that real data survives.

Two steps, 26 -> 32 and 36 -> 37, differ from the next schema only in
`index_broadcast_list_members_listId` (the migrations create it from v29, but the schemas for
v32-v36 were exported before the entity declared that index). The test allows exactly those two steps
to differ in exactly that table; `migration38To39` repairs it on real installs.

## Running it

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ajrpachon.chatapp.data.local.ChatDatabaseMigrationTest
```

If you delete a schema JSON, also delete `app/build/generated/assets/copyRoomSchemasToAndroidTestAssetsDebugAndroidTest`
and `app/build/intermediates/assets/debugAndroidTest`: the copy task does not remove files that
disappeared from the source folder.
