import { describe, expect, it } from 'vitest'
import type { ParsedTrack } from '../types/playlist'
import {
  createReparsePlan,
  createPlaylistDraft,
  mergeReparsePlan,
  recomputeTracks,
  removeDuplicateTracks,
  toDraftTrack,
} from './playlistLogic'

const parsedTrack = (overrides: Partial<ParsedTrack> = {}): ParsedTrack => ({
  clientId: 'track-1',
  sourceIndex: 1,
  rawText: '夜曲 - 周杰伦',
  title: '夜曲',
  artists: ['周杰伦'],
  version: null,
  status: 'PARSED',
  confidence: 0.98,
  warnings: [],
  ...overrides,
})

describe('playlist review logic', () => {
  it('补充歌手后将信息不完整条目更新为已识别', () => {
    const track = toDraftTrack(parsedTrack({
      artists: [],
      status: 'INCOMPLETE',
      confidence: 0.68,
      warnings: ['MISSING_ARTIST'],
    }))

    const [updated] = recomputeTracks([{
      ...track,
      artists: ['周杰伦'],
      manualFields: { ...track.manualFields, artists: true },
    }])

    expect(updated!.status).toBe('PARSED')
    expect(updated!.warnings).not.toContain('MISSING_ARTIST')
  })

  it('只删除后出现的重复项并保持其余顺序', () => {
    const tracks = recomputeTracks([
      toDraftTrack(parsedTrack({ clientId: 'first', sourceIndex: 1 })),
      toDraftTrack(parsedTrack({ clientId: 'second', sourceIndex: 2 })),
      toDraftTrack(parsedTrack({ clientId: 'third', sourceIndex: 3, title: '晴天', rawText: '晴天 - 周杰伦' })),
    ])

    expect(tracks.map((track) => track.status)).toEqual(['PARSED', 'DUPLICATE', 'PARSED'])
    expect(removeDuplicateTracks(tracks).map((track) => track.clientId)).toEqual(['first', 'third'])
    expect(removeDuplicateTracks(tracks).map((track) => track.sourceIndex)).toEqual([1, 3])
  })

  it('创建草稿时保留规则版本、顺序和未修改标记', () => {
    const draft = createPlaylistDraft('夜曲 - 周杰伦', 'v1', [parsedTrack()])

    expect(draft.schemaVersion).toBe(1)
    expect(draft.parserRuleVersion).toBe('v1')
    expect(draft.tracks[0]!.manualFields).toEqual({ title: false, artists: false, version: false })
    expect(draft.tracks[0]!.clientId).toBe('track-1')
  })

  it('重解析默认保留手工值，明确选择后才采用新值', () => {
    const current = toDraftTrack(parsedTrack({ title: '用户修改' }))
    current.manualFields.title = true
    const candidate = parsedTrack({ title: '新解析歌名' })
    const plan = createReparsePlan([current], [candidate])

    expect(plan.kind).toBe('conflict')
    expect(plan.conflicts[0]!.key).toBe('track-1:title')

    const kept = mergeReparsePlan(plan)[0]!
    expect(kept.title).toBe('用户修改')
    expect(kept.clientId).toBe('track-1')
    expect(kept.manualFields.title).toBe(true)

    const adopted = mergeReparsePlan(plan, { 'track-1:title': true })[0]!
    expect(adopted.title).toBe('新解析歌名')
    expect(adopted.manualFields.title).toBe(false)
  })
})
