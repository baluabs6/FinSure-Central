import { useState, useEffect } from 'react'
const api = async (url, opts) => { const r = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...opts }); if (!r.ok) throw new Error(await r.text()); return r.json() }
const box = { border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 20 }

function Claims() {
  const [claims, setClaims] = useState([]); const [form, setForm] = useState({ policyNumber: '', claimantName: '', description: '' }); const [err, setErr] = useState('')
  const load = () => api('/api/claims').then(setClaims).catch(e => setErr(e.message))
  useEffect(() => { load() }, [])
  const submit = async e => { e.preventDefault(); setErr(''); try { await api('/api/claims', { method: 'POST', body: JSON.stringify(form) }); setForm({ policyNumber: '', claimantName: '', description: '' }); load() } catch (e) { setErr(e.message) } }
  return (<section style={box}><h2>Claim Tracker</h2>
    <form onSubmit={submit} style={{ display: 'grid', gap: 8 }}>
      {['policyNumber', 'claimantName', 'description'].map(k => <input key={k} placeholder={k} value={form[k]} onChange={e => setForm({ ...form, [k]: e.target.value })} required />)}
      <button>Submit claim</button></form>
    {err && <p style={{ color: 'crimson' }}>{err}</p>}
    <ul>{claims.map(c => <li key={c.id}>#{c.id} · {c.policyNumber} · <b>{c.status}</b>{c.rejectionReason && ` – ${c.rejectionReason}`}</li>)}</ul></section>)
}

function Decoder() {
  const [text, setText] = useState(''); const [out, setOut] = useState(''); const [busy, setBusy] = useState(false)
  const run = async () => { setBusy(true); setOut(''); try { setOut((await api('/api/policies/decode', { method: 'POST', body: JSON.stringify({ policyText: text }) })).summary) } catch (e) { setOut('Error: ' + e.message) } setBusy(false) }
  return (<section style={box}><h2>Policy Decoder (AI)</h2>
    <textarea rows={8} style={{ width: '100%' }} placeholder="Paste policy wording here" value={text} onChange={e => setText(e.target.value)} />
    <button onClick={run} disabled={busy || !text.trim()}>{busy ? 'Decoding…' : 'Decode policy'}</button>
    <pre style={{ whiteSpace: 'pre-wrap' }}>{out}</pre></section>)
}

export default function App() {
  return (<main style={{ maxWidth: 800, margin: '0 auto', padding: 24, fontFamily: 'system-ui, sans-serif' }}>
    <h1>FinSure Central</h1><p>Finance and insurance, in plain language.</p><Claims /><Decoder /></main>)
}
