// Jeden typ błędu domenowego dla całego pipeline'u — niesie kod i status HTTP.
// index.ts mapuje go na body { error: { code, message } }.

export class PipelineError extends Error {
  constructor(readonly code: string, message: string, readonly status: number) {
    super(message);
    this.name = "PipelineError";
  }
}

export const invalidRequest = (msg: string) => new PipelineError("invalid_request", msg, 400);
export const analysisFailed = (msg: string) => new PipelineError("analysis_failed", msg, 502);
export const internal = (msg: string) => new PipelineError("internal", msg, 500);
