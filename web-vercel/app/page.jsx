import React, { useEffect, useMemo, useRef, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Slider } from "@/components/ui/slider";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Beaker, Brain, Check, Loader2, Play, Upload } from "lucide-react";
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from "recharts";

/**
 * API base resolution WITHOUT using process.env (which is undefined in some browser sandboxes).
 * Priority:
 *  1) <meta name="api-base" content="https://api.example.com" />
 *  2) window.__API_BASE__ (set in a script tag before this bundle)
 *  3) If on :3000 (dev), assume backend :8080. Otherwise same-origin.
 *  4) Fallback to http://localhost:8080 when SSR/no window.
 */
function resolveApiBase(): string {
  if (typeof window !== "undefined" && typeof document !== "undefined") {
    const meta = document.querySelector('meta[name="api-base"]') as HTMLMetaElement | null;
    if (meta?.content) return meta.content.trim();
    const winAny: any = window as any;
    if (winAny.__API_BASE__) return String(winAny.__API_BASE__);
    const { protocol, hostname, port } = window.location;
    if (port === "3000") return `${protocol}//${hostname}:8080`;
    return `${protocol}//${hostname}${port ? `:${port}` : ""}`;
  }
  // SSR or non-browser fallback
  return "http://localhost:8080";
}

// Tiny util
const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));

// Demo chart data
const demoSeries = Array.from({ length: 10 }, (_, i) => ({
  step: i + 1,
  loss: 1 / (i + 1) + Math.random() * 0.05,
  acc: Math.min(0.1 + i * 0.05, 0.99),
}));

// Helper to safely extract a prediction label from arbitrary API payloads
function extractPrediction(data: any): string {
  try {
    if (data && typeof data === "object" && "label" in data) return String((data as any).label);
    if (typeof data === "string") return data;
    return JSON.stringify(data);
  } catch {
    return "unknown";
  }
}

export default function NeuralNetStudio() {
  const apiBase = useMemo(resolveApiBase, []);

  const [model, setModel] = useState("mlp");
  const [batch, setBatch] = useState(32);
  const [epochs, setEpochs] = useState(5);
  const [lr, setLr] = useState(0.01);
  const [status, setStatus] = useState<string>("");
  const [busy, setBusy] = useState(false);
  const [series, setSeries] = useState(demoSeries);
  const [file, setFile] = useState<File | undefined>();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [prediction, setPrediction] = useState<string>("—");

  // --- Dev/Smoke tests state ---
  type T = { name: string; run: () => Promise<string> };
  const [testOutput, setTestOutput] = useState<string>("");
  const tests: T[] = useMemo(() => [
    {
      name: "Resolve API base",
      run: async () => `Resolved apiBase = ${apiBase}`,
    },
    {
      name: "GET /health",
      run: async () => {
        const r = await fetch(`${apiBase}/health`, { method: "GET" });
        const txt = await r.text().catch(() => "(no body)");
        return `status=${r.status} body=${txt.slice(0, 200)}`;
      },
    },
    {
      name: "POST /train (dry-call)",
      run: async () => {
        // Some servers accept empty body, others require fields; we try a minimal set.
        const r = await fetch(`${apiBase}/train`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ model: "mlp", batch: 8, epochs: 1, lr: 0.01 }),
        });
        const txt = await r.text().catch(() => "(no body)");
        return `status=${r.status} body=${txt.slice(0, 200)}`;
      },
    },
    // Added local parsing tests to assert UI behavior when API shape differs
    {
      name: "Parse sample predict payload (with label)",
      run: async () => {
        const sample = { label: 7, score: 0.98 };
        return `extractPrediction => ${extractPrediction(sample)}`;
      },
    },
    {
      name: "Parse sample predict payload (no label)",
      run: async () => {
        const sample = { value: 3, confidence: 0.77 };
        return `extractPrediction => ${extractPrediction(sample)}`; // should stringify sample
      },
    },
  ], [apiBase]);

  // Draw pad helpers
  useEffect(() => {
    const c = canvasRef.current; if (!c) return;
    const ctx = c.getContext("2d"); if (!ctx) return;
    ctx.fillStyle = "white"; ctx.fillRect(0, 0, c.width, c.height);
    let drawing = false; let last: { x: number; y: number } | null = null;
    const scale = typeof window !== "undefined" ? window.devicePixelRatio || 1 : 1;
    const start = (e: PointerEvent) => { drawing = true; last = { x: e.offsetX * scale, y: e.offsetY * scale }; };
    const move = (e: PointerEvent) => {
      if (!drawing || !last || !ctx) return;
      ctx.strokeStyle = "black"; ctx.lineWidth = 20; ctx.lineCap = "round"; ctx.beginPath();
      ctx.moveTo(last.x, last.y); ctx.lineTo(e.offsetX * scale, e.offsetY * scale); ctx.stroke();
      last = { x: e.offsetX * scale, y: e.offsetY * scale };
    };
    const end = () => { drawing = false; last = null; };
    c.addEventListener("pointerdown", start);
    c.addEventListener("pointermove", move);
    c.addEventListener("pointerup", end);
    c.addEventListener("pointerleave", end);
    return () => {
      c.removeEventListener("pointerdown", start);
      c.removeEventListener("pointermove", move);
      c.removeEventListener("pointerup", end);
      c.removeEventListener("pointerleave", end);
    };
  }, []);

  const clearCanvas = () => {
    const c = canvasRef.current; if (!c) return; const ctx = c.getContext("2d"); if (!ctx) return;
    ctx.fillStyle = "white"; ctx.fillRect(0, 0, c.width, c.height);
  };

  const postJSON = async (path: string, body: any) => {
    const r = await fetch(`${apiBase}${path}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    if (!r.ok) throw new Error(await r.text());
    return r.json();
  };

  const startTraining = async () => {
    setBusy(true); setStatus("Starting training...");
    try {
      // optional: upload weights or dataset if provided
      if (file) {
        setStatus("Uploading dataset...");
        const fd = new FormData(); fd.append("file", file);
        const up = await fetch(`${apiBase}/dataset/upload`, { method: "POST", body: fd });
        if (!up.ok) throw new Error(await up.text());
      }
      setStatus("Calling /train API...");
      // expected shape on server side (you can adapt your Spring controller)
      await postJSON("/train", { model, batch, epochs, lr });
      // Simulate streaming updates; replace with SSE/WebSocket in prod
      setStatus("Training...");
      for (let i = 0; i < epochs; i++) {
        await sleep(400);
        setSeries((s) => s.concat({ step: s.length + 1, loss: Math.max(0.01, s[s.length - 1].loss * 0.8), acc: Math.min(0.99, s[s.length - 1].acc + 0.05) }));
      }
      setStatus("Done");
    } catch (err: any) { setStatus(`Error: ${err.message}`); }
    finally { setBusy(false); }
  };

  const predictCanvas = async () => {
    if (!canvasRef.current) return;
    const blob: Blob = await new Promise((res) => canvasRef.current!.toBlob((b) => res(b as Blob), "image/png"));
    const fd = new FormData(); fd.append("file", blob, "digit.png");
    setBusy(true); setStatus("Predicting...");
    try {
      const r = await fetch(`${apiBase}/predict`, { method: "POST", body: fd });
      if (!r.ok) throw new Error(await r.text());
      const data = await r.json();
      setPrediction(extractPrediction(data));
      setStatus("Prediction complete");
    } catch (err: any) { setStatus(`Error: ${err.message}`); }
    finally { setBusy(false); }
  };

  const runTests = async () => {
    setTestOutput("");
    for (const t of tests) {
      setTestOutput((p) => p + `\n[RUN] ${t.name}`);
      try {
        const out = await t.run();
        setTestOutput((p) => p + `\n[OK ] ${out}`);
      } catch (e: any) {
        setTestOutput((p) => p + `\n[ERR] ${e?.message || e}`);
      }
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 text-slate-100">
      <header className="max-w-6xl mx-auto px-4 py-10 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-2xl bg-slate-800/80 backdrop-blur border border-slate-700"><Brain className="w-6 h-6" /></div>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Neural Net Studio</h1>
            <p className="text-sm text-slate-400">Train, monitor, and test your convulational neural networks models</p>
          </div>
        </div>
        <Badge variant="secondary" className="bg-slate-800 border-slate-700">API: {apiBase}</Badge>
      </header>

      <main className="max-w-6xl mx-auto px-4 pb-16">
        <Tabs defaultValue="train" className="">
          <TabsList className="grid grid-cols-4 w-full md:w-auto">
            <TabsTrigger value="train">Training</TabsTrigger>
            <TabsTrigger value="inference">Inference</TabsTrigger>
            <TabsTrigger value="logs">Logs</TabsTrigger>
            <TabsTrigger value="tests">Tests</TabsTrigger>
          </TabsList>

          <TabsContent value="train" className="mt-6">
            <div className="grid md:grid-cols-2 gap-6">
              <Card className="bg-slate-900/60 border-slate-800">
                <CardContent className="p-6 space-y-5">
                  <div>
                    <h2 className="text-lg font-semibold">Hyperparameters</h2>
                    <p className="text-slate-400 text-sm">Select model and tune knobs, then launch a training job.</p>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="col-span-2">
                      <label className="text-sm text-slate-300">Model</label>
                      <Select value={model} onValueChange={setModel}>
                        <SelectTrigger className="bg-slate-950 border-slate-800"><SelectValue placeholder="Select model" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="mlp">Dense (MLP)</SelectItem>
                          <SelectItem value="cnn">CNN</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <label className="text-sm text-slate-300">Batch size: {batch}</label>
                      <Slider value={[batch]} min={8} max={256} step={8} onValueChange={(v) => setBatch(v[0])} />
                    </div>
                    <div>
                      <label className="text-sm text-slate-300">Epochs: {epochs}</label>
                      <Slider value={[epochs]} min={1} max={50} step={1} onValueChange={(v) => setEpochs(v[0])} />
                    </div>
                    <div className="col-span-2">
                      <label className="text-sm text-slate-300">Learning rate: {lr}</label>
                      <Slider value={[lr]} min={0.0001} max={0.1} step={0.0001} onValueChange={(v) => setLr(Number(v[0].toFixed(4)))} />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm text-slate-300">Optional dataset (.csv or .zip)</label>
                    <div className="flex items-center gap-3">
                      <Input type="file" accept=".csv,.zip" onChange={(e) => setFile(e.target.files?.[0])} className="bg-slate-950 border-slate-800" />
                      <Button variant="secondary" className="gap-2" onClick={() => setFile(undefined)}><Check className="w-4 h-4" /> Clear</Button>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <Button onClick={startTraining} disabled={busy} className="gap-2"><Play className="w-4 h-4" />{busy ? "Working..." : "Start training"}</Button>
                    {busy && <Loader2 className="w-5 h-5 animate-spin" />}
                    <span className="text-slate-400 text-sm">{status}</span>
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-slate-900/60 border-slate-800">
                <CardContent className="p-6 space-y-4">
                  <div className="flex items-center gap-2"><Beaker className="w-5 h-5" /><h2 className="text-lg font-semibold">Training metrics</h2></div>
                  <div className="h-64">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={series} margin={{ top: 10, right: 20, bottom: 0, left: -20 }}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="step" tick={{ fontSize: 12 }} />
                        <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
                        <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} />
                        <Tooltip />
                        <Line yAxisId="left" type="monotone" dataKey="loss" dot={false} />
                        <Line yAxisId="right" type="monotone" dataKey="acc" dot={false} />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value="inference" className="mt-6">
            <div className="grid md:grid-cols-2 gap-6">
              <Card className="bg-slate-900/60 border-slate-800">
                <CardContent className="p-6 space-y-4">
                  <h2 className="text-lg font-semibold">Draw a digit</h2>
                  <p className="text-slate-400 text-sm">
                    Draw and send to <code>/predict</code>. Your Spring API should accept multipart form-data under key <code>file</code> and return JSON <code>{'{'} label, score {'}'}</code>.
                  </p>
                  <div className="p-3 rounded-2xl bg-white shadow-inner">
                    <canvas ref={canvasRef} width={300} height={300} className="w-full rounded-xl border" />
                  </div>
                  <div className="flex gap-3">
                    <Button onClick={predictCanvas} disabled={busy} className="gap-2"><Upload className="w-4 h-4" />Predict</Button>
                    <Button onClick={clearCanvas} variant="secondary">Clear</Button>
                  </div>
                  <div className="text-sm text-slate-400">Prediction: <span className="text-slate-100 font-semibold">{prediction}</span></div>
                </CardContent>
              </Card>

              <Card className="bg-slate-900/60 border-slate-800">
                <CardContent className="p-6 space-y-4">
                  <h2 className="text-lg font-semibold">Upload an image</h2>
                  <p className="text-slate-400 text-sm">PNG/JPG will be resized to 28×28 in your backend before inference.</p>
                  <div className="flex gap-3 items-center">
                    <Input type="file" accept="image/*" className="bg-slate-950 border-slate-800" onChange={async (e) => {
                      const f = e.target.files?.[0]; if (!f) return; const fd = new FormData(); fd.append("file", f);
                      setBusy(true); setStatus("Predicting...");
                      try { const r = await fetch(`${apiBase}/predict`, { method: "POST", body: fd }); if (!r.ok) throw new Error(await r.text()); const data = await r.json(); setPrediction(extractPrediction(data)); setStatus("Prediction complete"); }
                      catch (err: any) { setStatus(`Error: ${err.message}`) } finally { setBusy(false); }
                    }} />
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value="logs" className="mt-6">
            <Card className="bg-slate-900/60 border-slate-800">
              <CardContent className="p-6 space-y-3">
                <h2 className="text-lg font-semibold">System</h2>
                <div className="grid md:grid-cols-3 gap-4">
                  <div>
                    <div className="text-slate-400 text-sm">Model</div>
                    <div className="font-medium">{model.toUpperCase()}</div>
                  </div>
                  <div>
                    <div className="text-slate-400 text-sm">Epochs</div>
                    <div className="font-medium">{epochs}</div>
                  </div>
                  <div>
                    <div className="text-slate-400 text-sm">Learning rate</div>
                    <div className="font-medium">{lr}</div>
                  </div>
                </div>
                <div className="text-slate-400 text-sm">{status || "Idle"}</div>
                <Progress value={busy ? 66 : (status === "Done" ? 100 : 0)} className="h-2" />
                <Textarea placeholder="Console output (attach your SSE/WebSocket logs here)" className="bg-slate-950 border-slate-800 min-h-40" />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="tests" className="mt-6">
            <Card className="bg-slate-900/60 border-slate-800">
              <CardContent className="p-6 space-y-4">
                <h2 className="text-lg font-semibold">Smoke tests</h2>
                <p className="text-slate-400 text-sm">These simple tests help verify the expected API endpoints without changing your backend.</p>
                <div className="flex gap-3 flex-wrap">
                  {tests.map((t) => (
                    <Button key={t.name} variant="secondary" onClick={async () => {
                      setTestOutput((p) => p + `\n\n=== ${t.name} ===`);
                      try { const out = await t.run(); setTestOutput((p) => p + `\n${out}`); }
                      catch (e: any) { setTestOutput((p) => p + `\nERROR: ${e?.message || e}`); }
                    }}>{t.name}</Button>
                  ))}
                  <Button onClick={runTests} className="gap-2"><Beaker className="w-4 h-4"/>Run all</Button>
                </div>
                <Textarea value={testOutput} onChange={()=>{}} className="bg-slate-950 border-slate-800 min-h-48 font-mono text-xs" />
                <div className="text-xs text-slate-500">Tip: You can also set <code>window.__API_BASE__</code> or add <code>&lt;meta name="api-base" content="https://api.example.com" /&gt;</code> in your HTML to override.</div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        <footer className="mt-10 text-center text-slate-500 text-xs">Made for the Neural-networks Java project – drop this component into <code>app/page.tsx</code> (Next.js) or any React route. To configure the API in the browser-only builds, prefer <code>&lt;meta name="api-base" ...&gt;</code> or <code>window.__API_BASE__</code>.</footer>
      </main>
    </div>
  );
}
