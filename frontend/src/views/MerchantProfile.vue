<script setup>
import { onMounted, ref } from 'vue'
import { getMyMerchant, updateMyMerchant, listBusinessCategories, deleteMyMerchant, getMyShopBusinessCategories, updateMyShopBusinessCategories } from '@/api/merchant'
import ImageUploader from '@/components/ImageUploader.vue'

const info = ref({
  merchantName: '',
  merchantPhone: '',
  businessScope: '',
  shopName: '',
  shopStatus: '',
  shopId: null,
  imageUrl: '', coverImageUrl: '',
})
const message=ref('')
const categories=ref([])
const selectedCategoryIds=ref([])

onMounted(async () => {
  try {
    const [me,cats,selected] = await Promise.all([getMyMerchant(),listBusinessCategories(),getMyShopBusinessCategories()]);categories.value=cats||[];selectedCategoryIds.value=(selected||[]).map(c=>c.id)
    info.value = {
      merchantName: me?.merchantName || '',
      merchantPhone: me?.phone || '',
      businessScope: me?.businessScope || '',
      shopName: me?.shopName || '',
      shopStatus: me?.shopStatus || '',
      shopAddress: me?.shopAddress || '',
      shopId: me?.shopId,
      imageUrl: me?.imageUrl || '', coverImageUrl: me?.coverImageUrl || '',
    }
  } catch {
    // 后端不可用时留空显示
  }
})
async function save(){if(selectedCategoryIds.value.length<1||selectedCategoryIds.value.length>3){message.value='请选择一至三个经营品类';return}try{const [me]=await Promise.all([updateMyMerchant({businessScope:info.value.businessScope||'多品类',shopName:info.value.shopName,shopAddress:info.value.shopAddress}),updateMyShopBusinessCategories(selectedCategoryIds.value)]);info.value={...info.value,...me};message.value='资料已保存'}catch(e){message.value=e?.message||'保存失败'}}
async function remove(){if(!window.confirm('确认注销商家账号？'))return;try{await deleteMyMerchant();location.href='/login'}catch(e){message.value=e?.message||'注销失败'}}
</script>

<template>
  <section class="profile-page">
    <h2>商家信息</h2>
    <dl class="info-list">
      <div><dt>商家名称</dt><dd>{{ info.merchantName || '（未填写）' }}</dd></div>
      <div><dt>手机号</dt><dd>{{ info.merchantPhone || '（未填写）' }}</dd></div>
      <div><dt>经营类别</dt><dd><el-select v-model="selectedCategoryIds" multiple collapse-tags :max-collapse-tags="3" placeholder="请选择1～3项"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" /></el-select></dd></div>
      <div><dt>店铺名称</dt><dd><input v-model="info.shopName" /></dd></div>
      <div><dt>店铺地址</dt><dd><input v-model="info.shopAddress" /></dd></div>
      <div><dt>营业状态</dt><dd>{{ info.shopStatus || '（未填写）' }}</dd></div>
    </dl>

    <section class="uploads">
      <ImageUploader
        v-model="info.imageUrl"
        target-type="SHOP_IMAGE"
        :target-id="info.shopId"
        shape="square"
        title="店铺照片"
        tip="店铺列表和点单页使用的小图，建议正方形"
        placeholder="店铺照片"
        upload-label="上传照片"
        replace-label="更换照片"
        input-testid="profile-shop-image-input"
        @message="message = $event"
      />
      <ImageUploader
        v-model="info.coverImageUrl"
        target-type="SHOP_COVER"
        :target-id="info.shopId"
        shape="wide"
        title="封面照片"
        tip="顾客进店时店铺顶部的大图，建议横向"
        placeholder="封面照片"
        upload-label="上传封面"
        replace-label="更换封面"
        input-testid="profile-cover-image-input"
        @message="message = $event"
      />
    </section>

    <p v-if="message">{{message}}</p>

    <div class="actions">
      <button @click="save">保存资料</button>
      <button class="danger" @click="remove">注销商家账号</button>
    </div>
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
  align-items: center;
  gap: 0.75rem;
  /* 窄屏放不下时整行换行，而不是把输入框挤出屏幕 */
  flex-wrap: wrap;
  padding: 0.6rem 0;
  border-bottom: 1px dashed #f0f0f0;
}
.info-list dt { color: #999; flex: 0 0 auto; }
.info-list dd { margin: 0; flex: 1 1 10rem; min-width: 0; text-align: right; }
.info-list input {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 0.4rem 0.5rem;
  font: inherit;
  color: inherit;
}

/* 上传控件本身的样式在 components/ImageUploader.vue 里，四个页面共用 */
.uploads {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1.1rem;
}
.actions {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}
.actions button {
  border: 0;
  border-radius: 999px;
  padding: 0.5rem 1.2rem;
  background: var(--brand-gradient);
  color: #2b1d00;
  font-weight: 600;
  cursor: pointer;
}
.actions .danger {
  background: #fff1e8;
  color: #e34d1c;
}

/* 很窄的屏幕上按钮独占一行，避免和预览图挤在一起 */
@media (max-width: 420px) {
  .info-list dd { text-align: left; }
}
</style>
