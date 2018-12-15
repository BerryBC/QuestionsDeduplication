<%@page import="top.sanshishu.QuestionsDC"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>


<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>上传页面</title>
</head>
<body>
	<h1>
		<a href="/">三世书首页</a>
	</h1>
	<h1>请上传你想要去重的CSV文件</h1>
	<form method="post" action="./UploadHandle"
		enctype="multipart/form-data">
		选择一个文件: <input type="file" name="uploadFile" /> <br /> <br /> <input
			type="submit" value="上传" />
			
	</form>
	<p>如果想看看刚生成过的结果可以点击</p><a href="./message.jsp">Go Back My Love!</a>
</body>
</html>