package cn.edu.tju.takeout.rider;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.order.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RiderService {
    private final OrderMapper orders;
    private final RiderMapper riders;
    private final PasswordEncoder passwordEncoder;
    public RiderService(OrderMapper orders){this(orders, null, null);}
    @Autowired public RiderService(OrderMapper orders, RiderMapper riders, PasswordEncoder passwordEncoder){this.orders=orders;this.riders=riders;this.passwordEncoder=passwordEncoder;}
    @Transactional public RiderView register(RiderRegistrationRequest request){
        String name=request.riderName().trim();
        if(riders.findByName(name).isPresent())throw alreadyExists("骑手名称已存在");
        if(riders.findByPhone(request.phone()).isPresent())throw alreadyExists("手机号已存在");
        Rider rider=Rider.registered(name,request.phone(),passwordEncoder.encode(request.password()));
        riders.insertRegistration(rider);return RiderView.from(rider);
    }
    public List<RiderOrderView> available(){return orders.findReadyForDelivery().stream().map(RiderOrderView::available).toList();}
    public List<RiderOrderView> mine(Long riderId){return orders.findByRiderId(riderId).stream().map(RiderOrderView::assigned).toList();}
    @Transactional public RiderOrderView claim(Long riderId,Long orderId){if(orders.claimForDelivery(orderId,riderId)==0)throw conflict("订单已被领取、未支付或状态不可配送");return RiderOrderView.assigned(requireOrder(orderId));}
    @Transactional public RiderOrderView deliver(Long riderId,Long orderId){if(orders.markDelivered(orderId,riderId)==0)throw conflict("订单不属于当前骑手或状态不可送达");return RiderOrderView.assigned(requireOrder(orderId));}
    private Order requireOrder(Long id){return orders.findById(id).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","订单不存在"));}
    private BusinessException conflict(String message){return new BusinessException(HttpStatus.CONFLICT,"BUSINESS_CONFLICT",message);}
    private BusinessException alreadyExists(String message){return new BusinessException(HttpStatus.CONFLICT,"RIDER_ALREADY_EXISTS",message);}
}
