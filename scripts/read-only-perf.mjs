// Five-minute, read-only HTTP smoke load. Usage: node scripts/read-only-perf.mjs [seconds] [workers]
import { performance } from 'node:perf_hooks'

const seconds = Number(process.argv[2] ?? 300)
const workers = Number(process.argv[3] ?? 20)
if (!Number.isInteger(seconds) || seconds < 1 || !Number.isInteger(workers) || workers < 1) {
  throw new Error('seconds and workers must be positive integers')
}

const base = process.env.TAKEOUT_PERF_BASE ?? 'http://127.0.0.1:8080'
const paths = ['/api/v1/shops?page=1&size=20', '/api/v1/shops/2001']
const deadline = performance.now() + seconds * 1000
const durations = []
const failures = []
const counts = Object.fromEntries(paths.map(path => [path, 0]))

await Promise.all(Array.from({ length: workers }, async (_, worker) => {
  while (performance.now() < deadline) {
    const path = paths[worker % paths.length]
    const start = performance.now()
    try {
      const response = await fetch(new URL(path, base), { signal: AbortSignal.timeout(10000) })
      const body = await response.json()
      if (!response.ok || body.code !== 0) {
        failures.push({ path, status: response.status, code: body.code })
      }
    } catch (error) {
      failures.push({ path, error: String(error) })
    }
    durations.push(performance.now() - start)
    counts[path] += 1
  }
}))

durations.sort((a, b) => a - b)
const percentile = p => durations[Math.min(durations.length - 1, Math.ceil(durations.length * p) - 1)]
console.log(JSON.stringify({
  base,
  durationSeconds: seconds,
  workers,
  requests: durations.length,
  counts,
  p50Ms: Number(percentile(0.5).toFixed(2)),
  p95Ms: Number(percentile(0.95).toFixed(2)),
  p99Ms: Number(percentile(0.99).toFixed(2)),
  maxMs: Number(durations.at(-1).toFixed(2)),
  failures: failures.length,
  errorRatePercent: Number((failures.length / durations.length * 100).toFixed(4)),
  failureSamples: failures.slice(0, 5),
}))
