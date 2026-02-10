import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { seedData, warmCaches, BASE_URL } from './setup.js';

// Custom metrics
const errorRate = new Rate('errors');
const statusLatency = new Trend('status_latency', true);
const nearLatency = new Trend('near_latency', true);

export const options = {
  scenarios: {
    status_readers: {
      executor: 'constant-vus',
      vus: 100,
      duration: '2m',
      exec: 'readStatus',
      tags: { scenario: 'status_readers' },
    },
    near_readers: {
      executor: 'constant-vus',
      vus: 100,
      duration: '2m',
      exec: 'readNear',
      startTime: '0s',
      tags: { scenario: 'near_readers' },
    },
  },
  thresholds: {
    'status_latency': ['p(95)<50'],
    'near_latency': ['p(95)<50'],
    'errors': ['rate<0.001'],
    'http_req_failed': ['rate<0.001'],
  },
};

// Seed: 5 owners x 10 cafes = 50 cafes, then warm caches
export function setup() {
  const data = seedData(5, 10);
  warmCaches(data.owners);
  console.log(`Setup complete: ${data.allCafeIds.length} cafes seeded and cached`);
  return data;
}

// Scenario 1a: GET /api/cafes/{id}/status (random cafeId)
export function readStatus(data) {
  const cafeId = data.allCafeIds[Math.floor(Math.random() * data.allCafeIds.length)];
  const res = http.get(`${BASE_URL}/api/cafes/${cafeId}/status`, {
    tags: { name: 'GET /api/cafes/{id}/status' },
  });

  statusLatency.add(res.timings.duration);

  const passed = check(res, {
    'status 200': (r) => r.status === 200,
    'has crowdLevel': (r) => {
      try {
        return JSON.parse(r.body).crowdLevel !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(!passed);
  sleep(0.1);
}

// Scenario 1b: GET /api/cafes/near (Gangnam area)
export function readNear(data) {
  const lat = 37.56 + (Math.random() - 0.5) * 0.02;
  const lng = 126.97 + (Math.random() - 0.5) * 0.02;
  const res = http.get(
    `${BASE_URL}/api/cafes/near?lat=${lat}&lng=${lng}&radiusMeters=5000`,
    { tags: { name: 'GET /api/cafes/near' } }
  );

  nearLatency.add(res.timings.duration);

  const passed = check(res, {
    'status 200': (r) => r.status === 200,
    'returns array': (r) => {
      try {
        return Array.isArray(JSON.parse(r.body));
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(!passed);
  sleep(0.1);
}

export function teardown(data) {
  console.log('Read load test completed.');
}
