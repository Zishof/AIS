package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokParameterTambahanPertemuan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPertemuan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>RekapHasilMahasiswa &mdash; Rekap Hasil/Nilai Parameter Tambahan Mahasiswa Lintas Jenis Pertemuan</h1>
 *
 * <p>
 * Dasbor ini merekap <b>hasil isian "Parameter Tambahan"</b> (kolom nilai/keterangan bebas yang bisa
 * dikonfigurasi per kelompok, mis. nilai ujian, catatan penilaian, hasil wawancara, dsb.) yang sudah
 * diisi dosen/petugas pada satu atau beberapa {@link Pertemuan} (pertemuan/sesi kegiatan) sekaligus,
 * lalu menyusunnya menjadi satu berkas Excel siap unduh ("Hasil Ujian.xlsx"). Jenis pertemuan yang
 * didukung bermacam-macam &mdash; Perkuliahan, Ujian PMB, Tugas Akhir, KKN, PKL, Skripsi, KRS
 * Mahasiswa, Ujian PSB, Jadwal Pelajaran, Grup Pertemuan, hingga Formulir Kegiatan &mdash; sehingga
 * dasbor ini dipakai lintas modul akademik mana pun yang memanfaatkan mekanisme Parameter Tambahan.
 * Daftar {@link Pertemuan} yang direkap disuplai oleh pemanggil lewat konstruktor
 * {@link #RekapHasilMahasiswa(List)}; jendela ini sendiri tidak menampilkan filter tambahan, hanya
 * tombol <b>Tutup</b> dan <b>Download Data</b> pada toolbar.
 * </p>
 *
 * <h2>Alur teknis singkat</h2>
 * <p>
 * {@link #init()} membangun tata letak dasar (deskripsi singkat + toolbar pada region {@code North},
 * area hasil pada region {@code Center}) lalu langsung memanggil {@link #initSpreadsheet()}. Karena
 * jumlah pertemuan dan mahasiswa yang direkap bisa besar, penyusunan Excel dijalankan pada
 * {@link Thread} latar belakang terpisah agar antarmuka ZK tidak macet;
 * {@code Common.displayLoadBar(...)} menampilkan indikator "sedang memproses" yang di-<i>polling</i>
 * lewat {@link Label} tersembunyi, dan begitu proses selesai berkas Excel ditampilkan sebagai
 * tabel/grid ringan (lihat {@code ais.ui.util.PratinjauXlsxHelper#gantiSpreadsheetDenganGrid}, yang
 * dipanggil otomatis di dalam {@code Common.displayLoadBar}) alih-alih widget Excel penuh yang berat
 * di sisi peramban.
 * </p>
 *
 * <h2>Panel ringkasan visual (HTML/CSS, bukan JFreeChart)</h2>
 * <p>
 * Karena nilai "hasil" pada Parameter Tambahan berupa teks bebas (bukan angka baku seperti IPK), panel
 * ringkasan di sini berfokus pada <b>volume &amp; kelengkapan data</b> alih-alih rata-rata nilai:
 * kartu KPI (jumlah pertemuan diproses, jumlah kelompok parameter, jumlah baris mahasiswa, persentase
 * sel terisi), grafik donat komposisi jenis pertemuan yang direkap, grafik batang pertemuan dengan data
 * terbanyak, dan grafik donat kelengkapan pengisian (terisi vs kosong). Seluruh angka dihitung
 * <b>bersamaan</b> dengan penulisan sel Excel di dalam {@link #initSpreadsheet()} (loop yang sama,
 * tanpa kueri basis data tambahan) lalu dirender lewat {@code ais.ui.util.HtmlChartHelper} setelah
 * loop selesai, dibungkus percobaan/tangkapan tersendiri supaya kegagalan menggambar grafik tidak
 * pernah mengganggu penulisan Excel maupun tampilan grid.
 * </p>
 *
 * <h2>Ketahanan terhadap data tidak lengkap</h2>
 * <p>
 * Setiap {@link Pertemuan} diproses dalam blok percobaan tersendiri (satu pertemuan bermasalah tidak
 * menggagalkan pertemuan lain), dan di dalamnya setiap baris mahasiswa juga diproses dalam blok
 * percobaan tersendiri (satu mahasiswa bermasalah &mdash; mis. program studi belum diisi &mdash; tidak
 * menggagalkan mahasiswa lain pada kelompok yang sama, dan indeks baris tetap dimajukan lewat blok
 * {@code finally} agar baris berikutnya tidak menimpa baris yang gagal). Kesalahan dicatat lewat
 * {@code ais.common.ErrorAuditUtil#record}. {@link Thread} latar belakang juga dibungkus percobaan
 * paling luar: kegagalan proses apa pun tetap mengosongkan label pemuatan (agar indikator "sedang
 * memproses" tidak menggantung selamanya) dan dilaporkan lewat {@code Common#tampilErrorJikaAdmin}.
 * </p>
 *
 * <h2>Sesi Hibernate</h2>
 * <p>
 * Thread latar belakang mengambil sesi lewat {@code HibernateUtil.currentNativeSession()} (sesi
 * native, BUKAN sesi ber-scope permintaan/{@code currentSession()}), sehingga wajib ditutup secara
 * eksplisit; penutupan dilakukan tepat satu kali di blok {@code finally} agar sesi selalu tertutup
 * baik proses berhasil maupun gagal, dan tidak dipanggil berulang di banyak tempat.
 * </p>
 *
 * @author e-Campus UI Team
 */
public class RekapHasilMahasiswa extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private File file;

	private boolean simple;

	private Tbmuser tbmuser;

	private List<Pertemuan> pertemuans;

	public RekapHasilMahasiswa(List<Pertemuan> pertemuans) {
		super();
		this.pertemuans = pertemuans;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun tata letak dasar dasbor ini: deskripsi singkat dan toolbar pada region {@code North}
	 * berisi tombol Tutup (bila bukan mode {@code simple}) dan Download Data, serta area hasil pada
	 * region {@code Center}. Karena region {@code North} pada ZK Borderlayout hanya boleh memiliki
	 * SATU anak langsung, deskripsi dan toolbar dibungkus bersama dalam satu {@link Vlayout}. Method
	 * ini langsung memanggil {@link #initSpreadsheet()} di akhir karena dasbor ini tidak memiliki
	 * filter interaktif tambahan bagi pengguna &mdash; ia hanya menampilkan rekap untuk daftar
	 * {@link Pertemuan} yang sudah ditentukan pemanggil lewat konstruktor
	 * {@link #RekapHasilMahasiswa(List)}.
	 */
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		tbmuser = Common.getCurrentUser();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North south = new North();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		// North hanya boleh 1 anak langsung -> bungkus deskripsi + toolbar dlm 1 Vlayout.
		Vlayout northBox = new Vlayout();
		northBox.setWidth("100%");
		northBox.setParent(south);

		Html deskripsiDasbor = new Html();
		deskripsiDasbor.setContent("<div style='font-size:13px;color:#475569;padding:2px 4px 8px;'>"
				+ "Menampilkan rekap hasil (nilai/keterangan Parameter Tambahan) mahasiswa dari satu atau "
				+ "beberapa pertemuan &mdash; perkuliahan, ujian, tugas akhir, KKN, PKL, skripsi, dan "
				+ "sejenisnya &mdash; dalam satu berkas, lengkap dengan ringkasan jenis pertemuan dan "
				+ "kelengkapan pengisian datanya.</div>");
		deskripsiDasbor.setParent(northBox);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(northBox);

		if (!simple) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					detach();
				}
			});
			cancel.setParent(toolbar);
		}

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Hasil Ujian.xlsx");
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java:111");
				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	/**
	 * Method inti dasbor ini: dipanggil sekali dari {@link #init()}. Menjalankan penyusunan rekap
	 * hasil Parameter Tambahan pada {@link Thread} latar belakang terpisah (agar antarmuka tidak
	 * macet untuk data yang banyak), menuliskannya sebagai berkas Excel sekaligus mengumpulkan
	 * ringkasan angka (jumlah pertemuan diproses, jumlah kelompok parameter, jumlah baris mahasiswa,
	 * komposisi jenis pertemuan, kelengkapan pengisian sel) untuk panel grafik HTML/CSS yang dirender
	 * lewat {@link #renderChartRingkasan}. Region {@code Center} dibungkus satu {@link Vlayout} berisi
	 * dua anak: panel grafik ({@code chartPanel}) dan wadah grid/Excel ({@code gridHost}) yang
	 * terpisah, sehingga saat {@code Common.displayLoadBar(...)} membersihkan dan mengisi ulang area
	 * hasil di akhir proses, panel grafik yang sudah tampil tidak ikut terhapus. Setiap
	 * {@link Pertemuan} diproses dalam blok percobaan tersendiri, dan di dalamnya setiap mahasiswa
	 * juga diproses dalam blok percobaan tersendiri: data yang bermasalah dicatat lalu dilewati, tidak
	 * menggagalkan seluruh rekap. Seluruh isi {@link Thread} juga dibungkus percobaan paling luar agar
	 * kegagalan apa pun tetap mengosongkan label pemuatan. Sesi Hibernate native ditutup satu kali di
	 * blok {@code finally}.
	 */
	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		// Center hanya boleh 1 anak -> bungkus dlm Vlayout: panel grafik ringkasan di atas, wadah
		// grid/Excel di bawah lewat Div terpisah (gridHost) supaya saat Common.displayLoadBar(...)
		// membersihkan & mengisi ulang area hasil, panel grafik yang sudah tampil tidak ikut terhapus.
		final Vlayout centerBox = new Vlayout();
		centerBox.setWidth("100%");
		centerBox.setHeight("100%");
		centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);

		final Html chartPanel = new Html();
		chartPanel.setParent(centerBox);

		/* UBAH 21-08-2026: tabel rincian sebelumnya hanya sebuah Div polos tanpa penanda apa pun,
		 * sehingga setelah panel grafik yang tinggi (kartu KPI + donut) ia terdorong ke bawah lipatan
		 * dan terlihat seolah hilang. Kini diberi judul bagian dan tinggi minimum supaya jelas bahwa
		 * rincian datanya ada di bawah grafik. Isinya tetap Grid ZK biasa -- widget Excel dirender
		 * lalu diganti Grid oleh PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(). */
		final org.zkoss.zul.Html judulRincian = new org.zkoss.zul.Html(
				"<div style='margin:14px 2px 6px;font-size:14px;font-weight:700;color:#0f172a;'>"
				+ "Rincian Data</div>"
				+ "<div style='margin:0 2px 8px;font-size:12px;color:#64748b;'>"
				+ "Tabel lengkap di bawah ini memuat data yang meringkas grafik di atas.</div>");
		judulRincian.setParent(centerBox);

		final Div gridHost = new Div();
		gridHost.setStyle("width:100%;min-height:220px;");
		gridHost.setParent(centerBox);

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, gridHost, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					XSSFWorkbook workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet("DATA");
					sheet.setDefaultColumnWidth(20);
					int rowIndex = 0;

					Session session = HibernateUtil.currentNativeSession();

					// -- Akumulasi ringkasan utk panel grafik (dihitung BERSAMAAN dgn penulisan sel
					// Excel di bawah, tanpa kueri tambahan) -> dirender lewat renderChartRingkasan
					// setelah loop selesai. --
					int jumlahPertemuanDiproses = 0;
					int jumlahKelompokParameter = 0;
					int totalBarisMahasiswa = 0;
					int cellsTerisi = 0;
					int cellsKosong = 0;
					final Map<String, int[]> jumlahPerJenisPertemuan = new LinkedHashMap<String, int[]>();
					final Map<String, int[]> jumlahPerTopik = new LinkedHashMap<String, int[]>();

					for (Pertemuan pertemuan : pertemuans) {
						try {
							if (pertemuan.getAktif()) {
								Criterion criterion = Restrictions.sqlRestriction("false");
								String jenisPertemuan = "Lainnya";

								if (pertemuan.getPerkuliahan() != null) {
									criterion = Restrictions.eq("perkuliahan", true);
									jenisPertemuan = "Perkuliahan";
								} else if (pertemuan.getJadwalUjianPMB() != null) {
									criterion = Restrictions.eq("jadwalUjianPMB", true);
									jenisPertemuan = "Ujian PMB";
								} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
									criterion = Restrictions.eq("mahasiswaRequestTugasAkhir", true);
									jenisPertemuan = "Tugas Akhir";
								} else if (pertemuan.getKelompokKkn() != null) {
									criterion = Restrictions.eq("kelompokKkn", true);
									jenisPertemuan = "KKN";
								} else if (pertemuan.getKelompokPkl() != null) {
									criterion = Restrictions.eq("kelompokPkl", true);
									jenisPertemuan = "PKL";
								} else if (pertemuan.getSkripsi() != null) {
									criterion = Restrictions.eq("skripsi", true);
									jenisPertemuan = "Skripsi";
								} else if (pertemuan.getKrsMahasiswa() != null) {
									criterion = Restrictions.eq("krsMahasiswa", true);
									jenisPertemuan = "KRS Mahasiswa";
								} else if (pertemuan.getJadwalUjianPSB() != null) {
									criterion = Restrictions.eq("jadwalUjianPSB", true);
									jenisPertemuan = "Ujian PSB";
								} else if (pertemuan.getJadwalPelajaran() != null) {
									criterion = Restrictions.eq("jadwalPelajaran", true);
									jenisPertemuan = "Jadwal Pelajaran";
								} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
									criterion = Restrictions.eq("pertemuanPunyaGrupPertemuan", true);
									jenisPertemuan = "Grup Pertemuan";
								} else if (pertemuan.getFormulirKegiatan() != null) {
									criterion = Restrictions.eq("formulirKegiatan", true);
									jenisPertemuan = "Formulir Kegiatan";
								}

								List<Mahasiswa> mahasiswas = tbmuser != null && tbmuser.getMahasiswa() != null
										? new ArrayList<Mahasiswa>()
										: pertemuan.ambilMahasiswa();

								if (tbmuser != null && tbmuser.getMahasiswa() != null) {
									mahasiswas.add(tbmuser.getMahasiswa());
								}

								List<KelompokParameterTambahanPertemuan> kelompokParameterTambahanPertemuans = session
										.createCriteria(ParameterTambahanPertemuan.class).add(criterion)
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanPertemuan", "kelompokParameterTambahanPertemuan")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanPertemuan.aktif", true))
										.setProjection(Projections.groupProperty("kelompokParameterTambahanPertemuan")).list();
								if (kelompokParameterTambahanPertemuans.isEmpty()) {
									continue;
								}

								jumlahPertemuanDiproses++;

								String topikLabel = (pertemuan.getTanggal() == null ? "-"
										: Common.dateFormat2.get().format(pertemuan.getTanggal())) + ", "
										+ pertemuan.getTopik().toUpperCase();

								sheet.createRow(rowIndex).createCell(0).setCellValue(topikLabel);

								for (KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan : kelompokParameterTambahanPertemuans) {
									rowIndex++;
									sheet.createRow(rowIndex).createCell(0)
											.setCellValue(kelompokParameterTambahanPertemuan.getNama());
									jumlahKelompokParameter++;

									List<ParameterTambahan> parameterTambahans = session
											.createCriteria(ParameterTambahanPertemuan.class).add(criterion)
											.add(Restrictions.eq("kelompokParameterTambahanPertemuan",
													kelompokParameterTambahanPertemuan))
											.createAlias("parameterTambahan", "parameterTambahan")
											.createAlias("kelompokParameterTambahanPertemuan",
													"kelompokParameterTambahanPertemuan")
											.add(Restrictions.eq("parameterTambahan.aktif", true))
											.add(Restrictions.eq("kelompokParameterTambahanPertemuan.aktif", true))
											.setProjection(Projections.groupProperty("parameterTambahan")).list();
									Collections.sort(parameterTambahans);

									rowIndex++;
									XSSFRow rowhead = sheet.createRow(rowIndex);

									rowhead.createCell(0).setCellValue("No.");
									rowhead.createCell(1).setCellValue("NIM/No. Reg");
									rowhead.createCell(2).setCellValue("Nama");
									rowhead.createCell(3).setCellValue("Prodi");

									rowhead.createCell(4).setCellValue("Kelas");

									rowIndex++;

									int index = 5;
									for (ParameterTambahan parameterTambahan : parameterTambahans) {
										rowhead.createCell(index).setCellValue(parameterTambahan.getNama());
										index++;
									}

									int no = 0;
									for (Mahasiswa mahasiswa : mahasiswas) {

										try {
											label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
													+ Common.numberFormat.get().format(no * 100.0 / mahasiswas.size()) + " %)");

											XSSFRow row = sheet.createRow(rowIndex);
											row.createCell(0).setCellValue(++no);
											row.createCell(1).setCellValue(mahasiswa.getNim());
											row.createCell(2).setCellValue(mahasiswa.getNama());
											String namaJurusan = mahasiswa.getJurusan() == null ? ""
													: mahasiswa.getJurusan().getNama();
											row.createCell(3).setCellValue(namaJurusan);
											KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
											row.createCell(4).setCellValue(krsMahasiswa.getKelas());

											index = 5;
											for (ParameterTambahan parameterTambahan : parameterTambahans) {

												String jenis = kelompokParameterTambahanPertemuan.getId() + "->"
														+ parameterTambahan.getId();
												String valAsli = "";
												String ketAsli = "";
												String val = "";
												String ket = "";
												String[] spl = pertemuan.getParameterTambahanInds().split("\n");
												for (String d : spl) {
													String[] value = d.split("<=>");
													if (value[0].trim().equalsIgnoreCase(jenis)) {
														valAsli = value.length > 1 ? value[1].trim() : "";

														try {
															ketAsli = value.length > 0 ? value[value.length - 1] : "";
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java:267");

														}

														try {
															JSONObject jsonObject = new JSONObject(valAsli);
															val = jsonObject.getString(mahasiswa.getId().toString());
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java:274");

														}

														try {
															JSONObject jsonObject = new JSONObject(ketAsli);
															ket = jsonObject.getString(mahasiswa.getId().toString());
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java:281");

														}
													}
												}

												System.out.println("val " + val);
												String isiSel = val + (ket.isEmpty() ? "" : ". " + ket);
												if (isiSel.trim().isEmpty()) {
													cellsKosong++;
												} else {
													cellsTerisi++;
												}
												row.createCell(index).setCellValue(isiSel);
												index++;
											}

											totalBarisMahasiswa++;

											int[] cJenis = jumlahPerJenisPertemuan.get(jenisPertemuan);
											if (cJenis == null) {
												cJenis = new int[1];
												jumlahPerJenisPertemuan.put(jenisPertemuan, cJenis);
											}
											cJenis[0]++;

											int[] cTopik = jumlahPerTopik.get(topikLabel);
											if (cTopik == null) {
												cTopik = new int[1];
												jumlahPerTopik.put(topikLabel, cTopik);
											}
											cTopik[0]++;

										} catch (Exception exBaris) {
											// Satu data mahasiswa bermasalah (mis. prodi belum lengkap, isian
											// parameter rusak) tidak boleh menggagalkan seluruh proses rekap
											// kelompok ini; catat & lanjut ke mahasiswa berikutnya.
											ais.common.ErrorAuditUtil.record(exBaris,
													"auto-guard(per-mahasiswa) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java");
										} finally {
											rowIndex++;
										}
									}

								}
							}
						} catch (Exception exPertemuan) {
							// Satu pertemuan bermasalah (mis. relasi tugas/ujian/kelompok belum lengkap)
							// tidak boleh menggagalkan seluruh rekap; catat & lanjut ke pertemuan berikutnya.
							ais.common.ErrorAuditUtil.record(exPertemuan,
									"auto-guard(per-pertemuan) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java");
						}
					}

					Common.setStyled(sheet);
					sizedata.setValue(rowIndex + 1);

					try {
						FileOutputStream fileOut = new FileOutputStream(filename);
						workbook.write(fileOut);
						fileOut.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}

					System.out.println("Your excel file has been generated! ");

					// -- Render panel grafik (HTML/CSS, BUKAN JFreeChart) dari ringkasan yang sudah
					// dihitung di loop di atas. Kegagalan menggambar grafik tidak boleh mengganggu
					// Excel/grid yang sudah berhasil ditulis. --
					try {
						renderChartRingkasan(chartPanel, jumlahPertemuanDiproses, jumlahKelompokParameter,
								totalBarisMahasiswa, cellsTerisi, cellsKosong, jumlahPerJenisPertemuan, jumlahPerTopik);
					} catch (Exception exChart) { ais.common.ErrorAuditUtil.record(exChart,
							"auto-guard(chart) src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java"); }

					label.setValue("");

				} catch (Exception exProses) {
					// Kegagalan proses (mis. query bermasalah) tidak boleh membuat indikator "sedang
					// memproses" menggantung selamanya di sisi pengguna.
					ais.common.ErrorAuditUtil.record(exProses,
							"src/ais/action/master/dashboard/admin/RekapHasilMahasiswa.java:initSpreadsheet-thread");
					Common.tampilErrorJikaAdmin(exProses);
					label.setValue("");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	/**
	 * Mengurutkan peta akumulasi (label &rarr; jumlah) secara menurun berdasarkan jumlah, lalu
	 * mengambil paling banyak {@code topN} entri teratas. Dipakai supaya grafik batang "pertemuan
	 * dengan data terbanyak" tidak menjadi terlalu panjang saat jumlah pertemuan yang direkap sangat
	 * banyak.
	 *
	 * @param sumber   peta sumber label &rarr; {jumlah} (elemen tunggal per entri, mengikuti pola
	 *                 akumulasi di {@link #initSpreadsheet()}).
	 * @param labelOut daftar keluaran label terurut (diisi oleh method ini).
	 * @param nilaiOut daftar keluaran nilai terurut sejajar dengan {@code labelOut} (diisi oleh method
	 *                 ini).
	 * @param topN     jumlah maksimum entri teratas yang diambil.
	 */
	private static void urutkanTopN(Map<String, int[]> sumber, List<String> labelOut, List<Double> nilaiOut,
			int topN) {
		List<Map.Entry<String, int[]>> entries = new ArrayList<Map.Entry<String, int[]>>(sumber.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, int[]>>() {
			@Override
			public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) {
				return b.getValue()[0] - a.getValue()[0];
			}
		});
		int n = Math.min(topN, entries.size());
		for (int i = 0; i < n; i++) {
			labelOut.add(entries.get(i).getKey());
			nilaiOut.add((double) entries.get(i).getValue()[0]);
		}
	}

	/**
	 * Merender panel ringkasan visual dasbor ini sepenuhnya memakai {@code ais.ui.util.HtmlChartHelper}
	 * (HTML/CSS, bukan JFreeChart): kartu KPI (jumlah pertemuan diproses, jumlah kelompok parameter,
	 * jumlah baris mahasiswa, persentase sel hasil yang terisi), grafik donat komposisi jenis
	 * pertemuan yang direkap (Perkuliahan, Ujian PMB, Tugas Akhir, dsb.), grafik batang pertemuan
	 * dengan data terbanyak (maksimal 10 teratas, lihat {@link #urutkanTopN}), serta grafik donat
	 * kelengkapan pengisian hasil (sel terisi vs kosong). Seluruh parameter berasal dari akumulasi
	 * yang sudah dihitung selagi menulis sel Excel di {@link #initSpreadsheet()}, sehingga tidak ada
	 * kueri basis data tambahan yang dijalankan hanya untuk kebutuhan grafik.
	 *
	 * @param chartPanel                komponen tampilan tujuan.
	 * @param jumlahPertemuanDiproses   jumlah {@link Pertemuan} yang memiliki data kelompok parameter
	 *                                  aktif dan berhasil ditulis ke rekap.
	 * @param jumlahKelompokParameter   jumlah total {@link KelompokParameterTambahanPertemuan} yang
	 *                                  direkap di seluruh pertemuan.
	 * @param totalBarisMahasiswa       jumlah baris hasil mahasiswa yang berhasil ditulis ke Excel.
	 * @param cellsTerisi               jumlah sel hasil (mahasiswa &times; parameter) yang terisi.
	 * @param cellsKosong               jumlah sel hasil (mahasiswa &times; parameter) yang masih kosong.
	 * @param jumlahPerJenisPertemuan   peta jenis pertemuan (Perkuliahan/Ujian PMB/dsb.) &rarr; jumlah
	 *                                  baris hasil mahasiswa (untuk grafik donat).
	 * @param jumlahPerTopik            peta label pertemuan (tanggal + topik) &rarr; jumlah baris hasil
	 *                                  mahasiswa (untuk grafik batang).
	 */
	private static void renderChartRingkasan(Html chartPanel, int jumlahPertemuanDiproses,
			int jumlahKelompokParameter, int totalBarisMahasiswa, int cellsTerisi, int cellsKosong,
			Map<String, int[]> jumlahPerJenisPertemuan, Map<String, int[]> jumlahPerTopik) {

		int totalCells = cellsTerisi + cellsKosong;
		double persenTerisi = totalCells == 0 ? 0 : (cellsTerisi * 100.0 / totalCells);

		StringBuilder html = new StringBuilder(4096);
		html.append("<div style='padding:10px 12px 4px;display:flex;flex-direction:column;gap:14px;'>");

		html.append(ais.ui.util.HtmlChartHelper.kpiCards(
				new String[] { "Pertemuan Diproses", "Kelompok Data", "Baris Mahasiswa", "Persentase Sel Terisi" },
				new String[] { String.valueOf(jumlahPertemuanDiproses), String.valueOf(jumlahKelompokParameter),
						String.valueOf(totalBarisMahasiswa), Common.numberFormat.get().format(persenTerisi) + "%" },
				new String[] { "pertemuan dengan data parameter tambahan", "kelompok parameter tambahan direkap",
						"baris hasil mahasiswa ditulis ke Excel", "dari seluruh sel hasil pada rekap ini" },
				null, null, new String[] { "#2563eb", "#0891b2", "#16a34a", "#7c3aed" }));

		if (!jumlahPerJenisPertemuan.isEmpty()) {
			String[] labelJenis = jumlahPerJenisPertemuan.keySet().toArray(new String[jumlahPerJenisPertemuan.size()]);
			double[] nilaiJenis = new double[labelJenis.length];
			for (int i = 0; i < labelJenis.length; i++) {
				nilaiJenis[i] = jumlahPerJenisPertemuan.get(labelJenis[i])[0];
			}
			html.append(ais.ui.util.HtmlChartHelper.donut("Komposisi Jenis Pertemuan yang Direkap",
					"Menunjukkan campuran jenis kegiatan (perkuliahan, ujian, tugas akhir, KKN, PKL, skripsi, "
							+ "dan sejenisnya) yang datanya ikut direkap pada berkas ini, sehingga sumber data "
							+ "terlihat sekilas.",
					labelJenis, nilaiJenis, null, totalBarisMahasiswa + " baris"));
		}

		if (!jumlahPerTopik.isEmpty()) {
			List<String> labelTopik = new ArrayList<String>();
			List<Double> nilaiTopikList = new ArrayList<Double>();
			urutkanTopN(jumlahPerTopik, labelTopik, nilaiTopikList, 10);
			double[] nilaiTopik = new double[nilaiTopikList.size()];
			for (int i = 0; i < nilaiTopik.length; i++) {
				nilaiTopik[i] = nilaiTopikList.get(i);
			}
			html.append(ais.ui.util.HtmlChartHelper.barHorizontal("Pertemuan dengan Data Terbanyak",
					"Menunjukkan pertemuan mana saja yang datanya paling banyak direkap, sehingga pertemuan "
							+ "dengan hasil terbanyak mudah dikenali (maksimal 10 pertemuan teratas ditampilkan).",
					labelTopik.toArray(new String[labelTopik.size()]), nilaiTopik, "#2563eb"));
		}

		if (totalCells > 0) {
			html.append(ais.ui.util.HtmlChartHelper.donut("Kelengkapan Pengisian Hasil",
					"Menunjukkan berapa banyak sel hasil yang sudah terisi dibandingkan yang masih kosong, "
							+ "sehingga kelengkapan pengisian data mudah dipantau.",
					new String[] { "Terisi", "Kosong" }, new double[] { cellsTerisi, cellsKosong },
					new String[] { "#16a34a", "#e4e6eb" }, Common.numberFormat.get().format(persenTerisi) + "%"));
		}

		html.append("</div>");
		chartPanel.setContent(html.toString());
	}
}
