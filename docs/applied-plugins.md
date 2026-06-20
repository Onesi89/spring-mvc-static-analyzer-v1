# Applied Codex Plugins

현재 프로젝트/세션에서 확인된 Codex plugin 목록이다.

## Superpowers

- Plugin: `superpowers`
- Status: available in current session
- Version: `5.1.3`
- Source: `openai-curated-remote`
- Installed root:

```text
/home/kimwp/.codex/plugins/cache/openai-curated-remote/superpowers/5.1.3
```

- Main skills used in this project:
  - `superpowers:brainstorming`
  - `superpowers:writing-plans`
  - `superpowers:test-driven-development`
  - `superpowers:systematic-debugging`
  - `superpowers:verification-before-completion`
  - `superpowers:finishing-a-development-branch`

## Ponytail

- Plugin: `ponytail@ponytail`
- Status: `installed, enabled`
- Version: `4.7.0`
- Source: `https://github.com/DietrichGebert/ponytail.git`
- Marketplace: `ponytail`
- Installed root:

```text
/home/kimwp/.codex/plugins/cache/ponytail/ponytail/4.7.0
```

## Caveman

- Plugin: `caveman`
- Status: project-local marketplace registered
- Source: local project plugin
- Marketplace file:

```text
.agents/plugins/marketplace.json
```

- Plugin files:

```text
.codex-plugins/caveman/.codex-plugin/plugin.json
.codex-plugins/caveman/skills/caveman/SKILL.md
```

- Active skill path observed in session:

```text
/home/kimwp/.codex/plugins/cache/local-plugins/caveman/0.1.0/skills/caveman/SKILL.md
```

## Notes

- Superpowers is active in the current Codex session and provides the development workflow skills used during implementation.
- Ponytail is globally installed and enabled by `codex plugin add ponytail@ponytail`.
- Caveman was installed into this repository as a local plugin marketplace entry.
- Caveman mode is currently used at `full` intensity.
- Restart Codex after plugin changes when new skills do not appear immediately.
