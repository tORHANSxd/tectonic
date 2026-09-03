"""Fail-closed placeholder for the removed upstream release uploader.

The upstream script targeted Apollo's official Modrinth and CurseForge project
IDs across several unsupported loaders and Minecraft versions. A community
backport must never reuse those destinations.
"""

BLOCKED_MESSAGE = (
    "External publishing is disabled for this unofficial community backport. "
    "Create a separately reviewed publisher with community-owned project IDs "
    "only after every release blocker in docs/RELEASE_CHECKLIST.md is closed."
)


def main() -> None:
    raise SystemExit(BLOCKED_MESSAGE)


if __name__ == "__main__":
    main()
