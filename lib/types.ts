// ContextLog TypeScript Contracts

export type EntryType = 
  | "LOG" 
  | "REMINDER" 
  | "LEGAL_MATTER" 
  | "DECISION_PAUSE" 
  | "RAG_QUESTION";

export interface Matter {
  id: string;
  code: string;
  name: string;
  client_name: string;
  created_at: string;
}

export interface ParsedNote {
  clean_text: string;
  entry_type: EntryType;
  matter_or_project_code: string | null;
  scheduled_datetime: string | null;
  depth_level: number; // 1 to 5
  is_rag_query: boolean;
  google_calendar_sync: boolean;
}

export interface ContextNote {
  id: string;
  matter_id: string | null;
  matter_code?: string;
  raw_transcript: string;
  clean_text: string;
  entry_type: EntryType;
  depth_level: number;
  recorded_at: string;
  two_hour_block_start: string; // ISO String calculated via floor(epoch/7200)*7200
  scheduled_datetime: string | null;
  google_event_id: string | null;
}

export interface TwoHourRollup {
  block_start: string; // ISO String
  block_end: string;
  notes_count: number;
  total_billable_hours: number;
  matter_summaries: {
    matter_code: string;
    matter_name: string;
    client_name: string;
    notes: ContextNote[];
    hours: number;
  }[];
}

export interface GroundedCitation {
  document_id: string;
  document_title: string;
  chunk_content: string;
  page_number: number;
  similarity_score: number;
}

export interface DocumentChunk {
  id: string;
  document_id: string;
  title: string;
  content: string;
  page_number: number;
  embedding?: number[];
}
