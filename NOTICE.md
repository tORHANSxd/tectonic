# Notice

This repository contains an unofficial community backport of Tectonic for
Minecraft 1.20.1 Forge. It is not an official release and is not supported by
the upstream maintainers.

- Upstream: Tectonic by Apollo and contributors
- Upstream source: https://github.com/Apollounknowndev/tectonic
- License: MIT
- Base commit: `5373b2084e461f83bd6e0b5f2fe943e81bd59700` (official 3.0.17 source baseline)
- Audited upstream source head: `34241bdb35acda67b5367d49f354c66c05e098e2`
- Community source and issues: https://github.com/tORHANSxd/tectonic
- Community maintainer: tORHANS

The backport selectively adapts applicable changes from upstream 3.0.19
through the audited source history and documented release artifacts. The exact
decisions and source anchors are recorded in `UPSTREAM_DELTA.md` and
`BACKPORT_STATUS.md`.

Lithostitched is a required runtime dependency but is not bundled in this
project's JAR. It is an independent MIT-licensed project by Apollo and its
contributors: https://github.com/Apollounknowndev/lithostitched

Minecraft and Minecraft Forge are external runtime/platform dependencies and
are not redistributed by this repository. Their own terms apply.
