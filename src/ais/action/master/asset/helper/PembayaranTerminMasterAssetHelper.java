package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.PembayaranTerminMasterAsset;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK modul asset yang membangun grid pembayaran termin tagihan barang/jasa
 * ({@link PembayaranTerminMasterAsset}/{@link PembayaranTerminMasterAssetDetail}) untuk satu
 * {@link PenyediaAsset} (vendor): menampilkan setiap {@link PemesananPengadaanMasterAsset}
 * bertermin ({@code byTermin=true}) yang sudah disetujui, memilih termin tagihan yang belum dibayar
 * (dicocokkan via kunci {@code key} pada JSON formula pesanan terhadap daftar
 * {@code keysTagihan} yang sudah tertagih sebelumnya), menghitung nilai dibayar/pinalti/PPh, dan
 * menyimpan perubahan secara langsung (autosave per-sel) ke database.
 *
 * <p>
 * Alur data ganda: bila {@code pembayaranTerminMasterAsset} sudah tersimpan (punya id), baris
 * diambil dari {@link PembayaranTerminMasterAssetDetail} yang sudah ada (dan
 * {@code totalTelahDibayar}/{@code dibayar} pada pesanan induk disegarkan ulang); bila belum
 * tersimpan (form baru), baris dibangun on-the-fly dari seluruh pesanan bertermin milik vendor yang
 * belum lunas. Sel "Pilih Termin" memakai combobox berisi opsi termin yang sudah "setuju" tapi
 * belum tertagih; memilih satu opsi otomatis mengisi nilai dibayar dan pinalti dari data JSON
 * termin tersebut. Setelah form disetujui ({@code disetujuiOleh != null}), seluruh sel berubah
 * menjadi label read-only.
 * </p>
 */
public class PembayaranTerminMasterAssetHelper {

	private MyGrid gridPenerimaanTerminMasterAsset;
	private boolean edit = false;
	private PenyediaAsset penyediaAsset = null;
	private double totalDibayar = 0.0;
	private Footer footerTotalDibayar;
	private PembayaranTerminMasterAsset pembayaranTerminMasterAsset;
	private boolean setujui = false;

	/** Membungkus {@code gridPenerimaanTerminMasterAsset} sebagai grid target yang akan diisi oleh {@link #initDetail}. */
	public PembayaranTerminMasterAssetHelper(MyGrid gridPenerimaanTerminMasterAsset) {
		this.gridPenerimaanTerminMasterAsset = gridPenerimaanTerminMasterAsset;

	}

	/**
	 * Membangun kerangka grid termin (kolom Termin/Pajak/Tagihan/Tertagih/Sisa/Pinalti/Nilai
	 * Dibayar/Nominal PPh/Keterangan, dengan footer total nilai dibayar) di dalam {@link Groupbox}
	 * baru, lalu memuat isinya lewat {@link #loadDataDetail}. Kolom "Nilai Dibayar" disembunyikan
	 * (lebar 0) bila mode {@code setujui} atau data sudah disetujui — nilai lalu hanya ditampilkan
	 * sebagai label pada {@link #initRow}. Field {@link #edit} ditentukan dari status persetujuan
	 * ({@code true} hanya bila belum disetujui).
	 *
	 * @param pembayaranTerminMasterAsset entitas induk pembayaran termin
	 * @param setujui                     {@code true} bila grid dibuka dalam mode persetujuan (read-only nilai dibayar)
	 * @return groupbox berisi grid yang sudah terisi
	 */
	public Groupbox initDetail(final PembayaranTerminMasterAsset pembayaranTerminMasterAsset, boolean setujui)
			throws Exception {
		this.pembayaranTerminMasterAsset = pembayaranTerminMasterAsset;
		this.setujui = setujui;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Termin Tagihan Barang/Jasa"));

		edit = pembayaranTerminMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setDisabled(
				pembayaranTerminMasterAsset.getId() != null && pembayaranTerminMasterAsset.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(pembayaranTerminMasterAsset);
			}
		});

		Columns columns = new Columns();
		columns.setParent(gridPenerimaanTerminMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Termin");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pajak");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tagihan");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tertagih");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sisa");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pinalti");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Dibayar");

		column.setWidth((setujui
				|| (pembayaranTerminMasterAsset != null && pembayaranTerminMasterAsset.getDisetujuiOleh() != null))
						? "0px"
						: "12%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nominal PPh");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		gridPenerimaanTerminMasterAsset.setParent(myGroupboxStyled);
		gridPenerimaanTerminMasterAsset.setWidth("100%");
		gridPenerimaanTerminMasterAsset.setHeight("100%");
		gridPenerimaanTerminMasterAsset.setStyle("min-height:350px");
		gridPenerimaanTerminMasterAsset.setMold("paging");
		gridPenerimaanTerminMasterAsset.setPageSize(10);
		gridPenerimaanTerminMasterAsset.getPagingChild().setMold("os");

		Foot foot = new Foot();
		foot.setParent(gridPenerimaanTerminMasterAsset);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footerTotalDibayar = new Footer(Common.numberFormat.get().format(totalDibayar));
		foot.appendChild(footerTotalDibayar);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		loadDataDetail(pembayaranTerminMasterAsset);

		return myGroupboxStyled;
	}

	/**
	 * Mengisi ulang baris grid untuk {@link #penyediaAsset} saat ini: (1) mengumpulkan
	 * {@link #keysTagihan} — kunci termin vendor yang sudah pernah ditagih di baris
	 * {@link PembayaranTerminMasterAssetDetail} manapun; (2) bila {@code pembayaranTerminMasterAsset}
	 * sudah tersimpan, memuat detail yang ada dan menyegarkan
	 * {@code totalTelahDibayar}/{@code dibayar} pesanan induknya dari jumlah aktual yang tertagih;
	 * (3) bila belum tersimpan, membangun daftar detail baru dari seluruh
	 * {@link PemesananPengadaanMasterAsset} bertermin yang sudah disetujui dan masih bersaldo untuk
	 * vendor tersebut. Setiap perubahan dijalankan dalam transaksi Hibernate eksplisit dengan
	 * rollback saat gagal. Tidak melakukan apa pun bila {@link #penyediaAsset} belum diset.
	 */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PembayaranTerminMasterAsset pembayaranTerminMasterAsset) throws Exception {

		if (pembayaranTerminMasterAsset.getPenyedia() != null) {
			penyediaAsset = pembayaranTerminMasterAsset.getPenyedia();
		}

		Rows rows = gridPenerimaanTerminMasterAsset.getRows() == null ? new Rows()
				: gridPenerimaanTerminMasterAsset.getRows();
		rows.setParent(gridPenerimaanTerminMasterAsset);
		Common.clear(rows);
		if (penyediaAsset == null) {
			return;
		}

		Session session = null;
		List<String> vendorTermin;
		try {
			session = HibernateUtil.currentNativeSession();
			vendorTermin = session.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.setProjection(Projections.property("tagihan")).add(Restrictions.isNotNull("tagihan"))
					.createAlias("pembayaranTerminMasterAsset", "pembayaranTerminMasterAsset")
					.add(Restrictions.eq("pembayaranTerminMasterAsset.penyedia", penyediaAsset)).list();
		} finally {
			Common.closeOpenedSession(session);
		}

		keysTagihan = new ArrayList<String>();

		for (String tagihan : vendorTermin) {
			try {
				JSONObject jsonObject = new JSONObject(tagihan);
				String key = null;
				if (jsonObject == null || jsonObject.isNull("key")) {
					continue;
				} else {
					key = jsonObject.get("key") + "";
				}

				keysTagihan.add(key);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/PembayaranTerminMasterAssetHelper.java:229");
			}
		}
		vendorTermin = null;

		List<PembayaranTerminMasterAssetDetail> pembayaranTerminMasterAssetDetails;
		if (pembayaranTerminMasterAsset.getId() != null) {
			Transaction tx = null;
			try {
				session = HibernateUtil.currentNativeSession();
				tx = session.beginTransaction();

				pembayaranTerminMasterAssetDetails = session.createCriteria(PembayaranTerminMasterAssetDetail.class)
						.add(Restrictions.eq("pembayaranTerminMasterAsset", pembayaranTerminMasterAsset)).list();

				for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {

					PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranTerminMasterAssetDetail
							.getPemesananPengadaanMasterAsset();
					if (pemesananPengadaanMasterAsset != null) {
						session.refresh(pemesananPengadaanMasterAsset);
						Number nilaiTagihan = (Number) session.createCriteria(PembayaranTerminMasterAssetDetail.class)
								.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
								.setProjection(Projections.sum("dibayar")).uniqueResult();
						Double d = nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue();

						System.out.println("pemesananPengadaanMasterAsset " + pemesananPengadaanMasterAsset
								+ " nilaiTagihan -> " + nilaiTagihan);

						pembayaranTerminMasterAssetDetail.setTotalTelahDibayar(d);

						/* Pakai session transaksi ini secara eksplisit; varian
						 * tanpa argumen memakai session thread-local yang sama
						 * dan dulu ikut menutupnya sebelum tx.commit(). */
						Double dibayar = pemesananPengadaanMasterAsset.hitungDibayar(session);

						if (dibayar.intValue() != pemesananPengadaanMasterAsset.getDibayar().intValue()) {
							pemesananPengadaanMasterAsset.setDibayar(dibayar);
							Common.refreshUpdate(session, pemesananPengadaanMasterAsset, false);
						}

						Common.refreshUpdate(session, pembayaranTerminMasterAssetDetail, false);
					}
				}
				if (session != null && session.isOpen()) {
					tx.commit();
				}
			} catch (Exception e) {
					try {
						if (tx != null && tx.isActive() && session != null && session.isOpen()) {
							tx.rollback();
						}
					} catch (Exception rollbackException) { ais.common.ErrorAuditUtil.record(rollbackException, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PembayaranTerminMasterAssetHelper.java:281");
					}
				throw e;
			} finally {
				Common.closeOpenedSession(session);
			}
		} else {

			pembayaranTerminMasterAssetDetails = new ArrayList<PembayaranTerminMasterAssetDetail>();

			Transaction tx = null;
			try {
				session = HibernateUtil.currentNativeSession();
				tx = session.beginTransaction();
				List<PemesananPengadaanMasterAsset> pemesananPengadaanMasterAssets = session
						.createCriteria(PemesananPengadaanMasterAsset.class).add(Restrictions.isNotNull("disetujuiOleh"))
						.add(Restrictions.eq("byTermin", true)).add(Restrictions.gt("nilai", 0.1))
						.add(Restrictions.eq("penyedia", penyediaAsset)).list();

				totalDibayar = 0.0;
				for (PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset : pemesananPengadaanMasterAssets) {

					PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail = new PembayaranTerminMasterAssetDetail();
					pembayaranTerminMasterAssetDetail.setDibayar(0.0);
					pembayaranTerminMasterAssetDetail.setPembayaranTerminMasterAsset(pembayaranTerminMasterAsset);
					pembayaranTerminMasterAssetDetail.setPemesananPengadaanMasterAsset(pemesananPengadaanMasterAsset);

					Number nilaiTagihan = (Number) session.createCriteria(PembayaranTerminMasterAssetDetail.class)
							.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
							.setProjection(Projections.sum("dibayar")).uniqueResult();
					Double d = nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue();
					pembayaranTerminMasterAssetDetail.setTotalTelahDibayar(d);
					if (d.intValue() != pemesananPengadaanMasterAsset.getDibayar().intValue()) {
						pemesananPengadaanMasterAsset.setDibayar(pembayaranTerminMasterAssetDetail.getTotalTelahDibayar());
						Common.refreshUpdate(session, pemesananPengadaanMasterAsset, false);
					}

					totalDibayar += pembayaranTerminMasterAssetDetail.getDibayar();

					pembayaranTerminMasterAssetDetails.add(pembayaranTerminMasterAssetDetail);
				}
				tx.commit();
			} catch (Exception e) {
					try {
						if (tx != null && tx.isActive() && session != null && session.isOpen()) {
							tx.rollback();
						}
					} catch (Exception rollbackException) { ais.common.ErrorAuditUtil.record(rollbackException, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PembayaranTerminMasterAssetHelper.java:328");
					}
				throw e;
			} finally {
				Common.closeOpenedSession(session);
			}
		}

		for (PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail : pembayaranTerminMasterAssetDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, pembayaranTerminMasterAssetDetail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	private List<String> keysTagihan;

	/** Menghitung ulang {@link #totalDibayar} dari seluruh baris grid yang sedang terlihat (dijumlahkan dari atribut {@code pembayaranTerminMasterAssetDetail} tiap baris) dan memperbarui label footer {@link #footerTotalDibayar}. Dipicu setiap kali sel nilai dibayar/pilihan termin berubah. */
	private EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridPenerimaanTerminMasterAsset.getRows().getChildren();

			totalDibayar = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail = (PembayaranTerminMasterAssetDetail) row
							.getAttribute("pembayaranTerminMasterAssetDetail");

					totalDibayar += pembayaranTerminMasterAssetDetail.getDibayar();
				}
			}

			footerTotalDibayar.setLabel(Common.numberFormat.get().format(totalDibayar));

		}
	};

	/**
	 * Merender satu baris grid untuk {@code pembayaranTerminMasterAssetDetail}: identitas
	 * pesanan/termin (combobox pilihan termin yang belum tertagih untuk baris baru, atau label
	 * termin tersimpan untuk baris lama), kolom pajak (combobox {@link JenisPajakBarang} yang
	 * menghitung ulang Nominal PPh saat berubah), kolom tagihan/tertagih/sisa/pinalti (dari
	 * {@code getNilai()}/{@code getDibayar()}/{@code getDptotal()} pesanan induk), sel nilai dibayar
	 * (editable {@link MyDoublebox} atau label read-only tergantung status persetujuan), dan
	 * keterangan. Setiap perubahan sel langsung disimpan (jika detail sudah punya id) lewat
	 * {@code Common.refreshUpdate} dan memicu {@link #eventListenerHitungUlang}.
	 */
	public void initRow(final Row rowData, final PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail)
			throws Exception {
		boolean persetujuan = pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset()
				.getDisetujuiOleh() != null || !edit;

		rowData.setValign("top");
		rowData.setAttribute("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail);
		final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranTerminMasterAssetDetail
				.getPemesananPengadaanMasterAsset();
		PembayaranTerminMasterAsset pembayaranTerminMasterAsset = pembayaranTerminMasterAssetDetail
				.getPembayaranTerminMasterAsset();

		JSONObject jsonObjectDataTermin = pembayaranTerminMasterAssetDetail.getTagihan() == null ? null
				: new JSONObject(pembayaranTerminMasterAssetDetail.getTagihan());

		new PemesananPengadaanMasterAssetDetailAction(pemesananPengadaanMasterAsset,
				jsonObjectDataTermin == null || jsonObjectDataTermin.isNull("key") ? null
						: jsonObjectDataTermin.get("key") + "")
				.setParent(rowData);

		Vbox myvbox = new Vbox();
		myvbox.setParent(rowData);

		Double telahDibayar = pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getDibayar()
				+ pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getDptotal();
		Double termin = pemesananPengadaanMasterAsset == null ? 0.0 : pemesananPengadaanMasterAsset.getNilai();

		Double sisa = termin - telahDibayar;

		final MyCheckboxConfig pilih;
		pilih = new MyCheckboxConfig(pemesananPengadaanMasterAsset.getKode());
		pilih.setChecked(pembayaranTerminMasterAssetDetail.getPilih());
		rowData.setValign("top");
		rowData.setAttribute("pilih", pilih);

		if (pembayaranTerminMasterAssetDetail.getId() != null || sisa.intValue() == 0) {
			RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class, pemesananPengadaanMasterAsset,
					pemesananPengadaanMasterAsset.getKode()).setParent(myvbox);
		} else {
			myvbox.appendChild(pilih);
		}

		final Label pinaltiBox = new MyLabelKecil(
				Common.numberFormat.get().format(pembayaranTerminMasterAssetDetail.getPinalti()));

		final MyDoublebox dibayar = new MyDoublebox(pembayaranTerminMasterAssetDetail.getDibayar());
		JenisPajakBarang jenisPajakBarang = null;

		JSONObject jsonObjectData = null;
		int indexArray = 0;
		final JSONArray array = new JSONArray(pemesananPengadaanMasterAsset.getFormula());

		if (pembayaranTerminMasterAssetDetail.getId() != null) {

			if (jsonObjectDataTermin != null) {
				try {
					String nama = "";

					if (!jsonObjectDataTermin.isNull("nama")) {
						nama = jsonObjectDataTermin.get("nama") + "";
					}

					String nomor = "";

					if (!jsonObjectDataTermin.isNull("nomor")) {
						nomor = jsonObjectDataTermin.get("nomor") + "";
					}
					Label jumlah = new Label(nomor + " " + nama);
					(jumlah).setParent(myvbox);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/PembayaranTerminMasterAssetHelper.java:439");
					// TODO: handle exception
				}
			}

			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);

				String key = null;
				if (jsonObject.isNull("key")) {
					continue;
				} else {
					key = jsonObject.get("key") + "";
				}

				if (!keysTagihan.contains(key)) {

					jsonObjectData = jsonObject;
					indexArray = i;

					if (!jsonObject.isNull("pajak")) {
						jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
								Long.parseLong(jsonObject.get("pajak") + ""));
					} else {
						jenisPajakBarang = null;
					}
				}
			}

		} else {
			final Combobox comboboxTagihan = new Combobox();
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);

				String key = null;
				if (jsonObject.isNull("key")) {
					continue;
				} else {
					key = jsonObject.get("key") + "";
				}

				if (!keysTagihan.contains(key)) {

					jsonObjectData = jsonObject;
					indexArray = i;

					if (!jsonObject.isNull("pajak")) {
						jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
								Long.parseLong(jsonObject.get("pajak") + ""));
					} else {
						jenisPajakBarang = null;
					}

					Boolean setuju;
					if (!jsonObject.isNull("setuju")) {
						setuju = Boolean.parseBoolean(jsonObject.get("setuju") + "");
					} else {
						setuju = false;
					}
					if (setuju) {

						String nama = "";

						if (!jsonObject.isNull("nama")) {
							nama = jsonObject.get("nama") + "";
						}

						String nomor = "";

						if (!jsonObject.isNull("nomor")) {
							nomor = jsonObject.get("nomor") + "";
						}

						Double penagihan = 0.0;
						if (!jsonObject.isNull("penagihan")) {
							penagihan = jsonObject.getDouble("penagihan");
						}

						Double pinalti = 0.0;
						if (!jsonObject.isNull("pinalti")) {
							pinalti = jsonObject.getDouble("pinalti");
						}

						Double ppn = 0.0;
						if (!jsonObject.isNull("ppn")) {
							ppn = jsonObject.getDouble("ppn");
						}

						Double total = penagihan + ((ppn / 100.0) * penagihan);

						Comboitem comboitem = new Comboitem(nomor + " " + nama);
						comboitem.setDescription("Nilai termin " + Common.numberFormat.get().format(total)
								+ (pinalti > 0.1 ? ", pinalti " + Common.numberFormat.get().format(pinalti) : ""));
						comboitem.setAttribute("total", total);
						comboitem.setAttribute("penagihan", penagihan);
						comboitem.setAttribute("pinalti", pinalti);
						comboitem.setValue(jsonObject.toString());
						comboboxTagihan.appendChild(comboitem);
					}
				}
			}
			comboboxTagihan.setWidth("95%");
			comboboxTagihan.setReadonly(true);
			myvbox.appendChild(comboboxTagihan);

			comboboxTagihan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pembayaranTerminMasterAssetDetail.setPilih(pilih.isChecked());
					Comboitem comboitem = comboboxTagihan.getSelectedItem();

					Session session = HibernateUtil.currentSession();
					pembayaranTerminMasterAssetDetail.setTagihan(
							comboitem == null || comboitem.getValue() == null ? null : comboitem.getValue().toString());

					if (pembayaranTerminMasterAssetDetail.getTagihan() != null) {
						try {
							JSONObject jsonObject = pembayaranTerminMasterAssetDetail.getTagihan() == null ? null
									: new JSONObject(pembayaranTerminMasterAssetDetail.getTagihan());

							final Double penagihan;
							if (!jsonObject.isNull("penagihan")) {
								penagihan = jsonObject.getDouble("penagihan");
							} else {
								penagihan = 0.0;
							}

							Double pinalti = 0.0;
							if (!jsonObject.isNull("pinalti")) {
								pinalti = jsonObject.getDouble("pinalti");
							}

							Double ppn = 0.0;
							if (!jsonObject.isNull("ppn")) {
								ppn = jsonObject.getDouble("ppn");
							}

							Double total = penagihan + ((ppn / 100.0) * penagihan);

							Double nilai = Math.abs(total + pinalti);

							dibayar.setValue(nilai);
							dibayar.setDisabled(true);

							pembayaranTerminMasterAssetDetail.setDibayar(nilai);
							pembayaranTerminMasterAssetDetail.setPinalti(pinalti);

							pinaltiBox.setValue(Common.numberFormat.get().format(pinalti));

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/PembayaranTerminMasterAssetHelper.java:590");
						}
					} else {
						dibayar.setValue(0.0);
						dibayar.setDisabled(false);
					}

					rowData.setValign("top");
					rowData.setAttribute("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail);
					if (pembayaranTerminMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (pembayaranTerminMasterAssetDetail));
					}

					eventListenerHitungUlang.onEvent(arg0);
				}
			});

			pilih.addEventListener(Events.ON_CLICK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					dibayar.setDisabled(!pilih.isChecked());
					comboboxTagihan.setDisabled(!pilih.isChecked());

					Double saldo = Math.abs(dibayar.getValue() == null ? 0.0 : dibayar.getValue());
					dibayar.setValue(saldo);
					pembayaranTerminMasterAssetDetail.setPilih(pilih.isChecked());
					pembayaranTerminMasterAssetDetail.setDibayar(saldo);
					if (pembayaranTerminMasterAssetDetail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (pembayaranTerminMasterAssetDetail));
					}
					rowData.setValign("top");
					rowData.setAttribute("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail);

					eventListenerHitungUlang.onEvent(arg0);
				}
			});

			dibayar.setDisabled(!pilih.isChecked());
			comboboxTagihan.setDisabled(!pilih.isChecked());

		}

		// Sel "Pajak": kontrol pilih jenis pajak. Nominal PPh (tarif % x DPP) ditampilkan di KOLOM
		// tersendiri "Nominal PPh" (labelPph di-parent setelah kolom Nilai Dibayar).
		final PembayaranTerminMasterAssetDetail detailPph = pembayaranTerminMasterAssetDetail;
		final MyLabelKecil labelPph = new MyLabelKecil(Common.numberFormat.get().format(
				detailPph.getNilaiPphTermin() == null ? 0.0 : detailPph.getNilaiPphTermin()));
		labelPph.setStyle("text-align:right");
		if (jsonObjectData != null && !setujui) {
			final JSONObject jsonObject = jsonObjectData;
			final int indexData = indexArray;
			final Combobox comboboxPajak = new Combobox();
			Common.insertComboDanSemua(comboboxPajak, new String[] { "nama", "persen" }, "keterangan",
					JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(comboboxPajak, jenisPajakBarang);
			comboboxPajak.setWidth("95%");
			comboboxPajak.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					JenisPajakBarang barang = (JenisPajakBarang) (comboboxPajak.getSelectedItem() == null ? null
							: comboboxPajak.getSelectedItem().getValue());
					jsonObject.put("pajak", barang != null ? barang.getId() : null);

					array.put(indexData, jsonObject);
					pemesananPengadaanMasterAsset.setFormula(array.toString());
					Common.refreshUpdate(pemesananPengadaanMasterAsset);

					// Update kolom Nominal PPh langsung dari tarif pajak terpilih x DPP detail.
					double dppNow = detailPph.getDppTermin() == null ? 0.0 : detailPph.getDppTermin();
					double pphNow = (barang == null || barang.getPersen() == null) ? 0.0
							: (double) Math.round((barang.getPersen() / 100.0) * dppNow);
					labelPph.setValue(Common.numberFormat.get().format(pphNow));
				}
			});
			comboboxPajak.setParent(rowData);
			comboboxPajak.setAttribute("janganDisabled", true);
		} else {
			new Label(jenisPajakBarang == null ? "-" : jenisPajakBarang.getNama()).setParent(rowData);
		}

//		Double nilai = pemesananPengadaanMasterAsset == null ? 0.0 : pemesananPengadaanMasterAsset.getNilai();

		if (pembayaranTerminMasterAssetDetail.getId() != null) {
			RevisiHelper.createNewRevisi(PembayaranTerminMasterAssetDetail.class, pembayaranTerminMasterAssetDetail,
					Common.numberFormat.get().format(Math.round(termin))).setParent(rowData);
		} else {
			new MyLabelKecil(Common.numberFormat.get().format(Math.round(termin))).setParent(rowData);
		}
		new MyLabelKecil(Common.numberFormat.get().format(Math.round(telahDibayar))).setParent(rowData);

		new MyLabelKecil(Common.numberFormat.get().format(Math.round(sisa))).setParent(rowData);

		pinaltiBox.setParent(rowData);

		if (setujui || (pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset() != null
				&& pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset().getDisetujuiOleh() != null)) {
			new MyLabelKecil(Common.numberFormat.get().format(pembayaranTerminMasterAssetDetail.getDibayar()))
					.setParent(rowData);
		} else {
			(dibayar).setParent(rowData);
			dibayar.setDisabled(pembayaranTerminMasterAsset.getDisetujuiOleh() != null || !edit);
			dibayar.setStyle("text-align:right");
			dibayar.setWidth("90%");
			dibayar.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Double saldo = Math.abs(dibayar.getValue() == null ? 0.0 : dibayar.getValue());
					dibayar.setValue(saldo);
					pembayaranTerminMasterAssetDetail.setPilih(pilih.isChecked());
					pembayaranTerminMasterAssetDetail.setDibayar(saldo);
					if (pembayaranTerminMasterAssetDetail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (pembayaranTerminMasterAssetDetail));
					}
					rowData.setValign("top");
					rowData.setAttribute("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail);

					eventListenerHitungUlang.onEvent(arg0);
				}
			});
		}

		// Kolom "Nominal PPh" (setelah Nilai Dibayar, sebelum Keterangan).
		labelPph.setParent(rowData);

		if (persetujuan || setujui) {
			new MyLabelKecil(pembayaranTerminMasterAssetDetail.getKeterangan()).setParent(rowData);
		} else {
			final MyTextbox keterangan = new MyTextbox(pembayaranTerminMasterAssetDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(rowData);
			keterangan.setDisabled(
					pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset().getDisetujuiOleh() != null
							|| !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pembayaranTerminMasterAssetDetail.setKeterangan(keterangan.getValue());

					rowData.setValign("top");
					rowData.setAttribute("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail);
					if (pembayaranTerminMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (pembayaranTerminMasterAssetDetail));
					}
				}
			});
		}

	}

	/** @return vendor ({@link PenyediaAsset}) yang termin tagihannya sedang ditampilkan grid ini. */
	public PenyediaAsset getPenyediaAsset() {
		return penyediaAsset;
	}

	/** Mengganti vendor aktif dan langsung memuat ulang isi grid ({@link #loadDataDetail}) untuk vendor baru tersebut. */
	public void setPenyediaAsset(PenyediaAsset penyediaAsset) {
		this.penyediaAsset = penyediaAsset;
		try {
			loadDataDetail(pembayaranTerminMasterAsset);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/PembayaranTerminMasterAssetHelper.java:758");
		}
	}

}
