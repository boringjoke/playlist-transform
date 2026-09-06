<script setup lang="ts">
import type { ToastTone } from '~/composables/useToast'

defineProps<{
  message: string
  tone: ToastTone
}>()
</script>

<template>
  <Transition name="app-toast">
    <div
      v-if="message"
      class="app-toast"
      :class="`app-toast-${tone}`"
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      <span class="app-toast-dot" aria-hidden="true" />
      <span>{{ message }}</span>
    </div>
  </Transition>
</template>
