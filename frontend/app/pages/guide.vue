<script setup lang="ts">
useHead({
  title: '使用说明 - 歌单整理器',
})
</script>

<template>
  <div class="page-canvas">
    <section class="surface-card">
      <WorkflowHeader active-step="input" />

      <main class="content-page">
        <p class="section-kicker">GUIDE / WORKFLOW</p>
        <h1>使用说明</h1>
        <p class="content-lede">把文字清单整理成更容易继续处理的结构化歌单文本。</p>

        <ol class="guide-list">
          <li>
            <span class="guide-number">01</span>
            <div><strong>粘贴歌曲清单</strong><p>支持带序号、歌手缺失或使用横线、Tab、多个空格分隔的文本。</p></div>
          </li>
          <li>
            <span class="guide-number">02</span>
            <div><strong>检查和修正</strong><p>检查页会标出信息不完整、存在歧义和疑似重复的条目，你可以逐条编辑或删除。</p></div>
          </li>
          <li>
            <span class="guide-number">03</span>
            <div><strong>复制或下载</strong><p>导出能力将在下一阶段按目标平台生成标准化文本；网易云第一版不宣传官方文本导入。</p></div>
          </li>
        </ol>

        <NuxtLink class="text-link" to="/">返回首页 <span aria-hidden="true">→</span></NuxtLink>
      </main>
    </section>
  </div>
</template>
