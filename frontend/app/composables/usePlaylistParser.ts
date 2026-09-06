import { ref } from 'vue'
import type { ApiResponse, ParseTextResponse } from '~/types/playlist'

function getApiMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'data' in error) {
    const data = (error as { data?: unknown }).data
    if (typeof data === 'object' && data !== null && 'message' in data) {
      const message = (data as { message?: unknown }).message
      if (typeof message === 'string' && message.trim()) {
        return message
      }
    }
  }

  if (error instanceof Error && error.message.trim()) {
    const message = error.message.trim()
    if (!/^(fetch failed|failed to fetch|network error|load failed)$/i.test(message)) {
      return message
    }
  }

  return '无法连接解析服务，请稍后重试。'
}

export function usePlaylistParser() {
  const runtimeConfig = useRuntimeConfig()
  const isParsing = ref(false)

  const parseText = async (text: string): Promise<ParseTextResponse> => {
    isParsing.value = true
    const configuredBaseUrl = String(runtimeConfig.public.apiBaseUrl || '').replace(/\/+$/, '')
    const parseUrl = `${configuredBaseUrl}/api/playlist/parseText`

    try {
      const response = await $fetch<ApiResponse<ParseTextResponse>>(parseUrl, {
        method: 'POST',
        body: { text },
      })

      if (response.code !== 'SUCCESS' || response.data === null) {
        throw new Error(response.message || '解析失败，请稍后重试。')
      }

      return response.data
    } catch (error: unknown) {
      throw new Error(getApiMessage(error))
    } finally {
      isParsing.value = false
    }
  }

  return {
    isParsing,
    parseText,
  }
}
