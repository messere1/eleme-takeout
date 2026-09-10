package cn.edu.tju.takeout.admin;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.*;
import cn.edu.tju.takeout.merchant.*;
import cn.edu.tju.takeout.order.*;
import cn.edu.tju.takeout.product.*;
import cn.edu.tju.takeout.user.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserMapper users; private final MerchantMapper merchants; private final ProductMapper products;
    private final OrderMapper orders; private final AdminMapper admins;
    public AdminController(UserMapper users, MerchantMapper merchants, ProductMapper products,
            OrderMapper orders, AdminMapper admins) {
        this.users=users; this.merchants=merchants; this.products=products; this.orders=orders; this.admins=admins;
    }
    @GetMapping("/users") public ApiResponse<Map<String,Object>> users(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) { int off=offset(page,size);var safe=users.findPage(size,off).stream().map(u->Map.of("id",u.getId(),"username",u.getUsername(),"phone",mask(u.getPhone()),"nickname",u.getNickname(),"enabled",u.isEnabled())).toList();return ApiResponse.success(page(safe,page,size,users.countAll())); }
    @GetMapping("/merchants") public ApiResponse<Map<String,Object>> merchants(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) { int off=offset(page,size);var safe=merchants.findPage(size,off).stream().map(m->Map.of("id",m.getId(),"merchantName",m.getMerchantName(),"phone",mask(m.getPhone()),"businessScope",m.getBusinessScope(),"enabled",m.isEnabled())).toList();return ApiResponse.success(page(safe,page,size,merchants.countAll())); }
    @GetMapping("/products") public ApiResponse<Map<String,Object>> products(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) { return ApiResponse.success(page(products.findPageForAdmin(size, offset(page,size)),page,size,products.countForAdmin())); }
    @GetMapping("/orders") public ApiResponse<Map<String,Object>> orders(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) { return ApiResponse.success(page(orders.findPageForAdmin(size, offset(page,size)),page,size,orders.countAll())); }

    @PatchMapping("/users/{id}/status") public ApiResponse<Void> userStatus(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id, @Valid @RequestBody AdminStatusRequest r) {
        boolean enabled=parseEnabled(r.status()); if(users.setEnabled(id,enabled)==0) notFound(); admins.audit(p.userId(),"SET_STATUS","USER",id); return ApiResponse.success(null); }
    @PatchMapping("/merchants/{id}/status") public ApiResponse<Void> merchantStatus(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id, @Valid @RequestBody AdminStatusRequest r) {
        boolean enabled=parseEnabled(r.status()); if(merchants.setEnabled(id,enabled)==0) notFound(); admins.audit(p.userId(),"SET_STATUS","MERCHANT",id); return ApiResponse.success(null); }
    @PatchMapping("/products/{id}/status") public ApiResponse<Void> productStatus(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id, @Valid @RequestBody AdminStatusRequest r) {
        if(!Set.of("ON_SALE","OFF_SALE").contains(r.status()) || products.setStatusForAdmin(id,r.status())==0) notFound();
        admins.audit(p.userId(),"SET_STATUS","PRODUCT",id); return ApiResponse.success(null); }
    @PatchMapping("/orders/{id}/status") public ApiResponse<Void> orderStatus(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id, @Valid @RequestBody AdminStatusRequest r) {
        if(!Set.of("CREATED","ACCEPTED","DELIVERING","DELIVERED","COMPLETED","CANCELLED").contains(r.status())
                || orders.setStatusForAdmin(id,r.status())==0) notFound(); admins.audit(p.userId(),"SET_STATUS","ORDER",id); return ApiResponse.success(null); }

    private int offset(int page,int size){ if(page<1||size<1||size>100) throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","分页参数不合法"); return (page-1)*size; }
    private Map<String,Object> page(Object items,int page,int size,long total){ return Map.of("items",items,"page",page,"size",size,"total",total); }
    private boolean parseEnabled(String s){ if("ENABLED".equals(s))return true;if("DISABLED".equals(s))return false;throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","状态不合法"); }
    private void notFound(){ throw new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","资源不存在"); }
    private String mask(String p){return p==null?"":p.length()<7?"***":p.substring(0,3)+"****"+p.substring(p.length()-4);}
}
