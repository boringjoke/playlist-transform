<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { PlaylistDraft, ReparseConflict, ReparsePlan } from '~/types/playlist'
import { usePlaylistDraft } from '~/composables/usePlaylistDraft'
import { usePlaylistParser } from '~/composables/usePlaylistParser'
import {
  createPlaylistDraft,
  createReparsePlan,
  MAX_PLAYLIST_TRACKS,
  mergeReparsePlan,
} from '~/utils/playlistLogic'

useHead({
  title: '歌单整理器',
})

const sampleText = '1、龙卷风 周杰伦\n2、夜曲\n3. 演员 - 薛之谦'
const sourceText = ref(sampleText)
const currentDraft = ref<PlaylistDraft | null>(null)
const notice = ref('')
const noticeTone = ref<'info' | 'success' | 'error'>('info')
const pendingReparse = ref<{
  sourceText: string
  parserRuleVersion: string
  plan: ReparsePlan
} | null>(null)
const adoptNewValues = ref<Record<string, boolean>>({})

const { status: healthStatus, statusLabel: healthStatusLabel, check: checkBackend } = useBackendHealth()
const { isParsing, parseText } = usePlaylistParser()
const { loadDraft, saveDraft, clearDraft } = usePlaylistDraft()
const route = useRoute()

const nonEmptyLineCount = computed(() => {
  return sourceText.value
    .split(/\r?\n/u)
    .filter((line) => line.trim().length > 0)
    .length
})

const isOverTrackLimit = computed(() => nonEmptyLineCount.value > MAX_PLAYLIST_TRACKS)
const canStartOrganizing = computed(() => {
  return nonEmptyLineCount.value > 0 && !isOverTrackLimit.value && !isParsing.value
})

const setNotice = (message: string, tone: 'info' | 'success' | 'error' = 'info') => {
  notice.value = message
  noticeTone.value = tone
}

const routeNotice = (value: unknown): string => {
  if (value === 'draft-missing') {
    return '没有找到可恢复的本地草稿，请先从首页开始整理。'
  }
  if (value === 'draft-invalid') {
    return '本地草稿无法读取，已安全清除，请重新粘贴歌曲清单。'
  }
  if (value === 'draft-cleared') {
    return '本地草稿已清除，可以开始新的整理。'
  }
  return ''
}

onMounted(() => {
  const loaded = loadDraft()
  if (loaded.status === 'valid') {
    currentDraft.value = loaded.draft
    sourceText.value = loaded.draft.sourceText
    setNotice('已恢复本地草稿，当前输入和校对结果仍在浏览器中。', 'success')
  } else if (loaded.status === 'invalid') {
    sourceText.value = ''
    setNotice('本地草稿无法读取，已安全清除，请重新粘贴歌曲清单。', 'error')
  } else {
    const message = routeNotice(route.query.notice)
    if (message) {
      setNotice(message, 'info')
    }
  }

  void checkBackend()
})

const clearInput = () => {
  sourceText.value = ''
  setNotice('已清空当前输入，可以粘贴新的歌曲清单。')
}

const clearCurrentDraft = () => {
  if (!window.confirm('确定清除本地草稿和当前输入吗？此操作无法恢复。')) {
    return
  }

  clearDraft()
  currentDraft.value = null
  pendingReparse.value = null
  sourceText.value = ''
  setNotice('本地草稿和当前输入已清除。', 'success')
}

const saveAndOpenReview = async (draft: PlaylistDraft, message?: string) => {
  currentDraft.value = saveDraft(draft)
  if (message) {
    setNotice(message, 'success')
  }
  await navigateTo('/review')
}

const startOrganizing = async () => {
  if (nonEmptyLineCount.value === 0) {
    setNotice('请先粘贴至少一行歌曲清单。', 'error')
    return
  }

  if (isOverTrackLimit.value) {
    setNotice(`单次最多整理 ${MAX_PLAYLIST_TRACKS} 个非空条目，请删减后再试。`, 'error')
    return
  }

  if (currentDraft.value && currentDraft.value.sourceText === sourceText.value) {
    await navigateTo('/review')
    return
  }

  try {
    const response = await parseText(sourceText.value)
    const existingDraft = currentDraft.value

    if (!existingDraft || existingDraft.tracks.length === 0) {
      await saveAndOpenReview(createPlaylistDraft(sourceText.value, response.parserRuleVersion, response.tracks))
      return
    }

    const plan = createReparsePlan(existingDraft.tracks, response.tracks)
    if (plan.kind !== 'merge') {
      pendingReparse.value = {
        sourceText: sourceText.value,
        parserRuleVersion: response.parserRuleVersion,
        plan,
      }
      adoptNewValues.value = {}
      return
    }

    await saveAndOpenReview({
      ...existingDraft,
      sourceText: sourceText.value,
      parserRuleVersion: response.parserRuleVersion,
      tracks: mergeReparsePlan(plan),
    })
  } catch (error: unknown) {
    setNotice(error instanceof Error ? error.message : '解析失败，请检查输入后重试。', 'error')
  }
}

const displayReparseValue = (value: string | string[] | null): string => {
  if (Array.isArray(value)) {
    return value.length > 0 ? value.join('、') : '（空）'
  }
  return value?.trim() || '（空）'
}

const reparseFieldLabel = (field: ReparseConflict['field']): string => {
  if (field === 'title') {
    return '歌名'
  }
  if (field === 'artists') {
    return '歌手'
  }
  return '版本'
}

const toggleConflictChoice = (key: string, event: Event) => {
  adoptNewValues.value = {
    ...adoptNewValues.value,
    [key]: (event.target as HTMLInputElement).checked,
  }
}

const preserveCurrentDraft = () => {
  sourceText.value = currentDraft.value?.sourceText ?? sourceText.value
  pendingReparse.value = null
  adoptNewValues.value = {}
  setNotice('已保留当前草稿，手工修改和当前顺序没有变化。', 'success')
}

const cancelReparse = () => {
  pendingReparse.value = null
  adoptNewValues.value = {}
}

const useNewParseResult = async () => {
  const pending = pendingReparse.value
  const existingDraft = currentDraft.value
  if (!pending || !existingDraft) {
    return
  }

  await saveAndOpenReview({
    ...existingDraft,
    sourceText: pending.sourceText,
    parserRuleVersion: pending.parserRuleVersion,
    tracks: mergeReparsePlan(pending.plan, adoptNewValues.value),
  }, '已根据你的选择更新解析结果。')
  pendingReparse.value = null
  adoptNewValues.value = {}
}
</script>

<template>
  <div class="page-canvas">
    <section class="surface-card home-card">
      <WorkflowHeader active-step="input" />

      <main class="home-main">
        <section class="hero-block" aria-labelledby="page-title">
          <p class="eyebrow">TEXT PLAYLIST WORKBENCH</p>
          <h1 id="page-title">把凌乱的歌曲清单，整理成可导入的歌单。</h1>
          <p class="hero-assurance">
            <span class="shield-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" role="presentation">
                <path d="M12 3.2 19 6v5.3c0 4.5-2.8 7.9-7 9.5-4.2-1.6-7-5-7-9.5V6l7-2.8Z" />
                <path d="m8.7 12.1 2.2 2.2 4.6-4.7" />
              </svg>
            </span>
            不登录音乐账号 · 不上传歌单 · 不处理音频
          </p>
        </section>

        <section class="input-section" aria-labelledby="input-title">
          <div class="section-heading">
            <div>
              <p class="section-kicker">STEP 01 / SOURCE TEXT</p>
              <h2 id="input-title">输入清单</h2>
            </div>
            <p class="section-hint">支持歌名、歌手和序号混合格式</p>
          </div>

          <div v-if="currentDraft" class="draft-recovery-bar" role="status">
            <span class="draft-recovery-mark" aria-hidden="true">✓</span>
            <span>已恢复本地草稿，修改原始文本后重新开始会先显示冲突确认。</span>
            <button class="text-button" type="button" @click="clearCurrentDraft">清除草稿</button>
          </div>

          <label class="sr-only" for="playlist-source">歌曲清单文本</label>
          <textarea
            id="playlist-source"
            v-model="sourceText"
            class="playlist-textarea"
            rows="8"
            spellcheck="false"
            placeholder="把歌曲清单粘贴到这里……"
            aria-describedby="input-help"
          />

          <div class="input-toolbar">
            <div class="input-meta" id="input-help">
              <span class="line-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" role="presentation">
                  <rect x="5" y="3.5" width="14" height="17" rx="2" />
                  <path d="M8.5 8h7M8.5 12h7M8.5 16h4" />
                </svg>
              </span>
              <span>已输入 {{ nonEmptyLineCount }} 行</span>
              <span class="meta-divider" aria-hidden="true" />
              <span v-if="isOverTrackLimit" class="input-limit-warning">最多 {{ MAX_PLAYLIST_TRACKS }} 行</span>
              <span v-else>后端按规则解析字段</span>
            </div>

            <div class="input-actions">
              <button class="button button-secondary" type="button" :disabled="isParsing" @click="clearInput">
                <span class="trash-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" role="presentation">
                    <path d="M5 7h14M10 4h4M8 7v12h8V7M10.5 10.5v5M13.5 10.5v5" />
                  </svg>
                </span>
                清空输入
              </button>
              <button
                class="button button-primary"
                type="button"
                :disabled="!canStartOrganizing"
                @click="startOrganizing"
              >
                <span v-if="isParsing" class="button-spinner" aria-hidden="true" />
                {{ isParsing ? '解析中…' : '开始整理' }}
                <span v-if="!isParsing" class="arrow-icon" aria-hidden="true">→</span>
              </button>
            </div>
          </div>

          <p v-if="notice" class="inline-notice" :class="`inline-notice-${noticeTone}`" role="status">
            {{ notice }}
          </p>
        </section>

        <section class="preview-section" aria-labelledby="preview-title">
          <div class="preview-heading">
            <div>
              <p class="section-kicker">VISUAL PREVIEW</p>
              <h2 id="preview-title">解析结果预览</h2>
            </div>
            <span class="preview-caption">后端解析后将在检查页确认</span>
          </div>

          <div class="preview-table" role="table" aria-label="解析结果示例">
            <div class="preview-table-head" role="row">
              <span role="columnheader">顺序</span>
              <span role="columnheader">歌曲</span>
              <span role="columnheader">歌手</span>
              <span role="columnheader">状态</span>
            </div>

            <div class="preview-row" role="row">
              <span class="preview-order-mark" aria-label="第 1 首">01</span>
              <span class="track-title" role="cell">龙卷风</span>
              <span class="track-artist" role="cell">周杰伦</span>
              <span class="track-status track-status-success" role="cell">
                <span class="status-icon" aria-hidden="true">✓</span>
                已识别
              </span>
            </div>

            <div class="preview-row" role="row">
              <span class="preview-order-mark" aria-label="第 2 首">02</span>
              <span class="track-title" role="cell">夜曲</span>
              <span class="track-artist track-artist-muted" role="cell">—</span>
              <span class="track-status track-status-warning" role="cell">
                <span class="status-icon" aria-hidden="true">!</span>
                信息不完整
              </span>
            </div>
          </div>
        </section>
      </main>

      <footer class="home-footer">
        <span>本地优先 · 不接收音乐平台账号、Cookie 或 Token</span>
        <button
          class="integration-status"
          :class="`integration-status-${healthStatus}`"
          type="button"
          @click="checkBackend"
        >
          <span class="integration-dot" aria-hidden="true" />
          后端联调：{{ healthStatusLabel }}
        </button>
      </footer>
    </section>

    <div v-if="pendingReparse" class="modal-backdrop" @click.self="cancelReparse">
      <section class="reparse-dialog" role="dialog" aria-modal="true" aria-labelledby="reparse-title">
        <div class="dialog-kicker">SOURCE TEXT UPDATED</div>
        <h2 id="reparse-title">
          {{ pendingReparse.plan.kind === 'structure' ? '原始文本结构已变化' : '发现手工修改冲突' }}
        </h2>

        <p v-if="pendingReparse.plan.kind === 'structure'" class="dialog-lede">
          新文本增加、删除或改变了原始位置，系统不会自动猜测歌曲对应关系。请选择是否直接使用新的解析结果。
        </p>
        <p v-else class="dialog-lede">
          以下字段已经被你手动修改。默认保留当前值，勾选后才会采用本次重新解析的值。
        </p>

        <div v-if="pendingReparse.plan.kind === 'structure'" class="structure-change-box">
          <div><strong>新增位置</strong><span>{{ pendingReparse.plan.unmatchedCandidate.length }} 条</span></div>
          <div><strong>无法对照的旧条目</strong><span>{{ pendingReparse.plan.unmatchedCurrent.length }} 条</span></div>
        </div>

        <div v-else class="reparse-conflict-list">
          <article v-for="conflict in pendingReparse.plan.conflicts" :key="conflict.key" class="reparse-conflict-item">
            <div class="conflict-item-heading">
              <strong>第 {{ conflict.sourceIndex }} 行 · {{ reparseFieldLabel(conflict.field) }}</strong>
              <label class="conflict-choice">
                <input
                  type="checkbox"
                  :checked="Boolean(adoptNewValues[conflict.key])"
                  @change="toggleConflictChoice(conflict.key, $event)"
                >
                采用新解析值
              </label>
            </div>
            <div class="conflict-values">
              <p><span>当前手工值</span><strong>{{ displayReparseValue(conflict.currentValue) }}</strong></p>
              <p><span>新解析值</span><strong>{{ displayReparseValue(conflict.candidateValue) }}</strong></p>
            </div>
          </article>
        </div>

        <div class="dialog-actions">
          <button class="button button-secondary" type="button" @click="preserveCurrentDraft">
            保留当前草稿
          </button>
          <button class="button button-quiet" type="button" @click="cancelReparse">取消</button>
          <button class="button button-primary" type="button" @click="useNewParseResult">
            {{ pendingReparse.plan.kind === 'structure' ? '使用新解析结果' : '合并并继续' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
