import type {
  DraftTrack,
  EditableField,
  ManualFields,
  ParsedTrack,
  ParseSummary,
  PlaylistDraft,
  ReparseConflict,
  ReparseMatch,
  ReparsePlan,
  TrackStatus,
} from '~/types/playlist'

export const PLAYLIST_DRAFT_SCHEMA_VERSION = 1
export const MAX_PLAYLIST_TRACKS = 500

const DUPLICATE_WARNING = 'DUPLICATE_TRACK'
const INVALID_TITLE_WARNING = 'INVALID_EMPTY_TITLE'
const MISSING_ARTIST_WARNING = 'MISSING_ARTIST'

export function cleanEditedField(value: string | null | undefined): string {
  return String(value ?? '').trim().replace(/\s+/g, ' ')
}

export function splitArtistInput(value: string | null | undefined): string[] {
  return cleanEditedField(value)
    .split(/[、,，/／&＆]/u)
    .map((artist) => cleanEditedField(artist))
    .filter(Boolean)
}

export function artistInputValue(artists: string[]): string {
  return artists.join('、')
}

export function isMeaningfulTitle(value: string | null | undefined): boolean {
  return cleanEditedField(value).replace(/[\p{P}\p{S}\s]/gu, '').length > 0
}

export function normalizeKey(value: string | null | undefined): string {
  return cleanEditedField(value).normalize('NFKC').toLowerCase()
}

export function createManualFields(): ManualFields {
  return {
    title: false,
    artists: false,
    version: false,
  }
}

export function toDraftTrack(track: ParsedTrack): DraftTrack {
  return {
    ...track,
    artists: [...track.artists],
    warnings: [...track.warnings],
    manualFields: createManualFields(),
  }
}

export function createPlaylistDraft(
  sourceText: string,
  parserRuleVersion: string,
  tracks: ParsedTrack[],
): PlaylistDraft {
  return {
    schemaVersion: PLAYLIST_DRAFT_SCHEMA_VERSION,
    sourceText,
    parserRuleVersion,
    tracks: tracks.map(toDraftTrack),
    updatedTime: new Date().toISOString(),
  }
}

export function getParseSummary(tracks: DraftTrack[]): ParseSummary {
  const summary: ParseSummary = {
    total: tracks.length,
    parsed: 0,
    incomplete: 0,
    ambiguous: 0,
    duplicate: 0,
    invalid: 0,
  }

  for (const track of tracks) {
    switch (track.status) {
      case 'PARSED':
        summary.parsed += 1
        break
      case 'INCOMPLETE':
        summary.incomplete += 1
        break
      case 'AMBIGUOUS':
        summary.ambiguous += 1
        break
      case 'DUPLICATE':
        summary.duplicate += 1
        break
      case 'INVALID':
        summary.invalid += 1
        break
    }
  }

  return summary
}

export function hasManualFields(track: DraftTrack): boolean {
  return Object.values(track.manualFields).some(Boolean)
}

function removeWarning(warnings: string[], warning: string): string[] {
  return warnings.filter((item) => item !== warning)
}

function withWarning(warnings: string[], warning: string): string[] {
  return warnings.includes(warning) ? warnings : [...warnings, warning]
}

function getManualBaseState(track: DraftTrack): Pick<DraftTrack, 'status' | 'warnings'> {
  const title = cleanEditedField(track.title)
  const artists = track.artists.map((artist) => cleanEditedField(artist)).filter(Boolean)

  if (!isMeaningfulTitle(title)) {
    return {
      status: 'INVALID',
      warnings: [INVALID_TITLE_WARNING],
    }
  }

  if (artists.length === 0) {
    return {
      status: 'INCOMPLETE',
      warnings: [MISSING_ARTIST_WARNING],
    }
  }

  return {
    status: 'PARSED',
    warnings: [],
  }
}

function duplicateKey(track: DraftTrack): string | null {
  if (!isMeaningfulTitle(track.title) || track.artists.length === 0) {
    return null
  }

  return [normalizeKey(track.title), ...track.artists.map(normalizeKey)].join('\u001F')
}

export function recomputeTracks(tracks: DraftTrack[]): DraftTrack[] {
  const prepared = tracks.map((track) => {
    const cleanedTrack: DraftTrack = {
      ...track,
      title: cleanEditedField(track.title),
      artists: track.artists.map((artist) => cleanEditedField(artist)).filter(Boolean),
      version: track.version ? cleanEditedField(track.version) || null : null,
      warnings: [...track.warnings],
      manualFields: { ...track.manualFields },
    }

    if (hasManualFields(cleanedTrack)) {
      const manualState = getManualBaseState(cleanedTrack)
      return {
        ...cleanedTrack,
        status: manualState.status,
        warnings: manualState.warnings,
      }
    }

    return cleanedTrack
  })

  const seenKeys = new Set<string>()
  return prepared.map((track) => {
    const key = duplicateKey(track)
    if (key === null || track.status === 'INVALID' || track.status === 'AMBIGUOUS') {
      return track
    }

    if (seenKeys.has(key)) {
      return {
        ...track,
        status: 'DUPLICATE',
        warnings: withWarning(track.warnings, DUPLICATE_WARNING),
      }
    }

    seenKeys.add(key)
    return {
      ...track,
      status: track.status === 'DUPLICATE' ? 'PARSED' : track.status,
      warnings: removeWarning(track.warnings, DUPLICATE_WARNING),
    }
  })
}

export function removeDuplicateTracks(tracks: DraftTrack[]): DraftTrack[] {
  return recomputeTracks(tracks.filter((track) => track.status !== 'DUPLICATE'))
}

function fieldValue(track: DraftTrack | ParsedTrack, field: EditableField): string | string[] | null {
  if (field === 'artists') {
    return [...track.artists]
  }
  return track[field]
}

function fieldValuesEqual(left: string | string[] | null, right: string | string[] | null): boolean {
  if (Array.isArray(left) && Array.isArray(right)) {
    return left.length === right.length && left.every((value, index) => value === right[index])
  }
  return left === right
}

function getFieldConflict(
  current: DraftTrack,
  candidate: ParsedTrack,
  field: EditableField,
): ReparseConflict | null {
  if (!current.manualFields[field]) {
    return null
  }

  const currentValue = fieldValue(current, field)
  const candidateValue = fieldValue(candidate, field)
  if (fieldValuesEqual(currentValue, candidateValue)) {
    return null
  }

  return {
    key: `${current.clientId}:${field}`,
    sourceIndex: candidate.sourceIndex,
    clientId: current.clientId,
    field,
    currentValue,
    candidateValue,
  }
}

export function createReparsePlan(currentTracks: DraftTrack[], candidateTracks: ParsedTrack[]): ReparsePlan {
  const candidateByClientId = new Map(candidateTracks.map((track, index) => [track.clientId, { track, index }]))
  const candidateBySourceIndex = new Map(candidateTracks.map((track, index) => [track.sourceIndex, { track, index }]))
  const usedCandidateIndexes = new Set<number>()
  const matches: ReparseMatch[] = []
  const unmatchedCurrent: DraftTrack[] = []

  currentTracks.forEach((current, currentIndex) => {
    const byClientId = candidateByClientId.get(current.clientId)
    const bySourceIndex = candidateBySourceIndex.get(current.sourceIndex)
    const candidateEntry = byClientId && !usedCandidateIndexes.has(byClientId.index)
      ? byClientId
      : bySourceIndex && !usedCandidateIndexes.has(bySourceIndex.index)
        ? bySourceIndex
        : undefined

    if (!candidateEntry) {
      unmatchedCurrent.push(current)
      return
    }

    usedCandidateIndexes.add(candidateEntry.index)
    const conflicts = (['title', 'artists', 'version'] as EditableField[])
      .map((field) => getFieldConflict(current, candidateEntry.track, field))
      .filter((conflict): conflict is ReparseConflict => conflict !== null)

    matches.push({
      current,
      candidate: candidateEntry.track,
      currentIndex,
      candidateIndex: candidateEntry.index,
      conflicts,
    })
  })

  const unmatchedCandidate = candidateTracks.filter((_, index) => !usedCandidateIndexes.has(index))
  const conflicts = matches.flatMap((match) => match.conflicts)
  const hasStructureChange = unmatchedCurrent.length > 0 || unmatchedCandidate.length > 0

  return {
    kind: hasStructureChange ? 'structure' : conflicts.length > 0 ? 'conflict' : 'merge',
    matches,
    unmatchedCurrent,
    unmatchedCandidate,
    conflicts,
    candidateTracks,
  }
}

export function mergeReparsePlan(
  plan: ReparsePlan,
  adoptNewValues: Record<string, boolean> = {},
): DraftTrack[] {
  if (plan.kind === 'structure') {
    return plan.candidateTracks.map(toDraftTrack)
  }

  const merged = plan.matches
    .sort((left, right) => left.candidateIndex - right.candidateIndex)
    .map(({ current, candidate }) => {
      const manualFields = { ...current.manualFields }
      const values = {
        title: candidate.title,
        artists: [...candidate.artists],
        version: candidate.version,
      }
      let keptManualValue = false

      for (const field of ['title', 'artists', 'version'] as EditableField[]) {
        if (!current.manualFields[field]) {
          continue
        }

        const conflictKey = `${current.clientId}:${field}`
        if (adoptNewValues[conflictKey]) {
          manualFields[field] = false
        } else {
          values[field] = fieldValue(current, field) as never
          keptManualValue = true
        }
      }

      return recomputeTracks([{
        ...candidate,
        clientId: keptManualValue ? current.clientId : candidate.clientId,
        title: values.title as string,
        artists: values.artists as string[],
        version: values.version as string | null,
        manualFields,
      }])[0]!
    })

  return recomputeTracks(merged)
}

export function getStatusLabel(status: TrackStatus): string {
  const labels: Record<TrackStatus, string> = {
    PARSED: '已识别',
    INCOMPLETE: '信息不完整',
    AMBIGUOUS: '待确认',
    DUPLICATE: '疑似重复',
    INVALID: '无法识别',
  }
  return labels[status]
}

export function getWarningLabel(warning: string): string {
  const labels: Record<string, string> = {
    MISSING_ARTIST: '缺少歌手',
    AMBIGUOUS_SEPARATOR: '字段边界不明确',
    INVALID_EMPTY_TITLE: '歌名为空或无效',
    DUPLICATE_TRACK: '与前面的歌曲重复',
    SINGLE_SPACE_SPLIT: '按单个空格拆分，建议确认',
    VERSION_DETECTED: '已识别版本标记',
  }
  return labels[warning] ?? warning
}
