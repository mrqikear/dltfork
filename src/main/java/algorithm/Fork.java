package algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;


public class Fork {

    private  static double lineK = 0.38;  //默认曲线斜率最低值

    private String JsonStrTest1 = "";
    private String  JsonStrTest2 = "";


    public String Test(){
        String path = this.getClass().getClassLoader().getResource("data.json").getPath();
        String path2 = this.getClass().getClassLoader().getResource("data2.json").getPath();
        JsonStrTest1 = readFileContent(path);
        JsonStrTest2 = readFileContent(path2);
        String JsonStr = JsonUtil.MergerJson(JsonStrTest1,JsonStrTest2);
        String  str = Compare(JsonStr);
        System.out.println(str);
        return str;
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



    public String dengCompare(List<TopGraphicDataBean> beans){
        if(beans==null || beans.isEmpty()){
            return "";
        }
        return Compare(beans.toString());
    }

    /**
     * 分支识别结果
     */
    private String Compare(String jsonStr) {
        KalmanFilter kalmanFilter = new KalmanFilter();
        kalmanFilter.initial();
        //解析json
            //new JSONObject();
        JSONArray jsonArray = JSONObject.parseArray(jsonStr);

        HashMap<String, String> phaseMap = new HashMap<>();
                //System.out.println(jsonArray);
                for(int i=0;i<jsonArray.size();i++){
                    JSONObject jsonobj = (JSONObject) jsonArray.get(i);
                    boolean isFork =false; //是否有分支脉冲
                    jsonobj.put("isFork",isFork);
                    jsonobj.remove("report_time");
                    String data = (String) jsonobj.get("data");
                    String[] strArray2 = data.split("\\|");
                    ArrayList<Integer> point = new ArrayList<>();
                    //先对数据进行滤波
                    int oldValue = Integer.parseInt(strArray2[0]);
                    for (String value : strArray2) {
                    oldValue = (int) kalmanFilter.KalmanFilter(oldValue,Integer.parseInt(value));
                    //Y轴平移50
                    oldValue +=50;
                    point.add(oldValue);//过滤后的数据
                 }


            /**
             * 不同类型电表单项电表不同处理
             */
            String phase  = (String) jsonobj.get("phase");
            String qrcode  = (String) jsonobj.get("qrcode");//地址
            if(phase.equals("00")){    //三项电表读取方式
                ArrayList<Integer> dataArrA = new ArrayList<>();
                ArrayList<Integer> dataArrB = new ArrayList<>();
                ArrayList<Integer> dataArrC = new ArrayList<>();
                for (int index=0;index<point.size();index++){
                    if(index < 340){
                        dataArrA.add(point.get(index));  //A项采集数据
                    }

                    if(index>=340 && index<=680){
                        dataArrB.add(point.get(index));
                    }

                    if(index > 680){
                        dataArrC.add(point.get(index));
                    }

                }
                int resA  = this.SingleMeterA(dataArrA);
                int resB  = this.SingleMeterB(dataArrB);
                int resC  = this.SingleMeterC(dataArrC);


                //Test
                Derivative.Result(dataArrA,1);

//                if(true){
//                    return  "123";
//                }

               isFork =   resA == 1 || resB ==1 || resC ==1  ? true : false;
               if(resA==1){
                   phaseMap.put(qrcode,"A");
               }else if(resB ==1){
                   phaseMap.put(qrcode,"B");
               }else if(resC == 1){
                   phaseMap.put(qrcode,"C");
               }

                }else if (phase.equals("01")){  //A项
                isFork =  this.SingleMeterA(point) == 1 ?  true :false;
                if(isFork){
                    phaseMap.put(qrcode,"A");
                }
                }else if (phase.equals("02")){ //B项
                    isFork =  this.SingleMeterB(point) == 1 ?  true :false;
                if(isFork){
                    phaseMap.put(qrcode,"B");
                }

                }else if (phase.equals("03")){ //C项
                    isFork =  this.SingleMeterC(point) == 1 ?  true :false;
                if(isFork){
                    phaseMap.put(qrcode,"C");
                }

            }

            jsonobj.put("isFork",isFork);
        }

       // System.out.println(jsonArray);
        //生成树状结构json

        /**
         * 未识别的波形列表
         */

        /*
        ArrayList<String> isnotFork = new ArrayList<>();
        for ( Object obj: jsonArray) {
            JSONObject jsonObj = (JSONObject) obj;

            if(!(boolean)jsonObj.get("isFork")){
                isnotFork.add((String) jsonObj.get("qrcode"));
            }

        }
        */

        /**
         * 重复数据去重保留识别波形的结果
         */
        JSONArray FilterArry  = new JSONArray();
        FilterArry.addAll(jsonArray);

        /**
         * 过滤同样的数据 保留识别成功的数据
         */
        for (Object obj:jsonArray) {
            JSONObject jsonObj = (JSONObject) obj;
            for(int i=0;i<FilterArry.size();i++){
               JSONObject innerObj = (JSONObject) FilterArry.get(i);
               if(innerObj!=null){
                   //存在相同并且被识别才进行替换
                   String innerqrCode = (String) innerObj.get("qrcode");
                   String innerIIqrcode = (String) innerObj.get("IIqrcode");
                   boolean innerisFork = (boolean) innerObj.get("isFork");

                  String Outqrcode = (String) jsonObj.get("qrcode");
                  String OutIIqrcode = (String) jsonObj.get("IIqrcode");
                  boolean OutisFork = (boolean) jsonObj.get("isFork");
                   /**
                    *   同样的电表和II采数据 同时过滤容器为非分支识别，后来数据为分支识别 替换
                    */
                   if(innerqrCode.equals(Outqrcode) && innerIIqrcode.equals(OutIIqrcode) && OutisFork  && !innerisFork){
                       FilterArry.remove(innerObj);
                       FilterArry.add(jsonObj);
                  }

               }else{
                   FilterArry.add(jsonObj);
               }
            }

        }


        System.out.println(FilterArry);

        String Json =  Analysis(FilterArry);
        return Json;

    }


    /**
     * 通过最大值判断是否存在波峰
     *
     * @param point
     */

    private boolean JuggByMaxKey(ArrayList<Integer> point) {


        /**
         * 不同类型电表单项电表不同处理
         */

        /**
         * 假设第一个波形区间点
         * start 114
         * end  129
         */
        int start = 114;
        int end = 129;
        ArrayList<Integer> subSet = this.getSubSet(point, start, end);
        int maxKey1 = this.getMaxKey(point, subSet); //第一个波峰
        HashMap<String, Object> result = new HashMap<>();
        result.put("point", point);
        /**
         * 假设第二个区间
         * start 239
         * end  254
         */
        int start2 = 239;
        int end2 = 254;

        ArrayList<Integer> subSet2 = this.getSubSet(point, start2, end2);
        int maxKey2 = this.getMaxKey(point, subSet2);
        boolean checkRes1 = false;
        boolean checkRes2 = false;
        if (maxKey1 > 0)
        {
            checkRes1 = this.getlinearRegression(subSet, maxKey1, point, maxKey1 + 114);
        }
        if (maxKey2 > 0) {
            checkRes2 = this.getlinearRegression(subSet2, maxKey2, point, maxKey2 + 239);
        }

        if (checkRes1 && checkRes2) {
            result.put("result", 1);  //识别波形
        } else {
            //回归直线斜率判断
        }


        return false;
    }


    /**
     * 取得集合区间的子区间
     *
     * @param point
     * @param start
     * @param end
     * @return
     */
    private ArrayList<Integer> getSubSet(ArrayList<Integer> point, int start, int end){

        ArrayList<Integer> subSet = new ArrayList<>();
        for (int i = 0; i < point.size(); i++) {
            if (i >= start && i <= end) {
                subSet.add(point.get(i));
            }
        }
        return subSet;
    }


    /**
     * @param point
     * @param subSet
     * @return int max
     */
    private int getMaxKey(ArrayList<Integer> point, ArrayList<Integer> subSet) {
        int count = point.size();
        int sum = 0;
        double average = 0;
        for (int i = 0; i < point.size(); i++) {
            sum += point.get(i);
        }
        average = sum / count;
        //子集合的最大值
        double maxValue = 0;
        int maxKey = 0;
        for (int index = 0; index < subSet.size(); index++) {
            int teampValue = subSet.get(index);
            if (index > 0 && index < subSet.size() - 1) {
                teampValue = subSet.get(index - 1) + subSet.get(index) + subSet.get(index + 1) / 3;     //最近3点的平均值
            }
            if (teampValue > maxValue) {
                maxKey = index;
            }
        }
        //最大值区间 中间左右偏移3个点
        int mid = subSet.size() / 2;

        if (mid - 3 <= maxKey && maxKey <= mid + 3) {
            return maxKey;
        }

        return 0;
    }


    /**
     * 求线性回归方程斜率
     *
     * @param subSet    子集合
     * @param maxKey    子集合下标
     * @param point
     * @param offsetKey 主集合偏移下标
     * @return 先求整个区间的回归方程
     * 再比较左线性回归与右线性回归的比较
     */
    private boolean getlinearRegression(ArrayList<Integer> subSet, int maxKey, ArrayList<Integer> point, int offsetKey) {


        ArrayList<Integer> arr1 = new ArrayList<>();
        ArrayList<Integer> arr2 = new ArrayList<>();
        int sumKey1 = 0;
        int sumKey2 = 0;
        int sumY1 = 0;
        int sumY2 = 0;

        for (int i = 0; i < subSet.size(); i++) {
            if (i < maxKey) {
                arr1.add(subSet.get(i));
                sumKey1 += i;
                sumY1 += subSet.get(i);
            }

            if (i > maxKey) {
                arr2.add((subSet.get(i)));
                sumKey2 += i;
                sumY2 += subSet.get(i);

            }
        }

        int count1 = arr1.size();
        int count2 = arr2.size();

        double xavg1 = sumKey1 / count1;
        double xavg2 = sumKey2 / count2;

        double yavg1 = sumY1 / count1;
        double yavg2 = sumY2 / count2;

        double mdcross_sum1 = 0; // X,Y 离均差交乘积和
        double xdif_square_sum1 = 0; //X 离均差平方和

        for (int i = 0; i < count1; i++) {
            double xdif = i - xavg1;
            double ydif = subSet.get(i) - yavg1;
            mdcross_sum1 += xdif * ydif;
            xdif_square_sum1 += Math.pow(xdif, 2);
        }

        double b1 = new BigDecimal(mdcross_sum1 / xdif_square_sum1).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(); //保留2位小数


        double mdcross_sum2 = 0; // X,Y 离均差交乘积和
        double xdif_square_sum2 = 0; //X 离均差平方和
        for (int i = 0; i < count2; i++) {
            double xdif = i - xavg2;
            double ydif = subSet.get(i) - yavg2;
            mdcross_sum2 += xdif * ydif;
            xdif_square_sum2 += Math.pow(xdif, 2);
        }

        double b2 = new BigDecimal(mdcross_sum2 / xdif_square_sum2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(); //保留2位小数


        /**
         * 区间1倒退15个点求斜率
         */
        ArrayList<Integer> lefArr = new ArrayList<>();
        ArrayList<Integer> rightArr = new ArrayList<>();

        for (int i = 0; i < point.size(); i++) {
            if (i >= offsetKey - 15 && i <= offsetKey) {
                lefArr.add(point.get(i));
            }

            if (i > offsetKey && i < offsetKey + 15) {
                rightArr.add(point.get(i));
            }

        }

        //获取左右两边的斜率
        double leftK = this.getSlope(lefArr);
        double rightK = this.getSlope(rightArr);
        /**
         * 积分>=1 则认是正常波形
         */
        int score = 0;
        if (b1 > 0 && b2 < 0 && Math.abs(b1) > 1 && Math.abs(b2) > 1) {
            if (Math.abs(leftK) < Math.abs(b1) && Math.abs(b1) - Math.abs(leftK) > 0.8) {
                score += 1;
            }
            if (Math.abs(rightK) < Math.abs(b2) && Math.abs(b2) - Math.abs(rightK) > 0.8) {
                score += 1;
            }

        }

        if (score >= 1) {
            return true;
        }

        return false;
    }


    /**
     * 点集合的回归线性方程
     *
     * @param subSet
     * @return
     */
    private double getSlope(ArrayList<Integer> subSet) {

        int sumKey1 = 0;
        int sumY1 = 0;

        for (int i = 0; i < subSet.size(); i++) {
            sumKey1 += i;
            sumY1 += subSet.get(i);
        }
        int count1 = subSet.size();

        double xavg1 = sumKey1 / count1;
        double yavg1 = sumY1 / count1;

        double mdcross_sum1 = 0; // X,Y 离均差交乘积和
        double xdif_square_sum1 = 0; //X 离均差平方和
        for (int i = 0; i < count1; i++) {
            double xdif = i - xavg1;
            double ydif = subSet.get(i) - yavg1;
            mdcross_sum1 += xdif * ydif;
            xdif_square_sum1 += Math.pow(xdif, 2);
        }
        double b1 = new BigDecimal(mdcross_sum1 / xdif_square_sum1).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(); //保留2位小数
        return b1;
    }


    /**
     * 判断左右两边的斜率变化
     *
     * @return
     */
    private int Checkslope(ArrayList<Integer> point) {


        ArrayList<Integer> oneLeft = new ArrayList<>();
        ArrayList<Integer> oneArrLeftHafl = new ArrayList<>();
        ArrayList<Integer> oneArrRightHafl = new ArrayList<>();
        ArrayList<Integer> oneRight = new ArrayList<>();
        ArrayList<Integer> twoLeft = new ArrayList<>();
        ArrayList<Integer> twoArrLeftHalf = new ArrayList<>();
        ArrayList<Integer> twoArrRightHalf = new ArrayList<>();
        ArrayList<Integer> twoRight = new ArrayList<>();

        //不同点位的脉冲波形需要根据出现的范围进行调整
        //脉冲范围1
        int star1 = 50;
        int end1 = 117;
        int mid1 = (star1 + end1) / 2;
        //脉冲范围2
        int start2 = 250;
        int end2 = 317;
        int mid2 = (start2 + end2) / 2;


        //左边大范围
        for (int i = 0; i < point.size(); i++) {

            if (i >= star1 && i <= mid1) {
                oneLeft.add(point.get(i));
            }

            if (i >= star1 + 15 && i <= mid1) {    //左边趋势
                oneArrLeftHafl.add(point.get(i));
            }


            if (i >= mid1 && i <= end1 - 15) {  //右边趋势
                oneArrRightHafl.add(point.get(i));
            }

            if (i >= mid1 && i <= end1) {
                oneRight.add(point.get(i));
            }

            if (i >= start2 && i <= mid2) {
                twoLeft.add(point.get(i));
            }

            //第二个点左边趋势
            if (i >= start2 + 15 && i <= mid2) {
                twoArrLeftHalf.add(point.get(i));
            }
            //第二个点右边趋势
            if (i >= mid2 && i <= end2 - 15) {
                twoArrRightHalf.add(point.get(i));
            }

            if (i >= mid2 && i <= end2) {
                twoRight.add(point.get(i));
            }

        }

        double oneLeftK = this.getSlope(oneLeft);
        double oneArrLeftHalfK= this.getSlope(oneArrLeftHafl);
        double oneArrRightHalfk = this.getSlope(oneArrRightHafl);
        double oneRightK= this.getSlope(oneRight);

        double twoLeftK= this.getSlope(twoLeft);
        double twoArrLeftHalfK= this.getSlope(twoArrLeftHalf);
        double twoArrRightHalfK= this.getSlope(twoArrRightHalf);
        double twoRightK= this.getSlope(twoRight);

        //曲线判断时 斜率必须大于0.38否则认为接近直线
        if(!(Math.abs(oneLeftK) >= 0.38  && Math.abs(oneRightK) >=0.38 && Math.abs(twoLeftK) >= 0.38  && Math.abs(twoRightK) >=0.38 )){
            return 1;
        }

        return 0;
    }




    /**
     * A项目波形(有三个脉冲)
     *
     */

    private int SingleMeterA(ArrayList<Integer> dataArr){


        /***
         * 三个点
         *
         * 18-28 第一个波峰
         * 162-172 第二个波峰
         * 305-315 第三个波峰
         *
         */
        int start1=18;
        int end1 =28;
        int start2= 162;
        int end2 = 172;
        int start3 =305;
        int end3 = 315;

        ArrayList<Integer> frist = new ArrayList<>();
        ArrayList<Integer> second = new ArrayList<>();
        ArrayList<Integer> third = new ArrayList<>();

        for(int i=0;i<dataArr.size();i++){

           if( i>=start1-1 && i<=end1-1 ){
               frist.add(dataArr.get(i));
           }

           if(i>=start2-1 && i<=end2-1){
               second.add(dataArr.get(i));
           }

           if(i>=start3-1 && i<=end3-1){
                third.add(dataArr.get(i));
           }

       }

        int maxKey=this.getMaxKey(dataArr,frist);
        int dataArrKey1 = maxKey+start1-1;

        boolean checkRes1 =false;  //第一个波形
        boolean checkRes2 =false;
        boolean checkRes3 =false;

        if(maxKey > 0){
            checkRes1 = this.getlinearRegression(frist,maxKey,dataArr,dataArrKey1);
        }
        int maxKey2 =  this.getMaxKey(dataArr,second);
        int dataArrKey2 = maxKey2 +  start2-1;

        if(maxKey2 >0){
            checkRes2 = this.getlinearRegression(second,maxKey2,dataArr,dataArrKey2);
        }
        int maxKey3 = this.getMaxKey(dataArr,third);
        int dataArrKey3 = maxKey3+start3-1;

        if(maxKey3 >0){
            checkRes3 = this.getlinearRegression(third,maxKey3,dataArr,dataArrKey3);
        }


        if(checkRes1 && checkRes2 && checkRes3){
            return 1;
        }else{
            return  this.checkslopeA(dataArr); //分支识别结果

        }

    }


    /**
     * A项判断左右两边斜率变化
     */
    private int checkslopeA(ArrayList<Integer> point){

        /**
         * 18-28 第一个波峰
         * 第一波形点
         * 18-28 检测第一个波峰
         *
         * 中间点
         * 23
         * 左推10个点
         * 波形趋势左边
         *  8 -23
         * 右推10个点
         * 23-38
         * 波形右边趋势
         /**
         * 162-172 第二个波峰
         * 中间点 167
         *   左右各推10个点
         * 左边趋势
         * 152-167
         * 右边趋势
         *  167- 182
         */


        /**
         * 第三个波峰
         *
         * 中间点  310
         * 左右各推10个点
         *  305-315 第三个波峰
         * 左边趋势
         *   295-310
         * 右边趋势
         * 310 - 325
         *
         */
        ArrayList<Integer> oneLeft = new ArrayList<>();
        ArrayList<Integer> oneArrLeftHafl = new ArrayList<>();
        ArrayList<Integer> oneArrRightHafl = new ArrayList<>();
        ArrayList<Integer> oneRight = new ArrayList<>();
        ArrayList<Integer> twoLeft = new ArrayList<>();
        ArrayList<Integer> twoArrLeftHalf = new ArrayList<>();
        ArrayList<Integer> twoArrRightHalf = new ArrayList<>();
        ArrayList<Integer> twoRight = new ArrayList<>();
        ArrayList<Integer> treeLeft = new ArrayList<>();
        ArrayList<Integer> treeArrLeftHalf = new ArrayList<>();
        ArrayList<Integer> treeArrRightHalf = new ArrayList<>();
        ArrayList<Integer> treeRight = new ArrayList<>();
        /**
         * 第一个脉冲点
         */
        int start1 = 18;
        int mid1 = 23;
        int end1 = 28;

        /**
         * 第二个脉冲点
         * 162-172 第二个波峰
         *          * 中间点 167
         *          *   左右各推10个点
         *          * 左边趋势
         *          * 152-167
         *          * 右边趋势
         *         *  167- 182
         */
        int start2 = 162;
        int mid2= 167;
        int end2 = 172;

        /**
         第三个波峰
         * 中间点  310
         * 左右各推10个点
         *  305-315 第三个波峰
         * 左边趋势
         *   295-310
         * 右边趋势
         * 310 - 325
         */

        int start3 = 305;
        int mid3 = 310;
        int end3 = 315;

        //左边大范围
        for (int i = 0; i < point.size(); i++) {

            if (i >= start1 -10 && i <= mid1) {
                oneLeft.add(point.get(i));
            }

            if (i >= start1  && i <= mid1) {    //左边趋势
                oneArrLeftHafl.add(point.get(i));
            }


            if (i >= mid1 && i <= end1 ) {  //右边趋势
                oneArrRightHafl.add(point.get(i));
            }

            if (i >= mid1 && i <= end1+10) {
                oneRight.add(point.get(i));
            }

            /**
             * 第二点
             */
            if (i >= start2 -10 && i <= mid2) {
                twoLeft.add(point.get(i));
            }

            //第二个点左边趋势
            if (i >= start2  && i <= mid2) {
                twoArrLeftHalf.add(point.get(i));
            }
            //第二个点右边趋势
            if (i >= mid2 && i <= end2) {
                twoArrRightHalf.add(point.get(i));
            }

            if (i >= mid2 && i <= end2+10) {
                twoRight.add(point.get(i));
            }

            /**
             * 第三个波峰
             */
            if(i>= start3 -10 && i<=mid3){
                treeLeft.add(point.get(i));
            }

            if(i> start3  && i< mid3){
                treeArrLeftHalf.add(point.get(i));
            }

            if(i>=mid3  && i<=end3){
                treeArrRightHalf.add(point.get(i));
            }

            if(i>=mid3 && i<=end3+10){
                treeRight.add(point.get(i));
            }

        }

        double oneLeftK = this.getSlope(oneLeft);
        double oneArrLeftHalfK= this.getSlope(oneArrLeftHafl);
        double oneArrRightHalfk = this.getSlope(oneArrRightHafl);
        double oneRightK= this.getSlope(oneRight);

        double twoLeftK= this.getSlope(twoLeft);
        double twoArrLeftHalfK= this.getSlope(twoArrLeftHalf);
        double twoArrRightHalfK= this.getSlope(twoArrRightHalf);
        double twoRightK= this.getSlope(twoRight);


        double treeLeftK = this.getSlope(treeLeft);
        double treeArrLeftHalfK = this.getSlope(treeArrLeftHalf);
        double treeArrRightHalfK = this.getSlope(treeArrRightHalf);
        double treeRightK = this.getSlope(treeRight);

        //曲线判断时 斜率必须至少5条大于0.38否则认为接近直线

        int socer = 0;

        socer+=Math.abs(oneLeftK) >= lineK  ?  1 : 0;
        socer+=Math.abs(oneRightK) >= lineK  ?  1 : 0;
        socer+=Math.abs(twoLeftK) >= lineK  ?  1 : 0;
        socer+=Math.abs(twoRightK) >= lineK  ?  1 : 0;
        socer+=Math.abs(treeLeftK) >= lineK  ?  1 : 0;
        socer+=Math.abs(treeRightK) >= lineK  ?  1 : 0;

        if(socer < 4){
            return  3;   //非分支识别
        }
        //趋势判断
        ArrayList<Boolean> result = new ArrayList<>();

        if(oneArrLeftHalfK - oneArrRightHalfk > 0.8){
            result.add(this.jugger(oneLeftK,oneArrLeftHalfK,"left"));
            result.add(this.jugger(oneRightK,oneArrRightHalfk,"right"));
        }

        if(twoArrLeftHalfK - twoArrRightHalfK > 0.8){
            result.add(this.jugger(twoLeftK,twoArrLeftHalfK,"left"));
            result.add(this.jugger(twoRightK,twoArrRightHalfK,"right"));
        }

        if(treeArrLeftHalfK - treeArrRightHalfK > 0.8){
            result.add(this.jugger(treeLeftK,treeArrLeftHalfK,"left"));
            result.add(this.jugger(treeRightK,treeArrRightHalfK,"right"));
        }


        int num = this.score(result);
        if(num > 3){
            return  1; //分支识别
        }

        return 3; //分支识别失败
    }



    /**
     * B项波形有2个脉冲
     * @param dataArr
     * @return
     */
    private int SingleMeterB(ArrayList<Integer> dataArr){


        /**
         * B项 2个点
         *  72
         *  217
         *  第一个点
         *  67-77
         * 第二个点
         *  212-222
         */


        int start1= 67;
        int end1= 77;
        int start2 = 212;
        int end2 = 222;

        ArrayList<Integer> frist = new ArrayList<>();
        ArrayList<Integer> second = new ArrayList<>();

        for(int i=0;i<dataArr.size();i++){

            if( i>=start1-1 && i<=end1-1 ){
                frist.add(dataArr.get(i));
            }


            if(i>=start2-1 && i<=end2-1){
                second.add(dataArr.get(i));
            }

        }


        int maxKey=this.getMaxKey(dataArr,frist);
        int dataArrKey1 = maxKey+start1-1;

        boolean checkRes1 =false;  //第一个波形
        boolean checkRes2 =false;

        if(maxKey > 0){
            checkRes1 = this.getlinearRegression(frist,maxKey,dataArr,dataArrKey1);
        }
        int maxKey2 =  this.getMaxKey(dataArr,second);
        int dataArrKey2 = maxKey2 +  start2-1;
        if(maxKey2 >0){
            checkRes2 = this.getlinearRegression(second,maxKey2,dataArr,dataArrKey2);
        }

        if(checkRes1 && checkRes2 ){
            return 1;
        }else{
            return  this.checkslopeB(dataArr); //分支识别结果

        }

    }
    private int checkslopeB(ArrayList<Integer> point) {

        /**
         * 第一波形点
         * 67-77 检测第一个波峰
         *
         * 中间点
         * 70
         * 左推10个点
         *
         * 波形趋势左边
         *  57-70
         *
         * 右推10个点
         * 70-87
         * 波形右边趋势
         *
         * 第二个点
         *  212-222
         *中间点
         * 217
         * 左推10个点
         *
         * 波形趋势左边
         *  202-214
         * 右推10个点

         * 波形右边趋势
         *  214 -232
         **/

        ArrayList<Integer> oneLeft = new ArrayList<>();
        ArrayList<Integer> oneArrLeftHafl = new ArrayList<>();
        ArrayList<Integer> oneArrRightHafl = new ArrayList<>();
        ArrayList<Integer> oneRight = new ArrayList<>();
        ArrayList<Integer> twoLeft = new ArrayList<>();
        ArrayList<Integer> twoArrLeftHalf = new ArrayList<>();
        ArrayList<Integer> twoArrRightHalf = new ArrayList<>();
        ArrayList<Integer> twoRight = new ArrayList<>();

        /**
         * B项 2个点
         *  72
         *  217
         *  第一个点
         *  67-77
         * 第二个点
         *  212-222
         */

        int start1 = 67;
        int mid1= 72;
        int end1 =77;

        int start2 =210;
        int mid2= 214;
        int end2=220;


        for(int i=0;i<point.size();i++){

            if (i >= start1 -10 && i <= mid1) {
                oneLeft.add(point.get(i));
            }

            if (i >= start1  && i <= mid1) {    //左边趋势
                oneArrLeftHafl.add(point.get(i));
            }


            if (i >= mid1 && i <= end1 ) {  //右边趋势
                oneArrRightHafl.add(point.get(i));
            }

            if (i >= mid1 && i <= end1+10) {
                oneRight.add(point.get(i));
            }

            /**
             * 第二点
             */
            if (i >= start2 -10 && i <= mid2) {
                twoLeft.add(point.get(i));
            }

            //第二个点左边趋势
            if (i >= start2  && i <= mid2) {
                twoArrLeftHalf.add(point.get(i));
            }
            //第二个点右边趋势
            if (i >= mid2 && i <= end2) {
                twoArrRightHalf.add(point.get(i));
            }

            if (i >= mid2 && i <= end2+10) {
                twoRight.add(point.get(i));
            }

        }

        double oneLeftK = this.getSlope(oneLeft);
        double oneArrLeftHalfK= this.getSlope(oneArrLeftHafl);
        double oneArrRightHalfk = this.getSlope(oneArrRightHafl);
        double oneRightK= this.getSlope(oneRight);

        double twoLeftK= this.getSlope(twoLeft);
        double twoArrLeftHalfK= this.getSlope(twoArrLeftHalf);
        double twoArrRightHalfK= this.getSlope(twoArrRightHalf);
        double twoRightK= this.getSlope(twoRight);



        //曲线判断时 斜率必须至少5条大于0.38否则认为接近直线

        int socer = 0;

        socer+=Math.abs(oneLeftK) >= lineK  ?  1 : 0;
        socer+=Math.abs(oneRightK) >= lineK  ?  1 : 0;
        socer+=Math.abs(twoLeftK) >= lineK  ?  1 : 0;
        socer+=Math.abs(twoRightK) >= lineK  ?  1 : 0;

        if(socer <=2){
            return  3;   //非分支识别
        }

        //趋势判断
        ArrayList<Boolean> result = new ArrayList<>();

        if(oneArrLeftHalfK - oneArrRightHalfk > 0.8){
            result.add(this.jugger(oneLeftK,oneArrLeftHalfK,"left"));
            result.add(this.jugger(oneRightK,oneArrRightHalfk,"right"));
        }

        if(twoArrLeftHalfK - twoArrRightHalfK > 0.8){
            result.add(this.jugger(twoLeftK,twoArrLeftHalfK,"left"));
            result.add(this.jugger(twoRightK,twoArrRightHalfK,"right"));
        }



        int num = this.score(result);
        if(num >=2){
            return  1; //分支识别
        }

        return 3; //分支识别失败


    }



    /**
     * C项波形有2个脉冲
     * @param dataArr
     * @return
     */
    private int SingleMeterC(ArrayList<Integer> dataArr){


        /**
         * B项 2个点
         *  72
         *  217
         *  第一个点
         *  67-77
         * 第二个点
         *  212-222
         */
        int start1 = 115;
        int end1 = 125;
        int start2 = 260;
        int end2 = 270;

        ArrayList<Integer> frist = new ArrayList<>();
        ArrayList<Integer> second = new ArrayList<>();

        for(int i=0;i<dataArr.size();i++){

            if( i>=start1-1 && i<=end1-1 ){
                frist.add(dataArr.get(i));
            }


            if(i>=start2-1 && i<=end2-1){
                second.add(dataArr.get(i));
            }

        }


        int maxKey=this.getMaxKey(dataArr,frist);
        int dataArrKey1 = maxKey+start1-1;

        boolean checkRes1 =false;  //第一个波形
        boolean checkRes2 =false;

        if(maxKey > 0){
            checkRes1 = this.getlinearRegression(frist,maxKey,dataArr,dataArrKey1);
        }
        int maxKey2 =  this.getMaxKey(dataArr,second);
        int dataArrKey2 = maxKey2 +  start2-1;
        if(maxKey2 >0){
            checkRes2 = this.getlinearRegression(second,maxKey2,dataArr,dataArrKey2);
        }

        if(checkRes1 && checkRes2 ){
            return 1;
        }else{
            return  this.checkslopeC(dataArr); //分支识别结果

        }

    }

    /**
     * c項分支判断
     * @param point
     * @return
     */

    private int checkslopeC(ArrayList<Integer> point){
        /**
         *C项 2个点
         *  120
         *  265
         *  第一个点
         *  115-120-125
         * 第二个点
         *  260-265-270
         */



        /**
         *
         *
         *
         *
         *
         *
         *
         *
         * 第一波形点
         * 115-125 检测第一个波峰
         *
         * 中间点
         * 120
         * 左推10个点
         *
         * 波形趋势左边
         *  105-120
         *
         * 右推10个点
         * 120-135
         * 波形右边趋势
         *
         * 第二个点
         * 260-265-270
         *中间点
         * 265
         * 左推10个点
         *
         * 波形趋势左边
         *  250-265
         * 右推10个点

         * 波形右边趋势
         *  265 -280
         **/


            ArrayList<Integer> oneLeft = new ArrayList<>();
            ArrayList<Integer> oneArrLeftHafl = new ArrayList<>();
            ArrayList<Integer> oneArrRightHafl = new ArrayList<>();
            ArrayList<Integer> oneRight = new ArrayList<>();
            ArrayList<Integer> twoLeft = new ArrayList<>();
            ArrayList<Integer> twoArrLeftHalf = new ArrayList<>();
            ArrayList<Integer> twoArrRightHalf = new ArrayList<>();
            ArrayList<Integer> twoRight = new ArrayList<>();

            /**
             * B项 2个点
             *  72
             *  217
             *  第一个点
             *  67-77
             * 第二个点
             *  212-222
             */

            int start1 = 115;
            int mid1= 120;
            int end1 =125;

            int start2 =260;
            int mid2= 265;
            int end2=270;

            for(int i=0;i<point.size();i++){
                if (i >= start1 -10 && i <= mid1) {
                    oneLeft.add(point.get(i));
                }

                if (i >= start1 && i <= mid1) {    //左边趋势
                    oneArrLeftHafl.add(point.get(i));
                }

                if (i >= mid1 && i <= end1 ) {  //右边趋势
                    oneArrRightHafl.add(point.get(i));
                }

                if (i >= mid1 && i <= end1+10) {
                    oneRight.add(point.get(i));
                }

                /**
                 * 第二点
                 */
                if (i >= start2-10 && i <= mid2) {
                    twoLeft.add(point.get(i));
                }

                //第二个点左边趋势
                if (i >= start2  && i <= mid2) {
                    twoArrLeftHalf.add(point.get(i));
                }
                //第二个点右边趋势
                if (i >= mid2 && i <= end2) {
                    twoArrRightHalf.add(point.get(i));
                }

                if (i >= mid2 && i <= end2+10) {
                    twoRight.add(point.get(i));
                }

            }



            double oneLeftK = this.getSlope(oneLeft);
            double oneArrLeftHalfK= this.getSlope(oneArrLeftHafl);
            double oneArrRightHalfk = this.getSlope(oneArrRightHafl);
            double oneRightK= this.getSlope(oneRight);

            double twoLeftK= this.getSlope(twoLeft);
            double twoArrLeftHalfK= this.getSlope(twoArrLeftHalf);
            double twoArrRightHalfK= this.getSlope(twoArrRightHalf);
            double twoRightK= this.getSlope(twoRight);

            //曲线判断时 斜率必须至少5条大于0.38否则认为接近直线

            int socer = 0;

            socer+=Math.abs(oneLeftK) >= lineK  ?  1 : 0;
            socer+=Math.abs(oneRightK) >= lineK  ?  1 : 0;
            socer+=Math.abs(twoLeftK) >= lineK  ?  1 : 0;
            socer+=Math.abs(twoRightK) >= lineK  ?  1 : 0;

            if(socer <=2){
                return  3;   //非分支识别
            }
            //趋势判断
            ArrayList<Boolean> result = new ArrayList<>();

            if(oneArrLeftHalfK - oneArrRightHalfk > 0.8){
                result.add(this.jugger(oneLeftK,oneArrLeftHalfK,"left"));
                result.add(this.jugger(oneRightK,oneArrRightHalfk,"right"));
            }

            if(twoArrLeftHalfK - twoArrRightHalfK > 0.8){
                result.add(this.jugger(twoLeftK,twoArrLeftHalfK,"left"));
                result.add(this.jugger(twoRightK,twoArrRightHalfK,"right"));
            }
            int num = this.score(result);
            if(num >=2){
                return  1; //分支识别
            }

            return 3; //分支识别失败

    }

    /**
     *
     * @param k 延续点趋势
     * @param comK 波形脉冲点趋势
     * @param type
     * @return
     */
    //modify by mrqi
    private boolean jugger(double k,double comK,String type){
        boolean res = false;

        switch (type){
            case "left"://左侧为上升趋势
                //脉冲信号大于干扰信号
                if(k <0 && comK >0 ||  k>0 && comK >0){
                   res = comK > k  &&comK -k >=0.8 ?  true :false;
                }

                //脉冲信号小于干扰信号  左边趋势为下降趋势 脉冲点位上升趋势
                if(k <0 && comK <0){
                   res = Math.abs(k) > Math.abs(comK)  &&  Math.abs(k- comK) >= 0.8 ?  true :false;
                }


                return res;

            case "right": //右侧为下降沿趋势
                //1.脉冲信号大于干扰信号
                if(comK <0 && k >0 || comK <0 && k<0){
                    res = Math.abs(comK)  > Math.abs(k)  && Math.abs(k-comK) >= 0.8 ? true:false;
                }

                //脉冲信号小于干扰信号
                if(comK > 0  && k >0){
                    res = Math.abs(k) > Math.abs(comK) && Math.abs(k - comK) >= 0.8 ?  true : false;
                }


                return res;
        }
        return res;
    }


    /**
     *
     * @param reslut
     * @return
     */
    private int score(ArrayList<Boolean> reslut){
        int num =0;
        for(int i=0;i<reslut.size();i++){
            if(reslut.get(i)){
                num++;
            }
        }
        return num;
    }


    /**
     *
     * II采集与电表关系结构图
     * @param jsonArray
     */


    public String Analysis(JSONArray jsonArray){

        ArrayList<Map<String, String>> $IIcai = new ArrayList<>();
        ArrayList<Map<String,String>> $mater = new ArrayList<>();

        $IIcai=this.branch(jsonArray,1);
        $mater=this.branch(jsonArray,2);
        //只有一个II采的情况没有对应关系
       // System.out.println($mater);
         if($IIcai.isEmpty()){
             for ( Object obj: jsonArray) {
                 HashMap<String, String> IImap = new HashMap<>();
                 JSONObject jsonObj = (JSONObject) obj;
                 IImap.put("qrcode",(String) jsonObj.get("IIqrcode"));
                 IImap.put("parent","");
                 $IIcai.add(IImap);
             }

         }

        return  this.getTree($IIcai,$mater);

    }

    /**
     * type 1  II采集与II采的关系
     * type 2 电表与II采的关系
     * @param jsonArray
     * @param type
     * @return
     */

    public ArrayList<Map<String ,String>> branch(JSONArray jsonArray ,int type ){

        ArrayList<Map<String,String>> $mater = new ArrayList<>();  //电表和II采集关系
        ArrayList<Map<String,String>> $IIcai = new ArrayList<>();//II采和II采关系
        switch (type){
            case 1:
                for ( Object obj : jsonArray){
                    HashMap<String, String> IImap = new HashMap<>();
                    JSONObject jsonObj = (JSONObject) obj;
                    if(jsonObj.get("ao").equals("0")) continue;
                    if((boolean)jsonObj.get("isFork")){
                        IImap.put("qrcode",(String) jsonObj.get("qrcode"));
                        IImap.put("parent", (String) jsonObj.get("IIqrcode"));
                        //parent 是list
                    }else{
                        IImap.put("qrcode",(String) jsonObj.get("qrcode"));
                        IImap.put("parent","");
                    }
                    IImap.put("ao", (String) jsonObj.get("ao"));
                   //重复不添加
                        $IIcai.add(IImap);

                }
                return $IIcai;
            case 2:
                for ( Object obj : jsonArray){
                    JSONObject jsonObj = (JSONObject) obj;
                    if(jsonObj.get("ao").equals("1"))continue;
                    HashMap<String, String> map = new HashMap<>();
                    if((boolean)jsonObj.get("isFork")){
                        map.put("qrcode",(String) jsonObj.get("qrcode"));
                        map.put("parent", (String) jsonObj.get("IIqrcode"));
                    }else{
                        map.put("qrcode",(String) jsonObj.get("qrcode"));
                        //map.put("parent","");
                    }
                    //重复不添加
                    map.put("ao", (String) jsonObj.get("ao"));

                    $mater.add(map);
                }
                return $mater;
        }
        return new ArrayList<>();
    }
    /**
     *   生成树形结构json
     * @param IIcai
     * @param meter
     * @return
     */
    public String getTree(ArrayList<Map<String,String>> IIcai , ArrayList<Map<String,String>> meter) {
        //II采集上下层级关系
        HashMap<String, HashMap> IIMap = new HashMap<>();
        for (Map<String, String> value : IIcai) {
            String parentCode = value.get("parent");
            if ((parentCode == null || parentCode.length() <= 0) && !IIMap.containsKey(value.get("qrcode"))) {  //第一次入集合
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("qrcode", value.get("qrcode"));
                hashMap.put("lev", (int) 1);
                hashMap.put("meter",new ArrayList<>()); //电表列表
                hashMap.put("child",new ArrayList<>()); //子类列表
                hashMap.put("parent",new ArrayList<>()); //父类列表
                IIMap.put(value.get("qrcode"), hashMap);
            } else { //有父类或者已经存在II采在集合中的情况
                if (IIMap.containsKey(value.get("qrcode"))) {
                    int lev = (int) IIMap.get(value.get("qrcode")).get("lev") + 1;
                    ArrayList parentList = (ArrayList) IIMap.get(value.get("qrcode")).get("parent");
                    if(parentList != null && !value.get("parent").isEmpty()) {
                        parentList.add(value.get("parent"));
                    }
                } else {   //存在父类元素第一次入集合
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("qrcode", value.get("qrcode"));
                    hashMap.put("parent",new ArrayList<String>(){{add(value.get("parent"));}});
                    hashMap.put("lev", (int) 2);  //有父类起码2级开始
                    hashMap.put("meter",new ArrayList<>()); //电表列表
                    hashMap.put("child",new ArrayList<>());//子类列表
                    IIMap.put(value.get("qrcode"), hashMap);

                }

            }
        }

        /**
         * 递归排序lev
         * 有几个parentLIST就是几级别
         */
        for (String qrcodeIndex :IIMap.keySet()) {
            if (IIMap.get(qrcodeIndex).containsKey("parent")) {  //存在父节点列表
                ArrayList parentList = (ArrayList) IIMap.get(qrcodeIndex).get("parent");
                if (IIMap.get(qrcodeIndex).containsKey("lev")) {
                    IIMap.get(qrcodeIndex).put("lev", (int) parentList.size() + 1);
                }

            }
        }

        //II采与电表的层级关系
        ArrayList<HashMap> meterTree = new ArrayList<>();
        HashMap<String, HashMap<String,ArrayList>> meterMap = new HashMap<>();
        for (Map<String,String > value :meter) {
            String parentCode =  value.get("parent");
            if(parentCode == null || parentCode.length() <=0){
                continue;  //电表一般不存在父节点为空的情况
            }
            //已经存在键
            if(meterMap.containsKey(value.get("qrcode"))){
                if(!meterMap.get(value.get("qrcode")).get("parent").contains(value.get("parent"))){   //不存在parent 新增
                    meterMap.get(value.get("qrcode")).get("parent").add(value.get("parent"));
                }
            }else{
                HashMap<String , ArrayList> inlineHshmap = new HashMap<>();  //父类型列表
                ArrayList<String> prentList = new ArrayList<>();
                prentList.add(value.get("parent"));
                inlineHshmap.put("parent",prentList);
                meterMap.put(value.get("qrcode"),inlineHshmap);
            }
        }

        //把电表挂在II采叶节点（最深处）
        HashMap<String, HashMap> $IICaitree = new HashMap<>();
        $IICaitree.putAll(IIMap);
        for ( String qrcode : meterMap.keySet()) {
            //找到等级最高的II采
            ArrayList<String> IICaiList = new ArrayList<>();
            IICaiList = meterMap.get(qrcode).get("parent");
            String MaxLevIIqrcode = ""; //最深处II采集CODE
            int MaxLev = 0;

            //最深处II采
            for (String qrIIcode : IICaiList) {
                if(IIMap.containsKey(qrIIcode)) {
                    MaxLev = ((int) IIMap.get(qrIIcode).get("lev") >= MaxLev) ? (int) IIMap.get(qrIIcode).get("lev") : MaxLev;
                    MaxLevIIqrcode = ((int) IIMap.get(qrIIcode).get("lev") >= MaxLev) ? qrIIcode : MaxLevIIqrcode;
                }
            }
            /**
             * 最深处II采集
             */
            if($IICaitree.containsKey(MaxLevIIqrcode)){
                ArrayList meterList = (ArrayList) $IICaitree.get(MaxLevIIqrcode).get("meter");
                meterList.add(qrcode);
            }

        }
        //II采挂到上一级二采下面(深度优先)
        int MAXOUTLEV =0;
        for (String qrIIcode : $IICaitree.keySet()){
            MAXOUTLEV = ((int)IIMap.get(qrIIcode).get("lev") >= MAXOUTLEV) ? (int)IIMap.get(qrIIcode).get("lev") : MAXOUTLEV;
        }

        //深度优先
        while (MAXOUTLEV > 1){
            for (String qrIIcode : $IICaitree.keySet()){
                //挂载到上一层II采
                if((int)IIMap.get(qrIIcode).get("lev") == MAXOUTLEV){
                    ArrayList<String> parentIIcaiList = (ArrayList) $IICaitree.get(qrIIcode).get("parent");
                    String MaxLevIIqrcode = ""; //最深处II采集CODE
                    int MaxLev = 0;
                    //上一层II采
                    for (String IIcaiPrentqrcode : parentIIcaiList) {
                        if(IIMap.containsKey(IIcaiPrentqrcode)) {
                            MaxLev = ((int) IIMap.get(IIcaiPrentqrcode).get("lev") >= MaxLev) ? (int) IIMap.get(IIcaiPrentqrcode).get("lev") : MaxLev;
                            MaxLevIIqrcode = ((int) IIMap.get(IIcaiPrentqrcode).get("lev") >= MaxLev) ? IIcaiPrentqrcode : MaxLevIIqrcode;
                        }
                    }
                    //挂载II采
                    if($IICaitree.containsKey(MaxLevIIqrcode)){
                        ArrayList childList = (ArrayList) $IICaitree.get(MaxLevIIqrcode).get("child");
                        childList.add($IICaitree.get(qrIIcode));
                    }

                }
            }

            MAXOUTLEV--;
        }
        /**
         * 移除树枝
         */
    Iterator<Map.Entry<String, HashMap>> it = $IICaitree.entrySet().iterator();
        while(it.hasNext()){
        Map.Entry<String, HashMap> entry = it.next();
        HashMap inlineMap =  entry.getValue();
        if((int)inlineMap.get("lev") > 1){
            it.remove();
        }
    }

       return JSON.toJSONString($IICaitree);

}

}
