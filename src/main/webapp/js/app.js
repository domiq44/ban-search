const form = document.getElementById('form');
const msg = document.getElementById('msg');
const table = document.getElementById('result');
const tbody = table.querySelector('tbody');
const messages = msg.dataset;
const MAP_VIEWER_BASE_URL = 'https://cartes.gouv.fr/explorer-les-cartes/';
const MAP_ZOOM_LEVEL = 16;
const SCORE_FRACTION_DIGITS = 2;
const COORDINATE_FRACTION_DIGITS = 5;
const SINGLE_RESULT_COUNT = 1;
let activeSearchId = 0;
let activeSearchController;

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const searchId = ++activeSearchId;
    activeSearchController?.abort();
    activeSearchController = new AbortController();
    const params = new URLSearchParams();
    for (const [k, v] of new FormData(form)) if (v) params.append(k, v);
    params.set('lang', document.documentElement.lang);
    msg.className = '';
    msg.textContent = messages.searching;
    tbody.replaceChildren();
    table.hidden = true;
    try {
        const r = await fetch('api/search?' + params, { signal: activeSearchController.signal });
        const data = await r.json();
        if (searchId !== activeSearchId) return;
        if (!r.ok) throw new Error(data.error || data.message || ('HTTP ' + r.status));
        if (!data.features.length) { msg.textContent = messages.empty; return; }
        for (const f of data.features) {
            const p = f.properties, [lon, lat] = f.geometry.coordinates;
            const tr = tbody.insertRow();
            [p.label, p.postcode, p.city, p.type, p.score.toFixed(SCORE_FRACTION_DIGITS)]
                .forEach(t => tr.insertCell().textContent = t);

            const mapUrl = new URL(MAP_VIEWER_BASE_URL);
            mapUrl.search = new URLSearchParams({
                c: `${lon},${lat}`,
                z: String(MAP_ZOOM_LEVEL),
                permalink: 'yes'
            });

            const mapLink = document.createElement('a');
            mapLink.href = mapUrl.toString();
            mapLink.target = '_blank';
            mapLink.rel = 'noopener noreferrer';
            mapLink.textContent = `${lat.toFixed(COORDINATE_FRACTION_DIGITS)} / ${lon.toFixed(COORDINATE_FRACTION_DIGITS)}`;
            mapLink.setAttribute('aria-label', messages.mapLinkLabel.replace('{0}', p.label || ''));
            tr.insertCell().append(mapLink);
        }
        const resultMessage = data.features.length === SINGLE_RESULT_COUNT ? messages.resultOne : messages.resultOther;
        msg.textContent = resultMessage.replace('{0}', data.features.length);
        table.hidden = false;
    } catch (err) {
        if (searchId !== activeSearchId || err.name === 'AbortError') return;
        msg.className = 'error';
        msg.textContent = messages.errorPrefix + ' ' + err.message;
    }
});
