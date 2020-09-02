package algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class JsonUtil {



    public  static  String MergerJson(String JsonA,String JsonB){

        JSONArray jsonArrayA = new JSONArray();
        JSONArray jsonArrayB = new JSONArray();

        if(JsonA !=null && JsonA.length() >0 ){
            jsonArrayA = JSONObject.parseArray(JsonA);
        }

        if(JsonB !=null && JsonB.length() >0 ){
            jsonArrayB = JSON.parseArray(JsonB);
        }
        jsonArrayA.addAll(jsonArrayB);
        return  JSONArray.toJSONString(jsonArrayA);
    }


}
