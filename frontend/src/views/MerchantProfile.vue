<script setup>
import { onMounted, ref } from 'vue'
import { getMyMerchant, updateMyMerchant, listBusinessCategories, deleteMyMerchant } from '@/api/merchant'
import { uploadImage } from '@/api/upload'

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
async function pick(event,type){const file=event.target.files?.[0];if(!file)return;try{const r=await uploadImage(file,type,info.value.shopId);if(type==='SHOP_IMAGE')info.value.imageUrl=r.url;else info.value.coverImageUrl=r.url;message.value='图片已上传'}catch(e){message.value=e?.message||'上传失败'}}

onMounted(async () => {
  try {
    const [me,cats] = await Promise.all([getMyMerchant(),listBusinessCategories()]);categories.value=cats||[]
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
async function save(){try{const me=await updateMyMerchant({businessScope:info.value.businessScope,shopName:info.value.shopName,shopAddress:info.value.shopAddress});info.value={...info.value,...me};message.value='资料已保存'}catch(e){message.value=e?.message||'保存失败'}}
async function remove(){if(!window.confirm('确认注销商家账号？'))return;try{await deleteMyMerchant();location.href='/login'}catch(e){message.value=e?.message||'注销失败'}}
</script>

<template>
  <section class="profile-page">
    <h2>商家信息</h2>
    <dl class="info-list">
      <div><dt>商家名称</dt><dd>{{ info.merchantName || '（未填写）' }}</dd></div>
      <div><dt>手机号</dt><dd>{{ info.merchantPhone || '（未填写）' }}</dd></div>
      <div><dt>经营类别</dt><dd><input v-model="info.businessScope" list="profile-categories" /><datalist id="profile-categories"><option v-for="c in categories" :key="c.id" :value="c.name" /></datalist></dd></div>
      <div><dt>店铺名称</dt><dd><input v-model="info.shopName" /></dd></div>
      <div><dt>店铺地址</dt><dd><input v-model="info.shopAddress" /></dd></div>
      <div><dt>营业状态</dt><dd>{{ info.shopStatus || '（未填写）' }}</dd></div>
    </dl>
    <div class="uploads"><label>店铺照片 <input type="file" accept="image/jpeg,image/png,image/webp" @change="pick($event,'SHOP_IMAGE')" /></label>
      <label>封面照片 <input type="file" accept="image/jpeg,image/png,image/webp" @change="pick($event,'SHOP_COVER')" /></label></div>
    <p v-if="message">{{message}}</p>
    <button @click="save">保存资料</button><button class="danger" @click="remove">注销商家账号</button>
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
