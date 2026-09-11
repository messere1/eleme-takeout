<script setup>
import { onMounted, ref } from 'vue'
import { listUsers } from '@/api/user'
import { listMerchants } from '@/api/merchant'
import { listAdminProducts, listAdminOrders, setAdminStatus, listAdminRefunds, decideRefund } from '@/api/admin'

const props = defineProps({ initialTab: { type: String, default: 'users' } })
const tab=ref(props.initialTab),rows=ref([]),error=ref(''),loading=ref(false)
const loaders={users:listUsers,merchants:listMerchants,products:listAdminProducts,orders:listAdminOrders,refunds:listAdminRefunds}
const mask=v=>!v?'':v.length>=7?`${v.slice(0,3)}****${v.slice(-4)}`:'***'
async function refresh(){error.value='';loading.value=true;try{const data=await loaders[tab.value]();rows.value=Array.isArray(data)?data:data?.items??[]}catch(e){error.value=e?.message||'管理数据加载失败';rows.value=[]}finally{loading.value=false}}
async function toggle(row,type){try{await setAdminStatus(type,row.id,row.enabled===false?'ENABLED':'DISABLED');row.enabled=!row.enabled}catch(e){error.value=e?.message||'操作失败'}}
async function decide(row,status){try{const r=await decideRefund(row.id,status);row.status=r.status}catch(e){error.value=e?.message||'处理失败'}}
onMounted(refresh)
</script>

<template>
  <section class="admin">
    <h2>系统管理</h2>
    <p class="admin-tip">管理员独立工作区 · 写操作记录审计日志</p>
    <nav class="tabs">
      <button v-for="item in [{k:'users',t:'用户'},{k:'merchants',t:'商家'},{k:'products',t:'商品'},{k:'orders',t:'订单'},{k:'refunds',t:'退款'}]" :key="item.k" class="tab" :class="{active:tab===item.k}" @click="tab=item.k;refresh()">{{item.t}}</button>
    </nav>
    <p v-if="loading" data-testid="admin-loading">加载中…</p>
    <p v-if="error" data-testid="admin-error" class="admin-error">{{ error }}</p>
    <p v-if="!loading&&!error&&!rows.length" data-testid="admin-empty">暂无数据</p>
    <table v-if="tab === 'users' && rows.length" class="admin-table">
      <thead><tr><th>ID</th><th>用户名</th><th>手机号</th><th>昵称</th></tr></thead>
      <tbody>
        <tr v-for="u in rows" :key="u.id"><td>{{u.id}}</td><td>{{u.username}}</td><td>{{mask(u.phone)}}</td><td>{{u.nickname}}</td><td><button @click="toggle(u,'users')">{{u.enabled===false?'启用':'禁用'}}</button></td></tr>
      </tbody>
    </table>
    <table v-else-if="tab === 'merchants' && rows.length" class="admin-table">
      <thead><tr><th>ID</th><th>商家名称</th><th>手机号</th><th>经营范围</th></tr></thead>
      <tbody>
        <tr v-for="m in rows" :key="m.id"><td>{{m.id}}</td><td>{{m.merchantName}}</td><td>{{mask(m.phone)}}</td><td>{{m.businessScope}}</td><td><button @click="toggle(m,'merchants')">{{m.enabled===false?'启用':'禁用'}}</button></td></tr>
      </tbody>
    </table>
    <table v-else-if="tab==='products'&&rows.length" class="admin-table"><tbody><tr v-for="p in rows" :key="p.id"><td>{{p.id}}</td><td>{{p.name}}</td><td>{{p.status}}</td><td><button @click="setAdminStatus('products',p.id,p.status==='ON_SALE'?'OFF_SALE':'ON_SALE').then(refresh)">切换上下架</button></td></tr></tbody></table>
    <table v-else-if="tab==='orders'&&rows.length" class="admin-table"><tbody><tr v-for="o in rows" :key="o.id"><td>{{o.orderNo}}</td><td>{{o.status}}</td><td>{{o.paymentStatus}}</td></tr></tbody></table>
    <table v-else-if="tab==='refunds'&&rows.length" class="admin-table"><tbody><tr v-for="r in rows" :key="r.id"><td>{{r.orderId}}</td><td>{{r.amount}}</td><td>{{r.reason}}</td><td>{{r.status}}</td><td><button v-if="r.status==='PENDING'" @click="decide(r,'APPROVED')">批准</button><button v-if="r.status==='PENDING'" @click="decide(r,'REJECTED')">拒绝</button></td></tr></tbody></table>
  </section>
</template>

<style scoped>
.admin {
  max-width: 60rem;
  margin: 0 auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.admin h2 { margin: 0; }
.admin-tip { color: #b0b0b0; font-size: 0.85rem; }
.tabs { display: flex; gap: 0.5rem; margin: 1rem 0; }
.tab { border: 1px solid #eee; background: #fff; padding: 0.4rem 1.2rem; border-radius: 999px; cursor: pointer; }
.tab.active { background: var(--brand-gradient); color: #fff; border-color: transparent; }
.admin-error { color: #e34d1c; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { border: 1px solid #eee; padding: 0.5rem 0.75rem; text-align: left; }
.admin-table th { background: #fafafa; }
</style>
