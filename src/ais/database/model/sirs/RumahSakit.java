package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.VoKunci;
import ais.database.model.Tbmuser;
import ais.database.model.sop.DisposisiSop;

/**
 * Profil tenant fasilitas kesehatan untuk website dan branding eMedic.
 * Nama historis RumahSakit mencakup RS, Puskesmas, Posyandu, Klinik,
 * praktik mandiri, laboratorium, apotek, dan fasilitas kesehatan lain.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "rumah_sakit")
public class RumahSakit extends VoKunci {
    private static final long serialVersionUID = 1L;

    public static final String JENIS_RUMAH_SAKIT = "RUMAH_SAKIT";
    public static final String JENIS_PUSKESMAS = "PUSKESMAS";
    public static final String JENIS_POSYANDU = "POSYANDU";
    public static final String JENIS_KLINIK = "KLINIK";
    public static final String JENIS_PRAKTIK_MANDIRI = "PRAKTIK_MANDIRI";
    public static final String JENIS_LABORATORIUM = "LABORATORIUM";
    public static final String JENIS_APOTEK = "APOTEK";
    public static final String JENIS_LAINNYA = "LAINNYA";

    public static final String TAMPILAN_DEFAULT = "default";
    public static final String TAMPILAN_KLASIK = "klasik";
    public static final String TAMPILAN_BARU = "baru";

    private Long id;
    private String kode;
    private String jenisFasilitas;
    private String nama;
    private String namaSingkat;
    private String alamat;
    private String telepon;
    private String whatsapp;
    private String email;
    private String website;
    private String domain;
    private String motto;
    private String deskripsi;
    private String nomorIzinOperasional;
    private String css;
    private String warna;
    private String pilihanTampilan;
    private Boolean aktif;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Tbmuser dikunci;
    private DisposisiSop disposisiSop;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "kode", length = 50)
    public String getKode() { return clean(kode); }
    public void setKode(String kode) { this.kode = kode; }

    @Column(name = "jenis_fasilitas", nullable = false, length = 40)
    public String getJenisFasilitas() {
        return clean(jenisFasilitas).length() == 0 ? JENIS_RUMAH_SAKIT : jenisFasilitas.trim().toUpperCase();
    }
    public void setJenisFasilitas(String jenisFasilitas) { this.jenisFasilitas = jenisFasilitas; }

    @Column(name = "nama", nullable = false, length = 180)
    public String getNama() { return clean(nama); }
    public void setNama(String nama) { this.nama = nama; }

    @Column(name = "nama_singkat", length = 80)
    public String getNamaSingkat() { return clean(namaSingkat); }
    public void setNamaSingkat(String namaSingkat) { this.namaSingkat = namaSingkat; }

    @Column(name = "alamat", length = 300)
    public String getAlamat() { return clean(alamat); }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    @Column(name = "telepon", length = 80)
    public String getTelepon() { return clean(telepon); }
    public void setTelepon(String telepon) { this.telepon = telepon; }

    @Column(name = "whatsapp", length = 80)
    public String getWhatsapp() { return clean(whatsapp); }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    @Column(name = "email", length = 255)
    public String getEmail() { return clean(email); }
    public void setEmail(String email) { this.email = email; }

    @Column(name = "website", length = 200)
    public String getWebsite() { return clean(website); }
    public void setWebsite(String website) { this.website = website; }

    @Column(name = "domain", unique = true, length = 500)
    public String getDomain() { return clean(domain); }
    public void setDomain(String domain) { this.domain = domain; }

    @Column(name = "motto", length = 300)
    public String getMotto() { return clean(motto); }
    public void setMotto(String motto) { this.motto = motto; }

    @Column(name = "deskripsi", columnDefinition = "text")
    public String getDeskripsi() { return clean(deskripsi); }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    @Column(name = "nomor_izin_operasional", length = 100)
    public String getNomorIzinOperasional() { return clean(nomorIzinOperasional); }
    public void setNomorIzinOperasional(String nomorIzinOperasional) { this.nomorIzinOperasional = nomorIzinOperasional; }

    @Column(name = "css", length = 150)
    public String getCss() { return clean(css); }
    public void setCss(String css) { this.css = css; }

    @Column(name = "warna", length = 20)
    public String getWarna() { return clean(warna); }
    public void setWarna(String warna) { this.warna = warna; }

    @Column(name = "pilihan_tampilan", length = 30)
    public String getPiilhanTampilan() {
        return clean(pilihanTampilan).length() == 0 ? TAMPILAN_DEFAULT : pilihanTampilan.trim();
    }
    public void setPiilhanTampilan(String pilihanTampilan) { this.pilihanTampilan = pilihanTampilan; }

    @Column(name = "aktif")
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    @Column(name = "oleh")
    public String getOleh() { return oleh; }
    public void setOleh(String oleh) {
        if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh;
    }

    @Column(name = "oleh_id")
    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) {
        if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId;
    }

    @ManyToOne(fetch = javax.persistence.FetchType.LAZY,
            cascade = { javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE })
    @JoinColumn(name = "dikunci")
    public Tbmuser getDikunci() {
        dikunci = check(dikunci);
        return dikunci;
    }
    public void setDikunci(Tbmuser dikunci) { this.dikunci = dikunci; }

    @ManyToOne(fetch = javax.persistence.FetchType.LAZY,
            cascade = { javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE })
    @JoinColumn(name = "disposisi_sop")
    public DisposisiSop getDisposisiSop() {
        disposisiSop = check(disposisiSop);
        return disposisiSop;
    }
    public void setDisposisiSop(DisposisiSop disposisiSop) { this.disposisiSop = disposisiSop; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggalDirubah) { this.tanggal_dirubah = tanggalDirubah; }

    public String getLabelJenisFasilitas() {
        String jenis = getJenisFasilitas();
        if (JENIS_PUSKESMAS.equals(jenis)) return "Puskesmas";
        if (JENIS_POSYANDU.equals(jenis)) return "Posyandu";
        if (JENIS_KLINIK.equals(jenis)) return "Klinik";
        if (JENIS_PRAKTIK_MANDIRI.equals(jenis)) return "Praktik Mandiri";
        if (JENIS_LABORATORIUM.equals(jenis)) return "Laboratorium Kesehatan";
        if (JENIS_APOTEK.equals(jenis)) return "Apotek";
        if (JENIS_LAINNYA.equals(jenis)) return "Fasilitas Kesehatan";
        return "Rumah Sakit";
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    @Override
    public String toString() { return getNama(); }
}
