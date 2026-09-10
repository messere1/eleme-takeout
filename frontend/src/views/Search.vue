<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listShops } from '@/api/shop'

const router = useRouter()

const keyword = ref('')
const results = ref([])
const searched = ref(false)
const searching = ref(false)
const message = ref('')

const HOT = ['煎饼果子', '奶茶', '汉堡', '拉面', '烧烤', '甜品']

async function search(text) {
  const q = (text ?? keyword.value).trim()
  keyword.value = q
  if (!q) {
    message.value = '请输入店铺或美食名称'
    return
  }
  message.value = ''
  searching.value = true
  try {
    // 后端整体搜索接口待提供；目前按店铺名本地过滤已有店铺列表
    const data = await listShops({ page: 1, size: 100 })
    const items = data?.items ?? []
    results.value = items.filter((shop) =>
      String(shop.shopName || '').toLowerCase().includes(q.toLowerCase()),
    )
    searched.value = true
  } catch (error) {
    results.value = []
    searched.value = true
    message.value = '搜索失败：' + (error?.message || '请稍后重试')
  } finally {
    searching.value = false
  }
}

function pickHot(word) {
  search(word)
}

onMounted(() => {
  keyword.value = String(router.currentRoute.value.query.q || '')
  if (keyword.value) search(keyword.value)
})
</script>

<template>
  <section class="search-page">
    <div class="search-bar">
      <button class="back" @click="router.back()">←</button>
      <input
        v-model="keyword"
        class="search-input"
        type="search"
        data-testid="search-input"
        placeholder="搜索店铺或美食"
        @keyup.enter="search()"
      />
      <button class="search-btn" data-testid="search-submit" @click="search()">搜索</button>
    </div>

    <p v-if="message" data-testid="search-message" class="search-message">{{ message }}</p>

    <template v-if="!searched">
      <h3 class="block-title">热门搜索</h3>
      <div class="hot-tags">
        <button
          v-for="word in HOT"
          :key="word"
          class="hot-tag"
          @click="pickHot(word)"
        >{{ word }}</button>
      </div>
    </template>

    <template v-else>
      <ul v-if="results.length" class="result-list">
        <li
          v-for="shop in results"
          :key="shop.id"
          :data-testid="`search-result-${shop.id}`"
          class="result-card"
          @click="router.push(`/shops/${shop.id}`)"
        >
          <div class="result-thumb">🏪</div>
          <div class="result-body">
            <strong>{{ shop.shopName }}</strong>
            <span class="result-notice">{{ shop.notice || '暂无公告' }}</span>
          </div>
          <span class="result-enter">进店 ›</span>
        </li>
      </ul>
      <p v-else data-testid="search-empty" class="search-empty">没有找到相关店铺</p>
    </template>
  </section>
</template>

<style scoped>
.search-page {
  max-width: 48rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.search-bar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #fff;
  border-radius: 999px;
  padding: 0.45rem 0.6rem 0.45rem 0.9rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}
.back {
  border: none;
  background: transparent;
  font-size: 1.1rem;
  cursor: pointer;
  color: #666;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.98rem;
  color: #333;
}
.search-btn {
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  font-weight: 600;
  border-radius: 999px;
  padding: 0.4rem 1rem;
  cursor: pointer;
}
.search-message {
  margin: 0;
  color: #e34d1c;
  font-size: 0.9rem;
}
.block-title {
  margin: 0;
  font-size: 0.95rem;
}
.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.hot-tag {
  border: 1px solid #ffe3cf;
  background: #fff7f0;
  color: #ff6a00;
  border-radius: 999px;
  padding: 0.3rem 0.85rem;
  cursor: pointer;
  font-size: 0.88rem;
}
.result-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.result-card {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 0.9rem 1rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}
.result-thumb {
  flex-shrink: 0;
  width: 3.2rem;
  height: 3.2rem;
  border-radius: 10px;
  background: #fff4ec;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.6rem;
}
.result-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.result-notice {
  color: #999;
  font-size: 0.85rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-enter {
  color: #ff5000;
  font-weight: 700;
}
.search-empty {
  text-align: center;
  color: #999;
  padding: 2rem 0;
}
</style>
