package ais.common;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;

public class ObjectHelper {

	@SuppressWarnings("rawtypes")
	public static Object getContentAsObject(String data, Class clazz, Criterion criterion) {
		return getContentAsObject(data, clazz, criterion, true);
	}

	@SuppressWarnings("rawtypes")
	public static Object getContentAsObject(String data, Class clazz, Criterion criterion, boolean checkId) {
		if (data == null || data.trim().isEmpty()) {
			return null;
		}

		Object hasil = null;
		Session session = HibernateUtil.currentNativeSession();

		try {
			String[] dataParts = data.split("-");
			String part0 = dataParts.length > 0 ? dataParts[0].trim() : data;
			String part1 = dataParts.length > 1 ? dataParts[1].trim() : null;
			String part2 = dataParts.length > 2 ? dataParts[2].trim() : null;

			Criterion baseCriterion = (criterion == null) ? Restrictions.sqlRestriction("true") : criterion;

			if (checkId && part0 != null && part0.matches("\\d+")) {
				try {
					Long id = Long.parseLong(part0);
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.idEq(id)).add(baseCriterion), clazz);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:58");
					// Abaikan jika id numerik tidak valid/record tidak dapat dibaca.
				}
			}

			if (hasil == null) {
				ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
				List<String> props = Arrays.asList(classMetadata.getPropertyNames());

				// Cek Kode
				if (props.contains("kode")) {
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.ilike("kode", data, MatchMode.EXACT))
									.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
							clazz);

					if (hasil == null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("kode", part0, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part1 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("kode", part1, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}
				}

				// Cek Nama
				if (hasil == null && props.contains("nama")) {
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.ilike("nama", data, MatchMode.EXACT))
									.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
							clazz);

					if (hasil == null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("nama", part0, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part1 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("nama", part1, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part2 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("nama", part2, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}
				}
				
				// Cek Nama
				if (hasil == null && props.contains("namaSiswa")) {
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.ilike("namaSiswa", data, MatchMode.EXACT))
									.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
							clazz);

					if (hasil == null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("namaSiswa", part0, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part1 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("namaSiswa", part1, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part2 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("namaSiswa", part2, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}
				}
				
				// Cek Nama
				if (hasil == null && props.contains("namaGuru")) {
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.ilike("namaGuru", data, MatchMode.EXACT))
									.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
							clazz);

					if (hasil == null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("namaGuru", part0, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part1 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("namaGuru", part1, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}

					if (hasil == null && part2 != null) {
						hasil = ConstantValues.simpleObject(
								session.createCriteria(clazz).add(Restrictions.ilike("namaGuru", part2, MatchMode.EXACT))
										.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
								clazz);
					}
				}

				// Cek NIM (Spesifik Mahasiswa)
				if (hasil == null && props.contains("nim") && clazz.getName().equals(Mahasiswa.class.getName())) {
					hasil = ConstantValues.simpleObject(session.createCriteria(clazz)
							.add(Restrictions.sqlRestriction(
									"replace(replace(trim(this_.nim),'.',''),',','') = replace(replace(trim('" + data
											+ "'),'.',''),',','')"))
							.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion), clazz);
				}

				// Cek NIDN (Spesifik Dosen)
				if (hasil == null && props.contains("nidn") && clazz.getName().equals(Dosen.class.getName())) {
					hasil = ConstantValues.simpleObject(session.createCriteria(clazz)
							.add(Restrictions.sqlRestriction(
									"replace(replace(trim(this_.nidn),'.',''),',','') = replace(replace(trim('" + data
											+ "'),'.',''),',','')"))
							.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion), clazz);
				}

				// Cek Nomor Induk
				if (hasil == null && props.contains("nomorInduk")) {
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.ilike("nomorInduk", data, MatchMode.EXACT))
									.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
							clazz);
				}

				// Cek Nomor Induk Nasional
				if (hasil == null && props.contains("nomorIndukNasional")) {
					hasil = ConstantValues.simpleObject(session.createCriteria(clazz)
							.add(Restrictions.ilike("nomorIndukNasional", data, MatchMode.EXACT)).setMaxResults(1)
							.addOrder(Order.desc("id")).add(baseCriterion), clazz);
				}

				// Cek Keterangan
				if (hasil == null && props.contains("keterangan")) {
					hasil = ConstantValues.simpleObject(
							session.createCriteria(clazz).add(Restrictions.ilike("keterangan", data, MatchMode.EXACT))
									.setMaxResults(1).addOrder(Order.desc("id")).add(baseCriterion),
							clazz);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// Selalu pastikan resource database ditutup
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}

		return hasil;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map setObjectValues(ClassMetadata classMetadata, Object obj, String[] properties, int mulai,
			HttpServletRequest request) {
		Map list = new HashMap();
		Set<String> colomSudahMasuk = new HashSet<String>();

		for (int j = mulai; j < properties.length; j++) {
			String property = properties[j];
			boolean adaTitik = false;
			String rawParam = request.getParameter(property);
			String s = (rawParam == null) ? "" : rawParam.trim();

			if (StringUtils.countMatches(property, ".") > 0) {
				if (!s.isEmpty()) {
					list.put(property, s);
				}
				adaTitik = true;
				property = StringUtils.split(property, ".")[0];
			}

			if (colomSudahMasuk.contains(property)) {
				continue;
			}

			org.hibernate.type.Type propertyTypeHttp;
			try {
				// Resolusi metadata Hibernate dipisah dari sisa logika: nama parameter
				// request bisa saja bukan properti Hibernate langsung pada entitas
				// (mis. field yang sudah dihapus/di-rename dari entity, atau sisa
				// parameter form dinamis yang mapping-nya sudah usang) -> QueryException.
				// Jangan biarkan itu menggagalkan seluruh sisa properti lainnya; lewati
				// saja properti yang tidak bisa dipetakan.
				propertyTypeHttp = classMetadata.getPropertyType(property);
			} catch (org.hibernate.QueryException e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(kolom tak dikenal Hibernate, dilewati) src/ais/common/ObjectHelper.java:257 properti="
								+ property);
				continue;
			} catch (org.hibernate.HibernateException e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(kolom tak dikenal Hibernate, dilewati) src/ais/common/ObjectHelper.java:257 properti="
								+ property);
				continue;
			}

			try {
				String typeName = propertyTypeHttp.getReturnedClass().getSimpleName();
				boolean isNotEmpty = !s.isEmpty(); // Hindari parsing jika s kosong untuk mencegah NumberFormatException

				if (typeName.equals("String")) {
					list.put(property, s);
					classMetadata.setPropertyValue(obj, property, s, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Double")) {
					Double d = 0.0;
					if (isNotEmpty) {
						try {
							d = Double.parseDouble(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:269");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Integer")) {
					Integer d = 0;
					if (isNotEmpty) {
						try {
							d = Integer.parseInt(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:280");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Long")) {
					Long d = 0L;
					if (isNotEmpty) {
						try {
							d = Long.parseLong(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:291");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Date")) {
					Date d = null;
					if (isNotEmpty) {
						try {
							d = Common.dateFormat53.get().parse(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:302");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Boolean")) {
					Boolean d = s.equalsIgnoreCase("on");
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else {
					Object d = null;
					try {
						d = ObjectHelper.getContentAsObject(s,
								classMetadata.getPropertyType(property).getReturnedClass(), null);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:318");
					}

					if (adaTitik && d == null) {
						// Biarkan kosong sesuai logika asli
					} else {
						list.put(property, d != null ? d : s);
						classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					}
					colomSudahMasuk.add(property);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:329");
				// Abaikan error sesuai logika awal
			}
		}
		return list;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map setObjectValues(ClassMetadata classMetadata, Object obj, String[] properties, int mulai,
			XSSFSheet sheet, int row) {
		Map list = new HashMap();
		Set<String> colomSudahMasuk = new HashSet<String>();

		for (int j = mulai; j < properties.length; j++) {
			String property = properties[j];
			boolean adaTitik = false;

			if (StringUtils.countMatches(property, ".") > 0) {
				try {
					String d = Common.getSheetContentAsString(sheet, j, row);
					if (d != null) {
						list.put(property, d);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:352");
				}
				adaTitik = true;
				property = StringUtils.split(property, ".")[0];
			}

			if (colomSudahMasuk.contains(property)) {
				continue;
			}

			org.hibernate.type.Type propertyType;
			try {
				// Resolusi metadata Hibernate dipisah dari sisa logika: header kolom Excel
				// bisa saja tidak berupa properti Hibernate langsung pada entitas (mis.
				// "kelas" pada Siswa yang hanya punya getter @Transient) -> QueryException.
				// Jangan biarkan itu menggagalkan seluruh sisa kolom baris ini; lewati saja
				// kolom yang tidak bisa dipetakan.
				propertyType = classMetadata.getPropertyType(property);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(kolom tak dikenal Hibernate, dilewati) src/ais/common/ObjectHelper.java:363 properti="
								+ property);
				continue;
			}

			try {
				String typeName = propertyType.getReturnedClass().getSimpleName();

				if (typeName.equals("String")) {
					String d = Common.getSheetContentAsString(sheet, j, row);
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Double")) {
					Double d = Common.getSheetContentAsDouble(sheet, j, row);
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Integer")) {
					Integer d = Common.getSheetContentAsInteger(sheet, j, row);
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Long")) {
					Long d = Common.getSheetContentAsLong(sheet, j, row);
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Date")) {
					Date d = Common.getSheetContentAsDate(sheet, j, row);
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Boolean")) {
					Boolean d = Common.getSheetContentAsBoolean(sheet, j, row);
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else {
					Object d = Common.getSheetContentAsObject(sheet, j, row,
							classMetadata.getPropertyType(property).getReturnedClass());
					if (d == null) {
						// Relasi tidak dapat diselesaikan dari teks Excel → jangan bersihkan
						// relasi yang sudah ada (hindari FK violation saat saveOrUpdate).
						list.put(property, Common.getSheetContentAsString(sheet, j, row));
					} else {
						list.put(property, d);
						classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					}
					colomSudahMasuk.add(property);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:408");
				// Abaikan sesuai logika asli
			}
		}
		return list;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map setObjectValues(ClassMetadata classMetadata, Object obj, JSONObject dataParam) throws Exception {
		Map list = new HashMap();
		Set<String> colomSudahMasuk = new HashSet<String>();
		Iterator<String> properties = dataParam.keys();
		while (properties.hasNext()) {
			String property = properties.next();
			boolean adaTitik = false;

			String s = dataParam.get(property) + "";

			if (StringUtils.countMatches(property, ".") > 0) {
				if (!s.isEmpty()) {
					list.put(property, s);
				}
				adaTitik = true;
				property = StringUtils.split(property, ".")[0];
			}

			if (colomSudahMasuk.contains(property)) {
				continue;
			}

			org.hibernate.type.Type propertyTypeJson;
			try {
				// Sama seperti overload lain: pisahkan resolusi metadata Hibernate dari
				// sisa logika. Key JSON (mis. dari form dinamis/parameter tambahan) bisa
				// saja bukan properti Hibernate pada entitas -> QueryException. Lewati
				// saja key tsb, jangan gagalkan seluruh proses setObjectValues.
				propertyTypeJson = classMetadata.getPropertyType(property);
			} catch (org.hibernate.QueryException e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(kolom tak dikenal Hibernate, dilewati) src/ais/common/ObjectHelper.java:475 properti="
								+ property);
				continue;
			} catch (org.hibernate.HibernateException e) {
				ais.common.ErrorAuditUtil.record(e,
						"auto-audit(kolom tak dikenal Hibernate, dilewati) src/ais/common/ObjectHelper.java:475 properti="
								+ property);
				continue;
			}

			try {
				String typeName = propertyTypeJson.getReturnedClass().getSimpleName();
				boolean isNotEmpty = !s.isEmpty(); // Hindari parsing jika s kosong untuk mencegah NumberFormatException

				if (typeName.equals("String")) {
					list.put(property, s);
					classMetadata.setPropertyValue(obj, property, s, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Double")) {
					Double d = 0.0;
					if (isNotEmpty) {
						try {
							d = Double.parseDouble(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:451");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Integer")) {
					Integer d = 0;
					if (isNotEmpty) {
						try {
							d = Integer.parseInt(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:462");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Long")) {
					Long d = 0L;
					if (isNotEmpty) {
						try {
							d = Long.parseLong(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:473");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Date")) {
					Date d = null;
					if (isNotEmpty) {
						try {
							d = Common.dateFormat53.get().parse(s);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:484");
						}
					}
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else if (typeName.equals("Boolean")) {
					// Penyesuaian: Form biasa mengirim "on", JSON umumnya mengirim "true"
					// (boolean/string)
					Boolean d = s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equals("1");
					list.put(property, d);
					classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					colomSudahMasuk.add(property);
				} else {
					Object d = null;
					try {
						d = ObjectHelper.getContentAsObject(s,
								classMetadata.getPropertyType(property).getReturnedClass(), null);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:502");
					}

					if (adaTitik && d == null) {
						// Biarkan kosong sesuai logika asli
					} else {
						list.put(property, d != null ? d : s);
						classMetadata.setPropertyValue(obj, property, d, EntityMode.POJO);
					}
					colomSudahMasuk.add(property);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ObjectHelper.java:513");
				// Abaikan error sesuai logika awal
			}
		}
		System.out.println("colomSudahMasuk -> " + colomSudahMasuk);
		return list;
	}
}
