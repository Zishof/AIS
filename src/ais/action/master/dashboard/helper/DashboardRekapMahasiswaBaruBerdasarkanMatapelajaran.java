package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaMatapelajaran;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard rekap mahasiswa baru berdasarkan matapelajaran. Kelas
 * ini memilih variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan
 * dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox paket}, {@code Combobox
 * searchJenisSemester}, {@code Combobox searchprogram}, {@code Spreadsheet spreadsheet}, {@code Center center};
 * inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardRekapMahasiswaBaruBerdasarkanMatapelajaran extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox paket = new Combobox();
	protected Combobox searchJenisSemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchpilihan;

	public DashboardRekapMahasiswaBaruBerdasarkanMatapelajaran() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		searchpilihan = new Combobox();
		Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Prodi Lulus");
		comboitem.setValue("prodiLulus");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi I");
		comboitem.setValue("prodi1");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi II");
		comboitem.setValue("prodi2");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi III");
		comboitem.setValue("prodi3");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi IV");
		comboitem.setValue("prodi4");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi V");
		comboitem.setValue("prodi5");
		searchpilihan.appendChild(comboitem);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
		row.appendChild(searchpilihan);
		searchpilihan.setWidth("90%");
		searchpilihan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		searchpilihan.setReadonly(true);
		searchpilihan.setSelectedIndex(1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket *"));
		Map<Long, GeneralValueObject> pakets = ConstantValues.ambilBerdasarClass(Paket.class);

		for (GeneralValueObject generalValueObject : pakets.values()) {
			Paket myPaket = (Paket) generalValueObject;
			if (myPaket != null) {
				myPaket = GeneralValueObject.check(myPaket);
				if (myPaket.getAktif()) {
					int maxColoumn = 0;
					for (String nilaikelas : myPaket.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {
							maxColoumn++;
						}
					}
					if (maxColoumn > 0) {
						comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(myPaket.getNama());
						comboitem.setValue(myPaket);
						paket.appendChild(comboitem);
					}
				}
			}

		}
		paket.setReadonly(true);
		if (!paket.getChildren().isEmpty()) {
			paket.setSelectedIndex(0);
		}
		row.appendChild(paket);
		paket.setWidth("90%");
		paket.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisSemester.appendChild(comboitem);

		searchJenisSemester.setSelectedItem(comboitem);
		searchJenisSemester.setReadonly(true);
		row.appendChild(searchJenisSemester);
		searchJenisSemester.setWidth("90%");
		searchJenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
			}
		});

		refresh.setParent(hbox);
		print.setParent(hbox);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				String tahunAkademik = (String) (DashboardRekapMahasiswaBaruBerdasarkanMatapelajaran.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardRekapMahasiswaBaruBerdasarkanMatapelajaran.this.tahunAkademik
										.getSelectedItem().getValue());
				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				Paket myPaket = (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());

				String prodiPilihan = (String) (searchpilihan.getSelectedItem() == null ? null
						: searchpilihan.getSelectedItem().getValue());

				String semesterMulai = (String) (searchJenisSemester.getSelectedItem() == null ? null
						: searchJenisSemester.getSelectedItem().getValue());

				if (tahunAkademik == null || myPaket == null || prodiPilihan == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();

				List<BiodataCalonMahasiswa> calonMahasiswas = ConstantValues
						.simpleList(
								session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(jurusan == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq(prodiPilihan, jurusan))
										.add(program == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("program", program))
										.add(myPaket == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("paket", myPaket))
										.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik", tahunAkademik))
										.add(semesterMulai == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("semesterMulai", semesterMulai))
										.addOrder(Order.asc(prodiPilihan)).addOrder(Order.asc("noRegistrasi")),
								BiodataCalonMahasiswa.class);

				int maxColoumn = 0;
				for (String nilaikelas : myPaket.getKelasVerifikasiRapor().split(";")) {
					if (!nilaikelas.trim().isEmpty()) {
						maxColoumn++;
					}
				}

				spreadsheet = new ais.ui.util.MySpreadsheet();
Common.clear(center);spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns((maxColoumn) + 10);
				spreadsheet.setMaxrows(calonMahasiswas.size() + 5);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI MATAPELAJARAN CALON MAHASISWA " + searchpilihan.getValue().toUpperCase() + "\n"
								+ (fakultas == null ? "" : fakultas.getNama().toUpperCase() + "\n")
								+ (jurusan == null ? "" : jurusan.getNama().toUpperCase() + "\n") + " TAHUN AKADEMIK "
								+ tahunAkademik + " "
								+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
										: searchJenisSemester.getSelectedItem().getValue())
								+ "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()));

				int rowIndex = 0;

				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, true);

				rowIndex += 4;

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Fakultas");
				Utils.setColumnWidth(sheet, 0, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jurusan");
				Utils.setColumnWidth(sheet, 1, 200);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "No. Reg");
				Utils.setColumnWidth(sheet, 2, 100);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "No. Ujian");
				Utils.setColumnWidth(sheet, 3, 100);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Nama");
				Utils.setColumnWidth(sheet, 4, 250);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Mata pelajaran");
				Utils.setColumnWidth(sheet, 5, 200);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Keterangan Matpel");
				Utils.setColumnWidth(sheet, 6, 200);

				int index = 6;
				for (String nilaikelas : myPaket.getKelasVerifikasiRapor().split(";")) {
					if (!nilaikelas.trim().isEmpty()) {
						String[] ca = StringUtils.split(nilaikelas, ":");
						String kel = ca.length > 0 ? ca[0] : "";
						String sem = ca.length > 1 ? ca[1] : "";

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index,
								"Kls:" + kel + (sem.isEmpty() ? "" : ", Smt:" + sem));
						Utils.setColumnWidth(sheet, index, 70);
					}
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, "Total");
				Utils.setColumnWidth(sheet, index, 70);

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, "Rata-rata");
				Utils.setColumnWidth(sheet, index, 70);

				rowIndex++;

				String namaFakultas = "";
				String namaProdi = "";

				ClassMetadata classMetadata = HibernateUtil.getClassMetadata(BiodataCalonMahasiswa.class);
				List<MatapelajaranSekolah> matapelajaranSekolahs = session.createCriteria(PaketPunyaMatapelajaran.class)
						.setProjection(Projections.property("matapelajaranSekolah"))
						.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
						.add(Restrictions.eq("paket", myPaket)).add(Restrictions.eq("matapelajaranSekolah.aktif", true))
						.addOrder(Order.asc("matapelajaranSekolah.nama")).list();

				for (BiodataCalonMahasiswa biodataCalonMahasiswa : calonMahasiswas) {
					Jurusan jur = (Jurusan) classMetadata.getPropertyValue(biodataCalonMahasiswa, prodiPilihan, EntityMode.POJO);
					if (jur != null) {
						if (!namaFakultas.equals(jur.getFakultas().getNama())) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, jur.getFakultas().getNama());
							namaFakultas = jur.getFakultas().getNama();
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
						}

						if (!namaProdi.equals(jur.getNama())) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, jur.getNama());
							namaProdi = jur.getNama();
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
						}
					} else {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tidak pilih " + "Fakultas");
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
								"Tidak pilih " + Common.getBahasaConfig("Jurusan"));
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, biodataCalonMahasiswa.getNoRegistrasi());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, biodataCalonMahasiswa.getNoUjian());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, biodataCalonMahasiswa.getNama());

					Map<String, Double> nilais = new HashMap<String, Double>();
					for (MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

						BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) session
								.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.class)
								.add(Restrictions.eq("matapelajaranSekolah", matapelajaranSekolah))
								.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)).setMaxResults(1)
								.uniqueResult();

						if (biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp == null) {
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp
									.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
							biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp
									.setMatapelajaranSekolah(matapelajaranSekolah);
							Common.refreshSaveOrUpdate(session, biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp);
						}

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, matapelajaranSekolah.getNama());

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
								biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp.getKeterangan());
						index = 6;

						Double total = 0.0;
						Double jumlah = 0.0;

						for (String nilaikelas : myPaket.getKelasVerifikasiRapor().split(";")) {
							if (!nilaikelas.trim().isEmpty()) {
								Double nilai = biodataCalonMahasiswaPunyaVerifikasiMatapelajaranTemp
										.ambilNilai(nilaikelas.trim());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, nilai);

								Double jumlahVertical = nilais.get(nilaikelas);
								if (jumlahVertical == null) {
									jumlahVertical = 0.0;
								}
								if (nilai > 0.1) {
									jumlahVertical += nilai;
								}
								nilais.put(nilaikelas, jumlahVertical);

								if (nilai > 0.1) {
									total += nilai;
									jumlah++;
								}
							}
						}

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, total);

						double r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, r);

						rowIndex++;

					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Total");

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "");
					index = 6;

					Double total = 0.0;
					Double jumlah = 0.0;

					for (String nilaikelas : myPaket.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {
							Double jumlahVertical = nilais.get(nilaikelas);
							if (jumlahVertical == null) {
								jumlahVertical = 0.0;
							}
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, jumlahVertical);

							if (jumlahVertical > 0.1) {
								total += jumlahVertical;
								jumlah++;
							}
						}
					}
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, total);
					double r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, r);

					rowIndex++;

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Rata-Rata");

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "");

					index = 6;
					int jml = nilais.size();
					total = 0.0;
					jumlah = 0.0;

					for (String nilaikelas : myPaket.getKelasVerifikasiRapor().split(";")) {
						if (!nilaikelas.trim().isEmpty()) {
							Double jumlahVertical = nilais.get(nilaikelas);
							if (jumlahVertical == null) {
								jumlahVertical = 0.0;
							}
							r = jml == 0 ? 0.0 : jumlahVertical / jml;
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, r);

							if (r > 0.1) {
								total += r;
								jumlah++;
							}
						}
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, total);
					r = jumlah.intValue() == 0 ? 0.0 : total / jumlah;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++index, r);

					rowIndex++;
				}

				Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

			}
		});

	}
}
