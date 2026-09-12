package cn.edu.tju.takeout.rider;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.order.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RiderService {
    private final OrderMapper orders;
    public RiderService(OrderMapper orders){this.orders=orders;}
    public List<RiderOrderView> available(){return orders.findReadyForDelivery().stream().map(RiderOrderView::available).toList();}
    public List<RiderOrderView> mine(Long riderId){return orders.findByRiderId(riderId).stream().map(RiderOrderView::assigned).toList();}
    @Transactional public RiderOrderView claim(Long riderId,Long orderId){if(orders.claimForDelivery(orderId,riderId)==0)throw conflict("订单已被领取、未支付或状态不可配送");return RiderOrderView.assigned(requireOrder(orderId));}
    @Transactional public RiderOrderView deliver(Long riderId,Long orderId){if(orders.markDelivered(orderId,riderId)==0)throw conflict("订单不属于当前骑手或状态不可送达");return RiderOrderView.assigned(requireOrder(orderId));}
    private Order requireOrder(Long id){return orders.findById(id).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","订单不存在"));}
    private BusinessException conflict(String message){return new BusinessException(HttpStatus.CONFLICT,"BUSINESS_CONFLICT",message);}
}
