// A small hand-drawn SVG mark (three curved arrows forming a loop) instead
// of the emoji recycling symbol - same visual idea as the original ReLoop
// branding, but a crafted vector so it renders identically everywhere and
// stays in the site's own color palette instead of the OS emoji font.
export default function Logo({ size = 26 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        d="M11 4.5 6.5 12l4 2.3"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M6.5 12c-1.4 3.6.4 8.6 4.3 10.7"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
      <path
        d="M21 27.5 25.5 20l-4-2.3"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M25.5 20c1.4-3.6-.4-8.6-4.3-10.7"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
      <path
        d="M13 27.2c3.7 1.2 8.2-.8 10.1-4.8"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
      <path
        d="M19 4.8c-3.7-1.2-8.2.8-10.1 4.8"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
      />
    </svg>
  )
}
