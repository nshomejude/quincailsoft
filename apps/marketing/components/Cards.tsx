import type { Feature } from "@/lib/content";

export function Cards({ items, headingLevel = 3 }: { items: Feature[]; headingLevel?: 2 | 3 }) {
  const H = `h${headingLevel}` as "h2" | "h3";
  return (
    <ul className="cards">
      {items.map((f) => (
        <li key={f.title}>
          <H>{f.title}</H>
          <p>{f.body}</p>
        </li>
      ))}
    </ul>
  );
}
