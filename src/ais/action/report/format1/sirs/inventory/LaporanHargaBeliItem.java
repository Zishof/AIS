package ais.action.report.format1.sirs.inventory;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Window;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Penyedia;

/**
 * Penyusun/penyaji laporan untuk laporan harga beli item. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox v1},
 * {@code Combobox v2}, {@code Combobox v3}, {@code Combobox v4}, {@code Combobox v5}, {@code Combobox v6},
 * {@code Combobox v7}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetak()}); operasi
 * domain lain ({@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class LaporanHargaBeliItem extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox v1 = new Combobox();
	private Combobox v2 = new Combobox();
	private Combobox v3 = new Combobox();
	private Combobox v4 = new Combobox();
	private Combobox v5 = new Combobox();
	private Combobox v6 = new Combobox();
	private Combobox v7 = new Combobox();
	private Combobox v8 = new Combobox();

	private Combobox jenisBarang;

	public LaporanHargaBeliItem() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanHargaBeliItem.java:59");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Harga Beli Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanHargaBeliItem(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("unchecked")
	private void init() {

		List<Penyedia> penyedias = HibernateUtil.currentSession().createCriteria(Penyedia.class).list();
		Common.insertComboItems(v1, "nama", "alamat", penyedias);
		Common.selectComboItem(v1, penyedias.size() < 1 ? null : penyedias.get(0));

		Common.insertComboItems(v2, "nama", "alamat", penyedias);
		Common.selectComboItem(v2, penyedias.size() < 2 ? null : penyedias.get(1));

		Common.insertComboItems(v3, "nama", "alamat", penyedias);
		Common.selectComboItem(v3, penyedias.size() < 3 ? null : penyedias.get(2));

		Common.insertComboItems(v4, "nama", "alamat", penyedias);
		Common.selectComboItem(v4, penyedias.size() < 4 ? null : penyedias.get(3));

		Common.insertComboItems(v5, "nama", "alamat", penyedias);
		Common.selectComboItem(v5, penyedias.size() < 5 ? null : penyedias.get(4));

		Common.insertComboItems(v6, "nama", "alamat", penyedias);
		Common.selectComboItem(v6, penyedias.size() < 6 ? null : penyedias.get(5));

		Common.insertComboItems(v7, "nama", "alamat", penyedias);
		Common.selectComboItem(v7, penyedias.size() < 7 ? null : penyedias.get(6));

		Common.insertComboItems(v8, "nama", "alamat", penyedias);
		Common.selectComboItem(v8, penyedias.size() < 8 ? null : penyedias.get(7));

		v1.setWidth("90%");
		v2.setWidth("90%");
		v3.setWidth("90%");
		v4.setWidth("90%");
		v5.setWidth("90%");
		v6.setWidth("90%");
		v7.setWidth("90%");
		v8.setWidth("90%");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);

		Div div = new Div();
		div.setParent(north);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);

			}
		};

		v1.addEventListener("onChange", eventListener);
		v2.addEventListener("onChange", eventListener);
		v3.addEventListener("onChange", eventListener);
		v4.addEventListener("onChange", eventListener);
		v5.addEventListener("onChange", eventListener);
		v6.addEventListener("onChange", eventListener);
		v7.addEventListener("onChange", eventListener);
		v8.addEventListener("onChange", eventListener);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 1")));
		row.appendChild(v1);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 2")));
		row.appendChild(v2);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 3")));
		row.appendChild(v3);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 4")));
		row.appendChild(v4);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 5")));
		row.appendChild(v5);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 6")));
		row.appendChild(v6);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 7")));
		row.appendChild(v7);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Supplier 8")));
		row.appendChild(v8);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Item")));
		row.appendChild(jenisBarang = new Combobox());
		Common.insertCombo(jenisBarang, "nama", JenisItem.class);
		jenisBarang.setWidth("90%");
		jenisBarang.addEventListener("onChange", eventListener);
		jenisBarang.setSelectedIndex(0);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/daftar_harga_beli"));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		JenisItem myJenisItem = (JenisItem) (jenisBarang.getSelectedItem() == null ? null
				: jenisBarang.getSelectedItem().getValue());

		Penyedia penyedia1 = (Penyedia) (v1.getSelectedItem() == null ? null : v1.getSelectedItem().getValue());
		Penyedia penyedia2 = (Penyedia) (v2.getSelectedItem() == null ? null : v2.getSelectedItem().getValue());
		Penyedia penyedia3 = (Penyedia) (v3.getSelectedItem() == null ? null : v3.getSelectedItem().getValue());
		Penyedia penyedia4 = (Penyedia) (v4.getSelectedItem() == null ? null : v4.getSelectedItem().getValue());
		Penyedia penyedia5 = (Penyedia) (v5.getSelectedItem() == null ? null : v5.getSelectedItem().getValue());
		Penyedia penyedia6 = (Penyedia) (v6.getSelectedItem() == null ? null : v6.getSelectedItem().getValue());
		Penyedia penyedia7 = (Penyedia) (v7.getSelectedItem() == null ? null : v7.getSelectedItem().getValue());
		Penyedia penyedia8 = (Penyedia) (v8.getSelectedItem() == null ? null : v8.getSelectedItem().getValue());

		Map parameters = new HashMap();
		parameters.put("p_1", penyedia1 == null || penyedia1.getId() == null ? -1L : penyedia1.getId());
		parameters.put("p_2", penyedia2 == null || penyedia2.getId() == null ? -1L : penyedia2.getId());
		parameters.put("p_3", penyedia3 == null || penyedia3.getId() == null ? -1L : penyedia3.getId());
		parameters.put("p_4", penyedia4 == null || penyedia4.getId() == null ? -1L : penyedia4.getId());
		parameters.put("p_5", penyedia5 == null || penyedia5.getId() == null ? -1L : penyedia5.getId());
		parameters.put("p_6", penyedia6 == null || penyedia6.getId() == null ? -1L : penyedia6.getId());
		parameters.put("p_7", penyedia7 == null || penyedia7.getId() == null ? -1L : penyedia7.getId());
		parameters.put("p_8", penyedia8 == null || penyedia8.getId() == null ? -1L : penyedia8.getId());

		parameters.put("jenisItem", myJenisItem == null || myJenisItem.getId() == null ? -1L : myJenisItem.getId());
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {
			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress("sirs/daftar_harga_beli", Report.XLS, parameters, "sirs/daftar_harga_beli",
					new Date(), Sessions.getCurrent().getWebApp());
			CommonReport.tampilkanReportXLS(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanHargaBeliItem.java:254");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Harga Beli Item", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
