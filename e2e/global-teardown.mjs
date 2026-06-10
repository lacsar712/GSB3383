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

export default async function globalTeardown() {
  if (process.env.E2E_KEEP_CONTAINERS === '1') {
    return;
  }
  await run('docker', ['compose', 'down', '-v', '--remove-orphans']);
}
