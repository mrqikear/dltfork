package TestClass;


import algorithm.Derivative;
import algorithm.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Junit {


    private String JsonStrTest1 = "";
    private String  JsonStrTest2 = "";


    @Test
    public void  Test(){

        String path = this.getClass().getClassLoader().getResource("data.json").getPath();
        String path2 = this.getClass().getClassLoader().getResource("data2.json").getPath();
        JsonStrTest1 = readFileContent(path);
        JsonStrTest2 = readFileContent(path2);
        String JsonStr = JsonUtil.MergerJson(JsonStrTest1,JsonStrTest2);
        //Derivative.Result();
    }


    public  String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }

}
