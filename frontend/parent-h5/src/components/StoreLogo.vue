<template>
  <image
    class="store-logo"
    :src="logo"
    :style="{ width: size + 'px', height: size + 'px' }"
    mode="aspectFit"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ storeType?: number; size?: number }>(), {
  storeType: 1,
  size: 32
})

// 1=社区卫生服务中心 2=卫生院 → ws；3=妇幼保健院 → fy；4=医院 5=其它 → yyy
const logo = computed(() => {
  if (props.storeType === 3) return '/static/logos/fy.png'
  if (props.storeType === 4 || props.storeType === 5) return '/static/logos/yyy.png'
  return '/static/logos/ws.png'
})
</script>

<style scoped>
.store-logo {
  display: inline-block;
  vertical-align: middle;
  margin-right: 8px;
  flex-shrink: 0;
}
</style>
