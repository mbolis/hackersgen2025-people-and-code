import { execSync } from "node:child_process";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";

const cwd = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2).join(" ");

try {
  execSync("npm i --no-audit --no-fund", { cwd, stdio: "inherit" });
  execSync(`npx vitest run ${args}`, { cwd, stdio: "inherit" });
} catch (e) {
  process.exit(1);
}