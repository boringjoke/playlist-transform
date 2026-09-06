<script setup lang="ts">
import { computed } from 'vue'
import type { DraftTrack, EditableField } from '~/types/playlist'
import { artistInputValue, getStatusLabel, getWarningLabel } from '~/utils/playlistLogic'

const props = defineProps<{
  track: DraftTrack
  displayIndex: number
}>()

const emit = defineEmits<{
  update: [field: EditableField, value: string]
  remove: []
  editingStart: []
  editingEnd: []
}>()

const artistsText = computed(() => artistInputValue(props.track.artists))
const confidenceText = computed(() => `${Math.round(props.track.confidence * 100)}%`)

const handleInput = (field: EditableField, event: Event) => {
  emit('update', field, (event.target as HTMLInputElement).value)
}

const handleFocusOut = (event: FocusEvent) => {
  const currentTarget = event.currentTarget
  const relatedTarget = event.relatedTarget
  if (currentTarget instanceof HTMLElement && relatedTarget instanceof Node && currentTarget.contains(relatedTarget)) {
    return
  }

  emit('editingEnd')
}
</script>

<template>
  <article
    class="review-track-card"
    :class="`review-track-card-${track.status.toLowerCase()}`"
    @focusin="emit('editingStart')"
    @focusout="handleFocusOut"
  >
    <div class="review-track-topline">
      <div class="review-track-index" aria-label="当前显示序号">
        <span class="review-index-marker" aria-hidden="true">{{ String(displayIndex).padStart(2, '0') }}</span>
        <span>第 {{ displayIndex }} 首</span>
      </div>

      <span class="review-context-item review-context-raw" :title="track.rawText">
        <span class="review-context-label">原始文本</span>
        <span class="review-context-value">{{ track.rawText }}</span>
      </span>

      <span class="review-context-item review-context-confidence">
        <span class="review-context-label">原始解析置信度</span>
        <span class="review-context-value">{{ confidenceText }}</span>
      </span>

      <span
        v-if="track.manualFields.title || track.manualFields.artists || track.manualFields.version"
        class="review-context-item manual-edit-mark"
      >
        已手动修改
      </span>

      <ul v-if="track.warnings.length > 0" class="review-warning-list" aria-label="条目提示">
        <li v-for="warning in track.warnings" :key="warning">
          {{ getWarningLabel(warning) }}
        </li>
      </ul>

      <div class="review-track-status" :class="`review-status-${track.status.toLowerCase()}`" role="status">
        <span class="review-status-dot" aria-hidden="true" />
        {{ getStatusLabel(track.status) }}
      </div>
    </div>

    <div class="review-track-fields">
      <label class="review-field review-field-title" :for="`track-${track.clientId}-title`">
        <span>歌名</span>
        <input
          :id="`track-${track.clientId}-title`"
          :value="track.title"
          type="text"
          autocomplete="off"
          @input="handleInput('title', $event)"
        >
      </label>

      <label class="review-field" :for="`track-${track.clientId}-artists`">
        <span>歌手 <small>用“、”分隔</small></span>
        <input
          :id="`track-${track.clientId}-artists`"
          :value="artistsText"
          type="text"
          autocomplete="off"
          @input="handleInput('artists', $event)"
        >
      </label>

      <label class="review-field" :for="`track-${track.clientId}-version`">
        <span>版本 <small>可选</small></span>
        <input
          :id="`track-${track.clientId}-version`"
          :value="track.version ?? ''"
          type="text"
          autocomplete="off"
          placeholder="如 Live、Remix"
          @input="handleInput('version', $event)"
        >
      </label>
    </div>

    <div class="review-track-bottomline">
      <button class="review-delete-button" type="button" @click="emit('remove')">
        删除此条
      </button>
    </div>
  </article>
</template>
