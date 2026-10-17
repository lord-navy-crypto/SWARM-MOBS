# Connected Nest Core root ownership: runtime increment

- Passive Nest Core extensions now persist their original root coordinate in
  `ConnectedRoot`, alongside `ConnectedExtension`.
- New extensions inherit their owner's root when built next to an extension.
- Expansion aborts if the original root is not in loaded chunks, is missing,
  or is itself an extension. This does not force chunk loading.
- Expansion aborts if any of the six neighboring blocks belong to a
  different root or a malformed Nest Core. Prevents accidental merging.
- Legacy active cores retain their individual accounts. Existing passive
  extension save data from before this commit can lack a root; those are
  inert for expansion until a deliberate migration is implemented.

Open requirements:
- The root's growth tick must reserve and consume resource points exactly once.
- The colony census and capacity must include connected extensions without
  accidentally reassigning mobs of nearby independent colonies.
- Physical placement must respect claims/protections and mobGriefing.
- The new root inheritance/blocked merge behavior needs GameTest coverage.
- Gradle and GameTest have NOT been run here; keep the PR draft.
