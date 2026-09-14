package cn.edu.tju.takeout.rider;
public record RiderView(Long id,String riderName,String phone,boolean enabled){
    static RiderView from(Rider rider){return new RiderView(rider.getId(),rider.getRiderName(),rider.getPhone(),rider.isEnabled());}
}
