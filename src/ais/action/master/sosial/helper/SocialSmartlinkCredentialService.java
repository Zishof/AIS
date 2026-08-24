package ais.action.master.sosial.helper;

import java.math.BigDecimal;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import ais.common.Common;
import ais.database.model.sosial.JenisDanaSosial;
import ais.database.model.sosial.PembayaranDonasi;

/** Resolves a server-side Smartlink credential profile and never exposes its secrets. */
public final class SocialSmartlinkCredentialService {
 public static final class Credential {
  private final String code,url,username,password,channels,callbackSecret,allowedIps; private final BigDecimal fee;
  private Credential(String c,String u,String n,String p,String ch,String secret,String ips,BigDecimal f){code=c;url=u;username=n;password=p;channels=ch;callbackSecret=secret;allowedIps=ips;fee=f;}
  public String getCode(){return code;} public String getUrl(){return url;} public String getUsername(){return username;} public String getPassword(){return password;} public String getChannels(){return channels;} public String getCallbackSecret(){return callbackSecret;} public String getAllowedIps(){return allowedIps;} public BigDecimal getFee(){return fee;}
 }
 public String selectCode(SocialRequestContext context,JenisDanaSosial fund,String transactionNumber){
  String mapped=config("sosial_smartlink_fund_"+fund.getId()+"_profile",null);if(valid(mapped))return mapped;
  String tenantKey="sosial_smartlink_profiles_"+slug(context.getTenantKey());String csv=config(tenantKey,config("sosial_smartlink_profiles",null));if(csv==null||csv.trim().isEmpty())throw new IllegalStateException("Pool credential Smartlink tenant belum dikonfigurasi.");
  String[] raw=csv.split(",");java.util.List<String> codes=new java.util.ArrayList<String>();for(String code:raw)if(valid(code))codes.add(code.trim());if(codes.isEmpty())throw new IllegalStateException("Pool credential Smartlink tidak valid.");int index=(transactionNumber.hashCode()&0x7fffffff)%codes.size();return codes.get(index);
 }
 public Credential load(String code){if(!valid(code))throw new IllegalStateException("Credential Smartlink transaksi tidak valid.");String prefix="sosial_smartlink_profile_"+code+"_";String url=config(prefix+"url",null),username=config(prefix+"username",null),password=config(prefix+"password",null),channels=config(prefix+"channels","VA_CIMB,VA_BRI"),secret=config(prefix+"callback_secret",null),ips=config(prefix+"callback_allowed_ips",null);if(empty(url)||empty(username)||empty(password))throw new IllegalStateException("Profil credential Smartlink "+code+" belum lengkap.");return new Credential(code,url,username,password,channels,secret,ips,money(config(prefix+"fee",config("online_smartlink_biaya_administrasi","0"))));}
 public Credential forCallback(String raw){try{JSONObject data=new JSONObject(raw).getJSONObject("data");String order=data.getString("order_id");Session s=null;try{s=ais.database.hibernate.HibernateUtil.openSession();PembayaranDonasi p=(PembayaranDonasi)s.createCriteria(PembayaranDonasi.class).add(Restrictions.eq("gatewayOrderId",order)).setMaxResults(1).uniqueResult();if(p==null)throw new SecurityException("Order callback tidak dikenal.");return load(p.getSmartlinkCredentialCode());}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}}catch(SecurityException e){throw e;}catch(Exception e){throw new SecurityException("Callback tidak dapat dipetakan ke credential.");}}
 public void verifyCallback(String remoteAddress,String signature,String raw){Credential c=forCallback(raw);if(empty(c.getCallbackSecret())||c.getCallbackSecret().length()<16||empty(c.getAllowedIps()))throw new SecurityException("Keamanan callback profil belum lengkap.");boolean allowed=false;for(String ip:c.getAllowedIps().split(","))if(remoteAddress.equals(ip.trim()))allowed=true;if(!allowed)throw new SecurityException("Source callback ditolak.");if(signature==null||!SocialSecurity.constantEquals(SocialSecurity.hmacSha256(c.getCallbackSecret(),raw),signature.trim().toLowerCase()))throw new SecurityException("Signature callback ditolak.");}
 private String config(String key,String fallback){try{return Common.getKonfigurasi(key,fallback==null?"":fallback).getNilai();}catch(Exception e){return fallback;}}
 private BigDecimal money(String value){try{BigDecimal b=new BigDecimal(value.replace(",",""));return b.signum()<0?BigDecimal.ZERO:b.setScale(2,BigDecimal.ROUND_HALF_UP);}catch(Exception e){return BigDecimal.ZERO;}}
 private boolean valid(String code){return code!=null&&code.trim().matches("[A-Za-z0-9_-]{1,80}");} private boolean empty(String value){return value==null||value.trim().isEmpty();} private String slug(String value){return value==null?"default":value.replaceAll("[^A-Za-z0-9_-]","_");}
}
