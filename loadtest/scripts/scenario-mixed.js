import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { seedData, warmCaches, BASE_URL } from './setup.js';

// Custom metrics
const readErrorRate = new Rate('read_errors');
const writeErrorRate = new Rate('write_errors');
const readLatency = new Trend('read_latency', true);
const writeLatency = new Trend('write_latency', true);

const CROWD_LEVELS = ['RELAXED', 'NORMAL', 'FULL'];
const AVAILS = ['YES', 'MAYBE', 'NO'];

export const options = {
  scenarios: {
    readers: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 90,
      maxVUs: 150,
      exec: 'readStatus',
      tags: { scenario: 'readers' },
    },
    writers: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 10,
      maxVUs: 30,
      exec: 'writeStatus',
      tags: { scenario: 'writers' },
    },
  },
  thresholds: {
    'read_latency': ['p(95)<50'],
    'write_latency': ['p(95)<200'],
    'read_errors': ['rate<0.001'],
    'write_errors': ['rate<0.001'],
    'http_req_failed': ['rate<0.001'],
  },
};

// Seed: 5 owners x 10 cafes = 50 cafes
export function setup() {
  const data = seedData(5, 10);
  warmCaches(data.owners);
  console.log(`Setup complete: ${data.allCafeIds.length} cafes seeded`);
  return data;
}

// 90% of traffic: read status
export function readStatus(data) {
  const cafeId = data.allCafeIds[Math.floor(Math.random() * data.allCafeIds.length)];

  // Randomly choose between status and near endpoints
  let res;
  if (Math.random() < 0.5) {
    res = http.get(`${BASE_URL}/api/cafes/${cafeId}/status`, {
      tags: { name: 'GET /api/cafes/{id}/status' },
    });
  } else {
    const lat = 37.56 + (Math.random() - 0.5) * 0.02;
    const lng = 126.97 + (Math.random() - 0.5) * 0.02;
    res = http.get(
      `${BASE_URL}/api/cafes/near?lat=${lat}&lng=${lng}&radiusMeters=5000`,
      { tags: { name: 'GET /api/cafes/near' } }
    );
  }

  readLatency.add(res.timings.duration);

  const passed = check(res, {
    'read status 200': (r) => r.status === 200,
  });

  readErrorRate.add(!passed);
}

// 10% of traffic: write status
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
    'write status 200': (r) => r.status === 200,
  });

  writeErrorRate.add(!passed);
}

export function teardown(data) {
  console.log('Mixed load test completed.');
}
