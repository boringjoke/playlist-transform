export type TrackStatus = 'PARSED' | 'INCOMPLETE' | 'AMBIGUOUS' | 'DUPLICATE' | 'INVALID'

export type EditableField = 'title' | 'artists' | 'version'

export interface ParsedTrack {
  clientId: string
  sourceIndex: number
  rawText: string
  title: string
  artists: string[]
  version: string | null
  status: TrackStatus
  confidence: number
  warnings: string[]
}

export interface ManualFields {
  title: boolean
  artists: boolean
  version: boolean
}

export interface DraftTrack extends ParsedTrack {
  manualFields: ManualFields
}

export interface ReparseConflict {
  key: string
  sourceIndex: number
  clientId: string
  field: EditableField
  currentValue: string | string[] | null
  candidateValue: string | string[] | null
}

export interface ReparseMatch {
  current: DraftTrack
  candidate: ParsedTrack
  currentIndex: number
  candidateIndex: number
  conflicts: ReparseConflict[]
}

export type ReparsePlanKind = 'merge' | 'conflict' | 'structure'

export interface ReparsePlan {
  kind: ReparsePlanKind
  matches: ReparseMatch[]
  unmatchedCurrent: DraftTrack[]
  unmatchedCandidate: ParsedTrack[]
  conflicts: ReparseConflict[]
  candidateTracks: ParsedTrack[]
}

export type MusicProvider = 'QQ_MUSIC' | 'NETEASE_MUSIC'

export interface PlaylistDraft {
  schemaVersion: number
  sourceText: string
  parserRuleVersion: string
  tracks: DraftTrack[]
  selectedProvider?: MusicProvider
  updatedTime: string
}

export interface ParseSummary {
  total: number
  parsed: number
  incomplete: number
  ambiguous: number
  duplicate: number
  invalid: number
}

export interface ParseTextResponse {
  parserRuleVersion: string
  tracks: ParsedTrack[]
  summary: ParseSummary
}

export interface ApiResponse<T> {
  code: string
  message: string
  data: T | null
}
