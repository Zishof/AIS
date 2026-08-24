package ais.action.master.sosial.helper;

import java.math.BigDecimal;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import ais.common.Common;
import ais.database.model.sosial.JenisDanaSosial;
import ais.database.model.sosial.PembayaranDonasi;
import ais.database.model.sosial.SosialChannel;

/** Reads credentials once from the SosialChannel master selected for the donation. */
public final class SocialSmartlinkCredentialService {
 public static final class Credential {
  private final Long channelId; private final String code,url,username,password,channels,callbackSecret,allowedIps; private final BigDecimal fee;
  private Credential(Long id,String c,String u,String n,String p,String ch,String secret,String ips,BigDecimal f){channelId=id;code=c;url=u;username=n;password=p;channels=ch;callbackSecret=secret;allowedIps=ips;fee=f;}
  public Long getChannelId(){return channelId;} public String getCode(){return code;} public String getUrl(){return url;} public String getUsername(){return username;} public String getPassword(){return password;} public String getChannels(){return channels;} public String getCallbackSecret(){return callbackSecret;} public String getAllowedIps(){return allowedIps;} public BigDecimal getFee(){return fee;}
 }
 public SosialChannel requireChannel(SocialRequestContext context,JenisDanaSosial fund){SosialChannel channel=fund==null?null:fund.getSosialChannel();if(channel==null||channel.getId()==null)throw new IllegalStateException("Jenis dana belum memiliki SosialChannel.");if(!context.getTenantKey().equals(channel.getTenantKey())||!channel.getAktif())throw new IllegalStateException("SosialChannel tidak aktif untuk tenant ini.");if(!"SMARTLINK".equalsIgnoreCase(channel.getProvider())||!channel.getSmartlinkEnabled())throw new IllegalStateException("Smartlink pada SosialChannel belum aktif.");return channel;}
 public Credential load(SosialChannel channel){if(channel==null||channel.getId()==null||!channel.getAktif()||!channel.getSmartlinkEnabled())throw new IllegalStateException("SosialChannel transaksi tidak aktif.");String url=channel.getSmartlinkUrl(),username=channel.getSmartlinkUsername(),password=decrypt(channel.getSmartlinkPasswordCiphertext()),secret=decrypt(channel.getSmartlinkCallbackSecretCiphertext()),channels=channel.getSmartlinkChannels();if(empty(url)||empty(username)||empty(password))throw new IllegalStateException("Credential Smartlink pada SosialChannel "+channel.getKode()+" belum lengkap.");return new Credential(channel.getId(),channel.getKode(),url,username,password,channels,secret,channel.getSmartlinkCallbackAllowedIps(),channel.getSmartlinkFee());}
 public Credential forCallback(String raw){try{String order=new JSONObject(raw).getJSONObject("data").getString("order_id");Session session=null;try{session=ais.database.hibernate.HibernateUtil.openSession();PembayaranDonasi payment=(PembayaranDonasi)session.createCriteria(PembayaranDonasi.class).add(Restrictions.eq("gatewayOrderId",order)).setMaxResults(1).uniqueResult();if(payment==null)throw new SecurityException("Order callback tidak dikenal.");SosialChannel channel=payment.getSosialChannel();if(channel!=null)org.hibernate.Hibernate.initialize(channel);return load(channel);}finally{if(session!=null)try{session.close();}catch(Exception ignored){}}}catch(SecurityException e){throw e;}catch(Exception e){throw new SecurityException("Callback tidak dapat dipetakan ke SosialChannel.");}}
 public void verifyCallback(String remoteAddress,String signature,String raw){Credential c=forCallback(raw);if(empty(c.getCallbackSecret())||c.getCallbackSecret().length()<16||empty(c.getAllowedIps()))throw new SecurityException("Keamanan callback SosialChannel belum lengkap.");boolean allowed=false;for(String ip:c.getAllowedIps().split(","))if(remoteAddress.equals(ip.trim()))allowed=true;if(!allowed)throw new SecurityException("Source callback ditolak.");if(signature==null||!SocialSecurity.constantEquals(SocialSecurity.hmacSha256(c.getCallbackSecret(),raw),signature.trim().toLowerCase()))throw new SecurityException("Signature callback ditolak.");}
 public String encryptSecret(String plain){if(empty(plain))throw new IllegalArgumentException("Secret tidak boleh kosong.");return Common.desEncrypter.get().encrypt(plain);}
 private String decrypt(String cipher){if(empty(cipher))return null;try{return Common.desEncrypter.get().decrypt(cipher);}catch(Exception e){throw new IllegalStateException("Secret SosialChannel tidak dapat didekripsi.");}}
 private boolean empty(String value){return value==null||value.trim().isEmpty();}
}
