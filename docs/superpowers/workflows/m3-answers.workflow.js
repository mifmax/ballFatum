export const meta = {
  name: 'ballfatum-answers',
  description: 'Generate 2000 native Magic 8-Ball answers per language (en/ru/de/es/fr), tone 2:1:1, deduped',
  phases: [{ title: 'Generate', detail: '5 languages x 3 tones, batched with dedup' }],
}

const LANGS = [
  { code: 'en', name: 'English' },
  { code: 'ru', name: 'Russian' },
  { code: 'de', name: 'German' },
  { code: 'es', name: 'Spanish' },
  { code: 'fr', name: 'French' },
]
const TONES = [
  { key: 'affirmative', target: 1000, desc: 'clearly positive, yes-leaning, encouraging' },
  { key: 'neutral', target: 500, desc: 'non-committal, uncertain, "ask again" style' },
  { key: 'negative', target: 500, desc: 'clearly negative, no-leaning, discouraging' },
]
const MAX_ROUNDS = 40
const BATCH = 120
const MAX_LEN = 50
const SCHEMA = {
  type: 'object',
  properties: { answers: { type: 'array', items: { type: 'string' } } },
  required: ['answers'],
  additionalProperties: false,
}

function norm(s) {
  return s.toLowerCase().replace(/[\s.,!?…"'«»„“”()\-–—]+/g, ' ').trim()
}

async function genTone(lang, tone) {
  const seen = new Set()
  const kept = []
  let dry = 0
  for (let round = 0; round < MAX_ROUNDS && kept.length < tone.target; round++) {
    const sample = []
    const step = Math.max(1, Math.floor(kept.length / 25))
    for (let i = 0; i < kept.length && sample.length < 25; i += step) sample.push(kept[i])
    const prompt =
      `You write answers for a Magic 8-Ball fortune app, in ${lang.name}.\n` +
      `Produce ${BATCH} DISTINCT short mystical predictions that are ${tone.desc}.\n` +
      `Rules:\n` +
      `- ${lang.name} only, natural and idiomatic.\n` +
      `- Each answer <= ${MAX_LEN} characters, single line, no numbering, no surrounding quotes.\n` +
      `- Magic 8-Ball style, but VARIED wording; all ${BATCH} different from each other.\n` +
      (sample.length ? `- Do NOT repeat these already-used ones: ${sample.join(' | ')}\n` : '') +
      `Return JSON {"answers":[...]}. Need ${tone.target - kept.length} more (round ${round + 1}).`
    const res = await agent(prompt, { label: `${lang.code}:${tone.key}:r${round + 1}`, phase: 'Generate', schema: SCHEMA })
    const before = kept.length
    for (const raw of (res && res.answers) || []) {
      const a = (raw || '').trim()
      if (!a || a.length > MAX_LEN) continue
      const k = norm(a)
      if (!k || seen.has(k)) continue
      seen.add(k)
      kept.push(a)
      if (kept.length >= tone.target) break
    }
    log(`${lang.code}/${tone.key}: ${kept.length}/${tone.target}`)
    if (kept.length === before) { dry++; if (dry >= 3) break } else dry = 0
  }
  return { lang: lang.code, tone: tone.key, list: kept }
}

const results = await parallel(
  LANGS.flatMap(lang => TONES.map(tone => () => genTone(lang, tone)))
)

const byLang = {}
for (const L of LANGS) byLang[L.code] = []
for (const r of results.filter(Boolean)) byLang[r.lang].push(...r.list)

const counts = {}
for (const L of LANGS) counts[L.code] = byLang[L.code].length
log('final counts: ' + JSON.stringify(counts))
return byLang
