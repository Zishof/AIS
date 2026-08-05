package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Servlet implementation class CheckISBN
 */
public class MServet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MServet() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void doProsesLogin(HttpServletRequest request, HttpServletResponse response) {

	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String q = request.getParameter("q");
		try {
			String k = Common.desEncrypter.get().decrypt(q);
			String nim = k.replaceAll("abcdefghijklmnopqrstuvwxyz", "");
			boolean mobile = false;
			if (nim.toLowerCase().contains("mobile")) {
				mobile = true;
			}
			String user = null;
			System.out.println("Login dengan NIM = " + nim + " mobile " + mobile);
			String redirect = Common.getRequestHostWithProtocol(request) + "/main";
			String pass = null;

			if (nim.toLowerCase().contains("calonsiswa")) {
				Long id = Long.parseLong(nim.split("-")[0].trim());
				CalonSiswa calonSiswa = ((CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), id));
				if (calonSiswa != null) {
					Common.setLogin(request, calonSiswa);
					redirect = Common.getRequestHostWithProtocol(request) + "/ppdb";
					response.sendRedirect(redirect);
				} else {
					response.setHeader("Content-Type", "application/json");
					PrintWriter writer = response.getWriter();
					String body = "{ \"status\" : \"key salah\" }";
					writer.write(body);
				}
			} else {

				if (nim.toLowerCase().contains("biodatacalonmahasiswa")) {
					Long id = Long.parseLong(nim.split("-")[0].trim());
					BiodataCalonMahasiswa biodataCalonMahasiswa = ((BiodataCalonMahasiswa) ConstantValues
							.ambil(BiodataCalonMahasiswa.class.getName(), id));
					if (biodataCalonMahasiswa != null) {
						Common.setLogin(request, response, biodataCalonMahasiswa);
						redirect = Common.getRequestHostWithProtocol(request) + "/pmb";
						response.sendRedirect(redirect);
					} else {
						response.setHeader("Content-Type", "application/json");
						PrintWriter writer = response.getWriter();
						String body = "{ \"status\" : \"key salah\" }";
						writer.write(body);
					}
				} else {
					if (nim.toLowerCase().contains("mahasiswa")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Mahasiswa mahasiswa = ((Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id));
						pass = mahasiswa.getPass();
						user = mahasiswa.getNim();
					} else if (nim.toLowerCase().contains("minisiswa")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Siswa siswa = ((Siswa) ConstantValues.ambil(Siswa.class.getName(), id));
						pass = siswa.getPass();
						user = siswa.getNim();
					} else if (nim.toLowerCase().contains("alumni")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Mahasiswa mahasiswa = ((Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id));
						pass = mahasiswa.getPass();
						user = mahasiswa.getNim();
						redirect = Common.getRequestHostWithProtocol(request)
								+ "/loginAlumni?digunakanUntukPenggunaAlumni=false";
					} else if (nim.toLowerCase().contains("penggunalulusan")) {
						Long id = Long.parseLong(nim.split("-")[0].trim());
						Mahasiswa mahasiswa = ((Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), id));
						pass = mahasiswa.getPass();
						user = mahasiswa.getNim();
						redirect = Common.getRequestHostWithProtocol(request)
								+ "/loginAlumni?digunakanUntukPenggunaAlumni=true";
					} else if (nim.toLowerCase().contains("user")) {
						user = nim.split("-")[0].trim();
						// Guard: ambil() bisa null bila user tidak ditemukan di cache
						// sehingga .getUserPassword() melempar NullPointerException.
						Tbmuser tbmuserLogin = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), user);
						pass = tbmuserLogin == null ? null : tbmuserLogin.getUserPassword();
					} else {
						user = nim;
						Session session = HibernateUtil.getSessionFactory().openSession();
						try {
							pass = (String) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setProjection(Projections.property("pass")).add(Restrictions.eq("nim", nim))
									.setMaxResults(1).uniqueResult();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MServet.java:156");
						} finally {
							if (session != null) {
								try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MServet.java:159");}
								try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MServet.java:160");}
								try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MServet.java:161");}
							}
						}
					}

					if (pass != null && user != null) {
						SecurityFilter.doAutoLogin(user, Common.desEncrypter.get().decrypt(pass), mobile,
								mobile ? "Login via mobile" : "Login via link", request, response);
						// doAutoLogin bisa sudah menulis/commit response -> cek dulu agar tidak
						// "Cannot call sendRedirect() after the response has been committed".
						if (!response.isCommitted()) {
							response.sendRedirect(redirect);
						}
					} else {
						response.setHeader("Content-Type", "application/json");
						PrintWriter writer = response.getWriter();
						String body = "{ \"status\" : \"key salah\" }";
						writer.write(body);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			response.setHeader("Content-Type", "application/json");
			PrintWriter writer = response.getWriter();
			String body = "{ \"status\" : \"key salah\" }";
			writer.write(body);
		}

	}

	@SuppressWarnings("rawtypes")
	public static JSONObject getHeadersInfo(HttpServletRequest request) {

		JSONObject map = new JSONObject();

		try {
			Enumeration headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String key = (String) headerNames.nextElement();
				String value = request.getHeader(key);
				try {
					map.put(key, value);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MServet.java:206");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MServet.java:210");
		}

		return map;
	}

}
