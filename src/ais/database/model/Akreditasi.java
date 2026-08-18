package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.rab.SatuanKerja;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "akreditasi")
public class Akreditasi extends GeneralValueObject {

    private static final long serialVersionUID = 2463821577548439808L;

    public static final String JENIS_EKSTERNAL = "Sertifikasi/Akreditasi Eksternal";
    public static final String JENIS_INTERNASIONAL = "Akreditasi Internasional Program Studi";
    public static final String JENIS_EKSTERNAL_KEUANGAN = "Audit Eksternal Keuangan";
    public static final String DOKUMEN = "Dokumen";

    public static final List<String> LINGKUP = new ArrayList<String>();
    public static final List<String> TINGKAT = new ArrayList<String>();
    public static final List<String> JENIS = new ArrayList<String>();

    static {
        TINGKAT.add("Lokal");
        TINGKAT.add("Nasional");
        TINGKAT.add("Internasional");

        LINGKUP.add("PT");
        LINGKUP.add("Fakultas");
        LINGKUP.add("Unit");

        JENIS.add(JENIS_EKSTERNAL);
        JENIS.add(JENIS_INTERNASIONAL);
        JENIS.add(JENIS_EKSTERNAL_KEUANGAN);
        JENIS.add(DOKUMEN);
    }

    private Long id;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    private String nama;
    private String lembaga;
    private String lingkup;
    private String tingkat;
    private String peringkat;
    private String kodeGrupPengguna;
    private String masaberlaku;
    private Integer tahun;
    private String opini;
    private String keterangan;
    private Date mulai;
    private Date sampai;
    private Boolean aktif;
    private Jurusan jurusan;
    private Dosen dosen;
    private String jenis;
    private SatuanKerja satuanKerja;

    public Akreditasi() {
    }

    public static List<String> jenisDokumenDms() {
        List<String> data = new ArrayList<String>();
        for (String s : JENIS) {
            tambahJikaBelumAda(data, s);
        }
        try {
            String tambahan = Common.getKonfigurasi("jenis_dokumen_dms_tambahan", "").getNilai();
            if (tambahan != null && !tambahan.trim().isEmpty()) {
                String normalized = tambahan.replace('\n', ';').replace('\r', ';').replace('|', ';').replace(',', ';');
                String[] values = normalized.split(";");
                for (int i = 0; i < values.length; i++) {
                    tambahJikaBelumAda(data, values[i]);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Akreditasi.java:98");
        }
        return data;
    }

    private static void tambahJikaBelumAda(List<String> data, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String clean = value.trim();
        for (String s : data) {
            if (s != null && s.equalsIgnoreCase(clean)) {
                return;
            }
        }
        data.add(clean);
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOlehId() {
        return olehId;
    }

    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) {
            return;
        }
        this.olehId = olehId;
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) {
            return;
        }
        this.oleh = oleh;
    }

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    @Override
    public String toString() {
        return (id == null ? "" : id.toString()) + "-" + safe(nama);
    }

    @Column(name = "nama", columnDefinition = "text")
    public String getNama() {
        return nama == null ? null : nama.trim();
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    @Column(name = "keterangan", nullable = true, columnDefinition = "text")
    public String getKeterangan() {
        return keterangan == null ? "" : keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    @Column(columnDefinition = "text")
    public String getLembaga() {
        return lembaga == null ? "" : lembaga;
    }

    public void setLembaga(String lembaga) {
        this.lembaga = lembaga;
    }

    @Column(columnDefinition = "text")
    public String getTingkat() {
        return tingkat == null ? "" : tingkat;
    }

    public void setTingkat(String tingkat) {
        this.tingkat = tingkat;
    }

    @Column(columnDefinition = "text")
    public String getLingkup() {
        return lingkup == null ? "" : lingkup;
    }

    public void setLingkup(String lingkup) {
        this.lingkup = lingkup;
    }

    public String getMasaberlaku() {
        return masaberlaku == null ? "" : masaberlaku;
    }

    public void setMasaberlaku(String masaberlaku) {
        this.masaberlaku = masaberlaku;
    }

    @Temporal(TemporalType.DATE)
    public Date getMulai() {
        return mulai;
    }

    public void setMulai(Date mulai) {
        this.mulai = mulai;
    }

    @Temporal(TemporalType.DATE)
    public Date getSampai() {
        return sampai;
    }

    public void setSampai(Date sampai) {
        this.sampai = sampai;
    }

    public String getJenis() {
        if (dosen != null) {
            return DOKUMEN;
        }
        return jenis == null || jenis.trim().isEmpty() ? DOKUMEN : jenis.trim();
    }

    public void setJenis(String jenis) {
        this.jenis = jenis;
    }

    public Integer getTahun() {
        return tahun == null || tahun.intValue() <= 1800 ? null : tahun;
    }

    public void setTahun(Integer tahun) {
        this.tahun = tahun;
    }

    @Column(columnDefinition = "text")
    public String getOpini() {
        return opini == null ? "" : opini;
    }

    public void setOpini(String opini) {
        this.opini = opini;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jurusan")
    public Jurusan getJurusan() {
        jurusan = check(jurusan);
        return jurusan;
    }

    public void setJurusan(Jurusan jurusan) {
        this.jurusan = jurusan;
    }

    public String getPeringkat() {
        return peringkat == null ? "" : peringkat;
    }

    public void setPeringkat(String peringkat) {
        this.peringkat = peringkat;
    }

    public Boolean getAktif() {
        return aktif == null ? Boolean.TRUE : aktif;
    }

    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "dosen", nullable = true)
    public Dosen getDosen() {
        dosen = check(dosen);
        return dosen;
    }

    public void setDosen(Dosen dosen) {
        this.dosen = dosen;
    }

    public String getKodeGrupPengguna() {
        if (getDosen() != null) {
            return "";
        }
        kodeGrupPengguna = normalizeCommaText(kodeGrupPengguna);
        return kodeGrupPengguna;
    }

    public void setKodeGrupPengguna(String kodeGrupPengguna) {
        this.kodeGrupPengguna = kodeGrupPengguna;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() {
        satuanKerja = check(satuanKerja);
        return satuanKerja;
    }

    public void setSatuanKerja(SatuanKerja satuanKerja) {
        this.satuanKerja = satuanKerja;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeCommaText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim().replace(';', ',').replace('|', ',');
        String[] items = trimmed.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            String item = items[i] == null ? "" : items[i].trim();
            if (item.length() == 0) {
                continue;
            }
            if (sb.indexOf("," + item + ",") < 0) {
                sb.append(',').append(item).append(',');
            }
        }
        String result = sb.toString().replace(",,", ",");
        if (",".equals(result)) {
            return "";
        }
        return result;
    }
}
