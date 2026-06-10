import { promises as fs } from 'node:fs';
import path from 'node:path';

const ROOT = process.cwd();
const IGNORED_DIRS = new Set(['.git', 'node_modules', 'target', 'playwright-report', 'test-results']);
const IGNORED_FILES = new Set(['mvp.md', 'mvp.json', 'meta.json']);
const ALLOWED_EXTS = new Set([
  '.java', '.jsp', '.js', '.mjs', '.json', '.md', '.sql', '.xml', '.yml', '.yaml', '.properties', '.toml'
]);

const issues = [];

async function walk(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    const relPath = path.relative(ROOT, fullPath);

    if (entry.isDirectory()) {
      if (IGNORED_DIRS.has(entry.name)) {
        continue;
      }
      await walk(fullPath);
      continue;
    }

    if (!ALLOWED_EXTS.has(path.extname(entry.name))) {
      continue;
    }
    if (IGNORED_FILES.has(relPath)) {
      continue;
    }

    const content = await fs.readFile(fullPath, 'utf8');
    if (content.includes('\r')) {
      issues.push(`${relPath}: 包含 CRLF，请统一为 LF`);
    }

    const lines = content.split('\n');
    for (let i = 0; i < lines.length; i += 1) {
      const line = lines[i];
      const lineNo = i + 1;
      if (/\t/.test(line)) {
        issues.push(`${relPath}:${lineNo} 含有 Tab 字符`);
      }
      if (/[ \t]+$/.test(line)) {
        issues.push(`${relPath}:${lineNo} 行尾存在空白字符`);
      }
    }
  }
}

await walk(ROOT);

if (issues.length > 0) {
  console.error('Lint 失败，发现以下问题：');
  for (const issue of issues) {
    console.error(`- ${issue}`);
  }
  process.exit(1);
}

console.log('Lint 通过：未发现格式问题。');
