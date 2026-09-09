<script setup>
import { onMounted, ref } from 'vue'
import { getMyMerchant } from '@/api/merchant'

const info = ref({
  merchantName: '',
  merchantPhone: '',
  businessScope: '',
  shopName: '',
  shopStatus: '',
})

onMounted(async () => {
  try {
    const me = await getMyMerchant()
    info.value = {
      merchantName: me?.merchantName || '',
      merchantPhone: me?.phone || '',
      businessScope: me?.businessScope || '',
      shopName: me?.shopName || '',
      shopStatus: me?.shopStatus || '',
    }
  } catch {
    // 后端不可用时留空显示
  }
})
</script>

<template>
  <section class="profile-page">
    <h2>商家信息</h2>
    <dl class="info-list">
      <div><dt>商家名称</dt><dd>{{ info.merchantName || '（未填写）' }}</dd></div>
      <div><dt>手机号</dt><dd>{{ info.merchantPhone || '（未填写）' }}</dd></div>
      <div><dt>经营类别</dt><dd>{{ info.businessScope || '（未填写）' }}</dd></div>
      <div><dt>店铺名称</dt><dd>{{ info.shopName || '（未填写）' }}</dd></div>
      <div><dt>营业状态</dt><dd>{{ info.shopStatus || '（未填写）' }}</dd></div>
    </dl>
  </section>
</template>

<style scoped>
.profile-page {
  max-width: 32rem;
  margin: 1.5rem auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.profile-page h2 { margin: 0 0 1rem; }
.info-list { margin: 0; }
.info-list div {
  display: flex;
  justify-content: space-between;
  padding: 0.6rem 0;
  border-bottom: 1px dashed #f0f0f0;
}
.info-list dt { color: #999; }
.info-list dd { margin: 0; }
</style>
