@AGENTS.md

## Design convention: homepage hero photo (locked in, do not "fix" without asking)

In `src/app/page.tsx` + `src/app/page.module.css`, the Skupština photo background
(`.heroAndLists`, `background-image: url("/images/skupstina.jpg")`) intentionally spans
**both** the hero section (`.megaPhoto`, countdown/title) **and** the electoral lists section
(`.listGrid`, the white "Изборне листе" card) as one continuous image — it does not stop
after the hero like it originally did.

Within that same image span, only the hero part is tinted:
- `.tintBlue` (`rgba(15, 50, 112, 0.64)`) wraps ONLY the hero content (title/countdown) —
  keep this tint exactly where it is now, do not extend it further down.
- `.listGrid` must have **no background color of its own** — the photo shows through in its
  original, untinted colors behind/around the white list card. Do not add a background to
  `.listGrid` again (a previous iteration mistakenly added the same blue tint there and it
  was explicitly reverted — see git history around "heroAndLists").

Everything from the "Вести о изборима" news section downward (news + footer) is a separate,
plain dark background (page's `--bg` var) with no photo — never extend the photo or any tint
into that part.

If asked to touch the hero/list background again, confirm the intended boundary first rather
than assuming — this exact split (photo+tint on hero, photo-only on lists, plain dark below)
was iterated on deliberately.
