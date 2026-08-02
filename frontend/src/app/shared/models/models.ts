export interface GraphNode {
  spanId: string; service: string; status: string;
  startTime: number; duration: number; exclusiveTime: number;
  rootCause: boolean; sink: boolean;
}
export interface GraphEdge { from: string; to: string; }
export interface TimelineBar {
  spanId: string; service: string; startTime: number; duration: number; sink: boolean;
}
export interface FailureView {
  rootCauses: string[]; affectedServices: string[]; confidence: string; reason: string;
}
export interface ServiceSelf { service: string; exclusiveTime: number; contribution: number; }
export interface LatencyView {
  sink: string; contribution: number; confidence: string; reason: string; breakdown: ServiceSelf[];
}
export interface AnalyzeResponse {
  traceId: string; mode: string;
  failure: FailureView | null; latency: LatencyView | null;
  nodes: GraphNode[]; edges: GraphEdge[]; timeline: TimelineBar[]; totalDuration: number;
}
export interface LoginResponse { token: string; username: string; roles: string[]; }
