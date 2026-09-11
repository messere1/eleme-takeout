import { http } from './http'
export const listAdminProducts=(params={page:1,size:20})=>http.get('/admin/products',{params})
export const listAdminOrders=(params={page:1,size:20})=>http.get('/admin/orders',{params})
export const setAdminStatus=(type,id,status)=>http.patch(`/admin/${type}/${id}/status`,{status})
export const listAdminRefunds=()=>http.get('/admin/refunds')
export const decideRefund=(id,status)=>http.patch(`/admin/refunds/${id}`,{status})
