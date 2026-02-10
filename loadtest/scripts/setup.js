import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SEED_PASSWORD = 'TestPass123!';

/**
 * Register a new owner and return { accessToken, refreshToken }.
 */
export function signUp(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/signup`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (res.status !== 201) {
    console.error(`signUp failed: ${res.status} ${res.body}`);
    return null;
  }
  return JSON.parse(res.body);
}

/**
 * Login and return { accessToken, refreshToken }.
 */
export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (res.status !== 200) {
    console.error(`login failed: ${res.status} ${res.body}`);
    return null;
  }
  return JSON.parse(res.body);
}

/**
 * Create a cafe owned by the authenticated owner.
 * Returns CafeResponse { id, name, latitude, longitude, address, createdAt }.
 */
export function createCafe(accessToken, name, lat, lng, address) {
  const res = http.post(
    `${BASE_URL}/api/owner/cafes`,
    JSON.stringify({ name, latitude: lat, longitude: lng, address }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );
  if (res.status !== 201) {
    console.error(`createCafe failed: ${res.status} ${res.body}`);
    return null;
  }
  return JSON.parse(res.body);
}

/**
 * Update cafe status (owner-only).
 * Returns CafeStatusResponse.
 */
export function updateStatus(accessToken, cafeId, crowdLevel, party2, party3, party4) {
  const res = http.put(
    `${BASE_URL}/api/owner/cafes/${cafeId}/status`,
    JSON.stringify({ crowdLevel, party2, party3, party4 }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );
  if (res.status !== 200) {
    console.error(`updateStatus failed: ${res.status} ${res.body}`);
    return null;
  }
  return JSON.parse(res.body);
}

/**
 * Fetch the owner's cafe list.
 * Returns array of CafeResponse.
 */
function fetchOwnerCafes(accessToken) {
  const res = http.get(`${BASE_URL}/api/owner/cafes`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (res.status !== 200) return [];
  const page = JSON.parse(res.body);
  return page.content || page;
}

// ---------------------------------------------------------------------------
// Pre-seeded data support (SQL seed via docker-compose)
// ---------------------------------------------------------------------------

/**
 * Use pre-seeded SQL data: login existing owners, fetch their cafe IDs.
 * SQL seed creates owner1~10@loadtest.com with password "TestPass123!"
 * and 10 cafes per owner (100 total), all with initial status.
 *
 * @param {number} ownerCount - How many of the 10 seeded owners to use (1-10)
 * @returns {{ owners: Array, allCafeIds: number[] }}
 */
export function useSeededData(ownerCount) {
  const owners = [];
  const allCafeIds = [];
  const count = Math.min(ownerCount, 10);

  for (let i = 1; i <= count; i++) {
    const email = `owner${i}@loadtest.com`;
    const tokens = login(email, SEED_PASSWORD);
    if (!tokens) {
      console.warn(`Seeded owner ${email} login failed — falling back to API seed`);
      return null;
    }

    const cafes = fetchOwnerCafes(tokens.accessToken);
    const cafeIds = cafes.map((c) => c.id);

    owners.push({
      email,
      password: SEED_PASSWORD,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      cafeIds,
    });

    cafeIds.forEach((id) => allCafeIds.push(id));
  }

  console.log(`Seeded data loaded: ${owners.length} owners, ${allCafeIds.length} cafes`);
  return { owners, allCafeIds };
}

// ---------------------------------------------------------------------------
// API-based seeding (fallback)
// ---------------------------------------------------------------------------

/**
 * Seed N owners, each with M cafes near Seoul.
 * Returns { owners: [{ email, password, accessToken, cafeIds: [] }], allCafeIds: [] }
 */
export function seedData(ownerCount, cafesPerOwner) {
  // Try pre-seeded data first
  const seeded = useSeededData(ownerCount);
  if (seeded && seeded.owners.length > 0) {
    return seeded;
  }

  console.log('Pre-seeded data not available. Creating via API...');
  const owners = [];
  const allCafeIds = [];
  const baseLat = 37.56;
  const baseLng = 126.97;

  for (let i = 0; i < ownerCount; i++) {
    const email = `loadtest_owner_${Date.now()}_${i}@test.com`;
    const password = SEED_PASSWORD;

    const tokens = signUp(email, password);
    if (!tokens) continue;

    const owner = {
      email,
      password,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      cafeIds: [],
    };

    for (let j = 0; j < cafesPerOwner; j++) {
      const lat = baseLat + (Math.random() - 0.5) * 0.04;
      const lng = baseLng + (Math.random() - 0.5) * 0.04;
      const name = `LoadTest Cafe ${i}-${j} ${Date.now()}`;

      const cafe = createCafe(tokens.accessToken, name, lat, lng, `Test Address ${i}-${j}`);
      if (cafe) {
        owner.cafeIds.push(cafe.id);
        allCafeIds.push(cafe.id);
      }
    }

    owners.push(owner);
  }

  return { owners, allCafeIds };
}

/**
 * Warm caches by updating status for all cafes once.
 */
export function warmCaches(owners) {
  const levels = ['RELAXED', 'NORMAL', 'FULL'];
  const avails = ['YES', 'MAYBE', 'NO'];

  for (const owner of owners) {
    for (const cafeId of owner.cafeIds) {
      updateStatus(
        owner.accessToken,
        cafeId,
        levels[Math.floor(Math.random() * 3)],
        avails[Math.floor(Math.random() * 3)],
        avails[Math.floor(Math.random() * 3)],
        avails[Math.floor(Math.random() * 3)]
      );
    }
  }
}

export { BASE_URL };
