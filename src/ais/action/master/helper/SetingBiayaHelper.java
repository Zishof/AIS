package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.ConstantValues;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.SettingBiaya;
import ais.database.model.SettingBiayaDetail;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;

/**
 * Helper (murni statis) inti mesin penentuan biaya kuliah/pendaftaran: mencocokkan profil satu
 * mahasiswa/calon mahasiswa (angkatan, jenjang, semester, jenis kegiatan, status awal/status
 * mahasiswa, jenis seleksi, gelombang pendaftaran, paket, jurusan, program, kelamin, afiliasi,
 * tahun akademik) terhadap konfigurasi {@link SettingBiaya} yang berlaku, lalu memastikan baris
 * tagihan {@link DetailBiaya} untuk setiap item biaya ({@link DetailSettingBiaya}) di bawah
 * konfigurasi tersebut sudah ada di database — membuatnya bila belum, memperbaruinya bila
 * relasinya berubah, dan memakai ulang baris lama bila sudah sesuai (idempoten).
 *
 * <p>
 * Ada dua jalur utama:
 * </p>
 * <ol>
 * <li><b>Jalur "cohort" (umum)</b> — dipakai untuk mahasiswa/calon mahasiswa yang biayanya
 * mengikuti aturan umum per kelompok (bukan per-orang). Kandidat {@link SettingBiaya} dicari lewat
 * {@link #createSettingBiayaCriteria} (filter tahun akademik ≤ ta, jenis kegiatan, rentang semester
 * — baik yang diatur di {@code SettingBiaya} sendiri maupun mengikuti rentang
 * {@link JenisKegiatan}), disaring/diprioritaskan lewat
 * {@link SettingBiayaMahasiswaSelector#saringDanPrioritaskan} dan
 * {@link SettingBiayaMahasiswaSelector#pilihSatuDenganPrioritas} (memilih satu {@code SettingBiaya}
 * paling spesifik yang cocok dengan seluruh atribut profil), lalu direalisasikan jadi baris
 * {@link DetailBiaya} lewat {@link #getDefaultSettingBiaya(Session, SettingBiaya, Integer, Jenjang,
 * Integer, JenisKegiatan, StatusAwalMahasiswa, StatusMahasiswa, JenisSeleksi,
 * GelombangPendaftaran, Paket, Jurusan, String, String, AfiliasiCalonMahasiswa)}.</li>
 * <li><b>Jalur "khusus per mahasiswa"</b> — dipakai bila mahasiswa/calon mahasiswa punya baris
 * {@link SettingBiayaDetail} yang menautkannya langsung (bukan lewat kecocokan atribut) ke satu
 * {@link SettingBiaya} ber-flag {@code khususBuatMahasiswaTertentu}. Direalisasikan lewat overload
 * {@link #getDefaultSettingBiaya(Session, SettingBiayaDetail, Integer, Mahasiswa)}/
 * {@link #getDefaultSettingBiaya(Session, SettingBiayaDetail, Integer, BiodataCalonMahasiswa)}.</li>
 * </ol>
 *
 * <p>
 * Setiap {@code SettingBiaya} bisa mendaftarkan NIM tertentu sebagai pengecualian
 * ({@link SettingBiaya#isMahasiswaDikecualikan(String)}) — bila NIM yang diminta termasuk
 * dikecualikan, hasil dikembalikan sebagai daftar kosong lewat {@link PengecualianTagihanList#kosong()}
 * (bukan {@code null}), yang secara semantik berbeda dari "tidak ada SettingBiaya yang cocok sama
 * sekali" ({@code null}, lihat javadoc masing-masing method).
 * </p>
 *
 * <p>
 * Pembuatan {@link DetailBiaya} baru berlapis tiga jaring pencarian "sudah adakah baris ini"
 * sebelum benar-benar membuat baris baru — pencarian ketat (kunci id persis), pencarian longgar
 * (item+bayarKe, status boleh berbeda dari saat baris lama dibuat karena status mahasiswa bisa
 * berubah lewat sinkron KRS/cuti/kampus-merdeka), dan jaring pengaman terakhir (item+bayarKe+
 * semester+settingBiaya, ditambah jurusan ketika nominal diatur per prodi) — untuk mencegah baris
 * tagihan duplikat akibat race condition (klik ganda, tab ganda) tanpa mencampur nominal antarprodi.
 * Objek entity yang berasal dari
 * cache lintas-sesi (mis. {@code ConstantValues.simpleList}) selalu diikat ke query lewat alias+id,
 * bukan lewat objek langsung, untuk menghindari {@code TransientObjectException}. Method
 * {@link #closeSessionSafely(Session)} sengaja DIKOSONGKAN — {@code Session} yang diterima method
 * di kelas ini milik pemanggil ({@code PembayaranUtilHelper}), yang bertanggung jawab menutupnya
 * sendiri di akhir iterasi besar.
 * </p>
 */
public class SetingBiayaHelper {

    private static final String SQL_TRUE = "1=1";

    // ===================================================================================
    // PRIVATE HELPER METHODS (Untuk efisiensi memori & mencegah duplikasi kode)
    // ===================================================================================

    /**
     * Menambahkan filter umum item biaya ke {@code criteria} (yang sudah beralias {@code itemBiaya}):
     * item harus aktif, dan bila {@code semester} diberikan, item tidak ditandai "tidak ditagih" di
     * semester ganjil/genap yang bersangkutan serta berada dalam rentang {@code minSmt}/{@code maxSmt}
     * item tersebut.
     */
    private static void applyItemBiayaFilters(Criteria criteria, Integer semester, boolean withOrdering) {
        if (withOrdering) {
            criteria.addOrder(Order.asc("itemBiaya.nama")).addOrder(Order.asc("bayarKe"));
        }

        if (semester != null) {
            if (semester % 2 == 0) { // SMT Genap
                criteria.add(Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGenap"), Restrictions.eq("itemBiaya.tidakDitagihDiSmtGenap", false)));
            } else { // SMT Ganjil
                criteria.add(Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGanjil"), Restrictions.eq("itemBiaya.tidakDitagihDiSmtGanjil", false)));
            }
            criteria.add(Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"), Restrictions.le("itemBiaya.minSmt", semester)));
            criteria.add(Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"), Restrictions.ge("itemBiaya.maxSmt", semester)));
        } else {
            criteria.add(Restrictions.sqlRestriction(SQL_TRUE));
        }

        criteria.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"), Restrictions.eq("itemBiaya.aktif", true)));
    }

    private static org.hibernate.criterion.Criterion eqAtauNull(String property, Object value) {
        return value == null ? Restrictions.isNull(property) : Restrictions.eq(property, value);
    }

    private static org.hibernate.criterion.Criterion eqAtauKosong(String property, String value) {
        return value == null || value.trim().isEmpty()
                ? Restrictions.or(Restrictions.isNull(property), Restrictions.eq(property, ""))
                : Restrictions.eq(property, value);
    }

    /**
     * Membatasi pencarian kompatibilitas data lama ke satu induk SettingBiaya.
     * Relasi dapat tersimpan langsung atau melalui salah satu rincian turunannya.
     */
    private static Criteria batasiKeSettingBiayaTerpilih(Criteria criteria, SettingBiaya settingBiaya,
            String suffix) {
        if (criteria == null || settingBiaya == null) {
            return criteria;
        }
        String rincian = "settingPrioritasRincian" + suffix;
        String individual = "settingPrioritasIndividual" + suffix;
        criteria.createAlias("detailSettingBiaya", rincian, Criteria.LEFT_JOIN);
        criteria.createAlias("settingBiayaDetail", individual, Criteria.LEFT_JOIN);
        criteria.add(Restrictions.or(
                Restrictions.and(Restrictions.isNotNull(individual + ".id"),
                        Restrictions.eq(individual + ".settingBiaya", settingBiaya)),
                Restrictions.or(
                        Restrictions.and(Restrictions.isNull(individual + ".id"),
                                Restrictions.and(Restrictions.isNotNull(rincian + ".id"),
                                        Restrictions.eq(rincian + ".settingBiaya", settingBiaya))),
                        Restrictions.and(Restrictions.isNull(individual + ".id"),
                                Restrictions.and(Restrictions.isNull(rincian + ".id"),
                                        Restrictions.eq("settingBiaya", settingBiaya))))));
        return criteria;
    }

    private static boolean samaId(GeneralValueObject a, GeneralValueObject b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.getId() == null || b.getId() == null) {
            return false;
        }
        return a.getId().equals(b.getId());
    }

    /**
     * Membangun kriteria kandidat {@link SettingBiaya}: tahun akademik ≤ {@code ta}, jenis kegiatan
     * sama, flag {@code khususBuatMahasiswaTertentu}/{@code gunakanBiayaDefault} sesuai
     * {@code isKhusus}/{@code isDefault} (diabaikan bila {@code null}), dan rentang semester — baik
     * yang diatur langsung pada {@code SettingBiaya} ({@code smtIkutiSettinganDisini=true}) maupun
     * mengikuti rentang semester {@link JenisKegiatan} induk (bila flag itu false/null).
     */
    private static Criteria createSettingBiayaCriteria(Session session, Integer ta, JenisKegiatan jenisKegiatan, Integer semester, Boolean isKhusus, Boolean isDefault) {
        Criteria criteria = session.createCriteria(SettingBiaya.class);
        if (ta != null) criteria.add(Restrictions.le("ta", ta));
        if (jenisKegiatan != null) criteria.add(Restrictions.eq("jenisKegiatan", jenisKegiatan));

        if (isKhusus != null) {
            if (isKhusus) {
                criteria.add(Restrictions.eq("khususBuatMahasiswaTertentu", true));
            } else {
                criteria.add(Restrictions.or(Restrictions.isNull("khususBuatMahasiswaTertentu"), Restrictions.eq("khususBuatMahasiswaTertentu", false)));
            }
        }

        if (isDefault != null) {
            if (isDefault) {
                criteria.add(Restrictions.eq("gunakanBiayaDefault", true));
            } else {
                criteria.add(Restrictions.or(Restrictions.isNull("gunakanBiayaDefault"), Restrictions.eq("gunakanBiayaDefault", false)));
            }
        }

        if (semester != null) {
            // Rentang semester di dalam SettingBiaya ini sendiri (minSmt..maxSmt milik setting).
            org.hibernate.criterion.Criterion rentangSettingIni = Restrictions.and(
                    Restrictions.or(Restrictions.isNull("minSmt"), Restrictions.le("minSmt", semester)),
                    Restrictions.or(Restrictions.isNull("maxSmt"), Restrictions.ge("maxSmt", semester)));

            int jkMin = (jenisKegiatan == null || jenisKegiatan.getMinSmt() == null) ? 0
                    : jenisKegiatan.getMinSmt().intValue();
            int jkMax = (jenisKegiatan == null || jenisKegiatan.getMaxSmt() == null) ? 30
                    : jenisKegiatan.getMaxSmt().intValue();
            boolean dalamRentangJenisKegiatan = (semester.intValue() >= jkMin && semester.intValue() <= jkMax);

            // smtIkutiSettinganDisini = true  -> rentang DIATUR DI SINI: cukup berada di dalam rentang
            //                                    SettingBiaya ini; rentang semester JenisKegiatan TIDAK berlaku.
            // smtIkutiSettinganDisini = false -> IKUT JenisKegiatan: semester wajib di dalam rentang
            //                                    JenisKegiatan (sekaligus tetap di dalam rentang SettingBiaya).
            criteria.add(Restrictions.or(
                    Restrictions.and(Restrictions.eq("smtIkutiSettinganDisini", Boolean.TRUE), rentangSettingIni),
                    Restrictions.and(
                            Restrictions.or(Restrictions.isNull("smtIkutiSettinganDisini"),
                                    Restrictions.eq("smtIkutiSettinganDisini", Boolean.FALSE)),
                            Restrictions.and(rentangSettingIni, dalamRentangJenisKegiatan
                                    ? Restrictions.sqlRestriction("1=1") : Restrictions.sqlRestriction("1=0")))));
        }

        return criteria;
    }

    /** Sengaja tidak melakukan apa pun — lihat catatan session-ownership pada javadoc kelas. */
    private static void closeSessionSafely(Session session) {
        // PENTING: Dikosongkan agar tidak terjadi "org.hibernate.SessionException: Session is closed!"
        // Karena method di Helper ini hanya MENERIMA session dari class Parent (PembayaranUtilHelper),
        // maka HANYA class Parent tersebut yang berhak melakukan session.close() atau session.disconnect()
        // pada akhir proses iterasi besarnya. 
    }

    /** @return {@code true} bila {@code detailBiaya} sudah ada tapi relasi {@code detailSettingBiaya}/{@code settingBiaya}-nya berbeda dari {@code dsb}/{@code sb} yang diberikan (perlu di-update). */
    private static boolean isDetailBiayaNeedsUpdate(DetailBiaya detailBiaya, DetailSettingBiaya dsb, SettingBiaya sb) {
        if (detailBiaya == null) {
            return false;
        }
        boolean dsbDiff = dsb != null && !samaId(detailBiaya.getDetailSettingBiaya(), dsb);
        
        boolean sbDiff = sb != null && !samaId(detailBiaya.getSettingBiaya(), sb);
                         
        return dsbDiff || sbDiff;
    }

    /**
     * Menyimpan/memperbarui {@code entity}, membuka transaksi lokal sendiri bila belum ada transaksi
     * aktif pada {@code session} (dan meng-commit/rollback transaksi lokal itu saja — transaksi milik
     * pemanggil tidak disentuh). Melempar {@link RuntimeException} yang membungkus kegagalan asli
     * bila terjadi error.
     */
    private static void saveOrUpdateEntity(Session session, Object entity, boolean isUpdate) {
        if (session == null || entity == null) {
            return;
        }
        Transaction transaction = null;
        boolean localTransaction = false;
        try {
            transaction = session.getTransaction();
            if (transaction == null || !transaction.isActive()) {
                transaction = session.beginTransaction();
                localTransaction = true;
            }
            if (isUpdate) {
                session.update(entity);
            } else {
                session.save(entity);
            }
            if (localTransaction && transaction != null && transaction.isActive()) {
                transaction.commit();
            }
        } catch (Exception e) {
            if (localTransaction && transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/SetingBiayaHelper.java:171");
                }
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Menyalin template jadwal bulanan milik SettingBiaya ke DetailBiaya mahasiswa.
     * Template dibuat oleh editor bulanan yang dibuka dari panel Setting Biaya dan
     * sengaja tidak terikat ke SettingBiayaDetail (mahasiswa) tertentu.
     */
    @SuppressWarnings("unchecked")
    private static void sinkronkanPengaturanBulananDariTemplate(Session session, DetailBiaya tujuan,
            SettingBiayaDetail binding, DetailSettingBiaya detailSettingBiaya, Integer semester) {
        if (session == null || tujuan == null || tujuan.getId() == null || binding == null
                || binding.getSettingBiaya() == null || binding.getSettingBiaya().getId() == null
                || detailSettingBiaya == null || detailSettingBiaya.getId() == null
                || binding.getSettingBiaya().getJenisKegiatan() == null
                || !Boolean.TRUE.equals(binding.getSettingBiaya().getJenisKegiatan().getHanyaBerupaAngsuran())) {
            return;
        }

        Long settingBiayaId = binding.getSettingBiaya().getId();
        Criteria templateCriteria = session.createCriteria(DetailBiaya.class)
                .createAlias("settingBiaya", "settingBiayaTemplateBulanan")
                .createAlias("detailSettingBiaya", "detailSettingBiayaTemplateBulanan")
                .add(Restrictions.eq("settingBiayaTemplateBulanan.id", settingBiayaId))
                .add(Restrictions.eq("detailSettingBiayaTemplateBulanan.id", detailSettingBiaya.getId()))
                .add(semester == null ? Restrictions.isNull("semester") : Restrictions.eq("semester", semester))
                .add(Restrictions.isNull("settingBiayaDetail"))
                .add(Restrictions.eq("nama", "Template Bulanan Setting Biaya " + settingBiayaId))
                .add(eqAtauNull("jurusan", tujuan.getJurusan()))
                .add(eqAtauKosong("program", tujuan.getProgram()))
                .addOrder(Order.desc("id")).setMaxResults(1);
        DetailBiaya template = (DetailBiaya) templateCriteria.uniqueResult();
        if (template == null) {
            return;
        }

        List<PengaturanPembayaranBulanan> sumber = session.createCriteria(PengaturanPembayaranBulanan.class)
                .createAlias("detailBiaya", "detailBiayaSumberBulanan")
                .add(Restrictions.eq("detailBiayaSumberBulanan.id", template.getId()))
                .addOrder(Order.asc("bulan")).list();
        for (PengaturanPembayaranBulanan asal : sumber) {
            PengaturanPembayaranBulanan target = (PengaturanPembayaranBulanan) session
                    .createCriteria(PengaturanPembayaranBulanan.class)
                    .createAlias("detailBiaya", "detailBiayaTujuanBulanan")
                    .add(Restrictions.eq("detailBiayaTujuanBulanan.id", tujuan.getId()))
                    .add(Restrictions.eq("bulan", asal.getBulan())).setMaxResults(1).uniqueResult();
            boolean baru = target == null;
            if (baru) {
                target = new PengaturanPembayaranBulanan();
                target.setDetailBiaya(tujuan);
                target.setBulan(asal.getBulan());
            }
            target.setNominal(asal.getNominal());
            target.setPersentase(asal.getPersentase());
            target.setDeadline(asal.getDeadline());
            target.setAktif(asal.getAktif());
            target.setKeterangan(asal.getKeterangan());
            target.setDikalikanDenganKondisiKhusus(asal.getDikalikanDenganKondisiKhusus());
            target.setTetapDitampilkanWalaupunNol(asal.getTetapDitampilkanWalaupunNol());
            target.setTanggalTagihanSelaluDibuatAwalBulan(asal.getTanggalTagihanSelaluDibuatAwalBulan());
            saveOrUpdateEntity(session, target, !baru);
        }
    }

    // ===================================================================================
    // PUBLIC METHODS
    // ===================================================================================

	/** Pemilih kanonis untuk seluruh query legacy pada helper billing. */
	public static SettingBiaya getSettingBiayaTerpilih(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi,
			GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan, String program,
			String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta,
			String nimMahasiswa, boolean gunakanBiayaDefault) {
		Criteria criteria = createSettingBiayaCriteria(session, ta, jenisKegiatan, semester, false,
				Boolean.valueOf(gunakanBiayaDefault)).addOrder(Order.desc("ta")).addOrder(Order.desc("id"));
		List<SettingBiaya> kandidat = ConstantValues.simpleList(criteria, SettingBiaya.class);
		kandidat = SettingBiayaMahasiswaSelector.saringDanPrioritaskan(session, kandidat, nimMahasiswa);
		String[] properties = new String[] { "statusMahasiswa", "kelamin", "afiliasiCalonMahasiswa", "program",
				"angkatan", "jenjang", "statusAwalMahasiswa", "jenisSeleksi", "gelombangPendaftaran", "paket",
				"jurusan" };
		Object[] datas = new Object[] { statusMahasiswa, kelamin, afiliasiCalonMahasiswa, program, angkatan, jenjang,
				statusAwalMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan };
		return SettingBiayaMahasiswaSelector.pilihSatuDenganPrioritas(kandidat, properties, datas);
	}

    /** Seperti {@link #getItemBiaya(Session, Integer, Jenjang, Integer, JenisKegiatan, StatusAwalMahasiswa, StatusMahasiswa, JenisSeleksi, GelombangPendaftaran, Paket, Jurusan, String, String, AfiliasiCalonMahasiswa, Integer, String)} dengan {@code nimMahasiswa=null}. */
    public static List<ItemBiaya> getItemBiaya(Session session, Integer angkatan, Jenjang jenjang, Integer semester,
            JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa, StatusMahasiswa statusMahasiswa,
            JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan,
            String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta) {
		return getItemBiaya(session, angkatan, jenjang, semester, jenisKegiatan, statusAwalMahasiswa,
				statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program, kelamin,
				afiliasiCalonMahasiswa, ta, null);
	}

	/**
	 * Menentukan {@link ItemBiaya} apa saja (tanpa membuat baris tagihan) yang berlaku untuk profil
	 * yang diberikan pada jalur "cohort": mencari kandidat {@link SettingBiaya} lewat
	 * {@link #createSettingBiayaCriteria}, memprioritaskan via
	 * {@link SettingBiayaMahasiswaSelector}, lalu mengembalikan daftar item biaya unik pada
	 * {@link DetailSettingBiaya} di bawah {@code SettingBiaya} terpilih.
	 *
	 * @param nimMahasiswa NIM mahasiswa (untuk pengecekan pengecualian dan prioritas per-NIM), boleh
	 *                     {@code null}
	 * @return daftar item biaya yang berlaku; list kosong bila tidak ada {@code SettingBiaya} yang
	 *         cocok; {@code null} bila {@code SettingBiaya} yang cocok mengecualikan {@code nimMahasiswa}
	 */
	public static List<ItemBiaya> getItemBiaya(Session session, Integer angkatan, Jenjang jenjang, Integer semester,
			JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa, StatusMahasiswa statusMahasiswa,
			JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan,
			String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta,
			String nimMahasiswa) {

        try {
            Criteria criteria = createSettingBiayaCriteria(session, ta, jenisKegiatan, semester, false, false)
                    .addOrder(Order.desc("ta")).addOrder(Order.asc("statusMahasiswa")).addOrder(Order.asc("kelamin"))
                    .addOrder(Order.asc("paket")).addOrder(Order.asc("gelombangPendaftaran")).addOrder(Order.asc("jenjang"))
                    .addOrder(Order.asc("angkatan")).addOrder(Order.asc("jenisKegiatan"))
                    .addOrder(Order.asc("statusAwalMahasiswa")).addOrder(Order.asc("jenisSeleksi"))
                    .addOrder(Order.asc("jurusan")).addOrder(Order.asc("program"));

            List<SettingBiaya> settingBiayas = ConstantValues.simpleList(criteria, SettingBiaya.class);
			settingBiayas = SettingBiayaMahasiswaSelector.saringDanPrioritaskan(session, settingBiayas,
					nimMahasiswa);

            String[] properties = new String[] { "statusMahasiswa", "kelamin", "afiliasiCalonMahasiswa", "program",
                    "angkatan", "jenjang", "statusAwalMahasiswa", "jenisSeleksi", "gelombangPendaftaran", "paket", "jurusan" };
            Object[] datas = new Object[] { statusMahasiswa, kelamin, afiliasiCalonMahasiswa, program, angkatan, jenjang,
                    statusAwalMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan };

            SettingBiaya settingBiaya = SettingBiayaMahasiswaSelector.pilihSatuDenganPrioritas(settingBiayas,
                    properties, datas);

            if (settingBiaya == null) {
                return new ArrayList<ItemBiaya>();
            }
			if (settingBiaya.isMahasiswaDikecualikan(nimMahasiswa)) {
				return null;
			}

            Criteria dsbCriteria = session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya");
            applyItemBiayaFilters(dsbCriteria, semester, false);
            dsbCriteria.setProjection(Projections.groupProperty("itemBiaya.id"));
            dsbCriteria.add(Restrictions.eq("settingBiaya", settingBiaya));

            List<ItemBiaya> itemBiayas = ConstantValues.simpleList(dsbCriteria, ItemBiaya.class, false);
            return itemBiayas;

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/SetingBiayaHelper.java:217");
            return new ArrayList<ItemBiaya>();
        } finally {
            closeSessionSafely(session);
        }
    }

    /**
     * Jalur "khusus per mahasiswa" untuk calon mahasiswa: mencari {@link SettingBiayaDetail} yang
     * menautkan {@code biodataCalonMahasiswa} langsung ke satu {@link SettingBiaya} ber-flag
     * {@code khususBuatMahasiswaTertentu}, lalu merealisasikannya jadi baris {@link DetailBiaya}
     * lewat {@link #getDefaultSettingBiaya(Session, SettingBiayaDetail, Integer, BiodataCalonMahasiswa)}.
     *
     * @return daftar {@link DetailBiaya}; {@code null} bila tidak ada binding khusus untuk calon
     *         mahasiswa ini (pemanggil sebaiknya lanjut ke jalur cohort umum); list kosong (lewat
     *         {@link PengecualianTagihanList#kosong()}) bila binding ditemukan tapi NIM-nya
     *         dikecualikan dari {@code SettingBiaya} tersebut
     */
    public static List<DetailBiaya> getDetailBiayaDefault(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa,
            JenisKegiatan jenisKegiatan, Integer semester, Integer ta) {
        
        try {
            int smtValue = (semester == null ? 0 : semester);
            SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) ConstantValues.simpleObject(
                    session.createCriteria(SettingBiayaDetail.class)
                            .add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
                            .createAlias("settingBiaya", "settingBiaya")
                            .add(Restrictions.le("settingBiaya.ta", ta))
                            .add(Restrictions.eq("settingBiaya.khususBuatMahasiswaTertentu", true))
                            .add(Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
                            .add(Restrictions.or(Restrictions.isNull("settingBiaya.minSmt"), Restrictions.le("settingBiaya.minSmt", smtValue)))
                            .add(Restrictions.or(Restrictions.isNull("settingBiaya.maxSmt"), Restrictions.ge("settingBiaya.maxSmt", smtValue)))
                            .add(Restrictions.or(Restrictions.isNull("minSmt"), Restrictions.le("minSmt", smtValue)))
                            .add(Restrictions.or(Restrictions.isNull("maxSmt"), Restrictions.ge("maxSmt", smtValue)))
                            .addOrder(Order.asc("settingBiaya.prioritas"))
                            .addOrder(Order.desc("settingBiaya.ta")).addOrder(Order.desc("id")).setMaxResults(1),
                    SettingBiayaDetail.class);

            if (settingBiayaDetail != null) {
				if (settingBiayaDetail.getSettingBiaya() != null
						&& settingBiayaDetail.getSettingBiaya().isMahasiswaDikecualikan(
								biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getNim())) {
					return PengecualianTagihanList.kosong();
				}
                return getDefaultSettingBiaya(session, settingBiayaDetail, semester, biodataCalonMahasiswa);
            }
            return null;
        } finally {
            closeSessionSafely(session);
        }
    }

    /**
     * Jalur "khusus per mahasiswa" untuk mahasiswa aktif: setara dengan
     * {@link #getDetailBiayaDefault(Session, BiodataCalonMahasiswa, JenisKegiatan, Integer, Integer)}
     * tapi mencari binding {@link SettingBiayaDetail} berdasarkan {@code mahasiswa}.
     *
     * @return daftar {@link DetailBiaya}; {@code null} bila tidak ada binding khusus (lanjut ke
     *         jalur cohort umum); list kosong bila binding ditemukan tapi NIM dikecualikan
     */
    public static List<DetailBiaya> getDetailBiayaDefault(Session session, Mahasiswa mahasiswa,
            JenisKegiatan jenisKegiatan, Integer semester, Integer ta) {

        try {
            int smtValue = (semester == null ? 0 : semester);
            System.out.println("[TAGIHAN-DEBUG] ==> getDetailBiayaDefault(Mahasiswa) mahasiswaId="
                    + (mahasiswa == null ? "null" : mahasiswa.getId()) + " nim="
                    + (mahasiswa == null ? "null" : mahasiswa.getNim()) + " jenisKegiatan="
                    + (jenisKegiatan == null ? "null" : jenisKegiatan.getId() + "-" + jenisKegiatan.getNamaKegiatan())
                    + " semester=" + semester + " ta=" + ta);
            SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) ConstantValues.simpleObject(
                    session.createCriteria(SettingBiayaDetail.class)
                            .add(Restrictions.eq("mahasiswa", mahasiswa))
                            .createAlias("settingBiaya", "settingBiaya")
                            .add(Restrictions.le("settingBiaya.ta", ta))
                            .add(Restrictions.eq("settingBiaya.khususBuatMahasiswaTertentu", true))
                            .add(Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
                            .add(Restrictions.or(Restrictions.isNull("settingBiaya.minSmt"), Restrictions.le("settingBiaya.minSmt", smtValue)))
                            .add(Restrictions.or(Restrictions.isNull("settingBiaya.maxSmt"), Restrictions.ge("settingBiaya.maxSmt", smtValue)))
                            .add(Restrictions.or(Restrictions.isNull("minSmt"), Restrictions.le("minSmt", smtValue)))
                            .add(Restrictions.or(Restrictions.isNull("maxSmt"), Restrictions.ge("maxSmt", smtValue)))
                            .addOrder(Order.asc("settingBiaya.prioritas"))
                            .addOrder(Order.desc("settingBiaya.ta")).addOrder(Order.desc("id")).setMaxResults(1),
                    SettingBiayaDetail.class);

            System.out.println("[TAGIHAN-DEBUG] getDetailBiayaDefault(Mahasiswa): binding \"khusus per mahasiswa\" "
                    + (settingBiayaDetail == null ? "TIDAK DITEMUKAN -> lanjut ke pencarian cohort umum"
                            : "DITEMUKAN settingBiayaDetailId=" + settingBiayaDetail.getId() + " settingBiayaId="
                                    + (settingBiayaDetail.getSettingBiaya() == null ? "null"
                                            : settingBiayaDetail.getSettingBiaya().getId())));

            if (settingBiayaDetail != null) {
				if (settingBiayaDetail.getSettingBiaya() != null
						&& settingBiayaDetail.getSettingBiaya().isMahasiswaDikecualikan(mahasiswa == null ? null : mahasiswa.getNim())) {
					return PengecualianTagihanList.kosong();
				}
                return getDefaultSettingBiaya(session, settingBiayaDetail, semester, mahasiswa);
            }
            return null;
        } finally {
            closeSessionSafely(session);
        }
    }

    /** Seperti overload dengan {@code nimMahasiswa}, dipanggil dengan {@code nimMahasiswa=null}. */
    public static List<DetailBiaya> getDetailBiayaDefault(Session session, Integer angkatan, Jenjang jenjang,
            Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
            StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
            Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
            Integer ta) {
		return getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan, statusAwalMahasiswa,
				statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program, kelamin,
				afiliasiCalonMahasiswa, ta, null);
	}

	/**
	 * Jalur "cohort" utama: mencari kandidat {@link SettingBiaya} yang cocok dengan seluruh profil
	 * (lewat {@link #createSettingBiayaCriteria} + {@link SettingBiayaMahasiswaSelector}), lalu
	 * merealisasikannya jadi baris {@link DetailBiaya} lewat
	 * {@link #getDefaultSettingBiaya(Session, SettingBiaya, Integer, Jenjang, Integer, JenisKegiatan,
	 * StatusAwalMahasiswa, StatusMahasiswa, JenisSeleksi, GelombangPendaftaran, Paket, Jurusan,
	 * String, String, AfiliasiCalonMahasiswa)}.
	 *
	 * @param nimMahasiswa NIM untuk pengecekan pengecualian dan prioritas per-NIM, boleh {@code null}
	 * @return daftar {@link DetailBiaya}; list kosong bila tidak ada {@code SettingBiaya} yang
	 *         cocok atau bila terjadi kesalahan internal; list kosong (lewat
	 *         {@link PengecualianTagihanList#kosong()}) bila {@code SettingBiaya} cocok tapi
	 *         mengecualikan {@code nimMahasiswa}
	 */
	public static List<DetailBiaya> getDetailBiayaDefault(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
			Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
			Integer ta, String nimMahasiswa) {

        try {
            System.out.println("[TAGIHAN-DEBUG] ==> getDetailBiayaDefault(cohort) angkatan=" + angkatan + " jenjang="
                    + (jenjang == null ? "null" : jenjang.getId() + "-" + jenjang.getNama()) + " semester=" + semester
                    + " jenisKegiatan="
                    + (jenisKegiatan == null ? "null" : jenisKegiatan.getId() + "-" + jenisKegiatan.getNamaKegiatan())
                    + " statusAwalMahasiswa="
                    + (statusAwalMahasiswa == null ? "null" : statusAwalMahasiswa.getId() + "-" + statusAwalMahasiswa.getNama())
                    + " statusMahasiswa="
                    + (statusMahasiswa == null ? "null" : statusMahasiswa.getId() + "-" + statusMahasiswa.getNama())
                    + " jenisSeleksi=" + (jenisSeleksi == null ? "null" : jenisSeleksi.getId() + "-" + jenisSeleksi.getNama())
                    + " gelombangPendaftaran=" + (gelombangPendaftaran == null ? "null" : gelombangPendaftaran.getId())
                    + " paket=" + (paket == null ? "null" : paket.getId() + "-" + paket.getNama())
                    + " jurusan=" + (jurusan == null ? "null" : jurusan.getId() + "-" + jurusan.getNama())
                    + " program=" + program + " kelamin=" + kelamin + " afiliasiCalonMahasiswa="
                    + (afiliasiCalonMahasiswa == null ? "null" : afiliasiCalonMahasiswa.getId()) + " ta=" + ta);

            Criteria criteria = createSettingBiayaCriteria(session, ta, jenisKegiatan, semester, false, true)
                    .addOrder(Order.desc("ta")).addOrder(Order.asc("jenisSeleksi")).addOrder(Order.asc("statusMahasiswa"))
                    .addOrder(Order.asc("kelamin")).addOrder(Order.asc("program")).addOrder(Order.asc("paket"))
                    .addOrder(Order.asc("gelombangPendaftaran")).addOrder(Order.asc("jenjang"))
                    .addOrder(Order.asc("angkatan")).addOrder(Order.asc("jenisKegiatan"))
                    .addOrder(Order.asc("statusAwalMahasiswa")).addOrder(Order.asc("jurusan"));

            List<SettingBiaya> settingBiayas = ConstantValues.simpleList(criteria, SettingBiaya.class);
			settingBiayas = SettingBiayaMahasiswaSelector.saringDanPrioritaskan(session, settingBiayas,
					nimMahasiswa);

            StringBuilder idSettingBiayaSb = new StringBuilder();
            for (SettingBiaya sbCandidate : settingBiayas) {
                if (idSettingBiayaSb.length() > 0) {
                    idSettingBiayaSb.append(",");
                }
                idSettingBiayaSb.append(sbCandidate == null ? "null" : sbCandidate.getId());
            }
            System.out.println("[TAGIHAN-DEBUG] getDetailBiayaDefault(cohort): kandidat SettingBiaya (lolos filter kasar jenisKegiatan+semester+ta) = "
                    + settingBiayas.size() + " baris -> id: [" + idSettingBiayaSb + "]");

            String[] properties = new String[] { "jenisSeleksi", "statusMahasiswa", "kelamin", "afiliasiCalonMahasiswa",
                    "program", "angkatan", "jenjang", "statusAwalMahasiswa", "gelombangPendaftaran", "paket", "jurusan" };
            Object[] datas = new Object[] { jenisSeleksi, statusMahasiswa, kelamin, afiliasiCalonMahasiswa, program,
                    angkatan, jenjang, statusAwalMahasiswa, gelombangPendaftaran, paket, jurusan };

            SettingBiaya settingBiaya = SettingBiayaMahasiswaSelector.pilihSatuDenganPrioritas(settingBiayas,
                    properties, datas);

            System.out.println("[TAGIHAN-DEBUG] getDetailBiayaDefault(cohort): SettingBiaya TERPILIH = "
                    + (settingBiaya == null ? "TIDAK ADA YANG COCOK (kembalikan list kosong)" : "id=" + settingBiaya.getId()));

            if (settingBiaya == null) {
                return new ArrayList<DetailBiaya>();
            }
			if (settingBiaya.isMahasiswaDikecualikan(nimMahasiswa)) {
				System.out.println("[TAGIHAN-DEBUG] SettingBiaya id=" + settingBiaya.getId()
						+ " tidak berlaku untuk NIM " + nimMahasiswa + " (daftar pengecualian).");
				return PengecualianTagihanList.kosong();
			}
            return getDefaultSettingBiaya(session, settingBiaya, angkatan, jenjang, semester,
                    jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan,
                    program, kelamin, afiliasiCalonMahasiswa);
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/SetingBiayaHelper.java:310");
            return new ArrayList<DetailBiaya>();
        } finally {
            closeSessionSafely(session);
        }
    }

    /**
     * Merealisasikan satu {@link SettingBiaya} (jalur cohort) menjadi baris {@link DetailBiaya} untuk
     * setiap {@link DetailSettingBiaya} (item+bayarKe) di bawahnya yang lolos filter
     * {@link #applyItemBiayaFilters}. Untuk tiap item, dicari baris {@code DetailBiaya} yang sudah
     * ada lewat tiga lapis pencarian (ketat → longgar → jaring pengaman terakhir, lihat javadoc
     * kelas); bila tetap tidak ditemukan, baris baru dibuat. Baris yang sudah ada tapi relasi
     * {@code detailSettingBiaya}/{@code settingBiaya}-nya berbeda ikut diperbarui.
     *
     * @return daftar {@link DetailBiaya} (baru maupun hasil pakai-ulang) untuk seluruh item biaya di
     *         bawah {@code settingBiaya}; list kosong bila {@code settingBiaya} {@code null}/belum
     *         punya id
     */
    public static List<DetailBiaya> getDefaultSettingBiaya(Session session, SettingBiaya settingBiaya, Integer angkatan,
            Jenjang jenjang, Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
            StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
            Paket paket, Jurusan jurusan, String program, String kelamin,
            AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
        
        if (settingBiaya == null || settingBiaya.getId() == null) {
            // settingBiaya transient/tak ber-id (mis. dioper lintas-thread dari session lain
            // yang tidak sinkron) -> tidak aman dijadikan parameter Criteria di session ini.
            return new ArrayList<DetailBiaya>();
        }

        try {
            // PERBAIKAN (Error A - TransientObjectException): JANGAN bind objek entity settingBiaya
            // langsung ke Criteria (Restrictions.eq("settingBiaya", settingBiaya)). Method ini dipanggil
            // dari thread latar (TagihanUIBuilder via FutureTask) dengan objek settingBiaya yang bisa saja
            // berasal dari luar session ini -> query berbasis ID lewat alias, bukan objek entity, supaya
            // tidak bergantung pada status attachment objek yang dioper lintas-thread/session.
            Criteria dsbCriteria = session.createCriteria(DetailSettingBiaya.class)
                    .createAlias("itemBiaya", "itemBiaya")
                    .createAlias("settingBiaya", "settingBiayaAlias");
            applyItemBiayaFilters(dsbCriteria, semester, true);
            dsbCriteria.add(Restrictions.eq("settingBiayaAlias.id", settingBiaya.getId()));

            List<DetailSettingBiaya> detailSettingBiayas = ConstantValues.simpleList(dsbCriteria, DetailSettingBiaya.class);
            List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();

            System.out.println("[TAGIHAN-DEBUG] ==> getDefaultSettingBiaya(cohort) settingBiayaId=" + settingBiaya.getId()
                    + " angkatan=" + angkatan + " jenjang=" + (jenjang == null ? "null" : jenjang.getId())
                    + " semester=" + semester + " jurusan=" + (jurusan == null ? "null" : jurusan.getId())
                    + " program=" + program + " | jumlah DetailSettingBiaya (item+bayarKe) yg diproses = "
                    + detailSettingBiayas.size());

            for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {
                // PERBAIKAN (Error B - TransientObjectException: DetailSettingBiaya): sama seperti
                // settingBiaya di atas, detailSettingBiaya di sini berasal dari ConstantValues.simpleList()
                // yang bisa jadi ambil objek dari MemoryCache (bukan hasil load session ini) -> JANGAN
                // bind objek entity-nya langsung ke Criteria (Restrictions.eq("detailSettingBiaya", ..)),
                // ikat lewat alias+id saja. Bila ternyata benar-benar transient (id null, mis. cache basi),
                // lewati baris ini saja (fallback aman) drpd melempar TransientObjectException ke wizard bayar.
                if (detailSettingBiaya == null || detailSettingBiaya.getId() == null) {
                    continue;
                }
                boolean biayaPerProdi = Boolean.TRUE.equals(settingBiaya.getTampilkanPerProdi());
                Criteria dbCriteria1 = session.createCriteria(DetailBiaya.class)
                        .createAlias("settingBiaya", "settingBiayaAlias1")
                        .createAlias("detailSettingBiaya", "detailSettingBiayaAlias1")
                        .add(semester == null ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.eq("semester", semester))
                        .add(Restrictions.eq("detailSettingBiayaAlias1.id", detailSettingBiaya.getId()))
                        .add(Restrictions.eq("settingBiayaAlias1.id", settingBiaya.getId()))
                        .add(biayaPerProdi
                                ? (jurusan == null ? Restrictions.isNull("jurusan")
                                        : Restrictions.eq("jurusan", jurusan))
                                : eqAtauNull("jurusan", jurusan))
                        .add(eqAtauKosong("program", program))
                        .setMaxResults(1).addOrder(Order.desc("id"));

                DetailBiaya detailBiaya = (DetailBiaya) dbCriteria1.uniqueResult();

                System.out.println("[TAGIHAN-DEBUG]   - DetailSettingBiaya id=" + detailSettingBiaya.getId()
                        + " itemBiaya=" + detailSettingBiaya.getItemBiaya().getId() + "-"
                        + detailSettingBiaya.getItemBiaya().getNama() + " bayarKe=" + detailSettingBiaya.getBayarKe()
                        + " | dbCriteria1 (settingBiaya+detailSettingBiaya id, exact) = "
                        + (detailBiaya == null ? "TIDAK KETEMU" : "KETEMU detailBiayaId=" + detailBiaya.getId()));

                if (detailBiaya == null) {
                    Criteria dbCriteria2 = session.createCriteria(DetailBiaya.class)
                            .add(semester == null ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.eq("semester", semester))
                            .add(Restrictions.eq("bayarKe", detailSettingBiaya.getBayarKe()))
                            .add(Restrictions.eq("itemBiaya", detailSettingBiaya.getItemBiaya()))
                            // PERBAIKAN (duplikat "Biaya Semester" bertambah tiap dibuka ulang): statusAwalMahasiswa
                            // & statusMahasiswa DIHITUNG ULANG tiap kali method ini dipanggil (bisa berubah krn
                            // sinkron KRS/cuti/kampus-merdeka -- lihat PembayaranUtilHelper.currentStatusMahasiswa),
                            // jadi TIDAK boleh jadi syarat cocok/tidak-cocok yang KETAT di sini seperti sebelumnya
                            // (eq-atau-isNull) -- sedikit saja status hasil hitungan berbeda dari saat baris lama
                            // dibuat, baris lama itu dianggap "tidak ketemu" dan DetailBiaya BARU (duplikat) malah
                            // dibuat. Konsisten dgn kelamin/jenisSeleksi/gelombangPendaftaran/paket di bawah ini
                            // (juga bukan penentu identitas tagihan), pakai pola longgar: baris lama yg statusnya
                            // masih kosong (null) tetap cocok apa pun status yang dihitung sekarang. Pemilihan
                            // SettingBiaya/DetailSettingBiaya yang PALING SESUAI status mahasiswa sudah dilakukan
                            // di jenjang lebih atas (SetingBiayaAction.getItemBiaya), jadi tidak hilang differensiasi
                            // biaya per status di sini -- ini murni pencarian "apakah tagihan item ini utk periode
                            // ini SUDAH ada", bukan penentu besaran biayanya.
                            .add(Restrictions.or(Restrictions.isNull("statusAwalMahasiswa"), Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa)))
                            .add(Restrictions.or(Restrictions.isNull("statusMahasiswa"), Restrictions.eq("statusMahasiswa", statusMahasiswa)))
                            .add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program)))
                            .add(Restrictions.or(Restrictions.isNull("kelamin"), Restrictions.eq("kelamin", kelamin)))
                            .add(Restrictions.or(Restrictions.isNull("jenisSeleksi"), Restrictions.eq("jenisSeleksi", jenisSeleksi)))
                            .add(angkatan == null ? Restrictions.isNull("angkatan") : Restrictions.eq("angkatan", angkatan))
                            .add(jenjang == null ? Restrictions.isNull("jenjang") : Restrictions.eq("jenjang", jenjang))
                            .add(jenisKegiatan == null ? Restrictions.isNull("jenisKegiatan") : Restrictions.eq("jenisKegiatan", jenisKegiatan))
                            .add(jurusan == null ? Restrictions.isNull("jurusan") : Restrictions.eq("jurusan", jurusan))
                            .add(afiliasiCalonMahasiswa == null ? Restrictions.isNull("afiliasiCalonMahasiswa") : Restrictions.eq("afiliasiCalonMahasiswa", afiliasiCalonMahasiswa))
                            .add(Restrictions.or(Restrictions.isNull("gelombangPendaftaran"), Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)))
                            .add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", paket)))
                            .addOrder(Order.desc("id"))
                            .setMaxResults(1);

                    // Pencarian longgar hanya untuk data lama milik SettingBiaya yang sama.
                    // Tanpa batas ini, ItemBiaya serupa dari setting berprioritas lain dapat
                    // dipakai ulang lalu relasinya dipindahkan ke setting terpilih.
                    dbCriteria2 = batasiKeSettingBiayaTerpilih(dbCriteria2, settingBiaya, "Longgar");

                    detailBiaya = (DetailBiaya) dbCriteria2.uniqueResult();

                    System.out.println("[TAGIHAN-DEBUG]     dbCriteria2 (bayarKe+itemBiaya, longgar thd status/dll) = "
                            + (detailBiaya == null ? "TIDAK KETEMU" : "KETEMU detailBiayaId=" + detailBiaya.getId()));
                }

                // JARING PENGAMAN TERAKHIR (antisipasi mendalam thd duplikat "Biaya Semester"
                // dkk): dbCriteria1/dbCriteria2 di atas MASIH bisa "tidak ketemu" krn dua sebab
                // lain di luar status mahasiswa yg sudah diperbaiki: (1) RACE CONDITION -- dua
                // permintaan (klik ganda "Proses Bayar"/dua tab browser/refresh serentak) sama-
                // sama mengecek "belum ada" sebelum salah satunya sempat menyimpan, lalu KEDUANYA
                // membuat baris baru; (2) ada 2 baris DetailSettingBiaya berbeda utk item+bayarKe
                // yg SAMA di bawah SettingBiaya ini (biasanya salah konfigurasi admin di Setting
                // Biaya). Sebelum benar-benar membuat baris baru, cek SEKALI LAGI dgn kunci PALING
                // INTI saja (item+bayarKe+semester+settingBiaya, TANPA syarat status/jurusan/dll
                // yg justru rawan meleset) -- kalau ternyata SUDAH ada (baru saja dibuat panggilan
                // lain, atau memang sudah ada tapi terlewat kriteria di atas), PAKAI itu, JANGAN
                // buat baru, dan catat sbg audit supaya admin/pengembang bisa pantau apakah
                // skenario ini masih terjadi (idealnya juga dicegah lewat UNIQUE CONSTRAINT di
                // database sbg lapis pertahanan terakhir -- lihat catatan di PesanFormalHelper
                // terkait, silakan diskusikan dgn tim DBA).
                if (detailBiaya == null) {
                    Criteria jaringPengaman = session.createCriteria(DetailBiaya.class)
                            .createAlias("settingBiaya", "settingBiayaAliasJP")
                            .add(semester == null ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.eq("semester", semester))
                            .add(Restrictions.eq("bayarKe", detailSettingBiaya.getBayarKe()))
                            .add(Restrictions.eq("itemBiaya", detailSettingBiaya.getItemBiaya()))
                            .add(Restrictions.eq("settingBiayaAliasJP.id", settingBiaya.getId()));

                    // Nominal per prodi tidak boleh memakai DetailBiaya milik prodi lain. Tanpa
                    // filter ini, jaring pengaman dapat mengambil baris pertama (mis. Administrasi
                    // Negara Rp750.000) untuk mahasiswa Matematika yang semestinya Rp1.050.000.
                    if (biayaPerProdi) {
                        jaringPengaman.add(jurusan == null ? Restrictions.isNull("jurusan")
                                : Restrictions.eq("jurusan", jurusan));
                    }

                    DetailBiaya detailBiayaJaringPengaman = (DetailBiaya) jaringPengaman
                            .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

                    System.out.println("[TAGIHAN-DEBUG]     jaring pengaman terakhir (item+bayarKe+semester+settingBiaya saja) = "
                            + (detailBiayaJaringPengaman == null ? "TIDAK KETEMU -> akan buat DetailBiaya BARU"
                                    : "KETEMU detailBiayaId=" + detailBiayaJaringPengaman.getId() + " (dipakai ulang, BUKAN membuat baru)"));

                    if (detailBiayaJaringPengaman != null) {
                        System.out.println("[TAGIHAN-INFO] Duplikat DetailBiaya dicegah oleh jaring pengaman terakhir: item="
                                + detailSettingBiaya.getItemBiaya().getId() + " bayarKe="
                                + detailSettingBiaya.getBayarKe() + " semester=" + semester + " settingBiaya="
                                + settingBiaya.getId() + " detailBiayaDipakaiUlang="
                                + detailBiayaJaringPengaman.getId());
                        detailBiaya = detailBiayaJaringPengaman;
                    }
                }

                if (detailBiaya == null) {
                    System.out.println(
                            "[TAGIHAN-DEBUG]     => TIDAK ada baris cocok sama sekali -> MEMBUAT DetailBiaya BARU utk item="
                                    + detailSettingBiaya.getItemBiaya().getId() + " bayarKe="
                                    + detailSettingBiaya.getBayarKe());
                    detailBiaya = new DetailBiaya();
                    detailBiaya.setSemester(semester);
                    detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
                    detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
                    detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                    detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                    detailBiaya.setAfiliasiCalonMahasiswa(afiliasiCalonMahasiswa);
                    detailBiaya.setKelamin(kelamin);
                    detailBiaya.setAngkatan(angkatan);
                    detailBiaya.setJurusan(jurusan);
                    detailBiaya.setJenjang(jenjang);
                    detailBiaya.setGelombangPendaftaran(gelombangPendaftaran);
                    detailBiaya.setPaket(paket);
                    detailBiaya.setJenisSeleksi(jenisSeleksi);
                    detailBiaya.setProgram(program);
                    detailBiaya.setStatusAwalMahasiswa(statusAwalMahasiswa);
                    detailBiaya.setStatusMahasiswa(statusMahasiswa);
                    detailBiaya.setJenisKegiatan(jenisKegiatan);
                    detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());
                    detailBiaya.setSettingBiaya(settingBiaya);

                    saveOrUpdateEntity(session, detailBiaya, false);

                    System.out.println("[TAGIHAN-DEBUG]     => DetailBiaya BARU tersimpan dgn id=" + detailBiaya.getId());

                } else if (isDetailBiayaNeedsUpdate(detailBiaya, detailSettingBiaya, settingBiaya)) {
                    System.out.println("[TAGIHAN-DEBUG]     => DetailBiaya id=" + detailBiaya.getId()
                            + " DITEMUKAN, perlu di-UPDATE (isDetailBiayaNeedsUpdate=true) -- jurusan/kelamin/semester/detailSettingBiaya/settingBiaya disesuaikan");
                    detailBiaya.setJurusan(jurusan);
                    detailBiaya.setKelamin(kelamin);
                    detailBiaya.setSemester(semester);
                    detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                    detailBiaya.setSettingBiaya(settingBiaya);

                    saveOrUpdateEntity(session, detailBiaya, true);
                } else {
                    System.out.println("[TAGIHAN-DEBUG]     => DetailBiaya id=" + detailBiaya.getId()
                            + " DITEMUKAN dan sudah sesuai (dipakai apa adanya, TIDAK ada perubahan)");
                }

                detailBiaya.setSettingBiaya(settingBiaya);
                detailBiaya.setDefaultTanggalTagihan(detailSettingBiaya.getDefaultTanggalTagihan());
                detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                detailBiaya.setKeterangan(detailSettingBiaya.getDefaultKeterangan());
                detailBiayas.add(detailBiaya);
                System.out.println("[TAGIHAN-DEBUG]     => FINAL: DetailBiaya id=" + detailBiaya.getId()
                        + " (settingBiayaId=" + settingBiaya.getId() + ") ditambahkan ke hasil tagihan.");
            }

            return detailBiayas;
        } finally {
            closeSessionSafely(session);
        }
    }

    /**
     * Merealisasikan satu binding {@link SettingBiayaDetail} (jalur "khusus per mahasiswa") menjadi
     * baris {@link DetailBiaya} untuk {@code mahasiswa}, satu per {@link DetailSettingBiaya} di bawah
     * {@code SettingBiaya} terkait. Bila mahasiswa sudah lulus/keluar sebelum {@code semester} yang
     * diminta DAN jenis kegiatannya tidak berlaku untuk alumni, mengembalikan list kosong tanpa
     * memproses apa pun.
     *
     * @return daftar {@link DetailBiaya}; list kosong bila mahasiswa alumni yang tidak ditagih, atau
     *         bila terjadi kesalahan internal
     */
    public static List<DetailBiaya> getDefaultSettingBiaya(Session session, SettingBiayaDetail settingBiayaDetail,
            Integer semester, Mahasiswa mahasiswa) {
        List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();

        System.out.println("[TAGIHAN-DEBUG] ==> getDefaultSettingBiaya(SettingBiayaDetail, Mahasiswa) [jalur \"khusus per mahasiswa\"] mahasiswaId="
                + (mahasiswa == null ? "null" : mahasiswa.getId()) + " settingBiayaDetailId="
                + (settingBiayaDetail == null ? "null" : settingBiayaDetail.getId()) + " semester=" + semester);

        try {
            if (semester != null && mahasiswa.getStatusKeluar() != null
                    && ((mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester))) {
                if (settingBiayaDetail != null && settingBiayaDetail.getSettingBiaya() != null
                        && !settingBiayaDetail.getSettingBiaya().getJenisKegiatan().getTagihanJugaUntukAlumni()) {
                    return detailBiayas;
                }
            }

            Criteria dsbCriteria = session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya");
            applyItemBiayaFilters(dsbCriteria, semester, true);
            // PERBAIKAN (TransientObjectException): settingBiayaDetail.getSettingBiaya() bisa jadi
            // objek cache-derived (bukan hasil load session ini) -> ikat via id saja, bukan objek entity.
            dsbCriteria.createAlias("settingBiaya", "settingBiayaAliasDsb")
                    .add(Restrictions.eq("settingBiayaAliasDsb.id", settingBiayaDetail.getSettingBiaya().getId()));

            List<DetailSettingBiaya> detailSettingBiayas = ConstantValues.simpleList(dsbCriteria, DetailSettingBiaya.class);
            Jurusan jurusan = mahasiswa.getJurusan();

            for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {
                // PERBAIKAN (Error B - TransientObjectException: DetailSettingBiaya): detailSettingBiaya
                // di sini berasal dari ConstantValues.simpleList() yang bisa jadi ambil objek dari
                // MemoryCache (bukan hasil load session ini) -> JANGAN bind objek entity-nya langsung
                // ke Criteria, ikat lewat alias+id saja. Bila benar-benar transient (id null), lewati.
                if (detailSettingBiaya == null || detailSettingBiaya.getId() == null) {
                    continue;
                }
                DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
                        .createAlias("detailSettingBiaya", "detailSettingBiayaAlias2")
                        .createAlias("settingBiaya", "settingBiayaAlias2")
                        .add(semester == null ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.eq("semester", semester))
                        .add(Restrictions.eq("bayarKe", detailSettingBiaya.getBayarKe()))
                        .add(Restrictions.eq("detailSettingBiayaAlias2.id", detailSettingBiaya.getId()))
                        .add(Restrictions.eq("settingBiayaAlias2.id", settingBiayaDetail.getSettingBiaya().getId()))
                        .add(Restrictions.eq("settingBiayaDetail", settingBiayaDetail))
                        .add(Boolean.TRUE.equals(settingBiayaDetail.getSettingBiaya().getTampilkanPerProdi())
                                ? (jurusan == null ? Restrictions.isNull("jurusan")
                                        : Restrictions.eq("jurusan", jurusan))
                                : eqAtauNull("jurusan", jurusan))
                        .setMaxResults(1).addOrder(Order.desc("id"))
                        .uniqueResult();

                if (detailBiaya == null) {
                    Paket paket = null;
                    try {
                        BiodataCalonMahasiswa bcm = mahasiswa.getBiodataCalonMahasiswaData();
                        paket = bcm == null ? null : bcm.getPaket();
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SetingBiayaHelper.java:447");}

                    detailBiaya = new DetailBiaya();
                    detailBiaya.setSemester(semester);
                    detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
                    detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
                    detailBiaya.setPaket(paket);
                    detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
                    detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                    detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                    detailBiaya.setKelamin(detailSettingBiaya.getSettingBiaya().getKelamin());
                    detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
                    detailBiaya.setAngkatan(mahasiswa.getTahunangkatan());
                    detailBiaya.setJurusan(jurusan);
                    detailBiaya.setJenjang(mahasiswa.getJenjang());
                    detailBiaya.setGelombangPendaftaran(mahasiswa.getGelombangPendaftaran());
                    detailBiaya.setJenisSeleksi(mahasiswa.getJenisSeleksi());
                    detailBiaya.setProgram(mahasiswa.getProgram());
                    detailBiaya.setStatusAwalMahasiswa(mahasiswa.getStatusAwalMahasiswa());
                    detailBiaya.setJenisKegiatan(settingBiayaDetail.getSettingBiaya().getJenisKegiatan());
                    detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());
                    
                    saveOrUpdateEntity(session, detailBiaya, false);
                }
                detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
                detailBiaya.setJurusan(jurusan);
                detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
                detailBiaya.setDefaultTanggalTagihan(detailSettingBiaya.getDefaultTanggalTagihan());
                detailBiaya.setKeterangan(detailSettingBiaya.getDefaultKeterangan());
                detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                sinkronkanPengaturanBulananDariTemplate(session, detailBiaya, settingBiayaDetail,
                        detailSettingBiaya, semester);
                detailBiayas.add(detailBiaya);
            }

            return detailBiayas;
        } catch(Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/SetingBiayaHelper.java:482");
            return detailBiayas;
        } finally {
            closeSessionSafely(session);
        }
    }

    /**
     * Sama seperti {@link #getDefaultSettingBiaya(Session, SettingBiayaDetail, Integer, Mahasiswa)}
     * tapi untuk {@link BiodataCalonMahasiswa} (belum berstatus mahasiswa aktif) — jurusan diambil
     * dari prodi lulus (bila ada) atau prodi pilihan pertama.
     *
     * @return daftar {@link DetailBiaya} untuk seluruh item biaya di bawah binding tersebut
     */
    public static List<DetailBiaya> getDefaultSettingBiaya(Session session, SettingBiayaDetail settingBiayaDetail,
            Integer semester, BiodataCalonMahasiswa biodataCalonMahasiswa) {

        System.out.println("[TAGIHAN-DEBUG] ==> getDefaultSettingBiaya(SettingBiayaDetail, BiodataCalonMahasiswa) [jalur \"khusus per mahasiswa\"] biodataCalonMahasiswaId="
                + (biodataCalonMahasiswa == null ? "null" : biodataCalonMahasiswa.getId()) + " settingBiayaDetailId="
                + (settingBiayaDetail == null ? "null" : settingBiayaDetail.getId()) + " semester=" + semester);

        try {
            Criteria dsbCriteria = session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya");
            applyItemBiayaFilters(dsbCriteria, semester, true);
            // PERBAIKAN (TransientObjectException): settingBiayaDetail.getSettingBiaya() bisa jadi
            // objek cache-derived (bukan hasil load session ini) -> ikat via id saja, bukan objek entity.
            dsbCriteria.createAlias("settingBiaya", "settingBiayaAliasDsb")
                    .add(Restrictions.eq("settingBiayaAliasDsb.id", settingBiayaDetail.getSettingBiaya().getId()));

            List<DetailSettingBiaya> detailSettingBiayas = ConstantValues.simpleList(dsbCriteria, DetailSettingBiaya.class);
            Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1() : biodataCalonMahasiswa.getProdiLulus();

            List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
            for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {

                // PERBAIKAN (Error B - TransientObjectException: DetailSettingBiaya): detailSettingBiaya
                // di sini berasal dari ConstantValues.simpleList() yang bisa jadi ambil objek dari
                // MemoryCache (bukan hasil load session ini) -> JANGAN bind objek entity-nya langsung
                // ke Criteria, ikat lewat alias+id saja. Bila benar-benar transient (id null), lewati.
                if (detailSettingBiaya == null || detailSettingBiaya.getId() == null) {
                    continue;
                }
                DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
                        .createAlias("detailSettingBiaya", "detailSettingBiayaAlias3")
                        .createAlias("settingBiaya", "settingBiayaAlias3")
                        .add(semester == null ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.eq("semester", semester))
                        .add(Restrictions.eq("bayarKe", detailSettingBiaya.getBayarKe()))
                        .add(Restrictions.eq("detailSettingBiayaAlias3.id", detailSettingBiaya.getId()))
                        .add(Restrictions.eq("settingBiayaAlias3.id", settingBiayaDetail.getSettingBiaya().getId()))
                        .add(Boolean.TRUE.equals(settingBiayaDetail.getSettingBiaya().getTampilkanPerProdi())
                                ? (jurusan == null ? Restrictions.isNull("jurusan")
                                        : Restrictions.eq("jurusan", jurusan))
                                : eqAtauNull("jurusan", jurusan))
                        .add(Restrictions.eq("settingBiayaDetail", settingBiayaDetail))
                        .setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

                if (detailBiaya == null) {
                    detailBiaya = new DetailBiaya();
                    detailBiaya.setSemester(semester);
                    detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
                    detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
                    detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
                    detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                    detailBiaya.setKelamin(detailSettingBiaya.getSettingBiaya().getKelamin());
                    detailBiaya.setAfiliasiCalonMahasiswa(biodataCalonMahasiswa.getAfiliasiCalonMahasiswa());
                    detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                    detailBiaya.setPaket(biodataCalonMahasiswa.getPaket());
                    detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
                    detailBiaya.setAngkatan(biodataCalonMahasiswa.getTahun());
                    detailBiaya.setJurusan(jurusan);
                    detailBiaya.setJenjang(biodataCalonMahasiswa.getJenjang());
                    detailBiaya.setGelombangPendaftaran(biodataCalonMahasiswa.getGelombangPendaftaran());
                    detailBiaya.setJenisSeleksi(biodataCalonMahasiswa.getJenisSeleksi());
                    detailBiaya.setProgram(biodataCalonMahasiswa.getProgram());
                    detailBiaya.setStatusAwalMahasiswa(biodataCalonMahasiswa.getStatusAwalMahasiswa());
                    detailBiaya.setJenisKegiatan(settingBiayaDetail.getSettingBiaya().getJenisKegiatan());
                    detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());

                    saveOrUpdateEntity(session, detailBiaya, false);
                }
                
                detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
                detailBiaya.setJurusan(biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1() : biodataCalonMahasiswa.getProdiLulus());
                detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
                detailBiaya.setDefaultTanggalTagihan(detailSettingBiaya.getDefaultTanggalTagihan());
                detailBiaya.setKeterangan(detailSettingBiaya.getDefaultKeterangan());
                detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                sinkronkanPengaturanBulananDariTemplate(session, detailBiaya, settingBiayaDetail,
                        detailSettingBiaya, semester);
                detailBiayas.add(detailBiaya);
            }

            return detailBiayas;
        } finally {
            closeSessionSafely(session);
        }
    }

    /**
     * Varian jalur cohort yang HANYA menyertakan item biaya dengan {@code defaultBiaya > 0.1} (item
     * "bukan default/gratis"), memakai strategi pencarian baris {@link DetailBiaya} yang sudah ada
     * (ketat lalu longgar, tanpa jaring pengaman tambahan) sebelum membuat baris baru. NIM tidak
     * dipertimbangkan untuk pengecualian/prioritas di jalur ini ({@code nimMahasiswa} selalu
     * {@code null} saat memanggil {@link SettingBiayaMahasiswaSelector#saringDanPrioritaskan}).
     *
     * @return daftar {@link DetailBiaya} untuk item biaya berbayar yang berlaku; list kosong bila
     *         tidak ada {@code SettingBiaya} yang cocok
     */
    public static List<DetailBiaya> getDetailBiayaBukanDefaultBiaya(Session session, Integer angkatan, Jenjang jenjang,
            Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
            StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
            Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
            Integer ta) {
		return getDetailBiayaBukanDefaultBiaya(session, angkatan, jenjang, semester, jenisKegiatan,
				statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program,
				kelamin, afiliasiCalonMahasiswa, ta, null);
	}

	public static List<DetailBiaya> getDetailBiayaBukanDefaultBiaya(Session session, Integer angkatan,
			Jenjang jenjang, Integer semester, JenisKegiatan jenisKegiatan,
			StatusAwalMahasiswa statusAwalMahasiswa, StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi,
			GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan, String program,
			String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta, String nimMahasiswa) {

        try {
            Criteria criteria = createSettingBiayaCriteria(session, ta, jenisKegiatan, semester, false, false)
                    .addOrder(Order.desc("ta")).addOrder(Order.asc("statusMahasiswa")).addOrder(Order.asc("kelamin"))
                    .addOrder(Order.asc("paket")).addOrder(Order.asc("gelombangPendaftaran")).addOrder(Order.asc("jenjang"))
                    .addOrder(Order.asc("angkatan")).addOrder(Order.asc("jenisKegiatan"))
                    .addOrder(Order.asc("statusAwalMahasiswa")).addOrder(Order.asc("jenisSeleksi"))
                    .addOrder(Order.asc("jurusan")).addOrder(Order.asc("program"));

            List<SettingBiaya> settingBiayas = ConstantValues.simpleList(criteria, SettingBiaya.class);
			settingBiayas = SettingBiayaMahasiswaSelector.saringDanPrioritaskan(session, settingBiayas,
					nimMahasiswa);

            String[] properties = new String[] { "statusMahasiswa", "kelamin", "afiliasiCalonMahasiswa", "program",
                    "angkatan", "jenjang", "statusAwalMahasiswa", "jenisSeleksi", "gelombangPendaftaran", "paket", "jurusan" };
            Object[] datas = new Object[] { statusMahasiswa, kelamin, afiliasiCalonMahasiswa, program, angkatan, jenjang,
                    statusAwalMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan };

            SettingBiaya settingBiaya = SettingBiayaMahasiswaSelector.pilihSatuDenganPrioritas(settingBiayas,
                    properties, datas);

            if (settingBiaya == null) {
                return new ArrayList<DetailBiaya>();
            }
			if (settingBiaya.isMahasiswaDikecualikan(nimMahasiswa)) {
				return PengecualianTagihanList.kosong();
			}

            Criteria dsbCriteria = session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya");
            applyItemBiayaFilters(dsbCriteria, semester, true);
            dsbCriteria.add(Restrictions.gt("defaultBiaya", 0.1));
            dsbCriteria.add(Restrictions.eq("settingBiaya", settingBiaya));

            List<DetailSettingBiaya> detailSettingBiayas = ConstantValues.simpleList(dsbCriteria, DetailSettingBiaya.class);
            List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();

            for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {
                DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
                        .add(Restrictions.eq("detailSettingBiaya", detailSettingBiaya))
                        .add(Restrictions.eq("settingBiaya", settingBiaya))
                        .add(eqAtauKosong("program", program))
                        .add(semester == null ? Restrictions.sqlRestriction(SQL_TRUE) : Restrictions.eq("semester", semester))
                        .add(eqAtauNull("jurusan", jurusan))
                        .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

                if (detailBiaya == null) {
                    detailBiaya = new DetailBiaya();
                    detailBiaya.setSettingBiaya(settingBiaya);
                    detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
                    detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
                    detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                    detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                    detailBiaya.setAngkatan(angkatan);
                    detailBiaya.setJurusan(jurusan);
                    detailBiaya.setAfiliasiCalonMahasiswa(afiliasiCalonMahasiswa);
                    detailBiaya.setKelamin(kelamin);
                    detailBiaya.setJenisSeleksi(jenisSeleksi);
                    detailBiaya.setGelombangPendaftaran(gelombangPendaftaran);
                    detailBiaya.setPaket(paket);
                    detailBiaya.setJenjang(jenjang);
                    detailBiaya.setProgram(program);
                    detailBiaya.setStatusAwalMahasiswa(statusAwalMahasiswa);
                    detailBiaya.setStatusMahasiswa(statusMahasiswa);
                    detailBiaya.setJenisKegiatan(jenisKegiatan);
                    detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());
                    if (semester != null) detailBiaya.setSemester(semester);
                    
                    saveOrUpdateEntity(session, detailBiaya, false);

                } else if (isDetailBiayaNeedsUpdate(detailBiaya, detailSettingBiaya, settingBiaya)) {
                    detailBiaya.setJurusan(jurusan);
                    detailBiaya.setSettingBiaya(settingBiaya);
                    detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
                    
                    saveOrUpdateEntity(session, detailBiaya, true);
                }
                
                if (semester != null) detailBiaya.setSemester(semester);
                detailBiaya.setSettingBiaya(settingBiaya);
                detailBiaya.setDefaultTanggalTagihan(detailSettingBiaya.getDefaultTanggalTagihan());
                detailBiaya.setKeterangan(detailSettingBiaya.getDefaultKeterangan());
                detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
                detailBiayas.add(detailBiaya);
            }

            return detailBiayas;
        } finally {
            closeSessionSafely(session);
        }
    }
}
