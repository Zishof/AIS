package ais.ui.util;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import org.zkoss.zul.Vbox;

import ais.action.master.helper.KrsHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Perender baris ZK (mengimplementasikan kontrak {@code render(Row, Object)} dari
 * {@link ais.ui.util.MyRowRenderer}, basis kustom aplikasi untuk {@code org.zkoss.zul.RowRenderer})
 * untuk grid <b>rekap KRS (Kartu Rencana Studi) per semester/tahap</b> seorang mahasiswa. Satu baris
 * grid mewakili satu periode akademik (tahun ajaran + semester, atau tahun ajaran + tahapan bila
 * sistem tahapan aktif) dan menampilkan ringkasannya (keterangan, IP/IPK, total SKS) sekaligus
 * menyediakan area yang dapat dibuka (expander) berisi daftar mata kuliah KRS lengkap untuk periode
 * tersebut.
 *
 * <h2>Bentuk data masukan</h2>
 * <p>
 * {@code render(Row, Object)} menerima {@code arg1} berupa {@code String[]} (BUKAN entitas
 * Hibernate) dengan makna per indeks:
 * </p>
 * <ul>
 * <li>{@code data[0]} — label tahun ajaran, ditampilkan apa adanya di kolom pertama.</li>
 * <li>{@code data[1]} — string berformat {@code "semester,..."}; hanya bagian sebelum koma pertama
 * yang diparsing sebagai angka semester ({@code semester}). Bila parsing gagal, semester dianggap
 * {@code 0}. Nilai semester {@code 1000} adalah penanda khusus "mahasiswa sudah Lulus" (kolom
 * keterangan menampilkan teks "Lulus", bukan {@code data[1]} mentah).</li>
 * <li>{@code data[2]} — teks keterangan tambahan, ditampilkan di kolom ketiga; bila indeks ini tidak
 * ada ({@code ArrayIndexOutOfBoundsException}), kolom dikosongkan alih-alih melempar galat.</li>
 * <li>{@code data[3]} — angka tahapan, diparsing serupa {@code data[1]} (default {@code 0} bila
 * gagal). Nilai tahapan {@code -1} adalah penanda "tidak berlaku" — kolom semester maupun tahapan
 * dikosongkan untuk baris ini.</li>
 * </ul>
 *
 * <h2>Pola "muat saat dibuka" + "muat tertunda" berlapis</h2>
 * <p>
 * Kelas ini memakai DUA mekanisme penundaan render yang berbeda tujuan:
 * </p>
 * <ol>
 * <li><b>Expander {@link ais.ui.util.MyDetail}</b> — daftar mata kuliah KRS lengkap (dibangun lewat
 * {@link ais.action.master.helper.KrsHelper#display}) hanya dirender ke {@code html}/
 * {@code komentarshtml} ketika pengguna membuka baris (event {@code "onOpen"}), bukan saat grid
 * pertama kali dimuat — menghindari beban query untuk seluruh baris grid sekaligus.</li>
 * <li><b>Timer default ({@code Common.createDefaultTimer}), hanya bila {@code rinci=true}</b> —
 * dijadwalkan untuk berjalan setelah render awal grid selesai (mendekati pola "render dulu, hitung
 * data berat kemudian" agar grid tidak terasa lambat saat pertama tampil). Timer ini: (a)
 * menyinkronkan/menghitung ulang record {@link ais.database.model.KrsMahasiswa} lewat
 * {@code Common.singkronkanKrsMahasiswa}; (b) mengisi label catatan dan catatan KHS; (c) merender
 * ringkasan KRS sebagai HTML lewat {@code Mahasiswa#rubahKeteranganPengambilanKRS} dan jumlah
 * komentar; (d) bila {@code semester > 0}, mengisi label IP/IPK dan SKS (termasuk rincian SKS
 * konversi vs bukan-konversi bila ada); dan (e) MENDETEKSI apakah baris ini adalah periode akademik
 * AKTIF mahasiswa saat ini — bila cocok, baris disorot hijau ({@code "border:0px;background:
 * #C2FFA3;"}) dan expander-nya DIBUKA OTOMATIS beserta simulasi event {@code onOpen} (memanggil
 * {@code eventListener.onEvent(null)} secara langsung) sehingga daftar KRS periode aktif langsung
 * terlihat tanpa perlu diklik. Deteksi periode aktif bercabang dua sesuai
 * {@link ais.common.ConstantValues#aktifkanTahapan}: sistem semester klasik membandingkan
 * {@code Common.getSemester(...)} terhadap {@code semester}, sedangkan sistem tahapan membandingkan
 * {@code mahasiswa.currentTahapan()} terhadap {@code tahapan}.</li>
 * </ol>
 */
public class KrsMahasiswaDataRenderer extends ais.ui.util.MyRowRenderer {

	/** Sumber data KRS: {@code true} bila dihitung langsung dari database (bukan cache/snapshot). */
	private boolean keDatabase;
	/** Mahasiswa yang KRS-nya direkap oleh grid ini. */
	private Mahasiswa mahasiswa;
	/** Kode semester pendek (mis. semester antara), boleh {@code null} bila tidak berlaku. */
	private Integer semesterPendek;
	/** Bila {@code true}, jadwalkan perhitungan ringkasan (IP/IPK/SKS) dan auto-expand periode aktif. */
	private boolean rinci;
	/** Bila {@code true}, hitung/tampilkan KRS dalam konteks remedial. */
	private boolean remedial;

	/**
	 * Membuat perender untuk satu mahasiswa dengan parameter konteks perhitungan KRS yang akan
	 * diteruskan ke setiap baris yang dirender.
	 *
	 * @param keDatabase     ambil data langsung dari database (bukan cache) bila {@code true}
	 * @param mahasiswa      mahasiswa yang KRS-nya direkap
	 * @param semesterPendek kode semester pendek, boleh {@code null}
	 * @param remedial       tandai konteks remedial
	 * @param rinci          aktifkan perhitungan ringkasan tertunda (IP/IPK/SKS) dan
	 *                       auto-expand baris periode aktif
	 */
	public KrsMahasiswaDataRenderer(boolean keDatabase, Mahasiswa mahasiswa, Integer semesterPendek, boolean remedial,
			boolean rinci) {
		this.keDatabase = keDatabase;
		this.mahasiswa = mahasiswa;
		this.semesterPendek = semesterPendek;
		this.rinci = rinci;
		this.remedial = remedial;
	}

	/**
	 * Merender satu baris rekap KRS. Lihat javadoc kelas untuk pemetaan lengkap kolom {@code data}
	 * dan penjelasan mekanisme "muat saat dibuka"/timer tertunda. Ringkasnya, method ini: mem-parsing
	 * semester/tahapan dari {@code data}, membangun label tahun ajaran/semester/tahapan/keterangan,
	 * menyiapkan label kosong untuk IP/SKS yang akan diisi belakangan, memasang {@link MyDetail}
	 * dengan handler {@code onOpen} yang memuat daftar KRS lengkap via
	 * {@link ais.action.master.helper.KrsHelper}, dan — bila {@link #rinci} — menjadwalkan
	 * perhitungan ringkasan tertunda lewat {@code Common.createDefaultTimer}.
	 *
	 * @param arg0 baris grid ZK tujuan render
	 * @param arg1 data baris, di-cast ke {@code String[]} (lihat javadoc kelas untuk makna tiap
	 *             indeks)
	 * @throws Exception diteruskan dari kegagalan parsing/kalkulasi di dalam handler event
	 */
	@Override
	public void render(final Row arg0, Object arg1) throws Exception {
		arg0.setValign("top");
		// TODO Auto-generated method stub
		final String[] data = (String[]) arg1;
		final Boolean editable = true;
		final MyDetail detail = new MyDetail();
		Integer smt;
		try {
			smt = Integer.parseInt(data[1].split(",")[0]);
		} catch (Exception e) {
			smt = 0;
		}
		final Integer semester = smt;

		Integer tahap;
		try {
			tahap = Integer.parseInt(data[3]);
		} catch (Exception e) {
			tahap = 0;
		}
		final Integer tahapan = tahap;

		final String tahunAjaran = data[0];

		final Html html = new ais.ui.util.MyHtml("");
		final Html komentarshtml = new ais.ui.util.MyHtml("");
		detail.setParent(arg0);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					KrsHelper krsHelper = new KrsHelper(semesterPendek, remedial);
					krsHelper.display(editable, mahasiswa, tahunAjaran, semester, tahapan, detail, html, komentarshtml,
							keDatabase);
				}
			}
		};
		detail.addEventListener("onOpen", eventListener);

		new Label(data[0]).setParent(arg0);
		new Label(tahapan != null && tahapan.equals(-1) ? "" : semester.equals(1000) ? "Lulus" : data[1])
				.setParent(arg0);
		new Label(tahapan != null && tahapan.equals(-1) ? "" : tahapan + "").setParent(arg0);
		try {
			new Label(data[2]).setParent(arg0);
		} catch (Exception e) {
			new Label().setParent(arg0);
		}

		final Label ip = new Label();
		ip.setParent(arg0);
		final MyLabelAgakKecil sks = new MyLabelAgakKecil();
		sks.setParent(arg0);

		html.setParent(arg0);
		komentarshtml.setParent(arg0);

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		final MyLabelAgakKecil catatan = new MyLabelAgakKecil();
		final MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil();
		catatan.setParent(vbox);
		catatanKhs.setParent(vbox);

		if (rinci) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					if (!ConstantValues.aktifkanTahapan) {
						String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
						if (Common
								.getSemester(mahasiswa.getTahunangkatan(), semesterMulai,
										mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai())
								.equals(semester)) {
							arg0.setStyle("border:0px;background: #C2FFA3;");
							detail.setOpen(true);
							eventListener.onEvent(null);
						}
					} else {
						Integer t = mahasiswa.currentTahapan();
						if (t != null && !t.equals(0) && tahapan != null && !tahapan.equals(0) && tahapan.equals(t)) {
							arg0.setStyle("border:0px;background: #C2FFA3;");
							detail.setOpen(true);
							eventListener.onEvent(null);
						}
					}

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
							semesterPendek, keDatabase);
					catatan.setValue(krsMahasiswa.getCatatan());
					catatanKhs.setValue(krsMahasiswa.getCatatanKhs());
					String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek,
							krsMahasiswa, remedial);
					html.setContent(krs);
					Integer komentars = krsMahasiswa.getKomentars();

					String kom = komentars == 0 ? "Tidak ada komentar" : "Terdapat " + komentars + " komentar";
					komentarshtml.setContent(kom);

					if (semester > 0) {
						Double ipmhs = krsMahasiswa.getIps();
						Double ipkmhs = krsMahasiswa.getIpk();
						ip.setValue(Common.numberFormat.get().format(ipmhs) + " / " + Common.numberFormat.get().format(ipkmhs));

						Integer sksmhss = krsMahasiswa.getSksYangDiambil();
						Integer sksmhs = krsMahasiswa.getSksk();
						Integer skskonversi = krsMahasiswa.getSksKonversi();
						Integer sksBukanKonversi = krsMahasiswa.getSksBukanKonversi();
						sks.setValue(Common.numberFormat.get().format(sksmhss) + " / " + Common.numberFormat.get().format(sksmhs)
								+ (skskonversi > 0
										? " (Bukan Konversi : " + Common.numberFormat.get().format(sksBukanKonversi)
												+ " SKS, Konversi " + Common.numberFormat.get().format(skskonversi) + " SKS)"
										: ""));
					}
				}
			});
		}
	}
}
