<script setup>
// 统一的图片上传控件。四个上传点（顾客头像、店铺照片、店铺封面、商品图）共用一套交互：
//   预览图 + 隐藏的原生 input + 样式化按钮
// 走 POST /images，服务端落盘并把 URL 关联到目标资源，因此这里只负责把返回的 url 回填预览，
// 页面不需要再额外保存一次。
//
// 之前各页面写法不一：店铺封面用 FileReader 读成 base64 只做本地预览、图片从来没上传；
// 商品图预览只在上传时赋值、刷新后不显示已保存的图；商家资料页直接把原生 input 内联在页面上，
// 窄屏放不下。这里统一掉。
import { ref } from 'vue'
import { uploadImage } from '@/api/upload'

const props = defineProps({
  modelValue: { type: String, default: '' },
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], default: null },
  /** square：方形缩略图；wide：横向缩略图；round：圆形（头像） */
  shape: { type: String, default: 'square' },
  title: { type: String, default: '' },
  tip: { type: String, default: '' },
  uploadLabel: { type: String, default: '上传图片' },
  replaceLabel: { type: String, default: '更换图片' },
  placeholder: { type: String, default: '未上传' },
  inputTestid: { type: String, default: '' },
  /** 与后端校验保持一致：仅 JPEG/PNG/WebP */
  accept: { type: String, default: 'image/jpeg,image/png,image/webp' },
  /** 内嵌在已有的卡片/列表行里时去掉外框和内边距，避免双层边框 */
  plain: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'message'])

const uploading = ref(false)

async function pick(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (props.targetId === null || props.targetId === undefined || props.targetId === '') {
    emit('message', '尚未确定上传目标，请稍后重试')
    event.target.value = ''
    return
  }
  uploading.value = true
  try {
    const result = await uploadImage(file, props.targetType, props.targetId)
    emit('update:modelValue', result.url)
    emit('message', '图片已上传')
  } catch (error) {
    emit('message', error?.message || '图片上传失败')
  } finally {
    uploading.value = false
    // 清空 input，否则再次选同一个文件不会触发 change
    event.target.value = ''
  }
}
</script>

<template>
  <div class="upload-row" :class="{ plain }">
    <img
      v-if="modelValue"
      :src="modelValue"
      class="upload-thumb"
      :class="shape"
      :alt="`${title || '图片'}预览`"
      :data-testid="inputTestid ? `${inputTestid}-preview` : undefined"
    />
    <div v-else class="upload-thumb placeholder" :class="shape">{{ placeholder }}</div>

    <div class="upload-meta">
      <span v-if="title" class="upload-title">{{ title }}</span>
      <span v-if="tip" class="upload-tip">{{ tip }}</span>
      <label class="upload-btn">
        {{ uploading ? '上传中…' : (modelValue ? replaceLabel : uploadLabel) }}
        <input
          type="file"
          class="file-input"
          :accept="accept"
          :disabled="uploading"
          :data-testid="inputTestid || undefined"
          @change="pick"
        />
      </label>
    </div>
  </div>
</template>

<style scoped>
.upload-row {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  /* min-width:0 让子项能被压缩，窄屏时才不会把父容器撑出横向滚动 */
  min-width: 0;
  padding: 0.6rem 0.7rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fff;
}
.upload-thumb {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  object-fit: cover;
  background: #f2f3f5;
  color: #999;
  border-radius: 10px;
  overflow: hidden;
}
/* 嵌在已有卡片里时不要外框，只保留「预览 + 按钮」 */
.upload-row.plain {
  border: none;
  padding: 0;
  background: transparent;
  gap: 0.5rem;
}
.upload-thumb.square { width: 56px; height: 56px; }
.upload-thumb.wide { width: 84px; height: 56px; }
.upload-thumb.round { width: 64px; height: 64px; border-radius: 50%; }
.upload-thumb.placeholder {
  font-size: 0.7rem;
  text-align: center;
  padding: 0 0.25rem;
  box-sizing: border-box;
}
.upload-meta {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1 1 auto;
  min-width: 0;
}
.upload-title { font-size: 0.9rem; color: #333; }
.upload-tip { font-size: 0.75rem; color: #aaa; }
.upload-btn {
  flex: 0 0 auto;
  align-self: flex-start;
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 0.85rem;
  white-space: nowrap;
}
/* 原生 file 控件隐藏，用户点的是上面那行文案 */
.file-input { display: none; }

/* 很窄的屏幕上按钮独占一行，避免和预览图挤在一起 */
@media (max-width: 420px) {
  .upload-row { flex-wrap: wrap; }
  .upload-btn { flex-basis: 100%; text-align: right; }
}
</style>
