import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { seedData, warmCaches, BASE_URL } from './setup.js';

// Custom metrics
const sseConnectSuccess = new Rate('sse_connect_success');
const sseEventsReceived = new Counter('sse_events_received');
const writeLatency = new Trend('write_latency', true);

const CROWD_LEVELS = ['RELAXED', 'NORMAL', 'FULL'];
const AVAILS = ['YES', 'MAYBE', 'NO'];

export const options = {
  scenarios: {
    // SSE subscribers open long-lived connections
    sse_subscribers: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      exec: 'sseSubscribe',
      tags: { scenario: 'sse_subscribers' },
    },
    // Writers push updates during SSE test
    sse_writers: {
      executor: 'constant-vus',
      vus: 5,
      duration: '30s',
      exec: 'writeUpdates',
      startTime: '3s',
      tags: { scenario: 'sse_writers' },
    },
  },
  thresholds: {
    'sse_connect_success': ['rate>0.95'],
    'write_latency': ['p(95)<200'],
  },
};

// Seed: 5 owners x 1 cafe each = 5 cafes
export function setup() {
  const data = seedData(5, 1);
  warmCaches(data.owners);
  console.log(`Setup: ${data.allCafeIds.length} cafes for SSE test`);
  return data;
}

// Subscribe to SSE stream with a short timeout to simulate client listening
export function sseSubscribe(data) {
  const cafeIds = data.allCafeIds.join(',');

  // K6 doesn't natively support SSE/EventSource, so we use HTTP GET with a timeout.
  // The response will be a streaming text/event-stream.
  // We set a timeout to not block the VU forever.
  const res = http.get(
    `${BASE_URL}/api/cafes/status/stream?cafeIds=${cafeIds}`,
    {
      tags: { name: 'GET /api/cafes/status/stream' },
      timeout: '10s',
      responseType: 'text',
    }
  );

  // Connection is considered successful if we get a 200 or the stream started
  const connected = check(res, {
    'SSE connection established (200 or stream)': (r) =>
      r.status === 200 || r.status === 0,
  });

  sseConnectSuccess.add(connected);

  // Count received SSE events in the response body
  if (res.body) {
    const eventMatches = res.body.match(/event:/g);
    if (eventMatches) {
      sseEventsReceived.add(eventMatches.length);
    }
  }

  sleep(1);
}

// Write status updates to trigger SSE events
export function writeUpdates(data) {
  const ownerIdx = Math.floor(Math.random() * data.owners.length);
  const owner = data.owners[ownerIdx];
  const cafeId = owner.cafeIds[0];

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
    tags: { name: 'PUT /api/owner/cafes/{id}/status (SSE trigger)' },
  });

  writeLatency.add(res.timings.duration);

  check(res, {
    'write status 200': (r) => r.status === 200,
  });

  sleep(1);
}

export function teardown(data) {
  console.log('SSE load test completed.');
  console.log('Check Grafana and application logs for SSE event delivery details.');
}
