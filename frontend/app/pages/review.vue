<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { DraftTrack, EditableField, PlaylistDraft } from '~/types/playlist'
import { usePlaylistDraft } from '~/composables/usePlaylistDraft'
import ReviewTrackCard from '~/components/ReviewTrackCard.vue'
import {
  cleanEditedField,
  getParseSummary,
  getStatusLabel,
  MAX_PLAYLIST_TRACKS,
  recomputeTracks,
  removeDuplicateTracks,
  splitArtistInput,
} from '~/utils/playlistLogic'

useHead({
  title: '检查歌曲 - 歌单整理器',
})

const draft = ref<PlaylistDraft | null>(null)
const tracks = ref<DraftTrack[]>([])
const filter = ref<'ALL' | 'PROBLEMS'>('ALL')
const editingClientId = ref<string | null>(null)
const isLoading = ref(true)

const { loadDraft, saveDraft, clearDraft } = usePlaylistDraft()
const { show: showToast } = useToast()

const summary = computed(() => getParseSummary(tracks.value))
const problemCount = computed(() => {
  return summary.value.incomplete + summary.value.ambiguous + summary.value.duplicate + summary.value.invalid
})
const visibleTracks = computed(() => {
  return tracks.value
    .map((track, index) => ({ track, displayIndex: index + 1 }))
    .filter(({ track }) => {
      if (filter.value === 'ALL' || track.status !== 'PARSED') {
        return true
      }

      return track.clientId === editingClientId.value
    })
})

const saveCurrentDraft = (nextTracks: DraftTrack[], message?: string) => {
  if (!draft.value) {
    return
  }

  draft.value = saveDraft({
    ...draft.value,
    tracks: nextTracks,
  })
  tracks.value = draft.value.tracks
  if (message) {
    showToast(message, 'success')
  }
}

onMounted(() => {
  const loaded = loadDraft()
  if (loaded.status === 'valid') {
    draft.value = loaded.draft
    tracks.value = loaded.draft.tracks
    isLoading.value = false
    return
  }

  const noticeCode = loaded.status === 'invalid' ? 'draft-invalid' : 'draft-missing'
  void navigateTo({ path: '/', query: { notice: noticeCode } })
})

const updateField = (clientId: string, field: EditableField, value: string) => {
  const nextTracks = tracks.value.map((track) => {
    if (track.clientId !== clientId) {
      return track
    }

    const nextManualFields = {
      ...track.manualFields,
      [field]: true,
    }

    if (field === 'artists') {
      return {
        ...track,
        artists: splitArtistInput(value),
        manualFields: nextManualFields,
      }
    }

    return {
      ...track,
      [field]: field === 'version' ? cleanEditedField(value) || null : cleanEditedField(value),
      manualFields: nextManualFields,
    }
  })

  saveCurrentDraft(recomputeTracks(nextTracks))
}

const removeTrack = (clientId: string) => {
  const track = tracks.value.find((item) => item.clientId === clientId)
  if (!track || !window.confirm(`确定删除“${track.title || '未命名歌曲'}”吗？`)) {
    return
  }

  editingClientId.value = null
  const nextTracks = recomputeTracks(tracks.value.filter((item) => item.clientId !== clientId))
  saveCurrentDraft(nextTracks, '已删除该条目，其他条目的原始顺序保持不变。')
}

const confirmRemoveDuplicates = () => {
  if (summary.value.duplicate === 0) {
    return
  }

  if (!window.confirm(`确定删除 ${summary.value.duplicate} 条疑似重复歌曲吗？每组将保留最早出现的一条。`)) {
    return
  }

  saveCurrentDraft(removeDuplicateTracks(tracks.value), '已删除后出现的重复条目，剩余顺序保持不变。')
}

const clearCurrentDraft = () => {
  if (!window.confirm('确定清除本地草稿吗？当前校对结果将无法恢复。')) {
    return
  }

  clearDraft()
  draft.value = null
  tracks.value = []
  editingClientId.value = null
  void navigateTo({ path: '/', query: { notice: 'draft-cleared' } })
}

const returnToSource = () => {
  void navigateTo('/')
}

const startEditing = (clientId: string) => {
  editingClientId.value = clientId
}

const endEditing = (clientId: string) => {
  if (editingClientId.value === clientId) {
    editingClientId.value = null
  }
}
</script>

<template>
  <div class="page-canvas">
    <section class="surface-card">
      <WorkflowHeader active-step="review" />

      <main v-if="!isLoading && draft" class="review-page">
        <div class="review-page-heading">
          <div>
            <p class="section-kicker">STEP 02 / REVIEW</p>
            <h1>检查歌曲</h1>
            <p class="stage-lede">按原始输入顺序逐条确认。编辑会立即重算当前状态，原始文本始终保留在每条记录上。</p>
          </div>
          <div class="review-rule-note">
            <span>解析规则</span>
            <strong>{{ draft.parserRuleVersion }}</strong>
          </div>
        </div>

        <section class="review-summary" aria-label="解析状态汇总">
          <div class="summary-item summary-total">
            <span>全部条目</span>
            <strong>{{ summary.total }}</strong>
          </div>
          <div class="summary-item summary-parsed">
            <span>{{ getStatusLabel('PARSED') }}</span>
            <strong>{{ summary.parsed }}</strong>
          </div>
          <div class="summary-item summary-incomplete">
            <span>{{ getStatusLabel('INCOMPLETE') }}</span>
            <strong>{{ summary.incomplete }}</strong>
          </div>
          <div class="summary-item summary-ambiguous">
            <span>{{ getStatusLabel('AMBIGUOUS') }}</span>
            <strong>{{ summary.ambiguous }}</strong>
          </div>
          <div class="summary-item summary-duplicate">
            <span>{{ getStatusLabel('DUPLICATE') }}</span>
            <strong>{{ summary.duplicate }}</strong>
          </div>
          <div v-if="summary.invalid > 0" class="summary-item summary-invalid">
            <span>{{ getStatusLabel('INVALID') }}</span>
            <strong>{{ summary.invalid }}</strong>
          </div>
        </section>

        <div class="review-toolbar">
          <div class="review-filters" role="group" aria-label="条目筛选">
            <button
              class="filter-button"
              :class="{ 'filter-button-active': filter === 'ALL' }"
              type="button"
              :aria-pressed="filter === 'ALL'"
              @click="filter = 'ALL'"
            >
              全部 {{ summary.total }}
            </button>
            <button
              class="filter-button"
              :class="{ 'filter-button-active': filter === 'PROBLEMS' }"
              type="button"
              :aria-pressed="filter === 'PROBLEMS'"
              @click="filter = 'PROBLEMS'"
            >
              只看问题 {{ problemCount }}
            </button>
          </div>

          <div class="review-actions">
            <button
              class="button button-secondary button-compact"
              type="button"
              :disabled="summary.duplicate === 0"
              @click="confirmRemoveDuplicates"
            >
              确认去重
            </button>
            <button class="text-button" type="button" @click="returnToSource">返回修改原始文本</button>
          </div>
        </div>

        <div v-if="tracks.length === 0" class="review-empty-state" role="status">
          <span class="empty-state-mark" aria-hidden="true">∅</span>
          <h2>当前没有歌曲条目</h2>
          <p>返回首页粘贴歌曲清单，重新开始整理。</p>
          <button class="button button-primary" type="button" @click="returnToSource">返回输入页</button>
        </div>

        <div v-else-if="visibleTracks.length === 0" class="review-empty-state" role="status">
          <span class="empty-state-mark" aria-hidden="true">✓</span>
          <h2>没有符合条件的问题条目</h2>
          <p>当前列表中的条目都已通过基础检查。</p>
          <button class="button button-secondary" type="button" @click="filter = 'ALL'">查看全部条目</button>
        </div>

        <section v-else class="review-track-list" aria-label="歌曲检查列表">
          <ReviewTrackCard
            v-for="item in visibleTracks"
            :key="item.track.clientId"
            :track="item.track"
            :display-index="item.displayIndex"
            @update="(field, value) => updateField(item.track.clientId, field, value)"
            @remove="removeTrack(item.track.clientId)"
            @editing-start="startEditing(item.track.clientId)"
            @editing-end="endEditing(item.track.clientId)"
          />
        </section>

        <div class="review-bottom-bar">
          <p>当前列表共 {{ summary.total }} 条，保持用户文本中的非空行顺序。导出功能将在下一阶段接入。</p>
          <button class="text-button text-button-danger" type="button" @click="clearCurrentDraft">清除本地草稿</button>
        </div>
      </main>

      <main v-else class="stage-page review-loading" aria-live="polite">
        <p class="section-kicker">STEP 02 / REVIEW</p>
        <h1>正在恢复检查列表</h1>
        <p class="stage-lede">正在读取浏览器本地草稿。</p>
      </main>
    </section>
  </div>
</template>
