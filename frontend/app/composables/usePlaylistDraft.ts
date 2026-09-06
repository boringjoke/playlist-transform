import type { DraftTrack, PlaylistDraft, TrackStatus } from '~/types/playlist'
import { PLAYLIST_DRAFT_SCHEMA_VERSION } from '~/utils/playlistLogic'

export const PLAYLIST_DRAFT_STORAGE_KEY = 'playlist-transform:draft'

type DraftLoadResult =
  | { status: 'missing' }
  | { status: 'invalid' }
  | { status: 'unavailable' }
  | { status: 'valid'; draft: PlaylistDraft }

const TRACK_STATUSES: TrackStatus[] = ['PARSED', 'INCOMPLETE', 'AMBIGUOUS', 'DUPLICATE', 'INVALID']

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isDraftTrack(value: unknown): value is DraftTrack {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const track = value as Partial<DraftTrack>
  const manualFields = track.manualFields
  return Boolean(
    typeof track.clientId === 'string' &&
    track.clientId.length > 0 &&
    typeof track.sourceIndex === 'number' &&
    Number.isInteger(track.sourceIndex) &&
    track.sourceIndex > 0 &&
    typeof track.rawText === 'string' &&
    typeof track.title === 'string' &&
    isStringArray(track.artists) &&
    (typeof track.version === 'string' || track.version === null) &&
    typeof track.status === 'string' &&
    TRACK_STATUSES.includes(track.status as TrackStatus) &&
    typeof track.confidence === 'number' &&
    Number.isFinite(track.confidence) &&
    isStringArray(track.warnings) &&
    typeof manualFields === 'object' &&
    manualFields !== null &&
    typeof manualFields.title === 'boolean' &&
    typeof manualFields.artists === 'boolean' &&
    typeof manualFields.version === 'boolean',
  )
}

function isPlaylistDraft(value: unknown): value is PlaylistDraft {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const draft = value as Partial<PlaylistDraft>
  return Boolean(
    draft.schemaVersion === PLAYLIST_DRAFT_SCHEMA_VERSION &&
    typeof draft.sourceText === 'string' &&
    typeof draft.parserRuleVersion === 'string' &&
    Array.isArray(draft.tracks) &&
    draft.tracks.every(isDraftTrack) &&
    typeof draft.updatedTime === 'string' &&
    (draft.selectedProvider === undefined || draft.selectedProvider === 'QQ_MUSIC' || draft.selectedProvider === 'NETEASE_MUSIC'),
  )
}

export function usePlaylistDraft() {
  const loadDraft = (): DraftLoadResult => {
    if (!import.meta.client) {
      return { status: 'unavailable' }
    }

    try {
      const rawDraft = window.localStorage.getItem(PLAYLIST_DRAFT_STORAGE_KEY)
      if (!rawDraft) {
        return { status: 'missing' }
      }

      const parsed: unknown = JSON.parse(rawDraft)
      if (!isPlaylistDraft(parsed)) {
        window.localStorage.removeItem(PLAYLIST_DRAFT_STORAGE_KEY)
        return { status: 'invalid' }
      }

      return { status: 'valid', draft: parsed }
    } catch {
      try {
        window.localStorage.removeItem(PLAYLIST_DRAFT_STORAGE_KEY)
      } catch {
        // 浏览器禁止访问 storage 时保持当前页面可用，不向用户暴露底层异常。
      }
      return { status: 'invalid' }
    }
  }

  const saveDraft = (draft: PlaylistDraft): PlaylistDraft => {
    const nextDraft: PlaylistDraft = {
      ...draft,
      updatedTime: new Date().toISOString(),
      tracks: draft.tracks.map((track) => ({
        ...track,
        artists: [...track.artists],
        warnings: [...track.warnings],
        manualFields: { ...track.manualFields },
      })),
    }

    if (import.meta.client) {
      try {
        window.localStorage.setItem(PLAYLIST_DRAFT_STORAGE_KEY, JSON.stringify(nextDraft))
      } catch {
        // 草稿保存失败不阻断当前编辑流程，页面会继续使用内存状态。
      }
    }

    return nextDraft
  }

  const clearDraft = () => {
    if (!import.meta.client) {
      return
    }

    try {
      window.localStorage.removeItem(PLAYLIST_DRAFT_STORAGE_KEY)
    } catch {
      // 清除失败时不伪造成功状态，由调用方保留当前页面提示。
    }
  }

  return {
    loadDraft,
    saveDraft,
    clearDraft,
  }
}
