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

	// 上传文件存储目录
	private static final String UPLOAD_DIRECTORY = "UploadTmpFolder";

	// 上传配置
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
						"<strong>欢迎使用，上传请点击：</strong></br><a href=\"" + strReUpLoad + "\">" + strReUpLoad + "</a><br/>");

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
			qdcMain.setStrFB("<strong>请稍等</strong></br>");
			 PrintWriter out = response.getWriter();//通过servlet的doget方法获取response对象，通过getWriter方法获取PrintWriter对象
	         out.flush();//清空缓存
	         out.println("<script>");//输出script标签
	         out.println("window.location.href=\"./message.jsp\" ;");//js语句：输出alert语句
	         out.println("</script>");//输出script结尾标签
	         out.flush();
			

			intStatus = 1;
			if (!ServletFileUpload.isMultipartContent(request)) {
				// 如果不是则停止
//				PrintWriter writer = response.getWriter();
//				writer.println("Error: 表单必须包含 enctype=multipart/form-data");
//				writer.flush();
//				return;
				qdcMain.setStrFB("<strong>错误了！</strong></br>错误信息为:表单必须包含 enctype=multipart/form-data</br>"
						+ "</br></br><strong>重新上传请点击：</strong></br><a href=\"" + strReUpLoad + "\">" + strReUpLoad
						+ "</a></br>");
			}
			// 配置上传参数
			DiskFileItemFactory factory = new DiskFileItemFactory();
			// 设置内存临界值 - 超过后将产生临时文件并存储于临时目录中
			factory.setSizeThreshold(MEMORY_THRESHOLD);
			// 设置临时存储目录
			factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

			ServletFileUpload upload = new ServletFileUpload(factory);

			// 设置最大文件上传值
			upload.setFileSizeMax(MAX_FILE_SIZE);

			// 设置最大请求值 (包含文件和表单数据)
			upload.setSizeMax(MAX_REQUEST_SIZE);

			// 中文处理
			upload.setHeaderEncoding("UTF-8");

			// 构造临时路径来存储上传的文件
			// 这个路径相对当前应用的目录
			String uploadPath = getServletContext().getRealPath("/") + File.separator + UPLOAD_DIRECTORY;

			// 如果目录不存在则创建
			File uploadDir = new File(uploadPath);
			if (!uploadDir.exists()) {
				uploadDir.mkdir();
			}

			try {
				// 解析请求的内容提取文件数据
				@SuppressWarnings("unchecked")
				List<FileItem> formItems = upload.parseRequest(request);

				if (formItems != null && formItems.size() > 0) {
					// 迭代表单数据
					for (FileItem item : formItems) {
						// 处理不在表单中的字段
						if (!item.isFormField()) {
							String fileName = new File(item.getName()).getName();
							String filePath = uploadPath + File.separator + fileName;
							String fileHasSWPath = uploadPath + File.separator + "hasDone-" + fileName;

							File storeFile = new File(filePath);

							// 在控制台输出文件的上传路径
							System.out.println(filePath);
							// 保存文件到硬盘
							item.write(storeFile);

							qdcMain.blukInsertCSVToDB("tbTest", filePath);
							qdcMain.splitWordFMDB("tbTest");
							qdcMain.writeColToCSV("tbTest", fileHasSWPath);
//							request.setAttribute("message", "文件上传成功!");
							System.out.println(fileHasSWPath);
							qdcMain.setStrFB("<strong>成功</strong></br>文件下载地址为：" + strBasePath + "/" + UPLOAD_DIRECTORY
									+ "/hasDone-" + fileName + "</br></br><strong>继续上传请点击：</strong></br><a href=\""
									+ strReUpLoad + "\">" + strReUpLoad + "</a></br>");
						}
					}
				}
			} catch (Exception ex) {
//				request.setAttribute("message", "错误信息: " + ex.getMessage());
				qdcMain.setStrFB("<strong>错误了！</strong></br>错误信息为:" + ex.getMessage() + "</br>"
						+ "</br></br><strong>重新上传请点击：</strong></br><a href=\"" + strReUpLoad + "\">" + strReUpLoad
						+ "</a></br>");
			}
			intStatus = 2;
			// 跳转到 message.jsp

		}
	}

	public void destroy() {
		qdcMain.disConnectDB();
	}
}
