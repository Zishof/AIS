package ais.action.master.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Versi <b>SEKOLAH</b> dari {@code ProsesKehadiranDosen}: alat cetak laporan PDF
 * <b>kehadiran mengajar GURU</b> berbasis JadwalPelajaran/Pertemuan/MataPelajaran.
 *
 * <p>
 * Form filter ditata satu kolom (lega) memakai konteks sekolah (Yayasan/Sekolah/
 * Tahun Ajaran/Guru/Kelas/Mata Pelajaran/Jenis Semester/rentang tanggal). Tombol
 * "Proses" membangun data lewat
 * {@link CommonReportHelper#generateParameterMapAbsensiRinciGuru} (SEMUA jenis
 * pertemuan disertakan — tanpa pilihan CPMK gaya perkuliahan) lalu mencetak
 * template Jasper {@code Kehadiran_Dosen}; PDF tampil sebagai modal otomatis.
 * </p>
 */
public class ProsesKehadiranGuru extends MyWindow {

	private static final long serialVersionUID = 790038368339375118L;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private Combobox tahunAjaran = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private ais.action.master.sekolah.helper.AmbilDataGuruBanbox searchGuru = new ais.action.master.sekolah.helper.AmbilDataGuruBanbox();

	private Textbox kelas = new Textbox();
	private Textbox matapelajaran = new Textbox();
	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();

	public ProsesKehadiranGuru() {
		super();
		try {
			init();
			initYayasan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"menampilkan jendela proses kehadiran guru",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba buka jendela kembali.",
							"Periksa koneksi jaringan Anda ke server aplikasi.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public ProsesKehadiranGuru(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initYayasan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"menampilkan jendela proses kehadiran guru",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba buka jendela kembali.",
							"Periksa koneksi jaringan Anda ke server aplikasi.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initYayasan() {
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);

		Div wrap = new Div();
		wrap.setStyle("padding:20px 24px;overflow:auto;height:100%;box-sizing:border-box;font-size:14px;");
		wrap.setParent(this);

		MyLabelConfig judul = new MyLabelConfig("Cetak Laporan Kehadiran Mengajar Guru");
		judul.setStyle("font-size:18px;font-weight:700;color:#1e3a8a;display:block;margin-bottom:4px;");
		judul.setParent(wrap);
		MyLabelConfig sub = new MyLabelConfig("Pilih filter lalu klik \"Proses\" untuk mencetak laporan (PDF).");
		sub.setStyle("color:#64748b;display:block;margin-bottom:16px;");
		sub.setParent(wrap);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("max-width:820px;");
		grid.setParent(wrap);
		Rows rows = new Rows();
		rows.setParent(grid);

		baris(rows, "Yayasan", searchyayasan);
		baris(rows, "Sekolah", searchsekolah);

		tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
		tahunAjaran.setReadonly(true);
		baris(rows, "Tahun Ajaran", tahunAjaran);

		baris(rows, "Guru", searchGuru);
		baris(rows, "Kelas", kelas);
		baris(rows, "Mata Pelajaran", matapelajaran);

		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setReadonly(true);
		Common.selectComboItem(semesterAbsensi,
				Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);
		baris(rows, "Jenis Semester", semesterAbsensi);

		baris(rows, "Mulai", mulai);
		baris(rows, "Sampai", sampai);

		MyFormRow rowTombol = new MyFormRow();
		rowTombol.setParent(rows);
		rowTombol.appendChild(new MyLabelConfig(" "));
		Toolbar toolbar = new Toolbar();
		rowTombol.appendChild(toolbar);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.setStyle("font-size:14px;font-weight:600;");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				proses();
			}
		});
		print.setParent(toolbar);
	}

	/** Satu baris form: label di kiri + kontrol lebar penuh di kanan. */
	private void baris(Rows rows, String label, HtmlBasedComponent field) {
		MyFormRow row = new MyFormRow();
		row.setValign("middle");
		row.setParent(rows);
		MyLabelConfig l = new MyLabelConfig(label);
		l.setStyle("font-size:14px;");
		row.appendChild(l);
		row.appendChild(field);
		field.setWidth("100%");
		try {
			field.setStyle("min-width:320px;");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesKehadiranGuru.java:164");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void proses() {
		try {
			if (tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null) {
				ais.ui.util.MyMessageboxConfig.show("Mohon maaf, Tahun Ajaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih Tahun Ajaran dari daftar yang tersedia; (2) pastikan data Tahun Ajaran sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.");
				return;
			}

			Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
					|| searchyayasan.getSelectedItem().getValue() == null ? null
							: searchyayasan.getSelectedItem().getValue());
			Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
					|| searchsekolah.getSelectedItem().getValue() == null ? null
							: searchsekolah.getSelectedItem().getValue());
			Guru guru = (Guru) searchGuru.getAttribute("guru");
			String thn = tahunAjaran.getSelectedItem().getValue().toString();
			String jenisSemester = semesterAbsensi.getSelectedItem() == null
					|| semesterAbsensi.getSelectedItem().getValue() == null ? null
							: semesterAbsensi.getSelectedItem().getValue().toString();

			// Tanpa pilihan jenis pertemuan (CPMK) → daftar kosong = SEMUA jenis disertakan.
			List<Long> statusPertemuans = new ArrayList<Long>();

			List<Map<String, Serializable>> maps = CommonReportHelper.generateParameterMapAbsensiRinciGuru(yayasan,
					sekolah, kelas.getValue() == null ? null : kelas.getValue().trim(), thn, jenisSemester, guru,
					matapelajaran.getValue() == null ? null : matapelajaran.getValue().trim(), mulai.getValue(),
					sampai.getValue(), Boolean.TRUE, Boolean.FALSE, statusPertemuans);

			if (maps == null || maps.isEmpty()) {
				ais.ui.util.MyMessageboxConfig
						.show("Tidak ada data kehadiran mengajar guru untuk filter yang dipilih.");
				return;
			}

			Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
			parameters.put("maps", maps);

			Report.generatePDFReport(Report.PDF, parameters, "Kehadiran_Dosen", ais.ui.util.WaktuUtil.getDate(), maps);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"mencetak laporan kehadiran mengajar guru",
					e, new String[] {
							"Periksa kembali filter (yayasan/sekolah/rentang tanggal) yang dipilih.",
							"Muat ulang (refresh) halaman ini lalu coba cetak laporan kembali.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}
}
