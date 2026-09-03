package ais.database.model.payroll;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
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

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisCutiDanIzin;
import ais.database.model.employ.KelompokParameterTambahanCutiDanIzin;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entitas Hibernate untuk pengajuan cuti &amp; izin kepegawaian AIS — dipetakan ke tabel
 * {@code payroll.cuti_dan_izin}. Merupakan bagian dari modul payroll/kepegawaian dan sekaligus
 * subclass dari {@link DataSop}, sehingga alur persetujuannya (approval workflow) mengikuti pola
 * SOP/disposisi generik AIS lewat relasi {@link #getDisposisiSop() disposisiSop} — BUKAN lewat
 * field {@code setujui}/{@code disetujiOleh}/{@code setujuiTanggal} yang tersimpan langsung di
 * baris ini (lihat catatan pada masing-masing getter di bawah, yang justru MENGHITUNG ULANG nilai
 * tersebut dari {@code disposisiSop} setiap kali dipanggil).
 *
 * <h2>Alur persetujuan (lihat {@link #getDisposisiSop()}, {@link #getSetujui()},
 * {@link #getDisetujuiOleh()}, {@link #getSetujuiTanggal()}, {@link #getDiajukanOleh()})</h2>
 * <p>
 * Pengajuan cuti terhubung ke satu {@link DisposisiSop} (alur disposisi SOP generik AIS).
 * Ketiga getter {@code setujui}, {@code disetujuiOleh}, dan {@code setujuiTanggal} membaca ulang
 * nilainya dari {@code disposisiSop.getDisposisiSetuju()} setiap kali dipanggil (bukan murni
 * mengembalikan field yang di-set manual) — field instance-nya sendiri hanya dipakai sebagai
 * cache/fallback. Demikian pula {@link #getDiajukanOleh()} diturunkan dari
 * {@code disposisiSop.getDisposisiStart()} bila tersedia.
 * </p>
 *
 * <h2>Parameter tambahan berformat teks ({@link #getParameterTambahan()}/
 * {@link #getParameterTambahanInds()})</h2>
 * <p>
 * Sama seperti pola di {@link ais.database.model.IsiAngketParameterUmum}: daftar parameter
 * tambahan (custom field per {@link JenisCutiDanIzin}) disimpan sebagai SATU string {@code TEXT}
 * berisi banyak baris (dipisah {@code \n}), tiap baris berisi field dipisah token literal
 * {@code "<=>"} dengan urutan kurang-lebih
 * {@code label->nilai<=>url_lampiran<=>nomor_urut<=>id_parameter}. Lihat
 * {@link #populateParameterTambahan(List)} (menulis dari komponen ZK {@code Row}) dan
 * {@link #ambilDataParameterTambahan()} (mem-parse balik ke {@link CommonVO}) untuk format
 * persisnya. Rapuh terhadap token {@code "<=>"} atau {@code "\n"} yang muncul di dalam nilai
 * pengguna sendiri.
 * </p>
 *
 * <h2>Penghitungan jatah cuti ({@link #getJumlahHariCuti()}, {@link #getKecualiTanggals()})</h2>
 * <p>
 * Jumlah hari cuti dihitung dari rentang {@link #getMulai()}..{@link #getSampai()} (memakai hari
 * kerja saja bila {@link Statusabsensi#getHariLiburDihitung()} true), lalu dikurangi tanggal yang
 * ada di {@link #getKecualiTanggals()} — sebuah string JSON array (mis.
 * {@code ["Senin, 08-06-2026", ...]}, diparse dengan {@code Common.dateFormat4}) berisi
 * tanggal-tanggal dalam rentang yang SENGAJA tidak dihitung sebagai cuti. Method statis
 * {@link #apakahSedangCuti(Pegawai, Date)} memakai daftar kecuali yang sama untuk menentukan
 * apakah seorang pegawai sedang cuti pada tanggal tertentu.
 * </p>
 *
 * @see DataSop
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "cuti_dan_izin")
public class CutiDanIzin extends DataSop {

    private static final long serialVersionUID = 2463821577548439808L;
    private Long id;
    /** Nama pengguna yang terakhir mengubah baris ini (audit, bukan pemohon cuti — lihat {@link #diajukanOleh}). */
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private String keterangan;
    /** Tanggal mulai cuti/izin (inklusif). */
    private Date mulai;
    /** Tanggal selesai cuti/izin (inklusif). */
    private Date sampai;
    /** Pegawai yang mengajukan cuti/izin ini. */
    private Pegawai pegawai;
    /** Status absensi yang berlaku selama rentang cuti ini (menentukan apakah memotong jatah cuti &amp; apakah hari libur ikut dihitung). */
    private Statusabsensi statusabsensi;
    /** Cache; nilai efektif selalu dihitung ulang di {@link #getMemotongJatahCuti()} dari {@link Statusabsensi#getMemotongJatahCuti()}. */
    private Boolean memotongJatahCuti;
    /** Cache; nilai efektif selalu dihitung ulang di {@link #getSetujui()} dari {@link #disposisiSop}. */
    private Boolean setujui;
    /** Cache; nilai efektif selalu dihitung ulang di {@link #getDisetujuiOleh()} dari {@link #disposisiSop}. */
    private Tbmuser disetujiOleh;
    /** Cache; nilai efektif selalu dihitung ulang di {@link #getSetujuiTanggal()} dari {@link #disposisiSop}. */
    private Date setujuiTanggal;
    /** Jenis cuti/izin (mis. Cuti Tahunan, Izin Sakit) — menentukan parameter tambahan apa saja yang relevan. */
    private JenisCutiDanIzin jenisCutiDanIzin;
    /** Lihat "Parameter tambahan berformat teks" pada Javadoc kelas — bentuk dengan label lengkap. */
    private String parameterTambahan;
    /** Lihat "Parameter tambahan berformat teks" pada Javadoc kelas — bentuk ringkas dengan id saja (dipakai untuk pencarian/pencocokan lampiran). */
    private String parameterTambahanInds;
    /** Jatah cuti yang dipakai oleh pengajuan ini (unit hari, dari perhitungan bisnis di luar entitas ini). */
    private Double jumlahCuti;
    /** Cache; nilai efektif selalu dihitung ulang di {@link #getJumlahHariCuti()} dari rentang {@link #mulai}/{@link #sampai} dikurangi {@link #kecualiTanggals}. */
    private Integer jumlahHariCuti;
    private Double jumlahCutiBersama;
    private Double sisaCuti;
    /** Cache; nilai efektif diturunkan di {@link #getDiajukanOleh()} dari {@link #disposisiSop} bila tersedia. */
    private Tbmuser diajukanOleh;
    /** Alur disposisi SOP yang menjadi sumber kebenaran status persetujuan pengajuan ini — lihat "Alur persetujuan" pada Javadoc kelas. */
    private DisposisiSop disposisiSop;
    /** JSON array berisi tanggal (format {@code Common.dateFormat4}) dalam rentang {@link #mulai}-{@link #sampai} yang DIKECUALIKAN dari perhitungan hari cuti — lihat "Penghitungan jatah cuti" pada Javadoc kelas. */
    private String kecualiTanggals;

    public CutiDanIzin() {
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOlehId() {
        return olehId;
    }

    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    @Override
    public String toString() {
        return id + "-" + (getStatusabsensi() == null ? "" : getStatusabsensi().getNama());
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
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

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
    public String getKeterangan() {
        return this.keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    public Date getMulai() {
        return mulai;
    }

    public void setMulai(Date mulai) {
        this.mulai = mulai;
    }

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    public Date getSampai() {
        return sampai;
    }

    public void setSampai(Date sampai) {
        this.sampai = sampai;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "pegawai", nullable = true)
    public Pegawai getPegawai() {
        pegawai = check(pegawai);
        return pegawai;
    }

    public void setPegawai(Pegawai pegawai) {
        this.pegawai = pegawai;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "statusabsensi", nullable = true)
    public Statusabsensi getStatusabsensi() {
        statusabsensi = check(statusabsensi);
        return statusabsensi;
    }

    public void setStatusabsensi(Statusabsensi statusabsensi) {
        this.statusabsensi = statusabsensi;
    }

    /**
     * @return {@code true} bila {@link #disposisiSop} sudah memiliki disposisi "setuju" dengan
     *         pemroses ({@code getDiajukanOleh()} pada disposisi tersebut) terisi; nilai TIDAK
     *         dibaca langsung dari field {@link #setujui} — field itu hanya cache hasil hitung
     *         terakhir (lihat "Alur persetujuan" pada Javadoc kelas).
     */
    public Boolean getSetujui() {
        if (getDisposisiSop() != null) {
            disetujiOleh = getDisposisiSop().getDisposisiSetuju() == null ? null : getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
            setujui = (disetujiOleh != null);
        }
        return setujui == null ? false : setujui;
    }

    public void setSetujui(Boolean setujui) {
        this.setujui = setujui;
    }

    /** @return apakah pengajuan ini memotong jatah cuti pegawai — diturunkan dari {@link Statusabsensi#getMemotongJatahCuti()} bila {@link #statusabsensi} terisi, atau {@code true} bila tidak. */
    public Boolean getMemotongJatahCuti() {
        statusabsensi = getStatusabsensi();
        memotongJatahCuti = (statusabsensi != null ? statusabsensi.getMemotongJatahCuti() : true);
        return memotongJatahCuti;
    }

    public void setMemotongJatahCuti(Boolean memotongJatahCuti) {
        this.memotongJatahCuti = memotongJatahCuti;
    }

    /** @return pengguna yang menyetujui disposisi {@link #disposisiSop} ini, atau {@code null} bila belum/tidak ada disposisi setuju — lihat "Alur persetujuan" pada Javadoc kelas. */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "disetuji_oleh", nullable = true)
    public Tbmuser getDisetujuiOleh() {
        disetujiOleh = check(disetujiOleh);

        if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null && getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
            disetujiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
        }

        if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null || getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
            disetujiOleh = null;
        }

        return disetujiOleh;
    }

    public void setDisetujuiOleh(Tbmuser disetujiOleh) {
        this.disetujiOleh = disetujiOleh;
    }

    /** @return tanggal/waktu disposisi setuju pada {@link #disposisiSop}, atau {@code null} bila belum disetujui — lihat "Alur persetujuan" pada Javadoc kelas. */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getSetujuiTanggal() {
        if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null && getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
            setujuiTanggal = getDisposisiSop().getDisposisiSetuju().getWaktu();
        }

        if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null || getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
            setujuiTanggal = null;
        }

        return setujuiTanggal;
    }

    public void setSetujuiTanggal(Date setujuiTanggal) {
        this.setujuiTanggal = setujuiTanggal;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jenis_cuti_dan_izin", nullable = true)
    public JenisCutiDanIzin getJenisCutiDanIzin() {
        jenisCutiDanIzin = check(jenisCutiDanIzin);
        return jenisCutiDanIzin;
    }

    public void setJenisCutiDanIzin(JenisCutiDanIzin jenisCutiDanIzin) {
        this.jenisCutiDanIzin = jenisCutiDanIzin;
    }

    @Column(columnDefinition = "text")
    public String getParameterTambahanInds() {
        return parameterTambahanInds == null ? "" : parameterTambahanInds;
    }

    public void setParameterTambahanInds(String parameterTambahanInds) {
        this.parameterTambahanInds = parameterTambahanInds;
    }

    /**
     * Mem-parse {@link #getParameterTambahan()} (satu baris teks per parameter, lihat "Parameter
     * tambahan berformat teks" pada Javadoc kelas) menjadi daftar {@link CommonVO} yang siap
     * ditampilkan/diiterasi di layar ZK, terurut menurut nomor urut parameter.
     *
     * @return daftar parameter tambahan terisi milik pengajuan ini; kosong bila belum ada data
     */
    public List<CommonVO> ambilDataParameterTambahan() {
        List<CommonVO> commonVOs = new ArrayList<CommonVO>();
        String pt = getParameterTambahan();
        if (pt == null || pt.isEmpty()) return commonVOs;
        
        String[] splNama = pt.split("\n");
        for (int j = 0; j < splNama.length; j++) {
            CommonVO commonVO = new CommonVO();
            String namaCol = splNama.length > j ? splNama[j] : "";

            String[] value = namaCol.split("<=>");
            String lbl = value.length > 0 ? value[0].trim() : "";
            String url = value.length > 2 ? value[2].trim() : "";
            String val = value.length > 1 ? value[1].trim() : "";
            Integer nomorUrut = 1;
            try {
                nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/CutiDanIzin.java:272");
            }
            Long idItem = 1L;
            try {
                idItem = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/CutiDanIzin.java:277");
            }

            String[] param = lbl.split("->");

            commonVO.setId(idItem.toString());
            commonVO.setName(lbl);
            commonVO.setName1(val);
            commonVO.setName2(url);
            commonVO.setName5(param.length > 0 ? param[0] : "");
            commonVO.setNomorUrut(nomorUrut);
            commonVOs.add(commonVO);
        }
        Collections.sort(commonVOs);
        return commonVOs;
    }

    /**
     * Membangun ulang {@link #parameterTambahan} dan {@link #parameterTambahanInds} dari baris
     * komponen ZK ({@code parameterRows}) hasil input pengguna — dipanggil dari layar pengajuan
     * cuti/izin saat form parameter tambahan disimpan. Setiap {@link Row} diharapkan membawa
     * attribute {@code parameterTambahan} dan {@code kelompokParameterTambahanCutiDanIzin} (di-set
     * oleh kode ZK pemanggil); baris tanpa kedua attribute tersebut dilewati begitu saja. Bila
     * {@link ParameterTambahan#getHarusMenyertakanLampiran()} true, method ini juga mencoba
     * melampirkan URL berkas via {@link LampiranLain#ambil(Long, String)}.
     *
     * @param parameterRows baris ZK berisi komponen input parameter tambahan; tidak melakukan apa
     *                       pun bila {@code null}/kosong
     */
    public void populateParameterTambahan(List<Row> parameterRows) {
        if (parameterRows == null || parameterRows.isEmpty()) {
            return;
        }

        StringBuilder paramTambahanStrBuilder = new StringBuilder();
        StringBuilder paramTambahanIndsBuilder = new StringBuilder();

        for (Row row : parameterRows) {
            try {
                ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
                KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCutiDanIzin = (KelompokParameterTambahanCutiDanIzin) row.getAttribute("kelompokParameterTambahanCutiDanIzin");
                
                if (parameterTambahan != null && kelompokParameterTambahanCutiDanIzin != null) {
                    String jenis = kelompokParameterTambahanCutiDanIzin.getId() + "->" + parameterTambahan.getId();
                    String val = ParameterTambahan.ambilVal(row, parameterTambahan);
                    
                    Object objKeterangan = row.getAttribute("keterangan");
                    Textbox keteranganBox = (objKeterangan != null && objKeterangan instanceof Textbox) ? (Textbox) objKeterangan : null;
                    String ketVal = keteranganBox == null ? "" : keteranganBox.getValue().trim();
                    
                    String url = "";
                    if (parameterTambahan.getHarusMenyertakanLampiran()) {
                        LampiranLain lam = LampiranLain.ambil(getId(), jenis);
                        if (lam != null) {
                            try {
                                url = lam.createLinkUri();
                            } catch (Exception e) {
                                Common.tampilErrorJikaAdmin(e);
                            }
                        }
                    }

                    String s = kelompokParameterTambahanCutiDanIzin.getNama() + "->"
                            + parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
                            + parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
                            + kelompokParameterTambahanCutiDanIzin.getId() + "<=>" + ketVal;

                    if (paramTambahanStrBuilder.length() > 0) paramTambahanStrBuilder.append("\n");
                    paramTambahanStrBuilder.append(s);

                    String sIds = kelompokParameterTambahanCutiDanIzin.getId() + "->" + parameterTambahan.getId()
                            + "<=>" + val + "<=>" + url + "<=>" + ketVal;
                    
                    if (paramTambahanIndsBuilder.length() > 0) paramTambahanIndsBuilder.append("\n");
                    paramTambahanIndsBuilder.append(sIds);
                }
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        }
        setParameterTambahanInds(paramTambahanIndsBuilder.toString());
        setParameterTambahan(paramTambahanStrBuilder.toString());
    }

    @Column(columnDefinition = "text")
    public String getParameterTambahan() {
        return parameterTambahan == null ? "" : parameterTambahan;
    }

    public void setParameterTambahan(String parameterTambahan) {
        this.parameterTambahan = parameterTambahan;
    }

    public Double getJumlahCuti() {
        return jumlahCuti;
    }

    public void setJumlahCuti(Double jumlahCuti) {
        this.jumlahCuti = jumlahCuti;
    }

    public Double getJumlahCutiBersama() {
        return jumlahCutiBersama;
    }

    public void setJumlahCutiBersama(Double jumlahCutiBersama) {
        this.jumlahCutiBersama = jumlahCutiBersama;
    }

    public Double getSisaCuti() {
        return sisaCuti;
    }

    public void setSisaCuti(Double sisaCuti) {
        this.sisaCuti = sisaCuti;
    }

    /** @return pengaju cuti/izin, diambil dari disposisi awal ({@code disposisiSop.getDisposisiStart()}) bila tersedia, atau fallback ke field {@link #diajukanOleh} yang di-set manual. */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "diajukan_oleh", nullable = true)
    public Tbmuser getDiajukanOleh() {
        diajukanOleh = check(diajukanOleh);

        if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null && getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
            diajukanOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
        }

        return diajukanOleh;
    }

    public void setDiajukanOleh(Tbmuser diajukanOleh) {
        this.diajukanOleh = diajukanOleh;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "disposisi_sop", nullable = true)
    public DisposisiSop getDisposisiSop() {
        disposisiSop = check(disposisiSop);
        return disposisiSop;
    }

    /**
     * Menolak meng-set {@link #disposisiSop} ke {@code null} atau ke entitas belum tersimpan
     * (id {@code null}) — dipertahankan hingga diganti dengan disposisi lain yang valid.
     * Catatan: klausa ternary di badan method ini secara efektif selalu bernilai {@code
     * disposisiSop} (kondisi kedua ternary sudah pasti false pada titik ini karena guard di atas
     * baru saja memastikan {@code disposisiSop} bukan null dan id-nya bukan null) — perilaku
     * kode saat ini sederhana "set langsung setelah guard", dicatat di sini tanpa mengubah logika.
     *
     * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null} atau belum punya id
     */
    public void setDisposisiSop(DisposisiSop disposisiSop) {
        if (disposisiSop == null || disposisiSop.getId() == null) {
            return;
        }
        this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
    }

    /**
     * Query statis: mencari pengajuan cuti/izin milik {@code pegawai} yang SUDAH DISETUJUI
     * ({@code setujui = true}) dan mencakup {@code tanggal} dalam rentang {@code mulai..sampai},
     * lalu mengembalikan pengajuan pertama (terurut menurut {@code mulai}, lalu {@code id}) yang
     * tanggalnya TIDAK termasuk daftar {@link #getKecualiTanggals()} (lihat
     * {@link #apakahTanggalDikecualikan(CutiDanIzin, Date)}). Membuka sesi Hibernate native sendiri
     * ({@link HibernateUtil#currentNativeSession()}) dan SELALU menutupnya di blok
     * {@code finally} (termasuk {@link HibernateUtil#closeSession()}), sehingga aman dipanggil dari
     * luar siklus request/thread-session biasa.
     *
     * @param pegawai pegawai yang diperiksa; {@code null} langsung mengembalikan {@code null}
     * @param tanggal tanggal yang diperiksa; {@code null} langsung mengembalikan {@code null}
     * @return pengajuan cuti yang berlaku pada {@code tanggal} tsb, atau {@code null} bila tidak
     *         sedang cuti (atau terjadi exception apa pun — exception ditelan dan dianggap
     *         "tidak sedang cuti", TIDAK dilaporkan ke pemanggil)
     */
    @SuppressWarnings("unchecked")
    public static CutiDanIzin apakahSedangCuti(Pegawai pegawai, Date tanggal) {
        if (pegawai == null || tanggal == null) return null;
        
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            List<CutiDanIzin> cutiDanIzins = session.createCriteria(CutiDanIzin.class)
                    .add(Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(tanggal) + "') between mulai and sampai"))
                    .add(Restrictions.eq("pegawai", pegawai))
                    .add(Restrictions.eq("setujui", true))
                    .addOrder(Order.asc("mulai"))
                    .addOrder(Order.asc("id"))
                    .list();

            if (cutiDanIzins == null || cutiDanIzins.isEmpty()) {
                return null;
            }

            for (CutiDanIzin cutiDanIzin : cutiDanIzins) {
                if (cutiDanIzin == null) {
                    continue;
                }

                // Tanggal pada kolom kecuali_tanggals tidak dihitung sebagai sedang cuti.
                // Format data: JSON Array, contoh ["Senin, 08-06-2026", ...]
                // Pembanding utama mengikuti format yang dipakai saat menyimpan: Common.dateFormat4.
                if (!apakahTanggalDikecualikan(cutiDanIzin, tanggal)) {
                    return cutiDanIzin;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.disconnect();
                session.close();
            }
            HibernateUtil.closeSession();
        }
    }

    /**
     * @return {@code true} bila {@code tanggal} ada di dalam JSON array {@link #getKecualiTanggals()}
     *         milik {@code cutiDanIzin} — dicocokkan lebih dulu sebagai teks (format
     *         {@code Common.dateFormat4}, mis. {@code "Senin, 08-06-2026"}), lalu sebagai fallback
     *         diparse ulang jadi {@link Date} dan dibandingkan lewat {@code Common.databaseDateFormat}
     *         agar tetap cocok meski kapitalisasi nama hari berbeda. Format lama/tidak valid pada
     *         {@code kecualiTanggals} diabaikan secara aman (dianggap tidak ada tanggal dikecualikan).
     */
    private static boolean apakahTanggalDikecualikan(CutiDanIzin cutiDanIzin, Date tanggal) {
        if (cutiDanIzin == null || tanggal == null) {
            return false;
        }

        String kecualiStr = cutiDanIzin.getKecualiTanggals();
        if (kecualiStr == null || kecualiStr.trim().isEmpty()) {
            return false;
        }

        try {
            JSONArray arr = new JSONArray(kecualiStr);
            String tanggalText = Common.dateFormat4.get().format(tanggal);
            String tanggalDb = Common.databaseDateFormat.get().format(tanggal);

            for (int i = 0; i < arr.length(); i++) {
                String nilai = arr.isNull(i) ? "" : arr.getString(i);
                if (nilai == null) {
                    continue;
                }

                nilai = nilai.trim();
                if (nilai.isEmpty()) {
                    continue;
                }

                if (nilai.equalsIgnoreCase(tanggalText)) {
                    return true;
                }

                // Fallback aman: jika text tanggal dapat diparse oleh dateFormat4,
                // bandingkan sebagai tanggal agar tetap cocok meskipun kapitalisasi hari berbeda.
                try {
                    Date tanggalKecuali = Common.dateFormat4.get().parse(nilai);
                    if (tanggalKecuali != null && tanggalDb.equals(Common.databaseDateFormat.get().format(tanggalKecuali))) {
                        return true;
                    }
                } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/database/model/payroll/CutiDanIzin.java:493");
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/CutiDanIzin.java:496");
            // Format lama/invalid diabaikan supaya pengecekan cuti tetap berjalan normal.
        }

        return false;
    }

    /**
     * Menghitung jumlah hari cuti efektif dari rentang {@link #getMulai()}..{@link #getSampai()}
     * (inklusif kedua ujung), memakai hari kerja saja ({@code Common.getWorkingDaysBetweenTwoDates})
     * bila {@link Statusabsensi#getHariLiburDihitung()} pada {@link #statusabsensi} bernilai true,
     * atau seluruh hari kalender ({@code Common.getBetweenTwoDates}) bila tidak — lalu dikurangi
     * tanggal-tanggal pada {@link #getKecualiTanggals()} yang jatuh dalam rentang tsb DAN belum
     * dikecualikan otomatis oleh sistem (mis. tanggal itu memang hari libur/akhir pekan yang sudah
     * tidak dihitung sejak awal, dicek lewat {@code Common.isHolidayMerahDanAtauHariLibur}) — untuk
     * menghindari pengurangan ganda. Hasil dijamin tidak negatif; exception apa pun membuat method
     * ini mengembalikan {@code 0} alih-alih melempar.
     *
     * @return jumlah hari cuti yang dihitung; {@code 0} bila data tidak lengkap atau terjadi error
     */
    public Integer getJumlahHariCuti() {
        try {
            if (getStatusabsensi() != null && getMulai() != null && getSampai() != null) {
                
                int libur = getStatusabsensi().getHariLiburDihitung() != null && getStatusabsensi().getHariLiburDihitung()
                        ? Common.getWorkingDaysBetweenTwoDates(getMulai(), getSampai(), getPegawai()) + 1
                        : Common.getBetweenTwoDates(getMulai(), getSampai()) + 1;

                int potong = 0;
                String kecualiStr = getKecualiTanggals();
                
                if (kecualiStr != null && !kecualiStr.trim().isEmpty()) {
                    try {
                        JSONArray arr = new JSONArray(kecualiStr);
                        for (int i = 0; i < arr.length(); i++) {
                            String tglStr = arr.getString(i);
                            Date tglCb = Common.dateFormat4.get().parse(tglStr);

                            // Validasi keamanan: hanya proses jika tanggal ada dalam rentang mulai - sampai
                            if (tglCb.before(getMulai()) || tglCb.after(getSampai())) {
                                continue;
                            }

                            boolean dihitungOlehSystem = true;
                            
                            // Pastikan tidak ada pengurangan ganda jika sistem sudah mengecualikannya (libur dinasional/sabtu/minggu)
                            if (getStatusabsensi().getHariLiburDihitung() != null && getStatusabsensi().getHariLiburDihitung()) {
                                if (Common.isHolidayMerahDanAtauHariLibur(tglCb, getPegawai())) {
                                    dihitungOlehSystem = false;
                                }
                            }

                            if (dihitungOlehSystem) {
                                potong++;
                            }
                        }
                    } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/payroll/CutiDanIzin.java:539");
                        // Exception dari JSON parsing diabaikan secara aman agar tidak menghancurkan kalkulasi dasar libur.
                    }
                }

                libur = libur - potong;
                
                if (libur < 0) {
                    libur = 0;
                }
                
                jumlahHariCuti = libur;
            }
        } catch (Exception e) {
            jumlahHariCuti = 0;
        }
        
        return jumlahHariCuti;
    }

    public void setJumlahHariCuti(Integer jumlahHariCuti) {
        this.jumlahHariCuti = jumlahHariCuti;
    }

    @Column(columnDefinition = "text")
    public String getKecualiTanggals() {
        return kecualiTanggals == null ? "" : kecualiTanggals.trim();
    } 

    public void setKecualiTanggals(String kecualiTanggals) {
        this.kecualiTanggals = kecualiTanggals;
    }
}
