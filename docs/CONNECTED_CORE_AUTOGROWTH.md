# Connected Nest Core automatic physical growth (experimental)

The existing server-side `SwarmNestBlockEntity.runColonyCycle()` now invokes
`tryGrowConnectedCore()` after its local member census.

## Activation
Set the new SERVER config `nestConnectedCoreGrowthEnabled=true`.
**Default OFF** pending build, integration GameTests and world-protection review.
The feature also requires `nestLifecycleEnabled`, global mod `enabled`,
`mobGriefing`, an observing player within the existing 48-block simulation
range, no player in the existing shell safety zone, a FULL 128-point
resource warehouse, a local membership census at its current old-system
capacity, enough actual SOIL and TIMBER points to pay the existing 8+6 bill,
and an unoccupied adjacent air position in loaded chunks.

After successful physical placement, the root spends exactly 8 soil points
and 6 timber points, records a new LastConnectedTip and increments
ConnectedBuiltCount. The placed Nest Core persists `ConnectedExtension` and
`ConnectedRoot` and cannot run an independent spawner/economy cycle.
The next build starts at the most recent valid tip; no whole-colony scan is
required for each construction cycle, and there is no final block-count cap.

## Known limitations
- New extensions **do not yet increase actual effectiveCapacity** of the
  root, because reliable full-colony census across remote loaded components
  and root ownership reconciliation are not yet implemented.
- When the tip is broken/unloaded, growth pauses until a valid recovery path
  is designed. That fails closed and does not create free materials.
- Growth is restricted by the 48-block player observer requirement at the
  root, even when a long chain of extension blocks reaches distant chunks.
- Adjacent unrelated roots are rejected, but general land-claim/protection
  integration is not yet implemented. Do not enable on protected servers.
- Vanilla bee hive growth is NOT scheduled yet.
- This branch has not been compiled or GameTest verified. Keep PR Draft.
