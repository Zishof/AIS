package ais.database.model.sosial;

import java.math.BigDecimal;
import javax.persistence.*;
import org.hibernate.envers.Audited;
import ais.database.model.akunting.Akun;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entitas Hibernate: master kanal/saluran penerimaan pembayaran donasi ("channel") pada modul
 * sosial AIS — dipetakan ke tabel {@code public.sosial_channel} (SATU baris per kombinasi
 * {@code tenant_key}+{@code kode}, dijaga unique constraint). Merepresentasikan konfigurasi satu
 * gateway pembayaran (saat ini hanya provider {@code "SMARTLINK"}, lihat {@link #getProvider()})
 * berikut kredensial API-nya — kredensial disimpan TERENKRIPSI (akhiran {@code Ciphertext} pada
 * {@link #getSmartlinkPasswordCiphertext()} dan {@link #getSmartlinkCallbackSecretCiphertext()}
 * menandakan nilai plaintext tidak pernah disimpan di kolom ini) dan direferensikan oleh
 * jenis dana/transaksi donasi untuk memanggil API SmartLink serta memverifikasi signature
 * webhook callback gateway (lihat {@link SocialPaymentReconciliation} untuk pencatatan anomali
 * rekonsiliasi kanal ini).
 *
 * <p>
 * <b>Anomali penamaan (diinvestigasi secara eksplisit):</b> kelas ini bernama {@code
 * SosialChannel} (ejaan Indonesia) dan tabelnya {@code sosial_channel} — SATU-SATUNYA di antara
 * kedelapan entitas turunan {@link SocialRecord} pada paket ini yang tidak memakai ejaan Inggris
 * {@code Social}/{@code social_}. Riwayat SVN (r78236 lalu r78243/r78244, ketiganya 24 Agustus
 * 2026 dalam rentang waktu berdekatan) menunjukkan kelas ini dibuat pada sesi yang sama dengan
 * peluncuran awal seluruh paket {@code sosial}, sebelum konvensi penamaan {@code Social*}/
 * {@code social_*} dipakai secara konsisten untuk ketujuh entitas berikutnya. Kelas Action
 * pemakainya ({@code ais.action.master.sosial.SosialChannelAction},
 * {@code SosialTransaksiAction}) juga mempertahankan ejaan {@code Sosial} — konsisten dengan
 * konvensi menu/URL berbahasa Indonesia yang dipakai di seluruh AIS — sementara layer
 * service/helper baru di bawahnya ({@code ais.action.master.sosial.helper.Social*}, tanpa
 * padanan legacy langsung) memakai ejaan Inggris {@code Social}. <b>Kesimpulan:</b> ini BUKAN
 * entitas legacy terpisah yang kebetulan berada di paket yang sama — kelas ini MEWARISI
 * {@link SocialRecord} yang sama persis dengan ketujuh entitas {@code Social*} lain dan
 * merupakan bagian integral dari layer modern yang sama, hanya kebetulan menjadi satu-satunya
 * sisa penamaan Indonesia dari awal pembuatan modul ini.
 * </p>
 *
 * <p>
 * Berelasi many-to-one wajib ke {@link Akun} (akun akuntansi tujuan pencatatan dana yang
 * diterima lewat kanal ini) dan opsional ke {@link Yayasan}/{@link Sekolah} untuk kanal yang
 * dibatasi pada satu unit tertentu. Mewarisi kolom teknis multi-tenant &amp; jejak audit dari
 * {@link SocialRecord}. Diaudit lewat Hibernate Envers ({@code @Audited}).
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="sosial_channel",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class SosialChannel extends SocialRecord {
 private static final long serialVersionUID=1L;
 /** Kode unik kanal (per tenant); nama tampilan; keterangan/deskripsi bebas; kode provider gateway (default {@code "SMARTLINK"}); mode operasi (default {@code "SANDBOX"}); serta seluruh parameter integrasi SmartLink: URL endpoint, username, password terenkripsi, daftar kanal pembayaran yang diaktifkan (default {@code "VA_CIMB,VA_BRI"}), secret HMAC callback terenkripsi, dan daftar IP yang diizinkan memanggil endpoint callback. */
 private String kode,nama,keterangan,provider,operationMode,smartlinkUrl,smartlinkUsername,smartlinkPasswordCiphertext,smartlinkChannels,smartlinkCallbackSecretCiphertext,smartlinkCallbackAllowedIps;
 /** Flag status aktif kanal (default {@code true} bila belum diset — fail-open) dan flag pengaktifan integrasi SmartLink (default {@code false} bila {@code null}); biaya SmartLink per transaksi; akun akuntansi tujuan dana (wajib); yayasan dan sekolah pembatas cakupan kanal ini (keduanya opsional, independen). */
 private Boolean aktif,smartlinkEnabled; private BigDecimal smartlinkFee; private Akun akun; private Yayasan yayasan; private Sekolah sekolah;
 /** Kode unik kanal dalam tenant (dipangkas spasi awal/akhir saat disimpan). */
 @Column(name="kode",nullable=false,length=80) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
 /** Nama tampilan kanal. */
 @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
 /** Keterangan/deskripsi bebas kanal. */
 @Column(name="keterangan",length=1000) public String getKeterangan(){return keterangan;} public void setKeterangan(String v){keterangan=v;}
 /** Kode provider gateway pembayaran kanal ini; mengembalikan default {@code "SMARTLINK"} bila belum diset di database. */
 @Column(name="provider",nullable=false,length=40) public String getProvider(){return provider==null?"SMARTLINK":provider;} public void setProvider(String v){provider=trim(v);}
 /** Mode operasi kanal (mis. sandbox/produksi); default {@code "SANDBOX"} bila belum diset. */
 @Column(name="operation_mode",nullable=false,length=30) public String getOperationMode(){return operationMode==null?"SANDBOX":operationMode;} public void setOperationMode(String v){operationMode=trim(v);}
 /** Status aktif kanal; dianggap aktif ({@code true}) kecuali eksplisit diset {@code false} (fail-open bila {@code null}). */
 @Column(name="aktif") public Boolean getAktif(){return !Boolean.FALSE.equals(aktif);} public void setAktif(Boolean v){aktif=v;}
 /** Akun akuntansi tujuan pencatatan dana yang diterima lewat kanal ini (wajib). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="akun_id",nullable=false) public Akun getAkun(){return akun;} public void setAkun(Akun v){akun=v;}
 /** Yayasan yang membatasi cakupan kanal ini, bila kanal khusus satu yayasan (opsional). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="yayasan_id") public Yayasan getYayasan(){return yayasan;} public void setYayasan(Yayasan v){yayasan=v;}
 /** Sekolah yang membatasi cakupan kanal ini, bila kanal khusus satu sekolah (opsional). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sekolah_id") public Sekolah getSekolah(){return sekolah;} public void setSekolah(Sekolah v){sekolah=v;}
 /** Menandai apakah integrasi SmartLink aktif untuk kanal ini; dianggap {@code false} bila {@code null}. */
 @Column(name="smartlink_enabled") public Boolean getSmartlinkEnabled(){return Boolean.TRUE.equals(smartlinkEnabled);} public void setSmartlinkEnabled(Boolean v){smartlinkEnabled=v;}
 /** URL endpoint API SmartLink yang dipakai kanal ini. */
 @Column(name="smartlink_url",length=1500) public String getSmartlinkUrl(){return smartlinkUrl;} public void setSmartlinkUrl(String v){smartlinkUrl=trim(v);}
 /** Username otentikasi API SmartLink. */
 @Column(name="smartlink_username",length=320) public String getSmartlinkUsername(){return smartlinkUsername;} public void setSmartlinkUsername(String v){smartlinkUsername=trim(v);}
 /** Password otentikasi API SmartLink, TERENKRIPSI (bukan plaintext) — didekripsi oleh layer service ({@code SocialSmartlinkCredentialService}/{@code SocialSmartlinkAdapter}) sebelum dipakai memanggil gateway. */
 @Column(name="smartlink_password_ciphertext",length=2000) public String getSmartlinkPasswordCiphertext(){return smartlinkPasswordCiphertext;} public void setSmartlinkPasswordCiphertext(String v){smartlinkPasswordCiphertext=trim(v);}
 /** Daftar kode kanal pembayaran SmartLink yang diaktifkan (dipisah koma); default {@code "VA_CIMB,VA_BRI"} bila belum diset. */
 @Column(name="smartlink_channels",length=1000) public String getSmartlinkChannels(){return smartlinkChannels==null?"VA_CIMB,VA_BRI":smartlinkChannels;} public void setSmartlinkChannels(String v){smartlinkChannels=trim(v);}
 /** Biaya yang dikenakan SmartLink per transaksi lewat kanal ini; default {@link BigDecimal#ZERO} bila belum diset. */
 @Column(name="smartlink_fee",nullable=false,precision=19,scale=2) public BigDecimal getSmartlinkFee(){return smartlinkFee==null?BigDecimal.ZERO:smartlinkFee;} public void setSmartlinkFee(BigDecimal v){smartlinkFee=v;}
 /** Secret HMAC untuk memverifikasi signature webhook callback SmartLink (lihat {@code SocialSecurity#hmacSha256}), TERENKRIPSI di kolom ini. */
 @Column(name="smartlink_callback_secret_ciphertext",length=2000) public String getSmartlinkCallbackSecretCiphertext(){return smartlinkCallbackSecretCiphertext;} public void setSmartlinkCallbackSecretCiphertext(String v){smartlinkCallbackSecretCiphertext=trim(v);}
 /** Daftar alamat IP yang diizinkan memanggil endpoint callback kanal ini (dipisah koma), sebagai pembatasan tambahan di luar verifikasi signature HMAC. */
 @Column(name="smartlink_callback_allowed_ips",length=2000) public String getSmartlinkCallbackAllowedIps(){return smartlinkCallbackAllowedIps;} public void setSmartlinkCallbackAllowedIps(String v){smartlinkCallbackAllowedIps=trim(v);}
}
