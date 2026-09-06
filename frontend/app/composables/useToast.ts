import { onBeforeUnmount } from 'vue'

export type ToastTone = 'info' | 'success' | 'error'

export interface ToastState {
  id: string
  message: string
  tone: ToastTone
}

const TOAST_STATE_KEY = 'playlist-transform-toast'
let toastSequence = 0

export const useToast = () => {
  const toast = useState<ToastState | null>(TOAST_STATE_KEY, () => null)
  let timer: ReturnType<typeof setTimeout> | null = null
  let ownedToastId: string | null = null

  const dismiss = () => {
    if (ownedToastId === null || toast.value?.id === ownedToastId) {
      toast.value = null
    }
  }

  const show = (message: string, tone: ToastTone = 'info', duration = 3200) => {
    const normalizedMessage = message.trim()
    if (!normalizedMessage) {
      return
    }

    if (timer) {
      clearTimeout(timer)
    }

    const id = `${Date.now()}-${toastSequence++}`
    ownedToastId = id
    toast.value = {
      id,
      message: normalizedMessage,
      tone,
    }

    if (duration <= 0) {
      timer = null
      return
    }

    timer = setTimeout(() => {
      if (toast.value?.id === id) {
        toast.value = null
      }

      if (ownedToastId === id) {
        ownedToastId = null
        timer = null
      }
    }, duration)
  }

  onBeforeUnmount(() => {
    if (timer) {
      clearTimeout(timer)
    }

    if (toast.value?.id === ownedToastId) {
      toast.value = null
    }
  })

  return {
    toast,
    show,
    dismiss,
  }
}
