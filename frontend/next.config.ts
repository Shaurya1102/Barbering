import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Allow local preview instances to build separately from an existing app server.
  distDir: process.env.BARBERING_NEXT_DIST_DIR ?? ".next",
};

export default nextConfig;
