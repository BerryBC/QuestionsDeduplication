<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>返回状态页</title>
</head>
<body>
	<h1>
		<a href="/">三世书首页</a>
	</h1>
	<br />
	<p>请稍等哟~</p>
	<br />

	<div id="divCt"></div>
	<script src="./js/jquery.min.js" type="text/javascript"></script>
	<script type="text/javascript">
		var strGS = "";
		setInterval(function() {
			var intRand=Math.random();
			$.ajax({
				type : "GET",
				url : "./UploadHandle",
				data : {
					r : "baby",
					g:intRand
				},
				dataType : "json",
				success : function(data) {
					var jDivCT = $("#divCt");
					if (strGS != data.fbWord) {
						jDivCT.html(jDivCT.html() + data.fbWord);
					}
					strGS = data.fbWord;
				},
				error : function(XMLHttpRequest, textStatus, errorThrown) {
					console.log('系统异常', textStatus + errorThrown, 'error');
				}

			});
		}, 3000);
	</script>
</body>
</html>