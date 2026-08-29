// Field renames are deliberate, not cosmetic.
//
// `contribution` used to mean "self time / root duration", which summed to
// 170% on a real trace because concurrent siblings each claimed the same
// milliseconds. It is replaced by two fields that each sum to 100%:
//   workShare     - share of all work done in the trace
//   criticalShare - share of the critical path (i.e. what actually caused delay)
// A service can be high on the first and zero on the second; that is the whole
// point of the tool, so the UI should never conflate them again.

export interface GraphNode {
  spanId: string; service: string; status: string;
  startTime: number; duration: number; selfTime: number;
  rootCause: boolean; sink: boolean; onCriticalPath: boolean;
}

export interface GraphEdge { from: string; to: string; }

export interface TimelineBar {
  spanId: string; service: string; status: string;
  startTime: number; duration: number; selfTime: number;
  rootCause: boolean; sink: boolean; onCriticalPath: boolean;
}

export interface FailureView {
  rootCauses: string[];
  rootCauseSpanIds: string[]; // highlight by THIS, never by service name
  affectedServices: string[];
  confidence: string;
  reason: string;
}

export interface ServiceSelf {
  service: string;
  selfTime: number;
  workShare: number;
  criticalPathTime: number;
  criticalShare: number;
}

export interface LatencyView {
  sink: string;
  criticalShare: number;
  confidence: string;
  reason: string;
  criticalPathTime: number;
  breakdown: ServiceSelf[];
}

export interface AnalyzeResponse {
  traceId: string;
  mode: string; // which verdict to lead with; both are now always populated
  failure: FailureView; // no longer nullable
  latency: LatencyView; // no longer nullable
  nodes: GraphNode[];
  edges: GraphEdge[];
  timeline: TimelineBar[];
  totalDuration: number;
}

export interface AuthResponse { token: string; email: string; }
export interface ReportSummary { id: string; traceId: string; mode: string; createdAt: string; }
