import { existsSync, copyFileSync } from 'node:fs';
import { spawn } from 'node:child_process';

function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: 'inherit' });
    child.on('error', reject);
    child.on('exit', (code) => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(`${command} ${args.join(' ')} exited with code ${code}`));
    });
  });
}

async function waitForApp(url, timeoutMs = 240_000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    try {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 5_000);
      const response = await fetch(url, { redirect: 'manual', signal: controller.signal });
      clearTimeout(timer);
      if (response.status >= 200 && response.status < 500) {
        return;
      }
    } catch {
      // ignore and retry
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  throw new Error(`Application did not become ready in ${timeoutMs}ms: ${url}`);
}

export default async function globalSetup() {
  if (!existsSync('.env') && existsSync('.env.example')) {
    copyFileSync('.env.example', '.env');
  }

  await run('docker', ['compose', 'down', '-v', '--remove-orphans']);
  await run('docker', ['compose', 'up', '-d', '--build']);
  await waitForApp((process.env.E2E_BASE_URL || 'http://127.0.0.1:8080') + '/login');
}
