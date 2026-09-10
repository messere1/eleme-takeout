package cn.edu.tju.takeout.common;
import com.fasterxml.jackson.core.*;import com.fasterxml.jackson.databind.*;import java.io.IOException;import java.math.BigDecimal;
public class StrictMoneyDeserializer extends JsonDeserializer<BigDecimal>{
 @Override public BigDecimal deserialize(JsonParser p,DeserializationContext c)throws IOException{
  String raw=p.getText();if(!raw.matches("(?:0|[1-9]\\d{0,7})(?:\\.\\d{1,2})?"))throw JsonMappingException.from(p,"金额必须是普通十进制且最多两位小数");
  return new BigDecimal(raw);
 }
}
