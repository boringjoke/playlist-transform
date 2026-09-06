<script setup lang="ts">
const { toast } = useToast()
</script>

<template>
  <div class="app-root">
    <NuxtRouteAnnouncer />
    <NuxtPage />
    <AppToast v-if="toast" :message="toast.message" :tone="toast.tone" />
  </div>
</template>
