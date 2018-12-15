package top.sanshishu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ansj.domain.Result;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.bson.Document;
import org.json.JSONObject;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class QuestionsDC {
	private MongoClient mongoClient;
	private String strMDBDB;
	private long longTotalTest;
	private long longNowTest;
	private String[] csvHeader;
	private String strFB;

	public QuestionsDC() {
		// TODO �Զ����ɵĹ��캯�����
//		ArrayList<String> list = new ArrayList<String>();
		String strTmpBR = null;
		String strJSON = new String();
		strFB = new String("");
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(QuestionsDC.class.getResource("/..").getPath() + "conf/cfg.json"));

			System.out.println(QuestionsDC.class.getResource("/..").getPath() + "conf/cfg.json");
			while ((strTmpBR = br.readLine()) != null) {
				strJSON += strTmpBR;
			}
//	System.out.println(strJSON);
			br.close();
		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}

		JSONObject dataJson = new JSONObject(strJSON);// ����һ������ԭʼjson����json����
		String strMDBUser = dataJson.getString("user");
		char[] achMDBPW = dataJson.getString("pw").toCharArray();
		String strServer = dataJson.getString("server");
		int intPort = dataJson.getInt("host");

		try {
			dataJson = new JSONObject(strJSON);// ����һ������ԭʼjson����json����
			strMDBUser = dataJson.getString("user");
			strMDBDB = dataJson.getString("dbName");
			achMDBPW = dataJson.getString("pw").toCharArray();
			strServer = dataJson.getString("server");
			intPort = dataJson.getInt("host");
			System.out.println("��ȡJSON�ɹ�");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.err.println("��ȡJSONʧ��");
		}
		this.connectDB(strMDBUser, strMDBDB, achMDBPW, strServer, intPort);

	}

	private void connectDB(String strMDBUser, String strMDBDB, char[] achMDBPW, String strServer, int intPort) {

		MongoCredential credential = MongoCredential.createCredential(strMDBUser, strMDBDB, achMDBPW);

		MongoClientSettings settings = MongoClientSettings.builder().credential(credential)
				.applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(strServer, intPort))))
				.build();

		mongoClient = MongoClients.create(settings);
		System.out.println("�������ݿ� " + strMDBDB + " �ɹ�");

	}

	public void disConnectDB() {
		mongoClient.close();
		System.out.println("�Ͽ����ݿ�ɹ�");
	}

	void blukInsertCSVToDB(String strCol, String csvFilePath) {
		try {

			List<Document> ldReadyToInsert = new ArrayList<Document>();

			String[] csvData;
			int intInsert = 0;
			Map<String, Integer> msHasWord;

			longTotalTest = 0;

			MongoDatabase mongoDatabase = this.mongoClient.getDatabase(strMDBDB);
			MongoCollection<Document> collection = mongoDatabase.getCollection(strCol);

			collection.deleteMany(Filters.nor(Filters.eq("_id", "")));

			CsvReader reader = new CsvReader(csvFilePath, ',', Charset.forName("GBK"));
			reader.readHeaders();
			csvHeader = reader.getHeaders();
			while (reader.readRecord()) {
				csvData = reader.getValues();
				Document docGetEle = new Document();
				for (int intI = 0; intI < csvData.length; intI++) {
					docGetEle.append(csvHeader[intI], csvData[intI]);
				}
				docGetEle.append("isRM", false);
				docGetEle.append("OutID", 0);

				ArrayList<String> alstrSpltiWord = new ArrayList<>();
				ArrayList<Integer> alintWordFrq = new ArrayList<>();

				msHasWord = new HashMap<>();
				int intHWNum = 0;

				Result ltAllWord = ToAnalysis.parse(reader.getRawRecord());
				for (org.ansj.domain.Term termEle : ltAllWord) {
					if (!termEle.getNatureStr().equals("w") && !termEle.getNatureStr().equals("null")) {
						if (!msHasWord.containsKey(termEle.getRealName())) {
							msHasWord.put(termEle.getRealName(), intHWNum);
							alstrSpltiWord.add(termEle.getRealName());
							alintWordFrq.add(1);
							intHWNum++;
						} else {
							alintWordFrq.set(msHasWord.get(termEle.getRealName()),
									alintWordFrq.get(msHasWord.get(termEle.getRealName())) + 1);
						}
					}
				}

				docGetEle.append("sw", alstrSpltiWord);
				docGetEle.append("wf", alintWordFrq);

				ldReadyToInsert.add(docGetEle);
				intInsert++;
				if (intInsert == 100) {
					collection.insertMany(ldReadyToInsert);
					intInsert = 0;
					ldReadyToInsert.clear();
				}
				longTotalTest++;
			}
			if (intInsert > 0) {
				collection.insertMany(ldReadyToInsert);
				intInsert = 0;
				ldReadyToInsert.clear();
			}
			reader.close();

		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
	}

	double cosineSimilarity(ArrayList<String> alstrSpltiWord, ArrayList<Integer> alintWordFrq,
			ArrayList<String> alstrSpltiWordIn, ArrayList<Integer> alintWordFrqIn) {
		// ��������м�ֵ�����
		double fResult = 0;
		double dA = 0;
		double dB = 0;
		double dAB = 0;
		// ��������ؼ��ֲ����Ĺؼ���λ�ü����������Щ�ؼ��ֵĴ�Ƶ
		Map<String, Integer> msHasWord = new HashMap<>();
		int intHWNum = 0;
		ArrayList<String> asCheckWord = new ArrayList<>();
		ArrayList<Integer> aiCehckOne = new ArrayList<>();
		ArrayList<Integer> aiCehckTwo = new ArrayList<>();
		// �ѵ�һ��Ĵ�Ƶ���벢���������ɵ�һ��Ĵ�Ƶ
		for (int intI = 0; intI < alstrSpltiWord.size(); intI++) {
			String strEle = alstrSpltiWord.get(intI);
			if (!msHasWord.containsKey(strEle)) {
				msHasWord.put(strEle, intHWNum);
				intHWNum++;
				asCheckWord.add(strEle);
				aiCehckOne.add(0);
				aiCehckTwo.add(0);
			}
			aiCehckOne.set(msHasWord.get(strEle), alintWordFrq.get(intI));
		}
		// �Եڶ�����д���
		for (int intI = 0; intI < alstrSpltiWordIn.size(); intI++) {
			String strEle = alstrSpltiWordIn.get(intI);
			if (!msHasWord.containsKey(strEle)) {
				msHasWord.put(strEle, intHWNum);
				intHWNum++;
				asCheckWord.add(strEle);
				aiCehckOne.add(0);
				aiCehckTwo.add(0);
			}
			aiCehckTwo.set(msHasWord.get(strEle), alintWordFrqIn.get(intI));
		}

		// ���м���
		for (int intI = 0; intI < asCheckWord.size(); intI++) {
			dAB += aiCehckOne.get(intI) * aiCehckTwo.get(intI);
			dA += Math.pow(aiCehckOne.get(intI), 2);
			dB += Math.pow(aiCehckTwo.get(intI), 2);
		}
		fResult = dAB / (Math.pow(dA, 0.5) * Math.pow(dB, 0.5) + 1E-20);

		// ���
//		System.out.println("---------------------");
//		System.out.println(alstrSpltiWord);
//		System.out.println(alstrSpltiWordIn);
//		System.out.println(asCheckWord);
//		System.out.println(fResult);
//		System.out.println(aiCehckOne);
//		System.out.println(aiCehckTwo);

		return fResult;
	}

	void splitWordFMDB(String strCol) {
		MongoDatabase mongoDatabase = this.mongoClient.getDatabase(strMDBDB);
		MongoCollection<Document> collection = mongoDatabase.getCollection(strCol);
		int intGroupOfOnly = 1;
		longNowTest = 0;

		// ���������ĵ�
		/**
		 * 1. ��ȡ������FindIterable<Document> 2. ��ȡ�α�MongoCursor<Document> 3. ͨ���α�������������ĵ�����
		 */

		FindIterable<Document> findIterable = collection
				.find(Filters.and(Filters.eq("OutID", 0), Filters.eq("isRM", false)));
		MongoCursor<Document> cursor = findIterable.iterator();

		while (cursor.hasNext()) {
			Document docReadyCheck = cursor.next();
			ArrayList<String> alstrSpltiWord = new ArrayList<>();
			ArrayList<Integer> alintWordFrq = new ArrayList<>();

			alstrSpltiWord = (ArrayList<String>) docReadyCheck.get("sw");
			alintWordFrq = (ArrayList<Integer>) docReadyCheck.get("wf");

			collection.updateOne(Filters.eq("_id", docReadyCheck.get("_id")),
					new Document("$set", new Document("OutID", intGroupOfOnly)));

			FindIterable<Document> fdLeftCheck = collection
					.find(Filters.and(Filters.eq("OutID", 0), Filters.eq("isRM", false)));
			MongoCursor<Document> cursorLeftCheck = fdLeftCheck.iterator();

			while (cursorLeftCheck.hasNext()) {
				Document docReadyCheckIn = cursorLeftCheck.next();
				ArrayList<String> alstrSpltiWordIn = new ArrayList<>();
				ArrayList<Integer> alintWordFrqIn = new ArrayList<>();

				alstrSpltiWordIn = (ArrayList<String>) docReadyCheckIn.get("sw");
				alintWordFrqIn = (ArrayList<Integer>) docReadyCheckIn.get("wf");

				double dCosR = cosineSimilarity(alstrSpltiWord, alintWordFrq, alstrSpltiWordIn, alintWordFrqIn);

				if (dCosR > 0.8) {
					collection.updateOne(Filters.eq("_id", docReadyCheckIn.get("_id")),
							new Document("$set", new Document("OutID", intGroupOfOnly)));
					collection.updateOne(Filters.eq("_id", docReadyCheckIn.get("_id")),
							new Document("$set", new Document("isRM", true)));
				}

			}
			cursorLeftCheck.close();

			intGroupOfOnly++;
			findIterable = collection.find(Filters.and(Filters.eq("OutID", 0), Filters.eq("isRM", false)));
			cursor = findIterable.iterator();
			longNowTest++;
			if (Math.floorMod(longNowTest, 200) == 0) {
				double douPC = Math.round(((double) longNowTest / (double) longTotalTest) * 10000) / 100.00;
				System.out.println(String.valueOf(douPC) + "%");
				this.strFB = String.valueOf(douPC) + "%<br/>";
			}
		}
		cursor.close();
	}

	public String getStrFB() {
		return strFB;
	}

	public void setStrFB(String strFB) {
		this.strFB = strFB;
	}

	void writeColToCSV(String strCol, String csvFilePath) throws IOException {

		MongoDatabase mongoDatabase = this.mongoClient.getDatabase(strMDBDB);
		MongoCollection<Document> collection = mongoDatabase.getCollection(strCol);

		CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("GBK"));

		String[] straHeader = new String[csvHeader.length + 2];

		int intJ = 0;
		for (intJ = 0; intJ < csvHeader.length; intJ++) {
			straHeader[intJ] = csvHeader[intJ];
		}

		straHeader[intJ++] = "isRM";
		straHeader[intJ++] = "OutID";

		csvWriter.writeRecord(straHeader);

		// ���������ĵ�
		/**
		 * 1. ��ȡ������FindIterable<Document> 2. ��ȡ�α�MongoCursor<Document> 3. ͨ���α�������������ĵ�����
		 */
		FindIterable<Document> findIterable = collection.find();
		MongoCursor<Document> cursor = findIterable.iterator();

		while (cursor.hasNext()) {
			Document docOutputEle = cursor.next();
			String[] straContent = new String[csvHeader.length + 2];
			int intI;
			for (intI = 0; intI < csvHeader.length; intI++) {
				straContent[intI] = docOutputEle.getString(csvHeader[intI]);
			}

			straContent[intI++] = docOutputEle.getBoolean("isRM").toString();
			straContent[intI++] = docOutputEle.getInteger("OutID").toString();
			try {
				csvWriter.writeRecord(straContent);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		cursor.close();
		csvWriter.close();
	}

}
