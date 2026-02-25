# 01 – Fix footer spacing

**Problem**: Footer shows "byvadiolewith<3" without spaces. Should be "by vadiole with <3".

**Root cause**: Android XML trims leading/trailing whitespace from `<string>` resources. The strings `"by "` and `" with <3"` lose their spaces.

---

## Ticket 1: UX-UI

**Status**: Pending

Review the footer text. The intended rendering is: `by vadiole with <3` where "vadiole" is underlined and clickable. Confirm the fix approach: either use `\u0020` for spaces, or use `translatable="false"` with `xml:space="preserve"`, or hardcode the full template as one string with a placeholder.

---

## Ticket 2: Development

**Status**: Pending

Fix the string resources so spaces are preserved. Build and verify the footer renders correctly.

---

## Ticket 3: Review

**Status**: Pending

Verify the footer renders as "by vadiole with <3" with proper spacing, underline on "vadiole", and clickable link. Check both light and dark mode.
