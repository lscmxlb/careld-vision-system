<template>
  <img
    class="store-logo"
    :src="logo"
    :style="{ width: size + 'px', height: size + 'px' }"
    alt=""
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import wsLogo from '@/assets/logos/ws.png'
import fyLogo from '@/assets/logos/fy.png'
import yyyLogo from '@/assets/logos/yyy.png'

const props = withDefaults(defineProps<{ storeType?: number; size?: number }>(), {
  storeType: 1,
  size: 20
})

// 1=社区卫生服务中心 2=卫生院 → ws；3=妇幼保健院 → fy；4=医院 5=其它 → yyy
const logo = computed(() => {
  if (props.storeType === 3) return fyLogo
  if (props.storeType === 4 || props.storeType === 5) return yyyLogo
  return wsLogo
})
</script>

<style scoped>
.store-logo {
  display: inline-block;
  vertical-align: -4px;
  margin-right: 6px;
  object-fit: contain;
}
</style>
