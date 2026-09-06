<script setup lang="ts">
import { computed } from 'vue'

type WorkflowStepKey = 'input' | 'review' | 'export'

const props = defineProps<{
  activeStep: WorkflowStepKey
}>()

const steps: ReadonlyArray<{ key: WorkflowStepKey; label: string }> = [
  { key: 'input', label: '输入清单' },
  { key: 'review', label: '检查歌曲' },
  { key: 'export', label: '复制导出' },
]

const activeIndex = computed(() => steps.findIndex((step) => step.key === props.activeStep))

const getStepState = (index: number) => {
  if (index < activeIndex.value) {
    return 'complete'
  }

  if (index === activeIndex.value) {
    return 'active'
  }

  return 'pending'
}
</script>

<template>
  <header class="workflow-header">
    <div class="header-brand-row">
      <NuxtLink class="brand" to="/" aria-label="返回歌单整理器首页">
        <span class="brand-name">歌单整理器</span>
      </NuxtLink>

      <nav class="utility-nav" aria-label="辅助导航">
        <NuxtLink class="utility-link" to="/guide">使用说明</NuxtLink>
        <NuxtLink class="utility-link" to="/privacy">隐私说明</NuxtLink>
      </nav>
    </div>

    <ol class="workflow-steps" aria-label="整理流程">
      <li
        v-for="(step, index) in steps"
        :key="step.key"
        class="workflow-step"
        :class="`workflow-step-${getStepState(index)}`"
      >
        <span class="step-node" aria-hidden="true">
          <span v-if="getStepState(index) === 'complete'">✓</span>
          <span v-else>{{ index + 1 }}</span>
        </span>
        <span class="step-label">{{ step.label }}</span>
      </li>
    </ol>
  </header>
</template>
