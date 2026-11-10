const upstream = 'https://api.weatherapi.com/v1/';
const searchPattern = /^[\p{L}\p{M}\p{N} .,'’()-]{2,80}$/u;
const idPattern = /^[1-9][0-9]{0,9}$/;
const windowMs = 60_000;

class ApiError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
  }
}

export function createWeatherApi({
  key = process.env.WEATHERAPI_KEY,
  fetchUpstream = fetch,
  now = Date.now,
  requestLimit = 120,
} = {}) {
  let windowStart = now();
  let requests = 0;
  const cache = new Map();

  async function requestWeather(path, params) {
    if (!key) throw new ApiError(503, 'weather_unconfigured');
    const url = new URL(path, upstream);
    url.search = new URLSearchParams({ ...params, key });
    const response = await fetchUpstream(url, {
      signal: AbortSignal.timeout(8_000),
      redirect: 'error',
    });
    const data = await response.json();
    if (!response.ok || data?.error) {
      if (data?.error?.code === 1006) throw new ApiError(404, 'city_not_found');
      if (data?.error?.code === 2007 || response.status === 429) {
        throw new ApiError(503, 'weather_unavailable');
      }
      throw new ApiError(502, 'weather_unavailable');
    }
    return data;
  }

  async function handle(request, operation) {
    if (request.method !== 'GET') {
      return Response.json({ error: 'method_not_allowed' }, {
        status: 405,
        headers: { Allow: 'GET', 'Cache-Control': 'no-store' },
      });
    }

    const params = new URL(request.url).searchParams;
    const search = operation === 'search';
    const input = (params.get(search ? 'q' : 'id') ?? '').trim().normalize('NFC');
    const valid = search ? searchPattern.test(input) : idPattern.test(input);
    if (!valid) return errorResponse(400, 'invalid_query');

    const time = now();
    const cacheKey = search ? `search:${input.toLowerCase()}` : `forecast:${input}`;
    for (const [id, value] of cache) {
      if (value.expires <= time) cache.delete(id);
    }
    const existing = cache.get(cacheKey);
    if (existing) return await jsonResult(existing.result);

    if (time - windowStart >= windowMs) {
      windowStart = time;
      requests = 0;
    }
    if (requests >= requestLimit) return errorResponse(429, 'rate_limited', { 'Retry-After': '60' });
    requests++;

    const result = search
      ? requestWeather('search.json', { q: input }).then(normalizeCities)
      : requestWeather('forecast.json', { q: `id:${input}`, days: '3', aqi: 'no', alerts: 'no' })
        .then(data => normalizeForecast(data, input, Math.floor(time / 1000)));
    const entry = { result, expires: time + 300_000 };
    if (cache.size < 128) cache.set(cacheKey, entry);
    const response = await jsonResult(result);
    if (!response.ok && cache.get(cacheKey) === entry) cache.delete(cacheKey);
    return response;
  }

  return {
    search: request => handle(request, 'search'),
    forecast: request => handle(request, 'forecast'),
  };
}

function errorResponse(status, code, headers = {}) {
  return Response.json({ error: code }, {
    status,
    headers: { 'Cache-Control': 'no-store', ...headers },
  });
}

async function jsonResult(result) {
  try {
    return Response.json(await result, { headers: { 'Cache-Control': 'no-store' } });
  } catch (error) {
    if (error instanceof ApiError) return errorResponse(error.status, error.message);
    return errorResponse(502, 'weather_unavailable');
  }
}

function requiredString(value) {
  if (typeof value !== 'string' || !value.trim()) throw new TypeError('Invalid text');
  return value.trim();
}

function requiredNumber(value) {
  if (!Number.isFinite(value)) throw new TypeError('Invalid number');
  return value;
}

function rainChance(value) {
  // The upstream has returned percentages as strings on some accounts.
  const rain = typeof value === 'string' && /^\d{1,3}$/.test(value) ? Number(value) : value;
  if (!Number.isInteger(rain) || rain < 0 || rain > 100) throw new TypeError('Invalid rain chance');
  return rain;
}

function condition(value) {
  return { code: requiredNumber(value.code), text: requiredString(value.text) };
}

function normalizeCities(data) {
  const cities = new Map();
  for (const city of data) {
    const id = String(city.id);
    if (!idPattern.test(id) || !city.name || !city.country) continue;
    cities.set(id, {
      id,
      name: requiredString(city.name),
      region: typeof city.region === 'string' ? city.region.trim() : '',
      country: requiredString(city.country),
    });
    if (cities.size === 12) break;
  }
  return { cities: [...cities.values()] };
}

function normalizeForecast(data, id, fetchedAt) {
  const { current, location, forecast } = data;
  const timeZone = requiredString(location.tz_id);
  new Intl.DateTimeFormat('en', { timeZone });
  const days = forecast.forecastday.slice(0, 3);
  if (days.length === 0) throw new TypeError('Empty forecast');

  const hourly = days.flatMap(day => day.hour.map(hour => ({
    time: requiredNumber(hour.time_epoch),
    temperature: requiredNumber(hour.temp_c),
    rainChance: rainChance(hour.chance_of_rain),
    condition: condition(hour.condition),
  }))).sort((a, b) => a.time - b.time);

  return {
    cityId: id,
    timeZone,
    fetchedAt,
    current: {
      temperature: requiredNumber(current.temp_c),
      feelsLike: requiredNumber(current.feelslike_c),
      observedAt: requiredNumber(current.last_updated_epoch),
      condition: condition(current.condition),
    },
    hourly,
    daily: days.map(({ date, day }) => ({
      date: requiredString(date),
      low: requiredNumber(day.mintemp_c),
      high: requiredNumber(day.maxtemp_c),
      rainChance: rainChance(day.daily_chance_of_rain),
      condition: condition(day.condition),
    })),
  };
}

export const weatherApi = createWeatherApi();
