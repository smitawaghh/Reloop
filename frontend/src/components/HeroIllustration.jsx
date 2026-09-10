// A hand-built inline SVG illustration (devices + a recycling loop) instead
// of a stock photo or icon-library asset - keeps the bundle self-contained
// (no image files, no new dependency) while giving the hero section the
// kind of visual anchor a real product site has instead of just text.
export default function HeroIllustration() {
  return (
    <svg viewBox="0 0 420 360" xmlns="http://www.w3.org/2000/svg" className="hero-illustration" aria-hidden="true">
      <circle cx="210" cy="180" r="170" fill="var(--green-100)" />
      <circle cx="210" cy="180" r="128" fill="var(--surface)" stroke="var(--green-100)" strokeWidth="2" />

      {/* loop arrows */}
      <g fill="none" stroke="var(--green-600)" strokeWidth="6" strokeLinecap="round">
        <path d="M120 95 C70 130, 70 210, 112 248" />
        <path d="M100 240 L112 248 L120 232" />
        <path d="M300 95 C350 130, 350 210, 308 248" />
        <path d="M320 232 L308 248 L296 240" />
        <path d="M150 300 C185 315, 235 315, 270 300" />
        <path d="M262 288 L270 300 L256 306" />
      </g>

      {/* laptop */}
      <g transform="translate(140 118)">
        <rect x="0" y="0" width="100" height="66" rx="8" fill="var(--green-900)" />
        <rect x="7" y="7" width="86" height="52" rx="3" fill="var(--green-50)" />
        <rect x="-10" y="66" width="120" height="10" rx="4" fill="var(--neutral-300)" />
      </g>

      {/* phone */}
      <g transform="translate(255 150)">
        <rect x="0" y="0" width="38" height="70" rx="8" fill="var(--green-700)" />
        <rect x="4" y="6" width="30" height="52" rx="2" fill="var(--green-50)" />
        <circle cx="19" cy="63" r="2.4" fill="var(--green-50)" />
      </g>

      {/* battery */}
      <g transform="translate(95 205)">
        <rect x="6" y="0" width="10" height="8" rx="2" fill="var(--neutral-400)" />
        <rect x="0" y="8" width="22" height="40" rx="4" fill="var(--neutral-400)" />
        <rect x="4" y="30" width="14" height="14" rx="2" fill="var(--green-500)" />
      </g>

      {/* small leaf accent */}
      <g transform="translate(205 240)">
        <path d="M0 20 C0 5 15 -5 30 0 C25 15 15 22 0 20Z" fill="var(--green-600)" />
      </g>
    </svg>
  )
}
