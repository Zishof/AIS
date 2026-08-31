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

/**
 * Utilitas statis modul asset: (1) menyediakan konstanta {@link StatusAsset} baku ("Aktif"/"Tidak
 * Aktif", dimuat/dibuat sekali di blok inisialisasi statis kelas); dan (2) membangun/mengurai
 * "formula akun" — daftar pemetaan {@link Akun} biaya per {@link SatuanKerja} yang disimpan sebagai
 * {@link JSONArray} JSON pada suatu entitas (mis. jenis biaya asset), dipakai untuk menentukan akun
 * akuntansi mana yang berlaku untuk satuan kerja tertentu.
 *
 * <p>
 * Elemen array berformat {@code {key, akun, satuanKerja}}; entri dengan {@code satuanKerja} null
 * bertindak sebagai akun default/fallback. {@link #ambilDataAkun} mencari akun yang cocok persis
 * dengan {@code satuanKerjaData} yang diberikan, dan jatuh kembali ke akun default bila tidak ada
 * yang cocok. Entri "dihapus" dengan mengganti elemen array pada indeksnya menjadi
 * {@link JSONObject} kosong (bukan dipangkas dari array), sama seperti pola formula serupa di
 * {@code KursusUtil}.
 * </p>
 */
public class AssetUtil {

	/** Status asset baku "Aktif" (dibuat otomatis bila belum ada). */
	public static StatusAsset AKTIF = null;
	/** Status asset baku "Tidak Aktif" (dibuat otomatis bila belum ada). */
	public static StatusAsset TIDAK_AKTIF = null;

	/** Memuat (atau membuat bila belum ada) {@link #AKTIF} dan {@link #TIDAK_AKTIF} saat kelas ini pertama kali diakses. */
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

	/**
	 * Mencari {@link Akun} yang berlaku untuk {@code satuanKerjaData} dari daftar formula
	 * {@code akunStr} (JSON): mengembalikan entri yang {@code satuanKerja}-nya cocok persis, atau
	 * entri default ({@code satuanKerja} null) bila {@code satuanKerjaData} {@code null} atau tidak
	 * ada entri spesifik yang cocok.
	 *
	 * @param akunStr         JSON array formula akun {@code {key, akun, satuanKerja}}
	 * @param satuanKerjaData satuan kerja yang dicari akunnya, boleh {@code null} untuk langsung mengambil default
	 * @return akun yang cocok, akun default, atau {@code null} bila tidak ada sama sekali
	 */
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

	/**
	 * Titik masuk utama: menambahkan tombol "Tambah Akun" ke {@code rowFormula} bila {@code edit}
	 * true (menyisipkan entri baru ber-{@code key} acak ke {@code array}), lalu merender daftar
	 * formula lewat {@link #reloadDataFormula} ke baris baru setelah {@code rowFormula}.
	 */
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

	/**
	 * Merender {@code array} formula akun sebagai grid dua kolom (Akun, Satuan Kerja) ke dalam
	 * {@code rowU}: bila {@code edit} true, sel berupa banbox editable (yang menuliskan perubahan
	 * langsung ke {@code jsonObject} JSON entri terkait) plus tombol hapus per baris; bila
	 * {@code false}, sel berupa label read-only saja (mode tampilan). Dipanggil ulang secara
	 * rekursif setiap kali struktur berubah agar grid selalu konsisten dengan {@code array} terbaru.
	 */
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
