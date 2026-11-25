import type { HomeContent } from "@/lib/home-content";

/** Flat, text-based illustration of a counter sale screen. Illustrative only; labelled as such. */
export function ProductScreen({ s }: { s: HomeContent["hero"]["screen"] }) {
  return (
    <figure className="home-screen">
      <div className="home-screen-frame" aria-hidden="true">
        <div className="home-screen-bar">
          <span className="home-screen-title">{s.title}</span>
          <span className="home-screen-tag">{s.label}</span>
        </div>
        <div className="home-screen-meta">{s.branch}</div>
        <table className="home-screen-table">
          <thead>
            <tr>
              <th scope="col">{s.headers[0]}</th>
              <th scope="col" className="home-hide-sm">{s.headers[1]}</th>
              <th scope="col" className="home-num">{s.headers[2]}</th>
              <th scope="col" className="home-num">{s.headers[3]}</th>
            </tr>
          </thead>
          <tbody>
            {s.lines.map((l) => (
              <tr key={l[0]}>
                <td>{l[0]}</td>
                <td className="home-hide-sm">{l[1]}</td>
                <td className="home-num">{l[2]}</td>
                <td className="home-num">{l[3]}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="home-screen-total">
          <span>{s.totalLabel}</span>
          <strong>{s.total}</strong>
        </div>
        <div className="home-screen-tenders">
          {s.tenders.map((x) => (
            <span key={x} className="home-chip">{x}</span>
          ))}
        </div>
        <p className="home-screen-note">{s.stockNote}</p>
      </div>
      <figcaption className="home-caption">{s.caption}</figcaption>
    </figure>
  );
}
