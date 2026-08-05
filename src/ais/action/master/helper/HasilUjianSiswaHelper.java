package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.VOSiswa;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.Ambildata;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class HasilUjianSiswaHelper implements DataLoader {

	private PertemuanPunyaUjian pertemuanPunyaUjian;
	private MyGrid grid;
	private Siswa siswa;
	private CalonSiswa calonSiswa;

	private Center east;

	private Pertemuan pertemuan;
	private Textbox nama;

	public HasilUjianSiswaHelper(Pertemuan pertemuan) {
		this.siswa = null;
		this.calonSiswa = null;
		this.pertemuan = pertemuan;
	}

	public HasilUjianSiswaHelper(Siswa siswa, CalonSiswa calonSiswa, Pertemuan pertemuan) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		this.pertemuan = pertemuan;
	}

	@SuppressWarnings({ "deprecation" })
	private void displayStatistik(int jumlahPeserta, int terjawab, int pesertaYgIkutUjian) {
		Common.clear(east);

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(east);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Soal")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan())));

		int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan() * jumlahPeserta;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Soal")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(totalSoal)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Terjawab")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(terjawab)));

		int belum = totalSoal - terjawab;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total Belum Terjawab")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

		Double persen = (100.0 * terjawab) / totalSoal;
		Double persenBelum = (100.0 * belum) / totalSoal;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		// Donut HTML/CSS (HtmlChartHelper) menggantikan pie 3D JFreeChart + penjelasan ringkas.
		row.appendChild(new ais.ui.util.MyHtml(ais.ui.util.HtmlChartHelper.donut(
				"Kelengkapan Jawaban",
				"Menampilkan perbandingan jumlah soal yang telah dijawab dengan yang belum dijawab.",
				new String[] { "Terjawab", "Belum Terjawab" }, new double[] { terjawab, belum },
				new String[] { "#42b72a", "#e4e6eb" }, "terjawab")));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Peserta")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(jumlahPeserta)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta yg melaksanakan ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(pesertaYgIkutUjian)));

		belum = jumlahPeserta - pesertaYgIkutUjian;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta yg belum ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

		persen = (100.0 * pesertaYgIkutUjian) / jumlahPeserta;
		persenBelum = (100.0 * belum) / jumlahPeserta;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		// Donut HTML/CSS (HtmlChartHelper) menggantikan pie 3D JFreeChart + penjelasan ringkas.
		row.appendChild(new ais.ui.util.MyHtml(ais.ui.util.HtmlChartHelper.donut(
				"Keikutsertaan Ujian",
				"Menampilkan jumlah peserta yang telah mengikuti ujian dibandingkan dengan yang belum mengikuti.",
				new String[] { "Ikut ujian", "Belum ujian" }, new double[] { pesertaYgIkutUjian, belum },
				new String[] { "#1877f2", "#e4e6eb" }, "ikut ujian")));

		TreeMap<String, String> d = pertemuanPunyaUjian.getPertemuan().ambilData("ujian_" + pertemuanPunyaUjian.getId(),
				null);
		List<Dosen> dsn = pertemuanPunyaUjian.getPertemuan().ambilDosen();
		int jumlahTotal = jumlahPeserta + dsn.size();
		int telahAkses1 = d.size();
		int belumAkses1 = jumlahTotal - telahAkses1;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Total perserta yg bisa akses")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(jumlahTotal)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perserta yg akses ujian")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(telahAkses1)));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perserta yg belum akses")));
		row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belumAkses1)));

		persen = (100.0 * telahAkses1) / jumlahTotal;
		persenBelum = (100.0 * belumAkses1) / jumlahTotal;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		// Donut HTML/CSS (HtmlChartHelper) menggantikan pie 3D JFreeChart + penjelasan ringkas.
		row.appendChild(new ais.ui.util.MyHtml(ais.ui.util.HtmlChartHelper.donut(
				"Akses Ujian",
				"Menampilkan jumlah peserta yang telah mengakses ujian dibandingkan dengan yang belum mengakses.",
				new String[] { "Sudah akses", "Belum akses" }, new double[] { telahAkses1, belumAkses1 },
				new String[] { "#1877f2", "#e4e6eb" }, "sudah akses")));

		d.clear();
		d = null;
		dsn.clear();
		dsn = null;
	}

	public void display(final PertemuanPunyaUjian pertemuanPunyaUjian, final Component detail) {
		this.pertemuanPunyaUjian = pertemuanPunyaUjian;
		grid = new MyGrid();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(detail);

		North north = new North();
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		Tbmuser tbmuser = Common.getCurrentUser();

		boolean rakhasil = Common.bolehKonfigurasi("tampilkan_rekap_hasil_ujian");

		boolean masihAdaWaktu = (pertemuanPunyaUjian.getMulaiUjian() == null
				|| pertemuanPunyaUjian.getMulaiUjian().before(ais.ui.util.WaktuUtil.getDate()))
				&& (pertemuanPunyaUjian.getSampaiUjian() == null
						|| pertemuanPunyaUjian.getSampaiUjian().after(ais.ui.util.WaktuUtil.getDate()));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ulang Semua", "/img/svg/trash.svg");
		button.setVisible(tbmuser != null && siswa == null && calonSiswa == null);
		button.setDisabled(!masihAdaWaktu);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin mengulang semua ujian ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										List<HasilUjianMahasiswa> ujianMahasiswas = session
												.createCriteria(HasilUjianMahasiswa.class)
												.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
												.list();

										for (HasilUjianMahasiswa hasilUjianMahasiswa : ujianMahasiswas) {

											List<HasilUjianMahasiswaDetail> hasilUjianMahasiswaDetails = session
													.createCriteria(HasilUjianMahasiswaDetail.class)
													.add(Restrictions.eq("hasilUjianMahasiswa", hasilUjianMahasiswa))
													.list();
											for (HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail : hasilUjianMahasiswaDetails) {
												hasilUjianMahasiswaDetail.setBankSoalDetail(null);
												hasilUjianMahasiswaDetail.setJawaban(null);
												hasilUjianMahasiswaDetail.setWaktuJawab(null);
												Common.refreshUpdate(session, hasilUjianMahasiswaDetail);
											}
											hasilUjianMahasiswa.reset();
											Common.refreshUpdate(session, hasilUjianMahasiswa);
											session.flush();

										}

										session.flush();

										loadData(null);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}

								}

							}
						});
			}
		});
		button.setParent(toolbar);

		final String[] contents = new String[] {
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "siswa.nim"
						: "calonSiswa.noRegistrasi",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "siswa.nama" : "calonSiswa.nama",
				"pertemuanPunyaUjian", "lamaPengerjaan", "sisaWaktuPengerjaan", "totalNilai-number",
				"jumlahSoal-number", "jawabanBenar-number", "jawabanBenarMax-number", "telahIkutUjian", "nilai-number",
				"lulus", "mulaiPada", "selesaiPada", "jumlahIkut-number", "keyhasil" };

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Jumlah dikerjakan");
		columnHeadersAdding.add("Jumlah Belum dikerjakan");
		columnHeadersAdding.add("Telah dikerjakan");
		columnHeadersAdding.add("Belum dikerjakan");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) objects[0];
				// Long id = (Long) objects[1];
				XSSFRow row = (XSSFRow) objects[2];

				try {

					MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
							hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);

					int total = ujianPunyaSoals.size();
					Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
							.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

					String jawaban = "";
					for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
						for (Long hasilUjianMahasiswaDetailid : aa) {
							try {
								HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
										.ambilData(HasilUjianMahasiswaDetail.class,
												hasilUjianMahasiswaDetailid.toString());
								if (hasilUjianMahasiswaDetail != null) {

									String h = (hasilUjianMahasiswaDetail.getUjianPunyaSoal().getUjian()
											.getTampilanHurufDiPilihanJawaban()
													? hasilUjianMahasiswaDetail.getBankSoalDetail().getHuruf() + ". "
													: "");

									String j = hasilUjianMahasiswaDetail.getBankSoalDetail() != null
											? (h + hasilUjianMahasiswaDetail.getBankSoalDetail().getJawaban())
											: hasilUjianMahasiswaDetail.getJawaban();

									if (!j.trim().isEmpty()) {
										j = hasilUjianMahasiswaDetail.getUjianPunyaSoal().getBankSoal().getSoal()
												+ ";JAWABAN:" + j + "\n\n";

										jawaban += jawaban.isEmpty() ? j : "; " + j;
										ujianPunyaSoals.remove(hasilUjianMahasiswaDetail.getUjianPunyaSoal().getId());
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:418");
							}
						}
					}

					int size = ujianPunyaSoals.size();
					row.createCell(contents.length).setCellValue(total - size);
					row.createCell(contents.length + 1).setCellValue(size);

					jawaban = Common.maxPanjang(jawaban, 20000);

					row.createCell(contents.length + 2).setCellValue(jawaban);

					String belum = "";
					for (Long id : ujianPunyaSoals) {
						UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
								.ambilData(UjianPunyaSoal.class, id.toString());
						if (ujianPunyaSoal != null) {
							belum += belum.isEmpty() ? ujianPunyaSoal.getBankSoal().getSoal()
									: "; " + ujianPunyaSoal.getBankSoal().getSoal();
						}
					}

					belum = Common.maxPanjang(belum, 20000);

					row.createCell(contents.length + 3).setCellValue(belum);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:445");
				}
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(HasilUjianMahasiswa.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						Session session = HibernateUtil.currentSession();
						return session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian))
								.add(siswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("siswa", siswa))
								.add(calonSiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("calonSiswa", calonSiswa));
					}
				}, "Rekap Hasil Ujian", "/img/print.png", columnHeadersAdding, dataAdding, contents);
		cetakToolbarbutton.setVisible(tbmuser != null && rakhasil);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Peserta dianggap hadir", "/img/svg/check2.svg");
		masuk.setVisible(pertemuan != null && tbmuser != null && siswa == null && calonSiswa == null);
		masuk.setTooltiptext("Tutup");
		masuk.setParent(toolbar);
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				HasilUjianSiswaHelper.ujianDianggapHadir(pertemuanPunyaUjian, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						new PertemuanHelper().display(pertemuan, new DataLoader() {

							@Override
							public void loadData(Object value) {

							}
						}, 0);
					}
				});

			}
		});

		if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {
			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hitung Ulang Semua", "/img/svg/check2-circle.svg");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);

						}
					});

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {

							int rowIndex = 1;
							for (Object[] a : hasilUjianMahasiswas.values()) {
								Session session = HibernateUtil.currentNativeSession();
								try {
									HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
									session.refresh(hasilUjianMahasiswa);
									MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
											hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
											new Label(), true);
									Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
											.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
													ujianPunyaSoals, false);

									hasilUjianMahasiswa
											.setJumlahSoal(pertemuanPunyaUjian.getJmlDitampilkan() == null ? 0.0
													: pertemuanPunyaUjian.getJmlDitampilkan().doubleValue());
									ProsesUjianHelper.hitungPilihanGanda(hasilUjianMahasiswa,
											hasilUjianMahasiswaDetails);
									hasilUjianMahasiswaDetails = null;

									session.getTransaction().begin();
									Common.refreshUpdate(session, hasilUjianMahasiswa);
									session.getTransaction().commit();

									label.setValue("Sedang memproses data " + hasilUjianMahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / hasilUjianMahasiswas.size())
											+ " %)");
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:542");
								}
								HibernateUtil.closeSession();

								rowIndex++;
							}

							label.setValue("");
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				}
			});

			toolbar.appendChild(HasilUjianMahasiswaHelper.analsisButirSoal(pertemuanPunyaUjian, new Ambildata() {

				@Override
				public Object ambil() {
					return hasilUjianMahasiswas;
				}
			}));

		} else {
			MyToolbarbuttonConfig koreksiAiSemua = new MyToolbarbuttonConfig("Koreksi Otomatis via AI",
					"/img/svg/sparkles.svg");
			koreksiAiSemua.setTooltiptext(
					"Koreksi otomatis SEMUA peserta essay via AI (isi Skor & Koreksi) lalu hitung ulang");
			koreksiAiSemua.setStyle("color:#ffffff;background-color:#7c3aed;border-radius:6px;");
			koreksiAiSemua.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final java.util.List<Object[]> tugas = new java.util.ArrayList<Object[]>();
					for (Object[] a : hasilUjianMahasiswas.values()) {
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) a[0];
						java.util.List<Object[]> items = KoreksiHasilUjian.kumpulkanEssay(hum);
						if (items.isEmpty()) {
							continue;
						}
						String nama = "";
						try {
							if (hum.getSiswa() != null && hum.getSiswa().getNama() != null) {
								nama = hum.getSiswa().getNama();
							}
						} catch (Exception e) {
						}
						tugas.add(new Object[]{ hum.getId(), nama, KoreksiHasilUjian.promptKoreksiEssay(items, KoreksiHasilUjian.bangunKonteksUjian(hum)), items });
					}
					if (tugas.isEmpty()) {
						MyMessageboxConfig.show("Tidak ada jawaban essay untuk dikoreksi.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					final org.zkoss.zul.Window win = new org.zkoss.zul.Window("Koreksi Otomatis via AI", "normal",
							false);
					win.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					win.setWidth("560px");
					org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
					vb.setStyle("padding:16px;");
					vb.setHflex("1");
					vb.setParent(win);
					final org.zkoss.zul.Label statusLbl = new org.zkoss.zul.Label("Menyiapkan...");
					statusLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
					vb.appendChild(statusLbl);
					final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter(0);
					meter.setWidth("100%");
					vb.appendChild(meter);
					final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
					streamBox.setMultiline(true);
					streamBox.setReadonly(true);
					streamBox.setRows(8);
					streamBox.setHflex("1");
					streamBox.setStyle("width:100%;margin-top:10px;font-family:monospace;font-size:11px;");
					vb.appendChild(streamBox);
					win.doHighlighted();

					final int total = tugas.size();
					final int[] done = { 0 };
					final boolean[] selesai = { false };
					final StringBuffer sink = new StringBuffer();
					final String[] statusNow = { "" };

					new Thread(new Runnable() {
						@Override
						@SuppressWarnings("unchecked")
						public void run() {
							for (int i = 0; i < tugas.size(); i++) {
								Object[] t = tugas.get(i);
								statusNow[0] = "Mengoreksi " + (i + 1) + "/" + total
										+ (((String) t[1]).length() > 0 ? " — " + t[1] : "");
								sink.setLength(0);
								try {
									String resp = GenerateAiHelper.panggilAi((String) t[2], sink, 2048);
									KoreksiHasilUjian.terapkanKoreksiEssay((java.util.List<Object[]>) t[3], resp,
											(Long) t[0]);
								} catch (Exception e) {
									ais.common.ErrorAuditUtil.record(e, "HasilUjianSiswaHelper.koreksiAiSemua");
								}
								done[0] = i + 1;
							}
							selesai[0] = true;
						}
					}).start();

					final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(800);
					timer.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.setRepeats(true);
					timer.addEventListener("onTimer", new EventListener() {
						@Override
						public void onEvent(Event evtTimer) throws Exception {
							try {
								meter.setValue(total > 0 ? (int) (done[0] * 100L / total) : 100);
								statusLbl.setValue(statusNow[0]);
								String cur = sink.toString();
								if (!cur.equals(streamBox.getValue())) {
									streamBox.setValue(cur);
								}
							} catch (Exception ig) {
							}
							if (selesai[0]) {
								timer.stop();
								timer.detach();
								win.detach();
								loadData(true);
								MyMessageboxConfig.show(
										total + " peserta selesai dikoreksi via AI. Nilai dihitung ulang.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
					timer.start();
				}
			});
			koreksiAiSemua.setParent(toolbar);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Hitung Ulang Semua",
					"/img/Button-Refresh-icon.png");
			cari.setTooltiptext("Hitung Ulang Semua");
			cari.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(true);

						}
					});

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {

							int rowIndex = 1;
							for (Object[] a : hasilUjianMahasiswas.values()) {
								Session session = HibernateUtil.currentNativeSession();
								try {
									HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
									List<Object[]> sumNilais = session.createCriteria(HasilUjianMahasiswaDetail.class)
											.createAlias("bankSoal", "bankSoal")
											.add(Restrictions.eq("hasilUjianMahasiswa", hasilUjianMahasiswa))
											.setProjection(
													Projections.projectionList().add(Projections.property("nilai"))
															.add(Projections.property("bankSoal.skor")))
											.list();

									Double sumNilai = 0.0;
									for (Object[] o : sumNilais) {
										Double nilai = ((Number) o[0]).doubleValue();
										Double skor = ((Number) o[1]).doubleValue();
										sumNilai += (nilai * 100.0) / skor;
									}

									System.out.println("sumNilais = " + sumNilais + ", sumNilai = " + sumNilai);

									if (sumNilai != null && sumNilai.doubleValue() > 0.1) {

										Double n = sumNilai.doubleValue() / sumNilais.size();

										hasilUjianMahasiswa.setNilai(n);

										session.getTransaction().begin();
										Common.refreshUpdate(session, hasilUjianMahasiswa);
										session.getTransaction().commit();

									}
									label.setValue("Sedang memproses data " + hasilUjianMahasiswa.toString() + " ("
											+ Common.numberFormat.get().format(rowIndex * 100.0 / hasilUjianMahasiswas.size())
											+ " %)");
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:628");
								}
								HibernateUtil.closeSession();

								rowIndex++;
							}

							label.setValue("");
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				}

			});
			cari.setParent(toolbar);
		}

		button = new MyToolbarbuttonConfig("Download Lampiran", FileFoto.icon(null));
		button.setVisible(tbmuser != null && siswa == null && calonSiswa == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						File fileFolderLampiran = new File("/opt/ecampus/lampiran_hasil_ujian_"
								+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
						fileFolderLampiran.mkdirs();
						System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());
						Session session = HibernateUtil.currentSession();
						for (Object[] a : hasilUjianMahasiswas.values()) {
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];
							Siswa siswa = hasilUjianMahasiswa.getSiswa();

							MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
									hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
									true);

							Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
									.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(),
											ujianPunyaSoals);

							for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
								for (Long hasilUjianMahasiswaDetailid : aa) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													hasilUjianMahasiswaDetailid.toString());
									if (hasilUjianMahasiswaDetail != null) {
										BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
										for (int i = 0; i < bankSoal.getJumlahLampiran(); i++) {
											LampiranLain lampiranLain = LampiranLain
													.ambil(hasilUjianMahasiswaDetail.getId(), "Jawaban ke-" + (i + 1));

											if (lampiranLain != null) {

												if (hasilUjianMahasiswaDetail.getJawaban().trim().isEmpty()) {
													hasilUjianMahasiswaDetail
															.setJawaban("Jawaban terdapat di file terlampir");
													Common.refreshUpdate(session, hasilUjianMahasiswaDetail);
												}

												File fileFoto = lampiranLain.ambilFile();

												File folder = new File(fileFolderLampiran.getAbsolutePath() + "/"
														+ URLEncoder.encode((bankSoal.getSoal().length() > 55
																? bankSoal.getSoal().substring(0, 55)
																: bankSoal.getSoal()), "UTF-8"));
												folder.mkdirs();

												if (lampiranLain.getGdrive() != null
														&& !lampiranLain.getGdrive().trim().isEmpty()) {
													fileFoto = new File(folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(siswa.getNim() + "_" + siswa.getNama(),
																	"UTF-8")
															+ "_" + lampiranLain.getId() + "_" + fileFoto.getName()
															+ ".txt");
													ais.common.BacaTulisUtil.tulis(fileFoto,
															lampiranLain.forwardGDriveUrl());
												} else if (lampiranLain.getLink() != null
														&& !lampiranLain.getLink().trim().isEmpty()) {
													fileFoto = new File(folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(siswa.getNim() + "_" + siswa.getNama(),
																	"UTF-8")
															+ "_" + lampiranLain.getId() + "_" + fileFoto.getName()
															+ ".txt");
													ais.common.BacaTulisUtil.tulis(fileFoto,
															lampiranLain.getLink().trim());
												} else {
													File fileCopy = new File(folder.getAbsolutePath() + "/"
															+ URLEncoder.encode(siswa.getNim() + "_" + siswa.getNama(),
																	"UTF-8")
															+ "_" + lampiranLain.getId() + "_" + fileFoto.getName());
													System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
													FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
													FileInputStream fileInputStream = new FileInputStream(fileFoto);
													IOUtils.copyLarge(fileInputStream, fileOutputStream);
													fileInputStream.close();
													fileOutputStream.close();
												}
											}
										}
									}
								}
							}

						}

						File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
						Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
						Filedownload.save(fileFolderLampiranZip, "application/zip");

					}
				}, "Harap tunggu.. sedang melakukan proses download lampiran..");
			}
		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig drive = new MyToolbarbuttonConfig("Lampiran ke Drive", FileFoto.icon("drive.google"));
		drive.setParent(toolbar);
		drive.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Tbmuser tbmuser = Common.getCurrentUser();

				final List<Long> tugasFileContents = new ArrayList<Long>();

				for (Object[] a : hasilUjianMahasiswas.values()) {
					HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) a[0];

					MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
							hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);

					Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
							.ambilHasilUjianMahasiswaDetail(pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

					for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
						for (Long hasilUjianMahasiswaDetailid : aa) {
							HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());
							if (hasilUjianMahasiswaDetail != null) {
								BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
								for (int i = 0; i < bankSoal.getJumlahLampiran(); i++) {
									LampiranLain lampiranLain = LampiranLain.ambil(hasilUjianMahasiswaDetail.getId(),
											"Jawaban ke-" + (i + 1));

									if (lampiranLain != null) {
										tugasFileContents.add(lampiranLain.getId());
									}
								}
							}
						}
					}
				}

				if (tugasFileContents.isEmpty()) {
					MyMessageboxConfig.show("Tidak ada file ujian yang bisa dikirim ke google drive", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				} else {

					final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
					final Label label = Common.displayLoadBarjanganBerhenti(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});

					final GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
					File file = new File("/opt/ecampus/test.txt");
					ais.common.BacaTulisUtil.tulis(file, "test send..");
					driveUtilPerPengguna.prosesBackup(file, "test_files",

							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
											.getData();

									if (fileUpload != null && fileUpload.getId() != null) {

										new Thread(new Runnable() {

											@SuppressWarnings("unchecked")
											@Override
											public void run() {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												String tableName = "lampiran_lain";
												String colFotoName = "foto";

												String ids = "";
												for (Long id : tugasFileContents) {
													ids += ids.isEmpty() ? id.toString() : "," + id.toString();
												}

												List<Object[]> inds = session.createSQLQuery("select id," + colFotoName
														+ " from " + tableName + " where " + colFotoName
														+ " is not null and id in (" + ids + ")  order by id desc;")
														.list();
												StreamingHibernateUtil.getInstance().closeSession();

												int size = inds.size();
												int index = 0;
												for (Object[] o : inds) {
													index++;

													try {
														Object id = o[0];
														final Object fotoId = o[1];

														session = StreamingHibernateUtil.getInstance().currentSession();
														final FileFoto fileFoto = (FileFoto) session
																.createCriteria(LampiranLain.class)
																.add(Restrictions.idEq(Long.parseLong(id.toString())))
																.uniqueResult();
														StreamingHibernateUtil.getInstance().closeSession();
														if (fileFoto != null) {
															File file = fileFoto.ambilFile();
															if (file != null && file.exists()) {
																String s = "Mengirim file " + file.getName() + " ("
																		+ Common.numberFormat.get()
																				.format((index * 100.0) / size)
																		+ "%)";
																System.out.println(s);
																label.setValue(s);

																com.google.api.services.drive.model.File fileKirim = driveUtilPerPengguna
																		.kirimBackupLangsung(null, file,
																				perguruanTinggi,
																				fileFoto.getClass().getSimpleName(),
																				new EventListener() {

																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
																								.getData();

																						if (fileUpload != null
																								&& fileUpload
																										.getId() != null) {

																							Session session = StreamingHibernateUtil
																									.getInstance()
																									.currentSession();
																							try {

																								session.refresh(
																										fileFoto);

																								fileFoto.setFoto(null);
																								fileFoto.setGdrive(
																										fileUpload
																												.getId());
																								fileFoto.setGdriveUsername(
																										tbmuser.getUserId());

																								session.getTransaction()
																										.begin();
																								session.update(
																										fileFoto);
																								session.getTransaction()
																										.commit();

																								FileFoto.hapusTotal(
																										fotoId.toString(),
																										session);

																							} catch (Exception e) {
																								StreamingHibernateUtil
																										.getInstance()
																										.rollbackTransaction();
																								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:910");
																							}

																							StreamingHibernateUtil
																									.getInstance()
																									.closeSession();
																						}

																					}
																				});

																if (fileKirim == null) {
																	System.out.println(
																			"Gagal Terkirim " + file.getAbsolutePath());
																	break;
																} else {
																	System.out.println(
																			"Terkirim " + fileKirim.toPrettyString());

																}
															}
														}
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
														break;
													}

												}
												label.setValue("Selesai");
											}
										}).start();
									}

								}
							});

				}

			}
		});

		String[] contents1 = new String[] {
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "id"
						: "hasilUjianMahasiswa.calonSiswa.noRegistrasi",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "hasilUjianMahasiswa.siswa.nim"
						: "hasilUjianMahasiswa.calonSiswa.noUjian",
				pertemuanPunyaUjian.getPertemuan().getJadwalUjianPMB() == null ? "hasilUjianMahasiswa.siswa.nama"
						: "hasilUjianMahasiswa.calonSiswa.nama",
				"bankSoalDetail.huruf", "bankSoal.soal-text", "bankSoalDetail.jawaban", "bankSoalDetail.betul", "nilai",
				"jawaban", "koreksi", "waktuJawab", "hasilUjianMahasiswa", "hasilUjianMahasiswa.keyhasil" };
		cetakToolbarbutton = Common.cetakDataCustomButton(HasilUjianMahasiswaDetail.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(HasilUjianMahasiswaDetail.class)
						.createAlias("hasilUjianMahasiswa", "hasilUjianMahasiswa")
						.add(Restrictions.eq("hasilUjianMahasiswa.pertemuanPunyaUjian", pertemuanPunyaUjian));

				if (order) {
					criteria.addOrder(Order.asc("hasilUjianMahasiswa")).addOrder(Order.asc("id"));
				}
				return criteria;
			}
		}, "Soal dan Jawaban", "/img/print.png", contents1);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(true);
			}
		});

		nama = new Textbox();
		nama.setVisible(siswa == null && calonSiswa == null);
		nama.setCols(7);
		nama.setParent(toolbar);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.setVisible(siswa == null && calonSiswa == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				detail.detach();
			}
		});
		cancel.setParent(toolbar);

		east = new Center();
		east.setBorder("none");

		Center center = new Center();
		center.setParent(borderlayout);
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("15000px");
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSoal = new MyTabConfig("Peserta", "/img/svg/user-group.svg");
		tabs.appendChild(tabSoal);
		MyTabConfig tabPeserta = new MyTabConfig("Statistik", "/img/svg/chart-line-light.svg");
		tabs.appendChild(tabPeserta);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel parent = new ais.ui.util.MyTabpanel();
		parent.setHeight("15000px");
		parent.setParent(tabpanels);

		Borderlayout borderlayoutLagi = new Borderlayout();
		borderlayoutLagi.setParent(parent);

		Center centerLagi = new Center();
		centerLagi.setParent(borderlayoutLagi);
		centerLagi.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(centerLagi, true);

		parent = new ais.ui.util.MyTabpanel();
		parent.setHeight("15000px");
		parent.setParent(tabpanels);

		borderlayoutLagi = new Borderlayout();
		borderlayoutLagi.setParent(parent);

		east.setParent(borderlayoutLagi);
		ais.ui.util.ZkCompat.setFlex(east, true);

		grid.setParent(centerLagi);

		grid.setSclass("fgrid ais-data-grid");
		grid.setHeight("15000px");
		grid.setWidth("100%");
		grid.setMold("paging");
		// Tampilkan SEMUA peserta dalam satu halaman (page size besar).
		grid.setPageSize(1000);
		grid.getPagingChild().setMold("os");
		// Paging tampil di ATAS dan BAWAH tabel (sebelumnya hanya "top").
		grid.setPagingPosition("both");
		try {
			grid.getPagingChild().setDetailed(true);
		} catch (Exception ignorePg) { ais.common.ErrorAuditUtil.record(ignorePg, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:1082");
		}

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig("Peserta Ujian");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Waktu Pengerjaan");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Lama Pengerjaan");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Skor/Max");
		column.setParent(columns);
		column.setWidth("8%");
		column.setVisible(pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA));

		column = new MyColumnConfig("Statistik");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Nilai");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		column = new MyColumnConfig("Pelanggaran");
		column.setParent(columns);
		column.setWidth("13%");

		loadData(null);

	}

	public void tampilRow(final MyDetail detail, final HasilUjianMahasiswa tempHasilUjianMahasiswa) {
		if (tempHasilUjianMahasiswa != null && tempHasilUjianMahasiswa.getPertemuanPunyaUjian() != null) {
			pertemuanPunyaUjian = tempHasilUjianMahasiswa.getPertemuanPunyaUjian();
		}
		new KoreksiHasilUjian().display(detail, tempHasilUjianMahasiswa, pertemuanPunyaUjian);
	}

	

	public class DetailPertemuanPunyaUjianRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, final Object arg1) throws Exception {
			// Kartu peserta hasil ujian (foto+nama dari Vbox/Hbox ZK). Tandai dengan
			// ais-peserta-row agar CSS meratakan latar kotak bersarang -> hover tanpa garis putih.
			arg0.setSclass("ais-peserta-row");

			Siswa siswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
			CalonSiswa calonSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

			Object[] s = siswa != null ? hasilUjianMahasiswas.get(siswa.getId())
					: calonSiswa != null ? hasilUjianMahasiswas.get(calonSiswa.getId())
							: siswa != null ? hasilUjianMahasiswas.get(siswa.getId())
									: calonSiswa != null ? hasilUjianMahasiswas.get(calonSiswa.getId()) : null;

			if (s == null) {
				return;
			}

			final HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) s[0];

			final HasilUjianMahasiswa tempHasilUjianMahasiswa = hasilUjianMahasiswa;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					detail.setAttribute("eventListener", this);

					if (event != null && event.getData() != null && event.getData() instanceof HasilUjianMahasiswa) {
						Common.clear(arg0);
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								render(arg0, arg1);
							}
						});
					} else {
						tampilRow(detail, tempHasilUjianMahasiswa);
					}

				}
			};

			detail.addEventListener("onOpen", eventListener);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
			} else if (calonSiswa != null) {
				CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(hbox);
			}

			Vbox vb = RevisiHelper.createNewRevisi(HasilUjianMahasiswa.class, hasilUjianMahasiswa,
					siswa == null ? calonSiswa.getNoRegistrasi() : siswa.getNim());
			vb.setParent(hbox);

			vb.appendChild(new Label(siswa == null ? calonSiswa.getNama() : siswa.getNama()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			if (hasilUjianMahasiswa.getMulaiPada() != null) {
				new MyLabelKecil("Ujian tgl : " + (hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.dateFormat6.get().format(hasilUjianMahasiswa.getMulaiPada()))).setParent(vbox);
			}

			if (hasilUjianMahasiswa.getMulaiPada() != null) {
				new MyLabelKecil("waktu mulai : " + (hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.timeFormat.get().format(hasilUjianMahasiswa.getMulaiPada()))).setParent(vbox);
			}
			if (hasilUjianMahasiswa.getSelesaiPada() != null) {
				new MyLabelKecil("waktu selesai : " + (hasilUjianMahasiswa.getSelesaiPada() == null ? ""
						: Common.timeFormat.get().format(hasilUjianMahasiswa.getSelesaiPada()))).setParent(vbox);
			}

			vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox hb = new Hbox();
			hb.setParent(vbox);

			new MyLabelKecil("Ikut ujian").setParent(hb);
			final Intbox ikut = new Intbox(hasilUjianMahasiswa.getJumlahIkut());
			ikut.setCols(1);
			ikut.setStyle("font-size:9px;");
			ikut.setParent(hb);
			new MyLabelKecil("kali").setParent(hb);
			ikut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					hasilUjianMahasiswa.setJumlahIkut(ikut.getValue());
					Common.refreshUpdate(hasilUjianMahasiswa);
				}
			});

			// Lama Waktu = DURASI pengerjaan (getLamaPengerjaan dibuat via GregorianCalendar(0,0,0,jam,menit,detik)
			// -> bagian tanggalnya ngawur "31-12-0002"). Format bagian WAKTU-nya saja: HH:mm:ss (timeFormat1).
			new MyLabelKecil("Lama Waktu : " + (hasilUjianMahasiswa.getLamaPengerjaan() == null ? ""
					: Common.timeFormat1.get().format(hasilUjianMahasiswa.getLamaPengerjaan()))).setParent(vbox);

			final Timebox sisaWaktu = new ais.ui.util.MyTimebox(hasilUjianMahasiswa.getSisaWaktuPengerjaan());
			sisaWaktu.setStyle("font-size:9px;");
			sisaWaktu.setDisabled(hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null);
			sisaWaktu.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (sisaWaktu.getValue() != null) {
						// FIX (laporan: admin tidak bisa menambah waktu ujian, kembali ke 0 setelah
						// refresh) — pola lama (set lalu refreshUpdate) memicu Hibernate MEMANGGIL
						// getter getSisaWaktuPengerjaan() saat dirty-check/flush (mapping berbasis
						// PROPERTY access); getter itu SENGAJA menimpa nilai in-memory dengan cache
						// file "live" (retreive()) yang masih menyimpan nilai LAMA (sinkron ke file
						// baru terjadi SETELAH ini, lewat put() di bawah) -> nilai admin batal
						// tersimpan. FIX: UPDATE langsung via HQL bulk update (tanpa memuat entity /
						// memanggil getter) supaya nilai yang tersimpan pasti sesuai input admin.
						try {
							HibernateUtil.currentSession().createQuery(
									"update HasilUjianMahasiswa set sisaWaktuPengerjaan = :v where id = :id")
									.setParameter("v", sisaWaktu.getValue())
									.setParameter("id", hasilUjianMahasiswa.getId()).executeUpdate();
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e, "HasilUjianSiswaHelper sisaWaktu onChange");
						}
						hasilUjianMahasiswa.put(Common.databaseDateFormat1.get().format(sisaWaktu.getValue()));
						hasilUjianMahasiswa.setSisaWaktuPengerjaan(sisaWaktu.getValue());
					}
				}
			});

			hb = new Hbox();
			hb.setParent(vbox);
			new MyLabelKecil("Sisa Waktu").setParent(hb);
			sisaWaktu.setParent(hb);
			sisaWaktu.setCols(4);

			new Label(Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())
					+ (hasilUjianMahasiswa.getJawabanBenarMax() == null ? ""
							: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenarMax())))
					.setParent(arg0);

			hb = new Hbox();
			hb.setParent(vbox);
			new MyLabelKecil("Nomor Terakhir").setParent(hb);
			int startIndex = 0;
			try {
				String ss = hasilUjianMahasiswa.retreive("index");
				if (ss != null && !ss.trim().isEmpty()) {
					startIndex = Integer.parseInt(ss.trim());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1282");
			}

			final MyIntbox startIndexInput = new MyIntbox(startIndex + 1);
			startIndexInput.setStyle("font-size:8px;");
			startIndexInput.setCols(2);
			startIndexInput.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					hasilUjianMahasiswa.put(
							(startIndexInput.getValue() == null ? 0 : (startIndexInput.getValue() - 1)) + "", "index");
				}
			});
			startIndexInput.setParent(hb);

			int totalSoal = pertemuanPunyaUjian.getJmlDitampilkan();

			Set<Long> idsa = (Set<Long>) s[1];
			int terjawab = idsa.size();

			int belum = totalSoal - terjawab;

			Double persen = (100.0 * terjawab) / totalSoal;
			Double persenBelum = (100.0 * belum) / totalSoal;

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil("Jml Soal : " + Common.numberFormat.get().format(totalSoal)).setParent(vbox);
			new MyLabelAgakKecil("Soal Terjawab : " + Common.numberFormat.get().format(terjawab) + " / "
					+ Common.numberFormat.get().format(persen) + "%").setParent(vbox);
			new MyLabelAgakKecil("Soal Belum Terjawab : " + Common.numberFormat.get().format(belum) + " / "
					+ Common.numberFormat.get().format(persenBelum) + "%").setParent(vbox);

			if (persen.intValue() > 0 && persen.intValue() < 100) {
				arg0.setStyle("background-color: rgba(205,92,92,0.4);");
			} else if (persen.intValue() == 100) {
				arg0.setStyle("background:#eeffeb;");
			}

			final MyCheckboxConfig lengkapiJawaban = new MyCheckboxConfig(
					"Lengkapi ulang jawaban (pilihan ini tidak aktif kembali ketika peserta telah ujian ulang)");
			lengkapiJawaban.setStyle("font-size:8px;");
			lengkapiJawaban.setChecked(hasilUjianMahasiswa.getLengkapiJawaban());
			lengkapiJawaban.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// FIX (laporan: checklist ini kembali hilang setelah refresh) — pola sama dengan
					// perbaikan "Sisa Waktu" di atas: pakai HQL bulk update supaya nilai yang
					// tersimpan pasti sesuai pilihan admin.
					try {
						HibernateUtil.currentSession().createQuery(
								"update HasilUjianMahasiswa set lengkapiJawaban = :v where id = :id")
								.setParameter("v", lengkapiJawaban.isChecked())
								.setParameter("id", hasilUjianMahasiswa.getId()).executeUpdate();
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "HasilUjianSiswaHelper lengkapiJawaban onClick");
					}
					hasilUjianMahasiswa.setLengkapiJawaban(lengkapiJawaban.isChecked());
				}
			});
			lengkapiJawaban.setParent(vbox);

			if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {

				new Label(Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()) + "").setParent(arg0);
			} else {

				hbox = new Hbox();
				hbox.setParent(arg0);

				final MyDoublebox doublebox = new MyDoublebox();
				doublebox.setCols(3);
				doublebox.setValue(tempHasilUjianMahasiswa.getNilai());
				doublebox.setParent(hbox);
				final org.zkoss.zul.Label lblAutoNilaiSiswa = new org.zkoss.zul.Label("");
				lblAutoNilaiSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
				lblAutoNilaiSiswa.setParent(hbox);
				doublebox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = HibernateUtil.currentSession();
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
									.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.idEq(tempHasilUjianMahasiswa.getId())).uniqueResult();
							hasilUjianMahasiswa.setNilai(doublebox.getValue());
							session.update(hasilUjianMahasiswa);
							lblAutoNilaiSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
							lblAutoNilaiSiswa.setValue("✓");
							lblAutoNilaiSiswa.setTooltiptext("Tersimpan");
							org.zkoss.zk.ui.util.Clients.evalJavaScript(
								"(function(){var e=document.getElementById('" + lblAutoNilaiSiswa.getUuid() + "');" +
								"if(!e)return;e.style.transition='none';e.style.opacity='1';" +
								"setTimeout(function(){e.style.transition='opacity 0.8s';e.style.opacity='0';},2500);" +
								"})();"
							);
						} catch (Exception eSave) {
							lblAutoNilaiSiswa.setValue("✗");
							lblAutoNilaiSiswa.setStyle("color:red;font-size:13px;font-weight:bold;");
							lblAutoNilaiSiswa.setTooltiptext("Gagal simpan: " + eSave.getMessage());
							ais.common.ErrorAuditUtil.record(eSave, "auto-audit HasilUjianSiswaHelper onChange nilai auto-save");
						}
					}
				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang",
						"/img/Button-Refresh-icon.png");
				button.setOrient("vertical");
				button.setTooltiptext("Hitung Ulang");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						MyArrayList<Long> ujianPunyaSoals = tempHasilUjianMahasiswa.ambilUjianPunyaSoals(
								tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(),
								true);
						MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa = tempHasilUjianMahasiswa
								.ambilHasilUjianMahasiswaDetail(true,
										tempHasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
										new Label(), ujianPunyaSoals);

						Double sumNilai = 0.0;
						for (Long ujianPunyaSoalid : ujianPunyaSoals) {
							UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
									.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
							if (ujianPunyaSoal != null) {
								BankSoal bankSoal = ujianPunyaSoal.getBankSoal();
								Set<Long> hasilUjianMahasiswaDetails = hasilUjianMahasiswaDetailsa
										.get(bankSoal.getId());
								if (hasilUjianMahasiswaDetails != null && !hasilUjianMahasiswaDetails.isEmpty()) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													hasilUjianMahasiswaDetails.iterator().next().toString());
									if (hasilUjianMahasiswaDetail != null) {
										Double nilai = hasilUjianMahasiswaDetail.getNilai();
										Double skor = bankSoal.getSkor();
										sumNilai += (nilai * 100.0) / skor;
									}
								}
							}
						}

						System.out.println("ujianPunyaSoals = " + ujianPunyaSoals + ", sumNilai = " + sumNilai);

						if (sumNilai != null && sumNilai.doubleValue() > 0.1) {

							Double n = sumNilai.doubleValue() / ujianPunyaSoals.size();

							tempHasilUjianMahasiswa.setNilai(n);

							Session session = HibernateUtil.currentNativeSession();
							try {
								session.getTransaction().begin();
								Common.refreshUpdate(session, tempHasilUjianMahasiswa);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1422");
							}
							HibernateUtil.closeSession();
							doublebox.setValue(n);

						} else {
							MyMessageboxConfig.show("Hasil ujian siswa belum Anda koreksi", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
					}

				});
				button.setParent(hbox);

			}

			final MyTextbox keterangan = new MyTextbox(tempHasilUjianMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);
			org.zkoss.zul.Hbox hboxKet = new org.zkoss.zul.Hbox();
			hboxKet.setParent(arg0);
			keterangan.setParent(hboxKet);
			final org.zkoss.zul.Label lblAutoKetSiswa = new org.zkoss.zul.Label("");
			lblAutoKetSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
			lblAutoKetSiswa.setParent(hboxKet);

			// === Kolom Pelanggaran (rekap pengawasan ujian / anti-curang) ===
			int jmlLgr = tempHasilUjianMahasiswa.getJumlahPelanggaran() == null ? 0
					: tempHasilUjianMahasiswa.getJumlahPelanggaran().intValue();
			Vbox vboxLgr = new Vbox();
			vboxLgr.setParent(arg0);
			Label lblJmlLgr = new Label(jmlLgr > 0 ? (jmlLgr + " pelanggaran") : "0 (bersih)");
			lblJmlLgr.setStyle(jmlLgr > 0 ? "color:#b91c1c;font-weight:bold;" : "color:#16a34a;");
			lblJmlLgr.setParent(vboxLgr);
			String logLgr = tempHasilUjianMahasiswa.getLogPelanggaran();
			if (logLgr != null && !logLgr.trim().isEmpty()) {
				Label lblLogLgr = new Label(logLgr.length() > 400 ? logLgr.substring(0, 400) + " ..." : logLgr);
				lblLogLgr.setMultiline(true);
				lblLogLgr.setPre(true);
				lblLogLgr.setStyle("font-size:10px;color:#64748b;white-space:pre-wrap;");
				lblLogLgr.setTooltiptext(logLgr);
				lblLogLgr.setParent(vboxLgr);
			}

			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						tempHasilUjianMahasiswa.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(tempHasilUjianMahasiswa);
						lblAutoKetSiswa.setStyle("color:green;font-size:13px;font-weight:bold;");
						lblAutoKetSiswa.setValue("✓");
						lblAutoKetSiswa.setTooltiptext("Tersimpan");
						org.zkoss.zk.ui.util.Clients.evalJavaScript(
							"(function(){var e=document.getElementById('" + lblAutoKetSiswa.getUuid() + "');" +
							"if(!e)return;e.style.transition='none';e.style.opacity='1';" +
							"setTimeout(function(){e.style.transition='opacity 0.8s';e.style.opacity='0';},2500);" +
							"})();"
						);
					} catch (Exception eSave) {
						lblAutoKetSiswa.setValue("✗");
						lblAutoKetSiswa.setStyle("color:red;font-size:13px;font-weight:bold;");
						lblAutoKetSiswa.setTooltiptext("Gagal simpan: " + eSave.getMessage());
						ais.common.ErrorAuditUtil.record(eSave, "auto-audit HasilUjianSiswaHelper onChange keterangan auto-save");
					}
				}
			});

		}
	}
	
	private Map<Long, Object[]> hasilUjianMahasiswas = null;
	// PERBAIKAN 1: Gunakan tipe pasti <VOSiswa>, hilangkan '? extends'
	private List<VOSiswa> siswasTemorary = null;
	private int jumlahPeserta = 0;

	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {
		// PERBAIKAN 2: Gunakan ConcurrentHashMap agar aman diakses oleh Thread Background
		hasilUjianMahasiswas = new java.util.concurrent.ConcurrentHashMap<Long, Object[]>();
		siswasTemorary = new ArrayList<VOSiswa>();

		final Boolean refresh = (Boolean) (value == null ? false : value);
		final String searchValue = (nama != null && nama.getValue() != null) ? nama.getValue().trim() : "";
		final boolean isSearchEmpty = searchValue.isEmpty();

		if (siswa != null) {
			siswasTemorary.add(siswa);
			jumlahPeserta = pertemuanPunyaUjian.getPertemuan().getPerkuliahan().ambilJumlahDetailperkuliahan();
		} else if (calonSiswa != null) {
			siswasTemorary.add(calonSiswa);
			Session session = HibernateUtil.currentSession();
			jumlahPeserta = ((Number) session.createCriteria(HasilUjianMahasiswa.class)
					.add(Restrictions.isNotNull("keyhasil"))
					.setProjection(Projections.rowCount())
					.add(Restrictions.isNotNull("calonSiswa"))
					.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)).uniqueResult()).intValue();
		} else {

			if (pertemuanPunyaUjian.getPertemuan() != null
					&& pertemuanPunyaUjian.getPertemuan().getJadwalUjianPSB() != null) {
				Session session = HibernateUtil.currentSession();

				if (pertemuanPunyaUjian.getPertemuan().getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
					// Gunakan .addAll() untuk menyisipkan data list
					siswasTemorary.addAll(ConstantValues.simpleList(
							session.createCriteria(CalonSiswa.class)
									.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
									.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
											: Restrictions.or(
													Restrictions.ilike("nama", searchValue, MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("noRegistrasi", searchValue, MatchMode.ANYWHERE),
															Restrictions.ilike("noUjian", searchValue, MatchMode.ANYWHERE))))
									.add(Restrictions.eq("gelombangPendaftaranPsb", pertemuanPunyaUjian.getPertemuan().getJadwalUjianPSB().getGelombangPendaftaranPsb()))
									.addOrder(Order.asc("nomorInduk")), 
							CalonSiswa.class));
					
					jumlahPeserta = siswasTemorary.size();
				} else {
					siswasTemorary.addAll(ConstantValues.simpleList(
							session.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.isNotNull("keyhasil"))
									.setProjection(Projections.groupProperty("calonSiswa.id"))
									.add(Restrictions.isNotNull("calonSiswa"))
									.createAlias("calonSiswa", "calonSiswa")
									.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
											: Restrictions.or(
													Restrictions.ilike("calonSiswa.nama", searchValue, MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("calonSiswa.noRegistrasi", searchValue, MatchMode.ANYWHERE),
															Restrictions.ilike("calonSiswa.noUjian", searchValue, MatchMode.ANYWHERE))))
									.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
							CalonSiswa.class, false));

					jumlahPeserta = siswasTemorary.size();
				}
			} else if (pertemuanPunyaUjian.getPertemuan() != null) {
				List<Siswa> temp = AbsensiSiswaHelper.populateSiswaDariPertemuan(pertemuanPunyaUjian.getPertemuan());
				List<Siswa> siswas = new ArrayList<Siswa>();
				String searchLower = searchValue.toLowerCase();
				
				for (Siswa s : temp) {
					if (isSearchEmpty || 
					   (s.getNama() != null && s.getNama().toLowerCase().contains(searchLower)) || 
					   (s.getNim() != null && s.getNim().toLowerCase().contains(searchLower))) {
						
						Long id = s.getId();
						if (pertemuanPunyaUjian.getMhsYgTidakIkut() == null || !pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ",")) {
							siswas.add(s);
						}
					}
				}
				temp = null;
				siswasTemorary.addAll(siswas);
				jumlahPeserta = siswasTemorary.size();
			} else {
				Session session = HibernateUtil.currentSession();
				
				siswasTemorary.addAll(ConstantValues.simpleList(
						session.createCriteria(HasilUjianMahasiswa.class)
								.add(Restrictions.isNotNull("keyhasil"))
								.setProjection(Projections.groupProperty("calonSiswa.id"))
								.add(Restrictions.isNotNull("calonSiswa"))
								.createAlias("calonSiswa", "calonSiswa")
								.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("calonSiswa.nama", searchValue, MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("calonSiswa.noRegistrasi", searchValue, MatchMode.ANYWHERE),
														Restrictions.ilike("calonSiswa.noUjian", searchValue, MatchMode.ANYWHERE))))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)),
						CalonSiswa.class, false));

				List<Siswa> mhs = ConstantValues.simpleList(
						session.createCriteria(HasilUjianMahasiswa.class)
								.add(Restrictions.isNotNull("keyhasil"))
								.setProjection(Projections.groupProperty("siswa.id"))
								.add(Restrictions.isNotNull("siswa"))
								.createAlias("siswa", "siswa")
								.add(isSearchEmpty ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("siswa.nama", searchValue, MatchMode.ANYWHERE),
												Restrictions.ilike("siswa.nim", searchValue, MatchMode.ANYWHERE)))
								.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)), 
						Siswa.class, false);

				if (mhs != null) {
					siswasTemorary.addAll(mhs);
				}
				jumlahPeserta = siswasTemorary.size();
			}
		}

		final Label label = Common.displayLoadBar(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				ListModel strset = new SimpleListModel(siswasTemorary);
				grid.setRowRenderer(new DetailPertemuanPunyaUjianRenderer());
				grid.setModelCheckMobile(strset);

				int terjawab = 0;
				int pesertaYgIkutUjian = 0;
				
				for (Object[] obj : hasilUjianMahasiswas.values()) {
					if (obj != null && obj.length > 1) {
						Set<Long> terjwb = (Set<Long>) obj[1];
						int jumlhaTerjawab = terjwb == null ? 0 : terjwb.size();
						terjawab += jumlhaTerjawab;
						if (jumlhaTerjawab > 0) {
							pesertaYgIkutUjian++;
						}
					}
				}

				displayStatistik(jumlahPeserta, terjawab, pesertaYgIkutUjian);

				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (siswasTemorary != null) {
							siswasTemorary.clear();
						}
					}
				});
			}
		});

		final boolean reloadNama = isSearchEmpty;
		if (reloadNama) {
			pertemuanPunyaUjian.bersihkanLokasiHasilUjianMahasiswa();
			pertemuanPunyaUjian.tulisLokasiHasilUjianMahasiswa(new JSONObject().toString());
		}

		// PERBAIKAN 3: Penggunaan Multithreading Thread-Pool Executor untuk proses background
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final int size = siswasTemorary.size();
					if (size == 0) {
						if (label != null) label.setValue("");
						return;
					}

					// AtomicInteger mencegah bentrok saat update persentase dari berbagai thread
					final java.util.concurrent.atomic.AtomicInteger processedCounter = new java.util.concurrent.atomic.AtomicInteger(0);
					java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(50)); // Plafon aman c3p0

					for (final VOSiswa voSiswa : siswasTemorary) {
						if (voSiswa == null || hasilUjianMahasiswas.containsKey(voSiswa.getId())) {
							processedCounter.incrementAndGet();
							continue;
						}

						executor.submit(new Runnable() {
							@Override
							public void run() {
								try {
									Siswa s = (voSiswa instanceof Siswa) ? (Siswa) voSiswa : null;
									CalonSiswa cSiswa = (voSiswa instanceof CalonSiswa) ? (CalonSiswa) voSiswa : null;

									HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(
											pertemuanPunyaUjian, null, null, s, cSiswa);
									
									if (reloadNama && hasilUjianMahasiswa != null) {
										pertemuanPunyaUjian.populateHasilUjianMahasiswa(hasilUjianMahasiswa, true);
									}
									
									if (hasilUjianMahasiswa != null) {
										MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
												hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(),
												new Label(), true);

										hasilUjianMahasiswas.put(voSiswa.getId(), new Object[] { 
												hasilUjianMahasiswa,
												hasilUjianMahasiswa.ambilBankSoalIdTerjawab(
														pertemuanPunyaUjian.getJmlDitampilkan(),
														ujianPunyaSoals, refresh) 
										});
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1686");
								} finally {
									int currentIdx = processedCounter.incrementAndGet();
									double percentage = (currentIdx * 100.0) / size;
									if (label != null && voSiswa.getNama() != null) {
										try {
											label.setValue("Sedang memproses data " + voSiswa.getNama() + " ("
													+ Common.numberFormat.get().format(percentage) + " %)");
										} catch (Exception uiEx) { ais.common.ErrorAuditUtil.record(uiEx, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:1694");}
									}
								}
							}
						});
					}

					executor.shutdown();
					try {
						executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
					} catch (InterruptedException ie) {
						ie.printStackTrace(); ais.common.ErrorAuditUtil.record(ie, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1705");
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HasilUjianSiswaHelper.java:1709");
				} finally {
					if (label != null) {
						try { label.setValue(""); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianSiswaHelper.java:1712");}
					}
				}
			}
		}).start();
	}

	public static void ujianDianggapHadir(final PertemuanPunyaUjian pertemuanPunyaUjian,
			final EventListener eventListener) throws Exception {

		MyMessageboxConfig.show(
				"Apakah yakin semua siswa yang mengikuti \"" + pertemuanPunyaUjian.getUjian().getNama()
						+ "\" dianggap hadir kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
									if (pertemuan != null && pertemuan.getId() != null) {
										HibernateUtil.currentSession().refresh(pertemuan);
									}

									List<Long> hasilUjianMahasiswas = pertemuanPunyaUjian
											.ambilHasilUjianMahasiswa(true);
									System.out.println("hasilUjianMahasiswas -> " + hasilUjianMahasiswas);
									for (Long hasilUjianMahasiswaid : hasilUjianMahasiswas) {

										HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
												.ambilData(HasilUjianMahasiswa.class, hasilUjianMahasiswaid.toString());
										System.out.println("hasilUjianMahasiswa -> " + hasilUjianMahasiswa);
										if (hasilUjianMahasiswa != null) {
											if ((hasilUjianMahasiswa.getCalonSiswa() != null
													|| hasilUjianMahasiswa.getSiswa() != null)
													&& hasilUjianMahasiswa.getMulaiPada() != null
													&& hasilUjianMahasiswa.getSelesaiPada() != null) {
												Long mhs = hasilUjianMahasiswa.getCalonSiswa() != null
														? hasilUjianMahasiswa.getCalonSiswa().getId()
														: hasilUjianMahasiswa.getSiswa().getId();
												Statusabsensi statusabsensi = ConstantValues.MASUK;

												MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa
														.ambilUjianPunyaSoals(hasilUjianMahasiswa
																.getPertemuanPunyaUjian().getJmlDitampilkan(),
																new Label(), true);

												Set<Long> idsa = hasilUjianMahasiswa.ambilBankSoalIdTerjawab(
														pertemuanPunyaUjian.getJmlDitampilkan(), ujianPunyaSoals);

												int terjawab = idsa.size();

												String mulai = pertemuan.retreiveAbsensiMulai(mhs);
												String sampai = pertemuan.retreiveAbsensiSampai(mhs);
												if (mulai == null || mulai.trim().isEmpty()) {
													mulai = pertemuan.getWaktuMulai();
												}
												if (sampai == null || sampai.trim().isEmpty()) {
													sampai = pertemuan.getWaktuSelesai();
												}

												System.out.println("terjawab -> " + terjawab + ", mulai " + mulai
														+ ", sampai " + sampai);

												pertemuan.populate(mhs, statusabsensi, "Mengikuti ujian \""
														+ pertemuanPunyaUjian.getUjian().getNama() + "\" pada "
														+ Common.dateFormat5.get().format(hasilUjianMahasiswa.getMulaiPada())
														+ " sampai dengan "
														+ Common.dateFormat5.get()
																.format(hasilUjianMahasiswa.getSelesaiPada())
														+ " dengan jumlah soal "
														+ Common.numberFormat.get()
																.format(hasilUjianMahasiswa.getJumlahSoal())
														+ " dan telah terjawab " + Common.numberFormat.get().format(terjawab),
														null, mulai, sampai, "Siswa");
											}
										}
									}

									Common.refreshUpdate(pertemuan);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				});

	}
}
