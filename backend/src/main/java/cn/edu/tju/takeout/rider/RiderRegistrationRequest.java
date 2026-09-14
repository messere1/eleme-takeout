package cn.edu.tju.takeout.rider;
import jakarta.validation.constraints.*;
public record RiderRegistrationRequest(
        @NotBlank @Size(min=2,max=50) @Pattern(regexp="^[^\\p{Cntrl}]+$") String riderName,
        @NotBlank @Pattern(regexp="^1[3-9]\\d{9}$",message="手机号格式不正确") String phone,
        @NotBlank @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",message="密码必须为8到64位且同时包含字母和数字") String password){
    @Override public String toString(){return "RiderRegistrationRequest[riderName="+riderName+", phone=***, password=***]";}
}
