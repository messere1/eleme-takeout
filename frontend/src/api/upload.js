import { http } from './http'
export function uploadImage(file,targetType,targetId){const data=new FormData();data.append('file',file);return http.post('/images',data,{params:{targetType,targetId}})}
