package ais.action.master.koperasi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DepositoAroScheduler;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.DepositoRolloverKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h2>DepositoAroKoperasiAction — Pemantauan &amp; Kendali ARO Simpanan Berjangka</h2>
 *
 * <p>
 * Layar ini menampilkan daftar simpanan berjangka (deposito) koperasi beserta status
 * <b>perpanjangan otomatisnya (ARO)</b>: tanggal jatuh tempo berjalan, berapa kali sudah
 * diperpanjang, dan apakah ARO menyala. Pengurus dapat menyalakan/mematikan ARO tiap deposito, serta
 * memicu proses ARO secara manual kapan saja (selain berjalan otomatis harian lewat penjadwal). Kartu
 * ringkasan memperlihatkan jumlah dan nilai deposito ber-ARO serta yang menunggu pencairan.
 * </p>
 *
 * <h3>Kaidah teknis</h3>
 * <p>
 * Pembacaan daftar memakai {@link HibernateUtil#currentSession()} (ditutup otomatis). Pemicu manual
 * memanggil {@link DepositoAroScheduler#jalankanSekali()} yang membuka/menutup sesi sendiri sesuai
 * kaidah. Data status ARO dikelola engine {@code DepositoAroHelper}; layar ini hanya memantau dan
 * menyetel. Grafik memakai {@link DashboardUiKit} (HTML/CSS, tanpa JFreeChart). Kompatibel Java 1.7.
 * </p>
 *
 * @see ais.action.master.koperasi.helper.DepositoAroHelper
 * @see DepositoAroScheduler
 */
public class DepositoAroKoperasiAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 6620370014413771010L;

	private static final ThreadLocal<SimpleDateFormat> SDF = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("dd-MM-yyyy");
		}
	};

	private MyGrid grid;
	private Paging paging;
	private org.zkoss.zul.Div ringkasanHost;

	private boolean edit = false;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		edit = ais.common.CommonPrivilages.checkPrevilages(ais.common.CommonPrivilages.UPDATE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/** Peta deposito (id transaksi → transaksi) untuk baris yang sedang ditampilkan. */
	private Map<Long, TransaksiKoperasi> depositoMap = new HashMap<Long, TransaksiKoperasi>();

	class DepositoAroRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DepositoRolloverKoperasi r = (DepositoRolloverKoperasi) arg1;
			TransaksiKoperasi t = r.getTransaksiKoperasiId() == null ? null : depositoMap.get(r.getTransaksiKoperasiId());

			String anggota = "-";
			String produk = "-";
			double nominal = 0.0;
			try {
				if (t != null) {
					anggota = t.getAnggotaKoperasi() == null || t.getAnggotaKoperasi().getNama() == null ? "-"
							: t.getAnggotaKoperasi().getNama();
					produk = t.getProdukKoperasi() == null || t.getProdukKoperasi().getNama() == null ? "-"
							: t.getProdukKoperasi().getNama();
					nominal = t.getNilai() == null ? 0.0 : t.getNilai();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DepositoAroKoperasiAction.java:109");
			}

			new Label(anggota).setParent(arg0);
			new Label(produk).setParent(arg0);
			new Label("Rp " + DashboardUiKit.money(nominal)).setParent(arg0);
			new Label(r.getTanggalJatuhTempo() == null ? "-" : SDF.get().format(r.getTanggalJatuhTempo())).setParent(arg0);
			new Label(String.valueOf(r.getJumlahPerpanjangan()) + "x").setParent(arg0);

			final MyCheckboxConfig cbAro = new MyCheckboxConfig("ARO");
			cbAro.setDisabled(!edit);
			cbAro.setChecked(Boolean.TRUE.equals(r.getAroOtomatis()));
			cbAro.setParent(arg0);
			cbAro.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					r.setAroOtomatis(cbAro.isChecked());
					// bila ARO dinyalakan lagi untuk deposito yang sebelumnya jatuh tempo, jadikan berjalan
					if (cbAro.isChecked() && DepositoRolloverKoperasi.STATUS_JATUH_TEMPO.equals(r.getStatus())) {
						r.setStatus(DepositoRolloverKoperasi.STATUS_BERJALAN);
					}
					Common.refreshSaveOrUpdate(r);
					onSearchDefault(null);
				}
			});

			new Label(labelStatus(r.getStatus())).setParent(arg0);
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (grid == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		List<DepositoRolloverKoperasi> list = session.createCriteria(DepositoRolloverKoperasi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("tanggalJatuhTempo")).list();

		// Muat detail deposito (anggota/produk/nominal) sekaligus untuk baris yang tampil.
		depositoMap = new HashMap<Long, TransaksiKoperasi>();
		List<Long> ids = new ArrayList<Long>();
		for (DepositoRolloverKoperasi r : list) {
			if (r.getTransaksiKoperasiId() != null) {
				ids.add(r.getTransaksiKoperasiId());
			}
		}
		if (!ids.isEmpty()) {
			try {
				List<TransaksiKoperasi> txs = session.createQuery(
						"select t from TransaksiKoperasi t left join fetch t.anggotaKoperasi a "
								+ "left join fetch t.produkKoperasi p where t.id in (:ids)")
						.setParameterList("ids", ids).list();
				for (TransaksiKoperasi t : txs) {
					if (t != null && t.getId() != null) {
						depositoMap.put(t.getId(), t);
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (paging != null) {
			paging.setVisible(false);
		}
		ListModel strset = new SimpleListModel(list);
		grid.setRowRenderer(new DepositoAroRenderer());
		grid.setModelCheckMobile(strset);
		buildRingkasan(list);
	}

	/** Picu proses ARO manual sekarang, lalu segarkan tampilan &amp; laporkan hasilnya. */
	public void onProsesAro(Event event) throws Exception {
		int[] hasil = DepositoAroScheduler.jalankanSekali();
		onSearchDefault(null);
		MyMessageboxConfig.show("Proses ARO selesai. Deposito didaftarkan: " + hasil[0] + ", diperpanjang: " + hasil[1]
				+ ", ditandai jatuh tempo: " + hasil[2] + ".", "Informasi", MyMessageboxConfig.OK,
				MyMessageboxConfig.INFORMATION);
	}

	/** Kartu ringkasan: jumlah &amp; nilai deposito ber-ARO aktif, serta yang menunggu pencairan. */
	private void buildRingkasan(List<DepositoRolloverKoperasi> list) {
		if (ringkasanHost == null) {
			return;
		}
		ringkasanHost.getChildren().clear();
		int jmlAro = 0, jmlTempo = 0;
		double nilaiAro = 0.0, nilaiTempo = 0.0;
		for (DepositoRolloverKoperasi r : list) {
			try {
				TransaksiKoperasi t = r.getTransaksiKoperasiId() == null ? null
						: depositoMap.get(r.getTransaksiKoperasiId());
				double n = t == null || t.getNilai() == null ? 0.0 : t.getNilai();
				if (DepositoRolloverKoperasi.STATUS_JATUH_TEMPO.equals(r.getStatus())) {
					jmlTempo++;
					nilaiTempo += n;
				} else if (Boolean.TRUE.equals(r.getAroOtomatis())) {
					jmlAro++;
					nilaiAro += n;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/DepositoAroKoperasiAction.java:211");
			}
		}
		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Deposito ARO Aktif", DashboardUiKit.money(jmlAro),
				"Rp " + DashboardUiKit.money(nilaiAro), DashboardUiKit.GOOD));
		kartu.add(new DashboardUiKit.Stat("Menunggu Pencairan", DashboardUiKit.money(jmlTempo),
				"Rp " + DashboardUiKit.money(nilaiTempo), DashboardUiKit.WARN));
		ringkasanHost.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
				"Deposito ber-ARO diperpanjang otomatis saat jatuh tempo; sisanya ditandai untuk dicairkan.")));
		ringkasanHost.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));
	}

	/** Tombol toolbar untuk memicu proses ARO manual (dipakai bila diletakkan sebagai listener). */
	public MyToolbarbuttonConfig tombolProses() {
		MyToolbarbuttonConfig b = new MyToolbarbuttonConfig("Proses ARO Sekarang", "/img/refresh.gif");
		b.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onProsesAro(event);
			}
		});
		return b;
	}

	private String labelStatus(String s) {
		if (DepositoRolloverKoperasi.STATUS_JATUH_TEMPO.equals(s)) {
			return "Jatuh Tempo (cairkan)";
		}
		if (DepositoRolloverKoperasi.STATUS_DICAIRKAN.equals(s)) {
			return "Dicairkan";
		}
		return "Berjalan";
	}

	/** Dipertahankan agar konstanta tipe simpanan mudah diakses bila diperlukan pengembangan lanjut. */
	@SuppressWarnings("unused")
	private Long tipeSimpanan() {
		return ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;
	}
}
