import { useState, useEffect } from 'react'
const api = async (url, opts) => { const r = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...opts }); if (!r.ok) throw new Error(await r.text()); return r.json() }
const box = { border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 20 }

function Claims() {
  const [claims, setClaims] = useState([]); const [form, setForm] = useState({ policyNumber: '', claimantName: '', description: '' }); const [err, setErr] = useState('')
  const [assist, setAssist] = useState({ id: null, title: '', text: '', busy: false })
  const load = () => api('/api/claims').then(setClaims).catch(e => setErr(e.message))
  useEffect(() => { load() }, [])
  const submit = async e => { e.preventDefault(); setErr(''); try { await api('/api/claims', { method: 'POST', body: JSON.stringify(form) }); setForm({ policyNumber: '', claimantName: '', description: '' }); load() } catch (e) { setErr(e.message) } }
  const reject = async c => { const reason = window.prompt('Rejection reason from the insurer'); if (!reason) return; try { await api(`/api/claims/${c.id}/status`, { method: 'PUT', body: JSON.stringify({ status: 'REJECTED', rejectionReason: reason }) }); load() } catch (e) { setErr(e.message) } }
  const ask = async (c, path, title) => {
    setAssist({ id: c.id, title, text: '', busy: true })
    try {
      const body = { policyNumber: c.policyNumber, claimantName: c.claimantName, description: c.description, rejectionReason: c.rejectionReason }
      const r = await api(`/api/policies/${path}`, { method: 'POST', body: JSON.stringify(body) })
      setAssist({ id: c.id, title, text: r.result, busy: false })
    } catch (e) { setAssist({ id: c.id, title, text: 'Error: ' + e.message, busy: false }) }
  }
  return (<section style={box}><h2>Claim Tracker</h2>
    <form onSubmit={submit} style={{ display: 'grid', gap: 8 }}>
      {['policyNumber', 'claimantName', 'description'].map(k => <input key={k} placeholder={k} value={form[k]} onChange={e => setForm({ ...form, [k]: e.target.value })} required />)}
      <button>Submit claim</button></form>
    {err && <p style={{ color: 'crimson' }}>{err}</p>}
    <ul>{claims.map(c => <li key={c.id} style={{ marginBottom: 8 }}>#{c.id} · {c.policyNumber} · <b>{c.status}</b>{c.rejectionReason && ` – ${c.rejectionReason}`}{' '}
      {c.status !== 'REJECTED' && <button onClick={() => reject(c)}>Mark rejected</button>}
      {c.status === 'REJECTED' && <>
        <button onClick={() => ask(c, 'explain-rejection', 'Why was it rejected?')}>Explain rejection</button>{' '}
        <button onClick={() => ask(c, 'appeal-draft', 'Draft appeal')}>Draft appeal</button></>}
      {assist.id === c.id && <div style={{ background: '#f6f6f6', padding: 12, marginTop: 6, borderRadius: 6 }}>
        <b>{assist.title}</b>{assist.busy ? <p>Working…</p> : <pre style={{ whiteSpace: 'pre-wrap' }}>{assist.text}</pre>}</div>}
    </li>)}</ul></section>)
}

function Decoder() {
  const [text, setText] = useState(''); const [out, setOut] = useState(''); const [busy, setBusy] = useState(false)
  const run = async () => { setBusy(true); setOut(''); try { setOut((await api('/api/policies/decode', { method: 'POST', body: JSON.stringify({ policyText: text }) })).summary) } catch (e) { setOut('Error: ' + e.message) } setBusy(false) }
  return (<section style={box}><h2>Policy Decoder (AI)</h2>
    <textarea rows={8} style={{ width: '100%' }} placeholder="Paste policy wording here" value={text} onChange={e => setText(e.target.value)} />
    <button onClick={run} disabled={busy || !text.trim()}>{busy ? 'Decoding…' : 'Decode policy'}</button>
    <pre style={{ whiteSpace: 'pre-wrap' }}>{out}</pre></section>)
}

const NUM = new Set(['claimAmount', 'sumInsured', 'policyAgeMonths', 'documentsMissing', 'priorClaimsLast12Months', 'annualIncome', 'dependents', 'loans', 'existingCover'])
const LIST = new Set(['documents'])
const TOOLS = [
  { name: 'Ingest policy for Q&A', fields: [['policyNumber'], ['text', 1]], path: f => `/api/ai/policy/${f.policyNumber}/ingest` },
  { name: 'Policy Q&A', fields: [['policyNumber'], ['question'], ['language']], path: f => `/api/ai/policy/${f.policyNumber}/ask` },
  { name: 'Extract document fields', fields: [['documentType'], ['documentText', 1]], path: '/api/ai/extract' },
  { name: 'Check claim documents', fields: [['claimType'], ['documents', 1], ['language']], path: '/api/ai/check-documents' },
  { name: 'Compare policies', fields: [['policyA', 1], ['policyB', 1], ['language']], path: '/api/ai/compare' },
  { name: 'Claim risk and fraud score', fields: [['claimType'], ['claimAmount'], ['sumInsured'], ['policyAgeMonths'], ['documentsMissing'], ['priorClaimsLast12Months']], path: '/api/ai/risk' },
  { name: 'Coverage gap advisor', fields: [['annualIncome'], ['dependents'], ['loans'], ['city'], ['existingCover'], ['language']], path: '/api/ai/coverage' },
  { name: 'Scam checker', fields: [['text', 1]], path: '/api/ai/scam-check' },
  { name: 'Claims copilot', fields: [['message', 1]], path: '/api/ai/copilot' },
  { name: 'Notifications', fields: [], path: '/api/ai/notifications', get: true },
  { name: 'AI audit log', fields: [], path: '/api/ai/audit', get: true }
]

function AiTools() {
  const [i, setI] = useState(0); const [vals, setVals] = useState({}); const [out, setOut] = useState(''); const [busy, setBusy] = useState(false)
  const t = TOOLS[i]
  const run = async () => {
    setBusy(true); setOut('')
    try {
      const body = {}
      t.fields.forEach(([k]) => { const v = vals[k] ?? ''; body[k] = NUM.has(k) ? Number(v) : LIST.has(k) ? v.split('\n').filter(Boolean) : v })
      const r = await api(typeof t.path === 'function' ? t.path(vals) : t.path, t.get ? {} : { method: 'POST', body: JSON.stringify(body) })
      setOut(typeof r === 'string' ? r : JSON.stringify(r, null, 2))
    } catch (e) { setOut('Error: ' + e.message) }
    setBusy(false)
  }
  return (<section style={box}><h2>AI Tools</h2>
    <select value={i} onChange={e => { setI(Number(e.target.value)); setVals({}); setOut('') }}>{TOOLS.map((x, n) => <option key={x.name} value={n}>{x.name}</option>)}</select>
    <div style={{ display: 'grid', gap: 8, margin: '12px 0' }}>{t.fields.map(([k, multi]) => multi
      ? <textarea key={k} rows={5} placeholder={k} value={vals[k] || ''} onChange={e => setVals({ ...vals, [k]: e.target.value })} />
      : <input key={k} placeholder={k} value={vals[k] || ''} onChange={e => setVals({ ...vals, [k]: e.target.value })} />)}</div>
    <button onClick={run} disabled={busy}>{busy ? 'Working…' : 'Run'}</button>
    <pre style={{ whiteSpace: 'pre-wrap' }}>{out}</pre></section>)
}

export default function App() {
  return (<main style={{ maxWidth: 800, margin: '0 auto', padding: 24, fontFamily: 'system-ui, sans-serif' }}>
    <h1>FinSure Central</h1><p>Finance and insurance, in plain language.</p><Claims /><Decoder /><AiTools /></main>)
}
