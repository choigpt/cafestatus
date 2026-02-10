import http from 'k6/http';
import { check } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { seedData, BASE_URL } from './setup.js';

// Custom metrics
const consistencyFailures = new Counter('consistency_failures');
const consistencyChecks = new Counter('consistency_checks');
const errorRate = new Rate('errors');

export const options = {
  scenarios: {
    consistency: {
      executor: 'per-vu-iterations',
      vus: 10,
      iterations: 500,
      exec: 'consistencyCheck',
    },
  },
  thresholds: {
    'consistency_failures': ['count==0'],
    'errors': ['rate<0.001'],
    'checks': ['rate>0.999'],
  },
};

// Seed: 1 owner with 1 cafe (all VUs share the same cafe for conflict testing)
export function setup() {
  const data = seedData(1, 1);
  console.log(`Setup: owner with cafe ${data.allCafeIds[0]}`);
  return {
    owner: data.owners[0],
    cafeId: data.allCafeIds[0],
  };
}

export function consistencyCheck(data) {
  const { owner, cafeId } = data;
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${owner.accessToken}`,
  };

  // Step 1: Write FULL
  const writeFullPayload = JSON.stringify({
    crowdLevel: 'FULL',
    party2: 'NO',
    party3: 'NO',
    party4: 'NO',
  });

  const writeFullRes = http.put(
    `${BASE_URL}/api/owner/cafes/${cafeId}/status`,
    writeFullPayload,
    {
      headers,
      tags: { name: 'PUT status (FULL)' },
    }
  );

  const writeFullOk = check(writeFullRes, {
    'write FULL: status 200': (r) => r.status === 200,
  });

  if (!writeFullOk) {
    errorRate.add(1);
    return;
  }

  // Step 2: Immediately read — should see FULL
  const readFullRes = http.get(`${BASE_URL}/api/cafes/${cafeId}/status`, {
    tags: { name: 'GET status (expect FULL)' },
  });

  consistencyChecks.add(1);
  const readFullOk = check(readFullRes, {
    'read after FULL: status 200': (r) => r.status === 200,
    'read after FULL: crowdLevel is FULL': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.crowdLevel === 'FULL';
      } catch (e) {
        return false;
      }
    },
  });

  if (!readFullOk) {
    consistencyFailures.add(1);
    try {
      const body = JSON.parse(readFullRes.body);
      console.error(`CONSISTENCY FAIL: expected FULL, got ${body.crowdLevel}`);
    } catch (e) {
      console.error(`CONSISTENCY FAIL: could not parse response: ${readFullRes.body}`);
    }
  }

  // Step 3: Write RELAXED
  const writeRelaxedPayload = JSON.stringify({
    crowdLevel: 'RELAXED',
    party2: 'YES',
    party3: 'YES',
    party4: 'YES',
  });

  const writeRelaxedRes = http.put(
    `${BASE_URL}/api/owner/cafes/${cafeId}/status`,
    writeRelaxedPayload,
    {
      headers,
      tags: { name: 'PUT status (RELAXED)' },
    }
  );

  const writeRelaxedOk = check(writeRelaxedRes, {
    'write RELAXED: status 200': (r) => r.status === 200,
  });

  if (!writeRelaxedOk) {
    errorRate.add(1);
    return;
  }

  // Step 4: Immediately read — should see RELAXED
  const readRelaxedRes = http.get(`${BASE_URL}/api/cafes/${cafeId}/status`, {
    tags: { name: 'GET status (expect RELAXED)' },
  });

  consistencyChecks.add(1);
  const readRelaxedOk = check(readRelaxedRes, {
    'read after RELAXED: status 200': (r) => r.status === 200,
    'read after RELAXED: crowdLevel is RELAXED': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.crowdLevel === 'RELAXED';
      } catch (e) {
        return false;
      }
    },
  });

  if (!readRelaxedOk) {
    consistencyFailures.add(1);
    try {
      const body = JSON.parse(readRelaxedRes.body);
      console.error(`CONSISTENCY FAIL: expected RELAXED, got ${body.crowdLevel}`);
    } catch (e) {
      console.error(`CONSISTENCY FAIL: could not parse response: ${readRelaxedRes.body}`);
    }
  }

  errorRate.add(0);
}

export function teardown(data) {
  console.log('Consistency test completed.');
}
