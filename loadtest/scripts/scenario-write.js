import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { seedData, BASE_URL } from './setup.js';

// Custom metrics
const errorRate = new Rate('errors');
const writeLatency = new Trend('write_latency', true);

const CROWD_LEVELS = ['RELAXED', 'NORMAL', 'FULL'];
const AVAILS = ['YES', 'MAYBE', 'NO'];

export const options = {
  scenarios: {
    writers: {
      executor: 'constant-vus',
      vus: 50,
      duration: '2m',
      exec: 'writeStatus',
    },
  },
  thresholds: {
    'write_latency': ['p(95)<200'],
    'errors': ['rate<0.001'],
    'http_req_failed': ['rate<0.001'],
  },
};

// Seed: 10 owners x 1 cafe each = 10 cafes
export function setup() {
  const data = seedData(10, 1);
  console.log(`Setup complete: ${data.owners.length} owners, ${data.allCafeIds.length} cafes`);
  return data;
}

export function writeStatus(data) {
  const ownerIdx = Math.floor(Math.random() * data.owners.length);
  const owner = data.owners[ownerIdx];
  const cafeId = owner.cafeIds[Math.floor(Math.random() * owner.cafeIds.length)];

  const payload = JSON.stringify({
    crowdLevel: CROWD_LEVELS[Math.floor(Math.random() * 3)],
    party2: AVAILS[Math.floor(Math.random() * 3)],
    party3: AVAILS[Math.floor(Math.random() * 3)],
    party4: AVAILS[Math.floor(Math.random() * 3)],
  });

  const res = http.put(`${BASE_URL}/api/owner/cafes/${cafeId}/status`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${owner.accessToken}`,
    },
    tags: { name: 'PUT /api/owner/cafes/{id}/status' },
  });

  writeLatency.add(res.timings.duration);

  const passed = check(res, {
    'status 200': (r) => r.status === 200,
    'has cafeId in response': (r) => {
      try {
        return JSON.parse(r.body).cafeId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(!passed);
  sleep(0.2);
}

export function teardown(data) {
  console.log('Write load test completed.');
}
