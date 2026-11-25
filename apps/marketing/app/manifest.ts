import type { MetadataRoute } from "next";

export const dynamic = "force-static";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "QuincaiSoft",
    short_name: "QuincaiSoft",
    description: "ERP and point of sale for hardware shops, construction materials and wholesale in Cameroon and CEMAC.",
    start_url: "/",
    display: "browser",
    background_color: "#ffffff",
    theme_color: "#0b4f8a",
    icons: [{ src: "/icon.svg", sizes: "any", type: "image/svg+xml" }],
  };
}
