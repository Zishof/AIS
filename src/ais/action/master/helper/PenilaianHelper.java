package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Tbmuser;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk mengedit nilai satu baris {@link Detailperkuliahan} (satu mata kuliah yang
 * diambil mahasiswa) berdasarkan komponen-komponen {@link FormatNilai} yang berlaku pada
 * {@link Matakuliah} terkait (mis. Tugas, UTS, UAS masing-masing dengan bobot persentase).
 * Jendela menampilkan satu baris input angka per komponen format nilai; saat disimpan,
 * {@link #save()} menghitung nilai total terbobot, menentukan {@link NilaiHuruf} (huruf mutu dan
 * nilai IP) yang sesuai lewat {@code Common#getNilaiHuruf}, dan menuliskan hasilnya baik ke kolom
 * nilai "final" ({@code totalNilai}/{@code nilaiHuruf}/{@code totalIP}) maupun kolom "sementara"
 * ({@code totalNilaiSementara}/{@code nilaiHurufSementara}/{@code totalIPSementara}) pada
 * {@link Detailperkuliahan}.
 */
public class PenilaianHelper {

	private Detailperkuliahan detailperkuliahan;
	private MyGrid grid;

	/**
	 * Membaca nilai tiap komponen {@link FormatNilai} dari baris grid (kolom ketiga tiap baris,
	 * {@link MyDoublebox}), menyimpannya lewat
	 * {@link Detailperkuliahan#populateDetailNilai(FormatNilai, Object, Double, boolean, boolean, Tbmuser)}
	 * per komponen, lalu menghitung total nilai terbobot ({@code jumlah * persen/100} dijumlahkan
	 * antar komponen). Total ini dipetakan ke {@link NilaiHuruf} (huruf mutu, status lulus, dan nilai
	 * IP) lewat {@code Common#getNilaiHuruf}, dan hasilnya ditulis ke kolom nilai final maupun
	 * sementara pada {@link #detailperkuliahan} sebelum disimpan ke database.
	 */
	@SuppressWarnings("unchecked")
	public void save() {
		Tbmuser tbmuser = Common.getCurrentUser();
		Session session = HibernateUtil.currentSession();
		Rows rows = grid.getRows();
		Double total = 0.0;
		for (Component row : ((List<Component>) rows.getChildren())) {
			FormatNilai formatNilai = (FormatNilai) row.getAttribute("myValue");

			MyDoublebox doublebox = (MyDoublebox) row.getChildren().get(2);
			Double jumlah = doublebox.getValue();
			detailperkuliahan
					.populateDetailNilai(formatNilai, null, jumlah, true,
							detailperkuliahan.getPerkuliahan() == null ? false
									: detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi(),
							tbmuser);
			total += (jumlah * (formatNilai.getPersen() / 100.0));
		}

		Matakuliah matakuliah = detailperkuliahan == null ? null
				: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
						: detailperkuliahan.getMatakuliahKonversi();

		NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		if (nilaiHuruf != null) {
			detailperkuliahan.setTotalIP(nilaiHuruf.getNilaiDiIPK());
			detailperkuliahan.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
			detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
		}
		detailperkuliahan.setTotalNilai(total);

		Double totalSementara = total;
		nilaiHuruf = Common.getNilaiHuruf(totalSementara, detailperkuliahan.getMahasiswa().getTahunangkatan(),
				detailperkuliahan.getMahasiswa().getJurusan(),
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas(), detailperkuliahan.getTahunAkademik(),
				detailperkuliahan.getPerkuliahan() == null ? null : detailperkuliahan.getPerkuliahan().getGanjilGenap(),
				matakuliah == null ? "" : matakuliah.getKode(),
				matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

		detailperkuliahan.setTotalNilaiSementara(totalSementara);
		detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
		detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

		Common.refreshUpdate(session, detailperkuliahan);
	}

	/**
	 * Membangun jendela edit nilai: satu baris per {@link FormatNilai} yang berlaku bagi mata kuliah
	 * pada {@code detailperkuliahan} (diambil lewat {@code Common#getFormatNilais}), dipra-isi dari
	 * nilai komponen yang sudah tersimpan ({@code detailperkuliahan.retreiveDetailNilai}). Tombol
	 * Simpan memanggil {@link #save()} lalu menyegarkan tampilan pemanggil lewat {@code dataLoader}.
	 *
	 * @param detailperkuliahan baris KRS/nilai yang akan diedit
	 * @param window             jendela ({@link MyWindow}) yang dipakai ulang untuk menampilkan layar ini
	 * @param dataLoader         dipanggil setelah simpan untuk menyegarkan tampilan pemanggil
	 */
	public void display(final Detailperkuliahan detailperkuliahan, final MyWindow window, final DataLoader dataLoader) {
		this.detailperkuliahan = detailperkuliahan;
		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");

		Common.clear(window);
		window.setTitle("Edit Nilai");
		window.setWidth("450px");
		window.setHeight("150px");

		// ais.ui.util.MyDiv groupbox = new
		// ais.ui.util.MyDiv();groupbox.setStyle("min-height:
		// 200px;");
		// groupbox.setStyle("min-height: 200px;");
		// groupbox.setWidth("100%");
		// groupbox.setParent(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				save();
				window.setVisible(false);
				dataLoader.loadData(null);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Rows rows = new Rows();
		rows.setParent(grid);

		List<FormatNilai> formatNilais = Common.getFormatNilais(HibernateUtil.currentSession(),
				detailperkuliahan.getPerkuliahan());

		for (FormatNilai formatNilai : formatNilais) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			// row.setId(formatNilai.getId() + "");
			row.setValign("top");
			row.setAttribute("myValue", formatNilai);
			row.setParent(rows);
			new Label(formatNilai.getStatusPertemuan() == null ? "" : formatNilai.getNama()).setParent(row);
			new Label(formatNilai.getPersen() + " %").setParent(row);

			Double n = detailperkuliahan.retreiveDetailNilai(formatNilai);
			new MyDoublebox(n).setParent(row);
		}

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

	}

}
