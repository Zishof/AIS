package ais.action.master.helper;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Button;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Komentar;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.common.listener.DataLoader;
import ais.ui.util.MyHtml;
import ais.ui.util.MyWindow;

/**
 * <h2>AksiKrsMahasiswaHelper &mdash; Panel Aksi per-baris pada daftar "Rencana Studi Mahasiswa"</h2>
 *
 * <p>Kelas bantu (helper) ini bertugas <b>membangun satu sel "Aksi"</b> yang muncul di sisi kanan
 * setiap baris mahasiswa pada layar <i>Manajemen KRS / Rencana Studi Mahasiswa</i>
 * ({@code MonitorKRSMahasiswaAction}). Di dalam sel tersebut disediakan dua tombol ringkas dan
 * modern:</p>
 * <ol>
 *   <li><b>"Daftar KRS"</b> &mdash; membuka jendela (popup) berisi seluruh rincian Kartu Rencana
 *       Studi mahasiswa yang bersangkutan (mata kuliah yang diambil, status persetujuan, nilai,
 *       dan sebagainya). Isi jendela ini <i>tidak dibuat ulang dari nol</i>, melainkan
 *       memanfaatkan kembali ({@code reuse}) komponen yang sudah ada, yaitu
 *       {@link TampilStudiMahasiswaHelper#tampil(Mahasiswa, DataLoader, Boolean, Integer)}. Dengan
 *       cara ini, bila suatu saat tampilan rincian KRS diperbarui, panel di sini otomatis ikut
 *       menyesuaikan tanpa perlu diubah &mdash; memudahkan pemeliharaan (maintenance) di kemudian
 *       hari.</li>
 *   <li><b>"Komentar"</b> &mdash; membuka jendela berisi seluruh catatan/komentar yang pernah
 *       dituliskan untuk mahasiswa tersebut (mis. arahan dosen Pembimbing Akademik). Komentar
 *       ditampilkan sebagai kartu-kartu yang rapi dan mudah dibaca, terbaru di atas.</li>
 * </ol>
 *
 * <h3>Alasan dibuat sebagai kelas terpisah (bukan ditulis langsung di dalam renderer)</h3>
 * <p>Renderer baris pada {@code MonitorKRSMahasiswaAction} sudah sangat panjang. Menaruh logika
 * tombol, jendela popup, kueri komentar, serta gaya (CSS) di dalamnya akan membuatnya makin sulit
 * dibaca dan rawan salah. Dengan memindahkan seluruh urusan "panel aksi" ke satu kelas mandiri
 * yang berdiri sendiri, kita memperoleh beberapa keuntungan: (a) <b>dapat dipakai ulang</b> di
 * layar lain yang juga menampilkan daftar mahasiswa; (b) <b>satu titik perubahan</b> &mdash; ubah
 * gaya atau perilaku tombol cukup di sini; (c) <b>pengujian &amp; penelusuran galat lebih mudah</b>
 * karena tanggung jawabnya jelas dan sempit.</p>
 *
 * <h3>Tampilan &amp; keterbacaan pada perangkat (responsif)</h3>
 * <p>Tombol dirancang mengikuti praktik terbaik antarmuka masa kini: berbentuk <i>pill</i>
 * (sudut membulat), memakai ikon + teks agar maksud tombol langsung terbaca, serta disusun dalam
 * wadah <i>flex</i> yang otomatis menumpuk ke bawah pada layar sempit (ponsel) dan tetap ringkas
 * pada layar lebar (desktop). Gaya (CSS modern, tanpa pustaka chart pihak ketiga) disuntikkan
 * <b>satu kali saja per halaman</b> lewat {@link #pastikanGayaTerpasang(Component)} sehingga tidak
 * membebani memori walau daftar berisi ratusan baris &mdash; ini penting untuk efisiensi memori:
 * alih-alih menempelkan atribut {@code style} yang sama berulang kali di tiap tombol tiap baris,
 * kita cukup satu blok gaya global yang dipakai bersama melalui nama kelas (sclass).</p>
 *
 * <h3>Manajemen sesi database (Hibernate)</h3>
 * <p>Seluruh akses basis data di kelas ini berjalan di dalam konteks event ZK, sehingga memakai
 * {@link HibernateUtil#currentSession()}. Sesi jenis ini <b>tidak boleh ditutup manual</b> karena
 * daur hidupnya sudah dikelola otomatis oleh kerangka kerja (akan ditutup saat request selesai).
 * Menutupnya sendiri justru berisiko memutus sesi yang masih dipakai komponen lain pada halaman
 * yang sama. Kelas ini sengaja <i>tidak</i> membuka {@code openSession()} maupun
 * {@code currentNativeSession()} agar tidak ada sesi yang perlu ditutup di blok {@code finally}.</p>
 *
 * <h3>Efisiensi memori &amp; kinerja</h3>
 * <p>Kueri komentar dibatasi jumlahnya ({@link #BATAS_KOMENTAR}) agar tidak menarik data
 * berlebihan; jendela KRS dibangun hanya saat tombol benar-benar diklik (pemuatan malas/lazy),
 * bukan saat baris digambar &mdash; sehingga membuka daftar berisi banyak mahasiswa tetap ringan.
 * Semua teks yang berasal dari data (nama, komentar) di-<i>escape</i> lebih dulu lewat
 * {@link #amankanHtml(String)} untuk mencegah rusaknya tata letak maupun penyusupan skrip (XSS).</p>
 *
 * <h3>Kompatibilitas</h3>
 * <p>Kode ditulis kompatibel dengan Java 1.7 (memakai kelas anonim, bukan lambda; tanpa
 * <i>diamond operator</i> maupun <i>try-with-resources</i>), sehingga aman dikompilasi pada
 * lingkungan build lama sekalipun.</p>
 *
 * @author AIS
 */
public final class AksiKrsMahasiswaHelper {

	/** Jumlah maksimum komentar yang ditarik &amp; ditampilkan per mahasiswa (jaga memori). */
	private static final int BATAS_KOMENTAR = 200;

	/** Kunci penanda agar blok gaya (CSS) hanya disuntikkan sekali per halaman. */
	private static final String KUNCI_GAYA = "aisKrsAksiGayaTerpasang";

	/** Kelas util murni statis &mdash; cegah instansiasi. */
	private AksiKrsMahasiswaHelper() {
	}

	/**
	 * Membangun sel "Aksi" berisi tombol <b>"Daftar KRS"</b> dan <b>"Komentar"</b> untuk satu
	 * baris mahasiswa, lalu mengembalikannya sebagai satu {@link Component} yang siap
	 * di-{@code setParent(row)} pada renderer grid.
	 *
	 * @param mahasiswa      mahasiswa pemilik baris (tidak boleh {@code null} untuk hasil bermakna).
	 * @param semesterPendek nilai penanda semester pendek yang efektif (boleh {@code null}); diteruskan
	 *                       apa adanya ke {@link TampilStudiMahasiswaHelper} agar perilaku sama persis.
	 * @param ekstrakurikuler penanda mode ekstrakurikuler (boleh {@code null}); diteruskan apa adanya.
	 * @param semesterKrs    semester KRS yang sedang ditinjau (boleh {@code null}); dipakai sebagai
	 *                       titik awal tampilan rincian KRS.
	 * @param edit           {@code true} bila pengguna berhak menyunting (menentukan mode tampil KRS).
	 * @param dataLoader     objek pemuat-data (biasanya {@code MonitorKRSMahasiswaAction} itu sendiri)
	 *                       yang dipakai kembali oleh tampilan rincian KRS; boleh {@code null}.
	 * @return komponen sel aksi; tidak pernah {@code null} (minimal berisi wadah kosong).
	 */
	public static Component render(final Mahasiswa mahasiswa, final Integer semesterPendek,
			final Integer ekstrakurikuler, final Integer semesterKrs, final boolean edit,
			final DataLoader dataLoader) {

		org.zkoss.zul.Div wadah = new org.zkoss.zul.Div();
		wadah.setSclass("ais-krs-aksi");
		pastikanGayaTerpasang(wadah);

		if (mahasiswa == null) {
			return wadah;
		}

		// Ringkasan KRS per semester (disetujui vs belum, termasuk SP) di atas tombol. Badge dapat
		// diklik = sama dengan menekan "Daftar KRS" (langsung ke tab KRS).
		org.zkoss.zul.Div badge = buatBadgeRingkasKrs(mahasiswa);
		if (badge != null) {
			badge.setParent(wadah);
			badge.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaDaftarKrs(mahasiswa, semesterPendek, ekstrakurikuler, semesterKrs, edit, dataLoader);
				}
			});
		}

		// Kolom aksi rapi (pola MahasiswaAction): badge ringkasan TETAP tampil di sel,
		// seluruh tombol aksi dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi
		// sehingga kolom Aksi menjadi kecil.
		final java.util.List<Component> aksiButtons = new java.util.ArrayList<Component>();

		// Aksi PERSETUJUAN cepat untuk dosen wali/admin: menyetujui SELURUH KRS mahasiswa ini pada
		// semester yang sedang ditinjau dalam satu klik (tanpa membuka rincian mata kuliah satu per
		// satu). Hanya tampil bila pengguna berhak menyunting (edit).
		if (edit) {
			final Button btnSetujui = new Button("Setujui KRS");
			btnSetujui.setSclass("ais-krs-btn ais-krs-btn--setujui");
			btnSetujui.setTooltiptext("Setujui seluruh mata kuliah KRS mahasiswa ini pada semester yang ditinjau");
			aksiButtons.add(btnSetujui);
			btnSetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfirmasiUbahPersetujuanKrs(mahasiswa, semesterKrs, dataLoader, true);
				}
			});

			final Button btnBatal = new Button("Batalkan");
			btnBatal.setSclass("ais-krs-btn ais-krs-btn--batal");
			btnBatal.setTooltiptext("Batalkan persetujuan seluruh KRS mahasiswa ini pada semester yang ditinjau");
			aksiButtons.add(btnBatal);
			btnBatal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfirmasiUbahPersetujuanKrs(mahasiswa, semesterKrs, dataLoader, false);
				}
			});
		}

		final Button btnDasbor = new Button("Dasbor");
		btnDasbor.setSclass("ais-krs-btn ais-krs-btn--dasbor");
		btnDasbor.setTooltiptext("Lihat rangkuman/dasbor akademik & KRS mahasiswa ini");
		aksiButtons.add(btnDasbor);
		btnDasbor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaDasbor(mahasiswa, semesterPendek, ekstrakurikuler, semesterKrs, edit, dataLoader);
			}
		});

		final Button btnKrs = new Button("Daftar KRS");
		btnKrs.setSclass("ais-krs-btn ais-krs-btn--krs");
		btnKrs.setTooltiptext("Lihat rincian Kartu Rencana Studi mahasiswa ini");
		aksiButtons.add(btnKrs);
		btnKrs.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaDaftarKrs(mahasiswa, semesterPendek, ekstrakurikuler, semesterKrs, edit, dataLoader);
			}
		});

		final Button btnKomentar = new Button("Komentar");
		btnKomentar.setSclass("ais-krs-btn ais-krs-btn--komentar");
		btnKomentar.setTooltiptext("Lihat catatan/komentar untuk mahasiswa ini");
		aksiButtons.add(btnKomentar);
		btnKomentar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaKomentar(mahasiswa);
			}
		});

		ais.ui.util.UIHelper.buatBarisAksi(wadah, 3, aksiButtons);

		return wadah;
	}

	/**
	 * Membuka jendela rincian Kartu Rencana Studi dengan <b>memakai ulang</b>
	 * {@link TampilStudiMahasiswaHelper#tampil(Mahasiswa, DataLoader, Boolean, Integer)} &mdash;
	 * sehingga tampilan &amp; logikanya identik dengan yang sudah dipakai di tempat lain pada layar
	 * ini. Dibungkus try/catch defensif agar kegagalan satu mahasiswa tidak menggagalkan seluruh
	 * halaman.
	 */
	private static void bukaDaftarKrs(Mahasiswa mahasiswa, Integer semesterPendek, Integer ekstrakurikuler,
			Integer semesterKrs, boolean edit, DataLoader dataLoader) {
		try {
			TampilStudiMahasiswaHelper helper = new TampilStudiMahasiswaHelper(semesterPendek, ekstrakurikuler,
					Boolean.TRUE, Boolean.valueOf(edit));
			// langsungKeKrs=true → jendela terbuka LANGSUNG pada tab KRS (Rencana Studi Mahasiswa),
			// bukan tab Dasbor, sesuai permintaan tombol "Daftar KRS".
			helper.tampil(mahasiswa, dataLoader, Boolean.TRUE, semesterKrs, true);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembukaan jendela Daftar KRS mahasiswa",
					e,
					new String[] {
							"Periksa kembali apakah data mahasiswa dan semester KRS yang dipilih masih valid.",
							"Muat ulang (refresh) halaman ini, lalu coba buka kembali Daftar KRS mahasiswa tersebut.",
							"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/**
	 * Membuka jendela studi mahasiswa pada tab <b>Dasbor</b> (rangkuman akademik &amp; KRS) &mdash;
	 * memudahkan dosen/pengguna melihat ringkasan mahasiswa langsung tanpa menelusuri rincian.
	 * Memakai ulang {@link TampilStudiMahasiswaHelper#tampil(Mahasiswa, DataLoader, Boolean, Integer, boolean)}
	 * dengan {@code langsungKeKrs = false} sehingga tab awal tetap Dasbor (perilaku standar jendela).
	 */
	private static void bukaDasbor(Mahasiswa mahasiswa, Integer semesterPendek, Integer ekstrakurikuler,
			Integer semesterKrs, boolean edit, DataLoader dataLoader) {
		try {
			TampilStudiMahasiswaHelper helper = new TampilStudiMahasiswaHelper(semesterPendek, ekstrakurikuler,
					Boolean.TRUE, Boolean.valueOf(edit));
			helper.tampil(mahasiswa, dataLoader, Boolean.TRUE, semesterKrs, false);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembukaan jendela Dasbor studi mahasiswa",
					e,
					new String[] {
							"Periksa kembali apakah data mahasiswa dan semester KRS yang dipilih masih valid.",
							"Muat ulang (refresh) halaman ini, lalu coba buka kembali Dasbor mahasiswa tersebut.",
							"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/**
	 * Menghitung ringkasan KRS mahasiswa PER SEMESTER: berapa yang sudah <b>disetujui</b> dan berapa
	 * yang <b>belum</b>, dipisah semester reguler vs Semester Pendek (SP). Satu "KRS semester"
	 * dianggap DISETUJUI bila SELURUH matakuliahnya berstatus disetujui
	 * ({@code detailperkuliahan.persetujuan = Detailperkuliahan.DISETUJUI}); bila ada minimal satu
	 * yang belum, semester itu dihitung "belum disetujui". SP dikenali dari
	 * {@code perkuliahan.statusSemesterPendek == Perkuliahan.SEMESTER_PENDEK} (matakuliah konversi
	 * tanpa perkuliahan diperlakukan reguler).
	 *
	 * <p>Memakai {@link HibernateUtil#currentSession()} (konteks ZK &mdash; TIDAK ditutup) dengan
	 * proyeksi ringan (hanya semester, persetujuan, status SP) sehingga hemat memori; satu query per
	 * mahasiswa. Aman-error: bila gagal, mengembalikan nol semua agar render baris tetap jalan.
	 *
	 * @return array {@code [disetujuiReguler, belumReguler, disetujuiSP, belumSP]}.
	 */
	@SuppressWarnings("unchecked")
	private static int[] hitungRingkasKrs(Mahasiswa mahasiswa) {
		int[] hasil = new int[4];
		if (mahasiswa == null) {
			return hasil;
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<Object[]> baris = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.createAlias("perkuliahan", "pk", Criteria.LEFT_JOIN)
					.setProjection(Projections.projectionList()
							.add(Projections.property("semester"))
							.add(Projections.property("persetujuan"))
							.add(Projections.property("pk.statusSemesterPendek")))
					.list();

			// Kelompokkan per (semester, isSP). boolean[]{ada, semuaDisetujui}.
			java.util.Map<String, boolean[]> grup = new java.util.HashMap<String, boolean[]>();
			for (int i = 0; i < baris.size(); i++) {
				Object[] r = baris.get(i);
				Integer per = (Integer) r[1];
				Integer sp = (Integer) r[2];
				boolean isSp = Perkuliahan.SEMESTER_PENDEK.equals(sp);
				String key = String.valueOf(r[0]) + "|" + (isSp ? "S" : "R");
				boolean[] v = grup.get(key);
				if (v == null) {
					v = new boolean[] { true, true };
					grup.put(key, v);
				}
				if (per == null || !Detailperkuliahan.DISETUJUI.equals(per)) {
					v[1] = false;
				}
			}
			for (java.util.Iterator<java.util.Map.Entry<String, boolean[]>> it = grup.entrySet().iterator(); it
					.hasNext();) {
				java.util.Map.Entry<String, boolean[]> e = it.next();
				boolean isSp = e.getKey().endsWith("S");
				boolean disetujui = e.getValue()[1];
				if (isSp) {
					if (disetujui) {
						hasil[2]++;
					} else {
						hasil[3]++;
					}
				} else {
					if (disetujui) {
						hasil[0]++;
					} else {
						hasil[1]++;
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AksiKrsMahasiswaHelper.java:284");
			// abaikan: ringkasan bersifat informatif, tak boleh menggagalkan render baris.
		}
		return hasil;
	}

	/**
	 * Membangun badge ringkasan KRS berupa chip "disetujui", "belum", dan (bila ada) "SP", yang bisa
	 * diklik untuk membuka Daftar KRS. Mengembalikan {@code null} bila mahasiswa belum memiliki KRS
	 * sama sekali (agar sel tidak dipenuhi badge kosong).
	 */
	private static org.zkoss.zul.Div buatBadgeRingkasKrs(Mahasiswa mahasiswa) {
		int[] c = hitungRingkasKrs(mahasiswa);
		int disetujui = c[0] + c[2];
		int belum = c[1] + c[3];
		int totalSp = c[2] + c[3];
		if (disetujui + belum == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder(160);
		sb.append("<div class='ais-krs-badge' title='Klik untuk membuka Daftar KRS'>");
		sb.append("<span class='ais-krs-chip ais-krs-chip--ok'>✔ ").append(disetujui).append(" disetujui</span>");
		sb.append("<span class='ais-krs-chip ais-krs-chip--wait'>● ").append(belum).append(" belum</span>");
		if (totalSp > 0) {
			sb.append("<span class='ais-krs-chip ais-krs-chip--sp'>SP ").append(c[2]).append("✔/").append(c[3])
					.append("●</span>");
		}
		sb.append("</div>");

		org.zkoss.zul.Div box = new org.zkoss.zul.Div();
		box.setStyle("cursor:pointer;");
		new MyHtml(sb.toString()).setParent(box);
		return box;
	}

	/**
	 * Menampilkan konfirmasi lalu (bila disetujui pengguna) menyetujui atau membatalkan persetujuan
	 * <b>seluruh mata kuliah KRS</b> seorang mahasiswa pada semester yang sedang ditinjau &mdash;
	 * memberi dosen wali/admin cara cepat menyetujui KRS tanpa membuka rincian satu per satu.
	 *
	 * @param mahasiswa    mahasiswa pemilik KRS.
	 * @param semesterKrs  semester yang ditinjau; bila {@code null}, mencakup seluruh semester KRS.
	 * @param dataLoader   pemuat data untuk menyegarkan baris setelah perubahan (boleh {@code null}).
	 * @param setujui      {@code true} = setujui semua; {@code false} = batalkan persetujuan.
	 */
	private static void konfirmasiUbahPersetujuanKrs(final Mahasiswa mahasiswa, final Integer semesterKrs,
			final DataLoader dataLoader, final boolean setujui) {
		try {
			String aksi = setujui ? "MENYETUJUI" : "MEMBATALKAN persetujuan";
			String pesan = "Apakah Anda yakin ingin " + aksi + " SELURUH mata kuliah KRS mahasiswa \""
					+ (mahasiswa.getNim() == null ? "" : mahasiswa.getNim()) + " - "
					+ (mahasiswa.getNama() == null ? "" : mahasiswa.getNama()) + "\""
					+ (semesterKrs == null ? " (semua semester)" : " pada semester " + semesterKrs) + "?"
					+ (setujui ? ""
							: "\n\nCatatan: mata kuliah yang sudah memiliki nilai tidak akan dibatalkan persetujuannya.");
			ais.ui.util.MyMessageboxConfig.show(pesan, "Konfirmasi Persetujuan KRS",
					ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
					ais.ui.util.MyMessageboxConfig.QUESTION, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							if (Integer.parseInt(ev.getData().toString()) != ais.ui.util.MyMessageboxConfig.OK) {
								return;
							}
							int[] hasil = ubahPersetujuanKrs(mahasiswa, semesterKrs, setujui);
							int diubah = hasil[0];
							int dilewati = hasil[1];
							if (dataLoader != null) {
								try {
									dataLoader.loadData(null);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
							StringBuilder info = new StringBuilder();
							info.append(setujui ? diubah + " mata kuliah berhasil disetujui."
									: diubah + " mata kuliah berhasil dibatalkan persetujuannya.");
							if (dilewati > 0) {
								info.append("\n").append(dilewati)
										.append(" mata kuliah dilewati karena sudah memiliki nilai.");
							}
							if (diubah == 0 && dilewati == 0) {
								info.setLength(0);
								info.append(setujui
										? "Tidak ada mata kuliah yang perlu disetujui (kemungkinan semua sudah disetujui atau KRS belum diisi)."
										: "Tidak ada persetujuan yang dibatalkan.");
							}
							ais.ui.util.MyMessageboxConfig.show(info.toString(), "Informasi",
									ais.ui.util.MyMessageboxConfig.OK,
									diubah > 0 ? ais.ui.util.MyMessageboxConfig.INFORMATION
											: ais.ui.util.MyMessageboxConfig.EXCLAMATION);
						}
					});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menerapkan perubahan persetujuan pada seluruh {@link Detailperkuliahan} milik mahasiswa untuk
	 * semester tertentu. Memakai {@link HibernateUtil#currentSession()} (konteks ZK &mdash; TIDAK
	 * ditutup manual). Saat MEMBATALKAN, mata kuliah yang sudah bernilai (&gt; 1.0) dilewati bila
	 * konfigurasi {@code batalkan_persetujuan_harus_memiliki_nilai_nol} aktif, agar nilai yang sudah
	 * masuk tidak terganggu.
	 *
	 * @return array {@code [jumlahDiubah, jumlahDilewati]}.
	 */
	@SuppressWarnings("unchecked")
	private static int[] ubahPersetujuanKrs(Mahasiswa mahasiswa, Integer semesterKrs, boolean setujui) {
		int diubah = 0;
		int dilewati = 0;
		try {
			Session session = HibernateUtil.currentSession();
			boolean jagaNilai = !setujui
					&& Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol");
			Integer targetBaru = setujui ? Detailperkuliahan.DISETUJUI : Detailperkuliahan.BELUM_DISETUJUI;

			List<Detailperkuliahan> daftar = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(semesterKrs == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("semester", semesterKrs))
					.list();

			for (int i = 0; i < daftar.size(); i++) {
				Detailperkuliahan dp = daftar.get(i);
				if (dp == null) {
					continue;
				}
				// Lewati yang statusnya sudah sesuai target (tidak ada perubahan).
				if (targetBaru.equals(dp.getPersetujuan())) {
					continue;
				}
				// Saat membatalkan: jangan ganggu mata kuliah yang sudah bernilai.
				if (jagaNilai && dp.getTotalNilai() != null && dp.getTotalNilai() > 1.0) {
					dilewati++;
					continue;
				}
				try {
					dp.setPersetujuan(targetBaru);
					Common.refreshUpdate(session, dp);
					diubah++;
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/master/helper/AksiKrsMahasiswaHelper.java:ubahPersetujuanKrs");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return new int[] { diubah, dilewati };
	}

	/**
	 * Membuka jendela berisi daftar komentar/catatan untuk seorang mahasiswa, ditampilkan sebagai
	 * kartu-kartu yang rapi (terbaru di atas). Memakai {@link HibernateUtil#currentSession()}
	 * (konteks ZK) sehingga sesi TIDAK ditutup manual &mdash; sudah dikelola kerangka kerja.
	 */
	private static void bukaKomentar(Mahasiswa mahasiswa) {
		try {
			final MyWindow window = new MyWindow();
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
			window.setTitle("Komentar — " + (mahasiswa.getNama() == null ? "" : mahasiswa.getNama())
					+ " (" + (mahasiswa.getNim() == null ? "" : mahasiswa.getNim()) + ")");
			window.setClosable(true);
			window.setBorder("normal");
			window.setWidth("96%");
			window.setMaximizable(true);
			window.setContentStyle("max-width:720px;margin:0 auto;");

			pastikanGayaTerpasang(window);

			// Sesi ZK: JANGAN ditutup (dikelola otomatis oleh kerangka kerja).
			Session session = HibernateUtil.currentSession();
			@SuppressWarnings("unchecked")
			List<Komentar> daftar = session.createCriteria(Komentar.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.isNull("tbmuser"))
					.addOrder(Order.desc("tanggal"))
					.setMaxResults(BATAS_KOMENTAR)
					.list();

			int jml = daftar == null ? 0 : daftar.size();
			StringBuilder html = new StringBuilder(768);
			html.append("<div class='ais-komentar-wrap'>");
			// Header ringkas: identitas mahasiswa + jumlah komentar.
			html.append("<div class='ais-komentar-head'>")
					.append("<div class='ais-komentar-ava'>💬</div>")
					.append("<div class='ais-komentar-head-txt'>")
					.append("<div class='ais-komentar-head-nama'>").append(amankanHtml(mahasiswa.getNama()))
					.append("</div>")
					.append("<div class='ais-komentar-head-sub'>NIM ").append(amankanHtml(mahasiswa.getNim()))
					.append(" &middot; ").append(jml).append(" komentar</div>")
					.append("</div></div>");
			if (jml == 0) {
				html.append("<div class='ais-komentar-kosong'>Belum ada komentar untuk mahasiswa ini.</div>");
			} else {
				for (int i = 0; i < daftar.size(); i++) {
					Komentar k = daftar.get(i);
					String isi = k == null ? "" : amankanHtml(k.getKomentar());
					String tgl = formatTanggal(k == null ? null : k.getTanggal());
					html.append("<div class='ais-komentar-card'>")
							.append("<div class='ais-komentar-teks'>").append(isi.length() == 0 ? "-" : isi)
							.append("</div>")
							.append("<div class='ais-komentar-meta'>🕒 ").append(tgl).append("</div>")
							.append("</div>");
				}
			}
			html.append("</div>");

			MyHtml konten = new MyHtml(html.toString());
			konten.setParent(window);

			window.setPosition("center");
			window.doHighlighted();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyuntikkan blok gaya (CSS modern) untuk panel aksi &amp; kartu komentar <b>satu kali saja
	 * per halaman</b>. Idempoten: pemanggilan berikutnya pada halaman yang sama tidak melakukan
	 * apa-apa. Pola ini hemat memori karena gaya dibagikan lewat nama kelas (sclass), bukan
	 * disalin ke atribut {@code style} tiap komponen di tiap baris.
	 *
	 * @param acuan komponen mana saja yang sudah terpasang pada halaman (dipakai untuk meraih Page).
	 */
	private static void pastikanGayaTerpasang(Component acuan) {
		try {
			if (acuan == null) {
				return;
			}
			Page page = acuan.getPage();
			if (page == null) {
				page = ExecutionsCtrl.getCurrentCtrl() == null ? null
						: ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
			}
			if (page == null || page.getAttribute(KUNCI_GAYA) != null) {
				return;
			}
			page.setAttribute(KUNCI_GAYA, Boolean.TRUE);

			MyHtml gaya = new MyHtml(GAYA_CSS);
			Component root = page.getFirstRoot();
			if (root != null) {
				gaya.setParent(root);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AksiKrsMahasiswaHelper.java:414");
			// Gaya bersifat kosmetik: kegagalan menyuntik CSS tidak boleh mengganggu render data.
		}
	}

	/** Format tanggal aman-null memakai format standar aplikasi. */
	private static String formatTanggal(Date tanggal) {
		if (tanggal == null) {
			return "-";
		}
		try {
			return Common.dateFormat3.get().format(tanggal);
		} catch (Exception e) {
			return "-";
		}
	}

	/**
	 * Meng-<i>escape</i> teks agar aman ditaruh di dalam HTML (mencegah tata letak rusak &amp;
	 * potensi penyusupan skrip). {@code null} dikembalikan sebagai string kosong.
	 */
	private static String amankanHtml(String teks) {
		if (teks == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(teks.length() + 16);
		for (int i = 0; i < teks.length(); i++) {
			char c = teks.charAt(i);
			switch (c) {
			case '&':
				sb.append("&amp;");
				break;
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '"':
				sb.append("&quot;");
				break;
			case '\'':
				sb.append("&#39;");
				break;
			case '\n':
				sb.append("<br/>");
				break;
			default:
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Blok CSS modern (self-contained, tanpa pustaka chart pihak ketiga) untuk panel aksi &amp;
	 * kartu komentar. Ditulis sekali sebagai konstanta agar tidak dibangun ulang tiap render.
	 */
	private static final String GAYA_CSS =
			"<style>"
			+ ".ais-krs-aksi{display:flex;flex-direction:column;gap:6px;align-items:stretch;min-width:104px;}"
			+ ".ais-krs-btn{border:0;border-radius:999px;padding:6px 12px;font-size:12px;font-weight:600;"
			+ "cursor:pointer;color:#fff;line-height:1.2;transition:filter .15s ease,transform .05s ease;"
			+ "white-space:nowrap;text-align:center;box-shadow:0 1px 2px rgba(0,0,0,.12);}"
			+ ".ais-krs-btn:before{margin-right:5px;}"
			+ ".ais-krs-btn--setujui{background:linear-gradient(135deg,#16a34a,#15803d);}"
			+ ".ais-krs-btn--setujui:before{content:'\\2714';}"
			+ ".ais-krs-btn--batal{background:linear-gradient(135deg,#64748b,#475569);font-weight:500;padding:4px 12px;font-size:11px;}"
			+ ".ais-krs-btn--batal:before{content:'\\21BA';}"
			+ ".ais-krs-btn--dasbor{background:linear-gradient(135deg,#7c3aed,#6d28d9);}"
			+ ".ais-krs-btn--dasbor:before{content:'\\1F4CA';}"
			+ ".ais-krs-btn--krs{background:linear-gradient(135deg,#2563eb,#1d4ed8);}"
			+ ".ais-krs-btn--krs:before{content:'\\1F4CB';}"
			+ ".ais-krs-btn--komentar{background:linear-gradient(135deg,#0d9488,#0f766e);}"
			+ ".ais-krs-btn--komentar:before{content:'\\1F4AC';}"
			+ ".ais-krs-btn:hover{filter:brightness(1.08);}"
			+ ".ais-krs-btn:active{transform:translateY(1px);}"
			+ ".ais-komentar-wrap{display:flex;flex-direction:column;gap:10px;padding:12px;}"
			+ ".ais-komentar-kosong{padding:24px;text-align:center;color:#64748b;font-style:italic;}"
			+ ".ais-komentar-card{background:#fff;border:1px solid #e2e8f0;border-left:4px solid #0d9488;"
			+ "border-radius:10px;padding:10px 12px;box-shadow:0 1px 3px rgba(0,0,0,.06);}"
			+ ".ais-komentar-teks{color:#1e293b;font-size:13.5px;line-height:1.5;word-break:break-word;}"
			+ ".ais-komentar-meta{margin-top:6px;color:#94a3b8;font-size:11.5px;}"
			// Badge ringkasan KRS (chip disetujui/belum/SP) di sel Aksi.
			+ ".ais-krs-badge{display:flex;flex-wrap:wrap;gap:4px;justify-content:center;margin-bottom:2px;}"
			+ ".ais-krs-chip{font-size:10.5px;font-weight:700;border-radius:999px;padding:2px 7px;line-height:1.5;"
			+ "white-space:nowrap;}"
			+ ".ais-krs-chip--ok{background:#dcfce7;color:#166534;border:1px solid #86efac;}"
			+ ".ais-krs-chip--wait{background:#fef3c7;color:#92400e;border:1px solid #fcd34d;}"
			+ ".ais-krs-chip--sp{background:#ede9fe;color:#5b21b6;border:1px solid #c4b5fd;}"
			// Header kartu komentar (avatar + nama + jumlah).
			+ ".ais-komentar-head{display:flex;align-items:center;gap:12px;padding:14px;border-bottom:1px solid #e2e8f0;"
			+ "background:linear-gradient(135deg,#0d9488,#0f766e);border-radius:12px 12px 0 0;margin:-12px -12px 4px -12px;}"
			+ ".ais-komentar-ava{flex:none;width:42px;height:42px;border-radius:50%;background:rgba(255,255,255,.2);"
			+ "display:flex;align-items:center;justify-content:center;font-size:20px;}"
			+ ".ais-komentar-head-nama{color:#fff;font-weight:800;font-size:15px;line-height:1.2;}"
			+ ".ais-komentar-head-sub{color:#d1fae5;font-size:12px;margin-top:2px;}"
			+ "@media(max-width:640px){.ais-krs-aksi{min-width:0;}.ais-krs-btn{padding:8px 10px;font-size:13px;}}"
			+ "</style>";
}
