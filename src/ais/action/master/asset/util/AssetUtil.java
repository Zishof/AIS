package ais.action.master.asset.util;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.StatusAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AssetUtil {

	public static StatusAsset AKTIF = null;
	public static StatusAsset TIDAK_AKTIF = null;

	static {
		Session session = HibernateUtil.currentNativeSession();
		try {
			AKTIF = (StatusAsset) session.createCriteria(StatusAsset.class).add(Restrictions.eq("nama", "Aktif"))
					.setMaxResults(1).uniqueResult();
			if (AKTIF == null) {
				AKTIF = new StatusAsset();
				AKTIF.setKeterangan("Aktif");
				AKTIF.setNama("Aktif");
				session.getTransaction().begin();
				session.save(AKTIF);
				session.getTransaction().commit();
			}

			TIDAK_AKTIF = (StatusAsset) session.createCriteria(StatusAsset.class)
					.add(Restrictions.eq("nama", "Tidak Aktif")).setMaxResults(1).uniqueResult();
			if (TIDAK_AKTIF == null) {
				TIDAK_AKTIF = new StatusAsset();
				TIDAK_AKTIF.setKeterangan("Tidak Aktif");
				TIDAK_AKTIF.setNama("Tidak Aktif");
				session.getTransaction().begin();
				session.save(TIDAK_AKTIF);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/util/AssetUtil.java:58");
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public static Akun ambilDataAkun(String akunStr, SatuanKerja satuanKerjaData) throws Exception {
		Akun akunDefault = null;
		JSONArray array = new JSONArray(akunStr);
		for (int i = 0; i < array.length(); i++) {
			JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {
				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

				SatuanKerja satuanKerja = (SatuanKerja) (jsonObject.isNull("satuanKerja") ? null
						: ConstantValues.ambil(SatuanKerja.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"satuanKerja")));

				if (satuanKerja == null && akunBiaya != null) {
					akunDefault = akunBiaya;
				}

				if (akunBiaya != null) {

					if (satuanKerjaData == null && satuanKerja == null) {
						return akunBiaya;
					} else if (satuanKerjaData == null || (satuanKerja != null && satuanKerjaData != null
							&& satuanKerjaData.getId().equals(satuanKerja.getId()))) {
						return akunBiaya;
					}
				}
			}
		}

		return akunDefault;
	}

	public static void reloadFormula(final Row rowFormula, final JSONArray array, final boolean edit) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		if (edit) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Akun", "/img/svg/addthis.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					JSONObject jsonObject = new JSONObject();
					Long key = Math.abs(Common.randLong());
					jsonObject.put("key", key);

					array.put(jsonObject);

					reloadDataFormula(rowU, array, edit);
				}
			});
			button.setParent(rowFormula);
		}
		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, edit);

	}

	public static void reloadDataFormula(final Row rowU, final JSONArray array, final boolean edit) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Akun");
		column.setParent(columns);
		column.setWidth("45%");

		column = new MyColumnConfig("Satuan Kerja");
		column.setParent(columns);
		column.setWidth("45%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {

				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

				SatuanKerja satuanKerja = (SatuanKerja) (jsonObject.isNull("satuanKerja") ? null
						: ConstantValues.ambil(SatuanKerja.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"satuanKerja")));

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final AmbilDataAkunBanbox akunBanbox = new AmbilDataAkunBanbox(false);
				akunBanbox.setAttribute("akun", akunBiaya);
				akunBanbox.setValue(akunBiaya == null ? "" : akunBiaya.toString());
				akunBanbox.setWidth("95%");
				if (!edit) {
					row.appendChild(
							new Label(akunBiaya == null ? "" : akunBiaya.getKode() + "-" + akunBiaya.getNama()));
				} else {
					row.appendChild(akunBanbox);
				}
				final AmbilDataSatuanKerjaBanbox satuanKerjaBanbox = new AmbilDataSatuanKerjaBanbox(true);
				satuanKerjaBanbox.setAttribute("satuanKerja", satuanKerja);
				satuanKerjaBanbox.setValue(satuanKerja == null ? "" : satuanKerja.getNama());
				satuanKerjaBanbox.setWidth("95%");

				if (!edit) {
					row.appendChild(new Label(satuanKerja == null ? "" : satuanKerja.getNama()));
				} else {
					row.appendChild(satuanKerjaBanbox);
				}

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Akun akunPilih = (Akun) akunBanbox.getAttribute("akun");
						SatuanKerja satuanKerjaPilih = (SatuanKerja) satuanKerjaBanbox.getAttribute("satuanKerja");
						jsonObject.put("akun", akunPilih == null ? null : akunPilih.getId());
						jsonObject.put("satuanKerja", satuanKerjaPilih == null ? null : satuanKerjaPilih.getId());

					}
				};

				akunBanbox.setEventListener(eventListener);
				satuanKerjaBanbox.setEventListener(eventListener);

				if (edit) {
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {
													array.put(index, new JSONObject());

													reloadDataFormula(rowU, array, edit);

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

												}

											}

										}
									});

						}
					});

					button.setParent(row);
				} else {
					new Label().setParent(row);
				}
			}
		}
	}

}
