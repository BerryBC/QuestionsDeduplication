package top.sanshishu;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;

/**
 * Servlet implementation class UploadHandle
 */
@WebServlet("/UploadHandle")
public class UploadHandle extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// �ϴ��ļ��洢Ŀ¼
	private static final String UPLOAD_DIRECTORY = "UploadTmpFolder";

	// �ϴ�����
	private static final int MEMORY_THRESHOLD = 1024 * 1024 * 3; // 3MB
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 40; // 40MB
	private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 50; // 50MB

	QuestionsDC qdcMain;

	int intStatus;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UploadHandle() {
		super();
		qdcMain = new QuestionsDC();
		intStatus = 0;
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		response.getWriter().append("Served at: ").append(request.getContextPath());
//		System.out.println(request.getParameter("name") == (null));
		if (request.getParameter("r") == (null)) {
			getServletContext().getRequestDispatcher("/message.jsp").forward(request, response);
		} else if (request.getParameter("r").equals("baby")) {

			if (intStatus == 0) {
				String strBasePath = request.getScheme() + "://" + request.getServerName() + ":"
						+ request.getServerPort() + request.getContextPath();
				String strReUpLoad = strBasePath + "/upload.jsp";
				qdcMain.setStrFB(
						"<strong>��ӭʹ�ã��ϴ�������</strong></br><a href=\"" + strReUpLoad + "\">" + strReUpLoad + "</a><br/>");

			}
			JSONObject jsonFB = new JSONObject();
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			jsonFB.put("status", String.valueOf(intStatus));
			jsonFB.put("fbWord", qdcMain.getStrFB());
			response.getWriter().append(jsonFB.toString());

		} else {
			response.getWriter().append("Gosh!!");
		}

	}

	/**
	 * @see HttpServlet #doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		doGet(request, response);
		if (intStatus != 0 || intStatus != 2) {
			String strBasePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
					+ request.getContextPath();

			String strReUpLoad = strBasePath + "/upload.jsp";
//			getServletContext().getRequestDispatcher("/message.jsp").forward(request, response);
//			response.getWriter().flush();
			qdcMain.setStrFB("<strong>���Ե�</strong></br>");
			 PrintWriter out = response.getWriter();//ͨ��servlet��doget������ȡresponse����ͨ��getWriter������ȡPrintWriter����
	         out.flush();//��ջ���
	         out.println("<script>");//���script��ǩ
	         out.println("window.location.href=\"./message.jsp\" ;");//js��䣺���alert���
	         out.println("</script>");//���script��β��ǩ
	         out.flush();
			

			intStatus = 1;
			if (!ServletFileUpload.isMultipartContent(request)) {
				// ���������ֹͣ
//				PrintWriter writer = response.getWriter();
//				writer.println("Error: ��������� enctype=multipart/form-data");
//				writer.flush();
//				return;
				qdcMain.setStrFB("<strong>�����ˣ�</strong></br>������ϢΪ:��������� enctype=multipart/form-data</br>"
						+ "</br></br><strong>�����ϴ�������</strong></br><a href=\"" + strReUpLoad + "\">" + strReUpLoad
						+ "</a></br>");
			}
			// �����ϴ�����
			DiskFileItemFactory factory = new DiskFileItemFactory();
			// �����ڴ��ٽ�ֵ - �����󽫲�����ʱ�ļ����洢����ʱĿ¼��
			factory.setSizeThreshold(MEMORY_THRESHOLD);
			// ������ʱ�洢Ŀ¼
			factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

			ServletFileUpload upload = new ServletFileUpload(factory);

			// ��������ļ��ϴ�ֵ
			upload.setFileSizeMax(MAX_FILE_SIZE);

			// �����������ֵ (�����ļ��ͱ�����)
			upload.setSizeMax(MAX_REQUEST_SIZE);

			// ���Ĵ���
			upload.setHeaderEncoding("UTF-8");

			// ������ʱ·�����洢�ϴ����ļ�
			// ���·����Ե�ǰӦ�õ�Ŀ¼
			String uploadPath = getServletContext().getRealPath("/") + File.separator + UPLOAD_DIRECTORY;

			// ���Ŀ¼�������򴴽�
			File uploadDir = new File(uploadPath);
			if (!uploadDir.exists()) {
				uploadDir.mkdir();
			}

			try {
				// ���������������ȡ�ļ�����
				@SuppressWarnings("unchecked")
				List<FileItem> formItems = upload.parseRequest(request);

				if (formItems != null && formItems.size() > 0) {
					// ����������
					for (FileItem item : formItems) {
						// �����ڱ��е��ֶ�
						if (!item.isFormField()) {
							String fileName = new File(item.getName()).getName();
							String filePath = uploadPath + File.separator + fileName;
							String fileHasSWPath = uploadPath + File.separator + "hasDone-" + fileName;

							File storeFile = new File(filePath);

							// �ڿ���̨����ļ����ϴ�·��
							System.out.println(filePath);
							// �����ļ���Ӳ��
							item.write(storeFile);

							qdcMain.blukInsertCSVToDB("tbTest", filePath);
							qdcMain.splitWordFMDB("tbTest");
							qdcMain.writeColToCSV("tbTest", fileHasSWPath);
//							request.setAttribute("message", "�ļ��ϴ��ɹ�!");
							System.out.println(fileHasSWPath);
							qdcMain.setStrFB("<strong>�ɹ�</strong></br>�ļ����ص�ַΪ��" + strBasePath + "/" + UPLOAD_DIRECTORY
									+ "/hasDone-" + fileName + "</br></br><strong>�����ϴ�������</strong></br><a href=\""
									+ strReUpLoad + "\">" + strReUpLoad + "</a></br>");
						}
					}
				}
			} catch (Exception ex) {
//				request.setAttribute("message", "������Ϣ: " + ex.getMessage());
				qdcMain.setStrFB("<strong>�����ˣ�</strong></br>������ϢΪ:" + ex.getMessage() + "</br>"
						+ "</br></br><strong>�����ϴ�������</strong></br><a href=\"" + strReUpLoad + "\">" + strReUpLoad
						+ "</a></br>");
			}
			intStatus = 2;
			// ��ת�� message.jsp

		}
	}

	public void destroy() {
		qdcMain.disConnectDB();
	}
}
