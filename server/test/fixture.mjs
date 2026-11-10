import { listen } from '../dev.mjs';
import { createWeatherApi } from '../weather.mjs';

const cities = [
  { id: 101, name: 'Saarbrücken', region: 'Saarland', country: 'Germany', zone: 'Europe/Berlin', temperature: 11 },
  { id: 102, name: 'Springfield', region: 'Illinois', country: 'United States', zone: 'America/Chicago', temperature: 17 },
  { id: 103, name: 'Springfield', region: 'Massachusetts', country: 'United States', zone: 'America/New_York', temperature: 8 },
];

export function providerFixture(now = Date.now) {
  return url => {
    const query = url.searchParams.get('q');
    if (url.pathname.endsWith('/search.json')) {
      const normalized = query.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
      return Response.json(cities.filter(city =>
        city.name.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().includes(normalized),
      ));
    }

    const city = cities.find(city => `id:${city.id}` === query);
    if (!city) return Response.json({ error: { code: 1006 } }, { status: 400 });

    const timestamp = Math.floor(now() / 1000);
    const formatDate = new Intl.DateTimeFormat('en-CA', { timeZone: city.zone, dateStyle: 'short' });
    const condition = { code: 1003, text: 'Partly cloudy' };
    const byDate = new Map();
    const startHour = Math.floor(timestamp / 3600) * 3600;
    for (let index = 0; index < 72; index++) {
      const time = startHour + index * 3600;
      const date = formatDate.format(new Date(time * 1000));
      if (!byDate.has(date)) byDate.set(date, []);
      byDate.get(date).push({
        time_epoch: time,
        temp_c: city.temperature + (index % 4),
        chance_of_rain: String(20 + 5 * (index % 4)),
        condition,
      });
    }

    return Response.json({
      location: { tz_id: city.zone },
      current: {
        temp_c: city.temperature,
        feelslike_c: city.temperature - 2,
        last_updated_epoch: timestamp - 300,
        condition,
      },
      forecast: {
        forecastday: [...byDate].slice(0, 3).map(([date, hour], index) => ({
          date,
          hour,
          day: {
            mintemp_c: city.temperature - 5,
            maxtemp_c: city.temperature + 4,
            daily_chance_of_rain: 25 + 10 * index,
            condition,
          },
        })),
      },
    });
  };
}

if (import.meta.main) {
  console.log('Using synthetic weather fixtures.');
  listen(createWeatherApi({ key: 'fixture', fetchUpstream: providerFixture() }));
}
