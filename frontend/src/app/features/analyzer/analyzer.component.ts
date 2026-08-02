import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AnalyzeResponse, GraphNode } from '../../shared/models/models';

interface PositionedNode { x: number; y: number; node: GraphNode; }
interface Edge { d: string; }
interface Bar { y: number; x: number; w: number; label: string; dur: number; sink: boolean; }

const SAMPLE_FAIL = `{
  "traceId": "cascade-1",
  "spans": [
    {"spanId":"a","parentSpanId":null,"serviceName":"api-gateway","startTime":0,"duration":800,"status":"ERROR"},
    {"spanId":"b","parentSpanId":"a","serviceName":"order-service","startTime":50,"duration":600,"status":"ERROR"},
    {"spanId":"c","parentSpanId":"b","serviceName":"payment-service","startTime":100,"duration":400,"status":"ERROR"},
    {"spanId":"d","parentSpanId":"c","serviceName":"database","startTime":150,"duration":120,"status":"ERROR"}
  ]
}`;

const SAMPLE_LATENCY = `{
  "traceId": "latency-parallel-1",
  "spans": [
    {"spanId":"a","parentSpanId":null,"serviceName":"api-gateway","startTime":0,"duration":8000,"status":"OK"},
    {"spanId":"b","parentSpanId":"a","serviceName":"order-service","startTime":100,"duration":700,"status":"OK"},
    {"spanId":"c","parentSpanId":"a","serviceName":"payment-service","startTime":200,"duration":6200,"status":"OK"},
    {"spanId":"d","parentSpanId":"c","serviceName":"fraud-check","startTime":300,"duration":1200,"status":"OK"},
    {"spanId":"e","parentSpanId":"c","serviceName":"card-network","startTime":800,"duration":1200,"status":"OK"}
  ]
}`;

@Component({
  selector: 'app-analyzer',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './analyzer.component.html',
  styleUrl: './analyzer.component.css'
})
export class AnalyzerComponent {
  trace = SAMPLE_FAIL;
  mode = 'AUTO';
  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<AnalyzeResponse | null>(null);

  // node geometry (referenced by the template too)
  readonly NW = 150;
  readonly NH = 46;
  readonly labelW = 128;
  readonly barH = 16;
  readonly timelineW = 460;

  nodes = signal<PositionedNode[]>([]);
  edges = signal<Edge[]>([]);
  graphW = signal(0);
  graphH = signal(0);
  bars = signal<Bar[]>([]);
  timelineH = signal(0);

  constructor(private api: ApiService) { }

  loadFail(): void { this.trace = SAMPLE_FAIL; }
  loadLatency(): void { this.trace = SAMPLE_LATENCY; }

  analyze(): void {
    let parsed: unknown;
    try {
      parsed = JSON.parse(this.trace);
    } catch {
      this.error.set('Trace is not valid JSON.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.api.analyze(parsed, this.mode).subscribe({
      next: (res) => { this.renderResult(res); this.loading.set(false); },
      error: (err) => {
        this.result.set(null);
        this.error.set(this.messageFor(err));
        this.loading.set(false);
      }
    });
  }

  private messageFor(err: { status?: number; error?: { error?: string } }): string {
    if (err?.error?.error) { return err.error.error; }
    if (err?.status === 403) { return 'Forbidden — analyzing traces needs the ADMIN role.'; }
    if (err?.status === 401) { return 'Session expired — sign in again.'; }
    return 'Request failed.';
  }

  private renderResult(res: AnalyzeResponse): void {
    this.result.set(res);
    this.layoutGraph(res);
    this.layoutTimeline(res);
  }

  private layoutGraph(res: AnalyzeResponse): void {
    const COLW = 172, ROWH = 96, PADX = 16, PADY = 14;
    type N = GraphNode & { children: N[]; depth: number; slot: number };

    const byId = new Map<string, N>();
    res.nodes.forEach(n => byId.set(n.spanId, { ...n, children: [], depth: 0, slot: 0 }));

    const hasParent = new Set<string>();
    res.edges.forEach(e => {
      hasParent.add(e.to);
      const p = byId.get(e.from);
      const c = byId.get(e.to);
      if (p && c) { p.children.push(c); }
    });

    const rootMeta = res.nodes.find(n => !hasParent.has(n.spanId));
    if (!rootMeta) { this.nodes.set([]); this.edges.set([]); this.graphW.set(0); this.graphH.set(0); return; }
    const root = byId.get(rootMeta.spanId)!;

    const sortCh = (n: N) => { n.children.sort((a, b) => a.startTime - b.startTime); n.children.forEach(sortCh); };
    sortCh(root);

    let leaf = 0, maxDepth = 0;
    const assign = (n: N, depth: number) => {
      n.depth = depth;
      maxDepth = Math.max(maxDepth, depth);
      if (n.children.length === 0) {
        n.slot = leaf++;
      } else {
        n.children.forEach(c => assign(c, depth + 1));
        n.slot = (n.children[0].slot + n.children[n.children.length - 1].slot) / 2;
      }
    };
    assign(root, 0);

    const flat: N[] = [];
    const collect = (n: N) => { flat.push(n); n.children.forEach(collect); };
    collect(root);

    const cx = (n: N) => PADX + n.slot * COLW + this.NW / 2;
    const cy = (n: N) => PADY + n.depth * ROWH + this.NH / 2;

    this.nodes.set(flat.map(n => ({ x: PADX + n.slot * COLW, y: PADY + n.depth * ROWH, node: n })));

    const edges: Edge[] = [];
    flat.forEach(n => n.children.forEach(c => {
      const x1 = cx(n), y1 = cy(n) + this.NH / 2, x2 = cx(c), y2 = cy(c) - this.NH / 2, my = (y1 + y2) / 2;
      edges.push({ d: `M${x1},${y1} C${x1},${my} ${x2},${my} ${x2},${y2}` });
    }));
    this.edges.set(edges);

    this.graphW.set(PADX * 2 + (leaf - 1) * COLW + this.NW);
    this.graphH.set(PADY * 2 + maxDepth * ROWH + this.NH);
  }

  private layoutTimeline(res: AnalyzeResponse): void {
    const ROWH = 30, PADT = 6, PADR = 46;
    const total = res.totalDuration || Math.max(...res.timeline.map(b => b.startTime + b.duration), 1);
    const plotW = this.timelineW - this.labelW - PADR;

    this.bars.set(res.timeline.map((b, i) => ({
      y: PADT + i * ROWH,
      x: this.labelW + (b.startTime / total) * plotW,
      w: Math.max(3, (b.duration / total) * plotW),
      label: b.service.length > 16 ? b.service.slice(0, 15) + '…' : b.service,
      dur: b.duration,
      sink: b.sink
    })));
    this.timelineH.set(PADT * 2 + res.timeline.length * ROWH);
  }

  nodeClass(n: GraphNode): string {
    if (n.rootCause) { return 'n-root'; }
    if (n.sink) { return 'n-sink'; }
    if (n.status === 'ERROR') { return 'n-err'; }
    return 'n-ok';
  }

  nodeTag(n: GraphNode): string {
    if (n.rootCause) { return 'root cause'; }
    if (n.sink) { return 'self ' + n.exclusiveTime + 'ms'; }
    if (n.status === 'ERROR') { return 'errored'; }
    return n.duration + 'ms';
  }

  pct(x: number): string { return (x * 100).toFixed(1) + '%'; }
  round(x: number): number { return Math.round(x * 100); }
}
