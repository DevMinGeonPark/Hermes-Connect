# Ionicons for Compose

MIT-licensed original SVGs from [Ionicons](https://github.com/ionic-team/ionicons/tree/d1e2c48641fd5f4910ee42a144dc1c84b1a9a4ee), with hashes and semantic aliases in `manifest.json`.

Run `python3 scripts/generate-ionicons.py` from the repository to generate native Compose `ImageVector` assets. `--check` verifies the checked-in result offline; `--fetch` downloads missing assets from the pinned revision. Paths, primitive geometry, line widths, caps, joins, and fill rules are preserved. Directional navigation symbols use Compose's RTL mirroring. Material `Icon` supplies the theme tint.

The app packages the copyright and MIT notice at `assets/licenses/ionicons.txt`. The renderer has no runtime downloads or icon font dependency.
