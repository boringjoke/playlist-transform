<script setup lang="ts">
useHead({
  title: '复制导出 - 歌单整理器',
})
</script>

<template>
  <div class="page-canvas">
    <section class="surface-card">
      <WorkflowHeader active-step="export" />

      <main class="stage-page">
        <p class="section-kicker">STEP 03 / EXPORT</p>
        <h1>复制导出</h1>
        <p class="stage-lede">
          这里将根据用户确认后的歌曲顺序生成文本预览、复制内容和 UTF-8 TXT 文件。导出器将在阶段 4 接入。
        </p>

        <div class="stage-notice" role="status">
          <span class="stage-notice-mark" aria-hidden="true">03</span>
          <div>
            <strong>平台边界已确定</strong>
            <p>网易云音乐第一版只生成标准化文本，不提前承诺官方文本导入能力。</p>
          </div>
        </div>

        <NuxtLink class="button button-primary" to="/">返回输入页 <span class="arrow-icon" aria-hidden="true">←</span></NuxtLink>
      </main>
    </section>
  </div>
</template>
