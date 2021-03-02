package com.vi.realabs.script;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vi.realabs.model.CodelabData;

import java.io.*;
import java.util.List;

public class FileWork {
    public static List<CodelabData> readCodelabFile() throws IOException {
        String data = readFile("codelab.json", true);
        List<CodelabData> codelabDatas = new Gson().fromJson(data, new TypeToken<List<CodelabData>>(){}.getType());
        return codelabDatas;
    }

    public static void writeCodelabFile(String data) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("codelab.json"))) {
            out.write(data.getBytes());
            out.write(" ".getBytes());
        }
    }

    public static String readFile(String id, boolean isCodelabFile) throws IOException {
        String fileName = isCodelabFile ? id : id + "/codelab.json";
        InputStream inputStream = new FileInputStream(new File(fileName));
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public static void deleteFile(File file){
        for (File subFile : file.listFiles()) {
            if(subFile.isDirectory()) {
                deleteFile(subFile);
            } else {
                subFile.delete();
            }
        }
        file.delete();
    }
}
