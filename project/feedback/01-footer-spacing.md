# 01 – Fix footer spacing

**Problem**: Footer shows "byvadiolewith<3" without spaces. Should be "by vadiole with <3".

**Root cause**: Android XML trims leading/trailing whitespace from `<string>` resources. The strings `"by "` and `" with <3"` lose their spaces.

---

## Ticket 1: UX-UI

**Status**: Done

**Analysis**: Android XML trims leading/trailing whitespace from `<string>` resources. The three separate strings `"by "`, `"vadiole"`, `" with <3"` lose their spaces when concatenated.

**Recommended fix**: Replace the three separate strings with one template string: `"by %s with \u0026lt;3"` with a `%s` placeholder for "vadiole". The spaces are now interior to the string (not leading/trailing) so they survive trimming. Use `String.format()` to insert the link text, then apply spans at the known offset. This is the cleanest approach — no `\u0020` hacks, no extra strings.

---

## Ticket 2: Development

**Status**: Done

- Replaced 3 separate strings (`footer_by`, `footer_vadiole`, `footer_with_heart`) with 2: `footer_template` ("by %s with <3") and `footer_vadiole`.
- Updated `buildFooter()` to use `getString(R.string.footer_template, vadioleText)` and `indexOf()` to find span positions.

---

## Ticket 3: Review

**Status**: Done

- Template string has spaces as interior characters — won't be trimmed.
- `indexOf(vadioleText)` correctly finds position 3 in "by vadiole with <3".
- No references to removed `footer_by` or `footer_with_heart` resources remain.
- Build passes clean.
