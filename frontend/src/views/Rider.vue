<script setup>
import {onMounted,ref} from 'vue';import {claimOrder,deliverOrder,listRiderAvailable,listRiderOrders} from '@/api/order'
const available=ref([]),mine=ref([]),message=ref(''),loading=ref(true)
async function load(){loading.value=true;try{[available.value,mine.value]=await Promise.all([listRiderAvailable(),listRiderOrders()])}catch(e){message.value=e?.message||'加载失败'}finally{loading.value=false}}
// 领取/送达失败（被他人抢单、越权操作）时也刷新列表，避免界面停在过期状态。
async function claim(o){try{await claimOrder(o.id)}catch(e){message.value=e?.message||'接单失败'}finally{await load()}}
async function deliver(o){try{await deliverOrder(o.id)}catch(e){message.value=e?.message||'更新失败'}finally{await load()}}
onMounted(load)
</script>
<template><section class="rider"><h2>骑手配送台</h2><p v-if="loading">加载中…</p><p v-if="message" class="error">{{message}}</p>
<h3>可接订单</h3><p v-if="!loading&&!available.length" data-testid="rider-available-empty">暂无可接订单</p><div v-for="o in available" :key="o.id" class="card" :data-testid="`rider-available-${o.id}`"><span>{{o.orderNo}}</span><button :data-testid="`rider-claim-${o.id}`" @click="claim(o)">接单</button></div>
<h3>我的配送</h3><p v-if="!loading&&!mine.length" data-testid="rider-mine-empty">暂无配送任务</p><div v-for="o in mine" :key="o.id" class="card" :data-testid="`rider-mine-${o.id}`"><span>{{o.orderNo}} · {{o.status}}</span><button v-if="o.status==='DELIVERING'" :data-testid="`rider-deliver-${o.id}`" @click="deliver(o)">确认送达</button></div></section></template>
<style scoped>.rider{max-width:48rem;margin:auto}.card{background:#fff;margin:.6rem 0;padding:1rem;border-radius:12px;display:flex;justify-content:space-between}.card button{border:0;border-radius:999px;padding:.4rem 1rem;background:var(--brand-gradient)}.error{color:#e34d1c}</style>
