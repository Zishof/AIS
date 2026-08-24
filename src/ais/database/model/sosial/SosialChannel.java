package ais.database.model.sosial;

import java.math.BigDecimal;
import javax.persistence.*;
import org.hibernate.envers.Audited;
import ais.database.model.akunting.Akun;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/** Master kanal penerimaan sosial. Credential diisi sekali dan direferensikan oleh jenis dana/transaksi. */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="sosial_channel",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class SosialChannel extends SocialRecord {
 private static final long serialVersionUID=1L;
 private String kode,nama,keterangan,provider,operationMode,smartlinkUrl,smartlinkUsername,smartlinkPasswordCiphertext,smartlinkChannels,smartlinkCallbackSecretCiphertext,smartlinkCallbackAllowedIps;
 private Boolean aktif,smartlinkEnabled; private BigDecimal smartlinkFee; private Akun akun; private Yayasan yayasan; private Sekolah sekolah;
 @Column(name="kode",nullable=false,length=80) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
 @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
 @Column(name="keterangan",length=1000) public String getKeterangan(){return keterangan;} public void setKeterangan(String v){keterangan=v;}
 @Column(name="provider",nullable=false,length=40) public String getProvider(){return provider==null?"SMARTLINK":provider;} public void setProvider(String v){provider=trim(v);}
 @Column(name="operation_mode",nullable=false,length=30) public String getOperationMode(){return operationMode==null?"SANDBOX":operationMode;} public void setOperationMode(String v){operationMode=trim(v);}
 @Column(name="aktif") public Boolean getAktif(){return !Boolean.FALSE.equals(aktif);} public void setAktif(Boolean v){aktif=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="akun_id") public Akun getAkun(){return akun;} public void setAkun(Akun v){akun=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="yayasan_id") public Yayasan getYayasan(){return yayasan;} public void setYayasan(Yayasan v){yayasan=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sekolah_id") public Sekolah getSekolah(){return sekolah;} public void setSekolah(Sekolah v){sekolah=v;}
 @Column(name="smartlink_enabled") public Boolean getSmartlinkEnabled(){return Boolean.TRUE.equals(smartlinkEnabled);} public void setSmartlinkEnabled(Boolean v){smartlinkEnabled=v;}
 @Column(name="smartlink_url",length=1500) public String getSmartlinkUrl(){return smartlinkUrl;} public void setSmartlinkUrl(String v){smartlinkUrl=trim(v);}
 @Column(name="smartlink_username",length=320) public String getSmartlinkUsername(){return smartlinkUsername;} public void setSmartlinkUsername(String v){smartlinkUsername=trim(v);}
 @Column(name="smartlink_password_ciphertext",length=2000) public String getSmartlinkPasswordCiphertext(){return smartlinkPasswordCiphertext;} public void setSmartlinkPasswordCiphertext(String v){smartlinkPasswordCiphertext=trim(v);}
 @Column(name="smartlink_channels",length=1000) public String getSmartlinkChannels(){return smartlinkChannels==null?"VA_CIMB,VA_BRI":smartlinkChannels;} public void setSmartlinkChannels(String v){smartlinkChannels=trim(v);}
 @Column(name="smartlink_fee",nullable=false,precision=19,scale=2) public BigDecimal getSmartlinkFee(){return smartlinkFee==null?BigDecimal.ZERO:smartlinkFee;} public void setSmartlinkFee(BigDecimal v){smartlinkFee=v;}
 @Column(name="smartlink_callback_secret_ciphertext",length=2000) public String getSmartlinkCallbackSecretCiphertext(){return smartlinkCallbackSecretCiphertext;} public void setSmartlinkCallbackSecretCiphertext(String v){smartlinkCallbackSecretCiphertext=trim(v);}
 @Column(name="smartlink_callback_allowed_ips",length=2000) public String getSmartlinkCallbackAllowedIps(){return smartlinkCallbackAllowedIps;} public void setSmartlinkCallbackAllowedIps(String v){smartlinkCallbackAllowedIps=trim(v);}
}
