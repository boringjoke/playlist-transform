<script setup lang="ts">
useHead({
  title: '隐私说明 - 歌单整理器',
})
</script>

<template>
  <div class="page-canvas">
    <section class="surface-card">
      <WorkflowHeader active-step="input" />

      <main class="content-page">
        <p class="section-kicker">PRIVACY / SCOPE</p>
        <h1>隐私说明</h1>
        <p class="content-lede">歌单整理器只面向用户主动提交的歌曲文字元数据，不是音乐平台账号工具。</p>

        <div class="content-grid">
          <article class="content-card">
            <h2>不会接收什么</h2>
            <p>本站不要求音乐平台账号、密码、Cookie、Token 或二维码登录态，也不提供任意 URL 抓取入口。</p>
          </article>
          <article class="content-card">
            <h2>不会处理什么</h2>
            <p>本站不播放、下载、代理音频，不抓取歌词、封面或播放地址，也不绕过会员、地区和版权限制。</p>
          </article>
          <article class="content-card">
            <h2>本地草稿</h2>
            <p>解析结果和校对内容只保存在当前浏览器的项目专用本地草稿中，不会上传到本站的持久化存储。你可以在首页或检查页主动清除。</p>
          </article>
        </div>

        <NuxtLink class="text-link" to="/">返回首页 <span aria-hidden="true">→</span></NuxtLink>
      </main>
    </section>
  </div>
</template>
