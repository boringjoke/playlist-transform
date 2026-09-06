import { computed, ref } from 'vue'

type BackendHealthStatus = 'idle' | 'checking' | 'connected' | 'unavailable'

interface HealthResponse {
  code: string
  message: string
  data?: {
    service: string
    status: string
  } | null
}

export function useBackendHealth() {
  const runtimeConfig = useRuntimeConfig()
  const status = ref<BackendHealthStatus>('idle')

  const statusLabel = computed(() => {
    switch (status.value) {
      case 'checking':
        return '检查中'
      case 'connected':
        return '已连接'
      case 'unavailable':
        return '未连接'
      default:
        return '待检查'
    }
  })

  const check = async () => {
    status.value = 'checking'

    const configuredBaseUrl = String(runtimeConfig.public.apiBaseUrl || '').replace(/\/+$/, '')
    const healthUrl = `${configuredBaseUrl}/api/health`

    try {
      const response = await $fetch<HealthResponse>(healthUrl, {
        method: 'GET',
      })

      if (response.code !== 'SUCCESS' || response.data?.status !== 'UP') {
        throw new Error('后端健康检查未通过')
      }

      status.value = 'connected'
    } catch {
      status.value = 'unavailable'
    }
  }

  return {
    status,
    statusLabel,
    check,
  }
}
