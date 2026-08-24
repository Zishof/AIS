package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.util.Date; import org.json.JSONArray; import org.json.JSONObject;
import ais.common.Common; import ais.database.model.VirtualAccountBank;

/** Dedicated Smartlink adapter using the credential profile frozen on a donation. */
public final class SocialSmartlinkAdapter {
 public JSONObject createOrder(SocialSmartlinkCredentialService.Credential credential,String orderId,BigDecimal total,String description,String name,String email,String phone,Date expiry){
  if(credential==null)throw new IllegalStateException("Credential Smartlink transaksi tidak tersedia.");
  try{JSONObject p=new JSONObject();p.put("order_id",orderId).put("amount",total.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString()).put("description",description);
   JSONObject customer=new JSONObject();customer.put("name",safe(name,120)).put("email",safe(email,320)).put("phone",safe(phone,40));p.put("customer",customer);
   JSONArray items=new JSONArray();items.put(new JSONObject().put("name",safe(description,255)).put("amount",total.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString()).put("qty",1));p.put("item",items);
   JSONArray channels=new JSONArray();for(String channel:credential.getChannels().split(","))if(!channel.trim().isEmpty())channels.put(channel.trim());p.put("channel",channels);
   String host=Common.getRequestHostWithProtocol();p.put("type","payment-page").put("payment_mode","CLOSE").put("expired_time",Common.iso8601.get().format(expiry)).put("callback_url",host+"/SosialSmartlinkCallback").put("success_redirect_url",host+"/sosial/pembayaran/"+orderId).put("failed_redirect_url",host+"/sosial/pembayaran/"+orderId);
   String raw=VirtualAccountBank.curlSmartlink(credential.getUrl(),credential.getUsername(),credential.getPassword(),p);JSONObject out=new JSONObject(raw);if(!"0".equals(String.valueOf(out.opt("code"))))throw new IllegalStateException("Smartlink menolak order: "+out.optString("message","tanpa keterangan"));return out;
  }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Gagal membuat order Smartlink.",e);}
 }
 private String safe(String value,int max){if(value==null)return "";value=value.replaceAll("[^\\p{L}\\p{N} @._+\\-]"," ").trim();return value.length()>max?value.substring(0,max):value;}
}
