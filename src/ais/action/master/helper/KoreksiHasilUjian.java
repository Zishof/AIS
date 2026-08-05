package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.BankSoalAction;
import ais.action.master.SertifikatAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>KoreksiHasilUjian — Panel Koreksi dan Peninjauan Jawaban Ujian per Peserta</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini menyediakan komponen UI interaktif (ZK) yang menampilkan seluruh soal ujian
 * beserta jawaban yang diberikan peserta tertentu, dan memungkinkan dosen/admin melakukan koreksi manual
 * terhadap jawaban esai (input teks koreksi + lampiran) serta menyesuaikan nilai per-soal secara langsung.
 * Komponen ini digunakan sebagai "detail panel" yang muncul saat baris peserta di-expand pada grid rekap hasil
 * ujian ({@link HasilUjianMahasiswaHelper}).
 *
 * <p><b>Fitur utama:</b>
 * <ul>
 *   <li><b>Filter tampil soal:</b> Toolbar berisi enam checkbox: Belum dijawab, Telah dijawab, Jawaban benar,
 *       Jawaban salah, Telah dinilai, Belum dinilai — dapat dikombinasikan untuk menyaring soal yang
 *       ditampilkan di grid koreksi.</li>
 *   <li><b>Koreksi esai:</b> Untuk soal bertipe ESAY/JAWABAN_SINGKAT, dosen dapat mengetikkan catatan koreksi
 *       (Textbox multiline) dan mengunggah lampiran; peserta hanya melihat tampilan read-only.</li>
 *   <li><b>Input nilai esai:</b> Untuk soal esai, dosen dapat mengisi nilai perolehan (MyDoublebox) langsung
 *       di layar, dengan validasi agar tidak melebihi skor maksimal soal.</li>
 *   <li><b>Ubah jawaban PG (admin):</b> Bila konfigurasi {@code adminBolehMerubahJawabanUjian=AKTIF} dan
 *       pengguna adalah admin, muncul combobox untuk mengubah pilihan jawaban yang sudah diberikan peserta.</li>
 *   <li><b>Hitung Ulang:</b> Tombol yang memanggil {@link ProsesUjianHelper#hitungPilihanGanda} untuk
 *       menghitung ulang total nilai dari detail yang ada (berguna setelah perubahan jawaban/soal).</li>
 *   <li><b>Ulang Ujian:</b> Tombol (hanya untuk staf/admin) yang me-reset semua jawaban dan status ujian
 *       peserta sehingga bisa mengulang ujian (bila waktu masih ada).</li>
 *   <li><b>Cetak Sertifikat:</b> Bila peserta lulus dan ujian punya template sertifikat.</li>
 *   <li><b>History (Revisi):</b> Tombol untuk melihat riwayat perubahan data jawaban via
 *       {@link RevisiHasilUjianMahasiswaHelper}.</li>
 *   <li><b>Ekspor Excel:</b> Toolbar menyertakan tombol cetak/ekspor via {@link Common#cetakData}.</li>
 * </ul>
 *
 * <p><b>Cara kerja (arsitektur):</b> Kelas mengimplementasikan {@link DataCriteria} sehingga dapat digunakan
 * sebagai sumber data untuk {@link MyGrid} dengan paging. Method {@link #initCriteria(boolean)} membangun
 * {@link org.hibernate.Criteria} untuk {@link HasilUjianMahasiswaDetail} berdasarkan filter checkbox aktif.
 * Method {@link #loadData(Object)} mem-populasi list {@code bankSoals} (soal yang lolos filter) lalu mengatur
 * {@link org.zkoss.zul.ListModel} grid; renderer baris membuat sub-grid per soal dengan tampilan berbeda
 * antara soal Pilihan Ganda dan Esai/Jawaban Singkat.
 *
 * <p><b>Manajemen sesi Hibernate:</b> Method {@link #initCriteria(boolean)} menggunakan
 * {@code HibernateUtil.currentSession()} (sesi thread ZK yang sudah ada). Semua operasi write (update
 * jawaban/koreksi/nilai/ubah pilihan) di dalam event listener ZK membuka sesi mandiri via
 * {@code HibernateUtil.getSessionFactory().openSession()} dengan pola try-catch-finally eksplisit dan
 * rollback bila terjadi exception.
 *
 * <p><b>Hak akses:</b> Dua peran dibedakan berdasarkan field {@code mahasiswa} dan {@code biodataCalonMahasiswa}:
 * <ul>
 *   <li>Peserta (mahasiswa/calon/siswa): tampilan read-only untuk jawaban, koreksi, dan nilai.</li>
 *   <li>Staf/dosen/admin: dapat mengedit koreksi, nilai esai, dan (jika dikonfigurasi) jawaban PG.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Semua operasi UI berjalan di thread ZK. Event listener bersifat inner class anonymous
 * yang menangkap variable melalui closure (final). Operasi write DB di event listener membuka sesi Hibernate
 * baru secara sinkron dalam thread event ZK.
 *
 * <p><b>Pemeliharaan:</b> Bila menambah tipe soal baru (selain PILIHAN_GANDA dan ESAY/JAWABAN_SINGKAT),
 * tambahkan cabang di dalam {@link #loadData} pada blok renderer baris. Perubahan field filter baru (checkbox)
 * memerlukan: (1) deklarasi field, (2) inisialisasi di {@link #display}, (3) logika filter di
 * {@link #loadData}, (4) listener reload loadData.
 */
public class KoreksiHasilUjian implements DataCriteria {

	private HasilUjianMahasiswa tempHasilUjianMahasiswa;
	private MyGrid grid;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private MyCheckboxConfig belumDijawab;
	private MyCheckboxConfig telahDijawab;
	private MyCheckboxConfig benarDijawab;
	private MyCheckboxConfig salahDijawab;
	private MyCheckboxConfig nilaiDijawab;
	private MyCheckboxConfig belumNilaiDijawab;

	/**
	 * <b>Tujuan:</b> Membangun objek {@link org.hibernate.Criteria} Hibernate untuk mengambil data
	 * {@link HasilUjianMahasiswaDetail} yang sesuai dengan peserta ujian yang sedang diperiksa dan daftar soal
	 * yang lolos filter checkbox. Method ini dipanggil oleh {@link MyGrid} secara otomatis saat data grid perlu
	 * di-reload (paging, refresh) sebagai implementasi dari kontrak {@link DataCriteria}.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Ambil sesi Hibernate aktif via {@code HibernateUtil.currentSession()} (sesi thread ZK).</li>
	 *   <li>Buat {@link org.hibernate.Criteria} untuk kelas {@link HasilUjianMahasiswaDetail} dengan dua
	 *       filter:
	 *       <ul>
	 *         <li>{@code eq("hasilUjianMahasiswa", tempHasilUjianMahasiswa)} — membatasi ke peserta tertentu.</li>
	 *         <li>{@code in("bankSoal", bankSoals)} — membatasi ke soal-soal yang lolos filter checkbox;
	 *             bila {@code bankSoals} kosong, digunakan {@code sqlRestriction("false")} agar tidak ada
	 *             baris yang dikembalikan (menghindari error Hibernate pada {@code Restrictions.in()} dengan
	 *             collection kosong).</li>
	 *       </ul></li>
	 *   <li>Bila parameter {@code order=true}, tambahkan urutan ascending berdasarkan {@code id} agar soal
	 *       tampil sesuai urutan tersimpan.</li>
	 * </ol>
	 *
	 * <p><b>Ketergantungan state:</b> Method ini bergantung pada dua field instance:
	 * {@code tempHasilUjianMahasiswa} (harus tidak null) dan {@code bankSoals} (harus sudah ter-populate
	 * oleh {@link #loadData(Object)} sebelum method ini dipanggil). Urutan pemanggilan yang benar:
	 * {@code loadData()} terlebih dahulu, baru grid memanggil {@code initCriteria()}.
	 *
	 * <p><b>Sifat/thread-safety:</b> Harus dipanggil dari thread ZK karena menggunakan
	 * {@code currentSession()} yang bersifat ThreadLocal.
	 *
	 * @param order {@code true} untuk menambahkan urutan ascending berdasarkan {@code id} baris detail
	 * @return {@link org.hibernate.Criteria} yang sudah dikonfigurasi siap dieksekusi oleh {@link MyGrid}
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(HasilUjianMahasiswaDetail.class)
				.add(Restrictions.eq("hasilUjianMahasiswa", tempHasilUjianMahasiswa))
				.add(bankSoals.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("bankSoal", bankSoals));

		if (order) {
			criteria.addOrder(Order.asc("id"));
		}

		return criteria;
	}

	private List<BankSoal> bankSoals = new ArrayList<BankSoal>();
	private boolean adminBolehMerubahJawabanUjian = false;

	/**
	 * <b>Tujuan:</b> Memuat ulang data soal yang akan ditampilkan di grid koreksi, menerapkan semua filter
	 * checkbox yang aktif, dan merender setiap soal beserta jawaban peserta ke dalam baris grid menggunakan
	 * renderer kustom. Method ini adalah "otak" rendering panel koreksi: dipanggil saat panel pertama kali
	 * dibuka dan setiap kali pengguna mengubah pilihan filter atau mengklik Refresh.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Ambil pengguna aktif ({@code tbmuser}) via {@link Common#getCurrentUser()} — digunakan untuk
	 *       menentukan hak akses di dalam renderer (final variable dalam closure anonymous class).</li>
	 *   <li>Muat daftar ID soal yang sudah diacak untuk peserta ini via
	 *       {@link HasilUjianMahasiswa#ambilUjianPunyaSoals} (parameter {@code refresh=true} bila
	 *       {@code value instanceof Boolean && (Boolean)value == true}).</li>
	 *   <li>Muat map jawaban peserta ({@link HasilUjianMahasiswaDetail} per soal) via
	 *       {@link HasilUjianMahasiswa#ambilHasilUjianMahasiswaDetail}.</li>
	 *   <li>Hitung tiga set ID soal dari jawaban: {@code terjawab} (ada jawaban), {@code benar} (jawaban benar),
	 *       {@code dinilai} (esai sudah diberi nilai oleh dosen).</li>
	 *   <li><b>Filter soal:</b> Iterasi setiap soal; lewati soal yang tidak lolos filter checkbox aktif:
	 *       <ul>
	 *         <li>{@code telahDijawab}: hanya soal yang ada di set {@code terjawab}.</li>
	 *         <li>{@code belumDijawab}: hanya soal yang TIDAK ada di {@code terjawab}.</li>
	 *         <li>{@code benarDijawab}: hanya soal yang ada di set {@code benar}.</li>
	 *         <li>{@code salahDijawab}: hanya soal yang TIDAK ada di {@code benar}.</li>
	 *         <li>{@code nilaiDijawab}: hanya soal yang ada di set {@code dinilai}.</li>
	 *         <li>{@code belumNilaiDijawab}: hanya soal yang TIDAK ada di {@code dinilai}.</li>
	 *       </ul>
	 *       Soal yang lolos ditambahkan ke {@code bankSoals} (field instance yang digunakan oleh
	 *       {@link #initCriteria}).</li>
	 *   <li><b>Renderer baris:</b> Pasang {@link org.zkoss.zul.SimpleListModel} dan {@link ais.ui.util.MyRowRenderer}
	 *       ke grid. Renderer membuat sub-grid per soal dengan dua jalur berbeda:
	 *       <ul>
	 *         <li><b>PILIHAN_GANDA:</b> Tampilkan pertanyaan, lampiran soal, daftar pilihan (dengan tanda "(Benar)"
	 *             pada kunci jawaban), dan jawaban peserta. Bila peserta tidak memilih ({@code bankSoalDetail==null}),
	 *             tampilkan label "Tidak Dijawab". Dosen dapat menulis koreksi + unggah lampiran. Admin dengan
	 *             konfigurasi {@code adminBolehMerubahJawabanUjian} dapat mengubah pilihan via combobox. Tampilkan
	 *             riwayat revisi via {@link RevisiHelper}.</li>
	 *         <li><b>ESAY/JAWABAN_SINGKAT:</b> Tampilkan pertanyaan, lampiran soal, kunci jawaban (dari
	 *             {@link BankSoal#ambilBankSoalDetailEssay}), dan teks jawaban peserta beserta lampirannya.
	 *             Dosen dapat mengisi nilai (MyDoublebox dengan validasi maks=skor soal) dan menulis koreksi +
	 *             lampiran. Semua perubahan langsung di-update ke DB via sesi Hibernate mandiri di event
	 *             listener {@code onChange}.</li>
	 *       </ul></li>
	 *   <li>Panggil {@code grid.setModelCheckMobile(strset)} dan atur paging OS mode untuk navigasi per-soal.</li>
	 * </ol>
	 *
	 * <p><b>Manajemen sesi untuk write:</b> Setiap event listener {@code onChange} (koreksi, nilai, ubah jawaban)
	 * membuka sesi Hibernate mandiri via {@code HibernateUtil.getSessionFactory().openSession()} dengan
	 * begin/commit/rollback eksplisit, dan ditutup di {@code finally}. Ini mencegah konflik dengan sesi
	 * thread ZK yang sedang aktif.
	 *
	 * <p><b>Catatan bankSoalDetail null:</b> Pada soal PG, {@code bankSoalDetail==null} berarti peserta tidak
	 * memilih opsi apapun — bukan berarti jawaban hilang. Label khusus dengan pesan penjelasan ("peserta tidak
	 * memilih jawaban") ditampilkan untuk menghindari kebingungan yang pernah terjadi saat memeriksa skor 0.
	 *
	 * @param value bila bertipe {@link Boolean} dan {@code true}, memaksa refresh ulang cache soal dari DB;
	 *              bila {@code null} atau bukan Boolean, menggunakan cache yang ada
	 */
	@SuppressWarnings({ })
	public void loadData(Object value) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyArrayList<Long> ujianPunyaSoals = tempHasilUjianMahasiswa.ambilUjianPunyaSoals(
				tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);
		
		final MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = tempHasilUjianMahasiswa
				.ambilHasilUjianMahasiswaDetail((value != null && value instanceof Boolean) ? ((Boolean) value) : false,
						tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
						ujianPunyaSoals);
						
		Set<Long> terjawab = tempHasilUjianMahasiswa.ambilBankSoalIdTerjawab(hasilUjianMahasiswaDetailsa);
		Set<Long> benar = tempHasilUjianMahasiswa.ambilBankSoalIdTerjawabBenar(hasilUjianMahasiswaDetailsa);
		Set<Long> dinilai = tempHasilUjianMahasiswa.ambilBankSoalIdTerjawabDinilai(hasilUjianMahasiswaDetailsa);
		
		bankSoals = new ArrayList<BankSoal>();
		
		for (Long ujianPunyaSoalid : ujianPunyaSoals) {
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
			if (ujianPunyaSoal != null) {
				BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
				if (bankSoal != null) {
					if (telahDijawab.isChecked() && !terjawab.contains(bankSoal.getId())) {
						continue;
					}
					if (belumDijawab.isChecked() && terjawab.contains(bankSoal.getId())) {
						continue;
					}
					if (benarDijawab.isChecked() && !benar.contains(bankSoal.getId())) {
						continue;
					}
					// Perbaikan logika: Hapus duplikasi logika check benarDijawab
					if (salahDijawab.isChecked() && benar.contains(bankSoal.getId())) {
						continue;
					}
					if (nilaiDijawab.isChecked() && !dinilai.contains(bankSoal.getId())) {
						continue;
					}
					if (belumNilaiDijawab.isChecked() && dinilai.contains(bankSoal.getId())) {
						continue;
					}
					bankSoals.add(bankSoal);
				}
			}
		}

		ListModel strset = new SimpleListModel(bankSoals);
		grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {

			@Override
			public void render(Row rowUtama, Object arg1) throws Exception {
				rowUtama.setValign("top");
				final BankSoal bankSoal = (BankSoal) arg1;

				Grid gridSoal = new Grid();
				gridSoal.setOddRowSclass("non-odd");
				gridSoal.setWidth("100%");
				gridSoal.setParent(rowUtama);
				gridSoal.setHeight("100%");
				gridSoal.setSclass("fgrid");
				gridSoal.setStyle("background:transparent;");

				Rows myrowsSoal = new Rows();
				myrowsSoal.setParent(gridSoal);

				Set<Long> hasilUjianMahasiswaDetails = hasilUjianMahasiswaDetailsa.get(bankSoal.getId());

				if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {

					MyFormRow myrowaSoal = new MyFormRow();
					myrowaSoal.setParent(myrowsSoal);

					MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
					groupboxStyled.appendChild(new Caption("Pertanyaan"));
					groupboxStyled.setParent(myrowaSoal);
					new ais.ui.util.MyHtml(
							"<div style=\"font-size: 12px;font-family: Poppins,Helvetica,'sans-serif';\">"
									+ bankSoal.getSoal() + "</div>")
							.setParent(groupboxStyled);

					MyFormRow myrowalampiran = new MyFormRow();
					myrowalampiran.setParent(myrowsSoal);

					MyFormRow rowlampiran1Soal = new MyFormRow();
					rowlampiran1Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran2Soal = new MyFormRow();
					rowlampiran2Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran3Soal = new MyFormRow();
					rowlampiran3Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran4Soal = new MyFormRow();
					rowlampiran4Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran5Soal = new MyFormRow();
					rowlampiran5Soal.setParent(myrowsSoal);

					BankSoalAction.tampilkanLampiran(null, bankSoal, myrowalampiran, false, rowlampiran1Soal,
							rowlampiran2Soal, rowlampiran3Soal, rowlampiran4Soal, rowlampiran5Soal);

					List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(false);

					if (!bankSoalDetails.isEmpty()) {
						// Optimasi Memori: Gunakan StringBuilder untuk gabung list HTML
						StringBuilder sbJaw = new StringBuilder("<ul>");
						for (Long bankSoalDetailid : bankSoalDetails) {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
									.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
							if (bankSoalDetail != null) {

								String h = (tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian()
										.getTampilanHurufDiPilihanJawaban() ? bankSoalDetail.getHuruf() + ". " : "");

								String dd = h + (bankSoalDetail.getBetul() ? " (Benar) " : "") + bankSoalDetail.getJawaban();

								LampiranLain lampiranLain = LampiranLain.ambil(bankSoalDetail.getBankSoal().getId(),
										"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
								if (lampiranLain != null) {
									dd = dd + "<br><img src=\"" + lampiranLain.createLinkUri() + "\"></img>";
								}

								sbJaw.append("<li>").append(dd).append("</li>");
							}
						}
						sbJaw.append("</ul>");

						myrowaSoal = new MyFormRow();
						myrowaSoal.setParent(myrowsSoal);

						groupboxStyled = new MyGroupboxStyled();
						groupboxStyled.appendChild(new Caption("Pilihan"));
						groupboxStyled.setParent(myrowaSoal);

						new ais.ui.util.MyHtml(sbJaw.toString() + "<hr>").setParent(groupboxStyled);

						new Label(Common.getBahasaConfig("Skor Benar") + " : "
								+ Common.numberFormat.get().format(bankSoal.getSkor()) + ", "
								+ Common.getBahasaConfig("Skor Salah") + " : "
								+ Common.numberFormat.get().format(bankSoal.getSkorSalah()) + ", "
								+ Common.getBahasaConfig("Skor Default") + " : "
								+ Common.numberFormat.get().format(bankSoal.getSkorDefault())).setParent(groupboxStyled);
					}

					myrowaSoal = new MyFormRow();
					myrowaSoal.setParent(myrowsSoal);
					MyGroupboxStyled groupbox = new ais.ui.util.MyGroupboxStyled();
					groupbox.appendChild(new Caption("Jawaban"));
					groupbox.setParent(myrowaSoal);

					MyGrid mygrid = new MyGrid();
					mygrid.setWidth("100%");
					groupbox.appendChild(mygrid);
					mygrid.setSclass("fgrid");
					mygrid.setStyle("background:transparent;");

					Rows myrows = new Rows();
					myrows.setParent(mygrid);

					MyFormRow myrow = new MyFormRow();
					myrow.setValign("top");
					myrow.setParent(myrows);

					if (hasilUjianMahasiswaDetails == null || hasilUjianMahasiswaDetails.isEmpty()) {
						Label bolder = new Label(ais.common.Common.getBahasaConfig("Soal belum terjawab"));
						bolder.setStyle("font-size:16px;font-weight: bolder;color:red;");
						myrow.appendChild(bolder);
					} else {
						for (Long hasilUjianMahasiswaDetailid : hasilUjianMahasiswaDetails) {

							final HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());

							if (hasilUjianMahasiswaDetail != null) {

								BankSoalDetail bankSoalDetail = hasilUjianMahasiswaDetail.getBankSoalDetail();
								Vbox vbox = new Vbox();
								vbox.setParent(myrow);
								
								if (bankSoalDetail != null) {

									new ais.ui.util.MyHtml(bankSoalDetail == null
											? (hasilUjianMahasiswaDetail.getJawaban().isEmpty()
													? Common.getBahasaConfig("Tidak Dijawab")
													: hasilUjianMahasiswaDetail.getJawaban())
											: (bankSoalDetail.getHuruf() + ". " + bankSoalDetail.getJawaban()))
											.setParent(vbox);

									vbox.appendChild(new ais.ui.util.MyHtml(Common.getBahasaConfig("Hasil") + " : "
											+ (bankSoalDetail.getBetul() ? Common.getBahasaConfig("Benar")
													: Common.getBahasaConfig("Salah"))));

									groupbox = new ais.ui.util.MyGroupboxStyled();
									groupbox.appendChild(new Caption("Koreksi"));
									groupbox.setParent(vbox);

									Vbox vbox1 = new Vbox();
									vbox1.setParent(groupbox);
									
									if (mahasiswa == null && biodataCalonMahasiswa == null
											&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null) {

										final Textbox koreksi = new Textbox(hasilUjianMahasiswaDetail.getKoreksi());
										koreksi.setRows(2);
										koreksi.setWidth("450px");
										vbox1.appendChild(koreksi);
										koreksi.addEventListener("onChange", new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Session updateSession = null;
												Transaction tx = null;
												try {
													updateSession = HibernateUtil.getSessionFactory().openSession();
													tx = updateSession.beginTransaction();
													HasilUjianMahasiswaDetail hmd = (HasilUjianMahasiswaDetail) updateSession.get(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetail.getId());
													if(hmd != null) {
														hmd.setKoreksi(koreksi.getValue().trim());
														updateSession.update(hmd);
													}
													tx.commit();
												} catch(Exception e) {
													if(tx != null) tx.rollback();
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KoreksiHasilUjian.java:461");
												} finally {
													if(updateSession != null && updateSession.isOpen()) try{ updateSession.close(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KoreksiHasilUjian.java:463");}
												}
											}
										});

										Vbox myVbox = new Vbox();
										myVbox.setParent(vbox1);

										Hbox hbox = new Hbox();
										hbox.setParent(myVbox);
										LampiranLain.createDownloadUploadFileLain(hbox,
												hasilUjianMahasiswaDetail.getId(), "Lampiran Koreksi Ujian",
												"Lampiran Koreksi Ujian", false, new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {}
												}, null, false, false, false, true);

									} else {
										vbox1.appendChild(new Label(hasilUjianMahasiswaDetail.getKoreksi()));

										Vbox myVbox = new Vbox();
										myVbox.setParent(vbox1);

										Hbox hbox = new Hbox();
										hbox.setParent(myVbox);
										LampiranLain.createDownloadUploadFileLain(hbox,
												hasilUjianMahasiswaDetail.getId(), "Lampiran Koreksi Ujian",
												"Lampiran Koreksi Ujian", false, new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {}
												}, null, false, false, false, false);
									}

									RevisiHelper.createNewRevisi(HasilUjianMahasiswaDetail.class,
											hasilUjianMahasiswaDetail, "Lihat perubahan data").setParent(vbox);

									Double skor = bankSoalDetail.getBetul() ? bankSoal.getSkor() : bankSoal.getSkorSalah();

									new ais.ui.util.MyHtml(Common.getBahasaConfig("Skor yang diperoleh") + " : "
											+ Common.numberFormat.get().format(skor)).setParent(vbox);

									if (Common.getApakahAdmin() && adminBolehMerubahJawabanUjian) {

										Hbox hbox2 = new Hbox();
										hbox2.setParent(vbox);

										hbox2.appendChild(new Label(ais.common.Common.getBahasaConfig("Ubah jawaban ke : ")));
										final Combobox combobox = new Combobox();
										combobox.setCols(3);
										hbox2.appendChild(combobox);
										for (Long soalDetailid : bankSoal.ambilBankSoalDetail(false)) {

											BankSoalDetail soalDetail = (BankSoalDetail) GeneralValueObject
													.ambilData(BankSoalDetail.class, soalDetailid.toString());
											if (soalDetail != null) {
												Comboitem comboitem = new Comboitem(soalDetail.getHuruf()
														+ (soalDetail.getBetul() ? "(Benar)" : ""));
												comboitem.setDescription(soalDetail.getJawaban());
												comboitem.setValue(soalDetail);
												combobox.appendChild(comboitem);
											}
										}
										combobox.setReadonly(true);
										Common.selectComboItem(true, combobox, hasilUjianMahasiswaDetail.getBankSoalDetail());

										combobox.addEventListener("onChange", new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Session updateSession = null;
												Transaction tx = null;
												try {
													updateSession = HibernateUtil.getSessionFactory().openSession();
													tx = updateSession.beginTransaction();
													HasilUjianMahasiswaDetail hmd = (HasilUjianMahasiswaDetail) updateSession.get(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetail.getId());
													if (hmd != null) {
														hmd.setBankSoalDetail((BankSoalDetail) combobox.getSelectedItem().getValue());
														updateSession.update(hmd);
													}
													tx.commit();
												} catch(Exception e) {
													if(tx != null) tx.rollback();
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KoreksiHasilUjian.java:544");
												} finally {
													if(updateSession != null && updateSession.isOpen()) try{ updateSession.close(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KoreksiHasilUjian.java:546");}
												}

												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});
											}
										});
									}
								} else {
									// bankSoalDetail == null → peserta TIDAK memilih opsi pilihan-ganda.
									// Perjelas agar tidak rancu: skor 0 di sini karena tak ada pilihan
									// tersimpan, BUKAN karena jawaban benar dinilai salah. Tanda "(Benar)"
									// pada daftar Pilihan di atas adalah KUNCI jawaban, bukan pilihan peserta.
									String jwbPeserta = hasilUjianMahasiswaDetail.getJawaban();
									if (jwbPeserta == null || jwbPeserta.trim().isEmpty()) {
										Label tidakDijawab = new Label(
												Common.getBahasaConfig("Tidak Dijawab")
												+ " — peserta tidak memilih jawaban (tidak ada pilihan tersimpan)");
										tidakDijawab.setStyle("color:#b91c1c;font-weight:bold;");
										vbox.appendChild(tidakDijawab);
									} else {
										vbox.appendChild(new ais.ui.util.MyHtml(
												jwbPeserta.replaceAll("\n", "<br>")));
									}
									vbox.appendChild(new MyLabelBoldConfig("Skor yang diperoleh : "));
									vbox.appendChild(new Label(Common.numberFormat.get().format(hasilUjianMahasiswaDetail.getNilai())));
								}
							}
						}
					}

				} else {

					MyFormRow myrowaSoal = new MyFormRow();
					myrowaSoal.setParent(myrowsSoal);

					MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
					groupboxStyled.appendChild(new Caption("Soal"));
					groupboxStyled.setParent(myrowaSoal);
					new ais.ui.util.MyHtml(
							"<div style=\"font-size: 12px;font-family: Poppins,Helvetica,'sans-serif';\">"
									+ bankSoal.getSoal() + "</div>")
							.setParent(groupboxStyled);

					MyFormRow myrowalampiran = new MyFormRow();
					myrowalampiran.setParent(myrowsSoal);

					MyFormRow rowlampiran1Soal = new MyFormRow();
					rowlampiran1Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran2Soal = new MyFormRow();
					rowlampiran2Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran3Soal = new MyFormRow();
					rowlampiran3Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran4Soal = new MyFormRow();
					rowlampiran4Soal.setParent(myrowsSoal);

					MyFormRow rowlampiran5Soal = new MyFormRow();
					rowlampiran5Soal.setParent(myrowsSoal);

					BankSoalAction.tampilkanLampiran(null, bankSoal, myrowalampiran, false, rowlampiran1Soal,
							rowlampiran2Soal, rowlampiran3Soal, rowlampiran4Soal, rowlampiran5Soal);

					List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetailEssay(false);

					if (!bankSoalDetails.isEmpty()) {
						StringBuilder sbJaw = new StringBuilder();
						for (Long bankSoalDetailid : bankSoalDetails) {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
									.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
							if (bankSoalDetail != null) {
								sbJaw.append(bankSoalDetail.getEssay()).append("<br>");
							}
						}
						
						myrowaSoal = new MyFormRow();
						myrowaSoal.setParent(myrowsSoal);

						groupboxStyled = new MyGroupboxStyled();
						groupboxStyled.appendChild(new Caption("Jawaban"));
						groupboxStyled.setParent(myrowaSoal);

						new ais.ui.util.MyHtml(sbJaw.toString()).setParent(groupboxStyled);
						new ais.ui.util.MyHtml("<hr>Skor Soal : " + Common.numberFormat.get().format(bankSoal.getSkor()))
								.setParent(groupboxStyled);
					}

					myrowaSoal = new MyFormRow();
					myrowaSoal.setParent(myrowsSoal);

					MyGroupboxStyled groupbox = new ais.ui.util.MyGroupboxStyled();
					groupbox.appendChild(new Caption("Jawaban"));
					groupbox.setParent(myrowaSoal);

					MyGrid mygrid = new MyGrid();
					mygrid.setWidth("100%");
					groupbox.appendChild(mygrid);
					mygrid.setSclass("fgrid");
					mygrid.setStyle("background:transparent;");

					Rows myrows = new Rows();
					myrows.setParent(mygrid);

					MyFormRow myrow = new MyFormRow();
					myrow.setValign("top");
					myrow.setParent(myrows);

					if (hasilUjianMahasiswaDetails == null || hasilUjianMahasiswaDetails.isEmpty()) {
						Label bolder = new Label(ais.common.Common.getBahasaConfig("Soal belum terjawab"));
						bolder.setStyle("font-size:16px;font-weight: bolder;color:red;");
						myrow.appendChild(bolder);
					} else {
						for (Long hasilUjianMahasiswaDetailid : hasilUjianMahasiswaDetails) {

							final HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());

							if (hasilUjianMahasiswaDetail != null) {
								Vbox vbox = new Vbox();
								vbox.setParent(myrow);

								vbox.appendChild(new ais.ui.util.MyHtml(
										hasilUjianMahasiswaDetail.getJawaban().replaceAll("\n", "<br>")));

								BankSoalAction.tampilkanLampiran(hasilUjianMahasiswaDetail, vbox, false,
										bankSoal.getJumlahLampiran(), null);

								vbox.appendChild(new MyLabelBoldConfig("Skor yang diperoleh : "));

								if (mahasiswa == null && biodataCalonMahasiswa == null
										&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null) {
									final MyDoublebox nilai = new MyDoublebox(hasilUjianMahasiswaDetail.getNilai());
									vbox.appendChild(nilai);
									nilai.setCols(3);
									nilai.addEventListener("onChange", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											if (nilai.getValue() != null && nilai.getValue() > bankSoal.getSkor()) {
												MyMessageboxConfig.show("Skor soal adalah "
														+ Common.numberFormat.get().format(bankSoal.getSkor())
														+ ", Anda tidak bisa memasukkan skor yang didapat melebihi skor soal",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.INFORMATION, new EventListener() {
															@Override
															public void onEvent(Event arg0) throws Exception {
																nilai.setValue(0.0);
															}
														});
												return;
											}
											
											Session updateSession = null;
											Transaction tx = null;
											try {
												updateSession = HibernateUtil.getSessionFactory().openSession();
												tx = updateSession.beginTransaction();
												HasilUjianMahasiswaDetail hmd = (HasilUjianMahasiswaDetail) updateSession.get(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetail.getId());
												if(hmd != null) {
													hmd.setNilai(nilai.getValue());
													updateSession.update(hmd);
												}
												tx.commit();
												UjianRecomputeUtil.hitungUlangSekarang(tempHasilUjianMahasiswa.getId());
											} catch(Exception e) {
												if(tx != null) tx.rollback();
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KoreksiHasilUjian.java:716");
											} finally {
												if(updateSession != null && updateSession.isOpen()) try{ updateSession.close(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KoreksiHasilUjian.java:718");}
											}
										}
									});
								} else {
									vbox.appendChild(new Label(Common.numberFormat.get().format(hasilUjianMahasiswaDetail.getNilai())));
								}

								myrowaSoal = new MyFormRow();
								myrowaSoal.setParent(myrowsSoal);

								groupbox = new ais.ui.util.MyGroupboxStyled();
								groupbox.appendChild(new Caption("Koreksi"));
								groupbox.setParent(myrowaSoal);

								vbox = new Vbox();
								vbox.setParent(groupbox);

								if (mahasiswa == null && biodataCalonMahasiswa == null
										&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null) {
									final Textbox koreksi = new Textbox(hasilUjianMahasiswaDetail.getKoreksi());
									koreksi.setRows(5);
									koreksi.setWidth("450px");
									vbox.appendChild(koreksi);
									koreksi.addEventListener("onChange", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Session updateSession = null;
											Transaction tx = null;
											try {
												updateSession = HibernateUtil.getSessionFactory().openSession();
												tx = updateSession.beginTransaction();
												HasilUjianMahasiswaDetail hmd = (HasilUjianMahasiswaDetail) updateSession.get(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetail.getId());
												if (hmd != null) {
													hmd.setKoreksi(koreksi.getValue().trim());
													updateSession.update(hmd);
												}
												tx.commit();
											} catch(Exception e) {
												if(tx != null) tx.rollback();
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KoreksiHasilUjian.java:758");
											} finally {
												if(updateSession != null && updateSession.isOpen()) try{ updateSession.close(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KoreksiHasilUjian.java:760");}
											}
										}
									});

									Vbox myVbox = new Vbox();
									myVbox.setParent(vbox);

									Hbox hbox = new Hbox();
									hbox.setParent(myVbox);
									LampiranLain.createDownloadUploadFileLain(hbox, hasilUjianMahasiswaDetail.getId(),
											"Lampiran Koreksi Ujian", "Lampiran Koreksi Ujian", false,
											new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {}
											}, null, false, false, false, true);
								} else {
									new Label(hasilUjianMahasiswaDetail.getKoreksi()).setParent(vbox);
									Vbox myVbox = new Vbox();
									myVbox.setParent(vbox);

									Hbox hbox = new Hbox();
									hbox.setParent(myVbox);
									LampiranLain.createDownloadUploadFileLain(hbox, hasilUjianMahasiswaDetail.getId(),
											"Lampiran Koreksi Ujian", "Lampiran Koreksi Ujian", false,
											new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {}
											}, null, false, false, false, false);
								}

								RevisiHelper.createNewRevisi(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetail,
										"Lihat perubahan data").setParent(vbox);
							}
						}
					}
				}
			}
		});
		grid.setModelCheckMobile(strset);
		grid.getPagingChild().setMold("os");
	}

	/**
	 * <b>Tujuan:</b> Membangun seluruh UI panel koreksi hasil ujian di dalam komponen detail ({@link MyDetail})
	 * yang diberikan, mencakup toolbar aksi dan grid soal-jawaban. Method ini merupakan titik masuk utama
	 * dari luar kelas: dipanggil oleh {@link HasilUjianMahasiswaHelper} saat pengguna meng-expand baris peserta.
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Simpan referensi ke {@code tempHasilUjianMahasiswa} dan baca konfigurasi
	 *       {@code adminBolehMerubahJawabanUjian} dari tabel konfigurasi (AKTIF/TIDAK_AKTIF).</li>
	 *   <li>Tentukan {@link PertemuanPunyaUjian} yang digunakan: ambil dari {@code tempHasilUjianMahasiswa}
	 *       bila tidak null, jika tidak gunakan parameter {@code a}.</li>
	 *   <li>Bersihkan konten detail yang mungkin ada sebelumnya via {@link Common#clear(org.zkoss.zk.ui.Component)}.</li>
	 *   <li>Bila {@code pertemuanPunyaUjian != null} dan detail dalam keadaan terbuka ({@code isOpen()}),
	 *       bangun konten panel:
	 *       <ul>
	 *         <li>Container {@code MyDiv} dengan {@code min-height:2300px} agar scrollable.</li>
	 *         <li><b>Tombol "Ulang Ujian"</b> (hanya staf): mengkonfirmasi lewat {@link MyMessageboxConfig}
	 *             lalu (via timer ZK) me-reset semua {@link HasilUjianMahasiswaDetail} (null-kan jawaban,
	 *             bankSoalDetail, waktuJawab) dan memanggil {@link HasilUjianMahasiswa#reset()} untuk
	 *             mereset status peserta. Hanya tampil bila waktu ujian masih aktif.</li>
	 *         <li><b>Tombol "Cetak Sertifikat"</b>: tampil bila peserta lulus dan ujian punya template
	 *             sertifikat; memanggil {@link SertifikatAction#cetakSertifikat}.</li>
	 *         <li><b>Enam checkbox filter</b>: Belum/Telah dijawab, Benar/Salah, Telah/Belum dinilai;
	 *             masing-masing memanggil {@link #loadData(Object)} saat di-klik. Checkbox Benar/Salah
	 *             hanya tampil untuk soal PG; Telah/Belum dinilai hanya untuk Esai/Jawaban Singkat.</li>
	 *         <li><b>Tombol cetak/ekspor Excel</b>: via {@link Common#cetakData} dengan 12 kolom konten.</li>
	 *         <li><b>Tombol "Hitung Ulang"</b> (hanya untuk ujian PG): memanggil
	 *             {@link ProsesUjianHelper#hitungPilihanGanda} lewat sesi mandiri dan kemudian memicu
	 *             listener dari detail untuk refresh baris parent. Hanya tampil untuk jenis PILIHAN_GANDA.</li>
	 *         <li><b>Tombol "History"</b>: membuka {@link RevisiHasilUjianMahasiswaHelper} sebagai modal;
	 *             hanya aktif untuk staf (bukan mahasiswa/siswa/calon).</li>
	 *         <li><b>Tombol "Refresh"</b>: memanggil {@code loadData(true)} untuk muat ulang dari DB.</li>
	 *       </ul></li>
	 *   <li>Buat {@link MyGrid} dengan paging {@code mold="paging"} dan {@code pageSize=1} (satu soal per
	 *       halaman) dalam mode paging OS. Panggil {@link #loadData(Object)} untuk populasi grid pertama kali.</li>
	 * </ol>
	 *
	 * <p><b>Catatan sesi hitung ulang:</b> Listener tombol "Hitung Ulang" menggunakan
	 * {@code HibernateUtil.getSessionFactory().openSession()} (bukan currentSession) dengan try-finally,
	 * karena operasi ini berjalan di dalam event ZK yang sudah memiliki currentSession — membuka sesi baru
	 * secara eksplisit menghindari konflik transaksi.
	 *
	 * <p><b>Sifat/thread-safety:</b> Seluruh method berjalan di thread event ZK. Tidak ada penggunaan thread
	 * latar. Field instance ({@code grid}, {@code belumDijawab}, dst.) diinisialisasi di sini dan digunakan
	 * oleh {@link #loadData} yang dipanggil di thread yang sama.
	 *
	 * @param detail                   komponen detail ZK tempat panel koreksi akan dibangun; tidak boleh null
	 * @param tempHasilUjianMahasiswa  data hasil ujian peserta yang akan diperiksa; tidak boleh null
	 * @param a                        {@link PertemuanPunyaUjian} sebagai fallback bila
	 *                                 {@code tempHasilUjianMahasiswa.getPertemuanPunyaUjian()} null
	 */
	public void display(final MyDetail detail, final HasilUjianMahasiswa tempHasilUjianMahasiswa, PertemuanPunyaUjian a) {
		this.tempHasilUjianMahasiswa = tempHasilUjianMahasiswa;
		adminBolehMerubahJawabanUjian = Common.bolehKonfigurasi("adminBolehMerubahJawabanUjian", Konfigurasi.TIDAK_AKTIF);
		final PertemuanPunyaUjian pertemuanPunyaUjian;
		if (tempHasilUjianMahasiswa != null && tempHasilUjianMahasiswa.getPertemuanPunyaUjian() != null) {
			pertemuanPunyaUjian = tempHasilUjianMahasiswa.getPertemuanPunyaUjian();
		} else {
			pertemuanPunyaUjian = a;
		}

		Common.clear(detail);
		if (pertemuanPunyaUjian != null && detail.isOpen()) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 2300px;");
			groupbox.setParent(detail);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(groupbox);

			final EventListener hitungUlang = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();
						
						HasilUjianMahasiswa humRefresh = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, tempHasilUjianMahasiswa.getId());
						if (humRefresh != null) {
							MyArrayList<Long> ujianPunyaSoals = humRefresh.ambilUjianPunyaSoals(
									humRefresh.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);
							Map<Long, Set<Long>> hasilUjianMahasiswaDetails = humRefresh
									.ambilHasilUjianMahasiswaDetail(
											humRefresh.getPertemuanPunyaUjian().getJmlDitampilkan(),
											ujianPunyaSoals, true);
							ProsesUjianHelper.hitungPilihanGanda(humRefresh, hasilUjianMahasiswaDetails);
							
							session.update(humRefresh);
						}
						tx.commit();
					} catch (Exception e) {
						if (tx != null) tx.rollback();
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KoreksiHasilUjian.java:898");
					} finally {
						if (session != null && session.isOpen()) try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KoreksiHasilUjian.java:900");}
					}

					EventListener eventListener = (EventListener) detail.getAttribute("eventListener");
					if (eventListener != null) {
						eventListener.onEvent(new Event("", null, tempHasilUjianMahasiswa));
					}
				}
			};

			Tbmuser tbmuser = Common.getCurrentUser();
			mahasiswa = tbmuser.getMahasiswa();
			biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();

			boolean masihAdaWaktu = (pertemuanPunyaUjian.getMulaiUjian() == null
					|| pertemuanPunyaUjian.getMulaiUjian().before(ais.ui.util.WaktuUtil.getDate()))
					&& (pertemuanPunyaUjian.getSampaiUjian() == null
							|| pertemuanPunyaUjian.getSampaiUjian().after(ais.ui.util.WaktuUtil.getDate()));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ulang Ujian", "/img/svg/trash.svg");
			button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
			button.setDisabled(!masihAdaWaktu);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mengulang ujian ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {
											@SuppressWarnings("unchecked")
											@Override
											public void onEvent(Event arg0) throws Exception {
												Session session = null;
												Transaction tx = null;
												try {
													session = HibernateUtil.getSessionFactory().openSession();
													tx = session.beginTransaction();

													List<HasilUjianMahasiswaDetail> hasilUjianMahasiswaDetails = session
															.createCriteria(HasilUjianMahasiswaDetail.class)
															.add(Restrictions.eq("hasilUjianMahasiswa", tempHasilUjianMahasiswa))
															.list();
													
													for (HasilUjianMahasiswaDetail hmd : hasilUjianMahasiswaDetails) {
														hmd.setBankSoalDetail(null);
														hmd.setJawaban(null);
														hmd.setWaktuJawab(null);
														session.update(hmd);
													}
													
													HasilUjianMahasiswa humRefresh = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, tempHasilUjianMahasiswa.getId());
													if (humRefresh != null) {
														humRefresh.reset();
														session.update(humRefresh);
													}
													tx.commit();

													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															detail.setOpen(false);
														}
													});
												} catch(Exception e) {
													if(tx != null) tx.rollback();
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KoreksiHasilUjian.java:970");
												} finally {
													if(session != null && session.isOpen()) try{ session.close(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KoreksiHasilUjian.java:972");}
												}
											}
										});
									}
								}
							});
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Cetak Sertifikat", "/img/certificate-icon.png");
			button.setVisible(tempHasilUjianMahasiswa != null
					&& tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian() != null
					&& tempHasilUjianMahasiswa.getLulus()
					&& tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().getSertifikat() != null);
			button.setTooltiptext("Sertifikat");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					SertifikatAction.cetakSertifikat(tempHasilUjianMahasiswa);
				}
			});
			button.setParent(toolbar);

			belumDijawab = new MyCheckboxConfig("Yg belum dijawab");
			telahDijawab = new MyCheckboxConfig("yg telah dijawab");

			benarDijawab = new MyCheckboxConfig("Yg benar");
			salahDijawab = new MyCheckboxConfig("Yg salah");

			nilaiDijawab = new MyCheckboxConfig("Yg telah dinilai");
			belumNilaiDijawab = new MyCheckboxConfig("Yg belum dinilai");

			benarDijawab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			salahDijawab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			nilaiDijawab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			belumNilaiDijawab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			belumDijawab.setParent(toolbar);
			telahDijawab.setParent(toolbar);
			benarDijawab.setParent(toolbar);
			salahDijawab.setParent(toolbar);

			nilaiDijawab.setParent(toolbar);
			belumNilaiDijawab.setParent(toolbar);

			benarDijawab.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA));
			salahDijawab.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA));

			nilaiDijawab.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.ESAY)
					|| pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.JAWABAN_SINGKAT));
			belumNilaiDijawab.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.ESAY)
					|| pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.JAWABAN_SINGKAT));

			String[] contents = new String[] {
					tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getJadwalUjianPMB() == null
							? "hasilUjianMahasiswa.mahasiswa.nim"
							: "hasilUjianMahasiswa.biodataCalonMahasiswa.noRegistrasi",
					tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getJadwalUjianPMB() == null
							? "hasilUjianMahasiswa.mahasiswa.nama"
							: "hasilUjianMahasiswa.biodataCalonMahasiswa.nama",
					"bankSoal.soal-text", "bankSoalDetail.huruf", "bankSoalDetail.jawaban", "bankSoalDetail.betul",
					"bankSoalDetail.bankSoal.skor", "nilai", "jawaban", "koreksi", "waktuJawab",
					"hasilUjianMahasiswa" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
			toolbar.appendChild(cetakToolbarbutton);

			telahDijawab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			belumDijawab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/svg/check2-circle.svg");
			button.setTooltiptext("Hitung Ulang Nilai");
			button.setVisible(tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().getJenis()
					.equals(BankSoal.PILIHAN_GANDA));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(hitungUlang);
				}
			});
			button.setParent(toolbar);

			// Koreksi Otomatis (AI) — HANYA untuk ujian essay/manual & pemeriksa (bukan peserta).
			final MyToolbarbuttonConfig koreksiAiBtn = new MyToolbarbuttonConfig("Koreksi Otomatis (AI)",
					"/img/svg/sparkles.svg");
			koreksiAiBtn.setTooltiptext(
					"Koreksi otomatis jawaban peserta ini via AI (essay: Skor+Koreksi+hitung ulang; pilihan ganda: Koreksi/penjelasan)");
			koreksiAiBtn.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;");
			koreksiAiBtn.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null);
			koreksiAiBtn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					final boolean isPg = tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().getJenis()
							.equals(BankSoal.PILIHAN_GANDA);
					final java.util.List<Object[]> items = isPg ? kumpulkanPg(tempHasilUjianMahasiswa)
							: kumpulkanEssay(tempHasilUjianMahasiswa);
					if (items.isEmpty()) {
						MyMessageboxConfig.show("Tidak ada jawaban untuk dikoreksi.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					String konteks = bangunKonteksUjian(tempHasilUjianMahasiswa);
					String prompt = isPg ? promptKoreksiPg(items, konteks) : promptKoreksiEssay(items, konteks);
					GenerateAiHelper.jalankanAiStreaming("Koreksi Otomatis (AI)", prompt,
							new GenerateAiHelper.HasilAi() {
								@Override
								public void selesai(String resp) throws Exception {
									int n = isPg ? terapkanKoreksiPg(items, resp)
											: terapkanKoreksiEssay(items, resp, tempHasilUjianMahasiswa.getId());
									loadData(true);
									MyMessageboxConfig.show(
											n + (isPg ? " jawaban dikoreksi (penjelasan) via AI."
													: " jawaban essay dikoreksi via AI. Nilai dihitung ulang."),
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}, 2048);
				}
			});
			koreksiAiBtn.setParent(toolbar);

			button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setDisabled(!(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					RevisiHasilUjianMahasiswaHelper revisiHelper = new RevisiHasilUjianMahasiswaHelper(
							tempHasilUjianMahasiswa, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();
				}
			});
			button.setParent(toolbar);

			MyToolbarbuttonConfig riwayatJawaban = new MyToolbarbuttonConfig("Riwayat Jawaban", "/img/jadwal.png");
			riwayatJawaban.setTooltiptext(
					"Lihat riwayat jawaban peserta (essay & pilihan ganda) dan kembalikan ke jawaban terbaru yang pernah dijawab");
			riwayatJawaban.setDisabled(!(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null));
			riwayatJawaban.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					RiwayatJawabanUjianHelper.buka(tempHasilUjianMahasiswa, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);
						}
					});
				}
			});
			riwayatJawaban.setParent(toolbar);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			cari.setTooltiptext("Refresh");
			cari.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(true);
				}
			});
			cari.setParent(toolbar);

			grid = new MyGrid();
			grid.setSclass("fgrid");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(1);
			grid.setPagingPosition("top");
			groupbox.appendChild(grid);

			loadData(null);
		}
	}

	/**
	 * Kumpulkan seluruh jawaban ESSAY (yang terjawab) milik seorang peserta sebagai daftar
	 * {@code Object[]{detailId(Long), soal(String), kunci(String), jawaban(String), maxSkor(Double)}}.
	 * Dipakai bersama koreksi per-peserta & massal. Harus dipanggil pada thread event ZK.
	 */
	public static java.util.List<Object[]> kumpulkanEssay(HasilUjianMahasiswa hum) {
		java.util.List<Object[]> items = new java.util.ArrayList<Object[]>();
		try {
			MyArrayList<Long> ups = hum.ambilUjianPunyaSoals(hum.getPertemuanPunyaUjian().getJmlDitampilkan(),
					new Label(), true);
			MyHashMap<Long, java.util.Set<Long>> detailsa = hum.ambilHasilUjianMahasiswaDetail(false,
					hum.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), ups);
			for (Long upid : ups) {
				UjianPunyaSoal upo = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
						upid.toString());
				if (upo == null) {
					continue;
				}
				BankSoal bs = upo.getBankSoal();
				if (bs == null || bs.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
					continue;
				}
				StringBuilder kunci = new StringBuilder();
				for (Long bdid : bs.ambilBankSoalDetailEssay(false)) {
					BankSoalDetail bd = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							bdid.toString());
					if (bd != null && bd.getEssay() != null) {
						kunci.append(bd.getEssay()).append("\n");
					}
				}
				java.util.Set<Long> dset = detailsa.get(bs.getId());
				if (dset == null) {
					continue;
				}
				for (Long did : dset) {
					HasilUjianMahasiswaDetail d = (HasilUjianMahasiswaDetail) GeneralValueObject
							.ambilData(HasilUjianMahasiswaDetail.class, did.toString());
					if (d == null) {
						continue;
					}
					String jw = d.getJawaban() == null ? "" : d.getJawaban();
					// Koreksi SEMUA soal termasuk yang tidak dijawab (beri skor 0 + catatan).
					if (jw.trim().length() == 0) {
						jw = "(TIDAK DIJAWAB / kosong)";
					}
					items.add(new Object[]{ d.getId(), stripHtml(bs.getSoal()), kunci.toString().trim(), jw,
							bs.getSkor() });
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KoreksiHasilUjian.kumpulkanEssay");
		}
		return items;
	}

	/** Konteks OBE/mata kuliah dari peserta ujian (nama/SKS/prodi/deskripsi/CPMK) untuk koreksi lebih tepat. */
	public static String bangunKonteksUjian(HasilUjianMahasiswa hum) {
		StringBuilder c = new StringBuilder();
		try {
			ais.database.model.Matakuliah mk = null;
			try {
				if (hum != null && hum.getPertemuanPunyaUjian() != null
						&& hum.getPertemuanPunyaUjian().getUjian() != null) {
					mk = hum.getPertemuanPunyaUjian().getUjian().getMatakuliah();
				}
			} catch (Exception e) {
			}
			if (mk == null) {
				return "";
			}
			c.append("=== KONTEKS MATA KULIAH (untuk koreksi objektif; jangan diulang di jawaban) ===\n");
			c.append("Matakuliah: ").append(mk.getNama() == null ? "" : mk.getNama().trim());
			if (mk.getKode() != null) {
				c.append(" (").append(mk.getKode()).append(")");
			}
			c.append("\n");
			try {
				if (mk.getJurusan() != null && mk.getJurusan().getNama() != null) {
					c.append("Program Studi: ").append(mk.getJurusan().getNama().trim()).append("\n");
				}
			} catch (Exception e) {
			}
			try {
				String desk = mk.getDeskripsiPembelajaran();
				if (desk == null || desk.trim().length() == 0) {
					desk = mk.getKeterangan();
				}
				if (desk != null && desk.trim().length() > 0) {
					String d = desk.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
					c.append("Deskripsi MK: ").append(d.length() > 400 ? d.substring(0, 400) + "…" : d).append("\n");
				}
			} catch (Exception e) {
			}
			try {
				String cpmkCsv = mk.getCapaianPembelajaranLulusan();
				if (cpmkCsv != null) {
					StringBuilder cs = new StringBuilder();
					for (String s : cpmkCsv.split(",")) {
						s = s.trim();
						if (s.length() == 0) {
							continue;
						}
						Object o = GeneralValueObject.ambilData(
								ais.database.model.obe.CapaianPembelajaranLulusan.class, s);
						if (o instanceof ais.database.model.obe.CapaianPembelajaranLulusan) {
							ais.database.model.obe.CapaianPembelajaranLulusan cp = (ais.database.model.obe.CapaianPembelajaranLulusan) o;
							cs.append("  - ").append(cp.getKode() == null ? "" : cp.getKode()).append(": ")
									.append(cp.getNama() == null ? "" : cp.getNama()).append("\n");
						}
					}
					if (cs.length() > 0) {
						c.append("CPMK:\n").append(cs);
					}
				}
			} catch (Exception e) {
			}
			c.append("=== AKHIR KONTEKS ===\n\n");
		} catch (Exception e) {
		}
		return c.toString();
	}

	/** Bangun prompt koreksi essay dari daftar item {@link #kumpulkanEssay} + konteks OBE/MK. */
	public static String promptKoreksiEssay(java.util.List<Object[]> items, String konteks) {
		StringBuilder p = new StringBuilder();
		if (konteks != null && konteks.length() > 0) {
			p.append(konteks);
		}
		p.append("Anda dosen pemeriksa ujian. Koreksi jawaban ESSAY mahasiswa berikut secara objektif ");
		p.append("berdasarkan kunci & konteks mata kuliah. Untuk TIAP nomor beri: skor (angka 0 sampai skor ");
		p.append("maksimal nomor itu; beri 0 bila jawaban kosong/tidak dijawab) dan koreksi singkat ");
		p.append("(sebutkan yang sudah benar, yang kurang/salah, dan saran perbaikan) dalam Bahasa Indonesia.\n\n");
		for (int i = 0; i < items.size(); i++) {
			Object[] it = items.get(i);
			p.append("Nomor ").append(i + 1).append(":\n");
			p.append("Soal: ").append(it[1]).append("\n");
			String kunci = (String) it[2];
			if (kunci != null && kunci.trim().length() > 0) {
				p.append("Kunci jawaban: ").append(kunci).append("\n");
			}
			p.append("Jawaban mahasiswa: ").append(it[3]).append("\n");
			p.append("Skor maksimal: ").append(it[4]).append("\n\n");
		}
		p.append("Keluarkan HANYA JSON array valid (tanpa teks/markdown lain), satu objek per nomor sesuai urutan:\n");
		p.append("[{\"no\":1,\"skor\":8,\"koreksi\":\"...\"}]\n");
		p.append("skor TIDAK boleh melebihi skor maksimal nomor tersebut.\n");
		return p.toString();
	}

	/**
	 * Terapkan hasil koreksi AI (JSON array {no,skor,koreksi}) ke DB: isi nilai (dibatasi skor maks) &amp;
	 * koreksi tiap {@link HasilUjianMahasiswaDetail}, lalu hitung ulang nilai peserta. Kembalikan jumlah terisi.
	 */
	public static int terapkanKoreksiEssay(java.util.List<Object[]> items, String jsonResp, Long humId) {
		int n = 0;
		try {
			if (jsonResp == null) {
				return 0;
			}
			int a = jsonResp.indexOf('[');
			int b = jsonResp.lastIndexOf(']');
			if (a < 0 || b <= a) {
				return 0;
			}
			org.json.JSONArray arr = new org.json.JSONArray(jsonResp.substring(a, b + 1));
			org.hibernate.Session s = null;
			org.hibernate.Transaction tx = null;
			try {
				s = HibernateUtil.getSessionFactory().openSession();
				tx = s.beginTransaction();
				for (int i = 0; i < arr.length(); i++) {
					org.json.JSONObject o = arr.optJSONObject(i);
					if (o == null) {
						continue;
					}
					int no = o.optInt("no", i + 1);
					int idx = no - 1;
					if (idx < 0 || idx >= items.size()) {
						idx = i;
					}
					if (idx < 0 || idx >= items.size()) {
						continue;
					}
					Object[] it = items.get(idx);
					Long did = (Long) it[0];
					double maxSkor = it[4] == null ? 0.0 : ((Double) it[4]).doubleValue();
					double skor = o.optDouble("skor", 0.0);
					if (skor > maxSkor) {
						skor = maxSkor;
					}
					if (skor < 0) {
						skor = 0;
					}
					String kor = o.optString("koreksi", "");
					HasilUjianMahasiswaDetail hmd = (HasilUjianMahasiswaDetail) s
							.get(HasilUjianMahasiswaDetail.class, did);
					if (hmd != null) {
						hmd.setNilai(skor);
						hmd.setKoreksi(kor);
						s.update(hmd);
						n++;
					}
				}
				tx.commit();
			} catch (Exception e) {
				if (tx != null) {
					try {
						tx.rollback();
					} catch (Exception er) {
					}
				}
				ais.common.ErrorAuditUtil.record(e, "KoreksiHasilUjian.terapkanKoreksiEssay");
			} finally {
				if (s != null && s.isOpen()) {
					try {
						s.close();
					} catch (Exception e) {
					}
				}
			}
			if (n > 0 && humId != null) {
				UjianRecomputeUtil.hitungUlangSekarang(humId);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KoreksiHasilUjian.terapkanKoreksiEssay.parse");
		}
		return n;
	}

	private static String stripHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}

	private static String safeK(String s) {
		return s == null ? "" : s.trim();
	}

	/**
	 * Kumpulkan jawaban PILIHAN GANDA peserta sebagai {@code Object[]{detailId(Long), soal(String),
	 * opsi(String), jawabanPeserta(String), benar(Boolean)}}. Untuk membuat KOREKSI/penjelasan via AI
	 * (skor PG tetap otomatis dari sistem). Harus dipanggil pada thread event ZK.
	 */
	public static java.util.List<Object[]> kumpulkanPg(HasilUjianMahasiswa hum) {
		java.util.List<Object[]> items = new java.util.ArrayList<Object[]>();
		try {
			MyArrayList<Long> ups = hum.ambilUjianPunyaSoals(hum.getPertemuanPunyaUjian().getJmlDitampilkan(),
					new Label(), true);
			MyHashMap<Long, java.util.Set<Long>> detailsa = hum.ambilHasilUjianMahasiswaDetail(false,
					hum.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), ups);
			for (Long upid : ups) {
				UjianPunyaSoal upo = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
						upid.toString());
				if (upo == null) {
					continue;
				}
				BankSoal bs = upo.getBankSoal();
				if (bs == null || !bs.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
					continue;
				}
				StringBuilder opsi = new StringBuilder();
				for (Long did : bs.ambilBankSoalDetail(false)) {
					BankSoalDetail bd = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							did.toString());
					if (bd == null) {
						continue;
					}
					opsi.append(safeK(bd.getHuruf())).append(". ").append(safeK(bd.getJawaban()))
							.append(Boolean.TRUE.equals(bd.getBetul()) ? " (BENAR)" : "").append("\n");
				}
				java.util.Set<Long> dset = detailsa.get(bs.getId());
				if (dset == null) {
					continue;
				}
				for (Long hid : dset) {
					HasilUjianMahasiswaDetail d = (HasilUjianMahasiswaDetail) GeneralValueObject
							.ambilData(HasilUjianMahasiswaDetail.class, hid.toString());
					if (d == null) {
						continue;
					}
					BankSoalDetail chosen = null;
					try {
						chosen = d.getBankSoalDetail();
					} catch (Exception e) {
					}
					String jw;
					boolean benar;
					if (chosen != null) {
						jw = safeK(chosen.getHuruf()) + ". " + safeK(chosen.getJawaban());
						benar = Boolean.TRUE.equals(chosen.getBetul());
					} else {
						jw = "(tidak dijawab)";
						benar = false;
					}
					items.add(new Object[]{ d.getId(), stripHtml(bs.getSoal()), opsi.toString().trim(), jw,
							Boolean.valueOf(benar) });
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KoreksiHasilUjian.kumpulkanPg");
		}
		return items;
	}

	/** Prompt koreksi/penjelasan Pilihan Ganda (skor tetap otomatis; hanya mengisi kolom Koreksi). */
	public static String promptKoreksiPg(java.util.List<Object[]> items, String konteks) {
		StringBuilder p = new StringBuilder();
		if (konteks != null && konteks.length() > 0) {
			p.append(konteks);
		}
		p.append("Anda dosen. Berikan KOREKSI/PENJELASAN singkat untuk tiap jawaban PILIHAN GANDA mahasiswa: ");
		p.append("jelaskan mengapa jawaban yang benar itu tepat, dan bila jawaban mahasiswa SALAH jelaskan letak ");
		p.append("kesalahannya. Bahasa Indonesia, ringkas & edukatif.\n\n");
		for (int i = 0; i < items.size(); i++) {
			Object[] it = items.get(i);
			p.append("Nomor ").append(i + 1).append(":\nSoal: ").append(it[1]).append("\nOpsi:\n").append(it[2])
					.append("\nJawaban mahasiswa: ").append(it[3])
					.append(Boolean.TRUE.equals(it[4]) ? " (BENAR)" : " (SALAH)").append("\n\n");
		}
		p.append("Keluarkan HANYA JSON array valid (satu objek per nomor sesuai urutan):\n");
		p.append("[{\"no\":1,\"koreksi\":\"...\"}]\n");
		return p.toString();
	}

	/** Terapkan koreksi PG (hanya kolom koreksi; skor/nilai TIDAK diubah). Kembalikan jumlah terisi. */
	public static int terapkanKoreksiPg(java.util.List<Object[]> items, String jsonResp) {
		int n = 0;
		try {
			if (jsonResp == null) {
				return 0;
			}
			int a = jsonResp.indexOf('[');
			int b = jsonResp.lastIndexOf(']');
			if (a < 0 || b <= a) {
				return 0;
			}
			org.json.JSONArray arr = new org.json.JSONArray(jsonResp.substring(a, b + 1));
			org.hibernate.Session s = null;
			org.hibernate.Transaction tx = null;
			try {
				s = HibernateUtil.getSessionFactory().openSession();
				tx = s.beginTransaction();
				for (int i = 0; i < arr.length(); i++) {
					org.json.JSONObject o = arr.optJSONObject(i);
					if (o == null) {
						continue;
					}
					int no = o.optInt("no", i + 1);
					int idx = no - 1;
					if (idx < 0 || idx >= items.size()) {
						idx = i;
					}
					if (idx < 0 || idx >= items.size()) {
						continue;
					}
					Long did = (Long) items.get(idx)[0];
					String kor = o.optString("koreksi", "");
					HasilUjianMahasiswaDetail hmd = (HasilUjianMahasiswaDetail) s
							.get(HasilUjianMahasiswaDetail.class, did);
					if (hmd != null) {
						hmd.setKoreksi(kor);
						s.update(hmd);
						n++;
					}
				}
				tx.commit();
			} catch (Exception e) {
				if (tx != null) {
					try {
						tx.rollback();
					} catch (Exception er) {
					}
				}
				ais.common.ErrorAuditUtil.record(e, "KoreksiHasilUjian.terapkanKoreksiPg");
			} finally {
				if (s != null && s.isOpen()) {
					try {
						s.close();
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KoreksiHasilUjian.terapkanKoreksiPg.parse");
		}
		return n;
	}
}
