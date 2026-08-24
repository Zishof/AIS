package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.util.Date; import org.json.JSONArray; import org.json.JSONObject;
import ais.common.Common; import ais.database.model.Konfigurasi; import ais.database.model.VirtualAccountBank;

/** Dedicated Smartlink adapter; no academic Kegiatan/Tagihan is fabricated. */
public final class SocialSmartlinkAdapter {
 public JSONObject createOrder(String orderId,BigDecimal total,String description,String name,String email,String phone,Date expiry){
  String url=config("sosial_smartlink_create_order_url",config("gateway_url_va_e_smartlink",null));String username=config("sosial_smartlink_username",null);String password=config("sosial_smartlink_password",null);
  if(empty(url)||empty(username)||empty(password))throw new IllegalStateException("Kredensial/URL Smartlink Sosial belum dikonfigurasi.");
  try{JSONObject p=new JSONObject();p.put("order_id",orderId).put("amount",total.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString()).put("description",description);
   JSONObject customer=new JSONObject();customer.put("name",safe(name,120)).put("email",safe(email,320)).put("phone",safe(phone,40));p.put("customer",customer);
   JSONArray items=new JSONArray();items.put(new JSONObject().put("name",safe(description,255)).put("amount",total.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString()).put("qty",1));p.put("item",items);
   JSONArray channels=new JSONArray();String csv=config("sosial_smartlink_channels","VA_CIMB,VA_BRI");for(String c:csv.split(","))if(!c.trim().isEmpty())channels.put(c.trim());p.put("channel",channels);
   String host=Common.getRequestHostWithProtocol();p.put("type","payment-page").put("payment_mode","CLOSE").put("expired_time",Common.iso8601.get().format(expiry)).put("callback_url",host+"/SosialSmartlinkCallback").put("success_redirect_url",host+"/sosial/pembayaran/"+orderId).put("failed_redirect_url",host+"/sosial/pembayaran/"+orderId);
   String raw=VirtualAccountBank.curlSmartlink(url,username,password,p);JSONObject out=new JSONObject(raw);if(!"0".equals(String.valueOf(out.opt("code"))))throw new IllegalStateException("Smartlink menolak order: "+out.optString("message","tanpa keterangan"));return out;
  }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Gagal membuat order Smartlink.",e);}
 }
 private String config(String k,String d){try{return Common.getKonfigurasi(k,d==null?"":d).getNilai();}catch(Exception e){return d;}}
 private boolean empty(String v){return v==null||v.trim().isEmpty();} private String safe(String v,int max){if(v==null)return "";v=v.replaceAll("[^\\p{L}\\p{N} @._+\\-]"," ").trim();return v.length()>max?v.substring(0,max):v;}
}
