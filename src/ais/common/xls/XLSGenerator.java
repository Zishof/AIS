/*
 * XLSGenerator.java
 *
 * Created on September 16, 2007, 9:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ais.common.xls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

/**
 * 
 * @author M. Fauzi Murtadlo
 */
public class XLSGenerator {

	private static XLSGenerator xLSGenerator = new XLSGenerator();
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static XLSGenerator getInstance() {
		return xLSGenerator;
	}

	/** Creates a new instance of XLSGenerator */
	private XLSGenerator() {
	}

	@SuppressWarnings("rawtypes")
	public synchronized InputStream generateXLSFile(Class clazz) {
		File file = new File(Common.ROOT_UPLOAD + clazz.getName() + "_"
				+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + ".xlsx");

		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
			ClassMetadata metaData = HibernateUtil.getClassMetadata(clazz);
			String identifier = metaData.getIdentifierPropertyName();
			String[] propertyNames = metaData.getPropertyNames();
			String[] allProperty = new String[propertyNames.length + 1];
			allProperty[0] = identifier;
			int i = 1;
			for (String property : propertyNames) {
				allProperty[i] = property;
				i++;
			}

			save(allProperty, file, clazz);
		} catch (IOException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:72");
		}
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:78");
		}
		file.delete();
		return inputStream;
	}

	
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public synchronized InputStream generateXLSFileWithData(Class clazz) {
		File file = new File(Common.ROOT_UPLOAD + clazz.getName() + "_"
				+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + ".xlsx");

		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
			ClassMetadata metaData = HibernateUtil.getClassMetadata(clazz);
			String identifier = metaData.getIdentifierPropertyName();
			String[] propertyNames = metaData.getPropertyNames();
			String[] allProperty = new String[propertyNames.length + 1];
			allProperty[0] = identifier;
			int i = 1;
			for (String property : propertyNames) {
				allProperty[i] = property;
				i++;
			}

			Type identifier1 = metaData.getIdentifierType();
			Type[] propertyNames1 = metaData.getPropertyTypes();
			Type[] allProperty1 = new Type[propertyNames1.length + 1];
			allProperty1[0] = identifier1;
			i = 1;
			for (Type property : propertyNames1) {
				allProperty1[i] = property;
				i++;
			}

			Session sess = HibernateUtil.currentSession();
			Criteria crit = sess.createCriteria(clazz);
			List res = crit.setMaxResults(ais.common.Common.MAX_RESULT).list();

			List<Object[]> allData = new ArrayList<Object[]>();

			for (Object obj : res) {
				Object[] d = metaData.getPropertyValues(obj, EntityMode.POJO);
				Object[] data = new Object[d.length + 1];
				data[0] = metaData.getIdentifier(obj, EntityMode.POJO);
				int u = 1;
				for (Object o : d) {
					data[u] = o;
					u++;
				}
				allData.add(data);
			}

			save(allProperty1, allProperty, allData, file, clazz);
		} catch (IOException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:134");
		}
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:140");
		}
		file.delete();
		return inputStream;
	}

	
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public synchronized InputStream generateXLSFileWithData(Class clazz, List<String> fields) {
		File file = new File(Common.ROOT_UPLOAD + clazz.getName() + "_"
				+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + ".xlsx");

		System.err.println("fields = " + fields);

		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
			ClassMetadata metaData = HibernateUtil.getClassMetadata(clazz);
			String identifier = metaData.getIdentifierPropertyName();
			String[] allProperty = new String[fields.size() + 1];
			Type[] allProperty1 = new Type[fields.size() + 1];

			Type identifier1 = metaData.getIdentifierType();
			allProperty1[0] = identifier1;
			allProperty[0] = identifier;
			int i = 1;
			for (String property : fields) {
				allProperty[i] = property;
				allProperty1[i] = metaData.getPropertyType(property);
				i++;
			}

			Session sess = HibernateUtil.currentSession();
			Criteria crit = sess.createCriteria(clazz);
			List res = crit.setMaxResults(ais.common.Common.MAX_RESULT).list();

			List<Object[]> allData = new ArrayList<Object[]>();

			for (Object obj : res) {
				Object[] data = new Object[fields.size() + 1];
				data[0] = metaData.getIdentifier(obj, EntityMode.POJO);
				int u = 1;
				for (String property : fields) {
					data[u] = metaData.getPropertyValue(obj, property, EntityMode.POJO);
					u++;
				}
				allData.add(data);
			}

			save(allProperty1, allProperty, allData, file, clazz);
		} catch (IOException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:191");
		}
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:197");
		}
		file.delete();
		return inputStream;
	}

	@SuppressWarnings("rawtypes")
	public synchronized InputStream generateXLSFile(Class clazz, List<String> fields) {
		File file = new File(Common.ROOT_UPLOAD + clazz.getName() + "_"
				+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + ".xlsx");

		System.err.println("fields = " + fields);

		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
			ClassMetadata metaData = HibernateUtil.getClassMetadata(clazz);
			String identifier = metaData.getIdentifierPropertyName();
			String[] allProperty = new String[fields.size() + 1];

			allProperty[0] = identifier;
			int i = 1;
			for (String property : fields) {
				allProperty[i] = property;
				i++;
			}

			saveNoData(allProperty, file, clazz);
		} catch (IOException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:226");
		}
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:232");
		}
		file.delete();
		return inputStream;
	}

	@SuppressWarnings({ "rawtypes" })
	private Boolean save(String[] data, File file, Class className) {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("First Sheet");
			sheet.createRow(0).createCell(0).setCellValue(className.getName());
			if (data != null) {
				int ii = 0;
				for (String aObj : data) {

					sheet.createRow(ii).createCell(2).setCellValue(aObj);

					ii++;
				}
			}

			try {
				FileOutputStream fileOut = new FileOutputStream(file);
				workbook.write(fileOut);
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:264");
		}
		return false;
	}

	
	@SuppressWarnings({ "rawtypes", "deprecation" })
	private Boolean save(Type[] type, String[] data, List<Object[]> allData, File file, Class className) {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("First Sheet");
			sheet.createRow(0).createCell(0).setCellValue(className.getName());
			if (data != null) {
				int ii = 0;
				for (String aObj : data) {
					sheet.createRow(ii).createCell(2).setCellValue(aObj);
					ii++;
				}
			}
			/*
			 * if(type != null) { int ii = 0; for(Type aObj : type){ sheet.addCell(new
			 * Label(ii,1,aObj.getReturnedClass().getName())); ii ++; } }
			 */
			if (allData != null) {
				int jj = 3;
				for (Object[] aObj1 : allData) {
					int ii = 0;
					for (Object aObj : aObj1) {
						// // System.out.print(aObj);
						if (aObj instanceof String) {
							
							sheet.createRow(ii).createCell(jj).setCellValue((aObj == null ? "" : aObj).toString());
							
						} else if (aObj instanceof Integer) {
							
							sheet.createRow(ii).createCell(jj).setCellValue( (Integer) ((aObj == null ? new Integer(0) : aObj)));
							
						} else if (aObj instanceof Long) {
							
							sheet.createRow(ii).createCell(jj).setCellValue( (Long) ((aObj == null ? new Long(0L) : aObj)));
							
						} else if (aObj instanceof Double) {
							
							sheet.createRow(ii).createCell(jj).setCellValue( (Double) ((aObj == null ? new Double(0.0) : aObj)));
							
							
						} else if (aObj instanceof java.util.Date) {
							// System.err.println("date aObj = "+aObj);
							
							sheet.createRow(ii).createCell(jj).setCellValue( 
									format.format((Date) (aObj == null ? ais.ui.util.WaktuUtil.getDate() : aObj)));
							
						} else {
							try {
								ClassMetadata metaData = HibernateUtil.getClassMetadata(aObj.getClass());
								Object id = metaData.getIdentifier(aObj, EntityMode.POJO);
								// System.err.println("aObj.getClass() =
								// "+aObj.getClass()+
								// " id = "+id.getClass()+" id value= "+id);
								if (id instanceof String) {
									
									
									sheet.createRow(ii).createCell(jj).setCellValue((id == null ? "" : id).toString());
									
								} else if (id instanceof Integer) {
									sheet.createRow(ii).createCell(jj).setCellValue(
											(Integer) ((id == null ? new Integer(0) : id)));
								} else if (id instanceof Long) {
									sheet.createRow(ii).createCell(jj).setCellValue((Long) ((id == null ? new Long(0) : id)));
								} else if (id instanceof Double) {
									sheet.createRow(ii).createCell(jj).setCellValue((Double) ((id == null ? new Double(0) : id)));
								} else {
									sheet.createRow(ii).createCell(jj).setCellValue((id == null ? "" : id).toString());
								}
							} catch (Exception e) {
								
								
								sheet.createRow(ii).createCell(jj).setCellValue((aObj == null ? "" : aObj).toString());
							}
						}
						ii++;
					}
					//
					jj++;
				}
			}
			try {
				FileOutputStream fileOut = new FileOutputStream(file);
				workbook.write(fileOut);
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:360");
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes" })
	private Boolean saveNoData(String[] data, File file, Class className) {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("First Sheet");
			sheet.createRow(0).createCell(0).setCellValue(className.getName());
			if (data != null) {
				int ii = 0;
				for (String aObj : data) {
					sheet.createRow(ii).createCell(2).setCellValue(aObj);
					ii++;
				}
			}

			try {
				FileOutputStream fileOut = new FileOutputStream(file);
				workbook.write(fileOut);
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/xls/XLSGenerator.java:389");
		}
		return false;
	}

	



}
