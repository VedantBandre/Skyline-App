import assert from 'node:assert/strict';
import test from 'node:test';
import { createWeatherApi } from '../weather.mjs';
import { providerFixture } from './fixture.mjs';

const epoch = Date.parse('2026-11-10T23:30:00Z');
const request = (path, method = 'GET') => new Request(`https://test.example/api/v1/${path}`, { method });

test('search retains identifiers and regions for ambiguous matches', async () => {
  const api = createWeatherApi({ key: 'test', fetchUpstream: providerFixture() });
  const response = await api.search(request('search?q=Springfield'));
  assert.equal(response.status, 200);
  const { cities } = await response.json();
  assert.deepEqual(cities.map(city => [city.id, city.region]), [
    ['102', 'Illinois'], ['103', 'Massachusetts'],
  ]);
  assert.deepEqual(await (await api.search(request('search?q=NoWhereTown'))).json(), { cities: [] });
});

test('forecast uses the selected provider ID and retains local dates and ordered epochs', async () => {
  const api = createWeatherApi({
    key: 'test',
    now: () => epoch,
    fetchUpstream(url) {
      assert.equal(url.origin, 'https://api.weatherapi.com');
      assert.equal(url.searchParams.get('q'), 'id:101');
      assert.equal(url.searchParams.get('days'), '3');
      return providerFixture(() => epoch)(url);
    },
  });
  const response = await api.forecast(request('forecast?id=101'));
  assert.equal(response.status, 200);
  const data = await response.json();
  assert.equal(data.fetchedAt, epoch / 1000);
  assert.equal(data.timeZone, 'Europe/Berlin');
  assert.equal(data.current.temperature, 11);
  assert.equal(data.hourly[0].rainChance, 20);
  assert.ok(data.hourly[1].time > data.hourly[0].time);
  assert.deepEqual(data.daily.map(day => day.date), ['2026-11-11', '2026-11-12', '2026-11-13']);
});

test('rejects invalid queries and unsupported methods before calling the provider', async () => {
  const api = createWeatherApi({ key: 'test', fetchUpstream: () => assert.fail('unexpected upstream call') });
  assert.equal((await api.search(request('search?q=a'))).status, 400);
  assert.equal((await api.forecast(request('forecast?id=auto%3Aip'))).status, 400);
  assert.equal((await api.search(request('search?q=Berlin', 'POST'))).status, 405);
});

test('coalesces matching requests and expires the cache without changing fetchedAt', async () => {
  let time = epoch;
  let count = 0;
  const api = createWeatherApi({
    key: 'test',
    now: () => time,
    fetchUpstream(url) {
      count++;
      return providerFixture(() => time)(url);
    },
  });
  const fetchForecast = () => api.forecast(request('forecast?id=101'));
  const results = await Promise.all([fetchForecast(), fetchForecast()]);
  assert.equal(count, 1);
  assert.equal((await results[0].json()).fetchedAt, (await results[1].json()).fetchedAt);
  time += 301_000;
  assert.equal((await (await fetchForecast()).json()).fetchedAt, time / 1000);
  assert.equal(count, 2);
});

test('maps missing credentials and provider failures without exposing upstream details', async () => {
  const missing = createWeatherApi({ key: '' });
  assert.equal((await missing.forecast(request('forecast?id=101'))).status, 503);
  let count = 0;
  const failing = createWeatherApi({
    key: 'server-only',
    fetchUpstream() {
      count++;
      throw new Error('server-only upstream detail');
    },
  });
  const first = await failing.search(request('search?q=Berlin'));
  assert.equal(first.status, 502);
  assert.deepEqual(await first.json(), { error: 'weather_unavailable' });
  assert.equal((await failing.search(request('search?q=Berlin'))).status, 502);
  assert.equal(count, 2);
});

test('bounds cache misses and recovers after the throttle window', async () => {
  let time = epoch;
  const api = createWeatherApi({
    key: 'test', requestLimit: 1, now: () => time, fetchUpstream: providerFixture(),
  });
  assert.equal((await api.search(request('search?q=Berlin'))).status, 200);
  const limited = await api.search(request('search?q=Paris'));
  assert.equal(limited.status, 429);
  assert.equal(limited.headers.get('Retry-After'), '60');
  time += 60_000;
  assert.equal((await api.search(request('search?q=Paris'))).status, 200);
});
