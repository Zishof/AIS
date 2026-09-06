package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.payroll.LiburRutin;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Panel detail (baris ekspansi ZK {@link MyDetail}) yang menampilkan rekap absensi harian satu {@link Pegawai}
 * (karyawan non-dosen) untuk satu bulan-tahun terpilih, sekaligus menjadi tempat admin/HRD mengoreksi data
 * kehadiran secara manual. Data utama disimpan pada {@link StatuskehadiranKaryawanHarian}; baris yang belum ada
 * untuk tanggal tertentu dalam rentang bulan diisi otomatis lewat
 * {@link CommonPayroll#getDefaultStatuskehadiranKaryawanHarian}.
 *
 * <p>Selain status dan jam masuk/pulang, setiap baris menampilkan besaran turunan penggajian: jumlah jam
 * masuk/lembur/keluar cepat/terlambat ({@code getJumlahJamMasuk()} dkk. pada
 * {@link StatuskehadiranKaryawanHarian}) serta info {@link DetailJenisShiftPegawai} — otomatis dari jadwal shift
 * pegawai, atau override manual lewat checkbox "Manual" beserta combobox shift/jam lembur bila diaktifkan admin.
 * Baris yang sudah {@code getDikunci()} (mis. sudah diposting payroll) menyembunyikan kontrol override
 * tersebut.</p>
 *
 * <p><b>Efek samping non-obvious yang perlu diperhatikan pemanggil/pemelihara:</b></p>
 * <ul>
 * <li>Tombol toolbar "Singkronkan" menjalankan proses batch di {@link Thread} terpisah (tidak memblokir UI):
 * untuk tiap hari dalam bulan terpilih dibuka transaksi sendiri, dipanggil ulang
 * {@link CommonPayroll#getDefaultStatuskehadiranKaryawanHarian} dan {@link CommonPayroll#getDetailJenisShiftPegawai}
 * untuk MENGHITUNG ULANG jenis shift pegawai pada tanggal itu, lalu di-commit per hari (agar kegagalan satu hari
 * tidak membatalkan hari lain). Progres dipantau lewat {@link Timer} ZK yang polling flag
 * {@link AtomicBoolean} sampai thread selesai, baru memanggil {@link #loadData(Object)} ulang.</li>
 * <li>{@link #loadData(Object)} melakukan pengisian/koreksi otomatis jam pulang yang benar-benar menulis ke
 * database saat grid dimuat (bukan sekadar membaca): mengisi jam pulang yang kosong padahal ada scan online
 * yang memenuhi durasi kerja minimal shift, dan mengoreksi jam pulang "shadow" (tersimpan lebih awal dari scan
 * pulang asli).</li>
 * <li>Riwayat lokasi/foto absensi online (datang dan pulang) ditampilkan lewat sub-{@link MyDetail} per baris,
 * termasuk peta (iframe Google Maps) dan tautan pop-up foto; tabel "Sejarah Absensi Online" merender seluruh
 * riwayat scan dari {@code ambilSejarah()}.</li>
 * <li>{@link #editJam(StatuskehadiranKaryawanHarian)} membuka jendela modal terpisah untuk mengubah status
 * absensi, jam datang/pulang manual, dan keterangan satu baris dengan commit tersendiri, termasuk aksi cepat
 * "Jadikan Hanya Kepulangan" untuk kasus scan pertama pegawai yang salah tercatat sebagai kedatangan.</li>
 * </ul>
 *
 * <p>Mode edit grid ditentukan oleh {@link CommonPrivilages#checkPrevilages(String)} dengan hak
 * {@link CommonPrivilages#UPDATE}; baris yang berasal dari {@link CutiDanIzin} yang sudah disetujui tidak
 * dirender sebagai kontrol edit (status kehadirannya mengikuti data cuti/izin).</p>
 *
 * @see MyDetail
 * @see StatuskehadiranKaryawanHarian
 * @see Pegawai
 * @see CommonPayroll
 */
public class AbsensiKehadiranPegawaiHarianHelper extends MyDetail {

	/**
	 * Penanda versi serialisasi bawaan {@link MyDetail}/komponen ZK. Nilainya tidak boleh diubah agar sesi
	 * yang diserialisasi (mis. saat failover/passivation kontainer ZK) tetap kompatibel.
	 */
	private static final long serialVersionUID = -8823784546257272901L;

	/**
	 * Combobox pilihan bulan (nilai {@link Integer} 1&ndash;12, label dari {@link Common#BULAN}) yang menentukan
	 * rentang tanggal rekap. Dibuat readonly di {@link #display()} sehingga pemakai hanya bisa memilih dari
	 * daftar, dan setiap {@code onChange} memicu {@link #loadData(Object)}. Bernilai {@code null} sebelum
	 * {@link #display()} dipanggil.
	 */
	private Combobox bulan;

	/**
	 * Combobox pilihan tahun (nilai {@link Integer}, rentang tahun berjalan &minus;10 s.d. +10) yang melengkapi
	 * {@link #bulan} dalam menentukan periode rekap. Sama seperti {@link #bulan}: readonly dan memicu
	 * {@link #loadData(Object)} pada {@code onChange}.
	 */
	private Combobox tahun;

	/**
	 * Grid berpaging (100 baris/halaman) tempat seluruh baris rekap harian dirender. Dipegang sebagai field
	 * karena {@link #loadData(Object)} membersihkan lalu mengisi ulang {@link Rows}-nya setiap kali filter
	 * berubah, proses "Singkronkan" selesai, atau satu baris selesai diedit lewat
	 * {@link #editJam(StatuskehadiranKaryawanHarian)}.
	 */
	private MyGrid grid;

	/**
	 * Pegawai yang rekap absensinya ditampilkan dan dikelola panel ini. Ditetapkan sekali lewat konstruktor dan
	 * tidak pernah diganti; menjadi filter tunggal bagi seluruh query {@link CutiDanIzin},
	 * {@link StatuskehadiranKaryawanHarian}, dan perhitungan {@link DetailJenisShiftPegawai} di kelas ini.
	 *
	 * <p><b>Catatan cakupan akses:</b> kelas ini TIDAK memverifikasi sendiri apakah pengguna yang login berhak
	 * melihat/mengubah pegawai ini (tidak ada penyaringan satuan kerja maupun pencocokan identitas). Penentuan
	 * pegawai mana yang boleh dibuka sepenuhnya diserahkan kepada pemanggil — {@code AbsensKehadiranPegawaiHarianAction}
	 * (menyaring lewat {@code SekolahUtil.ambilSatuanKerjas()}) dan {@code BiodataPegawaiAction} (meresolusi
	 * pegawai dari akun yang login bila parameternya {@code null}).</p>
	 */
	private Pegawai pegawai;

	/**
	 * Menandai apakah kontrol pengubahan data dirender (tombol Ubah, checkbox "Abaikan Jarak", override shift
	 * dan lembur manual). Dievaluasi sekali di konstruktor dari
	 * {@link CommonPrivilages#checkPrevilages(String)} dengan hak {@link CommonPrivilages#UPDATE}, yaitu hak
	 * UPDATE pada menu yang sedang dibuka — bukan hak khusus modul absensi/penggajian. Baris yang berasal dari
	 * {@link CutiDanIzin} yang sudah disetujui atau yang sudah {@code getDikunci()} tetap dirender read-only
	 * meskipun {@code edit} bernilai {@code true}.
	 */
	private boolean edit = false;

	/**
	 * Membuat panel rekap absensi harian untuk satu {@link Pegawai}. Menentukan mode edit dari
	 * {@link CommonPrivilages#checkPrevilages(String)} dengan hak {@link CommonPrivilages#UPDATE} (dievaluasi
	 * sekali di sini, dipakai saat {@link #loadData(Object)} merender baris), dan mendaftarkan listener
	 * {@code onOpen} yang membersihkan komponen anak lalu memanggil {@link #display()} setiap kali panel ini
	 * dibuka (lazy render, mengikuti pola siklus hidup {@link MyDetail}).
	 *
	 * @param pegawai pegawai yang rekap absensinya akan ditampilkan
	 */
	public AbsensiKehadiranPegawaiHarianHelper(Pegawai pegawai) {
		this.pegawai = pegawai;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		addEventListener("onOpen", new EventListener() {

			/**
			 * Menerapkan pola lazy render {@link MyDetail}: setiap kali baris ekspansi dibuka atau ditutup,
			 * seluruh komponen anak dibuang lebih dulu lewat {@link Common#clear(Object)}, dan UI baru dibangun
			 * ulang lewat {@link #display()} hanya bila panel benar-benar dalam keadaan terbuka
			 * ({@code isOpen()}). Konsekuensinya, menutup lalu membuka kembali panel selalu memuat ulang data
			 * dari database dan membuang seluruh state kontrol yang belum disimpan.
			 *
			 * @param arg0 event {@code onOpen} dari komponen detail ZK
			 * @throws Exception diteruskan dari pembangunan UI/akses Hibernate bila terjadi kegagalan
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AbsensiKehadiranPegawaiHarianHelper.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Membangun UI panel: groupbox berjudul "Daftar absensi pegawai", toolbar dengan combobox Bulan (readonly,
	 * default bulan berjalan) dan Tahun (readonly, rentang tahun berjalan &minus;10 s.d. +10, default tahun
	 * berjalan), tombol "Singkronkan" (memicu perhitungan ulang shift satu bulan penuh secara background — lihat
	 * dokumentasi kelas), tombol "Cari", serta grid berpaging (100 baris/halaman, mold paging "os" di posisi
	 * atas) dengan kolom Tanggal, Status, Masuk/Pulang, Jam kerja, Shift, Lembur, Cepat, Terlambat, kolom
	 * tersembunyi "Abaikan Jarak" (lebar 0px, dipakai sebagai anchor kolom checkbox di
	 * {@link #loadData(Object)}), Keterangan, dan kolom aksi. Perubahan combobox atau klik tombol Cari memicu
	 * {@link #loadData(Object)} ulang. Data awal langsung dimuat; kegagalan pada pemuatan pertama ditelan dan
	 * hanya ditampilkan ke admin lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @return groupbox yang baru dibangun dan sudah menjadi anak dari panel ini
	 */
	public Groupbox display() {
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled(Common.getBahasaConfig("Daftar absensi pegawai")));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan : ")));
		toolbar.appendChild(bulan = new Combobox());
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		bulan.addEventListener("onChange", new EventListener() {

			/**
			 * Memuat ulang seluruh grid rekap begitu pemakai mengganti bulan. Tidak ada penyaringan sisi klien:
			 * {@link #loadData(Object)} membaca sendiri nilai combobox {@link #bulan} dan {@link #tahun} yang
			 * sedang terpilih, lalu membangun ulang baris untuk seluruh tanggal pada periode baru.
			 *
			 * @param arg0 event {@code onChange} dari combobox Bulan
			 * @throws Exception diteruskan dari {@link #loadData(Object)}
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		bulan.setReadonly(true);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun : ")));
		toolbar.appendChild(tahun = new Combobox());

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);

		tahun.addEventListener("onChange", new EventListener() {

			/**
			 * Kembaran listener {@link #bulan}: memuat ulang grid rekap begitu pemakai mengganti tahun, dengan
			 * periode diambil ulang dari kedua combobox di dalam {@link #loadData(Object)}.
			 *
			 * @param arg0 event {@code onChange} dari combobox Tahun
			 * @throws Exception diteruskan dari {@link #loadData(Object)}
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		tahun.setReadonly(true);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		toolbar.appendChild(cetakSksDosen);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

		    /**
		     * Memicu proses "Singkronkan" — perhitungan ulang jenis shift pegawai untuk seluruh tanggal pada
		     * bulan terpilih. Pekerjaan sebenarnya ditunda ke siklus event berikutnya lewat
		     * {@link Common#createDefaultTimer(EventListener)} agar indikator sibuk sempat ter-render lebih dulu.
		     *
		     * <p>Nama variabel {@code cetakSksDosen} adalah sisa salin-tempel dari layar lain dan tidak
		     * mencerminkan fungsinya; tombol ini tidak ada hubungannya dengan pencetakan SKS dosen.</p>
		     *
		     * @param arg0 event {@code onClick} dari tombol Singkronkan
		     * @throws Exception diteruskan dari penjadwalan timer ZK
		     */
		    @Override
		    public void onEvent(Event arg0) throws Exception {
		        Common.createDefaultTimer(new EventListener() {

		            /**
		             * Menjalankan proses sinkronisasi shift satu bulan penuh. Memvalidasi lebih dulu bahwa bulan
		             * dan tahun sudah dipilih (bila belum, menampilkan peringatan dan berhenti tanpa efek),
		             * menampilkan indikator sibuk lewat {@link Clients#showBusy(String)}, lalu menyerahkan
		             * pekerjaan berat ke {@link Thread} terpisah agar UI tidak terblokir.
		             *
		             * <p>Karena thread latar tidak boleh menyentuh komponen ZK, penyelesaiannya dikabarkan lewat
		             * flag {@link AtomicBoolean} yang dipantau sebuah {@link Timer} ZK ber-interval 500 ms. Timer
		             * dipasang pada root halaman (bukan pada panel ini) supaya tetap hidup walau baris ekspansi
		             * ditutup; begitu flag menyala, indikator sibuk dibersihkan, {@link #loadData(Object)}
		             * dipanggil untuk menampilkan hasil, dan timer melepas dirinya sendiri.</p>
		             *
		             * @param arg0 event timer penunda dari {@link Common#createDefaultTimer(EventListener)}
		             * @throws Exception diteruskan dari akses komponen ZK bila terjadi kegagalan
		             */
		            @Override
		            public void onEvent(Event arg0) throws Exception {

		                final Integer bulan = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem() == null 
		                        ? null : AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem().getValue());

		                final Integer tahun = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem() == null 
		                        ? null : AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem().getValue());

		                if (bulan == null) {
		                    MyMessageboxConfig.show("Mohon maaf, bulan belum dipilih. Silakan pilih bulan terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		                    return;
		                }
		                if (tahun == null) {
		                    MyMessageboxConfig.show("Mohon maaf, tahun belum dipilih. Silakan pilih tahun terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		                    return;
		                }

		                // Menggunakan AtomicBoolean sebagai flag Thread-Safe, BUKAN komponen UI (Label)
		                final AtomicBoolean isProsesSelesai = new AtomicBoolean(false);

		                // Tampilkan loading UI di awal sebelum Thread berjalan
		                Clients.showBusy("Proses singkronisasi shift...");

		                new Thread(new Runnable() {
		                    /**
		                     * Menghitung ulang {@link DetailJenisShiftPegawai} untuk setiap tanggal pada bulan
		                     * terpilih, di luar thread event ZK.
		                     *
		                     * <p>Satu {@link Session} Hibernate dibuka sekali untuk seluruh loop demi efisiensi,
		                     * tetapi setiap hari mendapat {@link Transaction} sendiri sehingga kegagalan pada satu
		                     * tanggal tidak membatalkan tanggal lain — blok {@code catch} per hari melakukan
		                     * rollback, mencatat error lewat {@code ErrorAuditUtil}, lalu meneruskan loop. Untuk tiap
		                     * tanggal: baris kehadiran diambil/dibuat lewat
		                     * {@link CommonPayroll#getDefaultStatuskehadiranKaryawanHarian}, disegarkan dari
		                     * database, lalu jenis shift dihitung ulang oleh
		                     * {@link CommonPayroll#getDetailJenisShiftPegawai} berdasarkan jam masuk aktual, nama
		                     * hari, dan status libur nasional, dan hasilnya disimpan.</p>
		                     *
		                     * <p><b>Dua penjagaan yang sengaja dipasang dan tidak boleh dihapus:</b> (1) commit
		                     * hanya dijalankan bila transaksi masih benar-benar aktif — sebab
		                     * {@code Common.refreshSaveOrUpdate} dapat menelan exception lalu diam-diam melakukan
		                     * rollback, sementara {@code simpanDetail}/{@code getDefaultStatuskehadiranKaryawanHarian}
		                     * dapat membuka dan menutup ulang transaksi pada session yang sama, sehingga commit buta
		                     * akan melempar "Transaction not successfully started" yang gejalanya jauh dari akar
		                     * masalah; kondisi tak-aktif ini dicatat sebagai audit, bukan didiamkan. (2) cache level
		                     * satu dibersihkan ({@code flush}+{@code clear}) tiap hari agar memori tidak membengkak
		                     * pada bulan berisi banyak baris.</p>
		                     *
		                     * <p>Blok {@code finally} berlapis menutup session dan memanggil
		                     * {@link HibernateUtil#closeSession()} untuk mencegah kebocoran koneksi, dan pada
		                     * lapisan terdalam SELALU menyalakan flag {@code isProsesSelesai} — sehingga timer
		                     * pemantau di sisi UI pasti berhenti walaupun proses gagal total.</p>
		                     */
		                    @Override
		                    public void run() {
		                        Session session = null;
		                        try {
		                            // 1. Buka Session SATU KALI saja di luar loop untuk efisiensi
		                            session = HibernateUtil.currentNativeSession();

		                            Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		                            calendar.set(Calendar.MONTH, bulan - 1);
		                            calendar.set(Calendar.YEAR, tahun);
		                            calendar.set(Calendar.DATE, 1);

		                            int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		                            for (int i = 1; i <= jumlahHari; i++) {
		                                calendar.set(Calendar.DATE, i);
		                                Date tanggal = calendar.getTime();
		                                String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];

		                                Transaction tx = null;
		                                try {
		                                    // 2. Mulai transaksi untuk tiap iterasi hari
		                                    tx = session.beginTransaction();

		                                    StatuskehadiranKaryawanHarian statusKehadiran = CommonPayroll
		                                            .getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, null, null,
		                                                    "", "", session, true);
		                                    
		                                    session.refresh(statusKehadiran);

		                                    Date waktuMasuk = statusKehadiran.ambilMasukjam() == null ? tanggal : statusKehadiran.ambilMasukjam();
		                                    boolean isLiburNasional = statusKehadiran.getLiburNasional() != null;

		                                    DetailJenisShiftPegawai jenis = CommonPayroll.getDetailJenisShiftPegawai(
		                                            pegawai, null, null, waktuMasuk, statusKehadiran.getTanggal(), 
		                                            hari, isLiburNasional);

		                                    statusKehadiran.setDetailJenisShiftPegawai(jenis);

		                                    Common.refreshSaveOrUpdate(session, statusKehadiran);
		                                    CommonPayroll.simpanDetail(session, statusKehadiran, true);

		                                    // 3. Commit data per hari -- GUARD: hanya commit bila transaksi masih
		                                    // benar-benar aktif. Root cause bug lama: Common.refreshSaveOrUpdate bisa
		                                    // menelan exception (mis. pelanggaran constraint NOT NULL yang bukan
		                                    // "unique constraint") lalu diam-diam rollback transaksi tanpa melempar
		                                    // ulang; atau CommonPayroll.simpanDetail/getDefaultStatuskehadiranKaryawanHarian
		                                    // (baru=true) sempat begin+commit ulang transaksi yang SAMA (session sama)
		                                    // sehingga tx di sini sudah tidak aktif lagi saat commit() dipanggil ->
		                                    // "TransactionException: Transaction not successfully started" (gejala jauh
		                                    // dari akar masalah, pola sama seperti bug kodeunik null di Kegiatan/Mandiri).
		                                    if (tx != null && tx.isActive()) {
		                                        tx.commit();
		                                    } else {
		                                        ais.common.ErrorAuditUtil.record(
		                                                new org.hibernate.TransactionException(
		                                                        "Transaksi tidak aktif saat hendak commit shift harian tanggal "
		                                                                + tanggal + " -- kemungkinan sudah di-rollback/di-commit lebih awal akibat error yang tertelan sebelumnya."),
		                                                "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:228");
		                                    }

		                                    // 4. MEMORY OPTIMIZATION: Bersihkan L1 Cache agar RAM tidak membengkak
		                                    session.flush();
		                                    session.clear();

		                                } catch (Exception e) {
		                                    // Cegah data korup jika terjadi error pada hari tertentu
		                                    if (tx != null && tx.isActive()) {
		                                        tx.rollback();
		                                    }
		                                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:239");
		                                    // Loop tetap lanjut memproses hari berikutnya meskipun hari ini error
		                                }
		                            }
		                        } catch (Exception e) {
		                            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:244");
		                        } finally {
		                            // 5. PASTIKAN session ditutup di blok finally untuk mencegah Connection Leak
		                            try {
		                                if (session != null && session.isOpen()) {
		                                    session.disconnect();
		                                    session.close();
		                                }
		                                HibernateUtil.closeSession(); // Jaga-jaga jika utility class butuh dipanggil
		                            } catch (Exception ex) {
		                                ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:254");
		                            } finally {
		                                // Tandai proses background telah sepenuhnya selesai
		                                isProsesSelesai.set(true);
		                            }
		                        }
		                    }
		                }).start();

		                // Timer UI untuk mengecek status background Thread
		                final Timer timer = new Timer(500);
		                timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		                timer.setRepeats(true);
		                timer.addEventListener("onTimer", new EventListener() {

		                    /**
		                     * Memantau flag {@link AtomicBoolean} yang dinyalakan thread sinkronisasi pada blok
		                     * {@code finally}-nya. Selama flag masih mati, tick timer tidak melakukan apa pun;
		                     * begitu menyala, indikator sibuk dibersihkan, grid dimuat ulang lewat
		                     * {@link #loadData(Object)} agar hasil perhitungan shift terlihat, dan timer melepas
		                     * dirinya dari halaman sehingga tidak terus berdetak.
		                     *
		                     * <p>Pola flag ini dipakai karena thread latar tidak boleh menyentuh komponen ZK secara
		                     * langsung; jembatan ke UI harus lewat siklus event ZK seperti timer ini.</p>
		                     *
		                     * @param arg0 event {@code onTimer} setiap 500 ms
		                     * @throws Exception diteruskan dari {@link #loadData(Object)}
		                     */
		                    @Override
		                    public void onEvent(Event arg0) throws Exception {
		                        // Jika isProsesSelesai == true, berarti blok finally pada Thread sudah dieksekusi
		                        if (isProsesSelesai.get()) {
		                            Clients.clearBusy();
		                            loadData(null);
		                            timer.detach();
		                        }
		                    }
		                });
		                timer.start();

		            }
		        });
		    }
		});

		Toolbarbutton button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			/**
			 * Memuat ulang grid rekap untuk periode yang sedang terpilih. Karena kedua combobox sudah memicu
			 * {@link #loadData(Object)} sendiri saat berubah, tombol ini terutama berguna untuk menyegarkan
			 * tampilan setelah data diubah dari layar lain, atau setelah pengisian/koreksi otomatis jam pulang
			 * yang dijalankan {@link #loadData(Object)} mengubah isi database.
			 *
			 * @param arg0 event {@code onClick} dari tombol Cari
			 * @throws Exception diteruskan dari {@link #loadData(Object)}
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setSclass("fgrid");
		grid.setPageSize(100);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Masuk/Pulang");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jam {" + ItemGaji.V_JAM + "}");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Shift");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Lembur {" + ItemGaji.V_LEM + "}");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Cepat {" + ItemGaji.V_CEP + "}");
		column.setWidth("9%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Terlambat {" + ItemGaji.V_TERL + "}");
		column.setWidth("9%");

		column = new Column("Abaikan Jarak");
		column.setParent(columns);
		column.setWidth("0px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("12%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setAlign("center");
		column.setWidth(ais.ui.util.GridKolomHelper.LEBAR_KOLOM_AKSI);

		try {
			loadData(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return groupbox;
	}

	/**
	 * Membuka jendela modal "Ubah Waktu Kehadiran" untuk mengedit satu baris {@link StatuskehadiranKaryawanHarian}
	 * secara manual: status absensi (disembunyikan bila baris berasal dari {@link CutiDanIzin} yang sudah
	 * disetujui — status mengikuti cuti/izin dan tidak boleh diubah di sini), checkbox "Tidak ada kehadiran" /
	 * "Tidak ada kedatangan" / "Tidak ada kepulangan", jam datang dan jam pulang manual (jam pulang yang
	 * ditampilkan mengutamakan hasil {@code ambilPulangUntukTampil()} — jam scan asli terkoreksi — dibanding
	 * nilai kolom tersimpan, agar operator melihat jam kepulangan sebenarnya), serta keterangan bebas.
	 *
	 * <p>Perubahan checkbox/status langsung meng-enable/disable kontrol jam terkait lewat listener bersama, dan
	 * bila {@link ConstantValues#aktifkanFingerPrintOtomatisDariKeterangan} aktif, mengetik keterangan yang
	 * cocok pola tertentu otomatis mengisi & mengunci jam datang/pulang dari
	 * {@link StatuskehadiranKaryawanHarian#mulaiOtomatisUlangAbsenDariKeterangan()} /
	 * {@code #sampaiOtomatisUlangAbsenDariKeterangan()}.</p>
	 *
	 * <p>Tombol "Jadikan Hanya Kepulangan" adalah aksi cepat untuk kasus pegawai yang tidak absen datang tetapi
	 * scan pertamanya terlanjur tercatat sebagai kedatangan: menandai "Tidak ada kedatangan", mengosongkan jam
	 * datang, memastikan jam pulang terisi dari scan pulang asli, lalu memicu event {@code onClick} tombol
	 * Simpan secara terprogram ({@link Events#sendEvent(Event)}). Tombol Simpan sendiri mencari baris
	 * {@link StatuskehadiranKaryawanHarian} yang sudah ada berdasarkan id (atau membuat baris baru bila belum
	 * ada — menyalin tanggal/pegawai/dosen/mahasiswa/guru/minggu/libur dari parameter), menuliskan field yang
	 * diubah, lalu commit dalam transaksi tersendiri ({@code session.getTransaction()}), menutup session, dan
	 * memuat ulang grid induk lewat {@link #loadData(Object)} setelah jendela ditutup.</p>
	 *
	 * @param statuskehadiranKaryawanHarianTemp baris kehadiran (bisa berupa objek sementara belum tersimpan)
	 *                                           yang akan diedit
	 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
	 */
	private void editJam(final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarianTemp) throws Exception {
		final MyWindow window = new MyWindow("Ubah Waktu Kehadiran", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("95%");
		window.setWidth("600px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		row.appendChild(new Label(statuskehadiranKaryawanHarianTemp.getPegawai() == null ? ""
				: statuskehadiranKaryawanHarianTemp.getPegawai().getNama()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari / Tanggal"));
		row.appendChild(new Label(Common.dateFormat6.get().format(statuskehadiranKaryawanHarianTemp.getTanggal())));

		List<Statusabsensi> statusabsensis = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Statusabsensi.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true),
								Restrictions.in("id", new Long[] { 1L, 3L, 4L, 5L })))
						.addOrder(Order.asc("nama")),
				Statusabsensi.class);
		final Combobox absen = new Combobox();
		absen.setWidth("90%");
		absen.setReadonly(true);
		Common.insertComboItems(absen, "nama", statusabsensis);
		Common.selectComboItem(absen, statuskehadiranKaryawanHarianTemp.getStatusabsensi());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kehadiran"));
		if (statuskehadiranKaryawanHarianTemp.getCutiDanIzin() != null
				&& statuskehadiranKaryawanHarianTemp.getCutiDanIzin().getSetujui()) {
			row.appendChild(new Label(statuskehadiranKaryawanHarianTemp.getStatusabsensi() == null ? ""
					: statuskehadiranKaryawanHarianTemp.getStatusabsensi().getNama()));
		} else {
			absen.setParent(row);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig tidakAdaKehadiran;
		row.appendChild(tidakAdaKehadiran = new MyCheckboxConfig("Tidak ada kehadiran"));
		tidakAdaKehadiran.setChecked(statuskehadiranKaryawanHarianTemp.getTidakAdaKehadiran());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam datang"));
		final MyTimebox datang = new MyTimebox(statuskehadiranKaryawanHarianTemp.getMasukjamManual());
		row.appendChild(datang);
		if (datang.getValue() == null) {
			datang.setValue(statuskehadiranKaryawanHarianTemp.getMasukjam());
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig tidakAdaKedatangan;
		row.appendChild(tidakAdaKedatangan = new MyCheckboxConfig("Tidak ada kedatangan"));
		tidakAdaKedatangan.setChecked(statuskehadiranKaryawanHarianTemp.getTidakAdaKedatangan());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam pulang"));
		final MyTimebox pulang = new MyTimebox(statuskehadiranKaryawanHarianTemp.getPulangJamManual());
		row.appendChild(pulang);
		// Utamakan jam pulang AKTUAL untuk ditampilkan di form edit:
		//   (a) KOREKSI shadow: bila jam pulang tersimpan (State/Manual) lebih AWAL dari scan pulang
		//       genuine terakhir (mis. tersimpan 07:31 padahal scan pulang asli 15:38), pakai scan asli.
		//   (b) Selain itu getPulangJam(); lalu fallback riwayat absensi online / keterangan.
		// Aturan durasi kerja minimal shift TIDAK diubah (koreksi hanya saat durasi > minimal). Operator
		// dapat melihat jam pulang yang benar lalu menekan Simpan untuk menyimpannya permanen.
		Date pulangTampil = statuskehadiranKaryawanHarianTemp.ambilPulangUntukTampil();
		if (pulangTampil != null) {
			pulang.setValue(pulangTampil);
		} else if (pulang.getValue() == null) {
			pulang.setValue(statuskehadiranKaryawanHarianTemp.getPulangJam());
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig tidakAdaKepulangan;
		row.appendChild(tidakAdaKepulangan = new MyCheckboxConfig("Tidak ada kepulangan"));
		tidakAdaKepulangan.setChecked(statuskehadiranKaryawanHarianTemp.getTidakAdaKepulangan());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan = new MyTextbox(statuskehadiranKaryawanHarianTemp.getKeterangan());
		row.appendChild(keterangan);
		keterangan.setWidth("95%");
		keterangan.setRows(10);
		final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		final EventListener eventListener = new EventListener() {

			/**
			 * Listener bersama yang menyelaraskan status aktif/nonaktif seluruh kontrol jendela edit setiap kali
			 * status absensi atau salah satu checkbox berubah. Dipasang pada combobox {@code absen}
			 * ({@code onChange}) serta ketiga checkbox {@code tidakAdaKehadiran}, {@code tidakAdaKedatangan}, dan
			 * {@code tidakAdaKepulangan} ({@code onClick}), lalu dipanggil sekali dengan {@code null} untuk
			 * menetapkan kondisi awal jendela.
			 *
			 * <p>Aturan yang diberlakukan: {@link Timebox} jam datang/pulang hanya boleh diisi bila status absensi
			 * yang dipilih adalah {@link ConstantValues#MASUK}, checkbox "Tidak ada kehadiran" tidak dicentang, dan
			 * checkbox "tidak ada kedatangan/kepulangan" masing-masing tidak dicentang. Kedua checkbox
			 * kedatangan/kepulangan sendiri ikut dinonaktifkan bila status bukan MASUK atau "Tidak ada kehadiran"
			 * aktif. Tombol Simpan disembunyikan bila status yang dipilih adalah
			 * {@link ConstantValues#BELUM_ABSEN} — baris yang belum diabsen tidak dimaksudkan untuk disimpan
			 * manual dari jendela ini.</p>
			 *
			 * @param arg0 event ZK pemicu, atau {@code null} saat dipanggil untuk menetapkan kondisi awal jendela
			 * @throws Exception diteruskan dari akses komponen ZK bila terjadi kegagalan
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {

				Statusabsensi statusabsensi = (Statusabsensi) (absen.getSelectedItem() == null ? null
						: absen.getSelectedItem().getValue());
				boolean masuk = statusabsensi != null && ConstantValues.MASUK != null
						&& statusabsensi.getId().equals(ConstantValues.MASUK.getId());

				datang.setDisabled(tidakAdaKedatangan.isChecked() || !masuk || tidakAdaKehadiran.isChecked());
				pulang.setDisabled(tidakAdaKepulangan.isChecked() || !masuk || tidakAdaKehadiran.isChecked());

				tidakAdaKedatangan.setDisabled(!masuk || tidakAdaKehadiran.isChecked());
				tidakAdaKepulangan.setDisabled(!masuk || tidakAdaKehadiran.isChecked());

				boolean tidakMasuk = statusabsensi != null && ConstantValues.BELUM_ABSEN != null
						&& statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId());

				save.setVisible(!tidakMasuk);

			}
		};

		keterangan.addEventListener("onChange", new EventListener() {

			/**
			 * Menyalin isi kotak Keterangan ke entity sementara dan — bila
			 * {@link ConstantValues#aktifkanFingerPrintOtomatisDariKeterangan} aktif — mengisi otomatis jam
			 * datang/pulang dari pola yang terbaca di dalam teks keterangan tersebut
			 * ({@link StatuskehadiranKaryawanHarian#mulaiOtomatisUlangAbsenDariKeterangan()} dan
			 * {@code sampaiOtomatisUlangAbsenDariKeterangan()}).
			 *
			 * <p>Jam yang berhasil diturunkan dari keterangan langsung DIKUNCI ({@code setDisabled(true)}) agar
			 * operator tidak mengubahnya lagi secara manual; bila polanya tidak terbaca, kontrol jam dibuka
			 * kembali. Perhatikan bahwa pembukaan kembali ini menimpa status disabled yang ditetapkan
			 * {@code eventListener} berdasarkan status absensi/checkbox, sehingga urutan interaksi pemakai
			 * menentukan kontrol mana yang akhirnya aktif. Penyalinan keterangan di sini hanya ke objek di memori;
			 * penyimpanan ke database baru terjadi lewat tombol Simpan.</p>
			 *
			 * @param arg0 event {@code onChange} dari kotak Keterangan
			 * @throws Exception diteruskan dari akses komponen ZK bila terjadi kegagalan
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				statuskehadiranKaryawanHarianTemp.setKeterangan(keterangan.getValue());

				if (ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan) {
					Date m = statuskehadiranKaryawanHarianTemp.mulaiOtomatisUlangAbsenDariKeterangan();
					if (m != null) {
						datang.setValue(m);
						datang.setDisabled(true);
					} else {
						datang.setDisabled(false);
					}

					m = statuskehadiranKaryawanHarianTemp.sampaiOtomatisUlangAbsenDariKeterangan();
					if (m != null) {
						pulang.setValue(m);
						pulang.setDisabled(true);
					} else {
						pulang.setDisabled(false);
					}
				}
			}
		});

		tidakAdaKedatangan.addEventListener("onClick", eventListener);
		absen.addEventListener("onChange", eventListener);
		tidakAdaKehadiran.addEventListener("onClick", eventListener);
		tidakAdaKepulangan.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Menutup jendela edit tanpa menyimpan apa pun. Perlu dicatat bahwa perubahan yang sudah terlanjur
			 * disalin ke objek {@code statuskehadiranKaryawanHarianTemp} di memori (mis. lewat listener
			 * Keterangan) tidak dikembalikan — namun karena objek tersebut tidak dipersist dan grid induk dimuat
			 * ulang dari database pada interaksi berikutnya, hasil akhirnya tetap setara dengan pembatalan.
			 *
			 * @param event event {@code onClick} dari tombol Batal
			 * @throws Exception diteruskan dari akses komponen ZK bila terjadi kegagalan
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		// Aksi cepat: tandai record ini sebagai HANYA KEPULANGAN (tanpa kedatangan), isi jam pulang dari
		// scan pulang asli, lalu Simpan — untuk kasus pegawai yang tidak absen datang tetapi scan pertamanya
		// terlanjur tercatat sebagai kedatangan.
		final MyToolbarbuttonConfig hanyaPulang = new MyToolbarbuttonConfig("Jadikan Hanya Kepulangan",
				"/img/save.gif");
		hanyaPulang.setTooltiptext("Tandai tanpa kedatangan, isi jam pulang dari scan pulang, lalu simpan");
		hanyaPulang.addEventListener("onClick", new EventListener() {
			/**
			 * Aksi cepat empat langkah untuk kasus pegawai yang tidak sempat absen datang sehingga scan
			 * pertamanya terlanjur tercatat sebagai kedatangan: (1) menandai checkbox "Tidak ada kedatangan" dan
			 * mengosongkan jam datang; (2) mengisi jam pulang dari scan pulang asli lewat
			 * {@link StatuskehadiranKaryawanHarian#ambilPulangUntukTampil()} bila kotak jam pulang masih kosong;
			 * (3) menyegarkan status aktif/nonaktif kontrol lewat {@code eventListener}; dan (4) menjalankan aksi
			 * simpan dengan mengirim event {@code onClick} secara terprogram ke tombol Simpan
			 * ({@link Events#sendEvent(Event)}).
			 *
			 * <p>Karena langkah terakhir menumpang pada listener tombol Simpan, seluruh perilaku penyimpanan
			 * (pencarian baris berdasarkan id, pembuatan baris baru bila belum ada, transaksi tersendiri, penutupan
			 * session, dan pemuatan ulang grid induk) berlaku identik — termasuk kenyataan bahwa aksi ini menyimpan
			 * langsung tanpa dialog konfirmasi tambahan.</p>
			 *
			 * @param event event {@code onClick} dari tombol "Jadikan Hanya Kepulangan"
			 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				// 1) Tandai TIDAK ADA KEDATANGAN & kosongkan jam datang.
				tidakAdaKedatangan.setChecked(true);
				datang.setValue(null);
				// 2) Pastikan jam pulang terisi dari scan pulang asli (koreksi shadow / riwayat).
				if (pulang.getValue() == null) {
					Date pulangAsli = statuskehadiranKaryawanHarianTemp.ambilPulangUntukTampil();
					if (pulangAsli != null) {
						pulang.setValue(pulangAsli);
					}
				}
				// 3) Segarkan status aktif/nonaktif field sesuai centang.
				eventListener.onEvent(null);
				// 4) Jalankan aksi Simpan (commit) memakai listener tombol Simpan yang sudah ada.
				org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", save));
			}
		});
		hanyaPulang.setParent(toolbar);

		save.setTooltiptext("Proses");
		save.addEventListener("onClick", new EventListener() {
			/**
			 * Menyimpan seluruh isi jendela edit ke satu baris {@link StatuskehadiranKaryawanHarian}.
			 *
			 * <p>Baris target dicari ulang dari database berdasarkan id objek sementara; bila objek sementara
			 * belum punya id (tanggal yang belum pernah tersimpan), dibuat baris BARU dengan menyalin
			 * tanggal, pegawai, dosen, mahasiswa, guru, minggu, serta relasi {@link LiburNasional}/
			 * {@link LiburRutin} dari objek sementara. Nilai yang ditulis: status absensi (hanya bila ada pilihan),
			 * jam datang/pulang — masing-masing disimpan GANDA ke kolom {@code ...Manual} dan {@code ...State} —
			 * ketiga flag "tidak ada kehadiran/kedatangan/kepulangan", dan keterangan (di-{@code trim}).
			 * Nilai yang sama juga disalin balik ke objek sementara agar tampilan grid induk konsisten sebelum
			 * dimuat ulang.</p>
			 *
			 * <p>Penyimpanan berjalan dalam transaksi tersendiri pada
			 * {@link HibernateUtil#currentNativeSession()} ({@code begin} &rarr;
			 * {@link Common#refreshSaveOrUpdate(Session, Object)} &rarr; {@code commit}), lalu session
			 * di-{@code disconnect}/{@code close} dan {@link HibernateUtil#closeSession()} dipanggil. Penutupan
			 * jendela dan pemuatan ulang grid induk lewat {@link #loadData(Object)} sengaja ditunda ke siklus event
			 * berikutnya memakai {@link Common#createDefaultTimer(EventListener)}, agar tidak berjalan di atas
			 * session yang baru saja ditutup.</p>
			 *
			 * <p><b>Catatan pemeliharaan:</b> listener ini tidak mengulang pemeriksaan hak {@link #edit} maupun
			 * status {@code getDikunci()}; pembatasan itu hanya diberlakukan saat merender tombol Ubah di grid.
			 * Setiap perubahan pada jalur render harus mempertimbangkan bahwa penyimpanan di sini tidak memiliki
			 * gerbang tersendiri.</p>
			 *
			 * @param event event {@code onClick} dari tombol Simpan (juga dikirim terprogram oleh tombol
			 *              "Jadikan Hanya Kepulangan")
			 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
			 */
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentNativeSession();
				StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = (StatuskehadiranKaryawanHarian) (statuskehadiranKaryawanHarianTemp
						.getId() == null
								? null
								: session.createCriteria(StatuskehadiranKaryawanHarian.class)
										.add(Restrictions.idEq(statuskehadiranKaryawanHarianTemp.getId()))
										.setMaxResults(1).uniqueResult());
				if (statuskehadiranKaryawanHarian == null) {
					statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
					statuskehadiranKaryawanHarian.setTanggal(statuskehadiranKaryawanHarianTemp.getTanggal());
					statuskehadiranKaryawanHarian.setPegawai(statuskehadiranKaryawanHarianTemp.getPegawai());
					statuskehadiranKaryawanHarian.setDosen(statuskehadiranKaryawanHarianTemp.getDosen());
					statuskehadiranKaryawanHarian.setMahasiswa(statuskehadiranKaryawanHarianTemp.getMahasiswa());
					statuskehadiranKaryawanHarian.setGuru(statuskehadiranKaryawanHarianTemp.getGuru());
					statuskehadiranKaryawanHarian.setMinggu(statuskehadiranKaryawanHarianTemp.getMinggu());
					statuskehadiranKaryawanHarian
							.setLiburNasional(statuskehadiranKaryawanHarianTemp.getLiburNasional());
					statuskehadiranKaryawanHarian.setLiburRutin(statuskehadiranKaryawanHarianTemp.getLiburRutin());
				}

				Statusabsensi statusabsensi = (Statusabsensi) (absen.getSelectedItem() == null ? null
						: absen.getSelectedItem().getValue());
				if (statusabsensi != null) {
					statuskehadiranKaryawanHarian.setStatusabsensi(statusabsensi);
				}

				if (statuskehadiranKaryawanHarianTemp != null) {
					statuskehadiranKaryawanHarianTemp.setMasukjamManual(datang.getValue());
					statuskehadiranKaryawanHarianTemp.setTidakAdaKedatangan(tidakAdaKedatangan.isChecked());
					statuskehadiranKaryawanHarianTemp.setPulangJamManual(pulang.getValue());
					statuskehadiranKaryawanHarianTemp.setTidakAdaKepulangan(tidakAdaKepulangan.isChecked());
					statuskehadiranKaryawanHarianTemp.setKeterangan(keterangan.getValue().trim());
					statuskehadiranKaryawanHarianTemp.setTidakAdaKehadiran(tidakAdaKehadiran.isChecked());
					statuskehadiranKaryawanHarianTemp.setMasukjamState(datang.getValue()); 
					statuskehadiranKaryawanHarianTemp.setPulangJamState(pulang.getValue()); 
				}

				statuskehadiranKaryawanHarian.setMasukjamState(datang.getValue()); 
				statuskehadiranKaryawanHarian.setPulangJamState(pulang.getValue()); 
				
				statuskehadiranKaryawanHarian.setMasukjamManual(datang.getValue());
				statuskehadiranKaryawanHarian.setTidakAdaKedatangan(tidakAdaKedatangan.isChecked());
				statuskehadiranKaryawanHarian.setPulangJamManual(pulang.getValue());
				statuskehadiranKaryawanHarian.setTidakAdaKepulangan(tidakAdaKepulangan.isChecked());
				statuskehadiranKaryawanHarian.setKeterangan(keterangan.getValue().trim());
				statuskehadiranKaryawanHarian.setTidakAdaKehadiran(tidakAdaKehadiran.isChecked());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);
				session.getTransaction().commit();
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						window.detach();

						loadData(null);

					}
				});
			}
		});
		save.setParent(toolbar);

		window.onModal();

		if (ConstantValues.aktifkanFingerPrintOtomatisDariKeterangan) {
			Date m = statuskehadiranKaryawanHarianTemp.mulaiOtomatisUlangAbsenDariKeterangan();
			if (m != null) {
				datang.setValue(m);
				datang.setDisabled(true);
			} else {
				datang.setDisabled(false);
			}

			m = statuskehadiranKaryawanHarianTemp.sampaiOtomatisUlangAbsenDariKeterangan();
			if (m != null) {
				pulang.setValue(m);
				pulang.setDisabled(true);
			} else {
				pulang.setDisabled(false);
			}
		}

	}

	/**
	 * Memuat/merender ulang grid rekap absensi {@code pegawai} untuk bulan dan tahun yang sedang dipilih di
	 * combobox. Dijalankan lewat {@link Common#createDefaultTimer(EventListener)} (dieksekusi pada siklus
	 * event berikutnya, bukan langsung) agar UI (mis. indikator busy) sempat ter-render lebih dulu. Menampilkan
	 * pesan peringatan lewat {@link MyMessageboxConfig#show} dan kembali tanpa efek bila bulan atau tahun belum
	 * dipilih.
	 *
	 * <p><b>Tahapan &amp; efek samping:</b></p>
	 * <ol>
	 * <li>Mengambil {@link CutiDanIzin} yang disetujui dan tumpang tindih dengan rentang bulan terpilih, lalu
	 * memakai {@link CommonPayroll#getDefaultStatuskehadiranKaryawanHarian(List, Integer, Integer, Pegawai,
	 * Session, boolean)} untuk mendapatkan peta tanggal &rarr; {@link StatuskehadiranKaryawanHarian} (baris yang
	 * belum ada di database dibuatkan objek default, bukan dipersist di langkah ini).</li>
	 * <li>Untuk setiap baris yang sudah punya id, memanggil {@code autoUpdatePulangDariSejarah} dan
	 * {@code autoKoreksiPulangShadow} lewat session Hibernate terpisah ({@code sesiPulangOtomatis}) — keduanya
	 * BENAR-BENAR MENULIS ke database bila syaratnya terpenuhi (mengisi jam pulang yang kosong dari riwayat scan
	 * online, atau mengoreksi jam pulang yang tersimpan lebih awal dari scan pulang asli), dengan pola
	 * buka-proses-tutup session yang dibungkus try/finally agar tidak bocor koneksi.</li>
	 * <li>Untuk setiap tanggal dalam bulan: menyiapkan sub-{@link MyDetail} berisi info lokasi/foto absen datang
	 * dan pulang (peta Google Maps via iframe, tautan pop-up foto) serta tabel "Sejarah Absensi Online" dari
	 * {@code ambilSejarah()} — hanya ditampilkan bila ada data terkait; memberi latar hijau muda pada hari libur
	 * rutin dan merah muda pada hari libur nasional (menimpa gaya hijau bila keduanya berlaku); merender label
	 * tanggal sebagai tautan revisi ({@link RevisiHelper#createNewRevisi}); menampilkan jam masuk/pulang dengan
	 * fallback berlapis ke riwayat scan/keterangan bila kolom tersimpan kosong akibat aturan durasi kerja
	 * minimal shift; dan, dalam mode edit (lihat dokumentasi kelas), merender kontrol override shift
	 * manual/lembur manual, checkbox "Abaikan Jarak", serta tombol Ubah (memanggil
	 * {@link #editJam(StatuskehadiranKaryawanHarian)}) dan tombol kunci
	 * ({@link GeneralValueObject#tampilKunci}).</li>
	 * </ol>
	 *
	 * <p>Kegagalan pada satu tanggal ditangkap dan dicatat lewat {@code ErrorAuditUtil.record} tanpa
	 * menghentikan render tanggal-tanggal lain.</p>
	 *
	 * @param object tidak digunakan; parameter dipertahankan agar cocok dengan signature listener pemanggil
	 *               ({@code onChange} combobox / {@code onClick} tombol Cari / callback setelah edit)
	 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object object) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();

				Integer bulan = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem() == null
						? null
						: AbsensiKehadiranPegawaiHarianHelper.this.bulan.getSelectedItem().getValue());

				Integer tahun = (Integer) (AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem() == null
						? null
						: AbsensiKehadiranPegawaiHarianHelper.this.tahun.getSelectedItem().getValue());

				if (bulan == null) {
					MyMessageboxConfig.show("Mohon maaf, bulan belum dipilih. Silakan pilih bulan terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (tahun == null) {
					MyMessageboxConfig.show("Mohon maaf, tahun belum dipilih. Silakan pilih tahun terlebih dahulu, kemudian ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, bulan - 1);
				calendar.set(Calendar.YEAR, tahun);
				calendar.set(Calendar.DATE, 1);

				int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

				Calendar mulai = Calendar.getInstance();
				mulai.set(Calendar.YEAR, tahun);
				mulai.set(Calendar.MONTH, bulan - 1);
				mulai.set(Calendar.DATE, 1);

				Calendar sampai = Calendar.getInstance();
				sampai.set(Calendar.YEAR, tahun);
				sampai.set(Calendar.MONTH, bulan - 1);
				sampai.set(Calendar.DATE, jumlahHari);

				Session sessionmy = HibernateUtil.currentNativeSession();

				List<CutiDanIzin> cutiDanIzins = sessionmy.createCriteria(CutiDanIzin.class)
						.addOrder(Order.asc("mulai"))

						.add(Restrictions.or(Restrictions.between("mulai", mulai.getTime(), sampai.getTime()),
								Restrictions.between("sampai", mulai.getTime(), sampai.getTime())))

						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("setujui", true)).list();

//				List<Statusabsensi> statusabsensis = ConstantValues.simpleList(sessionmy
//						.createCriteria(Statusabsensi.class)
//						.add(Restrictions.or(Restrictions.eq("aktif", true),
//								Restrictions.in("id", new Long[] { 1L, 3L, 4L, 5L })))
//						.addOrder(Order.asc("nama")), Statusabsensi.class);

//				System.out.println("cutiDanIzins size " + cutiDanIzins.size() + ", jumlahHari -> " + jumlahHari
//						+ ", statusabsensis -> " + statusabsensis.size());

				Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
				grid.appendChild(rows);
				rows.setParent(grid);
				Common.clear(rows);

				Map<String, StatuskehadiranKaryawanHarian> mapStatuskehadiranKaryawanHarian = CommonPayroll
						.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzins, bulan, tahun, pegawai, sessionmy, false);
				sessionmy.disconnect();
				sessionmy.close();
				HibernateUtil.closeSession();

				// === AUTO-ISI JAM PULANG DARI RIWAYAT ABSENSI ONLINE ===
				// Untuk record yang jam pulang-nya KOSONG padahal ada scan pulang online, dan durasi
				// kerja aktual (scan online pertama s.d terakhir) MELEBIHI "Waktu minimal bekerja (jam)"
				// pada shift, jam pulang diisi & disimpan otomatis (setPulangJamState + commit).
				// Memakai session terpisah dengan pola openSession + finally agar tidak bocor. Record
				// diproses saat sudah detached dari session baca, sehingga aman di-set di memori (untuk
				// tampilan) sekaligus dipersist lewat session khusus ini.
				Session sesiPulangOtomatis = null;
				try {
					int jmlDiproses = 0;
					int jmlTerisi = 0;
					sesiPulangOtomatis = HibernateUtil.openSession();
					for (StatuskehadiranKaryawanHarian skh : mapStatuskehadiranKaryawanHarian.values()) {
						if (skh != null && skh.getId() != null) {
							jmlDiproses++;
							Date hasilPulang = skh.autoUpdatePulangDariSejarah(sesiPulangOtomatis);
							if (hasilPulang != null) {
								jmlTerisi++;
							}
							// KOREKSI shadow: jam pulang tersimpan (State/Manual) lebih awal dari scan pulang
							// genuine terakhir (mis. tersimpan 07:31 padahal scan pulang asli 15:38 masuk lewat
							// jalur temp). Perbaiki agar kolom "Jam Pulang" & penggajian memakai scan asli.
							Date hasilKoreksi = skh.autoKoreksiPulangShadow(sesiPulangOtomatis);
							if (hasilKoreksi != null) {
								jmlTerisi++;
							}
						}
					}
					System.out.println("[AUTO-PULANG] pegawai="
							+ (pegawai == null ? "" : pegawai.getNama()) + " bulan=" + bulan + "/" + tahun
							+ " diproses=" + jmlDiproses + " jamPulangTerisiOtomatis=" + jmlTerisi);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:777");
				} finally {
					if (sesiPulangOtomatis != null) {
						try {
							if (sesiPulangOtomatis.isOpen()) {
								sesiPulangOtomatis.clear();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:784");
						}
						try {
							sesiPulangOtomatis.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:788");
						}
						try {
							if (sesiPulangOtomatis.isOpen()) {
								sesiPulangOtomatis.close();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:794");
						}
					}
				}

//				System.out.println("mapStatuskehadiranKaryawanHarian -> " + mapStatuskehadiranKaryawanHarian.size());

				for (int i = 1; i <= jumlahHari; i++) {

					try {
						calendar.set(Calendar.DATE, i);

						final Date tanggal = calendar.getTime();
						final Integer bln = calendar.get(Calendar.MONTH) + 1;
						final Integer thn = calendar.get(Calendar.YEAR);
						final Integer tgl = calendar.get(Calendar.DATE);
						final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

						final StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = mapStatuskehadiranKaryawanHarian
								.get(Common.dateFormat83.get().format(tanggal));
						LiburRutin liburRutin = statuskehadiranKaryawanHarian.getLiburRutin();
						LiburNasional liburNasional = statuskehadiranKaryawanHarian.getLiburNasional();

						MyFormRow row = new MyFormRow();
						row.setValign("top");

						List<String> urls = new ArrayList<String>();
						if (!statuskehadiranKaryawanHarian.getLokasiAbsenDatang().isEmpty()) {
							urls.add(statuskehadiranKaryawanHarian.getLokasiAbsenDatang());
						}
						if (!statuskehadiranKaryawanHarian.getFotoAbsenDatang().isEmpty()) {
							urls.add(toRelativeFotoUrl(statuskehadiranKaryawanHarian.getFotoAbsenDatang()));
						}

						List<String> urlsPulang = new ArrayList<String>();
						if (!statuskehadiranKaryawanHarian.getLokasiAbsenPulang().isEmpty()) {
							urlsPulang.add(statuskehadiranKaryawanHarian.getLokasiAbsenPulang());
						}
						if (!statuskehadiranKaryawanHarian.getFotoAbsenPulang().isEmpty()) {
							urlsPulang.add(toRelativeFotoUrl(statuskehadiranKaryawanHarian.getFotoAbsenPulang()));
						}

						String sebelumnya = statuskehadiranKaryawanHarian.retreive("sejarah");

						if (urls.isEmpty() && urlsPulang.isEmpty() && (sebelumnya == null || sebelumnya.isEmpty())) {
							new MyLabelAgakKecil().setParent(row);
						} else {

							MyDetail detail = new MyDetail();
							detail.setParent(row);
							detail.setOpen(false);

							Vbox vbox = new Vbox();
							vbox.setParent(detail);

							MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
							groupboxStyled.setParent(vbox);

							groupboxStyled.appendChild(new MyCaptionStyled("Info Kedatangan"));

							Box box = Common.isMobile() ? new Vbox() : new Hbox();
							box.setWidth("100%");
							box.setParent(groupboxStyled);

							for (String u : urls) {
								if (u.contains("iframe")) {
									MyHtml myHtml = new MyHtml(u);
									box.appendChild(myHtml);
								} else if (u.contains("maps")) {
									MyHtml myHtml = new MyHtml(
											"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
													+ u + "&amp;output=embed\"></iframe>");
									box.appendChild(myHtml);
								} else if (u.contains("download") || u.contains("/al?d=")) {
									MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
											+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\""
											+ u + "\"></image></a>");
									box.appendChild(myHtml);
								}
							}

							groupboxStyled = new MyGroupboxStyled();
							groupboxStyled.setParent(vbox);

							groupboxStyled.appendChild(new MyCaptionStyled("Info Kepulangan"));

							box = Common.isMobile() ? new Vbox() : new Hbox();
							box.setWidth("100%");
							box.setParent(groupboxStyled);

							for (String u : urlsPulang) {
								if (u.contains("maps")) {
									MyHtml myHtml = new MyHtml(
											"<iframe style=\"width:100%;height:200px\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\"  marginwidth=\"0\" src=\""
													+ u + "&amp;output=embed\"></iframe>");
									box.appendChild(myHtml);
								} else if (u.contains("download") || u.contains("/al?d=")) {
									MyHtml myHtml = new MyHtml("<a onclick=\"popupCenter({url: '" + u
											+ "', title: 'Foto', w: 1200, h: 600});\" ><image style=\"height:200px;\" src=\""
											+ u + "\"></image></a>");
									box.appendChild(myHtml);
								}
							}

							TreeMap<String, Map<String, String>> maps = statuskehadiranKaryawanHarian.ambilSejarah();

							System.out.println("hari -> " + i + " maps -> " + maps);

							if (!maps.isEmpty()) {
								groupboxStyled = new MyGroupboxStyled();
								groupboxStyled.setParent(vbox);

								groupboxStyled.appendChild(new MyCaptionStyled("Sejarah Absensi Online"));

								Grid grid = new Grid();
								grid.setSclass("dgrid");
								grid.setWidth("100%");
								grid.setParent(groupboxStyled);
								grid.setWidth("100%");
								grid.setHeight("100%");
								grid.setSclass("dgrif");

								Columns columns = new Columns();
								columns.setParent(grid);

								MyColumnConfig column = new MyColumnConfig("Tanggal");
								column.setParent(columns);
								column.setWidth("10%");

								column = new MyColumnConfig("Info");
								column.setParent(columns);

								column = new MyColumnConfig("Foto");
								column.setParent(columns);

								column = new MyColumnConfig("Lokasi");
								column.setParent(columns);

								Rows rowsData = new Rows();
								rowsData.setParent(grid);

								for (String key : maps.keySet()) {
									try {
										MyFormRow rowData = new MyFormRow();
										rowData.setValign("top");
										rowData.setParent(rowsData);
										try {
											rowData.appendChild(new MyLabelAgakKecil(
													Common.dateFormat5.get().format(Common.dateFormat9.get().parse(key))));
										} catch (Exception e) {
											rowData.appendChild(new MyLabelAgakKecil());
										}
										rowData.appendChild(new MyHtml(maps.get(key).containsKey(key + "_info")
												? "<div style='font-size:10px;'>" + maps.get(key).get(key + "_info")
														+ "</div>"
												: ""));
										A a;
										rowData.appendChild(a = new A(maps.get(key).containsKey(key + "_foto")
												? maps.get(key).get(key + "_foto")
												: ""));
										a.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.evalJavaScript(
														"popupCenter({url: '" + Common.jsEscape(((A) arg0.getTarget()).getLabel())
																+ "', title: 'Data', w: 1200, h: 600});");
											}
										});

										rowData.appendChild(a = new A(maps.get(key).containsKey(key + "_lokasi")
												? maps.get(key).get(key + "_lokasi")
												: ""));
										a.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Clients.evalJavaScript(
														"popupCenter({url: '" + Common.jsEscape(((A) arg0.getTarget()).getLabel())
																+ "', title: 'Data', w: 1200, h: 600});");
											}
										});
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:977");
									}
								}

								maps.clear();
								maps = null;
							}
						}

						if (liburRutin != null && liburRutin.getLibur()) {
							row.setStyle("border:0px;background: #d5f5dd;");
						}

						if (liburNasional != null) {
							row.setStyle("border:0px;background: pink;");
						}

						row.setParent(rows);

						RevisiHelper
								.createNewRevisi(StatuskehadiranKaryawanHarian.class, statuskehadiranKaryawanHarian,
										Common.dateFormat4.get().format(calendar.getTime())
												+ (liburNasional == null ? "" : " (" + liburNasional.toString() + ")"))
								.setParent(row);

						Vbox vboxMasukKeluar = new Vbox();
						vboxMasukKeluar.setWidth("100%");

						if (statuskehadiranKaryawanHarian.getMasukjamState() != null
								&& statuskehadiranKaryawanHarian.getPulangJamState() != null
								&& statuskehadiranKaryawanHarian.getPulangJamState()
										.before(statuskehadiranKaryawanHarian.getMasukjamState())) {
							new MyLabelAgakKecil("Pulang:"
									+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getPulangJamState()))
									.setParent(vboxMasukKeluar);
							new MyLabelAgakKecil("Masuk:"
									+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getMasukjamState()))
									.setParent(vboxMasukKeluar);
						} else {

							if (statuskehadiranKaryawanHarian.getTidakAdaKedatangan()) {
								new Label().setParent(vboxMasukKeluar);
							}

							else if (statuskehadiranKaryawanHarian.getMasukjamState() != null) {
								new MyLabelAgakKecil("Masuk:"
										+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getMasukjamState()))
										.setParent(vboxMasukKeluar);
							} else {
								Date m = statuskehadiranKaryawanHarian.getMasukjam();

								new MyLabelAgakKecil("Masuk:" + (m == null ? "" : Common.timeFormat.get().format(m)))
										.setParent(vboxMasukKeluar);
							}

							if (statuskehadiranKaryawanHarian.getTidakAdaKepulangan()) {
								new Label().setParent(vboxMasukKeluar);
							}

							else if (statuskehadiranKaryawanHarian.getPulangJamState() != null) {
								new MyLabelAgakKecil("Pulang:"
										+ Common.timeFormat.get().format(statuskehadiranKaryawanHarian.getPulangJamState()))
										.setParent(vboxMasukKeluar);
							} else {
								Date m = statuskehadiranKaryawanHarian.getPulangJam();

								// FALLBACK JAM PULANG AKTUAL:
								// getPulangJam() dapat mengembalikan null MESKIPUN pegawai SUDAH melakukan
								// scan pulang (mis. QR-CODE PULANG / absensi online). Penyebabnya adalah
								// aturan durasi kerja minimal shift (waktuBekerjaMinimal) di dalam
								// StatuskehadiranKaryawanHarian.getPulangJam(): bila selisih masuk->pulang
								// lebih pendek dari batas minimal shift, jam pulang di-null-kan untuk
								// keperluan perhitungan penggajian. Akibatnya kolom "Pulang:" tampak
								// KOSONG padahal data scan-nya ada. Agar operator tetap dapat MELIHAT jam
								// kepulangan yang sebenarnya (tanpa mengubah logika penggajian sama sekali),
								// kita ambil langsung jam scan pulang terakhir dari riwayat absensi online
								// (sejarah) lalu dari keterangan fingerprint sebagai cadangan. Kedua sumber
								// ini adalah data mentah scan yang TIDAK dikenai aturan durasi minimal.
								// FAIL-SAFE AUTO-ISI JAM PULANG (bila loop batch di atas terlewat pada baris ini):
								// jika jam pulang kosong TAPI memenuhi syarat "Waktu minimal bekerja (jam) <
								// (history_pulang - history_masuk)", isi & SIMPAN jam pulang di sini juga. Session
								// dibuka HANYA bila memang memenuhi syarat (hemat resource).
								if (m == null
										&& statuskehadiranKaryawanHarian.hitungPulangOtomatisDariSejarah() != null) {
									Session sesiPulangBaris = null;
									try {
										sesiPulangBaris = HibernateUtil.openSession();
										m = statuskehadiranKaryawanHarian.autoUpdatePulangDariSejarah(sesiPulangBaris);
									} catch (Exception exPulang) {
										exPulang.printStackTrace(); ais.common.ErrorAuditUtil.record(exPulang, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1066");
									} finally {
										if (sesiPulangBaris != null) {
											try {
												if (sesiPulangBaris.isOpen()) {
													sesiPulangBaris.clear();
												}
											} catch (Exception exPulang) { ais.common.ErrorAuditUtil.record(exPulang, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1073");
											}
											try {
												sesiPulangBaris.disconnect();
											} catch (Exception exPulang) { ais.common.ErrorAuditUtil.record(exPulang, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1077");
											}
											try {
												if (sesiPulangBaris.isOpen()) {
													sesiPulangBaris.close();
												}
											} catch (Exception exPulang) { ais.common.ErrorAuditUtil.record(exPulang, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1083");
											}
										}
									}
								}

								// Tetap tampilkan jam scan asli (read-only) walau belum memenuhi syarat auto-isi.
								if (m == null) {
									m = statuskehadiranKaryawanHarian.sampaiOtomatisUlangAbsenDariSejarah();
								}
								if (m == null) {
									m = statuskehadiranKaryawanHarian.sampaiOtomatisUlangAbsenDariKeterangan();
								}

								new MyLabelAgakKecil("Pulang:" + (m == null ? "" : Common.timeFormat.get().format(m)))
										.setParent(vboxMasukKeluar);

							}
						}

						final MyLabelAgakKecil jumlahJamMasuk = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
										+ ")");

						final MyLabelAgakKecil jumlahCepatKeluar = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
										+ ")");

						final MyLabelAgakKecil jumlahTerlambat = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
										+ ")");

						final MyLabelAgakKecil jumlahLemburMasuk = new MyLabelAgakKecil(
								Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
										+ Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
										+ ")");

						final MyLabelAgakKecil infoShift = new MyLabelAgakKecil(
								statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
										: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().toString());

						if (edit && statuskehadiranKaryawanHarian.getCutiDanIzin() == null) {

							new Label(statuskehadiranKaryawanHarian.getStatusabsensi() == null ? ""
									: statuskehadiranKaryawanHarian.getStatusabsensi().getNama()).setParent(row);

							vboxMasukKeluar.setParent(row);

							jumlahJamMasuk.setParent(row);

							final MyCheckboxConfig checkDetailJenisShiftPegawaiManual = new MyCheckboxConfig("Manual");
							checkDetailJenisShiftPegawaiManual.setChecked(
									statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiManual() != null);
							final Combobox detailJenisShiftPegawaiManual = new Combobox();

							Vbox vboxInfoShif = new Vbox();
							vboxInfoShif.setWidth("100%");
							vboxInfoShif.setParent(row);

							infoShift.setParent(vboxInfoShif);
							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								checkDetailJenisShiftPegawaiManual.setParent(vboxInfoShif);
							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								detailJenisShiftPegawaiManual.setParent(vboxInfoShif);

							detailJenisShiftPegawaiManual.setWidth("90%");
							EventListener eventListenerDetailJenisShiftPegawaiManual = new EventListener() {

								/**
								 * Menangani centang/lepas checkbox "Manual" pada kolom Shift sekaligus dipakai untuk
								 * inisialisasi kontrol saat baris pertama kali dirender.
								 *
								 * <p>Saat dicentang: combobox shift ditampilkan dan diisi ulang hanya dengan
								 * {@link DetailJenisShiftPegawai} milik {@link JenisShiftPunyaPegawai} pegawai ini yang
								 * masih berlaku pada tanggal baris ({@code berlakuMulai &le; tanggal} dan
								 * {@code berlakuSampai} null atau {@code &ge; tanggal}, hanya jenis shift aktif, diambil
								 * satu jenis shift terbaru lewat {@code setMaxResults(1)}); bila pegawai tidak punya jenis
								 * shift yang berlaku, daftar sengaja dikosongkan dengan {@code Restrictions.sqlRestriction("false")}
								 * sehingga hanya menyisakan pilihan "Shift dibuat otomatis". Saat dilepas, override
								 * {@code detailJenisShiftPegawaiManual} dikosongkan sehingga shift kembali mengikuti
								 * perhitungan otomatis.</p>
								 *
								 * <p><b>Efek samping:</b> selain mengubah tampilan, listener ini juga menulis kembali
								 * bulan/tahun/tanggal/minggu ke entity dan — HANYA bila dipicu event UI nyata
								 * ({@code arg0 != null}, bukan pemanggilan inisialisasi dengan {@code null}) — langsung
								 * mem-persist perubahan lewat {@link Common#refreshSaveOrUpdate(Object)} tanpa dialog
								 * konfirmasi. Semua label besaran penggajian (jam masuk, lembur, cepat keluar, terlambat)
								 * dan info shift dihitung ulang dari getter turunan entity setelah override berubah.</p>
								 *
								 * @param arg0 event ZK pemicu, atau {@code null} bila dipanggil untuk inisialisasi awal
								 *             baris (mode ini sengaja tidak menyimpan ke database)
								 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									detailJenisShiftPegawaiManual
											.setVisible(checkDetailJenisShiftPegawaiManual.isChecked());

									if (checkDetailJenisShiftPegawaiManual.isChecked()) {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiManual(
												(DetailJenisShiftPegawai) (detailJenisShiftPegawaiManual
														.getSelectedItem() == null ? null
																: detailJenisShiftPegawaiManual.getSelectedItem()
																		.getValue()));
										Session session = HibernateUtil.currentSession();
										List<Long> ids = session.createCriteria(JenisShiftPunyaPegawai.class)

												.add(Restrictions.eq("pegawai", pegawai))
												.createAlias("jenisShiftPegawai", "jenisShiftPegawai")

												.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
														Restrictions.eq("jenisShiftPegawai.aktif", true)))

												.add(Restrictions.le("jenisShiftPegawai.berlakuMulai", tanggal))
												.addOrder(Order.desc("jenisShiftPegawai.berlakuMulai"))
												.add(Restrictions.or(
														Restrictions.isNull("jenisShiftPegawai.berlakuSampai"),
														Restrictions.ge("jenisShiftPegawai.berlakuSampai", tanggal)))
												.setProjection(Projections.groupProperty("jenisShiftPegawai.id"))
												.setMaxResults(1).list();
										Criterion criterions = ids.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("jenisShiftPegawai.id", ids);
										Common.insertComboDanSemua(detailJenisShiftPegawaiManual,
												new String[] { "nama", "jenisShiftPegawai" }, "keterangan",
												DetailJenisShiftPegawai.class, "Shift dibuat otomatis", criterions);

										Common.selectComboItem(true, detailJenisShiftPegawaiManual,
												statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiManual());
										detailJenisShiftPegawaiManual.setReadonly(true);
									} else {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiManual(null);
									}

									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);

									if (arg0 != null) {
										Common.refreshSaveOrUpdate(statuskehadiranKaryawanHarian);
									}

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							};

							checkDetailJenisShiftPegawaiManual.addEventListener("onClick",
									eventListenerDetailJenisShiftPegawaiManual);

							try {
								eventListenerDetailJenisShiftPegawaiManual.onEvent(null);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1239");
								// TODO: handle exception
							}

							detailJenisShiftPegawaiManual.addEventListener("onChange", new EventListener() {

								/**
								 * Menyimpan pilihan shift manual yang baru dipilih operator pada kolom Shift. Berbeda
								 * dengan listener checkbox "Manual", method ini SELALU mem-persist (tidak ada cabang
								 * {@code arg0 != null}) karena hanya bisa terpicu oleh interaksi pemakai: override
								 * {@code detailJenisShiftPegawaiManual} beserta bulan/tahun/tanggal/minggu ditulis ke
								 * entity lalu disimpan lewat {@link Common#refreshSaveOrUpdate(Session, Object)} tanpa
								 * dialog konfirmasi.
								 *
								 * <p>Setelah menyimpan, seluruh label besaran penggajian (jam masuk, lembur, cepat
								 * keluar, terlambat) dan label info shift dihitung ulang dari getter turunan
								 * {@link StatuskehadiranKaryawanHarian} agar langsung mencerminkan shift baru.</p>
								 *
								 * @param arg0 event {@code onChange} dari combobox shift manual
								 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiManual(
											(DetailJenisShiftPegawai) (detailJenisShiftPegawaiManual
													.getSelectedItem() == null ? null
															: detailJenisShiftPegawaiManual.getSelectedItem()
																	.getValue()));
									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);
									Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							});

							final MyCheckboxConfig checkDetailJenisShiftPegawaiLembur = new MyCheckboxConfig("Manual");
							checkDetailJenisShiftPegawaiLembur.setChecked(
									statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiLembur() != null);
							final Combobox detailJenisShiftPegawaiLembur = new Combobox();
							vboxInfoShif = new Vbox();
							vboxInfoShif.setWidth("100%");
							vboxInfoShif.setParent(row);
							jumlahLemburMasuk.setParent(vboxInfoShif);

							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								detailJenisShiftPegawaiLembur.setParent(vboxInfoShif);
							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								checkDetailJenisShiftPegawaiLembur.setParent(vboxInfoShif);
							detailJenisShiftPegawaiLembur.setWidth("80%");

							final Timebox lamburMulai = new ais.ui.util.MyTimebox(
									statuskehadiranKaryawanHarian.getLamburMulai());

							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								vboxInfoShif.appendChild(lamburMulai);

							final Timebox lamburSampai = new ais.ui.util.MyTimebox(
									statuskehadiranKaryawanHarian.getLamburSampai());

							if (statuskehadiranKaryawanHarian.getDikunci() == null)
								vboxInfoShif.appendChild(lamburSampai);

							lamburMulai.setCols(3);
							lamburSampai.setCols(3);

							EventListener eventListenerDetailJenisShiftPegawaiLembur = new EventListener() {

								/**
								 * Kembaran {@code eventListenerDetailJenisShiftPegawaiManual} untuk kolom Lembur:
								 * menangani checkbox "Manual" lembur sekaligus dipakai untuk inisialisasi baris.
								 * Selain combobox shift lembur, listener ini juga menampilkan/menyembunyikan sepasang
								 * {@link Timebox} {@code lamburMulai}/{@code lamburSampai} sehingga operator dapat
								 * membatasi jendela jam lembur secara manual.
								 *
								 * <p>Query pengisian combobox identik dengan kolom Shift (hanya
								 * {@link DetailJenisShiftPegawai} dari {@link JenisShiftPunyaPegawai} pegawai ini yang
								 * berlaku pada tanggal baris, kosong-total lewat
								 * {@code Restrictions.sqlRestriction("false")} bila tidak ada), tetapi label pilihan
								 * bawaannya "Samakan dengan shift utama" — artinya lembur mengikuti shift utama bila
								 * tidak dipilih shift khusus. Melepas centang mengosongkan
								 * {@code detailJenisShiftPegawaiLembur}.</p>
								 *
								 * <p><b>Efek samping:</b> sama seperti kembarannya, perubahan langsung dipersist lewat
								 * {@link Common#refreshSaveOrUpdate(Object)} tanpa konfirmasi HANYA bila
								 * {@code arg0 != null}; pemanggilan inisialisasi dengan {@code null} sengaja tidak
								 * menyimpan. Seluruh label besaran penggajian dihitung ulang setelahnya.</p>
								 *
								 * @param arg0 event ZK pemicu, atau {@code null} bila dipanggil untuk inisialisasi awal
								 *             baris
								 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									detailJenisShiftPegawaiLembur
											.setVisible(checkDetailJenisShiftPegawaiLembur.isChecked());

									lamburMulai.setVisible(checkDetailJenisShiftPegawaiLembur.isChecked());
									lamburSampai.setVisible(checkDetailJenisShiftPegawaiLembur.isChecked());

									if (checkDetailJenisShiftPegawaiLembur.isChecked()) {

										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiLembur(
												(DetailJenisShiftPegawai) (detailJenisShiftPegawaiLembur
														.getSelectedItem() == null ? null
																: detailJenisShiftPegawaiLembur.getSelectedItem()
																		.getValue()));

										Session session = HibernateUtil.currentSession();
										List<Long> ids = session.createCriteria(JenisShiftPunyaPegawai.class)

												.add(Restrictions.eq("pegawai", pegawai))
												.createAlias("jenisShiftPegawai", "jenisShiftPegawai")

												.add(Restrictions.or(Restrictions.isNull("jenisShiftPegawai.aktif"),
														Restrictions.eq("jenisShiftPegawai.aktif", true)))

												.add(Restrictions.le("jenisShiftPegawai.berlakuMulai", tanggal))
												.addOrder(Order.desc("jenisShiftPegawai.berlakuMulai"))
												.add(Restrictions.or(
														Restrictions.isNull("jenisShiftPegawai.berlakuSampai"),
														Restrictions.ge("jenisShiftPegawai.berlakuSampai", tanggal)))
												.setProjection(Projections.groupProperty("jenisShiftPegawai.id"))
												.setMaxResults(1).list();
										Criterion criterions = ids.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("jenisShiftPegawai.id", ids);
										Common.insertComboDanSemua(detailJenisShiftPegawaiLembur,
												new String[] { "nama", "jenisShiftPegawai" }, "keterangan",
												DetailJenisShiftPegawai.class, "Samakan dengan shift utama",
												criterions);

										Common.selectComboItem(true, detailJenisShiftPegawaiLembur,
												statuskehadiranKaryawanHarian.getDetailJenisShiftPegawaiLembur());
										detailJenisShiftPegawaiLembur.setReadonly(true);
									} else {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiLembur(null);
									}

									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);

									if (arg0 != null) {
										Common.refreshSaveOrUpdate(statuskehadiranKaryawanHarian);
									}

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							};

							checkDetailJenisShiftPegawaiLembur.addEventListener("onClick",
									eventListenerDetailJenisShiftPegawaiLembur);

							try {
								eventListenerDetailJenisShiftPegawaiLembur.onEvent(null);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1413");
								// TODO: handle exception
							}

							EventListener eventListenerData = new EventListener() {

								/**
								 * Listener bersama yang dipasang pada tiga kontrol sekaligus — combobox shift lembur
								 * ({@code onChange}) serta kedua {@link Timebox} jam lembur {@code lamburMulai} dan
								 * {@code lamburSampai} — sehingga perubahan pada salah satunya menyimpan ketiga nilai
								 * secara utuh dan konsisten.
								 *
								 * <p>Menulis {@code detailJenisShiftPegawaiLembur}, {@code lamburMulai},
								 * {@code lamburSampai}, dan penanda periode (bulan/tahun/tanggal/minggu) ke entity, lalu
								 * mem-persist lewat {@link Common#refreshSaveOrUpdate(Session, Object)} tanpa dialog
								 * konfirmasi bila dipicu event nyata ({@code arg0 != null}). Seluruh label besaran
								 * penggajian dan info shift dihitung ulang dari getter turunan entity setelahnya.</p>
								 *
								 * @param arg0 event ZK pemicu dari salah satu dari ketiga kontrol lembur, atau
								 *             {@code null} bila dipanggil tanpa menyimpan
								 * @throws Exception diteruskan dari akses Hibernate/komponen ZK bila terjadi kegagalan
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {

									statuskehadiranKaryawanHarian.setDetailJenisShiftPegawaiLembur(
											(DetailJenisShiftPegawai) (detailJenisShiftPegawaiLembur
													.getSelectedItem() == null ? null
															: detailJenisShiftPegawaiLembur.getSelectedItem()
																	.getValue()));

									statuskehadiranKaryawanHarian.setLamburMulai(lamburMulai.getValue());
									statuskehadiranKaryawanHarian.setLamburSampai(lamburSampai.getValue());
									statuskehadiranKaryawanHarian.setBulan(bln);
									statuskehadiranKaryawanHarian.setTahun(thn);
									statuskehadiranKaryawanHarian.setTgl(tgl);
									statuskehadiranKaryawanHarian.setMinggu(hari);

									if (arg0 != null) {
										Session session = HibernateUtil.currentSession();
										Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);
									}

									jumlahJamMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahJamMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuJamMasuk())
											+ ")");

									jumlahLemburMasuk.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahLemburMasuk()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuLemburMasuk())
											+ ")");

									jumlahCepatKeluar.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahCepatKeluar()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuCepatKeluar())
											+ ")");

									jumlahTerlambat.setValue(Common.numberFormat.get()
											.format(statuskehadiranKaryawanHarian.getJumlahTerlambat()) + " ("
											+ Common.timeFormat1.get()
													.format(statuskehadiranKaryawanHarian.getWaktuTerlambat())
											+ ")");

									infoShift.setValue(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
													: statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai()
															.toString());
								}
							};

							detailJenisShiftPegawaiLembur.addEventListener("onChange", eventListenerData);
							lamburMulai.addEventListener("onChange", eventListenerData);
							lamburSampai.addEventListener("onChange", eventListenerData);

							jumlahCepatKeluar.setParent(row);
							jumlahTerlambat.setParent(row);

							if (statuskehadiranKaryawanHarian.getId() != null) {
								final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Abaikan Jarak");
								checkboxConfig.setParent(row);
								checkboxConfig.setChecked(statuskehadiranKaryawanHarian.getAbaikanJarak());
								checkboxConfig.addEventListener(Events.ON_CHECK, new EventListener() {

									/**
									 * Menyimpan langsung flag "Abaikan Jarak" pada baris kehadiran ini: bila dicentang,
									 * validasi radius/jarak lokasi absensi online tidak diberlakukan untuk tanggal
									 * tersebut, sehingga scan dari luar area kantor tetap dihitung sah.
									 *
									 * <p>Perubahan ditulis seketika tanpa dialog konfirmasi:
									 * {@code session.refresh()} menyegarkan entity dari database lebih dulu (menghindari
									 * menimpa perubahan bersamaan dari sesi lain), lalu {@code update()} dan
									 * {@code flush()} langsung dijalankan pada session ZK berjalan. Checkbox ini hanya
									 * dirender untuk baris yang sudah tersimpan ({@code getId() != null}); baris yang
									 * belum tersimpan hanya menampilkan nilai turunan dari
									 * {@link JenisShiftPunyaPegawai#getAbaikanJarak()} sebagai label read-only.</p>
									 *
									 * @param arg0 event {@code onCheck} dari checkbox "Abaikan Jarak"
									 * @throws Exception diteruskan dari akses Hibernate bila penyimpanan gagal
									 */
									@Override
									public void onEvent(Event arg0) throws Exception {
										Session session = HibernateUtil.currentSession();
										session.refresh(statuskehadiranKaryawanHarian);
										statuskehadiranKaryawanHarian.setAbaikanJarak(checkboxConfig.isChecked());
										session.update(statuskehadiranKaryawanHarian);
										session.flush();
									}
								});
							} else if (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() != null) {
								new Label(statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai().getAbaikanJarak()
										? "Ya"
										: "Tidak").setParent(row);
							} else {
								new Label().setParent(row);
							}


							statuskehadiranKaryawanHarian.renderKeteranganLink(row);
							
							

							// Tombol aksi (Ubah + Kunci) dirapikan menjadi satu button group yang ringkas
							// & sejajar di tengah kolom, bukan ikon lepas yang berjauhan.
							Hbox toolbar = new Hbox();
							toolbar.setSpacing("2px");
							toolbar.setAlign("center");
							toolbar.setStyle("display:inline-table;width:auto;background:#f8fafc;"
									+ "border:1px solid #e2e8f0;border-radius:10px;padding:2px 5px;");
							toolbar.setParent(row);
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
							button.setTooltiptext("Ubah Data");
							button.setVisible(edit && statuskehadiranKaryawanHarian.getDikunci() == null);

							button.addEventListener("onClick", new EventListener() {
								/**
								 * Membuka jendela modal "Ubah Waktu Kehadiran" untuk baris tanggal ini lewat
								 * {@link AbsensiKehadiranPegawaiHarianHelper#editJam(StatuskehadiranKaryawanHarian)}.
								 * Tombol pemicunya hanya terlihat bila panel berada dalam mode edit dan baris belum
								 * dikunci ({@code getDikunci() == null}); penyembunyian ini murni di sisi tampilan,
								 * sedangkan penulisan sebenarnya dilakukan di dalam {@code editJam}.
								 *
								 * @param event event {@code onClick} dari tombol Ubah Data
								 * @throws Exception diteruskan dari pembangunan jendela edit
								 */
								@Override
								public void onEvent(Event event) throws Exception {
									editJam(statuskehadiranKaryawanHarian);
								}

							});
							button.setParent(toolbar);

							GeneralValueObject.tampilKunci(toolbar, statuskehadiranKaryawanHarian, tbmuser,
									new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											loadData(null);
										}

									}, false);

						} else {

							row.appendChild(
									new MyLabelAgakKecil(statuskehadiranKaryawanHarian.getStatusabsensi().getNama()));

							row.appendChild(new MyLabelAgakKecil(statuskehadiranKaryawanHarian.ambilMasukjam() == null
									? ""
									: Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.ambilMasukjam())));

							row.appendChild(new MyLabelAgakKecil(statuskehadiranKaryawanHarian.ambilPulangjam() == null
									? ""
									: Common.dateFormat1.get().format(statuskehadiranKaryawanHarian.ambilPulangjam())));

							jumlahJamMasuk.setParent(row);
							infoShift.setParent(row);

							jumlahLemburMasuk.setParent(row);
							jumlahCepatKeluar.setParent(row);
							jumlahTerlambat.setParent(row);

							MyLabelAgakKecil l;
							(l = new MyLabelAgakKecil(statuskehadiranKaryawanHarian.getKeterangan())).setParent(row);
							l.setMultiline(true);
							row.appendChild(new MyLabelAgakKecil());
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AbsensiKehadiranPegawaiHarianHelper.java:1562");
					}
				}

			}
		});

	}

	/**
	 * Mengubah URL absolut foto absensi (yang menyertakan protokol dan host) menjadi URL relatif berbasis path
	 * {@code /al?d=...}, agar tautan foto tetap berfungsi walau alamat/IP server berubah setelah foto direkam.
	 * URL yang sudah relatif, kosong, {@code null}, atau tidak mengandung path {@code /al?d=} dikembalikan apa
	 * adanya (tidak diubah).
	 *
	 * @param url URL foto absensi yang akan dirapikan, boleh {@code null} atau kosong
	 * @return URL relatif mulai dari {@code /al?d=} bila ditemukan; selain itu URL asli tanpa perubahan
	 */
	private static String toRelativeFotoUrl(String url) {
		if (url == null || url.isEmpty()) return url;
		if (!url.startsWith("http")) return url;
		int idx = url.indexOf("/al?d=");
		if (idx >= 0) return url.substring(idx);
		return url;
	}
}
