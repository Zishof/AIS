package ais.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.sirs.CetakKartuPasienAction;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.master.sirs.util.CommonTarifItem;
import ais.action.report.Report;
import ais.action.report.format1.sirs.helper.PemeriksaanReportHelper;
import ais.action.report.helper.CommonReport;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.AlatMedisDiagnosaPenyakit;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.CetakKartuPasien;
import ais.database.model.sirs.DetailTransaksiLayanan;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.DiskonDetail;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Komunitas;
import ais.database.model.sirs.Minggu;
import ais.database.model.sirs.PajakDetail;
import ais.database.model.sirs.PajakMedis;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pemeriksaan;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanDiagnosaPenyakit;
import ais.ui.util.MyMessageboxConfig;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * Kelas utilitas statis untuk modul Sistem Informasi Rumah Sakit (SIRS) di AIS: menaungi logika
 * penjadwalan dokter/shift, pencetakan dokumen klinis (kartu pasien, status pasien, hasil diagnosa,
 * lembar tracer), perhitungan diskon/pajak/harga layanan medis, serta pembuatan kode/nomor rekam
 * medis pasien. Seluruh method bersifat statis (tidak ada instance state) dan sebagian besar
 * langsung memanggil sesi Hibernate aktif ({@link HibernateUtil#currentSession()}) untuk membaca
 * atau menulis data transaksional rumah sakit — kode kelas ini karenanya erat berpadanan dengan
 * struktur skema database {@code sirs.*} (mis. tabel {@code detail_transaksi_layanan},
 * {@code detail_transaksi_pasien}).
 *
 * <h2>Kelompok fungsi utama</h2>
 * <ol>
 * <li><b>Penjadwalan lokasi/shift/dokter</b> — {@link #getCurrentShift(Lokasi, Boolean)} mencari
 * shift yang sedang berlangsung pada suatu lokasi (dengan penanganan khusus shift yang melewati
 * tengah malam); {@link #initLokasiDanShift} (dua overload) membangun baris ZK berisi combobox
 * pemilihan lokasi (dan opsional shift) lengkap dengan event wiring untuk memberi tahu pemanggil
 * setiap kali pilihan berubah; {@link #initCalendarModel} dan {@link #createSimpleCalendarEvent}
 * membangun model kalender ZK ({@link SimpleCalendarModel}) dari jadwal praktik dokter
 * ({@link JadwalDokter}) untuk ditampilkan pada komponen {@link Calendars}.</li>
 * <li><b>Pencetakan dokumen klinis</b> — kelompok method {@code onCetak*}
 * ({@link #onCetakKartuPasien(Pendaftaran)}, {@link #onCetakStatusPasien(Pasien)},
 * {@link #onCetakHasilDiagnosaPasienRawatInap(DiagnosaPenyakit)},
 * {@link #onCetakHasilDiagnosaPasien(DiagnosaPenyakit)}, {@link #onCetakTracer(Pendaftaran)})
 * menyiapkan parameter laporan (termasuk barcode/QR kode pasien lewat
 * {@link BarcodeCommon#generateCRCode(String, java.io.File)}), memanggil
 * {@link Report#generateFileReport} untuk membangkitkan berkas PDF dari template Jasper, lalu
 * menampilkannya lewat {@link Report#tampil}. Pola parameter pada method-method ini sangat mirip
 * satu sama lain (data identitas pasien, kesatuan dinas TNI/PNS, riwayat kunjungan) sehingga
 * duplikasi logika cukup tinggi — perubahan pada satu template kemungkinan perlu diselaraskan
 * manual ke method serupa lainnya.</li>
 * <li><b>Diskon &amp; pajak medis</b> — {@link #getDiskonSekarang} dan {@link #getPajakSekarang}
 * mencari aturan diskon/pajak yang berlaku pada tanggal tertentu untuk kombinasi item/tindakan/alat
 * medis, asuransi, dan komunitas; {@link #getTotalDiskonDalamPersen} dan
 * {@link #getTotalPajakDalamPersen} menjumlahkan persentasenya; {@link #hitungDiskonRacikan} dan
 * {@link #hitungPajakRacikan} menerapkan perhitungan tersebut ke racikan obat (kumpulan item dengan
 * takaran masing-masing).</li>
 * <li><b>Harga jual &amp; HPP</b> — {@link #hitungHargaJualRacikan} menjumlahkan harga jual seluruh
 * komponen racikan; {@link #hitungHPP(ItemMedis, Session)} dan
 * {@link #hitungHargaBeli(ItemMedis, Session)} menghitung harga pokok penjualan/harga beli rata-rata
 * item medis langsung lewat SQL native atas tabel {@code sirs.detail_transaksi_pasien}.</li>
 * <li><b>Transaksi layanan &amp; kode pasien</b> — {@link #simpanTransaksiTindakan} mencatat baris
 * {@link DetailTransaksiLayanan} untuk satu tindakan yang dikenakan ke pasien (menghapus baris lama
 * yang belum lunas untuk pendaftaran/kartu cetak yang sama sebelum menyimpan ulang, mencegah baris
 * dobel saat aksi diulang); {@link #generateMaxByJenisPasien} dan {@link #generateCodePasien}
 * membangun kode rekam medis pasien baru berbasis jenis pasien (umum/dinas TNI-AD/AL/AU/PNS/siswa)
 * dengan pola awalan huruf ({@code D}, {@code S}, {@code L}) yang di-parse ulang dari kode existing
 * memakai fungsi SQL {@code to_number(replace(...))}, lalu memverifikasi keunikannya secara
 * rekursif bila terjadi tabrakan.</li>
 * <li><b>Data pendukung lain</b> — {@link #getMinggu(Integer, Integer)} memuat/membangun daftar
 * minggu kalender untuk suatu bulan-tahun (dipakai laporan periodik), memakai sesi Hibernate
 * dedikasi ({@code openSession()}) yang ditutup eksplisit di blok {@code finally} untuk mencegah
 * kebocoran koneksi; {@link #populateJasaRacik()} memastikan data induk "Jasa Racik" (Bubuk/Sirup/
 * Krim) selalu tersedia, membuatnya otomatis bila belum ada.</li>
 * </ol>
 *
 * <p>
 * <b>Catatan pengelolaan sesi Hibernate</b> — sebagian besar method di kelas ini memakai sesi
 * thread-bound lewat {@link HibernateUtil#currentSession()} (siklus hidupnya dikelola di luar
 * kelas ini, umumnya per-request), tetapi beberapa method (mis. {@link #getMinggu},
 * {@link #generateMaxByJenisPasien}, {@link #generateCodePasien}) sengaja membuka sesi dedikasi
 * lewat {@code HibernateUtil.getSessionFactory().openSession()} dan menutupnya sendiri di blok
 * {@code finally} — pola ini dipakai ketika method perlu memastikan sesi ditutup segera terlepas
 * dari sesi request yang sedang berjalan, dengan komentar kode menegaskan ini sebagai perbaikan atas
 * potensi kebocoran koneksi pada versi sebelumnya.
 * </p>
 */
public class CommonSirs {

	/**
	 * Mencari daftar {@link Shift} yang sedang berlaku pada {@code lokasi} tertentu berdasarkan jam
	 * saat ini. Bila {@code allTime} bernilai {@code true}, seluruh shift lokasi tersebut
	 * dikembalikan tanpa filter jam. Bila pencarian pada jam biasa (0-24) tidak menemukan hasil dan
	 * {@code allTime} bernilai {@code false}, pencarian diulang dengan menambahkan 24 jam ke waktu
	 * saat ini — menangani kasus shift yang jam mulainya lebih besar dari jam selesainya karena
	 * melewati tengah malam (mis. shift malam 22:00–06:00).
	 *
	 * @param lokasi  lokasi/cabang yang shift-nya dicari
	 * @param allTime bila {@code true}, abaikan filter jam dan kembalikan semua shift lokasi
	 * @return daftar {@link Shift} yang cocok; bisa kosong bila tidak ada shift yang berlaku
	 */
	@SuppressWarnings("unchecked")
	public static List<Shift> getCurrentShift(Lokasi lokasi, Boolean allTime) {

		Double sekarang = Double.parseDouble(Common.timeFormat2.get().format(new Date()));

		String sql = sekarang + " between mulaid and sampaid";

		Criterion crit = Restrictions.sqlRestriction(sql);

		List<Shift> shifts = HibernateUtil.currentSession().createCriteria(Shift.class)
				.add(Restrictions.eq("lokasi", lokasi)).add(allTime ? Restrictions.sqlRestriction("true") : crit)
				.list();

		if (!allTime && shifts.isEmpty()) {
			sekarang += +24.0;
			sql = sekarang + " between mulaid and sampaid";

			crit = Restrictions.sqlRestriction(sql);

			shifts = HibernateUtil.currentSession().createCriteria(Shift.class).add(Restrictions.eq("lokasi", lokasi))
					.add(allTime ? Restrictions.sqlRestriction("true") : crit).list();
		}

		return shifts;
	}

	/**
	 * Membangun baris ZK ({@link Row}) berisi combobox pemilihan {@link Lokasi} (beserta baris info
	 * "Toko" terkait bila lokasi memiliki toko), menyisipkannya ke {@code rows}, dan mendaftarkan
	 * event {@code onChange} yang meneruskan lokasi terpilih ke {@code myEventListener}. Bila
	 * {@code myLokasi} sudah diberikan, combobox langsung dinonaktifkan (terkunci pada lokasi
	 * tersebut); bila hanya ada satu pilihan lokasi, combobox otomatis memilihnya. Varian ini tidak
	 * menyertakan pemilihan shift — bandingkan dengan overload
	 * {@link #initLokasiDanShift(Lokasi, Shift, Rows, EventListener)}.
	 *
	 * @param myLokasi         lokasi yang sudah ditentukan sebelumnya (mengunci combobox), boleh
	 *                         {@code null} untuk membiarkan pengguna memilih
	 * @param rows             kontainer {@link Rows} ZK tempat baris combobox disisipkan
	 * @param myEventListener  listener yang dipanggil dengan lokasi terpilih setiap kali pilihan
	 *                         berubah
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya
	 */
	public static void initLokasiDanShift(Lokasi myLokasi, Rows rows, final EventListener myEventListener)
			throws Exception {
		final Label tokodata = new Label();
		final Row rowToko = new Row();
		Row row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		final Combobox lokasi;
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(lokasi, myLokasi);
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? Common.getCurrentLokasi()
						: lokasi.getSelectedItem().getValue());

				rowToko.setVisible(myLokasi != null && myLokasi.getToko() != null);

				myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi }));
				tokodata.setValue(myLokasi == null ? "" : myLokasi.getToko().getNama());
			}
		};
		lokasi.addEventListener("onChange", eventListener);

		Common.selectComboItem(lokasi, myLokasi);
		eventListener.onEvent(null);

		lokasi.setReadonly(true);
		if (lokasi.getSelectedItem() == null && lokasi.getChildren().size() == 1) {
			lokasi.setSelectedIndex(0);
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? Common.getCurrentLokasi()
					: lokasi.getSelectedItem().getValue());

			myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi }));
		}

		rowToko.setStyle("border:0px;background: transparent;");
		rowToko.setParent(rows);
		rowToko.appendChild(new Label(ais.common.Common.getBahasaConfig("Toko")));
		rowToko.appendChild(tokodata);

	}

	/**
	 * Varian lengkap {@link #initLokasiDanShift(Lokasi, Rows, EventListener)} yang menambahkan
	 * combobox pemilihan {@link Shift} berantai (<i>cascading</i>) di bawah combobox lokasi: setiap
	 * kali lokasi berubah, daftar shift dimuat ulang lewat {@link #getCurrentShift(Lokasi, Boolean)}
	 * untuk lokasi tersebut. Bila {@code myShift} sudah ditentukan, kedua combobox dikunci pada
	 * lokasi dan shift tersebut; bila belum, event awal dipicu manual ({@code eventListener.onEvent
	 * (null)}) agar tampilan langsung konsisten dengan pilihan default. Hasil akhir (lokasi + shift
	 * terpilih) diteruskan ke {@code myEventListener} setiap kali salah satu combobox berubah.
	 *
	 * @param myLokasi        lokasi yang sudah ditentukan sebelumnya, boleh {@code null}
	 * @param myShift         shift yang sudah ditentukan sebelumnya (mengunci kedua combobox), boleh
	 *                        {@code null}
	 * @param rows            kontainer {@link Rows} ZK tempat baris-baris combobox disisipkan
	 * @param myEventListener listener yang dipanggil dengan {@code [lokasi, shift]} terpilih setiap
	 *                        kali salah satu pilihan berubah
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya
	 */
	@SuppressWarnings("deprecation")
	public static void initLokasiDanShift(Lokasi myLokasi, Shift myShift, Rows rows,
			final EventListener myEventListener) throws Exception {
		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));

		final Combobox lokasi;
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift")));
		final Combobox shift;
		row.appendChild(shift = new Combobox());

		Common.selectComboItem(lokasi, myLokasi);
		lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");
		shift.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? Common.getCurrentLokasi()
						: lokasi.getSelectedItem().getValue());

				shift.setSelectedItem(null);
				Common.clear(shift);

				if (myLokasi != null) {
					Common.insertComboItems(shift, "nama", "jenisShift", CommonSirs.getCurrentShift(myLokasi, false));

					if (!shift.getChildren().isEmpty()) {
						shift.setSelectedIndex(0);
					}
				}

				Shift myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());

				myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi, myShift }));
			}
		};
		lokasi.addEventListener("onChange", eventListener);

		EventListener shiftEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null
						: lokasi.getSelectedItem().getValue());
				Shift myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());
				myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi, myShift }));

			}
		};

		shift.addEventListener("onChange", shiftEventListener);

		if (myShift != null) {
			lokasi.setDisabled(true);
			shift.setDisabled(true);
			Common.insertCombo(shift, "nama", Shift.class);

			Common.selectComboItem(lokasi, myLokasi);
			Common.selectComboItem(shift, myShift);
		} else {
			eventListener.onEvent(null);
		}

		lokasi.setReadonly(true);
		if (lokasi.getSelectedItem() == null && lokasi.getChildren().size() == 1) {
			lokasi.setSelectedIndex(0);
			myLokasi = (Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue());
			myShift = (Shift) (shift.getSelectedItem() == null ? null : shift.getSelectedItem().getValue());
			myEventListener.onEvent(new Event("", lokasi, new Object[] { myLokasi, myShift }));
		}

		shiftEventListener.onEvent(null);
	}

	/** Varian ringkas {@link #initCalendarModel(Lokasi, Dokter, Poly, Calendars, Boolean, String)} tanpa penyesuaian jam tampil otomatis dan tanpa filter jenis poli ({@code sesuaikan=false}, {@code jenis=null}). */
	public static void initCalendarModel(Lokasi myLokasi, Dokter myDokter, Poly myPoly, Calendars calendars) {
		initCalendarModel(myLokasi, myDokter, myPoly, calendars, false, null);
	}

	/**
	 * Membangun {@link SimpleCalendarModel} berisi jadwal praktik dokter ({@link JadwalDokter}) untuk
	 * rentang tanggal yang ditampilkan {@code calendars} (dari {@code getBeginDate()} sampai
	 * {@code getEndDate()}), difilter menurut hari dalam minggu, lokasi, dokter, poli, jenis poli, dan
	 * masa berlaku jadwal ({@code jadwalDokterDimulai}/{@code jadwalDokterSampai}). Setiap jadwal yang
	 * cocok diubah menjadi satu {@link SimpleCalendarEvent} lewat
	 * {@link #createSimpleCalendarEvent(JadwalDokter, Calendar)} dan ditambahkan ke model.
	 *
	 * <p>
	 * Bila {@code sesuaikan} bernilai {@code true}, method ini juga melacak jam mulai paling awal
	 * ({@code minjam}) dan jam selesai paling akhir ({@code maxjam}) dari seluruh shift yang muncul,
	 * lalu menyetel jam tampil awal/akhir komponen {@link Calendars} ke rentang tersebut — sehingga
	 * kalender secara otomatis "menyesuaikan diri" menampilkan hanya rentang jam yang relevan, bukan
	 * 24 jam penuh.
	 * </p>
	 *
	 * @param myLokasi  filter lokasi; {@code null} berarti semua lokasi
	 * @param myDokter  filter dokter; {@code null} berarti semua dokter
	 * @param myPoly    filter poli; {@code null} berarti semua poli
	 * @param calendars komponen ZK {@link Calendars} yang model dan rentang jam tampilnya diisi/diubah
	 * @param sesuaikan bila {@code true}, jam tampil kalender disesuaikan otomatis ke rentang shift
	 *                  yang ditemukan
	 * @param jenis     filter jenis poli ({@code poly.jenis}); {@code null} berarti semua jenis
	 */
	@SuppressWarnings("unchecked")
	public static void initCalendarModel(Lokasi myLokasi, Dokter myDokter, Poly myPoly, Calendars calendars,
			Boolean sesuaikan, String jenis) {

		Session session = HibernateUtil.currentSession();

		SimpleCalendarModel cm = new SimpleCalendarModel();
		Calendar current = Calendar.getInstance();
		current.setTime(calendars.getBeginDate());

		int minjam = 23;
		int maxjam = 0;

		while (current.getTime().before(calendars.getEndDate())) {

			String currHari = Common.haris[current.get(Calendar.DAY_OF_WEEK) - 1];
			List<JadwalDokter> jadwalDokter = session.createCriteria(JadwalDokter.class)
					.createAlias("poly", "poly", Criteria.LEFT_JOIN)
					.add(jenis == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("poly.jenis", jenis))

					.add(Restrictions.eq("hari", currHari))
					.add(Restrictions.or(Restrictions.isNull("jadwalDokterDimulai"),
							Restrictions.le("jadwalDokterDimulai", current.getTime())))
					.add(Restrictions.or(Restrictions.isNull("jadwalDokterSampai"),
							Restrictions.ge("jadwalDokterSampai", current.getTime())))
					.add(myLokasi == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("lokasi", myLokasi))
					.add(myDokter == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dokter", myDokter))
					.add(myPoly == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("poly", myPoly))

					.list();
			for (JadwalDokter myJadwalDokter : jadwalDokter) {
				cm.add(CommonSirs.createSimpleCalendarEvent(myJadwalDokter, current));

				if (myJadwalDokter.getShift() != null && sesuaikan) {
					Calendar dimulai = Calendar.getInstance();
					dimulai.setTime(myJadwalDokter.getShift().getMulai());

					if (dimulai.get(Calendar.HOUR_OF_DAY) <= minjam) {
						minjam = dimulai.get(Calendar.HOUR_OF_DAY);
					}

					Calendar sampai = Calendar.getInstance();
					sampai.setTime(myJadwalDokter.getShift().getSampai());

					if (sampai.get(Calendar.HOUR_OF_DAY) >= maxjam) {
						maxjam = sampai.get(Calendar.HOUR_OF_DAY);
					}
				}

			}

			current.set(Calendar.DATE, current.get(Calendar.DATE) + 1);
		}

		calendars.setModel(cm);

		if (sesuaikan && cm.size() > 0 && minjam < maxjam) {
			try {
				calendars.setBeginTime(minjam);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:307");
			}
			try {
				calendars.setEndTime(maxjam);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:311");
			}
		}
	}

	/**
	 * Mengubah satu {@link JadwalDokter} (jadwal praktik terkait shift tertentu) menjadi satu
	 * {@link SimpleCalendarEvent} untuk tanggal {@code current}: jam mulai/selesai event diambil dari
	 * jam mulai/selesai {@link Shift} milik jadwal tersebut, digabungkan dengan tanggal
	 * {@code current}. Bila jam mulai shift lebih besar dari jam selesainya (shift melewati tengah
	 * malam), tanggal selesai event dipaksa ke akhir hari ({@code 23:59:59}) alih-alih benar-benar
	 * menghitung ke hari berikutnya. Warna header/konten event diambil dari kolom {@code warna}
	 * jadwal (format {@code "headerColor,contentColor"}) bila diisi; teks konten event berisi ringkasan
	 * dokter, poli, lokasi, dan masa berlaku jadwal.
	 *
	 * @param myJadwalDokter jadwal praktik dokter yang akan direpresentasikan sebagai event kalender
	 * @param current        tanggal spesifik (dalam rentang berulang) yang dipasangkan dengan jam
	 *                       shift milik jadwal ini
	 * @return {@link SimpleCalendarEvent} terkunci ({@code locked=true}) siap ditambahkan ke model
	 *         kalender
	 */
	public static SimpleCalendarEvent createSimpleCalendarEvent(JadwalDokter myJadwalDokter, Calendar current) {
		Calendar m = Calendar.getInstance();
		m.setTime(myJadwalDokter.getShift().getMulai());
		Calendar s = Calendar.getInstance();
		s.setTime(myJadwalDokter.getShift().getSampai());

		Calendar dimulai = Calendar.getInstance();
		dimulai.setTime(current.getTime());
		dimulai.set(Calendar.HOUR_OF_DAY, m.get(Calendar.HOUR_OF_DAY));
		dimulai.set(Calendar.MINUTE, m.get(Calendar.MINUTE));
		dimulai.set(Calendar.SECOND, m.get(Calendar.SECOND));

		Calendar sampai = Calendar.getInstance();
		sampai.setTime(current.getTime());
		sampai.set(Calendar.HOUR_OF_DAY, s.get(Calendar.HOUR_OF_DAY));
		sampai.set(Calendar.MINUTE, s.get(Calendar.MINUTE));
		sampai.set(Calendar.SECOND, s.get(Calendar.SECOND));

		SimpleCalendarEvent sce = new SimpleCalendarEvent();
		sce.setLocked(true);
		sce.setTitle(myJadwalDokter.getId() + "");

		if (dimulai.getTime().after(sampai.getTime())) {
			sce.setBeginDate(dimulai.getTime());
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(sampai.getTime());
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			sce.setEndDate(calendar.getTime());
		} else {
			sce.setBeginDate(dimulai.getTime());
			sce.setEndDate(sampai.getTime());
		}

		if (myJadwalDokter.getWarna() != null) {
			try {
				String[] colors = ((String) myJadwalDokter.getWarna()).split(",");
				sce.setHeaderColor(colors[0]);
				sce.setContentColor(colors[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:356");
			}
		}

		Dokter dokter = myJadwalDokter.getDokter();
		Lokasi lokasi = myJadwalDokter.getLokasi();
		String poly = myJadwalDokter.getPoly() == null ? "" : myJadwalDokter.getPoly().getNama();
		sce.setContent((dokter == null ? "" : (dokter == null ? "" : dokter.getNama())) + " -> " + poly + " -> "
				+ (lokasi == null ? "" : (lokasi == null ? "" : lokasi.getNama())) + " "
				+ (myJadwalDokter.getJadwalDokterDimulai() == null ? ""
						: " -> Berlaku " + Common.dateFormat2.get().format(myJadwalDokter.getJadwalDokterDimulai()))

				+ (myJadwalDokter.getJadwalDokterSampai() == null ? ""
						: " s.d " + Common.dateFormat2.get().format(myJadwalDokter.getJadwalDokterSampai()))

				+ "");
		return sce;
	}

	/**
	 * Mencatat/menampilkan pencetakan kartu pasien untuk satu {@link Pendaftaran}. Bila belum ada
	 * record {@link CetakKartuPasien} untuk pendaftaran tersebut, method ini membuat satu record baru
	 * (dengan kode dibangkitkan lewat {@link Common#generateCode}) dan mencatat transaksi tindakan
	 * "Pembuatan Kartu" ({@code ConstantValues.PEMBUATAN_KARTU}) lewat
	 * {@link #simpanTransaksiTindakan(Pasien, Tindakan, KelasPerawatan, Lokasi, Double, Pendaftaran,
	 * CetakKartuPasien)} — sehingga biaya pembuatan kartu hanya dikenakan sekali per pendaftaran
	 * meskipun tombol cetak ditekan berulang kali. Pencetakan fisik dilakukan lewat
	 * {@link CetakKartuPasienAction#onCetakKartu(Pasien)} yang selalu dipanggil terlepas dari apakah
	 * record baru dibuat atau sudah ada sebelumnya.
	 *
	 * @param pendaftaran pendaftaran pasien yang kartunya akan dicetak
	 * @throws Exception diteruskan dari operasi Hibernate/pembuatan kode/pencetakan
	 */
	@SuppressWarnings({})
	public static void onCetakKartuPasien(Pendaftaran pendaftaran) throws Exception {
		Session session = HibernateUtil.currentSession();
		CetakKartuPasien cetakKartuPasien = (CetakKartuPasien) session.createCriteria(CetakKartuPasien.class)
				.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();

		if (cetakKartuPasien == null) {
			cetakKartuPasien = new CetakKartuPasien();
			cetakKartuPasien.setTanggal(new Date());
			cetakKartuPasien.setPasien(pendaftaran.getPasien());
			String mykode = Common.generateCode(CetakKartuPasien.class, 10, "CETAK-KARTU", pendaftaran.getLokasi());
			cetakKartuPasien.setKode(mykode);
			cetakKartuPasien.setKeterangan("");
			cetakKartuPasien.setPendaftaran(pendaftaran);
			if (cetakKartuPasien.getLokasi() == null) {
				cetakKartuPasien.setLokasi(pendaftaran.getLokasi());
			}
			session.save(cetakKartuPasien);

			CommonSirs.simpanTransaksiTindakan(cetakKartuPasien.getPasien(), ConstantValues.PEMBUATAN_KARTU,
					ConstantValues.kelasNormal, pendaftaran.getLokasi(), 1.0, pendaftaran, cetakKartuPasien);
		}

		CetakKartuPasienAction.onCetakKartu(pendaftaran.getPasien());

	}

	/**
	 * Membangkitkan dan menampilkan laporan PDF "Data Identitas Pasien" (template
	 * {@code sirs/data_identitas_pasien}). Method ini membuat berkas barcode/QR sementara dari kode
	 * pasien lewat {@link BarcodeCommon#generateCRCode(String, java.io.File)}, mengumpulkan seluruh
	 * data identitas pasien (nama, kesatuan dinas TNI-AD/AL/AU/PNS, pangkat, NIP, kontak, status
	 * perkawinan, agama, pendidikan, pekerjaan, tanggal kunjungan pertama, tanggal lahir, alamat,
	 * tanggal registrasi) ke sebuah {@link Map} parameter, lalu menyerahkannya ke
	 * {@link Report#generateFileReport} dan {@link Report#tampil} untuk dirender dan ditampilkan ke
	 * pengguna. Seluruh kegagalan ditangkap dan dicatat lewat {@code ErrorAuditUtil.record}; method
	 * ini tidak melempar exception keluar maupun mengembalikan nilai.
	 *
	 * @param pasien pasien yang datanya akan dicetak
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakStatusPasien(Pasien pasien) {

		try {

			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			File file = Report.generateFileReport("sirs/data_identitas_pasien", Report.PDF, parameters,
					"sirs/data_identitas_pasien", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/data_identitas_pasien");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:460");
		}
	}

	/**
	 * Membangkitkan dan menampilkan laporan PDF "Diagnosa Pasien Rawat Inap" (template
	 * {@code sirs/diagnosa_pasien_rawat_inap_satuan}) untuk satu {@link DiagnosaPenyakit}. Selain data
	 * identitas pasien (sama seperti {@link #onCetakStatusPasien(Pasien)}), laporan ini menyertakan
	 * data spesifik rawat inap dari {@link Pendaftaran} terkait (nomor registrasi, kelas/ruang/kamar
	 * perawatan, tempat tidur), riwayat pemeriksaan berdasarkan jenis (keluhan/riwayat/periksa lewat
	 * {@link PemeriksaanReportHelper}), daftar resep (item satuan maupun racikan beserta detailnya),
	 * daftar tindakan dan alat medis yang dipakai, serta enam kolom diagnosa awal/akhir. Berkas
	 * barcode/QR dibuat sekali dari kode pasien. Seluruh kegagalan ditangkap dan dicatat; method ini
	 * tidak melempar exception keluar.
	 *
	 * @param diagnosaPenyakit catatan diagnosa rawat inap yang akan dicetak, dari sini
	 *                         {@link Pendaftaran} dan {@link Pasien} terkait diturunkan
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakHasilDiagnosaPasienRawatInap(DiagnosaPenyakit diagnosaPenyakit) {

		Pendaftaran pendaftaran = diagnosaPenyakit.getPendaftaran();

		try {
			Pasien pasien = pendaftaran.getPasien();
			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();
			System.out.println("barcode = " + barcode);

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			Common.insertProperty(DiagnosaPenyakit.class, diagnosaPenyakit, parameters, "diagnosa");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pendaftaran.getTanggalPendaftaran() == null ? ""
					: Common.dateFormat3.get().format(pendaftaran.getTanggalPendaftaran()));

			parameters.put("noreg", pendaftaran.getKode());
			parameters.put("kelas",
					pendaftaran.getKelasPerawatan() == null ? "" : pendaftaran.getKelasPerawatan().getNama());
			parameters.put("ruang",
					pendaftaran.getRuangPerawatan() == null ? "" : pendaftaran.getRuangPerawatan().getNama());
			parameters.put("kamar",
					pendaftaran.getKamarPerawatan() == null ? "" : pendaftaran.getKamarPerawatan().getNama());
			parameters.put("bed", pendaftaran.getTempatTidur() == null ? "" : pendaftaran.getTempatTidur().getNama());

			parameters.put("id", diagnosaPenyakit.getId());

			parameters.put("dokter_pemeriksa",
					diagnosaPenyakit.getDokter() == null ? "-" : diagnosaPenyakit.getDokter().toString());

			parameters.put("poli_diperiksa",
					diagnosaPenyakit.getPoly() == null ? "-" : diagnosaPenyakit.getPoly().toString());

			parameters.put("waktu_diperiksa", diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal()));

			List dataPemeriksaan = new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_KELUHAN).getHasil();

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_RIWAYAT).getHasil());

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_PERIKSA).getHasil());

			JRMapCollectionDataSource data_keluhan = new JRMapCollectionDataSource(dataPemeriksaan);

			parameters.put("data_keluhan", data_keluhan);

			parameters.put("kode_rm", diagnosaPenyakit.getKode());
			parameters.put("menular", diagnosaPenyakit.getApakahMenular());
			parameters.put("d1",
					diagnosaPenyakit.getDiagnosaAwal1() == null ? "" : diagnosaPenyakit.getDiagnosaAwal1().toString());
			parameters.put("d2", diagnosaPenyakit.getDiagnosaAkhir1() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir1().toString());
			parameters.put("d3",
					diagnosaPenyakit.getDiagnosaAwal2() == null ? "" : diagnosaPenyakit.getDiagnosaAwal2().toString());
			parameters.put("d4", diagnosaPenyakit.getDiagnosaAkhir2() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir2().toString());
			parameters.put("d5",
					diagnosaPenyakit.getDiagnosaAwal3() == null ? "" : diagnosaPenyakit.getDiagnosaAwal3().toString());
			parameters.put("d6", diagnosaPenyakit.getDiagnosaAkhir3() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir3().toString());

			Session session = HibernateUtil.currentSession();
			List<ResepDetail> resepDetails = session.createCriteria(ResepDetail.class).addOrder(Order.desc("id"))
					.createAlias("resep", "resep").add(Restrictions.eq("resep.diagnosaPenyakit", diagnosaPenyakit))
					.list();

			List<String> strings = new ArrayList<String>();
			for (ResepDetail resepDetail : resepDetails) {
				if (resepDetail.getItem() != null) {
					strings.add(
							resepDetail.getItem().getNama() + ": " + Common.numberFormat.get().format(resepDetail.getJumlah())
									+ " " + (resepDetail.getItem().getSatuanItem() == null ? ""
											: resepDetail.getItem().getSatuanItem().getNama()));
				} else if (resepDetail.getRacikan() != null) {
					List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
							.add(Restrictions.eq("racikan", resepDetail.getRacikan())).list();
					for (RacikanDetail racikanDetail : racikanDetails) {
						strings.add(racikanDetail.getItem().getNama() + ": "
								+ Common.numberFormat.get().format(racikanDetail.getJumlah()) + " "
								+ (racikanDetail.getItem().getSatuanItem() == null ? ""
										: racikanDetail.getItem().getSatuanItem().getNama()));
					}
				}
			}

			parameters.put("resep", strings.toString());

			List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = session
					.createCriteria(TindakanDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit : tindakanDiagnosaPenyakits) {
				if (tindakanDiagnosaPenyakit.getTindakan() != null) {
					strings.add(tindakanDiagnosaPenyakit.getTindakan().getNama());
				}
			}
			parameters.put("tindakan", strings.toString());

			List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = session
					.createCriteria(AlatMedisDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit : alatMedisDiagnosaPenyakits) {
				if (alatMedisDiagnosaPenyakit.getAlatMedis() != null) {
					strings.add(alatMedisDiagnosaPenyakit.getAlatMedis().getNama());
				}
			}
			parameters.put("alatMedis", strings.toString());

			parameters.put("catatan", diagnosaPenyakit.getKeterangan());

			File file = Report.generateFileReport("sirs/diagnosa_pasien_rawat_inap_satuan", Report.PDF, parameters,
					"sirs/diagnosa_pasien_rawat_inap_satuan", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/diagnosa_pasien_rawat_inap_satuan"); 

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:623");
		}

	}

	/**
	 * Varian rawat jalan dari {@link #onCetakHasilDiagnosaPasienRawatInap(DiagnosaPenyakit)}:
	 * membangkitkan dan menampilkan laporan PDF "Diagnosa Pasien" (template
	 * {@code sirs/diagnosa_pasien}) tanpa data kelas/ruang/kamar/tempat tidur rawat inap, dengan
	 * pasien diambil langsung dari {@code diagnosaPenyakit.getPasien()} alih-alih lewat
	 * {@link Pendaftaran}. Isi laporan lainnya (riwayat pemeriksaan, resep, tindakan, alat medis, enam
	 * kolom diagnosa) identik strukturnya dengan varian rawat inap. Seluruh kegagalan ditangkap dan
	 * dicatat; method ini tidak melempar exception keluar.
	 *
	 * @param diagnosaPenyakit catatan diagnosa rawat jalan yang akan dicetak
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakHasilDiagnosaPasien(DiagnosaPenyakit diagnosaPenyakit) {

		try {

			Pasien pasien = diagnosaPenyakit.getPasien();

			final File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();
			System.out.println("barcode = " + barcode);

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			Common.insertProperty(DiagnosaPenyakit.class, diagnosaPenyakit, parameters, "diagnosa");
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi()));

			parameters.put("id", diagnosaPenyakit.getId());

			parameters.put("dokter_pemeriksa",
					diagnosaPenyakit.getDokter() == null ? "-" : diagnosaPenyakit.getDokter().toString());

			parameters.put("poli_diperiksa",
					diagnosaPenyakit.getPoly() == null ? "-" : diagnosaPenyakit.getPoly().toString());

			parameters.put("waktu_diperiksa", diagnosaPenyakit.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal()));

			List dataPemeriksaan = new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_KELUHAN).getHasil();

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_RIWAYAT).getHasil());

			dataPemeriksaan.add(new HashMap());
			dataPemeriksaan.addAll(new PemeriksaanReportHelper(diagnosaPenyakit, Pemeriksaan.JENIS_PERIKSA).getHasil());

			JRMapCollectionDataSource data_keluhan = new JRMapCollectionDataSource(dataPemeriksaan);

			parameters.put("data_keluhan", data_keluhan);

			parameters.put("kode_rm", diagnosaPenyakit.getKode());
			parameters.put("menular", diagnosaPenyakit.getApakahMenular());
			parameters.put("d1",
					diagnosaPenyakit.getDiagnosaAwal1() == null ? "" : diagnosaPenyakit.getDiagnosaAwal1().toString());
			parameters.put("d2", diagnosaPenyakit.getDiagnosaAkhir1() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir1().toString());
			parameters.put("d3",
					diagnosaPenyakit.getDiagnosaAwal2() == null ? "" : diagnosaPenyakit.getDiagnosaAwal2().toString());
			parameters.put("d4", diagnosaPenyakit.getDiagnosaAkhir2() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir2().toString());
			parameters.put("d5",
					diagnosaPenyakit.getDiagnosaAwal3() == null ? "" : diagnosaPenyakit.getDiagnosaAwal3().toString());
			parameters.put("d6", diagnosaPenyakit.getDiagnosaAkhir3() == null ? ""
					: diagnosaPenyakit.getDiagnosaAkhir3().toString());

			Session session = HibernateUtil.currentSession();
			List<ResepDetail> resepDetails = session.createCriteria(ResepDetail.class).addOrder(Order.desc("id"))
					.createAlias("resep", "resep").add(Restrictions.eq("resep.diagnosaPenyakit", diagnosaPenyakit))
					.list();

			List<String> strings = new ArrayList<String>();
			for (ResepDetail resepDetail : resepDetails) {
				if (resepDetail.getItem() != null) {
					strings.add(
							resepDetail.getItem().getNama() + ": " + Common.numberFormat.get().format(resepDetail.getJumlah())
									+ " " + (resepDetail.getItem().getSatuanItem() == null ? ""
											: resepDetail.getItem().getSatuanItem().getNama()));
				} else if (resepDetail.getRacikan() != null) {
					List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
							.add(Restrictions.eq("racikan", resepDetail.getRacikan())).list();
					for (RacikanDetail racikanDetail : racikanDetails) {
						strings.add(racikanDetail.getItem().getNama() + ": "
								+ Common.numberFormat.get().format(racikanDetail.getJumlah()) + " "
								+ (racikanDetail.getItem().getSatuanItem() == null ? ""
										: racikanDetail.getItem().getSatuanItem().getNama()));
					}
				}
			}

			parameters.put("resep", strings.toString());

			List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = session
					.createCriteria(TindakanDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit : tindakanDiagnosaPenyakits) {
				if (tindakanDiagnosaPenyakit.getTindakan() != null) {
					strings.add(tindakanDiagnosaPenyakit.getTindakan().getNama());
				}
			}
			parameters.put("tindakan", strings.toString());

			List<AlatMedisDiagnosaPenyakit> alatMedisDiagnosaPenyakits = session
					.createCriteria(AlatMedisDiagnosaPenyakit.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).list();
			strings = new ArrayList<String>();
			for (AlatMedisDiagnosaPenyakit alatMedisDiagnosaPenyakit : alatMedisDiagnosaPenyakits) {
				if (alatMedisDiagnosaPenyakit.getAlatMedis() != null) {
					strings.add(alatMedisDiagnosaPenyakit.getAlatMedis().getNama());
				}
			}
			parameters.put("alatMedis", strings.toString());

			parameters.put("catatan", diagnosaPenyakit.getKeterangan());

			File file = Report.generateFileReport("sirs/diagnosa_pasien", Report.PDF, parameters,
					"sirs/diagnosa_pasien", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/diagnosa_pasien");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:777");
		}
	}

	/**
	 * Membangkitkan dan menampilkan laporan PDF "Tracer Pasien" (template {@code sirs/tracer_pasien}),
	 * yaitu lembar pelacak lokasi rekam medis fisik pasien yang biasa diselipkan sebagai penanda saat
	 * berkas rekam medis dikeluarkan dari rak penyimpanan. Parameter yang disertakan mencakup barcode
	 * kode pendaftaran, tanggal pendaftaran, nomor rekam medis, nama pasien, nama poli, nomor antrian,
	 * dan nama dokter. Seluruh kegagalan ditangkap dan dicatat; method ini tidak melempar exception
	 * keluar.
	 *
	 * @param pendaftaran pendaftaran yang lembar tracer-nya akan dicetak
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onCetakTracer(Pendaftaran pendaftaran) {

		try {

			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pendaftaran.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pendaftaran.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();

			Map parameters = new HashMap();
			parameters.put("pendaftaran", pendaftaran.getId());
			parameters.put("mybarcode", barcode);
			parameters.put("tanggalpendaftaran", pendaftaran.getTanggalPendaftaran());
			parameters.put("mr", pendaftaran.getPasien() == null ? "" : pendaftaran.getPasien().getKode());

			parameters.put("nama_pasien", pendaftaran.getPasien() == null ? "" : pendaftaran.getPasien().getNama());
			parameters.put("nama_poli", pendaftaran.getPoly() == null ? "" : pendaftaran.getPoly().getNama());
			parameters.put("nomor_antrian", pendaftaran.getNomorAntrian());
			parameters.put("nama_dokter", pendaftaran.getDokter() == null ? "" : pendaftaran.getDokter().getNama());

			Common.insertProperty(Pendaftaran.class, pendaftaran, parameters, "");

			List<Map> maps = new ArrayList<Map>();
			maps.add(parameters);

			parameters.put("maps", maps);

			File file = Report.generateFileReport("sirs/tracer_pasien", Report.PDF, parameters, "sirs/tracer_pasien",
					new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/tracer_pasien");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonSirs.java:819");
		}
	}

	/**
	 * Menjumlahkan seluruh persentase diskon yang berlaku (lewat {@link #getDiskonSekarang}) untuk
	 * kombinasi item/tindakan/alat medis, jumlah, tanggal, asuransi, dan komunitas yang diberikan.
	 *
	 * @return total persentase diskon gabungan (mis. {@code 15.0} berarti 15%)
	 */
	public static Double getTotalDiskonDalamPersen(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis,
			Integer jumlah, Date tanggal, Asuransi asuransi, Set<Komunitas> komunitas) {
		Double total = 0.0;
		List<Diskon> diskons = getDiskonSekarang(item, tindakan, alatMedis, jumlah, tanggal, asuransi, komunitas);
		for (Diskon diskon : diskons) {
			total += diskon.getJumlah();
		}
		diskons = null;
		return total;
	}

	/**
	 * Menjumlahkan seluruh persentase pajak yang berlaku saat ini (lewat {@link #getPajakSekarang})
	 * untuk kombinasi item/tindakan/alat medis, asuransi, dan komunitas yang diberikan.
	 *
	 * @return total persentase pajak gabungan
	 */
	public static Double getTotalPajakDalamPersen(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis,
			Asuransi asuransi, Set<Komunitas> komunitas) {
		Double total = 0.0;
		List<PajakMedis> pajaks = getPajakSekarang(item, tindakan, alatMedis, asuransi, komunitas);
		for (PajakMedis pajak : pajaks) {
			total += pajak.getJumlah();
		}
		pajaks = null;
		return total;
	}

	/**
	 * Mencari aturan {@link Diskon} yang sedang berlaku (aktif, rentang tanggal {@code mulai}/
	 * {@code sampai} mencakup {@code tanggal}, rentang {@code jumlahMinimal}/{@code jumlahMaksimal}
	 * mencakup {@code jumlah}) untuk kombinasi asuransi dan komunitas yang diberikan, difilter lagi
	 * berdasarkan item/tindakan/alat medis spesifik bila parameter terkait tidak {@code null}.
	 * Query dikelompokkan per {@code diskon} ({@code groupProperty}) agar satu aturan diskon dengan
	 * banyak baris {@link DiskonDetail} tidak muncul berulang dalam hasil.
	 *
	 * @return daftar {@link Diskon} yang berlaku untuk kombinasi kriteria yang diberikan
	 */
	@SuppressWarnings("unchecked")
	public static List<Diskon> getDiskonSekarang(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis, Integer jumlah,
			Date tanggal, Asuransi asuransi, Set<Komunitas> komunitas) {
		List<Diskon> diskons = HibernateUtil.currentSession().createCriteria(DiskonDetail.class)
				.createAlias("diskon", "diskon").add(Restrictions.eq("diskon.aktif", true))
				.add(asuransi == null ? Restrictions.isNull("diskon.asuransi")
						: Restrictions.eq("diskon.asuransi", asuransi))

				.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("diskon.komunitas")
						: Restrictions.in("diskon.komunitas", komunitas))

				.add(Restrictions.le("diskon.jumlahMinimal", jumlah))
				.add(Restrictions.ge("diskon.jumlahMaksimal", jumlah))

				.add(Restrictions.le("diskon.mulai", tanggal))
				.add(Restrictions.or(Restrictions.isNull("diskon.sampai"), Restrictions.ge("diskon.sampai", tanggal)))
				.add(item == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("item", item))
				.add(tindakan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tindakan", tindakan))
				.add(alatMedis == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("alatMedis", alatMedis))
				.setProjection(Projections.groupProperty("diskon")).list();
		return diskons;
	}

	/**
	 * Mencari aturan {@link PajakMedis} yang sedang berlaku pada tanggal saat ini (aktif, rentang
	 * {@code mulai}/{@code sampai} mencakup {@link Date#Date() sekarang}) untuk kombinasi asuransi dan
	 * komunitas yang diberikan, difilter lagi berdasarkan item/tindakan/alat medis spesifik bila
	 * parameter terkait tidak {@code null}. Query dikelompokkan per {@code pajak} agar satu aturan
	 * pajak dengan banyak baris {@link PajakDetail} tidak muncul berulang.
	 *
	 * @return daftar {@link PajakMedis} yang berlaku untuk kombinasi kriteria yang diberikan
	 */
	@SuppressWarnings("unchecked")
	public static List<PajakMedis> getPajakSekarang(ItemMedis item, Tindakan tindakan, AlatMedis alatMedis,
			Asuransi asuransi, Set<Komunitas> komunitas) {
		List<PajakMedis> pajaks = HibernateUtil.currentSession().createCriteria(PajakDetail.class)
				.createAlias("pajak", "pajak")
				.add(asuransi == null ? Restrictions.isNull("pajak.asuransi")
						: Restrictions.eq("pajak.asuransi", asuransi))

				.add(komunitas == null || komunitas.isEmpty() ? Restrictions.isNull("pajak.komunitas")
						: Restrictions.in("pajak.komunitas", komunitas))

				.add(Restrictions.eq("pajak.aktif", true)).add(Restrictions.le("pajak.mulai", new Date()))
				.add(Restrictions.or(Restrictions.isNull("pajak.sampai"), Restrictions.ge("pajak.sampai", new Date())))
				.add(item == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("item", item))
				.add(tindakan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tindakan", tindakan))
				.add(alatMedis == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("alatMedis", alatMedis))
				.setProjection(Projections.groupProperty("pajak")).list();
		return pajaks;
	}

	/**
	 * Memastikan data induk untuk jasa peracikan obat selalu tersedia. Mencari (atau membuat bila
	 * belum ada) {@link JenisTindakan} bernama {@code JASA_RACIK}, lalu memastikan tiga
	 * {@link Tindakan} standar di bawahnya ("Bubuk", "Sirup", "Krim") ada — bila daftar tindakan
	 * untuk jenis tersebut masih kosong, ketiganya dibuat otomatis. Method ini idempoten: pemanggilan
	 * berulang tidak akan menduplikasi data yang sudah ada.
	 *
	 * @return daftar {@link Tindakan} jasa racik (baik yang sudah ada maupun yang baru dibuat)
	 */
	@SuppressWarnings("unchecked")
	public static List<Tindakan> populateJasaRacik() {

		Session session = HibernateUtil.currentSession();
		JenisTindakan jenisTindakan = (JenisTindakan) session.createCriteria(JenisTindakan.class)
				.add(Restrictions.eq("nama", JenisTindakan.JASA_RACIK)).setMaxResults(1).uniqueResult();
		if (jenisTindakan == null) {
			jenisTindakan = new JenisTindakan();
			jenisTindakan.setNama(JenisTindakan.JASA_RACIK);
			session.save(jenisTindakan);
		}

		List<Tindakan> tindakansJasaRaciks = session.createCriteria(Tindakan.class)
				.add(Restrictions.eq("jenisTindakan", jenisTindakan)).list();

		if (tindakansJasaRaciks.isEmpty()) {
			Tindakan tindakan = new Tindakan();
			tindakan.setNama("Bubuk");
			tindakan.setJenisTindakan(jenisTindakan);
			session.save(tindakan);
			tindakansJasaRaciks.add(tindakan);

			tindakan = new Tindakan();
			tindakan.setNama("Sirup");
			tindakan.setJenisTindakan(jenisTindakan);
			session.save(tindakan);
			tindakansJasaRaciks.add(tindakan);

			tindakan = new Tindakan();
			tindakan.setNama("Krim");
			tindakan.setJenisTindakan(jenisTindakan);
			session.save(tindakan);
			tindakansJasaRaciks.add(tindakan);
		}

		return tindakansJasaRaciks;
	}

	/**
	 * Menghitung total harga jual satu {@link Racikan} dengan menjumlahkan harga jual tiap item
	 * komponennya (lewat {@link CommonTarifItem#getHargaJualItem}, yang mempertimbangkan kelas
	 * perawatan, dokter, asuransi, komunitas, dan pasien) dikalikan takaran masing-masing item pada
	 * {@link RacikanDetail}. Item pada detail racikan yang bernilai {@code null} dilewati.
	 *
	 * @return total harga jual racikan (0.0 bila tidak ada item atau harga jual tidak ditemukan)
	 * @throws Exception diteruskan dari {@link CommonTarifItem#getHargaJualItem}
	 */
	@SuppressWarnings("unchecked")
	public static Double hitungHargaJualRacikan(Racikan racikan, KelasPerawatan kelasPerawatan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).list();
		Double harJual = 0.0;

		for (RacikanDetail racikanDetail : racikanDetails) {
			if (racikanDetail.getItem() == null) {
				continue;
			}

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					dokter, asuransi, komunitas, pasien);

			harJual += (hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
					: hargaJualItem.getHargaJual())
					* (racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah());
		}
		return harJual;
	}

	/**
	 * Menghitung total nilai diskon (dalam nominal, bukan persen) untuk satu {@link Racikan}: untuk
	 * tiap item komponen, persentase diskon yang berlaku ({@link #getTotalDiskonDalamPersen}) dikali
	 * harga jual item tersebut ({@link CommonTarifItem#getHargaJualItem}) dan takarannya, lalu
	 * dijumlahkan ke seluruh komponen racikan.
	 *
	 * @return total nominal diskon racikan
	 * @throws Exception diteruskan dari {@link CommonTarifItem#getHargaJualItem}
	 */
	@SuppressWarnings("unchecked")
	public static Double hitungDiskonRacikan(Racikan racikan, KelasPerawatan kelasPerawatan, Date tanggal,
			Dokter dokter, Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).list();
		Double totalDiskon = 0.0;

		for (RacikanDetail racikanDetail : racikanDetails) {
			if (racikanDetail.getItem() == null) {
				continue;
			}

			final Double diskon = CommonSirs.getTotalDiskonDalamPersen(racikanDetail.getItem(), null, null,
					racikanDetail.getJumlah().intValue(), tanggal, asuransi, komunitas);

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					dokter, asuransi, komunitas, pasien);
			totalDiskon += ((hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
					: hargaJualItem.getHargaJual()) * (diskon / 100.0))
					* (racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah());
		}
		return totalDiskon;
	}

	/**
	 * Menghitung total nilai pajak (dalam nominal) untuk satu {@link Racikan}, dengan pola perhitungan
	 * yang sama seperti {@link #hitungDiskonRacikan} tetapi memakai
	 * {@link #getTotalPajakDalamPersen} sebagai sumber persentase.
	 *
	 * @return total nominal pajak racikan
	 * @throws Exception diteruskan dari {@link CommonTarifItem#getHargaJualItem}
	 */
	@SuppressWarnings("unchecked")
	public static Double hitungPajakRacikan(Racikan racikan, KelasPerawatan kelasPerawatan, Dokter dokter,
			Asuransi asuransi, Set<Komunitas> komunitas, Pasien pasien) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).list();
		Double totalPajak = 0.0;

		for (RacikanDetail racikanDetail : racikanDetails) {
			if (racikanDetail.getItem() == null) {
				continue;
			}

			final Double pajak = CommonSirs.getTotalPajakDalamPersen(racikanDetail.getItem(), null, null, asuransi,
					komunitas);

			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(racikanDetail.getItem(), kelasPerawatan,
					dokter, asuransi, komunitas, pasien);

			totalPajak += ((hargaJualItem == null || hargaJualItem.getHargaJual() == null ? 0.0
					: hargaJualItem.getHargaJual()) * (pajak / 100.0))
					* (racikanDetail.getJumlah() == null ? 0.0 : racikanDetail.getJumlah());
		}
		return totalPajak;
	}

	/**
	 * Menghitung Harga Pokok Penjualan (HPP) rata-rata satu {@link ItemMedis} lewat SQL native:
	 * {@code sum(amount)/sum(qty)} atas baris {@code sirs.detail_transaksi_pasien} dengan kode
	 * transaksi "saldo awal" atau "beli masuk". Bila {@code session} diberikan {@code null}, sesi
	 * Hibernate aktif dipakai.
	 *
	 * @param item    item medis yang HPP-nya dihitung; bila {@code null}/tanpa id, hasil kueri kosong
	 * @param session sesi Hibernate yang dipakai; {@code null} berarti pakai
	 *                {@link HibernateUtil#currentSession()}
	 * @return HPP rata-rata, atau {@code 0.0} bila tidak ada data transaksi terkait
	 */
	public static Double hitungHPP(ItemMedis item, Session session) {
		String sql = "select sum(a.amount)/sum(a.qty) from sirs.detail_transaksi_pasien a where a.item = "
				+ (item == null || item.getId() == null ? -1 : item.getId()) + " and a.kode_transaksi in (" + ConstantValues.saldoAwal.getId()
				+ "," + ConstantValues.beliMasuk.getId() + ")";
		session = (session == null ? HibernateUtil.currentSession() : session);
		Number hpp = (Number) session.createSQLQuery(sql).uniqueResult();
		return hpp == null ? 0.0 : hpp.doubleValue();
	}

	/**
	 * Menghitung harga beli rata-rata satu {@link ItemMedis} lewat SQL native ({@code sum(amount)/
	 * sum(qty)}) atas baris {@code sirs.detail_transaksi_pasien} dengan kode transaksi "beli masuk"
	 * saja (berbeda dari {@link #hitungHPP} yang juga menyertakan "saldo awal").
	 *
	 * @param item    item medis yang harga belinya dihitung
	 * @param session sesi Hibernate yang dipakai; {@code null} berarti pakai
	 *                {@link HibernateUtil#currentSession()}
	 * @return harga beli rata-rata, atau {@code 0.0} bila tidak ada data pembelian terkait
	 */
	public static Double hitungHargaBeli(ItemMedis item, Session session) {
		String sql = "select sum(a.amount)/sum(a.qty) from sirs.detail_transaksi_pasien a where a.item = "
				+ (item == null || item.getId() == null ? -1 : item.getId()) + " and a.kode_transaksi in (" + ConstantValues.beliMasuk.getId()
				+ ")";
		session = (session == null ? HibernateUtil.currentSession() : session);
		Number hb = (Number) session.createSQLQuery(sql).uniqueResult();
		return hb == null ? 0.0 : hb.doubleValue();
	}

	/** Varian ringkas {@link #simpanTransaksiTindakan(Pasien, Tindakan, KelasPerawatan, Lokasi, Double, Pendaftaran, CetakKartuPasien)} tanpa keterkaitan ke pendaftaran atau cetak kartu pasien tertentu. */
	public static DetailTransaksiLayanan simpanTransaksiTindakan(Pasien pasien, Tindakan tindakan,
			KelasPerawatan kelasPerawatan, Lokasi lokasi, Double qty) throws Exception {
		return simpanTransaksiTindakan(pasien, tindakan, kelasPerawatan, lokasi, qty, null, null);
	}

	/**
	 * Mencatat satu baris {@link DetailTransaksiLayanan} untuk tindakan yang dikenakan kepada
	 * {@code pasien}. Bila {@code pendaftaran}/{@code cetakKartuPasien} diberikan, baris transaksi
	 * layanan lama yang <b>belum lunas</b> ({@code lunas=false}) untuk pendaftaran/cetak kartu yang
	 * sama dihapus lebih dulu lewat SQL native — mencegah baris dobel bila aksi (mis. cetak kartu)
	 * diulang sebelum dibayar. Bila {@code pasien} kosong, ditampilkan peringatan dan method
	 * mengembalikan {@code null}; bila {@code tindakan} kosong, method diam-diam mengembalikan
	 * {@code null} (pesan peringatan untuk kasus ini dinonaktifkan lewat komentar). Detail biaya
	 * (harga, diskon, pajak) ditentukan oleh
	 * {@link CommonPendaftaranUtil#setDetailBiaya(DetailTransaksiLayanan, KelasPerawatan)}.
	 *
	 * @param pasien           pasien yang dikenakan tindakan; wajib diisi
	 * @param tindakan         tindakan/layanan yang dikenakan; wajib diisi
	 * @param kelasPerawatan   kelas perawatan yang menentukan tarif berlaku
	 * @param lokasi           lokasi/cabang tempat transaksi dicatat
	 * @param qty              kuantitas tindakan; {@code null} diperlakukan sebagai {@code 0.0}
	 * @param pendaftaran      pendaftaran terkait (untuk pembersihan baris belum lunas), boleh
	 *                         {@code null}
	 * @param cetakKartuPasien record cetak kartu terkait (untuk pembersihan baris belum lunas), boleh
	 *                         {@code null}
	 * @return {@link DetailTransaksiLayanan} yang tersimpan lengkap dengan detail biaya, atau
	 *         {@code null} bila {@code pasien}/{@code tindakan} tidak diisi
	 * @throws Exception diteruskan dari operasi Hibernate/perhitungan biaya
	 */
	public static DetailTransaksiLayanan simpanTransaksiTindakan(Pasien pasien, Tindakan tindakan,
			KelasPerawatan kelasPerawatan, Lokasi lokasi, Double qty, Pendaftaran pendaftaran,
			CetakKartuPasien cetakKartuPasien) throws Exception {
		if (pasien == null) {
			MyMessageboxConfig.show("Pasien harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return null;
		}
		if (tindakan == null) {
//			Messagebox.show("Data layanan harus diisi", "Peringatan", Messagebox.OK, Messagebox.EXCLAMATION);
			return null;
		}

		Session session = HibernateUtil.currentSession();

		if (pendaftaran != null && pendaftaran.getId() != null) {
			session.createSQLQuery("delete from sirs.detail_transaksi_layanan where pendaftaran = "
					+ pendaftaran.getId() + " and lunas = false;").executeUpdate();
		}

		if (cetakKartuPasien != null && cetakKartuPasien.getId() != null) {
			session.createSQLQuery("delete from sirs.detail_transaksi_layanan where cetak_kartu_pasien = "
					+ cetakKartuPasien.getId() + " and lunas = false;").executeUpdate();
		}

		DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();
		detailTransaksiLayanan.setDiskon(0.0);
		detailTransaksiLayanan.setKeterangan(tindakan.getNama());
		detailTransaksiLayanan.setLokasi(lokasi);
		detailTransaksiLayanan.setPajak(0.0);
		detailTransaksiLayanan.setPasien(pasien);
		detailTransaksiLayanan.setQty(qty == null ? 0.0 : qty);
		detailTransaksiLayanan.setQtyBonus(0.0);
		detailTransaksiLayanan.setTanggal(new Date());
		detailTransaksiLayanan.setTindakan(tindakan);
		detailTransaksiLayanan.setPendaftaran(pendaftaran);
		detailTransaksiLayanan.setCetakKartuPasien(cetakKartuPasien);

		return CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan, kelasPerawatan);
	}

	/**
	 * Field statis publik yang dideklarasikan tetapi tidak diinisialisasi maupun dipakai di dalam
	 * kelas ini sendiri (kemungkinan dipakai/diisi dari kode pemanggil lain sebagai cache sementara
	 * daftar minggu). Bandingkan dengan variabel lokal {@code SELEDTED_MINGGUS} di
	 * {@link #getMinggu(Integer, Integer)} yang namanya mirip tetapi merupakan variabel terpisah.
	 */
	public static List<Minggu> CURRENT_MINGGUS;

	/**
	 * Memuat (atau membangun bila belum ada) daftar {@link Minggu} kalender untuk kombinasi
	 * {@code bulan}/{@code tahun} tertentu — dipakai laporan-laporan periodik SIRS berbasis minggu.
	 * Bila {@code bulan}/{@code tahun} tidak diberikan, dipakai bulan/tahun saat ini. Bila belum ada
	 * baris {@link Minggu} tersimpan untuk kombinasi tersebut, daftar dibangun lewat
	 * {@link CommonReport#getMinggu(Integer, Integer)} dan setiap barisnya disimpan dalam transaksi
	 * terpisah, ditandai dengan bulan/tahun/pengguna pembuat.
	 *
	 * <p>
	 * Method ini membuka sesi Hibernate dedikasi ({@code openSession()}, bukan sesi thread-bound)
	 * dan menjaminnya ditutup di blok {@code finally} (idempoten lewat pengecekan {@code isOpen()})
	 * agar tidak terjadi kebocoran koneksi walau terjadi exception di tengah proses.
	 * </p>
	 *
	 * @param bulan bulan (1-12) yang dicari; {@code null} berarti bulan saat ini
	 * @param tahun tahun yang dicari; {@code null} berarti tahun saat ini
	 * @return daftar {@link Minggu} untuk bulan/tahun tersebut
	 */
	@SuppressWarnings("unchecked")
	public static List<Minggu> getMinggu(Integer bulan, Integer tahun) {
		bulan = bulan == null ? (Calendar.getInstance().get(Calendar.MONTH) + 1) : bulan;
		tahun = tahun == null ? Calendar.getInstance().get(Calendar.YEAR) : tahun;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		List<Minggu> SELEDTED_MINGGUS = session.createCriteria(Minggu.class).add(Restrictions.eq("bulan", bulan))
				.add(Restrictions.eq("tahun", tahun)).list();

		if (SELEDTED_MINGGUS.size() == 0) {
			SELEDTED_MINGGUS = CommonReport.getMinggu(bulan, tahun);
			for (Minggu minggu : SELEDTED_MINGGUS) {
				minggu.setBulan(bulan);
				minggu.setTahun(tahun);
				minggu.setTbmuser(Common.getCurrentUser());
				session.getTransaction().begin();
				session.save(minggu);
				session.getTransaction().commit();
			}
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		return SELEDTED_MINGGUS;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1095");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1096");}
			}
		}
	}

	/**
	 * Mencari nomor urut tertinggi yang sudah dipakai pada kode rekam medis {@link Pasien} untuk
	 * {@code jenisPasien} tertentu, dipakai sebagai basis pembuatan kode pasien berikutnya oleh
	 * {@link #generateCodePasien}. Kode pasien memakai awalan huruf sebagai penanda kategori: {@code
	 * D} untuk pasien dinas TNI/PNS, kombinasi {@code D}+{@code S} untuk pasien dinas yang juga siswa,
	 * dan kode tanpa awalan {@code S}/{@code L}/{@code D} di posisi tertentu untuk pasien umum. Nomor
	 * urut diekstrak dari kode existing lewat fungsi SQL {@code to_number(replace(replace(replace(
	 * kode,'D',''),'L',''),'S',''))} dengan {@code max(...)} untuk mendapatkan nilai tertinggi; kode
	 * yang tidak mengandung digit murni setelah huruf penanda dihilangkan diperlakukan sebagai
	 * {@code 0}. Kode dengan awalan {@code SS}, {@code DD}, atau {@code L} tunggal (bukan bagian dari
	 * kombinasi valid) dikecualikan dari pencarian.
	 *
	 * @param jenisPasien kategori pasien (umum/dinas/siswa) yang menentukan pola filter kode;
	 *                    {@code null} berarti tanpa filter kategori tambahan
	 * @return nomor urut tertinggi yang ditemukan, atau {@code 0L} bila tidak ada kode yang cocok
	 */
	public static Long generateMaxByJenisPasien(JenisPasien jenisPasien) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		Long strmax = (Long) session.createCriteria(Pasien.class)
				// .createAlias("propinsi", "propinsi", Criteria.LEFT_JOIN)
				// .createAlias("kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				// .createAlias("kota", "kota", Criteria.LEFT_JOIN)
				// .createAlias("kelurahan", "kelurahan", Criteria.LEFT_JOIN)
				// .createAlias("jenisPasien", "jenisPasien",
				// Criteria.LEFT_JOIN)
				// .createAlias("agama", "agama", Criteria.LEFT_JOIN)
				// .createAlias("pendidikan", "pendidikan", Criteria.LEFT_JOIN)
				// .createAlias("prioritasPasien", "prioritasPasien",
				// Criteria.LEFT_JOIN)

				.add(Restrictions.not(Restrictions.ilike("kode", "SS", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "DD", MatchMode.ANYWHERE)))
				.add(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.ANYWHERE)))

				.add(Restrictions.gt("id", 0L)).add(Restrictions.gt("index", 0L)).add(Restrictions.isNotNull("index"))

				.add(jenisPasien == null || !jenisPasien.getId().equals(ConstantValues.PASIEN_DINAS.getId())
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.and(Restrictions.ilike("kode", "D", MatchMode.ANYWHERE),
								Restrictions.eq("jenisPasien", ConstantValues.PASIEN_DINAS)))

				.add(jenisPasien == null || !jenisPasien.getId().equals(ConstantValues.PASIEN_SISWA.getId())
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.and(Restrictions.ilike("kode", "D", MatchMode.ANYWHERE),
								Restrictions.and(Restrictions.ilike("kode", "S", MatchMode.ANYWHERE),
										Restrictions.eq("jenisPasien", ConstantValues.PASIEN_SISWA))))

				// .add(jenisPasien == null
				// || !jenisPasien.getId().equals(
				// ConstantValues.PASIEN_UMUM.getId()) ? Restrictions
				// .sqlRestriction("1=1") : Restrictions.or(Restrictions
				// .eq("jenisPasien", ConstantValues.PASIEN_ASURANSI),
				// Restrictions.eq("jenisPasien",
				// ConstantValues.PASIEN_UMUM)))

				.add(jenisPasien == null || !jenisPasien.getId().equals(ConstantValues.PASIEN_UMUM.getId())
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.and(Restrictions.not(Restrictions.ilike("kode", "S", MatchMode.END)),
								Restrictions.and(Restrictions.not(Restrictions.ilike("kode", "L", MatchMode.END)),
										Restrictions.and(
												Restrictions.not(Restrictions.ilike("kode", "D", MatchMode.START)),
												Restrictions.and(
														Restrictions.ne("jenisPasien", ConstantValues.PASIEN_SISWA),
														Restrictions.ne("jenisPasien", ConstantValues.PASIEN_DINAS))))))
				.setProjection(Projections.sqlProjection(
						"max(to_number(case when trim(replace(replace(replace(kode,'D',''), 'L', ''),'S','')) = '' then '0' else trim(replace(replace(replace(kode,'D',''), 'L', ''),'S','')) end,'99999999999999')) as maksimal",
						new String[] { "maksimal" }, new Type[] { new LongType() }))
				.uniqueResult();

		// Projections.sql

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}

		if (strmax == null) {
			strmax = 0L;
		}

		// Long max = Long.parseLong(strmax.replaceAll("D", "")
		// .replaceAll("S", "").replaceAll("L", ""));

		return strmax;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1175");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1176");}
			}
		}
	}

	/** Varian ringkas {@link #generateCodePasien(int, String, String, JenisPasien, Long)} yang memulai pencarian nomor urut dari {@code penambahan=1L} (nomor urut berikutnya setelah nomor tertinggi saat ini). */
	public static String generateCodePasien(int panjang, String awalan, String akhiran, JenisPasien jenisPasien) {
		return generateCodePasien(panjang, awalan, akhiran, jenisPasien, 1L);
	}

	/**
	 * Membangun kode rekam medis pasien baru yang dijamin unik, dengan format
	 * {@code awalan + angka(panjang digit, zero-padded) + akhiran}. Nomor urut dasar diambil dari
	 * nomor tertinggi existing untuk {@code jenisPasien} tersebut (lewat
	 * {@link #generateMaxByJenisPasien(JenisPasien)} bila {@code jenisPasien} diisi, atau dari id
	 * {@link Pasien} tertinggi bila tidak) ditambah {@code penambahan}. Setelah kode kandidat
	 * dibangun, method memeriksa apakah kode tersebut sudah dipakai; bila sudah, method memanggil
	 * dirinya sendiri secara rekursif dengan {@code penambahan} dinaikkan satu sampai ditemukan kode
	 * yang belum dipakai.
	 *
	 * <p>
	 * Method ini membuka sesi Hibernate dedikasi untuk pengecekan keunikan dan menjaminnya ditutup di
	 * blok {@code finally}. <b>Catatan konkurensi</b> — pengecekan keunikan dan penyimpanan kode
	 * akhir tidak dilakukan dalam satu transaksi atomik di method ini sendiri; bila dua proses
	 * memanggil method ini bersamaan untuk {@code jenisPasien} yang sama, keduanya berpotensi
	 * menerima kode kandidat yang sama sebelum salah satunya benar-benar menyimpan data pasien.
	 * </p>
	 *
	 * @param panjang     jumlah digit angka pada kode (di-zero-pad dari kiri)
	 * @param awalan      prefiks kode, boleh kosong/{@code null}
	 * @param akhiran     sufiks kode, boleh kosong
	 * @param jenisPasien kategori pasien yang menentukan basis pencarian nomor urut tertinggi
	 * @param penambahan  jumlah yang ditambahkan ke nomor urut tertinggi untuk mendapatkan kandidat
	 *                    nomor urut baru; dinaikkan otomatis pada tiap percobaan rekursif saat
	 *                    terjadi tabrakan kode
	 * @return kode pasien baru yang belum pernah dipakai
	 */
	public static String generateCodePasien(int panjang, String awalan, String akhiran, JenisPasien jenisPasien,
			Long penambahan) {
		Long max = null;
		if (jenisPasien == null) {
			max = (Long) HibernateUtil.currentSession().createCriteria(Pasien.class)
					.setProjection(Projections.max("id")).uniqueResult();
		} else {
			max = generateMaxByJenisPasien(jenisPasien);
		}
		if (max == null) {
			max = 0L;
		}
		String mykode = "00000000000000000000000000000" + (penambahan + max);
		mykode = (awalan == null || awalan.trim().equals("") ? "" : awalan)
				+ mykode.substring(mykode.length() - panjang, mykode.length());
		String hasil = mykode + akhiran;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
		Integer count = ((Number) session.createCriteria(Pasien.class).add(Restrictions.eq("kode", hasil))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}

		System.out.println("hasil = " + hasil + ", count = " + count);

		if (count.equals(0)) {
			return hasil;
		} else {
			return generateCodePasien(panjang, awalan, akhiran, jenisPasien, ++penambahan);
		}

	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1224");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/common/CommonSirs.java:1225");}
			}
		}
	}
}
